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

    /** When the blocklist update address was last answered (M3.6). Failed connections don't count. */
    var lastUpdateCheckMs: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, value).apply()

    /** Blocklist update attempts in a row that couldn't connect. */
    var updateFailures: Int
        get() = prefs.getInt(KEY_UPDATE_FAILURES, 0)
        set(value) = prefs.edit().putInt(KEY_UPDATE_FAILURES, value).apply()

    private companion object {
        const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        const val KEY_UPDATE_FAILURES = "update_failures"
        const val KEY_SESSION_OPEN = "session_open"
        const val FILE = "protection"
        const val KEY_WANTS = "wants_protection"
    }
}
