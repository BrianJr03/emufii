# The launch card and app navigation

The narrative that lived in `ui/components/GameLaunchDialog.kt` and
`ui/EmufiiApp.kt`, taken out of the code on 2026-08-24 (see
`docs/STYLE_COMMENTAIRES.md`). Headings are anchors cited from the code.

## The card replaced a bottom sheet, and for two reasons

The sheet showed a title and two bare buttons. It was wrong twice: it read as a
system menu bolted to the bottom edge, a rectangle anchored to a screen whose
whole direction is made of floating shapes with no framing, and it said nothing
about what the button was going to do, which for DS online play is a frankly
different thing from creating a session.

A floating card, then, carrying the artwork the player has just touched: the
object they chose is still the object in front of them.

It lies down when the screen does. Stacked, this card runs from floor to ceiling
on a handheld in landscape, measured from 100 to 970 px out of 1080 on the Thor,
while leaving some 470 dp of width empty on each side. Turning the stack into two
columns spends that width instead of rationing the height, which is what every
earlier "compact" concession paid.

It is bounded by the screen, never by a number: in landscape the device has about
415 dp of height, and a game whose title runs to two lines pushed the card below
the bottom edge. The buttons were still there, drawn off screen, and the last one
appeared as a bar with no text in it.

## The cursor has to enter the card, and not leave it again

The grid holds its own index and keeps focus: with no explicit request, the card
opened with none of its buttons reachable, and the directions went on moving the
selection behind it.

The request fails in touch mode, and that is correct: Compose only makes a
`clickable` focusable in keyboard mode. That distinction cost dearly, `adb input
tap` opening the card in touch mode, which produces exactly the symptoms of broken
focus.

But "a card opened by finger has no cursor" was the wrong conclusion drawn from
that: a handheld is touched and held, and the very first thing the player does
after tapping a tile is grab the stick. A plain `focusable()` node can take focus
in touch mode where a `clickable` cannot. The card therefore claims the keys in
both cases: the buttons take the cursor when the mode allows, otherwise the root
takes it showing nothing, and the first direction that arrives gives the cursor to
the primary button instead of moving the grid.

The request is retried a few times because the node does not exist at first
composition, then abandoned silently. And `getOrDefault` rather than `isSuccess`:
`requestFocus` returns `false` without throwing, so `runCatching` succeeds with
`false` and testing `isSuccess` reads as a win on the first go.

The cursor does not leave the card. It is a modal box: the grid is still there
behind, focusable, and an upward direction brought the selection back into it, the
box staying open over the top but with no key reaching it any more. `exit` refuses
traversal in every direction, where blocking one specific key would have covered
one edge only.

Not to be confused with `canFocus = false`, which on the contrary disables the
whole subtree, the mistake next door. The node swallowing presses so a tap in the
card does not reach the background therefore does not carry that property: it
swallows presses, and does not appear in the traversal because no direction stops
on it. The background likewise swallows presses without being a cursor stop:
without that the traversal stopped on it, on a node with no ring and no visible
effect.

B closes, and from the card, rather than relying on the `BackHandler` alone:
measured, the first press did not reach it, it only took the cursor off the
button, and a second was needed. Seen in preview, therefore before anything else
has a chance to use it.

The system gesture closes too, which a `Dialog` gave for free and a layer has to
ask for. Always active, including during startup: disabling it handed control back
to the screen below, which has nowhere to go from the library, so a B during the
launch closed the app. The card swallows the gesture and does nothing: the action
is already under way, and cancelling here would leave the caller half gone.

## The board darkens, it does not frost

Blurring the grid behind was the "glass" world's gesture. Here the card is a plate
lifted off the board, and what sets it there is the board darkening beneath, as a
console darkens its home screen when a title card comes up. The darkening also
keeps the artwork honest: blurring it turned six covers into one coloured smear.

## What gives way, and in what order

The explanations column gives way first, and it is the only one that gives way.
Unweighted, it was measured at the height it wanted, and the actions below were
laid out under the card's bottom edge and then clipped: the primary button simply
stopped existing on any card with long steps. Two columns bought room; they did
not buy infinite room, so the rule the stacked card already had still holds: the
explanation scrolls, the actions never.

