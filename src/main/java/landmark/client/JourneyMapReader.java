package landmark.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointGroup;
import landmark.waypoints.ExternalWaypoint;

/** Turns JourneyMap's waypoints into {@link ExternalWaypoint}s. Only loaded when JourneyMap's API is actually present. */
final class JourneyMapReader {
	private JourneyMapReader() {}

	static List<ExternalWaypoint> read(Object api) {
		List<ExternalWaypoint> out = new ArrayList<>();
		IClientAPI client = (IClientAPI) api;
		Map<String, WaypointGroup> groups = new HashMap<>();
		try {
			for (WaypointGroup g : client.getAllWaypointGroups()) {
				groups.put(g.getGuid(), g);
			}
		} catch (RuntimeException e) {
			// older API behaviour: no group information, everything counts as active
		}
		for (Waypoint w : client.getAllWaypoints()) {
			if (!w.isEnabled()) {
				continue; // switched off in JourneyMap
			}
			var dims = w.getDimensions();
			List<String> where = dims == null || dims.isEmpty() ? List.of(w.getPrimaryDimension()) : List.copyOf(dims);
			int argb = 0xFF000000 | (w.getColor() & 0xFFFFFF);
			WaypointGroup group = groups.get(w.getGroupId());
			String groupName = group == null || group.getName() == null ? "" : group.getName();
			boolean active = group == null || group.isEnabled();
			for (String dim : where) {
				if (dim != null) {
					out.add(new ExternalWaypoint(ExternalWaypoint.Source.JOURNEYMAP, w.getName(), dim, w.getX(), w.getY(), w.getZ(), argb, groupName, active));
				}
			}
		}
		return out;
	}
}
