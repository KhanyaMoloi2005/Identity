package app.identity.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xbill.DNS.Message
import org.xbill.DNS.Name
import org.xbill.DNS.Rcode
import org.xbill.DNS.Record
import org.xbill.DNS.Type

class DnsFilterTest {
    private val filter = DnsFilter(DomainMatcher(blocked = listOf("example.com"), allowed = listOf("ok.example.com")))

    private fun query(name: String, type: Int = Type.A): ByteArray =
        Message.newQuery(Record.newRecord(Name.fromString("$name."), type, 1)).toWire()

    @Test
    fun anEncryptedDnsServerNameIsAnsweredNoSuchDomainAndFlagged() {
        val withDoh = DnsFilter(
            DomainMatcher(blocked = listOf("example.com") + BlockPresets.DOH_ENDPOINTS),
            DomainMatcher(BlockPresets.DOH_ENDPOINTS),
        )
        val doh = withDoh.decide(query("chrome.cloudflare-dns.com")) as DnsDecision.Answer
        assertEquals(Rcode.NXDOMAIN, Message(doh.response).header.rcode)
        assertTrue(doh.outsideDns)
        val ordinary = withDoh.decide(query("www.example.com")) as DnsDecision.Answer
        assertTrue(!ordinary.outsideDns) // blocked for another reason: not an encrypted-DNS attempt
        assertSame(DnsDecision.Forward, withDoh.decide(query("wikipedia.org")))
    }

    @Test
    fun blockedNameGetsNxDomain() {
        val decision = filter.decide(query("www.example.com"))
        assertTrue(decision is DnsDecision.Answer)
        assertEquals(Rcode.NXDOMAIN, Message((decision as DnsDecision.Answer).response).header.rcode)
    }

    @Test
    fun everyRecordTypeOfABlockedNameIsBlocked() {
        for (type in listOf(Type.A, Type.AAAA, Type.HTTPS, Type.TXT)) {
            assertTrue(filter.decide(query("example.com", type)) is DnsDecision.Answer)
        }
    }

    @Test
    fun otherNamesAreForwarded() {
        assertSame(DnsDecision.Forward, filter.decide(query("wikipedia.org")))
        assertSame(DnsDecision.Forward, filter.decide(query("notexample.com")))
    }

    @Test
    fun allowRuleStillWins() {
        assertSame(DnsDecision.Forward, filter.decide(query("ok.example.com")))
    }

    @Test
    fun unreadableQueriesAreDropped() {
        assertSame(DnsDecision.Drop, filter.decide(ByteArray(0)))
        assertSame(DnsDecision.Drop, filter.decide(byteArrayOf(1, 2, 3)))
    }
}
