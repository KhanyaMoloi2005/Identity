package app.identity.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CompactDomainSetTest {
    private val list = CompactDomainSet.fromLines(
        sequenceOf(
            "# Title: test list",
            "",
            "adult.example",
            "  Mixed.CASE.test.  ",
            "b.example",
            "a.example",
            "b.example", // duplicate
            "bad domain.example", // space: rejected
            "ünicode.example", // not ASCII: rejected
            "..dots.example", // rejected
            "xn--bcher-kva.example", // punycode is fine
        ),
    )

    @Test
    fun keepsValidNamesOnceAndSkipsTheRest() {
        assertEquals(5, list.size)
        for (name in listOf("adult.example", "mixed.case.test", "a.example", "b.example", "xn--bcher-kva.example")) {
            assertTrue(name, name in list)
        }
        assertFalse("bad domain.example" in list)
        assertFalse("# title: test list" in list)
    }

    @Test
    fun exactNamesOnly() {
        // Subdomain matching is DomainMatcher's job; the set itself is exact.
        assertFalse("www.adult.example" in list)
        assertFalse("adult.exampl" in list)
        assertFalse("adult.examplex" in list)
        assertFalse("" in list)
    }

    @Test
    fun emptyListContainsNothing() {
        val empty = CompactDomainSet.fromLines(emptySequence())
        assertEquals(0, empty.size)
        assertFalse("anything.example" in empty)
    }

    @Test
    fun agreesWithAHashSetOnManyRandomNames() {
        val random = Random(7)
        val letters = "abcdefghijklmnopqrstuvwxyz0123456789-"
        fun name() = (1..random.nextInt(1, 4)).joinToString(".") {
            (1..random.nextInt(1, 8)).map { letters[random.nextInt(letters.length)] }.joinToString("")
        }
        val names = List(5_000) { name() }
        val compact = CompactDomainSet.fromLines(names.asSequence())
        val expected = names.toHashSet()
        assertEquals(expected.size, compact.size)
        repeat(20_000) {
            val probe = if (random.nextBoolean()) names[random.nextInt(names.size)] else name()
            assertEquals(probe, probe in expected, probe in compact)
        }
    }

    @Test
    fun matcherUsesItWithSubdomainsAndAllowRules() {
        val matcher = DomainMatcher(list, allowed = DomainSet.of(listOf("ok.adult.example")))
        assertTrue(matcher.isBlocked("adult.example"))
        assertTrue(matcher.isBlocked("www.Adult.Example."))
        assertFalse(matcher.isBlocked("notadult.example"))
        assertFalse(matcher.isBlocked("ok.adult.example"))
    }

    @Test
    fun setsCanBeCombined() {
        val combined = list + DomainSet.of(listOf("example.com"))
        assertTrue("example.com" in combined)
        assertTrue("a.example" in combined)
        assertFalse("other.example" in combined)
    }
}
