package app.identity.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xbill.DNS.ARecord
import org.xbill.DNS.CNAMERecord
import org.xbill.DNS.DClass
import org.xbill.DNS.Flags
import org.xbill.DNS.Message
import org.xbill.DNS.Name
import org.xbill.DNS.Rcode
import org.xbill.DNS.Record
import org.xbill.DNS.Section
import org.xbill.DNS.Type
import java.net.InetAddress

class SafeSearchTest {
    private fun query(name: String, type: Int = Type.A): ByteArray =
        Message.newQuery(Record.newRecord(Name.fromString("$name."), type, DClass.IN)).toWire()

    /** What the upstream server might answer for [name]: one A record. */
    private fun upstreamAnswer(redirectedQuery: ByteArray, name: String, address: String): ByteArray {
        val response = Message(Message(redirectedQuery).header.id)
        response.header.setFlag(Flags.QR.toInt())
        response.addRecord(Message(redirectedQuery).question, Section.QUESTION)
        response.addRecord(ARecord(Name.fromString("$name."), DClass.IN, 300, InetAddress.getByName(address)), Section.ANSWER)
        return response.toWire()
    }

    @Test
    fun searchEnginesAreRedirectedToTheirSafeHostnames() {
        assertEquals("forcesafesearch.google.com", SafeSearch.redirectFor("www.google.com"))
        assertEquals("forcesafesearch.google.com", SafeSearch.redirectFor("google.com"))
        assertEquals("forcesafesearch.google.com", SafeSearch.redirectFor("www.google.co.za"))
        assertEquals("forcesafesearch.google.com", SafeSearch.redirectFor("www.google.de"))
        assertEquals("strict.bing.com", SafeSearch.redirectFor("www.bing.com"))
        assertEquals("safe.duckduckgo.com", SafeSearch.redirectFor("duckduckgo.com"))
        assertEquals("restrictmoderate.youtube.com", SafeSearch.redirectFor("www.youtube.com"))
        assertEquals("restrictmoderate.youtube.com", SafeSearch.redirectFor("m.youtube.com"))
    }

    @Test
    fun otherGoogleServicesAndLookAlikesAreLeftAlone() {
        // Only search and YouTube are redirected; Gmail, Maps etc. must keep working normally.
        assertNull(SafeSearch.redirectFor("mail.google.com"))
        assertNull(SafeSearch.redirectFor("maps.google.com"))
        assertNull(SafeSearch.redirectFor("forcesafesearch.google.com")) // no loop
        assertNull(SafeSearch.redirectFor("notgoogle.com"))
        assertNull(SafeSearch.redirectFor("www.google.notarealtld"))
    }

    @Test
    fun allBundledGoogleDomainsAreLoaded() {
        // 187 country domains on 2026-10-29; a missing resource file would leave only google.com.
        val covered = listOf("com", "co.za", "co.uk", "com.au", "fr", "co.jp").count {
            SafeSearch.redirectFor("www.google.$it") != null
        }
        assertEquals(6, covered)
    }

    @Test
    fun filterAsksUpstreamAboutTheSafeHostnameAndAnswersWithAnAlias() {
        val filter = DnsFilter(DomainMatcher(emptyList()))
        val original = query("www.google.com")
        val decision = filter.decide(original) as DnsDecision.Redirect

        // The question sent upstream: same id and type, but about the safe hostname.
        val asked = Message(decision.query)
        assertEquals(Message(original).header.id, asked.header.id)
        assertEquals(Name.fromString("forcesafesearch.google.com."), asked.question.name)
        assertEquals(Type.A, asked.question.type)

        val reply = Message(decision.answer(upstreamAnswer(decision.query, "forcesafesearch.google.com", "216.239.38.120"))!!)
        assertEquals(Message(original).header.id, reply.header.id)
        assertEquals(Rcode.NOERROR, reply.header.rcode)
        assertEquals(Name.fromString("www.google.com."), reply.question.name) // the phone sees its own question
        val answers = reply.getSection(Section.ANSWER)
        val alias = answers[0] as CNAMERecord
        assertEquals(Name.fromString("www.google.com."), alias.name)
        assertEquals(Name.fromString("forcesafesearch.google.com."), alias.target)
        assertEquals("216.239.38.120", (answers[1] as ARecord).address.hostAddress)
    }

    @Test
    fun ipv6AndOtherRecordTypesAreRedirectedToo() {
        val decision = DnsFilter(DomainMatcher(emptyList())).decide(query("www.bing.com", Type.AAAA))
        assertEquals(Type.AAAA, Message((decision as DnsDecision.Redirect).query).question.type)
    }

    @Test
    fun anUpstreamAnswerAboutAnotherNameIsRejected() {
        val decision = DnsFilter(DomainMatcher(emptyList())).decide(query("www.google.com")) as DnsDecision.Redirect
        val wrong = upstreamAnswer(query("evil.example"), "evil.example", "203.0.113.9")
        assertNull(decision.answer(wrong))
        assertNull(decision.answer(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun blockingStillWinsOverSafeSearch() {
        val decision = DnsFilter(DomainMatcher(listOf("google.com"))).decide(query("www.google.com"))
        assertTrue(decision is DnsDecision.Answer)
    }
}
