package eu.emufii.app.ps2

/**
 * Reads the saves out of a PCSX2 folder memory card, to be written into a card
 * image. `_pcsx2_index` is read for file order and never copied.
 * pourquoi : docs/decisions/ps2-carte-memoire.md § The folder's saves come onto the image, not the other way round
 * pourquoi : docs/decisions/ps2-carte-memoire.md § `_pcsx2_index` is read, never copied
 */
object Ps2FolderCardImport {

    const val SUPERBLOCK = "_pcsx2_superblock"
    const val INDEX = "_pcsx2_index"

    /** One save directory, its files already in the order the card should carry. */
    data class Save(val directory: String, val files: List<Pair<String, ByteArray>>)

    /**
     * One entry of the flow mapping. Escape both braces: Android compiles regexes
     * with ICU, which rejects a bare `}` that the JVM accepts, so the desktop test
     * passes while the device throws `PatternSyntaxException` from a static
     * initialiser and every folder card reads as empty.
     */
    private val ENTRY = Regex("""([^,{}\s][^,{}:]*)\s*:\s*\{([^}]*)\}""")
    private val ORDER = Regex("""\border\s*:\s*(\d+)""")

    /**
     * Puts [files] in the order [indexText] gives, dropping [INDEX] itself. A
     * null or unreadable index falls back to a stable order by name.
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
        // Ranked first, then whatever the index forgot, by name, so two runs on
        // the same card produce the same card.
        val ranked = payload.keys.filter { it in ranks }.sortedBy { ranks.getValue(it) }
        val rest = payload.keys.filter { it !in ranks }.sorted()
        return (ranked + rest).map { it to payload.getValue(it) }
    }
}
