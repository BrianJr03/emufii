package eu.emufii.app.azahar

import eu.emufii.app.R
import eu.emufii.app.dolphin.DolphinNetplayDriver
import eu.emufii.app.ps2.Ps2NetplayDriver
import eu.emufii.app.ps2.Ps2MemoryCardProvisioningDriver
import eu.emufii.app.ps2.Ps2ProvisioningAutomation
import eu.emufii.app.ps2.Ps2ProvisioningProgress
import eu.emufii.app.ps2.Ps2ProvisioningStore
import eu.emufii.app.ps2.Ps2Target
import eu.emufii.app.dolphin.DolphinTarget
import eu.emufii.app.netplay.NetplayLabels
import eu.emufii.app.netplay.NetplayTarget
import eu.emufii.app.netplay.NetplayUi
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Drives the netplay dialog of Azahar *and* Eden so the user doesn't have to
 * retype an IP that Emufii already knows. Which emulators: [NetplayTarget].
 *
 * Do not rename this class: Android stores the ComponentName when the user
 * enables the service, so a rename silently disables it for everyone.
 *
 * Inert unless [NetplayAutomation] holds a plan, and best-effort by design.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Why an accessibility service, and not something else
 */
class AzaharNetplayService : AccessibilityService() {

    private var lastStepAt = 0L

    /**
     * The cap is what stops an armed plan making the in-game drawer unusable.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § An armed plan must not make the game unusable
     */
    private var navClicks = 0
    private var navPlan: NetplayPlan? = null

    private val handler = Handler(Looper.getMainLooper())

    /** The pending re-read, so a new burst of events replaces it instead of piling on. */
    private var pendingLook: Runnable? = null

    private val store by lazy { PlanStore(this) }
    private val ps2ProvisioningStore by lazy { Ps2ProvisioningStore(this) }

    private val ps2ProvisioningDriver by lazy {
        Ps2MemoryCardProvisioningDriver(
            this,
            { performGlobalAction(GLOBAL_ACTION_BACK) },
        ) { success, reason ->
            val plan = Ps2ProvisioningAutomation.plan.value
            if (success && plan != null) {
                Ps2ProvisioningAutomation.complete(this, plan, ps2ProvisioningStore)
            } else {
                Ps2ProvisioningAutomation.fail(
                    reason ?: "The global ARMSX2 configuration could not be verified.",
                    ps2ProvisioningStore,
                )
            }
            comeBackToEmufii()
        }
    }

    /**
     * Dolphin's netplay screen is Compose and exposes no resource ids, so it cannot
     * enter the id-based walk the other two share.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Three screen families, three drivers, one standard
     */
    private val dolphinDriver by lazy {
        DolphinNetplayDriver(this) { success ->
            store.clear()
            if (success) comeBackToEmufii()
        }
    }

