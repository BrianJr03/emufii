package eu.emufii.app.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The index-title overlay, and the one rule it may never break: it replaces a
 * filename, never a title read out of the file or a name the player chose.
 *
 * Pure strings, for the same reason `CompatKeysTest` takes its inputs field by
 * field: the rule being pinned is overlay logic, and the readers have their own
 * tests.
 */
class GameTitlesTest {

    @Test fun `a filename-derived name is replaced by the index title`() {
        assertEquals(
            "Metroid Dread™",
            GameTitles.resolve(
                titles = mapOf("switch:0100938012344000" to "Metroid Dread™"),
                displayName = "Metroid Dread",
                filename = "Metroid Dread [NSP] 0919838.nsp",
                keys = listOf("switch:0100938012344000")
            )
        )
    }

    @Test fun `a title the file gave up is not touched`() {
        // The cartridge speaks its own language; the index has no say.
        assertNull(
            GameTitles.resolve(
                titles = mapOf("switch:0100938012344000" to "Metroid Dread™"),
                displayName = "Metroid Dread",
                filename = "whatever.nsp",
                keys = listOf("switch:0100938012344000")
            )
        )
    }

    @Test fun `a 3ds game is found under its family key`() {
        // `CTR-P-ARRJ`: the region letter is dropped, exactly as for a badge.
        assertEquals(
            "Pokémon Omega Ruby",
            GameTitles.resolve(
                titles = mapOf("3ds:ARR" to "Pokémon Omega Ruby"),
                displayName = "omega",
                filename = "omega.cia",
                keys = listOf("3ds:ARR", "3ds:ARRJ", "3ds:t:000400000011C400")
            )
        )
    }

    @Test fun `an unknown game keeps its filename`() {
        assertNull(
            GameTitles.resolve(
                titles = emptyMap(),
                displayName = "Homebrew",
                filename = "Homebrew (v2).nsp",
                keys = listOf("switch:0100F00000000000")
            )
        )
    }
}
