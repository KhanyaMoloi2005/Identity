package app.identity.protection

import android.content.Context
import app.identity.filter.BlocklistUpdate
import app.identity.filter.InterruptionLog
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads and installs a signed blocklist update (M3.6, D-035). Only [BASE_URL] is ever contacted.
 * The three files are fetched, and the list is installed only if every check in
 * [BlocklistUpdate.verify] passes; otherwise the list already in use stays. The request carries no
 * account, identifier or browsing information. Call off the main thread.
 */
internal class BlocklistUpdater(context: Context) {
    private val appContext = context.applicationContext
    private val store = BlocklistStore(appContext)
    private val history = InterruptionStore(appContext)
    private val prefs = ProtectionPrefs(appContext)

    sealed class Outcome {
        data class Updated(val version: Long, val entries: Int) : Outcome()
        object UpToDate : Outcome()
        data class Refused(val reason: BlocklistUpdate.Reason) : Outcome()
        object NoConnection : Outcome()
    }

    fun checkNow(): Outcome {
        val outcome = try {
            check()
        } catch (e: Exception) {
            Outcome.NoConnection
        }
        record(outcome)
        return outcome
    }

    private fun check(): Outcome {
        val installed = store.installed()
        val currentVersion = installed?.manifest?.version ?: 0L
        val currentEntries = installed?.manifest?.entries ?: Blocklist.bundledEntries(appContext)

        val manifest = download("manifest.txt", MAX_SMALL_FILE) ?: return Outcome.NoConnection
        // Not yet trusted, but enough to skip a 1.4 MB download when nothing is newer.
        val announced = BlocklistUpdate.parseManifest(manifest.decodeToString())
        if (announced != null && announced.version <= currentVersion) return Outcome.UpToDate
        val signature = download("manifest.sig", MAX_SMALL_FILE) ?: return Outcome.NoConnection
        val list = download("nsfw.txt", BlocklistUpdate.MAX_LIST_BYTES + 1) ?: return Outcome.NoConnection

        return when (val result = BlocklistUpdate.verify(manifest, signature, list, currentVersion, currentEntries)) {
            is BlocklistUpdate.Result.Rejected -> Outcome.Refused(result.reason)
            is BlocklistUpdate.Result.Accepted -> {
                store.install(manifest, signature, list)
                Outcome.Updated(result.manifest.version, result.manifest.entries)
            }
        }
    }

    /** Keeps the history honest without filling it: a refusal is always logged; a missing connection only after 3 tries in a row. */
    private fun record(outcome: Outcome) {
        when (outcome) {
            is Outcome.Updated -> {
                prefs.updateFailures = 0
                prefs.lastUpdateCheckMs = System.currentTimeMillis()
                history.record(InterruptionLog.Kind.BLOCKLIST_UPDATED)
            }
            Outcome.UpToDate -> {
                prefs.updateFailures = 0
                prefs.lastUpdateCheckMs = System.currentTimeMillis()
            }
            is Outcome.Refused -> {
                prefs.updateFailures = 0
                prefs.lastUpdateCheckMs = System.currentTimeMillis()
                history.record(InterruptionLog.Kind.BLOCKLIST_UPDATE_FAILED)
            }
            Outcome.NoConnection -> {
                val failures = prefs.updateFailures + 1
                prefs.updateFailures = failures
                if (failures == FAILURES_BEFORE_LOGGING) history.record(InterruptionLog.Kind.BLOCKLIST_UPDATE_FAILED)
            }
        }
    }

    /** Reads at most [limit] bytes of [name] from the update address. Null if the server isn't reachable or doesn't answer 200. */
    private fun download(name: String, limit: Int): ByteArray? {
        val connection = URL(BASE_URL + name).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true // GitHub hands release files to its download servers (also https)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            connection.inputStream.use { input ->
                while (out.size() < limit) {
                    val read = input.read(buffer, 0, minOf(buffer.size, limit - out.size()))
                    if (read < 0) break
                    out.write(buffer, 0, read)
                }
            }
            out.toByteArray()
        } catch (e: IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** Where the signed update is published: assets of one fixed GitHub release (content/blocklists/PUBLISHING.md). */
        const val BASE_URL = "https://github.com/KhanyaMoloi2005/Identity/releases/download/blocklist-latest/"

        private const val MAX_SMALL_FILE = 4 * 1024
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val FAILURES_BEFORE_LOGGING = 3
    }
}
