package app.identity.filter

import app.identity.filter.InterruptionLog.Event
import app.identity.filter.InterruptionLog.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterruptionLogTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day

    @Test
    fun eventsOlderThanThirtyDaysAreDeleted() {
        val old = Event(now - 31 * day, Kind.TURNED_ON)
        val edge = Event(now - 30 * day + 1, Kind.TURNED_OFF)
        val log = InterruptionLog.add(listOf(old, edge), Event(now, Kind.ENDED_UNEXPECTEDLY), now)
        assertEquals(listOf(edge, Event(now, Kind.ENDED_UNEXPECTEDLY)), log)
    }

    @Test
    fun exactlyThirtyDaysOldIsDeleted() {
        assertEquals(emptyList<Event>(), InterruptionLog.prune(listOf(Event(now - 30 * day, Kind.TURNED_ON)), now))
    }

    @Test
    fun theListStaysInTimeOrderEvenWhenAddedOutOfOrder() {
        val log = InterruptionLog.add(listOf(Event(now - 1000, Kind.TURNED_ON)), Event(now - 2000, Kind.TURNED_OFF), now)
        assertEquals(listOf(now - 2000, now - 1000), log.map { it.timeMs })
    }

    @Test
    fun onlyTheNewestEventsAreKept() {
        var log = emptyList<Event>()
        for (i in 1..(InterruptionLog.MAX_EVENTS + 25)) log = InterruptionLog.add(log, Event(now - 1_000_000 + i, Kind.DNS_HANDLER_STOPPED), now)
        assertEquals(InterruptionLog.MAX_EVENTS, log.size)
        assertEquals(now - 1_000_000 + 26, log.first().timeMs)
    }

    @Test
    fun theUsersOwnOnAndOffAndListUpdatesAreNotInterruptions() {
        val routine = setOf(Kind.TURNED_ON, Kind.TURNED_OFF, Kind.BLOCKLIST_UPDATED, Kind.BLOCKLIST_UPDATE_FAILED)
        for (kind in routine) assertFalse(kind.name, InterruptionLog.isInterruption(kind))
        for (kind in Kind.values().filter { it !in routine }) assertTrue(kind.name, InterruptionLog.isInterruption(kind))
    }

    @Test
    fun everyProblemHasAnEventKind() {
        for (problem in ProtectionHealth.Problem.values()) {
            assertTrue(InterruptionLog.isInterruption(InterruptionLog.of(problem)))
        }
    }
}
