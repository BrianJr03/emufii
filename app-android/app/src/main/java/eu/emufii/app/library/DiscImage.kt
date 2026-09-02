package eu.emufii.app.library

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.FileInputStream

/**
 * Telling a GameCube or Wii disc from everything else that ends in `.iso`. Reads bytes
 * only to *promote* a file: anything not positively identified keeps what
 * [Console.forExtension] said, so this never takes a game away from PPSSPP.
 * pourquoi : docs/decisions/identite-disques.md § Reading the bytes, and only to promote
 */
object DiscImage {

    const val HEADER_BYTES = 0x80

    /** Big-endian, in the disc header. */
    private const val WII_MAGIC = 0x5D1C9EA3
    private const val WII_MAGIC_OFFSET = 0x18

    private const val GC_MAGIC = 0xC2339F3D.toInt()
    private const val GC_MAGIC_OFFSET = 0x1C

    /**
     * Where a compressed image keeps its uncompressed copy of the disc header, readable
     * without touching the compressed payload.
     * pourquoi : docs/decisions/identite-disques.md § Game identifiers, and what they are for
     */
    private const val WIA_DISC_TYPE_OFFSET = 0x48
    private const val WIA_DISC_HEADER_OFFSET = 0x58
    private const val WIA_TYPE_GAMECUBE = 1
    private const val WIA_TYPE_WII = 2

    /**
     * The ISO9660 volume descriptor: its system identifier names the console, at `0x8008`.
     * pourquoi : docs/decisions/identite-disques.md § The disc says what it is itself, at 0x8008
     */
    private const val PVD_OFFSET = 0x8000
    private const val PVD_MAGIC_OFFSET = PVD_OFFSET + 1
    private const val PVD_SYSTEM_ID_OFFSET = PVD_OFFSET + 8
    private const val PVD_VOLUME_ID_OFFSET = PVD_OFFSET + 40
    private const val PVD_ID_LENGTH = 32

    private const val PS_SYSTEM_ID = "PLAYSTATION"

    /** What a UMD rip writes in the same field, measured on the bench. */
    private const val PSP_SYSTEM_ID = "PSP GAME"

    const val PVD_BYTES = PVD_VOLUME_ID_OFFSET + PVD_ID_LENGTH

    /**
     * Null when the bytes do not say: leave the file where the extension put it.
     * pourquoi : docs/decisions/identite-disques.md § Reading the bytes, and only to promote
     */
    fun identify(head: ByteArray): Console? {
        if (head.size >= 4) {
            // Three characters, not four: the fourth byte of an RVZ or WIA magic is a
            // format version, 0x01 on every file measured here.
            val tag = String(head, 0, 3, Charsets.ISO_8859_1)
            if (tag == "RVZ" || tag == "WIA") return compressed(head)
            // WBFS only ever held Wii discs: the format was written for them.
            if (String(head, 0, 4, Charsets.ISO_8859_1) == "WBFS") return Console.WII
        }
        // The GameCube/Wii magics first: a Nintendo disc is not ISO9660, so the two
        // tests cannot fight over a file.
        return raw(head, 0) ?: playstation(head)
    }

    /**
     * A PS1 disc carries the same `PLAYSTATION`, so a `BOOT2` entry is required as well.
     * pourquoi : docs/decisions/identite-disques.md § `PLAYSTATION` is not enough: `BOOT2` is needed
     */
    private fun playstation(head: ByteArray): Console? = playstationAt(head, PVD_OFFSET)

    /** pourquoi : docs/decisions/identite-disques.md § The disc says what it is itself, at 0x8008 */
    private fun playstationAt(bytes: ByteArray, base: Int): Console? {
        if (base + PVD_ID_LENGTH * 2 + 8 > bytes.size) return null
        if (String(bytes, base + 1, 5, Charsets.ISO_8859_1) != "CD001") return null
        val system = ascii(bytes, base + 8)
        return when {
            // The byte-level claim only: a PS1 disc says the same, and is told apart by
            // the reader, which requires BOOT2 in SYSTEM.CNF.
            system.equals(PS_SYSTEM_ID, ignoreCase = true) -> Console.PS2
            system.equals(PSP_SYSTEM_ID, ignoreCase = true) -> Console.PSP
            else -> null
        }
    }

    /**
     * Where the volume descriptor sits inside one raw disc sector: 0, 16 (MODE1) or 24
     * (MODE2 FORM1), so all three must be tried.
     * pourquoi : docs/decisions/identite-disques.md § The disc says what it is itself, at 0x8008
     */
    private val SECTOR_USER_DATA_OFFSETS = intArrayOf(0, 16, 24)

