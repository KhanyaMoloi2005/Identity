package app.identity.filter

import org.xbill.DNS.CNAMERecord
import org.xbill.DNS.Flags
import org.xbill.DNS.Message
import org.xbill.DNS.Name
import org.xbill.DNS.Opcode
import org.xbill.DNS.Rcode
import org.xbill.DNS.Record
import org.xbill.DNS.Section

/** The one question a DNS query asks. [name] has no trailing dot and is lower-case. */
data class DnsQuery(val id: Int, val name: String, val type: Int)

/** Raised when a packet is not a well-formed, single-question DNS query. */
class DnsPacketException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Reads DNS queries and builds replies. The wire format is handled by dnsjava (D-021);
 * the rest of the app only ever sees these functions.
 */
object DnsPackets {
    /** How long phones may remember a SafeSearch alias (5 minutes). */
    private const val ALIAS_TTL = 300L

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
            Result.success(replyTo(query, Rcode.NXDOMAIN).toWire())
        } catch (e: DnsPacketException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(DnsPacketException("Could not build a reply", e))
        }

    /**
     * The same question as [queryPacket] (same id and record type) but asking about [target].
     * Used for SafeSearch: the answer for the safe hostname is then wrapped by [redirectedResponse].
     */
    fun redirectedQuery(queryPacket: ByteArray, target: String): Result<ByteArray> =
        try {
            val query = Message(queryPacket)
            toQuery(query)
            val question = Record.newRecord(Name.fromString("$target."), query.question.type, query.question.dClass)
            val redirected = Message.newQuery(question)
            redirected.header.id = query.header.id
            Result.success(redirected.toWire())
        } catch (e: DnsPacketException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(DnsPacketException("Could not build the redirected query", e))
        }

    /**
     * Answers the original [queryPacket] using the upstream answer for [target]: an alias record
     * ("the name you asked for is [target]") followed by the upstream's records for [target].
     * Rejects an upstream answer that isn't about [target].
     */
    fun redirectedResponse(queryPacket: ByteArray, target: String, upstreamPacket: ByteArray): Result<ByteArray> =
        try {
            val query = Message(queryPacket)
            toQuery(query)
            val upstream = Message(upstreamPacket)
            val targetName = Name.fromString("$target.")
            if (!upstream.header.getFlag(Flags.QR.toInt()) || upstream.question?.name != targetName) {
                throw DnsPacketException("The upstream answer is not about $target")
            }
            val response = replyTo(query, upstream.header.rcode)
            response.addRecord(CNAMERecord(query.question.name, query.question.dClass, ALIAS_TTL, targetName), Section.ANSWER)
            for (record in upstream.getSection(Section.ANSWER)) response.addRecord(record, Section.ANSWER)
            Result.success(response.toWire())
        } catch (e: DnsPacketException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(DnsPacketException("Could not build the redirected reply", e))
        }

    /** A reply header for [query]: same id and question, response and recursion-available flags. */
    private fun replyTo(query: Message, rcode: Int): Message {
        val response = Message(query.header.id)
        response.header.setFlag(Flags.QR.toInt())
        response.header.setFlag(Flags.RA.toInt())
        if (query.header.getFlag(Flags.RD.toInt())) response.header.setFlag(Flags.RD.toInt())
        response.header.rcode = rcode
        response.addRecord(query.question, Section.QUESTION)
        return response
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
