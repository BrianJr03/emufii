package eu.emufii.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.runtime.withFrameNanos

/**
 * A modal layer opens over a scaffold that has already placed its cursor elsewhere.
 * pourquoi : docs/decisions/coquille-ecrans.md § The cursor arrives with the screen
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
 * Six frames, about a hundred milliseconds: a modal layer often arrives with an entrance
 * animation, and the target node is only placed once that is done.
 */
private const val LANDING_FRAMES = 6
