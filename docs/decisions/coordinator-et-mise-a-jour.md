# The coordinator, and updating the app

The narrative that lived in `network/CoordinatorClient.kt`, taken out of the code
on 2026-08-24 (see `docs/STYLE_COMMENTAIRES.md`). Headings are anchors cited from
the code.

The updating half went with the feature on 2026-09-03: the app no longer
downloads, installs or announces a version. The file keeps its name, which
fourteen `pourquoi :` lines cite.

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
