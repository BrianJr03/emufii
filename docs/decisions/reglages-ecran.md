# The settings screen: one hub and seven pages

The narrative that lived in `ui/screens/SettingsScreen.kt`, taken out of the code
on 2026-08-24 (see `docs/STYLE_COMMENTAIRES.md`), then extended on 2026-08-25
when the accordion became a hub and seven pages. Headings are anchors cited from
the code: do not rename them lightly.

The code now lives in `ui/screens/settings/`: `SettingsScreen.kt` (the host and
the hub), `SettingsPieces.kt` (the shared parts) and one page per file.

## What this screen replaces

> The sections from that era, this one, "One column, bounded and centred", "A row
> takes the whole card", "Two focus traps" and "The theme opens a panel",
> describe the accordion, replaced on 2026-08-25 by the hub and its pages. They
> stay because each records an attempt that was paid for: reopening them without
> reading them would cost twice.

It was called "Profile" and carried eight cards of equal weight: the nickname,
the ROM folder, the console keys, the artwork key, the language, the theme, the
About box, the reset. Each set out a title, a paragraph of explanation and its
buttons, all the time, whether you had come to see them or not.

On the Thor, the Library card took a whole screen for three lines, the two
columns ended up misaligned, and finding a setting meant digging through a wall
of text.

Here every setting is a row: what it is on the left, where it stands on the
right. The explanatory text and the buttons only exist once the row is open,
which is to say at the precise moment they were asked for. The value shown on the
right is what replaces the paragraph: "Set", "ROMS", "French" already answer the
question you came to ask.

One row open at a time. Two expanded sections rebuild exactly the screen we just
dismantled, and on a handheld the page would start scrolling again at the first
setting touched.

The name is saved as you type rather than behind a button: there is nothing to
validate and nothing to send, storage is local, and a button that only means "yes,
really" is a button nobody needs.

## One hub and seven pages, plus an accordion

The single screen held fourteen expanding rows across four sections. Each was
defensible; the whole no longer was.

Three flaws, and the third is the one that decided it:

1. One row open at a time was the right rule for a screen of eight rows; at
   fourteen it means touching a setting pushes the whole rest of the page down,
   and you keep closing things to find your place again.
2. The filing lied. PPSSPP auto-configuration, the PS2 network profile and
   Azahar's autofill lived under "Application", between the language and the
   theme, because there was nowhere to put them. They are not application
   settings: they are rituals outside Emufii, without which a session is refused.
3. An expanded detail is not a page. The PS2 profile open is a note, two buttons,
   a state hollow with four facts and a caveat, in an 18 dp slot wedged between
   two other rows. The content had been asking for a page for a long time; the
   expanding row refused it one.

So: a hub containing only entries, and seven pages behind it: Profile, Library,
Consoles, Emulators, Appearance, General, About. The hub is this screen's root;
`B` returns there before leaving settings, as it used to close the expanded row.

What has not changed: the content of the blocks. `DetailNote`, `DetailActions`
and `DetailStatus` are exactly the accordion's, on the same three rules (see
`ui/components/SettingsDetail.kt`). A page is the same material, without the
slot. What disappears is the expansion machinery: the morphing corners, the
delayed `bringIntoView`, the enum of the open row.

## A hub entry is a plate, not a row

Each entry is its own `SoftCard`, not a row in a shared card. That removed at a
stroke all the machinery the shared card imposed: the corners animating to say
which row inherits the card's, and the opaque fill sliced out of the card's
gradient so the cursor's glow stays outside. A plate is already opaque, and
`SoftCard` already places the ring in its shape, before its own `clip`.

The sliced fill stays for what still lives inside a card, the choices in a list
(language), which are rows in a block.

And they are not tiles. The tile grid is the library's grammar, where the content
is the artwork and the eye aims at an image. A settings page has nothing to show,
it has names to read; a name is read in a row, at the speed you look for a word
in a list. The tile-grid variant was written and then removed on 2026-08-25: it
reproduced the main menu for content that is not content.

## The hub fits on one screen, without group labels

The hub first carried four group labels, then lost them, then got them back, and
all three states were seen on the device, which is worth telling in order.

