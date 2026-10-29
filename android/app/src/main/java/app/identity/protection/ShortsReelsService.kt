package app.identity.protection

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import app.identity.R
import app.identity.filter.ShortsReels

/**
 * Takes the user out of YouTube Shorts and Instagram Reels (M3.3c-2, D-031).
 *
 * Narrow on purpose: Android only delivers events from YouTube and Instagram (see
 * `res/xml/shorts_reels_service.xml`), and the only thing looked for is whether a Shorts/Reels
 * screen marker is on screen ([ShortsReels]). No text is read, and nothing is stored, logged or sent.
 * The one action is Back, or Home if Back didn't leave. The user can open the app again at once.
 */
class ShortsReelsService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val switches by lazy { ShortsReelsPrefs(this) }
    private var lastCheckMs = 0L
    private var lastActionMs = 0L
    private var lastActionPackage: String? = null
    private var checkQueued = false

    private val delayedCheck = Runnable {
        checkQueued = false
        check()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (ShortsReels.markerIds(pkg) == null || !switches.isOn(pkg)) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastCheckMs >= CHECK_INTERVAL_MS) {
            check()
        } else if (!checkQueued) {
            // Busy screens send events all the time; make sure the last one is still looked at.
            checkQueued = true
            handler.postDelayed(delayedCheck, CHECK_INTERVAL_MS)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(delayedCheck)
        super.onDestroy()
    }

    private fun check() {
        lastCheckMs = SystemClock.elapsedRealtime()
        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString() ?: return
        val markers = ShortsReels.markerIds(pkg) ?: return // some other app is in front
        if (!switches.isOn(pkg)) return // the user switched this one off since the event arrived
        if (markers.any { root.findAccessibilityNodeInfosByViewId(it).isNotEmpty() }) leave(pkg)
    }

    private fun leave(pkg: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastActionMs < ACTION_COOLDOWN_MS) return // let the last action finish first
        val stillThere = pkg == lastActionPackage && now - lastActionMs < ESCALATE_WITHIN_MS
        lastActionMs = now
        lastActionPackage = pkg
        // Back usually returns to the app's home screen. If it didn't leave, close to the launcher.
        performGlobalAction(if (stillThere) GLOBAL_ACTION_HOME else GLOBAL_ACTION_BACK)
        Toast.makeText(this, R.string.shorts_blocked_toast, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 300L
        const val ACTION_COOLDOWN_MS = 1_200L
        const val ESCALATE_WITHIN_MS = 4_000L
    }
}
