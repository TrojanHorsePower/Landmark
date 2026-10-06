# Export zip format (version 1)

The mod does not talk to the web map itself. A player runs `tools/export.js` in a browser tab that already has the
Pl3xMap page open, which saves `landmark-export.zip`; the zip is then dropped onto the in-game map screen.

```
landmark-export.zip
  manifest.json
  lands.json              the map's claims layer, unmodified (tiles/<world>/markers/lands.json)
  tiles/<x>_<z>.png|jpg   optional zoomed-out map tiles
```

`manifest.json`:

| field | meaning |
|---|---|
| `format` | `1` |
| `world` | web map world name, e.g. `world` |
| `dimension` | Minecraft dimension id, e.g. `minecraft:overworld` |
| `source` | where the data came from, shown to the player |
| `exportedAt` | export time, epoch milliseconds |
| `tiles` | optional: `blocksPerTile` (power of two) and `pixelSize` |

Tile `x_z` covers blocks `[x * blocksPerTile, (x + 1) * blocksPerTile)` on each axis.
(Inferred from the map's own requests; verify against a known claim when first using real data.)

The importer treats the zip as untrusted: names are matched against fixed patterns and never used as paths,
sizes are enforced while reading, and an import that fails leaves the previous data untouched.
