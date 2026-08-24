package eu.emufii.app.dolphin

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import eu.emufii.app.R
import eu.emufii.app.azahar.NetplayAutomation
import eu.emufii.app.azahar.NetplayPlan
import eu.emufii.app.azahar.NetplayProgress
import eu.emufii.app.netplay.NetplayLabels

/**
 * Fills Dolphin's Netplay Setup screen with the address Emufii already knows.
 * Separate from the Azahar/Eden walk on purpose, so a Dolphin change cannot
 * break a 3DS or Switch session. Best-effort, like its sibling.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Lire un formulaire Compose sans un seul identifiant
 */
class DolphinNetplayDriver(
    private val context: Context,
    /**
     * Called once the flow is over. True when the room was actually joined or
     * opened; the plan is cleared either way.
     */
    private val onFinished: (success: Boolean) -> Unit
) {

    /**
     * Resolved labels, read once: each costs ~30 lookups across every locale
     * Dolphin might run in, and they cannot change while it runs.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Les libellés sont résolus une fois
     */
    private val labels = HashMap<String, List<String>>()

    /** Nav clicks spent on the current plan; the Azahar side is capped the same way. */
    private var navClicks = 0
    private var navPlan: NetplayPlan? = null

    /** Same for the lobby: opening the game list is not retried forever. */
    private var lobbyClicks = 0

    /** Returns true if this pass advanced the flow. */
    fun step(root: AccessibilityNodeInfo, pkg: String, plan: NetplayPlan): Boolean {
        // A fresh plan resets every counter, and it has to happen before the
        // screen is read: the furthest-along steps come first, so a ceiling
        // inherited from the previous session would block the lobby on the very
        // first pass.
        if (navPlan !== plan) {
            navPlan = plan
            navClicks = 0
            lobbyClicks = 0
            DolphinTreeDump.reset()
        }

        val nodes = flatten(root)
        val direct = labelsFor(pkg, DolphinTarget.LABEL_DIRECT_CONNECTION)
        val traversal = labelsFor(pkg, DolphinTarget.LABEL_TRAVERSAL_SERVER)
        // One trace per pass, plus the count of resolved labels: when this driver
        // does nothing, the question is always "did it see the screen?" and "could
        // it read the emulator's words?". Without that, a silent driver and a
        // driver that was never called are indistinguishable.
        Log.d(TAG, "passe: ${nodes.size} nœuds, direct=${direct.size} libellés")

        // Work backwards, furthest-along screen first, so we never re-open
        // something we have already left.

        // 4. The lobby is open. FIRST because it is the last screen: the form
        //    is behind us and must not be touched again.
        //    pourquoi : docs/decisions/pilotes-emulateurs.md § L'ordre des écrans, et les deux pièges qu'il évite
        val gameField = DolphinScreen.fieldFor(nodes, labelsFor(pkg, DolphinTarget.LABEL_GAME))
        if (gameField != null) return settleLobby(gameField, plan)

        // 3b. The game list, and `lobbyClicks > 0` is NOT a detail: without it
        //     this fires on Dolphin's start-up grid and launches the game.
        //     pourquoi : docs/decisions/pilotes-emulateurs.md § L'ordre des écrans, et les deux pièges qu'il évite
        val wantedGame = plan.preferredGame
        if (wantedGame != null && lobbyClicks > 0 && nodes.none { it.isField }) {
            DolphinScreen.looseOption(nodes, wantedGame)?.let {
                Log.d(TAG, "choix du jeu « ${it.text} » pour « $wantedGame »")
                return it.live.click()
            }
        }

        // 3. Direct connection, never Traversal: it would route through
        //    Dolphin's STUN server and remove the port field entirely.
        //    pourquoi : docs/decisions/pilotes-emulateurs.md § L'ordre des écrans, et les deux pièges qu'il évite
        if (DolphinScreen.isDropdownOpen(nodes, direct, traversal)) {
            val option = DolphinScreen.option(nodes, direct)
            if (option == null) {
                giveUp(plan, R.string.netplay_automation_stopped)
                return true
            }
            Log.d(TAG, "picking direct connection")
            return option.live.click()
        }

        // 2. The form is up, the nickname field is on both tabs, so it is what
        //    identifies the screen.
        if (DolphinScreen.fieldFor(nodes, labelsFor(pkg, DolphinTarget.LABEL_NICKNAME)) != null) {
            return fillForm(nodes, pkg, plan, direct)
        }

        // Capped below here: we are walking the player towards a screen they
        // did not ask for. The ceiling is the moment to photograph.
        // pourquoi : docs/decisions/pilotes-emulateurs.md § Le plafond de navigation est le moment à photographier
        if (navClicks >= MAX_NAV_CLICKS) {
            DolphinTreeDump.capture(context, pkg, nodes, "plafond de $MAX_NAV_CLICKS clics de navigation atteint")
            return false
        }

        // 1. Netplay row, by TEXT not by id: appcompat renders titles into a
        //    view carrying `id/title`, so the item id never reaches the tree.
        //    pourquoi : docs/decisions/pilotes-emulateurs.md § Le bouton de débordement se trouve par sa forme
        DolphinScreen.option(nodes, labelsFor(pkg, DolphinTarget.LABEL_MENU_NETPLAY))?.let {
            Log.d(TAG, "opening netplay from the grid menu")
            NetplayAutomation.report(NetplayProgress.OpeningMenu)
            navClicks++
            return it.live.click()
        }

        // 0. The game grid → open the overflow that holds that row.
        val overflow = DolphinScreen.overflow(nodes, nodes.first().bounds)
        if (overflow == null) {
            Log.w(TAG, "bouton de débordement introuvable parmi ${nodes.size} nœuds")
            DolphinTreeDump.capture(context, pkg, nodes, "bouton de débordement introuvable")
            return false
        }
        Log.d(TAG, "ouverture du menu de débordement")
        NetplayAutomation.report(NetplayProgress.OpeningMenu)
        navClicks++
        return overflow.live.click()
    }

    /**
     * The lobby is reached: set the game, then hand back. **Never click
     * "Start"** — that is the host's decision, not ours.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § L'ordre des écrans, et les deux pièges qu'il évite
     */
    private fun settleLobby(gameField: Node, plan: NetplayPlan): Boolean {
        val wanted = plan.preferredGame
        // The guest does not get to choose the game, the host decides, so for
        // them the lobby is already the destination.
        if (wanted == null) {
            Log.d(TAG, "salon atteint, aucun jeu à régler")
            finishInLobby()
            return true
        }
        if (DolphinScreen.looselyMatches(gameField.text, wanted)) {
            Log.d(TAG, "salon prêt : « ${gameField.text} » correspond à « $wanted »")
            finishInLobby()
            return true
        }
        // Bounded like the walk towards the menu: if the list does not open, or
        // the target title is not in it, we stop clicking the field under the
        // player's nose and leave them a working lobby, the connection itself
        // being made.
        if (lobbyClicks >= MAX_LOBBY_CLICKS) {
            Log.w(TAG, "jeu « $wanted » introuvable dans la liste, salon laissé tel quel")
            finishInLobby()
            return true
        }
        lobbyClicks++
        Log.d(TAG, "le salon affiche « ${gameField.text} », ouverture de la liste des jeux")
        NetplayAutomation.report(NetplayProgress.ChoosingMode)
        return gameField.live.click()
    }

    /** This driver's real end of the line: the lobby is open and configured. */
    private fun finishInLobby() {
        NetplayAutomation.report(NetplayProgress.Done)
        onFinished(true)
    }

    private fun fillForm(
        nodes: List<Node>,
        pkg: String,
        plan: NetplayPlan,
        direct: List<String>
    ): Boolean {
        val hosting = plan.role == NetplayPlan.Role.Host
        val roleLabels = labelsFor(
            pkg,
            if (hosting) DolphinTarget.LABEL_ROLE_HOST else DolphinTarget.LABEL_ROLE_CONNECT
        )
        val ipLabels = labelsFor(pkg, DolphinTarget.LABEL_IP_ADDRESS)

        // The right tab first. Connect and Host are two different forms, and the
        // host's has no address field at all, which is exactly what tells them
        // apart without reading a tab. Typing before checking would put the
        // address into whichever form happened to be showing.
        val onHostTab = DolphinScreen.fieldFor(nodes, ipLabels) == null
        if (onHostTab != hosting) {
            val tab = DolphinScreen.tab(nodes, roleLabels)
            if (tab == null) {
                giveUp(plan, R.string.netplay_automation_stopped)
                return true
            }
            Log.d(TAG, "switching to the ${if (hosting) "Host" else "Connect"} tab")
            NetplayAutomation.report(NetplayProgress.ChoosingMode)
            return tab.live.click()
        }

        // Connection type BEFORE anything is typed: switching it rebuilds the
        // form. One shared setting behind both tabs, so set once.
        // pourquoi : docs/decisions/pilotes-emulateurs.md § L'ordre des écrans, et les deux pièges qu'il évite
        val typeField = DolphinScreen.fieldFor(
            nodes,
            labelsFor(pkg, DolphinTarget.LABEL_CONNECTION_TYPE)
        )
        if (typeField != null && !typeField.text.matches(direct)) {
            Log.d(TAG, "connection type is '${typeField.text}', opening the dropdown")
            NetplayAutomation.report(NetplayProgress.ChoosingMode)
            return typeField.live.click()
        }

        NetplayAutomation.report(NetplayProgress.FillingForm)

        // The guest points at the host; the host has no address to be given.
        var wrote = true
        if (!hosting) {
            val ip = DolphinScreen.fieldFor(nodes, ipLabels)
            if (ip == null) {
                giveUp(plan, R.string.netplay_automation_stopped)
                return true
            }
            wrote = ip.live.fillText(plan.ip)
        }

        DolphinScreen.fieldFor(nodes, labelsFor(pkg, DolphinTarget.LABEL_PORT))
            ?.live?.fillText(plan.port.toString())

        // Dolphin gives everyone the same default nickname, "Player". Two of
        // those in one lobby is a room where neither player can tell who is who
        // - the same reason Eden's plan carries a name.
        plan.username?.let { name ->
            DolphinScreen.fieldFor(nodes, labelsFor(pkg, DolphinTarget.LABEL_NICKNAME))
                ?.live?.fillText(name)
        }

        // A field that refused the write is the failure that used to look like a
        // success on the Azahar side. Say it out loud.
        if (!wrote) {
            Log.w(TAG, "the address field refused ACTION_SET_TEXT")
            giveUp(plan, R.string.netplay_automation_stopped)
            return true
        }

        val commit = DolphinScreen.actionButton(nodes, roleLabels)
        if (commit == null) {
            giveUp(plan, R.string.netplay_fields_filled)
            return true
        }
        NetplayAutomation.report(NetplayProgress.Confirming)
        commit.live.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
        // A click the emulator ignores is not a success, Eden's OK button
        // reports itself clickable and still does nothing, and believing our own
        // request told the player the room was open when it was not.
        if (!commit.live.click()) {
            giveUp(plan, R.string.netplay_fields_filled)
            return true
        }
        // And above all NO `Done` here: it clears the plan, which would disarm
        // the driver just before the lobby it still has to handle.
        // pourquoi : docs/decisions/pilotes-emulateurs.md § L'ordre des écrans, et les deux pièges qu'il évite
        Log.d(TAG, "formulaire validé, en attente du salon")
        return true
    }

    private fun giveUp(plan: NetplayPlan, message: Int) {
        NetplayAutomation.report(
            NetplayProgress.Failed(context.getString(message, EMULATOR, "${plan.ip}:${plan.port}"))
        )
        onFinished(false)
    }

    private fun labelsFor(pkg: String, name: String): List<String> =
        // Keyed on the package *and* the name: the same string is read from
        // Dolphin and from us, and a key on the name alone would return the first
        // one for the second.
        labels.getOrPut("$pkg/$name") { NetplayLabels.of(context, pkg, name) }

    /** The tree as inert data, each entry still carrying the node it came from. */
    private fun flatten(root: AccessibilityNodeInfo): List<Node> =
        flattenRaw(root).map { node ->
            Node(
                text = node.text?.toString().orEmpty(),
                className = node.className?.toString().orEmpty(),
                bounds = node.boundsOnScreen(),
                ancestorClasses = ancestorsOf(node),
                viewId = node.viewIdResourceName?.substringAfter(":id/").orEmpty(),
                description = node.contentDescription?.toString().orEmpty(),
                clickable = node.isClickable,
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

    private fun AccessibilityNodeInfo.boundsOnScreen(): Bounds {
        val r = android.graphics.Rect().also { getBoundsInScreen(it) }
        return Bounds(r.left, r.top, r.right, r.bottom)
    }

    private fun ancestorsOf(node: AccessibilityNodeInfo): List<String> {
        val out = ArrayList<String>()
        var parent = node.parent
        var hops = 0
        while (parent != null && hops < MAX_ANCESTOR_HOPS) {
            out += parent.className?.toString().orEmpty()
            parent = parent.parent
            hops++
        }
        return out
    }

    /** The live node behind a [Node]; only ever absent in a test's synthetic tree. */
    private val Node.live: AccessibilityNodeInfo
        get() = handle as AccessibilityNodeInfo

    private fun String.matches(labels: List<String>): Boolean =
        trim().lowercase() in labels.map { it.trim().lowercase() }

    /** Clicks the node, or the nearest ancestor that will take a click. */
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

    /**
     * Types [value] into the field. **Not named `setText`** — a member beats an
     * extension in Kotlin, which cost the Azahar side months.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § `typeText` et non `setText` : un an de test vert qui ne prouvait rien
     */
    private fun AccessibilityNodeInfo.fillText(value: String): Boolean {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        return performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private companion object {
        const val TAG = "DolphinNetplay"

        /** Written into the fallback message, so it names the emulator it drove. */
        const val EMULATOR = "Dolphin"
        const val MAX_ANCESTOR_HOPS = 5
        const val MAX_NAV_CLICKS = 4
        const val MAX_LOBBY_CLICKS = 3

        /** A Compose tree is deep; a bound keeps a pathological screen from stalling us. */
        const val MAX_NODES = 600
    }
}
