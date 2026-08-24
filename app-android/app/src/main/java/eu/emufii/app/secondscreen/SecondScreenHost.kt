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
 * Mounts the second screen while there is a reason to light it: a
 * [Presentation], the first of the two hosts the scout named.
 *
 * Bound to the activity's *lifetime*, which is not the same as its being in
 * front — hence [secondScreenWanted] deciding rather than the binding.
 * pourquoi : docs/decisions/second-ecran.md § Le panneau ne s'allume que s'il a une raison
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
 * Whether the rear panel has anything to be lit for: Emufii in front, or a
 * session running. Pure, so the rule is testable without a second display.
 * pourquoi : docs/decisions/second-ecran.md § Le panneau ne s'allume que s'il a une raison
 */
fun secondScreenWanted(
    enabled: Boolean,
    foreground: Boolean,
    model: SecondScreenModel
): Boolean = enabled && (foreground || model is SecondScreenModel.InSession)

/**
 * The window itself. It carries its own [SecondScreenWindowOwner] rather than
 * the activity's: the service host has no activity to borrow from.
 * pourquoi : docs/decisions/second-ecran.md § L'état du panneau vit à portée de processus, pas dans la composition
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

        // WARNING: `context` here is NOT the constructor parameter — a
        // non-`val` parameter is out of scope in a member function, so the name
        // resolves to the inherited `Dialog.getContext()`, which drops the
        // per-app locale. Keep its configuration, put only the locales back.
        // pourquoi : docs/decisions/second-ecran.md § La fenêtre : le contexte n'est pas celui qu'on croit
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
 * The same context, speaking the language the player chose. Read back from
 * [LocaleManager], so a change made in Android's own settings counts too;
 * empty is handed back untouched rather than pinned to today's answer.
 * pourquoi : docs/decisions/second-ecran.md § La langue vient de la fenêtre, pas du processus
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
 * The themed root, shared by every host. The theme is read from the store, not
 * inherited: the service host has no enclosing composition.
 * pourquoi : docs/decisions/second-ecran.md § L'état du panneau vit à portée de processus, pas dans la composition
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
