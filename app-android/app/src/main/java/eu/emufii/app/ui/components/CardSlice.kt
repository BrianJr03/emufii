package eu.emufii.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.plateColors

/**
 * Le remplissage opaque du curseur.
 *
 * Sorti de `screens/settings/SettingsPieces.kt` le 2026-08-28 : il y etait
 * `internal` au paquet des reglages, alors que le probleme qu'il resout — une
 * lueur est une ombre, et une ombre traverse tout ce qui n'est pas opaque — se
 * pose partout ou un controle transparent porte l'anneau. [PadTextField] en est
 * un, et il n'appartient a aucun ecran.
 */

/** Ou se trouve une carte de reglages et quelle hauteur elle fait, en coordonnees fenetre. */
data class CardBounds(val top: Float, val height: Float)

/**
 * La carte dans laquelle l'appelant dessine. Coordonnees **racine**, pas celles
 * d'un parent : ce qui en a besoin n'est pas a la meme profondeur.
 * pourquoi : docs/decisions/reglages-ecran.md § Le remplissage opaque existe pour le curseur, pas pour le look
 */
val LocalCardBounds = compositionLocalOf { CardBounds(0f, 0f) }

/**
 * Un remplissage opaque qui est, au pixel pres, ce que la carte peignait deja
 * ici. **Il existe pour le curseur, pas pour le look** : une lueur est une
 * ombre, et elle traverse tout ce qui n'est pas opaque.
 * pourquoi : docs/decisions/reglages-ecran.md § Le remplissage opaque existe pour le curseur, pas pour le look
 */
@Composable
fun Modifier.cardSliceFill(shape: Shape, tint: Color = Color.Transparent): Modifier {
    val card = LocalCardBounds.current
    val colors = plateColors(
        dark = LocalEmufiiDarkTheme.current,
        oled = LocalEmufiiOledTheme.current
    )
    var top by remember { mutableFloatStateOf(Float.NaN) }
    return this
        .onGloballyPositioned { top = it.positionInRoot().y }
        .background(
            brush = if (card.height <= 0f || top.isNaN()) SolidColor(colors.first())
            else Brush.verticalGradient(
                colors = colors,
                startY = card.top - top,
                endY = card.top - top + card.height
            ),
            shape = shape
        )
        .then(if (tint == Color.Transparent) Modifier else Modifier.background(tint, shape))
}
