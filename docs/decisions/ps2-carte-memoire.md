# PS2: the fabricated memory card, and YNCF encryption

The narrative that lived in `ps2/Ps2MemoryCard.kt` and `ps2/Ps2NetcnfConfig.kt`,
taken out of the code on 2026-08-24 (see `docs/STYLE_COMMENTAIRES.md`). This is
format specification: every constant below was measured on a real card, not
copied off a wiki. Headings are anchors cited from the code.

## What the emulator checks of a card: almost nothing

ARMSX2 opens the file, infers the geometry from its size, and otherwise passes
the pages through to the guest as they are, 512 bytes of data plus 16 bytes of
spare each (`pcsx2/SIO/Memcard/MemoryCardFile.cpp`, `MemoryCardProtocol.cpp`).

Every judgement (magic string, FAT consistency, directory modes, ECC) is made by
the emulated console, against bytes the image carries literally. A fabricated
card is therefore held to what the BIOS itself writes, and the layout followed
here is modelled on a card it actually wrote: the one the PS2 formatted through
ARMSX2 on 2026-08-20, then filled by Midnight Club 3's network utility, byte for
byte, before those same measurements became these constants.

## The layout, in card order

The image is the standard 8 MB RAW layout: 16,384 pages of 528 bytes =
8,650,752 bytes, a `BWNETCNF` save at the root, and free space erased to `0xFF`.
It is also why generation stays cheaply deterministic: nothing on the card
depends on anything but the save's bytes and the clock.

- Page 0, the superblock: `Sony PS2 Memory Card Format 1.2.0.0`, 512 bytes per
  page, 2 pages per cluster, 16 per block, 8192 clusters, allocation from cluster
  41, and the tail constants the BIOS writes at format time. There is no
  superblock checksum in this format, Sony's mcman leaving `0x48`-`0x4F` as
  padding (ps2sdk `mcman-internal.h:308`), and no directory entry carries one
  either.
- Pages 1-15, the rest of the first reserved block: erased data, but with their
  ECC spares, because that is what the BIOS format leaves there. The first 8
  bytes of page 1 are ARMSX2's business, it keeping a host-side checksum there at
  offset `0x210`, and it stamps them itself: generation leaves them erased.
- Cluster 8, the indirect FAT listing the 32 FAT clusters, 9 to 40. Entries are
  little-endian u32 indexed from the allocation offset: `0x7FFFFFFF` free,
  `0x80000000 | next` in a chain, `0xFFFFFFFF` for the last cluster.
- From cluster 41 on: the root directory (`.`, `..`, `BWNETCNF`), the save's own
  directory, then the file data, allocated in that order so a file's clusters stay
  contiguous, as the console's first-fit allocator leaves them.

A directory entry is zero-padded after its name, which is how the console leaves
them, and a reader walks names to the NUL, with an entirely `0xFF` slot after the
last entry to terminate. The name occupies 32 bytes at `0x40`.

The timestamp is PS2 time: eight bytes in Japan time, whatever the console is set
to (Ross Ridge, "PlayStation 2 Memory Card File System"): reserved, second,
minute, hour, day, month, then a little-endian year.

The 16 spare bytes of a written page are four 3-byte Hamming codes, one per
128-byte slice, then four zeroes. The algorithm is the one Sony's mcman, mymc and
PCSX2 all carry; the emulator never checks it, but the console can, so it is
computed rather than filled. A byte's column parity contribution is the odd
parity of the byte masked by the nth mask, for the code's seven masks, bits 3 and
6 always being zero, hence the `0x77`.

## The save, and why nothing of Sony's travels in it

`BWNETCNF` carries the network configuration in mode `0x842F`; its `0x08` bit
marks the directory copy-protected in the BIOS browser.

Inside: the index, `net000.cnf`, the two encrypted halves, and an icon pair
generated here rather than shipped. There is nothing of Sony's worth embedding:
`icon.sys` is 964 bytes of documented header fields, four corner colours, three
lights and an ambient, the title, and the icon name three times (normal, copy,
delete), and the icon itself is a single textured quad of one flat colour.

