package eu.emufii.app.secondscreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import eu.emufii.app.R
import eu.emufii.app.artwork.rememberTileArt
import eu.emufii.app.library.Console
import eu.emufii.app.session.Session
import eu.emufii.app.ui.components.CompatBadge
import eu.emufii.app.ui.components.compatLabel
import eu.emufii.app.ui.theme.ArtworkShape
import eu.emufii.app.ui.theme.CardShape
import eu.emufii.app.ui.theme.LocalAccent
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.PillShape
import eu.emufii.app.ui.theme.TileShape
import eu.emufii.app.ui.theme.moldedRim
import eu.emufii.app.ui.theme.tilePlateBrush
import eu.emufii.app.ui.theme.plate
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.wallpaper.TrayBackdrop

/**
 * What the second panel draws, whoever is holding the window.
 *
 * The same tray as the front screen, never a second style: the engraved ground,
 * the moulded plates, the one accent, as the direction contract pins them
 * (`ui/theme/Direction.kt`). A panel with its own look would read as another
 * app running on the back of the machine.
 *
 * Read at arm's length, off-axis, under the player's hands. So one object leads
 * each face and everything else labels it, and the panel never holds more than
 * a glance's worth. It has no cursor and no controls: it reports.
 *
 * Colour follows the product principle rather than the chrome — the box art is
 * the only thing on the browsing face allowed to be loud, and it even lends its
 * own extracted tone to the shadow it casts.
 */
@Composable
fun SecondScreenContent(model: SecondScreenModel) {
    val dark = LocalEmufiiDarkTheme.current

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // This window has no wallpaper behind it, so the tray is painted rather
        // than shown through.
        TrayBackdrop(modifier = Modifier.fillMaxSize(), dark = dark)

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                // Room reserved at the bottom for the legend, so a long title
                // never has to decide whether it may overlap it.
                .padding(start = 36.dp, end = 36.dp, top = 30.dp, bottom = 58.dp)
        ) {
            when (model) {
                is SecondScreenModel.Idle -> Idle()
                is SecondScreenModel.Browsing -> Browsing(model)
                is SecondScreenModel.InSession -> InSession(model)
            }
        }

        Legend(
            legend = model.legend,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 18.dp)
        )
    }
}

