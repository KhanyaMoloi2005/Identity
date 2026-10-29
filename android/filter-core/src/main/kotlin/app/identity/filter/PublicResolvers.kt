package app.identity.filter

/**
 * Well-known public DNS resolvers (M3.7b, D-036). An app or a browser setting such as Chrome's
 * "Use secure DNS" can send lookups straight to one of these, around Identity (R-02). Identity
 * routes these few addresses into its tunnel: plain DNS sent to them is answered by Identity's own
 * filter, and every other connection to them (encrypted DNS) is refused.
 *
 * Deliberately not listed: Cloudflare for Families (1.1.1.3, 1.0.0.3), which is Identity's own
 * upstream. Its connections are marked to skip the tunnel, and it must never be routed into it.
 */
object PublicResolvers {
    /** IPv4 addresses of the big public resolvers and the ones browsers offer as "secure DNS" choices. */
    val IPV4: List<String> = listOf(
        // Cloudflare
        "1.1.1.1", "1.0.0.1",
        // Google
        "8.8.8.8", "8.8.4.4",
        // Quad9
        "9.9.9.9", "149.112.112.112", "9.9.9.10", "149.112.112.10", "9.9.9.11", "149.112.112.11",
        // OpenDNS
        "208.67.222.222", "208.67.220.220",
        // AdGuard
        "94.140.14.14", "94.140.15.15",
        // CleanBrowsing
        "185.228.168.9", "185.228.169.9", "185.228.168.168", "185.228.169.168",
        // DNS.SB
        "185.222.222.222", "45.11.45.11",
    )

    /** IPv6 addresses of the same resolvers, so the encrypted-DNS route is not just "try IPv6 instead". */
    val IPV6: List<String> = listOf(
        "2606:4700:4700::1111", "2606:4700:4700::1001", // Cloudflare
        "2001:4860:4860::8888", "2001:4860:4860::8844", // Google
        "2620:fe::fe", "2620:fe::9", "2620:fe::10", "2620:fe::11", // Quad9
        "2620:119:35::35", "2620:119:53::53", // OpenDNS
        "2a10:50c0::ad1:ff", "2a10:50c0::ad2:ff", // AdGuard
    )

    /** Identity's own upstream, which must never be routed into the tunnel. */
    val OWN_UPSTREAM: List<String> = listOf("1.1.1.3", "1.0.0.3")
}
