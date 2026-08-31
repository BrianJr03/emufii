# The screen shell: floating chrome, veil, and chips

The narrative that lived in `ui/components/EmufiiScaffold.kt`, taken out of the
code on 2026-08-24 (see `docs/STYLE_COMMENTAIRES.md`). Headings are anchors cited
from the code.

## Two jobs: the insets, and consistency

First the system insets: the content gets a top margin already clearing the
status bar, so nothing ends up under the clock, the flaw this shell was written
to fix. Then consistency: the same background, the same floating header, on every
screen.

## The header floats, and what that costs

It floats above the content instead of being a bar with a ground. A bounded top
bar was tried and rejected on this project: the piece has to look like a 3DS home
screen, where nothing is boxed in.

Floating has a cost the first version did not pay: the top margin clears the
header at rest, but a screen that scrolls sends its content straight under the
title, and the two draw over each other.

Hence a second copy of the wallpaper, drawn over the content and erased
everywhere except on the band the floating chrome occupies. Because it is the
same background at the same size, the pixels line up exactly with those beneath:
the content dissolves into the scenery instead of meeting a seam or a box. The
same device anchored to the bottom edge serves for the dock.

To be placed inside the Haze source where there is one: the dock samples the
scenery to blur it, and sampling the unveiled grid would blur tiles the veil has
already hidden.

The veil and the fade margin exist only for content that rises under the header.
A screen that does not scroll has nothing to dissolve, and the 32 dp reserved for
the fade become an empty band: of the Thor's 468 dp, that is 7% of the height paid
for nothing.

The fade distance is long enough to read as a dissolve rather than a hard edge,
short enough not to darken the first card of a screen at rest.

## The header is declared before the content, and drawn over it

Compose traversal follows declaration order. With the content declared first,
"down" from the back button had nothing after it. The order is therefore put back
the right way round, and the drawing does not change: the header floats above the
content scrolling beneath it, by its `zIndex`.

That is not enough to get the cursor down into the page, though, and it is worth
knowing before coming back to it: three attempts failed to cross the boundary
between those two layers of one `Box`:

1. `focusProperties { down = ... }` on a `focusGroup`;
2. an explicit focus request, which returns `Success(true)` while giving focus to
   the group itself, not to one of its children;
3. a `moveFocus(Down)` from the header.

Every time, `uiautomator dump` showed focus still in the header.

What works in this repository is the library's method: name the destination with
a `FocusRequester` placed on a genuinely focusable control, never on a container.
It is also why a screen's two gamepad destinations travel in a `CompositionLocal`
rather than in the content's signature: each screen has only one control to name,
and hoisting it into a parameter would have meant touching every call site for
information one place uses.

The key is consumed only if the destination exists: a screen whose first control
is conditional may have none, and swallowing it there would trap the cursor,
where giving it back leaves ordinary traversal its chance.

## The cursor arrives with the screen

Set on 2026-08-28, on an annoyance reported by the user: "90% of the time the
selector is nowhere, so you have to press at least once for it to appear". One
direction press in two was spent doing nothing.

`padEntry()` named the destination, which is where the cursor goes down from the
header and where it goes back up from, but nobody asked for it on opening.
`EmufiiScaffold` does now, and it covers every screen except the library, which is
not scaffolded and holds its own cursor.

Two traps, both found by measuring on the Thor, and the order matters:

- Keyboard mode has to be asked for before focus. Compose holds two input modes,
  and in `InputMode.Touch` no element retains focus: `requestFocus` there is a
  call that raises nothing and does nothing. A screen opened by finger left the
  machine in touch mode and every request fell into the void, while the same
  call, made from the header in answer to a key, worked, because a key switches
  Compose into keyboard mode by itself. It is also the answer to "the cursor is
  nowhere" when you enter a page by touching it.
- It has to be asked again over several frames, without checking whether it
  worked. A `LaunchedEffect` fires as composition ends, when the node carrying
  `padEntry` is composed but not yet placed; the request raises nothing and does
  nothing there either. A first version stopped at that silence, taking it for
  success, and the cursor stayed nowhere to be found, exactly the flaw it was
  meant to fix. Six frames, about a hundred milliseconds: asking again on a node
  that already has focus costs nothing, so there is no point testing, and the
  window is too short to snatch the cursor from somebody who had already pressed.

The loop is bounded because a screen is allowed to have no first control, and a
loop waiting for one would never stop. The `autoFocus` parameter exists so a
screen placing its own cursor can decline without taking the cursor's arrival away
from all the others; nobody uses it today.

No `padEntry` lands on a text field, checked across the seven screens that place
one, they are all buttons or clickable areas. That is the condition for the
cursor's arrival not to open a keyboard in the player's face, and it meets the
rule on a text field not being a cursor stop.

## The ring surrounds the chip, it does not bite into it

