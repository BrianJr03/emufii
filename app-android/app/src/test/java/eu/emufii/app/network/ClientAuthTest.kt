package eu.emufii.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * The vectors shared with `coordinator/client-auth.test.js`. The day the server and the app
 * stop computing the same thing, every client is refused at once and the symptom says
 * nothing; hardcoding both sides makes a format change break here first.
 *
 * `ClientAuth.sign` reads `BuildConfig`, null in a JVM test, so the computation is
 * reproduced rather than called.
 */
class ClientAuthTest {

    private val secret = "secret-de-test-partage"

    private fun sha256Hex(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") {
            "%02x".format(it)
        }

    private fun sign(method: String, path: String, body: String?, ts: Long): String {
        val payload = "${method.uppercase()}\n$path\n$ts\n${sha256Hex(body ?: "")}"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `the signature of a known vector does not move`() {
        val signature = sign("POST", "/sessions", """{"code":"ABC-123"}""", 1_770_000_000L)
        assertEquals(
            "e232919421418371a85dfc2fc5d7b894b5eeaffe8825c069039786347558db95",
            signature
        )
    }

    @Test
    fun `the body goes into the computation`() {
        // Without it a signature valid for one request is valid for every request at the
        // same path.
        val a = sign("POST", "/sessions", """{"code":"UN"}""", 1_770_000_000L)
        val b = sign("POST", "/sessions", """{"code":"DEUX"}""", 1_770_000_000L)
        assertNotEquals(a, b)
    }

    @Test
    fun `the path goes into the computation`() {
        val a = sign("POST", "/me", """{"id":"X"}""", 1_770_000_000L)
        val b = sign("POST", "/friends", """{"id":"X"}""", 1_770_000_000L)
        assertNotEquals(a, b)
    }

    @Test
    fun `the timestamp goes into the computation`() {
        val a = sign("POST", "/sessions", null, 1_770_000_000L)
        val b = sign("POST", "/sessions", null, 1_770_000_060L)
        assertNotEquals(a, b)
    }

    @Test
    fun `an absent body and an empty body sign the same`() {
        // A GET request has no body, and the app then sends null where the server
        // reads the empty string.
        assertEquals(
            sign("GET", "/sessions", null, 1_770_000_000L),
            sign("GET", "/sessions", "", 1_770_000_000L)
        )
    }
}
