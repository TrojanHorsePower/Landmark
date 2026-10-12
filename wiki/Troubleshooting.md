# Troubleshooting

## The game says the mod needs a different Minecraft, Java or Fabric Loader

- Each jar covers a range of Minecraft versions: use the one whose name lists yours (see [[Installation]]) and keep only one
  Landmark jar in `mods`.
- Minecraft 26.x needs Java 25. 1.21.x needs Java 21 or newer.
- If the message names **Mod Menu** or **Fabric API** rather than Landmark, that mod needs a newer Fabric Loader or a version
  built for your Minecraft. Landmark's loader minimum for each jar is the higher of what Fabric API and Mod Menu need.

## The map is empty, or says "No claim data"

- You need to import the export once: see [[Getting the map data]].
- The web map only exports the Overworld. The Nether and The End show "No claim data" unless you import data for them.
- Claims change on the server over time; re-export now and then. The map shows how old its data is.

## No waypoints are shown

- Check that "show Xaero's / JourneyMap waypoints" is on in [[Settings]].
- By default only the set Xaero's is showing (or the JourneyMap groups that are switched on) is drawn. Open the **Waypoints**
  button and pick more sets, or choose All sets.
- Xaero's: the folder is matched by server address; an unusual address can miss it. Only waypoints switched on in Xaero's are
  read.
- JourneyMap on exactly Minecraft 1.21 does not call plugins, so nothing is shown there.
- You may be looking at another dimension: the sets and waypoints follow the Auto / Overworld / Nether / The End button.

## The map is slow

- Turn off the outline around every non-open land in [[Settings]] (it is off by default).
- A lower export quality means smaller and quicker data.

## Reporting a problem

Open an issue at <https://github.com/TrojanHorsePower/Landmark/issues> with your Minecraft version, the jar file name and
your `logs/latest.log`. Please remove anything private from the log first.
