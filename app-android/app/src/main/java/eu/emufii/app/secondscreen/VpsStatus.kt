package eu.emufii.app.secondscreen

import eu.emufii.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Whether the machine that makes multiplayer possible is answering, live.
 *
 * The panel is the one place in the app where this belongs. On the front screen
 * a permanent indicator would be chrome: the player finds out the coordinator
 * is down the moment they try to create a session, and the error says so
 * plainly. Here there is nothing to interrupt, the panel is already reporting,
 * and the answer is worth having *before* inviting someone.
 *
 * Three states and not two. [UNKNOWN] is the truth before the first answer and
 * after a probe that never returned in time, and it is drawn as grey rather
 * than red: telling a player on a train that Emufii's server is down, when what
 * is down is their own connection, is a wrong fact printed in colour.
 */
enum class VpsState { UNKNOWN, ONLINE, OFFLINE }

object VpsStatus {

    private val _state = MutableStateFlow(VpsState.UNKNOWN)
    val state: StateFlow<VpsState> = _state.asStateFlow()

    /**
     * Polls for as long as the caller's scope lives, which is exactly as long
     * as a panel is lit.
     *
     * A poll and not a socket: `/health` is a static answer on a plain HTTP
     * server, and holding a connection open for a coloured dot would cost the
     * relay a socket per handheld in the world for no fact it does not already
     * serve in one round trip.
     *
     * Slow on purpose. A dot that flickers between two shades every second is
     * an alarm, and the thing it reports changes on the scale of minutes.
     */
    suspend fun poll(baseUrl: String = BuildConfig.COORDINATOR_BASE_URL) {
        while (true) {
            _state.value = probe(baseUrl)
            delay(if (_state.value == VpsState.ONLINE) ONLINE_PERIOD_MS else RETRY_PERIOD_MS)
        }
    }

    /**
     * One probe. Any answer at all counts as up, including one we cannot parse:
     * what is being asked is whether the machine is there, and a coordinator
     * answering 500 is a coordinator that is running and worth waiting for.
     */
    private suspend fun probe(baseUrl: String): VpsState = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("$baseUrl/health").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
            }
            try {
                conn.responseCode
            } finally {
                conn.disconnect()
            }
        }.fold(
            onSuccess = { if (it in 200..599) VpsState.ONLINE else VpsState.UNKNOWN },
            // A refused connection, a DNS failure, a timeout: from a handheld
            // these are indistinguishable from our own machine being down, and
            // the dot says "not reachable" rather than naming a culprit.
            onFailure = { VpsState.OFFLINE }
        )
    }

    private const val ONLINE_PERIOD_MS = 60_000L
    private const val RETRY_PERIOD_MS = 15_000L
}
