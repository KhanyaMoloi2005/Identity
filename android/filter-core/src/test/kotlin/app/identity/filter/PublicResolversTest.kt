package app.identity.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

class PublicResolversTest {
    private val ipv4Literal = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
    private val ipv6Literal = Regex("""^[0-9a-f:]+$""")

    @Test
    fun everyEntryIsARealAddressOfTheRightKind() {
        for (text in PublicResolvers.IPV4) {
            assertTrue(text, ipv4Literal.matches(text))
            assertTrue(text, InetAddress.getByName(text) is Inet4Address) // a literal: no lookup happens
        }
        for (text in PublicResolvers.IPV6) {
            assertTrue(text, ipv6Literal.matches(text))
            assertTrue(text, InetAddress.getByName(text) is Inet6Address)
        }
    }

    @Test
    fun noEntryIsPrivateLoopbackOrLocal() {
        for (text in PublicResolvers.IPV4 + PublicResolvers.IPV6) {
            val address = InetAddress.getByName(text)
            assertFalse(text, address.isSiteLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress || address.isAnyLocalAddress)
        }
    }

    @Test
    fun noEntryRepeats() {
        assertEquals(PublicResolvers.IPV4.size, PublicResolvers.IPV4.toSet().size)
        assertEquals(PublicResolvers.IPV6.size, PublicResolvers.IPV6.toSet().size)
    }

    @Test
    fun identitysOwnUpstreamIsNeverRouted() {
        // Routing it into the tunnel would loop Identity's own lookups back into itself.
        for (own in PublicResolvers.OWN_UPSTREAM) {
            assertFalse(own, own in PublicResolvers.IPV4)
        }
        assertEquals(listOf("1.1.1.3", "1.0.0.3"), PublicResolvers.OWN_UPSTREAM)
    }

    @Test
    fun theResolversPeopleActuallyPickAreThere() {
        for (name in listOf("1.1.1.1", "1.0.0.1", "8.8.8.8", "8.8.4.4", "9.9.9.9", "208.67.222.222")) {
            assertTrue(name, name in PublicResolvers.IPV4)
        }
        assertTrue("2606:4700:4700::1111" in PublicResolvers.IPV6)
        assertTrue("2001:4860:4860::8888" in PublicResolvers.IPV6)
    }
}
