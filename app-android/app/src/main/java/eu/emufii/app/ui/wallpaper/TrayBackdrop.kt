package eu.emufii.app.ui.wallpaper

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Density
import eu.emufii.app.ui.rememberSlowMillis
import eu.emufii.app.ui.theme.Coral
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.Shelf
import eu.emufii.app.ui.theme.ShellDark
import eu.emufii.app.ui.theme.ShellDarkLow
import eu.emufii.app.ui.theme.ShellLight
import eu.emufii.app.ui.theme.ShellLightLow
import eu.emufii.app.ui.theme.ShellOled
import eu.emufii.app.ui.theme.Teal
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * Le plateau sur lequel tout est posé : deux étagères, leurs ondes, une vignette.
 *
 * **Le budget de mouvement est le sujet de ce fichier** : ce qui est immobile est
 * cuit dans un bitmap, et aucune lueur ne passe par un flou gaussien. Le lustre
 * qui le traversait a été retiré le 2026-08-29 — ne pas le réintroduire.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § MATIÈRE (fond)
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Le lustre est parti
 */
@Composable
fun TrayBackdrop(
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    // Lu dans le thème plutôt que passé : les six appelants passent déjà `dark`,
    // et leur faire porter un second drapeau qu'aucun ne calcule lui-même
    // n'aurait ajouté que des occasions d'en oublier un.
    oled: Boolean = LocalEmufiiOledTheme.current,
    /**
     * Faux pour un plateau qui ne bouge pas. Plus aucun appelant ne le passe.
     * pourquoi : docs/decisions/second-ecran.md § Le panneau arrière s'anime, finalement
     */
    animated: Boolean = true
) {
    // En cycles de fond, depuis l'horloge que tout le monde partage.
    val time = (if (animated) rememberSlowMillis() else FROZEN_MS) / CYCLE_MS

    /**
     * Le plateau immobile, **cuit dans une image**.
     *
     * Il a d'abord été enregistré dans une `Picture`, ce qui évitait de
     * reconstruire les dégradés à chaque image — et ne changeait rien, mesuré :
     * une `Picture` rejoue les ordres de dessin, donc elle **re-rastérise**.
     * Or ce qui coûte ici est justement le remplissage : quatre dégradés
     * radiaux ou linéaires étalés sur 1920 × 1080, chacun calculé pixel par
     * pixel, à chaque image.
     *
     * Cuit une fois dans un bitmap, tout cela devient une seule recopie de
     * texture — l'opération la moins chère qu'un GPU connaisse. À **demi
     * résolution**, parce qu'un dégradé n'a aucun détail à perdre et que
     * l'agrandissement ne se voit pas : quatre fois moins de pixels à calculer,
     * et 2 Mo au lieu de 8.
     */
    val still = remember { mutableStateOf<ImageBitmap?>(null) }
    var baked by remember { mutableStateOf<StillKey?>(null) }

    Canvas(modifier = modifier.graphicsLayer()) {
        val top = if (oled) ShellOled else if (dark) ShellDark else ShellLight
        val bottom = if (oled) ShellOled else if (dark) ShellDarkLow else ShellLightLow
        val geometry = TrayGeometry(size)

        val key = StillKey(size.width, size.height, dark, oled)
        if (baked != key && size.width >= 2f && size.height >= 2f) {
            val w = (size.width * STILL_SCALE).toInt().coerceAtLeast(1)
            val h = (size.height * STILL_SCALE).toInt().coerceAtLeast(1)
            val bitmap = ImageBitmap(w, h)
            CanvasDrawScope().draw(
                // La densité suit la réduction : sans ça le contour de 2 dp des
                // étagères serait tracé à sa taille en pixels puis agrandi, donc
                // deux fois trop épais.
                density = Density(density * STILL_SCALE, fontScale),
                layoutDirection = LayoutDirection.Ltr,
                canvas = androidx.compose.ui.graphics.Canvas(bitmap),
                size = Size(w.toFloat(), h.toFloat())
            ) { drawStillTray(TrayGeometry(Size(w.toFloat(), h.toFloat())), dark, oled, top, bottom) }
            still.value = bitmap
            baked = key
        }
        still.value?.let {
            drawImage(
                it,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.Low
            )
        }

        drawWaves(geometry, time, dark, oled)
    }
}

/**
 * La réduction de l'image cuite.
 *
 * Un demi de côté, donc un quart des pixels. Le plateau n'est fait que de
 * dégradés larges et d'un contour de 2 dp : le seul détail qui pourrait souffrir
 * est ce contour, et à cette échelle l'agrandissement le rend indiscernable d'un
 * tracé plein.
 */
