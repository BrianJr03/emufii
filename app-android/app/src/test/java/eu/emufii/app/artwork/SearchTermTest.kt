package eu.emufii.app.artwork

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What we send the catalogue to search for.
 *
 * A dump's name is not a game title: it drags its region and its revision along.
 * Searching for "Mario Kart 7 (USA) (Rev 1)" brings nothing back, and a game
 * that cannot be found is the cache's most expensive case, the one that produces
 * no image to remember.
 */
class SearchTermTest {

    @Test
    fun `strips the region and the revision`() {
        assertEquals("Mario Kart 7", SteamGridDb.searchTerm("Mario Kart 7 (USA) (Rev 1)"))
    }

    @Test
    fun `strips the markers in square brackets`() {
        assertEquals("Luigi's Mansion", SteamGridDb.searchTerm("Luigi's Mansion [!]"))
    }

    @Test
    fun `replaces filename separators with spaces`() {
        assertEquals("Kirby Planet Robobot", SteamGridDb.searchTerm("Kirby.Planet_Robobot"))
    }

    /** The suffix is part of the real title: removing it would break the search. */
    @Test
    fun `keeps the console suffix`() {
        assertEquals(
            "The Legend of Zelda Ocarina of Time 3D",
            SteamGridDb.searchTerm("The Legend of Zelda Ocarina of Time 3D (Europe)")
        )
    }

    @Test
    fun `an already clean title does not change`() {
        assertEquals("Majora's Mask 3D", SteamGridDb.searchTerm("Majora's Mask 3D"))
    }
}
