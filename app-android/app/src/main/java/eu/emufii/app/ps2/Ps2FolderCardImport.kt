package eu.emufii.app.ps2

/**
 * Reads the saves out of a PCSX2 folder memory card, so they can be written
 * into a card image.
 *
 * ### Why this exists
 *
 * A folder memory card cannot carry the network profile: PCSX2 indexes it
 * filtered by the running game, and `BWNETCNF` matches no game serial, so the
 * profile would be written where the console can never read it. The profile
 * therefore lives on a generated image in slot 1, and the question this answers
 * is what becomes of the player's own saves.
 *
 * Leaving the folder card in slot 2 is not the answer, and the reason is worth
 * stating precisely, because the obvious reading of the log is wrong. ARMSX2
 * opens the card before it knows what is booting:
 *
 * ```
 * McdSlot 0 [File]: EmuFii-Network.ps2 [8 MB, Formatted]
 * McdSlot 1: [Folder] /storage/emulated/0/Armsx2/memcards/MemoryCard
 * FolderMcd: Indexing slot 1 with filter "".
 * ```
 *
 * That empty filter is not a card the game cannot read — measured on the Thor
 * on 2026-08-23, the game does find its profile there. What does not work is
 * everything else the player needs: the BIOS browser shows the same card as
 * empty, so a save cannot be copied across by hand, and the two cards stay
 * split with no way to join them. Copying the saves onto the card that carries
 * the profile is what puts everything in one place the console agrees about.
 *
 * ### The layout, as ARMSX2 writes it
 *
 * ```
 * memcards/<card>/_pcsx2_superblock
 * memcards/<card>/<SAVE>/_pcsx2_index
 * memcards/<card>/<SAVE>/<the save's own files>
 * ```
 *
 * `_pcsx2_index` is PCSX2's own bookkeeping and must never be copied into a
 * card image: the console knows nothing about it, and a save carrying an extra
 * file is a save the game may refuse. What it is read for is the order of the
 * files, which is the order the console wrote them in and the order a directory
 * on a real card carries. A file the index does not mention is not dropped —
 * it goes after the ordered ones, by name — because losing a byte of somebody's
 * save to a bookkeeping mismatch is not a trade worth making.
 *
 * The file is a YAML flow mapping written by rapidyaml, not JSON, so it is read
 * with a tolerant scan rather than a parser: names carry dots and dashes, and
 * the only field that matters here is `order`.
 */
object Ps2FolderCardImport {

    const val SUPERBLOCK = "_pcsx2_superblock"
    const val INDEX = "_pcsx2_index"

    /** One save directory, its files already in the order the card should carry. */
    data class Save(val directory: String, val files: List<Pair<String, ByteArray>>)

    /**
     * `name: {` … `order: N` … `}`, one entry of the flow mapping.
     *
     * **Both braces are escaped, and the closing one is not optional.** Android
     * compiles regexes with ICU, which rejects a bare `}` outright; the JVM
     * accepts it, so a unit test on the desktop passes while the device throws
     * `PatternSyntaxException` from a static initialiser — surfacing as the save
     * import quietly reading every folder card as empty. Measured 2026-08-23.
     */
    private val ENTRY = Regex("""([^,{}\s][^,{}:]*)\s*:\s*\{([^}]*)\}""")
    private val ORDER = Regex("""\border\s*:\s*(\d+)""")

    /**
     * Puts [files] in the order [indexText] gives, dropping [INDEX] itself.
     *
     * Pure on purpose: the ordering is the part worth a unit test, and it does
     * not need a device to be proven. A null or unreadable index is not an
     * error, it just means falling back to a stable order by name.
     */
    fun order(indexText: String?, files: Map<String, ByteArray>): List<Pair<String, ByteArray>> {
        val payload = files.filterKeys { it != INDEX }
        val ranks = mutableMapOf<String, Int>()
        if (indexText != null) {
            for (match in ENTRY.findAll(indexText)) {
                val name = match.groupValues[1].trim()
                if (name == "\$ROOT") continue
                ORDER.find(match.groupValues[2])?.groupValues?.get(1)?.toIntOrNull()
                    ?.let { ranks[name] = it }
            }
        }
        // Ranked files first, in their rank; then whatever the index forgot,
        // by name, so two runs on the same card always produce the same card.
        val ranked = payload.keys.filter { it in ranks }.sortedBy { ranks.getValue(it) }
        val rest = payload.keys.filter { it !in ranks }.sorted()
        return (ranked + rest).map { it to payload.getValue(it) }
    }
}
