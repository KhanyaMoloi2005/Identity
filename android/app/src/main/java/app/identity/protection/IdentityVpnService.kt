package app.identity.protection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.identity.MainActivity
import app.identity.R
import app.identity.filter.BlockPresets
import app.identity.filter.DnsFilter
import app.identity.filter.DomainMatcher
import app.identity.filter.DomainSet
import app.identity.filter.InterruptionLog
import app.identity.filter.ProtectionHealth
import app.identity.filter.PublicResolvers
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Identity's on-device, DNS-only VPN.
 *
 * It tells Android to use a private DNS address ([DNS_ADDRESS]) and routes only that address into
 * the tunnel, so DNS queries come to us and all other traffic bypasses the VPN as before.
 * [DnsTunnel] answers names on the bundled [Blocklist] and the user's own rules itself, and forwards the rest.
 */
class IdentityVpnService : VpnService() {

    private var tunnel: ParcelFileDescriptor? = null
    private var dnsTunnel: DnsTunnel? = null
    private var tiktokMonitor: TikTokLimitMonitor? = null
    private var healthMonitor: HealthMonitor? = null
    private var updateExecutor: ExecutorService? = null
    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateCheck = Runnable { maybeCheckForUpdate() }

    // Loaded once per protection session; a rule change only rebuilds the small custom part.
    @Volatile private var bundled: DomainSet? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Anything other than "stop" or "reload" means start: the user, the boot receiver, Android
        // restarting us after ending the service (null intent), or Always-on VPN.
        when (intent?.action) {
            ACTION_STOP -> stopProtection()
            ACTION_BLOCKLIST_CHANGED -> if (tunnel != null) {
                bundled = null // read the list again, including the newly installed update
                dnsTunnel?.reload()
            }
            ACTION_RELOAD -> if (tunnel == null) {
                stopSelf()
            } else {
                dnsTunnel?.reload()
                tiktokMonitor?.refresh()
            }
            else -> {
                val choice = ProtectionPrefs(this)
                if (isAlwaysOnVpn()) choice.wantsProtection = true // Android itself started us for Always-on VPN
                if (choice.wantsProtection) startProtection() else stopSelf()
            }
        }
        // If Android ends the service while the user wants protection on, it restarts us (M3.4).
        return if (ProtectionPrefs(this).wantsProtection) START_STICKY else START_NOT_STICKY
    }

    override fun onRevoke() {
        // Called when the user turns us off in system Settings or another VPN app takes over.
        // Respect that: don't bring protection back by itself after the next restart.
        ProtectionPrefs(this).apply {
            wantsProtection = false
            sessionOpen = false
        }
        InterruptionStore(this).record(InterruptionLog.Kind.TURNED_OFF_IN_ANDROID)
        stopProtection()
    }

    override fun onDestroy() {
        closeTunnel()
        super.onDestroy()
    }

    private fun startProtection() {
        if (tunnel != null) return
        // prepare() returns null only when the user has already allowed Identity's VPN.
        if (prepare(this) != null) {
            stopProtection()
            return
        }
        try {
            showNotification()
            tunnel = Builder()
                .setSession(getString(R.string.app_name))
                .addAddress(TUNNEL_ADDRESS, 32)
                .addDnsServer(DNS_ADDRESS)
                .addRoute(DNS_ADDRESS, 32) // our own DNS address
                .apply { routePublicResolvers() }
                .establish()
            tunnel?.let { fd ->
                dnsTunnel = DnsTunnel(fd, ::createFilter, ::protect).also { it.start() }
                tiktokMonitor = TikTokLimitMonitor(this) { dnsTunnel?.reload() }.also { it.start() }
            }
        } catch (e: Exception) {
            // Android refused (e.g. permission revoked a moment ago) or the tunnel couldn't be set up.
            // Stop cleanly rather than crash.
            closeTunnel()
        }
        if (tunnel == null) {
            stopProtection()
        } else {
            isRunning = true
            alwaysOn = isAlwaysOnVpn()
            val choice = ProtectionPrefs(this)
            // Still open from last time: that session never ended on purpose (Android stopped us or the phone restarted).
            if (choice.sessionOpen) InterruptionStore(this).record(InterruptionLog.Kind.ENDED_UNEXPECTEDLY)
            choice.sessionOpen = true
            healthMonitor = HealthMonitor(this, { dnsTunnel }) { report -> health = report }.also { it.start() }
            updateExecutor = Executors.newSingleThreadExecutor()
            updateHandler.postDelayed(updateCheck, FIRST_UPDATE_CHECK_MS)
        }
    }

    /**
     * Also route the well-known public DNS resolvers into the tunnel (M3.7b, D-036): plain DNS sent
     * to them is answered by Identity's filter, and encrypted DNS to them is refused, so a browser's
     * "secure DNS" or an app's own DNS server can't go around Identity. Only these few addresses:
     * all other traffic still bypasses the VPN.
     */
    private fun Builder.routePublicResolvers() {
        for (address in PublicResolvers.IPV4) addRoute(address, 32)
        addAddress(TUNNEL_ADDRESS_V6, 128) // a source address, so IPv6 connections to the resolvers also enter the tunnel
        for (address in PublicResolvers.IPV6) addRoute(address, 128)
    }

    /** Whether the user has set Identity as Android's Always-on VPN (Android 10+). */
    private fun isAlwaysOnVpn(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isAlwaysOn

    /** Runs on a background thread, one call at a time. The lists stay in memory only while protection is on. */
    @Synchronized
    private fun createFilter(): DnsFilter {
        val list = bundled ?: Blocklist.load(this).also { bundled = it }
        // TikTok's servers are blocked only once today's allowance is used up (D-030).
        val tiktok = if (TikTokLimit(this).isOver()) DomainSet.of(BlockPresets.TIKTOK) else DomainSet.EMPTY
        return DnsFilter(
            Blocklist.withCustomRules(list, CustomRulesStore(this).load(), tiktok),
            DomainMatcher(BlockPresets.DOH_ENDPOINTS),
        )
    }

    private fun stopProtection() {
        closeTunnel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Once a day at most, while protection runs: look for a newer signed blocklist (M3.6). The timer doesn't wake a sleeping phone. */
    private fun maybeCheckForUpdate() {
        val executor = updateExecutor ?: return
        val due = System.currentTimeMillis() - ProtectionPrefs(this).lastUpdateCheckMs >= UPDATE_INTERVAL_MS
        if (due) {
            executor.execute {
                if (BlocklistUpdater(this).checkNow() is BlocklistUpdater.Outcome.Updated) {
                    updateHandler.post {
                        bundled = null
                        dnsTunnel?.reload()
                    }
                }
            }
        }
        updateHandler.postDelayed(updateCheck, UPDATE_RECHECK_MS)
    }

    private fun closeTunnel() {
        updateHandler.removeCallbacks(updateCheck)
        updateExecutor?.shutdownNow()
        updateExecutor = null
        healthMonitor?.stop()
        healthMonitor = null
        tiktokMonitor?.stop()
        tiktokMonitor = null
        dnsTunnel?.stop()
        dnsTunnel = null
        tunnel?.close()
        tunnel = null
        isRunning = false
        alwaysOn = false
        health = null
    }

    /** The visible "Identity protection is on" notification. Android requires it for a long-running service. */
    private fun showNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.protection_channel_name),
                NotificationManager.IMPORTANCE_LOW, // shown, but silent
            ),
        )
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_protection)
            .setContentTitle(getString(R.string.protection_notification_title))
            .setContentText(getString(R.string.protection_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: "systemExempted" is the type for VPN apps the user has allowed (D-022).
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val ACTION_STOP = "app.identity.protection.STOP"
        private const val ACTION_RELOAD = "app.identity.protection.RELOAD"
        private const val ACTION_BLOCKLIST_CHANGED = "app.identity.protection.BLOCKLIST_CHANGED"
        private const val FIRST_UPDATE_CHECK_MS = 2 * 60 * 1000L
        private const val UPDATE_RECHECK_MS = 6 * 60 * 60 * 1000L
        private const val UPDATE_INTERVAL_MS = 24 * 60 * 60 * 1000L
        private const val CHANNEL_ID = "protection"
        private const val NOTIFICATION_ID = 1

        /** Private addresses: the phone's end of the tunnel, and the DNS server only we answer. */
        private const val TUNNEL_ADDRESS = "10.111.222.2"
        private const val TUNNEL_ADDRESS_V6 = "fd69:6465:6e74:1::2"
        private const val DNS_ADDRESS = "10.111.222.1"

        /**
         * Whether the tunnel is up, for the Home screen. Compose state, so the screen redraws when it
         * changes. Only true while this process runs; M3 replaces it with a proper status monitor.
         */
        var isRunning by mutableStateOf(false)
            private set

        /** Whether Android's "Always-on VPN" is set for Identity. Only known while protection runs. */
        var alwaysOn by mutableStateOf(false)
            private set

        /** What protection is really doing, checked every 20 seconds while it runs (M3.5). Null while it isn't running. */
        var health by mutableStateOf<ProtectionHealth.Report?>(null)
            private set

        /**
         * Call only after the user has allowed the VPN (see [VpnService.prepare]). [byUser] is false
         * when the phone restarting or an app update starts it again, so that isn't logged as a turn-on.
         */
        fun start(context: Context, byUser: Boolean = true) {
            if (byUser) InterruptionStore(context).record(InterruptionLog.Kind.TURNED_ON)
            ProtectionPrefs(context).wantsProtection = true
            context.startService(Intent(context, IdentityVpnService::class.java))
        }

        /**
         * Called whenever the app comes to the front. If the user wants protection on but it isn't
         * running (Android ended it and hasn't restarted it yet), start it again now (M3.4/3.5).
         */
        fun resumeIfWanted(context: Context) {
            if (isRunning || !ProtectionPrefs(context).wantsProtection) return
            if (prepare(context) != null) return // VPN permission gone: only the user can grant it again
            start(context, byUser = false)
        }

        /** Tells running protection that a new blocklist was installed (from the Settings button). */
        fun blocklistChanged(context: Context) {
            if (isRunning) context.startService(Intent(context, IdentityVpnService::class.java).setAction(ACTION_BLOCKLIST_CHANGED))
        }

        /** Applies changed custom rules to running protection. Does nothing when protection is off. */
        fun reload(context: Context) {
            if (isRunning) context.startService(Intent(context, IdentityVpnService::class.java).setAction(ACTION_RELOAD))
        }

        fun stop(context: Context) {
            ProtectionPrefs(context).apply {
                wantsProtection = false
                sessionOpen = false
            }
            InterruptionStore(context).record(InterruptionLog.Kind.TURNED_OFF)
            context.startService(Intent(context, IdentityVpnService::class.java).setAction(ACTION_STOP))
        }
    }
}
