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
    /** Original name, retained because compressed PSP dumps often carry DISC_ID only here. */
    val filename: String? = null,
    /** PSP DISC_ID is stored as `PSP-ULUS10277`; other consoles keep their own product code. */
    val productCode: String? = null,
    /** ARMSX2's per-game settings suffix, carried so launch performs no disc scan. */
    val ps2ElfCrc: String? = null,
)

data class Session(
    val code: String,
    val hostIp: String,
    val port: String,
    val role: Role,
    val rom: RomRef? = null,
    /**
     * The secret that proves the right to modify this session.
     *
     * The host's is returned at creation; a guest's is returned to them on
     * joining, and only authorises them to withdraw themselves. The code is
     * public: the finder publishes it, so it proves nothing.
     *
     * Null while the tunnel is not up, or against an older coordinator, in which
     * case the calls go out without the header, as before.
     */
    val token: String? = null,
    /**
     * The Eden room the VPS holds for this session, when it has one.
     *
     * Its presence changes who hosts: with a room, nobody hosts on their phone,
     * both players join it. Without one, the host carries the room as before. The
     * field is therefore not decorative, it decides the role Emufii plays in
     * Eden's form.
     */
    val room: eu.emufii.app.network.RoomRef? = null
) {
    enum class Role { HOST, GUEST }

    /** Null when joining a session for a game we don't own locally. */
    val console: Console? get() = rom?.console

    val backend: Backend get() = console?.backend ?: Backend.NONE

    /**
     * L'adresse que le joueur doit voir, et la seule.
     *
     * Une **definition unique**, parce qu'il y en avait deux : l'ecran de
     * session calculait `room?.host ?: hostIp` et le panneau arriere recevait
     * `hostIp` brut. Tant que les deux s'affichaient cote a cote, la divergence
     * se voyait a peine ; le jour ou l'ecran de face cesse de la redire quand le
     * panneau est allume, une session Eden avec salon aurait affiche au dos une
     * adresse que l'emulateur n'attend pas.
     * pourquoi : docs/decisions/session.md § Ce que le panneau arrière porte, l'écran de face ne le redit pas
     */
    val shownAddress: String get() = when {
        room != null -> room.host
        // La PSP est le seul cas ou ce n'est pas une IP : son serveur ad hoc a
        // un nom fixe, que PPSSPP resout lui-meme.
        backend == Backend.PPSSPP -> eu.emufii.app.psp.HOST_SENTINEL
        else -> hostIp
    }

    /**
     * Le port qui va avec [shownAddress], ou **null** quand la console n'en
     * demande pas : celui du serveur ad hoc de la PSP est fixe et PPSSPP ne le
     * demande pas. Un champ de plus a remplir est un champ de plus a remplir
     * de travers.
     */
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
     * What the player typed, turned into the code the coordinator stores.
     *
     * The hyphen is a reading aid, nobody says it out loud, and someone who
     * types "HMM295" means the session called "HMM-295". Without this, they got
     * "session introuvable" and went looking for a typo that wasn't there.
     * Spaces, lowercase and a hyphen in the wrong place are all forgiven the
     * same way; anything else is left alone, so a genuinely wrong code still
     * fails as a wrong code.
     */
    fun normalize(typed: String): String {
        val body = typed.uppercase().filter { it.isLetterOrDigit() }
        if (body.length != 6) return typed.uppercase().trim()
        return "${body.take(3)}-${body.drop(3)}"
    }
}
