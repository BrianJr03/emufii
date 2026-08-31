# Library: the three layouts, the cursor, and what was taken back out

The narrative that lived in `ui/screens/LibraryScreen.kt`, taken out of the code
on 2026-08-24 (see `docs/STYLE_COMMENTAIRES.md`). The headings are anchors cited
from the code: do not rename them lightly.

The parent rule is in `CLAUDE.md`, a lazy grid holds its own cursor, and the
ring's detail is in [`navigation-manette.md`](navigation-manette.md).

## Three layouts, one cursor contract

Grid, carousel and list each keep their own index. It is the invariant learned the
hard way on the grid, and it holds just as much for a `LazyRow` or a
`LazyColumn`: a lazy list composes only what is on screen, so a direction's
destination often does not exist yet. What changes from one layout to the next is
what "right" means, and nothing else.

The shared gestures, confirm, open the menu, leave through the top, go up a
folder, are factored out: without that, a gamepad fix in the grid left the other
two broken.

A cell can be a game or a folder, in one list shared by all three layouts:
otherwise each would carry its own "am I in folder mode" branch, three places to
get one question wrong.

## The cursor is a computed index, never a guessed focus

A `LazyVerticalGrid` composes only what is on screen: the tile a direction aims at
often does not exist, Compose then finds no destination and falls back on the
first focusable element, the top left tile. Symptoms seen for real: a cursor that
disappears, a cursor that jumps to the very top on one press, a cursor that jumps
left when changing screen.

An index you compute yourself cannot get lost: it depends on no live component.

Corollary: the tile is clickable but never takes focus. `clickable` makes things
focusable by default, which left as many invisible stops as there are tiles; with
the cursor held by the grid, a tile catching focus makes it disappear with nothing
shown in its place.

## Bring the target, not merely make it "visible"

Compose scrolls to make the focused element visible, and "visible" is all it
wants: a tile half under the top veil counts as visible, so reaching the start of
the list did not scroll to the top.

The grid's insets say exactly what the top and the bottom take: they are used to
finish the movement instead of stopping at the first visible pixel.

In the list, the selection aims at the middle of the usable band. Bringing it just
inside was still wrong: going down, it ended up against the bottom edge with
nothing visible after it, so the player could not see what they were heading
towards. Aiming at the centre scrolls exactly one row per press, with as much list
ahead as behind, which is what every console menu does. Both ends sort themselves
out: `animateScrollBy` saturates, and the cursor moves freely through the first
and last rows.

Two things are added, and both are needed: an inset, because the selected row
carries a glow overflowing its bounds, and the band the bottom veil repaints,
invisible to layout, Compose considering a row underneath perfectly visible when
it is not visible at all.

## Whole rows, or nothing

A board shows objects, and half an object is a rendering fault, not a hint that
there is more. Left to itself, the grid filled the viewport and cut the last row
through the middle of the second line of its titles: "Shadow of the Colossus +
Ico" was sliced through its letters at rest, on a screen nobody had scrolled.

The leftover height is therefore measured and given to the top inset rather than
left at the bottom: the same rows are on screen, they are all whole, and the slack
becomes air under the header instead of a sectioned title. The bottom inset cannot
do that job: it is travel, and travel only exists once you have scrolled.

Only the last few dp of slack are spent, and at the top: a whole row of empty
space centred under the header would read as a botched layout.

The tile size comes from the height, not only from the width. Sized on width
alone, six columns gave a row too tall for two to fit. The column count therefore
rises, smaller plates, more of them, until the rows fit whole. It stops as soon as
they do, and never beyond three extra columns: past that the artwork stops being
recognisable, which costs more than the clipping.

Portrait keeps the three large tiles of the "console menu" feel. In landscape, the
way a handheld is actually held, that would stretch them to a third of a wide
screen, so the column count follows the width and the tiles keep their size.

One column count. Everything downstream, the empty slots squaring the board, and
above all the cursor arithmetic, which moves by plus or minus columns, reads that
one and nothing else. A grid returning one count while the cursor counts another
is the whole family of bugs this screen was written to end.

## The air under the header is named, no longer left to chance

It was nobody's job. The grid pours its leftover height into its top inset, and
that slack incidentally acted as a gap: the grid looked right and nothing said
why. The list has no slack to pour: its first plate arrived against the header's
chips, close enough to touch them, with their drop shadow falling on it.

Hence a named value, which each layout guarantees in its own way: the grid takes
it as a floor on its slack, the list adds it outright.

