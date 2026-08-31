# The player's identity, and what a dump says about itself

The narrative that lived in `profile/Profile.kt` and `library/RomTags.kt`, taken
out of the code on 2026-08-24 (see `docs/STYLE_COMMENTAIRES.md`). Headings are
anchors cited from the code.

## The friend code is the identity, and it is public by design

The id is a stable random value, derived from nothing about the device: it is
what the coordinator counts presence on, so it must survive a nickname change
without identifying the person beyond this app.

It also serves as the friend code, which is why it is short enough to read aloud.
That is deliberate: because the code carries the identity, adding a friend needs
no server-side directory. It means the id is public by construction, and it
always was in practice, since it travels with every session as a host or member
id.

There is no account and no server-side profile: the nickname travels with each
session as a plain string, and the picture never leaves the device. Other players
are therefore drawn as initials on a colour derived from their name, rather than
with an image that would have to be hosted, moderated and paid for. Uploading
real avatars is a product decision, not a missing feature.

The identity is durable but tied to the device: it lives here and nowhere else,
so a reinstall makes you a new person to your friends. Restoring it from one
device to another would need a recovery secret and somewhere to put it, which is
exactly the hosted account this design avoids.

Clearing the identity produces one unrelated to the old: anybody who had kept the
previous one no longer sees you, which is the point, and the only way out if a
code ends up somewhere unplanned. It also cuts you off from your own friends
list, so the caller must clear it and ask first.

## The nickname is constrained where it is entered

Azahar's netplay form refuses a nickname that is too short ("Invalid address or
name is too short!") and Emufii sends the profile name there as it is.

The constraint is applied where the name is entered, so the value on disk is
always usable, rather than patched at the point of use. A guard at storage stays
in place for callers that do not go through a form: nothing downstream should
have to wonder whether the stored nickname is acceptable to the emulator.

The minimum length was observed on the device, not read from a constant: the
validator lives in Azahar's DEX and its message does not carry the number.

The default nickname is a fixed sentinel, stored as it is and sent over the
network, rather than a resource: it is what "does it have a name?" compares
against, and it is already persisted on devices. Translation happens at display
time, which also gives you the other player's default name in your language.

## The avatar is copied, never referenced

Two reasons not to keep the original. The picker's SAF grant is not persisted, so
keeping the URI would leave a broken avatar after a restart. And a modern phone
photo is 50 megapixels: decoding a whole one to draw a 40 dp circle is how an app
gets killed for memory.

`inSampleSize` means the full image is never decoded: the decoder subsamples as
it reads.

---

# What a dump says about itself

## Nothing calls the network

A handheld on a train must be able to answer this question, and a fact that needs
the network to be read is a fact that disappears exactly when the player has time
to read it.

Two sources, in this order:

1. The serial or title id, when the console stamps a region into it. That is the
   dump's own word, taken from the disc or cartridge, and it survives a file
   being renamed, which players do constantly.
2. The filename tags, the No-Intro/Redump convention every set in the world is
   named after. Weaker, because it is only a name, but it is all a PS2 or PSP
   dump gives, their serials carrying unrelated numbers from one region to the
   next.

Unknown prints as nothing at all. A panel that inferred "USA" from silence would
be wrong for every European player of a game whose dumper skipped the tag.

And only the spellings the two big conventions actually use are accepted: a
looser match, any bracket containing a country name, would turn
`(Disney's Aladdin)` into a region, and a false fact printed in bold on a panel is
worse than a missing one.

## The Sony prefix is read letter by letter

The first letter says the medium (`S` a disc, `U` a UMD), the second the
publisher (`L` licensed, `C` Sony), and the third is the region: `U` America, `E`
Europe, `P`/`J` Japan, `K` Korea, `A` Asia.

Read that way rather than as a list of whole prefixes: that is how `ULUS-10041`,
every PSP game in the world, got no region while the PS2 had one.

## Region positions are repeated, not shared

They are the same ones the compatibility keys depend on, and they are
deliberately repeated because the two functions answer different questions: one
removes the region to build a key that outlives it, the other keeps it to display
it. Linking them would mean one changes the meaning of the other the day a
console is added.

The reading is also exposed without going through a `Rom`, so the rules can be
frozen by a unit test: a `Rom` carries a `Uri`, and `android.net.Uri` is a stub on
the desktop JVM, so a test that had to build one could not run where the rest of
these rules are checked.

## The revision was removed, and why

Removed on 2026-08-24 after reading a real library.

What a filename can yield is `Rev 1`, `Rev 2` and the Switch's `v0`, and none of
the three tells the player anything they can act on: `v0` is what every cartridge
is, and a revision number without the other revision to compare it against is a
fact about a pressing plant.

A real title version, the `1.0.2` an update installs, is not in the filename at
all; it is in the NSP metadata, which nothing here reads yet. Printing the weak
version because it was cheap was the mistake.

## A ROM yields several keys, never one

The whole difficulty of "does this game work" is not the verdict, it is saying
which game without asking anybody to name it. A verdict is given once and must
reach every copy of that game in the world, whatever the language of the player's
dump: a key must therefore survive a change of region.

Two console families, and they call for opposite treatment:

- The region is a character at a known position. 3DS, DS, GameCube and Wii stamp
  their region into one letter of an otherwise identical code
  (`CTR-P-ARRJ`, `ADAE`, `RMCP01`). Remove that letter and what is left is the
  game, for free, with no table to maintain and no game left out.
- The regions carry unrelated numbers. A PSP or PS2 release has nothing in common
  from one territory to another, `UCUS-98653` against `UCES-00842`, and no rule
  will ever relate them. Those match on the exact serial, and it falls to the
  database to list every serial a game was released under. The tool that writes
  the database resolves them from the public indexes; this file cannot, and must
  not pretend to.

Every ROM therefore yields several keys, a family key where one exists and always
the exact ids, and an entry matches if any one of them is listed. Belt and braces
on purpose: if a family rule one day meets a code it misreads, the exact id still
lands, and the failure costs a region rather than the whole game.

## The friend code alphabet, and its twelfth symbol

Crockford's base32 alphabet, which drops I, L, O and U. The first three because
they are re-read as 1 and 0 over the phone or from a screenshot; the U because
excluding it keeps unintended words out of generated codes.

Eleven random symbols make 2^55, far beyond any risk of two players colliding or
of somebody finding a live code by guessing, each attempt costing a request to a
rate-limited endpoint.

The twelfth symbol is a checksum, and it exists for a precise reason. Without it,
a typo is indistinguishable from a friend who simply has not opened the app: with
no directory to query, the app cannot tell "this code does not exist" from "they
are offline". The checksum lets it reject a mistyped code on the spot, rather
than recording a friend who will never connect.
