package eu.emufii.app.ps2

/**
 * Reads the saves out of a PCSX2 folder memory card, so they can be written
 * into a card image.
 *
 * `_pcsx2_index` se lit pour l'ordre des fichiers et ne se copie jamais.
 * pourquoi : docs/decisions/ps2-carte-memoire.md § Les sauvegardes du dossier viennent sur l'image, pas l'inverse
 * pourquoi : docs/decisions/ps2-carte-memoire.md § `_pcsx2_index` se lit, ne se copie jamais
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
