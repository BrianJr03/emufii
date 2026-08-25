package eu.emufii.app.ui.components

import eu.emufii.app.R
import eu.emufii.app.library.Console

/**
 * L'image de plaque d'une console, dans la variante du theme, ou null.
 *
 * Nullable a dessein : une console nouvellement ajoutee montre son nom plutot
 * que la machine d'une autre. Hissee hors de `LibraryScreen` le 2026-08-25,
 * quand la page « A propos » a voulu montrer les sept consoles servies : deux
 * copies de cette table auraient diverge au premier ajout.
 * pourquoi : docs/decisions/bibliotheque.md § Les dossiers de console
 */
internal fun consoleArtwork(console: Console, dark: Boolean): Int? = when (console) {
    Console.THREE_DS -> if (dark) R.drawable.console_three_ds_dark else R.drawable.console_three_ds_light
    Console.DS -> if (dark) R.drawable.console_ds_dark else R.drawable.console_ds_light
    Console.PSP -> if (dark) R.drawable.console_psp_dark else R.drawable.console_psp_light
    Console.SWITCH -> if (dark) R.drawable.console_switch_dark else R.drawable.console_switch_light
    Console.GAMECUBE -> if (dark) R.drawable.console_gamecube_dark else R.drawable.console_gamecube_light
    Console.WII -> if (dark) R.drawable.console_wii_dark else R.drawable.console_wii_light
    Console.PS2 -> if (dark) R.drawable.console_ps2_dark else R.drawable.console_ps2_light
    else -> null
}
