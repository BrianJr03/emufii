package eu.emufii.app.library

/**
 * The keys a ROM is looked up under in the compatibility database. A ROM yields
 * several: a family key when the region is a character at a known position, and always
 * the exact identifiers. Must stay in step with `scripts/compat.mjs`.
 * pourquoi : docs/decisions/identite-et-dumps.md § A ROM yields several keys, never one
 */
fun Rom.compatKeys(): List<String> = compatKeys(console, productCode, titleIdHex)

/**
 * Split out from [Rom]: pure string work, and taking the whole ROM would drag a `Uri`
 * into every test of a rule that has nothing to do with files.
 */
fun compatKeys(
    console: Console,
    productCode: String?,
    titleIdHex: String?
): List<String> {
    val keys = LinkedHashSet<String>()
    val code = productCode?.trim()?.uppercase()
    val titleId = titleIdHex?.trim()?.uppercase()

    when (console) {
        // `CTR-P-ARRJ`, the last of the four being the region. Read from the end, not by
        // stripping a prefix: `CTR-P-` retail, `CTR-N-` download, `KTR-P-` New 3DS, and a
        // fourth spelling would break a rule written the other way round.
        Console.THREE_DS -> {
            val four = code?.takeLast(4)?.takeIf { it.length == 4 && it.all(Char::isLetterOrDigit) }
            four?.let {
                keys += "3ds:${it.dropLast(1)}"
                keys += "3ds:$it"
            }
            titleId?.let { keys += "3ds:t:$it" }
        }

        // `NDS-ADAE-01`: game code then maker code, and the maker code is dropped.
        // GameTDB, the index the rating tool resolves names against, keys on the four
        // characters alone and publishes no maker code: a discriminator no source can
        // supply prevents every match rather than a collision.
        Console.DS -> {
            val game = code?.split('-')?.getOrNull(1)?.takeIf { it.length == 4 }
            if (game != null) {
                keys += "ds:${game.dropLast(1)}"
                keys += "ds:$game"
            }
        }

        // `RMCP01`: system, two for the game, the region, two for the publisher. Same
        // shape on both consoles, kept apart by the prefix: the same code exists on each.
        Console.GAMECUBE, Console.WII -> {
            val prefix = if (console == Console.WII) "wii" else "gc"
            val id = code?.takeIf { it.length == 6 && it.all(Char::isLetterOrDigit) }
            id?.let {
                keys += "$prefix:${it.take(3)}${it.substring(4)}"
                keys += "$prefix:$it"
            }
        }

        // The exact serial only: the database is expected to carry every region.
        Console.PSP -> code?.removePrefix("PSP-")?.let { keys += "psp:$it" }
        Console.PS2 -> code?.let { keys += "ps2:$it" }

        // Region-free by design: one title id worldwide, so the exact identifier is also
        // the family.
        Console.SWITCH -> titleId?.let { keys += "switch:$it" }

        else -> Unit
    }

    return keys.toList()
}
