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
 * Playing Emufii with the sticks and buttons, not the screen. Two jobs, separate
 * on purpose: [gamepadClick] makes a thing pressable from the pad, [focusRing]
 * makes it obvious which thing that is.
 * pourquoi : docs/decisions/navigation-manette.md § Two jobs, separated on purpose
 */

/**
 * How long the cursor takes to arrive. Everything marking the selected cell moves
 * on this one clock.
 * pourquoi : docs/decisions/navigation-manette.md § The cursor never lingers
 */
const val RING_IN_MS = 140

/** How long the ring takes to leave: not at all. Do not restore a fade here. */
private const val RING_OUT_MS = 0

/**
 * Face buttons that mean "do it". B is absent: it means back. Public because the
 * library grid handles its own keys and must recognise exactly these.
 */
val CONFIRM_KEYS = setOf(Key.ButtonA, Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

/**
 * Which axis the cursor's ring speaks: teal for play and system, coral on the
 * social zones. The colour of the cursor names the zone.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § GAMEPAD FOCUS
 */
enum class RingTone { TEAL, CORAL }

/**
 * Provide `RingTone.CORAL` around a social zone and every [controlRing] and
 * [focusRing] inside it that names no colour turns coral.
 */
val LocalRingTone = compositionLocalOf { RingTone.TEAL }

@Composable
fun ringColor(tone: RingTone = LocalRingTone.current, dark: Boolean = LocalEmufiiDarkTheme.current): Color =
    when (tone) {
        RingTone.TEAL -> if (dark) Teal.darkBright else Teal.bright
        RingTone.CORAL -> if (dark) Coral.darkBright else Coral.bright
    }

/**
 * Makes the element pressable from a controller. Sits next to a `clickable`,
 * never replaces it; both share the same [interactionSource] so the existing
 * press animation plays.
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
            // The pad path: `tap` covers the finger and the keys Compose knows
            // itself, never `ButtonA`.
            // pourquoi : docs/decisions/sons.md § The sound and the click are one call
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
 * The cursor: a lit contour and a wide coloured glow, animated in.
 * pourquoi : docs/decisions/navigation-manette.md § Three things at once, and the breath that was taken back
 */
@Composable
fun Modifier.focusRing(
    focused: Boolean,
    shape: Shape,
    /**
     * Teal, or coral via [LocalRingTone], unless the caller names one. Drawn by
     * hand rather than by Material, so the axis is fetched explicitly here.
     */
    color: Color = ringColor(),
    /**
     * The stroke's thickness and the glow's reach. Defaults are the tiles'
     * (150 dp wide); small controls pass reduced values.
     * pourquoi : docs/decisions/navigation-manette.md § The ring keeps the same weight everywhere
     */
    width: Dp = 4.dp,
    glowRadius: Dp = 28.dp,
    /**
     * How much of the control the neon band takes. Game tiles pass under the
     * default, where the band would take over the cover art it points at. No
     * effect on [FocusRingStyle.FLAT], whose thickness is [width] alone.
     */
    bandFraction: Float = 0.12f
): Modifier {
    // The cursor's sound lives here and nowhere else: this is the one point
    // everything carrying the cursor passes through, controls via `controlRing`
    // and grid tiles calling in directly with their own index.
    // pourquoi : docs/decisions/sons.md § Hover fires where the cursor is drawn
    var wasFocused by remember { mutableStateOf(focused) }
    if (focused != wasFocused) {
        wasFocused = focused
        if (focused) Sfx.hover()
    }

    // Out faster than in: an even spring leaves two selections lit at once.
    // pourquoi : docs/decisions/navigation-manette.md § The cursor never lingers
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
    // No breath, and do not add one: an animated `shadow` elevation shows through
    // a non-opaque surface and drifts inside the cursor.
    if (FOCUS_RING_STYLE == FocusRingStyle.NEON) {
        return this.neonFocusRing(
            focused = focused,
            shape = shape,
            // The gradient runs from the bright cut to the deep cut of the same
            // axis, so the cursor never invents a third colour.
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
        .shadow(
            elevation = glowRadius * glow,
            shape = shape,
            // Never clip. `shadow` defaults to `clip = elevation > 0`, so lighting
            // the ring cut the control to the ring's own shape: invisible on a
            // rectangle, but the profile avatar's pencil pill sits at the corner
            // of a square box, outside the circle, and half vanished under focus.
            // pourquoi : docs/decisions/navigation-manette.md § The ring surrounds, it does not clip
            clip = false,
            ambientColor = Color.Transparent,
            spotColor = color
        )
        .border(ring, color.copy(alpha = glow), shape)
}


/**
 * The two cursors the app can draw. [FLAT] is kept on purpose: a 4 dp stroke plus
 * the shadow bent into a halo, which holds on every ground and needs no
 * `BlurMaskFilter`. If the neon costs too much on a scrolling grid or reads
 * badly on light cover art, this constant is the whole switch.
 */
enum class FocusRingStyle { FLAT, NEON }

val FOCUS_RING_STYLE = FocusRingStyle.NEON

/**
 * The deep cut of the axis [bright] is the bright cut of. The band's gradient
 * needs both and the ring receives one colour, so the axis is recovered here
 * rather than threaded through forty callers.
 */
private fun deepCut(bright: Color): Color = when (bright) {
    Teal.bright, Teal.darkBright -> Teal.deep
    Coral.bright, Coral.darkBright -> Coral.deep
    // A colour named by a caller: darken it in place rather than fall back to an
    // axis that is not its own.
    else -> Color(
        red = bright.red * 0.72f,
        green = bright.green * 0.72f,
        blue = bright.blue * 0.72f,
        alpha = bright.alpha
    )
}

/**
 * Filled in by the scaffold, read by [controlRing]. Zero when there is no header.
 * pourquoi : docs/decisions/navigation-manette.md § Nothing must stop under the header
 */
val LocalScaffoldBand = compositionLocalOf { 0.dp }

/**
 * Named apart from [ActionShape] because the ring needs the number.
 * pourquoi : docs/decisions/navigation-manette.md § One radius, named once
 */
val ACTION_CORNER = 18.dp

val ActionShape = RoundedCornerShape(ACTION_CORNER)

/**
 * The standard ring on every ordinary control: [focusRing] with the control's own
 * shape. Reads focus itself, so place it before the `clickable` or `focusable`;
 * after, it sees nothing and stays dark.
 * pourquoi : docs/decisions/navigation-manette.md § The ring reads focus itself, and the order matters
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.controlRing(
    shape: Shape,
    /**
     * The tiles' stroke and glow. Do not shrink these.
     * pourquoi : docs/decisions/navigation-manette.md § The ring keeps the same weight everywhere
     */
    width: Dp = 4.dp,
    glowRadius: Dp = 28.dp,
    bandFraction: Float = 0.12f,
    scrollMargin: Dp = 28.dp,
    /** False to silence the ring while focus is inside it, as in a field being edited. */
    enabled: Boolean = true
): Modifier {
    var focused by remember { mutableStateOf(false) }
    var height by remember { mutableIntStateOf(0) }
    var widthPx by remember { mutableIntStateOf(0) }
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    // Top margin at least the header's height: Compose counts a control under the
    // header as visible, so asking for less never reaches the top.
    val top = with(LocalDensity.current) {
        maxOf(scrollMargin, LocalScaffoldBand.current).toPx()
    }
    val bottom = with(LocalDensity.current) { scrollMargin.toPx() }

    return this
        // The ring overflows, and between siblings the last drawn wins. Only
        // orders siblings: a multi-row grid raises its row too.
        // pourquoi : docs/decisions/navigation-manette.md § The selected control draws in front of its neighbours
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

@Composable
fun MutableInteractionSource.isFocused(): State<Boolean> = collectIsFocusedAsState()
