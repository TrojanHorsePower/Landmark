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

Tile `x_z` covers blocks `[x * blocksPerTile, (x + 1) * blocksPerTile)` on each axis; `x` runs along image columns
and `z` along image rows, with no flipping. With the default `tileFolder` 3 this is 4096 blocks (8 blocks per pixel).

This was checked against a real export: the explored area's edges land on the expected world-border blocks, and claim
centres fall on water far less often than chance only under this orientation.

The importer treats the zip as untrusted: names are matched against fixed patterns and never used as paths,
sizes are enforced while reading, and an import that fails leaves the previous data untouched.

## Export quality presets

The script has two settings in its `CONFIG` block that decide the tiles: `tileFolder` (how zoomed out they are; each step down
doubles the detail and quadruples the tile count) and `tileMode` (`small` = JPEG re-encoded in the browser, `full` = PNG as
served). The mod's settings choose one of four presets and write those two values into the script when it is copied:

| Preset | `tileFolder` | `tileMode` | `blocksPerTile` in the manifest |
|---|---|---|---|
| Low | 3 | `small` | 4096 |
| Medium | 3 | `full` | 4096 |
| High | 2 | `full` | 2048 |
| Ultra | 1 | `full` | 1024 |

On the Chillzone map the measured sizes were about 8 MB (Low), 52 MB (Medium) and 172 MB (High); Ultra is about 300 MB or more. A
folder-0 export (512-block tiles, not a preset, about 6,400 tiles and 640 MB) also imports. The script builds the zip from browser Blobs rather than one big buffer, and the importer
allows up to 50,000 files and 3 GB, so every preset can be imported. When there are many tiles (more than 800) the map uses
smaller overview copies of them to keep memory use down.
