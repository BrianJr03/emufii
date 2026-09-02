package eu.emufii.app.notify

import eu.emufii.app.profile.FriendStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendEventsTest {

    private fun online(game: String? = null) = FriendStatus(
        online = true,
        sessionCode = game?.let { "ABCD" },
        romTitle = game
    )

    @Test
    fun `a friend never seen before announces nothing`() {
        val events = friendEvents(
            previous = emptyMap(),
            current = mapOf("A" to online())
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `coming online announces once`() {
        val previous = mapOf("A" to SeenFriend(online = false, game = null))
        val events = friendEvents(previous, mapOf("A" to online()), mapOf("A" to "Clement"))

        assertEquals(1, events.size)
        val event = events.single()
        assertTrue(event is FriendEvent.CameOnline)
        assertEquals("Clement", event.name)
    }

    @Test
    fun `staying online announces nothing`() {
        val previous = mapOf("A" to SeenFriend(online = true, game = null))
        assertTrue(friendEvents(previous, mapOf("A" to online())).isEmpty())
    }

    @Test
    fun `arriving straight into a game announces the game and not the arrival`() {
        val previous = mapOf("A" to SeenFriend(online = false, game = null))
        val events = friendEvents(previous, mapOf("A" to online("Mario Kart")))

        assertEquals(1, events.size)
        assertEquals("Mario Kart", (events.single() as FriendEvent.StartedPlaying).game)
    }

    @Test
    fun `starting a game while already online announces it`() {
        val previous = mapOf("A" to SeenFriend(online = true, game = null))
        val events = friendEvents(previous, mapOf("A" to online("Smash")))

        assertEquals(1, events.size)
        assertTrue(events.single() is FriendEvent.StartedPlaying)
    }

    @Test
    fun `the same game twice announces once`() {
        val previous = mapOf("A" to SeenFriend(online = true, game = "Smash"))
        assertTrue(friendEvents(previous, mapOf("A" to online("Smash"))).isEmpty())
    }

    @Test
    fun `changing game announces the new one`() {
        val previous = mapOf("A" to SeenFriend(online = true, game = "Smash"))
        val events = friendEvents(previous, mapOf("A" to online("Mario Kart")))

        assertEquals("Mario Kart", (events.single() as FriendEvent.StartedPlaying).game)
    }

    @Test
    fun `going offline announces nothing`() {
        val previous = mapOf("A" to SeenFriend(online = true, game = "Smash"))
        assertTrue(friendEvents(previous, mapOf("A" to FriendStatus.Offline)).isEmpty())
    }

    @Test
    fun `leaving a game announces nothing`() {
        val previous = mapOf("A" to SeenFriend(online = true, game = "Smash"))
        assertTrue(friendEvents(previous, mapOf("A" to online())).isEmpty())
    }

    @Test
    fun `the memory carries what the next comparison needs`() {
        val seen = seenFrom(mapOf("A" to online("Smash"), "B" to FriendStatus.Offline))

        assertEquals(SeenFriend(online = true, game = "Smash"), seen["A"])
        assertEquals(SeenFriend(online = false, game = null), seen["B"])
        // A round trip through the comparison is silent: the property the feature rests on.
        assertTrue(friendEvents(seen, mapOf("A" to online("Smash"))).isEmpty())
    }

    @Test
    fun `several friends produce several events`() {
        val previous = mapOf(
            "A" to SeenFriend(online = false, game = null),
            "B" to SeenFriend(online = true, game = null),
            "C" to SeenFriend(online = true, game = "Smash")
        )
        val events = friendEvents(
            previous,
            mapOf("A" to online(), "B" to online("Tekken"), "C" to online("Smash"))
        )

        assertEquals(2, events.size)
        assertEquals(setOf("A", "B"), events.map { it.code }.toSet())
    }
}
