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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.emufii.app.ui.theme.LocalAccent

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
 * The cursor: a lit cyan contour that says "this is where you are".
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
     * The accent in force, not the tray's cyan. Drawn by hand rather than by
     * Material, so the accent is one of the two places fetched explicitly.
     */
    color: Color = LocalAccent.current.bright,
    /**
     * The stroke's thickness and the glow's reach. Defaults are the tiles'
     * (150 dp wide); small controls pass reduced values.
     * pourquoi : docs/decisions/navigation-manette.md § L'anneau garde le même poids partout
     */
    width: Dp = 4.dp,
    glowRadius: Dp = 28.dp
): Modifier {
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
    return this
        // Doubled on purpose: at 14 dp on white plates the colour barely showed.
        // pourquoi : docs/decisions/navigation-manette.md § Trois choses à la fois, et le souffle qui a été repris
        .shadow(
            elevation = glowRadius * glow,
            shape = shape,
            ambientColor = Color.Transparent,
            spotColor = color
        )
        .border(ring, color.copy(alpha = glow), shape)
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
        .focusRing(focused && enabled, shape, width = width, glowRadius = glowRadius)
}

/** Whether this source currently holds focus, for callers that want to read it once. */
@Composable
fun MutableInteractionSource.isFocused(): State<Boolean> = collectIsFocusedAsState()