The icon format is a 20-byte header, a vertex block, a short animation section,
and a 128x128 BGR555 texture, 32,768 bytes uncompressed, which is why Sony's
`SYS_NET.ICO` weighs 33 KB. The compressed-texture option fits in two RLE passes
of a single texel, so the whole icon here is a few hundred bytes.

The title is the only personalised thing on the card: the player's profile name,
reduced to printable ASCII, `Emufii` if nothing survives.

## YNCF: a save can only be read back on the console that encrypted it

Three of the files are in the clear: the save index, `net000.cnf`, and the shared
38-byte header of the other two. The other two, `ifc000.dat` and `dev000.dat`,
are that same text put through a cipher locked to the console: Sony's netcnf
library derives a table of rotations from the 8-byte i.Link id, and encodes every
little-endian 16-bit word as `rotl16(word, rotation) xor 0xFFFF` (ps2dev/ps2sdk,
`netcnf.c`, encoding at :775, key init at :875).

The consequence, and the reason this file exists: there is no key material in the
file, and no checksum to fail loudly. A console that does not match decodes mush,
and the game reports the configuration as invalid.

The rotation table cycles with a period of 24 words (48 bytes), three rotations
per id byte, which is why two files encrypted under one console share their first
48 bytes as soon as their plaintexts do. The 24 rotations are `(b shr 5) + 1`,
`((b shr 2) and 7) + 1`, `(b and 3) + 1`, each from 1 to 8.

One divergence from ps2sdk, settled by measurement: its transcription initialises
only seven of the eight id bytes and lets two table slots run off the array.
Decoding the bench card under the plain eight-byte table reproduces every word of
both encrypted files, and that is the reading followed here.

## Which id to encrypt for

ARMSX2 answers the netcnf library's `sceCdRI` from the `.nvm` placed next to the
running BIOS, and both paths producing that answer converge on a single constant
(`pcsx2/CDVD/CDVD.cpp`):

- no readable `.nvm` -> `cdvdCreateNewNVM()` writes the dummy id
  `00 AC FF FF FF FF B9 86` (CDVD.cpp:158);
- a `.nvm` whose i.Link area looks unprogrammed (bytes 2 and 3 both zero) ->
  `sceCdReadILinkId` replaces the read with the same constant
  (CDVD.cpp:2621-2631).

A card encrypted for that id therefore works on any install whose NVRAM ARMSX2
made itself, the single-`.bin` BIOS import, which is the normal install. An
install that imported a real console's `.nvm` keeps that console's real id and
must be encrypted for it.

That is the flaw this replaces: the card Emufii shipped was encrypted for a bench
console's id, and worked nowhere else.

Two cautions when reading the NVRAM: callers must choose the layout explicitly
from the BIOS actually detected, since inspecting both offsets can silently pick
up stale bytes left in an unrelated area of an imported `.nvm`. And ARMSX2
discards the imported content and calls `cdvdCreateNewNVM()` again if the NVM is
short, if the language block is blank, or if the slim region block is.

## What the configuration says, and what it must not say

`type nic` plus `dhcp`, nothing else. The plaintexts are byte for byte the ones
the PS2 wrote on the bench (measured 2026-08-20, recovered by decoding the
shipped card): `dhcp`, no address, no name server, and for the "device" half, the
name of SCE's Ethernet adapter.

A PS2's static address is not this file's business in Emufii: ARMSX2's Local Link
runs its own DHCP server and hands each peer a distinct address derived from its
peer id (`pcsx2/DEV9/LocalLinkAdapter.cpp:167`). The console therefore asks for a
lease and the emulator tells it apart from every other player. A static IP
written by hand here would instead put every player on the same address.

## The folder's saves come onto the image, not the other way round

A PCSX2 "folder" memory card cannot carry the network profile: PCSX2 indexes it
filtered by the running game, and `BWNETCNF` matches no serial, so the profile
would be written where the console can never read it. The profile therefore lives
on a generated image in slot 1, and the remaining question is what becomes of the
player's saves.

