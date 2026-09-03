# Settings and consoles: the defaults, and the extension table

The narrative that lived in `settings/AppSettings.kt` and `library/Console.kt`,
taken out of the code on 2026-08-24 (see `docs/STYLE_COMMENTAIRES.md`). Headings
are anchors cited from the code.

## Following the phone is the right default, except for the accent

Language and theme default to what the phone says, for the same reason: an app
that ignores the system setting is an app that contradicts its user.

The other values exist because the phone's setting is often a time range, and
somebody reading in bed should not have to change it for the whole device to get
a dark library.

The accent is the one exception, and it is deliberate. It can follow the colour
Android extracts from the wallpaper, but that is not its default: the cursor's
cyan is the app's signature, and a console menu that changes identity with the
wallpaper has no identity.

This setting does not touch the rule of the world either: there is exactly one
accent, spent on the cursor and the primary action, everything else coming from
the artwork. It says which hue plays the part, not how many are on screen.

## OLED is a dark, not a third universe

The handheld screens targeted are OLED: a black pixel there is a pixel switched
off, so the dark theme's bluish background draws power where it could cost
nothing, and leaves a grey haze in the dark.

It changes only the background and the card fill; everything asking "is it dark?"
still sees dark. That avoids rejudging 44 components for a setting that speaks
only about brightness.

## Language goes through the platform, the theme cannot

Language is set through the per-app language API rather than by juggling a
`Configuration` ourselves. minSdk is 33, so it is simply there: Android remembers
the choice from one launch to the next, shows it in the system settings next to
every other app, and recreates the activity so the new strings take effect.

No platform API owns the theme: Android has no per-app dark mode below
`setApplicationNightMode` (API 31), and even that one covers only the two forced
values. The choice therefore lives here and the theme reads it, which has the
added benefit of making the switch instant instead of recreating the activity the
way a language change does.

## What is stored is what was refused

Hidden consoles are recorded as what is hidden, never as what is shown, and that
is the load-bearing choice.

The other way round, a library containing only 3DS dumps would have five consoles
ticked at install, and would silently hide a console added by a later version:
the stored set simply would not mention it. Recording refusals means everything
new arrives visible, the one default that cannot lose a game.

A name no enum answers to is dropped on read. That happens after a downgrade, or
if a console is one day removed, and the game reappearing is a far better failure
than a grid silently missing a machine.

Same principle for the library layout: an unknown value, written by a newer
version and then downgraded, falls back on the default instead of taking the
launch down. It is kept here rather than in the screen, because it is a choice you
make once and expect to find again: losing it on every return from a session
screen would look like a bug.

## The "on" defaults, and why they are switches all the same

- The second-screen panel: on, because a player whose handheld has a rear screen
  bought it to use it, and a feature nobody finds in a settings page is a feature
  nobody has. It is a switch rather than a silent behaviour because the panel
  stays lit for the whole session, and somebody playing in the dark or counting
  their battery has the right to turn it off. The setting is stored on every
  device, even without a second screen: it costs one boolean, and a player moving
  their profile to a handheld that has one arrives with their choice intact.
- Friend notifications: on, because a friends list nobody is told about is an
  address book, the point of adding somebody being to know when a game is
  possible. It stays a switch because the same feature, seen from the other side,
  is an app saying who watches whom.
- Version announcements: on, and barely a preference at all. Emufii is
  sideloaded, no store speaks for it, and a fix that never reaches players fixes
  nothing. A switch only because somebody who updates by hand is entitled to
  silence.

## Every player brings their own key

The SteamGridDB key is empty until the player supplies one, and the library then
keeps the ROM icons, 32 or 48 pixels a side.

A key frozen into the APK would be the same for everyone: extractable by opening
the package, and it would be the author's account carrying the quota and the
abuse of the whole installed base.

The frontend's folder follows the same logic in reverse: Cocoon Shell and ES-DE
have already downloaded artwork for those very files, and the player has often
cropped some of it. Pointing Emufii at one makes their library look here exactly
as it looks over there, with no key, no network and no wait.

