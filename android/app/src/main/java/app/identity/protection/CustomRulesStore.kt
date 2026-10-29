package app.identity.protection

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.AtomicFile
import app.identity.filter.CustomRules
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** A site or a word the user chose to block (D-029). */
@Serializable
internal data class CustomRule(
    val kind: Kind,
    val value: String,
    /** Wall-clock time, only for showing when it was added. */
    val addedAtMs: Long,
    /** Boot number and time since start-up, for the undo window (the clock can be changed; these can't). */
    val addedBoot: Int,
    val addedElapsedMs: Long,
) {
    enum class Kind { SITE, WORD }
}

/**
 * The user's own block rules, in one small file on the phone (excluded from backup).
 * [AtomicFile] writes a complete new copy before replacing the old one, so a crash mid-save
 * can't leave half a file and silently drop rules.
 */
internal class CustomRulesStore(context: Context) {
    private val appContext = context.applicationContext
    private val file = AtomicFile(File(appContext.filesDir, "custom-rules.json"))
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(CustomRule.serializer())

    @Synchronized
    fun load(): List<CustomRule> =
        try {
            json.decodeFromString(serializer, file.readFully().decodeToString())
        } catch (e: Exception) {
            emptyList() // no file yet
        }

    /** Adds a rule. Returns what went wrong, or null on success. [value] must already be cleaned by [CustomRules]. */
    @Synchronized
    fun add(kind: CustomRule.Kind, value: String): AddResult {
        val rules = load()
        if (rules.any { it.kind == kind && it.value == value }) return AddResult.ALREADY_ADDED
        if (rules.size >= CustomRules.MAX_RULES) return AddResult.TOO_MANY
        save(rules + CustomRule(kind, value, System.currentTimeMillis(), bootCount(), SystemClock.elapsedRealtime()))
        return AddResult.ADDED
    }

    /** Removes [rule] only while its undo window is open. Returns whether it was removed. */
    @Synchronized
    fun remove(rule: CustomRule): Boolean {
        if (!canRemove(rule)) return false
        save(load().filterNot { it.kind == rule.kind && it.value == rule.value })
        return true
    }

    fun canRemove(rule: CustomRule): Boolean =
        CustomRules.canRemove(rule.addedBoot, rule.addedElapsedMs, bootCount(), SystemClock.elapsedRealtime())

    private fun save(rules: List<CustomRule>) {
        val stream = file.startWrite()
        try {
            stream.write(json.encodeToString(serializer, rules).encodeToByteArray())
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
    }

    private fun bootCount(): Int = Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT, -1)

    enum class AddResult { ADDED, ALREADY_ADDED, TOO_MANY }
}