The artwork is decoration: it gives way first. Smaller when the card is height
constrained, which it always is on a handheld in landscape. The steps are the part
that teaches, in public mode they are the whole instruction, and full-size artwork
pushed them out of the scrolling area, leaving a card that showed a title and
three buttons and explained nothing.

Both columns are vertically centred, and each for the opposite reason to the
other. The right column decides the row's height: on a talkative card, the PSP and
its instructions, the artwork and the title stayed stuck at the top with a large
void below. On a short card, the DS, three steps and one button, it was the right
that was shorter than the artwork and hung at the top. When both columns are the
same height, the ordinary case, the centring moves nothing: there is no rule to
add to tell the cases apart, the geometry does it.

## The buttons are stacked, and it is a trap avoided

Side by side was tried first and it is a trap: two pills sharing ~400 dp do not
hold these labels on one line, and the failure is silent, `Text` clipping by
default, so "Créer une session" was drawn "Créer une" with no ellipsis to admit
it. Widening one half only moves the clipping onto the other, and no split
survives a translation. The height those two buttons cost is the one thing the
two-column card has to spare.

## The choice of world comes first, not last

It was a text link under the buttons, and it did not work: a blue sentence placed
after two pills reads as a third action of the same family, while it triggers
nothing, it rewrites the card. On the PSP, the only console to offer it, you ended
up with three things to weigh under the steps.

At the top and as a selector, it says what it is: the question the steps below
already answer. A selector and not two buttons, because it is not an action:
nothing leaves when you touch it. Each half carries the ring on its side; with a
gamepad you cross a choice, not a block.

The PSP is the only console with a public side today: the PS2's was set aside on
2026-08-19 (`docs/PS2_ONLINE_MIS_DE_COTE.md`), so this branch no longer has two
cases to tell apart. And these are not the DS's steps: the DS dials a revival
server by itself, where the PSP player has two settings to choose in PPSSPP first.
Saying "we take care of it" here would be the lie the first scout already warned
against.

## "Private session" promises exactly what the coordinator delivers

The label says the session leaves the finder, and nothing more. Writing "nobody
can get in" would be false: the code protects entry to a private session exactly
as it does a public one, and anybody who believes otherwise will share their code
more lightly.

Public by default, because that is what keeps the finder alive: an app where every
game is invisible has no list left to show, and nobody finds anybody. The choice is
offered, not imposed.

The whole row is clickable: aiming at a switch with a thumb, on a card already
tight for height, is the kind of target you miss.

The switch is above the button, not among the scrolling steps: it is a decision you
take as you press, and a decision you cannot see is not offered. It is absent
everywhere no session is created, the DS dialling its server alone, the PSP's
public ad hoc being chosen in PPSSPP: hiding from the finder something that never
appears in it has nothing to offer.

## What replaces the buttons when a prerequisite is missing

A PS2 session with no network configuration on the memory card cannot be played,
whatever the tunnel does: the game's local menu never opens. The card is prepared
once, in Settings, and until the player has said it is in ARMSX2 there is nothing
worth starting here, so the actions are replaced by what to do, rather than left
to fail twenty minutes later.

A PSP session relies on the per-game INI Emufii writes to the memory stick:
without the folder granted once in Settings, PPSSPP never hears the session's
address and the game's ad hoc room stays empty. Same refusal, same place to fix
it. Public online mode is not blocked: it goes through PPSSPP's servers and needs
no permission.

These replacement screens state the prerequisite and where to set it, and nothing
else: there is no shortcut to Settings from here, because this card lives in the
library's tree and wiring a navigation through it for one message would cost more
than the sentence saves.

## The compatibility verdict, where the decision is made

The tile already carries the bead, but the tile is swept and this card is read:
here there is room to say what the mark means, and it is the last moment before a
player spends a session code and somebody else's evening on a game that does not
work. Nothing at all for a game nobody has rated, exactly as on the tile.

The bead is repeated rather than replaced by text alone, so the mark seen on the
tile is the same object here, and the sentence teaches what the colour means for
next time.