They cost twice. The second repeated word for word the name of the entry it
headed, "Library" above "Library", and the four together took enough height that
only two and a half entries stayed visible on the Thor's 1080 px. Removed, four
and a half fitted, and the families read from the spacing.

It was not the principle that was wrong, though, it was the wording. A settings
menu with no aisles is a list, and a list of seven entries gets scanned instead
of aimed at. A label must name the family, never its first member: "Library and
consoles" heads its two entries without repeating either, and the repetition
problem disappears without the hub losing its aisles.

The four names kept: "You", "Library and consoles", "Before playing", "The
application". None is a page title.

The spacing is tight between two entries of one family (10 dp) and open between
two families, label included (18 dp): it is the grouping that carries the
reading, the label only names it.

## The hub is a grid, and the panel shows the selected cell

Set on 2026-08-28, from a reference brought by the user: the settings menu of
another handheld console frontend, two rows of large cells scrolling sideways,
and the bottom screen showing the selected cell large.

What changes is not the row, it is its layout. The section "A hub entry is a
plate, not a row" stays true word for word: an entry is still a plate with its
icon on the left, its name, its description line and its chevron on the right,
because a settings page has no content to show but names to read. What was wrong
is the column: seven plates stacked on a screen 1080 wide left two thirds of the
width empty and scrolled what could have fitted at once.

Two columns of equal width, filled row by row, and the overflow below. Three
layouts were tried on the Thor, which is only ~800 dp wide:

- Four cells across: the name fits, the description line and the badge no longer
  fit together.
- Two rows overflowing to the right, like the reference: the cell stays large,
  but the page pushes sideways. Refused by the user, and for a reason about the
  machine rather than taste: on a device whose right thumb falls on a D-pad, you
  scroll a settings page down, you do not push it sideways.
- Two columns going down: each cell takes half the width, the seven fill row by
  row, and the overflow unrolls downwards. That is the layout kept.

The family labels went with the column. They headed stacked groups; a grid has no
aisles, it has cells, and seven cells visible at once are found by name. It is
the same argument as the section "The hub fits on one screen, without group
labels", which the column had ended up making false and the grid makes true
again.

Nothing lazy here, and that is the point. The seven cells are composed, so
Compose focus traversal always finds its destination and brings the selected cell
into view itself. It is the exact opposite of the library, which holds its own
cursor because a `LazyVerticalGrid` does not compose the tile a direction aims at
(`CLAUDE.md`, the grid holds its own cursor). Seven cells are not worth that
machinery, and writing it would have been copying a remedy without having the
disease.

### The panel shows the selected cell, it does not replace it

`SecondScreenModel.SettingsEntry` carries the name, the line, the path root and a
mark name. The panel draws them large: the mark in its notch tinted by domain,
the name, the description, then "Settings > <name>" small.

Two rules, both already written elsewhere and applying here:

- It delegates nothing. The front tile already carries all three pieces of
  information; a player with one screen loses not a word (`CLAUDE.md`, the single
  screen stays the main layout). What the panel adds is size.
- The mark travels as a name, not as a composable. The model lives process-wide
  and outlives the activity; a composition lambda held in there would hold the
  tree that created it with it. The panel does the mapping at its end, the same
  choice as the session step labels.

The fade between two entries is a cross-fade, never a slide: the hub's cells are
not an ordered sequence you move along, they are a table, and an invented
direction would state an order that does not exist.

## The hub badge reuses the bead, it does not invent a second one

An entry leading to something to prepare carries a badge: `DetailStatus`'s state
bead, the same four tones and the same four glyphs, plus the word the bead alone
does not carry. An application is allowed to say "this is fine" in one way only,
and a settings screen that had invented a second badge would have taught it
twice.

Two entries carry none, deliberately:

- Consoles: hiding a console is a taste, not a state to catch up on. A green
  badge there would say "nothing to do" on a page where there is never anything
  to do. The count ("7 consoles of 7") sits in the summary, where it reads as a
  fact and not as a verdict.
- Appearance, General, About: their summary is their state.

