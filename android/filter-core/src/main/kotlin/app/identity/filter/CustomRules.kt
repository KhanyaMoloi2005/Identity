package app.identity.filter

/**
 * Checks and cleans the user's own block rules (M3.3b, D-029). Rules only ever add blocks: there
 * are no user exceptions. A new rule can be undone for 10 minutes, to fix typos.
 */
object CustomRules {
    const val MIN_WORD_LENGTH = 4
    const val MAX_RULES = 500
    const val UNDO_WINDOW_MS = 10 * 60 * 1000L

    /**
     * Names the phone needs to know it's online. Custom rules (e.g. a broad word) never block these;
     * otherwise Android would show "no internet" all the time.
     */
    val NEVER_BLOCKED: List<String> = listOf("connectivitycheck.gstatic.com", "connectivitycheck.android.com")

    /**
     * Turns what the user typed into a site rule: `https://www.Example.com/page` -> `example.com`.
     * A leading `www.` is dropped so the whole site is blocked. Null if it isn't a website name.
     */
    fun normalizeSite(input: String): String? {
        val host = input.trim().lowercase()
            .substringAfter("://")
            .substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfter('@')
            .substringBefore(':')
            .removePrefix("www.")
        val name = normalizeDomain(host) ?: return null
        return if ('.' in name) name else null // a single word belongs in a word rule
    }

    /** `Casino ` -> `casino`. Null unless it's one word of at least [MIN_WORD_LENGTH] letters, digits or `-`. */
    fun normalizeWord(input: String): String? {
        val word = input.trim().lowercase()
        if (word.length < MIN_WORD_LENGTH) return null
        return if (word.all { it in 'a'..'z' || it in '0'..'9' || it == '-' }) word else null
    }

    /** Matches every name containing one of [words] anywhere: `casino` matches `bestcasino.net`. */
    fun containingAny(words: Collection<String>): DomainSet {
        val list = words.toList()
        if (list.isEmpty()) return DomainSet.EMPTY
        return DomainSet { name -> list.any { it in name } }
    }

    /**
     * Whether a rule added at [addedElapsedMs] (time since the phone started) during boot number
     * [addedBoot] may still be removed. Uses time since start-up rather than the clock, because the
     * clock can be changed in Settings; restarting the phone ends the undo window.
     */
    fun canRemove(addedBoot: Int, addedElapsedMs: Long, nowBoot: Int, nowElapsedMs: Long): Boolean {
        if (addedBoot != nowBoot) return false
        val age = nowElapsedMs - addedElapsedMs
        return age in 0 until UNDO_WINDOW_MS
    }
}
