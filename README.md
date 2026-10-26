<p align="center">
  <img src="docs/images/banner.png" alt="Magic Storage Ledger: Find anything in your chests" width="100%">
</p>

# Magic Storage Ledger

Which chest held the copper again? Magic Storage Ledger is a Hytale server plugin for Chapter 1 (Update 7) that adds the **Magic Ledger**: a magic register that lists what the chests around you hold and shows you the way to the item you pick. No signs, no labels, no sorting by hand.

It belongs to the same family as [Magic Storage](https://github.com/oridiu/hytale-magic-storage) but works on its own; neither mod needs the other.

## Features

- **The book:** a teal and gold grimoire with its own model, texture and icon. You carry it like a spellbook; when you open it, you hold it open in both hands and read.
- **Crafting:** the Magic Ledger is crafted at the Goblin Tinkerer's Workbench in the Forgotten Temple, under **Gadgets**, from 10 Scraps, 2 Void Crystal Shards and 1 Runestone Shard. The workbench must be powered.
- **What is nearby:** walk up to your chests and right-click with the ledger. A window like the repair kit's lists every item in the chests around you, with how many of it they hold together, most first. Right-click again to close it.
- **Where it is:** click an item. Every chest nearby that holds it is wrapped in glittering mist in the item's colour for ten seconds, all around the chest, so you can tell which one it is even in a stack of chests. A large chest glitters over both halves. A wisp of mist also drifts slowly from you to each of the five nearest of these chests, again every few seconds from where you stand then, to show you the way. Only you see all of this.
- **Range:** by default the ledger searches as far as workbenches reach for materials: 14 blocks sideways and 6 up and down. Any chest that stores items counts: small, large, vanilla or modded.
- **Languages:** English and German.

## Installation

Magic Storage Ledger is a single server plugin with its assets bundled in the `.jar`. Players do not need to install anything.

1. Download `MagicStorageLedger-<version>.jar`.
2. Put it into the `mods` folder of your server.
3. Restart the server.

Requires Hytale server 0.7.4 or a later 0.7.x build.

## Configuration

On first start the server creates `mods/oridiu_MagicStorageLedger/config.json`:

| Setting | Default | Description |
|---|---|---|
| `HorizontalRadius` | `0` | How far the ledger searches sideways, in blocks (up to 64). `0` uses the workbench range |
| `VerticalRadius` | `0` | How far the ledger searches up and down, in blocks (up to 64). `0` uses the workbench range |

Operators (or anyone with the plugin's command permission) can change them in game:

| Command | Description |
|---|---|
| `/magicledger settings` | Opens a settings window |
| `/magicledger show` | Shows the current settings |

`/ml` is short for `/magicledger`.

## Building

Requires Java 25.

```bash
./gradlew build
```

The jar ends up in `build/libs/`. With a local Hytale install, `./gradlew runServer -Phytale_home=<folder containing install and UserData>` starts a dev server with the plugin on port 5520.

## License

MIT, see [LICENSE](LICENSE).