Emulators only goes green at `3 / 3`. It is the one page that asks the player for
something, and "2 / 3" in green would read as "nothing to do".

## One icon per page, and not one more

Seven drawn marks (`TrayIcons.kt`), one per page. In a menu where every row has
the same shape, the eye finds a page by its silhouette before reading its name;
that is the only reason they exist, and it is why there is no eighth one for
decoration.

They follow the system's three rules: a 24-unit box, round caps and joins, one
stroke weight. Two drawing details:

- The consoles mark draws only three tiles out of four. The hole is the hidden
  console, and it says what the page does better than a fourth tile would.
- Every mark is placed in a round socket. Bare on the plate, an icon floats, and
  seven floating icons in a line read as decoration; in a hollow, each is one
  more moulded object.

They are in muted ink, never in the accent: the accent means one thing, "this is
here", and seven cyan marks would take that away from it (see
`direction-visuelle.md` on the single-accent rule). The hub's only colour is the
player's avatar, which comes from content.

## Emulators are not an application setting

The Emulators page gathers the three preparations done outside Emufii: PPSSPP
configuration, the PS2 network profile to import into ARMSX2, and the
accessibility service Azahar uses for autofill. They have nothing in common with
the language or the theme, and everything in common with each other: they are the
three things Emufii asks of the player, and a session is refused until they are
done.

It is also the only page whose state can be counted, hence the hub's `3 / 3`.

## The reset lives on the page it erases

It had its own section, "Red zone", for a single row. A whole section for a
gesture you make once in the app's life was already generous on a single screen;
on a hub of seven entries it would have made an eighth entry whose only function
is to be dangerous.

It is therefore at the foot of the Profile page, under the identity it erases, in
shell red, with no turning chevron, and the confirmation that follows carries the
warning. It is the only red on this screen.

## A setting with only two states is a switch

Notifications and the second screen were driven by buttons whose label changed:
"Friends off" became "Friends on" when pressed. Such a button asks a question on
every reading: does it describe the state, or the action it triggers? You ask it
for half a second before pressing, every time, and a switch never asks it.

The switch is moulded, not Material: the track is the same socket as the text
fields and the grid's empty slots, the knob is the same plate as everything else.
Material's `Switch` draws a tinted track and a flat pill, which on a moulded plate
reads as a sticker, and it paints a focus wash, switched off everywhere in this
app, which reads as "disabled" on a console where the cursor is permanently
somewhere.

Three details paid for:

- The knob is always the light plate, whatever the theme. As a dark plate on a
  dark theme it read as one more hole in the socket instead of the knob sliding
  in it.
- On, it is the hollow that takes the accent, not the knob: the accent there
  means "running", and the cursor keeps its ring to mean "here".
- The whole row is the target, not the 52 dp pill at the end of the line: aiming
  at it with a pad is work, and two-handed on a console it is the wrong gesture.
  The switch therefore carries no ring of its own.

There is only one in the app. The launch card had its own, a Material `Switch`,
the last Material control left on screen; it took this one.

## Appearance is compared at a glance

The four boards and the eight beads sat in one block, one above the other, and
the last row of accents fell below the Thor's fold. It is the one screen in the
app where everything must be visible together, since comparing is exactly what
you do there: a choice you have to scroll to see is no longer a comparison, it is
a list.

Two blocks side by side, theme and accent, and the whole fits. The beads go from
two rows of four to one row of eight when the column is wide enough: the grid
keeps its columns, it changes their number.

## The folder card shows the folder

The ROM folder is the most important thing this block contains, and it sat on a
label-value line lost across half an empty column: the card was, word for word,
"ROM folder / Folder ROMS" and two buttons.

It is now the block's subject: the folder mark, the name the player chose, and
under it what is true of that folder, that subfolders are walked too, or, when
there are none yet, what is expected of it. The game count lives in the header
badge, where it is a state.

## Giving up Cocoon needs a fresh walk

`CocoonMedia.forget()` only empties Cocoon's index. The thumbnails written during
the scan stay on disk: after giving up Cocoon, the preview strip and the grid
went on showing its images, and the setting looked inert while it had very much
taken effect.

