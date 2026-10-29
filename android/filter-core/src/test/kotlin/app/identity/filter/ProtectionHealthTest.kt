package app.identity.filter

import app.identity.filter.ProtectionHealth.Observation
import app.identity.filter.ProtectionHealth.Problem
import app.identity.filter.ProtectionHealth.State
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionHealthTest {

    @Test
    fun notRunningAndNotWantedIsOff() {
        assertEquals(State.OFF, ProtectionHealth.assess(Observation(wantsOn = false, serviceRunning = false)).state)
    }

    @Test
    fun notRunningButWantedIsInterrupted() {
        assertEquals(State.INTERRUPTED, ProtectionHealth.assess(Observation(wantsOn = true, serviceRunning = false)).state)
    }

    @Test
    fun runningAndHealthyIsProtected() {
        val report = ProtectionHealth.assess(Observation(wantsOn = true, serviceRunning = true))
        assertEquals(State.PROTECTED, report.state)
        assertEquals(emptySet<Problem>(), report.problems)
    }

    @Test
    fun aStoppedDnsHandlerNeedsAttention() {
        val report = ProtectionHealth.assess(Observation(true, true, dnsHandlerAlive = false))
        assertEquals(State.NEEDS_ATTENTION, report.state)
        assertEquals(setOf(Problem.DNS_HANDLER_STOPPED), report.problems)
    }

    @Test
    fun theFilteringServiceIsUnreachableOnlyAfterSeveralFailuresInARow() {
        val few = ProtectionHealth.assess(Observation(true, true, consecutiveUpstreamFailures = 4))
        assertEquals(State.PROTECTED, few.state)
        val many = ProtectionHealth.assess(Observation(true, true, consecutiveUpstreamFailures = 5))
        assertEquals(setOf(Problem.FILTER_SERVICE_UNREACHABLE), many.problems)
    }

    @Test
    fun aCustomPrivateDnsNeedsAttention() {
        val report = ProtectionHealth.assess(Observation(true, true, privateDnsBypass = true))
        assertEquals(setOf(Problem.PRIVATE_DNS_BYPASS), report.problems)
    }

    @Test
    fun anOutsideDnsServiceThatWasRefusedNeedsAttention() {
        val report = ProtectionHealth.assess(Observation(true, true, outsideDnsBlockedRecently = true))
        assertEquals(State.NEEDS_ATTENTION, report.state)
        assertEquals(setOf(Problem.OUTSIDE_DNS_BLOCKED), report.problems)
    }

    @Test
    fun severalProblemsAreAllReported() {
        val report = ProtectionHealth.assess(Observation(true, true, false, 9, true, true))
        assertEquals(Problem.values().toSet(), report.problems)
    }

    @Test
    fun problemsOnAStoppedServiceDoNotMatter() {
        // Not running: the state is about that, whatever else was last seen.
        val report = ProtectionHealth.assess(Observation(true, false, false, 9, true, true))
        assertEquals(State.INTERRUPTED, report.state)
        assertEquals(emptySet<Problem>(), report.problems)
    }
}
