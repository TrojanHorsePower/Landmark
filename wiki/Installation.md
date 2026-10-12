# Installation

You need [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft
version. Download the jar for your version from the [releases page](https://github.com/TrojanHorsePower/Landmark/releases)
and put it in your `mods` folder. It only loads on the client; nothing is needed on the server.

Each release has one jar per range of Minecraft versions. The Minecraft version is in the file name:
`landmark-<version>+mc<first>-<last>.jar`.

| Jar | Minecraft | Java | Fabric Loader | Fabric API |
|---|---|---|---|---|
| `…+mc1.21-1.21.1.jar` | 1.21, 1.21.1 | 21 or newer | 0.15.11 or newer | any |
| `…+mc1.21.2-1.21.4.jar` | 1.21.2, 1.21.3, 1.21.4 | 21 or newer | 0.16.9 or newer | any |
| `…+mc1.21.5.jar` | 1.21.5 | 21 or newer | 0.16.10 or newer | any |
| `…+mc1.21.6-1.21.8.jar` | 1.21.6, 1.21.7, 1.21.8 | 21 or newer | 0.16.13 or newer | any |
| `…+mc1.21.9-1.21.10.jar` | 1.21.9, 1.21.10 | 21 or newer | 0.17.2 or newer | any |
| `…+mc1.21.11.jar` | 1.21.11 | 21 or newer | 0.17.3 or newer | any |
| `…+mc26.1-26.1.2.jar` | 26.1, 26.1.1, 26.1.2 | 25 or newer | 0.18.4 or newer | any |
| `…+mc26.2.jar` | 26.2 | 25 or newer | 0.19.2 or newer | any |

- Use **one** Landmark jar. If you update Minecraft, replace the jar with the one for the new version and delete the old one.
- Newer Fabric Loader and Fabric API versions are fine. The loader numbers are what Fabric API and the matching
  [Mod Menu](https://modrinth.com/mod/modmenu) need, so a Mod Menu you have installed will not disagree with Landmark.
- Mod Menu, JourneyMap and Xaero's Minimap are optional.
- Snapshots, pre-releases and other mod loaders are not supported.

Next: [[Getting the map data]].