Changing the image source, choosing it as much as giving it up, therefore
triggers a fresh walk. It is the only way the source really changes, and it is
also what makes the preview move under the player's eyes.

## The two outbound links, and their order

They are the only two outbound links in the whole application, and they live on
About, never in a dialog, never at launch.

The Discord one is filled, the Ko-fi one is a ghost, and the order is not
alphabetical: Discord is the only one of the two that gives the player something
back, somebody to play with, somewhere to report what breaks. An app that asks
for money more loudly than it offers help reads as a ticket window.

Each carries its destination's mark, before its label. Discord's is the official
mark, in its blue; Ko-fi's is a cup redrawn in the app's icon language, a 24-unit
box, one stroke, and does not claim to be the service's logo, whose name is
spelled out right next to it. These marks are not tinted by the accent: they
point somewhere else, so they are content, like a console icon.

Two things were written and then removed from this page:

- The paragraph above the buttons. It said what the Discord is for and where the
  money goes. It was true, and nobody reads it before pressing the button it
  heads: two buttons whose labels already carry their destination have nothing to
  explain.
- The card of seven consoles. You visit About to find a version or a link; the
  list of consoles is already in the grid, on the Consoles page and on every
  tile. An image belongs only if it answers the question its page asks.

Two cards remain, side by side, with the same top and the same foot.

## Aligning two columns means measuring, not intrinsics

On About, the left column carries two blocks and the right one only one. Left to
itself, the lone block stops halfway down and leaves a three-hundred-pixel hole at
the foot of the page.

`Modifier.height(IntrinsicSize.Min)` is the obvious answer, and it is wrong here:
a paragraph's minimum intrinsic height is the one it would take at the width of
its longest word, so enormous. Tried, measured on the device: the row took two
and a half screens of height and the lone block stretched inside it.

What works is dumb and solid: measure both columns (`onSizeChanged`) and impose
the larger of the two as a minimum on both. The first frame sees them at zero,
the next has them, and nothing jumps because a card only ever grows.

The word minimum is the second trap, paid for after the first: imposing the
height measured on the left as a size on the right crushed the right card's last
button into a three-pixel line. An imposed height clips; a minimum lets things
grow.

And the stretched block does not spread all its content: only its foot moves
down. Even spreading also pushed the text away from its title, and the block had
a hole in the middle. Hence the `footer` slot, and the gap that goes only between
the two.

## A switched-off console is a hole in the board

The seven tiles were plates in both cases, with an accent bar underneath to say
which was on. Two neighbouring tiles looked alike to within four dp, and on dark
themes the bar was the only thing to read across a whole grid.

The board already knows how to say two things: "set on top" (`plate`) and "sunk
into" (`socket`). That is exactly the distinction this page makes, so it borrows
it: a shown console is a plate, a hidden console is a hole. The bar goes away, two
marks for one state being noise, and the content's dimming stays, so the
switched-off tile still names its machine.

## The console grid is not allowed an orphan

Taking the most columns that fit was the reflex, and it gives the worst result of
the lot: seven consoles in a card that carries six makes six tiles and then one
alone on the next line. An orphan reads as an oversight, not as a grid.

So the column count chosen is the one that best fills the last row: seven in six
columns becomes four plus three, and the incomplete row ends in an empty socket,
as the library already does. When everything fits on one line, it stays one line:
that is the layout the home page was drawn for.

Two consequences of form, both measured on the device:

- The tile has a fixed height. The last row's sockets must be the same, and an
  intrinsic height is not shared between siblings without measuring. The value is
  the content's, an icon and three lines; eight dp less and the version number
  was clipped.
- The explanatory sentence is bounded, and there is only one left. It ran the
  full width of the card, nearly 1700 px, where the eye loses the line before
  finding its end. The second one, "tested at the versions shown here", said what
  the numbers under each tile already say, and it was the one pushing the page
  below the fold.

And the block has a title, which is not the page's: without it, the state badge
ended up alone at the end of an empty line, where it read as an accident.

## On a page, the state comes before the explanation

