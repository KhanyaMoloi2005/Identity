package app.identity.protection

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.AtomicFile
import app.identity.filter.TimeLimit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/** The saved allowance, in the shape written to disk. */
@Serializable
private data class StoredAllowance(val currentMin: Int, val pendingMin: Int? = null, val pendingFromDay: Long? = null)

/**
 * TikTok's daily allowance (M3.3c-1, D-030): what the user chose, and how much of today is used.
 * Only the choice is saved, in one small file (excluded from backup). Time used is read from
 * Android's own usage history each time and never copied anywhere.
 */
internal class TikTokLimit(context: Context) {
    private val appContext = context.applicationContext
    private val file = AtomicFile(File(appContext.filesDir, "tiktok-limit.json"))
    private val json = Json { ignoreUnknownKeys = true }

    /** The allowance in force today (a raise that was waiting for today is applied and saved), or null if none was set. */
    @Synchronized
    fun allowance(): TimeLimit.Allowance? {
        val stored = read() ?: return null
        val resolved = TimeLimit.resolve(stored, today())
        if (resolved != stored) write(resolved)
        return resolved
    }

    /** Applies the user's pick: lowering at once, raising from tomorrow (see [TimeLimit.choose]). */
    @Synchronized
    fun choose(minutes: Int): TimeLimit.Allowance? {
        val updated = TimeLimit.choose(allowance(), minutes, today())
        if (updated != null) write(updated)
        return updated
    }

    /** Time on screen today and whether TikTok is in front right now. Needs usage access. */
    fun tallyToday(): TimeLimit.Tally {
        val manager = appContext.getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val events = ArrayList<TimeLimit.UsageEvent>()
        val stream = manager.queryEvents(dayStart, now)
        val event = UsageEvents.Event()
        while (stream.hasNextEvent()) {
            stream.getNextEvent(event)
            if (event.packageName !in PACKAGES) continue
            // 1 = came to the front (ACTIVITY_RESUMED / MOVE_TO_FOREGROUND), 2 = left it (ACTIVITY_PAUSED / MOVE_TO_BACKGROUND).
            // The same numbers on every Android version we support (minSdk 26).
            when (event.eventType) {
                1 -> events += TimeLimit.UsageEvent(event.timeStamp, toForeground = true)
                2 -> events += TimeLimit.UsageEvent(event.timeStamp, toForeground = false)
            }
        }
        return TimeLimit.tally(events, dayStart, now)
    }

    /** True once today's allowance is used up. False when no allowance is set or usage access is missing. */
    fun isOver(): Boolean {
        val allowance = allowance() ?: return false
        if (!hasUsageAccess(appContext)) return false
        return TimeLimit.isOver(tallyToday().usedMs, allowance.currentMin)
    }

    private fun today(): Long = LocalDate.now().toEpochDay()

    private fun read(): TimeLimit.Allowance? =
        try {
            val stored = json.decodeFromString<StoredAllowance>(file.readFully().decodeToString())
            TimeLimit.Allowance(stored.currentMin, stored.pendingMin, stored.pendingFromDay)
        } catch (e: Exception) {
            null // no file yet
        }

    private fun write(allowance: TimeLimit.Allowance) {
        val stream = file.startWrite()
        try {
            val stored = StoredAllowance(allowance.currentMin, allowance.pendingMin, allowance.pendingFromDay)
            stream.write(json.encodeToString(StoredAllowance.serializer(), stored).encodeToByteArray())
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
    }

    companion object {
        /** TikTok's app ids: the main one and the one used in some regions. */
        private val PACKAGES = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")

        /** Whether the user has switched on "Usage access" for Identity in system Settings. */
        fun hasUsageAccess(context: Context): Boolean {
            val ops = context.getSystemService(AppOpsManager::class.java)
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}
