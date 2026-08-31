package eu.emufii.app.secondscreen

import androidx.annotation.StringRes
import eu.emufii.app.R

/**
 * One line of the legend: a face button and what it does.
 *
 * The Thor is silk-printed the Nintendo way, so its buttons are swapped against
 * the keycodes: what we handle as `Key.ButtonA` (confirm) is the button marked
 * B, and `Key.ButtonB` (back) is the one marked A. The glyphs below are what is
 * written on the plastic.
 */
enum class PadHint(
    /**
     * The letter on the plastic, or null when the cap is drawn instead. Keep to
     * characters Rounded M+ carries: the d-pad used U+271B, which fell back to a
     * system font and showed a different weight, size and metrics in a 26.dp cap.
     */
    val glyph: String?,
    @StringRes val label: Int,
    val held: Boolean = false,
) {

    CONFIRM(glyph = "B", label = R.string.pad_hint_confirm),

    BACK(glyph = "A", label = R.string.pad_hint_back),

    /**
     * Held, opens the game's own menu. The same button as [CONFIRM]: the grid
     * starts a timer on key down and only launches if the button comes back up
     * first. Same letter, drawn pushed in.
     */
    HOLD(glyph = "B", label = R.string.pad_hint_hold, held = true),

    /**
     * Erases the last character typed. The same button as [BACK]: the code
     * keyboard has no delete key, so back undoes one character while any
     * remain, then leaves the screen.
     */
    ERASE(glyph = "A", label = R.string.pad_hint_erase),
    ;
}

/** Split by corner: left is where you leave from, right is where you act. */
data class PadLegend(
    val left: List<PadHint> = emptyList(),
    val right: List<PadHint> = emptyList(),
) {
    val isEmpty: Boolean get() = left.isEmpty() && right.isEmpty()

    companion object {
        val BROWSING = PadLegend(
            left = listOf(PadHint.BACK),
            right = listOf(PadHint.CONFIRM, PadHint.HOLD),
        )

        /**
         * No hold on a console's folder: the grid's long press asks
         * `entry as? Entry.Game`, so on a folder the timer fires into an empty
         * branch.
         */
        val FOLDER = PadLegend(
            left = listOf(PadHint.BACK),
            right = listOf(PadHint.CONFIRM),
        )

        /**
         * Empty in session: the pad does nothing on back, and B acts on the
         * front screen rather than the panel. Panel controls are touched.
         * pourquoi : docs/decisions/second-ecran.md § In session, the pad legend is empty
         */
        val IN_SESSION = PadLegend()
    }
}
