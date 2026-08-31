package eu.emufii.app.artwork

import android.content.Context
import androidx.core.content.edit
import eu.emufii.app.library.Rom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * What the app has learned about a game's icon. Failures are remembered too: a
 * game the catalogue does not know produces nothing to cache, so without a
 * negative mark it is the one we would ask about forever. The mark expires after
 * [MISS_TTL_MS], since a missing title may be added tomorrow.
 *
 * Only the URL is kept; caching the bytes is Coil's business, and redoing it
 * here would give two caches to age together.
 */
class ArtworkStore(context: Context) {

    private val prefs = context.getSharedPreferences("artwork", Context.MODE_PRIVATE)

    /** One search at a time: thirty tiles appear all at once. */
    private val lock = Mutex()

    /**
     * The remote icon's URL, or null when there is none.
     *
     * Never throws and never blocks the display: the tile shows the ROM's icon in
     * the meantime, and updates if a better one turns up.
     */
    suspend fun iconUrl(rom: Rom, apiKey: String): String? {
        val key = key(rom)

        // The player's choice comes before everything, before even the API key:
        // an icon already chosen stays on display if the key is removed later.
        chosen(key)?.let { return it }

        if (apiKey.isBlank()) return null

        cached(key)?.let { return it.takeIf { url -> url.isNotBlank() } }

        return lock.withLock {
            // Another tile may have answered while we waited on the lock.
            cached(key)?.let { return@withLock it.takeIf { url -> url.isNotBlank() } }
            val found = SteamGridDb.iconUrl(rom.displayName, apiKey)
            remember(key, found)
            found
        }
    }

    /**
     * The icon the player chose themselves, or null.
     *
     * Kept apart from the automatic one and never overwritten by it: that is what
     * makes a manual choice survive everything, a new key, an expired cache, a
     * catalogue that changes its mind. Matching by name gets things wrong, and
     * when someone has taken the trouble to correct it, correcting them back
     * would be the worst possible behaviour.
     */
    fun chosen(key: String): String? =
        prefs.getString(PICK_PREFIX + key, null)?.takeIf { it.isNotBlank() }

    fun choose(rom: Rom, url: String) {
        prefs.edit { putString(PICK_PREFIX + key(rom), url) }
        bumpRevision()
    }

    fun clearChoice(rom: Rom) {
        prefs.edit { remove(PICK_PREFIX + key(rom)) }
        bumpRevision()
    }

    fun chosenFor(rom: Rom): String? = chosen(key(rom))

    /**
     * What we already know: the URL, the empty string for "searched, found
     * nothing", or null for "never searched".
     */
    private fun cached(key: String): String? {
        val url = prefs.getString(URL_PREFIX + key, null) ?: return null
        if (url.isNotBlank()) return url
        val at = prefs.getLong(MISS_PREFIX + key, 0L)
        if (System.currentTimeMillis() - at > MISS_TTL_MS) return null
        return ""
    }

    private fun remember(key: String, url: String?) {
        prefs.edit {
            putString(URL_PREFIX + key, url ?: "")
            if (url == null) putLong(MISS_PREFIX + key, System.currentTimeMillis())
        }
    }

    /**
     * The title id when the console carries one, the displayed name otherwise.
     *
     * Two dumps of the same game then share a single entry, and a renamed ROM
     * does not start a fresh search.
     */
    fun key(rom: Rom): String = rom.sessionId ?: rom.displayName

    companion object {
        /**
         * Goes up at every manual choice.
         *
         * The tiles observe it, so the one just corrected repaints as the picker
         * closes; without that you would have to leave the library and come back
         * to see your own choice, which suggests it was not taken.
         */
        private val _revision = MutableStateFlow(0)
        val revision: StateFlow<Int> = _revision.asStateFlow()

        private fun bumpRevision() {
            _revision.value = _revision.value + 1
        }

        const val URL_PREFIX = "url:"
        const val MISS_PREFIX = "miss:"
        const val PICK_PREFIX = "pick:"
        const val MISS_TTL_MS = 7L * 24 * 60 * 60 * 1000 // one week
    }
}
