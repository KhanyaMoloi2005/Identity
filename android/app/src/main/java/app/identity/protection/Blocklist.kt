package app.identity.protection

import android.content.Context
import android.content.pm.ApplicationInfo
import app.identity.filter.BlockPresets
import app.identity.filter.CompactDomainSet
import app.identity.filter.CustomRules
import app.identity.filter.DomainMatcher
import app.identity.filter.DomainSet

/**
 * What Identity blocks: HaGeZi's NSFW list (D-025, built by `content/blocklists/build_blocklist.py`),
 * plus X/Reddit and image-search pictures (D-028). Always on: users have no switches for these.
 * The user's own rules (D-029) are added on top by [withCustomRules].
 */
internal object Blocklist {
    private const val ASSET = "blocklist/nsfw.txt"

    /** Test builds only: a harmless site to check blocking without visiting an adult one. */
    private val DEBUG_TEST_DOMAINS = listOf("example.com")

    /** Reads and packs the list (a fraction of a second). Call off the main thread. */
    fun load(context: Context): DomainSet {
        // A downloaded update that still passes its signature check wins over the list inside the app (M3.6).
        val packed: DomainSet = BlocklistStore(context).installed()?.domains
            ?: context.assets.open(ASSET).bufferedReader().useLines { CompactDomainSet.fromLines(it) }
        val list = packed +
            DomainSet.of(BlockPresets.X_AND_REDDIT) +
            DomainSet.of(BlockPresets.IMAGE_SEARCH)
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return if (debuggable) list + DomainSet.of(DEBUG_TEST_DOMAINS) else list
    }

    /** How many names the list inside the app has, read from its header (0 if the header is missing). */
    fun bundledEntries(context: Context): Int =
        context.assets.open(ASSET).bufferedReader().useLines { lines ->
            lines.take(12).firstOrNull { it.startsWith("# Entries:") }?.substringAfter(':')?.trim()?.toIntOrNull() ?: 0
        }

    /** What the lists in use are, for the Settings screen. */
    fun info(context: Context): ListInfo {
        val installed = BlocklistStore(context).installed()
        return if (installed != null) {
            ListInfo(installed.manifest.entries, updated = true, generated = installed.manifest.generated)
        } else {
            ListInfo(bundledEntries(context), updated = false, generated = null)
        }
    }

    class ListInfo(val entries: Int, val updated: Boolean, val generated: String?)

    /** The bundled list plus the user's own sites and words. Custom rules never block [CustomRules.NEVER_BLOCKED]. */
    fun withCustomRules(
        bundled: DomainSet,
        rules: List<CustomRule>,
        /** Extra blocks that are only sometimes on, such as TikTok once its daily allowance is used up. */
        timeLimited: DomainSet = DomainSet.EMPTY,
    ): DomainMatcher {
        val sites = rules.filter { it.kind == CustomRule.Kind.SITE }.map { it.value }
        val words = rules.filter { it.kind == CustomRule.Kind.WORD }.map { it.value }
        return DomainMatcher(
            blocked = bundled + timeLimited + DomainSet.of(sites) + CustomRules.containingAny(words),
            allowed = DomainSet.of(CustomRules.NEVER_BLOCKED),
        )
    }
}