The accordion presented a setting in this order: a paragraph, then the buttons,
then the state right at the bottom in its hollow. That was right for an expanded
row: you had just asked it to open, so you had come to learn.

On a page everything is already open, and what the player comes to check is where
things stand. Measured on the device: the PPSSPP block opened on four full-width
lines of technical prose before the word "ready" appeared, and the block alone
filled the Thor's 1080 px.

The order is therefore reversed, and it fits in one sentence: the name and the
state in the header, what can be done next, the explanation last, and only while
it still teaches something. Once the folder is chosen the steps disappear; what
remains is the fact (which folder) and the caveat (leave the game before changing
mode).

Two consequences of form:

- An explanation of method is given in numbered steps, never in a paragraph. Four
  technical sentences in a row do not get read, they get skipped; the same facts
  numbered get scanned, and the format forces them to be written short.
- The state hollow disappears from the ordinary case. `DetailStatus` placed one
  hollow per block; the state now being said by the header badge, it was only one
  more container in a stack that already had three. The facts are `BlockFact`
  lines, label on the left, value on the right, and the hollow is reserved for
  what is genuinely an aside.

## Two columns, once the accordion is gone

The single column was the right answer while the rows expanded: a height that
changes reopens a hole between two columns at every gesture, and that is what the
section "One column, bounded and centred" below says. A page no longer changes
height.

What was left, then, was a column bounded to 620 dp in the middle of the 850 dp
the Thor offers in landscape: a quarter of the screen empty to the right of every
block, and three screens to scroll where one would do.

So two columns as soon as the width carries them (700 dp), one below that. Two
rules:

- A block belongs to a whole column, it is never cut down the middle: otherwise
  its state and its buttons end up either side of the gutter.
- The page is bounded to the width of the two columns, not of one. The trap, paid
  for once: the measurement deciding the column count happens inside the shell, so
  a shell already squeezing to 620 dp always makes it answer "one". What wants to
  stay narrow, the hub, bounds itself.

## A warning is not an error, and does not carry the red

The shell red appears twice in the whole app, and that is why it reads when it
appears. The PS2 folder card note carried it: six lines of red under a green
"Ready" badge, seen on the device, and you re-read the badge to work out which of
the two is lying.

But that note is not a failure: it is the reason none of the player's saves were
cloned, therefore the one thing they absolutely must read. Two objects, rather
than one:

- `BlockCaveat`, red and short: something failed.
- `BlockNotice`, a hollow with the warning bead and ordinary ink: something to
  know while everything is fine.

The hollow says "here is what is", the bead says how heavy, and the text does not
shout.

## The pages' images come from the device, not from a stock library

These pages talked about images without ever showing one. Four places where that
cost something, and four images that already existed:

- The game icons show a strip of five pieces of artwork from the player's own
  library, taken from the cache the app already warmed at startup. The block
  announced "Cocoon is in force" and you had to go and check in the grid; here you
  see what the grid shows, in the place where you change its source. It is also
  the only colour on this whole screen, and it comes from content, as the
  direction requires.
- The emulator blocks carry the installed application's icon. "PPSSPP" as a title
  does not say whether PPSSPP is there; its icon does, and the empty socket, when
  it is missing, says so just as well.
- About shows the seven consoles served, from the same files the library uses.
  The table mapping them was hoisted out of `LibraryScreen`: two copies would have
  diverged at the first addition.
- "What others see" shows the row other players receive, and without the photo.
  The first version showed the local avatar, which gave exactly the opposite of
  the sentence below it: the photo does not leave the device, others receive the
  initials and the colour. A preview that contradicts its caption is worse than no
  preview.

Nothing is downloaded, nothing is drawn for the occasion, and none of these
images is decorative: each answers the question its block asks.

## One column, bounded and centred

Two columns looked like the right answer to a wide screen, and are not: four
sections of different lengths never divide evenly, so one ends up shorter than the
other and leaves a three-hundred-pixel hole nothing fills. The problem is not the
pairing, it is structural, and an expanding row changes height, which reopens the
hole at every opening even when the balance was right at rest.

Bounded, because a single column stretched over 1920 px puts the label and its
value at opposite edges of the screen, and the pair stops being readable.
Centred, because a bounded block stuck to the left would leave the very emptiness
we just removed, simply moved.

