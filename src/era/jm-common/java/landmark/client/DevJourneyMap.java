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
		var a = WaypointFactory.createWaypoint(modId, new BlockPos(300, 66, -250), "JM Lighthouse", overworld, true);
		a.setColor(0x33CCFF);
		var b = WaypointFactory.createWaypoint(modId, new BlockPos(-900, 70, 600), "JM Cabin", overworld, true);
		b.setColor(0xFF8800);
		jm.addWaypoint(modId, a);
		jm.addWaypoint(modId, b);
	}
}