## One store for the process

Every flow here is held in memory, and a second instance would not see the first
one's writes.

Built per screen until 2026-08-19, and the bug that came of it is the quiet kind:
consoles switched off during onboarding came back as soon as the library
appeared. The library had built its own store while onboarding was still on
screen, primed it from disk before anything had been written, and nothing ever
told it otherwise. On disk the choice was right from the start, so it survived a
restart, which makes this sort of thing read as random.

`SharedPreferences` is already process-wide and thread safe; what is not is the
`StateFlow` in front of it. So there is only one.

---

# The console table

## The grid stays a grid

The user drops a folder in and everything they own appears together. Which
emulator gets launched, and what has to happen on the network first, is our
problem, not theirs.

## The extension table is a map: one owner per key

That is the constraint explaining two apparent omissions.

GameCube and Wii are listed without `.iso`, although that is the commonest name
for a disc image. Adding `.iso` here would not share it with the PSP, it would
take it away, last registration winning, and every UMD rip in the library would
silently point at Dolphin.

The PS2 has not a single extension of its own, and that is not an oversight
either. On the Thor, the six PS2 games and the six PSP games are all `.iso`, in
two neighbouring folders, exactly the collision GameCube had already run into.

These consoles therefore claim only what nothing else uses, and the shared
extension is settled by reading the file. A PS2 game arrives through its folder
(`ps2/`), or through the byte reading that only promotes what it positively
recognised, a `BOOT2` in `SYSTEM.CNF`, which is also what distinguishes it from a
PS1 disc.

## The folder name is the cheapest and truest answer

The player sorted the file themselves, where an extension can collide and
sniffing the content costs a read.

Indexed on the normalised name, lowercase with separators removed, so `PS2`,
`ps_2` and `PlayStation 2` are one key. And on the file's immediate folder only,
not its ancestors: `ROMS/ps2/dumps/game.iso` is a PS2,
`ROMS/dumps/ps2-something.iso` is not decided by `ROMS`.

## The network name is a contract, never a label

The coordinator cannot infer the console from what it stores, a title and a
titleId that 3DS and Switch write the same way. The name is therefore written
here in stable lowercase, never derived from the label: a label gets retouched
for the screen, and this name decides whether to bring a room up on the VPS.

Same care for the emulator name shown to the player: it is not a translated
string, these are product names, identical in every language, and it is what is
written on the icon they are about to see. Hard-coding "Azahar" into a label is
how a Switch session ended up announcing "automatic Azahar setup" while it was
driving Eden.

## The port is part of the plan

Azahar and Eden share 24872, inherited from Citra; Dolphin listens on 2626. The
plan must carry the right one, failing which the guest dials a valid address on a
port where nobody answers, a fault that reads as a broken tunnel.

## The four multiplayer families

- Rooms on the session network (3DS/Azahar, Switch/Eden, GameCube-Wii/Dolphin,
  PS2/ARMSX2 in Local Link): the room has to be joined before the game starts.
- PSP: PPSSPP's ad hoc has no room to create and no box to fill, the console
  looking for "the ad hoc server" at an address set once and for all, and the
  relay translates it to the current session's host.
- WFC (DS): online play reached by moving the DNS, not by building a network. No
  session code, no tunnel between players: each console talks to the revival
  server. A second product inside the same app. It sits outside the "join a room
  first" rule because there is no room, only a resolver.
- Recognised, but with no multiplayer path built: those ROMs stay in the grid.
  Taking them out would make the library look broken to somebody who owns them.

And three screen shapes, three drivers: Azahar and Eden by view ids, Dolphin by
nesting of Compose texts, ARMSX2 by rows, with, for the last one, real Android
views but no translatable string in the APK, hence hard-coded English labels.

PS2 online play does not go through any of this: it is played on a revival
server, over DNS, with no session and no tunnel. The two must not be confused on
screen.
