package app.identity.filter

/**
 * The short history of protection turning on, off or being interrupted (M3.5). Kept on the phone
 * only, for 30 days, then deleted. Pure rules here; the app saves the list in a file.
 */
object InterruptionLog {
    const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    const val MAX_EVENTS = 200

    enum class Kind {
        TURNED_ON,
        TURNED_OFF,

        /** Turned off in Android's VPN settings, or another VPN app took over. */
        TURNED_OFF_IN_ANDROID,

        /** The last session never ended cleanly: Android stopped Identity, or the phone restarted. Cause unknown. */
        ENDED_UNEXPECTEDLY,
        DNS_HANDLER_STOPPED,
        FILTER_SERVICE_UNREACHABLE,
        PRIVATE_DNS_BYPASS,

        /** A signed blocklist update was installed (M3.6). */
        BLOCKLIST_UPDATED,

        /** An update was refused (bad signature, older, damaged) or could not be downloaded several times in a row. */
        BLOCKLIST_UPDATE_FAILED,
    }

    data class Event(val timeMs: Long, val kind: Kind)

    /** Adds [event] and drops anything older than 30 days, keeping at most [MAX_EVENTS] (the newest). */
    fun add(events: List<Event>, event: Event, nowMs: Long): List<Event> = prune(events + event, nowMs)

    fun prune(events: List<Event>, nowMs: Long): List<Event> =
        events.filter { nowMs - it.timeMs < RETENTION_MS }.sortedBy { it.timeMs }.takeLast(MAX_EVENTS)

    /** The kinds that mean protection stopped working, as opposed to the user's own on/off and routine list updates. */
    fun isInterruption(kind: Kind): Boolean = kind !in NOT_INTERRUPTIONS

    private val NOT_INTERRUPTIONS = setOf(Kind.TURNED_ON, Kind.TURNED_OFF, Kind.BLOCKLIST_UPDATED, Kind.BLOCKLIST_UPDATE_FAILED)

    fun of(problem: ProtectionHealth.Problem): Kind = when (problem) {
        ProtectionHealth.Problem.DNS_HANDLER_STOPPED -> Kind.DNS_HANDLER_STOPPED
        ProtectionHealth.Problem.FILTER_SERVICE_UNREACHABLE -> Kind.FILTER_SERVICE_UNREACHABLE
        ProtectionHealth.Problem.PRIVATE_DNS_BYPASS -> Kind.PRIVATE_DNS_BYPASS
    }
}
