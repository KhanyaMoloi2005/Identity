package app.identity.protection

import android.content.Context
import android.util.AtomicFile
import app.identity.filter.InterruptionLog
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** One history entry in the shape written to disk. */
@Serializable
private data class StoredEvent(val timeMs: Long, val kind: String)

/**
 * The protection history (M3.5): turn-ons, turn-offs and interruptions, kept on the phone for
 * 30 days (see [InterruptionLog]), in one small file excluded from backup. No domain names, no
 * browsing: only what protection itself did and when.
 */
internal class InterruptionStore(context: Context) {
    private val file = AtomicFile(File(context.applicationContext.filesDir, "protection-history.json"))
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(StoredEvent.serializer())

    /** Newest last. Entries older than 30 days are dropped each time the list is read. */
    @Synchronized
    fun load(nowMs: Long = System.currentTimeMillis()): List<InterruptionLog.Event> {
        val events = try {
            json.decodeFromString(serializer, file.readFully().decodeToString()).mapNotNull { stored ->
                val kind = InterruptionLog.Kind.values().firstOrNull { it.name == stored.kind } ?: return@mapNotNull null
                InterruptionLog.Event(stored.timeMs, kind)
            }
        } catch (e: Exception) {
            emptyList() // no file yet
        }
        return InterruptionLog.prune(events, nowMs)
    }

    @Synchronized
    fun record(kind: InterruptionLog.Kind, nowMs: Long = System.currentTimeMillis()) {
        val updated = InterruptionLog.add(load(nowMs), InterruptionLog.Event(nowMs, kind), nowMs)
        val stream = file.startWrite()
        try {
            stream.write(json.encodeToString(serializer, updated.map { StoredEvent(it.timeMs, it.kind.name) }).encodeToByteArray())
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
        }
    }
}
