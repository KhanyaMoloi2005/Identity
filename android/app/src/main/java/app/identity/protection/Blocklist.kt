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
        val list = context.assets.open(ASSET).bufferedReader().useLines { CompactDomainSet.fromLines(it) } +
            DomainSet.of(BlockPresets.X_AND_REDDIT) +
            DomainSet.of(BlockPresets.IMAGE_SEARCH)
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return if (debuggable) list + DomainSet.of(DEBUG_TEST_DOMAINS) else list
    }

    /** The bundled list plus the user's own sites and words. Custom rules never block [CustomRules.NEVER_BLOCKED]. */
    fun withCustomRules(bundled: DomainSet, rules: List<CustomRule>): DomainMatcher {
        val sites = rules.filter { it.kind == CustomRule.Kind.SITE }.map { it.value }
        val words = rules.filter { it.kind == CustomRule.Kind.WORD }.map { it.value }
        return DomainMatcher(
            blocked = bundled + DomainSet.of(sites) + CustomRules.containingAny(words),
            allowed = DomainSet.of(CustomRules.NEVER_BLOCKED),
        )
    }
}
