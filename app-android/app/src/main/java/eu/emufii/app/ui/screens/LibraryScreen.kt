package eu.emufii.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.key.KeyEvent
import eu.emufii.app.library.LibraryLayout
import eu.emufii.app.library.LibrarySort
import eu.emufii.app.library.byConsole
import eu.emufii.app.library.sortedFor
import eu.emufii.app.ui.components.consoleArtwork
import eu.emufii.app.ui.components.LayoutChip
import eu.emufii.app.ui.components.SearchChip
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import eu.emufii.app.ui.components.EmufiiKeyboard
import eu.emufii.app.ui.components.SearchField
import eu.emufii.app.ui.components.CompatBadge
import eu.emufii.app.library.compatKeys
import eu.emufii.app.compat.LocalCompatDb
import eu.emufii.app.library.RomTagReader
import eu.emufii.app.meta.LocalGameMetaDb
import eu.emufii.app.secondscreen.PanelFeed
import eu.emufii.app.secondscreen.SecondScreen
import eu.emufii.app.secondscreen.SecondScreenModel
import eu.emufii.app.ui.components.SortChip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import eu.emufii.app.R
import eu.emufii.app.library.Backend
import eu.emufii.app.library.Console
import eu.emufii.app.library.Rom
import eu.emufii.app.library.RomsRepository
import eu.emufii.app.library.shortLabel
import eu.emufii.app.profile.Profile
import eu.emufii.app.ui.RING_IN_MS
import eu.emufii.app.ui.focusRing
import eu.emufii.app.ui.CONFIRM_KEYS
import eu.emufii.app.ui.gamepadClick
import eu.emufii.app.ui.components.FriendsChip
import eu.emufii.app.ui.components.GameLaunchDialog
import eu.emufii.app.ui.components.IconPickerDialog
import eu.emufii.app.ui.components.HideRomDialog
import eu.emufii.app.ui.components.RenameRomDialog
import eu.emufii.app.ui.components.TileMenu
import eu.emufii.app.settings.SettingsStore
import eu.emufii.app.ui.components.UPDATE_BANNER_ROOM
import eu.emufii.app.ui.components.UpdateBanner
import eu.emufii.app.ui.components.WallpaperVeil
import eu.emufii.app.update.UpdateDismissals
import eu.emufii.app.update.UpdateCheck
import eu.emufii.app.update.LatestVersion
import eu.emufii.app.ui.components.ProfileChip
import eu.emufii.app.ui.components.SessionsChip
import eu.emufii.app.ui.components.tilePlate
import eu.emufii.app.ui.components.artworkRim
import eu.emufii.app.ui.theme.PillShape
import eu.emufii.app.ui.theme.ArtworkShape
import eu.emufii.app.ui.theme.moldedRim
import eu.emufii.app.ui.theme.socket
import eu.emufii.app.ui.theme.LocalEmufiiOledTheme
import eu.emufii.app.ui.theme.edgeColor
import eu.emufii.app.ui.components.ChevronLeft
import eu.emufii.app.ui.components.FolderMark
import eu.emufii.app.ui.theme.plate
import eu.emufii.app.ui.theme.TileShape
import eu.emufii.app.artwork.rememberTileArt
import eu.emufii.app.ui.theme.LocalEmufiiDarkTheme
import eu.emufii.app.ui.wallpaper.TrayBackdrop
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Portrait keeps three big tiles; landscape follows the width instead.
 * pourquoi : docs/decisions/bibliotheque.md § Des rangées entières, ou rien
 */
private const val GRID_COLS_PORTRAIT = 3
private const val TILE_MIN_WIDTH_DP = 104
private const val MIN_ROWS = 4
private const val EXTRA_ROWS_AFTER = 1

/**
 * The height a tile always reserves for its title: two lines of `labelMedium`.
 * Named, because the grid needs it before laying anything out.
 */
private val TILE_TITLE_ROOM = 32.dp

/**
 * The air between the floating header and the first thing under it.
 *
 * The grid takes it as a *floor* on its slack, the list adds it outright.
 * pourquoi : docs/decisions/bibliotheque.md § L'air sous l'en-tête est nommé, plus laissé au hasard
 */
private val HEADER_GAP = 14.dp

/** How deep the discs sit in their shelf. */
private val SHELF_INSET = 6.dp

/**
 * Whether a tile that is composing right now should play its arrival.
 *
 * Armed when the screen opens, disarmed afterwards; a rescan or entering a
 * folder arms it again.
 * pourquoi : docs/decisions/bibliotheque.md § L'arrivée des tuiles est armée, puis désarmée
 */
internal val LocalTileEntrance = staticCompositionLocalOf { false }

/** How long the library's arrival lasts before tiles simply appear. */
private const val ENTRANCE_WINDOW_MS = 900L

/** What an entry of the tile menu triggers. */
private enum class TileAction { ICON, RENAME, HIDE }

/**
 * What a library cell holds: a game or a folder. Shared by all three layouts.
 * pourquoi : docs/decisions/bibliotheque.md § Trois mises en page, un seul contrat de curseur
 */
private sealed interface Entry {
    /** Stable across recompositions: this is the lazy list's key. */
    val key: String

    data class Game(val rom: Rom) : Entry {
        override val key get() = rom.uri.toString()
    }

    data class Folder(val console: Console, val roms: List<Rom>) : Entry {
        override val key get() = "console:${console.name}"
    }
}

