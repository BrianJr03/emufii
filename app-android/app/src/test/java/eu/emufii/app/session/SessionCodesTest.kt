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
    fun `a code of the wrong length is left as it was typed`() {
        // Anything but six characters must fail as a wrong code rather than be
        // reshaped into a different session.
        assertEquals("HMM-29", SessionCodes.normalize("hmm-29"))
        assertEquals("HMM2955", SessionCodes.normalize("hmm2955"))
        assertEquals("", SessionCodes.normalize(""))
    }

    @Test
    fun `what generate produces survives normalize untouched`() {
        repeat(50) {
            val code = SessionCodes.generate()
            assertEquals(code, SessionCodes.normalize(code))
            assertTrue(code, Regex("^[A-Z]{3}-[2-9]{3}$").matches(code))
        }
    }
}
