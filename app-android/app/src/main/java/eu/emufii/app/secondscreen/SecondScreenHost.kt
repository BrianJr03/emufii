package eu.emufii.app.secondscreen

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.ui.theme.EmufiiTheme

/**
 * Mounts the second screen for as long as the app is in front.
 *
 * This is the first of the two hosts the scout named
 * (`docs/PHASE1_SCOUT_THOR_SCREEN2.md`): a [Presentation], which is a dialog
 * bound to the activity, costing no permission and dying with it. That death is
 * a real limit and not a bug to fix here: while Emufii is in the background the
 * panel goes dark, which covers the lobby and the library and says nothing
 * during a game. The service host lifts exactly that, and lifts only that,
 * because everything below is written to be shared with it.
 */
@Composable
fun SecondScreenHost(enabled: Boolean) {
    val context = LocalContext.current
    val display by rememberPresentationDisplay()

    // Keyed on both: turning the setting off, or the panel going away, must take
    // the window down the same way, and a new display gets a new window rather
    // than a reused one pointing at the old one.
    DisposableEffect(display, enabled) {
        val target = display
        if (!enabled || target == null) return@DisposableEffect onDispose {}

        val presentation = EmufiiPresentation(context, target)
        // A display can go dark between the moment it is listed and the moment
        // the window is shown; the platform answers that race by throwing.
        // Losing the second screen must never be able to take the app with it.
        val shown = runCatching { presentation.show() }.isSuccess

        onDispose {
            if (shown) runCatching { presentation.dismiss() }
            presentation.release()
        }
    }
}

/**
 * The window itself.
 *
 * It carries its own [SecondScreenWindowOwner] rather than the activity's, for
 * the reason that class documents at length: the service host has no activity,
 * and this is the piece that would otherwise have to be written twice.
 */
private class EmufiiPresentation(
    context: Context,
    display: Display,
) : Presentation(context, display) {

    private val owner = SecondScreenWindowOwner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nothing on this panel is touchable, so nothing on it should be able to
        // steal a press meant for the game or for the app. Without this the
        // window takes focus when it appears and the front screen loses it.
        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }

        val view = ComposeView(context).apply {
            setContent { SecondScreenSurface() }
        }
        owner.attachTo(view)
        setContentView(view)
    }

    /** Called by the host on the way out; a dismissed dialog does not do this itself. */
    fun release() = owner.detach()
}

/**
 * The themed root, shared by every host.
 *
 * The theme is read from the store rather than inherited from the caller's
 * composition on purpose: the service host has no enclosing composition to
 * inherit from, and a panel that ignored the player's dark or OLED choice while
 * the front screen honoured it would look like a bug in the app rather than a
 * second window.
 */
@Composable
private fun SecondScreenSurface() {
    val context = LocalContext.current
    val settings = remember(context) { SettingsStore.get(context) }
    val theme by settings.theme.collectAsState()
    val accent by settings.accent.collectAsState()
    val model by SecondScreen.model.collectAsState()

    // Resolved against this window's own configuration, which is the second
    // display's: a player on the system theme should see both panels agree,
    // and hard-coding either value here would make them disagree at dusk.
    EmufiiTheme(
        darkTheme = theme.isDark(androidx.compose.foundation.isSystemInDarkTheme()),
        oled = theme.isOled,
        accent = accent
    ) {
        SecondScreenContent(model)
    }
}
