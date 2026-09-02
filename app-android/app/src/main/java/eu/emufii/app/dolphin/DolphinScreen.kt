package eu.emufii.app.dolphin

/**
 * Reading Dolphin's Compose netplay form without a single resource id: all geometry and
 * text. A plain [Node] and [Bounds], never the platform types: a JVM test gets the
 * stubbed `android.jar`, where every `Rect` method quietly returns zero.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Reading a Compose form with no id at all
 */
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val area: Long get() = (right - left).toLong() * (bottom - top)

    /** Edges included. */
    fun contains(other: Bounds): Boolean =
        other.left >= left && other.top >= top &&
            other.right <= right && other.bottom <= bottom
}

data class Node(
    val text: String,
    val className: String,
    val bounds: Bounds,
    /** Outermost first. */
    val ancestorClasses: List<String> = emptyList(),
    /** `resource-id` without its package, or empty: Compose never sets one. */
    val viewId: String = "",
    /** `content-desc`, all an icon-only button ever carries. */
    val description: String = "",
    val clickable: Boolean = false,
    /** For the screens that have one (ARMSX2's DEV9 toggle). */
    val checked: Boolean = false,
    /** The `AccessibilityNodeInfo` read from, untyped to keep this file testable. */
    val handle: Any? = null
) {
    val isField: Boolean get() = className == EDIT_TEXT

    val hasButtonAncestor: Boolean get() = BUTTON_CLASSES.any { it in ancestorClasses }

    val isButton: Boolean get() = className in BUTTON_CLASSES

    companion object {
        const val EDIT_TEXT = "android.widget.EditText"
        const val TEXT_VIEW = "android.widget.TextView"
        val BUTTON_CLASSES = listOf("android.widget.Button", "android.widget.ImageButton")
    }
}

object DolphinScreen {

    /**
     * The `EditText` whose bounds contain the label's, Compose drawing the label inside
     * the field's border: containment survives a reorder, an insert and a rotation.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Containment, not parentage, not position
     */
    fun fieldFor(nodes: List<Node>, labels: Collection<String>): Node? {
        val wanted = labels.map { it.trim().lowercase() }.toSet()
        if (wanted.isEmpty()) return null
        val label = nodes.firstOrNull {
            !it.isField && it.text.trim().lowercase() in wanted
        } ?: return null
        return nodes
            .filter { it.isField && it.bounds.contains(label.bounds) }
            // Nested boxes are possible; the tightest one is the field itself.
            .minByOrNull { it.bounds.area }
    }

    /**
     * Dolphin gives the role tab and the commit button the same text, and clicking the
     * wrong one is not a no-op.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Containment, not parentage, not position
     */
    fun tab(nodes: List<Node>, labels: Collection<String>): Node? =
        matching(nodes, labels).firstOrNull { !it.isField && !inButton(nodes, it) }

    fun actionButton(nodes: List<Node>, labels: Collection<String>): Node? =
        matching(nodes, labels).firstOrNull { inButton(nodes, it) }

    /**
     * Containment rather than ancestry: measured on the Thor, the button and its caption
     * come out as siblings; the ancestor case is kept for a future build that nests them.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Containment, not parentage, not position
     */
    private fun inButton(nodes: List<Node>, node: Node): Boolean =
        node.hasButtonAncestor ||
            nodes.any { it.isButton && it !== node && it.bounds.contains(node.bounds) }

    /** The dropdown opens in its own window: the tree holds the options and no field. */
    fun option(nodes: List<Node>, labels: Collection<String>): Node? =
        matching(nodes, labels).firstOrNull { !it.isField }

    /** Both options and no field: a closed dropdown still shows its text. */
    fun isDropdownOpen(
        nodes: List<Node>,
        directLabels: Collection<String>,
        traversalLabels: Collection<String>
    ): Boolean =
        nodes.none { it.isField } &&
            matching(nodes, directLabels).isNotEmpty() &&
            matching(nodes, traversalLabels).isNotEmpty()

    /**
     * By shape: in the top strip, clickable, no id but a description, furthest right.
     * Dolphin's own buttons all carry an id; the framework's overflow carries none.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § The overflow button is found by its shape
     */
    fun overflow(nodes: List<Node>, window: Bounds): Node? {
        val strip = window.top + (window.bottom - window.top) / 4
        return nodes
            .filter {
                it.clickable && it.viewId.isEmpty() && it.description.isNotBlank() &&
                    it.bounds.bottom <= strip
            }
            .maxByOrNull { it.bounds.left }
    }

    /**
     * Containment on normalised strings, both ways round: the longest wins, and a tie
     * cancels everything, picking at random would start the wrong game.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Matching a game when the two sides do not name it the same
     */
    fun looseOption(nodes: List<Node>, target: String): Node? {
        val wanted = normalize(target)
        if (wanted.isEmpty()) return null
        val hits = nodes
            .filter { !it.isField && it.text.isNotBlank() }
            .mapNotNull { node ->
                val text = normalize(node.text)
                if (text.isEmpty()) return@mapNotNull null
                if (text in wanted || wanted in text) node to text.length else null
            }
        val best = hits.maxByOrNull { it.second } ?: return null
        if (hits.count { it.second == best.second } > 1) return null
        return best.first
    }

    fun looselyMatches(text: String, target: String): Boolean {
        val a = normalize(text)
        val b = normalize(target)
        return a.isNotEmpty() && b.isNotEmpty() && (a in b || b in a)
    }

    /** The punctuation is precisely where the two names diverge. */
    private fun normalize(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    private fun matching(nodes: List<Node>, labels: Collection<String>): List<Node> {
        val wanted = labels.map { it.trim().lowercase() }.toSet()
        if (wanted.isEmpty()) return emptyList()
        return nodes.filter { it.text.trim().lowercase() in wanted }
    }
}