## The three shape constants of a row

- The maximum width: beyond it, the label on the left and the value on the right
  end up at the extremities and the eye no longer pairs them.
- The corner radius, small: at 52 dp tall, a large radius gives a lozenge sitting
  in a card with far crisper corners. It is also the ring's radius, since the
  cursor traces the row's outline.
- The side inset: it is the width of a settings row, the separators draw it and
  the ring has to sit on it. One constant for both, or they diverge at the first
  adjustment.

## A row takes the whole card, edge to edge

No margin around it. Leaving even a few dp produced a white band between the
cursor and the edge, at which point the cursor surrounded nothing. In exchange,
every row takes the exact shape of the space it occupies, card corners included;
the ring's stroke is drawn inside its bounds, so clipping the card does not bite
into it.

Corollary, learned the hard way: no inset on the ring either. Shrinking it to
align the outline with the separators enclosed a box smaller than the row, and
the stroke ran through the middle of the label. The inset belongs to the text, so
it is applied lower down, inside.

The shape follows the space: a middle row is a plain rectangle, the end ones
inherit the card's corners. Open, it rounds everywhere: it detaches from the
stack, it becomes the header of what it has just revealed, and a crisp corner
there reads as a cut, at the top against the previous row as much as at the
bottom against its own detail. Both ends therefore follow the same rule and morph
progressively rather than snapping, or the shape jumps at the precise moment the
content unrolls.

The separator is drawn above, not below: an expanded row pushes its detail down,
and a line placed after it would end up separating the detail from the next row
instead of separating two rows.

## The opaque fill exists for the cursor, not for the look

The glow is a drop shadow of the control's outline, and a shadow cast by a
non-opaque layer is drawn through it: a focused row ended up filled with an
accent wash, bright at its rounded edges and hollow in the middle. Nothing clips
a shadow outside its own outline; the only thing that hides it is opaque content
on top.

A flat colour would have done that job and broken another: every row would freeze
the gradient at its own top, and a card of five rows would become five bands.
Slicing the card's gradient costs the same and does not show.

Hence the need to know which card the caller is drawing in, in root coordinates
and not a parent's: the things that need it are at different depths, a row being
a direct child of the card, a choice in an expanded detail three levels lower.

### It left settings, because the flaw was not confined there

Moved on 2026-08-28 from `SettingsPieces.kt` to `ui/components/CardSlice.kt`. It
was `internal` to the settings package, while the problem it solves, that a glow
is a shadow and a shadow goes through anything not opaque, arises anywhere a
transparent control carries the ring.

The first to need it elsewhere was `PadTextField`, which belongs to no screen:
its frame painted nothing, it let the card behind show through, and the cursor's
glow was therefore seen inside it. The field now paints the exact slice of the
gradient the card was already painting there, so nothing changes to the eye: the
fill exists for the cursor, not for the look, and that is still true outside
settings.

The order in the modifier chain is what holds the three layers together: the
shadow draws first, the opaque ground over it, and the ring's stroke over that
again.

## Two focus traps, and their workarounds

The "first row" flag is a flag, not a modifier passed in from outside. This row's
`Modifier` applies to the column containing the row and its detail, and a
`FocusRequester` placed there aims at a node that is not focusable: the request
fails silently. It is the clickable row that must carry it, and that row is
private.

Expanding does not bring the content on screen by itself. The cursor stays on the
row: nothing moves from focus's point of view, so automatic scrolling has no
reason to fire, and the content you just asked for opens below the fold. So it is
asked for explicitly, and on the whole column, row and detail, failing which only
the row is brought back, and it was already visible. And after the opening
animation, not during: the column is still measuring its previous height when the
state changes.

An expandable row's identity is an enum, not an index: the section order
rearranges between portrait and landscape, and an index would have opened the
wrong row on rotating the device.

A chevron that does not turn says "it is elsewhere". The theme opens its own
panel rather than expanding, a screen's way of saying the difference between "it
is below" and "it is elsewhere".

## The status lines, and what nobody would guess

