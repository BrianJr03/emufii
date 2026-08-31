package eu.emufii.app.library

/**
 * The last resort of a display name, and what it must never show.
 *
 * A dump's filename is scene shorthand: tags in brackets (`[NSP]`, `[Decrypted]`),
 * a region in parentheses, a release number nobody titles a game with. The title
 * itself is in the file, but an encrypted file keeps it, and this function is
 * only ever asked to speak when every reader has failed, on exactly those files.
 * What it can do is stop the tags from reaching the grid while the proper title
 * is fetched by its identifier (see [GameTitles]).
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
