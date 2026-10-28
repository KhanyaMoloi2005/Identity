package app.identity.filter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainMatcherTest {
    private val matcher = DomainMatcher(blocked = listOf("example.com", "Bad.Test."))

    @Test
    fun blocksTheListedDomainAndItsSubdomains() {
        assertTrue(matcher.isBlocked("example.com"))
        assertTrue(matcher.isBlocked("www.example.com"))
        assertTrue(matcher.isBlocked("a.b.c.example.com"))
    }

    @Test
    fun doesNotBlockLookAlikesOrParents() {
        assertFalse(matcher.isBlocked("notexample.com"))
        assertFalse(matcher.isBlocked("example.com.evil.org"))
        assertFalse(matcher.isBlocked("example.org"))
        assertFalse(matcher.isBlocked("com"))
    }

    @Test
    fun ignoresCaseAndTrailingDotsOnBothSides() {
        assertTrue(matcher.isBlocked("WWW.EXAMPLE.COM"))
        assertTrue(matcher.isBlocked("www.example.com."))
        assertTrue(matcher.isBlocked("bad.test"))
        assertTrue(matcher.isBlocked("x.BAD.test"))
    }

    @Test
    fun allowRuleBeatsBlockRule() {
        val m = DomainMatcher(blocked = listOf("example.com"), allowed = listOf("safe.example.com"))
        assertTrue(m.isBlocked("www.example.com"))
        assertFalse(m.isBlocked("safe.example.com"))
        assertFalse(m.isBlocked("deep.safe.example.com"))
    }

    @Test
    fun allowRuleWinsEvenWhenItIsLessSpecificThanTheBlockRule() {
        val m = DomainMatcher(blocked = listOf("sub.example.com"), allowed = listOf("example.com"))
        assertFalse(m.isBlocked("sub.example.com"))
    }

    @Test
    fun emptyAndBlankInputsNeverBlockAndBlankRulesAreIgnored() {
        val m = DomainMatcher(blocked = listOf("", "  ", "."))
        assertFalse(m.isBlocked(""))
        assertFalse(m.isBlocked("."))
        assertFalse(m.isBlocked("anything.com"))
        assertFalse(matcher.isBlocked(""))
    }

    @Test
    fun noRulesBlocksNothing() {
        assertFalse(DomainMatcher(emptyList()).isBlocked("example.com"))
    }

    @Test
    fun matcherAgreesWithAParsedQuery() {
        val query = DnsPackets.parseQuery(
            org.xbill.DNS.Message.newQuery(
                org.xbill.DNS.Record.newRecord(
                    org.xbill.DNS.Name.fromString("WWW.Example.com."), org.xbill.DNS.Type.A, org.xbill.DNS.DClass.IN,
                ),
            ).toWire(),
        ).getOrThrow()
        assertTrue(matcher.isBlocked(query.name))
    }
}
