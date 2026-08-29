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

    // Le sondage n'est plus attache au panneau : la lampe est aussi dans la
    // barre de la bibliotheque depuis le 2026-08-28, et l'ecran principal ne
    // depend de rien qui vive derriere. Il est lance la ou la lampe est lue —
    // ici pour le panneau, dans la bibliotheque pour la barre — et le premier
    // arrive suffit, l'etat etant partage.

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

    /**
     * **Retour ne ferme pas le panneau.** Un `Presentation` est un `Dialog`, et
     * un `Dialog` qui reçoit retour se ferme — or la Thor livre la touche a
     * l'ecran qui a le focus, et le panneau est tactile : l'avoir touche une
     * fois suffisait donc a ce que A l'eteigne, jusqu'a la recomposition
     * suivante. Le panneau n'est pas une boite de dialogue qu'on annule ; sa
     * duree de vie est celle de l'ecran qu'il decrit (cf. [SecondScreenHost]).
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Deliberately empty.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // **Le panneau est tactile.** Il ne l'etait pas : la fenetre portait
        // `FLAG_NOT_TOUCHABLE` et `FLAG_NOT_FOCUSABLE` pour qu'elle ne vole
        // jamais un appui destine au jeu, ce qui en faisait un afficheur et
        // rien d'autre. La Thor arbitre le focus entre ses deux ecrans — une
        // pression donne le focus a l'ecran presse — donc l'appui destine au
        // jeu ne se perd pas, et le panneau peut porter des commandes.
        //
        // `FLAG_NOT_TOUCH_MODAL` reste : ce qui est presse **a cote** de la
        // fenetre continue d'aller a ce qu'il y a derriere, au lieu d'etre
        // avale par une fenetre plein ecran qui n'en voulait pas.
        // pourquoi : docs/decisions/second-ecran.md § Le panneau prend les etapes, parce qu'il est tactile
        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
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
    val published by SecondScreen.model.collectAsState()
    val aside by SecondScreen.aside.collectAsState()

    /**
     * La face de repos attend [IDLE_GRACE] : changer d'ecran de face n'est pas
     * atomique, et le panneau repassait par le logo entre les deux. Ici et non
     * dans [SecondScreen], qui n'a pas de portee de coroutine.
     * pourquoi : docs/decisions/second-ecran.md § La face de repos attend son tour
     */
    var model by remember { mutableStateOf(published) }
    LaunchedEffect(published, aside) {
        // Le sursis ne vaut que pour un repos **subi** : un écran est parti et
        // le suivant n'a pas encore parlé. Un repos **posé** — le curseur qui
        // quitte la grille pour l'en-tête, qui pose délibérément la face de
        // repos par-dessus — est déjà la réponse, et l'attendre faisait traîner
        // le panneau d'une demi-seconde derrière le curseur.
        if (published is SecondScreenModel.Idle &&
            model !is SecondScreenModel.Idle &&
            aside == null
        ) {
            delay(IDLE_GRACE_MS)
        }
        model = published
    }

    // Resolved against this window's own configuration, which is the second
    // display's: a player on the system theme should see both panels agree,
    // and hard-coding either value here would make them disagree at dusk.
    EmufiiTheme(
        darkTheme = theme.isDark(androidx.compose.foundation.isSystemInDarkTheme()),
        oled = theme.isOled
    ) {
        SecondScreenContent(model)
    }
}

/**
 * Le sursis avant que le panneau ne retombe au repos.
 *
 * Regle sur la seule chose qu'il doit couvrir : le temps qu'un ecran de face
 * se compose et pose son curseur. Mesure sur la Thor a moins de 100 ms entre
 * le `clear` des reglages et la face de la bibliotheque ; 400 laisse de la
 * marge sans qu'une vraie mise au repos se fasse attendre.
 */
private const val IDLE_GRACE_MS = 400L
