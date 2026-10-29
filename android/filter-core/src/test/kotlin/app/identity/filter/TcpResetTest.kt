package app.identity.filter

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Random

class TcpResetTest {
    private val phone4 = byteArrayOf(10, 111, 222.toByte(), 2)
    private val cloudflare4 = byteArrayOf(1, 1, 1, 1)
    private val phone6 = ByteArray(16).also { it[0] = 0xfd.toByte(); it[15] = 2 }
    private val google6 = ByteArray(16).also { it[0] = 0x20; it[1] = 0x01; it[2] = 0x48; it[3] = 0x60; it[4] = 0x48; it[5] = 0x60; it[14] = 0x88.toByte(); it[15] = 0x88.toByte() }

    /** A TCP segment: ports, sequence, acknowledgement, flags, payload. The checksum is left empty (the reset code ignores it). */
    private fun segment(srcPort: Int, dstPort: Int, seq: Long, ack: Long, flags: Int, payload: ByteArray = ByteArray(0)): ByteArray {
        val s = ByteArray(20 + payload.size)
        put16(s, 0, srcPort); put16(s, 2, dstPort)
        put32(s, 4, seq); put32(s, 8, ack)
        s[12] = (5 shl 4).toByte(); s[13] = flags.toByte()
        put16(s, 14, 65535)
        payload.copyInto(s, 20)
        return s
    }

    private fun ipv4(src: ByteArray, dst: ByteArray, tcp: ByteArray, protocol: Int = 6, fragmentField: Int = 0x4000): ByteArray {
        val p = ByteArray(20 + tcp.size)
        p[0] = 0x45; put16(p, 2, p.size); put16(p, 6, fragmentField); p[8] = 64; p[9] = protocol.toByte()
        src.copyInto(p, 12); dst.copyInto(p, 16)
        tcp.copyInto(p, 20)
        return p
    }

    private fun ipv6(src: ByteArray, dst: ByteArray, tcp: ByteArray, nextHeader: Int = 6): ByteArray {
        val p = ByteArray(40 + tcp.size)
        p[0] = 0x60; put16(p, 4, tcp.size); p[6] = nextHeader.toByte(); p[7] = 64
        src.copyInto(p, 8); dst.copyInto(p, 24)
        tcp.copyInto(p, 40)
        return p
    }

    private fun u16(d: ByteArray, o: Int) = ((d[o].toInt() and 0xFF) shl 8) or (d[o + 1].toInt() and 0xFF)
    private fun u32(d: ByteArray, o: Int) = (u16(d, o).toLong() shl 16) or u16(d, o + 2).toLong()
    private fun put16(d: ByteArray, o: Int, v: Int) { d[o] = (v ushr 8).toByte(); d[o + 1] = v.toByte() }
    private fun put32(d: ByteArray, o: Int, v: Long) { put16(d, o, (v ushr 16).toInt() and 0xFFFF); put16(d, o + 2, v.toInt() and 0xFFFF) }

    /** True if the checksums inside [reset] add up (what the receiving phone checks before it believes the packet). */
    private fun checksumsAreValid(reset: ByteArray): Boolean = when ((reset[0].toInt() ushr 4) and 0xF) {
        4 -> IpPackets.finish(IpPackets.sum(reset, 0, 20)) == 0 &&
            IpPackets.finish(IpPackets.sum(reset, 20, 20, IpPackets.sum(reset, 12, 8) + 6 + 20)) == 0
        else -> IpPackets.finish(IpPackets.sum(reset, 40, 20, IpPackets.sum(reset, 8, 32) + 20 + 6)) == 0
    }

    @Test
    fun aSynToAResolverIsRefusedWithResetAndAck() {
        val syn = ipv4(phone4, cloudflare4, segment(51000, 443, seq = 1000, ack = 0, flags = 0x02))
        val reset = TcpResets.reset(syn)!!
        val p = reset.packet
        assertEquals(40, p.size)
        assertArrayEquals(cloudflare4, p.copyOfRange(12, 16)) // comes from the resolver
        assertArrayEquals(phone4, p.copyOfRange(16, 20))
        assertEquals(443, u16(p, 20)); assertEquals(51000, u16(p, 22))
        assertEquals(0x14, p[33].toInt() and 0xFF) // RST + ACK
        assertEquals(1001L, u32(p, 28)) // acknowledges the SYN
        assertEquals(443, reset.destinationPort)
        assertArrayEquals(cloudflare4, reset.destinationAddress)
        assertEquals(true, checksumsAreValid(p))
    }

