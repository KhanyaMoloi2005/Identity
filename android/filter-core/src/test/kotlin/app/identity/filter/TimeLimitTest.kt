package app.identity.filter

import app.identity.filter.TimeLimit.Allowance
import app.identity.filter.TimeLimit.UsageEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeLimitTest {
    private val minute = 60_000L
    private val start = 1_000_000L

    private fun up(min: Int) = UsageEvent(start + min * minute, toForeground = true)
    private fun down(min: Int) = UsageEvent(start + min * minute, toForeground = false)

    @Test
    fun `adds up separate visits`() {
        val tally = TimeLimit.tally(listOf(up(10), down(15), up(30), down(42)), start, start + 60 * minute)
        assertEquals(17 * minute, tally.usedMs)
        assertFalse(tally.inForeground)
    }

    @Test
    fun `an app still open counts up to now`() {
        val tally = TimeLimit.tally(listOf(up(10), down(15), up(50)), start, start + 60 * minute)
        assertEquals(15 * minute, tally.usedMs)
        assertTrue(tally.inForeground)
    }

    @Test
    fun `no events means no time`() {
        assertEquals(TimeLimit.Tally(0, false), TimeLimit.tally(emptyList(), start, start + minute))
    }

    @Test
    fun `an app already open when the window began counts from the start`() {
        val tally = TimeLimit.tally(listOf(down(5)), start, start + 60 * minute)
        assertEquals(5 * minute, tally.usedMs)
    }

    @Test
    fun `overlapping screens never count more than the time the app was open`() {
        // Screen B opens before screen A is reported as left: counted up to the first "left" (slightly low, never high).
        val tally = TimeLimit.tally(listOf(up(0), up(1), down(2), down(3)), start, start + 10 * minute)
        assertEquals(2 * minute, tally.usedMs)
    }

    @Test
    fun `a stray leave after a leave does not count from the window start`() {
        val tally = TimeLimit.tally(listOf(up(10), down(12), down(13)), start, start + 60 * minute)
        assertEquals(2 * minute, tally.usedMs)
    }

    @Test
    fun `isOver switches exactly at the allowance`() {
        assertFalse(TimeLimit.isOver(15 * minute - 1, 15))
        assertTrue(TimeLimit.isOver(15 * minute, 15))
    }

    @Test
    fun `first choice and lowering take effect at once`() {
        assertEquals(Allowance(120), TimeLimit.choose(null, 120, today = 100))
        assertEquals(Allowance(30), TimeLimit.choose(Allowance(120), 30, today = 100))
    }

    @Test
    fun `lowering drops a waiting raise`() {
        val waiting = Allowance(60, pendingMin = 180, pendingFromDay = 101)
        assertEquals(Allowance(30), TimeLimit.choose(waiting, 30, today = 100))
    }

    @Test
    fun `raising waits for tomorrow`() {
        val raised = TimeLimit.choose(Allowance(60), 180, today = 100)
        assertEquals(Allowance(60, pendingMin = 180, pendingFromDay = 101), raised)
        assertEquals(60, TimeLimit.resolve(raised!!, today = 100).currentMin)
        assertEquals(Allowance(180), TimeLimit.resolve(raised, today = 101))
    }

    @Test
    fun `choosing the current value cancels a waiting raise`() {
        val waiting = Allowance(60, pendingMin = 180, pendingFromDay = 101)
        assertEquals(Allowance(60), TimeLimit.choose(waiting, 60, today = 100))
    }

    @Test
    fun `values that are not on offer are ignored`() {
        assertNull(TimeLimit.choose(null, 45, today = 100))
        assertEquals(Allowance(60), TimeLimit.choose(Allowance(60), 9999, today = 100))
    }
}
