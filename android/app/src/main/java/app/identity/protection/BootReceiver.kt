package app.identity.protection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService

/**
 * Brings protection back after the phone restarts or Identity is updated (M3.4), if the user had it
 * on. It only starts what the user already allowed: with no VPN permission, or no "wants it on"
 * choice, it does nothing (Home then shows "Interrupted").
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (!ProtectionPrefs(context).wantsProtection) return
        if (VpnService.prepare(context) != null) return // permission gone: cannot start without the user
        // Installing an update ends the old session on purpose; only a restart or a crash is an interruption.
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) ProtectionPrefs(context).sessionOpen = false
        IdentityVpnService.start(context, byUser = false)
    }
}
