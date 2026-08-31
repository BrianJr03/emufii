# The tunnel: WireGuard, the single VPN slot, and the measurements

The narrative that lived in `wg/` and `tunnel/TunnelSlot.kt`, taken out of the
code on 2026-08-24 (see `docs/STYLE_COMMENTAIRES.md`). The relay-side network
traps are in `CLAUDE.md`. Headings are anchors cited from the code.

## Why Emufii has its own `VpnService`

`GoBackend` ships one already, so at first sight Emufii needs none. But it starts
it like this, read in the library sources before a line was written:

```java
context.startService(new Intent(context, VpnService.class));
```

`startService`, and `startForeground` is called nowhere in the library. The
tunnel would therefore live in a background service, which Android is free to
kill as soon as Emufii leaves the foreground, which is to say exactly when the
player switches to the emulator to play. This app has paid that bill once
already, on Dolphin's LAN service: without a foreground, the LAN segment dropped.

`GoBackend` is `final` and cannot be subclassed. `GoBackend.VpnService` is not,
and the `onCreate()` it inherits completes a static future that `GoBackend`
consults before starting anything. Hence the manoeuvre: subclass it, declare the
subclass in the manifest, and start it ourselves in the foreground. `GoBackend`
then finds the future already complete, skips its own `startService`, and works
through our instance.

This rests on an internal detail of the library, which is why the service owns
the tunnel's lifecycle rather than the manager: making the state change from
`onStartCommand` turns the order (`onCreate`, then `startForeground`, then the
state) into a property of the code rather than a hope about timing.

Corollary: the library's `onDestroy` must not be skipped. It brings the tunnel
down and resets the static future that lets `GoBackend` find this service;
skipping it would leave a future pointing at a dead instance, and the next tunnel
would never come up.

And swiping Emufii out of recents must bring the tunnel down: a foreground
service survives its task being removed by design, so without this the tunnel,
and the VPN key in the status bar, outlived the app with nothing left on screen
to stop them.

## The Wi-Fi lock is not a comfort detail

Measured between two distant Thors, through the relay: 25% loss at one ping per
second, 0% at three pings per second, and jitter from 46 to 369 ms. That is the
signature of Wi-Fi power saving: sparse traffic lets the radio doze, and rare
packets pay the wait or get lost.

And Switch LDN, which Eden upstream describes as extremely sensitive to latency
and loss, does its handshake with precisely those rare packets. A game that
connects then gives up after seven seconds, twice in a row, is exactly what a
handshake losing one packet in four produces.

`WIFI_MODE_FULL_LOW_LATENCY` does two things the old `HIGH_PERF` did not: it
turns power saving off and asks the driver to favour latency over throughput. It
only acts with the screen on and the app in the foreground, which during a game
means the emulator and not us, so the lock is held and the system applies it when
it can.

## Three numbers measured into the configuration

- The keepalive. Carrier NAT mappings expire well under a minute, and the relay
  can only reach a peer it has a mapping for. Lowered from 25 s on 2026-08-02:
  between two bursts the Wi-Fi radio falls asleep, and the wake-up packet paid up
  to 369 ms against 46 ms on a link kept awake.
- MTU at 1420. Without it the backend falls back to 1280, the IPv6 floor. 1420 is
  the wg-quick default and is safe here: the WireGuard header costs 60 bytes over
  IPv4, so the carrier packet is 1480 and crosses a 1500 link as well as a 1492
  PPPoE. Measured on the Thor on 2026-08-04: 1252 bytes get through, 1300 are
  lost, nothing fragments. That silent loss was the LDN failure mode.
- The topology is a star, so the configuration carries no other player's key: a
  client's only peer is the relay.

The configuration is rendered as text rather than through the library's builders:
one shape to get right, loggable when a tunnel refuses to come up, and it is the
format the WireGuard documentation uses.

## The host's second address, without which its packets are lost

The host has a second address at `.254`, null on a guest. The relay rewrites the
host's connection to itself there, and that is the one the ad hoc server hands
out. Without it here, packets sent to the host arrive through the tunnel and are
dropped.

## DNS is advertised for the PS2 only

Null everywhere else, deliberately: a VPN that advertises a DNS takes over name
resolution for the whole device. The other consoles dial addresses, not names.

The PS2 is the exception because the ARMSX2 keyboard has no full stop key: no
IPv4 address can be typed there. Local Link resolves names, and a single label is
enough, the relay answering that name with the sentinel.

## The WireGuard identity must persist

The reason is server side: the coordinator is idempotent on the public key, so
the same key always gets the same address. A key regenerated every session would
take a fresh address each time and leave the relay holding a route to a peer with
nobody behind it, which the other player sees as a game that connects and then
goes silent.

Kept in the app's private preferences, next to the profile and the friends list.
Not in the keystore: WireGuard needs the private key in the clear in user space
to do its handshake, so a hardware-backed key it could never extract would be
useless here. The app's private storage is the honest boundary, and it is already
the one the friend code rests on.

Clearing it goes with deleting the profile: the public key is a stable identifier
the coordinator sees, so leaving it behind would outlive the profile it came
from.

## "Online" means less than you think

`Tunnel.State` only distinguishes up from down, so "online" means the interface
exists, not that another player has joined, nor even that a handshake completed.
The app confirms real reachability by pinging the relay, which is why its address
is returned.

## Android has one VPN slot, and Emufii has two tunnels

The session tunnel, and the DNS tunnel that sends the DS to Kaeru. Whichever
calls `establish()` second wins, and the other is revoked without the app or the
player being consulted.

This is not a theoretical race: leaving the WFC screen with the system back
gesture leaves its tunnel standing, and creating a session afterwards cuts the DS
game off mid-play. It works the other way too, the session service being
`START_STICKY` and foreground, so it outlives the activity.

Who holds the slot is derived from the states the services already publish,
rather than tracked separately. Three rules:

- `Starting` counts as held. `establish()` may already have happened, and
  treating it as free is exactly the window where two tunnels collide. `Stopping`
  and `Error` do not count: the descriptor is on its way out, or was never
  opened.
- The session wins ties: an overlap means one of the two is a leftover being torn
  down, and the session is the one whose loss costs the player something.
- Asking for the slot you already hold is free: moving the session tunnel to
  another game is a restart, not a conflict.

## Swiping the app out of recents cuts the tunnel

Without this, the tunnel outlived the app indefinitely. `START_STICKY` made it
worse than a simple oversight: kill the process and Android brings the service
back, tunnel included, with no Emufii on screen to stop it. The key icon stays in
the status bar and the only way out is through Android's VPN settings.

`onDestroy` was not enough on its own: swiping the task away does not destroy a
started foreground service, which is the whole point of having one.
`onTaskRemoved` is the only signal Android gives for "the user is done with this
app", so that is where the decision belongs.

Deliberately unconditional. Swiping the app away while melonDS is still in
session will cut WFC name resolution under it, but a tunnel nothing can reach is
the worse of the two faults, and the emulator keeps its own task in recents, so
the gesture targets Emufii precisely.

`stopSelf` counts as much as cutting the tunnel: it clears the sticky restart, so
the service stays down instead of being resurrected.