    /**
     * The entry point for containers that must be decompressed. PlayStation discs only.
     * pourquoi : docs/decisions/identite-disques.md § Which extensions are worth opening
     */
    fun fromSector(sector: ByteArray): Pair<Console, String?>? {
        for (base in SECTOR_USER_DATA_OFFSETS) {
            val console = playstationAt(sector, base) ?: continue
            return console to volumeId(sector, base + 40)
        }
        return null
    }

    /**
     * The PS2 disc's real serial, read from `SYSTEM.CNF`, never the volume identifier,
     * which only two of the bench's eight discs filled in. Null on anything it cannot
     * follow, and the caller then keeps the volume identifier.
     * pourquoi : docs/decisions/identite-disques.md § The PS2 serial is in `SYSTEM.CNF`, not in the volume identifier
     */
    fun ps2Serial(reader: (Long, ByteArray) -> Int): String? = runCatching {
        val pvd = ByteArray(SECTOR)
        if (reader(PVD_OFFSET.toLong(), pvd) < SECTOR) return null
        if (String(pvd, 1, 5, Charsets.ISO_8859_1) != "CD001") return null

        // The root directory's own record is embedded in the descriptor, at a fixed
        // offset and a fixed 34 bytes long.
        val rootLba = leInt(pvd, ROOT_RECORD_OFFSET + 2)
        val rootSize = leInt(pvd, ROOT_RECORD_OFFSET + 10)
        if (rootLba <= 0 || rootSize <= 0 || rootSize > MAX_ROOT_BYTES) return null

        val dir = ByteArray(rootSize)
        if (reader(rootLba.toLong() * SECTOR, dir) < rootSize) return null

        var at = 0
        while (at < dir.size) {
            val length = dir[at].toInt() and 0xFF
            if (length == 0) {
                // A directory record never straddles a sector: the rest of this one is
                // padding, the next record starts at the top of the following sector.
                at = (at / SECTOR + 1) * SECTOR
                continue
            }
            if (at + length > dir.size) break
            val nameLength = dir[at + 32].toInt() and 0xFF
            val name = String(dir, at + 33, nameLength, Charsets.ISO_8859_1)
            // `;1` is the ISO9660 version suffix, always present on a file.
            if (name.substringBefore(';').equals("SYSTEM.CNF", ignoreCase = true)) {
                val lba = leInt(dir, at + 2)
                val size = leInt(dir, at + 10).coerceAtMost(MAX_CNF_BYTES)
                if (lba <= 0 || size <= 0) return null
                val cnf = ByteArray(size)
                if (reader(lba.toLong() * SECTOR, cnf) < size) return null
                return bootSerial(String(cnf, Charsets.ISO_8859_1))
            }
            at += length
        }
        null
    }.getOrNull()

    /**
     * `cdrom0:\SLES_537.17;1` reduced to `SLES-53717`, the way the serial is written on
     * the box and in PCSX2's index.
     * pourquoi : docs/decisions/identite-disques.md § The PS2 serial is in `SYSTEM.CNF`, not in the volume identifier
     */
    fun bootSerial(cnf: String): String? {
        val line = cnf.lineSequence().firstOrNull { it.trimStart().startsWith("BOOT2", true) }
            ?: return null
        val path = line.substringAfter('=', "").trim()
        val file = path.substringAfterLast('\\').substringAfterLast('/')
            .substringAfterLast(':').substringBefore(';')
        val serial = file.replace(".", "").replace('_', '-').uppercase().trim()
        // Shaped like a serial or nothing: a homebrew boots from an ELF with any name at
        // all, and would be filed under a key that means nothing.
        return serial.takeIf { it.matches(SERIAL_SHAPE) }
    }

    private fun leInt(bytes: ByteArray, at: Int): Int =
        (bytes[at].toInt() and 0xFF) or
            ((bytes[at + 1].toInt() and 0xFF) shl 8) or
            ((bytes[at + 2].toInt() and 0xFF) shl 16) or
            ((bytes[at + 3].toInt() and 0xFF) shl 24)

    private const val SECTOR = 2048
    /**
     * Relative to the descriptor, not absolute: an absolute offset indexes 32 kB into 2 kB.
     * pourquoi : docs/decisions/identite-disques.md § The PS2 serial is in `SYSTEM.CNF`, not in the volume identifier
     */
    private const val ROOT_RECORD_OFFSET = 156
    private const val MAX_ROOT_BYTES = 1 shl 20
    private const val MAX_CNF_BYTES = 4096
    private val SERIAL_SHAPE = Regex("^[A-Z]{4}-\\d{5}$")

    private fun volumeId(bytes: ByteArray, at: Int): String? {
        if (at + PVD_ID_LENGTH > bytes.size) return null
        return ascii(bytes, at)
            .replace('_', '-')
            .takeIf { it.isNotEmpty() && it.all { c -> c.isLetterOrDigit() || c == '-' } }
    }

