# The two interface sounds

Added on 2026-08-29, at the user's request, who supplied both files. The
headings are anchors cited from the code.

## Two sounds, one family

`hoversoundemufii` (96 ms) fires when the cursor lands on something new,
`clicksoundemufii` (144 ms) when something is pressed. They live in `res/raw/`
as `sfx_hover` and `sfx_click`.

`SoundPool` rather than `MediaPlayer`. These sounds last under two tenths of a
second, fire several times a second when sweeping a grid, and must trigger with
no latency. `MediaPlayer` prepares a source on every play and cannot overlap
two; `SoundPool` decodes once into memory and plays from there. Four concurrent
streams: the cursor can move on while a press is still ringing.

They are decoded when the activity starts rather than on first play, or the very
first hover would be silent, the one that happens before the player has worked
out that the app makes sound.

Hover is quieter than press (0.45 against 0.85). It fires on every cell crossed;
a movement as loud as an action would suggest something had happened.

## Android's own setting is authoritative

The app does not create its own sound switch: it reads
`Settings.System.SOUND_EFFECTS_ENABLED`. Somebody who has turned interface
sounds off has turned them off for every application, and ours has no reason to
be an exception.

The `AudioAttributes` file them under interface sounds
(`USAGE_ASSISTANCE_SONIFICATION`): they follow system volume, go quiet during a
call, and do not duck somebody's music.

## Hover fires where the cursor is drawn

`focusRing` is the single point every cursor passes through: ordinary controls
reach it through `controlRing`, and grid tiles call it directly with their own
computed index. A sound placed there therefore covers both families, including
the grid, which does not use Compose focus.

It is also what guarantees a silent cursor cannot be added: the app's rule was
already "everything that takes focus must show it", and it becomes "and say it".

## The sound and the click are one call

`Modifier.tap` and `Modifier.tapOrHold` replace `clickable` and
`combinedClickable` throughout the app, 32 places. This is not a sound modifier
placed next to the click: it is the same call, because a `clickable` added later
without sound would be a thing that answers silently in the middle of an
interface that speaks, and nothing would flag it.

Three exceptions, all deliberate:

- Press swallowers (`onClick = {}`) stay bare `clickable`s. They exist only to
  stop a press reaching what is behind, the keyboard plate, a dialog card, and a
  sound there would announce an action that does not happen.
- Dismiss scrims do sound: touching outside to close the keyboard or a dialog is
  an action, with a visible consequence.
- The gamepad path goes through `gamepadClick`, not `tap`: `Key.ButtonA` is not
  one of the keys Compose recognises itself. The sound is placed there too. The
  two paths do not cross, `gamepadClick` swallowing the matching press so one
  push does not count twice.

## Five key handlers short-circuit `tap`

The first attempt left opening a game silent: you heard the hover of the card
opening, never the click that opened it. The cause is structural and worth
naming, because it will happen again.

`Modifier.tap` covers only what `clickable` handles: the finger, and the keys
Compose recognises itself (Enter, D-pad centre). But this app often reads the
confirm key itself, because it holds its own cursors. Five places do, and none
of them went through `tap`:

- `gamepadClick`, the general `Key.ButtonA` case;
- `entryKeys`, the grid: short press, hold, and `Y` opening the menu;
- the session screen, for the step selected on the rear panel;
- the keyboard plate, whose keys are not focusable;
- `PadTextField`, where confirming opens the keyboard.

The sound is placed by hand in those. The rule to remember: a screen that holds
its own cursor holds its own sound too. Searching for `CONFIRM_KEYS` gives the
full list of places involved.

It is also what made the `Context` fall out: `entryKeys` is an ordinary lambda,
not a composable. Rather than threading a `Context` down to it, `Sfx` keeps the
application context from its own preparation, and neither `click()` nor
`hover()` asks the caller for anything.

## Silencing Android's own is done view by view

Android's interface sounds kept layering over ours. This is not an application
setting nor a theme attribute: `playSoundEffect` is gated by the flag on the
view that calls it, and there is no app-wide switch.

`SilenceSystemSfx()` therefore clears that flag on `LocalView.current` and on
its whole chain of parents: the Compose view is not always the emitter, and a
`Dialog`'s decor view is another one.

It has to be placed once per window, and a window is not a screen:

- the activity (covers every screen, and the layers placed inside it, the launch
  card not being a window of its own);
- every Compose `Dialog`, which opens its own: `PadDialog` and
  `IconPickerDialog`;
- the rear panel, which is a `Presentation`.

A `Dialog` added later without this call will bring both sounds back together.
