package eu.emufii.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import eu.emufii.app.ui.LocalScaffoldBand
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.focusRing
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import eu.emufii.app.ui.wallpaper.TrayBackdrop
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.plate
import eu.emufii.app.ui.theme.PlateDark
import eu.emufii.app.ui.theme.PlateLightLow

/**
 * The two gamepad destinations of a scaffolded screen.
 *
 * [first] must be placed on a genuinely focusable control, **never** on a
 * container: a request on a `focusGroup` succeeds by focusing the group itself.
 * pourquoi : docs/decisions/coquille-ecrans.md § L'en-tête est déclaré avant le contenu, et dessiné par-dessus
 */
class ScaffoldFocus(val first: FocusRequester, val header: FocusRequester)

val LocalScaffoldFocus = compositionLocalOf<ScaffoldFocus?> { null }

/**
 * To be placed on a screen's first control: it becomes the "down" destination
 * from the header, and "up" from it goes back there.
 */
@Composable
fun Modifier.padEntry(): Modifier {
    val focus = LocalScaffoldFocus.current ?: return this
    return this
        .focusRequester(focus.first)
        .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                runCatching { focus.header.requestFocus() }
                true
            } else {
                false
            }
        }
}

/**
 * The shell every screen sits in: system insets, and one wallpaper and header
 * for all screens. The header floats; it is never a bar with a background.
 * pourquoi : docs/decisions/coquille-ecrans.md § L'en-tête flotte, et ce que ça coûte
 */
@Composable
fun EmufiiScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    /**
     * La marque du bouton de retour, quand ce n'est pas un chevron.
     *
     * Un ecran dont le retour **ferme quelque chose** plutot que de remonter
     * doit le dire avant qu'on presse : en session, ce bouton met fin a la
     * session, et un chevron promettait l'inverse.
     * pourquoi : docs/decisions/session.md § Le retour ferme la session, et il le dit
     */
    backIcon: (@Composable (Color) -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    /**
     * False when the screen fits whole: the veil and its 32 dp only exist for
     * content rising under the header — 7 % of the Thor's height otherwise.
     * pourquoi : docs/decisions/coquille-ecrans.md § L'en-tête flotte, et ce que ça coûte
     */
    contentScrolls: Boolean = true,
    content: @Composable (topPadding: Dp) -> Unit
) {
    val dark = LocalEmufiiDarkTheme.current
    val scaffoldFocus = remember { ScaffoldFocus(FocusRequester(), FocusRequester()) }
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Header height + its vertical padding (2 × 12) + the status bar.
    val band = statusBar + HEADER_HEIGHT + 24.dp

    Box(modifier = modifier.fillMaxSize()) {
        TrayBackdrop(modifier = Modifier.fillMaxSize(), dark = dark)

        /**
         * Declared before the content (traversal follows declaration order) and
         * drawn above it by `zIndex`. Three other ways to cross the boundary
         * were tried and all failed; name the destination instead.
         * pourquoi : docs/decisions/coquille-ecrans.md § L'en-tête est déclaré avant le contenu, et dessiné par-dessus
         */
        Row(
            modifier = Modifier
                .zIndex(1f)
                // The way down is named, as in the library. Automatic traversal
                // does not cross the boundary between these two layers of a
                // single Box, and none of the variants tried before, focus
                // properties, group, `moveFocus`, ever managed it.
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                        // Consumed only if the destination exists, or the
                        // cursor is trapped on a screen with no first control.
                        // pourquoi : docs/decisions/coquille-ecrans.md § L'en-tête est déclaré avant le contenu, et dessiné par-dessus
                        runCatching { scaffoldFocus.first.requestFocus() }.isSuccess
                    } else {
                        false
                    }
                }
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null) {
                CircleIconButton(
                    onClick = onBack,
                    modifier = Modifier.focusRequester(scaffoldFocus.header)
                ) { tint ->
                    if (backIcon != null) backIcon(tint)
                    else ChevronLeft(size = 20.dp, color = tint)
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                // Explicit: the header floats in a plain Box, not a Surface, so
                // there is nothing to inherit a colour from and Text falls back
                // to black, invisible on the dark wallpaper.
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }

        // Past the band *and* past the fade. The veil is still fully opaque at
        // the band's lower edge, so content parked exactly there is drawn under
        // an undiluted copy of the wallpaper: legible in light mode, all but
        // erased in dark, where the first list header read as a grey smudge.
        CompositionLocalProvider(
            LocalScaffoldFocus provides scaffoldFocus,
            // What the header covers: the cursor uses it so as never to stop
            // underneath while going back up.
            LocalScaffoldBand provides if (contentScrolls) band + FADE_HEIGHT else band
        ) {
            content(if (contentScrolls) band + FADE_HEIGHT else band)
        }

        if (contentScrolls) WallpaperVeil(band = band, dark = dark)
    }
}

