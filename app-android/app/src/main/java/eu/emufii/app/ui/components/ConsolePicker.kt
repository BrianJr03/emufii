package eu.emufii.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import eu.emufii.app.R
import eu.emufii.app.library.Console
import eu.emufii.app.library.EmulatorInfo
import eu.emufii.app.library.allEmulators
import eu.emufii.app.ui.controlRing
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.plate
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.theme.TileShape

/**
 * The consoles and the emulators that play them, as tiles. The tile carries the icon
 * and the version and is the control: who plays this, and do I want it, in one glance
 * and one press.
 * pourquoi : docs/decisions/reglages-ecran.md § A console carries a row, not a tile
 */
@Composable
fun ConsoleGrid(
    hidden: Set<Console>,
    onSetVisible: (Console, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * True when this grid is its page's first control, the first tile becoming the
     * pad's named destination. The naming must land on a really focusable control,
     * never on a container.
     * pourquoi : docs/decisions/coquille-ecrans.md § The header is declared before the content, and drawn over it
     */
    firstTileIsEntry: Boolean = false,
    /**
     * True for the short form: no version number, a smaller icon, a shorter tile,
     * enough to hold seven consoles on one line.
     * pourquoi : docs/decisions/reglages-ecran.md § The console tile has a short version
     */
    compact: Boolean = false
) {
    val context = LocalContext.current
    // Read once: a row costs a package query and an icon rasterisation, and the
    // answer cannot change without the player leaving to install something,
    // which recreates this anyway.
    val emulators = remember { allEmulators(context) }

    // Counted on the width actually given to the grid, never the screen's: the settings
    // page is 90 dp narrower, and "GameCube" came out as "GameCu".
    // pourquoi : docs/decisions/reglages-ecran.md § How many tiles per line, and the width that decides it
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val minTile = if (compact) MIN_TILE_COMPACT else MIN_TILE
        val fits = ((maxWidth + GRID_GAP) / (minTile + GRID_GAP))
            .toInt()
            .coerceIn(3, emulators.size)
        val columns = balancedColumns(emulators.size, fits)

        // The row holding the cursor passes in front: `controlRing`'s `zIndex` only
        // orders siblings, so not rows.
        // pourquoi : docs/decisions/navigation-manette.md § The selected control draws in front of its neighbours
                var focusedRow by remember { mutableStateOf(-1) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(GRID_GAP)
        ) {
            emulators.chunked(columns).forEachIndexed { rowIndex, row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GRID_GAP),
                    modifier = Modifier.zIndex(if (rowIndex == focusedRow) 1f else 0f)
                ) {
                    row.forEachIndexed { index, info ->
                        ConsoleTile(
                            info = info,
                            visible = info.console !in hidden,
                            onToggle = { onSetVisible(info.console, info.console in hidden) },
                            entry = firstTileIsEntry && rowIndex == 0 && index == 0,
                            compact = compact,
                            onFocused = { if (it) focusedRow = rowIndex },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // The last row is not filled: a tile drawn at the end of a console
                    // grid reads as a console. The place is held, not painted.
                    // pourquoi : docs/decisions/reglages-ecran.md § The console grid is not allowed an orphan
                    repeat(columns - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * How many columns, once we know how many fit: the count that best fills the last row,
 * never the maximum, which leaves an orphan.
 * pourquoi : docs/decisions/reglages-ecran.md § The console grid is not allowed an orphan
 * pourquoi : docs/decisions/reglages-ecran.md § How many tiles per line, and the width that decides it
 */
internal fun balancedColumns(count: Int, fits: Int): Int {
    if (count <= fits) return count
    var best = fits
    var bestGap = Int.MAX_VALUE
    for (c in fits downTo 3) {
        val gap = (c - count % c) % c
        if (gap < bestGap) {
            best = c
            bestGap = gap
        }
    }
    return best
}

/** The narrowest a tile can be and still spell out its console and version. */
private val MIN_TILE = 118.dp

private val GRID_GAP = 8.dp

@Composable
private fun ConsoleTile(
    info: EmulatorInfo,
    visible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    entry: Boolean = false,
    compact: Boolean = false,
    /** Tells the row it holds the cursor, so it passes in front of the others. */
    onFocused: (Boolean) -> Unit = {}
) {
    // Off is dimmed, not greyed out and not removed: the tile still has to say
    // which console it is, because turning one back on is the other half of the
    // gesture and a blank square gives nothing to aim at.
    val alpha = if (visible) 1f else 0.45f

    /**
     * The icon loses its colour when the console is off: desaturated rather than
     * veiled, or the tile's loudest thing said on while the rest said otherwise.
     * pourquoi : docs/decisions/reglages-ecran.md § A switched-off console is a hole in the board
     */
    val iconFilter = remember(visible) {
        if (visible) null
        else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }
    val dark = LocalEmufiiDarkTheme.current
    val oled = LocalEmufiiOledTheme.current

    Column(
        modifier = modifier
            .height(if (compact) TILE_HEIGHT_COMPACT else TILE_HEIGHT)
            .onFocusEvent { onFocused(it.hasFocus) }
            // Before the `clickable`: a `focusRequester` placed after no longer targets
            // the focus node the clickable just created, and the request fails
            // silently.
            .then(if (entry) Modifier.padEntry() else Modifier)
            // The ring before the clip, always. After, its glow is cut to the tile's
            // shape and fills it with a hard-edged wash instead of spilling out.
            .controlRing(TILE_SHAPE)
            // On it is a plate, off it is a hole: the tray already knows how to say
            // laid on and carved into.
            // pourquoi : docs/decisions/reglages-ecran.md § A switched-off console is a hole in the board
            .then(
                if (visible) Modifier.plate(shape = TILE_SHAPE, dark = dark, oled = oled, lift = 5.dp)
                else Modifier.socket(TILE_SHAPE, dark)
            )
            .clickable { onToggle() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 32.dp else 40.dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (info.icon != null) {
                Image(
                    bitmap = info.icon,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = iconFilter,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // The console's abbreviation rather than a question mark: an absent
                // emulator is the ordinary case on a new device, and the tile must
                // still name its machine.
                Text(
                    info.console.shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            info.console.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            maxLines = 1,
            // Three narrow columns and names we do not choose: this is where the
            // default hard clip bit deepest.
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            // The emulator's name, on its own line, never translated: it is a product.
            // A tile saying only "Switch" left the page unable to answer the question
            // it asks, which is what to install.
            info.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (!compact) {
            Text(
                // The version alone, never "Installed, version x": the sentence does
                // not fit a tile, and the number is the part that is read.
                info.version?.let { stringResource(R.string.emulators_version_short, shortVersion(it)) }
                    ?: if (info.installed) stringResource(R.string.emulators_installed_unknown)
                    else stringResource(R.string.emulators_absent_short),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * A fixed tile height: the last row's empty slots must match it, and an intrinsic
 * height is not shared between siblings.
 * pourquoi : docs/decisions/reglages-ecran.md § How many tiles per line, and the width that decides it
 */
private val TILE_HEIGHT = 124.dp

/** The short form's height, set so seven tiles fit on one line. */
private val TILE_HEIGHT_COMPACT = 92.dp

/** A short tile's minimum width: "GameCube" still fits. */
private val MIN_TILE_COMPACT = 92.dp

/**
 * The tile's corner, matching the library's own: the theme's one squircle
 * radius, not a private copy of it.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § SHAPES
 */
private val TILE_SHAPE = TileShape

/**
 * The version as it fits on a tile, cut at display and not at the source,
 * because PPSSPP already carries its `v` and the other five do not.
 * pourquoi : docs/decisions/reglages-ecran.md § An emulator's version is trimmed at display, not at the source
 */
private fun shortVersion(version: String): String =
    version.removePrefix("v").removePrefix("V")
