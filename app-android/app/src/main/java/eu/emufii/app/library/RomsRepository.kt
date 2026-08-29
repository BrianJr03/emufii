package eu.emufii.app.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import eu.emufii.app.library.psp.PspUmdReader
import eu.emufii.app.library.switchfs.SwitchReader

private const val TAG = "RomsRepository"
private const val PREFS = "emufii_library"
private const val KEY_FOLDER_URI = "roms_folder_uri"

/**
 * Le second dossier, optionnel, ajoute le 2026-08-28.
 *
 * Une cle distincte plutot qu'un ensemble de N dossiers : le premier dossier est
 * deja ecrit chez tout le monde sous [KEY_FOLDER_URI], et une migration vers une
 * liste rendrait une bibliotheque vide a qui ouvrirait une build anterieure. Deux
 * cles, deux arbres marches a la suite, et rien a migrer.
 */
private const val KEY_FOLDER_URI_2 = "roms_folder_uri_2"

/**
 * How deep to walk: every extra level costs a query per directory.
 * pourquoi : docs/decisions/scan-bibliotheque.md § La marche de l'arbre
 */
private const val MAX_DEPTH = 6

/** Guards against a folder pick that lands on something enormous. */
private const val MAX_FILES = 5000

/**
 * The containers the PSP shares with other consoles: one of these enters the
 * library only once recognised as a PSP game.
 * pourquoi : docs/decisions/scan-bibliotheque.md § Une chaîne de décision, le moins cher d'abord
 */
class RomsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val headerReader = RomHeaderReader(context)
    private val discImages = DiscImageReader(context)
    private val smdhReader = SmdhReader(context)
    private val romNames = RomNames(context)
    private val hiddenRoms = HiddenRoms(context)
    private val ndsReader = NdsBannerReader(context)
    private val switchReader = SwitchReader(context)
    private val pspReader = PspUmdReader(context)

    private val iconCache = IconCache(context)

    fun savedFolderUri(): Uri? = prefs.getString(KEY_FOLDER_URI, null)?.let(Uri::parse)

    /** Le second dossier, ou null tant que le joueur n'en a pas ajoute. */
    fun secondFolderUri(): Uri? = prefs.getString(KEY_FOLDER_URI_2, null)?.let(Uri::parse)

    /**
     * Les arbres a parcourir, dans l'ordre. Le second n'existe pas sans le
     * premier : c'est un ajout, pas un remplacement.
     */
    private fun folderUris(): List<Uri> = listOfNotNull(savedFolderUri(), secondFolderUri())

    /**
     * Something the user can recognise, not the raw tree URI.
     * pourquoi : docs/decisions/scan-bibliotheque.md § Ce que le joueur voit du dossier choisi
     */
    fun savedFolderLabel(): String? = label(savedFolderUri())

    fun secondFolderLabel(): String? = label(secondFolderUri())

    private fun label(uri: Uri?): String? {
        if (uri == null) return null
        val docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        return docId?.substringAfter(':')?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment
    }

    fun setFolder(uri: Uri) = setFolder(KEY_FOLDER_URI, uri)

    /**
     * Ajoute ou remplace le second dossier. Choisir le meme arbre que le premier
     * est refuse : les deux marches se croiseraient sur chaque fichier, et le
     * joueur croirait avoir ajoute quelque chose.
     */
    fun setSecondFolder(uri: Uri): Boolean {
        if (uri == savedFolderUri()) return false
        setFolder(KEY_FOLDER_URI_2, uri)
        return true
    }

    private fun setFolder(key: String, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        // L'ancien arbre de cette cle n'est plus lu : on lui rend sa permission,
        // sauf si l'autre cle s'en sert encore.
        val previous = prefs.getString(key, null)?.let(Uri::parse)
        prefs.edit().putString(key, uri.toString()).apply()
        if (previous != null && previous != uri) release(previous)
        cachedRoms = null
    }

    fun clear() {
        val kept = secondFolderUri()
        savedFolderUri()?.takeIf { it != kept }?.let(::release)
        prefs.edit().remove(KEY_FOLDER_URI).apply()
        cachedRoms = null
    }

    /** Retire le second dossier ; le premier, lui, reste la bibliotheque. */
    fun clearSecondFolder() {
        val kept = savedFolderUri()
        secondFolderUri()?.takeIf { it != kept }?.let(::release)
        prefs.edit().remove(KEY_FOLDER_URI_2).apply()
        cachedRoms = null
    }

    private fun release(uri: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    /**
     * Last scan's result. Deliberately shared across instances: the cache
     * belongs to the process, not to the screen.
     * pourquoi : docs/decisions/scan-bibliotheque.md § Le cache appartient au processus, pas à l'écran
     */
    private companion object {
        @Volatile
        var cachedRoms: List<Rom>? = null
        val scanLock = Any()
    }

    fun cachedOrScan(): List<Rom> = cachedRoms?.let(::named) ?: scan()

    /**
     * [force] is for the explicit Rescan action, which has to look at the disc
     * again even when a perfectly good cache exists.
     */
    fun scan(force: Boolean = false): List<Rom> = synchronized(scanLock) {
        // A library scanned in French is stale the moment the app is switched to
        // English: every title in it is the wrong string. Changing the language
        // recreates the activity but not this process-level cache, so the check
        // belongs here rather than at the call sites.
        TitleLanguage.apply(context)
        val staleLanguage = scannedLanguage != null && scannedLanguage != TitleLanguage.tag
        // Another thread may have finished while we waited on the lock.
        if (!force && !staleLanguage) cachedRoms?.let { return named(it) }
        return doScan()
    }

    /** The language the cached list was read in. Null until the first scan. */
    private var scannedLanguage: String? = null

    private fun doScan(): List<Rom> {
        // Titles come out of the cartridges in whatever language is asked for,
        // so the app's own language has to be settled before a single one is
        // read, and re-read each scan, because changing it is what triggers one.
        TitleLanguage.apply(context)
        scannedLanguage = TitleLanguage.tag
        val folders = folderUris()
        if (folders.isEmpty()) return emptyList()
        // Un dossier illisible ne doit pas emporter l'autre : chaque arbre est
        // marche pour lui-meme, et celui qui echoue ne rend rien.
        val found = folders.flatMap { uri ->
            runCatching { walk(uri) }
                .onFailure { Log.w(TAG, "scan failed for $uri", it) }
                .getOrDefault(emptyList())
        }
            // Le second dossier peut etre un sous-dossier du premier, ou le meme
            // volume monte deux fois : un jeu vu deux fois est un doublon dans la
            // grille, et deux entrees pour la meme partie a l'ecran des sessions.
            .distinctBy { it.uri.toString() }

        Log.i(TAG, "walked ${found.size} candidate file(s) in ${folders.size} folder(s), titles in ${TitleLanguage.tag}")

        return found
            .mapNotNull { it.toRom() }
            .also { cachedRoms = it }
            .let(::named)
    }

    /**
     * The player's chosen names, laid over the scanned list on the way out and
     * deliberately *not* baked into the cache. The sort belongs here too.
     * pourquoi : docs/decisions/scan-bibliotheque.md § Les noms choisis par le joueur sont posés à la sortie, jamais dans le cache
     */
    private fun named(roms: List<Rom>): List<Rom> {
        // The index titles come before the player's choices, like every other
        // source of a name: they only ever replace a filename, never a title
        // read out of the file or a name someone typed.
        val titles = GameTitles.cached(context)
        return roms.filterNot(hiddenRoms::isHidden)
            .map { GameTitles.apply(titles, it) }
            .map(romNames::apply)
            .sortedWith(compareBy({ it.console.ordinal }, { it.displayName.lowercase() }))
    }

    private data class Candidate(
        val uri: Uri,
        val name: String,
        val console: Console,
        val addedAt: Long,
        val size: Long,
    )

    /**
     * Walks the picked tree, subfolders included. Queries [DocumentsContract]
     * directly, and breadth-first so shallow folders come first.
     * pourquoi : docs/decisions/scan-bibliotheque.md § La marche de l'arbre
     */
    private fun walk(treeUri: Uri): List<Candidate> {
        val resolver = context.contentResolver
        val out = mutableListOf<Candidate>()
        // The third element is the folder's own name, "" at the root: it is
        // what settles a file's console before any byte is read.
        val queue = ArrayDeque<Triple<String, Int, String>>()
        val seen = mutableSetOf<String>()

        queue += Triple(DocumentsContract.getTreeDocumentId(treeUri), 0, "")

        while (queue.isNotEmpty() && out.size < MAX_FILES) {
            val (parentId, depth, folderName) = queue.removeFirst()
            if (!seen.add(parentId)) continue

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                // The "recently added" sort has no other source: a document
                // provider exposes no creation date. Asked for in the same query
                // as the rest, it costs nothing, where fetching it afterwards
                // would be one round trip per file.
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_SIZE,
            )

            // A folder we can't read shouldn't abort the whole scan, some
            // providers throw on entries they've since lost access to.
            val cursor = runCatching { resolver.query(childrenUri, projection, null, null, null) }
                .onFailure { Log.w(TAG, "cannot list $parentId", it) }
                .getOrNull() ?: continue

            cursor.use {
                while (it.moveToNext() && out.size < MAX_FILES) {
                    val docId = it.getString(0) ?: continue
                    val name = it.getString(1) ?: continue
                    val mime = it.getString(2)

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        // Skip dot-folders: emulator caches, save states and
                        // `.git` checkouts hold nothing playable and can be big.
                        if (depth + 1 <= MAX_DEPTH && !name.startsWith(".")) {
                            queue += Triple(docId, depth + 1, name)
                        }
                        continue
                    }

                    val ext = name.substringAfterLast('.', "")
                    val byName = Console.forExtension(ext) ?: continue
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    // One chain, cheapest truth first: folder name, extension,
                    // then bytes. Unvouched shared extensions are not listed.
                    // pourquoi : docs/decisions/scan-bibliotheque.md § Une chaîne de décision, le moins cher d'abord
                    val extLower = ext.lowercase()
                    val folderConsole = Console.forFolder(folderName)
                    val console = when {
                        folderConsole != null -> folderConsole
                        extLower in DiscImage.AMBIGUOUS_EXTENSIONS ->
                            discImages.identify(uri) ?: continue
                        extLower in DiscImage.SNIFFED_EXTENSIONS ->
                            discImages.identify(uri) ?: byName
                        else -> byName
                    }
                    out += Candidate(
                        uri = uri,
                        name = name,
                        console = console,
                        // Null on some providers, hence the guarded read:
                        // `getLong` on a null column returns 0, but only if it
                        // does not throw first.
                        addedAt = if (it.isNull(3)) 0L else it.getLong(3),
                        size = if (it.isNull(4)) 0L else it.getLong(4),
                    )
                }
            }
        }

        if (out.size >= MAX_FILES) {
            Log.w(TAG, "stopped at $MAX_FILES files — library larger than expected")
        }
        return out
    }

    /**
     * 3DS and DS files get opened; disc images take their *title* from the
     * filename but their *identity* from the disc.
     * pourquoi : docs/decisions/scan-bibliotheque.md § Ce qu'on ouvre, et ce qu'on ne peut pas ouvrir
     */
    private fun Candidate.toRom(): Rom? = readRom()?.copy(addedAt = addedAt)

    /**
     * The title read out of the file, which is what gets cached — the chosen
     * name is laid over it in [named], never here.
     * pourquoi : docs/decisions/scan-bibliotheque.md § Les noms choisis par le joueur sont posés à la sortie, jamais dans le cache
     */
    private fun Candidate.readRom(): Rom? {
        if (console == Console.DS) return toDsRom()
        if (console == Console.SWITCH) return toSwitchRom()

        if (console == Console.PSP) return toPspRom()

        // Same path as the Nintendo discs: title from the filename, number
        // from the disc, exactly as ARMSX2 displays it.
        // pourquoi : docs/decisions/scan-bibliotheque.md § `productCode` et `titleIdHex` ne jouent pas le même rôle
        if (console == Console.GAMECUBE || console == Console.WII || console == Console.PS2) {
            return toDiscRom()
        }

        // What is left is what no path can serve: we do not list it. A grid
        // whose only function is to open sessions has no business showing games
        // it cannot put into a game.
        if (console != Console.THREE_DS) return null

        // A CIA is told apart here rather than in the reader: the cartridge
        // formats announce themselves by magic, the CIA does not, and only the
        // caller knows what the file claimed to be.
        val header = headerReader.read(uri, cia = name.substringAfterLast('.', "").equals("cia", true))
        val smdh = header?.let { readSmdhWithCache(uri, it) }
        val iconFile = header?.let { h -> iconCache.fileFor(h.titleIdHex).takeIf { it.exists() } }

        return Rom(
            uri = uri,
            filename = name,
            displayName = smdh?.title ?: displayNameFromFilename(name),
            console = console,
            titleIdHex = header?.titleIdHex,
            productCode = header?.productCode,
            iconFile = iconFile,
            accentArgb = header?.let { iconCache.readAccent(it.titleIdHex) }
        )
    }

    /**
     * The PSP: title and icon read from `PSP_GAME`, a few kilobytes on a disc
     * weighing a million. Disc id is the cache key, never the session identity.
     * pourquoi : docs/decisions/scan-bibliotheque.md § `productCode` et `titleIdHex` ne jouent pas le même rôle
     */
    private fun Candidate.toPspRom(): Rom? {
        val fallback = Rom(
            uri = uri,
            filename = name,
            displayName = displayNameFromFilename(name),
            console = console
        )

        val cachedKey = ndsKeyCache[uri.toString()]
        if (cachedKey != null) {
            val icon = iconCache.fileFor(cachedKey).takeIf { it.exists() }
            val title = iconCache.readTitle(cachedKey)
            if (icon != null && title != null) {
                return fallback.copy(
                    displayName = title,
                    iconFile = icon,
                    productCode = cachedKey,
                    accentArgb = iconCache.readAccent(cachedKey)
                )
            }
        }

        val data = pspReader.read(uri)
        // `.iso`/`.chd` must PROVE they are PSP (a `PSP_GAME` entry); `.pbp`
        // and `.cso` are admitted on their extension alone.
        // pourquoi : docs/decisions/scan-bibliotheque.md § Une chaîne de décision, le moins cher d'abord
        val ambiguous = name.substringAfterLast('.', "").lowercase() in DiscImage.AMBIGUOUS_EXTENSIONS
        if (ambiguous && !data.recognised) return null
        // A homebrew can have no disc id at all while still having an icon; with
        // no key it would have nowhere to be filed, so the filename stands in,
        // stable from one scan to the next, which is all a cache key is asked
        // for.
        val key = data.cacheKey ?: "PSP-F%08x".format(name.lowercase().hashCode())
        if (data.icon == null && data.title == null) return fallback
        ndsKeyCache[uri.toString()] = key

        data.icon?.let { bitmap ->
            iconCache.writeIcon(key, bitmap)
            IconAccent.fromBitmap(bitmap)?.let { iconCache.writeAccent(key, it) }
        }
        data.title?.let { iconCache.writeTitle(key, it) }

        return fallback.copy(
            displayName = data.title ?: fallback.displayName,
            iconFile = iconCache.fileFor(key).takeIf { it.exists() },
            productCode = key,
            accentArgb = iconCache.readAccent(key)
        )
    }

    /**
     * The DS path, cached like the 3DS one: the icon lands under the
     * cartridge's game code so a rescan does not re-decode every banner.
     * pourquoi : docs/decisions/scan-bibliotheque.md § Ce qu'on ouvre, et ce qu'on ne peut pas ouvrir
     */
    private fun Candidate.toDsRom(): Rom {
        val fallback = Rom(
            uri = uri,
            filename = name,
            displayName = displayNameFromFilename(name),
            console = console
        )

        val key = ndsKeyCache[uri.toString()]
        if (key != null) {
            val icon = iconCache.fileFor(key).takeIf { it.exists() }
            val title = iconCache.readTitle(key)
            if (icon != null && title != null) {
                return fallback.copy(
                    displayName = title,
                    iconFile = icon,
                    productCode = key,
                    accentArgb = iconCache.readAccent(key)
                )
            }
        }

        val data = ndsReader.read(uri)
        val cacheKey = data.cacheKey ?: return fallback
        ndsKeyCache[uri.toString()] = cacheKey

        data.icon?.let { bitmap ->
            iconCache.writeIcon(cacheKey, bitmap)
            IconAccent.fromBitmap(bitmap)?.let { iconCache.writeAccent(cacheKey, it) }
        }
        data.title?.let { iconCache.writeTitle(cacheKey, it) }

        return fallback.copy(
            displayName = data.title ?: fallback.displayName,
            iconFile = iconCache.fileFor(cacheKey).takeIf { it.exists() },
            productCode = cacheKey,
            accentArgb = iconCache.readAccent(cacheKey)
        )
    }

    /**
     * A GameCube or Wii disc image: title from the filename, disc id from the
     * header. Filed under `productCode`, never `titleIdHex`.
     * pourquoi : docs/decisions/scan-bibliotheque.md § `productCode` et `titleIdHex` ne jouent pas le même rôle
     */
    private fun Candidate.toDiscRom(): Rom {
        val fallback = Rom(
            uri = uri,
            filename = name,
            displayName = displayNameFromFilename(name),
            console = console
        )
        val info = discImages.read(uri, addedAt, size) ?: return fallback
        // The console read back wins: it is the same read that served the scan,
        // and it can tell a GameCube RVZ from a Wii RVZ where the extension
        // cannot.
        return fallback.copy(
            console = info.console,
            productCode = info.gameId,
            ps2ElfCrc = info.ps2Identity?.elfCrc,
        )
    }

    /**
     * The Switch path: a title id off the plaintext table of contents, and
     * nothing else out of the file — the name comes from the index
     * ([GameTitles]) and the icon from the artwork sources. Icons cached from
     * an era of console keys keep showing: they are on disk and still true.
     */
    private fun Candidate.toSwitchRom(): Rom {
        val fallback = Rom(
            uri = uri,
            filename = name,
            displayName = displayNameFromFilename(name),
            console = console
        )

        val cachedId = ndsKeyCache[uri.toString()]
        if (cachedId != null) {
            val icon = iconCache.fileFor(cachedId).takeIf { it.exists() }
            val title = iconCache.readTitle(cachedId)
            if (icon != null && title != null) {
                return fallback.copy(
                    displayName = title,
                    iconFile = icon,
                    titleIdHex = cachedId,
                    accentArgb = iconCache.readAccent(cachedId)
                )
            }
        }

        val key = switchReader.titleId(uri) ?: return fallback
        ndsKeyCache[uri.toString()] = key

        return fallback.copy(
            titleIdHex = key,
            iconFile = iconCache.fileFor(key).takeIf { it.exists() },
            accentArgb = iconCache.readAccent(key)
        )
    }

    /**
     * Which cache key a given file resolved to last time. Avoids re-reading a
     * header just to learn where its icon was filed.
     */
    private val ndsKeyCache = HashMap<String, String>()

    private fun readSmdhWithCache(uri: Uri, header: RomHeader): SmdhData {
        val cachedIcon = iconCache.fileFor(header.titleIdHex)
        val cachedTitle = iconCache.readTitle(header.titleIdHex)
        if (cachedIcon.exists() && cachedTitle != null) {
            return SmdhData(icon = null, title = cachedTitle)
        }
        val fresh = smdhReader.read(uri, header)
        if (fresh.icon != null) {
            iconCache.writeIcon(header.titleIdHex, fresh.icon)
            IconAccent.fromBitmap(fresh.icon)?.let { iconCache.writeAccent(header.titleIdHex, it) }
        }
        if (fresh.title != null) iconCache.writeTitle(header.titleIdHex, fresh.title)
        return fresh
    }
}
