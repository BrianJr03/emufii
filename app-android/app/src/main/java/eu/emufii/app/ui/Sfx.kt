package eu.emufii.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.provider.Settings
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.semantics.Role
import eu.emufii.app.R

/**
 * Les deux sons de l'interface : le curseur qui se pose, et l'appui.
 *
 * `SoundPool` et non `MediaPlayer` : ces sons durent 96 et 144 ms et doivent
 * partir sans latence. Ils se rangent derriere le reglage de sons d'interface
 * d'Android plutot que d'en inventer un second.
 * pourquoi : docs/decisions/sons.md § Deux sons, une seule famille
 */
object Sfx {

    /**
     * Le contexte applicatif, retenu a la preparation.
     *
     * Il permet a [click] et [hover] de ne rien demander a l'appelant : les
     * points de passage du son ne sont pas tous des composables — la grille lit
     * la touche de confirmation dans une lambda ordinaire — et faire descendre
     * un `Context` jusque-la aurait touche cinq signatures pour un son.
     */
    private var app: Context? = null

    private var pool: SoundPool? = null
    private var hoverId = 0
    private var clickId = 0

    /** Ce qui est decode : jouer un identifiant pas encore pret ne fait rien. */
    private val loaded = mutableSetOf<Int>()

    fun prepare(context: Context) {
        if (pool != null) return
        app = context.applicationContext
        val attrs = AudioAttributes.Builder()
            // La famille des sons d'interface : suit le volume systeme, se tait
            // pendant un appel, et ne coupe pas la musique de quelqu'un.
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val p = SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(attrs).build()
        p.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded += id }
        hoverId = p.load(context.applicationContext, R.raw.sfx_hover, 1)
        clickId = p.load(context.applicationContext, R.raw.sfx_click, 1)
        pool = p
    }

    /** Le curseur vient de se poser sur autre chose. */
    fun hover() = play(hoverId, HOVER_VOLUME)

    /** Quelque chose a ete presse — a la manette comme au doigt. */
    fun click() = play(clickId, CLICK_VOLUME)

    private fun play(id: Int, volume: Float) {
        val context = app ?: return
        if (id == 0 || id !in loaded) return
        val on = runCatching {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SOUND_EFFECTS_ENABLED,
                1
            ) != 0
        }.getOrDefault(true)
        if (on) pool?.play(id, volume, volume, 1, 0, 1f)
    }

    /** Quatre : le curseur peut glisser pendant qu'un appui resonne encore. */
    private const val MAX_STREAMS = 4

    /**
     * Le survol est plus bas que l'appui : il part a chaque case traversee, et
     * un deplacement aussi fort qu'une action fait croire qu'il s'est passe
     * quelque chose.
     */
    private const val HOVER_VOLUME = 0.45f
    private const val CLICK_VOLUME = 0.85f
}

/**
 * L'action, rendue sonore.
 *
 * `Modifier.tap` couvre tout ce que l'app rend cliquable elle-meme, mais pas les
 * controles de Material, qui prennent leur `onClick` en parametre et ne passent
 * par aucun modificateur a nous : `Button`, `OutlinedButton`, `Surface(onClick)`.
 * Ceux-la etaient muets — la moitie des boutons secondaires de l'app, dont tous
 * les [GhostButton], puisque la pastille est un `Surface` cliquable.
 *
 * Un enrobage plutot qu'un `Sfx.click()` recopie dans chaque lambda : le son
 * appartient a l'appui, pas a ce que l'appui declenche, et une lambda existante
 * n'a pas a etre relue pour qu'on lui en ajoute un.
 * pourquoi : docs/decisions/sons.md § Le son et le clic sont un seul appel
 */
fun sounded(onClick: () -> Unit): () -> Unit = { Sfx.click(); onClick() }

/**
 * Cliquable **et sonore** : le remplacant de `Modifier.clickable` dans cette app.
 *
 * Un seul appel pour les deux, a dessein.
 * pourquoi : docs/decisions/sons.md § Le son et le clic sont un seul appel
 */
@Composable
fun Modifier.tap(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier {
    return this.clickable(enabled = enabled, role = role) { Sfx.click(); onClick() }
}

/** [tap], pour un controle qui fournit sa source d'interaction et son indication. */
@Composable
fun Modifier.tap(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier {
    return this.clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        role = role
    ) { Sfx.click(); onClick() }
}

/**
 * L'appui court et le maintien, tous deux sonores.
 * pourquoi : docs/decisions/sons.md § Le son et le clic sont un seul appel
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.tapOrHold(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    onLongClick: () -> Unit,
    onClick: () -> Unit
): Modifier {
    return this.combinedClickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onLongClick = { Sfx.click(); onLongClick() },
        onClick = { Sfx.click(); onClick() }
    )
}

/**
 * Coupe les sons d'interface d'Android **pour cette fenetre**.
 *
 * A poser a la racine de chaque fenetre : l'activite, chaque `Dialog`, et le
 * panneau arriere. Emufii a les siens, et les deux se superposaient.
 * pourquoi : docs/decisions/sons.md § Couper ceux d'Android se fait vue par vue
 */
@Composable
fun SilenceSystemSfx() {
    val view = LocalView.current
    SideEffect {
        // Toute la chaine jusqu'a la racine : `playSoundEffect` est gate par le
        // drapeau de la vue qui l'appelle, et ce n'est pas toujours celle de
        // Compose — la vue de decor d'un `Dialog` en est une autre.
        var v: View? = view
        while (v != null) {
            v.isSoundEffectsEnabled = false
            v = v.parent as? View
        }
    }
}