Placed on the chip itself, its stroke bit into the tinted ground and crushed the
label: it read as a badly sized border on the button rather than as a selection
laid over it. The header's round button never had that flaw because its halo
overflows its white ground; here the gap plays that role.

The gap exists permanently, focused or not: making it appear on selection would
shift the button by that much, and a row of chips would jump every time the cursor
passed. And it takes the shape declared just above: the cursor traces its outline,
it does not infer it.

## The chip is the size of its touch target

`Surface(onClick)` automatically reserves the 48 dp Material imposes on a touch
target, then draws its ground at the label's size, centred inside. The frame, and
therefore the ring, followed the reservation, not the chip: five pixels of white
between the stroke and the edge, measured at the top as at the bottom.

Giving that height to the chip makes the drawing and the target coincide: the
ring fits snugly, and the button becomes easier to touch with a finger into the
bargain.

## The label is centred both ways, and both are needed

`textAlign` alone handles the horizontal. It does not handle the vertical: when
the chip is stretched to line up with a two-line neighbour, a one-line label stays
stuck at the top of the height it has just been given. The `Box` is what puts it
back in the middle: `Surface` propagates its minimum constraints to its content,
so the `Box` does fill the whole chip, stretched or not.

No `fillMaxWidth` here. There was one, and it broke every row of two unweighted
chips: the first took the whole width, the second fell to zero and folded its
label onto as many lines as it has letters, and the settings Library card measured
390 dp tall for three lines of text.

Without it the `Box` fits its content; and when the caller stretches the chip (a
weight, a `fillMaxWidth`), `Surface` propagates its minimum constraints and the
`Box` fills anyway. The centring holds in both cases, which is all it was ever
asked to do.

For the same reason, "this chip is alone and takes its card's width" is explicit
and not inferred from a `fillMaxWidth` placed by the caller: since the ring
surrounds the chip, it is the frame that receives the caller's modifier, and
letting the chip stretch by itself would replay the flaw above.

## The group title speaks the app's voice

It was a micro-label in spaced capitals, the "eyebrow" every dashboard ships, and
the one device the guidelines ban outright: a line of small capitals above a title
is a costume of importance, and it makes the app read as a settings screen from
somewhere else.

Sentence case at body weight says the same thing, in the voice the rest of the app
speaks, and stops competing with the content it introduces.

## The round button is a moulded disc

Round, moulded, floating on the board: the button a console puts in the corner of
its screen, a plastic disc with a lit top edge, with a drawn glyph inside, never a
typed character.

---

# Typing text with a gamepad

Taken out of `ui/components/PadTextField.kt` and `ui/screens/JoinScreen.kt`.

## A text field must not be a cursor stop

The flaw is Compose's, not ours: an `OutlinedTextField` that takes focus opens the
soft keyboard. With a gamepad, where focus moves by crossing the screen, merely
passing over a field was enough to make the keyboard appear, cover the page and
capture the directions: you no longer crossed a settings screen, you fell into it.

Here the field is not a step in the traversal: its frame is. The frame announces
itself with the usual ring, and A, or a finger, enters the field. B leaves it and
gives focus back to the frame, so you pick up where you were rather than falling
back to the start of the screen.

`canFocus` is denied to the field outside editing, and that is what really keeps
it out of the traversal: simply making it non-clickable would have left it
catching focus from a direction.

## It is the keyboard disappearing that ends editing, not the key

The keyboard swallows the first B and the back handler never sees it, measured on
the Thor: one press closed the keyboard leaving the field open and with no ring,
and a second was needed to get out.

So it is the keyboard's disappearance that ends editing. The back handler stays
for the case where there is no keyboard (a gamepad with a physical keyboard, a
hidden IME). And an "already open" flag is needed because the keyboard is not yet
visible at the instant you enter the field: without it, editing would close as
soon as it opened.

## The ring is the field's outline, and it is the only arrangement that holds

Two attempts failed on the Thor, and measurement is what settled it.

Drawing the ring on the same bounds put its stroke over the field's own outline:
two slightly offset lines. Insetting the field and widening the ring's radius to
make them concentric did not work either: measured at 4x magnification, the gap
was 4 dp on the sides and 11 dp at the top, because `OutlinedTextField` does not
fill the frame it is given. No radius makes two curves parallel when the space
between them is not even to start with.

So: the field's border becomes transparent under the cursor and the ring takes its
place, on the field's exact bounds and shape. One outline at a time, and there is
nothing left to align.

And before the `focusable`, order being everything: the ring reads focus through
`onFocusEvent`, which only sees nodes below it in the chain. Placed after, it
never saw the frame's focus and stayed dark while the cursor was very much there,
the field scrolling to the middle of the screen showing nothing, which reads as a
vanished cursor.

### The label sits above the frame, never inside it

