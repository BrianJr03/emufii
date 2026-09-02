package eu.emufii.app.secondscreen

import eu.emufii.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * [UNKNOWN] covers the state before the first answer and a probe that never returned;
 * it is drawn grey, not red: a player on a train would be told our server is down when
 * what is down is their connection.
 */
enum class VpsState { UNKNOWN, ONLINE, OFFLINE }

object VpsStatus {

    private val _state = MutableStateFlow(VpsState.UNKNOWN)
    val state: StateFlow<VpsState> = _state.asStateFlow()

    /**
     * A poll and not a socket: holding a connection open for a coloured dot would cost
     * the relay a socket per handheld, for a fact `/health` serves in one round trip.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watchers = 0
    private var job: Job? = null

    /**
     * One loop whatever the readers: the library bar and the rear panel show the same
     * lamp, often at once, and two loops would make the standing request twice.
     */
    suspend fun keepPolling() {
        synchronized(this) {
            if (watchers++ == 0) job = scope.launch { poll() }
        }
        try {
            awaitCancellation()
        } finally {
            synchronized(this) {
                if (--watchers == 0) {
                    job?.cancel()
                    job = null
                }
            }
        }
    }

    suspend fun poll(baseUrl: String = BuildConfig.COORDINATOR_BASE_URL) {
        while (true) {
            _state.value = probe(baseUrl)
            delay(if (_state.value == VpsState.ONLINE) ONLINE_PERIOD_MS else RETRY_PERIOD_MS)
        }
    }

    /**
     * Any answer counts as up, 500 included: a coordinator answering is a coordinator
     * that is running.
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
            // A refusal, a DNS failure and a timeout are indistinguishable from a
            // handheld: the dot says "not reachable" rather than naming a culprit.
            onFailure = { VpsState.OFFLINE }
        )
    }

    private const val ONLINE_PERIOD_MS = 60_000L
    private const val RETRY_PERIOD_MS = 15_000L
}
