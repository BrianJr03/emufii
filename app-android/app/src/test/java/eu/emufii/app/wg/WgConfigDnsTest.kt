package eu.emufii.app.wg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tunnel's DNS: present for the PS2, absent everywhere else.
 *
 * This test exists for the second half of that sentence. Announcing a DNS sends
 * the device's entire resolution through the relay; adding it inadvertently to
 * 3DS, Switch or PSP sessions would put a fresh point of failure on consoles
 * that work, and nothing on screen would say so.
 */
class WgConfigDnsTest {

    private val info = WgTunnelInfo(
        address = "10.67.1.2",
        subnet = "10.67.1.0/24",
        relayEndpoint = "relais.example:51820",
        relayPublicKey = "key",
        relayAllowedIps = "10.67.1.0/24,10.67.0.1/32,10.66.1.1/32"
    )

    @Test
    fun `with no DNS asked for, the config carries none`() {
        assertFalse(WgConfig.render(info, "private key").contains("DNS"))
    }

    @Test
    fun `the DNS asked for is the relay's, and it is in the AllowedIPs`() {
        val text = WgConfig.render(info, "private key", dns = WgConfig.RELAY_ADDRESS)
        assertTrue(text.contains("DNS = 10.67.0.1"))
        // A DNS outside the AllowedIPs would never be reached: the query would
        // go out over Wi-Fi and be lost, without a word.
        assertTrue(info.relayAllowedIps.contains("${WgConfig.RELAY_ADDRESS}/32"))
    }

    @Test
    fun `DNS lands in the interface, not in the peer`() {
        val text = WgConfig.render(info, "private key", dns = WgConfig.RELAY_ADDRESS)
        assertTrue(
            "DNS must come before [Peer], or wg-quick reads it as a peer setting",
            text.indexOf("DNS = ") < text.indexOf("[Peer]")
        )
    }

    @Test
    fun `the name typed by the PS2 guest can be entered on the ARMSX2 keyboard`() {
        // Lowercase letters only, no punctuation: that is the whole constraint,
        // and it is what dictated this detour.
        assertTrue(WgConfig.PS2_HOST_NAME.all { it in 'a'..'z' })
        assertEquals("emufii", WgConfig.PS2_HOST_NAME)
    }
}
