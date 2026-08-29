package eu.emufii.app.library

import android.content.Context
import android.net.Uri
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val MEDIA_UNIT = 0x200L

data class RomHeader(
    val titleIdHex: String,
    val productCode: String?,
    val ncchOffset: Long,
    val exefsOffset: Long,
    val exefsSize: Long,
    val isDecrypted: Boolean
)

class RomHeaderReader(private val context: Context) {

    fun read(uri: Uri, cia: Boolean = false): RomHeader? = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            FileInputStream(pfd.fileDescriptor).channel.use { ch -> parse(ch, cia) }
        }
    }.getOrNull()

    private fun parse(ch: FileChannel, cia: Boolean): RomHeader? {
        val header = ByteBuffer.allocate(0x200).order(ByteOrder.LITTLE_ENDIAN)
        if (readAt(ch, 0L, header) < 0x200) return null
        val magicAt100 = String(header.array(), 0x100, 4)

        val ncchOffset: Long = when (magicAt100) {
            "NCSD" -> {
                val partOffMediaUnits = header.getInt(0x120).toLong() and 0xFFFFFFFFL
                partOffMediaUnits * MEDIA_UNIT
            }
            "NCCH" -> 0L
            // A CIA wears none of the cartridge magics: its own header names the
            // offset of its first content, and the NCCH starts there.
            else -> if (cia) contentOffset(header, ch.size()) ?: return null else return null
        }

        val ncchHeader = ByteBuffer.allocate(0x200).order(ByteOrder.LITTLE_ENDIAN)
        if (readAt(ch, ncchOffset, ncchHeader) < 0x200) return null
        if (String(ncchHeader.array(), 0x100, 4) != "NCCH") return null

        val partitionId = ncchHeader.getLong(0x108)
        val titleIdHex = String.format("%016X", partitionId)

        val productBytes = ByteArray(16)
        System.arraycopy(ncchHeader.array(), 0x150, productBytes, 0, 16)
        val productCode = String(productBytes)
            .substringBefore('\u0000')
            .trim()
            .ifBlank { null }

        val flag7 = ncchHeader.get(0x188 + 7).toInt() and 0xFF
        val isDecrypted = (flag7 and 0x04) != 0

        val exefsOffMediaUnits = ncchHeader.getInt(0x1A0).toLong() and 0xFFFFFFFFL
        val exefsSizeMediaUnits = ncchHeader.getInt(0x1A4).toLong() and 0xFFFFFFFFL
        val exefsOffset = ncchOffset + exefsOffMediaUnits * MEDIA_UNIT
        val exefsSize = exefsSizeMediaUnits * MEDIA_UNIT

        return RomHeader(
            titleIdHex = titleIdHex,
            productCode = productCode,
            ncchOffset = ncchOffset,
            exefsOffset = exefsOffset,
            exefsSize = exefsSize,
            isDecrypted = isDecrypted
        )
    }

    /**
     * Where a CIA's first content starts, read off its 0x20-byte header.
     *
     * The offset is a plain `u32` at 0x18, but a file we misidentified as a CIA
     * would read garbage there, so the value has to prove it lands inside the
     * file and past the header's fixed regions before anything is read at it —
     * the NCCH magic check at the destination does the rest.
     */
    private fun contentOffset(header: ByteBuffer, fileSize: Long): Long? {
        val offset = header.getInt(0x18).toLong() and 0xFFFFFFFFL
        // 0x2020 is the smallest real CIA: header + certificate chain + ticket
        // + TMD, all 0x40-aligned, before a single byte of content.
        return offset.takeIf { it in 0x2020 until fileSize }
    }

    private fun readAt(ch: FileChannel, pos: Long, buf: ByteBuffer): Int {
        ch.position(pos)
        var total = 0
        while (buf.hasRemaining()) {
            val n = ch.read(buf)
            if (n < 0) break
            total += n
        }
        return total
    }
}
