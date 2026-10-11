package landmark.client;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;

/** Development aid: creates waypoints through JourneyMap's API to test reading them. Only used by the dev harness. */
final class DevJourneyMap {
	private DevJourneyMap() {}

	static void addTestWaypoints(Object api, String modId) {
		IClientAPI jm = (IClientAPI) api;
		net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> overworld = landmark.compat.Platform.dimensionKey("minecraft:overworld");
		var a = create(modId, new BlockPos(300, 66, -250), "JM Lighthouse", overworld);
		a.setColor(0x33CCFF);
		var b = create(modId, new BlockPos(-900, 70, 600), "JM Cabin", overworld);
		b.setColor(0xFF8800);
		jm.addWaypoint(modId, a);
		jm.addWaypoint(modId, b);
	}

	/** Some earlier JourneyMap builds name the factory method createClientWaypoint; try that when createWaypoint is missing. */
	private static journeymap.api.v2.common.waypoint.Waypoint create(String modId, BlockPos pos, String name, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim) {
		try {
			return WaypointFactory.createWaypoint(modId, pos, name, dim, true);
		} catch (NoSuchMethodError e) {
			try {
				for (var m : WaypointFactory.class.getMethods()) {
					if (m.getName().equals("createClientWaypoint") && m.getParameterCount() == 5 && m.getParameterTypes()[2] == String.class
						&& m.getParameterTypes()[3].isAssignableFrom(dim.getClass())) {
						return (journeymap.api.v2.common.waypoint.Waypoint) m.invoke(null, modId, pos, name, dim, true);
					}
				}
			} catch (ReflectiveOperationException ex) {
				throw new IllegalStateException(ex);
			}
			throw e;
		}
	}
}
