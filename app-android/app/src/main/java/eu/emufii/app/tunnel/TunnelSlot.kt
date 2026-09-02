package eu.emufii.app.tunnel

import eu.emufii.app.wfc.WfcState
import eu.emufii.app.wg.WgState

/**
 * Android runs one `VpnService` at a time and Emufii has two: whichever calls `establish()`
 * second wins, silently.
 * pourquoi : docs/decisions/tunnel-wireguard.md § Android has one VPN slot, and Emufii has two tunnels
 */
enum class TunnelHolder { NONE, SESSION, WFC }

/**
 * `Starting` counts as held; SESSION wins ties.
 * pourquoi : docs/decisions/tunnel-wireguard.md § Android has one VPN slot, and Emufii has two tunnels
 */
fun tunnelHolder(
    session: WgState,
    wfc: WfcState
): TunnelHolder = when {
    session is WgState.Starting || session is WgState.Online || session is WgState.Offline ->
        TunnelHolder.SESSION
    wfc is WfcState.Active -> TunnelHolder.WFC
    else -> TunnelHolder.NONE
}

/**
 * Whether [want] can take the slot without cutting anything: asking for the one you hold is free.
 * pourquoi : docs/decisions/tunnel-wireguard.md § Android has one VPN slot, and Emufii has two tunnels
 */
fun slotIsFree(
    session: WgState,
    wfc: WfcState,
    want: TunnelHolder
): Boolean {
    val held = tunnelHolder(session, wfc)
    return held == TunnelHolder.NONE || held == want
}
