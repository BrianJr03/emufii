package eu.emufii.app.azahar

/**
 * All three names coexist depending on where the install came from; the order is
 * preference. Drivability is decided by [NetplayUiSupport], not by the name.
 * pourquoi : docs/decisions/pilotes-emulateurs.md § Azahar has not finished changing its id
 */
object AzaharPackage {
    const val MAIN = "org.azahar_emu.azahar"
    const val DEBUG = "org.azahar_emu.azahar.debug"

    /** The identifier inherited from Lime3DS, which some Azahar builds still carry. */
    const val LEGACY = "io.github.lime3ds.android"

    val candidates = listOf(MAIN, DEBUG, LEGACY)
}
