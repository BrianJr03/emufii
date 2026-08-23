# PS2 multiplayer auto-configuration

## Result

Stock ARMSX2 can be configured by Emufii without root, a fork, private app data,
or accessibility navigation.

ARMSX2 loads a native per-game settings layer from its public data folder:

```
ArmSX2/gamesettings/<SERIAL>_<ELF_CRC>.ini
```

That layer is applied after global settings and before the VM and DEV9 adapter
start. Emufii writes the selected session and prepared network card there, then
launches the ROM directly with ARMSX2's explicitly named `ACTION_VIEW` activity.

Example guest file:

```ini
[DEV9/Eth]
EthEnable = true
EthApi = Local Link
LocalLinkHost = false
LocalLinkAddress = emufii
LocalLinkPort = 19072
LocalLinkRoomCode = ABC123

[MemoryCards]
Slot1_Enable = true
Slot1_Filename = EmuFii_Network.ps2
```

The host receives `LocalLinkHost = true` and no address. Emufii deliberately
does not write `LocalLinkPeerId`, preserving ARMSX2's device-unique value.

ARMSX2 source evidence, pinned to the version inspected:

- game-settings filename and uppercase eight-digit CRC:
  <https://github.com/ARMSX2/ARMSX2/blob/dbd7be271ca8a768df92027e91f25432e2a3b571/pcsx2/VMManager.cpp#L961-L969>
- per-game layer loading after the base layer:
  <https://github.com/ARMSX2/ARMSX2/blob/dbd7be271ca8a768df92027e91f25432e2a3b571/pcsx2/VMManager.cpp#L1134-L1156>
- Android/native names for DEV9 and Local Link settings:
  <https://github.com/ARMSX2/ARMSX2/blob/dbd7be271ca8a768df92027e91f25432e2a3b571/platforms/android/app/src/main/java/com/armsx2/config/Settings.kt#L897-L928>

## Disc identity

The suffix called CRC is not CRC32. PCSX2 and ARMSX2 XOR every complete
little-endian 32-bit word in the boot ELF and ignore a trailing one to three
bytes:

```text
crc = 0
for each complete u32le in BOOT2 ELF:
    crc = crc XOR u32le
```

Source:
<https://github.com/ARMSX2/ARMSX2/blob/dbd7be271ca8a768df92027e91f25432e2a3b571/pcsx2/Elfheader.cpp#L248-L258>

Emufii reads the ISO9660 primary volume descriptor, opens `SYSTEM.CNF`, parses
its `BOOT2 = cdrom0:\...;1` path, follows directories to the executable and
streams only that file through the XOR. The disc is never extracted.

The resulting serial and CRC are cached against the document URI, size and
last-modified value. A normal launch therefore does no disc scan and only writes
one small INI before opening ARMSX2.

## ISO and CHD coverage

Plain ISO is read through a seekable SAF file descriptor.

CHDv5 is exposed as the same 2048-byte ISO stream through a reusable random
reader. It parses the compressed hunk map once, stores map fields in primitive
arrays and caches the current decoded hunk. Supported paths include:

- raw and SELF-referenced hunks;
- Zstandard, zlib and LZMA DVD data;
- `cdlz` / `cdzl` raw-CD data;
- raw MODE1/MODE2 sector headers and subcode removal;
- FLAC constant-zero hunks used as the sparse target of SELF references.

Zstandard uses Aircompressor's pure-Java decoder. This is intentional: the
ordinary `zstd-jni` Maven artifact contains glibc desktop binaries rather than
an Android-compatible shared library. Arbitrary FLAC audio, Huffman data and
parent/delta CHDs are not guessed; if one of those appears in the boot ELF,
identity remains unknown and Emufii keeps the old accessibility path as a
compatibility fallback.

## Safe settings ownership

The writer performs a sparse, line-preserving merge. It owns only these keys:

- `DEV9/Eth`: enable, API, host role, guest address, port and room code;
- `MemoryCards`: Slot 1 enable and filename.

Unknown sections, comments, graphics settings, patches, speedhacks, Slot 2 and
`LocalLinkPeerId` survive. A staging document is written and read back before
the live per-game file is touched, then the published file is read back again.

The prepared card no longer needs to occupy global Slot 1. Its presence,
network save and BIOS/NVM identity remain verified, but each launch assigns it
only to the selected game. Other games keep the player's global card.

Do not rewrite the same game's INI while its VM is running. Emufii applies it
immediately before launch; later changes take effect at the next boot.

## Device and file proof

Validated on 2026-08-23 against an AYN Thor running Android 13 and ARMSX2
2.6.6.8.1 (`versionCode 1562`). A reversible probe showed ARMSX2 loading:

```
/storage/emulated/0/ArmSX2/gamesettings/SCUS-97481_2F123FD8.ini
```

and logging:

```
DEV9: Local Link client ready on port 19079 as peer 31337
```

The probe file was removed and the public settings mirror was unchanged.

The production readers were also run on temporary copies of two real files from
the Thor, and matched CRCs previously reported by ARMSX2:

- ISO, Splinter Cell Double Agent: `SLES-53826 / ABE3FDEA`;
- Zstandard DVD CHD, Midnight Club 3 Remix: `SLUS-21355 / 60A42FF5`.

The real CHD identity pass takes about 0.3 seconds on the development Mac and is
cached after the library scan. Conditional regression tests document how to run
the same checks against local commercial-disc fixtures without committing them.

## User flow

One-time setup still asks the player to select ARMSX2's custom folder and
prepares a network card. It no longer opens ARMSX2's global card manager and no
longer requires the accessibility service.

For a supported ISO or CHD session:

1. the guest waits until the host starts;
2. the player presses the single launch button;
3. Emufii verifies and merges the per-game file;
4. ARMSX2 opens the game directly with Local Link and the network card active.

The previous two-step settings-navigation UI appears only when a trustworthy
ELF CRC cannot be obtained.
