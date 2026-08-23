package eu.emufii.app.library

import io.airlift.compress.zstd.ZstdDecompressor
import java.io.ByteArrayInputStream
import java.util.zip.Inflater
import org.tukaani.xz.LZMAInputStream

/**
 * Reading just enough of a CHD to know which console pressed the disc.
 *
 * `.chd` is the one container where the extension settles nothing: the PSP, the
 * PS2 and the Dreamcast all ship in it, and on this machine two of the three are
 * sitting in neighbouring folders. Unlike an `.iso`, the bytes that would answer
 * the question are compressed, so nothing can be read straight off the front of
 * the file. This decodes the container far enough to hand back one disc sector,
 * and no further: no full extraction, no temporary file, a few hundred kilobytes
 * read per candidate.
 *
 * Everything below was measured on two real files, never taken from a wiki:
 * a Dreamcast `Phantasy Star Online Ver. 2` and a PS2 `Unreal Tournament`.
 * Both are v5, both `cdlz/cdzl/cdfl`, both `hunkbytes 19584` over
 * `unitbytes 2448`. The PS2 one yields, at sector 16 offset 24, exactly:
 *
 * ```
 * CD001   system id 'PLAYSTATION'   volume id 'UT'
 * ```
 *
 * which is the same descriptor [DiscImage] already reads on a plain `.iso`, and
 * the reason this file stops at the sector rather than deciding anything: the
 * console is settled in one place for every disc format, not two.
 */
object ChdImage {

    private const val MAGIC = "MComprHD"
    private const val VERSION_5 = 5
    private const val HEADER_V5_BYTES = 124

    // Header fields, at the offsets the format fixes.
    private const val OFF_VERSION = 12
    private const val OFF_COMPRESSORS = 16
    private const val OFF_LOGICAL_BYTES = 32
    private const val OFF_MAP_OFFSET = 40
    private const val OFF_META_OFFSET = 48
    private const val OFF_HUNK_BYTES = 56
    private const val OFF_UNIT_BYTES = 60

    /** A raw CD frame: 2352 bytes of sector, then 96 of subcode. */
    private const val CD_FRAME_BYTES = 2448
    private const val CD_SECTOR_BYTES = 2352

    /** The ISO9660 volume descriptor lives in sector 16, on every disc. */
    const val PVD_SECTOR = 16

    /**
     * Metadata tags, read from the chain that starts right after the header.
     *
     * `CHGD`/`CHGT` are the GD-ROM ones, and they are the whole reason this
     * check exists: a Dreamcast disc is the false positive to avoid, it is
     * `unitbytes 2448` exactly like a PS2 CD, and only the tag tells them apart.
     * Measured: the Dreamcast file carries `CHGD "TRACK:1 TYPE:MODE1_RAW …"`
     * where the PS2 one carries `CHT2 "TRACK:1 TYPE:MODE2_RAW …"`.
     */
    private const val TAG_GDROM_TRACK = "CHGD"
    private const val TAG_GDROM_OLD = "CHGT"

    /** Hunk compression types, as the v5 map encodes them. */
    private const val TYPE_BASE_0 = 0
    private const val TYPE_NONE = 4
    private const val TYPE_SELF = 5
    private const val TYPE_PARENT = 6
    private const val TYPE_RLE_SMALL = 7
    private const val TYPE_RLE_LARGE = 8
    private const val TYPE_SELF_0 = 9
    private const val TYPE_SELF_1 = 10

    /** Where the bytes come from, so tests need no Android and no provider. */
    interface Source {
        /** Fills [into] from [offset]; returns how much was actually read. */
        fun read(offset: Long, into: ByteArray, count: Int): Int
    }

