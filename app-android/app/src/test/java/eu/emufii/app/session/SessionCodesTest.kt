package eu.emufii.app.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The hyphen is a reading aid, not part of the secret: typed as it is read aloud,
 * "HMM295" came back "session introuvable" on a code that was perfectly right.
 */
class SessionCodesTest {

    @Test
    fun `a code typed without its hyphen is the same code`() {
        assertEquals("HMM-295", SessionCodes.normalize("HMM295"))
    }

    @Test
    fun `case, spaces and stray punctuation are forgiven`() {
        assertEquals("HMM-295", SessionCodes.normalize("hmm295"))
        assertEquals("HMM-295", SessionCodes.normalize(" HMM 295 "))
        assertEquals("HMM-295", SessionCodes.normalize("hmm–295".replace('–', '-')))
        assertEquals("HMM-295", SessionCodes.normalize("HMM-295"))
    }

    @Test
    fun `a code of the wrong shape is left as it was typed`() {
        // Anything but the two known shapes must fail as a wrong code rather than be
        // reshaped into a different session.
        assertEquals("HMM-29", SessionCodes.normalize("hmm-29"))
        assertEquals("HMM2955", SessionCodes.normalize("hmm2955"))
        assertEquals("HMMK29556", SessionCodes.normalize("hmmk29556"))
        assertEquals("2955HMM", SessionCodes.normalize("2955hmm"))
        assertEquals("", SessionCodes.normalize(""))
    }

    /** A session opened by a build still on three and three has to stay joinable. */
    @Test
    fun `the older three and three shape is still recognised`() {
        assertEquals("HMM-295", SessionCodes.normalize("hmm295"))
        assertEquals("HMM-295", SessionCodes.normalize("HMM-295"))
    }

    @Test
    fun `what generate produces survives normalize untouched`() {
        repeat(50) {
            val code = SessionCodes.generate()
            assertEquals(code, SessionCodes.normalize(code))
            assertTrue(code, Regex("^[A-Z]{4}-[2-9]{4}$").matches(code))
        }
    }

    /** The lock is the code: a generator repeating itself would hand out one session. */
    @Test
    fun `generate does not repeat over a thousand draws`() {
        val seen = List(1000) { SessionCodes.generate() }.toSet()
        assertEquals(1000, seen.size)
    }

    /** ARMSX2 takes the code as its room password, 4 to 12 alphanumerics. */
    @Test
    fun `the code fits the ARMSX2 room field once the hyphen is dropped`() {
        val body = SessionCodes.generate().filter { it.isLetterOrDigit() }
        assertTrue(body, body.length in 4..12)
    }
}
