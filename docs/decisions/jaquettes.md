# Game artwork: where it comes from, and what wins over what

Taken out of the code on 2026-08-29 (see `docs/STYLE_COMMENTAIRES.md`). The
headings are anchors cited from the code: do not rename them lightly.

## The icon, never the box art

A ROM carries only a tiny icon, 32x32 on DS and 48x48 on 3DS, and blown up to
tile size it is the first flaw anyone sees on opening the app. SteamGridDB
publishes high-resolution versions of these, made and rated by a community.

We take the icons, not the box art, although the same service serves both. Box
art is 2:3: adopting it would mean turning the whole grid into vertical tiles,
which means giving up the "3DS menu" target, whose tiles are square. The icon
drops into the existing tile without changing anything else, and the gain we are
after, sharpness, is the same.

## Nothing is packaged into the APK

These images belong to their publishers: the app downloads them at runtime, on
the player's device, and keeps them in its local cache. That is what every
launcher does, and it is the difference between displaying an image and
redistributing it.

## Every player brings their own key

A key frozen into the APK would be the same for everyone: extractable by opening
the package, and it would be the author's account carrying the quota and the
abuse of the whole installed base.

Without a key the feature does not exist: no request goes out and the tiles keep
their embedded icon. That is not a failure, just a library without remote icons.

## The order of sources, and what wins

Strongest to weakest:

1. The image the player chose. When somebody has taken the trouble to correct
   something, correcting them back would be the worst possible behaviour.
2. Cocoon, when its folder is linked: those images are on the device, were
   downloaded for those very files, and in places cropped by hand. Preferring a
   fresh guess from a catalogue over an image somebody already chose would be
   taking the problem backwards.
3. The catalogue (SteamGridDB), if a key is given.
4. The ROM icon, which never goes away.

## The grid opens complete, or it fills in under the player's eyes

The loading screen used to wait for the ROM walk and then hand over: the grid
appeared, and filled in under the player's eyes. Three things arrived late, and
none of the three was the walk.

1. The index of local images, built console by console at the first tile that
   asks for one. It is a SAF folder enumeration, the slowest thing in the app:
   the first 3DS tile paid for the whole 3DS index, the first PS2 tile for the
   whole PS2 index, and it showed row by row.
2. The address of each piece of artwork, resolved per tile as it composed.
3. Image decoding, done at paint time.

All three now happen while the logo holds the screen anyway. After that the
tiles find everything cached and paint on the first frame.

None of this warm-up is essential. Every step is wrapped: an unreadable folder,
no network or an unexpected format must let the app open exactly as before, with
late images. A warm-up that could keep you out would be worse than no warm-up.