    /**
     * One disc sector, or null when this file cannot answer.
     *
     * Null is the ordinary answer for a GD-ROM, for a codec we do not decode,
     * for a CHD older than v5 and for anything truncated. Every one of them
     * means the same thing to the caller, and it is never "this is not a PS2":
     * it is "the bytes did not say", which leaves the file exactly where its
     * extension put it.
     */
    fun readSector(source: Source, sectorIndex: Int = PVD_SECTOR): ByteArray? =
        open(source)?.readDiscSector(sectorIndex)

    /**
     * Opens one v5 CHD as a reusable ISO9660 reader.
     *
     * The old one-sector path decoded the Huffman map again for every seek. A
     * boot ELF spans hundreds of seeks in a DVD CHD, so that approach turns a
     * few megabytes into gigabytes of repeated map work. This parses the map
     * once and keeps only the most recent decoded hunk, which is enough for the
     * sequential reads performed by [Ps2DiscIdentityReader].
     */
    fun open(source: Source): Reader? {
        val header = ByteArray(HEADER_V5_BYTES)
        if (source.read(0, header, header.size) < header.size) return null
        if (String(header, 0, 8, Charsets.ISO_8859_1) != MAGIC) return null
        if (beInt(header, OFF_VERSION) != VERSION_5) return null

        val hunkBytes = beInt(header, OFF_HUNK_BYTES)
        val unitBytes = beInt(header, OFF_UNIT_BYTES)
        if (hunkBytes <= 0 || unitBytes <= 0) return null
        val logicalBytes = beLong(header, OFF_LOGICAL_BYTES)
        val mapOffset = beLong(header, OFF_MAP_OFFSET)
        val metaOffset = beLong(header, OFF_META_OFFSET)

        if (isGdRom(source, metaOffset)) return null

        val compressors = (0 until 4).map {
            String(header, OFF_COMPRESSORS + 4 * it, 4, Charsets.ISO_8859_1)
        }
        val rawCd = unitBytes == CD_FRAME_BYTES
        val totalHunks = ((logicalBytes + hunkBytes - 1) / hunkBytes).toInt()
        if (totalHunks <= 0 || totalHunks > MAX_HUNKS) return null
        val entries = mapEntries(source, mapOffset, totalHunks, hunkBytes) ?: return null
        return Reader(source, compressors, entries, logicalBytes, hunkBytes, unitBytes, rawCd)
    }

    /** A bounded number well beyond a dual-layer DVD with 4 KiB hunks. */
    private const val MAX_HUNKS = 4 * 1024 * 1024

    /**
     * The Dreamcast, excluded before a single byte is decompressed.
     *
     * The metadata chain sits at `0x7c` on both files measured, that is, right
     * behind the header, so this costs one short read and settles the console
     * this project must never claim.
     */
    private fun isGdRom(source: Source, metaOffset: Long): Boolean {
        var offset = metaOffset
        var seen = 0
        val entry = ByteArray(16)
        while (offset > 0 && seen < MAX_METADATA_ENTRIES) {
            if (source.read(offset, entry, entry.size) < entry.size) return false
            val tag = String(entry, 0, 4, Charsets.ISO_8859_1)
            if (tag == TAG_GDROM_TRACK || tag == TAG_GDROM_OLD) return true
            offset = beLong(entry, 8)
            seen++
        }
        return false
    }

    /** Enough to walk a disc's tracks; a bound, so a cyclic chain cannot hang. */
    private const val MAX_METADATA_ENTRIES = 64

    internal data class Entry(val type: Int, val offset: Long, val length: Int)
    internal class Entries(
        private val types: ByteArray,
        private val offsets: LongArray,
        private val lengths: IntArray,
    ) {
        val size: Int get() = types.size
        fun getOrNull(index: Int): Entry? = if (index in types.indices) {
            Entry(types[index].toInt() and 0xFF, offsets[index], lengths[index])
        } else null
    }

