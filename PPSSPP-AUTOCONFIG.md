# PSP multiplayer auto-configuration

## Result

Stock PPSSPP can be configured by Emufii without root, accessibility, a fork,
or access to PPSSPP's private app data.

PPSSPP supports a per-game INI in its memory stick:

```
PSP/SYSTEM/<DISC_ID>_ppsspp.ini
```

It reads that file when the game boots, including when PPSSPP is already open
at its menu. Emufii receives one persistent Android Storage Access Framework
grant to the memory-stick root, merges four settings, then launches the ROM by
the same `ACTION_VIEW content://` intent it already used.

Implemented values:

```ini
[Network]
EnableWlan = True
EnableAdhocServer = True
proAdhocServer = 10.66.1.1
AdhocServerRelayMode = 2
```

`2` is PPSSPP's `AlwaysOff` packet-relay mode. Emufii's VPN/relay transports
the peer-to-peer packets itself; PPSSPP's public-server packet relay must not
be placed in the path as well.

Emufii does not touch nickname, MAC address, port offset, UPnP, DNS, graphics,
controls, or any other setting.

## Device proof

Validated on 2026-08-23 with:

- AYN Thor, Android 13 / API 33
- PPSSPP free v1.20.4 (`versionCode 120040000`)
- memory-stick root `Internal storage/ROMs/psp`
- game `Castlevania: The Dracula X Chronicles`, `ULUS10277`, CHD

The device trace showed PPSSPP opening
`PSP/SYSTEM/ULUS10277_ppsspp.ini` through
`com.android.externalstorage.documents`. PPSSPP's live Networking screen then
showed WLAN enabled, server `10.66.1.1`, packet relay `No`, and the built-in ad
hoc server enabled. The test file was removed afterward and the global
`ppsspp.ini` SHA-256 returned to its original value.

The memory-stick path is not universal. Emufii validates the picked tree by
locating `PSP/SYSTEM/ppsspp.ini`; it never assumes `/sdcard/PPSSPP` or the ROM
library directory.

## Safe ownership and restoration

Before the first private launch of a game, Emufii records the presence and
original value of only those four keys in private app storage. It then performs
a line-preserving merge into the game's existing file.

Before public-server settings or public launch, Emufii restores those four
original states. It merges the restoration into the file as it exists then, so
graphics, controls, and other settings changed during private play survive. If
Emufii created an otherwise empty file, the file is removed after restoration.

Changing the configured memory-stick root is refused while restorations are
pending: a backup must never be applied to a different PPSSPP tree.

## DISC_ID coverage and fallback

PPSSPP names the file from the game's `DISC_ID`, for example `ULUS10277`.

- ISO and PBP: Emufii already reads `PARAM.SFO` and stores `PSP-<DISC_ID>` in
  `Rom.productCode`.
- CHD and CSO: Emufii accepts the conventional four-letter/five-digit ID in the
  filename, such as `[ULUS10277]`.
- No trustworthy ID: the established manual setup remains available. Emufii
  never guesses a config filename and never blocks the game from launching.

`RomRef` now retains both `productCode` and the original filename so session
launches have the same identity evidence as the library.

## Android permission model

The normal Emufii ROM-library grant stays read-only. Automatic PPSSPP setup has
its own narrower `ACTION_OPEN_DOCUMENT_TREE` grant, persisted with both read
and write flags. The user selects the folder containing `PSP/SYSTEM/ppsspp.ini`
once in Settings.

This separation is intentional: making the entire multi-console ROM library
writable would grant much more authority than editing PPSSPP's small config
directory requires.

## `--appendconfig` finding

The earlier note that PPSSPP 1.20.4 cannot read
`--appendconfig=content://...` was incorrect. It was retested successfully on
the Thor when the URI was inside a tree for which PPSSPP already held a
persistent grant.

It is still not the production route:

- PPSSPP only parses command-line options on a cold activity start; its
  `onNewIntent()` path forwards the whole string as a game shortcut.
- `Config::LoadAppendedConfig()` immediately calls `Save()`, persisting the
  appended values into global `ppsspp.ini`.
- A later per-game config can override the appended values during game boot.

Direct per-game configuration is hot-loadable from PPSSPP's menu, scoped to one
game, and does not alter the user's global public-server choice.

## Operational limit

Do not switch a game's private/public mode while that same game is still
running. PPSSPP holds its per-game settings in memory and saves them when it
leaves the game, which can overwrite a file changed underneath it. Exit the
running game to PPSSPP's menu first. This was already required to reach the
network settings and is now stated explicitly in the UI.
