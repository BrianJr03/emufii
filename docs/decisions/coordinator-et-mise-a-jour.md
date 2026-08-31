# The coordinator, and updating the app

The narrative that lived in `network/CoordinatorClient.kt` and
`update/UpdateInstaller.kt`, taken out of the code on 2026-08-24 (see
`docs/STYLE_COMMENTAIRES.md`). Headings are anchors cited from the code.

## A token, because the session code is public

The finder publishes the code, so it cannot serve as authorisation. Without a
token, the routes that modify a session required nothing: anybody could rewrite
the host address of an unknown game, close it, or eject a player.

The token is returned at creation only, and never leaves the device. It is what
authorises, for instance, declaring that the host's room exists or no longer
does, and only the host has that answer, the coordinator being unable to see
inside the emulator.

Same reasoning one level down for members: the coordinator no longer publishes
friend codes in the member list, reading them being enough to follow anybody. The
id found there is therefore no longer ours, and it is the handle returned by the
heartbeat that has to be compared to recognise oneself.

The member token arrives only on the first heartbeat, the one that registers us;
afterwards the field is absent, deliberately. Returning it to whoever asks again
amounted to giving it to whoever knows an id.

## Telling "it does not exist" from "I could not ask"

The distinction matters to the player, not only to the log: a code that does not
exist is theirs to fix, an unreachable coordinator is ours. Conflating them,
which a bare `getOrNull()` did, told somebody whose network was down that their
friend's session did not exist.

## The defaults for an absent field are chosen in a precise direction

"Has the host opened their room?" is true when the field is missing, and the
direction is the whole point: the absence comes from an older coordinator that
does not know the question. The opposite default would have blocked every guest
until deployment, and a sequencing setting that stops people playing is worse
than the mess it fixes.

Conversely, the console is sent explicitly: the coordinator does not guess it, it
sees only a title and a titleId that 3DS and Switch write the same way. Saying
nothing means "no room".

And "private" is sent only when true: the coordinator treats absence as public,
so leaving the default unsaid avoids making a behaviour depend on a field the app
might one day forget.

A friend absent from a response is offline: this type has no "online" flag, their
presence is the signal. And only the codes we send can come back, there being no
listing route and no directory behind it.

## The Eden room on the VPS changes the shape of a Switch game

Instead of one player hosting on their phone and the other joining through the
tunnel, both join the same public room. The link "one player must be reachable",
the most fragile in the chain and the only one depending on the host's device,
disappears. Proved on 2026-08-05 before being written.

`null` for any other console, and `null` too when the coordinator has no room to
offer: the app then falls back on player hosting, which has not gone away. An
incomplete room counts as no room: all three fields are needed to dial, and
falling back on player hosting beats aiming at a guessed port.

## Claiming an address is idempotent on the key

Idempotent server side, so a retry after a lost response lands on the same
address rather than burning a second one and leaving the relay routing to a peer
with nobody behind it.

The profile id lets the coordinator recognise the host claiming its own address
and publish `host_ip` itself, so the app never has to report it back.

A field absent on a guest, and absent too from a coordinator older than
2026-08-03, means in both cases that the interface has only one address, which is
exactly the old behaviour. Testing for null covers both at once and avoids the
`optString` trap, which returns the string `"null"` for a JSON `null`.

## Presence outside a session, and why it goes out inside one

The session heartbeat already reports presence, and says which game is being
played. Moving to "outside a session" on leaving clears that at once, rather than
leaving friends watching a finished game.

---

# Updating

## Why this is acceptable when the S5 review excluded it

`docs/SECURITY_REVIEW.md` (S5) had decided: the app neither downloads nor
installs. The reasoning rested on one thing, that updating from a URL read off
the network is a code execution path, and the review assumes the network is
untrusted.

That path is reopened, and closed again by three locks, in this order:

1. The URL is not followed as it stands. Only the coordinator's host is accepted,
   over HTTPS. A `url` pointing elsewhere in `latest.json` is ignored:
   compromising the JSON is therefore not enough to have an arbitrary binary
   downloaded, you would already have to hold the server.