    /**
     * The map entry for one hunk, decoded from a Huffman-compressed stream.
     *
     * Two full passes, and the first one cannot be cut short: the types of
     * *every* hunk are decoded before the first length is written, so stopping
     * at the hunk we want reads lengths out of the middle of the type stream and
     * yields offsets that look plausible and decompress to nothing. That mistake
     * cost an afternoon; the loop below runs to [totalHunks] deliberately.
     */
    private fun mapEntries(
        source: Source,
        mapOffset: Long,
        totalHunks: Int,
        hunkBytes: Int
    ): Entries? {
        val head = ByteArray(16)
        if (source.read(mapOffset, head, head.size) < head.size) return null
        val mapBytes = beInt(head, 0)
        if (mapBytes <= 0 || mapBytes > MAX_MAP_BYTES) return null
        val firstOffset = be48(head, 4)
        val lengthBits = head[12].toInt() and 0xFF
        val hunkBits = head[13].toInt() and 0xFF
        val parentBits = head[14].toInt() and 0xFF

        val compressed = ByteArray(mapBytes)
        if (source.read(mapOffset + 16, compressed, mapBytes) < mapBytes) return null
        val bits = BitReader(compressed)
        val huff = Huffman(numCodes = 16, maxBits = 8)
        if (!huff.importTreeRle(bits)) return null

        val types = ByteArray(totalHunks)
        var last = 0
        var repeat = 0
        for (i in 0 until totalHunks) {
            if (repeat > 0) {
                types[i] = last.toByte()
                repeat--
                continue
            }
            when (val value = huff.decodeOne(bits) ?: return null) {
                TYPE_RLE_SMALL -> {
                    types[i] = last.toByte()
                    repeat = 2 + (huff.decodeOne(bits) ?: return null)
                }
                TYPE_RLE_LARGE -> {
                    types[i] = last.toByte()
                    val high = huff.decodeOne(bits) ?: return null
                    val low = huff.decodeOne(bits) ?: return null
                    repeat = 2 + 16 + (high shl 4) + low
                }
                else -> {
                    last = value
                    types[i] = value.toByte()
                }
            }
        }

        val offsets = LongArray(totalHunks)
        val lengths = IntArray(totalHunks)
        var current = firstOffset
        var lastSelf = 0L
        for (i in 0 until totalHunks) {
            val type = types[i].toInt()
            var offset = current
            var length = 0
            when (type) {
                TYPE_BASE_0, 1, 2, 3 -> {
                    length = bits.read(lengthBits) ?: return null
                    current += length
                    bits.read(16) ?: return null
                }
                TYPE_NONE -> {
                    length = hunkBytes
                    current += hunkBytes
                    bits.read(16) ?: return null
                }
                TYPE_SELF -> {
                    offset = (bits.read(hunkBits) ?: return null).toLong()
                    lastSelf = offset
                }
                TYPE_PARENT -> {
                    bits.read(parentBits) ?: return null
                }
                TYPE_SELF_0 -> offset = lastSelf
                TYPE_SELF_1 -> {
                    lastSelf++
                    offset = lastSelf
                }
                // Parent references remain in the table. A standalone reader
                // cannot resolve them, but another hunk may still be readable;
                // refusing the entire CHD here would throw useful data away.
                else -> Unit
            }
            offsets[i] = offset
            lengths[i] = length
        }
        return Entries(types, offsets, lengths)
    }

    /** A bound on the map: 64 MB covers a dual-layer disc many times over. */
    private const val MAX_MAP_BYTES = 64 * 1024 * 1024

