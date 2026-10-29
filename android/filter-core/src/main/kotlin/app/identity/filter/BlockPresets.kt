package app.identity.filter

/**
 * Ready-made groups of extra block rules the user can switch on or off (D-028).
 * Each rule also blocks its subdomains, like the main blocklist.
 */
object BlockPresets {

    /**
     * X/Twitter and Reddit: neither has a safe mode that can be enforced on the network, so the
     * whole site, its apps and its image servers are blocked (posts embedded in other sites too).
     */
    val X_AND_REDDIT: List<String> = listOf(
        "x.com", "twitter.com", "twimg.com", "t.co",
        "reddit.com", "redd.it", "redditmedia.com", "redditstatic.com", "reddituploads.com",
    )

    /**
     * The servers that deliver the pictures in Google, Bing and DuckDuckGo image results, so the
     * results stay blank. Normal search, Gmail and Maps use other servers and keep working.
     * Limit: opening a result can still load the full picture from the site that hosts it.
     */
    val IMAGE_SEARCH: List<String> = listOf(
        "encrypted-tbn0.gstatic.com", "encrypted-tbn1.gstatic.com",
        "encrypted-tbn2.gstatic.com", "encrypted-tbn3.gstatic.com", // Google thumbnails
        "mm.bing.net", "th.bing.com", // Bing thumbnails
        "external-content.duckduckgo.com", // DuckDuckGo's image proxy
    )

    /**
     * Mozilla's "canary" name (R-02, M3.7). Firefox asks for it before switching on its own encrypted
     * DNS; "no such domain" tells it that the network filters DNS and it should keep using the
     * phone's DNS (which is Identity). Always on. Has no other effect.
     */
    val DOH_CANARY: List<String> = listOf("use-application-dns.net")

    /**
     * TikTok's app, video and image servers. Not always on: they are blocked only once the user's
     * daily TikTok allowance is used up (M3.3c-1, D-030).
     */
    val TIKTOK: List<String> = listOf(
        "tiktok.com", "tiktokv.com", "tiktokv.us", "tiktokcdn.com", "tiktokcdn-us.com", "tiktokcdn-eu.com",
        "tiktokw.us", "tiktokw.eu", "musical.ly", "muscdn.com", "byteoversea.com", "ibytedtos.com",
    )
}
