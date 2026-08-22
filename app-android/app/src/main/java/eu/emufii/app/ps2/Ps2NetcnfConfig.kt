package eu.emufii.app.ps2

/**
 * The PS2's network-configuration payload, the "Your Network Configuration
 * File" (YNCF) that lives inside a `BWNETCNF` save.
 *
 * Three of the files are plain text: the save index (named `BWNETCNF`),
 * `net000.cnf`, and the shared 38-byte header of the other two. The other two,
 * `ifc000.dat` and `dev000.dat`, are that same text run through a console-locked
 * cipher: Sony's netcnf library derives a shift table from the console's 8-byte
 * i.Link ID and encodes each little-endian 16-bit word as
 * `rotl16(word, shift) xor 0xFFFF` (ps2dev/ps2sdk,
 * `iop/network/netcnf/src/netcnf.c`, encode at :775, key init at :875).
 *
 * The key consequence, and the reason this file exists: **a YNCF save only
 * reads back on the console whose i.Link ID encrypted it.** There is no key
 * material in the file, and no checksum to fail loudly — a mismatched console
 * decodes word soup and the game reports the configuration as invalid.
 *
 * ### Which ID to encrypt for
 *
 * ARMSX2 answers the netcnf library's `sceCdRI` from the `.nvm` beside the
 * running BIOS, and both paths that produce that answer converge on one
 * constant (`pcsx2/CDVD/CDVD.cpp`):
 *
 * - no readable `.nvm` → `cdvdCreateNewNVM()` writes the dummy ID
 *   `00 AC FF FF FF FF B9 86` (CDVD.cpp:158);
 * - an `.nvm` whose i.Link area looks unprogrammed (bytes 2 and 3 both zero)
 *   → `sceCdReadILinkId` overrides the read with the same constant
 *   (CDVD.cpp:2621-2631).
 *
 * So a card encrypted for [ARMSX2_CONSOLE_ID] works on every install whose
 * NVRAM ARMSX2 generated itself — the single-`.bin` BIOS import that is the
 * normal setup. An install that imported a real console's `.nvm` alongside its
 * BIOS keeps that console's true ID and must be encrypted for it; hand the
 * bytes from [ilinkIdFromNvm] to the generator in that case. The card Emufii
 * used to ship was encrypted for one bench console's ID and worked nowhere
 * else — the defect this replaces.
 *
 * ### What the configuration says
 *
 * `type nic` + `dhcp`, nothing else. The static address a PS2 ends up with is
 * not this file's business in Emufii: ARMSX2's Local Link runs its own DHCP
 * server and hands each peer a distinct address derived from its peer id
 * (`pcsx2/DEV9/LocalLinkAdapter.cpp:167`), so the console asks for a lease and
 * is told apart from every other player by the emulator. A hand-written static
 * IP here would put every player on the same address instead.
 */
object Ps2NetcnfConfig {

    /**
     * The two NVRAM layouts used by ARMSX2/PCSX2. Callers must select one from
     * the BIOS they actually detected; inspecting both offsets can silently
     * pick stale bytes left in an unrelated area of an imported `.nvm`.
     */
    enum class NvmLayout(
        internal val ilinkIdOffset: Int,
        internal val config1Offset: Int,
        internal val regionParamsOffset: Int,
    ) {
        BEFORE_1_70(0x1C0, 0x300, 0x180),
        FROM_1_70(0x1E0, 0x2B0, 0x180),
    }

    /** The numeric major/minor pair stored in the BIOS's `ROMVER` entry. */
    data class BiosVersion(val major: Int, val minor: Int) {
        init {
            require(major >= 0) { "BIOS major version must be non-negative" }
            require(minor in 0..99) { "BIOS minor version must be in 0..99" }
        }

        val nvmLayout: NvmLayout
            get() = if (major > 1 || major == 1 && minor >= 70) {
                NvmLayout.FROM_1_70
            } else {
                NvmLayout.BEFORE_1_70
            }
    }