Leaving the folder card in slot 2 is not the answer, and the reason deserves to
be stated precisely, because the obvious reading of the log is wrong. ARMSX2
opens the card before knowing what is booting:

```
McdSlot 0 [File]: EmuFii-Network.ps2 [8 MB, Formatted]
McdSlot 1: [Folder] /storage/emulated/0/Armsx2/memcards/MemoryCard
FolderMcd: Indexing slot 1 with filter "".
```

That empty filter is not a card the game cannot read: measured on the Thor on
2026-08-23, the game does find its profile there. What does not work is
everything else: the BIOS browser shows that card as empty, so a save cannot be
copied across by hand, and the two cards stay separate with no way to reunite
them. Copying the saves onto the card carrying the profile is what puts
everything in one place, the one the console agrees on.

## `_pcsx2_index` is read, never copied

The layout ARMSX2 writes:

```
memcards/<card>/_pcsx2_superblock
memcards/<card>/<SAVE>/_pcsx2_index
memcards/<card>/<SAVE>/<the save's files>
```

`_pcsx2_index` is PCSX2's bookkeeping and must never land in a card image: the
console knows nothing of it, and a save carrying one extra file is a save the
game may refuse.

What it is read for is the order of the files, the one the console wrote them in,
and the one a real card's directory carries. A file the index does not mention is
not thrown away: it comes after the others, alphabetically. Losing a byte of
somebody's save over a bookkeeping mismatch is not a trade worth making.

The file is a flow YAML mapping written by rapidyaml, not JSON, so it is read by
tolerant scanning rather than with a parser: the names carry dots and dashes, and
the only field that matters here is `order`.

## Operating on the player's card rather than handing them a new one

The player's source image is read, `BWNETCNF` inserted or replaced, and a new
byte array returned for the provisioning layer to publish as a clone. The input
array is never modified. Existing saves' payloads survive the filesystem rewrite
unchanged, so no BIOS copy ceremony is needed.

The card is read through its own superblock (geometry, indirect FAT, FAT chains,
root directory), never by assumption: an 8 MB card and a 64 MB one, a BIOS format
and a PCSX2 format, all declare themselves.

An existing `BWNETCNF`, if there is one, is freed: its file chains and directory
clusters returned to the FAT, its root entry compacted, the saves that followed
it moved up with their back references corrected. A fresh save is written for the
target console id, allocated at the first free FAT hole, fragmentation being of
no consequence, and every touched page is rewritten with its data and a
recomputed ECC.

No checksum exists anywhere in the format that would need maintaining, and the
superblock keeps no account of free space: nothing outside the FAT and the two
directories changes.

An entirely `0xFF` card, which is what ARMSX2 makes at install before the BIOS
has ever formatted it, has no filesystem to read. It is therefore formatted
first, with the generator's constants, at the size of the file received.

## Recovering the id of an already-written card

`recoverConsoleId` is a diagnostic and migration tool, not a normal path. Every
YNCF file starts with the same 38-byte header: a `BWNETCNF` the console wrote
therefore yields its own cipher stream, and that stream is worth three rotations
per id byte.

That covers the player who has already done a network configuration with any
compatible game. It must never override a contradictory identity proved by the
active NVM. A card this app has already written decodes under the same id, which
also makes the tool useful for validation.

## A prepared card is not verified byte by byte

That is what was done first, and it was wrong in a way that only shows in use: a
memory card is a living disc. As soon as a game saves, or ARMSX2 simply mounts
it, its bytes change, the checksum stops matching, and the player is told their
preparation has vanished while their card is right there, perfectly good, in the
right slot. It cost one player their PS2 games between two launches of the app.

What has to hold is narrower and survives normal play:

- slot 1 is still active and still names this card;
- the card is still there;
- the network configuration is still on it, and reading it back yields the
  console id it was written for.

That last point is the real proof: the save is encrypted per console, so
recovering the right id from it means both that our profile is present and that
it is in its place. New saves alongside are none of our business, and that is
precisely the point.