private const val STILL_SCALE = 0.5f

/** Ce qui périme l'image cuite : la taille et le thème, rien d'autre. */
private data class StillKey(
    val width: Float,
    val height: Float,
    val dark: Boolean,
    val oled: Boolean,
)

/**
 * Où sont les deux étagères, calculé une fois par image et partagé entre le
 * plateau figé et les ondes — sans quoi les deux dériveraient au premier
 * réglage changé d'un seul côté.
 */
private class TrayGeometry(size: Size) {
    /** Assez grand pour sortir par deux bords de chaque étagère. */
    val side = 0.58f * max(size.width, size.height)
    val radius = CornerRadius(side * 0.30f, side * 0.30f)

    val coral = shelfRect(size.width * 0.02f, size.height * -0.06f)
    val teal = shelfRect(size.width * 0.98f, size.height * 1.06f)

    /** Le coin qui regarde le milieu de l'écran : le motif, et la source des ondes. */
    val coralCorner = Offset(coral.right, coral.bottom)
    val tealCorner = Offset(teal.left, teal.top)

    private fun shelfRect(cx: Float, cy: Float) = RoundRect(
        left = cx - side / 2f,
        top = cy - side / 2f,
        right = cx + side / 2f,
        bottom = cy + side / 2f,
        cornerRadius = radius
    )
}

/**
 * Le plateau immobile : le fond, les deux étagères et la vignette.
 *
 * **Rien ici ne lit l'horloge.** Ce qui est dessiné dans cette fonction est
 * enregistré une fois : ce qui devrait bouger et qu'on y poserait par
 * distraction gèlerait sans qu'aucune erreur ne le dise.
 */
private fun DrawScope.drawStillTray(
    geometry: TrayGeometry,
    dark: Boolean,
    oled: Boolean,
    top: Color,
    bottom: Color,
) {
    drawRect(brush = Brush.verticalGradient(listOf(top, bottom)))

    val fill = when {
        oled -> Shelf.fillOled
        dark -> Shelf.fillDark
        else -> Shelf.fillLight
    }
    val stroke = when {
        oled -> Shelf.edgeOled
        dark -> Shelf.edgeDark
        else -> Shelf.edgeLight
    }
    val glowAlpha = when {
        oled -> 0.075f
        dark -> 0.110f
        else -> 0.130f
    }

    fun shelf(
        rect: RoundRect,
        corner: Offset,
        axisBright: Color,
        axisDeep: Color,
        ground: Color,
    ) {
        val path = Path().apply { addRoundRect(rect) }
        // Une paire verticale dans la tuile : clair à son bord haut, profond à
        // son pied. Un aplat la faisait paraître imprimée dessus ; la paire lui
        // donne un corps sans lui donner un reflet qu'elle n'a pas mérité.
        drawPath(
            path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    axisBright.copy(alpha = fill),
                    axisDeep.copy(alpha = fill * 0.72f)
                ),
                startY = rect.top,
                endY = rect.bottom
            )
        )
        drawPath(path, color = axisBright.copy(alpha = stroke), style = Stroke(width = 2.dp.toPx()))

        // **Ce qui dissout les longs côtés, et pourquoi le coin n'y touche pas.**
        //
        // D'une étagère on voit trois choses : le coin qui regarde le milieu —
        // le motif — et les deux longs côtés qui filent hors de l'écran. Sur
        // crème, la vignette et le grain les avalent. Sur du noir absolu, non :
        // ils deviennent deux droites franches, et une droite franche est ce que
        // l'œil trouve en premier quand tout le reste est noir. On repeint donc
        // le fond par-dessus la tuile en s'éloignant du coin : près du coin rien,
        // loin tout. Le motif garde son arête là où il la veut.
        if (dark || oled) {
            drawPath(
                path,
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.52f to Color.Transparent,
                        1f to ground
                    ),
                    center = corner,
                    radius = geometry.side * 1.10f
                )
            )
        }

        // La lueur du coin. L'étagère est un aplat à arête nette — c'est ce
        // qu'on lui demande — mais son coin s'arrêtait sur rien. Le halo lui
        // donne de quoi finir, et l'arête garde son tranchant puisqu'il est plus
        // clair qu'elle.
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to axisBright.copy(alpha = glowAlpha),
                    0.45f to axisBright.copy(alpha = glowAlpha * 0.45f),
                    1f to Color.Transparent
                ),
                center = corner,
                radius = geometry.side * 0.82f
            ),
            radius = geometry.side * 0.82f,
            center = corner
        )
    }

    shelf(
        geometry.coral,
        geometry.coralCorner,
        if (dark) Coral.darkBright else Coral.bright,
        Coral.deep,
        top
    )
    shelf(
        geometry.teal,
        geometry.tealCorner,
        if (dark) Teal.darkBright else Teal.bright,
        Teal.deep,
        bottom
    )

    // La vignette assoit le plateau dans sa coque : sans elle les halos filent
    // par les quatre bords et l'écran se lit comme un échantillon plutôt que
    // comme une surface qui a des bords.
    if (!oled) {
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = if (dark) 0.32f else 0.14f)
                ),
                center = Offset(size.width * 0.5f, size.height * 0.42f),
                radius = max(size.width, size.height) * 0.78f
            ),
            size = Size(size.width, size.height)
        )
    }
}

