package landmark.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Registers the settings screen with Mod Menu. Mod Menu is optional: this class is only ever loaded by Mod Menu itself, so the
 * mod works without it (the same screen is also reachable from the Settings button on the map).
 */
public class LandmarkModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ConfigScreen::new;
	}
}
