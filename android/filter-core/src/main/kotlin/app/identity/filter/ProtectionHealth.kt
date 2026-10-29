package app.identity.filter

/**
 * What protection is really doing, worked out from things Identity can observe (M3.5), rather than
 * from the user's switch alone. Pure logic, so it is tested on a computer.
 */
object ProtectionHealth {
    /** What Identity can see right now. */
    data class Observation(
        /** The user wants protection on (they turned it on and have not turned it off). */
        val wantsOn: Boolean,
        /** The VPN service is running. */
        val serviceRunning: Boolean,
        /** The thread that answers lookups is alive. If it has stopped, websites fail (D-023). */
        val dnsHandlerAlive: Boolean = true,
        /** Lookups sent to the filtering service (Cloudflare) that failed one after another. */
        val consecutiveUpstreamFailures: Int = 0,
        /** Android's Private DNS is set to a custom provider (R-02). */
        val privateDnsBypass: Boolean = false,
        /** Something on the phone tried to use an outside encrypted-DNS service and was refused (M3.7b). */
        val outsideDnsBlockedRecently: Boolean = false,
    )

    enum class Problem { DNS_HANDLER_STOPPED, FILTER_SERVICE_UNREACHABLE, PRIVATE_DNS_BYPASS, OUTSIDE_DNS_BLOCKED }

    enum class State { OFF, PROTECTED, NEEDS_ATTENTION, INTERRUPTED }

    data class Report(val state: State, val problems: Set<Problem> = emptySet())

    /** How many lookups in a row must fail before the filtering service counts as unreachable. */
    const val UNREACHABLE_AFTER_FAILURES = 5

    fun assess(observation: Observation): Report {
        if (!observation.serviceRunning) {
            return Report(if (observation.wantsOn) State.INTERRUPTED else State.OFF)
        }
        val problems = buildSet {
            if (!observation.dnsHandlerAlive) add(Problem.DNS_HANDLER_STOPPED)
            if (observation.consecutiveUpstreamFailures >= UNREACHABLE_AFTER_FAILURES) add(Problem.FILTER_SERVICE_UNREACHABLE)
            if (observation.privateDnsBypass) add(Problem.PRIVATE_DNS_BYPASS)
            if (observation.outsideDnsBlockedRecently) add(Problem.OUTSIDE_DNS_BLOCKED)
        }
        return Report(if (problems.isEmpty()) State.PROTECTED else State.NEEDS_ATTENTION, problems)
    }
}