The tester's note is deliberately not shown. It was, for one version, and it read
as a second voice arguing with the verdict at the very place the player decides:
the verdict's four words are the whole message this card has to carry. The field
stays in the database and in the tool, for where there is room to argue.

## The button keeps its colour while it works

Material greys out a disabled button, which says "you cannot press this", but the
reason you cannot press it is that it is already working, and a grey button under
a spinner reads as a fault.

The delay before handing over is just long enough for the press to register on the
targeted button, and no longer. It was two full seconds, spent covering a wait the
player had no other sign of; the screen that follows now names its own progress, so
that padding has nothing left to hide and is only lateness.

The steps are numbered points rather than bullets: the three lines are a sequence,
and a bullet would not say so.

---

# App navigation

## One named destination per screen

`padEntry()` places the screen's `FocusRequester`, the one the shell aims at when
the cursor comes down from the header, and the one that sends it back to the
header when it goes up. There is one per screen, and that is the whole contract.

The session finder carried two: the search bar, and every session card. It had it
on the card first, back when the card was the first control; the bar arrived
afterwards, with its own, and nobody removed the other. A `FocusRequester` shared
between twelve nodes no longer designates anything, and the two symptoms are
exactly those of an ambiguous destination:

- coming down from the header skipped the search and landed on a card;
- going up from any card went straight to the back button, instead of moving to
  the card above, because every card also carried `padEntry`'s `onPreviewKeyEvent`,
  which sends "up" to the header.

The missing rule, and it holds for every screen: one `padEntry` per screen, on the
first control, never on a list item. A list traverses itself; what has to be named
is the front door.

## Search opens the app's keyboard

The finder's bar was a `PadTextField`, therefore a real input field, therefore the
system IME: it covers half the screen, it cannot be driven from a gamepad, and it
has nothing to do with the rest of the app.

The library has never had a field. It shows the query and places its own keyboard,
four rows, keys that are plates, and a panel that swallows presses beside it so a
miss does not open a game mid-word. The finder does the same now: a socket showing
the query, pressable, focusable, and not editable, which is precisely what stops
the IME opening.

Two ways to close it, and both are the library's: `B`, since it is a sub-level, and
a tap beside it. The tap goes through an invisible scrim over the whole screen,
declared before the panel so the keys stay above it. Without that scrim, `B` was
the only way out, and a tap on a card joined a session mid-word.

The magnifier was hoisted into `TrayIcons`. It lived in two copies, drawn to its
own proportions in the library's bar, and the finder would have made a third. A
glyph is the same everywhere it appears, or it is no longer the same glyph.

## An empty screen is centred on the screen, not under the header

The finder's empty state got the shell's top inset, the band the floating header
occupies, and nothing at the bottom. Centred in what is left under the header, its
middle fell some fifty dp below the middle of the screen. On a screen with nothing
else to look at, that offset is the only thing you look at. The same inset top and
bottom, and the block centres.

The socket carries its mark. It had been left bare on purpose: "nobody yet" is a
slot with no game, and the board already has a word for that, the hollow the grid
leaves in its last row. Two crescent-moon attempts had proved a borrowed metaphor
would not say it better.

Seen on the device, it does not read as a metaphor: it reads as an icon that
failed to load. An empty frame in the middle of an empty screen is a fault, not a
figure of speech. The hollow stays, being the right word for an absence, and it
gets the silhouette the app already draws for a player. The absence is said twice,
by the hole and by what is missing in it.

## What "back" means, screen by screen

This is the hole that closed the app. Nothing intercepted back above the library:
on friends, sessions, settings, code entry, DS or PSP online play, a press on B,
which the console delivers as a system back, bubbled up to the activity, which had
nothing to do but finish. Measured: from the Sessions screen, `BUTTON_B` handed
control back to the launcher.

Null on the library, which is the root: there, leaving is the right answer, and
holding the gesture would trap the player in the app.

Null too during preparation and in session, but back is consumed there all the
same, and that distinction is the whole point. A preparation has no stable state
to return to, the tunnel being half up; leaving a session means telling the
coordinator and cutting the tunnel, which the screen's "Leave" button already does.
Letting the gesture bubble up to the system, on the other hand, closed the app
mid-game and left behind a session nobody closes. Doing nothing is the only safe
behaviour at that moment.

