package landmark.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class LandSectionsTest {
	private static Polygon square(int x, int z, int size) {
		return new Polygon(List.of(new int[] {x, z, x + size, z, x + size, z + size, x, z + size}));
	}

	private static Land land(Polygon... ps) {
		return new Land("Test", null, List.of(), 0, List.of(ps));
	}

	@Test
	void areaSubtractsHoles() {
		Polygon p = new Polygon(List.of(new int[] {0, 0, 100, 0, 100, 100, 0, 100}, new int[] {10, 10, 30, 10, 30, 30, 10, 30}));
		assertEquals(10000 - 400, p.area(), 0.001);
		assertTrue(p.contains(50, 50));
		assertFalse(p.contains(20, 20), "inside the hole");
		assertFalse(p.contains(150, 50));
	}

	@Test
	void startsAtTheLargestSectionThenTheRestBySize() {
		Land l = land(square(0, 0, 16), square(5000, 0, 64), square(-9000, 300, 32));
		assertArrayEquals(new int[] {1, 2, 0}, l.sectionOrder(null));
	}

	@Test
	void startsAtTheSectionHoldingTheSpawn() {
		Land l = land(square(0, 0, 16), square(5000, 0, 64), square(-9000, 300, 32));
		assertArrayEquals(new int[] {2, 1, 0}, l.sectionOrder(new double[] {-8990, 310}));
	}

	@Test
	void aSpawnOutsideEverySectionFallsBackToTheLargest() {
		Land l = land(square(0, 0, 16), square(5000, 0, 64));
		assertArrayEquals(new int[] {1, 0}, l.sectionOrder(new double[] {2500, 2500}));
	}

	@Test
	void aSingleSectionIsJustItself() {
		assertArrayEquals(new int[] {0}, land(square(0, 0, 16)).sectionOrder(null));
	}
}