/**
 * Les ondes que les deux étagères émettent vers le milieu.
 *
 * La même forme, gonflée, répétée à trois distances. Gonfler le carré arrondi
 * plutôt que tracer des cercles : une onde circulaire partant d'un coin carré se
 * lit comme un objet étranger posé dessus, un contour parallèle se lit comme
 * quelque chose que l'étagère *fait*. C'est aussi la seule façon de garder la
 * grammaire du logo dans un mouvement.
 *
 * Elles s'éteignent en chemin, donc aucune n'atteint jamais un bord : une onde
 * qui disparaît au bord de l'écran serait une ligne qui sort, pas une onde qui
 * s'éteint.
 */
private fun DrawScope.drawWaves(
    geometry: TrayGeometry,
    time: Double,
    dark: Boolean,
    oled: Boolean,
) {
    val waveAlpha = when {
        oled -> 0.055f
        dark -> 0.085f
        else -> 0.100f
    }

    fun waves(rect: RoundRect, axisBright: Color) {
        repeat(WAVES) { i ->
            val p = ((time * WAVE_SPEED) + i.toDouble() / WAVES).mod(1.0).toFloat()
            // Nulle au départ et à l'arrivée, pleine au milieu du trajet.
            val alpha = waveAlpha * sin(p * PI).toFloat()
            if (alpha <= 0.002f) return@repeat
            val reach = p * geometry.side * 0.75f
            val ripple = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = rect.left - reach,
                        top = rect.top - reach,
                        right = rect.right + reach,
                        bottom = rect.bottom + reach,
                        cornerRadius = CornerRadius(
                            geometry.radius.x + reach,
                            geometry.radius.y + reach
                        )
                    )
                )
            }
            // Trois traits concentriques, du plus large et pâle au plus fin et
            // dense : c'est le profil d'un flou, échantillonné en trois points,
            // et le GPU le trace sans repasser par le processeur.
            for ((width, share) in WAVE_HALO) {
                drawPath(
                    ripple,
                    color = axisBright.copy(alpha = alpha * share),
                    style = Stroke(width = width.toPx())
                )
            }
        }
    }

    waves(geometry.coral, if (dark) Coral.darkBright else Coral.bright)
    waves(geometry.teal, if (dark) Teal.darkBright else Teal.bright)
}

/** Combien d'ondes chaque étagère tient en vol. */
private const val WAVES = 2

/**
 * Leur vitesse, en cycles de [CYCLE_MS] : environ trente-cinq secondes par onde.
 * Assez lent pour qu'on ne puisse pas la suivre du regard, ce qui est la
 * condition pour qu'un fond reste un fond.
 */
private const val WAVE_SPEED = 0.55

/** Le trait net de l'onde : il porte la forme. */
private val WAVE_STROKE = 2.5.dp

/**
 * Le halo, en traits empilés : largeur, puis part de l'opacité. Du plus large et
 * pâle au plus fin et dense. Le dernier est le trait de l'onde elle-même.
 */
private val WAVE_HALO: List<Pair<Dp, Float>> = listOf(
    13.dp to 0.22f,
    WAVE_STROKE to 1.0f,
)

/** La durée d'un cycle de fond, à vitesse 1. */
private const val CYCLE_MS = 19_000

/**
 * L'instant où un plateau immobile se fige : les ondes à mi-chemin. Pas zéro,
 * où elles naissent — la seule composition qui ne montre rien.
 */
private const val FROZEN_MS = 8_000.0
