package app.identity.filter

/** What to do with one DNS query from the tunnel. */
sealed interface DnsDecision {
    /** Blocked: send this "no such domain" reply straight back. */
    class Answer(val response: ByteArray) : DnsDecision

    /** Allowed: pass the query, unchanged, to the upstream resolver. */
    data object Forward : DnsDecision

    /** Not a query we can read: ignore it. The app that asked times out and tries again. */
    data object Drop : DnsDecision
}

/** Decides each DNS query using [matcher]. Works on the DNS message only, not the packet around it. */
class DnsFilter(private val matcher: DomainMatcher) {

    fun decide(query: ByteArray): DnsDecision {
        val parsed = DnsPackets.parseQuery(query).getOrNull() ?: return DnsDecision.Drop
        if (!matcher.isBlocked(parsed.name)) return DnsDecision.Forward
        val reply = DnsPackets.nxDomainResponse(query).getOrNull() ?: return DnsDecision.Drop
        return DnsDecision.Answer(reply)
    }
}
