package landmark.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class SnapshotCodecTest {
	private static ClaimSnapshot snapshot(int... ring) {
		Land l = new Land("A", "B", List.of("B", "C"), 3, List.of(new Polygon(List.of(ring))));
		return new ClaimSnapshot("minecraft:overworld", 1234L, "test", List.of(l));
	}

	@Test
	void roundTripsChunkAlignedData() throws IOException {
		ClaimSnapshot in = snapshot(-32, 16, 48, 16, 48, 80, -32, 80);
		ClaimSnapshot out = SnapshotCodec.decode(SnapshotCodec.encode(in));
		assertEquals(in.dimension(), out.dimension());
		assertEquals(1234L, out.fetchedAtMillis());
		Land l = out.lands().get(0);
		assertEquals("B", l.owner());
		assertEquals(List.of("B", "C"), l.members());
		assertArrayEquals(new int[] {-32, 16, 48, 16, 48, 80, -32, 80}, l.polygons().get(0).outline());
	}

	@Test
	void roundTripsUnalignedDataLosslessly() throws IOException {
		ClaimSnapshot out = SnapshotCodec.decode(SnapshotCodec.encode(snapshot(1, 2, 3, 4, 5, 7)));
		assertArrayEquals(new int[] {1, 2, 3, 4, 5, 7}, out.lands().get(0).polygons().get(0).outline());
	}

	@Test
	void rejectsGarbage() {
		assertThrows(IOException.class, () -> SnapshotCodec.decode(new byte[] {1, 2, 3}));
	}
}
