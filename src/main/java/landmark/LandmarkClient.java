package landmark;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LandmarkClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Branding.MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("{} loaded", Branding.DISPLAY_NAME);
	}
}
