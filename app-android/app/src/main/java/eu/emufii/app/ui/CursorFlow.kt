package eu.emufii.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import eu.emufii.app.ui.rememberSlowMillis
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Le dégradé qui coule autour du curseur.
 *
 * **Ce n'est pas un balayage angulaire** : chaque pixel est ramené à sa position
 * en abscisse curviligne sur le périmètre, donc la couleur avance à la même
 * vitesse sur un bord droit et dans un coin. Rendu en bitmap étiré, mis en cache.
 * pourquoi : docs/decisions/performance-rendu.md § Le dégradé du curseur n'est pas un balayage angulaire
 */
internal object CursorFlow {

    /**
     * Le nombre de positions distinctes dans un tour.
     *
     * **27 pas**, soit quinze par seconde. Le chiffre ne se choisit pas sur ce
     * que l'œil distingue mais sur ce qu'un pas coûte : chez nous le curseur
     * redessine la fenêtre — quatorze tuiles avec leurs plaques et leurs
     * moulages. Un pas de plus n'est pas un bitmap de plus, c'est une fenêtre
     * repeinte de plus.
     *
     * Quinze par seconde restent au-dessus du seuil où un dégradé qui coule
     * paraît saccadé : ce qui se voit dans un mouvement lent est la continuité,
     * pas la cadence. Mesuré : le curseur passait de 0 à 27 points de processeur
     * à 25 pas par seconde.
     */
    const val STEPS = 27

    /** La durée d'un tour complet, en millisecondes. */
    const val PERIOD_MS = 1800

    /**
     * Le grand côté du bitmap calculé. Le shader l'étire ensuite à la taille
     * réelle : un dégradé n'a pas de détail fin, et 192 px suffisent pour que
     * l'étirement ne se voie pas.
     */
    private const val MAX_SIDE = 192

