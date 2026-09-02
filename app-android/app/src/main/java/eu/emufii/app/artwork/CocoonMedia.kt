package eu.emufii.app.artwork

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import eu.emufii.app.library.Console
import eu.emufii.app.library.Rom
import java.util.concurrent.ConcurrentHashMap

/**
 * Cocoon Shell files what it scrapes under the ROM's own filename: no catalogue to search,
 * no key to type, and the cover a player re-cropped is the one they mean. Read-only,
 * always; Emufii never writes into Cocoon's folders.
 */
object CocoonMedia {

    enum class Kind(val folder: String) {
        /** Square key art, 1024×1024 in practice. */
        ICON("icon"),

        /** Wide banner, 1920×620 in practice. */
        HERO("hero"),

        /** The title, drawn, on transparency. */
        LOGO("logo"),

        SCREENSHOT_GAMEPLAY("screenshot_gameplay"),

        SCREENSHOT_TITLE("screenshot_title"),
    }

    /**
     * Cocoon's own names, not ours. GameCube is absent rather than mapped to `wii`: Cocoon
     * files them apart, and the Wii folder would hand it the artwork of whatever Wii game
     * shares its filename.
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
     * One index per console and kind: a grid draws hundreds of tiles per scroll, and
     * listing a folder through the storage provider costs a real query.
     */
    private val indexes = ConcurrentHashMap<String, Map<String, Uri>>()

    /** Dropped when the player picks another folder, so the next tile rebuilds. */
    fun forget() = indexes.clear()

    /** [root] is the Cocoon folder the player granted us, `Cocoonv2` in practice. */
    fun uriFor(context: Context, root: Uri?, rom: Rom, kind: Kind): Uri? {
        val console = folderFor(rom.console) ?: return null
        if (root == null) return null
        val index = indexes.getOrPut("$root|$console|${kind.folder}") {
            buildIndex(context, root, console, kind)
        }
        return index[baseOf(rom.filename)]
    }

    /** The name Cocoon files artwork under. */
    private fun baseOf(filename: String): String =
        filename.substringBeforeLast('.', filename)

    /**
     * Read off the device and nothing else: the served catalogue carries screenshot links,
     * but this panel is looked at on a handheld, often offline. Gameplay before the title
     * screen, which is a logo the player has already seen on the cover next to it.
     */
    fun stillsFor(context: Context, root: Uri?, rom: Rom): List<Uri> =
        listOfNotNull(
            uriFor(context, root, rom, Kind.SCREENSHOT_GAMEPLAY),
            uriFor(context, root, rom, Kind.SCREENSHOT_TITLE),
        )

    private fun buildIndex(context: Context, root: Uri, console: String, kind: Kind): Map<String, Uri> {
        val folder = runCatching {
            DocumentFile.fromTreeUri(context, root)
                ?.findFile("downloaded_media")
                ?.findFile(console)
                ?.findFile(kind.folder)
        }.getOrNull() ?: return emptyMap()

        // `DocumentFile.listFiles()` allocates a document object per entry to read two
        // columns off it.
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
     * Three shapes come out of Cocoon, and the order between them is the point:
     *
     *  - `Game__cocoon_edit_108_<hash>.png`, the player's own edit, wins outright.
     *  - `Game.png` is what the scraper downloaded.
     *  - `Game (1).png` is a second download, taken only when `Game` is really there next
     *    to it: otherwise a title genuinely ending in "(1)" is filed under a name no ROM has.
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
