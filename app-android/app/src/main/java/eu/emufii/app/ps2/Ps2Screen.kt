package eu.emufii.app.ps2

import eu.emufii.app.dolphin.Bounds
import eu.emufii.app.dolphin.Node

/**
 * ARMSX2's Settings, Network screen: label and value are two sibling `TextView`s,
 * paired by their horizontal band and never by node order.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § ARMSX2: two sibling `TextView`s, paired by their horizontal band
 */
object Ps2Screen {

    fun sameRow(a: Bounds, b: Bounds): Boolean {
        val overlap = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
        val shortest = minOf(a.bottom - a.top, b.bottom - b.top)
        // Two neighbouring lines graze each other by a few pixels, one line overlaps
        // almost entirely.
        return shortest > 0 && overlap * 2 >= shortest
    }

    fun label(nodes: List<Node>, label: String): Node? =
        nodes.firstOrNull { it.text.trim().equals(label, ignoreCase = true) }

    /**
     * The first text to the right of the label, not the last: the "Room code" row
     * carries the code then a "Generate" button, neither clickable in the tree.
     * Taking the rightmost, the driver read "Generate", rewrote the code and read it
     * again, seven times in five seconds on the Thor on 2026-08-17.
     */
    fun valueFor(nodes: List<Node>, label: String): Node? {
        val anchor = label(nodes, label) ?: return null
        return nodes
            .filter { it !== anchor && it.text.isNotBlank() && sameRow(anchor.bounds, it.bounds) }
            .filter { it.bounds.left > anchor.bounds.right }
            .minByOrNull { it.bounds.left }
    }

    /**
     * Neither the label nor the value is clickable, their shared container is; hence
     * the smallest clickable node containing the label, the whole page containing it too.
     */
    fun row(nodes: List<Node>, label: String): Node? {
        val anchor = label(nodes, label) ?: return null
        return nodes
            .filter { it.clickable && it.bounds.contains(anchor.bounds) }
            .minByOrNull { it.bounds.area }
    }

    /**
     * The three "Network mode" choices are labels to tap directly, not a drop-down: all
     * three are visible at once, no open-and-return trip as on Dolphin.
     */
    fun modeButton(nodes: List<Node>, label: String): Node? {
        val anchor = label(nodes, label) ?: return null
        if (anchor.clickable) return anchor
        return nodes
            .filter { it.clickable && it.bounds.contains(anchor.bounds) }
            .minByOrNull { it.bounds.area }
    }

    fun toggleFor(nodes: List<Node>, label: String): Node? {
        val anchor = label(nodes, label) ?: return null
        return nodes
            .filter { it !== anchor && it.clickable && sameRow(anchor.bounds, it.bounds) }
            .filter { it.bounds.left > anchor.bounds.right }
            .minByOrNull { it.bounds.area }
    }

    /**
     * ARMSX2 has no `EditText`: input goes key by key on its own keypad, and that
     * keypad has no dot, so an IPv4 address cannot be typed there, by us or by the
     * player.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § ARMSX2 has no editable field, and that is a wall
     */
    const val KEY_CLEAR = "Clear"
    const val KEY_DONE = "Done"
    const val KEY_BACKSPACE = "⌫"
    const val KEY_SHIFT = "⇧"

    fun canType(text: String): Boolean = text.all { it.isLetterOrDigit() && it.code < 128 }

    fun key(nodes: List<Node>, char: Char): Node? =
        nodes.firstOrNull { it.text.length == 1 && it.text[0].equals(char, ignoreCase = true) }

    fun commandKey(nodes: List<Node>, label: String): Node? = label(nodes, label)

    fun keyboardIsOpen(nodes: List<Node>): Boolean =
        commandKey(nodes, KEY_DONE) != null && commandKey(nodes, KEY_CLEAR) != null
}