    private fun ascii(head: ByteArray, at: Int): String =
        String(head, at, PVD_ID_LENGTH, Charsets.ISO_8859_1).trim { it <= ' ' }

    /** Read at [base], so the same code serves an embedded copy. */
    private fun raw(head: ByteArray, base: Int): Console? = when {
        beInt(head, base + WII_MAGIC_OFFSET) == WII_MAGIC -> Console.WII
        beInt(head, base + GC_MAGIC_OFFSET) == GC_MAGIC -> Console.GAMECUBE
        else -> null
    }

    /**
     * RVZ and WIA state their console outright; the embedded header is checked too, and
     * is not redundant: it proves the claim.
     * pourquoi : docs/decisions/identite-disques.md § Game identifiers, and what they are for
     */
    private fun compressed(head: ByteArray): Console? =
        when (beInt(head, WIA_DISC_TYPE_OFFSET)) {
            WIA_TYPE_GAMECUBE -> Console.GAMECUBE
            WIA_TYPE_WII -> Console.WII
            else -> raw(head, WIA_DISC_HEADER_OFFSET)
        }

    /**
     * The six-character game id (`RMGP01`), the identity Dolphin sorts by. Read at the
     * same base as the console: 0 raw, 0x58 inside a container.
     * pourquoi : docs/decisions/identite-disques.md § Game identifiers, and what they are for
     */
    fun gameId(head: ByteArray): String? {
        // The PS2 files its number where the disc does, not at the start.
        // pourquoi : docs/decisions/identite-disques.md § Game identifiers, and what they are for
        if (playstation(head) == Console.PS2) return volumeId(head, PVD_VOLUME_ID_OFFSET)
        val base = if (head.size >= 3 &&
            String(head, 0, 3, Charsets.ISO_8859_1).let { it == "RVZ" || it == "WIA" }
        ) WIA_DISC_HEADER_OFFSET else 0
        if (base + 6 > head.size) return null
        val id = String(head, base, 6, Charsets.ISO_8859_1)
        // Letters and digits only: anything else is compressed bytes rather than a disc
        // header, and a garbage id would be published as if it meant something.
        return id.takeIf { it.all { c -> c.isLetterOrDigit() } }
    }

    private fun beInt(bytes: ByteArray, at: Int): Int? {
        if (at < 0 || at + 4 > bytes.size) return null
        return (bytes[at].toInt() and 0xFF shl 24) or
            (bytes[at + 1].toInt() and 0xFF shl 16) or
            (bytes[at + 2].toInt() and 0xFF shl 8) or
            (bytes[at + 3].toInt() and 0xFF)
    }

    /**
     * Deliberately absent: `.gcz`, whose sub-type field there is no sample here to check.
     * pourquoi : docs/decisions/identite-disques.md § Which extensions are worth opening
     */
    val SNIFFED_EXTENSIONS = setOf("iso", "gcm", "rvz", "wia", "wbfs", "chd")

    /**
     * The extensions three consoles share at once: only the bytes settle it, and an
     * unrecognised file is not listed at all.
     * pourquoi : docs/decisions/identite-disques.md § Which extensions are worth opening
     */
    val AMBIGUOUS_EXTENSIONS = setOf("iso", "chd")
}

/**
 * Opens a candidate far enough to ask [DiscImage.identify] the question; a refused read
 * answers null, so a failure here is invisible.
 * pourquoi : docs/decisions/identite-disques.md § The cost of all this, and why it is invisible when it fails
 */
class DiscImageReader(private val context: Context) {

    data class Info(
        val console: Console,
        val gameId: String?,
        val ps2Identity: Ps2DiscIdentity? = null,
    )

    private val memory = HashMap<String, Info>()

    fun read(uri: Uri, modified: Long = 0L, size: Long = 0L): Info? {
        memory[uri.toString()]?.let {
            if (modified > 0L || size > 0L) remember(uri, modified, size, it)
            return it
        }
        if (modified > 0L || size > 0L) {
            cached(uri, modified, size)?.let {
                memory[uri.toString()] = it
                return it
            }
        }
        val head = head(uri) ?: return null
        if (isChd(head)) {
            val info = chdInfo(uri) ?: return null
            remember(uri, modified, size, info)
            return info
        }
        val console = DiscImage.identify(head) ?: return null
        // A second read, falling back to the volume identifier rather than to nothing.
        // pourquoi : docs/decisions/identite-disques.md § The PS2 serial is in `SYSTEM.CNF`, not in the volume identifier
        if (console == Console.PS2) {
            val identity = ps2Identity(uri) ?: return null
            Log.i("DiscImage", "PS2 ${identity.serial} ELF CRC ${identity.elfCrc}")
            val info = Info(console, identity.serial, identity)
            remember(uri, modified, size, info)
            return info
        }
        return Info(console, DiscImage.gameId(head)).also { memory[uri.toString()] = it }
    }

