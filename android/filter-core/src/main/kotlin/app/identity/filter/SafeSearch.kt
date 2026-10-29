package app.identity.filter

/**
 * Forces search engines into their safe mode (D-027). Each engine publishes a "safe" hostname;
 * when a lookup asks for the normal one, we answer with the safe one's address instead, so every
 * search comes back filtered and the setting can't be switched off in the browser.
 */
object SafeSearch {
    private const val GOOGLE = "forcesafesearch.google.com"
    private const val BING = "strict.bing.com"
    private const val DUCKDUCKGO = "safe.duckduckgo.com"

    /** YouTube's "moderate" restricted mode (D-027); "restrict.youtube.com" would be strict. */
    private const val YOUTUBE = "restrictmoderate.youtube.com"

    private val redirects: Map<String, String> by lazy {
        val map = HashMap<String, String>()
        for (domain in googleDomains()) {
            map["google.$domain"] = GOOGLE
            map["www.google.$domain"] = GOOGLE
        }
        for (name in listOf("bing.com", "www.bing.com")) map[name] = BING
        for (name in listOf("duckduckgo.com", "www.duckduckgo.com", "start.duckduckgo.com")) map[name] = DUCKDUCKGO
        for (name in listOf(
            "youtube.com", "www.youtube.com", "m.youtube.com",
            "youtubei.googleapis.com", "youtube.googleapis.com", "www.youtube-nocookie.com",
        )) map[name] = YOUTUBE
        map
    }

    /** The safe-mode hostname to use instead of [name], or null if [name] isn't a search engine we redirect. */
    fun redirectFor(name: String): String? = redirects[name]

    /** Google's country domains (`com`, `co.za`, ...), bundled from google.com/supported_domains. */
    private fun googleDomains(): List<String> {
        val stream = SafeSearch::class.java.getResourceAsStream("google-search-domains.txt")
            ?: return listOf("com") // never expected; keeps google.com covered if the file went missing
        return stream.bufferedReader().useLines { lines -> lines.mapNotNull(::normalizeDomain).toList() }
    }
}
