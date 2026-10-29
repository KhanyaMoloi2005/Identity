package app.identity.filter

/** A read-only set of domain names. Callers pass names already lower-cased, without a trailing dot. */
fun interface DomainSet {
    operator fun contains(name: String): Boolean

    /** A set that contains every name in this one or in [other]. */
    operator fun plus(other: DomainSet): DomainSet = DomainSet { it in this || it in other }

    companion object {
        val EMPTY = DomainSet { false }

        /** A small set from a list of rules, e.g. a few test or user rules. */
        fun of(rules: Collection<String>): DomainSet {
            val names = rules.mapNotNull(::normalizeDomain).toHashSet()
            return DomainSet { it in names }
        }
    }
}

/**
 * A large, read-only domain list (the bundled blocklist) packed for low memory use: every name is
 * stored once, sorted, in one byte array, and found by binary search. About 2 MB for 92,000 names,
 * versus roughly 9 MB as ordinary Java strings in a hash set, which matters for a VPN running all day.
 */
class CompactDomainSet private constructor(
    private val bytes: ByteArray,
    /** Where each name starts in [bytes]; one extra entry at the end marks where the last one ends. */
    private val starts: IntArray,
) : DomainSet {

    val size: Int get() = starts.size - 1

    override fun contains(name: String): Boolean {
        var low = 0
        var high = size - 1
        while (low <= high) {
            val middle = (low + high) ushr 1
            val comparison = compareAt(middle, name)
            when {
                comparison < 0 -> low = middle + 1
                comparison > 0 -> high = middle - 1
                else -> return true
            }
        }
        return false
    }

    /** Compares stored name number [index] with [name], byte by byte (names are ASCII). */
    private fun compareAt(index: Int, name: String): Int {
        val start = starts[index]
        val length = starts[index + 1] - start
        val shared = minOf(length, name.length)
        for (i in 0 until shared) {
            val difference = (bytes[start + i].toInt() and 0xFF) - name[i].code
            if (difference != 0) return difference
        }
        return length - name.length
    }

    companion object {
        /**
         * Builds the set from blocklist lines. Blank lines and `#` comments are skipped, and so is
         * anything that isn't a plain ASCII domain name, so a damaged file can't add strange entries.
         */
        fun fromLines(lines: Sequence<String>): CompactDomainSet {
            val names = lines.mapNotNull(::normalizeDomain).toSortedSet()
            val starts = IntArray(names.size + 1)
            val bytes = ByteArray(names.sumOf { it.length })
            var offset = 0
            names.forEachIndexed { index, name ->
                starts[index] = offset
                for (char in name) bytes[offset++] = char.code.toByte()
            }
            starts[names.size] = offset
            return CompactDomainSet(bytes, starts)
        }
    }
}

/**
 * Lower-cases [line] and drops a trailing dot. Returns null for blank lines, comments and anything
 * that isn't made of letters, digits, `-`, `_` and `.` (internationalised names arrive as `xn--` ASCII).
 */
internal fun normalizeDomain(line: String): String? {
    val name = line.trim().trimEnd('.').lowercase()
    if (name.isEmpty() || name.startsWith('#')) return null
    if (name.startsWith('.') || ".." in name) return null
    if (!name.all { it in 'a'..'z' || it in '0'..'9' || it == '-' || it == '_' || it == '.' }) return null
    return name
}
