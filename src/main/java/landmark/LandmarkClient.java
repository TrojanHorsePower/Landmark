package landmark;

import com.mojang.blaze3d.platform.InputConstants;
import landmark.client.LandmarkState;
import landmark.client.MapScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LandmarkClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Branding.MOD_ID);
	/** Namespace for textures this mod registers at runtime. */
	public static final String MOD_ID_FOR_ASSETS = Branding.MOD_ID;

	/** The key that opens the map; registered with Minecraft, so it also appears in the normal Controls screen. */
	public static KeyMapping openKey;

	@Override
	public void onInitializeClient() {
		LOGGER.info("{} loaded", Branding.DISPLAY_NAME);
		LandmarkState.init(FabricLoader.getInstance().getGameDir(), FabricLoader.getInstance().getConfigDir());
		landmark.client.LandmarkDev.install();

		openKey = landmark.compat.Platform.registerKey("key.landmark.open", InputConstants.KEY_M, "main");
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			LandmarkState.get().tickWatcher(mc);
			while (openKey.consumeClick()) {
				if (mc.level != null && landmark.compat.Platform.screen(mc) == null) {
					mc.setScreenAndShow(new MapScreen());
				}
			}
		});
	}
}
