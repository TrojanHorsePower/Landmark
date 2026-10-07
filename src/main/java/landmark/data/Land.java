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
			int[] pb = p.bounds();
			if (b == null) {
				b = pb.clone();
			} else {
				b[0] = Math.min(b[0], pb[0]);
				b[1] = Math.min(b[1], pb[1]);
				b[2] = Math.max(b[2], pb[2]);
				b[3] = Math.max(b[3], pb[3]);
			}
		}
		return b;
	}
}
