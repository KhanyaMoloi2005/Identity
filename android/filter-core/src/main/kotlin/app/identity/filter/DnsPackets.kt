package app.identity.filter

import org.xbill.DNS.Flags
import org.xbill.DNS.Message
import org.xbill.DNS.Opcode
import org.xbill.DNS.Rcode
import org.xbill.DNS.Section

/** The one question a DNS query asks. [name] has no trailing dot and is lower-case. */
data class DnsQuery(val id: Int, val name: String, val type: Int)

/** Raised when a packet is not a well-formed, single-question DNS query. */
class DnsPacketException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Reads DNS queries and builds replies. The wire format is handled by dnsjava (D-021);
 * the rest of the app only ever sees these two functions.
 */
object DnsPackets {

    /** Parses [packet] as a standard query with exactly one question. Bad input returns a failure. */
    fun parseQuery(packet: ByteArray): Result<DnsQuery> =
        try {
            Result.success(toQuery(Message(packet)))
        } catch (e: DnsPacketException) {
            Result.failure(e)
        } catch (e: Exception) {
            // dnsjava signals truncated or corrupt packets with several exception types.
            Result.failure(DnsPacketException("Not a readable DNS packet", e))
        }

    /**
     * Builds a "no such domain" (NXDOMAIN) reply to [queryPacket]. It copies the query's id and
     * question, sets the response and recursion-available flags, and carries no answers.
     */
    fun nxDomainResponse(queryPacket: ByteArray): Result<ByteArray> =
        try {
            val query = Message(queryPacket)
            toQuery(query)
            val response = Message(query.header.id)
            response.header.setFlag(Flags.QR.toInt())
            response.header.setFlag(Flags.RA.toInt())
            if (query.header.getFlag(Flags.RD.toInt())) response.header.setFlag(Flags.RD.toInt())
            response.header.rcode = Rcode.NXDOMAIN
            response.addRecord(query.question, Section.QUESTION)
            Result.success(response.toWire())
        } catch (e: DnsPacketException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(DnsPacketException("Could not build a reply", e))
        }

    private fun toQuery(message: Message): DnsQuery {
        val header = message.header
        if (header.getFlag(Flags.QR.toInt())) throw DnsPacketException("This is a response, not a query")
        if (header.opcode != Opcode.QUERY) throw DnsPacketException("Only standard queries are handled")
        if (header.getCount(Section.QUESTION) != 1) throw DnsPacketException("Expected exactly one question")
        val question = message.question
        return DnsQuery(
            id = header.id,
            name = question.name.toString(true).lowercase(),
            type = question.type,
        )
    }
}
