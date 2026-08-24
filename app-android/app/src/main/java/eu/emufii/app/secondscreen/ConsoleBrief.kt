package eu.emufii.app.secondscreen

import androidx.annotation.StringRes
import eu.emufii.app.R
import eu.emufii.app.library.Console

/**
 * How Emufii plays together on one machine, in as few words as it takes.
 *
 * Written per console because the answer genuinely differs per console, and the
 * differences are the part a player has to know before they invite anybody: the
 * Switch joins a room raised on our server, the DS is a redirection towards a
 * fan-run service with no session at all, the PS2 wants an address typed into
 * the game by hand. Saying "Emufii connects you" everywhere would be shorter
 * and would be a lie four times out of seven.
 *
 * Two lines and at most one warning, and that ceiling is the design. This is
 * read at arm's length while a cursor is moving across a shelf of folders; a
 * third line would not be read by anybody, and the manual already exists on the
 * front screen where it can be scrolled.
 *
 * [warning] is for what will otherwise be discovered as a failure — an emulator
 * whose stable build has no multiplayer UI, a VPN that has to be up or the
 * console silently calls servers that were switched off in 2014. Not for
 * caveats: a console with nothing to warn about carries none, and the absence
 * is what makes the ones that are there mean something.
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
