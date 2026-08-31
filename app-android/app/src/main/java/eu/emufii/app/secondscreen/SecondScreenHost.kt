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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.emufii.app.notify.AppForeground
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.ui.theme.EmufiiTheme

/**
 * Mounts the second screen while there is a reason to light it, as a [Presentation].
 * pourquoi : docs/decisions/second-ecran.md § The panel only lights up if it has a reason
 */
@Composable
fun SecondScreenHost(enabled: Boolean) {
    val context = LocalContext.current
    val display by rememberPresentationDisplay()
    val foreground by AppForeground.visible.collectAsState()
    val model by SecondScreen.model.collectAsState()

    val wanted = secondScreenWanted(enabled, foreground, model)

    // The poll is no longer tied to the panel: the lamp is in the library's bar too.

    // Keyed on all of it: setting off, app left, or panel gone must take the window
    // down alike.
    DisposableEffect(display, wanted) {
        val target = display
        if (!wanted || target == null) return@DisposableEffect onDispose {}

        val presentation = EmufiiPresentation(context, target)
        // A display can go dark between being listed and being shown.
        val shown = runCatching { presentation.show() }.isSuccess

        onDispose {
            if (shown) runCatching { presentation.dismiss() }
            presentation.release()
        }
    }
}

/**
 * Emufii in front, or a session running. Pure, so the rule is testable.
 * pourquoi : docs/decisions/second-ecran.md § The panel only lights up if it has a reason
 */
fun secondScreenWanted(
    enabled: Boolean,
    foreground: Boolean,
    model: SecondScreenModel
): Boolean = enabled && (foreground || model is SecondScreenModel.InSession)

/**
 * Carries its own [SecondScreenWindowOwner]: the service host has no activity to borrow
 * from.
 * pourquoi : docs/decisions/second-ecran.md § The panel's state lives process-wide, not in the composition
 */
private class EmufiiPresentation(
    context: Context,
    display: Display,
) : Presentation(context, display) {

    private val owner = SecondScreenWindowOwner()

    /**
     * Back does not close the panel: a `Presentation` is a `Dialog`, and a `Dialog`
     * closes on back.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The panel is touchable. It was not: the window carried `FLAG_NOT_TOUCHABLE`
        // and `FLAG_NOT_FOCUSABLE`.
        // pourquoi : docs/decisions/second-ecran.md § The panel takes the steps, because it is touch
        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        }

        // `context` here is not the constructor parameter: a non-`val` parameter is out
        // of scope in a member function.
        // pourquoi : docs/decisions/second-ecran.md § The window: the context is not the one you think
        val view = ComposeView(context.withAppLocales()).apply {
            setContent { SecondScreenSurface() }
        }
        owner.attachTo(view)
        setContentView(view)
    }

    fun release() = owner.detach()
}

/**
 * Read back from [LocaleManager], so an Android-side change reaches this window.
 * pourquoi : docs/decisions/second-ecran.md § The language comes from the window, not from the process
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
 * The theme is read from the store, not inherited: the service host has no enclosing
 * one.
 * pourquoi : docs/decisions/second-ecran.md § The panel's state lives process-wide, not in the composition
 */
@Composable
private fun SecondScreenSurface() {
    val context = LocalContext.current
    val settings = remember(context) { SettingsStore.get(context) }
    val theme by settings.theme.collectAsState()
    val published by SecondScreen.model.collectAsState()
    val aside by SecondScreen.aside.collectAsState()

    /**
     * The resting face waits [IDLE_GRACE]: switching front screens is not atomic.
     * pourquoi : docs/decisions/second-ecran.md § The resting face waits its turn
     */
    var model by remember { mutableStateOf(published) }
    LaunchedEffect(published, aside) {
        // The grace covers a rest that is suffered, one screen gone before the next
        // speaks, never one laid deliberately.
        if (published is SecondScreenModel.Idle &&
            model !is SecondScreenModel.Idle &&
            aside == null
        ) {
            delay(IDLE_GRACE_MS)
        }
        model = published
    }

    // Against this window's configuration, which is the second display's.
    EmufiiTheme(
        darkTheme = theme.isDark(androidx.compose.foundation.isSystemInDarkTheme()),
        oled = theme.isOled
    ) {
        SecondScreenContent(model)
    }
}

/** Set on the only thing it must cover: the time one screen takes to replace another. */
private const val IDLE_GRACE_MS = 400L