    /**
     * The i.Link ID ARMSX2 reports for BIOSes without a real console NVRAM.
     * Encrypting for it is what makes a generated card readable everywhere
     * ARMSX2 manufactured its own `.nvm`.
     */
    val ARMSX2_CONSOLE_ID: ByteArray =
        byteArrayOf(0x00.toByte(), 0xAC.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xB9.toByte(), 0x86.toByte())

    /** The 38-byte header every YNCF file starts with, blank line included. */
    private const val HEADER = "# <Sony Computer Entertainment Inc.>\n\n"

    /**
     * `ifc000.dat` plaintext: the link half of the configuration.
     *
     * Byte-for-byte the text the PS2 wrote on the bench (measured 2026-08-20,
     * recovered by decoding the shipped card): `dhcp`, no address, no nameserver.
     */
    private const val IFC_PLAIN = HEADER + "type nic\ndhcp\n"

    /**
     * `dev000.dat` plaintext: the device half, naming SCE's Ethernet adaptor.
     * Same provenance as [IFC_PLAIN].
     */
    private const val DEV_PLAIN =
        HEADER + "type nic\nvendor \"SCE\"\nproduct \"Ethernet (Network Adaptor)\"\nphy_config auto\n"

    /** `net000.cnf`, plain on the card: ties the interface pair together. */
    val NET_CNF: ByteArray =
        ("# <Sony Computer Entertainment Inc.>\n\n" +
            "interface \"ifc000.dat + dev000.dat\" \"ifc000.dat\" \"dev000.dat\"\n").toByteArray(Charsets.US_ASCII)

    /** The save index: slot, type, file, and the label the console shows. */
    val INDEX: ByteArray =
        ("0,1,net000.cnf,Combination1\n" +
            "1,1,ifc000.dat,Parametre 1\n" +
            "2,1,dev000.dat,SCE/Ethernet (Network Adaptor)\n").toByteArray(Charsets.US_ASCII)

    /** The encrypted link parameters, for the console identified by [consoleId]. */
    fun ifcDat(consoleId: ByteArray = ARMSX2_CONSOLE_ID): ByteArray =
        encode(IFC_PLAIN.toByteArray(Charsets.US_ASCII), consoleId)

    /** The encrypted device parameters, for the console identified by [consoleId]. */
    fun devDat(consoleId: ByteArray = ARMSX2_CONSOLE_ID): ByteArray =
        encode(DEV_PLAIN.toByteArray(Charsets.US_ASCII), consoleId)

    /**
     * The console's i.Link ID out of a `.nvm`, selected by an explicit BIOS
     * [version], or null when that exact area is absent/unprogrammed.
     */
    fun ilinkIdFromNvm(nvm: ByteArray, version: BiosVersion): ByteArray? =
        ilinkIdFromNvm(nvm, version.nvmLayout)

    /**
     * The ID ARMSX2 will effectively expose after `cdvdLoadNVRAM()` sanity checks.
     * A short NVM, a blank language block, or a blank slim-region block makes
     * ARMSX2 discard the imported contents and call `cdvdCreateNewNVM()`.
     */
    fun effectiveIlinkIdFromNvm(nvm: ByteArray, version: BiosVersion): ByteArray {
        val layout = version.nvmLayout
        if (nvm.size < NVM_SIZE || nvm.allZero(layout.config1Offset + 0x10, 16)) {
            return ARMSX2_CONSOLE_ID.copyOf()
        }
        if (version.major == 2 && version.minor != 10 && nvm.allZero(layout.regionParamsOffset, 12)) {
            return ARMSX2_CONSOLE_ID.copyOf()
        }
        return ilinkIdFromNvm(nvm, layout) ?: ARMSX2_CONSOLE_ID.copyOf()
    }

    /**
     * Layout-level form for callers that already resolved ARMSX2's NVM layout.
     * ARMSX2 treats an area whose bytes 2 and 3 are both zero as unprogrammed
     * and exposes [ARMSX2_CONSOLE_ID] instead; returning null preserves that
     * distinction for the caller.
     */
    fun ilinkIdFromNvm(nvm: ByteArray, layout: NvmLayout): ByteArray? {
        val offset = layout.ilinkIdOffset
        if (offset + 8 > nvm.size) return null
        val id = nvm.copyOfRange(offset, offset + 8)
        return id.takeIf { it[2].toInt() != 0 || it[3].toInt() != 0 }
    }

