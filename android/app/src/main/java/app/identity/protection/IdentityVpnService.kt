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
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.identity.MainActivity
import app.identity.R
import app.identity.filter.BlockPresets
import app.identity.filter.DnsFilter
import app.identity.filter.DomainSet

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

    // Loaded once per protection session; a rule change only rebuilds the small custom part.
    @Volatile private var bundled: DomainSet? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Anything other than "stop" or "reload" means start: the user, the boot receiver, Android
        // restarting us after ending the service (null intent), or Always-on VPN.
        when (intent?.action) {
            ACTION_STOP -> stopProtection()
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
        ProtectionPrefs(this).wantsProtection = false
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
                .addRoute(DNS_ADDRESS, 32) // only DNS enters the tunnel
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
        }
    }

    /** Whether the user has set Identity as Android's Always-on VPN (Android 10+). */
    private fun isAlwaysOnVpn(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isAlwaysOn

    /** Runs on a background thread, one call at a time. The lists stay in memory only while protection is on. */
    @Synchronized
    private fun createFilter(): DnsFilter {
        val list = bundled ?: Blocklist.load(this).also { bundled = it }
        // TikTok's servers are blocked only once today's allowance is used up (D-030).
        val tiktok = if (TikTokLimit(this).isOver()) DomainSet.of(BlockPresets.TIKTOK) else DomainSet.EMPTY
        return DnsFilter(Blocklist.withCustomRules(list, CustomRulesStore(this).load(), tiktok))
    }

    private fun stopProtection() {
        closeTunnel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun closeTunnel() {
        tiktokMonitor?.stop()
        tiktokMonitor = null
        dnsTunnel?.stop()
        dnsTunnel = null
        tunnel?.close()
        tunnel = null
        isRunning = false
        alwaysOn = false
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
        private const val CHANNEL_ID = "protection"
        private const val NOTIFICATION_ID = 1

        /** Private addresses: the phone's end of the tunnel, and the DNS server only we answer. */
        private const val TUNNEL_ADDRESS = "10.111.222.2"
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

        /** Call only after the user has allowed the VPN (see [VpnService.prepare]). */
        fun start(context: Context) {
            ProtectionPrefs(context).wantsProtection = true
            context.startService(Intent(context, IdentityVpnService::class.java))
        }

        /** Applies changed custom rules to running protection. Does nothing when protection is off. */
        fun reload(context: Context) {
            if (isRunning) context.startService(Intent(context, IdentityVpnService::class.java).setAction(ACTION_RELOAD))
        }

        fun stop(context: Context) {
            ProtectionPrefs(context).wantsProtection = false
            context.startService(Intent(context, IdentityVpnService::class.java).setAction(ACTION_STOP))
        }
    }
}
