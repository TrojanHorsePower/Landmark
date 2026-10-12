# Waypoints from Xaero's Minimap and JourneyMap

If you use either mod, the waypoints you made there appear on Landmark's map as small diamonds in their own colour (hover for
the name and position). Nothing to set up, and neither mod is required.

- **JourneyMap** is read through JourneyMap's own plugin API, so it needs no files and works with the version you have.
  On exactly Minecraft 1.21, JourneyMap's only build does not call plugins, so its waypoints are not shown there (1.21.1 and
  newer are fine).
- **Xaero's Minimap** has no API, so Landmark reads its waypoint files from your game folder
  (`xaero/minimap/Multiplayer_<server>/`). Only the folder for the server you are connected to is read, and only waypoints
  that are switched on in Xaero's. The server folder is matched by address, so if you joined with an unusual address and no
  waypoints appear, check that first.

## Sets and groups

By default only the set Xaero's is showing (its active set, per dimension) or the JourneyMap groups that are switched on are
drawn, as in those mods. The **Waypoints** button in the side panel (shown when a waypoint mod has waypoints) opens the sets
that have waypoints in the dimension you are viewing, with counts: tick the ones you want, or choose **All sets**. It has the
same Auto / Overworld / Nether / The End button as the map (one shared setting). The same choice is in [[Settings]].

A set called "Default" in the Overworld and one in the Nether are separate sets.

## Overworld and Nether (off by default)

Turn on *Overworld and Nether waypoints on both maps* (Settings, or the "Also show Nether waypoints" button on the sets
screen) to see Nether waypoints on the Overworld map and the reverse, with coordinates scaled by 8. They are drawn as rings,
and the tooltip gives their real position. Their sets are listed under their own heading ("From The Nether"), so same-named
sets never mix. The End and modded dimensions are not converted.

![The waypoint sets screen, with the Nether's sets under their own heading](https://raw.githubusercontent.com/TrojanHorsePower/Landmark/main/docs/images/waypoint-sets.png)

Waypoints stay visible when you hide claims, because they are not claims. Both mods are read-only: Landmark never changes or
uploads your waypoints, and none of their code is included in Landmark.
