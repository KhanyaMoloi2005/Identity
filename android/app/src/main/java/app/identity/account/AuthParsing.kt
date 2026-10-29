package app.identity.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** Why something with the account failed, in terms the screens can explain. */
internal enum class AccountError {
    EMAIL_TAKEN, WRONG_LOGIN, WEAK_PASSWORD, INVALID_EMAIL, RATE_LIMITED, NO_CONNECTION,
    SESSION_EXPIRED, DEVICE_LIMIT, SERVER, UNKNOWN
}

/** A signed-in session: the tokens Supabase gave us. [expiresAtSec] is in seconds since 1970. */
internal data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtSec: Long,
    val userId: String,
    val email: String,
)

internal sealed interface AuthResult {
    data class Signed(val session: Session) : AuthResult

    /** The account exists but its email must be confirmed before signing in (switched off while developing). */
    data object NeedsConfirmation : AuthResult
    data class Failed(val error: AccountError) : AuthResult
}

@Serializable
private data class AuthUser(val id: String? = null, val email: String? = null)

@Serializable
private data class AuthBody(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("expires_at") val expiresAt: Long? = null,
    val user: AuthUser? = null,
    val id: String? = null,
    val email: String? = null,
)

@Serializable
private data class ErrorBody(
    val code: JsonElement? = null,
    @SerialName("error_code") val errorCode: String? = null,
    val msg: String? = null,
    val message: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

/** Reads Supabase's answers. Pure, so it is tested on a computer with real response shapes. */
internal object AuthParsing {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** [status] is 0 when there was no connection at all. */
    fun parseAuth(status: Int, body: String, nowSec: Long): AuthResult {
        if (status == 0) return AuthResult.Failed(AccountError.NO_CONNECTION)
        if (status !in 200..299) return AuthResult.Failed(errorFrom(status, body))
        val parsed = try {
            json.decodeFromString<AuthBody>(body)
        } catch (e: Exception) {
            return AuthResult.Failed(AccountError.UNKNOWN)
        }
        val access = parsed.accessToken
        val refresh = parsed.refreshToken
        val userId = parsed.user?.id ?: parsed.id
        if (access == null || refresh == null || userId == null) {
            // A user came back but no session: the project wants the email confirmed first.
            return if (userId != null) AuthResult.NeedsConfirmation else AuthResult.Failed(AccountError.UNKNOWN)
        }
        val expiresAt = parsed.expiresAt ?: (nowSec + (parsed.expiresIn ?: DEFAULT_LIFETIME_SEC))
        return AuthResult.Signed(Session(access, refresh, expiresAt, userId, parsed.user?.email ?: parsed.email ?: ""))
    }

    /** Turns a failed answer (auth or database) into one of our error kinds. */
    fun errorFrom(status: Int, body: String): AccountError {
        if (status == 0) return AccountError.NO_CONNECTION
        val parsed = try {
            json.decodeFromString<ErrorBody>(body)
        } catch (e: Exception) {
            null
        }
        val code = parsed?.errorCode.orEmpty()
        val text = listOfNotNull(parsed?.msg, parsed?.message, parsed?.error, parsed?.errorDescription).joinToString(" ").lowercase()
        val sqlCode = (parsed?.code as? JsonPrimitive)?.content.orEmpty()
        return when {
            code == "user_already_exists" || code == "email_exists" || "already registered" in text -> AccountError.EMAIL_TAKEN
            code == "invalid_credentials" || "invalid login credentials" in text -> AccountError.WRONG_LOGIN
            code == "weak_password" -> AccountError.WEAK_PASSWORD
            // Checked before "validation_failed": a dead refresh token is reported as "validation_failed: Refresh token is not valid".
            code in SESSION_CODES || "refresh token" in text || "jwt expired" in text || sqlCode == "PGRST301" || status == 401 -> AccountError.SESSION_EXPIRED
            code == "email_address_invalid" || (code == "validation_failed" && "email" in text) -> AccountError.INVALID_EMAIL
            status == 429 || code.contains("rate_limit") -> AccountError.RATE_LIMITED
            "device_limit" in text -> AccountError.DEVICE_LIMIT
            status >= 500 -> AccountError.SERVER
            else -> AccountError.UNKNOWN
        }
    }

    private val SESSION_CODES = setOf("refresh_token_not_found", "refresh_token_already_used", "session_not_found", "session_expired", "bad_jwt")
    private const val DEFAULT_LIFETIME_SEC = 3600L
}