2. The signature decides, not the provenance. The downloaded APK is opened here
   and its certificate compared to that of the running application. A binary
   signed with another key is thrown away without ever being shown to Android.
   That is the lock that still holds on the day the server is no longer ours, the
   assumption the review stated explicitly.
3. Nothing starts without a press. The download begins when the player presses
   Install, never on its own.

On that third point, measured on the Thor and against expectation: Android shows
no confirmation box. Since Android 12, an app updating itself with the same
signature is installed without asking, and the session goes straight to
`INSTALL_SUCCEEDED`. The result receiver is still needed for all that: nothing
guarantees that shortcut from one version or manufacturer to the next, and where
it does not exist the button would visibly do nothing without it.

A consequence to own rather than hide: pressing Install is the only consent
collected. Lock 2 is what carries the security, not a system screen, and it is
stricter than what a browser would offer on the same link, since the refusal
happens before Android opens the file, with a message saying what happened rather
than "parse error".

## The central lock: two questions, and both must hold

The certificate is ours, and the version is the one announced. The second closes
off rollback, serving an old signed and therefore authentic version to bring back
an already-fixed flaw.

We compare certificates, not key pairs. `hasMultipleSigners` separates two worlds
that must not be mixed: an app with several signers has no rotation history, and
reading the wrong array returns an empty list, which would compare equal to
another empty list. Hence the explicit refusal when there is nothing to compare.

And intersection, not equality: after a key rotation the installed app knows its
history and the new APK carries only part of it. Requiring equality would fail
the one update you would really need to see succeed that day.

## Three outcomes, not two

The distinction was paid for: a boolean made a transfer that had started
perfectly and then stalled report "this version is not downloadable here yet".
The player went off looking for a binary missing from a server that was serving
it perfectly well.

60 s timeout: a 32 MB APK on a home network is not an API call. At 30 s a
transfer that stalled for a moment was abandoned, measured for real on the Thor,
`broken pipe` server side to the second.

The file goes into the cache: if the install succeeds it is of no further use,
and if it fails Android reclaims it by itself when space runs short. An APK
forgotten in the player's documents would be the only lasting trace of this
feature.

The size ceiling leaves headroom above the current 32 MB and stops a talkative
server filling the device's cache while we look away.

## Two refusals that are not errors

"Android does not let Emufii install applications yet" is not an error: it is a
permission to grant once, and the app opens the exact screen to do it.

A link pointing elsewhere is not treated as an attack: the field also serves to
publish a page to read, and the "View" button opens it in the browser, where the
player judges.

With no `url` published we fall back on the coordinator's `/download`: the server
that announced the version also serves it, which avoids having to keep two fields
consistent in order to publish.

`PackageInstaller` rather than `ACTION_INSTALL_PACKAGE`: the latter has been
deprecated since Oreo and demands a `FileProvider` plus URI permissions just to
name a file we already own.

## Signing the client changes the cost, not the identity

The coordinator's address travels in the clear inside the APK, a `strings` on the
dex is enough to read it, and the API required nothing of its callers. A session
could therefore be created in production with a plain `curl`, measured on
2026-08-09. Anybody holding the public APK could run their games on a VPS they do
not pay for.

This is not a proof of identity and cannot be one. The client is in the hands of
the very person we want to keep out: the key is in the binary, therefore
extractable, and claiming otherwise would be a lie. What the signature changes is
the cost: reading a URL is no longer enough, you have to take the APK apart, find
the key in it, and reimplement this computation. And since the key changes with
every version, the exercise has to be redone each time.

The rest of the defence is server side, where it really lives: the coordinator
logs the version calling it, which makes an out-of-date or foreign client
visible, and therefore blockable.

The shape: `HMAC-SHA256(secret, method + "\n" + path + "\n" + timestamp + "\n" +
SHA-256(body))`, in lowercase hex. The body enters the computation, without which
a signature valid for one request would be valid for any other at the same path.
The timestamp bounds the replayability of an intercepted signature to a few
minutes.
