# Disc identity: recognising a console in bytes

The narrative that lived in `library/DiscImage.kt`, taken out of the code on
2026-08-24 (see `docs/STYLE_COMMENTAIRES.md`). `CLAUDE.md` carries the summary;
here are the measurements. Headings are anchors cited from the code.

Every offset below was measured on real files present on this machine, never
copied off a wiki.

## Reading the bytes, and only to promote

The PSP had `.iso` first and does not give it back: a UMD rip and a GameCube
image share the extension and nothing else. Sorting them by name would be a coin
toss.

Hence the rule: the first bytes are read, and read only to promote a file.
Anything that cannot be positively identified stays what the extension already
said, which is why adding Dolphin cannot take a single game away from PPSSPP.
`null` is the ordinary answer for a PSP rip, and it is also the answer for a
truncated read or an unlisted format: all three mean the same thing to the
caller, leave the file where the extension put it.

Two checks that close the door on false positives:

- a PSP rip carries clean zeroes at `0x18` and `0x1C`, where the two Nintendo
  magics live (checked on three);
- an RVZ carries its disc type at `0x48` and a verbatim copy of the disc header
  at `0x58`, which is why the Wii magic appears at `0x70` (checked on four).

## The disc says what it is itself, at 0x8008

A PS2 disc and a UMD rip are both `.iso`, and on the Thor they look alike right
down to the filename. But the disc says so itself, measured on the real files:

```
TimeSplitters 2 (PS2) : system id 'PLAYSTATION'  volume id 'SLES_50877'
WipEout Pulse  (PSP)  : system id 'PSP GAME'     volume id 'SCEE'
```

The first data sector of a CD/DVD starts at `0x8000` (sector 16, of 2048 bytes):
the type and the `CD001` signature first, then the 32-byte system identifier.

The same rule applies wherever the descriptor starts. On an `.iso` it starts at
`0x8000`; on a sector extracted from a CHD, at 0, 16 or 24 depending on how the
disc was pressed. One rule, one place: a PS2 recognised through a compressed
container is recognised on exactly the same evidence as a PS2 read from a bare
file. A 2048-byte sector is already user data; a raw CD sector carries 16 bytes
of sync and header before it (MODE1) or 24 (MODE2 FORM1), and the PS2 pressing
measured here is MODE2, so both have to be tried. Measured: `CD001` falls at 24.

## `PLAYSTATION` is not enough: `BOOT2` is needed

A PS1 disc carries the same `PLAYSTATION` at that offset. That evidence alone
therefore does not settle the console: the reader goes on to require a `BOOT2`
entry in `SYSTEM.CNF`, which a PS2 disc has and a PS1 disc does not. Without it
the disc is refused rather than mislabelled: a PS1 CHD has already been listed as
a "PS2" game, from a folder that had nothing to do with the PS2.

## The PS2 serial is in `SYSTEM.CNF`, not in the volume identifier

The volume identifier used to serve that purpose, and it was the wrong field.
Measured on the eight PS2 discs on the bench: two carried a serial (`SLES_50877`,
`SCED_53990`). The other six said `MC3REMIX`, `FINAL_FANTASY_X`, `1_01`, or
nothing at all. A publisher writes what it likes there, and nothing obliges it to
write the disc number.

The serial all PS2 tooling actually uses is the boot file named in `SYSTEM.CNF`,
at the root of the disc:

```
BOOT2 = cdrom0:\SLES_537.17;1
```

That is what PCSX2 indexes its own database on, so it is also the only thing that
can match ours. Reaching it means walking the ISO9660, reading the primary
descriptor, following its root directory record, finding the file, which is a few
hundred bytes of reading, and the reason this function takes a random-access
reader where everything else works on a prefix.

`cdrom0:\SLES_537.17;1` is reduced to `SLES-53717`: the dot inside the number is a
filename convention and not part of the serial, and the underscore is how a serial
is written on a filesystem with no hyphen. Both are undone so the result is the
serial as it is written everywhere else, on the box, in PCSX2's index, in our
database.

The fallback is the volume identifier, never nothing. A disc that was
misidentified must not become a disc that is not identified at all.

The root record's offset is relative to the descriptor, not absolute: by that
point the descriptor has already been read into a buffer of its own. It was
`PVD_OFFSET + 156` at first, which indexed 32 KB into a 2 KB array, caught by the
walk test, which would otherwise have been the badge silently never appearing on
a PS2 game.

## An ISO walk needs a channel, and does not apply to CHD

A channel, not the sequential stream the rest of this class uses: the root
directory sits wherever the disc was mastered, several hundred megabytes further
in on a dual-layer game, and reading that far would mean reading the game.

For a bare image only. A CHD would have to decompress a hunk per seek, and the
sector it already reads for identification carries the volume identifier, which
stays the answer there.

A CHD announces itself in its first eight bytes, so nothing here depends on the
file being named `.chd`. It is on the other hand the only format that cannot be
handled by reading forward, its hunk map being near the end of the file, hence a
file descriptor and a channel. A provider that refuses to give one answers
`null`, and the file keeps its extension's console.

## Game identifiers, and what they are for

Nintendo: the six characters stamped at the very start of a disc header,
`RMGP01`, `GALE01`, the identity Dolphin itself sorts its games on, and the only
thing here that lets a guest recognise the host's game as one they own. A disc
image has neither SMDH nor banner at a fixed offset (an RVZ is compressed), so
the title still comes from the filename; this is the part that does not depend on
how somebody named their file. Read at the same base as the console: 0 on a raw
image, `0x58` in a compressed container.

PS2: the number is where the disc files it, not at the start of the file.
Measured `SLES_50877` on TimeSplitters 2, where ARMSX2 shows `SLES-50877`, the
same number bar the separator, so the guest will recognise the host's game as
their own emulator names it to them.

