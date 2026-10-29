package app.identity.filter

/** What to do with one DNS query from the tunnel. */
sealed interface DnsDecision {
    /** Blocked: send this "no such domain" reply straight back. */
    class Answer(val response: ByteArray) : DnsDecision

    /** Allowed: pass the query, unchanged, to the upstream resolver. */
    data object Forward : DnsDecision

    /**
     * SafeSearch: send [query] (the same question about the engine's safe hostname) upstream,
     * then pass the upstream answer to [answer] to get the reply for the phone.
     */
    class Redirect(val query: ByteArray, private val finish: (ByteArray) -> ByteArray?) : DnsDecision {
        /** The reply to send back, or null if the upstream answer was unusable (the app retries). */
        fun answer(upstreamResponse: ByteArray): ByteArray? = finish(upstreamResponse)
    }

    /** Not a query we can read: ignore it. The app that asked times out and tries again. */
    data object Drop : DnsDecision
}

/**
 * Decides each DNS query using [matcher], then SafeSearch. Works on the DNS message only, not the
 * packet around it. A blocked name stays blocked even if it is also a search engine.
 */
class DnsFilter(private val matcher: DomainMatcher) {

    fun decide(query: ByteArray): DnsDecision {
        val parsed = DnsPackets.parseQuery(query).getOrNull() ?: return DnsDecision.Drop
        if (!matcher.isBlocked(parsed.name)) {
            val target = SafeSearch.redirectFor(parsed.name) ?: return DnsDecision.Forward
            val redirected = DnsPackets.redirectedQuery(query, target).getOrNull() ?: return DnsDecision.Drop
            return DnsDecision.Redirect(redirected) { upstream ->
                DnsPackets.redirectedResponse(query, target, upstream).getOrNull()
            }
        }
        val reply = DnsPackets.nxDomainResponse(query).getOrNull() ?: return DnsDecision.Drop
        return DnsDecision.Answer(reply)
    }
}
