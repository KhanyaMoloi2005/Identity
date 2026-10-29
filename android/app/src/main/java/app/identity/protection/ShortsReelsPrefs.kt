package app.identity.protection

import android.content.Context
import app.identity.filter.ShortsReels

/**
 * The two switches for the Shorts/Reels blocker (D-031): one for YouTube Shorts, one for Instagram
 * Reels. Both are on until the user turns them off. The setting is the only thing saved, in the
 * app's private preferences (excluded from backup).
 */
internal class ShortsReelsPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Whether [packageName] (YouTube or Instagram) should be kept out of Shorts/Reels. */
    fun isOn(packageName: String): Boolean = prefs.getBoolean(packageName, true)

    fun setOn(packageName: String, on: Boolean) {
        if (packageName in ShortsReels.WATCHED_PACKAGES) prefs.edit().putBoolean(packageName, on).apply()
    }

    private companion object {
        const val FILE = "shorts-reels"
    }
}
