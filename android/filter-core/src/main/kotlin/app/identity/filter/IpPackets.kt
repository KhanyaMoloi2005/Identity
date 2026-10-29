package app.identity.filter

/**
 * A UDP datagram inside an IPv4 packet: the form a DNS query takes when it arrives through the
 * VPN tunnel. Addresses are 4 bytes each.
 */
class UdpPacket(
    val sourceAddress: ByteArray,
    val sourcePort: Int,
    val destinationAddress: ByteArray,
    val destinationPort: Int,
    val payload: ByteArray,
) {
    /** A packet carrying [replyPayload] back the way this one came (addresses and ports swapped). */
    fun reply(replyPayload: ByteArray): ByteArray =
        IpPackets.buildUdp(destinationAddress, destinationPort, sourceAddress, sourcePort, replyPayload)
}

/**
 * Reads and builds IPv4 + UDP packets. Only what a DNS-only tunnel needs: no IPv6, TCP,
 * IP options on output, or fragments (a fragmented packet is ignored).
 */
object IpPackets {
    private const val IPV4_HEADER = 20
    private const val UDP_HEADER = 8
    private const val PROTOCOL_UDP = 17

    /** Largest UDP payload that fits in one IPv4 packet. */
    const val MAX_UDP_PAYLOAD = 65_535 - IPV4_HEADER - UDP_HEADER

    /** Reads the first [length] bytes of [packet] as IPv4 + UDP. Anything else, or bad input, returns null. */
    fun parseUdp(packet: ByteArray, length: Int = packet.size): UdpPacket? {
        if (length < IPV4_HEADER || length > packet.size) return null
        val version = (packet[0].toInt() ushr 4) and 0xF
        val headerLength = (packet[0].toInt() and 0xF) * 4
        if (version != 4 || headerLength < IPV4_HEADER) return null
        val totalLength = u16(packet, 2)
        if (totalLength > length || totalLength < headerLength + UDP_HEADER) return null
        if ((packet[9].toInt() and 0xFF) != PROTOCOL_UDP) return null
        // The "more fragments" flag or a fragment offset: only part of a datagram, so not ours to answer.
        if ((u16(packet, 6) and 0x3FFF) != 0) return null
        val udpLength = u16(packet, headerLength + 4)
        if (udpLength < UDP_HEADER || udpLength > totalLength - headerLength) return null
        return UdpPacket(
            sourceAddress = packet.copyOfRange(12, 16),
            sourcePort = u16(packet, headerLength),
            destinationAddress = packet.copyOfRange(16, 20),
            destinationPort = u16(packet, headerLength + 2),
            payload = packet.copyOfRange(headerLength + UDP_HEADER, headerLength + udpLength),
        )
    }

    /** Builds an IPv4 + UDP packet with valid IP and UDP checksums. */
    fun buildUdp(
        source: ByteArray,
        sourcePort: Int,
        destination: ByteArray,
        destinationPort: Int,
        payload: ByteArray,
    ): ByteArray {
        require(source.size == 4 && destination.size == 4) { "IPv4 addresses are 4 bytes" }
        require(payload.size <= MAX_UDP_PAYLOAD) { "Payload too large for one packet" }
        val udpLength = UDP_HEADER + payload.size
        val packet = ByteArray(IPV4_HEADER + udpLength)
        packet[0] = 0x45 // version 4, 20-byte header
        put16(packet, 2, packet.size)
        put16(packet, 6, 0x4000) // don't fragment
        packet[8] = 64 // time to live
        packet[9] = PROTOCOL_UDP.toByte()
        source.copyInto(packet, 12)
        destination.copyInto(packet, 16)
        put16(packet, 10, finish(sum(packet, 0, IPV4_HEADER)))

        put16(packet, IPV4_HEADER, sourcePort)
        put16(packet, IPV4_HEADER + 2, destinationPort)
        put16(packet, IPV4_HEADER + 4, udpLength)
        payload.copyInto(packet, IPV4_HEADER + UDP_HEADER)
        put16(packet, IPV4_HEADER + 6, udpChecksum(packet))
        return packet
    }

    /** UDP checksum over the "pseudo-header" (addresses, protocol, length) plus the UDP segment. */
    internal fun udpChecksum(packet: ByteArray): Int {
        val udpLength = packet.size - IPV4_HEADER
        val pseudoHeader = sum(packet, 12, 8) + PROTOCOL_UDP + udpLength
        val checksum = finish(sum(packet, IPV4_HEADER, udpLength, pseudoHeader))
        return if (checksum == 0) 0xFFFF else checksum // 0 means "no checksum" in UDP
    }

    /** The internet checksum's running sum of 16-bit words; a last odd byte is padded with zero. */
    internal fun sum(data: ByteArray, offset: Int, length: Int, start: Long = 0): Long {
        var total = start
        var i = offset
        val end = offset + length
        while (i + 1 < end) {
            total += u16(data, i)
            i += 2
        }
        if (i < end) total += (data[i].toInt() and 0xFF) shl 8
        return total
    }

    /** Folds the carries back in and takes the one's complement. */
    internal fun finish(total: Long): Int {
        var folded = total
        while ((folded ushr 16) != 0L) folded = (folded and 0xFFFF) + (folded ushr 16)
        return folded.inv().toInt() and 0xFFFF
    }

    private fun u16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun put16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value ushr 8).toByte()
        data[offset + 1] = value.toByte()
    }
}
