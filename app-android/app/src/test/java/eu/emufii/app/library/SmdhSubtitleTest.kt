package eu.emufii.app.library

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The short SMDH description truncates: A Link Between Worlds is called "The Legend of
 * Zelda" there, so two Zeldas carried the same name and the icon search brought back
 * whichever came first. Taking the long one systematically is worse: it is sometimes
 * cover-art copy.
 */
class SmdhSubtitleTest {

    /**
     * The European dump: the separator is a line break, not punctuation. The rule's first
     * version normalised whitespace before looking for the separator, and missed it.
     */
    @Test
    fun `the SMDH's line break separates the title from its subtitle`() {
        assertEquals(
            "The Legend of Zelda: A Link Between Worlds",
            fullTitle("The Legend of Zelda", "The Legend of Zelda\nA Link Between Worlds")
        )
    }

    @Test
    fun `a subtitle after a colon is taken`() {
        assertEquals(
            "Bravely Default: Flying Fairy",
            fullTitle("Bravely Default", "Bravely Default: Flying Fairy")
        )
    }

    @Test
    fun `a subtitle after an em dash is taken`() {
        assertEquals(
            "Bravely Default: Flying Fairy",
            fullTitle("Bravely Default", "Bravely Default — Flying Fairy")
        )
    }

    @Test
    fun `a tagline on the second line is set aside`() {
        assertEquals(
            "Mario Kart 7",
            fullTitle("Mario Kart 7", "Mario Kart 7\nRace your friends!")
        )
    }

    @Test
    fun `a tagline stuck to the title is set aside`() {
        assertEquals(
            "Mario Kart 7",
            fullTitle("Mario Kart 7", "Mario Kart 7 is back and faster")
        )
    }

    @Test
    fun `an unrelated long one is set aside`() {
        assertEquals(
            "Luigi's Mansion",
            fullTitle("Luigi's Mansion", "Chase ghosts through a haunted manor")
        )
    }

    @Test
    fun `a long one identical to the short one changes nothing`() {
        assertEquals("Majora's Mask 3D", fullTitle("Majora's Mask 3D", "Majora's Mask 3D"))
    }

    @Test
    fun `an empty long one changes nothing`() {
        assertEquals("Persona Q", fullTitle("Persona Q", ""))
    }
}
