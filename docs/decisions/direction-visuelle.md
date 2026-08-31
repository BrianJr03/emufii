# Visual direction: the "HOME MENU" contract, the palette, the accents

Taken out of `ui/theme/` on 2026-08-24 (see `docs/STYLE_COMMENTAIRES.md`). The
full system is in `DESIGN.md`; this file carries the contract and the measured
reasons behind every colour. Headings are anchors cited from the code.

## The direction contract, "HOME MENU"

Set on 2026-08-22, pinned brief. This text was the entire body of
`ui/theme/Direction.kt`; the Kotlin object now serves only as a naming anchor.

Thesis. Emufii is a handheld console's home menu, not an application with a list
of games. It refuses the translucent glass dashboard it used to be, blurred
panels, iOS blue, drifting gradients, because glass is what a phone app wears,
and this thing carries itself like a console.

Its own world. Moulded plastic under the light of a board. A cold silver floor
carrying a fine engraved grid; white plastic plates with a hairline edge, a lit
top bevel and a real offset drop shadow; a typeface with rounded terminals
(Rounded M+); one signature colour, the board's cyan, spent only on the cursor
and the primary action. Colour otherwise comes from the artwork. The dark is the
same board at night, never an inverse.

Story. The player sees their games as objects on a board, finds the cursor
without looking for it, presses A, and is away in the emulator.

First screen. The status band at the top like a console's inset screen (profile,
tunnel, consoles), then the board of square plates filling the rest, the cursor
lit on the first game.

Form. World pinned by the user (the draw is beaten by a pinned brief); Operate
mode.

Finish. Unreviewed and undocumented counts as unfinished; this build ends with
the finish review, the verdict, and `DESIGN.md`.

## One language of corners

The radius of a moulded corner, at four sizes. A sampled superellipse was tried
and removed: at tile size, 128 segments left a visible facet in every corner,
which read as dirty rather than soft. Simple radii, then, generous and constant,
which is what injected plastic actually gives.

Slightly tighter than the "glass" world's values: plastic has an edge, and a
28 dp radius on a panel makes it a shapeless lozenge rather than a plate.

The panel radius is named once (`CardCorner`). The rows at either end of a
settings card inherit the card's corner, since the cursor traces the outline of
the space a row actually occupies. That number lived a second time over there,
and when the plastic world took the panel from 28 dp to 22, the copy stayed
behind: the ring rounded wider than the card it sat in, and overshot its corner
on the first and last row of every block.

## Three floors, one accent, and nothing else has a hue

A handheld's menu is a board of coloured objects on a neutral shell: every colour
that is not the cyan is a grey with a few degrees of blue in it, because the
artwork must be the only thing that shouts.

- Day is a cold silver, not a white: the plates are white, and a white shell
  would leave them nothing to sit on. Sampled dark enough that a plate's hairline
  is readable without drawing an outline a second time.
- Night is the same board under a lamp: blued black, never neutral grey.
- OLED is exactly off. A black pixel is a pixel not lit, and `0xFF050505` is lit.

The moulded edge is a hairline one tone darker than the plate in light, one tone
lighter in dark. It is what makes a plate an object rather than a fill, and it is
the only separator left in OLED, where a shadow draws nothing. Deepened on
2026-08-22: at `0x1F` the outline disappeared at glancing distance and a white
plate read as a flat card carried by its shadow alone. A moulding has an edge you
see without looking for it.

## The cyan is spent on the cursor, and on nothing else

The cursor, the primary action, the current selection. Nothing else. Spending it
on decoration is what would make "where am I?" a question again, and on a
handheld the cursor is permanently somewhere.

There are two cyans, and it is measured. The light cyan is a light: at 4.7:1
against black it is perfect for a cursor glowing on a dark board, and hopeless as
a background for white text, 2.2:1, which is illegible and not up for debate. The
light theme therefore fills its primary button with a deeper cut (4.6:1 under
white) and keeps the bright one for the cursor. The dark theme does not have that
problem: the bright cyan carries deep ink there, at 8:1.

