package eu.emufii.app.azahar

/**
 * The names Azahar installs under. Never replace one with another: all three coexist
 * depending on where the install came from. The order is preference. What really
 * decides drivability is [NetplayUiSupport], not the name.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Azahar has not finished changing its id
 */
object AzaharPackage {
    const val MAIN = "org.azahar_emu.azahar"
    const val DEBUG = "org.azahar_emu.azahar.debug"

    /** The identifier inherited from Lime3DS, which some Azahar builds still carry. */
    const val LEGACY = "io.github.lime3ds.android"

    val candidates = listOf(MAIN, DEBUG, LEGACY)
}