## What the second screen receives

Published from the one place that knows, and derived from the current screen
rather than pushed on every call: a session ends in several ways, some of them
failures, and a panel updated by hand would end up showing the code of a session
that no longer exists. Deriving guarantees the panel cannot contradict the app,
and it costs one effect.

Published into a process-wide holder and not passed down the composition, so the
service host that will outlive this activity reads the same thing.

A friend event is put into a sentence for the rear panel: it has neither a name
column nor an avatar, it gets one line in the middle of something else, so the
sentence has to carry the name itself. These are deliberately the strings the
Android notification already uses: the same event must not read differently
depending on which surface caught it first.

## The logo, once per process and never on first launch

Not on first launch, because there is nothing to load then: the ROM folder has not
been chosen yet, and the home is onboarding. Making somebody wait in front of a
logo for a scan that will find nothing would be time stolen from the very first
contact with the app.

Once per process, and not once per composition: changing the language or the theme
recreates the activity, and a logo returning at every setting touched reads as a
crash. A `rememberSaveable` would not have been enough, saved state being restored
with the activity, so the logo would have come back. What we want to remember does
not belong to the screen but to the app's launch: it therefore lives where the app
lives.

For the same reason, the current screen survives the activity recreation a
language change causes: without that the player fell back onto the library right
after touching a setting.

## Refusals are said before the VPN prompt

The PS2 network profile also governs joining, not only hosting. The launch card
refused to open a session without it, but the finder, the friends list and a typed
code all bypassed that check, and the guest landed in a tunnel whose game never
opens its local menu. Same refusal, same words, said before the VPN prompt, and
before the tunnel slot is taken too, so a refusal never costs a running session.

Only decidable when the ROM is ours: a session for a game we do not have carries no
console, and that case has its own answer below.

Likewise, joining a session for another game opens a tunnel that can never carry
anything: the two emulators would never find each other. Said before the VPN
prompt rather than after a silent in-game failure. Only different titles are
caught: two regional dumps of the same game share a title id and are
indistinguishable here.

## Android's single VPN slot

Slot free, or already held by us: it goes straight away, asking every time would
put a dialog in front of an ordinary session start. Held by the other tunnel: we
wait for an answer, because taking it ends a running game.

Nothing here relies on the system's revocation: it works, but it is silent, and
the loser learns about it by having their descriptor taken away.

Waiting for the tunnel has an end: it returns the online state, or `null` if it
failed or took too long. Both happen in practice, another VPN app can preempt us,
and a handshake on a bad network simply does not complete. Previously, either left
the loading screen in place indefinitely. There is no address to wait for: the
coordinator assigns it before the tunnel starts, so it is known from the outset.

Tearing a tunnel down is local work, closing a descriptor, joining a thread, so it
is quick or it is stuck. Waiting longer would only delay the moment we tell the
user something is wrong.

No screen change during the preparation step, on purpose: the launch card is still
up and still spinning, so it carries that step itself. Switching to a full screen
just to show a second spinner made it look as though the card's animation had been
cut off. The tunnel step does get one, because it is the one that can really take
time.

The published port is the target emulator's, never a shared default: Dolphin
listens on 2626 where the others listen on 24872. It is what the coordinator
publishes, therefore what the guest will dial; a single default sent them to a
silent port, with a perfectly good address.

A first heartbeat before entering announces the arrival and brings back the token
that will allow withdrawing. Without it, leaving the session would rest on knowing
an id alone, which the coordinator no longer accepts.

Finally, the loop waiting for the host's address used `return@repeat`, which only
ends the current iteration: every join attempt therefore took the full ten seconds
even when the address was there on the first try.

## What is hoisted to app level, and why

- Presence ("my friends see me online"): silent during a session, where the member
  heartbeat already reports it and says which game is being played. Leaving a
  session lights it again, and its first call is what clears "in game" for
  everybody watching. Outside a session, the cadence tolerates a few failures
  before a friend sees us blink offline, the coordinator's presence entries lasting
  two minutes.
