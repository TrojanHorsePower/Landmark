package landmark.data;

import java.util.List;

/**
 * One filled area: ring 0 is the outline, further rings are holes. Rings are flat {@code x0,z0,x1,z1,...} block coordinates.
 * The outline's bounding box is worked out once, because the map asks for it every frame.
 */
public final class Polygon {
	private final List<int[]> rings;
	private final int[] bounds;

	public Polygon(List<int[]> rings) {
		this.rings = List.copyOf(rings);
		this.bounds = boundsOf(this.rings.get(0));
	}

	public List<int[]> rings() {
		return rings;
	}

	public int[] outline() {
		return rings.get(0);
	}

	/** {@code {minX, minZ, maxX, maxZ}} of the outline. The array is shared: do not modify it. */
	public int[] bounds() {
		return bounds;
	}

	private static int[] boundsOf(int[] ring) {
		if (ring.length < 2) {
			return new int[] {0, 0, 0, 0};
		}
		int[] b = {ring[0], ring[1], ring[0], ring[1]};
		for (int i = 2; i + 1 < ring.length; i += 2) {
			b[0] = Math.min(b[0], ring[i]);
			b[1] = Math.min(b[1], ring[i + 1]);
			b[2] = Math.max(b[2], ring[i]);
			b[3] = Math.max(b[3], ring[i + 1]);
		}
		return b;
	}
}
