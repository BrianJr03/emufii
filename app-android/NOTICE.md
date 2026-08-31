# Third-Party Notices: Android application

Third-party content shipped in the APK. Emufii's own licence, the copyright and
the full list of attributions are in the `NOTICE.md` and `LICENSE` at the
repository root.

Emufii is under AGPL-3.0 since 2026-08-09, replacing the Apache-2.0 that held
from 2026-07-28. Versions up to and including 1.10.8 stay under Apache-2.0.

## WireGuard (session network layer)

- Source: https://github.com/WireGuard/wireguard-android
- Artifact: `com.wireguard.android:tunnel`
- Licence: Apache License 2.0

Used through its userspace backend (`GoBackend`), which goes through Android's
`VpnService` and therefore needs no root. No native binary is vendored: the
library is an ordinary Maven dependency.

## Interface

AndroidX and Jetpack Compose, Coil, Haze, all under Apache License 2.0.

## CHD decompression

Aircompressor (`io.airlift:aircompressor`), under Apache License 2.0, provides
the pure-Java Zstandard decoder for PS2 CHDs. XZ for Java (`org.tukaani:xz`) is
public domain and provides their LZMA decoder. Emufii decompresses only the
hunks needed for `SYSTEM.CNF` and the boot ELF; no game image is extracted or
redistributed.

## Rounded M+ (M PLUS Rounded 1c)

Under SIL Open Font License 1.1. The licence text travels with the APK, at
`assets/ROUNDED-MPLUS-OFL.txt`, as the OFL requires of every copy of the font
software. Full attribution in the `NOTICE.md` at the repository root.

---

## History: why GPL v2 was considered, then dropped

Until 2026-07-28, Emufii shipped code derived from ZerotierFix (kaaass, GPL v2)
and ZeroTier One (BSL 1.1): Java stubs `com.zerotier.sdk.*` and a
`libZeroTierOneJNI.so`. That link is where the obligation to distribute Emufii
under GPL v2 came from.

All of that code was removed when the app moved to WireGuard: the `zt/` package,
the `com.zerotier.sdk.*` stubs, both `.so` files (6.7 MB) and the licence files
that came with them.

With the constraint gone, the licence was chosen freely: Apache-2.0 first, then
AGPL-3.0 on 2026-08-09, to make a hosted fork commercially pointless. The full
reasoning, including the options set aside, is in the root `NOTICE.md` and in
`docs/M10_LICENCES.md`.
