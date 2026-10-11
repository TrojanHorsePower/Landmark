package landmark.waypoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import landmark.data.LandmarkConfig;
import landmark.data.WaypointSetMode;
import org.junit.jupiter.api.Test;

class WaypointSetsTest {
	private static ExternalWaypoint wp(String dim, String group, boolean active) {
		return new ExternalWaypoint(ExternalWaypoint.Source.XAERO, "w", dim, 0, 64, 0, 0xFFFFFFFF, group, active);
	}

	@Test
	void activeModeShowsOnlyTheActiveSet() {
		assertTrue(WaypointSets.shown(wp("d", "A", true), WaypointSetMode.ACTIVE, List.of()));
		assertFalse(WaypointSets.shown(wp("d", "B", false), WaypointSetMode.ACTIVE, List.of()));
	}

	@Test
	void allModeShowsEverything() {
		assertTrue(WaypointSets.shown(wp("d", "B", false), WaypointSetMode.ALL, List.of("xaero:d:B")));
	}

	@Test
	void pickModeHidesOnlyTheListedSetsSoNewOnesAppear() {
		var hidden = List.of("xaero:d:B");
		assertFalse(WaypointSets.shown(wp("d", "B", true), WaypointSetMode.PICK, hidden));
		assertTrue(WaypointSets.shown(wp("d", "New set", false), WaypointSetMode.PICK, hidden));
	}

	@Test
	void sourcesDoNotShareSetNames() {
		var jm = new ExternalWaypoint(ExternalWaypoint.Source.JOURNEYMAP, "w", "d", 0, 64, 0, 0, "B", true);
		assertFalse(WaypointSets.shown(wp("d", "B", true), WaypointSetMode.PICK, List.of("xaero:d:B")));
		assertTrue(WaypointSets.shown(jm, WaypointSetMode.PICK, List.of("xaero:d:B")));
	}

	@Test
	void sameSetNameInTwoDimensionsIsTwoSets() {
		var all = List.of(wp("o", "Zed", true), wp("o", "alpha", false), wp("o", "alpha", false), wp("n", "alpha", true));
		var g = WaypointSets.groups(all, "o");
		assertEquals(3, g.size());
		assertEquals("alpha", g.get(0).name());
		assertEquals("o", g.get(0).dimension());
		assertEquals(2, g.get(0).count());
		assertFalse(g.get(0).active());
		assertEquals("Zed", g.get(1).name());
		assertEquals("n", g.get(2).dimension(), "the other dimension's set comes last");
		assertEquals(0, g.get(2).count());
		assertTrue(g.get(2).activeAnywhere());
		assertEquals("xaero:o:alpha", g.get(0).key());
		assertEquals("xaero:n:alpha", g.get(2).key());
	}

	@Test
	void hidingASetInOneDimensionLeavesTheSameNameInAnotherVisible() {
		var hidden = List.of("xaero:o:Default");
		assertFalse(WaypointSets.shown(wp("o", "Default", true), WaypointSetMode.PICK, hidden));
		assertTrue(WaypointSets.shown(wp("n", "Default", true), WaypointSetMode.PICK, hidden));
	}

	@Test
	void convertedWaypointsKeepTheirOwnDimensionsSet() {
		var nether = wp(DimensionScale.NETHER, "Default", true);
		var conv = WaypointSets.viewable(List.of(nether), DimensionScale.OVERWORLD, true).get(0);
		assertEquals("xaero:minecraft:the_nether:Default", conv.groupKey());
		var g = WaypointSets.groups(List.of(conv), DimensionScale.OVERWORLD);
		assertEquals(DimensionScale.NETHER, g.get(0).dimension());
		assertEquals(1, g.get(0).count());
	}

	@Test
	void journeyMapGroupsAreNotSplitByDimension() {
		var a = new ExternalWaypoint(ExternalWaypoint.Source.JOURNEYMAP, "w", "o", 0, 64, 0, 0, "G", true);
		var b = new ExternalWaypoint(ExternalWaypoint.Source.JOURNEYMAP, "w", "n", 0, 64, 0, 0, "G", true);
		assertEquals("journeymap:G", a.groupKey());
		assertEquals(1, WaypointSets.groups(List.of(a, b), "o").size());
	}

	@Test
	void unknownModeFallsBackToActive() {
		assertEquals(WaypointSetMode.ACTIVE, WaypointSetMode.byId("nonsense"));
		assertEquals(WaypointSetMode.ACTIVE, new LandmarkConfig().waypointMode());
	}
}
