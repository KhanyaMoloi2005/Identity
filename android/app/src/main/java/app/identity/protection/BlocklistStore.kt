package app.identity.protection

import android.content.Context
import app.identity.filter.BlocklistUpdate
import app.identity.filter.CompactDomainSet
import java.io.File

/**
 * The blocklist update installed on this phone (M3.6), kept as three files in the app's private
 * folder: the list, the manifest and the signature. Every time it is read it is checked again
 * against the public key, so a damaged or swapped file is ignored and the bundled list is used.
 */
internal class BlocklistStore(context: Context) {
    private val parent = context.applicationContext.filesDir
    private val dir = File(parent, "blocklist-update")
    private val staging = File(parent, "blocklist-update.new")

    class Installed(val manifest: BlocklistUpdate.Manifest, val domains: CompactDomainSet)

    /** The installed update, or null if there is none or it no longer passes the checks. */
    @Synchronized
    fun installed(): Installed? {
        return try {
            val manifest = File(dir, MANIFEST).readBytes()
            val signature = File(dir, SIGNATURE).readBytes()
            val list = File(dir, LIST).readBytes()
            val result = BlocklistUpdate.verify(manifest, signature, list, currentVersion = 0, currentEntries = 0)
            (result as? BlocklistUpdate.Result.Accepted)?.let { Installed(it.manifest, it.domains) }
        } catch (e: Exception) {
            null // nothing installed yet
        }
    }

    /**
     * Replaces the installed update with a new one that has already passed [BlocklistUpdate.verify].
     * The new files are written to a side folder first, so a crash can't leave a half-written list in use.
     */
    @Synchronized
    fun install(manifest: ByteArray, signature: ByteArray, list: ByteArray) {
        staging.deleteRecursively()
        staging.mkdirs()
        File(staging, MANIFEST).writeBytes(manifest)
        File(staging, SIGNATURE).writeBytes(signature)
        File(staging, LIST).writeBytes(list)
        dir.deleteRecursively()
        if (!staging.renameTo(dir)) staging.deleteRecursively()
    }

    private companion object {
        const val MANIFEST = "manifest.txt"
        const val SIGNATURE = "manifest.sig"
        const val LIST = "nsfw.txt"
    }
}
