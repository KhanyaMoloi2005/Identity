package app.identity.filter

/** What to do with one DNS query from the tunnel. */
sealed interface DnsDecision {
    /**
     * Blocked: send this "no such domain" reply straight back. [outsideDns] is true when the name was
     * an outside encrypted-DNS server, so the phone can tell the user something tried to use one.
     */
    class Answer(val response: ByteArray, val outsideDns: Boolean = false, val name: String? = null) : DnsDecision

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
class DnsFilter(
    private val matcher: DomainMatcher,
    /** The names of outside encrypted-DNS servers ([BlockPresets.DOH_ENDPOINTS]); only used to flag them. */
    private val encryptedDnsServers: DomainMatcher = DomainMatcher(emptyList()),
) {

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
        val outside = encryptedDnsServers.isBlocked(parsed.name)
        // The name is only passed on for encrypted-DNS servers (never for a user's own blocks), for debug builds' logs.
        return DnsDecision.Answer(reply, outsideDns = outside, name = if (outside) parsed.name else null)
    }
}
