package eu.emufii.app.netplay

import eu.emufii.app.library.Backend

/**
 * Emufii writes no pseudo into Azahar's form: padding it to Azahar's minimum once
 * overwrote a valid pseudo the player had set there. The 3..20 bound is Azahar's own,
 * verbatim from its resources (2126.0-rc5).
 */
object NetplayNames {

    const val MIN_ROOM_NAME = 3
    const val MAX_ROOM_NAME = 20

    /**
     * Measured on Azahar, whose validator lives in the dex rather than the resources,
     * and reused for Eden, which descends from the same code.
     */
    const val MIN_USERNAME = 5

    const val MAX_USERNAME = 20

    fun usernameFor(backend: Backend, profileName: String?): String? =
        // Eden and Dolphin ship everyone the same default pseudo, and two players
        // carrying it cannot share a room. Azahar keeps its own.
        if (backend == Backend.EDEN || backend == Backend.DOLPHIN) username(profileName)
        else null

    fun username(profileName: String?): String? {
        val name = profileName?.trim().orEmpty()
        if (name.isEmpty()) return null
        // Dots rather than letters: "Jo..." reads as a shortened name, "Joxxx" as a
        // different one.
        return name.take(MAX_USERNAME).padEnd(MIN_USERNAME, '.')
    }

    fun roomName(sessionCode: String): String {
        val code = sessionCode.trim()
        val full = if (code.isEmpty()) "Emufii" else "Emufii $code"
        return full.take(MAX_ROOM_NAME).padEnd(MIN_ROOM_NAME, 'x')
    }
}
