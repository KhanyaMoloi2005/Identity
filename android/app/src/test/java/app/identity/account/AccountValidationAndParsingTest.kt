package app.identity.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountValidationTest {
    @Test
    fun emailsNeedAnAtAndADot() {
        for (good in listOf("a@b.co", " Someone.Name+tag@example.com ", "x@sub.domain.org")) assertNull(good, AccountValidation.email(good))
        for (bad in listOf("", "plain", "a@b", "a@@b.com", "a b@c.com", "@b.com", "a@")) {
            assertEquals(bad, FieldProblem.EMAIL_INVALID, AccountValidation.email(bad))
        }
        assertEquals(FieldProblem.EMAIL_INVALID, AccountValidation.email("a@" + "b".repeat(260) + ".com"))
    }

    @Test
    fun emailsAreTidiedBeforeSending() {
        assertEquals("someone@example.com", AccountValidation.cleanEmail("  Someone@Example.COM "))
    }

    @Test
    fun passwordsNeedEightCharactersAndFitInSeventyTwoBytes() {
        assertEquals(FieldProblem.PASSWORD_TOO_SHORT, AccountValidation.password("1234567"))
        assertNull(AccountValidation.password("12345678"))
        assertNull(AccountValidation.password("a".repeat(72)))
        assertEquals(FieldProblem.PASSWORD_TOO_LONG, AccountValidation.password("a".repeat(73)))
        // 40 two-byte characters is 80 bytes: too long, even though it is only 40 characters.
        assertEquals(FieldProblem.PASSWORD_TOO_LONG, AccountValidation.password("é".repeat(40)))
    }

    @Test
    fun nicknamesAreOneTidyLineOfAtMostThirtyCharacters() {
        assertEquals("Alex Smith", AccountValidation.cleanNickname("  Alex \n  Smith\t"))
        assertEquals("Alex", AccountValidation.cleanNickname("Al\u0000ex"))
        assertNull(AccountValidation.nickname("Alex"))
        assertNull(AccountValidation.nickname("a".repeat(30)))
        assertEquals(FieldProblem.NICKNAME_TOO_LONG, AccountValidation.nickname("a".repeat(31)))
        assertEquals(FieldProblem.NICKNAME_EMPTY, AccountValidation.nickname("   \n "))
        assertEquals(FieldProblem.NICKNAME_EMPTY, AccountValidation.nickname("\u0000\u0001"))
    }
}

/** Uses the shapes the real project sent on 2026-10-29. */
class AuthParsingTest {
    private val signedUp = """{"access_token":"AT","token_type":"bearer","expires_in":3600,"expires_at":1793311474,"refresh_token":"RT",
        "user":{"id":"u-1","aud":"authenticated","email":"a@b.co","email_confirmed_at":"2026-10-29T21:04:34Z","identities":[{"id":"x"}]}}"""

    @Test
    fun aSignedUpAccountIsASession() {
        val result = AuthParsing.parseAuth(200, signedUp, nowSec = 1000) as AuthResult.Signed
        assertEquals(Session("AT", "RT", 1793311474, "u-1", "a@b.co"), result.session)
    }

    @Test
    fun withoutExpiresAtTheLifetimeIsUsed() {
        val body = """{"access_token":"AT","refresh_token":"RT","expires_in":600,"user":{"id":"u-1","email":"a@b.co"}}"""
        assertEquals(1600L, (AuthParsing.parseAuth(200, body, nowSec = 1000) as AuthResult.Signed).session.expiresAtSec)
    }

    @Test
    fun anAccountWaitingForEmailConfirmationHasNoSession() {
        val body = """{"id":"u-2","aud":"authenticated","email":"a@b.co","confirmation_sent_at":"2026-10-29T21:00:00Z"}"""
        assertEquals(AuthResult.NeedsConfirmation, AuthParsing.parseAuth(200, body, nowSec = 1000))
    }

    @Test
    fun noConnectionAndJunkAreFailures() {
        assertEquals(AuthResult.Failed(AccountError.NO_CONNECTION), AuthParsing.parseAuth(0, "", 0))
        assertEquals(AuthResult.Failed(AccountError.UNKNOWN), AuthParsing.parseAuth(200, "<html>", 0))
        assertEquals(AuthResult.Failed(AccountError.UNKNOWN), AuthParsing.parseAuth(200, "{}", 0))
    }

    @Test
    fun theServersRealErrorsAreUnderstood() {
        fun error(status: Int, body: String) = AuthParsing.errorFrom(status, body)
        assertEquals(AccountError.EMAIL_TAKEN, error(422, """{"code":422,"error_code":"user_already_exists","msg":"User already registered"}"""))
        assertEquals(AccountError.WRONG_LOGIN, error(400, """{"code":400,"error_code":"invalid_credentials","msg":"Invalid login credentials"}"""))
        assertEquals(AccountError.WEAK_PASSWORD, error(422, """{"code":422,"error_code":"weak_password","msg":"Password should be at least 6 characters.","weak_password":{"reasons":["length"]}}"""))
        assertEquals(AccountError.INVALID_EMAIL, error(400, """{"code":400,"error_code":"validation_failed","msg":"Unable to validate email address: invalid format"}"""))
        assertEquals(AccountError.RATE_LIMITED, error(429, """{"code":429,"error_code":"over_request_rate_limit","msg":"Request rate limit reached"}"""))
        assertEquals(AccountError.SERVER, error(503, "Service Unavailable"))
        assertEquals(AccountError.NO_CONNECTION, error(0, ""))
    }

    @Test
    fun aDeadRefreshTokenIsASessionProblemNotABadEmail() {
        // The real answer to a bad refresh token is "validation_failed", which must not be read as a bad email.
        assertEquals(AccountError.SESSION_EXPIRED, AuthParsing.errorFrom(400, """{"code":400,"error_code":"validation_failed","msg":"Refresh token is not valid"}"""))
        assertEquals(AccountError.SESSION_EXPIRED, AuthParsing.errorFrom(400, """{"code":400,"error_code":"refresh_token_not_found","msg":"Invalid Refresh Token: Refresh Token Not Found"}"""))
        assertEquals(AccountError.SESSION_EXPIRED, AuthParsing.errorFrom(401, """{"code":"PGRST301","message":"JWT expired"}"""))
    }

    @Test
    fun theDevicesLimitFromTheDatabaseIsRecognised() {
        assertEquals(AccountError.DEVICE_LIMIT, AuthParsing.errorFrom(400, """{"code":"P0001","details":null,"hint":null,"message":"device_limit"}"""))
    }

    @Test
    fun unknownThingsAreUnknown() {
        assertEquals(AccountError.UNKNOWN, AuthParsing.errorFrom(418, "teapot"))
        assertTrue(AuthParsing.errorFrom(400, "") == AccountError.UNKNOWN)
    }
}