The shell red is borrowed from the console this world comes from: errors and
destructive confirmations only. It appears perhaps twice in the whole app, and
that is why it reads when it appears.

The green is an inherited alias: the green ring is gone, the cursor being cyan,
one accent for one meaning, but several screens still name that colour for a
"ready" state.

## One accent, but always in three cuts

A single hue cannot do the accent's three jobs: the bright one must be seen at a
glance on a black board, the deep one must carry white text, and the ink must be
readable on the bright one. The shipped cyan was already built that way, the deep
cut existing because white on the light cyan is 2.2:1, so making the accent a
choice means carrying all three cuts for every colour, not swapping a hex value.

Secondary badges never take a flat fill: they take the accent at a fifth.

The fixed colours were not chosen by eye: every deep cut is its base darkened in
HSL until it passes 4.6:1 under white, and every ink until it passes 5:1 on its
own base, exactly the ratios the shipped cyan measures, so a chosen colour is as
readable as the one it replaces. Darkening by lightness rather than scaling the
channels is what keeps the hue: scaling empties a light colour to near black
before it even reaches the ratio.

Green is deliberately absent: it falls on the "connected" green, and an accent
that reads as a reserved meaning takes that meaning away from both.

Red was absent for the same reason and is offered anyway, by the user's decision
(2026-08-23). What that costs has to be known: the cursor then carries almost
exactly the colour the shell spends on errors and destructive confirmations, so
on that accent "this is where you are" and "this will delete something" stop
being distinguished by hue alone. The red kept here is one hue cooler than the
shell red, to keep a little daylight between them.

The white's ink is the app's dark ink, not the grey where the ratio rule would
stop. The rule sets a floor, not a target: a mid grey on white passes 5:1 and
still reads as a disabled label, where the ink used everywhere else passes 14:1
and reads as writing.

## The system accent is taken from the platform, not derived

It is taken from Android's two schemes rather than recomputed here, because they
already carry the contrast guarantees wanted: the dark scheme's `primary` is a
light tone, made to be read on a dark background, the cursor's job; the light
scheme's `primary` is a dark tone, made to carry white, the filled action's job;
and the dark scheme's `onPrimary` is, by construction, what is readable on the
tone taken for the bright cut.

Below Android 12 there is no extracted colour, and the board's cyan takes over.
The setting stays on show: it is not a lie, it says "follow the system", and on a
phone with nothing to follow, that is the app's colour.

## The only two places that read the accent by hand

The cursor ring, drawn by hand, and the filled action, which needs the deep cut
Material has no slot for. Everything else goes through
`MaterialTheme.colorScheme.primary`, which the theme fills from the same source,
so a new screen receives the chosen accent without knowing this exists.

---

# Moulded plastic, and the top bar's chips

Taken out of `ui/theme/Plastic.kt` and `ui/components/ProfileChip.kt`.

## A moulded plate is four things, in this order

The order is the whole knack:

1. a shadow with a real vertical offset: the light comes from above the board, so
   a plate casts below itself. A halo with no offset is decoration, not depth,
   and that is what the "glass" world drew;
2. a fill lighter at the top than at the bottom, which is what a curved plastic
   face does under this light;
3. a hairline edge, the moulding's own outline;
4. a lit bevel a hair inside, along the top.

Remove one and the plate flattens into a coloured rectangle.

The gradient is barely a gradient on the light theme: white plastic under diffuse
light has a very short falloff, and more reads as grey grime.

The relief says how high the plate is set, 0 dp for flat, 10 dp for a tile the
cursor has lifted, and the shadow's offset follows it: a plate that rises without
its shadow moving reads as a magnification, not as an elevation.

Pressed, the plate loses its relief and its lit edge and takes on a tint of the
board's shadow: the three parts that made it stand proud, removed, which is what
"pushed in" is made of. A moulded button that never travels is a picture of a
button.

## The bevel does only the top third, and its depth is fixed

A highlight that goes all the way round is a stroke, and the stroke is already
the outline. It is drawn over the content, on purpose: it is a hairline, and it
belongs to the surface, not to what is beneath.

