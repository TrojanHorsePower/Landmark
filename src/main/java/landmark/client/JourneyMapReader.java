package landmark.client;

import java.util.ArrayList;
import java.util.List;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.waypoint.Waypoint;
import landmark.waypoints.ExternalWaypoint;

/** Turns JourneyMap's waypoints into {@link ExternalWaypoint}s. Only loaded when JourneyMap's API is actually present. */
final class JourneyMapReader {
	private JourneyMapReader() {}

	static List<ExternalWaypoint> read(Object api) {
		List<ExternalWaypoint> out = new ArrayList<>();
		for (Waypoint w : ((IClientAPI) api).getAllWaypoints()) {
			if (!w.isEnabled()) {
				continue; // switched off in JourneyMap
			}
			var dims = w.getDimensions();
			List<String> where = dims == null || dims.isEmpty() ? List.of(w.getPrimaryDimension()) : List.copyOf(dims);
			int argb = 0xFF000000 | (w.getColor() & 0xFFFFFF);
			for (String dim : where) {
				if (dim != null) {
					out.add(new ExternalWaypoint(ExternalWaypoint.Source.JOURNEYMAP, w.getName(), dim, w.getX(), w.getY(), w.getZ(), argb));
				}
			}
		}
		return out;
	}
}
