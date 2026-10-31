package app.identity.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

internal sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val error: AccountError) : ApiResult<Nothing>
}

/** The answer of a database function: done (with its fields), refused for a known reason, or failed. */
internal sealed interface RpcResult {
    data class Ok(val fields: JsonObject) : RpcResult
    data class Refused(val reason: PartnerError) : RpcResult
    data class Err(val error: AccountError) : RpcResult
}

/** One partnership row as the database returns it. */
@Serializable
internal data class PartnershipRow(
    val id: String,
    @SerialName("supported_id") val supportedId: String,
    @SerialName("partner_id") val partnerId: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("ended_by") val endedBy: String? = null,
    @SerialName("share_status") val shareStatus: Boolean = true,
    @SerialName("share_blocked_count") val shareBlockedCount: Boolean = false,
    @SerialName("share_setting_changes") val shareSettingChanges: Boolean = false,
)

@Serializable
internal data class InvitationRow(
    val id: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("used_at") val usedAt: String? = null,
    @SerialName("revoked_at") val revokedAt: String? = null,
)

/** What is sent about a phone when it is registered: model and versions only (D-037). */
internal data class DeviceInfo(val model: String, val androidVersion: String, val appVersion: String)

/**
 * The few calls the app makes to the accounts backend: sign up, sign in, refresh, sign out, the
 * nickname, this phone's registration, and partners. Each returns a result instead of throwing.
 */