    /**
     * Walks the ISO for its boot file over a channel: reading forwards to the root
     * directory would mean reading the game. Plain images only.
     * pourquoi : docs/decisions/identite-disques.md § An ISO walk needs a channel, and does not apply to CHD
     */
    private fun ps2Identity(uri: Uri): Ps2DiscIdentity? = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { stream ->
                val channel = stream.channel
                Ps2DiscIdentityReader.read { offset, into, count ->
                    channel.position(offset)
                    var done = 0
                    while (done < count) {
                        val n = stream.read(into, done, count - done)
                        if (n <= 0) break
                        done += n
                    }
                    done
                }
            }
        }
    }.onFailure { Log.w("DiscImage", "SYSTEM.CNF illisible $uri", it) }.getOrNull()

    fun identify(uri: Uri): Console? = read(uri)?.console

    /** A CHD announces itself in its first eight bytes, whatever its name. */
    private fun isChd(head: ByteArray): Boolean =
        head.size >= 8 && String(head, 0, 8, Charsets.ISO_8859_1) == "MComprHD"

    /**
     * A CHD's hunk map sits near the end of the file: the one format that cannot be
     * answered by reading forwards.
     * pourquoi : docs/decisions/identite-disques.md § An ISO walk needs a channel, and does not apply to CHD
     */
    private fun chdInfo(uri: Uri): Info? = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { stream ->
                val channel = stream.channel
                val reader = ChdImage.open(object : ChdImage.Source {
                    override fun read(offset: Long, into: ByteArray, count: Int): Int {
                        channel.position(offset)
                        var done = 0
                        while (done < count) {
                            val n = stream.read(into, done, count - done)
                            if (n <= 0) break
                            done += n
                        }
                        return done
                    }
                }) ?: return null
                val sector = reader.readDiscSector(ChdImage.PVD_SECTOR) ?: return null
                val (console, fallbackId) = DiscImage.fromSector(sector) ?: return null
                val identity = if (console == Console.PS2) {
                    Ps2DiscIdentityReader.read(reader)
                } else null
                // Same rule as the plain ISO: a PLAYSTATION descriptor with no BOOT2 in
                // SYSTEM.CNF is a PS1 disc, and Emufii serves none.
                if (console == Console.PS2 && identity == null) return null
                identity?.let { Log.i("DiscImage", "PS2 CHD ${it.serial} ELF CRC ${it.elfCrc}") }
                Info(console, identity?.serial ?: fallbackId, identity)
            }
        }
    }.onFailure { Log.w("DiscImage", "CHD illisible $uri", it) }.getOrNull()

    private fun remember(uri: Uri, modified: Long, size: Long, info: Info) {
        memory[uri.toString()] = info
        val identity = info.ps2Identity ?: return
        if (modified <= 0L && size <= 0L) return
        context.getSharedPreferences(IDENTITY_PREFS, Context.MODE_PRIVATE).edit()
            .putString(uri.toString(), listOf(modified, size, identity.serial, identity.elfCrc).joinToString("|"))
            .apply()
    }

    private fun cached(uri: Uri, modified: Long, size: Long): Info? {
        val parts = context.getSharedPreferences(IDENTITY_PREFS, Context.MODE_PRIVATE)
            .getString(uri.toString(), null)?.split('|') ?: return null
        if (parts.size != 4 || parts[0].toLongOrNull() != modified || parts[1].toLongOrNull() != size) {
            return null
        }
        val serial = parts[2].takeIf { it.isNotBlank() } ?: return null
        val crc = parts[3].takeIf { it.matches(Regex("^[0-9A-F]{8}$")) } ?: return null
        val identity = Ps2DiscIdentity(serial, crc)
        return Info(Console.PS2, serial, identity)
    }

    /**
     * Reads as far as the volume descriptor, the PS2 being only recognisable at `0x8000`.
     * Truncated to what was read: a tail of zeroes would have `identify()` examining bytes
     * not from the file.
     * pourquoi : docs/decisions/identite-disques.md § The cost of all this, and why it is invisible when it fails
     */
    private fun head(uri: Uri): ByteArray? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArray(DiscImage.PVD_BYTES)
            var read = 0
            while (read < buffer.size) {
                val n = stream.read(buffer, read, buffer.size - read)
                if (n <= 0) break
                read += n
            }
            when {
                read < DiscImage.HEADER_BYTES -> null
                read < buffer.size -> buffer.copyOf(read)
                else -> buffer
            }
        }
    }.onFailure { Log.w("DiscImage", "cannot read $uri", it) }.getOrNull()

    private companion object {
        const val IDENTITY_PREFS = "ps2_disc_identity"
    }
}
