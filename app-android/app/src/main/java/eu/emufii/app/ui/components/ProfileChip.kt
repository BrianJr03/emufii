package eu.emufii.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import eu.emufii.app.profile.Profile
import eu.emufii.app.profile.playerDisplayName
import eu.emufii.app.ui.focusRing
import eu.emufii.app.ui.theme.plate
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.LocalAccent
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.PlateDark
import eu.emufii.app.ui.theme.PlateLight
import eu.emufii.app.ui.tap

/**
 * The top bar's buttons all share one size, or the row reads as misaligned.
 * pourquoi : docs/decisions/direction-visuelle.md § Les pastilles de la barre du haut sont une famille
 */
private val CHIP_SIZE = 46.dp

/**
 * The floating pill the top-bar buttons are cut from.
 *
 * The dark fill is deliberately lighter than it looks like it should be, and
 * there is **no Material indication** here: its state layer also covers focus,
 * which a gamepad grants permanently, leaving a "disabled"-looking wash.
 * pourquoi : docs/decisions/direction-visuelle.md § Pas d'indication Material : une animation de pression
 */
@Composable
fun TopBarChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Appelee quand la pastille prend ou rend le curseur. C'est par la que le
     * panneau arriere apprend ce qui est vise : la barre du haut est la seule
     * couche de la bibliotheque a n'avoir rien a dire de son cote, et le
     * panneau y montrait le repos.
     * pourquoi : docs/decisions/second-ecran.md § Ce qui voyage jusqu'au panneau
     */
    onFocused: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val dark = LocalEmufiiDarkTheme.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip-scale"
    )

    // The grid hands back to the top bar when going up from the first row:
    // without a ring the cursor simply became invisible there and the screen had
    // to be touched to find out where you were.
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { onFocused(focused) }

    Box(
        modifier = modifier
            .size(CHIP_SIZE)
            .scale(scale)
            .focusRing(focused, CircleShape, width = 2.5.dp, glowRadius = 10.dp)
            .plate(
                shape = CircleShape,
                dark = dark,
                oled = LocalEmufiiOledTheme.current,
                lift = 5.dp,
                pressed = pressed
            )
            .tap(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * The profile in the top bar: **just the avatar**, never a nudge that would
 * change the chip's width with its state.
 * pourquoi : docs/decisions/direction-visuelle.md § Les pastilles de la barre du haut sont une famille
 */
@Composable
fun ProfileChip(
    profile: Profile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (Boolean) -> Unit = {}
) {
    TopBarChip(onClick = onClick, modifier = modifier, onFocused = onFocused) {
        Box(modifier = Modifier.padding(3.dp)) {
            Avatar(
                name = playerDisplayName(profile.name),
                imageFile = profile.avatarFile,
                size = 40.dp,
                ring = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.clip(CircleShape)
            )
        }
    }
}

/**
 * Friends, straight from the home screen: seeing who is online is something you
 * do *instead* of browsing, not a preference you adjust.
 * pourquoi : docs/decisions/direction-visuelle.md § Les pastilles de la barre du haut sont une famille
 */
@Composable
fun FriendsChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (Boolean) -> Unit = {}
) {
    TopBarChip(onClick = onClick, modifier = modifier, onFocused = onFocused) {
        // La silhouette du systeme d'icones, celle que l'etat vide du chercheur
        // porte deja.
        //
        // C'etaient deux avatars empiles, au motif que l'app dit « d'autres
        // joueurs » avec des disques et non avec un pictogramme. Le motif tenait
        // tant qu'aucune autre marque ne disait « joueur » ; depuis que le
        // chercheur en a une, deux dessins repondaient a la meme question — et
        // deux avatars vides a 23 dp se lisaient de loin comme une tache, pas
        // comme des gens.
        // pourquoi : docs/decisions/direction-visuelle.md § Les glyphes disent « d'autres joueurs » comme le reste de l'app
        PersonMark(size = 22.dp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Open games, in the same family of pills as friends and profile — all three
 * are navigation. Two linked *screens*, not two people: discs are people here.
 * pourquoi : docs/decisions/direction-visuelle.md § Les glyphes disent « d'autres joueurs » comme le reste de l'app
 */
@Composable
fun SessionsChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (Boolean) -> Unit = {}
) {
    val tint = MaterialTheme.colorScheme.onSurface
    TopBarChip(onClick = onClick, modifier = modifier, onFocused = onFocused) {
        Canvas(Modifier.size(23.dp)) {
            val w = size.width
            val h = size.height
            val screenW = w * 0.46f
            val screenH = h * 0.34f
            val stroke = Stroke(width = w * 0.10f)
            val radius = androidx.compose.ui.geometry.CornerRadius(w * 0.09f)

            // Offset diagonally: two consoles side by side would read as a
            // single object cut in half.
            drawRoundRect(
                color = tint,
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(screenW, screenH),
                cornerRadius = radius,
                style = stroke
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(w - screenW, h - screenH),
                size = androidx.compose.ui.geometry.Size(screenW, screenH),
                cornerRadius = radius,
                style = stroke
            )
            // The link, dotted: a session goes over the network, it is not a
            // cable between two devices sitting next to each other.
            drawLine(
                color = tint,
                start = Offset(screenW * 0.55f, screenH * 1.25f),
                end = Offset(w - screenW * 0.55f, h - screenH * 1.25f),
                strokeWidth = w * 0.10f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.02f, w * 0.13f))
            )
        }
    }
}

/**
 * Two **blank** avatars, overlapped like [AvatarStack]. Filling them with real
 * friends was dropped: the empty state is what most installs show.
 * pourquoi : docs/decisions/direction-visuelle.md § Les glyphes disent « d'autres joueurs » comme le reste de l'app
 */
@Composable
private fun FriendsAvatars(modifier: Modifier = Modifier) {
    val dark = LocalEmufiiDarkTheme.current
    val ring = if (dark) PlateDark else PlateLight

    // By offset from the centre, never corner alignment: the overlap is the
    // whole point of the shape.
    // pourquoi : docs/decisions/direction-visuelle.md § Les glyphes disent « d'autres joueurs » comme le reste de l'app
    Box(modifier = modifier.size(34.dp), contentAlignment = Alignment.Center) {
        // Behind: muted, up and to the right, so the two read as depth rather
        // than as two things of equal weight.
        //
        // **La profondeur vient de la valeur, plus de la temperature.**
        //
        // C'etait la seule palette parallele qui restait dans l'app : quatre hex
        // ecrits ici, avec leur propre paire clair/sombre, et gris-bleu froids
        // dans un monde dont tous les neutres sont chauds. Le contrat du projet
        // dit « plus aucun hex hors des fichiers de theme », et un accent qui
        // change ne les aurait jamais suivis.
        //
        // La teinte se prend donc a l'encre de second plan du theme, ramenee
        // vers la surface : on garde exactement ce que le froid cherchait a
        // obtenir — quelque chose de plus sourd que la pastille de devant — sans
        // introduire une temperature que rien d'autre ne parle.
        // pourquoi : docs/decisions/theme-duotone-shelves.md § Deux axes sémantiques
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        val ground = MaterialTheme.colorScheme.surfaceVariant
        Disc(
            colors = listOf(lerp(ground, muted, 0.52f), lerp(ground, muted, 0.34f)),
            ring = ring,
            modifier = Modifier.offset(x = 6.dp, y = (-4).dp)
        )
        // In front: the accent **in force**, not a hardcoded colour.
        // pourquoi : docs/decisions/direction-visuelle.md § Les glyphes disent « d'autres joueurs » comme le reste de l'app
        val accent = LocalAccent.current
        Disc(
            colors = listOf(accent.bright, accent.deep),
            ring = ring,
            modifier = Modifier.offset(x = (-6).dp, y = 4.dp)
        )
    }
}

@Composable
private fun Disc(colors: List<Color>, ring: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors))
            .border(BorderStroke(2.dp, ring), CircleShape)
    )
}