`OutlinedTextField` reserves space at the top where its label will float, even
while it is still at rest: the text sits noticeably below the middle, plenty of
air above, little below. In a frame whose ring is the outline, that asymmetry
reads as a badly sized ring, which is what was reported on the profile nickname.

The reservation served no purpose here anyway: a floating label sits in the notch
of Material's outline, an outline this field erases in favour of the ring. It
would therefore have gone off to float on the ring itself at the first character
typed.

`PadTextField` therefore renders the label itself, above the frame, and passes
`label = null` to Material. The text finds its centre again, and the label stays
readable permanently, including once the field is filled, where the floating
version would have got lost in the stroke.

### All four outlines go dark, not three

The field switches off its own outline when the frame carries the cursor, so there
are never two at once. Three colours did that, `unfocused`, `disabled`, `focused`,
and the fourth was missing: `errorBorderColor`, which Material puts in front of
the others as soon as `isError` is true.

A field in error therefore kept its red stroke under the ring: two outlines of
different sizes inside each other, which reads as a badly sized ring. It showed
every time the profile opened, where the nickname is empty and therefore in error
from arrival, and the cursor's automatic arrival made it permanently visible.

`framed` is false while editing, the cursor then being in the field and not on its
frame: the red therefore comes back exactly when the ring goes out, a rule the
other three already followed.

## The finger could not reach the frame

Reported on the home screen, true everywhere this field is used.

Detection was on the frame, under the field. But Compose tests children first, and
`BasicTextField` installs its own pointer handler to place the insertion caret: it
consumed the tap, then asked for a focus `canFocus = false` refused. The gesture
therefore vanished between the two, moving nothing on screen.

The detection surface is therefore drawn after the field, and so touched before
it, and it exists only outside editing: once inside, the field has to get the taps
back to place its caret.

## Six slots rather than a field

The code entry screen was a full-width `OutlinedTextField` with its label and its
helper text, centred in a column, a form where there is only one thing to type,
and whose field took the screen's 784 dp for six characters.

Six slots instead. We know in advance how many are needed, so show them: progress
is seen without reading, the current slot carries the accent, and the code is
displayed at the size you read it at arm's length. The input field still exists,
invisible, under the slots: it is what brings the keyboard, selection and pasting
without our having to rewrite them.

Each slot is a hollow rather than a plate: a code is typed into something. The lit
one carries the cursor's ring, the same object as the tiles, so "where am I" has
one answer everywhere in the app.

No autocorrect, and it is not a spelling matter: it is what stops the keyboard
opening a composition region on the field. The field's caret and text both being
transparent, the pale block sitting in the lit hollow was the keyboard's
composition highlight, drawn on a code that has nothing to correct.

## The keyboard's key closes the keyboard, and stops there

`ImeAction` only draws the key; with no action to answer it, pressing it did
nothing at all, the IME staying over the screen and the back button being the only
way out, on a screen whose whole job is to take six characters.

What it must certainly not do is start the session: the screen already has a
button for that, and a keyboard key launching from the last character takes the
decision out of the player's hands, without their being able to re-read the code.

Closing counts for the button too: the code can be complete while the IME is still
up, and a session would then start under a keyboard nobody closed.

## No automatic keyboard, and the block is centred on the screen

In landscape on this machine the IME opens full screen (extract mode) and covers
everything: you arrived at a bare text editor, never having seen the six slots nor
the game's name. The keyboard comes when you touch the slots, which is to say once
you have decided to type. The full screen itself is not ours, the IME deciding that
on a short screen, but being subjected to it without having seen the screen is.

The block is centred on the screen, not under the header: reserving the top margin
centred it in what was left below the title, some 90 px too low. Nothing here
reaches the header, the block being 212 dp of the device's 468, so there is no room
to reserve for it.

## The app's keyboard is an engraved slab, not a board of buttons

The system IME is made for a phone held upright: on a handheld in landscape its
full-screen mode takes the whole panel, and the library you are looking for
disappears exactly when you need to see it. This one never rises above half the
screen.

Each key started as a hollow of its own: its outline, its rounded corner, seven
points of gap from its neighbour. Thirty-eight objects placed side by side, so
thirty-eight outlines for the eye to follow, and one object was distinguished from
the next only by the groove separating them, the same groove everywhere. The panel
read as a heap.

There is only one piece now. The keys are cells cut into it, separated by a
one-point engraving, a dark stroke then a light one, exactly what moulded plastic
already does everywhere else. Nothing floats, nothing has a corner of its own, and
the panel's outline is the only outline of the whole. It is the screen printing of
a console shell, which is the object this keyboard has been imitating all along.

What lights a cell is state, never shape again: the cursor on it, the finger
pressing, or shift locked. A cell at rest has no drawing of its own, so a lit cell
is the only thing the eye finds.

