package landmark.map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import landmark.data.Land;
import landmark.data.Pl3xLandsParser;
import landmark.data.Polygon;
import landmark.data.Pl3xLandsParserTest;
import org.junit.jupiter.api.Test;

class ChunkOwnerGridTest {
	private static Land land(String name, int[]... rings) {
		return new Land(name, null, List.of(), 0, List.of(new Polygon(List.of(rings))));
	}

	private static int[] square(int x, int z, int size) {
		return new int[] {x, z, x + size, z, x + size, z + size, x, z + size};
	}

	@Test
	void fillsChunksAndCarvesHoles() {
		var grid = new ChunkOwnerGrid(List.of(land("A", square(0, 0, 64), square(16, 16, 16))));
		assertEquals(16 - 1, grid.claimedCells());
		assertEquals(0, grid.landIndexAt(0, 0));
		assertEquals(0, grid.landIndexAt(63, 63));
		assertEquals(-1, grid.landIndexAt(20, 20), "inside the hole");
		assertEquals(-1, grid.landIndexAt(64, 0), "outside");
	}

	@Test
	void handlesNegativeCoordinatesAndTileBoundaries() {
		int edge = ChunkOwnerGrid.TILE_CELLS * ChunkOwnerGrid.CELL;
		var grid = new ChunkOwnerGrid(List.of(land("A", square(-32, -32, 64)), land("B", square(edge - 16, 0, 32))));
		assertEquals(0, grid.landIndexAt(-1, -1));
		assertEquals(0, grid.landIndexAt(-32, -32));
		assertEquals(-1, grid.landIndexAt(-33, -32));
		assertEquals(1, grid.landIndexAt(edge - 1, 0));
		assertEquals(1, grid.landIndexAt(edge, 0), "land B crosses a tile boundary");
	}

	@Test
	void lShapeAreaMatchesPolygonArea() {
		int[] l = {0, 0, 48, 0, 48, 16, 16, 16, 16, 48, 0, 48};
		var grid = new ChunkOwnerGrid(List.of(land("L", l)));
		assertEquals(3 + 2, grid.claimedCells());
		assertEquals(0, grid.landIndexAt(40, 8));
		assertEquals(-1, grid.landIndexAt(40, 24));
	}

	@Test
	void parsedSampleClaimsHitTest() throws Exception {
		var lands = Pl3xLandsParser.parse(Pl3xLandsParserTest.sample()).lands();
		var grid = new ChunkOwnerGrid(lands);
		assertEquals("Sample Land", lands.get(grid.landIndexAt(5, 5)).name());
		assertEquals(-1, grid.landIndexAt(20, 20), "hole in the sample land");
		assertEquals("Fake_Farm", lands.get(grid.landIndexAt(-90, -40)).name());
	}

	@Test
	void holeThatTheOutlineAlreadyTracesStaysEmpty() {
		// Shape seen on the real map: the outline walks around a one-chunk notch and the same hole is listed again.
		int[] outline = {0, 0, 48, 0, 48, 32, 32, 32, 32, 16, 16, 16, 16, 32, 32, 32, 32, 48, 0, 48};
		int[] hole = {16, 16, 32, 16, 32, 32, 16, 32};
		var grid = new ChunkOwnerGrid(List.of(land("Pinch", outline, hole)));
		assertEquals(-1, grid.landIndexAt(24, 24), "the hole must not be filled in again");
		assertEquals(0, grid.landIndexAt(8, 8));
		assertEquals(7, grid.claimedCells());
	}
}
