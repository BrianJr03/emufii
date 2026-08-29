package eu.emufii.app.ui

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Le curseur « tube de néon » : une bande autour du contrôle, sa lueur derrière,
 * et deux liserés blancs qui lui donnent son épaisseur.
 *
 * Toutes les mesures sont des fractions de la largeur de bande, elle-même une
 * fraction de la taille du contrôle. L'ancien anneau reste à une ligne de
 * distance — voir [FocusRingStyle].
 * pourquoi : docs/decisions/navigation-manette.md § Les quatre couches du curseur néon
 */
@Composable
fun Modifier.neonFocusRing(
    focused: Boolean,
    shape: Shape,
    start: Color,
    end: Color,
    /** L'épaisseur minimale de la bande : ce que les petits contrôles gardent. */
    minBand: Dp,
    /**
     * La part de la taille du contrôle que prend la bande.
     *
     * 0,12 convient à des icônes qui flottent dans une grille aérée. Une
     * jaquette de bibliothèque en demande un peu moins : nos tuiles sont plus
     * serrées, et le tube y devenait le sujet de la case.
     */
    bandFraction: Float = 0.12f,
    /** Le plafond, pour qu'une grande tuile ne se retrouve pas dans un tube. */
    maxBand: Dp = 24.dp,
    inMs: Int,
    outMs: Int,
): Modifier {
    val density = LocalDensity.current
    // La bande naît et meurt en s'épaississant, comme l'ancien anneau : c'est
    // le geste que l'app a toujours eu, et il n'y a aucune raison d'en changer
    // en changeant de matière.
    val grow by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(if (focused) inMs else outMs),
        label = "neon-ring"
    )
    // **Sortie avant l'animation, et c'est le point.** L'anneau est pose sur
    // chaque controle de l'ecran, et une transition infinie creee au-dessus de
    // cette ligne tournait sur les quarante d'un coup — quarante invalidations
    // par image pour un seul curseur visible. Sous la ligne, une seule vit a la
    // fois, celle du controle qui porte le curseur. Elle repart de zero a
    // chaque arrivee, ce qui est aussi ce qu'on veut voir.
    if (grow <= 0f) return this

    /**
     * Le pas de phase, et **il ne change que vingt-cinq fois par seconde**.
     *
     * Une `InfiniteTransition` ordinaire ecrit sa valeur a chaque image : sur un
     * ecran a 120 Hz, l'app redessinait donc 120 fois par seconde pour une
     * animation qui n'a que quarante-cinq positions distinctes en 1,8 s. Le
     * curseur etant a l'ecran en permanence, c'etait un rendu permanent a
     * pleine cadence — la moitie de la chaleur mesuree venait de la.
     *
     * L'horloge n'ecrit donc que quand le **pas** change. Entre deux pas, rien
     * n'est invalide et l'app ne redessine pas. C'est la meme regle que le fond,
     * pour la meme raison : la cadence se choisit sur ce que le mouvement
     * montre, pas sur ce que l'ecran sait faire.
     */
    val step = rememberFlowStep()

    val paints = remember { NeonPaints() }
    return this.drawWithCache {
        val band = bandWidth(size, bandFraction, minBand, maxBand, density) * grow
        val radius = cornerRadiusOf(shape, size, density)
        // Le liseré : 16 % de la bande, borné. Sous 1,5 dp il disparaît, au
        // dela de 4 il cesse d'etre un liseré et devient une seconde bande.
        val hair = (0.16f * band).coerceIn(1.5f * density.density, 4f * density.density)
        // La lueur : 70 % de la bande. Le plafond de 14 dp est ce qui l'empêche
        // de devenir un brouillard sur les grandes tuiles.
        val blur = (0.7f * band).coerceIn(4f * density.density, 14f * density.density)

        val w = size.width
        val h = size.height
        // **La bande entoure le contrôle, elle ne mord pas dedans.** C'est ce
        // qu'il faut faire, et l'essayer autrement l'a montré : posée vers
        // l'intérieur sur une tuile de 150 dp, elle recouvre 18 dp de jaquette
        // sur chaque bord — le curseur mange ce qu'il désigne.
        //
        // Ce qui l'empêchait de sortir n'était pas la géométrie mais un
        // `shadow` place avant elle dans la chaîne : il rogne par defaut tout
        // ce qui se dessine ensuite. Les deux tuiles concernées disent
        // maintenant `clip = false`, comme l'anneau le fait deja pour la sienne.
        // pourquoi : docs/decisions/navigation-manette.md § L'anneau entoure, il ne rogne pas
        val rOuter = radius + band
        val rInner = radius

        // Les chemins ne dependent que de la taille et de l'epaisseur.
        //
        // **Un seul rectangle arrondi par chemin, jamais deux** : Skia
        // rasterise tout le reste sur le processeur, et l'epaisseur s'anime.
        // pourquoi : docs/decisions/navigation-manette.md § Un seul rectangle arrondi par chemin, jamais deux
        val midline = Path().apply {
            val half = band / 2f
            val r = (rOuter + rInner) / 2f
            addRoundRect(RectF(-half, -half, w + half, h + half), r, r, Path.Direction.CW)
        }
        val outerEdge = Path().apply {
            val i = -band + hair / 2f
            val r = (rOuter - hair / 2f).coerceAtLeast(0f)
            addRoundRect(RectF(i, i, w - i, h - i), r, r, Path.Direction.CW)
        }
        val innerEdge = Path().apply {
            val i = -hair / 2f
            val r = (rInner + hair / 2f).coerceAtLeast(0f)
            addRoundRect(RectF(i, i, w - i, h - i), r, r, Path.Direction.CW)
        }

        // **La lueur se fait en traits empiles, pas au flou.**
        //
        // `BlurMaskFilter` n'a pas d'equivalent sur le GPU : Android dessine le
        // chemin sur le processeur, dans une image intermediaire, a chaque
        // image. Le curseur est sur l'ecran en permanence, donc c'etait un
        // rendu logiciel permanent. Trois traits concentriques donnent le meme
        // degrade de bord et se tracent en materiel.
        val halo = listOf(
            (band + blur * 1.6f) to 0.16f,
            (band + blur * 0.8f) to 0.30f,
            band to 0.55f,
        )

        // Le repli, quand la forme est trop petite pour qu'un tour se lise :
        // le dégradé vertical fixe, qui est la forme non animée de la bande.
        val plain = LinearGradient(
            0f, -band, 0f, h + band,
            start.copy(alpha = start.alpha * grow).toArgb(),
            end.copy(alpha = end.alpha * grow).toArgb(),
            Shader.TileMode.CLAMP
        )
        paints.outer.strokeWidth = hair
        paints.outer.shader = LinearGradient(
            0f, 0f, 0f, h,
            Color.White.copy(alpha = 0.50f * grow).toArgb(),
            Color.White.copy(alpha = 0.30f * grow).toArgb(),
            Shader.TileMode.CLAMP
        )
        paints.inner.strokeWidth = hair
        paints.inner.color = Color.White.copy(alpha = 0.40f * grow).toArgb()

        onDrawWithContent {
            drawContent()
            // La phase est lue ici : c'est ce qui fait couler le dégradé sans
            // rien recomposer. Le bitmap correspondant est calculé une fois par
            // pas, puis retrouvé dans le cache.
            paints.band.shader = CursorFlow.shader(
                w = w + 2f * band,
                h = h + 2f * band,
                band = band,
                radius = (rOuter + rInner) / 2f,
                start = start.copy(alpha = start.alpha * grow),
                end = end.copy(alpha = end.alpha * grow),
                step = step
            ) ?: plain
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                for ((width, share) in halo) {
                    paints.glow.strokeWidth = width
                    paints.glow.color =
                        start.copy(alpha = start.alpha * grow * share).toArgb()
                    native.drawPath(midline, paints.glow)
                }
                paints.band.strokeWidth = band
                native.drawPath(midline, paints.band)
                native.drawPath(outerEdge, paints.outer)
                native.drawPath(innerEdge, paints.inner)
            }
        }
    }
}