    private fun ByteArray.allZero(offset: Int, length: Int): Boolean =
        offset < 0 || offset + length > size || (offset until offset + length).all { this[it].toInt() == 0 }

    /**
     * YNCF encode: per little-endian 16-bit word `k`,
     * `rotl16(word, shifts[k % 24]) xor 0xFFFF`; an odd trailing byte gets the
     * 8-bit form. The shift table cycles with period 24 words (48 bytes), three
     * shifts per ID byte, which is why two files encrypted under one console
     * share their first 48 bytes whenever their plaintexts do.
     */
    fun encode(plain: ByteArray, consoleId: ByteArray): ByteArray {
        require(consoleId.size == 8) { "i.Link ID is 8 bytes" }
        val shifts = shifts(consoleId)
        val out = ByteArray(plain.size)
        var k = 0
        var i = 0
        while (i + 1 < plain.size) {
            val word = (plain[i].toInt() and 0xFF) or ((plain[i + 1].toInt() and 0xFF) shl 8)
            val cipher = rotl16(word, shifts[k % 24]) xor 0xFFFF
            out[i] = (cipher and 0xFF).toByte()
            out[i + 1] = ((cipher shr 8) and 0xFF).toByte()
            k++
            i += 2
        }
        if (plain.size % 2 == 1) {
            val b = plain[plain.size - 1].toInt() and 0xFF
            out[plain.size - 1] = (rotl8(b, shifts[k % 24]) xor 0xFF).toByte()
        }
        return out
    }

    /** The inverse of [encode], kept beside it so the pair is testable as one. */
    fun decode(cipher: ByteArray, consoleId: ByteArray): ByteArray {
        require(consoleId.size == 8) { "i.Link ID is 8 bytes" }
        val shifts = shifts(consoleId)
        val out = ByteArray(cipher.size)
        var k = 0
        var i = 0
        while (i + 1 < cipher.size) {
            val word = (cipher[i].toInt() and 0xFF) or ((cipher[i + 1].toInt() and 0xFF) shl 8)
            val plain = rotr16(word xor 0xFFFF, shifts[k % 24])
            out[i] = (plain and 0xFF).toByte()
            out[i + 1] = ((plain shr 8) and 0xFF).toByte()
            k++
            i += 2
        }
        if (cipher.size % 2 == 1) {
            val b = cipher[cipher.size - 1].toInt() and 0xFF
            out[cipher.size - 1] = (rotr8(b xor 0xFF, shifts[k % 24])).toByte()
        }
        return out
    }

    /**
     * The 24 shift amounts, three per ID byte:
     * `(b shr 5) + 1`, `((b shr 2) and 7) + 1`, `(b and 3) + 1` — each 1..8.
     *
     * ps2sdk's transcription initialises only seven of the eight ID bytes and
     * lets two table slots run off the end of the array; decoding the bench
     * card under the plain eight-byte table reproduces every word of both
     * encrypted files, which is the reading followed here.
     */
    internal fun shifts(consoleId: ByteArray): IntArray {
        val table = IntArray(24)
        for (i in 0 until 8) {
            val b = consoleId[i].toInt() and 0xFF
            table[3 * i] = (b shr 5) + 1
            table[3 * i + 1] = ((b shr 2) and 7) + 1
            table[3 * i + 2] = (b and 3) + 1
        }
        return table
    }

    private fun rotl16(x: Int, s: Int): Int {
        val n = s and 15
        return ((x shl n) or (x ushr (16 - n))) and 0xFFFF
    }

    private fun rotr16(x: Int, s: Int): Int {
        val n = s and 15
        return ((x ushr n) or (x shl (16 - n))) and 0xFFFF
    }

    private fun rotl8(x: Int, s: Int): Int {
        val n = s and 7
        return ((x shl n) or (x ushr (8 - n))) and 0xFF
    }

    private fun rotr8(x: Int, s: Int): Int {
        val n = s and 7
        return ((x ushr n) or (x shl (8 - n))) and 0xFF
    }

    private const val NVM_SIZE = 1024
}
