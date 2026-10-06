package landmark.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import landmark.data.Land;
import landmark.data.Polygon;

/**
 * Which land owns each 16x16 chunk, as a sparse set of {@value #TILE_CELLS}x{@value #TILE_CELLS}-chunk tiles.
 * Cell values are land index + 1 (0 = unclaimed). Used both for hit-testing (O(1)) and for painting claim textures.
 * Polygons are filled with the even-odd rule sampled at chunk centres, so holes are carved out.
 */
public final class ChunkOwnerGrid {
	public static final int CELL = 16;
	public static final int TILE_CELLS = 512;

	private final Map<Long, int[]> tiles = new HashMap<>();
	private final List<Land> lands;

	public ChunkOwnerGrid(List<Land> lands) {
		this.lands = List.copyOf(lands);
		for (int i = 0; i < this.lands.size(); i++) {
			for (Polygon p : this.lands.get(i).polygons()) {
				fill(p, i + 1);
			}
		}
	}

	public List<Land> lands() {
		return lands;
	}

	/** Index into {@link #lands()} of the land at this block position, or -1. */
	public int landIndexAt(int blockX, int blockZ) {
		int cx = Math.floorDiv(blockX, CELL);
		int cz = Math.floorDiv(blockZ, CELL);
		int[] t = tiles.get(key(Math.floorDiv(cx, TILE_CELLS), Math.floorDiv(cz, TILE_CELLS)));
		return t == null ? -1 : t[Math.floorMod(cz, TILE_CELLS) * TILE_CELLS + Math.floorMod(cx, TILE_CELLS)] - 1;
	}

	/** Calls {@code consumer(tileX, tileZ, cells)} for every non-empty tile; cells are row-major, {@code z * TILE_CELLS + x}. */
	public void forEachTile(TileConsumer consumer) {
		for (Map.Entry<Long, int[]> e : tiles.entrySet()) {
			consumer.accept((int) (e.getKey() >> 32), (int) (long) e.getKey(), e.getValue());
		}
	}

	@FunctionalInterface
	public interface TileConsumer {
		void accept(int tileX, int tileZ, int[] cells);
	}

	public int claimedCells() {
		int n = 0;
		for (int[] t : tiles.values()) {
			for (int v : t) {
				if (v != 0) {
					n++;
				}
			}
		}
		return n;
	}

	private void fill(Polygon poly, int value) {
		int[] outline = poly.outline();
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		for (int i = 0; i + 1 < outline.length; i += 2) {
			minX = Math.min(minX, outline[i]);
			maxX = Math.max(maxX, outline[i]);
			minZ = Math.min(minZ, outline[i + 1]);
			maxZ = Math.max(maxZ, outline[i + 1]);
		}
		int cx0 = Math.floorDiv(minX, CELL);
		boolean[] row = new boolean[(maxX - minX) / CELL + 2];
		for (int cz = Math.floorDiv(minZ, CELL); cz * CELL < maxZ; cz++) {
			java.util.Arrays.fill(row, false);
			double zc = cz * CELL + CELL / 2.0;
			// The outline is filled on its own and every hole ring is carved out on its own. Pl3xMap's outline can
			// already trace a notch around a hole and then list that hole again, so one even-odd pass over all
			// rings would fill such holes in again.
			mark(outline, zc, cx0, row, true);
			for (int r = 1; r < poly.rings().size(); r++) {
				mark(poly.rings().get(r), zc, cx0, row, false);
			}
			for (int i = 0; i < row.length; i++) {
				if (row[i]) {
					set(cx0 + i, cz, value);
				}
			}
		}
	}

	/** Sets (or clears) the cells of {@code row} whose centre lies inside {@code ring} at height {@code zc} (even-odd). */
	private static void mark(int[] ring, double zc, int cx0, boolean[] row, boolean on) {
		double[] xs = new double[16];
		int n = 0;
		int len = ring.length / 2;
		for (int i = 0; i < len; i++) {
			int j = (i + 1) % len;
			double z1 = ring[2 * i + 1];
			double z2 = ring[2 * j + 1];
			if ((z1 <= zc && zc < z2) || (z2 <= zc && zc < z1)) {
				if (n == xs.length) {
					xs = java.util.Arrays.copyOf(xs, n * 2);
				}
				xs[n++] = ring[2 * i] + (zc - z1) * (ring[2 * j] - ring[2 * i]) / (z2 - z1);
			}
		}
		java.util.Arrays.sort(xs, 0, n);
		for (int k = 0; k + 1 < n; k += 2) {
			int from = (int) Math.ceil((xs[k] - CELL / 2.0) / CELL) - cx0;
			int to = (int) Math.floor((xs[k + 1] - CELL / 2.0) / CELL) - cx0;
			for (int cx = Math.max(from, 0); cx <= to && cx < row.length; cx++) {
				row[cx] = on;
			}
		}
	}

	private void set(int cx, int cz, int value) {
		int[] t = tiles.computeIfAbsent(key(Math.floorDiv(cx, TILE_CELLS), Math.floorDiv(cz, TILE_CELLS)), k -> new int[TILE_CELLS * TILE_CELLS]);
		t[Math.floorMod(cz, TILE_CELLS) * TILE_CELLS + Math.floorMod(cx, TILE_CELLS)] = value;
	}

	private static long key(int tx, int tz) {
		return ((long) tx << 32) | (tz & 0xFFFFFFFFL);
	}
}
