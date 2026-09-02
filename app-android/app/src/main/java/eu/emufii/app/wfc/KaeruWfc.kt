package eu.emufii.app.wfc

/**
 * Kaeru WFC, the third-party replacement for Nintendo's shut-down DS service,
 * reached by pointing the console's DNS at it. Emufii never routes to Nintendo.
 */
object KaeruWfc {

    /** A full recursive resolver, not just a WFC responder: melonDS's unrelated traffic keeps working. */
    const val DNS_SERVER = "178.62.43.212"

    /**
     * Advertised to Android in place of [DNS_SERVER]: the tunnel answers over a protected
     * socket, so Kaeru need not be routable through a tun. Clear of 10.67.x and 10.0.2.x.
     */
    const val SENTINEL_DNS = "10.66.53.53"

    const val TUN_ADDRESS = "10.66.53.2"

    const val DNS_PORT = 53

    /** Enough for EDNS0; larger is not a DNS query worth relaying. */
    const val MAX_DNS_MESSAGE = 4096
}
