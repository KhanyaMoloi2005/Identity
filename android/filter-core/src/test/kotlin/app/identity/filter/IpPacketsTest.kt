package app.identity.filter

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.xbill.DNS.Message
import org.xbill.DNS.Rcode
import kotlin.random.Random

class IpPacketsTest {
    private val phone = byteArrayOf(10, 111, 222.toByte(), 2)
    private val dnsServer = byteArrayOf(10, 111, 222.toByte(), 1)
    private val payload = "hello dns".toByteArray()

    @Test
    fun builtPacketReadsBackUnchanged() {
        val packet = IpPackets.buildUdp(phone, 40000, dnsServer, 53, payload)
        val udp = IpPackets.parseUdp(packet)!!
        assertArrayEquals(phone, udp.sourceAddress)
        assertEquals(40000, udp.sourcePort)
        assertArrayEquals(dnsServer, udp.destinationAddress)
        assertEquals(53, udp.destinationPort)
        assertArrayEquals(payload, udp.payload)
    }

    @Test
    fun replySwapsAddressesAndPorts() {
        val query = IpPackets.parseUdp(IpPackets.buildUdp(phone, 40000, dnsServer, 53, payload))!!
        val reply = IpPackets.parseUdp(query.reply("answer".toByteArray()))!!
        assertArrayEquals(dnsServer, reply.sourceAddress)
        assertEquals(53, reply.sourcePort)
        assertArrayEquals(phone, reply.destinationAddress)
        assertEquals(40000, reply.destinationPort)
        assertArrayEquals("answer".toByteArray(), reply.payload)
    }

    @Test
    fun checksumsAreValid() {
        // A correct checksum makes the whole header (or pseudo-header + segment) sum to zero.
        for (body in listOf(payload, "odd".toByteArray(), ByteArray(0))) {
            val packet = IpPackets.buildUdp(phone, 40000, dnsServer, 53, body)
            assertEquals(0, IpPackets.finish(IpPackets.sum(packet, 0, 20)))
            val udpLength = packet.size - 20
            val pseudoHeader = IpPackets.sum(packet, 12, 8) + 17 + udpLength
            assertEquals(0, IpPackets.finish(IpPackets.sum(packet, 20, udpLength, pseudoHeader)))
        }
    }

    @Test
    fun matchesAKnownChecksum() {
        // Classic worked example of an IPv4 header checksum (value 0xB861).
        val header = intArrayOf(
            0x45, 0x00, 0x00, 0x73, 0x00, 0x00, 0x40, 0x00, 0x40, 0x11,
            0x00, 0x00, 0xc0, 0xa8, 0x00, 0x01, 0xc0, 0xa8, 0x00, 0xc7,
        ).map { it.toByte() }.toByteArray()
        assertEquals(0xB861, IpPackets.finish(IpPackets.sum(header, 0, 20)))
    }

    @Test
    fun honoursTheLengthArgumentWhenTheBufferIsLarger() {
        val packet = IpPackets.buildUdp(phone, 40000, dnsServer, 53, payload)
        val buffer = packet.copyOf(1500) // the tunnel reads into a reusable, bigger buffer
        assertArrayEquals(payload, IpPackets.parseUdp(buffer, packet.size)!!.payload)
        assertNull(IpPackets.parseUdp(buffer, packet.size - 1))
    }

    @Test
    fun readsPastIpOptions() {
        val plain = IpPackets.buildUdp(phone, 40000, dnsServer, 53, payload)
        // Same packet with a 24-byte header (4 bytes of options).
        val withOptions = ByteArray(plain.size + 4)
        plain.copyInto(withOptions, 0, 0, 20)
        plain.copyInto(withOptions, 24, 20)
        withOptions[0] = 0x46
        withOptions[3] = (plain.size + 4).toByte()
        assertArrayEquals(payload, IpPackets.parseUdp(withOptions)!!.payload)
    }

    @Test
    fun rejectsWhatIsNotIpv4Udp() {
        val packet = IpPackets.buildUdp(phone, 40000, dnsServer, 53, payload)
        assertNull(IpPackets.parseUdp(packet.copyOf().also { it[0] = 0x65 })) // IPv6 version nibble
        assertNull(IpPackets.parseUdp(packet.copyOf().also { it[9] = 6 })) // TCP
        assertNull(IpPackets.parseUdp(packet.copyOf().also { it[6] = 0x20 })) // more fragments
        assertNull(IpPackets.parseUdp(packet.copyOf().also { it[7] = 0x01 })) // fragment offset
        assertNull(IpPackets.parseUdp(packet.copyOf().also { it[0] = 0x44 })) // header shorter than 20
        assertNull(IpPackets.parseUdp(packet.copyOf().also { it[25] = 4 })) // UDP length below 8
        assertNull(IpPackets.parseUdp(packet.copyOf().also { it[25] = 0xFF.toByte() })) // UDP length too long
        assertNull(IpPackets.parseUdp(packet.copyOf(packet.size - 1))) // truncated
        assertNull(IpPackets.parseUdp(ByteArray(0)))
    }

    @Test
    fun randomBytesNeverCrash() {
        val random = Random(42)
        repeat(1000) {
            val junk = random.nextBytes(random.nextInt(0, 80))
            if (random.nextBoolean() && junk.isNotEmpty()) junk[0] = 0x45 // look like IPv4 half the time
            IpPackets.parseUdp(junk)
        }
    }

    @Test
    fun dnsQueryThroughTheWholePathGetsABlockedReply() {
        // An IPv4/UDP packet carrying a DNS query for www.example.com, as the tunnel would see it.
        val dnsQuery = intArrayOf(
            0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            3, 'w'.code, 'w'.code, 'w'.code,
            7, 'e'.code, 'x'.code, 'a'.code, 'm'.code, 'p'.code, 'l'.code, 'e'.code,
            3, 'c'.code, 'o'.code, 'm'.code, 0,
            0x00, 0x01, 0x00, 0x01,
        ).map { it.toByte() }.toByteArray()
        val udp = IpPackets.parseUdp(IpPackets.buildUdp(phone, 40000, dnsServer, 53, dnsQuery))!!

        val decision = DnsFilter(DomainMatcher(listOf("example.com"))).decide(udp.payload)
        val reply = IpPackets.parseUdp(udp.reply((decision as DnsDecision.Answer).response))
        assertNotNull(reply)
        assertEquals(40000, reply!!.destinationPort)
        val answer = Message(reply.payload)
        assertEquals(0x1234, answer.header.id)
        assertEquals(Rcode.NXDOMAIN, answer.header.rcode)
    }
}
