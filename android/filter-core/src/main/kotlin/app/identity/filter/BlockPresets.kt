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
     * The names of well-known encrypted-DNS (DNS over HTTPS / TLS) servers (M3.7b, D-036). A browser
     * or phone setting that wants one of them first looks up its name through Identity; answering
     * "no such domain" leaves it unable to find the server. Found on the phone: routing the
     * resolvers' main addresses was not enough, because Chrome reaches Cloudflare's encrypted
     * service through other addresses it learns from the name. Always on.
     */
    val DOH_ENDPOINTS: List<String> = listOf(
        // Cloudflare (Chrome and Firefox both use these)
        "cloudflare-dns.com", "one.one.one.one", "1dot1dot1dot1.cloudflare-dns.com",
        // Google
        "dns.google", "dns.google.com",
        // Quad9
        "dns.quad9.net", "dns9.quad9.net", "dns10.quad9.net", "dns11.quad9.net", "dns12.quad9.net",
        // OpenDNS / Cisco
        "doh.opendns.com", "doh.familyshield.opendns.com", "dns.umbrella.com",
        // AdGuard
        "dns.adguard.com", "dns-family.adguard.com", "adguard-dns.com",
        // CleanBrowsing, DNS.SB, NextDNS, Mullvad
        "doh.cleanbrowsing.org", "dns.sb", "dns.nextdns.io", "dns.mullvad.net", "doh.mullvad.net",
        // Others offered in browser and phone lists
        "dns.alidns.com", "doh.pub", "dns.pub", "doh.360.cn", "doh.libredns.gr", "doh.applied-privacy.net",
        "dns.switch.ch", "doh.ffmuc.net", "dns.digitalcourage.de", "doh.tiarap.org", "dnsforge.de",
    )

    /**
     * TikTok's app, video and image servers. Not always on: they are blocked only once the user's
     * daily TikTok allowance is used up (M3.3c-1, D-030).
     */
    val TIKTOK: List<String> = listOf(
        "tiktok.com", "tiktokv.com", "tiktokv.us", "tiktokcdn.com", "tiktokcdn-us.com", "tiktokcdn-eu.com",
        "tiktokw.us", "tiktokw.eu", "musical.ly", "muscdn.com", "byteoversea.com", "ibytedtos.com",
    )
}
