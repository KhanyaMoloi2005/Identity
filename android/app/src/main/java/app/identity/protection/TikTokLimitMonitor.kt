package app.identity.protection

import android.content.Context
import android.os.Handler
import android.os.Looper
import app.identity.filter.TimeLimit

/**
 * Watches TikTok's time while protection is on (M3.3c-1, D-030). When the daily allowance is used
 * up it tells the service (so TikTok's servers get blocked) and covers TikTok with
 * [LimitReachedOverlay] whenever it is in front.
 *
 * Checks run on a timer that doesn't wake a sleeping phone, and not at all until the user has
 * chosen an allowance and given usage access. Nothing is logged or saved except the allowance.
 */
internal class TikTokLimitMonitor(
    context: Context,
    /** Called when the allowance becomes used up, or stops being used up (a new day, a raise). */
    private val onOverChanged: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val limit = TikTokLimit(appContext)
    private val overlay = LimitReachedOverlay(appContext)
    private val handler = Handler(Looper.getMainLooper())
    private var wasOver: Boolean? = null
    private var running = false

    private val check = Runnable { tick() }

    fun start() {
        running = true
        refresh()
    }

    /** Picks up a changed allowance or newly granted permission straight away. */
    fun refresh() {
        handler.removeCallbacks(check)
        if (running) tick()
    }

    fun stop() {
        running = false
        handler.removeCallbacks(check)
        overlay.hide()
    }

    private fun tick() {
        if (limit.allowance() == null || !TikTokLimit.hasUsageAccess(appContext)) {
            overlay.hide()
            changeOver(false)
            return // nothing to watch; refresh() restarts this when the user sets it up
        }
        val allowance = limit.allowance() ?: return
        val tally = try {
            limit.tallyToday()
        } catch (e: Exception) {
            null
        }
        val over = tally != null && TimeLimit.isOver(tally.usedMs, allowance.currentMin)
        changeOver(over)
        if (over && tally?.inForeground == true) overlay.show() else overlay.hide()
        handler.postDelayed(check, if (over) OVER_INTERVAL_MS else UNDER_INTERVAL_MS)
    }

    private fun changeOver(over: Boolean) {
        if (wasOver != over) {
            val first = wasOver == null
            wasOver = over
            if (!first || over) onOverChanged()
        }
    }

    private companion object {
        const val UNDER_INTERVAL_MS = 10_000L
        const val OVER_INTERVAL_MS = 1_500L // quick, so TikTok is covered soon after it is opened
    }
}