The second screen: without its status line, the row is a promise the player
cannot check. They switch it on, nothing happens, and they cannot tell whether the
feature is broken or their device has only one screen. Naming the panel found, or
saying there is none, answers before they go looking.

Notifications: Emufii is sideloaded, so no push service can wake it. Outside the
app, Android decides when to let the watcher look, and that is a quarter of an
hour at best. Saying so here costs one sentence and buys the player's trust in
every alert that does arrive; leaving it out would make the feature look broken
the first time an alert is late.

The accessibility service is re-read while this screen is showing, not once:
going off to Android's settings is a journey outside the app, not a dialog with a
result, so the answer only exists on return. Polled, which costs less than a
lifecycle observer for one boolean, and it is what turns the row green under the
player's eyes.

## Autofill has its own row because Android can switch it off

An accessibility service is not a permission the app holds: it is a system
setting the player granted, and one the system withdraws by itself, on an update,
on a restore to a new device, from a battery optimiser.

The session screen used to carry the way back, as a button that only appeared
once the automation was already off, at the foot of a card nobody scrolls. Its
place is here: a switch you flip once in the app's life is plumbing, and plumbing
lives in settings. The row shows the state whether it is on or off, which makes it
findable before something goes wrong rather than after.

## What settings say about what Emufii does not do

The console keys row deliberately says what Emufii does not do, supply keys,
download any, send the file anywhere, because asking for a key file with no
explanation is how an app gets uninstalled.

The SteamGridDB key is shown in the clear and not masked: it is not a password,
it opens only a catalogue of public images, read only. Masking it would mostly
get in the way of spotting a typo, the one likely incident: a wrong key says
nothing, it simply brings nothing back.

The ROM folder and its rescan button were chips in the library dock, permanently
in front of somebody who had chosen their folder months earlier. Plumbing you set
once belongs in settings.

## The theme opens a panel, it does not expand

The app's appearance is not a settings-line detail. It was nine labelled choices
stacked in the card, pushing everything after it off screen, and asking the
reader to imagine what each name looked like. Its value always names both halves
of the choice, so the row says where things stand without being open.

## A button is named for what it does

Of two actions, only one is filled: the one that does the work. As pills of equal
weight they read as a choice between equals, which sent people back to the folder
picker they had just come through.

Once the work is done, the accent leaves rather than the label turning into a
boast. A button saying "profile installed" that nonetheless reruns the whole
preparation lies, and a button that does nothing is not a button. The work is
therefore demoted to an ordinary errand, doing it again, and it is the hollow
below that says it is installed. The section stops asking for something, which is
the change the eye looks for.

## Restoring hidden games is all or nothing

A list of hidden games would need their titles and their icons, read from files
this screen never scans: it would therefore show paths. And a player who has
hidden three regional copies of one game gains nothing from choosing between
three identical lines.

Bringing everything back costs one more deletion to redo, and it always works.

## A state badge carries two words, never a sentence

In a block header the title carries the weight and the badge does not, and in
Compose, inside a row, what has no weight is served first. A badge whose label was
a whole sentence ("SteamGridDB, for what Cocoon does not cover", 665 px) took the
whole line and left zero pixels to the title. The "Fallback images" card no longer
showed its name: just a sentence floating in the middle of nothing.

Two fixes, because one is not enough. The label went short again, and the
explanation went back into the block's note, which is its place. And the badge
gets a weight without a fill, which bounds it to half the row: a short label keeps
its natural width, an over-long one ellipsises instead of erasing the title.

The remedy is still to write short; the guard only stops the mistake being
invisible next time. Checked on 2026-08-29: no other badge in the app runs past
fifteen characters.

## The console tile has a short version

With no version number and a smaller icon, it goes from 124 to 92 dp tall and
from 118 to 92 dp minimum wide. Written for onboarding, where seven full tiles do
not fit on one line: they need 118 dp each for "GameCube" and a number to fit on
their own lines, and the Thor offers only 709 once the margins are paid.

What is removed is the version number. At install time the question asked is
"which machines do you play", not "which version of the emulator do I have"; that
last one comes later, on this page, where the full tile still carries it. The icon
and the emulator name stay: they already say whether it is installed, which is the
other half of the answer.

