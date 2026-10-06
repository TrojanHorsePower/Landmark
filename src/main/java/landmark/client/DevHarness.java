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

	private static String searchQuery = "";

	/** Zooms onto an open-spawn land with an owner and other members and parks the cursor over it. */
	private static void hoverOpenLand(net.minecraft.client.Minecraft mc) {
		if (!(mc.gui.screen() instanceof MapScreen ms)) {
			return;
		}
		DimensionMap map = LandmarkState.get().map(DEMO_DIMENSION);
		if (map == null) {
			return;
		}
		List<Land> lands = map.grid.lands();
		for (int i = 0; i < lands.size(); i += 5) {
			Land l = lands.get(i);
			int[] b = l.bounds();
			if (b != null && l.owner() != null && l.members().stream().anyMatch(m -> !m.equalsIgnoreCase(l.owner())) && l.chunks() >= 12) {
				searchQuery = l.owner().replaceAll("\\d+$", "");
				// aim at a chunk that really belongs to the land (a bounding-box centre can be a hole or notch)
				int ax = (b[0] + b[2]) / 2, az = (b[1] + b[3]) / 2;
				outer:
				for (int x = b[0]; x < b[2]; x += 16) {
					for (int z = b[1]; z < b[3]; z += 16) {
						if (map.grid.landIndexAt(x + 8, z + 8) == i) {
							ax = x + 8;
							az = z + 8;
							if (Math.abs(x - (b[0] + b[2]) / 2) < 48 && Math.abs(z - (b[1] + b[3]) / 2) < 48) {
								break outer;
							}
						}
					}
				}
				ms.view().setScale(0.5);
				ms.view().centerOn(ax, az);
				var w = mc.getWindow();
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
					for (int i = 0; i < lands.size(); i += 5) {
						names.add(lands.get(i).name());
					}
					state.openSpawns = new OpenSpawns(names, System.currentTimeMillis() - 90_000);
					for (int i = 0; i < lands.size() && i < 30; i += 15) {
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
				// Screenshot script (saved in order to run/screenshots): overview, hover tooltip, search by owner, help.
				if (framesOpen == 100) {
					Screenshot.grab(mc, false);
				}
				if (framesOpen == 110) {
					hoverOpenLand(mc);
				}
				if (framesOpen == 140) {
					Screenshot.grab(mc, false);
				}
				if (framesOpen == 150 && mc.gui.screen() instanceof MapScreen ms) {
					ms.devSearch(searchQuery);
					ms.devSelectFirst();
				}
				if (framesOpen == 180) {
					Screenshot.grab(mc, false);
				}
				if (framesOpen == 190) {
					mc.setScreenAndShow(new HelpScreen(mc.gui.screen()));
				}
				if (framesOpen == 215) {
					Screenshot.grab(mc, false);
				}
				if (framesOpen == 235) {
					mc.stop();
				}
			}
		});
	}
}
