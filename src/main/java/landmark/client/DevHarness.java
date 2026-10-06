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
	private static final boolean PERF = System.getProperty("landmark.dev.perf") != null;
	private static long perfSum;
	private static int perfN;

	private DevHarness() {}

	/** Zooms onto a land that has a known owner and other members and parks the cursor over it, to check the tooltip. */
	private static void hoverLandWithOwnerAndMembers(net.minecraft.client.Minecraft mc) {
		if (!(mc.gui.screen() instanceof MapScreen ms)) {
			return;
		}
		DimensionMap map = LandmarkState.get().map(DEMO_DIMENSION);
		if (map == null) {
			return;
		}
		for (Land l : map.grid.lands()) {
			int[] b = l.bounds();
			if (b != null && l.owner() != null && l.members().stream().anyMatch(m -> !m.equalsIgnoreCase(l.owner())) && l.chunks() > 20) {
				ms.view().setScale(0.25);
				ms.view().centerOn((b[0] + b[2]) / 2.0, (b[1] + b[3]) / 2.0);
				var w = mc.getWindow();
				// the map area is x >= 190 (GUI px), y < height - 34; its centre is where the land now is
				double gx = 190 + (w.getGuiScaledWidth() - 190) / 2.0;
				double gy = (w.getGuiScaledHeight() - 34) / 2.0;
				org.lwjgl.glfw.GLFW.glfwSetCursorPos(w.handle(), gx * w.getGuiScale(), gy * w.getGuiScale());
				return;
			}
		}
	}

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
					for (int i = 0; i < lands.size() && i < 120; i += 12) {
						int[] b = lands.get(i).bounds();
						if (b != null) {
							state.learned.record(new landmark.data.LearnedSpawns.Spawn(lands.get(i).name(), DEMO_DIMENSION, (b[0] + b[2]) / 2, 70, (b[1] + b[3]) / 2, 0));
						}
					}
					state.spawnsVersion++;
				}
				mc.setScreenAndShow(new MapScreen());
				framesOpen = 0;
			} else if (framesOpen >= 0) {
				framesOpen++;
				if (PERF && mc.gui.screen() instanceof MapScreen ms) {
					// 30-frame windows at different zoom levels: average CPU time spent building the frame
					int[] marks = {100, 130, 160, 190};
					double[] scales = {1.0 / 128, 1.0 / 48, 1.0 / 24, 1.0 / 8};
					for (int k = 0; k < marks.length; k++) {
						if (framesOpen == marks[k]) {
							ms.view().setScale(scales[k]);
							ms.view().centerOn(0, 0);
							perfSum = 0;
							perfN = 0;
						}
						if (framesOpen > marks[k] + 5 && framesOpen <= marks[k] + 30) {
							perfSum += ms.lastExtractNanos;
							perfN++;
						}
						if (framesOpen == marks[k] + 30 && perfN > 0) {
							LandmarkClient.LOGGER.info("PERF scale=1/{} avg extract {} ms", Math.round(1 / scales[k]), String.format("%.2f", perfSum / 1e6 / perfN));
						}
					}
				}
				if (framesOpen == 240) {
					Screenshot.grab(mc, false);
				}
				if (framesOpen == 250) {
					mc.setScreenAndShow(new HelpScreen(mc.gui.screen()));
				}
				if (framesOpen == 270) {
					Screenshot.grab(mc, false);
				}
				if (framesOpen == 280) {
					mc.gui.screen().onClose(); // Back: must return to a working map
				}
				if (framesOpen == 300) {
					Screenshot.grab(mc, false);
					hoverLandWithOwnerAndMembers(mc);
				}
				if (framesOpen == 320) {
					Screenshot.grab(mc, false);
				}
				if (framesOpen == 340) {
					mc.stop();
				}
			}
		});
	}
}
