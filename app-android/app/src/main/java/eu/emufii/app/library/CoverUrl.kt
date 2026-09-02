package eu.emufii.app.library

/**
 * A display name's last resort, asked only when every reader has failed. A dump's
 * filename is scene shorthand: bracketed tags (`[NSP]`, `[Decrypted]`), a region in
 * parentheses, a release number. This keeps those off the grid while [GameTitles]
 * fetches the proper title by identifier.
 */
fun displayNameFromFilename(filename: String): String {
    val noExt = filename.substringBeforeLast('.', filename)
    val stripped = noExt
        .replace(Regex("""\s*\[[^\]]*\]"""), "")
        .substringBefore(" (")
        .trim()
        .replace(Regex("""\s+"""), " ")
    // Four digits or more at the end is a scene release number (`0919838`);
    // shorter runs stay, because "Portal 2" and "Final Fantasy VII" are titles.
    val withoutSceneNumber = stripped.replace(Regex("""\s+\d{4,}$"""), "")
    return withoutSceneNumber.ifBlank { noExt }
}

fun shortLabel(displayName: String): String {
    val words = displayName.split(Regex("[\\s._-]+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> words.take(2).joinToString("") { it.take(1).uppercase() }
    }
}