A floor and not an addition: the slack is usually larger than the gap needs to be,
and adding the two pushed the board 14 dp further for nothing, bringing the last
row's titles into the bottom veil.

## The tiles' arrival is armed, then disarmed

The arrival is for the library opening: the tiles come in and it reads as the
shelf being filled. But a lazy grid composes a tile as soon as it approaches the
viewport, so every row reached replayed it.

And the arrival brings a tile in from transparency, and a translucent layer lets
the cursor's shadow be seen through the tile it surrounds. That is the "hollow
glow" that survived the timing fix: not a badly drawn ring, a tile not yet opaque
underneath.

The animation is therefore armed at opening and then disarmed. A rescan or
entering a folder rearms it, which is what it was written for.

## One clock for everything that marks the cell

The selected tile grows: it is the first signal a console menu gives, and on a
grid of white tiles it carries further than an outline.

On the same clock as the ring, and starting at the same instant. It was a bouncy
spring settling in half a second, four times the ring's arrival, which now leaves
instantly. The tile you had just left therefore stayed enlarged with no ring
around it, a frame with nothing in it, while the one you were arriving at was
still at rest with a ring appearing: for a few frames the cursor looked as though
it had split into two halves leaving a hole behind it.

The ring is never drawn on a tile still appearing, and it is drawn on the clipped
tile, outside the artwork's outline, so the two do not read as one thick
two-coloured border.

## Leaving through the top is named, and depends on the column

From the first row, you leave the grid through the top. The destination is named:
the two layers are siblings in one `Box`, and automatic traversal sees no path
there. The way back down is named the same way, and placed on the whole row rather
than on the right-hand group alone: now that the left corner carries buttons, you
have to be able to come back down from it.

And the destination depends on the column. Every upward move aimed at sessions, on
the right: from a tile at the left edge, the cursor crossed the whole screen for a
gesture that only asked to go up. It now joins the group on its own side. Layouts
with no columns, the carousel with its centred card, the list with its full-width
rows, have no side to infer: they keep the right, where the app leads.

## The carousel has to follow the finger without turning on the gamepad

The selected card comes to the centre, not "somewhere on screen": a carousel whose
active element ends up stuck to an edge no longer reads as a carousel.

The active card is the one closest to the middle of the viewport, whatever brought
it there. That is what makes it work under a finger: the active one used to be
whichever the D-pad had last designated, so a touch scroll moved the row while the
enlarged card stayed behind, and the carousel came to rest between two cards with
the wrong one lit.

But the tracking has to be switched off during our own scrolls: a programmed
scroll sweeps every card between here and the target, and if the cursor followed
the centre meanwhile, a quick double press would compute its second step from the
card passed over on the way.

Likewise, snapping must answer to the finger only. Keyed on `isScrollInProgress`
alone, it also fired at the end of our own animations and snapped from wherever
that scroll declared itself at that instant: a tap on the neighbouring card landed
two cards further on. A drag interaction is the only honest signal that a person,
and not this file, moved the row.

A tap on a card that is not in the middle brings it to the middle; only the middle
one opens. Launching straight from a side card was the other half of the carousel
being gamepad-only: the centre meant something and touch could ignore it. It also
made the neighbours, drawn small and faded precisely to say "not this one", the
easiest things to launch by accident.

### Three carousel measurements, all corrected from a screenshot

- The card is sized on the height actually free, not on the screen's. A fraction
  of the screen's smallest dimension gave cards that overflowed: the top band, the
  banner and the navigation bar each take their share, and the title under the card
  wants forty-odd dp more.
- The side insets are half of what is left around a card, and that is what lets
  the first and the last reach the centre. With a fixed inset, a list cannot scroll
  before its start: the active card stayed stuck to the left edge for up to two
  steps, so the carousel always opened crooked.
- It is the card that is centred, not the column. An item is a card above its
  title; centring the whole puts the column's middle at the screen's middle, so the
  artwork, its top part, the only one looked at, ends up too high. The title's space
  is therefore moved from bottom to top rather than added at the top: the sum of the
  two insets does not change, so the card comes down without losing any of its size.
  The first attempt only added at the top, and the card lost a fifth of itself for a
  purely positional flaw. And half the title's space, not all of it: moving x from
  one side to the other shifts the content by x, and it has to shift by half a
  title. Measured on a screenshot, not judged by eye.

