package eu.emufii.app.notify

import android.content.Context
import eu.emufii.app.network.CoordinatorClient
import eu.emufii.app.profile.FriendStatus
import eu.emufii.app.profile.FriendStore
import eu.emufii.app.settings.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Who is around, while the app is running.
 *
 * One poll for the whole app, and that is the change: the friends screen used to
 * ask on its own, which meant nothing at all was known anywhere else. A player
 * sitting in their library had no way of learning that someone had just come
 * online, short of walking over to the list and waiting.
 *
 * What it produces is read in two ways. The list of statuses feeds the friends
 * screen, unchanged in spirit. The stream of events feeds the alerts, and where
 * an alert lands depends on whether anybody is looking: on screen if the app is
 * in front, in the system shade otherwise. The same comparison decides both, and
 * it is the comparison the background job uses too, against the same stored
 * memory, so nothing is ever announced twice.
 */
class FriendWatcher(context: Context, private val client: CoordinatorClient) {

    private val appContext = context.applicationContext
    private val store = FriendStore.get(appContext)
    private val state = WatchState(appContext)
    private val settings = SettingsStore.get(appContext)

    private val _statuses = MutableStateFlow<Map<String, FriendStatus>>(emptyMap())
    val statuses: StateFlow<Map<String, FriendStatus>> = _statuses.asStateFlow()

    /**
     * Alerts worth showing inside the app.
     *
     * Replay of one, so an alert raised the instant before a screen change is
     * still shown rather than falling into the gap. Extra buffer because three
     * friends can arrive between two polls and none of them should be dropped.
     */
    private val _alerts = MutableSharedFlow<FriendEvent>(replay = 1, extraBufferCapacity = 8)
    val alerts: SharedFlow<FriendEvent> = _alerts.asSharedFlow()

    /**
     * Polls until cancelled.
     *
     * The cadence is the friends screen's old one: a handful of rows, a request
     * the coordinator answers from memory, and a player who is watching for
     * someone to appear. It stops with the composition, which is what stops it
     * when the app is destroyed.
     */
    suspend fun run(codes: List<String>) {
        while (true) {
            poll(codes)
            delay(REFRESH_MS)
        }
    }

    private suspend fun poll(codes: List<String>) {
        if (codes.isEmpty()) {
            _statuses.value = emptyMap()
            return
        }

        val fresh = client.friendStatuses(codes).getOrNull() ?: return
        val current = codes.associateWith { code ->
            fresh[code]?.let {
                FriendStatus(
                    online = true,
                    sessionCode = it.sessionCode,
                    romTitle = it.romTitle,
                    romTitleId = it.romTitleId,
                    players = it.players,
                    ready = it.ready
                )
            } ?: FriendStatus.Offline
        }
        _statuses.value = current

        val known = store.friends.value.associate { it.code to it.name }
        val names = codes.associateWith { fresh[it]?.name ?: known[it] }
        store.noteNames(fresh.mapNotNull { (c, p) -> p.name?.let { c to it } }.toMap())

        // The memory advances whether or not anything is announced. A player who
        // switched the alerts off must not come back to a burst of everything
        // that happened while they were quiet.
        val events = friendEvents(state.seen(), current, names)
        state.setSeen(seenFrom(current))

        if (!settings.notifyFriends.value) return
        val onScreen = AppForeground.visible.value
        events.forEach { event ->
            if (onScreen) _alerts.tryEmit(event) else Notifications.friendEvent(appContext, event)
        }
    }

    private companion object {
        const val REFRESH_MS = 5_000L
    }
}
