package app.identity.filter

/**
 * A "connection refused" reply to a TCP packet that arrived through the tunnel (M3.7b). [packet] is
 * ready to write back; [destinationAddress] (4 or 16 bytes) and [destinationPort] say where the
 * refused connection was heading, so the caller can tell what was being attempted.
 */
class TcpReset(val packet: ByteArray, val destinationAddress: ByteArray, val destinationPort: Int)

/**
 * Builds TCP resets for IPv4 and IPv6 packets. Used for connections to the public DNS resolvers
 * that Identity routes into its tunnel (see [PublicResolvers]): refusing at once makes an app fall
 * back quickly, where silence would leave it waiting for timeouts.
 */
object TcpResets {
    private const val IPV4_HEADER = 20
    private const val IPV6_HEADER = 40
    private const val TCP_HEADER = 20
    private const val PROTOCOL_TCP = 6
    private const val FIN = 0x01
    private const val SYN = 0x02
    private const val RST = 0x04
    private const val ACK = 0x10

    /**
     * The reset for the first [length] bytes of [packet], or null if it isn't a TCP segment that
     * deserves one: not IPv4/IPv6 TCP, a fragment, truncated, or itself a reset (never answer a reset).
     */
    fun reset(packet: ByteArray, length: Int = packet.size): TcpReset? {
        if (length < 1 || length > packet.size) return null
        return when ((packet[0].toInt() ushr 4) and 0xF) {
            4 -> resetV4(packet, length)
            6 -> resetV6(packet, length)
            else -> null
        }
    }

    private fun resetV4(p: ByteArray, length: Int): TcpReset? {
        if (length < IPV4_HEADER) return null
        val headerLength = (p[0].toInt() and 0xF) * 4
        if (headerLength < IPV4_HEADER) return null
        val totalLength = u16(p, 2)
        if (totalLength > length || totalLength < headerLength + TCP_HEADER) return null
        if ((p[9].toInt() and 0xFF) != PROTOCOL_TCP) return null
        if ((u16(p, 6) and 0x3FFF) != 0) return null // a fragment
        val segment = headerLength
        val tcpLength = totalLength - headerLength
        val source = p.copyOfRange(12, 16)
        val destination = p.copyOfRange(16, 20)
        val tcp = reply(p, segment, tcpLength) ?: return null

        val out = ByteArray(IPV4_HEADER + TCP_HEADER)
        out[0] = 0x45
        put16(out, 2, out.size)
        put16(out, 6, 0x4000)
        out[8] = 64
        out[9] = PROTOCOL_TCP.toByte()
        destination.copyInto(out, 12) // swapped: the reply comes from where the connection was going
        source.copyInto(out, 16)
        put16(out, 10, finish(sum(out, 0, IPV4_HEADER)))
        tcp.copyInto(out, IPV4_HEADER)
        val pseudo = sum(out, 12, 8) + PROTOCOL_TCP + TCP_HEADER
        put16(out, IPV4_HEADER + 16, finish(sum(out, IPV4_HEADER, TCP_HEADER, pseudo)))
        return TcpReset(out, destination, u16(p, segment + 2))
    }

    private fun resetV6(p: ByteArray, length: Int): TcpReset? {
        if (length < IPV6_HEADER + TCP_HEADER) return null
        val payloadLength = u16(p, 4)
        if (payloadLength < TCP_HEADER || IPV6_HEADER + payloadLength > length) return null
        if ((p[6].toInt() and 0xFF) != PROTOCOL_TCP) return null // extension headers are not handled
        val source = p.copyOfRange(8, 24)
        val destination = p.copyOfRange(24, 40)
        val tcp = reply(p, IPV6_HEADER, payloadLength) ?: return null

        val out = ByteArray(IPV6_HEADER + TCP_HEADER)
        out[0] = 0x60
        put16(out, 4, TCP_HEADER)
        out[6] = PROTOCOL_TCP.toByte()
        out[7] = 64
        destination.copyInto(out, 8)
        source.copyInto(out, 24)
        tcp.copyInto(out, IPV6_HEADER)
        val pseudo = sum(out, 8, 32) + TCP_HEADER + PROTOCOL_TCP
        put16(out, IPV6_HEADER + 16, finish(sum(out, IPV6_HEADER, TCP_HEADER, pseudo)))
        return TcpReset(out, destination, u16(p, IPV6_HEADER + 2))
    }

    /**
     * The 20-byte TCP header of the reset (checksum left empty), or null if [p] holds no usable
     * segment at [offset] or is itself a reset. A segment with ACK is answered with a bare reset
     * at the sequence number it expects; any other (such as the opening SYN) with reset+ack.
     */
    private fun reply(p: ByteArray, offset: Int, tcpLength: Int): ByteArray? {
        if (tcpLength < TCP_HEADER) return null
        val dataOffset = ((p[offset + 12].toInt() ushr 4) and 0xF) * 4
        if (dataOffset < TCP_HEADER || dataOffset > tcpLength) return null
        val flags = p[offset + 13].toInt() and 0xFF
        if (flags and RST != 0) return null
        val sequence = u32(p, offset + 4)
        val acknowledgement = u32(p, offset + 8)
        val payload = tcpLength - dataOffset

        val tcp = ByteArray(TCP_HEADER)
        put16(tcp, 0, u16(p, offset + 2)) // from the port it was heading to
        put16(tcp, 2, u16(p, offset)) // to the port it came from
        tcp[12] = (5 shl 4).toByte()
        if (flags and ACK != 0) {
            put32(tcp, 4, acknowledgement)
            tcp[13] = RST.toByte()
        } else {
            val consumed = payload + (if (flags and SYN != 0) 1 else 0) + (if (flags and FIN != 0) 1 else 0)
            put32(tcp, 8, (sequence + consumed) and 0xFFFFFFFFL)
            tcp[13] = (RST or ACK).toByte()
        }
        return tcp
    }

    private fun sum(data: ByteArray, offset: Int, length: Int, start: Long = 0): Long = IpPackets.sum(data, offset, length, start)

    private fun finish(total: Long): Int = IpPackets.finish(total)

    private fun u16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun u32(data: ByteArray, offset: Int): Long =
        (u16(data, offset).toLong() shl 16) or u16(data, offset + 2).toLong()

    private fun put16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value ushr 8).toByte()
        data[offset + 1] = value.toByte()
    }

    private fun put32(data: ByteArray, offset: Int, value: Long) {
        put16(data, offset, (value ushr 16).toInt() and 0xFFFF)
        put16(data, offset + 2, value.toInt() and 0xFFFF)
    }
}