    /**
     * The bytes of one hunk, decompressed.
     *
     * For a raw CD the payload returned is the sectors only: the codec keeps
     * sector data and subcode in two separately compressed blocks, and the
     * subcode carries nothing that names a console.
     */
    private fun hunkPayload(
        source: Source,
        entries: Entries,
        index: Int,
        entry: Entry,
        compressors: List<String>,
        hunkBytes: Int,
        rawCd: Boolean,
        depth: Int = 0,
    ): ByteArray? {
        if (depth > MAX_SELF_DEPTH) return null
        if (entry.type == TYPE_SELF || entry.type == TYPE_SELF_0 || entry.type == TYPE_SELF_1) {
            val referenced = entry.offset.toInt()
            if (referenced !in 0 until entries.size || referenced == index) return null
            return hunkPayload(
                source,
                entries,
                referenced,
                entries.getOrNull(referenced) ?: return null,
                compressors,
                hunkBytes,
                rawCd,
                depth + 1,
            )
        }
        if (entry.type == TYPE_PARENT) return null

        val storedLength = if (entry.type == TYPE_NONE) hunkBytes else entry.length
        if (storedLength <= 0 || storedLength > MAX_HUNK_BYTES) return null
        val raw = ByteArray(storedLength)
        if (source.read(entry.offset, raw, storedLength) < storedLength) return null

        val frames = hunkBytes / CD_FRAME_BYTES
        val decoded = if (entry.type == TYPE_NONE) raw else when (val codec = compressors.getOrNull(entry.type)) {
            "cdlz", "cdzl" -> {
                if (!rawCd || frames <= 0) return null
                // The CD codecs put a header first: one ECC bit per frame,
                // rounded up to bytes, then the compressed length of the sector
                // block. The subcode block follows it and is of no use here.
                val eccBytes = (frames + 7) / 8
                val lengthBytes = if (hunkBytes < 65536) 2 else 3
                val headerBytes = eccBytes + lengthBytes
                if (raw.size <= headerBytes) return null
                var complen = ((raw[eccBytes].toInt() and 0xFF) shl 8) or
                    (raw[eccBytes + 1].toInt() and 0xFF)
                if (lengthBytes > 2) {
                    complen = (complen shl 8) or (raw[eccBytes + 2].toInt() and 0xFF)
                }
                if (complen <= 0 || headerBytes + complen > raw.size) return null
                val out = frames * CD_SECTOR_BYTES
                if (codec == "cdzl") inflate(raw, headerBytes, complen, out)
                else lzma(raw, headerBytes, complen, out)
            }
            "zlib" -> inflate(raw, 0, raw.size, hunkBytes)
            "lzma" -> lzma(raw, 0, raw.size, hunkBytes)
            "zstd" -> zstd(raw, hunkBytes)
            // DVD CHDs commonly keep one canonical all-zero hunk in FLAC and
            // SELF-reference it throughout sparse files. Decoding arbitrary
            // audio is unnecessary for an ELF reader; recognising FLAC's
            // constant-zero subframes is enough to resolve those references,
            // and anything else cleanly falls back instead of being guessed.
            "flac" -> flacSilence(raw, hunkBytes)
            // `cdfl` is FLAC, which only ever holds CD audio, and `huff` is
            // CHD's own Huffman codec. Neither has been seen on a data sector,
            // so an image using one for its ELF falls back to the accessibility
            // path instead of producing a guessed CRC.
            else -> null
        }
        decoded ?: return null
        return if (rawCd && decoded.size == hunkBytes) stripSubcode(decoded, frames) else decoded
    }

    private const val MAX_SELF_DEPTH = 64

    /** TYPE_NONE stores complete 2448-byte frames; expose only their 2352-byte sectors. */
    private fun stripSubcode(framesBytes: ByteArray, frames: Int): ByteArray? {
        if (frames <= 0 || frames * CD_FRAME_BYTES > framesBytes.size) return null
        return ByteArray(frames * CD_SECTOR_BYTES).also { out ->
            for (frame in 0 until frames) {
                framesBytes.copyInto(
                    out,
                    frame * CD_SECTOR_BYTES,
                    frame * CD_FRAME_BYTES,
                    frame * CD_FRAME_BYTES + CD_SECTOR_BYTES,
                )
            }
        }
    }