internal class AccountApi(private val http: HttpTransport, private val nowSec: () -> Long = { System.currentTimeMillis() / 1000 }) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun signUp(email: String, password: String): AuthResult =
        auth("/auth/v1/signup", email, password)

    fun signIn(email: String, password: String): AuthResult =
        auth("/auth/v1/token?grant_type=password", email, password)

    fun refresh(refreshToken: String): AuthResult {
        val body = buildJsonObject { put("refresh_token", refreshToken) }.toString()
        val result = http.send("POST", "/auth/v1/token?grant_type=refresh_token", body)
        return AuthParsing.parseAuth(result.status, result.body, nowSec())
    }

    /** Ends only this phone's session (not the person's other phones). Best effort: failure changes nothing for the caller. */
    fun signOut(accessToken: String) {
        http.send("POST", "/auth/v1/logout?scope=local", token = accessToken)
    }

    /** The person's nickname, or null if no profile exists yet. */
    fun getNickname(token: String, userId: String): ApiResult<String?> {
        val result = http.send("GET", "/rest/v1/profiles?id=eq.$userId&select=nickname", token = token)
        if (result.status !in 200..299) return ApiResult.Err(AuthParsing.errorFrom(result.status, result.body))
        val rows = parseRows(result.body) ?: return ApiResult.Err(AccountError.UNKNOWN)
        return ApiResult.Ok(rows.firstOrNull()?.get("nickname")?.let { (it as? JsonPrimitive)?.contentOrNull })
    }

    fun createProfile(token: String, userId: String, nickname: String): ApiResult<Unit> {
        val body = buildJsonObject { put("id", userId); put("nickname", nickname) }.toString()
        return unit(http.send("POST", "/rest/v1/profiles", body, token, mapOf("Prefer" to "return=minimal")))
    }

    /** Changes the nickname. Ok(false) if there was no profile to change. */
    fun setNickname(token: String, userId: String, nickname: String): ApiResult<Boolean> {
        val body = buildJsonObject { put("nickname", nickname) }.toString()
        val result = http.send("PATCH", "/rest/v1/profiles?id=eq.$userId", body, token, mapOf("Prefer" to "return=representation"))
        if (result.status !in 200..299) return ApiResult.Err(AuthParsing.errorFrom(result.status, result.body))
        return ApiResult.Ok(!parseRows(result.body).isNullOrEmpty())
    }

    /** Registers this phone and returns its id. */
    fun registerDevice(token: String, userId: String, device: DeviceInfo): ApiResult<String> {
        val body = buildJsonObject {
            put("owner_id", userId)
            put("model", device.model.take(60))
            put("android_version", device.androidVersion.take(20))
            put("app_version", device.appVersion.take(20))
        }.toString()
        val result = http.send("POST", "/rest/v1/devices", body, token, mapOf("Prefer" to "return=representation"))
        if (result.status !in 200..299) return ApiResult.Err(AuthParsing.errorFrom(result.status, result.body))
        val id = parseRows(result.body)?.firstOrNull()?.get("id")?.let { (it as? JsonPrimitive)?.contentOrNull }
        return if (id != null) ApiResult.Ok(id) else ApiResult.Err(AccountError.UNKNOWN)
    }

    /** Marks this phone as seen now, and keeps its model and versions current. Ok(false) if the phone is no longer registered. */
    fun touchDevice(token: String, deviceId: String, device: DeviceInfo, nowIso: String): ApiResult<Boolean> {
        val body = buildJsonObject {
            put("last_seen_at", nowIso)
            put("model", device.model.take(60))
            put("android_version", device.androidVersion.take(20))
            put("app_version", device.appVersion.take(20))
        }.toString()
        val result = http.send("PATCH", "/rest/v1/devices?id=eq.$deviceId", body, token, mapOf("Prefer" to "return=representation"))
        if (result.status !in 200..299) return ApiResult.Err(AuthParsing.errorFrom(result.status, result.body))
        return ApiResult.Ok(!parseRows(result.body).isNullOrEmpty())
    }

    fun deleteDevice(token: String, deviceId: String): ApiResult<Unit> =
        unit(http.send("DELETE", "/rest/v1/devices?id=eq.$deviceId", token = token))

    // ---- Partners (M4.3). Creating, accepting, cancelling and ending go through database functions
    // that check everything on the server; the app only reads its own rows and the sharing choices.

    fun createInvitation(token: String): RpcResult = rpc(token, "create_invitation", buildJsonObject {})

    fun acceptInvitation(token: String, code: String): RpcResult = rpc(token, "accept_invitation", buildJsonObject { put("p_code", code) })

    fun revokeInvitation(token: String, invitationId: String): RpcResult =
        rpc(token, "revoke_invitation", buildJsonObject { put("p_invitation_id", invitationId) })

    fun endPartnership(token: String, partnershipId: String): RpcResult =
        rpc(token, "end_partnership", buildJsonObject { put("p_partnership_id", partnershipId) })

    /** Every partnership the person is part of, either side, newest first. */
    fun listPartnerships(token: String): ApiResult<List<PartnershipRow>> =
        rows(http.send("GET", "/rest/v1/partnerships?select=$PARTNERSHIP_COLUMNS&order=created_at.desc", token = token))

    /** The person's invitations that are neither used nor cancelled (expiry is checked by the caller). Columns are always named: the hash column is not readable. */
    fun listOpenInvitations(token: String): ApiResult<List<InvitationRow>> =
        rows(http.send("GET", "/rest/v1/invitations?select=$INVITATION_COLUMNS&used_at=is.null&revoked_at=is.null&order=created_at.desc", token = token))

    /** Nicknames by account id. The database only returns those of people the caller is actively connected to. */
    fun nicknames(token: String, userIds: Collection<String>): ApiResult<Map<String, String>> {
        if (userIds.isEmpty()) return ApiResult.Ok(emptyMap())
        val result = http.send("GET", "/rest/v1/profiles?id=in.(${userIds.joinToString(",")})&select=id,nickname", token = token)
        if (result.status !in 200..299) return ApiResult.Err(AuthParsing.errorFrom(result.status, result.body))
        val rows = parseRows(result.body) ?: return ApiResult.Err(AccountError.UNKNOWN)
        return ApiResult.Ok(
            rows.mapNotNull { row ->
                val id = (row["id"] as? JsonPrimitive)?.contentOrNull
                val nickname = (row["nickname"] as? JsonPrimitive)?.contentOrNull
                if (id != null && nickname != null) id to nickname else null
            }.toMap(),
        )
    }

    /** Saves the sharing choices. Ok(false) if nothing changed: the connection ended, or the caller is the partner (only the supported person may choose). */
    fun setSharing(token: String, partnershipId: String, sharing: Sharing): ApiResult<Boolean> {
        val body = buildJsonObject {
            put("share_status", sharing.status)
            put("share_blocked_count", sharing.blockedCount)
            put("share_setting_changes", sharing.settingChanges)
        }.toString()
        val result = http.send("PATCH", "/rest/v1/partnerships?id=eq.$partnershipId", body, token, mapOf("Prefer" to "return=representation"))
        if (result.status !in 200..299) return ApiResult.Err(AuthParsing.errorFrom(result.status, result.body))
        return ApiResult.Ok(!parseRows(result.body).isNullOrEmpty())
    }

    private fun rpc(token: String, name: String, args: JsonObject): RpcResult {
        val result = http.send("POST", "/rest/v1/rpc/$name", args.toString(), token)
        if (result.status !in 200..299) return RpcResult.Err(AuthParsing.errorFrom(result.status, result.body))
        val fields = try {
            json.parseToJsonElement(result.body).jsonObject
        } catch (e: Exception) {
            return RpcResult.Err(AccountError.UNKNOWN)
        }
        if ((fields["ok"] as? JsonPrimitive)?.booleanOrNull == true) return RpcResult.Ok(fields)
        val code = (fields["error"] as? JsonPrimitive)?.contentOrNull
        if (code == "not_signed_in") return RpcResult.Err(AccountError.SESSION_EXPIRED)
        return PartnerError.fromServer(code)?.let { RpcResult.Refused(it) } ?: RpcResult.Err(AccountError.UNKNOWN)
    }

    private inline fun <reified T> rows(result: HttpResult): ApiResult<List<T>> {
        if (result.status !in 200..299) return ApiResult.Err(AuthParsing.errorFrom(result.status, result.body))
        return try {
            ApiResult.Ok(json.decodeFromString<List<T>>(result.body.ifBlank { "[]" }))
        } catch (e: Exception) {
            ApiResult.Err(AccountError.UNKNOWN)
        }
    }

    private fun auth(path: String, email: String, password: String): AuthResult {
        val body = buildJsonObject { put("email", email); put("password", password) }.toString()
        val result = http.send("POST", path, body)
        return AuthParsing.parseAuth(result.status, result.body, nowSec())
    }

    private fun unit(result: HttpResult): ApiResult<Unit> =
        if (result.status in 200..299) ApiResult.Ok(Unit) else ApiResult.Err(AuthParsing.errorFrom(result.status, result.body))

    private fun parseRows(body: String): List<JsonObject>? =
        try {
            (json.parseToJsonElement(body.ifBlank { "[]" }) as? JsonArray)?.map { it.jsonObject }
        } catch (e: Exception) {
            null
        }

    private companion object {
        const val PARTNERSHIP_COLUMNS =
            "id,supported_id,partner_id,created_at,ended_at,ended_by,share_status,share_blocked_count,share_setting_changes"
        const val INVITATION_COLUMNS = "id,expires_at,used_at,revoked_at"
    }
}