Finally, the leading inset is already counted in `animateScrollToItem`'s frame of
reference (`viewportStartOffset` equals `-beforeContentPadding`): passing that
same offset again applied it twice, the list did not move a step on the first
cards, and the cursor advanced while the selected card stayed right of centre.

## The list exists to tell two dumps of one game apart

An icon does not separate two dumps of the same game, nor two episodes of a series
sharing artwork. The list exists for that moment: the full name on one line, the
console on the right, a thumbnail big enough to recognise without dominating.

The ring first, before anything that clips, the house rule, and above all before
an opaque fill. The row was a translucent film (white at 8% on the board) with the
ring applied afterwards. Two faults in one: the glow is a shadow, and a shadow
under a transparent layer is drawn through it, so the cursor's light spread into
the row as a flat square-ended wash, the "glow that breaks up and goes hollow",
its second and last source. And a film is not this app's material: every
selectable surface in it is a moulded plate.

## The console folders

A folder borrows the tiles' shape (square, same corners, same focus glow) and
departs from it in substance: a coloured plate carrying the console's name, not
artwork. The distinction has to hold at the speed you sweep a grid, without
reading.

The title's space is reserved but left empty: the plate already carries the name
large, and repeating it below gave the same word twice ten dp apart. The space has
to stay, without which a folder would lift its whole row relative to the empty
slots completing it.

Two illustration files per console, light and dark, because these are
illustrations with their own ground and not glyphs to tint: recolouring one would
spoil the drawing, and showing the light one on the dark theme puts a white square
in the middle of a black grid. The return is nullable and that is the whole point:
a console added tomorrow shows its name in type until its two files exist, instead
of borrowing another machine's illustration.

Failing that, the plate takes a colour from the palette already used for missing
artwork, keyed on the console's name: two consoles do not land on the same colour,
and a console's colour does not move between launches.

The breadcrumb lives in the settings row rather than on a line of its own: a
full-width band for three words pushed the grid, the list and the carousel down by
that much, while the top bar has the room, and "where am I" belongs to the same
family of questions as "how am I looking".

## The top bar: two shelves, never a bar

On the left what I am looking at, on the right who I am. The logo held the left
corner and did nothing there: a mark you read once, on the screen you open most
often. The two display settings replaced it, being what you actually come to
touch.

Nothing about the tunnel here either: it is driven by the session, so it is not
plumbing the player starts or stops, and an indicator reporting it would report
something they cannot act on.

Each group is in its own hollow, the inset screen where a console puts its lamps.
Two shelves and not a bar: a full-width rectangle at the top has been rejected on
this project repeatedly, and it would crush the board under a header. Sunk in, the
discs stop reading as five scattered buttons across nine hundred pixels of nothing
and become a panel with controls in it.

The dock keeps one destination per chip. It carried Folder and Rescan alongside:
two maintenance items you touch once and never again, and putting them permanently
on the home screen gave three chips of equal appearance of which only one led
anywhere.

The empty slots are barely there, deliberately: they exist to keep the grid
square, as a console menu does, not to look like content that failed to load. A
hollow rather than a pale plate: an empty place on a board is a socket with
nothing in it, and it is lit from below where a plate is lit from above.

## Search takes the shelf, and the two states do not cross

A field and the layout chips at the same time would promise two things at once,
and on a handheld the shelf does not have the width anyway. What the player was
looking at comes back intact on closing.

The two states take turns, they do not share the shelf. Crossing them is what made
closing flicker: the field and each of the three chips draw their own hollow, and
a hollow is a translucent inset. Overlaid at partial alpha, they stacked into a
lighter slab for two frames, which reads as a blink, not as an exchange. The
outgoing one leaves before the incoming one starts.

Size is left to the shelf's `animateContentSize`, which also has the breadcrumb to
carry: `AnimatedContent` animates size too by default, and the two pulling on the
same node made the rest of the wobble.

The input panel is a state of its own, not a second reading of "search is open".
Linking them meant putting the keyboard away threw the query out: the player put
the panel down to see the results they had just spelled out, and it was the
results that disappeared. Typing and reading are two halves of one search: only
closing the search clears the field.

The keyboard floats over the bottom of the grid and never beyond its half: what
the player is looking for stays on screen while they spell it. It rises from the
bottom edge rather than appearing: a keyboard that pops up is a system dialog, a
keyboard that slides is part of the board.

Its exit is a slide alone, with no fade, and past the edge. The panel is frosted by
Haze, and fading it amounted to animating the alpha of a blurred layer: the blur
lets go meanwhile, so for a frame or two the flat fill underneath shows. On top of
that, the old exit only travelled a third of the height, so the panel reached zero
alpha while still two thirds on screen and vanished on the spot instead of
leaving.