    /**
     * Re-reads the tree: ARMSX2 needs a dozen keyboard clicks, redrawing at each.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Three screen families, three drivers, one standard
     */
    private val ps2Driver by lazy {
        Ps2NetplayDriver(
            this,
            { rootInActiveWindow },
            { performGlobalAction(GLOBAL_ACTION_BACK) }
        ) { success ->
            store.clear()
            if (success) comeBackToEmufii()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // We may be starting because Android killed Emufii mid-flow and brought the
        // service back on its own.
        NetplayAutomation.restore(store)
        Ps2ProvisioningAutomation.restore(ps2ProvisioningStore)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        // Both emulators emit bursts of events per screen: one step per burst keeps
        // us from racing our own clicks.
        if (System.currentTimeMillis() - lastStepAt < STEP_DEBOUNCE_MS) return
        stepNow(pkg)
    }

    /**
     * Runs one step against what is on screen, then looks again: a view can arrive
     * after the last event of its own screen.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Acting on events is not enough: you have to look again
     */
    private fun stepNow(pkg: String, looksLeft: Int = RECHECKS) {
        if (Ps2Target.owns(pkg) && Ps2ProvisioningAutomation.plan.value != null) {
            stepProvisioningNow(pkg, looksLeft)
            return
        }
        val plan = NetplayAutomation.plan.value ?: return
        // Three families of screen, three drivers: a package none of them knows
        // leaves without anything being touched.
        // pourquoi : docs/decisions/pilotes-emulateurs.md § Three screen families, three drivers, one standard
        val dolphin = DolphinTarget.owns(pkg)
        val ps2 = Ps2Target.owns(pkg)
        val target = if (dolphin || ps2) null else NetplayTarget.forPackage(pkg) ?: return
        val root = rootInActiveWindow ?: return
        val advanced = try {
            when {
                dolphin -> dolphinDriver.step(root, pkg, plan)
                ps2 -> ps2Driver.step(root, pkg, plan)
                else -> step(root, pkg, target!!, plan)
            }
        } catch (t: Throwable) {
            // A malformed tree must not take down the service: the user would lose
            // accessibility until they toggled it back on by hand.
            Log.w(TAG, "netplay step failed", t)
            NetplayAutomation.report(
                NetplayProgress.Failed(getString(R.string.azahar_automation_stopped, "${plan.ip}:${plan.port}"))
            )
            false
        } finally {
            root.recycle()
        }
        if (advanced) lastStepAt = System.currentTimeMillis()
        pendingLook?.let { handler.removeCallbacks(it) }
        pendingLook = null
        // A pass that made progress earns its budget back, or the budget caps the
        // route's length and the PS2 stops halfway, silently.
        // pourquoi : docs/decisions/pilotes-emulateurs.md § Acting on events is not enough: you have to look again
        val looksNext = if (advanced) RECHECKS else looksLeft - 1
        if (looksNext > 0 && NetplayAutomation.plan.value != null) {
            val again = Runnable { stepNow(pkg, looksNext) }
            pendingLook = again
            handler.postDelayed(again, RECHECK_MS)
        }
    }

    private fun stepProvisioningNow(pkg: String, looksLeft: Int) {
        if (Ps2ProvisioningAutomation.expireIfNeeded(ps2ProvisioningStore)) {
            comeBackToEmufii()
            return
        }
        val plan = Ps2ProvisioningAutomation.plan.value ?: return
        if (!Ps2Target.owns(pkg)) return
        val root = rootInActiveWindow
        if (root == null) {
            if (looksLeft > 1) {
                val again = Runnable { stepProvisioningNow(pkg, looksLeft - 1) }
                pendingLook = again
                handler.postDelayed(again, RECHECK_MS)
            }
            return
        }
        val advanced = try {
            ps2ProvisioningDriver.step(root, plan)
        } catch (t: Throwable) {
            Log.w(TAG, "PS2 provisioning step failed", t)
            Ps2ProvisioningAutomation.fail(
                t.message ?: "The ARMSX2 screen could not be read.",
                ps2ProvisioningStore,
            )
            comeBackToEmufii()
            false
        } finally {
            root.recycle()
        }
        if (advanced) lastStepAt = System.currentTimeMillis()
        pendingLook?.let(handler::removeCallbacks)
        pendingLook = null
        val looksNext = if (advanced) RECHECKS else looksLeft - 1
        if (looksNext > 0 && Ps2ProvisioningAutomation.plan.value != null) {
            val again = Runnable { stepProvisioningNow(pkg, looksNext) }
            pendingLook = again
            handler.postDelayed(again, RECHECK_MS)
        }
    }

    private fun step(
        root: AccessibilityNodeInfo,
        pkg: String,
        target: NetplayTarget,
        plan: NetplayPlan
    ): Boolean {
        // Work backwards: the furthest-along screen wins, so we never re-open a menu
        // we already left.

        // 3. Room form is up.
        val ipField = root.findById(pkg, NetplayUi.IP_ADDRESS)
        if (ipField != null) {
            Log.d(TAG, "filling room form as ${plan.role}: ${plan.ip}:${plan.port} room=${plan.roomName} user=${plan.username}")
            NetplayAutomation.report(NetplayProgress.FillingForm)
            val wrote = ipField.fillText(plan.ip)
            root.findAnywhere(pkg, NetplayUi.IP_PORT)?.fillText(plan.port.toString())
            // Eden only, both roles; on Azahar the plan leaves it null, deliberately.
            // pourquoi : docs/decisions/pilotes-emulateurs.md § The nickname is written on Eden only
            plan.username?.let { root.findAnywhere(pkg, NetplayUi.USERNAME)?.fillText(it) }
            // Set only for a room on the VPS: it listens on a public port, and the
            // password is the session code both players already know.
            plan.password?.let { root.findAnywhere(pkg, NetplayUi.PASSWORD)?.fillText(it) }
            if (plan.role == NetplayPlan.Role.Host) {
                plan.roomName?.let { root.findAnywhere(pkg, NetplayUi.ROOM_NAME)?.fillText(it) }
                // Eden refuses to create a room without one, keeping OK disabled;
                // Azahar has no such field.
                plan.preferredGame?.let { game ->
                    NetplayUi.PREFERRED_GAME_IDS
                        .firstNotNullOfOrNull { root.findAnywhere(pkg, it) }
                        ?.fillText(game)
                }
            }

            // ACTION_SET_TEXT returns false on a node that won't take it: the one
            // failure that used to look like a success, on the guest's form.
            if (!wrote) {
                Log.w(TAG, "ip_address refused ACTION_SET_TEXT (role=${plan.role})")
                NetplayAutomation.report(
                    NetplayProgress.Failed(getString(R.string.azahar_automation_stopped, "${plan.ip}:${plan.port}"))
                )
                store.clear()
                return true
            }

            // NOT [findById]: OK is usually below the fold, and a visible-only lookup
            // finding nothing is the "click doesn't take" of legend.
            // pourquoi : docs/decisions/pilotes-emulateurs.md § Visibility is the right filter for locating yourself, the wrong one for acting
            val confirm = root.findAnywhere(pkg, NetplayUi.BTN_CONFIRM)
            confirm?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
            if (confirm == null) {
                NetplayAutomation.report(
                    NetplayProgress.Failed(getString(R.string.azahar_fields_filled))
                )
                store.clear()
                return true
            }
            NetplayAutomation.report(NetplayProgress.Confirming)
            // Eden's OK reports itself enabled and clickable and still ignores the
            // action.
            // pourquoi : docs/decisions/pilotes-emulateurs.md § A click that does not take is not a success
            if (!confirm.performClick()) {
                NetplayAutomation.report(
                    NetplayProgress.Failed(getString(R.string.azahar_fields_filled))
                )
                store.clear()
                return true
            }
            NetplayAutomation.report(NetplayProgress.Done)
            store.clear()
            comeBackToEmufii()
            return true
        }

        // 2. Multiplayer sheet is up. Any one visible button means "up"; the button
        // to press is looked up WITHOUT the visibility filter, a landscape sheet
        // cutting off the bottom one.
        // pourquoi : docs/decisions/pilotes-emulateurs.md § Visibility is the right filter for locating yourself, the wrong one for acting
        val sheetUp = SHEET_BUTTONS.any { root.findById(pkg, it) != null }
        if (sheetUp) {
            val modeId =
                if (plan.role == NetplayPlan.Role.Host) NetplayUi.BTN_CREATE else NetplayUi.BTN_JOIN
            val modeNode = root.findAnywhere(pkg, modeId)
            if (modeNode != null) {
                Log.d(TAG, "role=${plan.role} → clicking $modeId")
                NetplayAutomation.report(NetplayProgress.ChoosingMode)
                modeNode.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
                if (modeNode.performClick()) return true
                Log.w(TAG, "$modeId refused the click (role=${plan.role})")
            } else {
                // Absent from the tree: an upstream rename, not a scrolled layout;
                // walking back to the settings hub would re-open this sheet.
                Log.w(TAG, "multiplayer sheet up but $modeId absent (role=${plan.role})")
            }
            NetplayAutomation.report(
                NetplayProgress.Failed(
                    getString(R.string.azahar_automation_stopped, "${plan.ip}:${plan.port}")
                )
            )
            store.clear()
            return true
        }

        // Everything below walks the player towards the sheet rather than acting on
        // a screen they asked for, so it must know when to give up: [navClicks].
        if (navPlan !== plan) {
            navPlan = plan
            navClicks = 0
        }
        if (navClicks >= MAX_NAV_CLICKS) return false

        // 1. In-game menu is up. Only Azahar has one; on Eden the player reaches the
        // sheet from the app's settings, so the flow starts at step 2.
        target.inGameMenuId?.let { menuId ->
            root.findById(pkg, menuId)?.let {
                NetplayAutomation.report(NetplayProgress.OpeningMenu)
                navClicks++
                it.performClick()
                return true
            }
        }

        // 0b. Settings hub is up. The card is found by TEXT, the rows sharing ids,
        // read from the emulator's own resources, and the label may be either of two.
        // pourquoi : docs/decisions/pilotes-emulateurs.md § A recycling list does not contain what you have not seen yet
        val listId = (listOfNotNull(target.homeListId) + target.extraListIds)
            .firstOrNull { root.findById(pkg, it) != null }
        if (listId != null) {
            val labels = NetplayLabels.MULTIPLAYER_STRINGS
                .flatMap { NetplayLabels.of(this, pkg, it) }
                .map { it.trim().lowercase() }
            if (labels.isEmpty()) {
                Log.w(TAG, "no multiplayer label in $pkg's resources to match a card on")
                return false
            }
            // Not [findAllById]: a row below the fold is a real row. Two id families,
            // the gear and the tab not landing on the same kind of list.
            val titles = NetplayUi.ROW_TITLE_IDS.flatMap { id ->
                root.findAccessibilityNodeInfosByViewId(NetplayUi.id(pkg, id)).orEmpty()
            }
            val card = titles.firstOrNull { node ->
                node.text?.toString()?.trim()?.lowercase() in labels
            }
            if (card != null) {
                Log.d(TAG, "opening the '${card.text}' card in $pkg")
                NetplayAutomation.report(NetplayProgress.OpeningMenu)
                navClicks++
                card.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
                card.performClick()
                return true
            }
            // A recycling list only holds what it has drawn: one scroll per pass,
            // counted like a click so it cannot loop.
            // pourquoi : docs/decisions/pilotes-emulateurs.md § A recycling list does not contain what you have not seen yet
            val list = root.findById(pkg, listId)
            if (list != null && list.isScrollable) {
                navClicks++
                if (list.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                    Log.d(TAG, "multiplayer card not in view, scrolling the settings list")
                    return true
                }
            }
            Log.w(
                TAG,
                "no card matching $labels; saw " + titles.map { it.text }
            )
            return false
        }

        // 0a. Emulator is on its game grid. Multiplayer sits behind a bottom tab on
        // one emulator, a top-bar gear on the other; the absent one is simply absent.
        val settingsEntries = listOfNotNull(target.homeNavId) + target.homeSettingsButtonIds
        for (entryId in settingsEntries) {
            root.findById(pkg, entryId)?.let {
                Log.d(TAG, "opening $pkg's settings via $entryId")
                NetplayAutomation.report(NetplayProgress.OpeningMenu)
                navClicks++
                it.performClick()
                return true
            }
        }

        // Nothing recognised on screen, and not an error: the player is probably
        // still in the game.
        return false
    }

    /**
     * A node the player can currently see: the right filter for deciding
     * *where we are*, the wrong one for acting: see [findAnywhere].
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Visibility is the right filter for locating yourself, the wrong one for acting
     */
    /**
     * Brings Emufii back to the front, the room having been joined. Delayed, or
     * it races the emulator's own "joined" toast. Best-effort.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § We bring the player home
     */
    private fun comeBackToEmufii() {
        val home = packageManager.getLaunchIntentForPackage(packageName) ?: return
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        handler.postDelayed({
            runCatching { startActivity(home) }
                .onFailure { Log.w(TAG, "could not bring Emufii back", it) }
        }, COME_BACK_MS)
    }

    private fun AccessibilityNodeInfo.findById(pkg: String, id: String): AccessibilityNodeInfo? =
        findAllById(pkg, id).firstOrNull()

    /** A node in the tree, on screen or scrolled past the edge of it. */
    private fun AccessibilityNodeInfo.findAnywhere(pkg: String, id: String): AccessibilityNodeInfo? =
        findAccessibilityNodeInfosByViewId(NetplayUi.id(pkg, id))?.firstOrNull()

    private fun AccessibilityNodeInfo.findAllById(pkg: String, id: String): List<AccessibilityNodeInfo> =
        findAccessibilityNodeInfosByViewId(NetplayUi.id(pkg, id))
            ?.filter { it.isVisibleToUser }
            .orEmpty()

    /** Clicks the node, or the nearest ancestor that will take a click. */
    private fun AccessibilityNodeInfo.performClick(): Boolean {
        var node: AccessibilityNodeInfo? = this
        var hops = 0
        while (node != null && hops < MAX_ANCESTOR_HOPS) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            node = node.parent
            hops++
        }
        return false
    }

