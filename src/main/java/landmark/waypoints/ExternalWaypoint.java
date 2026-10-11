package landmark.waypoints;

import org.jspecify.annotations.Nullable;

/**
 * A waypoint the player made in another mod (Xaero's Minimap or JourneyMap), shown on the map. {@code y} is null when the other
 * mod does not know a height. {@code argb} is the colour the player gave it there. {@code group} is the Xaero set or JourneyMap
 * group it belongs to ("" when unknown), and {@code active} says whether the other mod is showing that group right now (Xaero's
 * current set for that dimension; a JourneyMap group that is switched on). {@code origin} is set only on a copy that was
 * converted from another dimension for display (see {@link DimensionScale}); {@code x}/{@code z} are then the converted
 * position and {@code origin} the real one.
 */
public record ExternalWaypoint(Source source, String name, String dimension, int x, @Nullable Integer y, int z, int argb,
	String group, boolean active, @Nullable Origin origin) {
	/** Where a converted waypoint really is. */
	public record Origin(String dimension, int x, int z) {}

	public enum Source {
		XAERO("Xaero's Minimap"),
		JOURNEYMAP("JourneyMap");

		public final String displayName;

		Source(String displayName) {
			this.displayName = displayName;
		}
	}

	public ExternalWaypoint(Source source, String name, String dimension, int x, @Nullable Integer y, int z, int argb) {
		this(source, name, dimension, x, y, z, argb, "", true, null);
	}

	public ExternalWaypoint(Source source, String name, String dimension, int x, @Nullable Integer y, int z, int argb,
		String group, boolean active) {
		this(source, name, dimension, x, y, z, argb, group, active, null);
	}

	/** This waypoint placed in another dimension at the given converted position. */
	public ExternalWaypoint convertedTo(String targetDimension, int cx, int cz) {
		return new ExternalWaypoint(source, name, targetDimension, cx, y, cz, argb, group, active, new Origin(dimension, x, z));
	}

	/** Identifies the group across dimensions and sources; this is what the "hidden sets" setting stores. */
	public String groupKey() {
		return source.name().toLowerCase(java.util.Locale.ROOT) + ":" + group;
	}
}
