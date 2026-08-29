package eu.emufii.app.artwork

import android.content.Context
import androidx.core.net.toUri
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import eu.emufii.app.library.Rom
import eu.emufii.app.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Ce qu'il faut avoir fait pour que la grille s'ouvre **complète**.
 *
 * **Rien ici n'est indispensable** : chaque étape est enveloppée, et un
 * préchauffage qui empêcherait d'entrer serait pire que pas de préchauffage.
 * pourquoi : docs/decisions/jaquettes.md § La grille s'ouvre complète, ou elle se remplit sous les yeux du joueur
 */
object ArtworkPreload {

    /**
     * Combien de jaquettes sont décodées d'avance.
     *
     * Deux écrans de grille : ce que le joueur voit en arrivant, plus la rangée
     * qu'il découvrira au premier mouvement. Au-delà on remplirait la mémoire
     * d'images que personne ne regardera, et sur un handheld elle manque
     * ailleurs.
     */
    private const val DECODED_AHEAD = 24

    /** La taille à laquelle une tuile de grille dessine sa jaquette, en pixels. */
    private const val TILE_PX = 360

    suspend fun warm(context: Context, roms: List<Rom>) = withContext(Dispatchers.IO) {
        if (roms.isEmpty()) return@withContext
        val app = context.applicationContext
        val settings = SettingsStore.get(app)
        val apiKey = settings.steamGridDbKey.value
        val cocoon = settings.cocoonFolder.value.takeIf { it.isNotBlank() }?.toUri()
        val store = ArtworkStore(app)

        // Les adresses, toutes : c'est ici que les index de dossier se
        // construisent, une fois par console au lieu d'une fois par première
        // tuile de chaque console.
        val models = roms.map { rom ->
            runCatching {
                val local = if (store.chosenFor(rom) == null) {
                    CocoonMedia.uriFor(app, cocoon, rom, CocoonMedia.Kind.ICON)
                } else {
                    null
                }
                local?.toString() ?: store.iconUrl(rom, apiKey) ?: rom.iconFile
            }.getOrNull()
        }

        // Le décodage. À la taille de la tuile et non `ORIGINAL` : le chargeur
        // garde ce qu'il a décodé, et une jaquette pleine résolution par jeu
        // remplirait la mémoire de bitmaps que la tuile réduira de toute façon.
        // **Le chargeur unique, pas un nouveau.** `ImageLoader(app)` en
        // construirait un second, avec son propre cache mémoire : on décoderait
        // tout, et les tuiles ne trouveraient rien — le préchauffage aurait
        // chauffé un cache que personne ne lit.
        val loader = SingletonImageLoader.get(app)
        coroutineScope {
            models.take(DECODED_AHEAD).map { model ->
                async {
                    if (model == null) return@async
                    runCatching {
                        loader.execute(
                            ImageRequest.Builder(app)
                                .data(model)
                                .size(Size(TILE_PX, TILE_PX))
                                .build()
                        )
                    }
                }
            }.awaitAll()
        }
    }
}
