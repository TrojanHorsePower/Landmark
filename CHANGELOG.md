# Changelog

All notable changes to Landmark. Dates are the release dates (UTC). Each version has a
[GitHub release](https://github.com/TrojanHorsePower/Landmark/releases) with the jars.

## [0.3.1] - 2026-10-15

### Added
- **Minecraft 26.3.** A new jar, `landmark-<version>+mc26.3.jar` (Java 25, Fabric Loader 0.19.3 or newer). Minecraft 26.3
  moved from GLFW to SDL for windowing and input and renamed some rendering classes, so the 26.2 jar does not load on it.

### Fixed
- Mouse input on Minecraft 26.3: its mouse buttons start at 1 instead of 0, so left clicks were ignored (no panning,
  no selecting or teleporting). Left-click checks now use Minecraft's own constant on every version.

### Changed
- Key handling in the common code uses Minecraft's own key constants instead of GLFW's, so it follows the key space of
  whichever version is running.

## [0.3.0] - 2026-10-11

### Added
- **Minecraft 1.21 to 26.2.** One jar per range of Minecraft versions, named
  `landmark-<version>+mc<first>-<last>.jar` (eight jars cover all 16 releases). Each jar declares the Java version and the
  Fabric Loader minimum its Minecraft versions need, down to Loader 0.15.11 and Java 21 for 1.21.x.
- **Choose which waypoint sets are shown.** A **Waypoints** button in the side panel (shown when you have Xaero's Minimap
  or JourneyMap waypoints) opens a list of your sets and groups with a count for each. By default only the set Xaero's is
  showing, or the JourneyMap groups that are switched on, are drawn. Tick the sets you want, or switch to *All sets* on the
  screen or in Settings.
- **Sets are per dimension, like in Xaero's.** The sets screen has the same Auto / Overworld / Nether / The End button as
  the map (one shared setting) and lists the sets of the dimension being viewed. A set called "Default" in the Overworld and
  one in the Nether are separate sets.
- **Overworld and Nether waypoints on both maps** (off by default; Settings, or a button on the sets screen). Nether
  waypoints appear on the Overworld map with coordinates scaled by 8, and Overworld ones on the Nether map divided by 8,
  drawn as rings. Their sets are listed under their own heading.
- Waypoint tooltips show the Xaero's set or JourneyMap group.
- The build, CI and release workflows cover every Minecraft version range; the README has a table of jars with the
  Minecraft, Java and Fabric Loader requirements.

### Changed
- The jar file name always includes the Minecraft version, also for 26.2.
- The Fabric Loader requirement is the lowest each Minecraft version can run (or Mod Menu's, where that is higher) instead
  of a single high minimum.
- README screenshots retaken, plus one for the waypoint sets screen.

### Fixed
- Rendering and input differences between Minecraft versions found while adding the older ones (map tiles on 1.21.1, the
  background covering the map on 1.21.5, the settings list's scroll position).
- A waypoint note in Settings sat under the wrong row.

### Notes
- On exactly Minecraft 1.21, JourneyMap's only build does not call plugins, so JourneyMap waypoints are not shown there
  (1.21.1 and newer are fine). Xaero's Minimap waypoints are unaffected.

## [0.2.1] - 2026-10-10

### Added
- **Dimension button** in the side panel: cycles Auto (follows the dimension you are in), Overworld, Nether, The End. The
  choice is remembered between openings and restarts.

### Fixed
- Large exports could not be imported: zips with more than 5,000 files were rejected; the limit is now 50,000 (the 3 GB
  size limit is unchanged).
- The Ultra export preset's description in Settings shows the measured size instead of an old estimate.

### Changed
- README screenshots retaken from the current version.

## [0.2.0] - 2026-10-07

### Added
- **Settings screen**, from the map's Settings button or the mod's page in Mod Menu (optional): the keybind (same setting
  as Options > Controls, with a conflict check), feature toggles, every colour the mod draws with (colour picker, hex box
  and Reset), and an export quality choice (Low, Medium, High, Ultra) that sets the tile detail of the export script.
- **Hide claims** button: shows bare terrain and hides claims and spawn pins.
- **Waypoints from Xaero's Minimap and JourneyMap** are shown on the map (on by default, optional; neither mod is
  required).
- **Delete all** button for saved spawn locations, with a count; turning "Remember spawn locations" off hides the saved
  pins.
- Optional outline around every non-open land (off by default, as it can slow the map with thousands of lands).
- Drop the export zip directly on the help page.

### Changed
- "Other lands" is one plain, editable colour instead of a hue per land (a previously changed value resets).

### Fixed
- Text overlapping or cut off on small windows and large GUI scales; the help page scrolls when it does not fit.
- One unreadable Xaero waypoint file hid the waypoints from the others.

## [0.1.1] - 2026-10-06

### Added
- Mod icon, a demo-data generator, and screenshots in the README.

### Changed
- README polish; removed the demo video link.

## [0.1.0] - 2026-10-06

First release. A client-side Fabric mod for Minecraft 26.2.

### Added
- Map of land claims (press `M`, rebindable): claims with a public spawn are highlighted; click one, confirm, and the mod
  runs `/lands spawn <name>`.
- Live open-spawn list from the server's own command suggestions, with no setup.
- Claim shapes, names and terrain from an export of the web map: an export script you run once in your own browser console,
  a zip importer (drop the zip on the Minecraft window) and a documented format.
- Map zoom and pan, including zooming out to the whole world; search; claim tooltips.
- Help screen walking through the export, and learned spawn points (pins at spawns you arrived at).
- MIT license.

### Fixed
- Empty map after returning from child screens, lag when zoomed far out, and missing members in tooltips.

[0.3.1]: https://github.com/TrojanHorsePower/Landmark/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/TrojanHorsePower/Landmark/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/TrojanHorsePower/Landmark/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/TrojanHorsePower/Landmark/compare/v0.1.1...v0.2.0
[0.1.1]: https://github.com/TrojanHorsePower/Landmark/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/TrojanHorsePower/Landmark/commits/v0.1.0
