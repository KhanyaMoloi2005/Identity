package app.identity.filter

/**
 * Decides whether a domain is blocked.
 *
 * A rule for `example.com` covers `example.com` and every subdomain (`www.example.com`),
 * but not look-alikes such as `notexample.com`. Case and a trailing dot are ignored.
 * Order of precedence: **any matching allow rule wins over any block rule**, however specific.
 */
class DomainMatcher(private val blocked: DomainSet, private val allowed: DomainSet = DomainSet.EMPTY) {

    constructor(blocked: Collection<String>, allowed: Collection<String> = emptyList()) :
        this(DomainSet.of(blocked), DomainSet.of(allowed))

    fun isBlocked(domain: String): Boolean {
        val name = domain.trim().trimEnd('.').lowercase()
        if (name.isEmpty()) return false
        val suffixes = suffixesOf(name)
        if (suffixes.any { it in allowed }) return false
        return suffixes.any { it in blocked }
    }

    /** `www.example.com` -> `www.example.com`, `example.com`, `com`. */
    private fun suffixesOf(name: String): List<String> {
        val parts = name.split('.')
        return parts.indices.map { parts.drop(it).joinToString(".") }
    }
}
