# Friends, presence, and what we dare announce

Taken out of the code on 2026-08-29 (see `docs/STYLE_COMMENTAIRES.md`). The
headings are anchors cited from the code.

## What background watching can promise, and what it cannot

Emufii installs outside any store and has no push service behind it: nothing on
a server can wake this app. The only honest mechanism left is to ask, now and
then, from the device itself.

Android's floor for periodic work is fifteen minutes, and Doze stretches it
further on a phone in a pocket. An alert about a friend can therefore arrive a
quarter of an hour after they did, sometimes more, and a friend who plays for
ten minutes may never be announced at all.

That is a real limit, and it is written into the settings copy rather than
hidden. A feature that quietly delivers less than it promised teaches people to
distrust every notification the app will ever send. What it does deliver
reliably is slow news: a new version, and a friend settling in for an evening.

`JobScheduler` rather than WorkManager: WorkManager would bring a dependency, a
database and a hundred kilobytes for a periodic task with no chaining, no
constraint beyond the network, and no result to observe. The platform scheduler
does exactly this job.

## The announcement rules, each earned by picturing the notification it avoids

Comparing two polls is a pure function, and that is the point: the same function
serves the in-app alert and the background job, so what the two announce cannot
drift apart. It is also the only part of this feature that can be tested without
a device.

- A friend never seen before produces nothing. The first poll after adding one,
  or after the app was killed for a day, would otherwise announce the whole list
  at once as though everybody had just arrived.
- Coming online is announced once. If they are already in a game at that moment,
  the game is what gets announced, not both.
- Starting a game is announced even for someone already online. That is the case
  that really counts: they are there, and now there is something to join.
- A friend already in that same game produces nothing, however many polls go by.