@Composable
fun LibraryScreen(
    profile: Profile,
    onOpenProfile: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenFinder: () -> Unit,
    onCreate: (Rom, private: Boolean) -> Unit,
    onJoinWith: (Rom) -> Unit,
    /**
     * Open a game straight into its console's public multiplayer, no session,
     * no tunnel. PSP only; see `PHASE1_SCOUT_PPSSPP_ONLINE.md`.
     */
    onPlayPublic: (Rom) -> Unit,
    onFolderPicked: (Uri) -> Unit,
    libraryRevision: Int
) {
    val context = LocalContext.current
    val dark = LocalEmufiiDarkTheme.current
    val repo = remember { RomsRepository(context) }
    val settings = remember { SettingsStore.get(context) }
    val artworkKey by settings.steamGridDbKey.collectAsState()
    val layout by settings.libraryLayout.collectAsState()
    val sort by settings.librarySort.collectAsState()
    val hiddenConsoles by settings.hiddenConsoles.collectAsState()

    // The bridge between the grid and the top bar. Compose's automatic traversal
    // does not cross it: the two live in sibling layers of one Box, with no
    // geometric relation it can follow. So the destination is named rather than
    // hoped for.
    val topBarLeftFocus = remember { FocusRequester() }
    val topBarFocus = remember { FocusRequester() }
    // Which end of the header answers a move upwards. See [HeaderSide].
    fun headerFocus(side: HeaderSide) =
        if (side == HeaderSide.LEFT) topBarLeftFocus else topBarFocus
    val gridFocus = remember { FocusRequester() }

    // Folder and rescan now live in the settings page, so this screen no longer
    // owns them: it reads whatever the repository holds and rebuilds when
    // [libraryRevision] says something upstream changed it.
    var folderUri by remember(libraryRevision) { mutableStateOf(repo.savedFolderUri()) }
    var roms by remember { mutableStateOf<List<Rom>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Rom?>(null) }

    // The launch dialog holds the cursor while it is open; on closing it has to
    // be handed back to the grid, as after the tile menu. Without this it went
    // back up into the top bar, and you came down by hand onto the very tile you
    // had just left.
    LaunchedEffect(selected) {
        if (selected == null) runCatching { gridFocus.requestFocus() }
    }

    // The game whose menu was opened by long press, then the one whose icon is
    // being chosen. Two states and not one: you move from menu to choice, and
    // conflating them would reopen the menu on closing the choice.
    var menuFor by remember { mutableStateOf<Rom?>(null) }
    var pickIconFor by remember { mutableStateOf<Rom?>(null) }
    var renameFor by remember { mutableStateOf<Rom?>(null) }
    var hideFor by remember { mutableStateOf<Rom?>(null) }

    // A rename is neither a folder change nor a rescan asked for elsewhere: it
    // needs its own trigger to rebuild the list.
    var reload by remember { mutableStateOf(0) }

    // A published version newer than this one, which the player has not yet
    // dismissed. Probed once per library opening: it is the one screen everybody
    // goes through, and announcing more often would say nothing more.
    val dismissals = remember { UpdateDismissals(context) }
    var update by remember { mutableStateOf<LatestVersion?>(null) }
    LaunchedEffect(Unit) {
        val latest = UpdateCheck.fetch()
        if (latest != null && UpdateCheck.isNewer(latest) && !dismissals.isDismissed(latest.versionCode)) {
            update = latest
            // Mirrored onto the rear panel: the banner below keeps saying it on
            // the front screen, and the panel is where it can still be read once
            // an emulator owns that screen.
            PanelFeed.post(
                context.getString(R.string.notify_update_title, latest.versionName),
                PanelFeed.Kind.UPDATE
            )
        }
    }

    // Only reachable from the empty state, there is nothing to browse yet, so
    // picking a folder is the one action that screen can offer. The repository
    // write itself is done by the caller, which is the single owner of it.
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> if (uri != null) onFolderPicked(uri) }

    LaunchedEffect(folderUri, libraryRevision, reload) {
        if (folderUri != null) {
            loading = true
            // Never forced: the explicit rescan already refreshed the shared
            // cache before bumping the revision, so this is a cheap read.
            roms = withContext(Dispatchers.IO) { repo.scan() }
            loading = false
        } else {
            roms = emptyList()
        }
    }

    /**
     * The open console folder, when filing by console is active. Reset as soon
     * as the filing mode changes.
     * pourquoi : docs/decisions/bibliotheque.md § Les consoles masquées le sont ici, pas dans le scan
     */
    var openConsole by remember { mutableStateOf<Console?>(null) }
    LaunchedEffect(sort) { if (sort != LibrarySort.CONSOLE) openConsole = null }

    // The search sits above the folders: its question ("where is that game")
    // crosses consoles, which is exactly what the folders cannot do. The field
    // keeps its text while closed-with-results elsewhere in the app, and back
    // closes it before anything else.
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    // Its own state, never a second reading of `searchOpen`: only the search's
    // own close empties the field.
    // pourquoi : docs/decisions/bibliotheque.md § La recherche prend l'étagère, et les deux états ne se croisent pas
    var keyboardOpen by rememberSaveable { mutableStateOf(false) }

    // Applied here, never in the scan: the repository's cache is shared with
    // the session flow, which must still find a hidden console's ROM.
    // pourquoi : docs/decisions/bibliotheque.md § Les consoles masquées le sont ici, pas dans le scan
    val shown = remember(roms, hiddenConsoles) {
        if (hiddenConsoles.isEmpty()) roms else roms.filter { it.console !in hiddenConsoles }
    }

    val entries = remember(shown, sort, openConsole, query) {
        val needle = query.trim()
        when {
            needle.isNotEmpty() ->
                shown.filter { it.displayName.contains(needle, ignoreCase = true) }
                    .sortedFor(LibrarySort.NAME)
                    .map(Entry::Game)
            sort != LibrarySort.CONSOLE -> shown.sortedFor(sort).map(Entry::Game)
            openConsole != null ->
                shown.filter { it.console == openConsole }
                    .sortedFor(LibrarySort.NAME)
                    .map(Entry::Game)
            else -> shown.byConsole().map { (console, list) -> Entry.Folder(console, list) }
        }
    }

    // A folder emptied by a rescan must not leave a blank screen with no way
    // out: we go up a level rather than wait for a gesture.
    LaunchedEffect(entries.isEmpty(), openConsole) {
        if (openConsole != null && entries.isEmpty()) openConsole = null
    }

    // The system back button closes the folder before leaving the screen. That
    // is what any file browser does, and without it entering a console was a one
    // way trip for anyone without a controller.
    BackHandler(enabled = openConsole != null) { openConsole = null }
    // Registered after the folder's, so back closes the search first. And the
    // panel before the search: back undoes one layer at a time, so a player who
    // wanted the grid uncovered does not lose their query for asking.
    BackHandler(enabled = searchOpen) {
        if (keyboardOpen) keyboardOpen = false else { searchOpen = false; query = "" }
    }
    LaunchedEffect(searchOpen) {
        if (searchOpen) runCatching { topBarLeftFocus.requestFocus() }
    }

    val onEntry: (Entry) -> Unit = { entry ->
        when (entry) {
            is Entry.Game -> selected = entry.rom
            is Entry.Folder -> openConsole = entry.console
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        val hazeState = rememberHazeState()

        // Source layer (backdrop for Haze): wallpaper + content
        Box(modifier = Modifier.fillMaxSize().hazeSource(hazeState)) {
            TrayBackdrop(modifier = Modifier.fillMaxSize(), dark = dark)

            when {
                folderUri == null -> EmptyState(
                    title = stringResource(R.string.lib_no_folder_title),
                    subtitle = stringResource(R.string.lib_no_folder_body),
                    cta = stringResource(R.string.lib_choose_folder),
                    onCta = { folderPicker.launch(null) },
                    topPadding = topInset + 72.dp,
                    bottomPadding = bottomInset + 24.dp
                )

                loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.lib_scanning),
                        style = MaterialTheme.typography.titleMedium,
                        // Straight on the wallpaper, so nothing supplies a
                        // content colour: without this it falls back to black
                        // and the scan looks like a blank screen in dark mode.
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                else -> {
                    val onMenuAction: (Rom, TileAction) -> Unit = { rom, action ->
                        menuFor = null
                        when (action) {
                            TileAction.ICON -> pickIconFor = rom
                            TileAction.RENAME -> renameFor = rom
                            TileAction.HIDE -> hideFor = rom
                        }
                    }
                    val contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp,
                        // The banner pushes the grid down rather than covering
                        // its first row. The air after the header is
                        // [HEADER_GAP], added by each layout in its own way.
                        // pourquoi : docs/decisions/bibliotheque.md § Les voiles, et pourquoi la carte de lancement est là où elle est
                        top = topInset + 72.dp +
                            (if (update != null) UPDATE_BANNER_ROOM else 0.dp),
                        // Travel, not empty space: without it the last row
                        // never rises fully into the usable area.
                        // pourquoi : docs/decisions/bibliotheque.md § Des rangées entières, ou rien
                        bottom = bottomInset + 88.dp
                    )

                    // Armed only while the shelf is really being filled.
                    // pourquoi : docs/decisions/bibliotheque.md § L'arrivée des tuiles est armée, puis désarmée
                    var arriving by remember(openConsole, reload) { mutableStateOf(true) }
                    LaunchedEffect(openConsole, reload) {
                        arriving = true
                        delay(ENTRANCE_WINDOW_MS)
                        arriving = false
                    }

                    // All three layouts get the same inputs and the same named
                    // cursor: what changes is the geometry, not the navigation
                    // mechanics.
                    CompositionLocalProvider(LocalTileEntrance provides arriving) {
                    when (layout) {
                        LibraryLayout.GRID -> RomsGrid(
                            entries = entries,
                            onSelect = onEntry,
                            onLongPress = { menuFor = it },
                            menuFor = menuFor,
                            onMenuAction = onMenuAction,
                            onDismissMenu = { menuFor = null },
                            onExitTop = { side -> runCatching { headerFocus(side).requestFocus() } },
                            onBack = { openConsole = null },
                            canGoBack = openConsole != null,
                            gridFocus = gridFocus,
                            contentPadding = contentPadding
                        )

                        LibraryLayout.CAROUSEL -> RomsCarousel(
                            entries = entries,
                            onSelect = onEntry,
                            onLongPress = { menuFor = it },
                            menuFor = menuFor,
                            onMenuAction = onMenuAction,
                            onDismissMenu = { menuFor = null },
                            onExitTop = { side -> runCatching { headerFocus(side).requestFocus() } },
                            onBack = { openConsole = null },
                            canGoBack = openConsole != null,
                            gridFocus = gridFocus,
                            contentPadding = contentPadding
                        )

                        LibraryLayout.LIST -> RomsList(
                            entries = entries,
                            onSelect = onEntry,
                            onLongPress = { menuFor = it },
                            menuFor = menuFor,
                            onMenuAction = onMenuAction,
                            onDismissMenu = { menuFor = null },
                            onExitTop = { side -> runCatching { headerFocus(side).requestFocus() } },
                            onBack = { openConsole = null },
                            canGoBack = openConsole != null,
                            gridFocus = gridFocus,
                            contentPadding = contentPadding
                        )
                    }
                    }
                }
            }

            // Inside the Haze source and after the grid. They trim the grid
            // rather than take room from it.
            // pourquoi : docs/decisions/bibliotheque.md § Les voiles, et pourquoi la carte de lancement est là où elle est
            WallpaperVeil(band = topInset + 60.dp, dark = dark)
            // The bottom scrim protected the dock, which is gone: all that
            // remains is a blur eating a band of covers for nothing. Cut back to
            // just enough that the last row does not touch the screen edge while
            // scrolling.
            WallpaperVeil(band = bottomInset + 14.dp, dark = dark, fromTop = false)
        }

        // Declared BEFORE the top bar on purpose, so the search field stays
        // above it: the field is the one thing outside that must not dismiss.
        // pourquoi : docs/decisions/bibliotheque.md § La recherche prend l'étagère, et les deux états ne se croisent pas
        if (searchOpen && keyboardOpen) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .focusProperties { canFocus = false }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { keyboardOpen = false }
                    )
            )
        }

        // OVERLAY : floating wordmark + profile (no glass rectangle wrapper)
        FloatingTopBar(
            profile = profile,
            layout = layout,
            onPickLayout = settings::setLibraryLayout,
            sort = sort,
            onPickSort = settings::setLibrarySort,
            openConsole = openConsole,
            openConsoleCount = entries.size,
            onLeaveFolder = { openConsole = null },
            searchOpen = searchOpen,
            query = query,
            onSearchOpen = { searchOpen = true; keyboardOpen = true },
            onQueryChange = { query = it },
            onSearchClose = { searchOpen = false; keyboardOpen = false; query = "" },
            // Tapping the field is how the panel comes back once it has been
            // put down. Without it, dismissing the keyboard was a one way trip
            // and the only way back was to close the search and retype.
            onFieldTap = { keyboardOpen = true },
            onOpenProfile = onOpenProfile,
            onOpenFriends = onOpenFriends,
            onOpenFinder = onOpenFinder,
            topBarLeftFocus = topBarLeftFocus,
            topBarFocus = topBarFocus,
            onLeaveDown = { runCatching { gridFocus.requestFocus() } },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        )

        // Never past half the grid, and it takes nothing with it when it goes
        // down. Rises from the edge on the same beat as the field's swap.
        // pourquoi : docs/decisions/bibliotheque.md § La recherche prend l'étagère, et les deux états ne se croisent pas
        androidx.compose.animation.AnimatedVisibility(
            visible = searchOpen && keyboardOpen,
            // Slide only, no fade, all the way past the edge: fading a
            // Haze-frosted layer drops the blur and shows the flat fill.
            // pourquoi : docs/decisions/bibliotheque.md § La recherche prend l'étagère, et les deux états ne se croisent pas
            enter = androidx.compose.animation.slideInVertically(
                // Stiff and barely sprung: the panel comes up like a drawer
                // pushed home, not like a ball dropped on a trampoline. The
                // soft spring read as hesitation, and opening is the one
                // gesture that must not hesitate.
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                ),
                initialOffsetY = { it }
            ),
            exit = androidx.compose.animation.slideOutVertically(
                // Leaving is quicker than arriving and does not bounce: a
                // panel the player has dismissed should be out of the way,
                // not asking for one more look.
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 180,
                    easing = androidx.compose.animation.core.FastOutLinearInEasing
                ),
                targetOffsetY = { it }
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            // Frosted, on the player's say-so: the keys float on a panel of
            // blurred tray, and the grid stays readable through it. The
            // keyboard itself keeps the half-screen ceiling; the panel's
            // padding is subtracted so the keys never exceed it.
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .hazeEffect(hazeState)
                    .background(Color(0xFF0A1220).copy(alpha = if (dark) 0.45f else 0.28f))
                    .border(1.dp, edgeColor(dark, LocalEmufiiOledTheme.current), RoundedCornerShape(28.dp))
                    // The panel swallows every tap that is not on a key,
                    // the gaps included: without this, a miss landed on the
                    // tile behind and opened a game mid-word.
                    .focusProperties { canFocus = false }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                EmufiiKeyboard(
                    onKey = { query += it },
                    onBackspace = { if (query.isNotEmpty()) query = query.dropLast(1) },
                    maxHeight = LocalConfiguration.current.screenHeightDp.dp / 2 - 32.dp,
                )
            }
        }

        // OVERLAY: "a new version exists".
        update?.let { latest ->
            UpdateBanner(
                latest = latest,
                onDismiss = { dismissals.dismiss(latest.versionCode); update = null },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 20.dp, end = 20.dp, top = 76.dp)
            )
        }

        // OVERLAY : the launch card. Sibling of the Haze source (so it can
        // blur the grid) and last (so a modal covers the chrome).
        // pourquoi : docs/decisions/bibliotheque.md § Les voiles, et pourquoi la carte de lancement est là où elle est
        renameFor?.let { rom ->
            RenameRomDialog(
                rom = rom,
                // The name is applied when the repository builds the list, so
                // the list has to be rebuilt, otherwise the game keeps its old
                // name until the next scan and the rename looks ignored.
                onRenamed = {
                    renameFor = null
                    reload++
                },
                onDismiss = { renameFor = null }
            )
        }

        // Same reload as a rename, and for the same reason: the list is built
        // by the repository, so a tile only leaves the grid once it is rebuilt.
        hideFor?.let { rom ->
            HideRomDialog(
                rom = rom,
                onHidden = {
                    hideFor = null
                    reload++
                },
                onDismiss = { hideFor = null }
            )
        }

        pickIconFor?.let { rom ->
            IconPickerDialog(
                rom = rom,
                apiKey = artworkKey,
                onDismiss = { pickIconFor = null }
            )
        }

        selected?.let { rom ->
            GameLaunchDialog(
                rom = rom,
                onDismiss = { selected = null },
                // Deliberately left up: nothing else publishes a screen until
                // the tunnel leg, so this spinner is what covers the wait.
                // pourquoi : docs/decisions/bibliotheque.md § Les voiles, et pourquoi la carte de lancement est là où elle est
                onPrimary = { private -> onCreate(rom, private) },
                // DS online play has no session to create and none to join: each
                // console dials the revival server on its own. Offering a code
                // field there asked a question with no meaning.
                onJoinWithCode =
                    if (rom.console.backend == Backend.MELONDS_WFC) null
                    else ({ selected = null; onJoinWith(rom) }),
                // The PSP's public ad hoc: a second kind of multiplayer, hence
                // its own button. (PS2's was set aside, see docs.)
                // pourquoi : docs/decisions/bibliotheque.md § Les voiles, et pourquoi la carte de lancement est là où elle est
                onPlayOnline =
                    if (rom.console.backend == Backend.PPSSPP) ({ onPlayPublic(rom) })
                    else null
            )
        }
    }
}