    /** Random-access ISO view backed by one parsed CHD map. */
    class Reader internal constructor(
        private val source: Source,
        private val compressors: List<String>,
        private val entries: Entries,
        private val logicalBytes: Long,
        private val hunkBytes: Int,
        private val unitBytes: Int,
        private val rawCd: Boolean,
    ) : Ps2DiscIdentityReader.Reader {
        private var cachedIndex = -1
        private var cachedHunk: ByteArray? = null
        private var isoUserOffset: Int? = null

        /** The complete disc sector, useful for the cheap console sniff. */
        fun readDiscSector(sectorIndex: Int): ByteArray? {
            if (sectorIndex < 0) return null
            if (!rawCd) {
                if (unitBytes <= 0) return null
                val bytes = ByteArray(unitBytes)
                return bytes.takeIf { readFlat(sectorIndex.toLong() * unitBytes, it, it.size) == it.size }
            }
            val logicalOffset = sectorIndex.toLong() * unitBytes
            if (logicalOffset + unitBytes > logicalBytes) return null
            val hunkIndex = (logicalOffset / hunkBytes).toInt()
            val within = (logicalOffset % hunkBytes).toInt()
            val frame = within / CD_FRAME_BYTES
            val hunk = hunk(hunkIndex) ?: return null
            val at = frame * CD_SECTOR_BYTES
            if (at + CD_SECTOR_BYTES > hunk.size) return null
            return hunk.copyOfRange(at, at + CD_SECTOR_BYTES)
        }

        /**
         * Reads the 2048-byte ISO user-data stream, hiding raw CD headers and
         * subcode. PS2 CDs on the bench are MODE2 (offset 24); MODE1 (16) and
         * already-cooked sectors are detected as well from the PVD signature.
         */
        override fun read(offset: Long, into: ByteArray, count: Int): Int {
            if (offset < 0 || count < 0 || count > into.size) return 0
            if (!rawCd) return readFlat(offset, into, count)
            val userOffset = isoUserOffset ?: detectIsoUserOffset()?.also { isoUserOffset = it }
                ?: return 0
            var done = 0
            while (done < count) {
                val isoAt = offset + done
                val sectorIndex = isoAt / ISO_SECTOR_BYTES
                val within = (isoAt % ISO_SECTOR_BYTES).toInt()
                val sector = readDiscSector(sectorIndex.toInt()) ?: break
                val copied = minOf(count - done, ISO_SECTOR_BYTES - within)
                if (userOffset + within + copied > sector.size) break
                sector.copyInto(into, done, userOffset + within, userOffset + within + copied)
                done += copied
            }
            return done
        }

        private fun detectIsoUserOffset(): Int? {
            val pvd = readDiscSector(PVD_SECTOR) ?: return null
            return intArrayOf(0, 16, 24).firstOrNull { at ->
                at + 6 <= pvd.size && String(pvd, at + 1, 5, Charsets.ISO_8859_1) == "CD001"
            }
        }

        private fun readFlat(offset: Long, into: ByteArray, count: Int): Int {
            if (offset < 0 || offset >= logicalBytes) return 0
            var done = 0
            val allowed = minOf(count.toLong(), logicalBytes - offset).toInt()
            while (done < allowed) {
                val at = offset + done
                val index = (at / hunkBytes).toInt()
                val within = (at % hunkBytes).toInt()
                val payload = hunk(index) ?: break
                val copied = minOf(allowed - done, payload.size - within)
                if (copied <= 0) break
                payload.copyInto(into, done, within, within + copied)
                done += copied
            }
            return done
        }

        private fun hunk(index: Int): ByteArray? {
            if (index == cachedIndex) return cachedHunk
            val entry = entries.getOrNull(index) ?: return null
            val decoded = hunkPayload(
                source,
                entries,
                index,
                entry,
                compressors,
                hunkBytes,
                rawCd,
            )
            cachedIndex = index
            cachedHunk = decoded
            return decoded
        }

    }

    private const val ISO_SECTOR_BYTES = 2048

    /** A hunk is bounded by the format itself; this guards a corrupt length. */
    private const val MAX_HUNK_BYTES = 4 * 1024 * 1024

