package landmark.data;

import java.util.List;

/** One filled area: ring 0 is the outline, further rings are holes. Rings are flat {@code x0,z0,x1,z1,...} block coordinates. */
public record Polygon(List<int[]> rings) {
	public Polygon {
		rings = List.copyOf(rings);
	}

	public int[] outline() {
		return rings.get(0);
	}
}
