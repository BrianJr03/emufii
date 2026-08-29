package eu.emufii.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's icons, drawn rather than typed.
 *
 * Un carre de 24 unites, bouts ronds, jonctions rondes, une seule graisse : tout
 * ce qu'on ajoute se dessine aux trois memes regles.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Les icônes de l'app sont dessinées, pas tapées
 */

/** The stroke every icon here is drawn with, relative to its box. */
private const val WEIGHT = 2.6f / 24f

@Composable
private fun TrayIcon(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    draw: (Path.(Float) -> Unit)
) {
    Canvas(modifier = modifier.size(size)) {
        val unit = this.size.minDimension / 24f
        val path = Path().apply { draw(unit) }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = this.size.minDimension * WEIGHT,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/** Back. Points left; [ChevronRight] is the same drawing mirrored. */
@Composable
fun ChevronLeft(size: Dp = 20.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        moveTo(15f * u, 5f * u); lineTo(9f * u, 12f * u); lineTo(15f * u, 19f * u)
    }

/** Forward, and "this row opens something". */
@Composable
fun ChevronRight(size: Dp = 20.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        moveTo(9f * u, 5f * u); lineTo(15f * u, 12f * u); lineTo(9f * u, 19f * u)
    }

/** Close, dismiss, remove. */
@Composable
fun CrossIcon(size: Dp = 18.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        moveTo(6.5f * u, 6.5f * u); lineTo(17.5f * u, 17.5f * u)
        moveTo(17.5f * u, 6.5f * u); lineTo(6.5f * u, 17.5f * u)
    }

/** Done, ready, confirmed. */
@Composable
fun CheckIcon(size: Dp = 18.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        moveTo(5f * u, 12.5f * u); lineTo(10f * u, 17.5f * u); lineTo(19f * u, 6.5f * u)
    }

/**
 * The signal mark: two arcs leaving a point. Remplace « 📡 » dans les etats vides
 * du chercheur de sessions.
 */
@Composable
fun SignalMark(size: Dp = 40.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        addArc(
            androidx.compose.ui.geometry.Rect(
                Offset(4f * u, 4f * u), Size(16f * u, 16f * u)
            ), -70f, -50f
        )
        addArc(
            androidx.compose.ui.geometry.Rect(
                Offset(8f * u, 8f * u), Size(8f * u, 8f * u)
            ), -70f, -50f
        )
        addOval(
            androidx.compose.ui.geometry.Rect(
                Offset(11f * u, 15f * u), Size(2f * u, 2f * u)
            )
        )
    }

/**
 * The sleep mark: a crescent. Un seul chemin ferme, sinon le trait donne deux
 * arcs qui se rencontrent.
 */
/**
 * The folder mark: a tab and a body. Remplace « 📁 » sur une bibliotheque sans
 * dossier — le premier ecran qu'un joueur voit.
 */
@Composable
fun FolderMark(size: Dp = 44.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        moveTo(3.5f * u, 18.5f * u)
        lineTo(3.5f * u, 6f * u)
        lineTo(9.5f * u, 6f * u)
        lineTo(11.5f * u, 8.5f * u)
        lineTo(20.5f * u, 8.5f * u)
        lineTo(20.5f * u, 18.5f * u)
        close()
    }

/**
 * Caution: this game runs, with something to know first. Juste la marque, sans
 * triangle autour : un contour dans un contour se lit a l'etroit.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Les icônes de l'app sont dessinées, pas tapées
 */
@Composable
fun WarnIcon(size: Dp = 14.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        moveTo(12f * u, 5f * u); lineTo(12f * u, 14f * u)
        moveTo(12f * u, 18.6f * u); lineTo(12f * u, 18.6f * u)
    }

/** Does not usefully run. The universal "no", and it is drawn, not typed. */
@Composable
fun BlockedIcon(size: Dp = 14.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        addOval(
            androidx.compose.ui.geometry.Rect(
                Offset(4f * u, 4f * u),
                Size(16f * u, 16f * u)
            )
        )
        moveTo(7.2f * u, 7.2f * u); lineTo(16.8f * u, 16.8f * u)
    }

/**
 * Not tried yet: a wave, meaning « roughly, maybe ». Deliberement la plus
 * discrete des quatre marques — les trois autres sont des verdicts.
 */
@Composable
fun TildeIcon(size: Dp = 14.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        moveTo(4.5f * u, 14f * u)
        cubicTo(7f * u, 8.5f * u, 9.5f * u, 8.5f * u, 12f * u, 12f * u)
        cubicTo(14.5f * u, 15.5f * u, 17f * u, 15.5f * u, 19.5f * u, 10f * u)
    }

/*
 * Les sept marques des pages de reglages. Une par page, et c'est la seule
 * raison qu'elles ont d'exister : dans un menu ou toutes les rangees se
 * ressemblent, l'oeil retrouve une page a sa forme avant d'en lire le nom.
 * pourquoi : docs/decisions/reglages-ecran.md § Une icône par page, et pas une de plus
 */

/** Profil : une tete et des epaules. */
@Composable
fun PersonMark(size: Dp = 20.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        addOval(
            androidx.compose.ui.geometry.Rect(
                Offset(8f * u, 3.5f * u), Size(8f * u, 8f * u)
            )
        )
        moveTo(4.5f * u, 20.5f * u)
        cubicTo(4.5f * u, 15.5f * u, 19.5f * u, 15.5f * u, 19.5f * u, 20.5f * u)
    }

/** Bibliotheque : trois tranches sur une etagere. */
@Composable
fun ShelfMark(size: Dp = 20.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        moveTo(5f * u, 4.5f * u); lineTo(5f * u, 17f * u)
        moveTo(10f * u, 4.5f * u); lineTo(10f * u, 17f * u)
        moveTo(15f * u, 5.5f * u); lineTo(18.5f * u, 16.5f * u)
        moveTo(3f * u, 19.5f * u); lineTo(21f * u, 19.5f * u)
    }

/** Consoles : la grille de tuiles, dont une eteinte. */
@Composable
fun GridMark(size: Dp = 20.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = 4f * u, top = 4f * u, right = 10.5f * u, bottom = 10.5f * u,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * u)
            )
        )
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = 13.5f * u, top = 4f * u, right = 20f * u, bottom = 10.5f * u,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * u)
            )
        )
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = 4f * u, top = 13.5f * u, right = 10.5f * u, bottom = 20f * u,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * u)
            )
        )
        // La quatrieme n'est pas dessinee : c'est la console masquee, et le
        // trou dit ce que la page fait mieux qu'une quatrieme tuile.
    }

