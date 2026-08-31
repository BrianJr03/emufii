package eu.emufii.app.library

/**
 * What a dump says about itself: which region it is. Nothing here calls
 * anything: a handheld in a train has to be able to answer.
 *
 * The serial first, then the filename tags. Null means unknown, and unknown is
 * printed as nothing at all.
 * pourquoi : docs/decisions/identite-et-dumps.md § Nothing calls the network
 */
data class RomTags(
    /** `USA`, `Europe`, `Japan`… as it will be shown, already tidy. */
    val region: String? = null,
) {
    val isEmpty: Boolean get() = region == null

    /** What the panel prints, or nothing at all. */
    fun line(): String? = region
}

/*
 * The revision was here and was taken out: a filename can only
 * yield facts about a pressing plant, never the version an update installs.
 * pourquoi : docs/decisions/identite-et-dumps.md § The revision was removed, and why
 */

object RomTagReader {

    fun read(rom: Rom): RomTags =
        read(rom.console, rom.productCode, rom.titleIdHex, rom.filename)

    /**
     * The same reading without a [Rom], so the rules can be pinned by a unit
     * test: `android.net.Uri` is a stub on the desktop JVM.
     * pourquoi : docs/decisions/identite-et-dumps.md § Region positions are repeated, not shared
     */
    fun read(
        console: Console,
        productCode: String?,
        titleIdHex: String?,
        filename: String,
    ): RomTags = RomTags(
        region = regionFromId(console, productCode, titleIdHex) ?: regionFromName(filename),
    )

    /**
     * The region letter, at the positions `compatKeys` also uses: repeated
     * rather than shared, because that one strips what this one keeps.
     * pourquoi : docs/decisions/identite-et-dumps.md § Region positions are repeated, not shared
     */
    private fun regionFromId(console: Console, productCode: String?, titleIdHex: String?): String? {
        val code = productCode?.trim()?.uppercase() ?: return null
        return when (console) {
            // `CTR-P-ARRJ`, last of the four.
            Console.THREE_DS -> code.takeLast(4).takeIf { it.length == 4 }?.last()?.let(::threeDsRegion)

            // `NDS-ADAE-01`, last of the game code.
            Console.DS -> code.split('-').getOrNull(1)?.takeIf { it.length == 4 }?.last()
                ?.let(::nintendoRegion)

            // `RMCP01`, fourth character.
            Console.GAMECUBE, Console.WII ->
                code.takeIf { it.length == 6 }?.get(3)?.let(::nintendoRegion)

            // A PSP or PS2 serial encodes the region in its prefix, not in a
            // position: `SLUS` is American, `SLES` European, `SLPS` Japanese.
            Console.PSP, Console.PS2 -> sonyRegion(code)

            // One title id worldwide. There is nothing to read, and saying so is
            // the correct answer rather than a gap.
            Console.SWITCH -> null
        }
    }

    private fun nintendoRegion(letter: Char): String? = when (letter) {
        'E' -> "USA"
        'P' -> "Europe"
        'J' -> "Japan"
        'K' -> "Korea"
        'U' -> "Australia"
        'F' -> "France"
        'D' -> "Germany"
        'S' -> "Spain"
        'I' -> "Italy"
        'H' -> "Netherlands"
        else -> null
    }

    /** The 3DS uses the same letters, plus `A` for the region-free releases. */
    private fun threeDsRegion(letter: Char): String? =
        if (letter == 'A') "World" else nintendoRegion(letter)

    /**
     * The four-letter Sony prefix, read letter by letter: medium, publisher,
     * region. A list of whole prefixes missed every PSP game in the world.
     * pourquoi : docs/decisions/identite-et-dumps.md § The Sony prefix is read letter by letter
     */
    private fun sonyRegion(code: String): String? {
        val prefix = code.filter { it.isLetter() }.take(4)
        if (prefix.length < 3) return null
        if (prefix[0] != 'S' && prefix[0] != 'U') return null
        return when (prefix[2]) {
            'U' -> "USA"
            'E' -> "Europe"
            // `SLPS`, `SLPM`, `ULJM`, `ULJS`: Japan spells itself two ways
            // depending on the medium, and both are the third letter.
            'P', 'J' -> "Japan"
            'K' -> "Korea"
            'A' -> "Asia"
            else -> null
        }
    }

    /**
     * The region as the dumper wrote it. Only the two conventions' actual
     * spellings: a looser match turns `(Disney's Aladdin)` into a region.
     * pourquoi : docs/decisions/identite-et-dumps.md § Nothing calls the network
     */
    private fun regionFromName(filename: String): String? {
        val tags = tagsIn(filename)
        for (tag in tags) {
            NAME_REGIONS[tag.uppercase()]?.let { return it }
            // `(USA, Europe)` and `(Japan, Asia)` are common; the first named
            // region is the one shown, and the rest would not fit on one line.
            val first = tag.split(',').firstOrNull()?.trim()?.uppercase()
            first?.let { NAME_REGIONS[it]?.let { region -> return region } }
        }
        return null
    }


    /** Everything between round or square brackets, in order. */
    private fun tagsIn(filename: String): List<String> =
        TAG.findAll(filename.substringBeforeLast('.')).map { it.groupValues[1].ifEmpty { it.groupValues[2] } }.toList()

    // Escaped completely, braces included: Android compiles its regexes with
    // ICU, which is stricter than the JVM and throws on a brace the desktop
    // tests happily accept. See CLAUDE.md.
    private val TAG = Regex("""\(([^()]*)\)|\[([^\[\]]*)\]""")

    private val NAME_REGIONS = mapOf(
        "USA" to "USA",
        "US" to "USA",
        "EUROPE" to "Europe",
        "EUR" to "Europe",
        "EU" to "Europe",
        "JAPAN" to "Japan",
        "JPN" to "Japan",
        "JP" to "Japan",
        "WORLD" to "World",
        "KOREA" to "Korea",
        "CHINA" to "China",
        "TAIWAN" to "Taiwan",
        "AUSTRALIA" to "Australia",
        "FRANCE" to "France",
        "GERMANY" to "Germany",
        "SPAIN" to "Spain",
        "ITALY" to "Italy",
        "NETHERLANDS" to "Netherlands",
        "SWEDEN" to "Sweden",
        "BRAZIL" to "Brazil",
        "CANADA" to "Canada",
        "ASIA" to "Asia",
    )
}