/**
 * What a layout's cursor must be able to do, whatever its geometry. All three
 * layouts keep their own index.
 * pourquoi : docs/decisions/bibliotheque.md § Trois mises en page, un seul contrat de curseur
 */
private class Cursor(val moveTo: (Int) -> Boolean)

/**
 * The gamepad bindings shared by all three layouts, so a fix in one cannot
 * leave the other two broken.
 * pourquoi : docs/decisions/bibliotheque.md § Trois mises en page, un seul contrat de curseur
 */
/** How long A is held before the tile's menu opens, matching touch's own delay. */
private const val HOLD_TO_MENU_MS = 480L

/**
 * The state of a held confirm button, on a controller. A press must do exactly
 * one thing: menu on the hold, or launch on the release, never both.
 * pourquoi : docs/decisions/bibliotheque.md § Le maintien de A, et le titre qui s'efface
 */
private class ConfirmHold(private val scope: CoroutineScope) {
    /**
     * Compose state, not a plain field: the tile reads it to sink while held,
     * so the grid has to recompose when it moves.
     */
    var down by mutableStateOf(false)
        private set
    private var fired = false
    private var job: Job? = null

    fun press(onHold: () -> Unit) {
        down = true
        fired = false
        job = scope.launch {
            delay(HOLD_TO_MENU_MS)
            fired = true
            onHold()
        }
    }

    /** True when the release should still count as a plain press. */
    fun release(): Boolean {
        down = false
        job?.cancel()
        job = null
        return !fired
    }
}

@Composable
private fun rememberConfirmHold(): ConfirmHold {
    val scope = rememberCoroutineScope()
    return remember(scope) { ConfirmHold(scope) }
}

private fun entryKeys(
    entries: List<Entry>,
    cursorIndex: () -> Int,
    onSelect: (Entry) -> Unit,
    onLongPress: (Rom) -> Unit,
    onBack: () -> Unit,
    canGoBack: Boolean,
    hold: ConfirmHold,
    directions: (Key) -> Boolean?
): (KeyEvent) -> Boolean = keys@{ event ->
    // Confirm is the one key read on the way up as well as down: that is what
    // separates a press from a hold, and everything else is decided on KeyDown.
    if (event.key in CONFIRM_KEYS) {
        val entry = entries.getOrNull(cursorIndex())
        return@keys when (event.type) {
            KeyEventType.KeyDown -> {
                // Auto-repeat sends KeyDown again while the button is held; only
                // the first one starts the timer, or the menu would be armed
                // over and over and fire on the last repeat instead of on time.
                if (!hold.down) {
                    hold.press { (entry as? Entry.Game)?.let { onLongPress(it.rom) } }
                }
                true
            }
            KeyEventType.KeyUp -> {
                // A hold that already opened the menu must not also launch the
                // game on release: that is exactly the double action a long
                // press exists to avoid.
                if (hold.release() && entry != null) onSelect(entry)
                true
            }
            else -> false
        }
    }
    if (event.type != KeyEventType.KeyDown) return@keys false
    directions(event.key)?.let { return@keys it }
    when (event.key) {
        // Y stays: it opens the menu outright, with no wait, and a player who
        // learned it keeps it. The hold is what someone coming from touch tries
        // first, which is why both exist.
        Key.ButtonY ->
            (entries.getOrNull(cursorIndex()) as? Entry.Game)
                ?.let { onLongPress(it.rom); true } ?: false
        // B goes up a folder, as on every console. Returning `false` when there
        // is nowhere to go lets the system close the screen.
        Key.ButtonB, Key.Back -> if (canGoBack) { onBack(); true } else false
        // La commande du panneau, ecoutee **ici aussi**. Le panneau l'ecoute de
        // son cote depuis qu'il est tactile, mais le focus clavier va a une
        // fenetre : tant que c'est l'ecran de face qui l'a — le cas ordinaire,
        // et le seul sur une machine a un ecran — c'est cette ecoute-ci qui
        // repond. Sur une machine sans panneau la pression ne fait rien du tout,
        // ce qui reste la regle : l'ecran de face ne gagne ni ne perd rien parce
        // qu'un panneau existe.
        // pourquoi : docs/decisions/second-ecran.md § R tourne la page depuis les deux écrans
        Key.ButtonR1 -> { SecondScreen.flipPage(); true }
        else -> false
    }
}

