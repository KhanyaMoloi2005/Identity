package app.identity.account

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** One answer from the server. [status] is 0 when there was no connection at all. */
internal class HttpResult(val status: Int, val body: String)

/** Sends one request to the backend. An interface so the logic above it is tested without a network. */
internal interface HttpTransport {
    fun send(method: String, path: String, body: String? = null, token: String? = null, headers: Map<String, String> = emptyMap()): HttpResult
}

/**
 * Android's built-in HTTPS client, talking only to [BackendConfig.URL] (D-038). Every request carries
 * the public key; a signed-in request also carries the person's token. Nothing is logged: bodies
 * hold passwords and tokens.
 */
internal class UrlConnectionTransport(private val baseUrl: String = BackendConfig.URL) : HttpTransport {
    override fun send(method: String, path: String, body: String?, token: String?, headers: Map<String, String>): HttpResult {
        require(baseUrl.startsWith("https://")) { "The backend must be reached over HTTPS" }
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("apikey", BackendConfig.ANON_KEY)
            connection.setRequestProperty("Authorization", "Bearer " + (token ?: BackendConfig.ANON_KEY))
            connection.setRequestProperty("Accept", "application/json")
            for ((name, value) in headers) connection.setRequestProperty(name, value)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            val text = stream?.use { it.readBytes().decodeToString() }.orEmpty()
            HttpResult(status, text.take(MAX_BODY))
        } catch (e: IOException) {
            HttpResult(0, "")
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_BODY = 1_000_000
    }
}
