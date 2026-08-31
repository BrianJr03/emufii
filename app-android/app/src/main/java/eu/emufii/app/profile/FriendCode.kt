package eu.emufii.app.profile

import java.security.SecureRandom

/**
 * The string you give someone so they can add you. The code is the identity: there is
 * no directory. Eleven Crockford base32 symbols plus a checksum, which tells a typo
 * from a friend who is offline.
 * pourquoi : docs/decisions/identite-et-dumps.md § The friend code is the identity, and it is public by design
 * pourquoi : docs/decisions/identite-et-dumps.md § The friend code alphabet, and its twelfth symbol
 */
object FriendCode {

    const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    /** Random symbols, before the trailing checksum. */
    const val RANDOM_SYMBOLS = 11

    /** Total length of a canonical (undashed) code. */
    const val LENGTH = RANDOM_SYMBOLS + 1

    private const val GROUP = 4

    private val secureRandom by lazy { SecureRandom() }

    fun generate(): String {
        val body = buildString {
            repeat(RANDOM_SYMBOLS) { append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]) }
        }
        return body + checksum(body)
    }

    /** `E7K29QM4XR8T` → `E7K2-9QM4-XR8T`, for display only. */
    fun format(code: String): String =
        code.chunked(GROUP).joinToString("-")

    /**
     * Canonical form of whatever the user typed, or null if it cannot be one of
     * our codes.
     *
     * Accepts lower case, stray dashes and spaces, and the four characters
     * Crockford maps back onto digits, someone reading a code off a screen
     * types the letter O for zero often enough that refusing it would be our
     * bug, not theirs.
     */
    fun normalize(input: String): String? {
        val cleaned = buildString {
            for (raw in input) {
                when (raw) {
                    '-', ' ', '\t', '\n' -> continue
                    else -> append(
                        when (raw.uppercaseChar()) {
                            'O' -> '0'
                            'I', 'L' -> '1'
                            else -> raw.uppercaseChar()
                        }
                    )
                }
            }
        }
        if (cleaned.length != LENGTH) return null
        if (cleaned.any { it !in ALPHABET }) return null
        if (cleaned.last() != checksum(cleaned.take(RANDOM_SYMBOLS))) return null
        return cleaned
    }

    fun isValid(input: String): Boolean = normalize(input) != null

    /**
     * Positional weights rather than a plain sum, so that swapping two adjacent
     * symbols, the other half of how people mistype a code, changes the
     * result. A plain sum would not notice.
     */
    private fun checksum(body: String): Char {
        var acc = 0
        for ((index, symbol) in body.withIndex()) {
            acc += ALPHABET.indexOf(symbol) * (index + 1)
        }
        return ALPHABET[acc % ALPHABET.length]
    }
}
