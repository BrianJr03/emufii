package eu.emufii.app.library

enum class LibraryLayout { GRID, CAROUSEL, LIST }

enum class LibrarySort { NAME, RECENT, CONSOLE }

/**
 * Date order breaks ties by name: a library copied in one go carries the same
 * date throughout, and the order would otherwise follow the directory walk.
 * CONSOLE returns a folder's internal order; the grouping is the screen's.
 */
fun List<Rom>.sortedFor(sort: LibrarySort): List<Rom> = when (sort) {
    LibrarySort.NAME -> sortedBy { it.displayName.lowercase() }
    LibrarySort.RECENT -> sortedWith(
        compareByDescending<Rom> { it.addedAt }.thenBy { it.displayName.lowercase() }
    )
    LibrarySort.CONSOLE -> sortedBy { it.displayName.lowercase() }
}

fun List<Rom>.byConsole(): List<Pair<Console, List<Rom>>> =
    groupBy { it.console }
        .toList()
        .sortedBy { (console, _) -> console.ordinal }
        .map { (console, roms) -> console to roms.sortedBy { it.displayName.lowercase() } }
