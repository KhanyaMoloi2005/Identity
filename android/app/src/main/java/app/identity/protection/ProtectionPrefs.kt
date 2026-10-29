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

    /**
     * True from when protection starts until the user (or Android's VPN settings) ends it on purpose.
     * Still true at the next start means the last session never ended cleanly (M3.5).
     */
    var sessionOpen: Boolean
        get() = prefs.getBoolean(KEY_SESSION_OPEN, false)
        set(value) = prefs.edit().putBoolean(KEY_SESSION_OPEN, value).apply()

    private companion object {
        const val KEY_SESSION_OPEN = "session_open"
        const val FILE = "protection"
        const val KEY_WANTS = "wants_protection"
    }
}
