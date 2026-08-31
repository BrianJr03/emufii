package eu.emufii.app.ps2

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import eu.emufii.app.R
import eu.emufii.app.azahar.NetplayAutomation
import eu.emufii.app.azahar.NetplayPlan
import eu.emufii.app.azahar.NetplayProgress
import eu.emufii.app.dolphin.Bounds
import eu.emufii.app.dolphin.Node
import eu.emufii.app.wg.WgConfig

/**
 * Sets ARMSX2's Settings -> Network screen up for a Local Link game. Read by
 * rows; the container takes the click. See [Ps2Target].
 *
 * No text field anywhere (ARMSX2's own keyboard, key by key), and that keyboard
 * has no dot key: hence the `emufii` name resolved by `relay/dns.js`.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Two peculiarities that exist nowhere else
 */
class Ps2NetplayDriver(
    private val context: Context,
    /**
     * Re-reads the tree between two keystrokes: essential here and nowhere
     * else: the screen redraws at every character.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Entry is done in one pass
     */
    private val readTree: () -> AccessibilityNodeInfo?,
    /**
     * The system "back" gesture, to get out of a screen we cannot read.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Entry is done in one pass
     */
    private val goBack: () -> Boolean,
    private val onFinished: (success: Boolean) -> Unit
) {

    private val labels by lazy { Ps2Labels(context) }

    private var navClicks = 0
    private var navPlan: NetplayPlan? = null

    /** Scrolls spent on the current plan. */
    private var scrolls = 0

    /** System backs spent looking for a screen we know. */
    private var backs = 0

    /** Clicks spent on the mode choice. One is enough; two are a bug. */
    private var modeClicks = 0

    /** Passes in a row on a screen we cannot read. Reset as soon as we read one. */
    private var unknownPasses = 0

    /**
     * Entries attempted on the current plan: a ceiling, or a screen that does
     * not read back what was written makes the driver start over endlessly.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Two caps, two failures avoided
     */
    private var writes = 0

    /**
     * The steps already set: we go down the screen and never back up, so a
     * passed toggle leaves the tree entirely.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § The order of the settings is not cosmetic
     */
    private val done = HashSet<String>()

    /** Returns true if this pass advanced the flow. */
    fun step(root: AccessibilityNodeInfo, pkg: String, plan: NetplayPlan): Boolean {
        if (navPlan !== plan) {
            navPlan = plan
            navClicks = 0
            scrolls = 0
            writes = 0
            backs = 0
            modeClicks = 0
            unknownPasses = 0
            done.clear()
        }

        val nodes = flatten(root)
        val hosting = plan.role == NetplayPlan.Role.Host
        Log.d(TAG, "pass: ${nodes.size} nodes, role=${if (hosting) "host" else "guest"}")

        // The keyboard first: while it is open nothing else is reachable, and
        // the screen we were reading is behind it.
        if (Ps2Screen.keyboardIsOpen(nodes)) {
            val wanted = pendingValue ?: run {
                // Open with nothing we know to put in it: close it rather than
                // leave the player facing a keyboard we brought up.
                Log.w(TAG, "keyboard open with no value to type, closing")
                return Ps2Screen.commandKey(nodes, Ps2Screen.KEY_DONE)?.live?.click() ?: false
            }
            return type(nodes, wanted)
        }

        // 3. The Network screen, recognised by ANY of its markers: the screen
        //    is taller than the device and the tree holds only what is drawn.
        //    pourquoi : docs/decisions/pilotes-emulateurs.md § The order of the settings is not cosmetic
        val dev9 = labels.of(Ps2Target.I18n.KEY_ENABLE_DEV9, Ps2Target.LABEL_ENABLE_DEV9)
            .firstNotNullOfOrNull { Ps2Screen.label(nodes, it) }
        val onNetworkScreen = dev9 != null || NETWORK_MARKERS.any { Ps2Screen.label(nodes, it) != null }
        if (onNetworkScreen) {
            unknownPasses = 0
            return settleNetwork(nodes, plan, hosting, dev9)
        }

        if (navClicks >= MAX_NAV_CLICKS) {
            Log.w(TAG, "cap of $MAX_NAV_CLICKS clicks reached, handing control back")
            return false
        }

        // 2. The settings screen -> the Network tab.
        labels.of(Ps2Target.I18n.KEY_NETWORK_TAB, Ps2Target.LABEL_NETWORK)
            .firstNotNullOfOrNull { Ps2Screen.modeButton(nodes, it) }
            ?.let {
                Log.d(TAG, "opening the Network tab")
                unknownPasses = 0
                navClicks++
                return it.live.click()
            }

        // 1. The menu -> Settings.
        labels.of(KEY_SETTINGS, Ps2Target.LABEL_SETTINGS)
            .firstNotNullOfOrNull { Ps2Screen.modeButton(nodes, it) }
            ?.let {
                Log.d(TAG, "opening settings")
                NetplayAutomation.report(NetplayProgress.OpeningMenu)
                unknownPasses = 0
                navClicks++
                return it.live.click()
            }

        // 0. The library -> the menu. The button has no translatable text: it
        //    is a glyph, and so much the better, it does not change language.
        Ps2Screen.modeButton(nodes, MENU_GLYPH)?.let {
            Log.d(TAG, "ouverture du menu")
            NetplayAutomation.report(NetplayProgress.OpeningMenu)
            unknownPasses = 0
            navClicks++
            return it.live.click()
        }

        // Unknown screen, and we do NOT go back straight away: a screen
        // mid-animation is an unknown screen, and going back undoes the click.
        // pourquoi : docs/decisions/pilotes-emulateurs.md § A screen mid-animation is an unknown screen
        unknownPasses++
        if (unknownPasses < UNKNOWN_BEFORE_BACK) {
            Log.d(TAG, "unknown screen (${nodes.size} nodes), letting it settle")
            return false
        }
        if (backs < MAX_BACKS) {
            backs++
            unknownPasses = 0
            Log.d(TAG, "unrecognised screen (${nodes.size} nodes), going back ($backs)")
            return goBack()
        }
        Log.w(TAG, "unrecognised screen and $MAX_BACKS backs spent, giving up")
        return false
    }

    /** What we are in the middle of writing, set just before opening a row. */
    private var pendingValue: String? = null

    /**
     * One setting per pass, in the order in which they depend on each other,
     * changing mode redraws the bottom half of the screen.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § The order of the settings is not cosmetic
     */
    private fun settleNetwork(
        nodes: List<Node>,
        plan: NetplayPlan,
        hosting: Boolean,
        dev9Label: Node?
    ): Boolean {
        // a. The adapter, without which nothing below exists. An absent label
        //    means "scrolled past", never "no toggle".
        //    pourquoi : docs/decisions/pilotes-emulateurs.md § The order of the settings is not cosmetic
        if (dev9Label != null && STEP_DEV9 !in done) {
            val toggle = Ps2Screen.toggleFor(nodes, dev9Label.text)
            if (toggle != null && !toggle.checked) {
                Log.d(TAG, "activation de DEV9")
                NetplayAutomation.report(NetplayProgress.ChoosingMode)
                return toggle.live.click()
            }
            done += STEP_DEV9
        }

        // b. The mode. It cannot be read off the button: measured, none of the
        //    three carries `selected` or `checked`. Inferred from the fields.
        //    pourquoi : docs/decisions/pilotes-emulateurs.md § The order of the settings is not cosmetic
        if (STEP_MODE !in done) {
            val marker = if (hosting) Ps2Target.LABEL_OWN_ADDRESS else Ps2Target.LABEL_HOST_ADDRESS
            when {
                Ps2Screen.label(nodes, marker) != null -> done += STEP_MODE
                // One click, never two: the confirming marker sits below the
                // fold, and eight clicks in a row were measured before this.
                // pourquoi : docs/decisions/pilotes-emulateurs.md § The order of the settings is not cosmetic
                modeClicks > 0 -> return scroll(nodes, plan)
                else -> {
                    val wanted =
                        if (hosting) Ps2Target.LABEL_MODE_HOST else Ps2Target.LABEL_MODE_JOIN
                    val button = Ps2Screen.modeButton(nodes, wanted) ?: return scroll(nodes, plan)
                    Log.d(TAG, "switching to \"$wanted\"")
                    NetplayAutomation.report(NetplayProgress.ChoosingMode)
                    modeClicks++
                    return button.live.click()
                }
            }
        }

        NetplayAutomation.report(NetplayProgress.FillingForm)

        // c. The address, on the guest only, and it is a name, for want of a
        //    dot key. The host has nothing to enter: ARMSX2 shows its own
        //    addresses, tunnel included.
        if (!hosting && STEP_ADDRESS !in done) {
            val row = Ps2Screen.label(nodes, Ps2Target.LABEL_HOST_ADDRESS)
                ?: return scroll(nodes, plan)
            val current = Ps2Screen.valueFor(nodes, row.text)?.text?.trim()
            if (!current.equals(WgConfig.PS2_HOST_NAME, ignoreCase = true)) {
                return open(nodes, Ps2Target.LABEL_HOST_ADDRESS, WgConfig.PS2_HOST_NAME, plan)
            }
            done += STEP_ADDRESS
        }

        // d. The port: the same everywhere, "there is no automatic negotiation".
        if (STEP_PORT !in done) {
            Ps2Screen.label(nodes, Ps2Target.LABEL_PORT) ?: return scroll(nodes, plan)
            val port = plan.port.toString()
            if (Ps2Screen.valueFor(nodes, Ps2Target.LABEL_PORT)?.text?.trim() != port) {
                return open(nodes, Ps2Target.LABEL_PORT, port, plan)
            }
            done += STEP_PORT
        }

        // e. The room code: the session code, which both sides already know
        //    without anything having to be transmitted.
        val room = roomCode(plan)
        if (room != null && STEP_ROOM !in done) {
            Ps2Screen.label(nodes, Ps2Target.LABEL_ROOM_CODE) ?: return scroll(nodes, plan)
            val current = Ps2Screen.valueFor(nodes, Ps2Target.LABEL_ROOM_CODE)?.text?.trim()
            if (!current.equals(room, ignoreCase = true)) {
                return open(nodes, Ps2Target.LABEL_ROOM_CODE, room, plan)
            }
            done += STEP_ROOM
        }

        Log.d(TAG, "network screen set")
        NetplayAutomation.report(NetplayProgress.Done)
        onFinished(true)
        return true
    }

    /**
     * Scrolls down a notch: a row below the fold is not in the tree at all.
     * Bounded, or it scrolls forever under the player's thumb.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Two caps, two failures avoided
     */
    private fun scroll(nodes: List<Node>, plan: NetplayPlan): Boolean {
        if (scrolls >= MAX_SCROLLS) {
            Log.w(TAG, "nothing found after $MAX_SCROLLS scrolls")
            giveUp(plan, R.string.netplay_fields_filled)
            return true
        }
        val scrollable = nodes
            .filter { it.live.isScrollable && it.bounds.bottom - it.bounds.top > MIN_SCROLL_HEIGHT }
            .maxByOrNull { it.bounds.area }
        if (scrollable == null) {
            giveUp(plan, R.string.netplay_fields_filled)
            return true
        }
        scrolls++
        Log.d(TAG, "scrolling ($scrolls)")
        return scrollable.live.performAction(
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id
        )
    }

    /** Opens a row, remembering what we mean to write into it. */
    private fun open(nodes: List<Node>, label: String, value: String, plan: NetplayPlan): Boolean {
        if (!Ps2Screen.canType(value)) {
            // Never try: the keyboard has neither dot nor punctuation, and
            // typing half a value is worse than typing nothing.
            Log.w(TAG, "\"$value\" cannot be typed on this keyboard")
            giveUp(plan, R.string.netplay_automation_stopped)
            return true
        }
        if (writes >= MAX_WRITES) {
            Log.w(TAG, "\"$label\" does not keep what is typed into it, giving up")
            giveUp(plan, R.string.netplay_fields_filled)
            return true
        }
        val row = Ps2Screen.row(nodes, label)
        if (row == null) {
            giveUp(plan, R.string.netplay_fields_filled)
            return true
        }
        Log.d(TAG, "opening \"$label\" to type \"$value\"")
        writes++
        pendingValue = value
        return row.live.click()
    }

    /**
     * Enters a value on ARMSX2's keyboard: clear, type, confirm, all in ONE
     * pass. One pass per character would look safer and be worse.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Entry is done in one pass
     */
    private fun type(first: List<Node>, value: String): Boolean {
        var nodes = first
        Ps2Screen.commandKey(nodes, Ps2Screen.KEY_CLEAR)?.live?.click()
        for (ch in value) {
            nodes = flatten(readTree() ?: return false)
            val key = Ps2Screen.key(nodes, ch)
            if (key == null) {
                Log.w(TAG, "key \"$ch\" not found, input abandoned")
                pendingValue = null
                return false
            }
            key.live.click()
        }
        nodes = flatten(readTree() ?: return false)
        pendingValue = null
        Log.d(TAG, "\"$value\" typed, confirming")
        return Ps2Screen.commandKey(nodes, Ps2Screen.KEY_DONE)?.live?.click() ?: false
    }

    /**
     * The room code, cut to ARMSX2's bounds. Too short, and we do not invent
     * one: a code the other player will not have is worse than none.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Entry is done in one pass
     */
    internal fun roomCode(plan: NetplayPlan): String? {
        val raw = plan.password?.filter { it.isLetterOrDigit() && it.code < 128 } ?: return null
        val cut = raw.take(Ps2Target.ROOM_CODE_LENGTH.last)
        return cut.takeIf { it.length >= Ps2Target.ROOM_CODE_LENGTH.first }
    }

    private fun giveUp(plan: NetplayPlan, message: Int) {
        NetplayAutomation.report(
            NetplayProgress.Failed(
                context.getString(message, EMULATOR, "${WgConfig.PS2_HOST_NAME}:${plan.port}")
            )
        )
        onFinished(false)
    }

    private fun flatten(root: AccessibilityNodeInfo): List<Node> =
        flattenRaw(root).map { node ->
            Node(
                text = node.text?.toString().orEmpty(),
                className = node.className?.toString().orEmpty(),
                bounds = node.bounds(),
                viewId = node.viewIdResourceName?.substringAfter(":id/").orEmpty(),
                description = node.contentDescription?.toString().orEmpty(),
                clickable = node.isClickable,
                checked = node.isChecked,
                handle = node
            )
        }

    private fun flattenRaw(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val out = ArrayList<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue += root
        while (queue.isNotEmpty() && out.size < MAX_NODES) {
            val node = queue.removeFirst()
            out += node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue += it }
        }
        return out
    }

    private fun AccessibilityNodeInfo.bounds(): Bounds {
        val r = android.graphics.Rect().also { getBoundsInScreen(it) }
        return Bounds(r.left, r.top, r.right, r.bottom)
    }

    private val Node.live: AccessibilityNodeInfo get() = handle as AccessibilityNodeInfo

    /** Clicks the node, or the first ancestor that accepts a click. */
    private fun AccessibilityNodeInfo.click(): Boolean {
        var node: AccessibilityNodeInfo? = this
        var hops = 0
        while (node != null && hops < MAX_ANCESTOR_HOPS) {
            if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node = node.parent
            hops++
        }
        return false
    }

    private companion object {
        const val TAG = "Ps2Netplay"
        const val EMULATOR = "ARMSX2"

        /** ARMSX2's i18n key for "Settings". */
        const val KEY_SETTINGS = "action.settings"

        /** The library's menu button: a glyph, hence language-free. */
        const val MENU_GLYPH = "☰"

        const val STEP_DEV9 = "dev9"
        const val STEP_MODE = "mode"
        const val STEP_ADDRESS = "address"
        const val STEP_PORT = "port"
        const val STEP_ROOM = "room"

        /** The markers that say "we are on the Network screen", at any height. */
        val NETWORK_MARKERS = listOf(
            Ps2Target.LABEL_NETWORK_MODE,
            Ps2Target.LABEL_MODE_HOST,
            Ps2Target.LABEL_PORT,
            Ps2Target.LABEL_ROOM_CODE,
            Ps2Target.LABEL_OWN_ADDRESS,
            Ps2Target.LABEL_HOST_ADDRESS
        )

        /** A tab bar scrolls too: we only want the large container. */
        const val MIN_SCROLL_HEIGHT = 400
        const val MAX_SCROLLS = 8

        /** Enough to get out of a settings sub-screen, not enough to quit a game. */
        const val MAX_BACKS = 3

        /** Three fields, plus one retry each: past that, the screen is not reading us. */
        const val MAX_WRITES = 6
        const val MAX_ANCESTOR_HOPS = 5
        /**
         * The PS2 route is longer than the others, and a ceiling set too low
         * reads as "the setup does not work".
         * pourquoi : docs/decisions/pilotes-emulateurs.md § Two caps, two failures avoided
         */
        const val MAX_NAV_CLICKS = 8

        /** Enough to let a transition draw before drawing conclusions. */
        const val UNKNOWN_BEFORE_BACK = 4
        const val MAX_NODES = 600
    }
}
