package eu.emufii.app.secondscreen

import androidx.annotation.StringRes
import eu.emufii.app.R
import eu.emufii.app.library.Console

/**
 * How Emufii plays together on one machine, in as few words as it takes.
 *
 * Deux lignes et au plus un avertissement, et ce plafond est le dessin.
 * [warning] est reserve a ce qui se decouvrirait autrement comme une panne, pas
 * aux nuances : son absence est ce qui donne du sens a sa presence.
 * pourquoi : docs/decisions/second-ecran.md § Une fiche console tient en deux lignes et un avertissement
 */
data class ConsoleBrief(
    @StringRes val first: Int,
    @StringRes val second: Int,
    @StringRes val warning: Int? = null,
)

fun consoleBrief(console: Console): ConsoleBrief = when (console) {
    Console.THREE_DS -> ConsoleBrief(
        first = R.string.brief_3ds_1,
        second = R.string.brief_3ds_2,
        warning = R.string.brief_3ds_warning,
    )
    Console.SWITCH -> ConsoleBrief(
        first = R.string.brief_switch_1,
        second = R.string.brief_switch_2,
    )
    Console.PSP -> ConsoleBrief(
        first = R.string.brief_psp_1,
        second = R.string.brief_psp_2,
    )
    Console.DS -> ConsoleBrief(
        first = R.string.brief_ds_1,
        second = R.string.brief_ds_2,
        warning = R.string.brief_ds_warning,
    )
    Console.PS2 -> ConsoleBrief(
        first = R.string.brief_ps2_1,
        second = R.string.brief_ps2_2,
        warning = R.string.brief_ps2_warning,
    )
    Console.GAMECUBE, Console.WII -> ConsoleBrief(
        first = R.string.brief_dolphin_1,
        second = R.string.brief_dolphin_2,
        warning = R.string.brief_dolphin_warning,
    )
}
