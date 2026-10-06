package landmark.data;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** A claim as shown on the web map. {@code owner} is only known when the map shows it. */
public record Land(String name, @Nullable String owner, List<String> members, int chunks, List<Polygon> polygons) {
	public Land {
		members = List.copyOf(members);
		polygons = List.copyOf(polygons);
	}

	/** Bounding box {@code {minX, minZ, maxX, maxZ}} over all outlines, or null if the land has no geometry. */
	public int @Nullable [] bounds() {
		int[] b = null;
		for (Polygon p : polygons) {
			int[] r = p.outline();
			for (int i = 0; i + 1 < r.length; i += 2) {
				if (b == null) {
					b = new int[] {r[i], r[i + 1], r[i], r[i + 1]};
				} else {
					b[0] = Math.min(b[0], r[i]);
					b[1] = Math.min(b[1], r[i + 1]);
					b[2] = Math.max(b[2], r[i]);
					b[3] = Math.max(b[3], r[i + 1]);
				}
			}
		}
		return b;
	}
}
