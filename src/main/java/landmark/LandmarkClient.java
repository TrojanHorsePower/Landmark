package landmark;

import com.mojang.blaze3d.platform.InputConstants;
import landmark.client.LandmarkState;
import landmark.client.MapScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LandmarkClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Branding.MOD_ID);
	/** Namespace for textures this mod registers at runtime. */
	public static final String MOD_ID_FOR_ASSETS = Branding.MOD_ID;

	@Override
	public void onInitializeClient() {
		LOGGER.info("{} loaded", Branding.DISPLAY_NAME);
		LandmarkState.init(FabricLoader.getInstance().getGameDir(), FabricLoader.getInstance().getConfigDir());
		landmark.client.LandmarkDev.install();

		KeyMapping open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.landmark.open", InputConstants.KEY_M,
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Branding.MOD_ID, "main"))));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			LandmarkState.get().tickWatcher(mc);
			while (open.consumeClick()) {
				if (mc.level != null && mc.gui.screen() == null) {
					mc.setScreenAndShow(new MapScreen());
				}
			}
		});
	}
}