    @Test
    fun aSegmentWithAckIsRefusedWithABareResetAtTheExpectedSequence() {
        val data = ipv4(phone4, cloudflare4, segment(51000, 853, seq = 5000, ack = 777, flags = 0x18, payload = ByteArray(30)))
        val p = TcpResets.reset(data)!!.packet
        assertEquals(0x04, p[33].toInt() and 0xFF) // RST only
        assertEquals(777L, u32(p, 24))
        assertEquals(true, checksumsAreValid(p))
    }

    @Test
    fun theAcknowledgementCountsPayloadAndWrapsAround() {
        val seq = 0xFFFFFFF0L
        val pkt = ipv4(phone4, cloudflare4, segment(1, 443, seq = seq, ack = 0, flags = 0x01, payload = ByteArray(32)))
        val p = TcpResets.reset(pkt)!!.packet
        assertEquals((seq + 32 + 1) and 0xFFFFFFFFL, u32(p, 28)) // payload plus the FIN
    }

    @Test
    fun anIpv6SynIsRefusedToo() {
        val syn = ipv6(phone6, google6, segment(52000, 443, seq = 42, ack = 0, flags = 0x02))
        val reset = TcpResets.reset(syn)!!
        val p = reset.packet
        assertEquals(60, p.size)
        assertArrayEquals(google6, p.copyOfRange(8, 24))
        assertArrayEquals(phone6, p.copyOfRange(24, 40))
        assertEquals(0x14, p[53].toInt() and 0xFF)
        assertEquals(43L, u32(p, 48))
        assertEquals(443, reset.destinationPort)
        assertEquals(16, reset.destinationAddress.size)
        assertEquals(true, checksumsAreValid(p))
    }

    @Test
    fun aResetIsNeverAnsweredWithAReset() {
        assertNull(TcpResets.reset(ipv4(phone4, cloudflare4, segment(1, 443, 1, 1, flags = 0x04))))
        assertNull(TcpResets.reset(ipv6(phone6, google6, segment(1, 443, 1, 1, flags = 0x14))))
    }

    @Test
    fun otherPacketsAreIgnored() {
        assertNull(TcpResets.reset(ipv4(phone4, cloudflare4, segment(1, 443, 1, 0, 0x02), protocol = 17))) // UDP
        assertNull(TcpResets.reset(ipv4(phone4, cloudflare4, segment(1, 443, 1, 0, 0x02), fragmentField = 0x2000))) // a fragment
        assertNull(TcpResets.reset(ipv6(phone6, google6, segment(1, 443, 1, 0, 0x02), nextHeader = 17)))
        assertNull(TcpResets.reset(ipv6(phone6, google6, segment(1, 443, 1, 0, 0x02), nextHeader = 0))) // extension header
        assertNull(TcpResets.reset(ByteArray(0)))
        assertNull(TcpResets.reset(ByteArray(10) { 0x45 }))
    }

    @Test
    fun truncatedOrLyingPacketsAreIgnoredWithoutCrashing() {
        val good = ipv4(phone4, cloudflare4, segment(1, 443, 1, 0, 0x02))
        assertNotNull(TcpResets.reset(good))
        for (cut in 0 until good.size) TcpResets.reset(good.copyOf(cut)) // never throws
        assertNull(TcpResets.reset(good, length = 30)) // shorter than the packet says
        val badOffset = good.copyOf().also { it[20 + 12] = 0xF0.toByte() } // header longer than the segment
        assertNull(TcpResets.reset(badOffset))
        val badLength = good.copyOf().also { put16(it, 2, 100) }
        assertNull(TcpResets.reset(badLength))
    }

    @Test
    fun randomBytesNeverCrashIt() {
        val random = Random(7)
        repeat(2000) {
            val junk = ByteArray(random.nextInt(120)).also { random.nextBytes(it) }
            if (junk.isNotEmpty()) junk[0] = (if (random.nextBoolean()) 0x45 else 0x60).toByte()
            TcpResets.reset(junk)
        }
    }
}
