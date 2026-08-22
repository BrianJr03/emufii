package eu.emufii.app.ps2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Ps2BiosTest {

    @Test
    fun `romver selects the old and new nvm layouts`() {
        assertEquals(0x1C0, Ps2Bios.ilinkOffset(0x145))
        assertEquals(0x1E0, Ps2Bios.ilinkOffset(0x146))
        assertEquals(0x1E0, Ps2Bios.ilinkOffset(0x214)) // BIOS 2.20: minor is decimal 20.
    }

    @Test
    fun `reads romver from a romdir`() {
        val bios = syntheticBios("0220EC20060210")
        assertEquals(0x214, Ps2Bios.version(bios))
    }

    @Test
    fun `rejects a file without a bios romdir`() {
        assertNull(Ps2Bios.version(ByteArray(4096)))
    }

    private fun syntheticBios(romver: String): ByteArray {
        val out = ByteArray(4096)
        // Payloads: RESET occupies the first 32 bytes; ROMVER follows it.
        romver.toByteArray(Charsets.US_ASCII).copyInto(out, destinationOffset = 32)
        record(out, 0x100, "RESET", 32)
        record(out, 0x110, "ROMVER", 14)
        record(out, 0x120, "", 0)
        return out
    }

    private fun record(out: ByteArray, at: Int, name: String, size: Int) {
        name.toByteArray(Charsets.US_ASCII).copyInto(out, at)
        out[at + 12] = (size and 0xff).toByte()
        out[at + 13] = ((size ushr 8) and 0xff).toByte()
        out[at + 14] = ((size ushr 16) and 0xff).toByte()
        out[at + 15] = ((size ushr 24) and 0xff).toByte()
    }
}
