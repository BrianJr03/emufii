# Second screen: why the panel is built this way

The narrative that lived in `secondscreen/SecondScreenContent.kt`, taken out of
the code on 2026-08-24 (see `docs/STYLE_COMMENTAIRES.md`). The headings serve as
anchors: the code cites them, so they are not renamed lightly.

The reminder that outranks everything else: the single screen stays the main
layout. The panel completes, it never delegates; a player with no second screen
loses not a word.

## The panel has no style of its own

The same board as the main screen, never a second style: engraved floor, moulded
plates, one accent, as the direction contract freezes them
(`ui/theme/Direction.kt`). A panel with its own look would read as another
application running on the back of the machine.

It is read at arm's length, off axis, under the player's hands. So one object
leads each face and everything else labels it; the panel never holds more than a
glance picks up.

It had neither cursor nor control: it reported, and nothing else. That changed on
2026-08-25, and only in session, see "The panel takes the steps, because it is
touch". Everywhere else the rule holds.

Colour follows the product and not the chrome: the artwork is the only thing
allowed to be loud on the hover face, and it even lends its extracted hue to the
shadow it casts.

## The panel takes the steps, because it is touch

The panel is touch, and the system says so: the Thor's rear screen declares itself
`touch EXTERNAL`. It was not for Emufii, because its window carried
`FLAG_NOT_TOUCHABLE` and `FLAG_NOT_FOCUSABLE`, set so it never steals a press
meant for the game, which made it a display and nothing else.

Both flags go. The Thor arbitrates focus between its two screens: the window stack
carries focus per screen, checked on the device, the presentation having its own
on screen 4, the activity keeping its own on screen 0. The press meant for the
game is therefore not lost. `FLAG_NOT_TOUCH_MODAL` stays: what is pressed beside
the window still goes to whatever is behind.

What that changes in session: both steps go down to the back, under the thumbs
when the machine is held two-handed, and the front screen gives back the 130 dp
they took to the explanation, which needed it, the PS2 card alone running to more
than a screen. As soon as the panel goes out, or there is none, they come back up
front: the single screen stays the main layout, and a player with no second screen
loses no button.

Three precautions:

- The labels travel already translated. The panel's window has its own display
  context, which has already made the app speak French inside an English
  interface once.
- The actions are removed on leaving. They are one composition's lambdas: the
  screen that places them clears them at its death, or the panel keeps a dead
  session under the finger.
- The launch button's label and state have a single definition. Two screens now
  draw them; the day the two diverge, half the players press a button that says
  something other than what it does.

The panel is wide and short, 537 dp by 320 usable, and the face lives in a centred
box that does not scroll: what overflows is clipped at both ends, without a word.
Two layouts were tried and measured on the device before the right one.

1. Stacked. Chip, code, facts, title, then two controls one above the other: some
   380 dp asked for 320 available. An Eden session showed only one button out of
   two, and nothing said why.
2. Two columns, identity on the left and controls on the right, to win back
   height. Worse: each column fell to 268 dp, the code broke into "NRX-" and
   "572", and the port was written one digit per line. A code cut in two is no
   longer a code, it is two pieces to glue back together aloud.

What works: one column, and what repeats goes side by side. The code takes the
whole width on one line (`softWrap = false`, never negotiable), the console and
the game title share a line of labels, the two reference hollows share another,
and the two controls share a fixed-height row, 64 dp each, the same, so a
two-line label does not make its plate grow next to its neighbour.

The lesson to keep: this box clips silently. Any face that gains an object is
re-measured against those 320 dp. Capturing the panel is possible but its id is
not the logical `displayId`: `screencap -d` wants the physical id, the one
`dumpsys SurfaceFlinger --display-id` gives. Without that you code by guesswork,
and those two failed layouts shipped without anybody seeing them.

### The panel's cursor hangs on two things, and a badly placed probe sent me looking elsewhere

Reported on 2026-08-28: "I have to press down twice to reach the button on the
bottom screen, when once is enough to come back up."

Two causes, independent, and both were needed for the count to come out right.

On opening, nobody placed the cursor. The driver did it from an `onFocusChanged`
alone, which ran a race it lost: focus lands before `publishSteps` has sent the
steps to the panel, so "there are steps" was false at the only instant the
question was asked. The panel opened with no cursor, and the first press on Down
served only to make it appear. An effect woken by `panelLive` and by the steps now
places it without depending on an order of arrival.

