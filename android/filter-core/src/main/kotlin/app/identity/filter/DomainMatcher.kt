package app.identity.filter

/**
 * Decides whether a domain is blocked.
 *
 * A rule for `example.com` covers `example.com` and every subdomain (`www.example.com`),
 * but not look-alikes such as `notexample.com`. Case and a trailing dot are ignored.
 * Order of precedence: **any matching allow rule wins over any block rule**, however specific.
 */
class DomainMatcher(blocked: Collection<String>, allowed: Collection<String> = emptyList()) {
    private val blockedRules = normalizeRules(blocked)
    private val allowedRules = normalizeRules(allowed)

    fun isBlocked(domain: String): Boolean {
        val name = normalize(domain)
        if (name.isEmpty()) return false
        val suffixes = suffixesOf(name)
        if (suffixes.any { it in allowedRules }) return false
        return suffixes.any { it in blockedRules }
    }

    /** `www.example.com` -> `www.example.com`, `example.com`, `com`. */
    private fun suffixesOf(name: String): List<String> {
        val parts = name.split('.')
        return parts.indices.map { parts.drop(it).joinToString(".") }
    }

    private companion object {
        fun normalize(domain: String): String = domain.trim().trimEnd('.').lowercase()

        fun normalizeRules(rules: Collection<String>): Set<String> =
            rules.map(::normalize).filter { it.isNotEmpty() }.toSet()
    }
}
