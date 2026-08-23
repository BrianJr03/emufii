# PSP multiplayer: the manual step, and how to remove it

This is an open working problem, not a finished design. It lives on `dev`
because nothing here is ready for `main`.

## What works today

PSP multiplayer works. Two remote players, two devices, two different homes,
proven on WipEout Pulse and Tekken. The link comes up and the games play.

## The rough edge

**PPSSPP cannot be automated.** It draws its interface with its own renderer,
so there is not a single Android view in it, and Emufii's accessibility service
— which configures Azahar, Eden, Dolphin and ARMSX2 — has nothing to hold on
to.

So the player configures PPSSPP by hand before every first launch: an address
to type into the network settings, and one setting to change. The PSP session
screen carries a "manual setup (required)" button that says so, and turns green
once done.

Two things people get wrong, worth knowing before you start:

- **Packet relay is a three-state choice** (`Automatic` / `Enabled` /
  `Disabled`), not a checkbox, and it defaults to `Automatic`. Public servers
  are happy with `Automatic`; PPSSPP's own help asks for `Disabled` over LAN or
  VPN, which is our case.
- **PPSSPP keeps its own live directory of public ad-hoc servers**
  (`metadata.ppsspp.org/adhoc-servers.json`). That is a different feature with
  its own screen. Do not confuse the two.

## The lead: PPSSPP's per-game config is writable

Measured on PPSSPP **v1.20.4**, and this is the part worth building on. It was
scouted and proven; **none of it is implemented**.

- **The per-game ini is read when the ISO is opened**, not when PPSSPP starts
  (`PSP/SYSTEM/<SERIAL>_ppsspp.ini`, see `Core/Util/PathUtil.cpp:164`). So the
  emulator does not have to be stopped or restarted for a change to take.
  Proven with a visible witness: an FPS overlay written by our file appeared on
  screen.
- **PPSSPP's memstick is on shared storage today** (`/sdcard/PPSSPP`), which is
  what makes this reachable at all. This is the fact that expired an earlier
  "impossible, verified" verdict — what changed was the memstick location, not
  Android.
- **A game is launched by intent**, extra `org.ppsspp.ppsspp.Args`, with the
  ROM's `content://` URI (`PpssppActivity.java:166`). PPSSPP is
  `singleInstance`, so the intent only starts a game when PPSSPP is closed; the
  per-game ini applies either way.
- It needs **one persistent SAF grant** on the PPSSPP folder, the same shape as
  the ROM folder grant. Validate the pick by checking `PSP/SYSTEM/ppsspp.ini`
  exists.

## Dead ends, please do not repeat them

- **`--appendconfig=/sdcard/…` does not work.** PPSSPP holds no storage
  permission of its own and reaches its folders only through persistent SAF
  grants, so a plain path is unreadable to it.
- **`--appendconfig=content://…` does not work in 1.20.4 either.** A real
  command-line parser landed in `Core/CmdLine.cpp` after that release, so this
  is worth retrying on 1.21 and later — but it is not a 1.20.4 answer.
- **If a player's memstick is on internal storage, this whole path is closed**
  for them. The manual fallback has to stay, whatever gets built.

## Why it is worth doing

If the per-game ini is written by Emufii, then the host address for a session
can be carried by `proAdhocServer` directly, per game and per session. The
manual step disappears for everyone whose memstick is on shared storage, and
the fixed address the player types today stops being necessary.

## Scope

Everything above is about PPSSPP and about Android. Emufii's session server and
its relay are not part of this repository and are not part of this problem: what
is needed here is the client side writing a config file and launching a game.
