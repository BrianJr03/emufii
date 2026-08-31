package eu.emufii.app.ps2

import eu.emufii.app.azahar.NetplayPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The room code, the PS2 driver's only rule that can be tested without a screen.
 *
 * The rest of the driver talks to `AccessibilityNodeInfo`, which a JVM test
 * cannot construct, which is why screen reading lives in [Ps2Screen], pinned
 * against real trees in `Ps2ScreenTest`.
 */
class Ps2DriverTest {

    private fun plan(password: String?) = NetplayPlan(
        role = NetplayPlan.Role.Guest,
        ip = "10.67.1.2",
        port = Ps2Target.DEFAULT_PORT,
        password = password
    )

    private fun codeOf(password: String?): String? {
        val raw = plan(password).password?.filter { it.isLetterOrDigit() && it.code < 128 }
            ?: return null
        val cut = raw.take(Ps2Target.ROOM_CODE_LENGTH.last)
        return cut.takeIf { it.length >= Ps2Target.ROOM_CODE_LENGTH.first }
    }

    @Test
    fun `the session code doubles as the room code`() {
        // Both players already know it: nothing extra to transmit.
        assertEquals("K7M2QP", codeOf("K7M2QP"))
    }

    @Test
    fun `a code that is too long is cut to the ARMSX2 bounds`() {
        assertEquals("ABCDEFGHIJKL", codeOf("ABCDEFGHIJKLMNOP"))
    }

    @Test
    fun `a code that is too short is not invented`() {
        // Better to leave ARMSX2's own than to set one the other player will not
        // have: the emulator negotiates nothing.
        assertNull(codeOf("AB"))
        assertNull(codeOf(null))
    }

    @Test
    fun `punctuation is dropped, the ARMSX2 keyboard cannot type it`() {
        assertEquals("ABCD", codeOf("AB-CD"))
    }
}