Shift locks like a caps lock instead of firing once: a shift that undoes itself
after one letter is behaviour nobody can predict without watching it. Search
ignores case anyway, so the key is there for the player to watch themselves type,
not to help them find anything.

## The slab holds its own cursor

First attempt: make every key focusable and let two-dimensional traversal do the
rest. It does not. The rows are separate `Row`s, and a `focusGroup` changed nothing
there, checked twice on the Thor: "A" then up left the keyboard instead of moving
onto "Q". The move failed, the key bubbled up unconsumed, and the escape hatch
meant for the first row applied to all of them.

It is exactly the library's lesson, written in black and white in `CLAUDE.md`: on
a grid, you do not hand navigation to focus traversal. One focusable node, the
slab, an index (row, column) it computes itself, and keys that are told whether
they carry the cursor.

Three consequences, all intended:

- The keys are no longer focusable. Thirty-eight invisible stops fewer, and no
  way left for the cursor to get lost between two.
- The edges give back control. Up from the first row, down from the last: the key
  is not consumed and the screen hosting the slab does what it likes with it. A
  slab swallowing everything would be a trap.
- The column remembers. Changing row keeps the column index, brought back within
  the arriving row's bounds; without it, moving from the row of ten letters to
  the row of four service keys sent the cursor to the edge on every round trip.

## An expanded row is made of three things, and nothing else

Closed, the settings list is a stack of moulded plates and reads at arm's length.
Open, every section had picked up habits: a paragraph, then two buttons of equal
weight, then three or four sentences in four different colours saying what had
happened. Every line was defensible and the result was a wall: the PS2 profile
ended up with eleven stacked texts, the most important one last.

1. The note, at most one paragraph, and only while it still teaches something.
   Once the thing is done, the explanation gives way to the state.
2. The actions, the first filled, the others as ghosts. Two pills side by side at
   equal weight said "these are two things of the same kind", which was never
   true.
3. The state, what the app knows, in a hollow: a moulded bead, a sentence, and the
   facts in aligned rows rather than as prose with mid-dots.

The hollow is the board's vocabulary, the same hole the grid uses for an empty
socket, so a settings screen made of plates has one kind of hollow only, and it
means "here is what is, not what you can do".

## Join: the app's keyboard rather than an invisible field

There were six notches, a caret, a small example, and beneath it all an invisible
input field waiting for the system keyboard. On a gamepad console that keyboard
never opened: nothing on screen said how to produce a character, and it was the
app's only screen to ask for one. The invisible field brought pasting and
selection, two gestures that do not exist without a touchscreen or a pointer, at
the cost of the one gesture that matters here.

It is replaced by the search slab. The code is therefore typed the same way you
look for a game, with the same keys and the same cursor, and the screen has
nothing special about it any more.

In two columns, because the machine lies on its side. Stacked, the keyboard would
have pushed the notches under the header. On the left what you read, the game, the
template, the six slots, the action; on the right what you write with. It is the
order of the right hand on a console held two-handed.

## The code keyboard is not the search keyboard

The "Join by code" screen asked for six characters from a gamepad and said nowhere
how to produce them. The answer was already in the repository: the same slab, set
up differently.

Three differences, and one reason for each: a code is not a sentence. No case, so
no shift to lock; no words, so no space; no letters/digits toggle, because a code
mixes the two and fetching them across two pages would double the number of
gestures. The whole alphabet and the ten digits fit in four rows, which is exactly
the height of the other one.

The alphabet, not AZERTY. You do not type a code from muscle memory but character
by character, reading it off a screen or out of a message. On a typewriter layout
you have to hunt every letter; on the alphabet you know where it is before looking
for it.

## A slab cell draws nothing at rest

Only three states light it, and they stack cleanly because they are in order of
certainty: shift locked (a lasting state, the most discreet), the cursor (where
you are), the press (what you are doing). The strongest wins. The outline appears
only with the cursor: that is what has to be found at a glance among thirty-eight
cells, a press being already visible under the finger.

The cursor had been forgotten. A key was `clickable`, therefore focusable,
therefore a cursor stop, and nothing showed it. On an app driven by gamepad, the
keyboard was the one place you typed blind. The `focusable` is now explicit, with
the same `interactionSource` as the click.

Corollary paid for dearly by the library: clickable by finger, never focusable.
`clickable` makes things focusable by default, which would leave as many invisible
stops as there are keys, competing with the cursor the slab holds. The touchscreen
keeps its path, the gamepad has its own, and both point at the same cell.

And the slab shows its cursor only while it has it: the lit cell is designated by
an index, not by focus, so it stayed lit after up had handed control back to the
search field. The index remembers, which is what you want on return, but it only
draws when the slab has focus.

The engraving separating two cells is a dark stroke then a light one a point below
it: the same pair as the plates' bevel, at the scale of a groove. A single grey
stroke would have made a table; the pair makes a groove.
