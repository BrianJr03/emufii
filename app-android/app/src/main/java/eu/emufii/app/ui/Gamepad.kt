package eu.emufii.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.emufii.app.ui.theme.Coral
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.Teal

/**
 * Playing Emufii with the sticks and buttons, not the screen.
 *
 * Two jobs, separate on purpose: [gamepadClick] makes a thing pressable from
 * the pad, [focusRing] makes it obvious which thing that is.
 * pourquoi : docs/decisions/navigation-manette.md § Deux métiers séparés exprès
 */

/**
 * Face buttons that mean "do it". B is deliberately absent: it means back.
 *
 * Public because the library grid handles its own keys and must recognise
 * exactly these (see the CLAUDE.md rule on lazy grids).
 */
/** How long the ring takes to appear, then to fade out. */
/**
 * How long the cursor takes to arrive. Everything marking the selected cell
 * must move on this one clock.
 * pourquoi : docs/decisions/navigation-manette.md § Le curseur ne s'attarde jamais
 */
const val RING_IN_MS = 140

/**
 * How long the ring takes to leave: not at all. Do not restore a fade here.
 * pourquoi : docs/decisions/navigation-manette.md § Le curseur ne s'attarde jamais
 */
private const val RING_OUT_MS = 0

val CONFIRM_KEYS = setOf(Key.ButtonA, Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

/**
 * Which axis the cursor's ring speaks. DUOTONE SHELVES: the ring is **teal**
 * (play + system) by default and **coral** on the social zones — chips
 * session/amis, friends lists, join. The colour of the cursor says the zone.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § FOCUS MANETTE
 */
enum class RingTone { TEAL, CORAL }

/**
 * The zone the ring is in. Provide `RingTone.CORAL` around a social zone and
 * every [controlRing] / [focusRing] inside it (that does not name an explicit
 * colour) turns coral; nothing else changes — timings and glow included.
 */
val LocalRingTone = compositionLocalOf { RingTone.TEAL }

/** The ring's colour for the tone in force, dark-aware. */
@Composable
fun ringColor(tone: RingTone = LocalRingTone.current, dark: Boolean = LocalEmufiiDarkTheme.current): Color =
    when (tone) {
        RingTone.TEAL -> if (dark) Teal.darkBright else Teal.bright
        RingTone.CORAL -> if (dark) Coral.darkBright else Coral.bright
    }

/**
 * Makes the element focusable and pressable from a controller.
 *
 * Sits *next to* a `clickable`, never replaces it; both share the same
 * [interactionSource] so the existing press animation plays for a button press.
 */
fun Modifier.gamepadClick(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    /**
     * Whether to make the element focusable here. Off by default: `clickable`
     * already does it, and two focus targets in one chain means two d-pad stops
     * for one thing on screen.
     */
    focusable: Boolean = false,
    onClick: () -> Unit
): Modifier = this
    .onKeyEvent { event ->
        if (!enabled) return@onKeyEvent false
        if (event.type == KeyEventType.KeyUp && event.key in CONFIRM_KEYS) {
            // Le chemin manette : `tap` ne couvre que le doigt et les touches
            // que Compose reconnait lui-meme, pas `ButtonA`.
            // pourquoi : docs/decisions/sons.md § Le son et le clic sont un seul appel
            Sfx.click()
            onClick()
            true
        } else {
            // Swallow the matching key-down as well, or the platform delivers it
            // onwards and a single press reads as two.
            event.type == KeyEventType.KeyDown && event.key in CONFIRM_KEYS
        }
    }
    .then(if (focusable) Modifier.focusable(enabled = enabled, interactionSource = interactionSource) else Modifier)

/**
 * The cursor: a lit contour that says "this is where you are".
 *
 * A contour and a wide coloured glow, animated in. The breath was tried and
 * taken back out.
 * pourquoi : docs/decisions/navigation-manette.md § Trois choses à la fois, et le souffle qui a été repris
 */
@Composable
fun Modifier.focusRing(
    focused: Boolean,
    shape: Shape,
    /**
     * The ring's colour, teal (or coral via [LocalRingTone]) unless the caller
     * names one. Drawn by hand rather than by Material, so it is one of the two
     * places the axis is fetched explicitly.
     */
    color: Color = ringColor(),
    /**
     * The stroke's thickness and the glow's reach. Defaults are the tiles'
     * (150 dp wide); small controls pass reduced values.
     * pourquoi : docs/decisions/navigation-manette.md § L'anneau garde le même poids partout
     */
    width: Dp = 4.dp,
    glowRadius: Dp = 28.dp,
    /**
     * La part de la taille du contrôle que prend la bande du curseur néon.
     *
     * Les tuiles de jeu passent sous le défaut : la
     * grille est plus serrée que la sienne, et la bande y prenait le pas sur la
     * jaquette qu'elle désigne. Sans effet sur [FocusRingStyle.FLAT], dont
     * l'épaisseur est [width] et rien d'autre.
     */
    bandFraction: Float = 0.12f
): Modifier {
    // **Le son du curseur vit ici, et nulle part ailleurs.**
    //
    // C'est le seul point de passage de tout ce qui porte le curseur : les
    // controles par `controlRing`, et les tuiles de la grille qui appellent
    // ceci directement avec leur propre index.
    // pourquoi : docs/decisions/sons.md § Le survol se déclenche là où le curseur se dessine
    var wasFocused by remember { mutableStateOf(focused) }
    if (focused != wasFocused) {
        wasFocused = focused
        if (focused) Sfx.hover()
    }

    // Out faster than in: an even spring leaves two selections lit at once.
    // pourquoi : docs/decisions/navigation-manette.md § Le curseur ne s'attarde jamais
    val ring by animateDpAsState(
        targetValue = if (focused) width else 0.dp,
        animationSpec = tween(if (focused) RING_IN_MS else RING_OUT_MS),
        label = "focus-ring"
    )
    val glow by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(if (focused) RING_IN_MS else RING_OUT_MS),
        label = "focus-glow"
    )
    // No breath, and do not add one: an animated `shadow` elevation shows
    // through a non-opaque surface and drifts inside the cursor.
    // pourquoi : docs/decisions/navigation-manette.md § Trois choses à la fois, et le souffle qui a été repris
    if (FOCUS_RING_STYLE == FocusRingStyle.NEON) {
        return this.neonFocusRing(
            focused = focused,
            shape = shape,
            // Le degrade court de la coupe claire vers la coupe pleine du meme
            // axe : le curseur reste turquoise ou corail, il n'invente pas une
            // troisieme couleur. C'est la seule chose que nous ne reprenons pas
            // de la reference d'origine, qui laisse choisir un accent decoratif.
            start = color,
            end = deepCut(color),
            minBand = width,
            bandFraction = bandFraction,
            inMs = RING_IN_MS,
            outMs = RING_OUT_MS
        )
    }
    return this
        // Doubled on purpose: at 14 dp on white plates the colour barely showed.
        // pourquoi : docs/decisions/navigation-manette.md § Trois choses à la fois, et le souffle qui a été repris
        .shadow(
            elevation = glowRadius * glow,
            shape = shape,
            // **Ne rogne pas.** `shadow` fait defaut a `clip = elevation > 0`,
            // donc l'anneau, en s'allumant, decoupait le controle a sa propre
            // forme. Invisible sur un rectangle, fatal sur l'avatar du profil :
            // sa pastille crayon est posee au coin d'une boite carree, donc hors
            // du cercle de l'anneau, et elle disparaissait a moitie des que le
            // curseur arrivait dessus. Un anneau entoure, il ne taille pas.
            // pourquoi : docs/decisions/navigation-manette.md § L'anneau entoure, il ne rogne pas
            clip = false,
            ambientColor = Color.Transparent,
            spotColor = color
        )
        .border(ring, color.copy(alpha = glow), shape)
}


