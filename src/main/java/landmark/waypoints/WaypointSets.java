package landmark.waypoints;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import landmark.data.WaypointSetMode;

/** Decides which external waypoints are drawn, and lists the sets and groups the player can choose between. */
public final class WaypointSets {
	private WaypointSets() {}

	public static boolean shown(ExternalWaypoint w, WaypointSetMode mode, Collection<String> hidden) {
		return switch (mode) {
			case ACTIVE -> w.active();
			case ALL -> true;
			case PICK -> !hidden.contains(w.groupKey());
		};
	}

	/**
	 * The waypoints that can be drawn in {@code dimension} before the set choice is applied: those made there and, when
	 * {@code crossDimension} is on, the Overworld's in the Nether and the Nether's in the Overworld at converted positions.
	 */
	public static List<ExternalWaypoint> viewable(Collection<ExternalWaypoint> all, String dimension, boolean crossDimension) {
		List<ExternalWaypoint> out = new ArrayList<>();
		for (ExternalWaypoint w : all) {
			if (w.dimension().equals(dimension)) {
				out.add(w);
			} else if (crossDimension) {
				int[] c = DimensionScale.convert(w.x(), w.z(), w.dimension(), dimension);
				if (c != null) {
					out.add(w.convertedTo(dimension, c[0], c[1]));
				}
			}
		}
		return out;
	}

	/**
	 * One Xaero set or JourneyMap group. {@code dimension} is the dimension a Xaero set belongs to ("" for JourneyMap). {@code count} is its waypoints in the viewed dimension, {@code total} in all of them;
	 * {@code active} is whether the other mod shows it in the viewed dimension and {@code activeAnywhere} in any dimension.
	 */
	public record Group(ExternalWaypoint.Source source, String dimension, String name, int count, int total, boolean active,
		boolean activeAnywhere) {
		public String key() {
			return ExternalWaypoint.key(source, dimension, name);
		}
	}

	/** Xaero's built-in set name is a translation key that is shown as "Default", so it sorts as that. */
	private static String sortName(String name) {
		return name.startsWith("gui.xaero_") ? name.substring("gui.xaero_".length()) : name;
	}

	/** The groups that have at least one waypoint, ordered by source and then name. */
	public static List<Group> groups(Collection<ExternalWaypoint> all, String dimension) {
		record Acc(ExternalWaypoint.Source source, String dimension, String name, int[] counts, boolean[] active) {}
		Map<String, Acc> byKey = new LinkedHashMap<>();
		for (ExternalWaypoint w : all) {
			Acc a = byKey.computeIfAbsent(w.groupKey(), k -> new Acc(w.source(), w.setDimension(), w.group(), new int[2], new boolean[2]));
			a.counts[1]++;
			a.active[1] |= w.active();
			if (w.dimension().equals(dimension)) {
				a.counts[0]++;
				a.active[0] |= w.active();
			}
		}
		List<Group> out = new ArrayList<>();
		for (Acc a : byKey.values()) {
			out.add(new Group(a.source, a.dimension, a.name, a.counts[0], a.counts[1], a.active[0], a.active[1]));
		}
		Collator collator = Collator.getInstance();
		// by source, then the viewed dimension's sets before other dimensions', then name
		out.sort((x, y) -> {
			if (x.source != y.source) {
				return x.source.compareTo(y.source);
			}
			boolean xHere = x.dimension.equals(dimension);
			boolean yHere = y.dimension.equals(dimension);
			if (xHere != yHere) {
				return xHere ? -1 : 1;
			}
			int d = x.dimension.compareTo(y.dimension);
			return d != 0 ? d : collator.compare(sortName(x.name), sortName(y.name));
		});
		return out;
	}
}