A tap outside the panel puts it away. Nothing said so before: the keyboard had no
exit but system back, and a tap on the grid went through to a tile and opened a
game mid-word. The area is invisible and declared before the top bar, so the
search field stays above it and still answers: it is the one thing outside the
panel that must not put it away, since touching it is how you bring it back.

## The veils, and why the launch card is where it is

Both veils are inside the Haze source and after the grid. This screen was the only
one carrying floating chrome without them: the name chip and the profile pills
ended up bare on the artwork as soon as the grid scrolled, and the dock
permanently hid two tile titles. Content that rises has to go somewhere.
`EmufiiScaffold` already solved exactly that everywhere else: it is its technique,
not a second one.

They clip the grid rather than taking space from it: the top band and the dock
still float above, and the tiles dissolve into them instead of crossing them.

The launch card is in that `Box`, and last, on purpose. It has to be a sibling of
the Haze source rather than a `Dialog` window in order to blur the grid behind it,
and to come after the top bar and the dock so a modal covers the chrome instead of
leaving it floating over.

It is deliberately left up: the work it has just started publishes no screen of its
own before the tunnel step, so its own indicator is what covers the wait. It goes
with the library when the flow finally navigates, and its state is held in this
screen, so coming back gives a clean one rather than the card still open.

The update banner pushes the grid down instead of sitting on it: covering the
first row would make the announcement be paid for in games become untouchable.

Online play has its own button for the one console that has it alongside a
session, the PSP's public ad hoc. It is a second kind of multiplayer, hence a
button of its own rather than one more crossroads before creating a session. The
PS2 had one too, its revival servers, set aside on 2026-08-19 (see
`docs/PS2_ONLINE_MIS_DE_COTE.md`); its Local Link is intact and still goes through
a session.

## Holding A, and the title that fades out

Compose's `combinedClickable` only gives long press to fingers: a gamepad's A
arrives as a key event, and a key event has no duration, it is up to the app to
time it. Hence a timer armed on key down and disarmed on key up, whose only
guarantee to keep is that one press does exactly one thing: open the menu on hold,
or launch on release, never both.

The hold state is Compose state and not a plain field: the tile under the cursor
reads it to sink, so the grid has to recompose when it changes. A button held with
no answer on screen reads as a failed press.

The menu is composed inside the tile: that is what gives the `Popup` the tile's
bounds as an anchor, without reading or carrying coordinates by hand. And it is
always composed, never conditional: that is what gives it time to close. It only
opens its window if it has something to show.

The title fades out at the end of the line when it overflows, instead of an
ellipsis. The dots cut hard and eat three characters to say something is missing:
on "The Legend of Zelda: A Link Between Worlds" you lost the subtitle and the room
to announce it. The fade lets you read everything that fits and trails off, so the
reader understands there is more without it costing space.

Two lines always reserved, even for a one-word title: otherwise a short-named tile
would lift its whole row and the grid would lose its alignment.

## What is published to the second screen

One place, called by all three cursor holders: the grid, the carousel and the list
each keep their index, and duplicating the mapping is how the two screens would end
up disagreeing about what is selected. They are alternatives: exactly one is
composed at a time.

On leaving, the panel is returned to its resting face rather than leaving the last
game shining on the back of the machine after the player has gone elsewhere. The
next screen publishes its own state just after, so the pair converges without
either knowing the other.

Publication waits a tenth of a second of stillness. A held direction crosses a
shelf at some ten tiles a second, and each one was published: the panel spent its
time starting work, an artwork lookup, a folder listing, for a game the cursor had
already left, and the visible result was a face that seemed to load several times.
The effect is cancelled and restarted on every move, so the wait never falls due
while the player is moving. A tenth of a second: below the threshold where a
deliberate press feels delayed, above the time it takes to cross a tile on hold.

## Hidden consoles are hidden here, not in the scan

The filter is applied when the grid is built, not at scan time: the repository's
cache is shared with the session flow, which must go on finding a ROM by its title
id even for a hidden console. Hiding a console is a statement about this screen,
not about what the app owns; a friend's code still opens what it opens.

The open folder is reset as soon as the sort mode changes: keeping a folder open
while going back to A-Z would leave the library silently missing a console, with
nothing on screen to explain it.

## The air under the bar is the cursor's, and it is computed

