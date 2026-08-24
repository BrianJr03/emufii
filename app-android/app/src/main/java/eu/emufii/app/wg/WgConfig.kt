package eu.emufii.app.wg

/**
 * What the coordinator hands back when you claim an address. No other player's
 * key: the topology is hub-and-spoke.
 * pourquoi : docs/decisions/tunnel-wireguard.md § Trois nombres mesurés dans la configuration
 */
data class WgTunnelInfo(
    /** This device's address on the session subnet, e.g. `10.67.1.2`. */
    val address: String,
    /**
     * The host's second address, null on a guest. Without it, packets sent to
     * the host arrive through the tunnel and are dropped.
     * pourquoi : docs/decisions/tunnel-wireguard.md § La seconde adresse de l'hôte, sans quoi ses paquets se perdent
     */
    val hairpinAddress: String? = null,
    /** The session subnet, e.g. `10.67.1.0/24`. */
    val subnet: String,
    val relayEndpoint: String,
    val relayPublicKey: String,
    /** The relay's `AllowedIPs`: the session subnet plus the relay's own /32. */
    val relayAllowedIps: String
)

/**
 * Renders a wg-quick configuration. Text rather than the builders: one shape to
 * get right, and loggable when a tunnel refuses to come up.
 * pourquoi : docs/decisions/tunnel-wireguard.md § Trois nombres mesurés dans la configuration
 */
object WgConfig {

    /**
     * Carrier NAT mappings expire well under a minute. Lowered from 25 s on
     * 2026-08-02.
     * pourquoi : docs/decisions/tunnel-wireguard.md § Trois nombres mesurés dans la configuration
     */
    const val KEEPALIVE_SECONDS = 10

    /**
     * Without this the backend defaults to 1280, the IPv6 floor. Measured on
     * the Thor: 1252 bytes get through, 1300 is lost, nothing fragments.
     * pourquoi : docs/decisions/tunnel-wireguard.md § Trois nombres mesurés dans la configuration
     */
    const val MTU = 1420

    /** The relay's address inside the tunnel, which also answers DNS. */
    const val RELAY_ADDRESS = "10.67.0.1"

    /**
     * The name a PS2 guest types instead of an address: ARMSX2's keyboard has
     * no dot key, so no IPv4 address can be entered at all.
     * pourquoi : docs/decisions/tunnel-wireguard.md § Le DNS n'est annoncé que pour la PS2
     */
    const val PS2_HOST_NAME = "emufii"

    fun render(
        info: WgTunnelInfo,
        privateKeyBase64: String,
        /**
         * The DNS to advertise, null everywhere but PS2 — a VPN advertising one
         * takes over the whole device's resolution.
         * pourquoi : docs/decisions/tunnel-wireguard.md § Le DNS n'est annoncé que pour la PS2
         */
        dns: String? = null
    ): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = $privateKeyBase64")
        val addresses = listOfNotNull(info.address, info.hairpinAddress)
        // The interface address must carry the session prefix and not a /32:
        // Eden reads the mask to hand it to the game. See docs/NOTES_TUNNEL.md.
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

    /** The same thing with the private key replaced, for logs and bug reports. */
    fun renderRedacted(info: WgTunnelInfo, dns: String? = null): String =
        render(info, "«clé privée retirée»", dns)
}
