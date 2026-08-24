package eu.emufii.app.secondscreen

import android.app.LocaleManager
import android.app.Presentation
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import eu.emufii.app.notify.AppForeground
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.ui.theme.EmufiiTheme

/**
 * Mounts the second screen while there is a reason to light it.
 *
 * This is the first of the two hosts the scout named
 * (`docs/PHASE1_SCOUT_THOR_SCREEN2.md`): a [Presentation], which is a dialog
 * bound to the activity, costing no permission and dying with it.
 *
 * Bound to the activity's *lifetime*, which is not the same thing as its being
 * in front, and that difference was a real defect: leaving Emufii for the home
 * screen left the rear panel lit on a process that merely happened to still be
 * alive. What decides now is [secondScreenWanted].
 */
@Composable
fun SecondScreenHost(enabled: Boolean) {
    val context = LocalContext.current
    val display by rememberPresentationDisplay()
    val foreground by AppForeground.visible.collectAsState()
    val model by SecondScreen.model.collectAsState()

    val wanted = secondScreenWanted(enabled, foreground, model)

    // The service light is only asked about while there is a panel to draw it
    // on. Polling a server for a dot nobody can see would be a handheld's
    // battery spent on nothing, and this is the app's only permanent request.
    LaunchedEffect(wanted) { if (wanted) VpsStatus.poll() }

    // Keyed on all of it: turning the setting off, leaving the app, or the panel
    // going away must take the window down the same way, and a new display gets
    // a new window rather than a reused one pointing at the old one.
    DisposableEffect(display, wanted) {
        val target = display
        if (!wanted || target == null) return@DisposableEffect onDispose {}

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
 * Whether the rear panel has anything to be lit for.
 *
 * Two answers, and the second one is the whole point of the feature:
 *
 * - Emufii in front: the panel mirrors what the player is doing, so it follows
 *   the app and goes down when they leave it. A panel still glowing on the back
 *   of a handheld whose owner has gone to their home screen is exactly the kind
 *   of thing that gets a feature switched off for good.
 * - A session running: it stays lit even though Emufii is behind the emulator,
 *   because that is when it earns its place. The code on the back of the console
 *   is what the other player reads, and it is needed precisely when the front
 *   screen has been given over to the game.
 *
 * Pure, so the rule can be read and tested without a second display.
 */
fun secondScreenWanted(
    enabled: Boolean,
    foreground: Boolean,
    model: SecondScreenModel
): Boolean = enabled && (foreground || model is SecondScreenModel.InSession)

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

        // `context` here is *not* the one handed to the constructor: that
        // parameter is not a `val`, so it is not in scope in a member function,
        // and the name resolves instead to the inherited [Dialog.getContext] —
        // the display context [Presentation] builds for itself. This compiles
        // without a word, which is why it went unnoticed.
        //
        // That display context is the right one for everything except language.
        // It is built by `createDisplayContext`, which starts again from the
        // *display's* configuration and so drops the per-app locale: the app
        // set to English kept a panel in French, the system language, because
        // the panel was reading a different set of resources from the front
        // screen. Keep the display's configuration, which is what makes the
        // window size and theme itself correctly, and put only the locales
        // back.
        val view = ComposeView(context.withAppLocales()).apply {
            setContent { SecondScreenSurface() }
        }
        owner.attachTo(view)
        setContentView(view)
    }

    /** Called by the host on the way out; a dismissed dialog does not do this itself. */
    fun release() = owner.detach()
}

/**
 * The same context, speaking the language the player chose for Emufii.
 *
 * The per-app locale is the platform's, set through [LocaleManager], so it is
 * read back from the platform rather than from the settings store: a change
 * made in Android's own settings screen counts too, and the store is not told
 * about those.
 *
 * Empty means the player never chose, and then the system language is the right
 * answer and the context is already correct — so it is handed back untouched
 * rather than pinned to whatever it resolves to today.
 */
private fun Context.withAppLocales(): Context {
    val locales = getSystemService(LocaleManager::class.java)
        ?.applicationLocales
        ?.takeIf { !it.isEmpty }
        ?: return this
    val config = Configuration(resources.configuration).apply { setLocales(locales) }
    return createConfigurationContext(config)
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