    /**
     * Les bitmaps déjà calculés, par forme, couleurs et pas de phase.
     *
     * Sans lui, un curseur posé sur une tuile recalculerait 45 bitmaps par
     * cycle, indéfiniment. Avec, il en calcule 45 une seule fois puis n'en
     * calcule plus aucun. Le plafond évite qu'une session qui a survolé
     * beaucoup de formes différentes les garde toutes.
     */
    private val cache = object : LinkedHashMap<Key, BitmapShader>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, BitmapShader>) =
            size > 96
    }

    private data class Key(
        val w: Int,
        val h: Int,
        val band: Int,
        val radius: Int,
        val start: Int,
        val end: Int,
        val step: Int,
    )

    /**
     * Le shader pour une bande donnée, à une phase donnée.
     *
     * [w] et [h] sont ceux de la boîte du curseur (le contrôle plus la bande de
     * chaque côté), [radius] le rayon de la **ligne médiane** de la bande, et
     * [step] le pas de phase dans `0 until STEPS`.
     */
    fun shader(
        w: Float,
        h: Float,
        band: Float,
        radius: Float,
        start: Color,
        end: Color,
        step: Int,
    ): Shader? {
        if (w < 1f || h < 1f) return null
        val key = Key(
            w = w.roundToInt(),
            h = h.roundToInt(),
            band = (band * 10f).roundToInt(),
            radius = (radius * 10f).roundToInt(),
            start = start.toArgb(),
            end = end.toArgb(),
            step = ((step % STEPS) + STEPS) % STEPS,
        )
        cache[key]?.let { return it }

        val scale = min(1f, MAX_SIDE / max(w, h))
        val bw = max(2, (w * scale).roundToInt())
        val bh = max(2, (h * scale).roundToInt())

        // La ligne médiane de la bande : c'est elle que le dégradé parcourt.
        val half = band / 2f
        val hx = max(1f, w / 2f - half)
        val hy = max(1f, h / 2f - half)
        val r = radius.coerceIn(0f, min(hx, hy))
        // Les demi-longueurs droites, coins retirés.
        val ax = hx - r
        val ay = hy - r
        val quarter = (r * PI / 2.0).toFloat()
        val perimeter = 4f * (ax + ay + quarter)
        if (perimeter <= 0f) return null

        // Les bornes cumulées, dans le sens horaire depuis le milieu du bord
        // haut : c'est l'origine, et le choix est arbitraire — il ne se voit
        // pas, puisque le cycle est fermé.
        val s1 = ax
        val s2 = s1 + quarter
        val s3 = s2 + 2f * ay
        val s4 = s3 + quarter
        val s5 = s4 + 2f * ax
        val s6 = s5 + quarter
        val s7 = s6 + 2f * ay

        val phase = key.step.toFloat() / STEPS
        val sr = start.red; val sg = start.green; val sb = start.blue
        val er = end.red; val eg = end.green; val eb = end.blue

        val pixels = IntArray(bw * bh)
        for (row in 0 until bh) {
            val y = (row + 0.5f) / scale - h / 2f
            for (col in 0 until bw) {
                val x = (col + 0.5f) / scale - w / 2f
                val t = arcLength(x, y, ax, ay, r, s1, s2, s3, s4, s5, s6, s7, perimeter)
                var u = (t / perimeter - phase) % 1f
                if (u < 0f) u += 1f
                val mix = 0.5f - 0.5f * cos(2.0 * PI * u).toFloat()
                val cr = ((sr + (er - sr) * mix) * 255f).roundToInt().coerceIn(0, 255)
                val cg = ((sg + (eg - sg) * mix) * 255f).roundToInt().coerceIn(0, 255)
                val cb = ((sb + (eb - sb) * mix) * 255f).roundToInt().coerceIn(0, 255)
                pixels[row * bw + col] = (0xFF shl 24) or (cr shl 16) or (cg shl 8) or cb
            }
        }

        val bitmap = Bitmap.createBitmap(pixels, bw, bh, Bitmap.Config.ARGB_8888)
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(
            Matrix().apply {
                setScale(w / bw, h / bh)
                // Le bitmap couvre la boite du curseur, dont l'origine est en
                // (-band, -band) : sans cette translation le degrade serait
                // decale d'une bande vers le bas et la droite.
                postTranslate(-band, -band)
            }
        )
        cache[key] = shader
        return shader
    }

    /**
     * La distance parcourue le long du périmètre, pour un point donné en
     * coordonnées centrées.
     *
     * Le point est rabattu sur la ligne médiane : sur un bord droit c'est sa
     * projection, dans un coin c'est son angle depuis le centre de l'arc. Les
     * pixels loin de la bande reçoivent une valeur qui ne sera jamais lue — le
     * chemin de la bande les découpe.
     */
    private fun arcLength(
        x: Float,
        y: Float,
        ax: Float,
        ay: Float,
        r: Float,
        s1: Float,
        s2: Float,
        s3: Float,
        s4: Float,
        s5: Float,
        s6: Float,
        s7: Float,
        perimeter: Float,
    ): Float {
        val halfPi = (PI / 2.0).toFloat()
        return when {
            // Les quatre bords droits, dans le sens horaire.
            x in -ax..ax && y < 0f -> if (x >= 0f) x else perimeter + x
            x > ax && y in -ay..ay -> s2 + (y + ay)
            x in -ax..ax && y > 0f -> s4 + (ax - x)
            x < -ax && y in -ay..ay -> s6 + (ay - y)
            // Les quatre arcs, mesurés depuis leur propre centre.
            x > ax && y < -ay -> s1 + (atan2(y + ay, x - ax) + halfPi).coerceIn(0f, halfPi) * r
            x > ax -> s3 + atan2(y - ay, x - ax).coerceIn(0f, halfPi) * r
            y > ay -> s5 + (atan2(y - ay, x + ax) - halfPi).coerceIn(0f, halfPi) * r
            else -> {
                var a = atan2(y + ay, x + ax)
                if (a < 0f) a += (2.0 * PI).toFloat()
                s7 + (a - PI.toFloat()).coerceIn(0f, halfPi) * r
            }
        }
    }
}

/**
 * Le pas de phase courant, écrit **seulement quand il change**.
 *
 * C'est toute la différence avec une `InfiniteTransition` : celle-ci publie une
 * valeur à chaque image de l'écran, donc 120 fois par seconde sur la Thor, et
 * chaque écriture invalide le contrôle et fait redessiner l'app. Ce flux-ci
 * n'écrit que 25 fois par seconde — le nombre de positions que le dégradé
 * possède réellement. Entre deux pas, rien n'est invalidé, et l'app ne
 * redessine pas du tout.
 *
 * Immobile si le système a coupé les animations, comme le fond : le curseur
 * garde alors une phase fixe, ce qui reste une bande dégradée parfaitement
 * lisible.
 */
@Composable
internal fun rememberFlowStep(): Int {
    val millis = rememberSlowMillis()
    return ((millis / CursorFlow.PERIOD_MS) * CursorFlow.STEPS).toInt().mod(CursorFlow.STEPS)
}
