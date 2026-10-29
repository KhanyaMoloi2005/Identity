package app.identity.account

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

internal sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val error: AccountError) : ApiResult<Nothing>
}

/** What is sent about a phone when it is registered: model and versions only (D-037). */
internal data class DeviceInfo(val model: String, val androidVersion: String, val appVersion: String)

/**
 * The few calls the app makes to the accounts backend: sign up, sign in, refresh, sign out, the
 * nickname, and this phone's registration. Each returns a result instead of throwing.
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
}
