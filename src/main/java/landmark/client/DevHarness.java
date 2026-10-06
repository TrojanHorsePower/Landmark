package landmark.client;

import java.util.ArrayList;
import java.util.List;
import landmark.LandmarkClient;
import landmark.data.Land;
import landmark.data.OpenSpawns;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Development aid, inert in a normal game: only active in a Fabric dev environment and only when the system property
 * {@code landmark.dev.demo} is set. It opens the map from the title screen with pretend open spawns, saves a screenshot
 * to the run folder and quits, so the screen can be checked without joining a server.
 */
final class DevHarness {
	static final String DEMO_DIMENSION = "minecraft:overworld";
	static String devDimension = "";

	private static int ticks;
	private static int framesOpen = -1;

	private DevHarness() {}

	static void install() {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment() || System.getProperty("landmark.dev.demo") == null) {
			return;
		}
		LandmarkClient.LOGGER.info("Dev demo enabled");
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			ticks++;
			if (framesOpen < 0 && mc.gui.screen() instanceof TitleScreen && ticks > 60) {
				devDimension = DEMO_DIMENSION;
				LandmarkState state = LandmarkState.get();
				DimensionMap map = state.map(DEMO_DIMENSION);
				if (map != null) {
					List<String> names = new ArrayList<>();
					List<Land> lands = map.grid.lands();
					for (int i = 0; i < lands.size(); i += 12) {
						names.add(lands.get(i).name());
					}
					state.openSpawns = new OpenSpawns(names, System.currentTimeMillis() - 90_000);
					state.spawnsVersion++;
				}
				mc.setScreenAndShow(new MapScreen());
				framesOpen = 0;
			} else if (framesOpen >= 0) {
				framesOpen++;
				if (framesOpen == 120) {
					Screenshot.grab(mc, false);
				}
				if (framesOpen == 180) {
					mc.stop();
				}
			}
		});
	}
}