@Composable
private fun RomsGrid(
    entries: List<Entry>,
    onSelect: (Entry) -> Unit,
    onLongPress: (Rom) -> Unit,
    menuFor: Rom?,
    onMenuAction: (Rom, TileAction) -> Unit,
    onDismissMenu: () -> Unit,
    /** What happens when the player moves up past the first row. */
    onExitTop: (HeaderSide) -> Unit,
    /** Going back up out of the open console folder. */
    onBack: () -> Unit,
    canGoBack: Boolean,
    /** Held by the screen, so the top bar can hand control back. */
    gridFocus: FocusRequester,
    contentPadding: PaddingValues
) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    // Whole rows, or none: leftover height goes to the *top* padding, never
    // the bottom, which is travel and only exists once scrolled.
    // pourquoi : docs/decisions/bibliotheque.md § Des rangées entières, ou rien
    val gutter = 18.dp
    val rowGap = 24.dp
    val topPad = contentPadding.calculateTopPadding()
    // Where the tray really ends: the bottom veil paints the backdrop back over
    // this band, so anything laid out under it is invisible even though Compose
    // considers it on screen. Measuring against `maxHeight` alone is what put a
    // row's titles under that veil.
    val bottomLimit = WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding() + 14.dp
    val available = maxHeight - topPad - bottomLimit

    // Tile size comes from the height too, not width alone. Never more than
    // three extra columns: past that the covers stop being recognisable.
    // pourquoi : docs/decisions/bibliotheque.md § Des rangées entières, ou rien
    fun cellFor(c: Int) = (maxWidth - 40.dp - gutter * (c - 1)) / c
    fun rowFor(c: Int) = cellFor(c) + 8.dp + TILE_TITLE_ROOM
    val wantRows = if (landscape) 2 else 3
    // What the width alone would have given: as many tiles of a sane size as it
    // holds, gutters kept.
    val widthCols = if (landscape) {
        max(GRID_COLS_PORTRAIT, (configuration.screenWidthDp - 40) / (TILE_MIN_WIDTH_DP + 18))
    } else {
        GRID_COLS_PORTRAIT
    }
    var cols = widthCols
    while (
        cols < widthCols + 3 &&
        rowFor(cols) * wantRows + rowGap * (wantRows - 1) > available
    ) cols++

    val rowHeight = rowFor(cols)
    val wholeRows = ((available + rowGap) / (rowHeight + rowGap)).toInt().coerceAtLeast(1)
    // A floor of [HEADER_GAP], never an addition to it: adding both pushed the
    // tray down 14 dp for nothing.
    // pourquoi : docs/decisions/bibliotheque.md § L'air sous l'en-tête est nommé, plus laissé au hasard
    val slack = (available - (rowHeight * wholeRows + rowGap * (wholeRows - 1)))
        .coerceIn(0.dp, 20.dp)
        .coerceAtLeast(HEADER_GAP)


    // The grid's ONE column count: the cursor moves by ±columns and reads this
    // and nothing else. Two counts is the whole bug family this screen ended.
    // pourquoi : docs/decisions/bibliotheque.md § Des rangées entières, ou rien
    val columns = cols

    val rowsFromEntries = if (entries.isEmpty()) 0 else (entries.size + columns - 1) / columns
    val totalRows = max(if (landscape) 2 else MIN_ROWS, rowsFromEntries + EXTRA_ROWS_AFTER)
    val totalSlots = totalRows * columns

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val MARGIN_PX = with(LocalDensity.current) { 14.dp.roundToPx() }

    /**
     * Where the player is in the grid: an index we compute ourselves, which
     * cannot get lost because it depends on no live component.
     * pourquoi : docs/decisions/bibliotheque.md § Le curseur est un index calculé, jamais un focus deviné
     */
    var cursor by rememberSaveable { mutableStateOf(0) }
    var padFocused by remember { mutableStateOf(false) }
    PublishHovered(entries, cursor)

    // A rescan, or entering a folder, can shorten the list under the cursor.
    LaunchedEffect(entries.size) {
        if (cursor > entries.lastIndex) cursor = entries.lastIndex.coerceAtLeast(0)
    }

    // The grid takes focus on opening: on a handheld the player already has
    // their thumbs on the sticks, and a screen with nothing selected answers
    // directions by doing nothing.
    LaunchedEffect(Unit) { runCatching { gridFocus.requestFocus() } }

    // The tile menu opens a window that takes focus; on closing, without this,
    // nobody holds it any more and directions do nothing, which is what forced
    // you to touch the screen to regain control.
    LaunchedEffect(menuFor) {
        if (menuFor == null) runCatching { gridFocus.requestFocus() }
    }

    /**
     * Brings the targeted tile fully into the usable area — Compose stops at
     * the first visible pixel, which is not the same thing.
     * pourquoi : docs/decisions/bibliotheque.md § Amener la cible, pas seulement la rendre « visible »
     */
    fun reveal(index: Int) {
        scope.launch {
            val info = gridState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
            if (item == null) {
                // Off screen: nothing to refine, bring it in outright. The
                // offset lifts the row under the band rather than pinning it to
                // the edge.
                gridState.animateScrollToItem(index, -info.beforeContentPadding)
                return@launch
            }
            val top = item.offset.y
            val bottom = top + item.size.height
            val safeTop = info.beforeContentPadding
            val safeBottom = info.viewportEndOffset - info.viewportStartOffset - info.afterContentPadding
            // Margin on top of strict visibility: the targeted tile is scaled
            // up 7 % and carries a glow, so it spills past its own layout
            // bounds. Stopping at the exact pixel left it clipped by the edge.
            val margin = MARGIN_PX
            val delta = when {
                top < safeTop + margin -> top - safeTop - margin
                bottom > safeBottom - margin -> bottom - safeBottom + margin
                else -> 0
            }
            if (delta != 0) gridState.animateScrollBy(delta.toFloat())
        }
    }

    fun moveTo(index: Int): Boolean {
        if (index !in entries.indices) return false
        cursor = index
        reveal(index)
        return true
    }

    val hold = rememberConfirmHold()
    val onKey = entryKeys(
        entries = entries,
        cursorIndex = { cursor },
        onSelect = onSelect,
        onLongPress = onLongPress,
        onBack = onBack,
        canGoBack = canGoBack,
        hold = hold
    ) { key ->
        when (key) {
            Key.DirectionLeft -> moveTo(cursor - 1)
            Key.DirectionRight -> moveTo(cursor + 1)
            Key.DirectionDown -> moveTo(cursor + columns)
            Key.DirectionUp ->
                if (cursor < columns) {
                    // Named destination, and named per column: sibling layers
                    // in one Box have no automatic path between them.
                    // pourquoi : docs/decisions/bibliotheque.md § Sortir par le haut se nomme, et selon la colonne
                    onExitTop(
                        if (cursor % columns < columns / 2) HeaderSide.LEFT
                        else HeaderSide.RIGHT
                    )
                    true
                } else {
                    moveTo(cursor - columns)
                }
            else -> null
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(cols),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
            top = topPad + slack,
            bottom = contentPadding.calculateBottomPadding()
        ),
        horizontalArrangement = Arrangement.spacedBy(gutter),
        verticalArrangement = Arrangement.spacedBy(rowGap),
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(gridFocus)
            .onFocusChanged { padFocused = it.hasFocus }
            .focusable()
            .onPreviewKeyEvent(onKey)
    ) {
        items(count = totalSlots, key = { it }) { i ->
            val entry = entries.getOrNull(i)
            when (entry) {
                null -> EmptySlot()
                is Entry.Folder -> FolderTile(
                    folder = entry,
                    onClick = { onSelect(entry) },
                    selected = padFocused && i == cursor,
                    padHeld = hold.down && i == cursor
                )
                is Entry.Game -> RomTile(
                    rom = entry.rom,
                    onClick = { onSelect(entry) },
                    onLongClick = { onLongPress(entry.rom) },
                    // The cursor is ours: a tile no longer asks whether *it*
                    // has focus, it is told. So a tile destroyed on leaving the
                    // screen can no longer take the selection with it.
                    selected = padFocused && i == cursor,
                    padHeld = hold.down && i == cursor,
                    menuOpen = menuFor?.uri == entry.rom.uri,
                    onChangeIcon = { onMenuAction(entry.rom, TileAction.ICON) },
                    onRename = { onMenuAction(entry.rom, TileAction.RENAME) },
                    onHide = { onMenuAction(entry.rom, TileAction.HIDE) },
                    onDismissMenu = onDismissMenu
                )
            }
        }
    }
    }
}

/** A carousel card's width, as a fraction of the available width. */
private const val CAROUSEL_CARD_FRACTION = 0.42f

