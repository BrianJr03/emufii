package eu.emufii.app.artwork

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import eu.emufii.app.library.Rom
import eu.emufii.app.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What a tile has to paint itself with.
 *
 * [remote] is the high-resolution icon when we have one, [embedded] the one the
 * ROM carries. The distinction is not cosmetic: the two are not drawn the same
 * way. A ROM icon is 48 px of pixel art that has to be scaled up without
 * smoothing, on pain of mush; a remote icon is a real image that has to be
 * smoothed instead.
 */
data class TileArt(val remote: String?, val embedded: java.io.File?) {
    val isPixelArt: Boolean get() = remote == null
    val model: Any? get() = remote ?: embedded
}

/**
 * The icon to display for this game, better as soon as we know it.
 *
 * Always starts with what the ROM carries, so the grid is painted immediately:
 * waiting on the network to show a library already on disk would display holes
 * at startup. The search then sets off, and the tile repaints if it succeeds.
 */
@Composable
fun rememberTileArt(rom: Rom): State<TileArt> {
    val context = LocalContext.current
    val store = remember(context) { ArtworkStore(context.applicationContext) }
    val settings = remember(context) { SettingsStore.get(context) }
    val apiKey by settings.steamGridDbKey.collectAsState()
    val cocoon by settings.cocoonFolder.collectAsState()
    val revision by ArtworkStore.revision.collectAsState()
    val state = remember(rom.uri) { mutableStateOf(TileArt(null, rom.iconFile)) }

    // Restarted if the player enters their key, or points us at Cocoon, while
    // the grid is already on screen: the tiles then fill in without their
    // having to go back.
    LaunchedEffect(rom.uri, apiKey, cocoon, revision) {
        // Cocoon comes before the catalogue, and only the player's own explicit
        // choice comes before Cocoon — which `iconUrl` already honours.
        //
        // The order is the whole feature: this artwork sits on the device, was
        // downloaded for this exact file, and in places was re-cropped by hand.
        // Preferring a fresh guess from a catalogue over a picture someone
        // already chose would be getting it backwards.
        if (store.chosenFor(rom) == null) {
            val local = withContext(Dispatchers.IO) {
                runCatching {
                    CocoonMedia.uriFor(
                        context,
                        cocoon.takeIf { it.isNotBlank() }?.toUri(),
                        rom,
                        CocoonMedia.Kind.ICON
                    )
                }.getOrNull()
            }
            if (local != null) {
                state.value = TileArt(local.toString(), rom.iconFile)
                return@LaunchedEffect
            }
        }
        val url = store.iconUrl(rom, apiKey) ?: return@LaunchedEffect
        state.value = TileArt(url, rom.iconFile)
    }
    return state
}
