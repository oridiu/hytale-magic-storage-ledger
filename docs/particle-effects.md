# Ledger mist effects

## Why the effects appeared white

`TraceChoice` already resolves the selected item's colour through `ItemColors`
and passes the same tint to the chest markers and travelling wisps. Blocks use
their computed texture colour (or particle colour); other items use a dominant,
saturation-weighted icon colour. Dark colours are brightened without changing
their hue. Missing or still-loading icons fall back to the ledger's violet.

The four particle spawners explicitly set `Color: "#ffffff"` at animation frames
0 and 100. Hytale's runtime colour is a fallback for an unspecified particle
colour, so these white keyframes overrode the item's tint. Every explicit
particle colour has been removed, including on the glitter. See Hytale's
[WorldParticle colour contract](https://docs.hytale.com/assets/misc/worldparticle/).

## Changes

| Effect | Previous implementation | Revised implementation |
|---|---|---|
| Chest mist | Additive round glow sprites throughout the block | Alpha-blended smoke sprites on six thin faces outside each occupied block cell |
| Chest glitter | Large stars, some inverted scale ranges | Smaller, less frequent pinpricks with valid scale ranges and a short twinkle |
| Travelling mist | Straight samples every 100 ms, 2–3 glow sprites per sample | Samples every 50 ms, one smoke sprite and one tiny sparkle per sample |
| Motion | Straight line at constant speed; removed before reaching the endpoint | Gentle lateral curl and lift, tapered size at departure and arrival, final sample at the chest centre |
| Ending | Chest capped at 10 seconds; wisps launched for 12 seconds and could continue afterwards | Shared 10-second window; late waves launch only if they can arrive and dissolve in time |

The smoke uses four random frames from the existing `Smoke_Smooth2.png` atlas,
with random rotation, a little drift and a gradual fade. Alpha blending keeps
the body of the mist coloured; additive blending is limited to the small sparks.
Soft particles reduce hard intersections with geometry. The smoke emitters stop
at 8.6 seconds and the glitter at 9.2 seconds, leaving their last particles time
to dissolve by the end of the mark.

Wisps normally move at 3.5 blocks per second. Long journeys speed up enough to
take at most 4.5 seconds, including when the search range is increased. The curl
stays within 0.16 blocks sideways and 0.12 blocks upwards and vanishes at both
ends. This is a visual guide towards the chest, without obstacle avoidance.
The five-target limit, repeat launches from the player's current position,
coverage of both large-chest halves and private viewer list remain in place.

The configured chest particle cap is 26 per occupied cell (six smoke emitters
with three particles each, plus eight sparks), compared with 30 previously.
This is a configured bound, not a measured performance result.

## Verification

Checked on 2026-10-27 with Java 25 and Hytale Server 0.7.4:

- `gradlew.bat build --no-daemon`: passed, 13 tests, no failures.
- Hytale's `--bare --validate-assets --shutdown-after-validate`: exit code 0
  against the built plugin and a writable test copy of the installed vanilla
  asset archive. Existing vanilla warnings remain. The final run is recorded
  in `build/particle-asset-check/validation-copy.log`.
- Built artifact: `build/libs/MagicStorageLedger-1.0.0.jar`.

The unit tests decode the actual particle animation frames with Hytale's codec
and inspect the resulting packet colours. They require every colour to stay
unset, check fade endpoints and ensure the chest particles finish within the
mark's lifetime. Movement tests cover curling, tapering, exact arrival, vertical
and coincident targets, the late-wave cutoff and extended search ranges.

Visual acceptance in the Hytale client: **Not run**. Check a strongly coloured
item, a neutral item, a small chest, a large chest, vertically stacked chests,
movement between repeat launches, and a second player who must see no effects.
Assess saturation and smoke density in both daylight and a dark storeroom.
