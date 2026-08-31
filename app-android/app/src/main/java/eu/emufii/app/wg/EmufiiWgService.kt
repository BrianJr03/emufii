package eu.emufii.app.wg

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import eu.emufii.app.MainActivity
import eu.emufii.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

/**
 * The session tunnel, carried by a foreground service.
 *
 * `GoBackend` ships its own `VpnService` but starts it with `startService` and
 * never calls `startForeground`, so the tunnel would die exactly when the player
 * switches to the emulator. Hence subclassing `GoBackend.VpnService` and
 * starting it ourselves. The service owns the tunnel's lifecycle so the ordering
 * is a property of the code rather than a hope about timing.
 * pourquoi : docs/decisions/tunnel-wireguard.md § Why Emufii has its own `VpnService`
 */
class EmufiiWgService : GoBackend.VpnService() {

    companion object {
        private const val TAG = "EmufiiWgService"
        private const val NOTIFICATION_ID = 5919813
        private const val CHANNEL_ID = "emufii_wg_vpn"
        private const val ACTION_START = "eu.emufii.app.wg.START"
        private const val ACTION_STOP = "eu.emufii.app.wg.STOP"
        private const val EXTRA_CODE = "code"
        private const val EXTRA_CONFIG = "config"
        private const val EXTRA_IP = "ip"

        /** Must match WireGuard's own name rules: `[a-zA-Z0-9_=+.-]{1,15}`. */
        private const val TUNNEL_NAME = "emufii"

        private val _state = MutableStateFlow(WgState.Idle as WgState)
        val state: StateFlow<WgState> = _state.asStateFlow()

        fun startIntent(ctx: Context, code: String, configText: String, ip: String): Intent =
            Intent(ctx, EmufiiWgService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_CODE, code)
                .putExtra(EXTRA_CONFIG, configText)
                .putExtra(EXTRA_IP, ip)

        fun stopIntent(ctx: Context): Intent =
            Intent(ctx, EmufiiWgService::class.java).setAction(ACTION_STOP)
    }

    private var backend: Backend? = null
    private var tunnel: SessionTunnel? = null
    private var scope: CoroutineScope? = null

    /**
     * Keeps the Wi-Fi radio awake for the session. Measured: 25 % loss at one
     * ping/s, 0 % at three, jitter 46→369 ms, and the Switch's LDN handshake is
     * made of exactly those rare packets.
     * pourquoi : docs/decisions/tunnel-wireguard.md § The Wi-Fi lock is not a comfort detail
     */
    private var wifiLock: WifiManager.WifiLock? = null

    /**
     * The library reports handshake progress here. `Online` means the interface
     * exists, not that a player joined, nor that a handshake landed.
     * pourquoi : docs/decisions/tunnel-wireguard.md § "Online" means less than you think
     */
    private inner class SessionTunnel(val code: String, val ip: String) : Tunnel {
        override fun getName(): String = TUNNEL_NAME

        override fun onStateChange(newState: Tunnel.State) {
            Log.d(TAG, "tunnel → $newState")
            _state.value = when (newState) {
                Tunnel.State.UP -> WgState.Online(code, ip)
                Tunnel.State.DOWN -> WgState.Offline(code)
                else -> _state.value
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")

        if (intent?.action == ACTION_STOP) {
            stopTunnel()
            return START_NOT_STICKY
        }

        val code = intent?.getStringExtra(EXTRA_CODE)
        val configText = intent?.getStringExtra(EXTRA_CONFIG)
        val ip = intent?.getStringExtra(EXTRA_IP)
        if (code == null || configText == null || ip == null) {
            // START_STICKY had the system restart us with a null intent; there is
            // no session to rejoin, so go away rather than sit on the VPN slot.
            Log.w(TAG, "started with no configuration, stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.svc_wg_connecting, code)))
        holdWifiAwake()
        _state.value = WgState.Starting(code)

        val s = scope ?: CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scope = it }
        s.launch {
            try {
                val config = Config.parse(ByteArrayInputStream(configText.toByteArray()))
                val b = backend ?: GoBackend(applicationContext).also { backend = it }
                val t = SessionTunnel(code, ip).also { tunnel = it }
                // Blocking, and deliberately off the main thread: the library
                // re-resolves the endpoint with one-second waits between attempts,
                // so this can sit for several seconds on a cold network.
                b.setState(t, Tunnel.State.UP, config)
                notify(getString(R.string.svc_wg_online, ip))
            } catch (e: Exception) {
                Log.e(TAG, "bringing the tunnel up: ${e.message}", e)
                _state.value = WgState.Error(e.message ?: "tunnel failed")
                stopSelf()
            }
        }

        // Not START_STICKY: a session is brokered by the coordinator and its peers
        // expire, so a tunnel resurrected blindly after a process death would
        // point at a game that no longer exists.
        return START_NOT_STICKY
    }

    /** Taken when the tunnel comes up, released when it falls. Never twice. */
    private fun holdWifiAwake() {
        if (wifiLock?.isHeld == true) return
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return
        // Low-latency mode has existed since Android 10 and minSdk is 33: there
        // is no fallback to write.
        val lock = runCatching {
            wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, TAG)
        }.getOrNull() ?: return
        // Without this, a lock taken twice would need releasing twice, and a
        // tunnel brought back up after a fall would leave the radio locked for
        // good.
        lock.setReferenceCounted(false)
        runCatching { lock.acquire() }
            .onSuccess { Log.d(TAG, "verrou Wi-Fi basse latence pris") }
            .onFailure { Log.w(TAG, "Wi-Fi lock refused", it) }
        wifiLock = lock
    }

    private fun releaseWifi() {
        wifiLock?.let { lock ->
            runCatching { if (lock.isHeld) lock.release() }
                .onFailure { Log.w(TAG, "releasing the Wi-Fi lock", it) }
        }
        wifiLock = null
    }

    private fun stopTunnel() {
        _state.value = WgState.Stopping
        val b = backend
        val t = tunnel
        val s = scope
        if (b != null && t != null && s != null) {
            s.launch {
                runCatching { b.setState(t, Tunnel.State.DOWN, null) }
                    .onFailure { Log.w(TAG, "stopping the tunnel: ${it.message}") }
                stopSelf()
            }
        } else {
            stopSelf()
        }
    }

    /**
     * Swiped out of recents: bring the tunnel down. A foreground service
     * survives task dismissal by design, so the VPN key outlived the app.
     * pourquoi : docs/decisions/tunnel-wireguard.md § Why Emufii has its own `VpnService`
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "Emufii swiped away, taking the session tunnel down")
        stopTunnel()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        releaseWifi()
        scope?.cancel()
        scope = null
        tunnel = null
        backend = null
        // Never skip the library's onDestroy: it resets the static future that
        // lets GoBackend find this service.
        // pourquoi : docs/decisions/tunnel-wireguard.md § Why Emufii has its own `VpnService`
        super.onDestroy()
        _state.value = WgState.Idle
    }

    private fun notify(text: String) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.svc_wg_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.svc_wg_channel_desc)
            }
        )
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.svc_wg_title))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }
}
