package eu.emufii.app.wg

import android.content.Context
import android.content.Intent
import android.net.VpnService
import kotlinx.coroutines.flow.StateFlow

/**
 * Starting a tunnel provisions nothing anywhere: the coordinator hands back an address
 * and how to reach the relay.
 */
object EmufiiWgManager {

    val state: StateFlow<WgState> get() = EmufiiWgService.state

    fun prepare(ctx: Context): Intent? = VpnService.prepare(ctx)

    /**
     * Started as a foreground service, see the note in [EmufiiWgService] about `GoBackend`
     * starting its own in the background. [announceDns] is true for the PS2 only; see
     * [WgConfig.render]'s `dns` parameter for what that commits to.
     */
    fun start(ctx: Context, code: String, info: WgTunnelInfo, announceDns: Boolean = false) {
        val configText = WgConfig.render(
            info,
            WgKeys.privateKeyBase64(ctx),
            dns = if (announceDns) WgConfig.RELAY_ADDRESS else null
        )
        ctx.startForegroundService(
            EmufiiWgService.startIntent(ctx, code, configText, info.address)
        )
    }

    fun stop(ctx: Context) {
        ctx.startService(EmufiiWgService.stopIntent(ctx))
    }

    fun publicKey(ctx: Context): String = WgKeys.publicKeyBase64(ctx)
}
