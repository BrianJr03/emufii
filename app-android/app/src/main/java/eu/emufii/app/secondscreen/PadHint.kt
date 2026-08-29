package eu.emufii.app.secondscreen

import androidx.annotation.StringRes
import eu.emufii.app.R

/**
 * One line of the legend: a face button and what it does.
 *
 * The Thor is silk-printed the Nintendo way, so its buttons are swapped against
 * the keycodes: what we handle as `Key.ButtonA` (confirm) is the button marked
 * **B**, and `Key.ButtonB` (back) is the one marked **A**. The letters below are
 * what is written on the plastic, which is the only thing the player can read.
 */
enum class PadHint(
    /**
     * The letter on the plastic, or null when the cap is drawn instead.
     *
     * The d-pad had a letter here once, the character `U+271B`. Rounded M+ does
     * not carry it, so it fell back to a system font: a different weight, a
     * different size and different metrics, inside a 26.dp cap where all three
     * show. Every other symbol in this app is drawn by the file that needs it,
     * for exactly this reason.
     */
    val glyph: String?,
    @StringRes val label: Int,
    /** True when the cap is drawn pushed in, for a hint that is about holding. */
    val held: Boolean = false,
) {

    /** Opens, joins, launches. Reaches us as `Key.ButtonA`; worn as B. */
    CONFIRM(glyph = "B", label = R.string.pad_hint_confirm),

    /** Leaves the screen. Reaches us as `Key.ButtonB`; worn as A. */
    BACK(glyph = "A", label = R.string.pad_hint_back),

    /**
     * Held, it opens the game's own menu — rename, icon, hide.
     *
     * The same button as [CONFIRM], because it genuinely is: the grid starts a
     * timer on key down and only launches the game if the button comes back up
     * before it fires. So the cap wears the same letter, and is drawn **pushed
     * in** instead. A held button is a button that has travelled, which the
     * plate already knows how to say; a second letter would have been a lie and
     * a word like "hold" on the cap would have been a caption on an icon.
     */
    HOLD(glyph = "B", label = R.string.pad_hint_hold, held = true),

    /**
     * Efface le dernier caractere saisi. Meme bouton que [BACK], et c'est
     * voulu : le clavier de code n'a pas de touche d'effacement, c'est retour
     * qui defait — un cran de saisie tant qu'il en reste, l'ecran ensuite. La
     * legende le dit la ou il sert, sur l'ecran de face cette fois : c'est le
     * premier endroit de l'app ou une touche fait deux choses selon l'etat, et
     * un joueur ne peut pas le deviner.
     */
    ERASE(glyph = "A", label = R.string.pad_hint_erase),
    ;
}

/**
 * The legend a given face of the panel is allowed to show.
 *
 * Split in two because it is rendered in the bottom corners, and the corners
 * mean different things: the left is where you leave from, the right is where
 * you act. That is the arrangement of every console shell the player already
 * owns, and inventing a third one buys nothing.
 *
 * Deliberately short. A legend listing every key is a manual, and a manual read
 * at arm's length under the player's hands is read by nobody. Two on the right,
 * one on the left, and the panel stays a glance.
 */
data class PadLegend(
    val left: List<PadHint> = emptyList(),
    val right: List<PadHint> = emptyList(),
) {
    val isEmpty: Boolean get() = left.isEmpty() && right.isEmpty()

    companion object {
        /** Browsing the tray: open, hold for the game's menu, back out. */
        val BROWSING = PadLegend(
            left = listOf(PadHint.BACK),
            right = listOf(PadHint.CONFIRM, PadHint.HOLD),
        )

        /**
         * On a console's folder: open it, or go back. And nothing else.
         *
         * The hold is missing because it genuinely does nothing there — the
         * grid's long press asks `entry as? Entry.Game`, and a folder is not
         * one, so the timer fires into an empty branch. Printing it would be a
         * key the panel claims and the machine ignores, which is the one thing
         * this legend exists not to do.
         */
        val FOLDER = PadLegend(
            left = listOf(PadHint.BACK),
            right = listOf(PadHint.CONFIRM),
        )

        /**
         * En session, la legende est **vide**, et ce n'est pas un oubli : le pad
         * ne fait rien sur retour, et B agit sur l'ecran de face, pas sur le
         * panneau. Les commandes du panneau se pressent au doigt.
         * pourquoi : docs/decisions/second-ecran.md § En session, la légende du pad est vide
         */
        val IN_SESSION = PadLegend()
    }
}
