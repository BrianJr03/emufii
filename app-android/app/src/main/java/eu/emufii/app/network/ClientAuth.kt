package eu.emufii.app.network

import eu.emufii.app.BuildConfig
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * What tells Emufii apart from `curl` in the coordinator's eyes.
 *
 * `HMAC-SHA256(secret, methode + "\n" + chemin + "\n" + horodatage + "\n" +
 * SHA-256(corps))`, en hexadecimal minuscule. Le corps entre dans le calcul ;
 * l'horodatage borne le rejeu. **Ce n'est pas une preuve d'identite** : la cle
 * est dans le binaire, donc extractible.
 * pourquoi : docs/decisions/coordinator-et-mise-a-jour.md § La signature du client change le coût, pas l'identité
 */
object ClientAuth {

    /** The header carrying the signature. */
    const val HEADER_AUTH = "X-Emufii-Auth"

    /** The timestamp the signature was computed over, in seconds. */
    const val HEADER_TIMESTAMP = "X-Emufii-Ts"

    /** The calling version, so the server can see what is calling it. */
    const val HEADER_CLIENT = "X-Emufii-Client"

    /**
     * Empty on a build that received no key, typically a dev build.
     *
     * Such a build sends no signature at all, and it is the local coordinator
     * that decides to accept it: development must not depend on a production
     * secret.
     */
    private val secret: String get() = BuildConfig.CLIENT_SECRET

    val isConfigured: Boolean get() = secret.isNotEmpty()

    /** The app's version, as it announces itself to the coordinator. */
    val clientVersion: String get() = BuildConfig.VERSION_CODE.toString()

    /**
     * Signs a request, or returns null when this build has no key.
     *
     * [timestampSeconds] is a parameter so the test can freeze the clock; the app
     * never passes it.
     */
    fun sign(
        method: String,
        path: String,
        body: String?,
        timestampSeconds: Long = System.currentTimeMillis() / 1000
    ): Signature? {
        if (!isConfigured) return null
        val payload = buildString {
            append(method.uppercase()).append('\n')
            append(path).append('\n')
            append(timestampSeconds).append('\n')
            append(sha256Hex(body ?: ""))
        }
        return Signature(hmacHex(secret, payload), timestampSeconds.toString())
    }

    data class Signature(val value: String, val timestamp: String)

    private fun hmacHex(key: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(message.toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .toHex()

    private fun ByteArray.toHex(): String {
        val out = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            out.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return out.toString()
    }

    private const val HEX = "0123456789abcdef"
}