On returning from the cross, nothing placed it again. The header consumes Down and
asks the driver for focus; the driver did take it, but nothing designated a step
any more, since the opening effect does not replay (its keys have not changed).
Hence the second press. The selection is therefore also hooked to focus returning,
which is the path in question.

And a badly placed probe made me blame the wrong culprit. I had put the
`onFocusChanged` after the `focusable()` to trace: an `onFocusChanged` observes
what follows it in the chain, so it saw nothing of the node it was supposed to
watch, and the trace announced "the driver never takes focus" while it was taking
it. On that lie I built a fix removing the focus condition, the one path by which
returning from the cross could work. A probe goes before what it observes, or it
lies more surely than it informs.

Along the way, the driver's focus request stopped being a `delay(150)`, a bet on
timing, in favour of the same bounded frame-by-frame loop as the shell, and the
two `focusRequester`s stacked on that node became one again: a destination has
only one address.

Measured from the trace, on the Thor: on opening, `selectStep(0)` fires with no
key pressed; Up gives `clearStepCursor` and hands focus back; Down gives
`focus=true` then `selectStep(0)`. One press in each direction.

## R turns the page from both screens

The R key is the hover face's only control: it flips the plate to the catalogue
card. It was listened for by the front screen's grid, and that was right while the
panel could receive nothing.

Since it became touch, a press on it gives it its screen's focus, and R no longer
reached anybody. The panel's control stopped working at the precise moment you had
just touched the panel, which is the worst possible moment.

Both listeners therefore coexist, each on its own screen, and they call the same
thing. Keyboard focus goes to a window, not to the device: the one that has it
answers, the other sees nothing, and there is no double firing to fear. On a
single-screen machine nothing changes, the press still doing nothing at all, which
remains the rule: the front screen neither gains nor loses anything because a
panel exists.

The panel's ear is a `focusable` with no destination: nothing is selected on it,
it serves only to receive the key.

## The friends list goes to the back, both cards stay in front

The friends page carries three things: your code, the field for adding somebody,
and the list. The first two ask for something: you read them, you touch them. The
third reports: who is there, who is playing what. That is exactly the dividing
line between the two screens, so the list goes to the back and both cards centre
in front.

What that gives:

- The panel shows the whole list, in the same order as the front screen, in game,
  then online, then the rest by name. Two orders for one list would be two lists.
  Two columns beyond five friends, because the panel's box is short and clips
  silently.
- The front screen keeps its two cards, centred, and gains a line saying where the
  list went and how many friends it holds. Without it, a player with nobody online
  closes the page believing they have no friends: the panel is behind the machine,
  it does not draw attention to itself.
- With no panel, nothing moves: the page takes back its document order, the cards
  then the list, and reads from the top.

Removal lives at the back too, and the asymmetry is closed. Each case carries its
cross, on the right; the press opens the question on the panel, not on the front
screen. That is the deciding point: the finger has just pressed at the back, and a
question asked on the other side of the machine is not seen.

It is not a `Dialog`: a dialog window belongs to the screen that launches it, and
this one would open in front of the player. It is a scrim and a plate, in the
panel's window, with the same answers as elsewhere: cancel first, shell red for
what cannot be undone.

Three details of form, all demanded by use:

- The title is at the top left, in bold. Centred above a list, it read as an
  object's caption; it is the name of what the face carries.
- A case never runs past half the screen. At full width, a name and two words
  spread over 500 dp and the row reads as a bar. Two columns always, even with one
  friend: the right column stays empty rather than letting the case grow.
- The presence dot touches the nickname, it is not at the far end of the case: it
  is a property of the person, not a column.