    /**
     * Deliberately not called `setText`: the platform has a member of that name,
     * and in Kotlin a member beats an extension.
     * pourquoi : docs/decisions/pilotes-emulateurs.md § `typeText` and not `setText`: a year of a green test proving nothing
     */
    private fun AccessibilityNodeInfo.fillText(value: String): Boolean {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        return performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        // Only the in-memory copy: the service dies with every process recycle,
        // exactly when the stored plan has to survive.
        NetplayAutomation.clear()
        Ps2ProvisioningAutomation.clear()
    }

    private companion object {
        const val TAG = "AzaharNetplay"
        const val STEP_DEBOUNCE_MS = 250L
        const val MAX_ANCESTOR_HOPS = 5

        /**
         * Any one on screen means the sheet is up: only the topmost is reliably
         * in view.
         * pourquoi : docs/decisions/pilotes-emulateurs.md § Visibility is the right filter for locating yourself, the wrong one for acting
         */
        val SHEET_BUTTONS = listOf(
            NetplayUi.BTN_LOBBY_BROWSER,
            NetplayUi.BTN_JOIN,
            NetplayUi.BTN_CREATE
        )

        /**
         * Four covers the longest real path; past that, the player is doing
         * something else.
         * pourquoi : docs/decisions/pilotes-emulateurs.md § An armed plan must not make the game unusable
         */
        const val MAX_NAV_CLICKS = 4

        const val RECHECK_MS = 500L
        const val RECHECKS = 6

        /** Long enough for the emulator to act on the confirm we just clicked. */
        const val COME_BACK_MS = 1500L
    }
}
