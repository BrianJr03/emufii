# NOTICE: Emufii

> Emufii was called EOEA until 2026-08-01. The old name survives in the git
> history, in the unit and path names already deployed on the server, and in the
> name of the signing keystore, all kept on purpose.

Copyright 2026 Emufii contributors

This product is distributed under the GNU Affero General Public License v3
since 2026-08-09. The full text is in `LICENSE`, at the root of the repository.

Versions up to and including 1.10.8 were released under Apache-2.0, and that
concession holds for those binaries; everything from 1.10.9 onwards is
AGPL-3.0.

> The copyright holder above is a placeholder wording: put in it the name the
> author wishes to be identified by.

## The licence decision

AGPL-3.0, settled on 2026-08-09, replacing the Apache-2.0 of 2026-07-28.
What moved the decision: the worry that a fork would live on the VPS and make
the Patreon disappear along the way.

What to keep in mind, in the order in which it really matters:

- The licence is not what protects the infrastructure. A legal text is
  argued in court, it does not filter. What protects the VPS is the
  coordinator's access control (`coordinator/client-auth.js`) and never
  publishing `coordinator/` or `relay/`: a fork ends up with a client and no
  network, and is left to stand up and pay for its own.
- The AGPL does not forbid forking, it makes forking commercially
  pointless. Whoever forks must republish their sources under the same
  licence, including when operating it as a network service. Nobody can close
  this code and resell it.
- The name and the logo are covered by no code licence. A legal fork must
  change its name. That was already true under Apache-2.0.
- The copyright stays with the project, so commercial licences outside the
  AGPL can be negotiated case by case.

What was ruled out: source-available (PolyForm-style), which would have required
written agreement for any modification. That is what was asked for at the
outset, but it is not open source and cannot be called that.

The original file, with the options weighed in July: `docs/M10_LICENCES.md`.

## Third-party contributions

### WireGuard: the sessions' network layer

- Source: https://github.com/WireGuard/wireguard-android
- Artifact: `com.wireguard.android:tunnel`
- Licence: Apache License 2.0

An ordinary Maven dependency, used through its userspace backend (`GoBackend`)
which goes through Android's `VpnService`. No native binary is vendored.

### Android libraries

AndroidX and Jetpack Compose, Coil, Haze (`dev.chrisbanes.haze`), and the Kotlin
standard library, all under Apache License 2.0.

### CHD image decompression

- Aircompressor (`io.airlift:aircompressor`), Apache License 2.0, a pure
  Java Zstandard decoder used to read the single boot ELF of PS2 CHDs.
- XZ for Java (`org.tukaani:xz`), public domain, the CHDs' LZMA decoder.

The images are never extracted and no emulator code is incorporated: these
libraries only present a few bytes of the file chosen by the player to the
identity computation documented in `docs/ARMSX2-AUTOCONFIG.md`.

### Coordinator

Express, MIT licence. The relay (`relay/`) has no dependency.

### Rounded M+ (M PLUS Rounded 1c)

`app-android/app/src/main/res/font/rounded_*.ttf`, Rounded M+ 1c, by the
Rounded M+ Project (`github.com/coz-m/MPLUS_FONTS`), under the SIL Open Font
License 1.1. Taken from Google Fonts, not from a third-party binary, then
cut down to Latin: the family covers the whole of Japanese, the app speaks
French and English, and the full version weighed 3.4 MB per weight.

It is a sans-serif with rounded terminals, the voice of handheld console menus,
and it is what the app's visual direction calls for. It has replaced Poppins
since 2026-08-22; versions up to 1.12.1 shipped Poppins (Indian Type Foundry,
Jonny Pinhorn, Ninad Kale), also under the OFL.

The SIL OFL demands more than attribution: its text must accompany the font
software, in every copy and therefore inside the APK. So it sits in two
places, `licenses/ROUNDED-MPLUS-OFL.txt` for whoever reads the repository, and
`app-android/app/src/main/assets/ROUNDED-MPLUS-OFL.txt` for whoever only has the
binary.
It is the only obligation a third-party contribution imposes on Emufii, and it
is met by those two files.

## What Emufii does not incorporate

Emufii contains no emulator code. Azahar, Dolphin and melonDS are launched
by intent, as third-party applications installed separately. Their respective
licences therefore do not reach up into this repository.

Likewise: no ROM, no BIOS, no keys. That is a project invariant.

## The bundled PS2 network configuration

Since 2026-08-20, Emufii bundles a PlayStation 2 memory card containing nothing
but a `BWNETCNF` save: the console's network configuration. That card was not
made here: it was created by ARMSX2, formatted by the emulated PS2, then
written by Midnight Club 3's network utility, and taken as is. That is what
guarantees it is valid without having to reimplement the PS2 cards' error
checking.

It is a deliberate redistribution, and the reasoning has three points. Without
that data, no PS2 LAN game opens its local menu: it is not a setting, it is
a save, and most games expect it without knowing how to create it. The utility
that writes it ships in only a handful of titles, so a player who owns none of
them has no way in. And what is reused is a network interface configuration, not
a BIOS nor code: it can be read in the clear for the most part.

The save's Sony icon (33 KB) is included, for want of being able to remove
it: the game reads the configuration without it, that is measured, but taking
it out would mean rewriting the card, and therefore recomputing an ECC that this
very choice exists to avoid implementing.

Should this choice be revisited, the entry point is `Ps2NetworkProfile` and the
`assets/ps2/emufii-ps2-net.ps2` asset; removing it would leave the feature in
place and merely deprive the player of the ready-made configuration.