    private fun inflate(src: ByteArray, at: Int, count: Int, outSize: Int): ByteArray? =
        runCatching {
            val inflater = Inflater(true)
            try {
                inflater.setInput(src, at, count)
                val out = ByteArray(outSize)
                var done = 0
                while (done < outSize) {
                    val n = inflater.inflate(out, done, outSize - done)
                    if (n == 0) break
                    done += n
                }
                if (done == outSize) out else null
            } finally {
                inflater.end()
            }
        }.getOrNull()

    private fun zstd(src: ByteArray, outSize: Int): ByteArray? = runCatching {
        val out = ByteArray(outSize)
        val written = ZstdDecompressor().decompress(src, 0, src.size, out, 0, out.size)
        if (written == outSize) out else null
    }.getOrNull()

    /** Decodes a CHD FLAC frame only when every channel is the constant zero. */
    internal fun flacSilence(src: ByteArray, outSize: Int): ByteArray? {
        // CHD prefixes the headerless frame with output endianness. Zero is the
        // same in either order, but an unknown marker is not a frame we own.
        if (src.size < 10 || (src[0].toInt() != 'L'.code && src[0].toInt() != 'B'.code)) return null
        if (src[1].toInt() and 0xFF != 0xFF || src[2].toInt() and 0xFE != 0xF8) return null
        val blockCode = (src[3].toInt() ushr 4) and 0x0F
        val rateCode = src[3].toInt() and 0x0F
        val assignment = (src[4].toInt() ushr 4) and 0x0F
        val channels = if (assignment <= 7) assignment + 1 else if (assignment <= 10) 2 else return null

        var at = 5
        val firstNumber = src.getOrNull(at)?.toInt()?.and(0xFF) ?: return null
        val numberBytes = when {
            firstNumber and 0x80 == 0 -> 1
            firstNumber and 0xE0 == 0xC0 -> 2
            firstNumber and 0xF0 == 0xE0 -> 3
            firstNumber and 0xF8 == 0xF0 -> 4
            firstNumber and 0xFC == 0xF8 -> 5
            firstNumber and 0xFE == 0xFC -> 6
            firstNumber == 0xFE -> 7
            else -> return null
        }
        at += numberBytes
        at += when (blockCode) { 6 -> 1; 7 -> 2; else -> 0 }
        at += when (rateCode) { 12 -> 1; 13, 14 -> 2; else -> 0 }
        at++ // header CRC-8
        if (at >= src.size - 2) return null

        val bits = BitReader(src.copyOfRange(at, src.size - 2)) // frame CRC-16 is last
        repeat(channels) {
            if (bits.read(1) != 0 || bits.read(6) != 0) return null // constant subframe
            val wasted = bits.read(1) ?: return null
            var sampleBits = 16
            if (wasted != 0) {
                var wastedBits = 1
                while (bits.read(1) == 0) wastedBits++
                sampleBits -= wastedBits
            }
            if (sampleBits <= 0 || bits.read(sampleBits) != 0) return null
        }
        return ByteArray(outSize)
    }

    /**
     * Raw LZMA, with the properties CHD leaves implicit.
     *
     * The stream carries no header: MAME compresses with `lc=3, lp=0, pb=2`,
     * which packs into the single properties byte 0x5D, and a dictionary
     * normalised up to the next power of two. Verified against the real PS2
     * file, where the sector block decodes to exactly 18816 bytes.
     */
    private fun lzma(src: ByteArray, at: Int, count: Int, outSize: Int): ByteArray? =
        runCatching {
            val stream = LZMAInputStream(
                ByteArrayInputStream(src, at, count),
                outSize.toLong(),
                LZMA_PROPS_BYTE,
                dictionarySizeFor(outSize)
            )
            stream.use {
                val out = ByteArray(outSize)
                var done = 0
                while (done < outSize) {
                    val n = it.read(out, done, outSize - done)
                    if (n <= 0) break
                    done += n
                }
                if (done == outSize) out else null
            }
        }.getOrNull()

