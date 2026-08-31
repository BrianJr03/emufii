# What rendering costs, measured rather than assumed

Campaign of 2026-08-29, started from an observation by the user: the grid was not
smooth when moving quickly between games. The headings are anchors cited from the
code.

The companion to this page is `docs/STYLE_COMMENTAIRES.md` for form, and the
section of `CLAUDE.md` on not judging a debug build for smoothness, which is the
precondition without which none of these measurements mean anything.

## The reference point: Cocoon

`rip.moth.cocoonshell` 3.04, installed on the Thor, measured under the same
conditions (25 fast scrolls, `dumpsys gfxinfo`).

| | Cocoon | Emufii (before) |
|---|---|---|
| median / 90th | 5 ms / 6 ms | 9 ms / 31-53 ms |
| janky frames | 0% | 12-23% |
| ART state | `speed-profile` | `run-from-apk` |
| software masks | 75 KB / 1 | 1.1 MB / 14 |
| offscreen targets | 4.9 MB / 16 | 34 MB / 23 |
| GPU memory | 17.6 MB | 61.6 MB |
| frames per cursor step | ~1.1 | ~2.3 |

What we took from it: the compilation profile, and the hunt for intermediate
layers. What we do not take: its tiles are light, one image, no moulded plate, no
coloured drop shadow, no rim, and its selection triggers no animation. Trading
our visual direction for its would be paying for smoothness in identity.

Analysis detail: Cocoon does not use Compose lazy lists (no trace of
`androidx.compose.foundation.lazy` nor `LazyListState` in its six dex files,
while `androidx.compose.foundation` is there). It has its own scrolling.

## The compilation profile

`app/src/main/baseline-prof.txt`, with `androidx.profileinstaller`. Android does
not install a compiled application: ART interprets it, compiles on the fly what
comes up often, and only compiles seriously after hours of idleness, which is to
say after the first sessions, the ones where you judge whether the app is smooth.

Measured, same code, same device, only compilation changing:

| build | median | 90th | 99th |
|---|---|---|---|
| debug | 12 ms | 48 ms | 113 ms |
| release, not yet compiled | 9 ms | 53 ms | 97 ms |
| release compiled | 9 ms | 31 ms | 46 ms |

It is the tail that moves, and the tail is what you feel. The profile is written
by hand with wildcards on the hot packages: the official tool wants a
macrobenchmark module, and an approximate profile is infinitely better than none.
What it misses is simply compiled later, as before.

## The blur source is only wired up when something blurs

`hazeSource` records everything it carries into an offscreen layer, so the
keyboard plate can blur through it. It was placed on the entire library,
permanently, while the screen's only `hazeEffect` lives in the search plate,
which is composed only when the keyboard is open. The whole grid therefore went
through a full-screen render target on every frame, for nobody.

Wired to `searchOpen` rather than `keyboardOpen`: a head start on the plate
opening, so the blur is already recorded when it arrives.

## An offscreen layer is not a drawing setting

Each tile's title carried a `CompositingStrategy.Offscreen` for a fade gradient
that is only drawn if the title overflows, which is to say, most of the time,
never. Fourteen render targets allocated, cleared and recomposed per frame, for
nothing.

The rule: an `Offscreen` is placed when the effect that requires it is active,
not in anticipation of its possibility.

## What was measured and came to nothing

- The publication threshold to the second screen is discussed elsewhere (110 ms
  at the time): it was not the cause, but it was raised to 200 ms anyway, see
  `bibliotheque.md`.
- `RomTagReader.read`, `CompatDb.ratingFor` and `GameMetaDb.metaFor` run on the
  UI thread in `PublishHovered`: suspected, ruled out. They are lookups in
  in-memory tables and string operations.
- `rememberTileArt` opens three flow collections per tile: suspected, ruled out.
  `SettingsStore` is a singleton whose flows are backed by `SharedPreferences`,
  so subscribing costs almost nothing.

## What is still open

Isolated frames at 100-150 ms remain, probably artwork decoding or the
composition of an incoming row. Driving through `adb` sends an event every
~200 ms and does not reproduce a thumb: telling them apart needs a Perfetto trace
taken while somebody actually scrolls the grid.

## One clock for everything that moves continuously

Two things run without stopping in the app, the background and the cursor, and
each had its own. Nothing aligned them: at twelve and fifteen steps per second
they wrote at different instants, so the app redrew twenty-seven times a second
instead of twelve.

What costs is not how much is drawn but how many times: every repaint forces the
whole window, fourteen tiles with their plates, mouldings and shadows, a dozen
milliseconds of CPU whatever the reason for the repaint.

Both therefore beat together, and the app redraws once per beat. Measured on the
Thor with the library still: 85% of a core at the start, 0% when nothing moves.

Do not create a second one. Anything that has to advance on its own derives from
this one: that is the only guarantee the repaint count does not creep back up as
things get added. It is still when the system has turned animations off, a
setting that exists for people bothered by motion, and also the one taken by
people saving battery.

## The cursor gradient is not an angular sweep

A `SweepGradient` turns around a centre: on a square its bands spread out at the
corners, and on a wide row they compress at the ends, so the colour no longer
advances at constant speed along the stroke, which is precisely what one wants to
see.

Here each pixel is reduced to its arc-length position along the perimeter of the
rounded rectangle, and the colour depends only on that distance travelled. It
therefore advances at the same speed on a straight edge and in a corner, whatever
the shape.

```
u   = frac(t / perimeter - phase)
mix = 0.5 - 0.5*cos(2*pi*u)
```

The cosine is what makes the cycle seamless: it is 0 at 0 and at 1, so the colour
returns to its starting point by itself after a full turn. A linear interpolation
would have left a hard break there, turning with the ring.

Rendering is a small stretched bitmap, not a per-pixel shader: the computation is
redone only when the phase step changes, and the result is cached.
