package eu.emufii.app.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which console an `.iso` belongs to, when the extension refuses to say. The offsets
 * were measured on the disc images on this machine: six PSP rips stayed on the PSP,
 * eleven RVZ files came out as Wii, the one Dreamcast `.chd` did not move.
 *
 * A wrong "yes" moves a game onto an emulator that cannot open it, a wrong "no" leaves
 * it where it was; the assertions weigh accordingly.
 */
class DiscImageTest {

    private fun header(build: ByteArray.() -> Unit) =
        ByteArray(DiscImage.HEADER_BYTES).apply(build)

    private fun ByteArray.putBE(at: Int, value: Int) {
        this[at] = (value ushr 24).toByte()
        this[at + 1] = (value ushr 16).toByte()
        this[at + 2] = (value ushr 8).toByte()
        this[at + 3] = value.toByte()
    }

    private fun ByteArray.putAscii(at: Int, text: String) {
        for ((i, c) in text.withIndex()) this[at + i] = c.code.toByte()
    }

    @Test
    fun `a raw image is read from the magic at its own offset`() {
        // Wii and GameCube magics sit four bytes apart: checked in order, never scanned for.
        assertEquals(Console.WII, DiscImage.identify(header { putBE(0x18, 0x5D1C9EA3) }))
        assertEquals(
            Console.GAMECUBE,
            DiscImage.identify(header { putBE(0x1C, 0xC2339F3D.toInt()) })
        )
    }

    @Test
    fun `a PSP rip is not identified, so it stays with PPSSPP`() {
        // Measured: a UMD rip has plain zeroes at both offsets.
        assertNull(DiscImage.identify(ByteArray(DiscImage.HEADER_BYTES)))
    }

    @Test
    fun `RVZ states its console in the disc type field`() {
        assertEquals(
            Console.WII,
            DiscImage.identify(header { putAscii(0, "RVZ"); putBE(0x48, 2) })
        )
        assertEquals(
            Console.GAMECUBE,
            DiscImage.identify(header { putAscii(0, "WIA"); putBE(0x48, 1) })
        )
    }

    @Test
    fun `an unknown disc type falls back to the copy of the real header`() {
        // The container copies the disc's first 0x80 bytes verbatim at 0x58: the Wii
        // magic shows up at 0x70 in a hex dump.
        assertEquals(
            Console.WII,
            DiscImage.identify(
                header {
                    putAscii(0, "RVZ")
                    putBE(0x48, 99)
                    putBE(0x58 + 0x18, 0x5D1C9EA3)
                }
            )
        )
    }

    @Test
    fun `WBFS only ever held Wii discs`() {
        assertEquals(Console.WII, DiscImage.identify(header { putAscii(0, "WBFS") }))
    }

    @Test
    fun `a truncated or empty read identifies nothing`() {
        assertNull(DiscImage.identify(ByteArray(0)))
        assertNull(DiscImage.identify(ByteArray(3)))
    }

    @Test
    fun `the PSP keeps every extension it had`() {
        assertEquals(Console.PSP, Console.forExtension("iso"))
        assertEquals(Console.PSP, Console.forExtension("chd"))
        assertEquals(Console.PSP, Console.forExtension("cso"))
        assertEquals(Console.PSP, Console.forExtension("pbp"))
    }

    /**
     * A disc as far as the ISO9660 volume descriptor. The values come from the Thor's
     * real files on 2026-08-17: TimeSplitters 2 (PS2) is `PLAYSTATION` / `SLES_50877`,
     * WipEout Pulse (PSP) is `PSP GAME` / `SCEE`.
     */
    private fun disc(systemId: String, volumeId: String = "") =
        ByteArray(DiscImage.PVD_BYTES).apply {
            this[0x8000] = 1
            putAscii(0x8001, "CD001")
            putAscii(0x8008, systemId.padEnd(32))
            putAscii(0x8028, volumeId.padEnd(32))
        }

    @Test
    fun `a PS2 disc is recognised by its system identifier`() {
        assertEquals(Console.PS2, DiscImage.identify(disc("PLAYSTATION", "SLES_50877")))
    }

    @Test
    fun `a UMD rip stays with the PSP, even read as far as the descriptor`() {
        // On the Thor the six PS2 games and the six PSP games are all `.iso`: only the
        // descriptor settles one the folder did not speak for.
        assertEquals(Console.PSP, DiscImage.identify(disc("PSP GAME", "SCEE")))
    }

    @Test
    fun `the PS2 number is the one ARMSX2 shows`() {
        assertEquals("SLES-50877", DiscImage.gameId(disc("PLAYSTATION", "SLES_50877")))
    }

    @Test
    fun `a short header promotes nothing to the PS2`() {
        // The read stops before `0x8000`: say nothing rather than trust absent bytes.
        assertNull(DiscImage.identify(ByteArray(DiscImage.HEADER_BYTES)))
    }

    @Test
    fun `the PS2 claims no extension`() {
        // The table is a map: giving the PS2 `.iso` would take it from the PSP.
        assertTrue(Console.PS2.extensions.isEmpty())
        assertEquals(Console.PSP, Console.forExtension("iso"))
    }

    @Test
    fun `the sniffed extensions are ones the scan actually recognises`() {
        for (ext in DiscImage.SNIFFED_EXTENSIONS) {
            assert(Console.forExtension(ext) != null) {
                ".$ext is sniffed but no console claims it, so the scan drops it first"
            }
        }
    }

    @Test
    fun `a console folder name settles the console`() {
        // The player sorted the file themselves: the folder outranks any extension clash.
        assertEquals(Console.PS2, Console.forFolder("ps2"))
        assertEquals(Console.PS2, Console.forFolder("PlayStation 2"))
        assertEquals(Console.PSP, Console.forFolder("PSP"))
        assertEquals(Console.DS, Console.forFolder("nds"))
        assertEquals(Console.THREE_DS, Console.forFolder("n3ds"))
        assertEquals(Console.SWITCH, Console.forFolder("Nintendo Switch"))
        assertEquals(Console.GAMECUBE, Console.forFolder("gc"))
        assertNull(Console.forFolder("ROMS"))
        assertNull(Console.forFolder("dumps"))
        assertNull(Console.forFolder("ps1"))
        assertNull(Console.forFolder("psvita"))
    }
}