/** What the title claims under the card: two lines, plus the gap. */
private val CAROUSEL_TITLE_ROOM = 48.dp

/**
 * One game at a time, large. A single row, so up and down do not navigate: up
 * exits to the bar, and that is all.
 * pourquoi : docs/decisions/bibliotheque.md § Le carrousel doit suivre le doigt sans se retourner contre la manette
 */
@Composable
private fun RomsCarousel(
    entries: List<Entry>,
    onSelect: (Entry) -> Unit,
    onLongPress: (Rom) -> Unit,
    menuFor: Rom?,
    onMenuAction: (Rom, TileAction) -> Unit,
    onDismissMenu: () -> Unit,
    onExitTop: (HeaderSide) -> Unit,
    onBack: () -> Unit,
    canGoBack: Boolean,
    gridFocus: FocusRequester,
    contentPadding: PaddingValues
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var cursor by rememberSaveable { mutableStateOf(0) }
    var padFocused by remember { mutableStateOf(false) }
    PublishHovered(entries, cursor)

    LaunchedEffect(entries.size) {
        if (cursor > entries.lastIndex) cursor = entries.lastIndex.coerceAtLeast(0)
    }
    LaunchedEffect(Unit) { runCatching { gridFocus.requestFocus() } }
    LaunchedEffect(menuFor) {
        if (menuFor == null) runCatching { gridFocus.requestFocus() }
    }

    /**
     * The targeted card comes to the centre, not "somewhere on screen".
     * pourquoi : docs/decisions/bibliotheque.md § Le carrousel doit suivre le doigt sans se retourner contre la manette
     */
    // True while [reveal] is animating the row itself: centre-following must be
    // off while *we* scroll, or a double press computes from a passing card.
    var settling by remember { mutableStateOf(false) }

    fun reveal(index: Int) {
        scope.launch {
            val info = listState.layoutInfo
            // Leading padding is already in `animateScrollToItem`'s frame;
            // passing it again applied it twice.
            // pourquoi : docs/decisions/bibliotheque.md § Le carrousel doit suivre le doigt sans se retourner contre la manette
            val viewport = info.viewportEndOffset - info.viewportStartOffset
            val itemWidth = info.visibleItemsInfo.firstOrNull()?.size ?: 0
            val offset = ((viewport - itemWidth) / 2 - info.beforeContentPadding)
            settling = true
            try {
                listState.animateScrollToItem(index, -offset)
            } finally {
                settling = false
            }
        }
    }

    /**
     * The card nearest the middle of the viewport, whatever put it there. This
     * is what makes the carousel work under a finger.
     * pourquoi : docs/decisions/bibliotheque.md § Le carrousel doit suivre le doigt sans se retourner contre la manette
     */
    val centred by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2 - middle) }
                ?.index
        }
    }

    // While the finger has it, the cursor is whatever is in the middle: the card
    // grows as it arrives there instead of after the fact.
    LaunchedEffect(centred, settling) {
        val index = centred
        if (!settling && index != null && index in entries.indices) cursor = index
    }

    /**
     * Whether the row has been touched since it last came to rest. A drag
     * interaction is the only honest signal that a *person* moved the row.
     * pourquoi : docs/decisions/bibliotheque.md § Le carrousel doit suivre le doigt sans se retourner contre la manette
     */
    var dragged by remember { mutableStateOf(false) }
    LaunchedEffect(listState.interactionSource) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) dragged = true
        }
    }

    // And when the finger lets go, the row stops *on* a card. Left where a fling
    // ends it rests between two, which is the one thing a carousel must not do.
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && dragged) {
            dragged = false
            centred?.let { if (it in entries.indices) reveal(it) }
        }
    }

    fun moveTo(index: Int): Boolean {
        if (index !in entries.indices) return false
        cursor = index
        reveal(index)
        return true
    }

    val hold = rememberConfirmHold()
    val onKey = entryKeys(
        entries = entries,
        cursorIndex = { cursor },
        onSelect = onSelect,
        onLongPress = onLongPress,
        onBack = onBack,
        canGoBack = canGoBack,
        hold = hold
    ) { key ->
        when (key) {
            Key.DirectionLeft -> moveTo(cursor - 1)
            Key.DirectionRight -> moveTo(cursor + 1)
            Key.DirectionUp -> { onExitTop(HeaderSide.RIGHT); true }
            // Down leads nowhere on a single row, but it is captured anyway:
            // letting it through would hand control back to Compose's traversal,
            // which would go looking for a focusable elsewhere on screen.
            Key.DirectionDown -> true
            else -> null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(gridFocus)
            .onFocusChanged { padFocused = it.hasFocus }
            .focusable()
            .onPreviewKeyEvent(onKey),
        contentAlignment = Alignment.Center
    ) {
        /**
         * Sized on the height actually free, never the screen's — the Thor's
         * landscape punishes anything measured against the screen.
         * pourquoi : docs/decisions/bibliotheque.md § Trois mesures du carrousel, toutes corrigées sur capture
         */
        val free = maxHeight -
            contentPadding.calculateTopPadding() -
            contentPadding.calculateBottomPadding() -
            CAROUSEL_TITLE_ROOM
        val cardSize = minOf(maxWidth * CAROUSEL_CARD_FRACTION, free)
            .coerceIn(120.dp, 320.dp)

        /**
         * Half of what remains around a card, which is what lets the first and
         * last reach the centre.
         * pourquoi : docs/decisions/bibliotheque.md § Trois mesures du carrousel, toutes corrigées sur capture
         */
        val sidePad = ((maxWidth - cardSize) / 2).coerceAtLeast(16.dp)

        // The active card reaches the centre on opening, without waiting for a
        // first direction.
        LaunchedEffect(cardSize) { reveal(cursor) }

        LazyRow(
            state = listState,
            // The vertical padding comes from the screen (band, banner), but the
            // side margins are the carousel's: the first and last card must be
            // able to reach the centre.
            contentPadding = PaddingValues(
                start = sidePad,
                end = sidePad,
                // The card is centred, not the column: the title's room is
                // *moved* bottom to top, and only half of it.
                // pourquoi : docs/decisions/bibliotheque.md § Trois mesures du carrousel, toutes corrigées sur capture
                top = contentPadding.calculateTopPadding() + CAROUSEL_TITLE_ROOM / 2,
                bottom = (contentPadding.calculateBottomPadding() - CAROUSEL_TITLE_ROOM / 2)
                    .coerceAtLeast(16.dp)
            ),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(count = entries.size, key = { entries[it].key }) { i ->
                val entry = entries[i]
                val active = i == cursor
                // The neighbours step back. Without that gap a row of
                // equally-sized cards reads as a one-line grid, and nothing
                // points at the one about to be launched.
                val recede by animateFloatAsState(
                    targetValue = if (active) 1f else 0.86f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "carousel-recede"
                )
                Box(
                    modifier = Modifier
                        .width(cardSize)
                        .scale(recede)
                        .alpha(if (active) 1f else 0.62f)
                ) {
                    // A side card comes to the middle; only the middle opens.
                    // pourquoi : docs/decisions/bibliotheque.md § Le carrousel doit suivre le doigt sans se retourner contre la manette
                    val onTap = { if (active) onSelect(entry) else moveTo(i); Unit }
                    when (entry) {
                        is Entry.Folder -> FolderTile(
                            folder = entry,
                            onClick = onTap,
                            selected = padFocused && active,
                            padHeld = hold.down && active
                        )
                        is Entry.Game -> RomTile(
                            rom = entry.rom,
                            onClick = onTap,
                            onLongClick = { onLongPress(entry.rom) },
                            selected = padFocused && active,
                            padHeld = hold.down && active,
                            menuOpen = menuFor?.uri == entry.rom.uri,
                            onChangeIcon = { onMenuAction(entry.rom, TileAction.ICON) },
                            onRename = { onMenuAction(entry.rom, TileAction.RENAME) },
                            onHide = { onMenuAction(entry.rom, TileAction.HIDE) },
                            onDismissMenu = onDismissMenu
                        )
                    }
                }
            }
        }
    }
}

/**
 * Titles spelled out: the layout for telling two dumps of one game apart.
 * pourquoi : docs/decisions/bibliotheque.md § La liste existe pour distinguer deux dumps du même jeu
 */
