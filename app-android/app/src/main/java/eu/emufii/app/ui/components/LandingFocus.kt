package eu.emufii.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.runtime.withFrameNanos

/**
 * Poser le curseur sur ce qui vient de s'ouvrir.
 *
 * Le scaffold le fait pour un ecran ; une couche modale s'ouvre par-dessus un
 * scaffold qui a deja pose le sien ailleurs. Meme mecanisme, memes deux pieges.
 * [key] relance la pose : l'identite de la couche, ou `Unit`.
 * pourquoi : docs/decisions/coquille-ecrans.md § Le curseur arrive avec l'écran
 */
@Composable
fun LandOn(target: FocusRequester, key: Any? = Unit, enabled: Boolean = true) {
    val inputMode = LocalInputModeManager.current
    LaunchedEffect(key, enabled) {
        if (!enabled) return@LaunchedEffect
        repeat(LANDING_FRAMES) {
            withFrameNanos { }
            inputMode.requestInputMode(InputMode.Keyboard)
            runCatching { target.requestFocus() }
        }
    }
}

/**
 * Combien d'images la pose reclame son controle.
 *
 * Six, soit une centaine de millisecondes : une couche modale se stabilise plus
 * vite qu'un ecran, mais elle arrive souvent avec une animation d'entree, et le
 * noeud vise n'est place qu'une fois celle-ci demarree. La fenetre reste trop
 * courte pour arracher le curseur a quelqu'un qui aurait deja appuye.
 */
private const val LANDING_FRAMES = 6
