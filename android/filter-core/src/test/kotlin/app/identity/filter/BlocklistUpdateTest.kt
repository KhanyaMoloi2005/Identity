package app.identity.filter

import app.identity.filter.BlocklistUpdate.Reason
import app.identity.filter.BlocklistUpdate.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class BlocklistUpdateTest {
    private val keys: KeyPair = newKeys()
    private val otherKeys: KeyPair = newKeys()

    private fun newKeys(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

    private fun sign(message: ByteArray, key: KeyPair = keys): ByteArray =
        Signature.getInstance("SHA256withECDSA").run {
            initSign(key.private)
            update(message)
            sign()
        }

    private fun names(count: Int) = (1..count).map { "site$it.example" }

    private fun listBytes(count: Int, header: String = "# test list\n") = (header + names(count).joinToString("\n") + "\n").toByteArray()

    private fun manifestText(list: ByteArray, version: Long = 100, entries: Int, sha: String = BlocklistUpdate.sha256Hex(list)) =
        "version=$version\nsha256=$sha\nentries=$entries\ngenerated=2026-10-29T21:00:00Z\nsource_version=test\n"

    private fun verify(
        list: ByteArray,
        manifest: String,
        signer: KeyPair = keys,
        currentVersion: Long = 50,
        currentEntries: Int = 0,
        minEntries: Int = 10,
    ): Result {
        val bytes = manifest.toByteArray()
        return BlocklistUpdate.verify(bytes, sign(bytes, signer), list, currentVersion, currentEntries, keys.public, minEntries)
    }

    @Test
    fun aGoodUpdateIsAccepted() {
        val list = listBytes(20)
        val result = verify(list, manifestText(list, entries = 20)) as Result.Accepted
        assertEquals(100L, result.manifest.version)
        assertEquals(20, result.domains.size)
        assertTrue("site7.example" in result.domains)
        assertEquals("test", result.manifest.sourceVersion)
    }

    @Test
    fun aManifestSignedByTheWrongKeyIsRejected() {
        val list = listBytes(20)
        val result = verify(list, manifestText(list, entries = 20), signer = otherKeys)
        assertEquals(Result.Rejected(Reason.BAD_SIGNATURE), result)
    }

    @Test
    fun aTamperedManifestIsRejected() {
        val list = listBytes(20)
        val original = manifestText(list, entries = 20).toByteArray()
        val signature = sign(original)
        val tampered = manifestText(list, version = 999, entries = 20).toByteArray()
        assertEquals(
            Result.Rejected(Reason.BAD_SIGNATURE),
            BlocklistUpdate.verify(tampered, signature, list, 50, 0, keys.public, 10),
        )
    }

    @Test
    fun garbageInPlaceOfASignatureIsRejectedWithoutCrashing() {
        val list = listBytes(20)
        val manifest = manifestText(list, entries = 20).toByteArray()
        for (junk in listOf(ByteArray(0), byteArrayOf(1, 2, 3), ByteArray(200) { it.toByte() })) {
            assertEquals(Result.Rejected(Reason.BAD_SIGNATURE), BlocklistUpdate.verify(manifest, junk, list, 50, 0, keys.public, 10))
        }
    }

    @Test
    fun aMissingKeyAcceptsNothing() {
        val list = listBytes(20)
        val manifest = manifestText(list, entries = 20).toByteArray()
        assertEquals(Result.Rejected(Reason.BAD_SIGNATURE), BlocklistUpdate.verify(manifest, sign(manifest), list, 50, 0, null, 10))
    }

    @Test
    fun aListChangedAfterSigningIsRejected() {
        val list = listBytes(20)
        val manifest = manifestText(list, entries = 20)
        val swapped = listBytes(20, header = "# something else\n") // same names, different bytes
        assertEquals(Result.Rejected(Reason.HASH_MISMATCH), verify(swapped, manifest))
    }

    @Test
    fun anOldOrEqualVersionIsRejected() {
        val list = listBytes(20)
        assertEquals(Result.Rejected(Reason.NOT_NEWER), verify(list, manifestText(list, version = 50, entries = 20), currentVersion = 50))
        assertEquals(Result.Rejected(Reason.NOT_NEWER), verify(list, manifestText(list, version = 49, entries = 20), currentVersion = 50))
        assertTrue(verify(list, manifestText(list, version = 51, entries = 20), currentVersion = 50) is Result.Accepted)
    }

    @Test
    fun theEntryCountMustMatchTheList() {
        val list = listBytes(20)
        assertEquals(Result.Rejected(Reason.ENTRY_COUNT_MISMATCH), verify(list, manifestText(list, entries = 25)))
    }

    @Test
    fun aListThatIsTooSmallIsRejected() {
        val list = listBytes(5)
        assertEquals(Result.Rejected(Reason.TOO_FEW_ENTRIES), verify(list, manifestText(list, entries = 5), minEntries = 10))
    }

    @Test
    fun aListThatShrinksByMoreThanHalfIsRejected() {
        val list = listBytes(20)
        assertEquals(
            Result.Rejected(Reason.SHRANK_TOO_MUCH),
            verify(list, manifestText(list, entries = 20), currentEntries = 50),
        )
        assertTrue(verify(list, manifestText(list, entries = 20), currentEntries = 40) is Result.Accepted)
    }

    @Test
    fun anOversizedListIsRejectedBeforeItIsRead() {
        val big = ByteArray(BlocklistUpdate.MAX_LIST_BYTES + 1) { 'a'.code.toByte() }
        assertEquals(Result.Rejected(Reason.TOO_BIG), verify(big, manifestText(big, entries = 20)))
    }

    @Test
    fun brokenManifestsAreRejected() {
        val list = listBytes(20)
        val sha = BlocklistUpdate.sha256Hex(list)
        for (bad in listOf(
            "",
            "version=abc\nsha256=$sha\nentries=20\n",
            "version=0\nsha256=$sha\nentries=20\n",
            "version=100\nsha256=nothex\nentries=20\n",
            "version=100\nsha256=$sha\n",
            "version=100\nentries=20\n",
        )) {
            assertNull(bad, BlocklistUpdate.parseManifest(bad))
            assertEquals(Result.Rejected(Reason.BAD_MANIFEST), verify(list, bad))
        }
    }

    @Test
    fun theBuiltInKeyIsValid() {
        assertNotNull(BlocklistUpdate.defaultKey())
    }

    @Test
    fun aListSignedWithTheRealKeyOnTheProductOwnersLaptopVerifies() {
        // Made with `openssl dgst -sha256 -sign` and the real private key, so this proves openssl's
        // signature format works with the Java code on the phone.
        fun resource(name: String) = javaClass.getResourceAsStream("signed-sample/$name")!!.readBytes()
        val result = BlocklistUpdate.verify(
            resource("manifest.txt"), resource("manifest.sig"), resource("list.txt"),
            currentVersion = 0, currentEntries = 0, minEntries = 1,
        )
        assertTrue(result.toString(), result is Result.Accepted)
        val accepted = result as Result.Accepted
        assertEquals(202610292100L, accepted.manifest.version)
        assertTrue("gamma.example" in accepted.domains)
    }

    @Test
    fun theRealSignedSampleFailsIfOneByteOfTheListChanges() {
        fun resource(name: String) = javaClass.getResourceAsStream("signed-sample/$name")!!.readBytes()
        val list = resource("list.txt").also { it[it.size - 3] = 'X'.code.toByte() }
        val result = BlocklistUpdate.verify(resource("manifest.txt"), resource("manifest.sig"), list, 0, 0, minEntries = 1)
        assertEquals(Result.Rejected(Reason.HASH_MISMATCH), result)
    }
}