@Composable
private fun RomsList(
    entries: List<Entry>,
    onSelect: (Entry) -> Unit,
    onLongPress: (Rom) -> Unit,
    menuFor: Rom?,
    onMenuAction: (Rom, TileAction) -> Unit,
    onDismissMenu: () -> Unit,
    onExitTop: (HeaderSide) -> Unit,
    onBack: () -> Unit,
    canGoBack: Boolean,
    gridFocus: FocusRequester,
    contentPadding: PaddingValues
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val MARGIN_PX = with(density) { 14.dp.roundToPx() }
    // The strip the bottom veil paints back over the list. Layout does not know
    // about it; the eye does.
    val VEIL_PX = with(density) {
        (WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 14.dp).roundToPx()
    }
    var cursor by rememberSaveable { mutableStateOf(0) }
    var padFocused by remember { mutableStateOf(false) }
    PublishHovered(entries, cursor)

    LaunchedEffect(entries.size) {
        if (cursor > entries.lastIndex) cursor = entries.lastIndex.coerceAtLeast(0)
    }
    LaunchedEffect(Unit) { runCatching { gridFocus.requestFocus() } }
    LaunchedEffect(menuFor) {
        if (menuFor == null) runCatching { gridFocus.requestFocus() }
    }

    /**
     * Brings the selected row fully into the usable band, with room to spare:
     * a margin for the glow, plus the band the bottom veil repaints.
     * pourquoi : docs/decisions/bibliotheque.md § Amener la cible, pas seulement la rendre « visible »
     */
    fun reveal(index: Int) {
        scope.launch {
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
            if (item == null) {
                listState.animateScrollToItem(index, -info.beforeContentPadding)
                return@launch
            }
            val top = item.offset
            val bottom = top + item.size
            val safeTop = info.beforeContentPadding + MARGIN_PX
            val safeBottom = info.viewportEndOffset - info.viewportStartOffset -
                info.afterContentPadding - MARGIN_PX - VEIL_PX

            // Aim at the centre of the band, not merely inside it: one row per
            // press, with as much list ahead as behind. Both ends clamp.
            // pourquoi : docs/decisions/bibliotheque.md § Amener la cible, pas seulement la rendre « visible »
            val centre = (safeTop + safeBottom) / 2
            val delta = (top + bottom) / 2 - centre
            if (delta != 0) listState.animateScrollBy(delta.toFloat())
        }
    }

    fun moveTo(index: Int): Boolean {
        if (index !in entries.indices) return false
        cursor = index
        reveal(index)
        return true
    }

    val hold = rememberConfirmHold()
    val onKey = entryKeys(
        entries = entries,
        cursorIndex = { cursor },
        onSelect = onSelect,
        onLongPress = onLongPress,
        onBack = onBack,
        canGoBack = canGoBack,
        hold = hold
    ) { key ->
        when (key) {
            Key.DirectionDown -> moveTo(cursor + 1)
            Key.DirectionUp ->
                if (cursor == 0) {
                    onExitTop(HeaderSide.RIGHT)
                    true
                } else {
                    moveTo(cursor - 1)
                }
            // A list has no columns: left and right are captured so Compose does
            // not go looking for a focusable off screen.
            Key.DirectionLeft, Key.DirectionRight -> true
            else -> null
        }
    }

    LazyColumn(
        state = listState,
        // Added outright: a list has no slack to pour, so nothing else was ever
        // going to hold its first plate off the header's pills.
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
            top = contentPadding.calculateTopPadding() + HEADER_GAP,
            bottom = contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(gridFocus)
            .onFocusChanged { padFocused = it.hasFocus }
            .focusable()
            .onPreviewKeyEvent(onKey)
    ) {
        items(count = entries.size, key = { entries[it].key }) { i ->
            EntryRow(
                entry = entries[i],
                selected = padFocused && i == cursor,
                onClick = { onSelect(entries[i]) },
                onLongClick = { (entries[i] as? Entry.Game)?.let { onLongPress(it.rom) } },
                menuFor = menuFor,
                onMenuAction = onMenuAction,
                onDismissMenu = onDismissMenu
            )
        }
    }
}