The state labels ("Offline", "Online", the game's title) are resolved on the front
screen side and travel already translated, as for the session steps: the panel's
window has its own display context.

## Each face centres for itself

The faces are centred in the band left between the header and the legend. That
band is not the same for all of them: the session face has an empty legend, its
controls being pressed by finger, there being no key to name, while the menu faces
have one. Its geometric centre therefore falls lower than theirs, and it looked
set too low when the others were right.

The inset that lifts it therefore belongs to it alone. Placed first on the shared
box, it lifted the menu faces, which had asked for nothing: one face's fix threw
three others off.

The rule: a face that needs an offset carries its offset. The shared box only
centres.

## The fade between two faces is not decoration

Without it the panel cuts: a cursor running along a shelf replaces a whole face
per press, and text appearing at full intensity in one frame reads as a flash out
of the corner of the eye, which is precisely where this screen is. The fade also
gives an image that has not arrived yet the two hundred milliseconds it needs, so
a quick pass over the grid stops looking like a repeated load.

It is keyed on the game's identity, not on the model: the compatibility badge and
the catalogue card are published an instant later, against the same ROM. Fading on
the whole model would dissolve a face into an almost identical face every time,
which shows as a stutter.

### The fade takes all the room, or the face leaving slides

Added on 2026-08-28. Without `fillMaxSize`, the fade's container sizes itself on
its content: while it lasts, both faces are composed, it takes the size of the
larger, and the one leaving finds itself centred in a box that is no longer its
own, so it slides while it is only disappearing. The flaw stayed invisible while
the faces being swapped were the same size (two game cards); it showed on going
from the library to settings, which are not. The fade therefore fills the space,
and each face centres inside it.

Corollary for any future face: its height must not depend on its content. The
settings face freezes the line count of every text, two for the summary, filled or
not. Without that, "Library"'s summary runs to two lines where "Profile"'s fits on
one, the block changes height, and since it is centred, the mark and the title
move up at every cursor movement in the grid.

## The console is read live, the other faces are frozen

The model is remembered on the face key so that a face on its way out does not
redraw itself with the content of the one arriving.

The console branch is the only one that has to escape that freeze. Every console
shares the key `"console"`, so the remembered value would stay on the first
console shown and the card would never change text, which happened, and showed as
"the second screen has stopped working". Since the key does not change between two
consoles, no fade is running at that moment: there is no outgoing face to protect.
The frozen value stays the fallback, for leaving towards another face, when the
model has already become something else.

## The console card is a plate that grows, not a plate that gets replaced

Three attempts each broke something: a card sized to its text jumped from one
console to the next; a card stretched to the full height was two thirds empty; a
card hung at the top ended up under the header. All three tried to prevent a cut,
the panel replacing one card with another of a different size between two frames.

What was missing is that the change itself can be shown. The frame is never
replaced: it is the same plate from start to finish, centred, whose size is
animated towards what the next console demands while the words cross-fade inside
it. Nothing snaps, nothing is padded to a common size, and the card is never
larger than it needs to be.

Centred, so a growing card opens from its middle both ways: growing downwards only
would pull the eye, and this panel is read out of the corner of it.

The frame takes longer than the text, on purpose: the words have gone before the
plate has finished travelling, so nothing is being read while it moves.

## The panel does not shout

A console's warning is a bar, not a triangle with an exclamation mark. The panel
draws its own symbols, and one of the two things it must never do is shout: it is
something to know before starting, not an error that has just happened.

## The version is shown on the resting face

It is the only screen where it belongs without being asked for: it is the answer
to the question you actually ask the panel when nothing is running, "which version
is this machine on?", and answering it here saves a trip to settings. One notch
smaller and one notch fainter than the name, so it reads as a footnote and not as
a second line of equal weight.

## The console card: what it says, and what it does not

A plate and nothing else on screen. It is the panel's only face that is read
rather than glanced at, so it takes the shape a thing to be read has here, a plate
in relief with air around it, rather than the layout of the hover face, whose job
is to set artwork and a badge side by side.

The machine's name leads, because the player is looking at a shelf of folders and
the first thing the panel owes them is which one is under the cursor. Then two
lines, then a warning if that console has one. Nothing else fits, and nothing else
belongs: the main screen keeps all its explanations.

## The hover face: two pages, the second genuinely optional

Page one is what you look at while moving a cursor: the artwork, the machine, the
title, whether it plays together, which dump it is. Page two is what somebody who
has stopped on a game wants, what it is about, when it came out, what it looks
like, and it is reached by a button on the main screen, since this one has no
cursor.

It slides instead of fading. The button says "further down", and a page arriving
from below is the gesture the player has just made; a fade would say "replaced",
which is not what happened.

The artwork is the object and takes the weight: it is the only thing the player
recognises before reading anything. The column beside it answers what artwork
cannot say: which machine, whether this one really plays together, and which dump
is in the drive, two copies of the same game not being the same file, and it is
the player who has to know.

Nothing is guessed. Missing halves are simply not printed: a panel inferring "USA"
from silence would be wrong for every European player whose dumper skipped the
tag. Likewise page two, entirely editorial and entirely fallible: a synopsis in the
wrong language is presented as being in that language, and a game unknown to the
catalogue gets an honest sentence rather than an empty layout.

## Nothing scrolls, so everything has to fit

This window does not scroll, no cursor, no touch, so what goes below the fold is
lost, and a page cut through the middle of an image looks broken rather than long.
It is the paragraph that gives up its lines first: a synopsis reads perfectly well
truncated, an image does not.

## The language comes from the window, not from the process

It is read from this window's configuration: the panel is a second display with
its own configuration, and the player's language choice applies per configuration.
Taking `Locale.getDefault()` would be right by accident and wrong the day the two
diverge.

Corollary caught on 2026-08-24, and it cost dearly: the window is a `Presentation`,
whose context is made by `createDisplayContext()`. That context starts from the
display's configuration and loses the per-app locale: an app set to English kept a
French panel, the system's language. The language trap that hid it is described in
[`SecondScreenHost`](#the-window-the-context-is-not-the-one-you-think).

## The service light has its own colour

One lit dot and two words, nothing else: it is the panel's only piece of chrome
and it has to be readable without being read, the colour answering across the
room, the words only confirming.

Its colour is not the app's accent. The accent means "this is where you are"
everywhere else in Emufii, and a lamp borrowing it would make the cursor say two
things. Green and red are what a socket, a router and a console charger already
say.

## News arrives from above and leaves by itself

A friend online, a version published: the main screen always says both, and a
player with one screen loses nothing. What this adds is the case the main screen
cannot serve, the emulator owning it, where the alternative is a notification
shade pulled down over a running game.

It leaves by itself because nobody can dismiss it: this window takes no touch, by
design. Anything wanting an acknowledgement would stay forever.

## The session code carries no label

The six characters in the app's accent, on the only plate lifted off the board,
are already the one thing on the panel you could read aloud to somebody. Naming
them would be a costume of importance, which this app does not wear.

The two numbers the emulator's dialog demands are engraved into the board rather
than plated: they are references to note down, not objects to grab. Side by side
in hollows because you type them together, and a player copying one at a time has
to come back.

## The legend, and why the symbols are drawn

On the left you leave, on the right you act: the layout of every console shell the
player already owns. An empty side takes no room, so a face with nothing to say on
the left leaves no hole.

A button is a plate, not a hollow: it is the picture of a thing that stands proud
and can be pressed, and the board's hollows are for holes. Laid flat, 2 dp of
relief, because a legend is a diagram and must not compete with what it labels. A
hold instruction shows a held button: the plate loses its relief and its lit edge
and takes the shadow's tint.

The D-pad and the arrows are drawn, never typed. A character would arrive from the
first font that carries it, and a cap is 26 dp: at that size both the weight and
the baseline of a fallback font show. The system says icons are drawn.

## A letter is centred on its ink, not on its box

Three distinct things pushed it off centre, and laying the text out could only fix
one.

- To the left. `labelLarge` carries `letterSpacing = 0.1.sp`, and Compose adds
  that space after the last character as it does between characters. On a
  one-letter string, the measured width is the glyph plus a trailing gap:
  centring the measurement leaves the ink to the left.
- Downwards. `labelLarge` sets `lineHeight` 18 sp on `fontSize` 14 sp. Trimming
  that leading still leaves a box running from ascender to descender, while a
  capital with no descender only fills from baseline to cap height. Centring that
  box is not centring the letter, and no trimming makes the two identical.
- To the right, once the first two are fixed. Centring on the advance width is
  still not centring the ink: a glyph's side bearings differ, and this font's B is
  measurably right of the centre of its own advance. Measured offline against
  `rounded_bold.ttf` at that exact size, since a second screen cannot be captured.

Hence: the glyph is drawn and placed from
`android.graphics.Paint.getTextBounds`, the smallest rectangle enclosing the ink.
Setting the pen at `w/2 - (left + right)/2`, the ink's centre falls on the cap's
centre by construction, on both axes, for any glyph and any type scale.

## The artwork is moulded into the board, and its shadow is its colour

The shadow is tinted with the colour the artwork itself supplied (`Rom.accentArgb`,
already extracted for the main screen). It is the content-colour rule taken at its
word: the hue is the game's, not the app's, and it arrives as depth, a real offset
shadow, rather than as a wash laid over the chrome. A game with no extracted hue
simply casts the board's shadow, and nothing about the layout changes.

A rim alone was not enough here, and the arithmetic says why. The outline is a
1.5 dp stroke centred on the path, so clipping eats its outer half and it survives
at 1.73 px, at 24% opacity, on artwork 452 px wide, 0.38% of the width, half the
presence it has on a grid tile. That is why it reads on the main screen and
disappears on this one. Scaling the stroke would have been a second rule for one
place. A plate with the image inset is the frame this world already owns: face,
edge and lit bevel, at a size the eye finds from across a room.

## A control belongs to what it acts on

The artwork, and the path to its other page directly beneath. Placed in the middle
of the panel, that control was a fourth floating object between two columns and
read as a caption for the whole screen rather than for the game.

It is drawn as a pressable thing, a plate, like the legend's caps, because that is
what it is: the trigger on the front of the machine does that. Nothing on this
window is touch, so a control that looked like a touch target would be a lie.

---

## The window: the context is not the one you think

Lives in `secondscreen/SecondScreenHost.kt`.

In `EmufiiPresentation(context: Context, display: Display)`, the `context`
parameter is not declared `val`. A non-`val` constructor parameter is not visible
from a member function, so the `context` written in `onCreate` does not name the
one passed in: the name silently resolves to the inherited `Dialog.getContext()`
property, the display context `Presentation` makes for itself. It compiles without
a word, which explains how it went unnoticed.

That display context is the right one for everything but the language: it is built
by `createDisplayContext`, which starts from the display's configuration and
therefore drops the per-app locale. Hence the fix: keep the display's
configuration, which is what gives the window its size and its theme, and reinject
only the locales, read from `LocaleManager` so a change made in Android's settings
counts too. An empty locale means "the player never chose": the context is then
returned as it is rather than frozen on the day's language.

---

## The panel's state lives process-wide, not in the composition

That is the whole design, and it is not tidiness: the second screen's reason for
existing is the moment when the emulator owns the main screen and Emufii is
nowhere. A model held in a composition dies with it, so the only host that will
matter later, a foreground service outliving the activity, could never read it.
Publishing here costs nothing today and makes that host a new subscriber rather
than a rewrite.

Both hosts render the same content from that flow: there is therefore exactly one
description of the screen, whoever carries the window.

The current page lives there too, for two reasons: the button that turns it is on
the front screen, the panel having neither cursor nor touch, and the panel is
redrawn from scratch every time the window is remade. It returns to the first page
as soon as the cursor changes game: a second page left open would show one game's
synopsis under the next one's artwork, until the player noticed.

Hence also the distinction between "same game" and "equal models": the
compatibility badge and the metadata arrive after the ROM, and republishing the
same game with its badge filled in must not slam shut a second page open under the
player's hands.

## What travels to the panel

The whole [Rom], not a handful of extracted fields: the panel resolves its own
artwork from it, by the same path as a front screen tile. Both screens therefore
answer from one cache and one set of rules; the day a player picks artwork by
hand, the rear panel is not a second place to tell.

Except the region and the revision, which are passed and not computed here: they
are read once, on the ROM the front screen already holds. A panel parsing filenames
would redo it at every cursor movement.

The faces are deliberately few: a second screen trying to be a second app is a
second app to maintain.

The "console folder" face exists because a folder is the only place in the app
where the player thinks about the machine, and every machine plays together
differently here: one goes through a room on our server, another through a
redirect to a community service, another wants an address typed into the game
itself. There had never been a moment to say so: the front screen's tile is an
image with a count, and a paragraph laid over it would be a wall in the middle of
a grid.

Finally, the two numbers the emulator's box demands are also on the front screen,
and that is exactly the problem they solve here: the moment you need them is when
you are inside ARMSX2 or Azahar typing them, and the front screen has gone. The
clipboard carries only one at a time, and the box wants two.

## The panel only lights up if it has a reason

The host is a `Presentation`, a dialog tied to the activity, which costs no
permission and dies with it.

But being tied to an activity's lifetime is not the same as being in front, and
that difference was a real flaw: leaving Emufii for the home screen left the rear
panel lit on a process that merely happened to still be alive. What decides now is
a rule with two answers:

- Emufii in front: the panel reflects what the player is doing, so it follows the
  app and goes out when it is left. A panel still glowing on the back of a handheld
  whose owner has gone to their home screen is exactly the kind of thing that gets
  a feature switched off for good.
- A session running: it stays lit even if Emufii is behind the emulator, because
  that is where it earns its place. The code on the back of the console is what the
  other player reads, and it is needed precisely when the front screen has been
  given over to the game.

The rule is pure, therefore readable and testable with no second screen.

## Both bands are permanent, the legend included

The key legend reads `model.legend` live, outside the fade, and the resting face
has none. It therefore simply disappeared.

It is the column's last child, and the fade takes what is left. On vanishing, it
gave it back its 58 dp at once: the fade's centre dropped by 29 dp, taking the
still fully visible game card with it. Then the 220 ms fade began. From outside,
everything adjusted to the next screen before there was even a transition.

It is the third time this file has learned the same thing: a cross-fade does not
tolerate two geometries. The band therefore keeps its height even when empty, like
the top one, and what the resting face loses in centring is the price of a
transition that does not move.

The height is that of a legend key; the two values have to stay in agreement, or
the empty band and the full band are no longer the same size and the jump returns.

## The rear panel animates, in the end

It was frozen so as not to pay for the background twice: a window that redraws
costs the same whether it is being watched or not, and nobody contemplates the
panel, you glance at it to read a code or a state.

The stillness got noticed anyway: two identical backgrounds of which only one
breathes, that shows. And since the lustre went, what is left to redraw is two
waves drawn at twelve frames a second, and the half being saved was already the
smaller one. The `animated` flag stays for the next surface that really has to
keep quiet; no caller passes it any more.

## The artwork's corners follow its scale, not its measurement

The panel's artwork is the same tile as the grid's, drawn much larger: 196 dp a
side against 117 for a cell on the Thor. It nonetheless carried the same shapes in
dp, and a fixed radius placed on a surface 1.7 times wider looks 1.7 times less
rounded. The panel's white outline read as a square with broken corners next to
the same outline on the front screen.

Both shapes are therefore expressed as a fraction of their side, the fraction the
dp shapes are worth on a grid tile. The rounding is then the same to the eye on
both screens, whatever size each gives its tile. The front screen is untouched: it
is the reference.

## The three owners are self-contained from the first line

A `ComposeView` refuses to compose if its view tree does not carry a
`LifecycleOwner`, a `ViewModelStoreOwner` and a `SavedStateRegistryOwner`. On the
main screen the activity supplies all three and it is invisible work. On a window
that is not its own, nobody supplies them, and the failure is a crash on the first
frame rather than something a compiler catches.

The obvious shortcut is to lend the activity's owners to the second screen's
window. It compiles, it runs, and it is a dead end: the host this whole feature
exists for, a foreground service keeping the panel alive while the emulator owns
the front screen, has no activity to borrow from. Writing the shortcut now would
amount to writing this class later anyway, once the feature had shipped on it.

The owner is therefore self-contained from the first line. Both hosts attach one
to their own window, and the second host is a subscriber rather than a rewrite.

## The resting face waits its turn

Changing front screen is not atomic: the one leaving hands back
(`SecondScreen.clear()`), and the one arriving only publishes its own once
composed and its cursor placed. In between, the panel passed back through "Emufii
1.12.7", so returning from settings to the library showed the logo for a moment,
then the selected game. Two 220 ms fades for a movement that asked for none.

The resting face is held back for a grace delay. Any face arriving during that
delay cancels the wait, and the panel goes straight from one to the other. If
nothing comes, the resting face settles in, with a lateness nobody can see since
there is then nothing else to look at.

It lives in the host and not in `SecondScreen`: the object has no coroutine scope,
and giving it one on the main thread makes the JVM unit tests fail. The
composition already has one.

## A panel that asserts something false is a fault

The panel only subscribed to two cursors, the grid's and the settings hub's.
Everything else was invisible to it: the cursor goes up into the header, a launch
card opens, a close confirmation asks for a decision, and it went on showing the
last thing it knew, legend included. So it announced "B - Open" while B opened the
profile, and it showed a session's code at the very moment the front screen was
asking whether to close it.

A silent panel would have been a gap. A panel that asserts something false is a
fault, and that is the difference justifying the `Asking` face: it is not there to
show something more, it is there to stop showing something false. What it carries
is therefore exactly the question asked in front, never a summary nor a shortcut.

## A console card fits in two lines and a warning

Written per console because the answer really does differ per console, and the
differences are what a player has to know before inviting somebody: the Switch
joins a room brought up on our server, the DS is a redirect to a community service
with no session at all, the PS2 wants an address typed into the game by hand.
Saying "Emufii connects you" everywhere would be shorter and would be a lie four
times out of seven.

Two lines and at most one warning, and that cap is the design. It is read at arm's
length while a cursor crosses a shelf of folders; a third line would be read by
nobody, and the manual already exists on the front screen, where it can scroll.

The warning is reserved for what would otherwise be discovered as a fault, an
emulator whose stable version has no multiplayer interface, a VPN that has to be
up or the console silently calls servers switched off in 2014. Not for nuances: a
console with nothing to report carries none, and it is that absence that gives
meaning to the ones that are there.

## The panel's cursor does not depend on focus, which never arrives

Two wrong versions before the right one. It was first an `onFocusChanged` alone,
which ran a race it lost on opening: focus arrives before the steps have been
published, so "there are steps" was false at the only instant the question was
asked.

The next fix woke the selection on focus or on the steps arriving, except that the
trace says it unambiguously: `pilot focus=false` at composition, and never
anything again. The driver does not take focus. It receives the keys because it is
near the root and a key event bubbles up the chain, not because it is designated.

The right condition therefore never had anything to do with focus: it is "the
panel carries the controls, and they are published". The cursor is there from
opening, and the first press on Down is no longer spent making it appear. It does
not replay after a deliberate departure towards the cross: its keys have not
changed.

## In session, the pad legend is empty

`goBack` is null for the session screen and the press is swallowed rather than
passed on: letting it bubble up to the system closed the app mid-game and left
behind a session nobody closes. Leaving is the screen's button, by design. The pad
therefore really does nothing on back here, and printing it would be a lie at the
one place the player cannot check without leaving the thing they are being told how
to leave.

And nothing at all since the panel carries the steps: "B - Open" stayed shown
there while B, in session, acts on the front screen's selected control, not on this
panel. A legend naming a key the face does not take is the one flaw this legend
exists to prevent. The panel's controls are pressed by finger, and a button
carrying its own name does not need the finger captioned.

## R turns the page from both screens

The key was listened for by the front screen's grid, and that was right while the
panel could receive nothing. Since it became touch, a press on it gives it its
screen's focus, and R no longer reached anybody: the panel's control stopped
working as soon as you had touched the panel.

Both listeners coexist, each on its own screen, and they call the same thing.
Keyboard focus goes to a window, not to the device: the one that has it answers,
the other sees nothing.

## Every face centres in the same place

The resting face was granted, for an hour, a bottom inset lifting it to the middle
of the whole panel rather than to the middle of what is left under the header. It
was better centred, and the remedy was worse: during the fade, both faces are
composed at once, one with the inset and one without, so everything slid at the
very moment the cursor moved from the grid to the header, which is precisely the
app's most frequent transition.

The fade therefore takes all the room, and each face centres inside it. Without
`fillMaxSize`, the fade's container sizes itself on its content: while it lasts it
takes the size of the larger of the two faces, and the one leaving finds itself
centred in a box that is no longer its own, so it slides while it is only
disappearing. It did not show between two game cards, which are the same size; it
showed on going from the library to settings, which are not.

And the settings face has a constant height by construction: every text has its
line count frozen, two lines reserved for the summary even when it fills only one.
Without that, "Library"'s summary runs to two lines where "Profile"'s fits on one,
the block changes height, and since it is centred, the mark and the title move up
or down at every cursor movement.

## The resting mark is a mark, not an emptiness with a number in it

That face only served the first seconds of a launch, where nobody was looking at
it. It now serves every time the cursor leaves the grid for the header, so several
times a minute, and the app's name lost in the centre of 1240 by 1080 of black
read there as a screen that had not finished loading.

What is added to it is the background's motif stopped at its smallest size: the
staircase of two tiles, coral then turquoise, the logo's. Nothing new to learn,
nothing moving, nothing asking to be read, and the resting face becomes again what
it claims to be, a machine switched on and waiting.

The real logo, not a hand-drawn quotation. It was two tiles traced on a `Canvas`.
That held while the resting face only appeared at launch; it now appears several
times a minute, and an approximate mark set next to the real one on the front
screen reads as a botched logo, not as a reminder.

## A question's face repeats, it does not ask another

The panel takes no decision and offers no button while a modal layer waits for an
answer in front: the answer is given on the front screen, where the controls live
and where the cursor is. Its only task is to stop showing the previous scene, the
game card, the session code, at the moment it has become false.

Hence the plainness: a ring, a title, a line. A rich face here would ask the player
to choose which of the two screens to read while they are being asked to decide,
and that is exactly the divided attention a modal exists to remove.

## The hub face completes, it takes nothing

The mark above, the name, its description line, then the path leading there.
Nothing the front tile does not already say: the panel completes, it takes nothing
from anybody. What it adds is size, a name read at arm's length, under the thumbs,
while the eye is still on the grid.

A cross-fade between two entries, and not a slide: the hub's cells are not an
ordered sequence you move along, they are a table, and an invented direction would
state an order that does not exist.

## One column, and the code takes the whole width

The previous version cut the session face in two, identity on the left, controls
on the right, to gain height. Seen for real, it was worse than the problem: each
column fell to 268 dp, the code broke into "NRX-" and "572", and the port was
written one digit per line.

The panel is wide (537 dp) and short (320 dp). What has to spread spreads
sideways, and what repeats goes side by side: the two controls share a row instead
of stacking 130 dp. They say the same kind of thing as each other, "do this, then
that". The gamepad designates one of them through a virtual cursor driven from the
front screen: focus does not cross windows, but the selected index travels through
the singleton, like R's page.

## Each face centres for itself

The bottom inset lifts that face, and it alone. The session face is centred in what
is left between the header and the legend, and its legend is empty while the
others' is not: its geometric centre therefore falls lower than theirs, and it
looked set too low when the others were right.

The inset was first placed on the shared box, which lifted the menu faces, which
had asked for nothing. An offset that fixes one face is placed on that face.

## A locked step has to stay readable at arm's length

Material's disabled colours are tuned for a button hovered at thirty centimetres:
on the panel's black, at the distance you hold a console, "2. Start emulation"
became an illegible grey.

But that step is not out of service, it is the next one, and it is in fact all the
face has to say about what is going to happen. It therefore keeps a plain plate and
plain ink, and it is the absence of a ring that says it cannot be pressed yet.

## A stack rather than one more publication

The background publishers, the grid, the hub, the session, set their face in a
`LaunchedEffect` whose key is what they observe: the cursor, the session code. A
modal that had simply published would therefore have overwritten their face with
none of them having any reason to set it again on leaving, and the panel would have
stayed on the question after the answer.

The background is therefore kept intact underneath, and the modal only masks it for
as long as it lives. Nothing to restore, therefore nothing to forget to restore.
The token returned on placing avoids the classic failure of this kind of stack: two
layers opened back to back, the first closing last and removing the second's face.
Here it removes only its own, or nothing.

Two rests have to be told apart: the one a screen leaves on departing, where you
have to wait for the next to speak, and the one a layer places deliberately, the
cursor going up into the header. The first is held back, the second is the answer
and must appear at once.

The background is recomputed on every write rather than derived by `combine`: a
derived flow needs a scope, and a scope on `Dispatchers.Main` built in an object's
initialiser makes the JVM unit tests fail, they having no main loop.

## What travels to the panel travels already resolved

The steps, the friends, the marks: label, state, action, everything is computed on
the front screen. Two reasons, and the second is measured.

The state deciding a label lives in the session screen's composition. And the
panel's window has its own display context, which has already made the app speak
French inside an English interface once.

Corollary for the marks: a name, not a composable. The model lives process-wide
and outlives the activity; a composition lambda held in there would hold the tree
that created it with it. The panel does the mapping at its end.

## The cursor only stops on a pressable step

It stopped on all of them, locked ones included: "2. Start emulation" took the
ring while it was disabled until step 1 had succeeded, and its label stayed in the
disabled state's grey. The selected element was therefore the least readable thing
on the face, and it announced that nothing would happen.

A locked step stays shown, it says what comes next, and that is half the point of
a numbered list, but it stops being a stop. You move in the requested direction as
far as the first open step; if there is none, the cursor does not move rather than
landing on a dead button.
