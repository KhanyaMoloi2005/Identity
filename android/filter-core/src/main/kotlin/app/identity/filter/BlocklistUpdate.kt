package app.identity.filter

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * The rules for accepting a downloaded blocklist (M3.6, R-05, D-035). An update is three files:
 * the list, a small manifest that describes it (version, SHA-256, number of entries), and a
 * signature over the manifest made with a private key that only the product owner holds. The app
 * carries the matching public key. Anything that does not pass every check is thrown away and the
 * list already on the phone stays in use. Pure logic, so it is tested on a computer.
 */
object BlocklistUpdate {
    /** The public half of the signing key (X.509, base64). The private half is kept offline. */
    const val PUBLIC_KEY_BASE64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEBHcydlzN9HfGaBbWVyB0ow9siw+uv6KKsuHkuAW0OlgMtiAD+1hpCi02kFR0hFQ0draJFTGxaN40gbimBu9XYw=="

    /** A list bigger than this is refused before it is read (the bundled one is about 1.4 MB). */
    const val MAX_LIST_BYTES = 20_000_000

    /** A list with fewer names than this is refused: it is more likely a broken download than a real list. */
    const val MIN_ENTRIES = 50_000

    /** A new list must keep at least this share of the names already in use, so an update cannot quietly gut protection. */
    const val MIN_SHARE_OF_CURRENT = 0.5

    /** What the signed manifest says about the list. [generated] and [sourceVersion] are for display only. */
    data class Manifest(
        val version: Long,
        val sha256: String,
        val entries: Int,
        val generated: String? = null,
        val sourceVersion: String? = null,
    )

    enum class Reason {
        BAD_SIGNATURE,
        BAD_MANIFEST,
        NOT_NEWER,
        TOO_BIG,
        HASH_MISMATCH,
        ENTRY_COUNT_MISMATCH,
        TOO_FEW_ENTRIES,
        SHRANK_TOO_MUCH,
    }

    sealed class Result {
        data class Accepted(val manifest: Manifest, val domains: CompactDomainSet) : Result()
        data class Rejected(val reason: Reason) : Result()
    }

    /**
     * Checks a downloaded update. [currentVersion] and [currentEntries] describe the list in use now
     * (0 for none). Every check must pass, in this order: signature, manifest, newer version, size,
     * hash, entry count, minimum size, not shrinking too much.
     */
    fun verify(
        manifestBytes: ByteArray,
        signature: ByteArray,
        listBytes: ByteArray,
        currentVersion: Long,
        currentEntries: Int,
        publicKey: PublicKey? = defaultKey(),
        minEntries: Int = MIN_ENTRIES,
    ): Result {
        if (publicKey == null || !signatureIsValid(manifestBytes, signature, publicKey)) return Result.Rejected(Reason.BAD_SIGNATURE)
        val manifest = parseManifest(manifestBytes.decodeToString()) ?: return Result.Rejected(Reason.BAD_MANIFEST)
        if (manifest.version <= currentVersion) return Result.Rejected(Reason.NOT_NEWER)
        if (listBytes.size > MAX_LIST_BYTES) return Result.Rejected(Reason.TOO_BIG)
        if (!sha256Hex(listBytes).equals(manifest.sha256, ignoreCase = true)) return Result.Rejected(Reason.HASH_MISMATCH)
        val domains = CompactDomainSet.fromLines(listBytes.decodeToString().lineSequence())
        if (domains.size != manifest.entries) return Result.Rejected(Reason.ENTRY_COUNT_MISMATCH)
        if (domains.size < minEntries) return Result.Rejected(Reason.TOO_FEW_ENTRIES)
        if (currentEntries > 0 && domains.size < currentEntries * MIN_SHARE_OF_CURRENT) return Result.Rejected(Reason.SHRANK_TOO_MUCH)
        return Result.Accepted(manifest, domains)
    }

    /** `key=value` lines. `version`, `sha256` and `entries` are required; other keys are ignored. Null if anything is wrong. */
    fun parseManifest(text: String): Manifest? {
        val values = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
            .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
        val version = values["version"]?.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val sha256 = values["sha256"]?.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F' } } ?: return null
        val entries = values["entries"]?.toIntOrNull()?.takeIf { it > 0 } ?: return null
        return Manifest(version, sha256.lowercase(), entries, values["generated"], values["source_version"])
    }

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun signatureIsValid(message: ByteArray, signature: ByteArray, key: PublicKey): Boolean =
        try {
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(key)
                update(message)
                verify(signature)
            }
        } catch (e: Exception) {
            false // a malformed signature is just a bad one
        }

    /** The app's built-in key, or null if it cannot be read (then nothing is ever accepted). */
    fun defaultKey(): PublicKey? =
        try {
            KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY_BASE64)))
        } catch (e: Exception) {
            null
        }
}
