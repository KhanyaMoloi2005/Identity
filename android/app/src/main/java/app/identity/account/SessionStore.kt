package app.identity.account

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts and decrypts the saved login. An interface so the store is tested on a computer without Android's keystore. */
internal interface SessionCipher {
    fun encrypt(plain: ByteArray): ByteArray

    /** Null if [data] cannot be decrypted (for example the key was lost): the login is then treated as gone. */
    fun decrypt(data: ByteArray): ByteArray?
}

/**
 * AES-256-GCM with a key that lives in Android's keystore and never leaves it (not even this app can
 * read it out). The saved login is useless without it, and the file is excluded from backup anyway.
 */
internal class KeystoreCipher : SessionCipher {
    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv + cipher.doFinal(plain) // the random 12-byte IV travels with the ciphertext
    }

    override fun decrypt(data: ByteArray): ByteArray? =
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES))
            cipher.doFinal(data, IV_BYTES, data.size - IV_BYTES)
        } catch (e: Exception) {
            null
        }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ALIAS = "identity-account-key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}

/** What is kept on the phone about the signed-in account. The tokens are secret; the rest is what the screens show. */
@Serializable
internal data class StoredAccount(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtSec: Long,
    val userId: String,
    val email: String,
    val nickname: String? = null,
    val deviceId: String? = null,
    val lastTouchMs: Long = 0,
    /** Partners' nicknames by account id, so a connection that ended can still be named (the server hides them then). Only for connections still listed. */
    val knownNicknames: Map<String, String> = emptyMap(),
) {
    fun session() = Session(accessToken, refreshToken, expiresAtSec, userId, email)

    fun withSession(s: Session) = copy(accessToken = s.accessToken, refreshToken = s.refreshToken, expiresAtSec = s.expiresAtSec)
}

/**
 * The saved login: one encrypted file in the app's private folder (excluded from backup). Written to
 * a side file first and then moved into place, so a crash can't leave half a file.
 */
internal class SessionStore(private val dir: File, private val cipher: SessionCipher) {
    private val file = File(dir, "account.bin")
    private val temp = File(dir, "account.bin.tmp")
    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun load(): StoredAccount? {
        if (!file.exists()) return null
        return try {
            val plain = cipher.decrypt(file.readBytes()) ?: run {
                file.delete() // unreadable (key gone): treat as signed out
                return null
            }
            json.decodeFromString<StoredAccount>(plain.decodeToString())
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun save(account: StoredAccount) {
        dir.mkdirs()
        temp.writeBytes(cipher.encrypt(json.encodeToString(StoredAccount.serializer(), account).encodeToByteArray()))
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    @Synchronized
    fun clear() {
        file.delete()
        temp.delete()
    }
}