/** One row of the list: thumbnail, name, console. */
@Composable
private fun EntryRow(
    entry: Entry,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    menuFor: Rom?,
    onMenuAction: (Rom, TileAction) -> Unit,
    onDismissMenu: () -> Unit
) {
    val context = LocalContext.current
    val dark = LocalEmufiiDarkTheme.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            // The ring FIRST, before anything that clips and before an opaque
            // fill: a glow is a shadow, and it draws through a see-through row.
            // pourquoi : docs/decisions/bibliotheque.md § La liste existe pour distinguer deux dumps du même jeu
            .focusRing(selected, shape)
            .plate(
                shape = shape,
                dark = dark,
                oled = LocalEmufiiOledTheme.current,
                lift = if (selected) 7.dp else 3.dp
            )
            // The selected row brightens instead of growing: scaling a
            // full-width row pushes its neighbours and makes the whole list jump
            // on every press. Laid over the opaque face, so it tints the plate
            // rather than letting anything through it.
            .then(
                if (selected) Modifier.background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), shape
                ) else Modifier
            )
            .focusProperties { canFocus = false }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .gamepadClick(interaction, onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(ArtworkShape)
                .background(tilePlate())
        ) {
            when (entry) {
                is Entry.Folder -> {
                    // The same artwork as the tiles, at thumbnail size: a list
                    // that named its consoles in text while the grid showed
                    // their logos read as two different libraries.
                    val plate = consoleArtwork(entry.console, LocalEmufiiDarkTheme.current)
                    if (plate != null) {
                        Image(
                            painter = painterResource(plate),
                            contentDescription = entry.console.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(consolePlate(entry.console)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                entry.console.shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                is Entry.Game -> {
                    val art by rememberTileArt(entry.rom)
                    if (art.model != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(art.model).build(),
                            contentDescription = entry.rom.displayName,
                            contentScale = if (art.isPixelArt) ContentScale.Fit else ContentScale.Crop,
                            filterQuality =
                                if (art.isPixelArt) FilterQuality.None else FilterQuality.High,
                            modifier = Modifier.fillMaxSize(),
                            placeholder = ColorPainter(Color.Transparent),
                            error = ColorPainter(Color.Transparent)
                        )
                    } else {
                        PlaceholderArtwork(entry.rom.displayName)
                    }

                    // Anchored on the thumbnail, as it is on the tile in the
                    // grid: a Popup takes the bounds of whatever contains it.
                    TileMenu(
                        expanded = menuFor?.uri == entry.rom.uri,
                        title = entry.rom.displayName,
                        changeIconLabel = stringResource(R.string.tile_menu_icon),
                        renameLabel = stringResource(R.string.tile_menu_rename),
                        hideLabel = stringResource(R.string.tile_menu_hide),
                        accent = entry.rom.accentArgb?.let { Color(it) },
                        onChangeIcon = { onMenuAction(entry.rom, TileAction.ICON) },
                        onRename = { onMenuAction(entry.rom, TileAction.RENAME) },
                        onHide = { onMenuAction(entry.rom, TileAction.HIDE) },
                        onDismiss = onDismissMenu
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                when (entry) {
                    is Entry.Folder -> entry.console.label
                    is Entry.Game -> entry.rom.displayName
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (entry is Entry.Folder) {
                Text(
                    gameCount(entry.roms.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (entry is Entry.Game) {
            LocalCompatDb.current.ratingFor(entry.rom.compatKeys())?.let { known ->
                CompatBadge(rating = known.rating, modifier = Modifier.padding(end = 8.dp))
            }
            ConsoleBadge(console = entry.rom.console, modifier = Modifier.padding(end = 4.dp))
        }
    }
}

/**
 * A console's folder, in a tile's place: the tiles' shape, a different
 * substance. The distinction must hold without reading.
 * pourquoi : docs/decisions/bibliotheque.md § Les dossiers de console
 */
@Composable
private fun FolderTile(
    folder: Entry.Folder,
    onClick: () -> Unit,
    selected: Boolean,
    /** True while the pad's confirm button is held on this tile. */
    padHeld: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Same clock as the ring, and gone the instant the cursor leaves — see the
    // ROM tile, where the desync this fixes is written up.
    val focusScale by animateFloatAsState(
        targetValue = if (selected) 1.07f else 1f,
        animationSpec = tween(if (selected) RING_IN_MS else 0),
        label = "folder-focus-scale"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed || padHeld) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "folder-scale"
    )

    Column(
        modifier = Modifier.fillMaxWidth().zIndex(if (selected) 1f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(scale * focusScale)
                .shadow(elevation = 8.dp, shape = TileShape)
                .focusRing(selected, TileShape)
                .clip(TileShape)
                .background(consolePlate(folder.console))
                .moldedRim(TileShape, dark = LocalEmufiiDarkTheme.current, oled = LocalEmufiiOledTheme.current)
                .focusProperties { canFocus = false }
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .gamepadClick(interaction, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            val plate = consoleArtwork(folder.console, LocalEmufiiDarkTheme.current)
            if (plate != null) {
                Image(
                    painter = painterResource(plate),
                    contentDescription = folder.console.label,
                    // Crop, and the tile is square like the source: nothing is
                    // actually cut. What it does is guarantee the plate is
                    // covered whatever rounding the grid gives the cell, where
                    // Fit would leave a hairline of gradient at one edge.
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // The count sits on artwork now, not on a flat plate, so it
                // carries its own ground: the images are busy at the bottom, and
                // a bare label was legible on three consoles out of seven.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .clip(PillShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        gameCount(folder.roms.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            } else {
                // No artwork for this console: the name in type, as before. A
                // console added later must not land on an empty tile.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText(
                        text = folder.console.label,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 13.sp,
                            maxFontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            stepSize = 0.5.sp
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                    )
                    Text(
                        gameCount(folder.roms.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Reserved but left empty: the plate already carries the name, and
        // without the space a folder would lift its whole row.
        // pourquoi : docs/decisions/bibliotheque.md § Les dossiers de console
        Spacer(Modifier.height(32.dp))
    }
}


/**
 * A console's plate, indexed by name so its colour never moves between
 * launches.
 * pourquoi : docs/decisions/bibliotheque.md § Les dossiers de console
 */
@Composable
private fun consolePlate(console: Console): Brush {
    val (c1, c2) = paletteFor(console.name)
    return Brush.linearGradient(colors = listOf(c1, c2), start = Offset.Zero, end = Offset.Infinite)
}

@Composable
private fun gameCount(n: Int): String =
    if (n == 1) stringResource(R.string.lib_folder_count_one)
    else stringResource(R.string.lib_folder_count, n)

/**
 * Where you are, and how to go back up. Hardware back and B do the same thing:
 * this exists for the hand touching the screen.
 * pourquoi : docs/decisions/bibliotheque.md § Les dossiers de console
 */

/**
 * Which end of the header you come back up to. Layouts without columns keep
 * [RIGHT], where the app leads.
 * pourquoi : docs/decisions/bibliotheque.md § Sortir par le haut se nomme, et selon la colonne
 */
private enum class HeaderSide { LEFT, RIGHT }

@Composable
private fun FolderHeader(
    console: Console,
    count: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalEmufiiDarkTheme.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = CircleShape

    Box(
        modifier = modifier
            .focusRing(focused, shape)
            .plate(shape = shape, dark = dark, oled = LocalEmufiiOledTheme.current, lift = 5.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onBack)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            ChevronLeft(size = 18.dp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                console.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                gameCount(count),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Small helper: LazyGridScope items(count, key, itemContent) shorthand
private inline fun androidx.compose.foundation.lazy.grid.LazyGridScope.items(
    count: Int,
    noinline key: ((Int) -> Any)? = null,
    crossinline itemContent: @Composable (Int) -> Unit
) = items(count = count, key = key) { index -> itemContent(index) }

/**
 * What I am looking at on the left, who I am on the right. Nothing about the
 * tunnel: the player cannot act on it.
 * pourquoi : docs/decisions/bibliotheque.md § La barre du haut : deux étagères, jamais une barre
 */
@Composable
private fun FloatingTopBar(
    profile: Profile,
    layout: LibraryLayout,
    onPickLayout: (LibraryLayout) -> Unit,
    sort: LibrarySort,
    onPickSort: (LibrarySort) -> Unit,
    /** The open console folder, if there is one. */
    openConsole: Console?,
    openConsoleCount: Int,
    onLeaveFolder: () -> Unit,
    searchOpen: Boolean,
    query: String,
    onSearchOpen: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onFieldTap: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenFinder: () -> Unit,
    topBarLeftFocus: FocusRequester,
    topBarFocus: FocusRequester,
    /** Going back down into the grid. */
    onLeaveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        // Named, like going up, and on the whole row: the left corner carries
        // buttons now, so one must be able to come down from there too.
        // pourquoi : docs/decisions/bibliotheque.md § Sortir par le haut se nomme, et selon la colonne
        modifier = modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                onLeaveDown()
                true
            } else {
                false
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Two shelves, never one bar: a full-width rectangle has been rejected
        // on this project again and again.
        // pourquoi : docs/decisions/bibliotheque.md § La barre du haut : deux étagères, jamais une barre
        val shelfDark = LocalEmufiiDarkTheme.current
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            // The left group yields to the right group rather than pushing it
            // off screen: the breadcrumb carries a console name, short, but
            // nothing guarantees it stays that way.
            modifier = Modifier
                .weight(1f, fill = false)
                .socket(PillShape, shelfDark)
                .animateContentSize()
                .padding(SHELF_INSET)
        ) {
            // The two states take turns and never share the shelf; size is left
            // to the shelf's own `animateContentSize`, not to `AnimatedContent`.
            // pourquoi : docs/decisions/bibliotheque.md § La recherche prend l'étagère, et les deux états ne se croisent pas
            androidx.compose.animation.AnimatedContent(
                targetState = searchOpen,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(
                        androidx.compose.animation.core.tween(
                            durationMillis = 120,
                            delayMillis = 100,
                            easing = androidx.compose.animation.core.LinearOutSlowInEasing
                        )
                    ).togetherWith(
                        androidx.compose.animation.fadeOut(
                            androidx.compose.animation.core.tween(
                                durationMillis = 100,
                                easing = androidx.compose.animation.core.FastOutLinearInEasing
                            )
                        )
                    ).using(
                        androidx.compose.animation.SizeTransform(clip = false) { _, _ ->
                            androidx.compose.animation.core.snap()
                        }
                    )
                },
                label = "shelf-search-swap"
            ) { open ->
                if (open) {
                    SearchField(
                        value = query,
                        onValueChange = onQueryChange,
                        onClose = onSearchClose,
                        onTap = onFieldTap,
                        modifier = Modifier.focusRequester(topBarLeftFocus)
                    )
                } else {
                    // The same 10.dp the right-hand shelf sets between its own
                    // chips. This Row carried no arrangement at all, so the
                    // three pills sat touching while their opposite numbers
                    // across the bar breathed: one shelf, two rhythms.
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchChip(onClick = onSearchOpen)
                        LayoutChip(
                            current = layout,
                            onPick = onPickLayout,
                            modifier = Modifier.focusRequester(topBarLeftFocus)
                        )
                        SortChip(current = sort, onPick = onPickSort)
                    }
                }
            }
            // In the settings' row, not a line of its own: a full-width band
            // for three words pushed all three layouts down by as much.
            // pourquoi : docs/decisions/bibliotheque.md § Les dossiers de console
            if (!searchOpen) openConsole?.let { console ->
                FolderHeader(
                    console = console,
                    count = openConsoleCount,
                    onBack = onLeaveFolder
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.socket(PillShape, shelfDark).padding(SHELF_INSET)
        ) {
            // Sessions first: that is what the app exists for, and the reading
            // order runs towards oneself, the others, then you.
            SessionsChip(onClick = onOpenFinder, modifier = Modifier.focusRequester(topBarFocus))
            FriendsChip(onClick = onOpenFriends)
            ProfileChip(profile = profile, onClick = onOpenProfile)
        }
    }
}

/**
 * One destination, one pill. Maintenance lives in the settings, not here.
 * pourquoi : docs/decisions/bibliotheque.md § La barre du haut : deux étagères, jamais une barre
 */
@Composable
private fun EmptySlot() {
    val dark = LocalEmufiiDarkTheme.current
    // Barely there on purpose, and a recess rather than a faint plate.
    // pourquoi : docs/decisions/bibliotheque.md § La barre du haut : deux étagères, jamais une barre
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .socket(TileShape, dark)
        )
        Spacer(Modifier.height(8.dp))
        // Reserve same label area height as RomTile (2 lines of labelMedium ≈ 32.dp)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun RomTile(
    rom: Rom,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    /** True when the grid's cursor is on this tile. */
    selected: Boolean,
    /** True while the pad's confirm button is held on this tile. */
    padHeld: Boolean,
    menuOpen: Boolean,
    onChangeIcon: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onDismissMenu: () -> Unit
) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused = selected

    // Tiles arrive rather than appear. Keyed on the ROM so a rescan replays it
    // for what actually changed, and a recomposition doesn't.
    // Composed with the arrival already over, unless the screen has just opened:
    // see [LocalTileEntrance].
    val playEntrance = LocalTileEntrance.current
    var shown by remember(rom.uri) { mutableStateOf(!playEntrance) }
    LaunchedEffect(rom.uri) { shown = true }



    // On the same clock as the ring, leaving on the same instant. A bouncy
    // spring here split the cursor into two halves for a few frames.
    // pourquoi : docs/decisions/bibliotheque.md § Une seule horloge pour tout ce qui marque la cellule
    val focusScale by animateFloatAsState(
        targetValue = if (focused) 1.07f else 1f,
        animationSpec = tween(if (focused) RING_IN_MS else 0),
        label = "tile-focus-scale"
    )

    val entrance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "tile-entrance"
    )

    val scale by animateFloatAsState(
        targetValue = if (pressed || padHeld) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tile-scale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (pressed || padHeld) 2f else 8f,
        label = "tile-elev"
    )

    Column(
        // Above its neighbours while enlarged, otherwise the next one draws over
        // it and the glow is cut clean off.
        modifier = Modifier.fillMaxWidth().zIndex(if (focused) 1f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The glow is the game's own colour, pulled from its artwork: the chrome
        // stays neutral and the content brings the palette. A title with no
        // colour to borrow simply gets the plain shadow.
        val accent = rom.accentArgb?.let { Color(it) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(scale * focusScale * (0.88f + 0.12f * entrance))
                .alpha(entrance)
                .shadow(
                    elevation = (elevation + if (accent != null) 10f else 0f).dp,
                    shape = TileShape,
                    // Ambient stays neutral so the glow reads as light under the
                    // tile rather than as a coloured outline around it.
                    ambientColor = Color.Black.copy(alpha = 0.22f),
                    spotColor = accent ?: Color.Black.copy(alpha = 0.30f)
                )
                // Never on a tile still fading in: a glow is a shadow, and it
                // draws through a translucent layer.
                // pourquoi : docs/decisions/bibliotheque.md § Une seule horloge pour tout ce qui marque la cellule
                .focusRing(focused && entrance > 0.99f, TileShape)
                .clip(TileShape)
                .background(tilePlate())
                // The moulding, over the artwork: a tile is an object with an
                // edge, and box art that runs to the very corner turns it back
                // into a printed square.
                .moldedRim(TileShape, dark = LocalEmufiiDarkTheme.current, oled = LocalEmufiiOledTheme.current)
                // Clickable but NEVER focusable: the grid holds the cursor, so a
                // tile capturing focus makes it vanish.
                // pourquoi : docs/decisions/bibliotheque.md § Le curseur est un index calculé, jamais un focus deviné
                .focusProperties { canFocus = false }
                // combinedClickable and not clickable: long press opens the tile
                // menu. That is the gesture everyone already tries on a grid of
                // icons, and which did nothing until now.
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .gamepadClick(interaction, onClick = onClick)
        ) {
            val art by rememberTileArt(rom)
            if (art.model != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(art.model).build(),
                    contentDescription = rom.displayName,
                    // A remote icon is cropped to fill the tile; the ROM's is
                    // left whole, because at 48 px cropping removes a visible part
                    // of the drawing.
                    contentScale = if (art.isPixelArt) ContentScale.Fit else ContentScale.Crop,
                    // Pixel art scales up without smoothing, otherwise it turns
                    // to mush; a real image does get smoothed.
                    filterQuality =
                        if (art.isPixelArt) FilterQuality.None else FilterQuality.High,
                    // A thin white contour, like the reference app puts around
                    // its icons: it separates artwork from background whatever
                    // the box art happens to be. Kept to 3dp so it reads as a
                    // rim, not as the white plate this used to have.
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                        .clip(ArtworkShape)
                        .border(2.dp, artworkRim(), ArtworkShape),
                    placeholder = ColorPainter(Color.Transparent),
                    error = ColorPainter(Color.Transparent)
                )
            } else {
                PlaceholderArtwork(rom.displayName)
            }

            // Composed *inside* the tile (the Popup's anchor) and *always*
            // composed, never conditioned, so it has time to close.
            // pourquoi : docs/decisions/bibliotheque.md § Le maintien de A, et le titre qui s'efface
            TileMenu(
                expanded = menuOpen,
                title = rom.displayName,
                changeIconLabel = stringResource(R.string.tile_menu_icon),
                renameLabel = stringResource(R.string.tile_menu_rename),
                hideLabel = stringResource(R.string.tile_menu_hide),
                accent = accent,
                onChangeIcon = onChangeIcon,
                onRename = onRename,
                onHide = onHide,
                onDismiss = onDismissMenu
            )

            // Only 3DS files carry an icon, so without a marker a GameCube
            // tile is just a coloured square, and the grid now mixes consoles.
            ConsoleBadge(
                console = rom.console,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
            )

            // Opposite corner from the console badge, and never beside it: the
            // two say different kinds of thing, and stacked in one corner the
            // pair reads as one compound label. Nothing is drawn at all for a
            // game that works, so most tiles keep this corner empty.
            LocalCompatDb.current.ratingFor(rom.compatKeys())?.let { entry ->
                CompatBadge(
                    rating = entry.rating,
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        TileTitle(rom.displayName)
    }
}

/**
 * The title under the tile, whole, fading out at the end when it overflows.
 * Two lines always reserved, even for a one-word title.
 * pourquoi : docs/decisions/bibliotheque.md § Le maintien de A, et le titre qui s'efface
 */
@Composable
private fun TileTitle(title: String) {
    val style = MaterialTheme.typography.labelMedium
    val density = LocalDensity.current
    // Two lines, always, whatever the length: a tile with a short name would
    // otherwise lift its whole row and the grid would lose its alignment.
    val boxHeight = TILE_TITLE_ROOM

    // The fade is only justified when there really is more to come. Applied to
    // every title it made a name that fitted perfectly look truncated: "Crash of
    // the Titans" lost "Titans" in the haze while being complete.
    var overflows by remember(title) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(boxHeight)
            // The gradient applies to the text's own rendering, hence DstIn on a
            // layer: painting a background-coloured rectangle over it would only
            // work on a flat background, and this one is an animated gradient.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                if (overflows) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.55f to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
    ) {
        Text(
            title,
            style = style,
            // Three lines allowed in a box showing only two: that is what makes
            // an over-long title fade downwards instead of stopping dead. A
            // horizontal fade did not work, since the line breaks at the end of a
            // word, so the gradient landed after the text and masked nothing.
            maxLines = 3,
            // No Ellipsis: the dots eat three characters to say something is
            // missing, and the fade already says it.
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            onTextLayout = { overflows = it.lineCount > 2 },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Small, dark, unobtrusive, it labels the tile without competing with the art. */
@Composable
private fun ConsoleBadge(console: Console, modifier: Modifier = Modifier) {
    // Sticker treatment, borrowed from Cocoon's icon style: a white contour is
    // what makes a small mark legible over artwork we don't control. A dark
    // translucent chip disappeared on dark box art and muddied light art.
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFF23262E),
        border = BorderStroke(1.5.dp, Color.White),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Text(
            console.shortLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun PlaceholderArtwork(title: String) {
    val (c1, c2) = paletteFor(title)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(c1, c2),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shortLabel(title),
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black
        )
    }
}

private val PALETTE = listOf(
    Color(0xFF6C5CE7) to Color(0xFF00CEC9),
    Color(0xFFFD79A8) to Color(0xFFE84393),
    Color(0xFF00B894) to Color(0xFF55EFC4),
    Color(0xFFFDCB6E) to Color(0xFFE17055),
    Color(0xFF74B9FF) to Color(0xFF0984E3),
    Color(0xFFA29BFE) to Color(0xFF6C5CE7),
    Color(0xFFFF7675) to Color(0xFFD63031),
    Color(0xFFFAB1A0) to Color(0xFFE17055),
    Color(0xFF81ECEC) to Color(0xFF00CEC9),
    Color(0xFFDDA0DD) to Color(0xFF9B59B6),
    Color(0xFFFFAA5C) to Color(0xFFFF6348)
)

private fun paletteFor(seed: String): Pair<Color, Color> {
    val h = abs(seed.hashCode())
    return PALETTE[h % PALETTE.size]
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String,
    cta: String,
    onCta: () -> Unit,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding, bottom = bottomPadding, start = 32.dp, end = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val dark = LocalEmufiiDarkTheme.current
        // The mark sits on the same moulded disc as every other empty state: a
        // library with no folder yet is still the tray, not a hole in it.
        Box(
            modifier = Modifier
                .size(96.dp)
                .plate(
                    shape = CircleShape,
                    dark = dark,
                    oled = LocalEmufiiOledTheme.current,
                    lift = 6.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            FolderMark(size = 46.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            // On the wallpaper, outside any Surface: it has to name its colour or
            // it falls back to black.
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCta, shape = RoundedCornerShape(50)) {
            Text(cta, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }
}

/**
 * Tells the second display which game the cursor is on. One place, called by
 * all three cursor owners; on the way out it restores the resting face.
 * pourquoi : docs/decisions/bibliotheque.md § Ce qui est publié au second écran
 */
@Composable
private fun PublishHovered(entries: List<Entry>, cursor: Int) {
    val entry = entries.getOrNull(cursor)
    val hovered = (entry as? Entry.Game)?.rom
    // A folder is a machine, and the panel has something to say about a machine
    // that has never had anywhere to be said: how playing together works there.
    val folder = (entry as? Entry.Folder)?.console
    val db = LocalCompatDb.current
    val meta = LocalGameMetaDb.current
    LaunchedEffect(hovered, folder, db, meta) {
        // Cancelled and restarted on each move, so the wait never elapses while
        // the player is still moving: only what they stopped on is announced.
        // pourquoi : docs/decisions/bibliotheque.md § Ce qui est publié au second écran
        delay(SECOND_SCREEN_SETTLE_MS)
        SecondScreen.publish(
            folder?.let { SecondScreenModel.ConsoleFolder(it) } ?: hovered?.let { rom ->
                SecondScreenModel.Browsing(
                    rom = rom,
                    rating = db.ratingFor(rom.compatKeys())?.rating,
                    // Read off the ROM the cursor is already on, never off the
                    // disc: the panel must not make a cursor move cost a file
                    // read, and everything here is in the name and the serial.
                    tags = RomTagReader.read(rom),
                    meta = meta.metaFor(rom.compatKeys()),
                )
            } ?: SecondScreenModel.Idle
        )
    }
    DisposableEffect(Unit) { onDispose { SecondScreen.clear() } }
}

/**
 * How long the cursor has to stand still before the rear panel is told.
 * pourquoi : docs/decisions/bibliotheque.md § Ce qui est publié au second écran
 */
private const val SECOND_SCREEN_SETTLE_MS = 110L
