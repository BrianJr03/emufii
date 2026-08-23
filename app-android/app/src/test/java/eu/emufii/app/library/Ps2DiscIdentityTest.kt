package eu.emufii.app.library

import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Test

class Ps2DiscIdentityTest {

    @Test
    fun `SYSTEM CNF path locates the ELF and reproduces ARMSX2 XOR`() {
        val disc = ByteArray(32 * SECTOR)
        val rootLba = 20
        val cnfLba = 21
        val modulesLba = 22
        val elfLba = 23
        val cnf = "BOOT2 = cdrom0:\\MODULES\\SLUS_999.99;1\r\nVER = 1.00\r\n"
            .toByteArray(Charsets.ISO_8859_1)
        // The last three bytes are deliberately ignored: ARMSX2 XORs only
        // complete u32 words, unlike a conventional checksum.
        val elf = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        disc[16 * SECTOR] = 1
        putAscii(disc, 16 * SECTOR + 1, "CD001")
        putRecord(disc, 16 * SECTOR + 156, rootLba, SECTOR, "\u0000", directory = true)
        val firstLength = putRecord(disc, rootLba * SECTOR, cnfLba, cnf.size, "SYSTEM.CNF;1")
        putRecord(
            disc,
            rootLba * SECTOR + firstLength,
            modulesLba,
            SECTOR,
            "MODULES",
            directory = true,
        )
        putRecord(disc, modulesLba * SECTOR, elfLba, elf.size, "SLUS_999.99;1")
        cnf.copyInto(disc, cnfLba * SECTOR)
        elf.copyInto(disc, elfLba * SECTOR)

        val identity = Ps2DiscIdentityReader.read { offset, into, count ->
            if (offset < 0 || offset >= disc.size) return@read 0
            val copied = minOf(count, disc.size - offset.toInt())
            disc.copyInto(into, 0, offset.toInt(), offset.toInt() + copied)
            copied
        }

        assertEquals(Ps2DiscIdentity("SLUS-99999", "0C040404"), identity)
        assertEquals("SLUS-99999_0C040404.ini", identity?.settingsFilename)
    }

    @Test
    fun `BOOT instead of BOOT2 is not claimed as a PS2 identity`() {
        assertNull(Ps2DiscIdentityReader.bootPath("BOOT = cdrom:\\PSX.EXE;1"))
    }

    @Test
    fun `a real ISO yields the identity ARMSX2 reports`() {
        val path = System.getenv("EMUFII_PS2_ISO")
        val serial = System.getenv("EMUFII_PS2_ISO_SERIAL")
        val crc = System.getenv("EMUFII_PS2_ISO_CRC")
        assumeTrue(path != null && File(path).exists() && serial != null && crc != null)
        RandomAccessFile(path!!, "r").use { file ->
            val identity = Ps2DiscIdentityReader.read { offset, into, count ->
                file.seek(offset)
                var done = 0
                while (done < count) {
                    val read = file.read(into, done, count - done)
                    if (read <= 0) break
                    done += read
                }
                done
            }
            assertEquals(Ps2DiscIdentity(serial!!, crc!!), identity)
        }
    }

    private fun putRecord(
        into: ByteArray,
        at: Int,
        lba: Int,
        size: Int,
        name: String,
        directory: Boolean = false,
    ): Int {
        val nameBytes = name.toByteArray(Charsets.ISO_8859_1)
        val length = 33 + nameBytes.size + if (nameBytes.size % 2 == 0) 1 else 0
        into[at] = length.toByte()
        putLe(into, at + 2, lba)
        putLe(into, at + 10, size)
        into[at + 25] = if (directory) 2 else 0
        into[at + 32] = nameBytes.size.toByte()
        nameBytes.copyInto(into, at + 33)
        return length
    }

    private fun putLe(into: ByteArray, at: Int, value: Int) {
        into[at] = value.toByte()
        into[at + 1] = (value ushr 8).toByte()
        into[at + 2] = (value ushr 16).toByte()
        into[at + 3] = (value ushr 24).toByte()
    }

    private fun putAscii(into: ByteArray, at: Int, text: String) {
        text.forEachIndexed { index, char -> into[at + index] = char.code.toByte() }
    }

    private companion object {
        const val SECTOR = 2048
    }
}