@Composable
private fun Idle() {
    Text(
        stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineMedium,
        // Dimmed on purpose: at rest this panel is the least interesting thing
        // in the room, not a second logo competing with the front screen.
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * The game under the cursor: its box on the left, what we know of it on the right.
 *
 * The cover is the object and gets the weight — it is the one thing the player
 * recognises before reading anything. The column beside it answers the two
 * questions a box cannot: which machine, and whether this one actually plays
 * together.
 */
@Composable
private fun Browsing(model: SecondScreenModel.Browsing) {
    val rom = model.rom
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(30.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Cover(model, modifier = Modifier.width(196.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            ConsoleBadge(rom.console)
            Text(
                rom.displayName,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            model.rating?.let { rating ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    CompatBadge(rating)
                    Text(
                        compatLabel(rating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * The box, moulded onto the tray.
 *
 * Its shadow is tinted with the colour the artwork itself gave up
 * (`Rom.accentArgb`, already extracted for the front screen). That is the
 * content-colour rule taken literally: the tone is the game's, not the app's,
 * and it arrives as depth — a real offset shadow — rather than as a wash laid
 * over the chrome. Games with no extracted tone simply cast the tray's own
 * shadow, and nothing about the layout changes.
 */
@Composable
private fun Cover(model: SecondScreenModel.Browsing, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val art by rememberTileArt(model.rom)

    val tone = model.rom.accentArgb?.let { Color(it) }
    val shadow = tone ?: Color(0xFF0A1220)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            // The game's own tone, and it arrives as depth rather than as a
            // wash over the chrome: a real offset shadow, in the colour the
            // artwork gave up. A title with none casts the tray's own.
            .shadow(
                // On OLED the tray is truly off and a shadow draws nothing; the
                // edge and bevel below carry the separation alone.
                elevation = if (oled) 0.dp else 20.dp,
                shape = TileShape,
                clip = false,
                ambientColor = shadow.copy(alpha = if (dark) 0.55f else 0.30f),
                spotColor = shadow.copy(alpha = if (dark) 0.75f else 0.42f)
            )
            // The frame itself: a moulded plate, and the artwork sits *in* it.
            //
            // A rim alone was not enough here, and the arithmetic says why. The
            // contour is a 1.5.dp stroke centred on the outline, so the clip
            // eats its outer half and 1.73px survive, at 24% opacity, across a
            // 452px cover — 0.38% of the width, half the presence it has on a
            // grid tile, which is why it reads on the front screen and vanishes
            // on this one. Scaling the stroke instead would have been a second
            // rule for one place. A plate with the picture inset is the frame
            // this world already owns: face, edge and lit bevel, at a size the
            // eye can find from across a room.
            .plate(TileShape, dark = dark, oled = oled, lift = 0.dp)
            .padding(9.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ArtworkShape)
                // White under the artwork in daylight, as the tiles do: box art
                // of any colour has to read against something, and the tray is
                // not it.
                .background(tilePlateBrush(dark, oled))
                // The picture keeps its own contour inside the frame, drawn over
                // the image the way a tile does it.
                .moldedRim(ArtworkShape, dark = dark, oled = oled)
        ) {
            val cover = art.model
            if (cover != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(cover).build(),
                    contentDescription = null,
                    contentScale = if (art.isPixelArt) ContentScale.Fit else ContentScale.Crop,
                    filterQuality = if (art.isPixelArt) FilterQuality.None else FilterQuality.High,
                    placeholder = ColorPainter(Color.Transparent),
                    error = ColorPainter(Color.Transparent),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // No art: an empty slot rather than a broken picture, which is
                // the shape this tray already uses for nothing.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().socket(ArtworkShape, dark)
                ) {
                    Text(
                        model.rom.console.shortLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** The machine, engraved into the tray: a label on the shell, not an object on it. */
@Composable
private fun ConsoleBadge(console: Console) {
    val dark = LocalEmufiiDarkTheme.current
    Text(
        console.label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .socket(PillShape, dark)
            .padding(horizontal = 13.dp, vertical = 5.dp)
    )
}

/**
 * A session is up, and the code is the whole point.
 *
 * It carries no label above it. The six characters in the app's accent, on the
 * one plate lifted off the tray, are already the only thing on the panel that
 * could be read out to somebody — naming them would be a costume of importance,
 * which this app does not wear.
 */
@Composable
private fun InSession(model: SecondScreenModel.InSession) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val accent = LocalAccent.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        model.console?.let { ConsoleBadge(it) }

        // Ten of lift rather than the usual four: this is the one object on the
        // screen and it has to read as such from across a room.
        Box(
            modifier = Modifier
                .plate(CardShape, dark = dark, oled = oled, lift = 10.dp)
                .padding(horizontal = 44.dp, vertical = 22.dp)
        ) {
            Text(
                model.code,
                fontSize = 80.sp,
                lineHeight = 86.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                color = accent.bright,
                textAlign = TextAlign.Center
            )
        }

        // The two numbers the emulator's dialog asks for, engraved into the
        // tray rather than plated: they are a reference to read off, not an
        // object to reach for. Recessed side by side because they are typed
        // together, and a player copying one at a time has to come back.
        if (model.hostAddress != null || model.port != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                model.hostAddress?.let { Fact(stringResource(R.string.session_host_address), it) }
                model.port?.let { Fact(stringResource(R.string.session_port), it) }
            }
        }

        model.gameTitle?.let { title ->
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** One reference value in a recess: what it is, then what it says. */
@Composable
private fun Fact(label: String, value: String) {
    val dark = LocalEmufiiDarkTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier
            .socket(RoundedCornerShape(14.dp), dark)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * The button legend, in the two bottom corners.
 *
 * Left is where you leave from, right is where you act: the arrangement of
 * every console shell the player already owns. An empty side takes no room, so
 * a face with nothing to say on the left leaves no hole.
 */
@Composable
private fun Legend(legend: PadLegend, modifier: Modifier = Modifier) {
    if (legend.isEmpty) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cluster(legend.left)
        Cluster(legend.right)
    }
}

@Composable
private fun Cluster(hints: List<PadHint>) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        hints.forEach { hint ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                KeyCap(hint)
                Text(
                    stringResource(hint.label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * One button, moulded like the machine's own.
 *
 * A plate and not a recess: this is the picture of a thing that sticks out and
 * can be pressed, and the tray's sockets are for holes. Flush, at 2.dp of lift,
 * because a legend is a diagram and must not compete with what it labels.
 */
@Composable
private fun KeyCap(hint: PadHint) {
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current
    val tint = MaterialTheme.colorScheme.onSurface
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(26.dp)
            .plate(
                PillShape,
                dark = dark,
                oled = oled,
                lift = 2.dp,
                // A hint about holding shows a held button: the plate loses its
                // lift and its lit edge and takes the shadow's shade, which is
                // what "pushed in" is made of here. It is the same letter as the
                // press above it, because it is the same button.
                pressed = hint.held
            )
    ) {
        val glyph = hint.glyph
        if (glyph == null) DPadGlyph(tint) else CapLetter(glyph, tint)
    }
}

/**
 * The letter, centred on its own ink.
 *
 * Three separate things pushed it off centre, and laying the text out could
 * only ever fix one of them.
 *
 * **Left.** `labelLarge` carries `letterSpacing = 0.1.sp`, and Compose adds that
 * space *after* the last character as well as between characters. On a
 * one-letter string the measured width is the glyph plus one trailing gap, so
 * centring the measurement leaves the ink sitting left.
 *
 * **Down.** `labelLarge` sets `lineHeight` 18.sp over `fontSize` 14.sp. Trimming
 * that leading still leaves a box running ascent to descent, while a capital
 * with no descender fills only baseline to cap height. Centring that box is not
 * centring the letter, and no amount of trimming makes the two the same.
 *
 * **Right, once the first two were fixed.** Centring on the advance width is
 * still not centring the ink: a glyph's side bearings differ, so B in this face
 * sits measurably right of its own advance centre. Measured offline against
 * `rounded_bold.ttf` at this exact size, since a second display cannot be
 * screenshotted.
 *
 * So the glyph is drawn, and placed from [android.graphics.Paint.getTextBounds],
 * the smallest rectangle enclosing the ink. Put the pen at
 * `w/2 - (left + right)/2` and the ink centre lands on the cap centre by
 * construction, on both axes, for any glyph and any type scale.
 */
@Composable
private fun CapLetter(glyph: String, tint: Color) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val paint = remember(context, tint, density) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = runCatching { ResourcesCompat.getFont(context, R.font.rounded_bold) }
                .getOrNull() ?: android.graphics.Typeface.DEFAULT_BOLD
            textSize = with(density) { 14.sp.toPx() }
            // LEFT, not CENTER: the pen is positioned from the ink bounds below,
            // and CENTER would subtract half an advance on top of it.
            textAlign = android.graphics.Paint.Align.LEFT
            color = tint.toArgb()
        }
    }
    val bounds = remember(paint, glyph) {
        android.graphics.Rect().also { paint.getTextBounds(glyph, 0, glyph.length, it) }
    }
    Canvas(Modifier.size(26.dp)) {
        drawContext.canvas.nativeCanvas.drawText(
            glyph,
            size.width / 2f - (bounds.left + bounds.right) / 2f,
            size.height / 2f - (bounds.top + bounds.bottom) / 2f,
            paint
        )
    }
}

/**
 * The d-pad, drawn rather than typed.
 *
 * Drawn because a character would arrive from whatever font happens to carry
 * it, and a cap is 26.dp wide: a fallback face's weight and baseline both show
 * at that size. The system says icons are drawn, never characters.
 */
@Composable
private fun DPadGlyph(tint: Color) {
    Canvas(Modifier.size(10.dp)) {
        // Arms a touch wider than a third, the proportion a moulded d-pad
        // actually has: thinner reads as a mathematical plus.
        val arm = size.width * 0.38f
        val radius = CornerRadius(size.width * 0.06f, size.width * 0.06f)
        // Two bars crossing, each centred: the overlap is the hub, so the cross
        // has no seam and needs no third shape.
        drawRoundRect(
            color = tint,
            topLeft = Offset((size.width - arm) / 2f, 0f),
            size = Size(arm, size.height),
            cornerRadius = radius
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, (size.height - arm) / 2f),
            size = Size(size.width, arm),
            cornerRadius = radius
        )
    }
}
