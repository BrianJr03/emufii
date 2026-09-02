package eu.emufii.app.eden

import eu.emufii.app.netplay.NetplayTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eden ships as a matrix of packages, two of which do not carry the word "eden", the
 * "Optimized" one presenting itself under Genshin Impact's identity. Two flaws live there:
 * not recognising a variant at all, and recognising several then opening the wrong one.
 * Both happened on the Thor on 2026-08-10.
 */
class EdenVariantTest {

    @Test
    fun `the Optimized variant is a known variant`() {
        // Recorded on the Thor: `com.miHoYo.Yuanshen.nightly`, launch activity
        // `org.yuzu.yuzu_emu.ui.main.MainActivity`. Emufii declared it nowhere, so
        // the player read "not installed" in front of an emulator that was very
        // much there.
        assertTrue(
            "the Optimized nightly must be recognised as an Eden",
            NetplayTarget.EDEN.packages.contains("com.miHoYo.Yuanshen.nightly")
        )
        assertEquals(
            NetplayTarget.EDEN,
            NetplayTarget.forPackage("com.miHoYo.Yuanshen.nightly")
        )
    }

    @Test
    fun `the most recently installed one wins, whatever its rank in the list`() {
        // The real case: the stable installed on 2026-08-08, the Optimized on
        // 2026-08-10. The list's order puts the stable first, and yet the other is
        // the one the player wants, having just installed it.
        val installed = listOf(
            "dev.eden.eden_emulator" to 1_754_600_000_000L,
            "com.miHoYo.Yuanshen.nightly" to 1_754_800_000_000L
        )
        assertEquals("com.miHoYo.Yuanshen.nightly", pickEden(installed))
    }

    @Test
    fun `on equal dates the list order decides, and the fork comes first`() {
        // Our fork is the only one that lets the network interface be chosen: on
        // a device that has both, targeting the official Eden amounts to never
        // leaving the tunnel.
        val same = 1_754_800_000_000L
        val installed = listOf(
            "dev.eden.eden_emulator.emufii" to same,
            "dev.eden.eden_emulator" to same
        )
        assertEquals("dev.eden.eden_emulator.emufii", pickEden(installed))
    }

    @Test
    fun `no variant installed stays a case of its own`() {
        assertNull(pickEden(emptyList()))
    }
}