RVZ and WIA announce their console outright. The embedded header is checked as
well, and that is not redundant: it is what proves the file really is what its
`disc_type` claims, and it is the answer on a container whose type field this
build does not know. Both share a container: a file header, then a `WIADisc`
whose `disc_type` says which console it came from, then the first 128 bytes of
the original disc, verbatim, both readable without touching the compressed
payload.

## Which extensions are worth opening

`.iso`, because the PSP owns it and only the bytes can decide. Dolphin's own
extensions too, because they still have to say which of the two consoles they
are.

`.chd` since 2026-08-20, and it is the one that took real work: the PSP, the PS2
and the Dreamcast all ship in it, and the bytes that answer the question are
compressed. `ChdImage` decodes just enough of the container to return a sector,
which then goes through the same descriptor rule as everything else. A Dreamcast
disc is refused before any of that, on its GD-ROM metadata tag.

Deliberately absent: `.gcz`. It says GameCube or Wii in a subtype field this
project has no sample to check against, and guessing would risk moving somebody's
game to an emulator that cannot open it, which is worse than not listing a
format.

## The cost of all this, and why it is invisible when it fails

We read as far as the volume descriptor, not just the header: the GameCube and
Wii magics fit in the first 128 bytes, but the PS2 can only be recognised at
`0x8000`, where the ISO9660 starts. Hence a 32 KB read per sniffed file,
sequential, once, during the library's enrichment pass, which already opens every
3DS and DS file for its icon.

The array returned is truncated to what was actually read, and that is the
delicate point: a 32 KB array whose tail was unread zeroes would have
identification examine bytes that did not come from the file. A file shorter than
the header wanted is not a disc image at all, and returns nothing.

A provider that refuses the read answers `null`, which the scan reads as "keep
the extension's guess", exactly as for a PSP rip: that is why a failure here is
invisible rather than destructive.

---

# CHD: decoding just enough

Taken out of `library/ChdImage.kt`. Everything below was measured on two real
files, never taken from a wiki: a Dreamcast `Phantasy Star Online Ver. 2` and a
PS2 `Unreal Tournament`. Both are v5, `cdlz/cdzl/cdfl`, `hunkbytes 19584` over
`unitbytes 2448`.

## We stop at the sector, and decide nothing

`.chd` is the only container where the extension settles nothing: the PSP, the
PS2 and the Dreamcast all ship in it, and on this machine two of the three are in
neighbouring folders. Unlike an `.iso`, the bytes that would answer the question
are compressed.

This decoder therefore goes just far enough to return one disc sector, and no
further: no full extraction, no temporary file, a few hundred kilobytes read per
candidate. The measured PS2 file returns, at sector 16 offset 24, exactly:

```
CD001   system id 'PLAYSTATION'   volume id 'UT'
```

the same descriptor already read on a bare `.iso`. That is the whole reason for
stopping at the sector: the console is settled in one place for every disc
format, not two.

`null` is the ordinary answer for a GD-ROM, for a codec we do not decode, for a
CHD older than v5 and for any truncated file. All of them mean the same thing,
and never "this is not a PS2": "the bytes did not speak", which leaves the file
where its extension put it.

## The Dreamcast is ruled out before a byte is decompressed

It is the false positive to avoid: it is `unitbytes 2448` exactly like a PS2 CD,
and only the metadata tag tells them apart. Measured: the Dreamcast file carries
`CHGD "TRACK:1 TYPE:MODE1_RAW ..."` where the PS2 carries
`CHT2 "TRACK:1 TYPE:MODE2_RAW ..."`.

The metadata string is at `0x7c` on both measured files, just behind the header,
so it costs one short read and settles the console this project must never claim.

## A reusable reader, or the work explodes

The old "one sector" path re-decoded the Huffman map on every seek. A boot ELF
spreads over hundreds of seeks in a DVD CHD: that approach turns a few megabytes
into gigabytes of repeated map work. The map is therefore parsed once, and only
the most recently decoded hunk is kept, which is enough for the sequential reads
of PS2 identification.

## Two decoding traps that cost dearly

The first pass over the map cannot be cut short. The types of every hunk are
decoded before the first length is written: stopping at the hunk you want
therefore reads lengths from the middle of the type stream and produces offsets
that look plausible and decompress nothing. That mistake cost an afternoon; the
loop runs to the end deliberately.

MAME's canonical Huffman has two details that cannot be guessed, taken from
`huffman.cpp` rather than reconstructed: the repeat count comes from a third read
of the stream, and what is repeated is the length just read, not zero. Getting
either wrong still produces a tree, simply not one whose code lengths sum to 1,
which the decoder checks exactly, and refuses the file otherwise.

## What is decoded, and what is not

- On a raw CD, only the sectors are returned: the codec keeps the data and the
  subcode in two separately compressed blocks, and the subcode carries nothing
  that names a console.
- LZMA has no header: MAME compresses with `lc=3, lp=0, pb=2`, which fits in the
  properties byte `0x5D`, with a dictionary normalised to the next power of two.
  Checked against the real PS2 file, where the sector block decodes to exactly
  18,816 bytes.
- FLAC is recognised only for its constant-zero subframes. DVD CHDs commonly keep
  one entirely null canonical hunk in FLAC and refer to it throughout sparse
  files. Decoding arbitrary audio is useless to an ELF reader; recognising that
  case is enough to resolve those references, and everything else falls back
  cleanly rather than being guessed.
- The 2048-byte user data stream hides raw CD headers and the subcode. The PS2
  CDs on the bench are MODE2 (offset 24); MODE1 (16) and already-cooked sectors
  are detected too, from the descriptor signature.
