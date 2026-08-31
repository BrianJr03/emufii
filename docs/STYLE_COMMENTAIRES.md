# What stays in the code, and what goes into `docs/`

Observation of 2026-08-24, raised by @22sh and measured: 11,132 comment lines for
22,172 lines of code, so 50%, across 159 files. Several files carry more comment
than code, `DolphinScreen.kt` 148%, `Gamepad.kt` 132%, `Console.kt` 106%,
`DiscImage.kt` 100%. A reader opening one of those files is not reading code, they
are reading a narrative with code embedded in it.

## The rule

The code says what is done. `docs/decisions/` says why, and what was tried before.

Three things only are allowed to stay as comments:

1. The KDoc of a public declaration: one to three lines, what the thing is and
   what it guarantees. No history, no justification.
2. A trap not deducible from the line, in one or two lines, when ignoring it
   breaks something: a mandatory call order, an API that lies, a measured value.
   If it takes more than two lines to explain, the line stays and the explanation
   goes into `docs/decisions/`, with a pointer.
3. A pointer, one line: `// pourquoi : docs/decisions/<file>.md#<anchor>`.

Everything else, the narrative of failed attempts, the design argument, the field
anecdote, the comparison between two visual worlds, goes into `docs/decisions/`.

## What is not a gain

Deleting a reason is not cleaning. Those reasons were paid for: they say what has
already been tried and why it failed, and that is what stops it being tried again.
The point is to move and index them, not to lose them. A comment removed without
its content turning up in `docs/decisions/` is a regression, not an improvement.

## The shape of an entry

One file per domain (`ui-bibliotheque.md`, `second-ecran.md`,
`identite-disques.md`, and so on). In each, one section per decision, with a
stable title serving as the anchor, which is what the code cites. Every section
says: the decision, what was tried before and why it did not do, and what would
break if it were reopened.

A title is not renamed lightly: it is cited from the code.

## Second pass, opened on 2026-08-31

The first pass brought the repository from 50% to 22.4% (8,751 comment lines for
39,086 of code). That is still 3.5 times the density of a hand-written project in
the same domain: measured on Eden-DS (`src/`, C++), 41,844 comment lines for
660,571, so 6.3%.

The rule above has not moved; what was missing was the form. What was left after
the first sort is still written as a plea, and that is what shows at a glance on
opening the repository:

- essay turns of phrase: rhetorical bold, the em dash, section titles in a file
  header, "X, not Y" constructions;
- justifying the options set aside (that is `docs/decisions/`'s job, not a file
  header's);
- dates and archaeology in a comment, which `git log` carries better;
- essay headers: `scripts/meta.mjs` opens on 45 lines with subheadings.

A hand-kept comment looks like this, and not like anything else:

```
// Only connect the minimum number of required players.
// max is used to zeroextend < 32 to 32, and > 32 to 64
```

One line, factual, stuck to what it explains. Their measured average: 50
characters. Ours: 62.

Target: under 10% per file, no header longer than 6 lines.
`scripts/comment-density.sh` measures and ranks, densest first; rerun it at the
start of every session to pick up where you left off. Once the head of the list is
down to constant files, move to `comment-density.sh -n`, which ranks by absolute
volume, and that is what is left to do, not the ratio.

The guard from the previous section still holds, and it is the most important one
here: move, do not lose. A paid reason that disappears without turning up in
`docs/decisions/` is a regression.

### Progress at 2026-08-31

| | start | now |
|---|---|---|
| comment lines (`app-android/app/src`) | 10,839 / 24% | 8,653 / 20% |
| `/** */` blocks per Kotlin file | 7.99 | 7.05 |
| French comment lines | 1,589 | 0 |
| em dashes | 276 | 0 |
| bold `**...**` | 190 | 0 |
| dates dating a code change | 21 | 0 |

Reference point: Eden-DS carries 0.56 blocks per file, zero em dashes over 41,844
comment lines, and 8 bolds.

162 files touched, 5,522 lines removed for 2,924 added.

What stays in French, and must: the `pourquoi : docs/decisions/x.md § <title>`
pointers, whose keyword is the repository's convention. The titles themselves are
English since the 2026-08-31 translation pass, and
`scripts/check-decision-links.mjs` checks all 741 pointers resolve. One other line
cites `« Créer une session »`, which is the interface's real label.

The dates kept date an observation an upstream update can make stale: "measured on
the Thor, 2026-08-23", "recorded on ARMSX2 2.6.6.7 with `uiautomator` on
2026-08-17". Those that dated a code change are gone, `git log` carrying them
better.

### What is left to do

1. The published `docs/` carried the same tics of form and were never measured.
   Done on 2026-08-31: `docs/decisions/` and this file are in English, with the
   741 code pointers rewritten and checked. The `docs/` phase journals followed.
2. The density tail: a hundred or so Kotlin files never opened. None exceeds 145
   comment lines. Pick up with `./scripts/comment-density.sh -n`.
3. The heaviest still left: `RomsRepository.kt` (135), `DiscImage.kt` (134),
   `Ps2NetplayDriver.kt`, `ChdImage.kt`, `CoordinatorClient.kt`.

### The guards in place

- `scripts/comment-density.sh` measures and ranks (`-n` for absolute volume).
- `scripts/check-decision-links.mjs` checks every `pourquoi :` pointer lands on a
  heading that exists, and exits non-zero if one misses.
- `scripts/retitle-decisions.mjs` rewrites the pointers after a heading is
  renamed.
- `.claude/hooks/rappel-commentaires.sh` (PreToolUse) states the rule before `.kt`,
  `.js` or `.mjs` is written.
- `.claude/hooks/delta-commentaires.sh` (PostToolUse) compares the file to HEAD and
  only warns on the signature of sterile shortening: lines down, blocks not.
