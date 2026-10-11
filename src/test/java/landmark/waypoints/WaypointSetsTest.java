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
		assertTrue(WaypointSets.shown(wp("d", "B", false), WaypointSetMode.ALL, List.of("xaero:B")));
	}

	@Test
	void pickModeHidesOnlyTheListedSetsSoNewOnesAppear() {
		var hidden = List.of("xaero:B");
		assertFalse(WaypointSets.shown(wp("d", "B", true), WaypointSetMode.PICK, hidden));
		assertTrue(WaypointSets.shown(wp("d", "New set", false), WaypointSetMode.PICK, hidden));
	}

	@Test
	void sourcesDoNotShareSetNames() {
		var jm = new ExternalWaypoint(ExternalWaypoint.Source.JOURNEYMAP, "w", "d", 0, 64, 0, 0, "B", true);
		assertFalse(WaypointSets.shown(wp("d", "B", true), WaypointSetMode.PICK, List.of("xaero:B")));
		assertTrue(WaypointSets.shown(jm, WaypointSetMode.PICK, List.of("xaero:B")));
	}

	@Test
	void groupsAreCountedPerDimensionAndSorted() {
		var all = List.of(wp("o", "Zed", true), wp("o", "alpha", false), wp("o", "alpha", false), wp("n", "alpha", true));
		var g = WaypointSets.groups(all, "o");
		assertEquals(2, g.size());
		assertEquals("alpha", g.get(0).name());
		assertEquals(2, g.get(0).count());
		assertEquals(3, g.get(0).total());
		assertFalse(g.get(0).active(), "not active in the viewed dimension");
		assertTrue(g.get(0).activeAnywhere());
		assertEquals("Zed", g.get(1).name());
	}

	@Test
	void unknownModeFallsBackToActive() {
		assertEquals(WaypointSetMode.ACTIVE, WaypointSetMode.byId("nonsense"));
		assertEquals(WaypointSetMode.ACTIVE, new LandmarkConfig().waypointMode());
	}
}
