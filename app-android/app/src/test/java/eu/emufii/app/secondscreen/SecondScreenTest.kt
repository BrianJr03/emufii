package eu.emufii.app.secondscreen

import eu.emufii.app.library.Console
import eu.emufii.app.session.Session
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The holder is three lines, and it is still worth pinning.
 *
 * What it guarantees is not the assignment but the contract the second host
 * depends on: the model is readable at process scope, and clearing really goes
 * back to the resting face. A regression here shows up as a session code left
 * glowing on the back of a handheld after the session ended, which is the one
 * failure this feature would be remembered for.
 */
class SecondScreenTest {

    @After fun reset() = SecondScreen.clear()

    @Test fun `starts idle`() {
        assertEquals(SecondScreenModel.Idle, SecondScreen.model.value)
    }

    @Test fun `publishes a session and reads it back`() {
        SecondScreen.publish(
            SecondScreenModel.InSession(
                code = "HMM-295",
                role = Session.Role.HOST,
                console = Console.PS2,
                gameTitle = "Midnight Club 3",
            )
        )
        val model = SecondScreen.model.value
        assertTrue(model is SecondScreenModel.InSession)
        assertEquals("HMM-295", (model as SecondScreenModel.InSession).code)
    }

    @Test fun `clearing returns to idle`() {
        SecondScreen.publish(
            SecondScreenModel.InSession(
                code = "ABC-234",
                role = Session.Role.GUEST,
                console = null,
                gameTitle = null,
            )
        )
        SecondScreen.clear()
        assertEquals(SecondScreenModel.Idle, SecondScreen.model.value)
    }
}
