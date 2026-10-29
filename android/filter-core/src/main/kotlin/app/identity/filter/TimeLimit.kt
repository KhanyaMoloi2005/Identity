package app.identity.filter

/**
 * The daily time allowance for an app (M3.3c-1, D-030): the fixed choices, how time on screen is
 * added up from Android's usage events, and the rule for changing the allowance. Pure logic, so it
 * is tested on a computer. Reading the events from the phone is the app's job.
 */
object TimeLimit {
    /** The only allowances offered: 15 min, 30 min, 1 h, 2 h, 3 h, 5 h. No free typing. */
    val ALLOWANCE_CHOICES_MIN: List<Int> = listOf(15, 30, 60, 120, 180, 300)

    /** The app came to the front ([toForeground] true) or left it, at [timeMs]. */
    data class UsageEvent(val timeMs: Long, val toForeground: Boolean)

    /** How long the app was on screen in the window, and whether it is on screen right now. */
    data class Tally(val usedMs: Long, val inForeground: Boolean)

    /**
     * Adds up time on screen from [events] (oldest first) between [windowStartMs] and [nowMs].
     * A first event that is "left" means the app was already open when the window began.
     */
    fun tally(events: List<UsageEvent>, windowStartMs: Long, nowMs: Long): Tally {
        var total = 0L
        var since: Long? = null
        var first = true
        for (event in events) {
            val time = event.timeMs.coerceIn(windowStartMs, nowMs)
            if (event.toForeground) {
                if (since == null) since = time
            } else {
                val start = since ?: if (first) windowStartMs else null
                if (start != null) total += time - start
                since = null
            }
            first = false
        }
        since?.let { total += nowMs - it }
        return Tally(total, since != null)
    }

    fun isOver(usedMs: Long, allowanceMin: Int): Boolean = usedMs >= allowanceMin * 60_000L

    /**
     * The allowance the user has chosen, plus a raise waiting for tomorrow. Lowering takes effect at
     * once; raising never does, because a raise on a bad day would defeat the point. Days are
     * counted as whole days since 1970 in the phone's time zone.
     */
    data class Allowance(val currentMin: Int, val pendingMin: Int? = null, val pendingFromDay: Long? = null)

    /** The user picked [newMin]. [current] is null the first time. Unknown values change nothing. */
    fun choose(current: Allowance?, newMin: Int, today: Long): Allowance? {
        if (newMin !in ALLOWANCE_CHOICES_MIN) return current
        return when {
            current == null || newMin < current.currentMin -> Allowance(newMin)
            newMin == current.currentMin -> Allowance(current.currentMin) // also cancels a waiting raise
            else -> current.copy(pendingMin = newMin, pendingFromDay = today + 1)
        }
    }

    /** Applies a waiting raise once its day has come. */
    fun resolve(allowance: Allowance, today: Long): Allowance {
        val from = allowance.pendingFromDay
        val pending = allowance.pendingMin
        return if (pending != null && from != null && today >= from) Allowance(pending) else allowance
    }
}
