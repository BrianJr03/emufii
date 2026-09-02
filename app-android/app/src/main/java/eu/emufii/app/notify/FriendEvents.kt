package eu.emufii.app.notify

import eu.emufii.app.profile.FriendStatus

/** Only the start of something joinable interrupts: going offline is not an event. */
sealed interface FriendEvent {
    val code: String
    val name: String?

    data class CameOnline(override val code: String, override val name: String?) : FriendEvent

    data class StartedPlaying(
        override val code: String,
        override val name: String?,
        val game: String?
    ) : FriendEvent
}

/**
 * The game is kept by title, not by session code: relaunching the same game opens a new
 * session, and announcing it twice teaches people to ignore the notification.
 */
data class SeenFriend(val online: Boolean, val game: String?)

/**
 * Pure by design: the same function serves the in-app alert and the background job, so
 * the two cannot diverge, and it is the only part testable without a device.
 * pourquoi : docs/decisions/amis-et-notifications.md § The announcement rules, each earned by picturing the notification it avoids
 */
fun friendEvents(
    previous: Map<String, SeenFriend>,
    current: Map<String, FriendStatus>,
    names: Map<String, String?> = emptyMap()
): List<FriendEvent> {
    val events = mutableListOf<FriendEvent>()

    for ((code, status) in current) {
        val before = previous[code] ?: continue
        if (!status.online) continue

        val name = names[code]
        val game = status.romTitle

        when {
            // Arriving straight into a game: one line, the one that says the most.
            !before.online && game != null -> events += FriendEvent.StartedPlaying(code, name, game)
            !before.online -> events += FriendEvent.CameOnline(code, name)
            game != null && game != before.game -> events += FriendEvent.StartedPlaying(code, name, game)
        }
    }

    return events
}

fun seenFrom(current: Map<String, FriendStatus>): Map<String, SeenFriend> =
    current.mapValues { (_, s) -> SeenFriend(online = s.online, game = s.romTitle) }
