package eu.emufii.app.wg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rendered tunnel configuration. Every mistake here fails the same way, a tunnel
 * that comes up and carries nothing. Two lines are load-bearing: the subnet prefix on
 * the address, which Switch LDN reads as its mask (docs/M19_SWITCH_LDN.md), and the
 * relay's /32 in AllowedIPs.
 */
class WgConfigTest {

    private val info = WgTunnelInfo(
        address = "10.67.1.2",
        subnet = "10.67.1.0/24",
        relayEndpoint = "85.215.52.3:51820",
        relayPublicKey = "OuWkhmV54Idvxl1T+SAwtRdlyp3LXl2rfeZu6F/59Vk=",
        relayAllowedIps = "10.67.1.0/24,10.67.0.1/32"
    )

    private val privateKey = "aFakePrivateKeyForTestsOnly0000000000000000="

    @Test
    fun `the host carries its second address, the guest carries none`() {
        // The host's ad hoc server hands this address out to the other players: without
        // it here their packets arrive through the tunnel and are dropped.
        val host = WgConfig.render(info.copy(hairpinAddress = "10.67.1.254"), privateKey)
        assertTrue(host.contains("Address = 10.67.1.2/24, 10.67.1.254/24"))

        assertTrue(WgConfig.render(info, privateKey).contains("Address = 10.67.1.2/24\n"))
    }

    @Test
    fun `the MTU is declared, and below the carrier link's bar`() {
        // With no explicit line the backend falls back to 1280 and Switch LDN breaks on
        // it: discovery connects, then the game frames are dropped unfragmented. Measured
        // on the Thor, 1252 bytes of payload got through, 1300 did not.
        val out = WgConfig.render(info, privateKey)
        assertTrue(out.contains("MTU = 1420"))

        // The WireGuard header costs 60 bytes over IPv4: the carrying packet still has
        // to fit a 1492 PPPoE.
        assertTrue(WgConfig.MTU + 60 <= 1492)
    }

    @Test
    fun `renders both sections wg-quick expects`() {
        val out = WgConfig.render(info, privateKey)
        assertTrue(out.contains("[Interface]"))
        assertTrue(out.contains("[Peer]"))
        // wg-quick assigns every key after a [Peer] header to that peer.
        assertTrue(out.indexOf("[Interface]") < out.indexOf("[Peer]"))
    }

    @Test
    fun `the address is a slash 32`() {
        // On Android the routes come from the peer's AllowedIPs, so this only names the
        // device; a wider prefix would claim other players' addresses.
        assertTrue(WgConfig.render(info, privateKey).contains("Address = 10.67.1.2/24"))
    }

    @Test
    fun `allowed ips are passed through untouched`() {
        assertTrue(
            WgConfig.render(info, privateKey)
                .contains("AllowedIPs = 10.67.1.0/24,10.67.0.1/32")
        )
    }

    @Test
    fun `keepalive is set, because the phone is behind NAT`() {
        // Without it the relay loses its NAT mapping after a minute or so and the player
        // silently stops being reachable.
        val out = WgConfig.render(info, privateKey)
        assertTrue(out.contains("PersistentKeepalive = ${WgConfig.KEEPALIVE_SECONDS}"))
        // Upper bound from NAT traversal, lower bound from the radio. The move from 25
        // to 10 s was measured: an idle link costs 369 ms on the first packet, 46 ms warm.
        assertTrue(WgConfig.KEEPALIVE_SECONDS in 5..30)
    }

    @Test
    fun `the private key appears exactly once, and only in the interface section`() {
        val out = WgConfig.render(info, privateKey)
        assertEquals(1, out.split(privateKey).size - 1)
        val peerSection = out.substringAfter("[Peer]")
        assertFalse(peerSection.contains(privateKey))
    }

    @Test
    fun `the redacted form is complete but carries no private key`() {
        val redacted = WgConfig.renderRedacted(info)
        assertTrue(redacted.contains("Address = 10.67.1.2/24"))
        assertTrue(redacted.contains("Endpoint = 85.215.52.3:51820"))
        assertTrue(redacted.contains(info.relayPublicKey))
        assertFalse(redacted.contains(privateKey))
    }

    @Test
    fun `the relay is the only peer`() {
        val out = WgConfig.render(info, privateKey)
        assertEquals(1, out.split("[Peer]").size - 1)
    }
}
