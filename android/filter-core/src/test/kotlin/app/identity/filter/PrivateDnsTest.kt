package app.identity.filter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateDnsTest {

    @Test
    fun aCustomHostnameBypassesProtection() {
        assertTrue(PrivateDns.bypassesProtection("hostname"))
        assertTrue(PrivateDns.bypassesProtection(" Hostname "))
    }

    @Test
    fun offAndAutomaticAreFine() {
        assertFalse(PrivateDns.bypassesProtection("off"))
        assertFalse(PrivateDns.bypassesProtection("opportunistic"))
    }

    @Test
    fun unknownOrMissingValuesAreNotTreatedAsABypass() {
        assertFalse(PrivateDns.bypassesProtection(null))
        assertFalse(PrivateDns.bypassesProtection(""))
        assertFalse(PrivateDns.bypassesProtection("something-new"))
    }
}
