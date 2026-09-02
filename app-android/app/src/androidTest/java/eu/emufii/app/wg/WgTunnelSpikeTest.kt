package eu.emufii.app.wg

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import eu.emufii.app.MainActivity
import eu.emufii.app.network.CoordinatorClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Instrumented rather than a unit test: what is at risk is what Android adds around the
 * code, the foreground service right, the VPN slot, the tun device. The pure parts are
 * covered by `WgConfigTest`.
 *
 * Two halves, run separately so each can be a different device:
 *
 * ```
 * # on the host device
 * ./gradlew :app:connectedDebugAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=eu.emufii.app.wg.WgTunnelSpikeTest#hostBringsTunnelUp
 * # then on the guest, with the code and address the host printed
 * ... #guestReachesHost -Pandroid.testInstrumentationRunnerArguments.spikeCode=…
 * ```
 *
 * The host half holds its tunnel for `spikeHoldSeconds`: ending the test tears down the
 * process hosting the instrumentation, the service goes with it, and the relay's `latest
 * handshake` then ages instead of refreshing, which reads like a tunnel that survived.
 *
 * Both halves are opt-in on `-e spike true`: they create a real session on the hosted
 * coordinator and expect a human on the other side.
 *
 * This does not prove the tunnel survives Emufii going to the background: the harness
 * kills the process either way.
 */
class WgTunnelSpikeTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val args = InstrumentationRegistry.getArguments()
    private val client = CoordinatorClient()

    @Before
    fun onlyOnDemand() {
        assumeTrue(
            "manual spike: rerun with -e spike true",
            args.getString("spike") == "true"
        )
    }

    private companion object {
        /** Crockford base32, the shape the coordinator validates friend codes against. */
        const val HOST_ID = "E7K29QM4XR8T"
        const val GUEST_ID = "0123456789AB"
    }

    private fun bringAppToForeground() {
        // Android 12+ refuses `startForegroundService` from the background, and an
        // instrumentation run is not itself a foreground app.
        ctx.startActivity(
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        Thread.sleep(2_500)
    }

    private fun ping(target: String): Pair<Boolean, String> {
        val p = ProcessBuilder("/system/bin/ping", "-c", "3", "-W", "2", target)
            .redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        return (p.waitFor() == 0) to out
    }

    private fun startTunnelFor(code: String, profileId: String): WgTunnelInfo = runBlocking {
        // The profile id ties the route on the relay to a player, so their heartbeat keeps
        // it alive; claiming without one killed the first spike run's tunnel at two minutes.
        val info = client
            .claimAddress(code, EmufiiWgManager.publicKey(ctx), "spike", profileId)
            .getOrThrow()
        println("SPIKE config:\n" + WgConfig.renderRedacted(info))
        bringAppToForeground()
        EmufiiWgManager.start(ctx, code, info)

        val online = withTimeoutOrNull(45_000) {
            EmufiiWgManager.state.first { it is WgState.Online || it is WgState.Error }
        }
        assertNotNull("the tunnel never reached a terminal state", online)
        assertTrue("unexpected state: $online", online is WgState.Online)
        assertEquals(info.address, (online as WgState.Online).ip)
        info
    }

    @Test
    fun hostBringsTunnelUp() {
        val code = args.getString("spikeCode") ?: "SPIKE-01"
        runBlocking { client.deleteSession(code, null) }
        val created = runBlocking {
            // The coordinator reaps a session whose host stopped checking in, and
            // `hostIsPresent` is only ever true when host_id is set.
            client.createSession(code, null, "Spike", "Host", HOST_ID).getOrThrow()
        }
        runBlocking { client.heartbeat(code, HOST_ID, "Host") }
        println("SPIKE session=${created.code} subnet=${created.subnet}")

        val info = startTunnelFor(code, HOST_ID)
        println("SPIKE host_address=${info.address}")

        // The relay is the one address every session reaches: the ping proves the handshake
        // completed and that packets survive the round trip through carrier NAT.
        val (ok, out) = ping("10.67.0.1")
        println("SPIKE ping relais:\n$out")
        assertTrue("the relay 10.67.0.1 is unreachable inside the tunnel\n$out", ok)

        val hold = args.getString("spikeHoldSeconds")?.toIntOrNull() ?: 0
        repeat(hold / 5) {
            Thread.sleep(5_000)
            runBlocking { client.heartbeat(code, HOST_ID, "Host") }
        }
        if (hold > 0) {
            val (still, o) = ping("10.67.0.1")
            assertTrue("the host tunnel went down while waiting\n$o", still)
        }
    }

    @Test
    fun guestReachesHost() {
        val code = args.getString("spikeCode") ?: "SPIKE-01"
        val hostIp = requireNotNull(args.getString("spikeHostIp")) {
            "pass -e spikeHostIp <address advertised by the host>"
        }

        val info = startTunnelFor(code, GUEST_ID)
        println("SPIKE guest_address=${info.address}")

        val (relayOk, relayOut) = ping("10.67.0.1")
        assertTrue("the relay is unreachable\n$relayOut", relayOk)

        val (ok, out) = ping(hostIp)
        println("SPIKE ping host:\n$out")
        assertTrue("host $hostIp is unreachable inside the tunnel\n$out", ok)

        // Hold past the peer TTL: without the heartbeat the tunnel came up, worked, and
        // died exactly two minutes in.
        val hold = args.getString("spikeHoldSeconds")?.toIntOrNull() ?: 0
        repeat(hold / 5) {
            Thread.sleep(5_000)
            runBlocking { client.heartbeat(code, GUEST_ID, "Guest") }
        }
        if (hold > 0) {
            val (still, o) = ping(hostIp)
            assertTrue("the host became unreachable while waiting\n$o", still)
        }
    }
}
