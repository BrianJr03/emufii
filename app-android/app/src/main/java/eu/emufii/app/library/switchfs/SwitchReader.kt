package eu.emufii.app.library.switchfs

import android.content.Context
import android.net.Uri
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * The title id is the one thing a Switch dump says without a console key: title and icon
 * live in an encrypted NCA, and the titles now come from the public index instead (see
 * `GameTitles`). One small read of the NSP's plaintext table of contents, no decryption.
 * A cartridge dump (`.xci`) carries no ticket and is named by its filename, cleaned.
 */
class SwitchReader(private val context: Context) {

    /**
     * Read off the ticket or certificate entry name, which an NSP carries in clear
     * (`0100cd801ce5e0000000000000000011.tik`), never off the file name on disk.
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
