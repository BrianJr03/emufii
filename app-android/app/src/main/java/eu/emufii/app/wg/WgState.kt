package eu.emufii.app.wg

/**
 * Who holds the tunnel, and where it is in coming up. `Starting` is a state of
 * its own on purpose.
 * pourquoi : docs/decisions/tunnel-wireguard.md § Android n'a qu'un créneau VPN, et Emufii a deux tunnels
 */
sealed interface WgState {
    data object Idle : WgState

    /** `establish()` may already have happened, this counts as holding the slot. */
    data class Starting(val code: String) : WgState

    /**
     * The tunnel is up — meaning the interface exists, not that another player
     * has joined.
     * pourquoi : docs/decisions/tunnel-wireguard.md § « En ligne » veut dire moins qu'on ne croit
     */
    data class Online(val code: String, val ip: String) : WgState

    /** The interface exists but no handshake has landed yet, or it went stale. */
    data class Offline(val code: String) : WgState

    data object Stopping : WgState

    data class Error(val message: String) : WgState
}