    private const val LZMA_PROPS_BYTE: Byte = 0x5D

    private fun dictionarySizeFor(size: Int): Int {
        var dict = 4096
        while (dict < size && dict < (1 shl 26)) dict = dict shl 1
        return dict
    }

    /** MSB-first bit reader; the format's streams are all big-endian. */
    private class BitReader(private val data: ByteArray) {
        private var position = 0L

        fun read(count: Int): Int? {
            if (count == 0) return 0
            if (count < 0 || count > 32) return null
            var value = 0
            repeat(count) {
                val index = (position ushr 3).toInt()
                if (index >= data.size) return null
                val bit = (data[index].toInt() ushr (7 - (position and 7).toInt())) and 1
                value = (value shl 1) or bit
                position++
            }
            return value
        }
    }

    /**
     * MAME's canonical Huffman, with its run-length coded tree.
     *
     * Two details here are not guessable and were taken from `huffman.cpp`
     * rather than reconstructed: the repeat count comes from a *third* read of
     * the stream, and what gets repeated is the length just read, not zero.
     * Getting either wrong still produces a tree, just not one whose code
     * lengths sum to 1 — which is why [importTreeRle] checks exactly that
     * before returning, and refuses the file otherwise.
     */
    private class Huffman(private val numCodes: Int, private val maxBits: Int) {
        private val lengths = IntArray(numCodes)
        private val codes = HashMap<Int, Int>()

        fun importTreeRle(bits: BitReader): Boolean {
            val numBits = if (maxBits >= 16) 5 else if (maxBits >= 8) 4 else 3
            var current = 0
            while (current < numCodes) {
                val nodeBits = bits.read(numBits) ?: return false
                if (nodeBits != 1) {
                    lengths[current++] = nodeBits
                } else {
                    val escaped = bits.read(numBits) ?: return false
                    if (escaped == 1) {
                        lengths[current++] = 1
                    } else {
                        var repeat = (bits.read(numBits) ?: return false) + 3
                        while (repeat > 0 && current < numCodes) {
                            lengths[current++] = escaped
                            repeat--
                        }
                    }
                }
            }
            return assignCanonicalCodes()
        }

        private fun assignCanonicalCodes(): Boolean {
            val histogram = IntArray(33)
            for (length in lengths) {
                if (length > maxBits) return false
                histogram[length]++
            }
            var start = 0
            for (length in 32 downTo 1) {
                val next = (start + histogram[length]) shr 1
                if (length != 1 && next * 2 != start + histogram[length]) return false
                histogram[length] = start
                start = next
            }
            codes.clear()
            for (symbol in 0 until numCodes) {
                val length = lengths[symbol]
                if (length > 0) {
                    codes[key(length, histogram[length])] = symbol
                    histogram[length]++
                }
            }
            return true
        }

        fun decodeOne(bits: BitReader): Int? {
            var code = 0
            for (length in 1..maxBits) {
                code = (code shl 1) or (bits.read(1) ?: return null)
                codes[key(length, code)]?.let { return it }
            }
            return null
        }

        private fun key(length: Int, code: Int) = (length shl 24) or code
    }

    private fun beInt(bytes: ByteArray, at: Int): Int =
        (bytes[at].toInt() and 0xFF shl 24) or
            (bytes[at + 1].toInt() and 0xFF shl 16) or
            (bytes[at + 2].toInt() and 0xFF shl 8) or
            (bytes[at + 3].toInt() and 0xFF)

    private fun beLong(bytes: ByteArray, at: Int): Long {
        var value = 0L
        for (i in 0 until 8) value = (value shl 8) or (bytes[at + i].toLong() and 0xFF)
        return value
    }

    private fun be48(bytes: ByteArray, at: Int): Long {
        var value = 0L
        for (i in 0 until 6) value = (value shl 8) or (bytes[at + i].toLong() and 0xFF)
        return value
    }
}
