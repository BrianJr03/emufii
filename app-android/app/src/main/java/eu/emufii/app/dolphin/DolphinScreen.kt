package eu.emufii.app.dolphin

/**
 * Reading Dolphin's Compose netplay form without a single resource id: all
 * geometry and text, pinned by `DolphinScreenTest` against real trees.
 *
 * A plain [Node] and [Bounds], never the platform types: a JVM test gets the
 * stubbed `android.jar`, where every `Rect` method quietly returns zero.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Lire un formulaire Compose sans un seul identifiant
 */
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val area: Long get() = (right - left).toLong() * (bottom - top)

    /** True when [other] lies entirely within this box, edges included. */
    fun contains(other: Bounds): Boolean =
        other.left >= left && other.top >= top &&
            other.right <= right && other.bottom <= bottom
}

/** One node of the flattened accessibility tree, as inert data. */
data class Node(
    val text: String,
    val className: String,
    val bounds: Bounds,
    /** Class names of this node's ancestors, outermost first. */
    val ancestorClasses: List<String> = emptyList(),
    /** `resource-id` without its package, or empty, Compose never sets one. */
    val viewId: String = "",
    /** `content-desc`, which is all an icon-only button ever carries. */
    val description: String = "",
    val clickable: Boolean = false,
    /**
     * A switch's state, for the screens that have one (ARMSX2's DEV9 toggle).
     * Defaults to false, so a test tree that ignores it stays valid.
     */
    val checked: Boolean = false,
    /**
     * The live `AccessibilityNodeInfo` this was read from, untyped so this file
     * stays free of the platform class and testable.
     */
    val handle: Any? = null
) {
    val isField: Boolean get() = className == EDIT_TEXT

    /** True when a button is one of this node's ancestors. */
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
     * The field a label belongs to: the `EditText` whose bounds **contain** the
     * label's, since Compose draws the label inside the field's border.
     * Containment survives a reorder, an inserted row and a rotation.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § La contenance, pas la parenté, pas la position
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
     * The tab that switches role, as opposed to the button that commits it —
     * Dolphin gives both the same text, and clicking the wrong one is not a
     * no-op.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § La contenance, pas la parenté, pas la position
     */
    fun tab(nodes: List<Node>, labels: Collection<String>): Node? =
        matching(nodes, labels).firstOrNull { !it.isField && !inButton(nodes, it) }

    /** The commit button, "Connect" or "Host", at the bottom right. */
    fun actionButton(nodes: List<Node>, labels: Collection<String>): Node? =
        matching(nodes, labels).firstOrNull { inButton(nodes, it) }

    /**
     * Whether [node] is the label of a button, by **containment** rather than
     * ancestry: measured on the Thor, the button and its caption come out as
     * siblings. The ancestor case is kept in case a future build nests them.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § La contenance, pas la parenté, pas la position
     */
    private fun inButton(nodes: List<Node>, node: Node): Boolean =
        node.hasButtonAncestor ||
            nodes.any { it.isButton && it !== node && it.bounds.contains(node.bounds) }

    /**
     * An entry of the connection-type dropdown: it opens in its own window, so
     * the tree holds the two options and nothing else of the form.
     */
    fun option(nodes: List<Node>, labels: Collection<String>): Node? =
        matching(nodes, labels).firstOrNull { !it.isField }

    /**
     * True when the dropdown is showing rather than the form: both options
     * present **and** no field, since a closed dropdown still shows its text.
     */
    fun isDropdownOpen(
        nodes: List<Node>,
        directLabels: Collection<String>,
        traversalLabels: Collection<String>
    ): Boolean =
        nodes.none { it.isField } &&
            matching(nodes, directLabels).isNotEmpty() &&
            matching(nodes, traversalLabels).isNotEmpty()

    /**
     * The overflow button, by **shape**: in the top strip, the clickable node
     * with no id but a description, furthest right. Dolphin's own buttons all
     * carry an id; the framework's overflow carries none.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Le bouton de débordement se trouve par sa forme
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
     * The list entry that means [target], within tolerance: containment on
     * normalised strings, both ways round. The longest wins, and **a tie
     * cancels everything** — picking at random would start the wrong game.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Apparier un jeu quand les deux côtés ne le nomment pas pareil
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

    /** True when [text] means [target] in [looseOption]'s sense. */
    fun looselyMatches(text: String, target: String): Boolean {
        val a = normalize(text)
        val b = normalize(target)
        return a.isNotEmpty() && b.isNotEmpty() && (a in b || b in a)
    }

    /**
     * Lowercased, punctuation removed, whitespace collapsed — the punctuation
     * is precisely where the two names diverge.
     */
    private fun normalize(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    private fun matching(nodes: List<Node>, labels: Collection<String>): List<Node> {
        val wanted = labels.map { it.trim().lowercase() }.toSet()
        if (wanted.isEmpty()) return emptyList()
        return nodes.filter { it.text.trim().lowercase() in wanted }
    }
}
