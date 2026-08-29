package eu.emufii.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

/**
 * L'horloge lente : **une seule pour tout ce qui bouge en permanence**.
 *
 * Ne pas en creer une seconde — ce qui coute n'est pas combien on dessine, mais
 * combien de fois. Immobile si le systeme a coupe les animations.
 * pourquoi : docs/decisions/performance-rendu.md § Une seule horloge pour tout ce qui bouge en permanence
 */
@Composable
fun rememberSlowMillis(): Double {
    if (!rememberAnimationsEnabled()) return FROZEN_MS

    val millis = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        // **`delay`, et surtout pas `withInfiniteAnimationFrameNanos`.**
        //
        // Celui-ci rappelle à *chaque image de l'écran* — 120 fois par seconde
        // sur la Thor — même quand on n'écrit rien. Le rappel maintient la
        // boucle d'images éveillée : l'app ne s'endort jamais, et la cadence
        // qu'on croit régler ne gate que l'écriture, pas le réveil. C'est ce qui
        // faisait qu'abaisser le battement de 30 à 12 ne changeait rien du tout
        // à la dépense — mesuré deux fois, 30 % dans les deux cas.
        //
        // `delay` dort. Entre deux battements l'app ne reçoit rien, ne mesure
        // rien, ne recompose rien. L'animation n'est plus alignée sur la
        // synchronisation verticale, et à douze battements par seconde pour un
        // dégradé qui met dix-neuf secondes à traverser, cela ne se voit pas.
        val origin = System.nanoTime()
        while (true) {
            millis.longValue = (System.nanoTime() - origin) / 1_000_000
            delay(FRAME_INTERVAL_MS)
        }
    }
    return millis.longValue.toDouble()
}

/**
 * Le battement : douze fois par seconde.
 *
 * Choisi sur ce que les mouvements montrent, jamais sur ce que l'écran sait
 * faire. Le cycle du fond dure dix-neuf secondes, une onde
 * trente-cinq, et le dégradé du curseur fait un tour en 1,8 s : à 120 Hz, tous
 * avancent d'une fraction de pixel entre deux images. La fréquence n'achetait
 * aucune douceur, seulement de la chaleur.
 */
private const val FRAME_INTERVAL_MS = 1_000L / 12

/**
 * L'instant auquel tout se fige quand les animations sont coupées.
 *
 * Pas zéro : à zéro les ondes sont à leur naissance,
 * donc l'écran se fige sur la seule composition qui ne montre rien. Huit
 * secondes, c'est les ondes à mi-chemin.
 */
private const val FROZEN_MS = 8_000.0
