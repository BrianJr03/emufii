package eu.emufii.app.session

import android.net.Uri
import eu.emufii.app.library.Backend
import eu.emufii.app.library.Console
import kotlin.random.Random

data class RomRef(
    val uri: Uri,
    val displayName: String,
    val console: Console,
    /**
     * What the session is compared against when joining by a typed code. Null
     * for a dump the library could not identify, and then no comparison is
     * possible, see the join flow, which lets those through.
     */
    val titleIdHex: String? = null,
    /** Compressed PSP dumps often carry DISC_ID only here. */
    val filename: String? = null,
    /** PSP DISC_ID is stored as `PSP-ULUS10277`; other consoles keep their own product code. */
    val productCode: String? = null,
    /** ARMSX2's per-game settings suffix, so launch performs no disc scan. */
    val ps2ElfCrc: String? = null,
)

data class Session(
    val code: String,
    val hostIp: String,
    val port: String,
    val role: Role,
    val rom: RomRef? = null,
    /**
     * The secret that proves the right to modify this session. The host's comes
     * back at creation, a guest's on joining, and a guest's only authorises
     * withdrawing themselves. The code is public and proves nothing. Null while
     * the tunnel is down or against an older coordinator, and the calls then go
     * out without the header.
     */
    val token: String? = null,
    /**
     * The Eden room the VPS holds, when it has one. Its presence decides who
     * hosts: with a room both players join it, without one the host carries it.
     */
    val room: eu.emufii.app.network.RoomRef? = null
) {
    enum class Role { HOST, GUEST }

    val console: Console? get() = rom?.console

    val backend: Backend get() = console?.backend ?: Backend.NONE

    /**
     * The one address the player sees. A single definition because there were
     * two: the session screen computed `room?.host ?: hostIp` while the panel
     * got raw `hostIp`, so an Eden session with a room showed an address the
     * emulator does not expect once the front screen stopped repeating it.
     * pourquoi : docs/decisions/session.md § What the rear panel carries, the front screen does not repeat
     */
    val shownAddress: String get() = when {
        room != null -> room.host
        // The PSP is the only case that is not an IP: its ad hoc server has a
        // fixed name, which PPSSPP resolves itself.
        backend == Backend.PPSSPP -> eu.emufii.app.psp.HOST_SENTINEL
        else -> hostIp
    }

    /** Null when the console does not ask: the PSP's ad hoc port is fixed. */
    val shownPort: String? get() = when {
        room != null -> room.port.toString()
        backend == Backend.PPSSPP -> null
        else -> port
    }
}

object SessionCodes {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val DIGITS = "23456789"

    fun generate(): String {
        val letters = (1..3).map { ALPHABET.random(Random.Default) }.joinToString("")
        val digits = (1..3).map { DIGITS.random(Random.Default) }.joinToString("")
        return "$letters-$digits"
    }

    /**
     * What the player typed, turned into the code the coordinator stores. The
     * hyphen is a reading aid nobody says out loud, so "HMM295" means "HMM-295".
     * Spaces, lowercase and a misplaced hyphen are forgiven the same way;
     * anything else is left alone, so a wrong code still fails as one.
     */
    fun normalize(typed: String): String {
        val body = typed.uppercase().filter { it.isLetterOrDigit() }
        if (body.length != 6) return typed.uppercase().trim()
        return "${body.take(3)}-${body.drop(3)}"
    }
}