## Two columns, and it goes down, never sideways

The single column had been chosen because a settings page has no content to show,
only names to read. That was true of the row and false of the column: seven plates
stacked on a screen 1080 wide left two thirds of the width empty.

Two columns of equal width, filled row by row, and the overflow below. Sideways
overflow was tried first, two rows scrolling across, like the reference, and
refused: on a machine whose right thumb falls on a D-pad, you scroll a settings
page down, you do not push it sideways.

Nothing lazy in this hub, and that is the point: the seven cells are composed, so
focus traversal always finds its destination and brings the selected cell into
view itself. It is the exact opposite of the library, which holds its own cursor
because a `LazyVerticalGrid` does not compose the tile a direction aims at. Seven
cells are not worth that machinery.

## A console carries a row, not a tile

Three rebuilds in a day, and it is the question added, "with which build", that
settled the shape. The page was a grid of square tiles: good for "what plays
what", weak for "which one is on" (seven tiles with the same treatment, the state
carried by a difference in ink), and with no room for the build, which does not
fit in a square already full of four centred lines.

A console therefore carries a row: icon, name, build, switch, on a single reading
axis, and the build choice below it when there is one to make. Two columns as soon
as the screen carries them: seven rows in a line would waste half a landscape
screen and still overflow.

What the enclosing card carried has gone back where it costs no height: the count
at the end of the page title, the sentence right above the rows it governs. A card
that groups nothing against nothing is not a group, it is a frame.

## The ring and the pencil badge are siblings, not parent and child

One cursor stop on the avatar, not two: the photo and the pencil trigger the same
thing, and two focusable nodes for one gesture made two direction presses with
nothing moving on screen.

`Modifier.border` draws over the content of the node carrying it: while the
pencil badge was inside, the ring's stroke went through it, since it sits at the
corner of a square box, therefore straddling the circle. The ring is therefore on
the avatar's box alone, and the badge declared after it, so drawn on top. It is
the only thing that has to stand out of the circle, and it stands out whole.

Focus stays on the outer box, one stop, and the finger reaches the badge too, but
`controlRing` is silent there and serves only to bring the photo into view; it is
`focusRing` that draws, underneath.

## What sits at the end of a page title

For the state of a page with only one subject. Putting it in a block header would
force inventing a second title under the first, and two titles 40 dp apart say the
same thing twice.

## A block fact has no hollow around it

The hollow says "here is what is", and it deserved one when the state arrived at
the foot of an expanded row. In a block header the state is already said by the
badge, and one more hollow per block stacked three levels of container.

## Collapsing is reserved for what you set once

A service key, not a state you come to check. A collapsed block says only its
title and its badge; if what it hides is what the player came to read, collapsing
has only added a gesture.

## How many tiles per line, and the width that decides it

The count is measured on the width actually given to the grid, never on the
screen's. Onboarding gives it the entire width; the settings page is the same
screen but some 90 dp narrower once the card and its margins are paid, and a
count taken from the screen put seven tiles there anyway: "GameCube" came out as
"GameCu" and a version as "v2126.0-va". A tile that has to abbreviate its own
console has stopped doing its job.

The minimum width is the one where a tile still holds the longest console name
and the longest version, each on its own line. Below three columns the grid stops
being a grid, so that is the floor.

And we do not take the maximum that fits: that was the reflex, and it gives the
worst result of the lot, seven consoles in a card that carries six making six
tiles and then one alone on the next line. An orphan reads as an oversight, not
as a grid. The count chosen is the one that best fills the last row, widest to
narrowest: seven in six columns becomes four plus three.

A tile's height is fixed: the last row's sockets must be the same, and an
intrinsic height is not shared between siblings without measuring.

## An emulator's version is trimmed at display, not at the source

PPSSPP names its builds "v1.20.4", already carrying the letter the label adds, and
"vv1.20.4" is what came out on the Thor. So it is trimmed at display rather than
stripping the prefix from the string: the other five report a bare number, and a
column of versions with only one marked out reads worse than either.
