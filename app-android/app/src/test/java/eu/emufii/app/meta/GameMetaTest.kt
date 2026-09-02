package eu.emufii.app.meta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class GameMetaTest {

    @Test fun `an entry is found by any of its keys`() {
        val db = GameMetaDb.parse(
            """
            {"games":[{"keys":["ps2:SLES-53717","ps2:SLUS-21355"],
              "genre_fr":"Course","genre_en":"Racing","released":"2005-04-12",
              "summary_fr":"Des voitures.","summary_en":"Cars."}]}
            """
        )
        // One entry, filed under each of its keys: the map is read inside a cursor
        // move, so it is flattened on parse.
        assertEquals(2, db.size)
        assertEquals("Course", db.metaFor(listOf("ps2:SLUS-21355"))?.genreFr)
        assertEquals("Racing", db.metaFor(listOf("ps2:SLES-53717"))?.genreEn)
        assertNull(db.metaFor(listOf("ps2:SLES-00000")))
    }

    @Test fun `the summary falls back to the other language`() {
        val meta = GameMeta(keys = listOf("k"), summaryEn = "Cars.")
        assertEquals("Cars.", meta.summaryFor(Locale.FRENCH))
        assertEquals("Cars.", meta.summaryFor(Locale.ENGLISH))
    }

    @Test fun `only the first genre is printed`() {
        val meta = GameMeta(keys = listOf("k"), genreFr = "jeu d'action · jeu de tir")
        assertEquals("jeu d'action", meta.genreFor(Locale.FRENCH))
    }

    @Test fun `the genre is the word the source wrote, in each language`() {
        val meta = GameMeta(keys = listOf("k"), genreFr = "Course", genreEn = "Racing")
        assertEquals("Course", meta.genreFor(Locale.FRENCH))
        assertEquals("Racing", meta.genreFor(Locale.ENGLISH))
        assertEquals("Racing", GameMeta(keys = listOf("k"), genreEn = "Racing").genreFor(Locale.FRENCH))
    }

    @Test fun `a still served in clear is dropped`() {
        val db = GameMetaDb.parse(
            """{"games":[{"keys":["k"],"screenshots":["http://x/1.png","https://x/2.png"]}]}"""
        )
        assertEquals(listOf("https://x/2.png"), db.metaFor(listOf("k"))?.screenshots)
    }

    @Test fun `one broken entry costs one game`() {
        val db = GameMetaDb.parse(
            """{"games":[{"genre_fr":"Course"},{"keys":["k"],"genre_fr":"Action"}]}"""
        )
        assertEquals(1, db.size)
        assertEquals("Action", db.metaFor(listOf("k"))?.genreFr)
    }

    @Test fun `a document that is not one parses to nothing rather than throwing`() {
        assertEquals(0, GameMetaDb.parse("<html>portail captif</html>").size)
        assertEquals(0, GameMetaDb.parse("{}").size)
    }

    @Test fun `an entry with nothing to say knows it`() {
        assertTrue(GameMeta(keys = listOf("k")).isEmpty(Locale.FRENCH))
    }
}