/**
 * Les deux curseurs que l'app sait dessiner.
 *
 * **L'ancien reste, et c'est deliberé.** [FLAT] est le trait plein de 4 dp plus
 * l'ombre detournee en halo, qui a servi depuis le debut : il est connu, il
 * tient sur tous les fonds, et il ne depend d'aucun `BlurMaskFilter`. Si le
 * neon coute trop cher sur une grille qui defile, ou s'il se lit mal sur une
 * jaquette claire, la bascule est cette constante et rien d'autre.
 */
enum class FocusRingStyle { FLAT, NEON }

/** Le curseur en vigueur. Passer a [FocusRingStyle.FLAT] restitue l'ancien. */
val FOCUS_RING_STYLE = FocusRingStyle.NEON

/**
 * La coupe pleine de l'axe dont [bright] est la coupe claire.
 *
 * Le degrade de la bande a besoin des deux, et l'anneau ne recoit qu'une
 * couleur. Plutot qu'un second parametre a passer aux quarante appelants, on
 * retrouve l'axe : les deux coupes vivent cote a cote dans le theme.
 */
private fun deepCut(bright: Color): Color = when (bright) {
    Teal.bright, Teal.darkBright -> Teal.deep
    Coral.bright, Coral.darkBright -> Coral.deep
    // Une couleur nommee par un appelant : la bande se fonce d'elle-meme
    // plutot que de retomber sur un axe qui n'est pas le sien.
    else -> Color(
        red = bright.red * 0.72f,
        green = bright.green * 0.72f,
        blue = bright.blue * 0.72f,
        alpha = bright.alpha
    )
}