/**
 * A second copy of the wallpaper, drawn over the content and erased except
 * where the floating chrome sits. [fromTop] false anchors it to the bottom.
 *
 * Put this *inside* the Haze source where one exists.
 * pourquoi : docs/decisions/coquille-ecrans.md § L'en-tête flotte, et ce que ça coûte
 */
@Composable
fun WallpaperVeil(
    band: Dp,
    dark: Boolean,
    modifier: Modifier = Modifier,
    fromTop: Boolean = true,
    fade: Dp = FADE_HEIGHT
) {
    TrayBackdrop(
        modifier = modifier
            .fillMaxSize()
            // The mask erases most of this copy, and DstIn only sees what the
            // layer holds, without an offscreen layer it would punch through
            // to the content below instead.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // Fully opaque across the whole band, and only then fading.
                // Starting the fade inside the band left a ghost line of text
                // sitting on the title's baseline.
                val solid = band.toPx() / size.height
                val clear = (band + fade).toPx() / size.height
                val stops = if (fromTop) {
                    arrayOf(
                        0f to Color.Black,
                        solid to Color.Black,
                        clear.coerceAtMost(1f) to Color.Transparent,
                        1f to Color.Transparent
                    )
                } else {
                    arrayOf(
                        0f to Color.Transparent,
                        (1f - clear).coerceAtLeast(0f) to Color.Transparent,
                        (1f - solid).coerceAtLeast(0f) to Color.Black,
                        1f to Color.Black
                    )
                }
                drawRect(
                    brush = Brush.verticalGradient(colorStops = stops),
                    blendMode = BlendMode.DstIn
                )
            },
        dark = dark
    )
}

/**
 * Round, moulded, floating over the tray, with a **drawn** glyph inside.
 * pourquoi : docs/decisions/coquille-ecrans.md § Le bouton rond est un disque moulé
 */
@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Color) -> Unit
) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    // Back is the first thing a gamepad looks for on a secondary screen, and it
    // showed nothing when the pad found it. Everything that takes focus must
    // show it: with no ring, the cursor vanishes and the screen passes for
    // frozen.
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    // The travel, on top of the material's own depression: on the two dark
    // themes a plate that only loses its shadow loses almost nothing, because
    // there was little shadow to lose. Shrinking it is what carries the press on
    // every theme, and it is what the tiles have always done.
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "circle-press"
    )
    Box(
        modifier = modifier
            .size(HEADER_HEIGHT)
            .scale(press)
            .focusRing(focused, CircleShape, width = 3.dp, glowRadius = 18.dp)
            .plate(shape = CircleShape, dark = dark, oled = oled, lift = 5.dp, pressed = pressed)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon(MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * The name of a group of rows, in the app's own voice: sentence case at body
 * weight, never a tracked uppercase eyebrow.
 * pourquoi : docs/decisions/coquille-ecrans.md § Le titre de groupe parle la voix de l'app
 */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 6.dp)
    )
}