/**
 * L'épaisseur de la bande : 12 % du plus petit côté, bornée.
 *
 * C'est cette fraction qui fait qu'une tuile de
 * bibliothèque porte un tube et qu'une pastille de barre porte un filet, sans
 * qu'aucun appelant ait à le dire.
 */
private fun bandWidth(size: Size, fraction: Float, min: Dp, max: Dp, density: Density): Float {
    val floor = with(density) { min.toPx() }
    val ceiling = with(density) { max.toPx() }
    return (fraction * minOf(size.width, size.height)).coerceIn(floor, ceiling)
}

/**
 * Le rayon du contrôle, lu dans sa forme.
 *
 * La bande doit épouser ce qu'elle entoure : un rayon deviné donnerait un
 * anneau carré autour d'un bouton rond. Les formes que l'app dessine sont
 * toutes des [RoundedCornerShape] ; pour les autres, le contour est calculé au
 * plus près par la forme elle-même.
 */
private fun cornerRadiusOf(shape: Shape, size: Size, density: Density): Float =
    if (shape is RoundedCornerShape) {
        shape.topStart.toPx(size, density)
    } else {
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
        when (outline) {
            is androidx.compose.ui.graphics.Outline.Rounded ->
                outline.roundRect.topLeftCornerRadius.x
            else -> 0f
        }
    }

/** Les quatre pinceaux, gardés d'une image à l'autre : en allouer coûte plus que dessiner. */
private class NeonPaints {
    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    /** Un trait, pas un remplissage : son epaisseur suit l'animation d'arrivee. */
    val band = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
}
