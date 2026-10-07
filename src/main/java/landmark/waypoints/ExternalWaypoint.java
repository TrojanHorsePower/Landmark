package landmark.waypoints;

import org.jspecify.annotations.Nullable;

/**
 * A waypoint the player made in another mod (Xaero's Minimap or JourneyMap), shown on the map. {@code y} is null when the other
 * mod does not know a height. {@code argb} is the colour the player gave it there.
 */
public record ExternalWaypoint(Source source, String name, String dimension, int x, @Nullable Integer y, int z, int argb) {
	public enum Source {
		XAERO("Xaero's Minimap"),
		JOURNEYMAP("JourneyMap");

		public final String displayName;

		Source(String displayName) {
			this.displayName = displayName;
		}
	}
}
