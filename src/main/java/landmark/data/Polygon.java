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

	/** Area of the outline minus the holes, in square blocks. */
	public double area() {
		double a = 0;
		for (int r = 0; r < rings.size(); r++) {
			double ra = Math.abs(ringArea(rings.get(r)));
			a += r == 0 ? ra : -ra;
		}
		return Math.max(0, a);
	}

	/** Whether the block position is inside the outline and not inside a hole. */
	public boolean contains(double x, double z) {
		boolean in = false;
		for (int r = 0; r < rings.size(); r++) {
			if (inRing(rings.get(r), x, z)) {
				in = r == 0;
				if (r > 0) {
					return false;
				}
			} else if (r == 0) {
				return false;
			}
		}
		return in;
	}

	private static double ringArea(int[] ring) {
		double sum = 0;
		int n = ring.length / 2;
		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			sum += (double) ring[2 * i] * ring[2 * j + 1] - (double) ring[2 * j] * ring[2 * i + 1];
		}
		return sum / 2;
	}

	private static boolean inRing(int[] ring, double x, double z) {
		boolean in = false;
		int n = ring.length / 2;
		for (int i = 0, j = n - 1; i < n; j = i++) {
			double xi = ring[2 * i], zi = ring[2 * i + 1], xj = ring[2 * j], zj = ring[2 * j + 1];
			if ((zi > z) != (zj > z) && x < (xj - xi) * (z - zi) / (zj - zi) + xi) {
				in = !in;
			}
		}
		return in;
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
