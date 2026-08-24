package eu.emufii.app.notify

import eu.emufii.app.profile.FriendStatus

/**
 * What changed about a friend since the last time we looked.
 *
 * Only two things are worth interrupting someone for, and both are the start of
 * something they can join: a friend appearing, and a friend starting a game.
 * Going offline and closing a game are not events, they are the absence of one.
 */
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
 * A friend as the last poll left them, reduced to what the next comparison
 * needs.
 *
 * The game is kept by title and not by session code: a player who quits and
 * relaunches the same game gets a new session, and announcing it twice would
 * teach people to ignore the notification.
 */
data class SeenFriend(val online: Boolean, val game: String?)

/**
 * Compares two polls and says what deserves to be announced.
 *
 * Pure, and that is the point: the same function serves the in-app alert and
 * the background job, so what the two announce cannot drift apart. It is also
 * the only part of this feature that can be tested without a device.
 *
 * The rules, and each one was earned by imagining the notification it prevents:
 *
 * - A friend we have never seen produces nothing. The first poll after adding
 *   someone, or after the app was killed for a day, would otherwise announce
 *   the whole list at once as if everyone had just arrived.
 * - Coming online announces once. If they are already in a game at that moment,
 *   the game is what gets announced, not both.
 * - Starting a game announces even for someone who was already online, which is
 *   the case that actually matters: they are there, and now there is something
 *   to join.
 * - A friend who was already in that same game produces nothing, however many
 *   times we poll.
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

/** The state to carry to the next comparison. */
fun seenFrom(current: Map<String, FriendStatus>): Map<String, SeenFriend> =
    current.mapValues { (_, s) -> SeenFriend(online = s.online, game = s.romTitle) }
