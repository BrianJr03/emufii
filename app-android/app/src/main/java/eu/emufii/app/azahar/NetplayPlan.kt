package eu.emufii.app.azahar

import eu.emufii.app.netplay.NetplayUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What Emufii wants the emulator's netplay dialog to end up configured as. */
data class NetplayPlan(
    val role: Role,
    val ip: String,
    val port: Int = NetplayUi.DEFAULT_PORT,
    val roomName: String? = null,
    /**
     * Filled for Eden, never for Azahar: two players with the same nickname cannot share an
     * Eden room, and Eden ships the same default to everybody. Azahar keeps its own, see
     * `NetplayNames`.
     */
    val username: String? = null,
    /** Eden makes this mandatory when hosting, its own dropdown; Azahar has no equivalent. */
    val preferredGame: String? = null,
    /**
     * Filled for the rooms the VPS holds: they listen on a public port. It is the session
     * code, which both sides already know, so nothing extra is transmitted.
     */
    val password: String? = null
) {
    enum class Role { Host, Guest }
}

sealed class NetplayProgress {
    data object Idle : NetplayProgress()
    data object OpeningMenu : NetplayProgress()
    data object ChoosingMode : NetplayProgress()
    data object FillingForm : NetplayProgress()
    data object Confirming : NetplayProgress()
    data object Done : NetplayProgress()

    /** [reason] is user-facing: the fallback is always "do it by hand", so it says what to type. */
    data class Failed(val reason: String) : NetplayProgress()
}

/**
 * An accessibility service is instantiated by the system, so there is no constructor to
 * pass the plan through. Process-global and single-slot: only one Azahar is ever in the
 * foreground, so a second plan means the flow was restarted and the previous one is stale.
 */
object NetplayAutomation {

    /** When [arm] last ran, for [neverStarted]. Not persisted: a plan that came
     *  back through [restore] means the service was alive enough to restore it. */
    private var armedAtMs = 0L

    private val _plan = MutableStateFlow<NetplayPlan?>(null)
    val plan: StateFlow<NetplayPlan?> = _plan.asStateFlow()

    private val _progress = MutableStateFlow<NetplayProgress>(NetplayProgress.Idle)
    val progress: StateFlow<NetplayProgress> = _progress.asStateFlow()

    /**
     * Call right before launching the ROM. [store] lets the plan survive Emufii being
     * killed while the emulator eats the memory, see [PlanStore]; optional only so a caller
     * with no context can arm in-memory.
     */
    fun arm(plan: NetplayPlan, store: PlanStore? = null) {
        _plan.value = plan
        _progress.value = NetplayProgress.OpeningMenu
        armedAtMs = System.currentTimeMillis()
        store?.save(plan)
    }

    fun clear(store: PlanStore? = null) {
        _plan.value = null
        _progress.value = NetplayProgress.Idle
        armedAtMs = 0L
        store?.clear()
    }

    /**
     * Called by the accessibility service when it starts: the system restarts it after
     * killing us, and without this it comes back with no way to know it had been asked.
     */
    fun restore(store: PlanStore) {
        if (_plan.value != null) return
        store.load()?.let {
            _plan.value = it
            _progress.value = NetplayProgress.OpeningMenu
            armedAtMs = System.currentTimeMillis()
        }
    }

    /**
     * Was the automation armed, given its chance, and never heard from? Total silence
     * is not a failure: its cause is an accessibility service that is bound but mute,
     * which is what an `install -r` leaves. Asked on the player's return, and bounded
     * by [SILENCE_MS].
     * pourquoi : docs/decisions/pilotes-emulateurs.md § Total silence is not a failure
     */
    fun neverStarted(now: Long = System.currentTimeMillis()): Boolean =
        _plan.value != null &&
            _progress.value == NetplayProgress.OpeningMenu &&
            armedAtMs > 0L &&
            now - armedAtMs > SILENCE_MS

    internal fun report(progress: NetplayProgress) {
        _progress.value = progress
        if (progress is NetplayProgress.Done || progress is NetplayProgress.Failed) {
            _plan.value = null
            armedAtMs = 0L
        }
    }

    private const val SILENCE_MS = 8_000L
}
