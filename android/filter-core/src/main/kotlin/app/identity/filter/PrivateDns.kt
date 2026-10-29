package app.identity.filter

/**
 * Android's "Private DNS" setting (M3.4, R-02). In "hostname" mode Android sends every lookup to a
 * provider the user typed in, around Identity, so the blocklist and SafeSearch never see it.
 * "off" and "opportunistic" (the "Automatic" choice) are fine: Android uses the VPN's own DNS.
 */
object PrivateDns {
    /** [mode] is the raw value of the `private_dns_mode` setting (null if the phone doesn't report one). */
    fun bypassesProtection(mode: String?): Boolean = mode?.trim()?.lowercase() == "hostname"
}
