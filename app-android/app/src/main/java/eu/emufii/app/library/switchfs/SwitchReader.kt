package eu.emufii.app.library.switchfs

import android.content.Context
import android.net.Uri
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Reads the one thing a Switch dump says without any console key: its title id.
 *
 * The title and the icon live in an encrypted NCA, and reading them used to be
 * the whole point of this package — a few megabytes of AES per file, against a
 * key file the player had to be talked into providing. The titles now come from
 * the public index by title id (see `GameTitles`), in the app's language, and
 * the icons from the artwork sources; the decryption stack, and the `prod.keys`
 * plumbing that fed it, are gone.
 *
 * What is left is the cheapest read in the library: the plaintext table of
 * contents at the head of an NSP, no decryption, one small read. A cartridge
 * dump (`.xci`) carries no ticket and says nothing without keys — its game is
 * named by its filename, cleaned, and that is the honest limit.
 */
class SwitchReader(private val context: Context) {

    /**
     * The title id an NSP gives away for free, e.g. `0100CD801CE5E000`.
     *
     * Read off the ticket or certificate entry name, which an NSP carries in
     * clear: `0100cd801ce5e0000000000000000011.tik`. Not from the file name on
     * disk, which is whoever-dumped-it's opinion.
     */
    fun titleId(uri: Uri): String? = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            FileInputStream(pfd.fileDescriptor).channel.use { channel ->
                Pfs0.entries(ChannelAccess(channel))?.asSequence()
                    ?.mapNotNull { entry ->
                        val stem = entry.name.substringBefore('.')
                        stem.takeIf {
                            (entry.name.endsWith(".tik") || entry.name.endsWith(".cert")) &&
                                it.length >= 16 && it.take(16).all { c -> c.isDigit() || c in 'a'..'f' || c in 'A'..'F' }
                        }
                    }
                    ?.firstOrNull()
                    ?.take(16)
                    ?.uppercase()
            }
        }
    }.getOrNull()

    private class ChannelAccess(private val channel: FileChannel) : Pfs0.RandomAccess {
        override val size: Long get() = channel.size()
        override fun read(offset: Long, length: Int): ByteArray {
            require(length >= 0) { "negative read" }
            val buffer = ByteBuffer.allocate(length)
            channel.position(offset)
            while (buffer.hasRemaining()) {
                if (channel.read(buffer) < 0) break
            }
            return buffer.array()
        }
    }
}
