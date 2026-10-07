package landmark.client;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import landmark.Branding;

/**
 * Lets Landmark read the player's JourneyMap waypoints through JourneyMap's official API. JourneyMap finds this class itself
 * (the {@code journeymap} entrypoint) and only if it is installed; the API is a compile-time dependency and none of its classes
 * are bundled, so the mod works without JourneyMap.
 */
@JourneyMapPlugin(apiVersion = "2.0.0")
public class LandmarkJourneyMapPlugin implements IClientPlugin {
	@Override
	public void initialize(IClientAPI api) {
		ExternalWaypoints.journeyMapApi = api;
	}

	@Override
	public String getModId() {
		return Branding.MOD_ID;
	}
}
