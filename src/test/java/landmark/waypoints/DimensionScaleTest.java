package landmark.waypoints;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DimensionScaleTest {
	private static final String OW = DimensionScale.OVERWORLD;
	private static final String NE = DimensionScale.NETHER;
	private static final String END = "minecraft:the_end";

	private static ExternalWaypoint wp(String dim, int x, int z) {
		return new ExternalWaypoint(ExternalWaypoint.Source.XAERO, "w", dim, x, 64, z, 0xFFFFFFFF, "A", true);
	}

	@Test
	void netherToOverworldMultipliesByEight() {
		assertArrayEquals(new int[] {-800, 80}, DimensionScale.convert(-100, 10, NE, OW));
	}

	@Test
	void overworldToNetherDividesByEightRoundingDown() {
		assertArrayEquals(new int[] {12, -13}, DimensionScale.convert(100, -101, OW, NE));
	}

	@Test
	void theEndAndUnknownDimensionsDoNotConvert() {
		assertNull(DimensionScale.convert(1, 1, END, OW));
		assertNull(DimensionScale.convert(1, 1, OW, END));
		assertNull(DimensionScale.convert(1, 1, "minecraft:resource_world", OW));
		assertFalse(DimensionScale.pairs(OW, OW));
	}

	@Test
	void viewableIsUnchangedWhenOff() {
		var all = List.of(wp(OW, 0, 0), wp(NE, 10, 10));
		var out = WaypointSets.viewable(all, OW, false);
		assertEquals(1, out.size());
		assertNull(out.get(0).origin());
	}

	@Test
	void viewableAddsConvertedWaypointsWhenOn() {
		var all = List.of(wp(OW, 0, 0), wp(NE, 10, -5), wp(END, 3, 3));
		var out = WaypointSets.viewable(all, OW, true);
		assertEquals(2, out.size());
		var conv = out.get(1);
		assertEquals(80, conv.x());
		assertEquals(-40, conv.z());
		assertEquals(OW, conv.dimension());
		assertNotNull(conv.origin());
		assertEquals(NE, conv.origin().dimension());
		assertEquals(10, conv.origin().x());
		assertTrue(WaypointSets.viewable(all, END, true).size() == 1);
	}
}
