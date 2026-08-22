package eu.emufii.app.ps2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** JSON behavior is Android-backed; keep the assignment policy pure here. */
class Ps2ProvisioningPlanTest {
    @Test fun `plan preserves an original card only when requested`() {
        val plan = Ps2ProvisioningPlan("EmuFii-Network.ps2", "abc123", "mcd001.ps2", 42L)
        assertEquals("EmuFii-Network.ps2", plan.cardName)
        assertEquals("abc123", plan.cardSha256)
        assertEquals("mcd001.ps2", plan.sourceCardForSlot2)
        assertNull(Ps2ProvisioningPlan("EmuFii-Network.ps2", "abc123").sourceCardForSlot2)
    }

    @Test fun `blank nullable candidate is rejected by the same rule as the store`() {
        assertNull("".takeIf { it.isNotBlank() })
    }
}
