package eu.emufii.app.wg

/**
 * No other player's key: the topology is hub-and-spoke.
 * pourquoi : docs/decisions/tunnel-wireguard.md § Three numbers measured into the configuration
 */
data class WgTunnelInfo(
    val address: String,
    /**
     * Null on a guest. Without it, packets sent to the host arrive through the tunnel
     * and are dropped.
     * pourquoi : docs/decisions/tunnel-wireguard.md § The host's second address, without which its packets are lost
     */
    val hairpinAddress: String? = null,
    val subnet: String,
    val relayEndpoint: String,
    val relayPublicKey: String,
    /** The relay's `AllowedIPs`: the session subnet plus the relay's own /32. */
    val relayAllowedIps: String
)

/**
 * Text rather than the builders: one shape to get right, and loggable when a tunnel
 * refuses to come up.
 * pourquoi : docs/decisions/tunnel-wireguard.md § Three numbers measured into the configuration
 */
object WgConfig {

    /** Carrier NAT mappings expire well under a minute. */
    const val KEEPALIVE_SECONDS = 10

    /**
     * Without this the backend defaults to 1280, the IPv6 floor. Measured on the Thor:
     * 1252 bytes get through, 1300 is lost, nothing fragments.
     */
    const val MTU = 1420

    const val RELAY_ADDRESS = "10.67.0.1"

    /**
     * ARMSX2's keyboard has no dot key, so a PS2 guest cannot type an IPv4 address.
     * pourquoi : docs/decisions/tunnel-wireguard.md § DNS is advertised for the PS2 only
     */
    const val PS2_HOST_NAME = "emufii"

    fun render(
        info: WgTunnelInfo,
        privateKeyBase64: String,
        /**
         * Null everywhere but PS2: a VPN advertising a DNS takes over the device's
         * whole resolution.
         * pourquoi : docs/decisions/tunnel-wireguard.md § DNS is advertised for the PS2 only
         */
        dns: String? = null
    ): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = $privateKeyBase64")
        val addresses = listOfNotNull(info.address, info.hairpinAddress)
        // The session prefix, not a /32: Eden reads the mask to hand to the game.
        val prefix = info.subnet.substringAfter('/', "24")
        appendLine("Address = ${addresses.joinToString(", ") { "$it/$prefix" }}")
        appendLine("MTU = $MTU")
        dns?.let { appendLine("DNS = $it") }
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = ${info.relayPublicKey}")
        appendLine("Endpoint = ${info.relayEndpoint}")
        appendLine("AllowedIPs = ${info.relayAllowedIps}")
        appendLine("PersistentKeepalive = $KEEPALIVE_SECONDS")
    }

    fun renderRedacted(info: WgTunnelInfo, dns: String? = null): String =
        render(info, "<private key redacted>", dns)
}
