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

	/**
	 * The order to visit the separate sections of a land in: first the one containing the given position (a known spawn),
	 * or the largest if there is none or it is not inside any section; then the rest, largest first. Entries are indexes into
	 * {@link #polygons()}.
	 */
	public int[] sectionOrder(double @Nullable [] near) {
		int n = polygons.size();
		List<Integer> rest = new java.util.ArrayList<>();
		for (int i = 0; i < n; i++) {
			rest.add(i);
		}
		rest.sort(java.util.Comparator.comparingDouble((Integer i) -> -polygons.get(i).area()));
		int first = rest.isEmpty() ? -1 : rest.get(0);
		if (near != null) {
			for (int i : rest) {
				if (polygons.get(i).contains(near[0], near[1])) {
					first = i;
					break;
				}
			}
		}
		int[] out = new int[n];
		int k = 0;
		if (first >= 0) {
			out[k++] = first;
		}
		for (int i : rest) {
			if (i != first) {
				out[k++] = i;
			}
		}
		return out;
	}
}