/** Emulateurs : une puce et ses broches. */
@Composable
fun ChipMark(size: Dp = 20.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = 6.5f * u, top = 6.5f * u, right = 17.5f * u, bottom = 17.5f * u,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * u)
            )
        )
        moveTo(10f * u, 3.5f * u); lineTo(10f * u, 6.5f * u)
        moveTo(14f * u, 3.5f * u); lineTo(14f * u, 6.5f * u)
        moveTo(10f * u, 17.5f * u); lineTo(10f * u, 20.5f * u)
        moveTo(14f * u, 17.5f * u); lineTo(14f * u, 20.5f * u)
        moveTo(3.5f * u, 10f * u); lineTo(6.5f * u, 10f * u)
        moveTo(3.5f * u, 14f * u); lineTo(6.5f * u, 14f * u)
        moveTo(17.5f * u, 10f * u); lineTo(20.5f * u, 10f * u)
        moveTo(17.5f * u, 14f * u); lineTo(20.5f * u, 14f * u)
    }

/** Apparence : une goutte de peinture. */
@Composable
fun PaintMark(size: Dp = 20.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        moveTo(12f * u, 3.5f * u)
        cubicTo(12f * u, 3.5f * u, 5f * u, 11.5f * u, 5f * u, 15.2f * u)
        cubicTo(5f * u, 19f * u, 8.2f * u, 20.5f * u, 12f * u, 20.5f * u)
        cubicTo(15.8f * u, 20.5f * u, 19f * u, 19f * u, 19f * u, 15.2f * u)
        cubicTo(19f * u, 11.5f * u, 12f * u, 3.5f * u, 12f * u, 3.5f * u)
        close()
    }

/** General : deux curseurs de reglage. */
@Composable
fun SlidersMark(size: Dp = 20.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        moveTo(4f * u, 8.5f * u); lineTo(20f * u, 8.5f * u)
        moveTo(4f * u, 15.5f * u); lineTo(20f * u, 15.5f * u)
        addOval(
            androidx.compose.ui.geometry.Rect(
                Offset(7f * u, 5.5f * u), Size(6f * u, 6f * u)
            )
        )
        addOval(
            androidx.compose.ui.geometry.Rect(
                Offset(13f * u, 12.5f * u), Size(6f * u, 6f * u)
            )
        )
    }

/** A propos : la marque d'information. */
@Composable
fun InfoMark(size: Dp = 20.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        addOval(
            androidx.compose.ui.geometry.Rect(
                Offset(3.5f * u, 3.5f * u), Size(17f * u, 17f * u)
            )
        )
        moveTo(12f * u, 11f * u); lineTo(12f * u, 16.5f * u)
        moveTo(12f * u, 7.6f * u); lineTo(12f * u, 7.6f * u)
    }

/** Modifier, retoucher : le crayon du bouton de photo de profil. */
@Composable
fun PencilMark(size: Dp = 14.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        moveTo(4.5f * u, 19.5f * u)
        lineTo(5.5f * u, 15f * u)
        lineTo(16f * u, 4.5f * u)
        lineTo(19.5f * u, 8f * u)
        lineTo(9f * u, 18.5f * u)
        close()
        moveTo(13.5f * u, 7f * u); lineTo(17f * u, 10.5f * u)
    }

/**
 * La loupe : un cercle et un manche. Elle vivait en deux exemplaires a des
 * proportions differentes ; un glyphe est le meme partout, sinon ce n'en est
 * plus le meme.
 * pourquoi : docs/decisions/lancement-et-navigation.md § Les icônes de l'app sont dessinées, pas tapées
 */
@Composable
fun LensMark(size: Dp = 20.dp, color: Color, modifier: Modifier = Modifier) =
    TrayIcon(size, color, modifier) { u ->
        addOval(
            androidx.compose.ui.geometry.Rect(
                Offset(3.5f * u, 3.5f * u), Size(13f * u, 13f * u)
            )
        )
        moveTo(15.8f * u, 15.8f * u); lineTo(20.5f * u, 20.5f * u)
    }
