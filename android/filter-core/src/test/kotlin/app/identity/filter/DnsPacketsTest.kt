package app.identity.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xbill.DNS.Flags
import org.xbill.DNS.Message
import org.xbill.DNS.Rcode
import org.xbill.DNS.Section
import org.xbill.DNS.Type

class DnsPacketsTest {
    // A query for "www.Example.com" type A, written byte by byte (id 0x1234, recursion desired),
    // so the tests do not depend on dnsjava's own query builder.
    private val rawQuery: ByteArray = intArrayOf(
        0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        3, 'w'.code, 'w'.code, 'w'.code,
        7, 'E'.code, 'x'.code, 'a'.code, 'm'.code, 'p'.code, 'l'.code, 'e'.code,
        3, 'c'.code, 'o'.code, 'm'.code,
        0,
        0x00, 0x01, 0x00, 0x01,
    ).map { it.toByte() }.toByteArray()

    @Test
    fun parsesNameTypeAndIdFromARealPacket() {
        val query = DnsPackets.parseQuery(rawQuery).getOrThrow()
        assertEquals("www.example.com", query.name)
        assertEquals(Type.A, query.type)
        assertEquals(0x1234, query.id)
    }

    @Test
    fun blockedReplyIsNxDomainAndMatchesTheQuery() {
        val reply = Message(DnsPackets.nxDomainResponse(rawQuery).getOrThrow())
        assertEquals(0x1234, reply.header.id)
        assertTrue(reply.header.getFlag(Flags.QR.toInt()))
        assertTrue(reply.header.getFlag(Flags.RA.toInt()))
        assertTrue(reply.header.getFlag(Flags.RD.toInt()))
        assertEquals(Rcode.NXDOMAIN, reply.header.rcode)
        assertEquals(0, reply.header.getCount(Section.ANSWER))
        assertEquals(Message(rawQuery).question, reply.question)
    }

    @Test
    fun replyDoesNotClaimRecursionWasDesiredWhenTheQueryDidNotAskForIt() {
        val noRd = rawQuery.copyOf().also { it[2] = 0x00 }
        val reply = Message(DnsPackets.nxDomainResponse(noRd).getOrThrow())
        assertFalse(reply.header.getFlag(Flags.RD.toInt()))
    }

    @Test
    fun aResponsePacketIsNotAcceptedAsAQuery() {
        val response = DnsPackets.nxDomainResponse(rawQuery).getOrThrow()
        assertTrue(DnsPackets.parseQuery(response).isFailure)
        assertTrue(DnsPackets.nxDomainResponse(response).isFailure)
    }

    @Test
    fun emptyAndTooShortPacketsAreRejected() {
        assertTrue(DnsPackets.parseQuery(ByteArray(0)).isFailure)
        assertTrue(DnsPackets.parseQuery(rawQuery.copyOf(5)).isFailure)
    }

    @Test
    fun packetCutOffInsideTheQuestionIsRejected() {
        assertTrue(DnsPackets.parseQuery(rawQuery.copyOf(20)).isFailure)
        assertTrue(DnsPackets.nxDomainResponse(rawQuery.copyOf(20)).isFailure)
    }

    @Test
    fun queryWithNoQuestionIsRejected() {
        val none = rawQuery.copyOf(12).also { it[5] = 0x00 }
        assertTrue(DnsPackets.parseQuery(none).isFailure)
    }

    @Test
    fun queryWithTwoQuestionsIsRejected() {
        val two = rawQuery.copyOf().also { it[5] = 0x02 }
        assertTrue(DnsPackets.parseQuery(two).isFailure)
    }

    @Test
    fun nameCompressionPointerToItselfIsRejectedWithoutHanging() {
        // The question name is a pointer (0xC00C) to offset 12, which is the pointer itself.
        val loop = rawQuery.copyOf(12) + byteArrayOf(0xC0.toByte(), 0x0C, 0x00, 0x01, 0x00, 0x01)
        assertTrue(DnsPackets.parseQuery(loop).isFailure)
    }

    @Test
    fun randomGarbageNeverCrashes() {
        val random = java.util.Random(42)
        repeat(200) {
            val junk = ByteArray(random.nextInt(80)).also(random::nextBytes)
            // Either result is fine; the point is that no exception escapes.
            DnsPackets.parseQuery(junk)
            DnsPackets.nxDomainResponse(junk)
        }
    }
}
