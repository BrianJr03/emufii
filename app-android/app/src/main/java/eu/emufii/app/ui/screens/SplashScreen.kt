package eu.emufii.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import eu.emufii.app.R
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.secondscreen.SecondScreen
import eu.emufii.app.secondscreen.SecondScreenModel
import eu.emufii.app.ui.wallpaper.TrayBackdrop
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * The opening screen: the logo, for as long as the library takes to load.
 *
 * [MIN_MS] evite le clignotement quand le cache est chaud, [MAX_MS] cede quand
 * le parcours s'eternise. Non focalisable : rien a viser, donc rien a signaler.
 * pourquoi : docs/decisions/lancement-et-navigation.md § L'écran d'ouverture tient le logo entre deux durées
 */
@Composable
fun SplashScreen(ready: Boolean, onDone: () -> Unit) {
    val dark = LocalEmufiiDarkTheme.current

    // **Le panneau porte le repos tant que le logo est la.**
    //
    // La bibliotheque est composee **dessous** pendant tout le demarrage — c'est
    // meme le point du logo opaque : tout est mesure et peint quand il s'efface.
    // Son curseur se pose donc sur une tuile et publie la fiche du jeu, que le
    // panneau montrait pendant que l'ecran de face en etait encore a son logo.
    // Les deux ecrans racontaient deux moments differents de l'app.
    //
    // Face **posee par-dessus** plutot que publiee : la bibliotheque garde la
    // sienne intacte dessous, et elle reapparait a la seconde ou le logo part,
    // sans que personne ait a la republier.
    // pourquoi : docs/decisions/second-ecran.md § Une pile plutôt qu'une publication de plus
    DisposableEffect(Unit) {
        val token = SecondScreen.putAside(SecondScreenModel.Idle)
        onDispose { SecondScreen.takeBack(token) }
    }

    TrayBackdrop(modifier = Modifier.fillMaxSize(), dark = dark)

    // The minimum runs from the first frame, alongside the scan; it is a floor,
    // not a wait that adds on top.
    var floorPassed by remember { mutableStateOf(false) }
    var expired by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(MIN_MS)
        floorPassed = true
        delay(MAX_MS - MIN_MS)
        expired = true
    }
    LaunchedEffect(ready, floorPassed, expired) {
        if ((ready && floorPassed) || expired) onDone()
    }

    // The logo arrives from very slightly too small. A fade on its own reads as
    // an image slow to load; the scale, even at 6 %, makes it *enter*.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appearance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "splash-appearance"
    )

    // Le logo est centre seul : empiles, c'est la paire qui se centrait. Et la
    // barre d'etat est cachee le temps du logo, puis rendue.
    // pourquoi : docs/decisions/lancement-et-navigation.md § Le logo est centré seul, et la barre d'état n'existe pas
    val view = LocalView.current
    DisposableEffect(view) {
        val controller = WindowCompat.getInsetsController(
            (view.context as android.app.Activity).window, view
        )
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.emufii_logo_v3),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .widthIn(max = LOGO_SIZE)
                .fillMaxWidth()
                .alpha(appearance)
                .scale(0.94f + 0.06f * appearance)
        )
        SignalPips(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = LOGO_SIZE * LOGO_ASPECT / 2 + BAR_GAP)
                .alpha(appearance)
        )
    }
}

/**
 * The loading indicator: a row of LEDs in the tray, and a charge running
 * through them.
 *
 * The first frame anyone sees teaches the app's world, so the indicator is
 * built from that world's own parts. A `CircularProgressIndicator` carries
 * Material's colours and reads as a control; a gradient-filled rectangle reads
 * as a web page. Five pips in the tray's own sockets read as what they are on
 * a console: lights that say "working", in the logo's own pink running to its
 * own blue.
 *
 * The charge moves back and forth with a bell-shaped falloff, so each pip
 * lights as the charge passes and dims behind it — never a hard edge, never a
 * seam to hide at the turnaround.
 */
@Composable
private fun SignalPips(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "splash-pips")
    val travel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash-charge"
    )

    val dark = LocalEmufiiDarkTheme.current
    val shape = CircleShape

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PIP_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(PIP_COUNT) { index ->
            val stop = index / (PIP_COUNT - 1).toFloat()
            val tint = lerp(LogoPink, LogoBlue, stop)
            // One and a half pip-pitches of reach: neighbours catch the light,
            // the far end stays dark.
            val distance = abs(travel - stop) * (PIP_COUNT - 1)
            val charge = (1f - distance / 1.5f).coerceIn(0f, 1f)
            Pip(tint = tint, charge = charge, shape = shape, dark = dark)
        }
    }
}

/** One LED: a socket in the tray, a core that lights, and a halo when lit. */
@Composable
private fun Pip(tint: Color, charge: Float, shape: Shape, dark: Boolean) {
    Box(
        modifier = Modifier.size(PIP_SIZE).socket(shape, dark),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            // The unlit pip stays visible as a deep ember of its own colour,
            // so the row reads as five lights and not four gaps plus a light.
            drawCircle(color = tint.copy(alpha = 0.16f + 0.14f * charge), radius = radius)
            if (charge > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = 0.45f * charge), Color.Transparent),
                        center = center,
                        radius = radius * 2.4f
                    ),
                    radius = radius * 2.4f
                )
                drawCircle(color = tint.copy(alpha = 0.84f * charge), radius = radius)
            }
        }
    }
}

/**
 * The LED row's two ends: resampled from `emufii_logo_v3.png` onto the theme's
 * own axes — the coral tile top-left, the teal tile bottom-right. The first
 * frame anyone sees teaches the app's duotone.
 */
private val LogoPink = eu.emufii.app.ui.theme.Coral.bright
private val LogoBlue = eu.emufii.app.ui.theme.Teal.bright

private val LOGO_SIZE = 232.dp

/**
 * The logo's height over its width, `1 / 1`: the V3 mark is a square icon. It
 * serves to know where the logo ends so the pips can be hooked below it, which
 * the layout can no longer say now that the two are not stacked.
 */
private const val LOGO_ASPECT = 1f

/**
 * L'ecart entre le bas du logo et la rangee de temoins.
 *
 * A 40 dp la rangee flottait : la marque V3 est un carre dont la masse est
 * basse, donc le bas de sa boite de mise en page est loin sous le dernier pixel
 * qu'on en voit, et les 40 dp s'ajoutaient a ce vide-la. Les temoins se lisaient
 * comme un second element pose sur l'ecran plutot que comme le socle du logo.
 * Mesure sur la Thor : il fallait les remonter d'environ 35 px, soit 15 dp.
 */
private val BAR_GAP = 25.dp
private val PIP_COUNT = 5
private val PIP_SIZE = 12.dp
private val PIP_GAP = 14.dp

/** The floor: below it, the opening reads as a flicker. */
private const val MIN_MS = 4000L

/** The ceiling: past it, the library fills up better in plain sight. */
private const val MAX_MS = 12000L