/** Material's minimum touch target, and therefore a pill's height. */
private val TOUCH_TARGET = 48.dp

/** Compact affordance for a secondary action inside a card. */
@Composable
fun GhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    /**
     * True for a lone pill that takes the width of its card. Explicit, never
     * inferred: the frame is what receives the caller's modifier.
     * pourquoi : docs/decisions/coquille-ecrans.md § Le libellé est centré dans les deux sens, et les deux sont nécessaires
     */
    fillWidth: Boolean = false,
    /**
     * Drawn instead of the label, for buttons whose action is a symbol rather
     * than a word. [label] still travels with it, as the spoken name.
     */
    icon: (@Composable (Color) -> Unit)? = null,
    /**
     * Une marque posee **avant** le libelle, qui reste. A distinguer de [icon],
     * qui le remplace : celle-ci sert aux boutons qui menent quelque part et
     * dont la destination a un visage — un logo de service, jamais un
     * pictogramme decoratif.
     * pourquoi : docs/decisions/reglages-ecran.md § Les deux liens sortants, et leur ordre
     */
    leading: (@Composable () -> Unit)? = null
) {
    val accent = tint ?: MaterialTheme.colorScheme.primary
    // Every secondary action in the app goes through here, so this is the one
    // place that has to know about the pad for all of them, ring included.
    val shape = RoundedCornerShape(50)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    // Around the pill, never inside it, and the gap exists at all times —
    // appearing on selection would make a row of pills jump.
    // pourquoi : docs/decisions/coquille-ecrans.md § L'anneau entoure la pastille, il ne mord pas dedans
    Box(modifier = modifier.controlRing(shape), propagateMinConstraints = true) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = accent.copy(alpha = 0.12f),
        interactionSource = interaction,
        // The pill is the size of its touch area: `Surface(onClick)` reserves
        // 48 dp and draws its background smaller, which the ring followed.
        // pourquoi : docs/decisions/coquille-ecrans.md § La pastille fait la taille de sa cible tactile
        modifier = Modifier
            .heightIn(min = TOUCH_TARGET)
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
    ) {
        // Centred both ways: `textAlign` cannot do the vertical. And NO
        // `fillMaxWidth` here — it broke any row of two unweighted pills.
        // pourquoi : docs/decisions/coquille-ecrans.md § Le libellé est centré dans les deux sens, et les deux sont nécessaires
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                icon(accent)
            } else if (leading != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    leading()
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    }
}

/**
 * A row of overlapping avatars, the way a group of people is usually shown.
 * Beyond [max], the remainder becomes a "+n" chip.
 */
@Composable
fun AvatarStack(
    names: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    max: Int = 4
) {
    val dark = LocalEmufiiDarkTheme.current
    val ring = if (dark) Color(0xFF0E1116) else Color.White
    val shown = names.take(max)
    val extra = names.size - shown.size
    // Overlap has to come from an offset: a negative Spacer width isn't a
    // thing in Compose, and laying them out edge to edge reads as a list
    // rather than as a group.
    val overlap = size / 3

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        shown.forEachIndexed { i, name ->
            Avatar(
                name = name,
                size = size,
                ring = ring,
                modifier = Modifier
                    .offset(x = -overlap * i)
                    .zIndex((shown.size - i).toFloat())
            )
        }
        if (extra > 0) {
            Box(
                modifier = Modifier
                    .offset(x = -overlap * shown.size)
                    .size(size)
                    .clip(CircleShape)
                    .background(if (dark) PlateDark else PlateLightLow),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+$extra",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val HEADER_HEIGHT = 44.dp

/**
 * How far below the header the backdrop takes to become transparent again.
 * pourquoi : docs/decisions/coquille-ecrans.md § L'en-tête flotte, et ce que ça coûte
 */
private val FADE_HEIGHT = 32.dp