A fixed depth, never a fraction of the height. Proportional, a large settings
panel saw half its face washed out: the bevel is a property of the edge, and an
edge does not thicken because the object grows.

The plate's two colours are exposed because a caller sometimes has to build the
gradient itself: a settings row fills with the slice of its card's face that
belongs to it, which is the same colours over shifted bounds. Taking the list
rather than copying the values is what stops the two diverging.

On a tile whose artwork covers the whole face, the outline is drawn after the
content, without which it would pass underneath, so a photographic image keeps a
moulded edge catching the board's light.

## The board is engraved, and a hollow lights the other way round

A console menu's floor is never a flat fill: it carries a fine repeated texture
giving the eye a scale reference, so the plates read as objects of a certain size
rather than as shapes on a plane. Two families of hairlines, one dark one light,
at a millimetre: this is an engraving, not a chequerboard.

An empty slot is a hollow, not a plate: it lights the inverse of everything else,
dark at the top where a plate is lit, and that is the whole reason a hole reads as
a hole.

## The top bar's chips are one family

All three, profile, friends, sessions, have the same shape, the same size and the
same relief, because all three are navigation. The sessions button used to be a
solid blue pill floating alone at the bottom of the screen: it did say it led
somewhere, but in a language nothing else spoke, and it scrolled over the
artwork.

Friends were reachable only through the profile page, two taps further and filed
under settings, the wrong shelf: seeing who is online and joining them is
something you do instead of browsing the library, not a preference you adjust.

The profile now carries the avatar alone. It used to carry an invitation to
choose a nickname while the profile had none, which made the chip change width
with state and put a permanent chore on the home screen for something a session
gets along fine without.

## The dark fill is lighter than it should be, and it is measured

The previous value was a hair from the wallpaper's colour, and a shadow does
nothing on a dark background: the chip was invisible. It went unseen while the
profile avatar was the only occupant of one, a light disc filling the chip not
needing a chip. It is glaring as soon as there is a glyph.

## No Material indication: a press animation

`Surface(onClick)` brings a ripple whose state layer also covers focus, and
Android gives focus to the first focusable view as soon as a keyboard or a
gamepad is plugged in, always, on a machine like the Thor.

The result was a flat 10% wash placed permanently over that chip, which reads as
"disabled" rather than "selected". The profile had it too, hidden all along under
its avatar.

Growing on press is the feedback the dock and the tiles already use, and the
chips stay focusable: the D-pad still reaches them.

## The glyphs say "other players" the way the rest of the app does

The friends chip started as a 👥 emoji, which arrived with the system font's
palette next to a chip whose only other occupant is a photo or two initials in
the app's colours, half a two-button bar coming from somewhere else, and redrawn
by every Android version into the bargain. Redrawing it by hand fixed the palette
and kept the problem: a small figure of a person is still a symbol stuck on a bar
that contains no others.

This app already says "the other players" in a precise way: overlapping discs
with a ring cut out between them, on the session screen and the presence card.
Saying it again here costs nothing and makes the pair read as one family of
shapes: you on the right, the others on the left.

The two discs are white on purpose. Filling them with the first two friends was
considered and dropped: the state everybody sees on a fresh install is the empty
one, so the icon would be that one most of the time anyway, with a branch to
maintain and an appearance that changes under the user. The ring is the chip's
fill rather than plain white, so the gap between the discs stays a gap in both
themes.

They are placed by offset from the centre, not by corner alignment: aligned top
right and bottom left they only touched diagonally, two discs grazing each other,
the pair off centre in a round chip. The overlap is the whole point of the shape:
it is what makes two circles read as two people rather than as a diagram.

For sessions it is two linked screens, not two people: a session is two consoles
talking to each other. The distinction is carried by the shape, discs against
rectangles, not by a decorative detail.

And the front glyph takes the accent in force. It said "the app's accent" and was
a hard-coded iOS blue, a leftover of the "glass" world that had never followed
the cyan either, and therefore the last thing on the board whose colour answered
to nothing.
