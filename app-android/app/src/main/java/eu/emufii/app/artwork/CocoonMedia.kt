package eu.emufii.app.artwork

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import eu.emufii.app.library.Console
import eu.emufii.app.library.Rom
import java.util.concurrent.ConcurrentHashMap

/**
 * The artwork Cocoon already downloaded, on this device, for these exact files.
 *
 * Cocoon Shell scrapes a library and files what it finds under the ROM's own
 * filename. That is the whole trick: no catalogue to search, no name to guess,
 * no key to type. A player who has already given their games a face in Cocoon
 * should see that same face here, and the point is not to save a download — it
 * is that the choice was theirs. A game they re-cropped, or whose alternate
 * cover they preferred, is the one they mean.
 *
 * Read-only, always. Emufii never writes into Cocoon's folders.
 */
object CocoonMedia {

    /** The kinds Cocoon files, of which we use the first two. */
    enum class Kind(val folder: String) {
        /** Square key art, 1024×1024 in practice. What a tile wants. */
        ICON("icon"),

        /** Wide banner, 1920×620 in practice. What a card's backdrop wants. */
        HERO("hero"),

        /** The title, drawn, on transparency. */
        LOGO("logo"),
    }

    /**
     * Cocoon's own name for each console, which is not ours.
     *
     * GameCube is deliberately absent rather than mapped to `wii`: Cocoon files
     * them apart, and pointing a GameCube game at the Wii folder would hand it
     * the artwork of whatever Wii game happens to share its filename.
     */
    private fun folderFor(console: Console): String? = when (console) {
        Console.THREE_DS -> "n3ds"
        Console.DS -> "nds"
        Console.PSP -> "psp"
        Console.PS2 -> "ps2"
        Console.SWITCH -> "switch"
        Console.WII -> "wii"
        Console.GAMECUBE -> null
    }

    /**
     * One index per console and kind, built once and kept.
     *
     * A grid draws hundreds of tiles per scroll and each one asks this question.
     * Listing a folder through the storage provider costs a real query, so it is
     * paid once per folder rather than once per tile.
     */
    private val indexes = ConcurrentHashMap<String, Map<String, Uri>>()

    /** Dropped when the player picks another folder, so the next tile rebuilds. */
    fun forget() = indexes.clear()

    /**
     * The picture for this game, or null when Cocoon has none.
     *
     * [root] is the Cocoon folder the player granted us, `Cocoonv2` in practice.
     */
    fun uriFor(context: Context, root: Uri?, rom: Rom, kind: Kind): Uri? {
        val console = folderFor(rom.console) ?: return null
        if (root == null) return null
        val index = indexes.getOrPut("$root|$console|${kind.folder}") {
            buildIndex(context, root, console, kind)
        }
        return index[baseOf(rom.filename)]
    }

    /** A filename without its extension: the name Cocoon files artwork under. */
    private fun baseOf(filename: String): String =
        filename.substringBeforeLast('.', filename)

    private fun buildIndex(context: Context, root: Uri, console: String, kind: Kind): Map<String, Uri> {
        val folder = runCatching {
            DocumentFile.fromTreeUri(context, root)
                ?.findFile("downloaded_media")
                ?.findFile(console)
                ?.findFile(kind.folder)
        }.getOrNull() ?: return emptyMap()

        // Queried directly rather than through `DocumentFile.listFiles()`, which
        // allocates a document object per entry to read two columns off it.
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            folder.uri,
            DocumentsContract.getDocumentId(folder.uri)
        )
        val names = mutableListOf<Pair<String, Uri>>()
        runCatching {
            context.contentResolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ?: continue
                    val name = cursor.getString(1) ?: continue
                    names += name to DocumentsContract.buildDocumentUriUsingTree(root, id)
                }
            }
        }
        if (names.isEmpty()) return emptyMap()

        val plain = names.map { baseOf(it.first) }.toHashSet()
        val best = HashMap<String, Pair<Int, Uri>>()
        for ((name, uri) in names) {
            val (base, rank) = classify(baseOf(name), plain)
            val score = rank * 2 + if (name.endsWith(".png", ignoreCase = true)) 0 else 1
            val current = best[base]
            if (current == null || score < current.first) best[base] = score to uri
        }
        return best.mapValues { it.value.second }
    }

    /**
     * A media filename reduced to the game it belongs to, and how much we want it.
     *
     * Three shapes come out of Cocoon, and the order between them is the point:
     *
     *  - `Game__cocoon_edit_108_<hash>.png` is the player's own edit, and it wins
     *    outright. Someone cropped that cover on purpose.
     *  - `Game.png` is what the scraper downloaded.
     *  - `Game (1).png` is a second download kept beside the first. Taken only
     *    when nothing better exists, and only when `Game` is really there next to
     *    it — otherwise a game whose title genuinely ends in "(1)" would be
     *    filed under a name no ROM has.
     */
    private fun classify(stem: String, plain: Set<String>): Pair<String, Int> {
        val edit = stem.indexOf(EDIT_MARK)
        if (edit > 0) return stem.substring(0, edit) to 0

        val duplicate = DUPLICATE.find(stem)
        if (duplicate != null) {
            val without = stem.removeRange(duplicate.range)
            if (without in plain) return without to 2
        }
        return stem to 1
    }

    private const val EDIT_MARK = "__cocoon_edit_"
    private val DUPLICATE = Regex(" \\(\\d+\\)$")
}