- Who is there, asked once for the whole app: taken out of the friends screen
  because presence is not that screen's private business, a friend's arrival being
  worth knowing from the library, and it is what feeds both the card sliding in
  here and the notification that goes out when nobody is watching.
- The watcher that continues with the app closed: resynchronised as soon as what
  it depends on moves, the two settings, and whether there is anybody to watch.
  Scheduling is idempotent, and an app with no friends and no version alert
  schedules nothing.
- Library maintenance: driven from settings but observed by the library, therefore
  owned here, the only place both screens hang from. The revision is what makes the
  grid rebuild: the library only ever reads the repository's shared cache, and
  incrementing that number tells it the cache has moved.
- The compatibility verdicts, read from the cache synchronously so the beads are
  there on the first frame: a warning arriving a second after the grid is drawn is
  a warning the player has already scrolled past. The network call only ever
  replaces it, and never with less than it had. The editorial catalogue follows the
  same pattern for the same reasons, but it is read by a panel page and not by
  every tile, so nothing waits for it.

## Two routes that are not sessions

The Kaeru route (DS) and the PSP public ad hoc carry a ROM and not a session:
nothing is created, nothing is joined, no other player is involved. The PSP has a
screen rather than a card because you leave it to go and configure PPSSPP and have
to find your place again on return.

## The opening screen holds the logo between two durations

Without it, the app opened on an empty grid topped by a loading spinner, the
library being walked at the very moment the home screen composes, and a reasonably
full ROM folder takes a few seconds to read. The app's first screen was therefore
its ugliest. Here the walk happens behind the logo, and the home only appears once
filled.

Two durations, and they pull in opposite directions:

- A minimum holds the logo even when the cache is already warm. An animation
  lasting three frames does not read as an opening but as a flicker, the same flaw
  already fixed on the preparation screen, except that here the screen is always
  crossed.
- A maximum makes it give way when the walk drags on. A first scan on a large SD
  card can take more than ten seconds, and holding the player in front of a logo
  that long would be worse than letting them watch the library fill: it has its own
  spinner for that.

Deliberately not focusable and with no control: nothing to aim at with a gamepad,
so nothing to signal. The cursor takes its place back on the grid.

## The logo is centred alone, and the status bar does not exist

Stacked in a centred column, it was the pair that centred: the bar pushed the logo
up by half of what it occupied. Measured on the Thor, the rings fell 30 px above
the middle of the screen. The bar is therefore positioned relative to the centre
without entering the logo's layout calculation.

The other half of the offset came from the image itself: the PNG carried 113 px of
emptiness on the left and none on the right, 92 at the top against 142 at the
bottom. Cropped to its content, which is what finally makes "centred" mean what you
read on screen.

The clock and the battery have no business on the first frame. The splash is a
full black held for a few seconds, and Android's status bar stood out on it as a
line of foreign text laid over the logo: it is the app's only second where you can
do nothing, therefore the only one where you really look at what is around. They
are hidden while the logo shows, and given back on leaving: the rest of the app
needs them, a player looking for a game wants to know how much battery they have
left.

## PSP online: two panes, and centred on the screen

This screen was the last one built in portrait: two full-width cards and two 56 dp
buttons stacked in a scrolling column, of which the Thor showed only a third, and
the instructions you come for were precisely in the invisible two thirds. On the
left the game and what this mode is, on the right what there is to do and the two
buttons that do it.

Centred on the screen, not under the header. Reserving the top inset centred the
card in what was left under the title, so 87 px too low. A height cap was tried so
it could be centred without risking going behind the header: it clipped the content
instead of compressing it, the left column not being scrollable, so what overflows
disappears. Removed.

What makes simple centring possible is that the card has slimmed down: 310 dp of
the device's 468, its top edge falling at 79 dp where the header stops at 68. The
`heightIn` that remains is only a screen stop, and it does not fire here.

## The preload runs, and the app composes behind it

The logo was a screen instead of the app: when it faded, the library only then
began to compose, measuring the grid, asking for its artwork, placing its cursor.
Hence the few hundredths of a second where the tiles were not there, despite
already-warm caches: what was missing was no longer the data, it was the rendering
work.