Two constants come out of the same calculation and have to be redone together:
`SHELF_INSET`, the chips' inset in their hollow, and `HEADER_GAP`, the air between
the floating bar and the first row.

The neon cursor overflows what it surrounds. On a 46 dp chip: `band` is
`TILE_BAND` on its side, plus the halo still radiating 0.8 times its blur over
that, about 8.7 dp. The hollow left it only 6, and the halo of the chips at either
end was cut off flat on the pill's edge.

Two things clipped it, not one: the hollow clips to its shape, and
`animateContentSize`, on the left shelf, is a `clipToBounds` followed by a size
animation. Neither can be removed without consequence, and neither needs to be: it
is enough for the hollow to be wide enough to hold the cursor it is supposed to
show.

Same calculation for the grid's first row, on a 130 dp tile: about 20 dp above its
layout box, the 7% growth included. The previous 14 dp were already not enough;
they held because the shelf stopped 8 dp higher, and the account went overdrawn
the day the hollow made room for the cursor.

If the neon changes calibre (`TILE_BAND`, `minBand`, `CursorRing`'s blur), redo
this sum: the symptom itself is silent.

## Bringing the target: both edges are read in the same frame

`item.offset.y` counts from the start of the content. The top inset therefore
lives before zero, at negative offsets, and `viewportStartOffset` is precisely
minus that inset. The usable top edge is zero, the bottom one `viewportEndOffset`
minus the bottom inset.

The previous calculation added the top inset to both edges:
`beforeContentPadding` at the top, and `viewportEndOffset - viewportStartOffset`
at the bottom, which is the viewport's height and not its edge. At the top the
generosity did not show: the tile arrived lower than needed, which is pleasant. At
the bottom, the grid thought itself a hundred pixels taller than it is: it stopped
before bringing the tile in, which stayed stuck to the edge or bit into it.

Hence a downward move distinctly worse than an upward one, for one term too many,
and an asymmetry nobody could have guessed from reading the line.

## What the tile reads must change only for it

`selected` and `padHeld` were computed in the item lambda: both states were
therefore read by the fourteen tiles on screen, and one cursor step recomposed them
all, fourteen plates, mouldings, shadows and pieces of artwork rebuilt so two could
change state. Scrolling fast, one step every fifty milliseconds, that is what ate
the frame budget.

A derived state only tells its readers when its result changes. The value is still
computed at every step, it being an integer comparison, but only the tile lighting
up and the one going dark recompose. Same treatment for the list and the carousel.

Corollary: neither `cursor` nor `padFocused` must be read in the body of a
composable on this page, or everything in it re-subscribes to the cursor. They are
kept as state objects and delegated just after; the readings that remain are in
event callbacks or effects, which subscribe to nothing. `PublishHovered` is the
one exception, and it is intended: that composable renders nothing, so its
recomposition costs only itself.

## One animation for the cursor's three marks

The selected tile's growth and its two staircase steps share a clock: a cursor
does not split into pieces each arriving in its own time. They were nonetheless
held by three separate animations, so three `Animatable`s, three effects and three
subscriptions per tile on screen, for one value. They are now one, from which the
three are derived.

## What wakes the second screen has a threshold, and it was too short

Publishing wakes the second window: it recomposes a whole face, opens its 220 ms
fade and asks for the artwork. At 110 ms, scrolling at one step every two tenths,
already fast, not frantic, crossed the threshold at every step, and therefore
redid that work at every step.

Raised to 200 ms on 2026-08-29: a sustained scroll publishes nothing while it
lasts, and the panel announces only what the player stops on. That was already the
written intent; the threshold did not hold it.

## One menu row, not two pixel-identical copies

It existed in two identical copies: `MenuRow` in a tile's menu and `ChipMenuRow`
in the Display and Sort menus, same interaction source, same 7% highlight, same
radius of 14, same insets, same 18 dp glyph. Two copies of one part stay identical
only while nobody retouches one, and that is exactly what was about to happen: one
of the two had just received a cursor landing point the other did not have.

What really differed comes down to two parameters: a tick on the right for the
current option, and the cursor holder the menu places on that row. A tile's menu
has neither, you are not "in" it, you act in it, and leaves them null.

The highlight is a ground, never a ring: these menus are three lines, are read at
a glance, and a ring there would carry the weight of an isolated control when it
is a list.

## The insets are read "ignoring visibility"

The loading screen hides the system bars while the logo shows and gives them back
on leaving: their insets are therefore zero throughout the logo, and come back at
the precise instant it fades. Any layout reading the ordinary inset laid itself out
wrongly for four seconds and then recounted under the player's eyes, the grid going
from six columns to seven.

The "ignoring visibility" variant gives the space the bar would take, whether it
is shown or not. The layout is therefore the same before and after, and nothing
moves. The final value does not change by a pixel: it is the only way to fix the
jump without touching the grid's density.

And the bottom veil repaints the board over its band: what is laid out under it is
invisible although Compose considers it on screen. Measuring against the available
height alone is what put a row's titles under that veil.

## The search slab is wide, therefore short

At 72% wide, a row's ten keys were narrow, therefore tall to stay clickable, so
the panel ate half the screen, the half where the results being filtered live.
Widening it makes each key wider for the same area, and the four rows fit in 42%
of the height instead of 50%. A whole row of artwork comes back.

Leaving through the top is named too, and through `onKeyEvent` rather than
`onPreviewKeyEvent`: the latter only fires if nobody below has consumed the key. A
direction that moves the cursor is consumed by the focus system; this one
therefore only bubbles up from the top row, where there is nothing above it in the
slab. That is exactly the moment you want to return to the search field.

## The panel stops talking about the game when you leave the grid

The grid holds its own cursor, and the panel subscribed to that alone: the cursor
went up into the header and the panel went on showing the last selected game's
card, with its "B - Open" and "Hold - Game menu" caption. But B, up there, opens
the profile or the search, and holding opens nothing. It was the app's only control
caption, and it lied as soon as you left the grid.

The resting face is placed over the top rather than published: the grid's publisher
would not fire again on return, its key being the selected game, which has not
changed.

## The service lamp goes out when the panel is lit

Hidden while search holds the shelf: the field takes all the width it is left, and
two words to its right clipped it exactly when you are typing.

And hidden when the rear panel is lit, where the same lamp already burns at the
top of the resting face. It is not information leaving the main screen, the
single-screen rule holds, it is the one thing both screens would say at the same
time, word for word, thirty centimetres apart.

## The console badge is 9 dp from the edge, not 6

The tile carries a moulding, a 1.5 dp light edge followed by a bevel, and at 6 dp
the badge bit into it. It did not show as an overlap but as a white rim eaten into
over two or three pixels, which is enough to make the tile read as badly cut.

The marker itself exists because only 3DS files carry an icon: without it a
GameCube tile is only a coloured square, and the grid mixes the consoles up.

## The two library settings took the logo's corner

A logotype does nothing; these two buttons change what is in front of you. The top
bar therefore reads "what I am looking at" on the left, "who I am" on the right.

The glyph shows the state, not the function. The display pill draws the current
layout rather than a generic settings icon: without that, nothing on screen would
say which mode you are in once the menu is closed, and in carousel, where only one
game is visible, that is precisely the question you ask. Each glyph draws its own
layout: three squares for the grid, one large card flanked by two slices for the
carousel, thumbnailed lines for the list.

Sort fits in three symbols. A-Z and date share the descending bar scale, the
universal sign for sorting, and are told apart by what accompanies them: nothing
for alphabetical order, a clock for date. "By console" is a folder, because it is
not an order but a filing, and the glyph has to say so before you try it.

## A menu's cursor lands on the current option

A three-line menu where you already are somewhere: putting the cursor at the top
forces you to re-read all three to find where you were, when the tick already says
it. Placed on the ticked line, the menu opens by answering "what is it, now?" and
one press is enough to go to the neighbour.

And without that placing there was no ring at all on opening: a modal layer opens
over a scaffold that has already placed its cursor elsewhere, and nothing takes it
back from it.

The card unrolling under a pill reuses the tile menu's material and movement: two
menus opening differently on the same screen read as two mechanisms, when there is
only one. The window outlives the close for as long as the unroll takes to
reverse: removed at the instant of the click, there would be nothing left to
animate.

## Search, and the cross that closes it

Results cross every console: "where is this game" is exactly the question console
folders cannot answer, since the answer is often a console the player was not
looking at.

Touching the field brings the keyboard up. The slab can be put down to read the
results without ending the search, so the field has to be the way back to typing;
the cross stays the only control that ends the search.

Hence its area: it was clickable over its 18 dp of stroke, well under the touch
minimum, and missing it sends the press onto the field, which reopens the
keyboard, the exact opposite of what was asked. The bar is only 36 dp tall, so 48
would not fit; 32 is what the room allows, and it is already three times the
previous area. The area grows, the glyph does not.
