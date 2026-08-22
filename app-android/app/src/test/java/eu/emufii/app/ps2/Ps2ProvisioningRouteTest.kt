package eu.emufii.app.ps2

import org.junit.Assert.assertEquals
import org.junit.Test

class Ps2ProvisioningRouteTest {
    @Test fun `drawer overlay wins over the home screen below it`() {
        assertEquals(
            Ps2ProvisioningRoute.Screen.DRAWER,
            Ps2ProvisioningRoute.classify(onManager = true, inDrawer = true, onHome = true),
        )
    }

    @Test fun `memory manager without a drawer marker remains a manager`() {
        assertEquals(
            Ps2ProvisioningRoute.Screen.MEMORY_CARDS,
            Ps2ProvisioningRoute.classify(onManager = true, inDrawer = false, onHome = false),
        )
    }

    @Test fun `route cannot toggle the top left button while a transition settles`() {
        val route = Ps2ProvisioningRoute()
        assertEquals(Ps2ProvisioningRoute.Action.OPEN_DRAWER, route.next(Ps2ProvisioningRoute.Screen.HOME))
        route.performed(Ps2ProvisioningRoute.Action.OPEN_DRAWER)
        repeat(3) {
            assertEquals(Ps2ProvisioningRoute.Action.WAIT, route.next(Ps2ProvisioningRoute.Screen.HOME))
        }
        assertEquals(Ps2ProvisioningRoute.Action.OPEN_DRAWER, route.next(Ps2ProvisioningRoute.Screen.HOME))
    }

    @Test fun `only a manager entered through the drawer is trusted as global`() {
        val route = Ps2ProvisioningRoute()
        assertEquals(
            Ps2ProvisioningRoute.Action.BACK_TO_HOME,
            route.next(Ps2ProvisioningRoute.Screen.MEMORY_CARDS),
        )
        route.performed(Ps2ProvisioningRoute.Action.BACK_TO_HOME)
        assertEquals(Ps2ProvisioningRoute.Action.OPEN_DRAWER, route.next(Ps2ProvisioningRoute.Screen.HOME))
        route.performed(Ps2ProvisioningRoute.Action.OPEN_DRAWER)
        assertEquals(Ps2ProvisioningRoute.Action.OPEN_MEMORY_CARDS, route.next(Ps2ProvisioningRoute.Screen.DRAWER))
        route.performed(Ps2ProvisioningRoute.Action.OPEN_MEMORY_CARDS)
        assertEquals(
            Ps2ProvisioningRoute.Action.USE_MEMORY_CARDS,
            route.next(Ps2ProvisioningRoute.Screen.MEMORY_CARDS),
        )
    }
}
