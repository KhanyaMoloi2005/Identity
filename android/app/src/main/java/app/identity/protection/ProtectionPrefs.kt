package app.identity.protection

import android.content.Context

/**
 * Whether the user wants protection on (M3.4). Set when they turn it on, cleared when they turn it
 * off or turn the VPN off in Android's settings. It is what lets protection come back by itself
 * after a restart, an app update or Android ending the service, and only then. Nothing else is saved.
 */
internal class ProtectionPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var wantsProtection: Boolean
        get() = prefs.getBoolean(KEY_WANTS, false)
        set(value) = prefs.edit().putBoolean(KEY_WANTS, value).apply()

    private companion object {
        const val FILE = "protection"
        const val KEY_WANTS = "wants_protection"
    }
}