The logo is therefore a layer on top: everything you see next is composed, measured
and painted while it holds the screen. When it leaves, there is nothing left to do,
it uncovers an already-finished frame.

The budget granted to the warm-ups follows the same logic: the first four seconds
are free, that being the floor for which the logo stays anyway. The next two are
spent only by a cold start, folder indexes to build, artwork to decode for the
first time. The loading screen's cap stays above: a warm-up that drags gives up by
itself, it cannot hold the app back.

The "once per process" rule is gone. Android keeps the process alive for several
minutes after the player leaves, so reopening from the launcher skipped the logo
entirely and dropped straight onto the grid, read as a broken splash rather than as
a warm cache. The token is rearmed at every real start, except while a session
lives: coming back from the emulator must return you to the game's screen, not to a
logo.

## An in-flight attempt must not teleport somebody who has given up

The waiting screen had no way out: no button, no caption, no change of state. A
tunnel that does not come up or a VPS that does not answer left the player in front
of a spinning ring, with the Home key as their only exit.

They can give up, but giving up is not enough to stop a coroutine already under
way, which would complete later and teleport somebody who had gone quietly back to
their library into a session they left.

Every attempt therefore holds the number it carried at the start, and is only
allowed to open a session if it is still its own. Giving up increments the counter,
which is enough to orphan any in-flight attempt without having to know where it has
got to.

## The app's icons are drawn, not typed

They were text: `‹` for back, `✕` for removing a friend, an emoji in every empty
state. Three problems, and the third decided it: a character is positioned by its
font's metrics, so it never falls quite in the centre of the button carrying it; a
character inherits the system font of a device whose emoji set is not yours; and an
emoji is somebody else's illustration, at somebody else's weight, in the middle of
an entirely moulded world.

They are all built the same way: a 24-unit square, round caps, round joins, one
stroke weight. That is the whole icon system, and whatever gets added later is
drawn to the same three rules.

Two drawing consequences that keep coming back:

- A dot is a stroke with no length. A round cap renders it as a disc exactly at the
  icon's weight, so it stays the same drawing instead of being a filled shape
  smuggled into a system that has only strokes.
- No shape inside a shape. The warning triangle was removed: the badge is already a
  round bead with a white rim, an outline inside an outline reads as cramped, and it
  left the mark itself too small. Its two neighbours are single-stroke figures
  filling the bead, a tick and a cross, and this one is the third of that set.

A glyph is the same everywhere it appears, or it is no longer the same glyph: the
magnifier lived in two copies at different proportions, and the finder would have
made a third.

## The session finder polls, it does not listen

Sessions last an hour at most and the list is short: a socket would be a lot of
machinery for a screen you stay on for twenty seconds.

The coordinator knows only a title: it has neither artwork nor console to offer,
and it should not have any, these being ROMs, which live on the device. So the
announced title is matched against what we have locally, and when it lands the card
shows the game's real icon. Otherwise it shows the host: a session stays
identifiable by whoever opens it.

A card's facts are chips, not a sentence with mid-dots. A sentence has to be read
whole to extract one detail; chips are swept, which is what you do in front of a
list of sessions.

## One named destination per screen

The session card was the gamepad's named destination, back when it was the
screen's first control. Since a search bar precedes it, both carried it, and a
`FocusRequester` shared between twelve nodes no longer designates anything: the
cursor came down from the header onto a random card, skipping the search, and "up"
from any card went straight to the back button instead of moving to the card above.

## An empty screen is centred on the screen, not under the header

There was only the top inset, the header's band, so the block was centred in what
is left under the header, and its middle fell some fifty dp below the middle of the
screen. An empty screen has nothing else to look at: the offset shows.

The mark sits on a moulded object, the same as the header's round button: an empty
state is still part of the board, it is not a hole in it. Except when it is
precisely about an absence, and there it is a socket, but with its mark inside. The
socket was left bare at first, on the grounds that a slot with no game is already
what the grid draws; seen for real, it does not read as a metaphor, it reads as an
icon that failed to load.