/**
 * The height of the current screen's floating header, zero if there is none.
 * Filled in by the scaffold, read by [controlRing].
 * pourquoi : docs/decisions/navigation-manette.md § Rien ne doit s'arrêter sous l'en-tête
 */
val LocalScaffoldBand = compositionLocalOf { 0.dp }

/**
 * The radius of the large action buttons, shared so their ring is too.
 * pourquoi : docs/decisions/navigation-manette.md § Un rayon nommé une fois
 */
val ACTION_CORNER = 18.dp

/**
 * The shape of the large action buttons. Its radius is named separately above,
 * because the ring needs the number and not the shape.
 */
val ActionShape = RoundedCornerShape(ACTION_CORNER)

/**
 * The standard ring, the one on every ordinary control: exactly [focusRing],
 * with the control's own shape.
 *
 * Reads focus itself, so it must be placed **before** the `clickable` or the
 * `focusable` — placed after it sees nothing and stays dark. Brings the control
 * into view with a margin and at least the header's height.
 * pourquoi : docs/decisions/navigation-manette.md § L'anneau lit le focus lui-même, et l'ordre compte
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.controlRing(
    shape: Shape,
    /**
     * The tiles' stroke and glow, unchanged. Do not shrink these.
     * pourquoi : docs/decisions/navigation-manette.md § L'anneau garde le même poids partout
     */
    width: Dp = 4.dp,
    glowRadius: Dp = 28.dp,
    /** Voir [focusRing] : la part de la taille du controle que prend la bande. */
    bandFraction: Float = 0.12f,
    scrollMargin: Dp = 28.dp,
    /**
     * False to silence the ring even though focus is inside it, as for a text
     * field being edited, where the cursor has moved within.
     */
    enabled: Boolean = true
): Modifier {
    var focused by remember { mutableStateOf(false) }
    var height by remember { mutableIntStateOf(0) }
    var widthPx by remember { mutableIntStateOf(0) }
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    // Top margin at least the header's height: Compose counts a control under
    // the header as "visible", so asking for less never reaches the top.
    // pourquoi : docs/decisions/navigation-manette.md § Rien ne doit s'arrêter sous l'en-tête
    val top = with(LocalDensity.current) {
        maxOf(scrollMargin, LocalScaffoldBand.current).toPx()
    }
    val bottom = with(LocalDensity.current) { scrollMargin.toPx() }

    return this
        // Le controle vise passe devant ses voisins : l'anneau deborde, et
        // entre freres c'est le dernier dessine qui gagne. Ne classe qu'entre
        // freres — une grille a plusieurs rangs leve aussi son rang.
        // pourquoi : docs/decisions/navigation-manette.md § Le contrôle visé passe devant ses voisins
        .zIndex(if (focused && enabled) 1f else 0f)
        .bringIntoViewRequester(requester)
        .onSizeChanged { widthPx = it.width; height = it.height }
        .onFocusEvent { event ->
            focused = event.hasFocus
            if (event.hasFocus) {
                scope.launch {
                    runCatching {
                        requester.bringIntoView(
                            Rect(0f, -top, widthPx.toFloat(), height + bottom)
                        )
                    }
                }
            }
        }
        .focusRing(
            focused && enabled,
            shape,
            width = width,
            glowRadius = glowRadius,
            bandFraction = bandFraction
        )
}

/** Whether this source currently holds focus, for callers that want to read it once. */
@Composable
fun MutableInteractionSource.isFocused(): State<Boolean> = collectIsFocusedAsState()
