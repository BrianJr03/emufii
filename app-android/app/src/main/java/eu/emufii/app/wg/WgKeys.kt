package eu.emufii.app.wg

import android.content.Context
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair

/**
 * This device's WireGuard identity, generated once and kept: the
 * coordinator is idempotent on the public key, so the same key always gets the
 * same address. Not in the keystore: WireGuard needs the raw private key.
 * pourquoi : docs/decisions/tunnel-wireguard.md § The WireGuard identity must persist
 */
object WgKeys {

    private const val PREFS = "emufii_wg"
    private const val KEY_PRIVATE = "private_key"

    @Volatile
    private var cached: KeyPair? = null

    fun keyPair(ctx: Context): KeyPair {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val stored = prefs.getString(KEY_PRIVATE, null)
            val pair = stored?.let { existing ->
                // A corrupt value must not brick the tunnel: mint a new identity, at the
                // cost of one new address from the coordinator.
                runCatching { KeyPair(Key.fromBase64(existing)) }.getOrNull()
            } ?: KeyPair().also {
                prefs.edit().putString(KEY_PRIVATE, it.privateKey.toBase64()).apply()
            }
            cached = pair
            return pair
        }
    }

    fun publicKeyBase64(ctx: Context): String = keyPair(ctx).publicKey.toBase64()

    fun privateKeyBase64(ctx: Context): String = keyPair(ctx).privateKey.toBase64()

    /**
     * Drops the identity. Belongs with deleting the profile: the public key is a
     * stable identifier the coordinator sees.
     * pourquoi : docs/decisions/tunnel-wireguard.md § The WireGuard identity must persist
     */
    fun reset(ctx: Context) {
        synchronized(this) {
            cached = null
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY_PRIVATE).apply()
        }
    }
}
