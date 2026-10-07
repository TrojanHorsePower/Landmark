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
	private static final boolean SETTINGS = System.getProperty("landmark.dev.settings") != null;
	private static final boolean LAYOUT = System.getProperty("landmark.dev.layout") != null;
	private static final boolean SPAWNS = System.getProperty("landmark.dev.spawns") != null;
	private static final boolean WAYPOINTS = System.getProperty("landmark.dev.waypoints") != null;
	private static final boolean JM_TEST = System.getProperty("landmark.dev.jmtest") != null;
	private static final boolean JM_WORLD = System.getProperty("landmark.dev.jmworld") != null;
	private static int worldStage;
	private static int worldTicks;
	/** Every Nth land is pretended to have an open spawn (the real server has roughly 1 in 12). */
	private static final int OPEN_EVERY = Integer.getInteger("landmark.dev.openEvery", 5);
	private static long perfSum;
	private static int perfN;

	private DevHarness() {}

	private static net.minecraft.client.gui.screens.Screen mapScreen;
	private static ConfigScreen configScreen;

	/** Exercises the settings screens, the hide-claims toggle, key rebinding and the help-page import; logs results as SCENARIO lines. */
	private static void settingsScenario(net.minecraft.client.Minecraft mc, int f) {
		var shot = (Runnable) () -> Screenshot.grab(mc, false);
		LandmarkState state = LandmarkState.get();
		if (f == 100) {
			mapScreen = mc.gui.screen();
			shot.run();
		} else if (f == 110 && mapScreen instanceof MapScreen ms) {
			ms.devToggleClaims();
			LandmarkClient.LOGGER.info("SCENARIO claims hidden={}", state.hideClaims);
		} else if (f == 135) {
			shot.run();
		} else if (f == 140 && mapScreen instanceof MapScreen ms) {
			ms.devToggleClaims();
			LandmarkClient.LOGGER.info("SCENARIO claims hidden={}", state.hideClaims);
		} else if (f == 150) {
			configScreen = new ConfigScreen(mapScreen);
			mc.setScreenAndShow(configScreen);
		} else if (f == 175) {
			shot.run();
		} else if (f == 180) {
			configScreen.devSetPreset(landmark.data.ExportPreset.HIGH);
			configScreen.devScrollTo(170);
		} else if (f == 200) {
			shot.run();
		} else if (f == 205) {
			configScreen.devScrollTo(560);
		} else if (f == 230) {
			shot.run();
		} else if (f == 235) {
			mc.setScreenAndShow(new ColorPickerScreen(configScreen, landmark.data.ColorKey.OPEN_FILL));
		} else if (f == 260) {
			shot.run();
		} else if (f == 265) {
			mc.setScreenAndShow(configScreen);
		} else if (f == 270) {
			var key = LandmarkClient.openKey;
			String before = key.saveString();
			int free = configScreen.devFreeKey();
			LandmarkClient.LOGGER.info("SCENARIO free key chosen: {}", configScreen.devConflicts(free));
			LandmarkClient.LOGGER.info("SCENARIO screen before rebind: {}", mc.gui.screen().getClass().getSimpleName());
			configScreen.devRebind(free);
			LandmarkClient.LOGGER.info("SCENARIO screen after rebind: {}", mc.gui.screen().getClass().getSimpleName());
			LandmarkClient.LOGGER.info("SCENARIO rebind from M to a free key: before={} after={} sameObjectAsControlsScreen={}", before, key.saveString(),
				java.util.Arrays.asList(mc.options.keyMappings).contains(key));
		} else if (f == 275) {
			configScreen.devRebind(org.lwjgl.glfw.GLFW.GLFW_KEY_W); // already used by "forward": must ask first
			LandmarkClient.LOGGER.info("SCENARIO conflict dialog shown={} key still={}", mc.gui.screen() instanceof net.minecraft.client.gui.screens.ConfirmScreen, LandmarkClient.openKey.saveString());
		} else if (f == 290) {
			shot.run();
		} else if (f == 295) {
			mc.setScreenAndShow(configScreen);
			LandmarkClient.openKey.setKey(LandmarkClient.openKey.getDefaultKey());
			net.minecraft.client.KeyMapping.resetMapping();
			LandmarkClient.LOGGER.info("SCENARIO key restored to {}", LandmarkClient.openKey.saveString());
		} else if (f == 300) {
			state.setNotice(null, false);
			var help = new HelpScreen(mapScreen);
			mc.setScreenAndShow(help);
			String clip = help.devCopy();
			LandmarkClient.LOGGER.info("SCENARIO copied script ({} chars) has tileFolder: 2 = {}, tileMode: 'full' = {}, is the real script = {}", clip.length(),
				clip.contains("tileFolder: 2"), clip.contains("tileMode: 'full'"), clip.contains("buildZip"));
			String zip = System.getProperty("landmark.dev.zip");
			if (zip != null) {
				help.onFilesDrop(java.util.List.of(java.nio.file.Path.of(zip)));
			}
		} else if (f == 340) {
			LandmarkClient.LOGGER.info("SCENARIO help-page import notice: {} (error={})", state.notice, state.noticeIsError);
			shot.run();
		} else if (f == 345) {
			mc.setScreenAndShow(mapScreen); // back to the map: it must rebuild itself from the freshly imported data
		} else if (f == 385) {
			shot.run();
		} else if (f == 390 && mapScreen instanceof MapScreen ms) {
			var map = state.map(DEMO_DIMENSION);
			var l = map.grid.lands().get(7);
			int[] b = l.bounds();
			ms.view().setScale(0.4); // 1024-block tiles are ~410 GUI px wide here, so detail tiles are used
			ms.view().centerOn((b[0] + b[2]) / 2.0, (b[1] + b[3]) / 2.0);
			LandmarkClient.LOGGER.info("SCENARIO zoomed on {} for the detail-tile check", l.name());
		} else if (f == 430) {
			shot.run();
		} else if (f == 435) {
			var factory = new LandmarkModMenu().getModConfigScreenFactory();
			LandmarkClient.LOGGER.info("SCENARIO modmenu factory creates {}", factory.create(mapScreen).getClass().getSimpleName());
			mc.setScreenAndShow(com.terraformersmc.modmenu.api.ModMenuApi.createModsScreen(mapScreen));
		} else if (f == 460) {
			state.config.setExportPreset(landmark.data.ExportPreset.LOW);
			mc.stop();
		}
	}

	/** Visits every screen at the current window size so overlaps and clipping can be checked in the screenshots. */
	private static void layoutScenario(net.minecraft.client.Minecraft mc, int f) {
		var shot = (Runnable) () -> Screenshot.grab(mc, false);
		LandmarkState state = LandmarkState.get();
		var w = mc.getWindow();
		if (f == 95) {
			LandmarkClient.LOGGER.info("LAYOUT window {}x{} scale {} -> GUI {}x{}", w.getWidth(), w.getHeight(), w.getGuiScale(), w.getGuiScaledWidth(), w.getGuiScaledHeight());
			mapScreen = mc.gui.screen();
		} else if (f == 100 || f == 125) {
			shot.run(); // 1: the map
		} else if (f == 105) {
			devDimension = "minecraft:the_nether"; // a dimension with no claim data: the "no data" message
		} else if (f == 128) {
			devDimension = DEMO_DIMENSION;
		} else if (f == 140) {
			state.setNotice("Imported 130 lands for minecraft:overworld with 4 map tiles (6 map shapes could not be read and were skipped)", false);
			state.config.setExportPreset(landmark.data.ExportPreset.HIGH);
			var help = new HelpScreen(mapScreen);
			mc.setScreenAndShow(help);
			configScreen = null;
			helpScreen = help;
		} else if (f == 160) {
			shot.run(); // 3: help page, top of the text
		} else if (f == 165) {
			helpScreen.devScroll(100000);
		} else if (f == 185) {
			shot.run(); // 4: help page scrolled to the end
		} else if (f == 190) {
			configScreen = new ConfigScreen(mapScreen);
			mc.setScreenAndShow(configScreen);
		} else if (f == 210) {
			shot.run(); // 5: settings, top
		} else if (f == 215) {
			configScreen.devScrollTo(150);
		} else if (f == 235) {
			shot.run(); // 6: settings, features and quality notes
		} else if (f == 240) {
			configScreen.devScrollTo(560);
		} else if (f == 260) {
			shot.run(); // 7: settings, colours
		} else if (f == 265) {
			picker = new ColorPickerScreen(configScreen, landmark.data.ColorKey.OPEN_FILL);
			mc.setScreenAndShow(picker);
		} else if (f == 285) {
			shot.run(); // 8: colour picker
		} else if (f == 290) {
			picker.devHex("zzz");
		} else if (f == 310) {
			shot.run(); // 9: colour picker with an invalid hex code
		} else if (f == 320) {
			state.config.setExportPreset(landmark.data.ExportPreset.LOW);
			state.setNotice(null, false);
			mc.stop();
		}
	}

	/** Saved spawn locations: hidden when saving is switched off, and removable from the settings. */
	private static void spawnsScenario(net.minecraft.client.Minecraft mc, int f) {
		var shot = (Runnable) () -> Screenshot.grab(mc, false);
		LandmarkState state = LandmarkState.get();
		java.nio.file.Path file = mc.gameDirectory.toPath().resolve("landmark").resolve("learned-spawns.json");
		if (f == 95) {
			mapScreen = mc.gui.screen();
			LandmarkClient.LOGGER.info("SPAWNS saved={} saveSpawns={} visibleOnMap={}", state.learned.size(), state.config.saveSpawns, state.visibleSpawns(DEMO_DIMENSION).size());
		} else if (f == 100) {
			shot.run(); // 1: pins visible
		} else if (f == 105) {
			state.config.saveSpawns = false;
			LandmarkClient.LOGGER.info("SPAWNS after switching saving OFF: saved={} visibleOnMap={}", state.learned.size(), state.visibleSpawns(DEMO_DIMENSION).size());
		} else if (f == 130) {
			shot.run(); // 2: pins gone, data kept
		} else if (f == 135) {
			state.config.saveSpawns = true;
			LandmarkClient.LOGGER.info("SPAWNS after switching saving back ON: visibleOnMap={}", state.visibleSpawns(DEMO_DIMENSION).size());
		} else if (f == 140) {
			configScreen = new ConfigScreen(mapScreen);
			mc.setScreenAndShow(configScreen);
			configScreen.devScrollTo(70);
		} else if (f == 165) {
			shot.run(); // 3: settings row with the count and the Delete button
		} else if (f == 170) {
			configScreen.devDeleteSaved();
		} else if (f == 195) {
			LandmarkClient.LOGGER.info("SPAWNS after Delete all: saved={} visibleOnMap={} fileContent={}", state.learned.size(), state.visibleSpawns(DEMO_DIMENSION).size(),
				java.nio.file.Files.exists(file) ? readSmall(file) : "(no file)");
			shot.run(); // 4: count 0, button disabled
		} else if (f == 200) {
			mc.setScreenAndShow(mapScreen);
		} else if (f == 225) {
			shot.run(); // 5: map without pins
		} else if (f == 235) {
			mc.stop();
		}
	}

	private static String readSmall(java.nio.file.Path p) {
		try {
			return java.nio.file.Files.readString(p).replaceAll("\\s+", "");
		} catch (java.io.IOException e) {
			return "(unreadable)";
		}
	}

	/** Writes Xaero-format waypoint files for a made-up server, then checks they show on the map (and that decoys do not). */
	private static void waypointsScenario(net.minecraft.client.Minecraft mc, int f) {
		var shot = (Runnable) () -> Screenshot.grab(mc, false);
		LandmarkState state = LandmarkState.get();
		if (f == 60) {
			try {
				java.nio.file.Path root = mc.gameDirectory.toPath().resolve("xaero/minimap");
				java.nio.file.Path mine = root.resolve("Multiplayer_play.example.invalid");
				java.nio.file.Files.createDirectories(mine.resolve("dim%0"));
				java.nio.file.Files.createDirectories(mine.resolve("dim%-1"));
				java.nio.file.Files.createDirectories(root.resolve("Multiplayer_other.example.invalid/dim%0"));
				java.nio.file.Files.write(mine.resolve("dim%0/mw$default_1.txt"), java.util.List.of(
					"#", "#waypoint:name:initials:x:y:z:color:disabled:type:set:rotate_on_tp:tp_yaw:visibility_type:destination", "#",
					"sets:gui.xaero_default",
					"waypoint:Home Base:H:0:70:0:12:false:0:gui.xaero_default:false:0:0:false",
					"waypoint:Iron Mine:I:900:~:-700:6:false:0:gui.xaero_default:false:0:0:false",
					"waypoint:Farm:F:-1500:64:1100:10:false:0:gui.xaero_default:false:0:0:false",
					"waypoint:Old Camp:O:2400:64:2200:1:false:0:gui.xaero_default:false:0:0:false",
					"waypoint:Switched Off:X:300:64:300:15:true:0:gui.xaero_default:false:0:0:false",
					"waypoint:gui.xaero_deathpoint:D:-600:70:-900:0:false:1:gui.xaero_default:false:0:0:false"));
				java.nio.file.Files.write(mine.resolve("dim%-1/mw$default_1.txt"), java.util.List.of("waypoint:Nether Portal:P:10:70:10:9:false:0:gui.xaero_default:false:0:0:false"));
				java.nio.file.Files.write(root.resolve("Multiplayer_other.example.invalid/dim%0/mw$default_1.txt"), java.util.List.of("waypoint:Someone Elses:E:100:64:100:14:false:0:gui.xaero_default:false:0:0:false"));
			} catch (java.io.IOException e) {
				LandmarkClient.LOGGER.warn("fixture failed", e);
			}
			state.reloadWaypoints(mc); // the map was opened before these files existed, so read them now
		} else if (f == 95) {
			mapScreen = mc.gui.screen();
		} else if (f == 120) {
			var wps = state.external.forDimension(DEMO_DIMENSION);
			LandmarkClient.LOGGER.info("WAYPOINTS loaded {} for the overworld: {}", wps.size(), wps.stream().map(w -> w.name()).sorted().toList());
			shot.run(); // 1: markers on the map
		} else if (f == 125 && mapScreen instanceof MapScreen ms) {
			var home = state.external.forDimension(DEMO_DIMENSION).stream().filter(w -> w.name().equals("Home Base")).findFirst().orElseThrow();
			ms.view().setScale(0.3);
			ms.view().centerOn(home.x() + 0.5, home.z() + 0.5);
			var win = mc.getWindow();
			int pw = Math.max(150, Math.min(190, win.getGuiScaledWidth() / 3 + 24)); // the map screen's adaptive panel width
			ms.devMouseX = (int) (pw + (win.getGuiScaledWidth() - pw) / 2.0);
			ms.devMouseY = (int) ((win.getGuiScaledHeight() - 34) / 2.0);
		} else if (f == 150) {
			shot.run(); // 2: tooltip on a waypoint
		} else if (f == 155) {
			state.config.showExternalWaypoints = false;
			if (mapScreen instanceof MapScreen ms) {
				ms.devMouseX = -1;
			}
		} else if (f == 175) {
			LandmarkClient.LOGGER.info("WAYPOINTS drawn while switched off: toggle={}", state.config.showExternalWaypoints);
			shot.run(); // 3: switched off
		} else if (f == 180) {
			state.config.showExternalWaypoints = true;
			configScreen = new ConfigScreen(mapScreen);
			mc.setScreenAndShow(configScreen);
			configScreen.devScrollTo(140);
		} else if (f == 205) {
			shot.run(); // 4: settings toggle with the 'found' line
		} else if (f == 215) {
			mc.stop();
		}
	}

	/** Creates and joins a real singleplayer world, then tests JourneyMap's waypoints and the map screen inside it. */
	private static void jmWorldTick(net.minecraft.client.Minecraft mc) {
		LandmarkState state = LandmarkState.get();
		worldTicks++;
		switch (worldStage) {
			case 0 -> {
				if (mc.gui.screen() instanceof TitleScreen && ticks > 80) {
					LandmarkClient.LOGGER.info("JMWORLD creating a singleplayer world");
					var settings = new net.minecraft.world.level.LevelSettings("landmark-dev", net.minecraft.world.level.GameType.CREATIVE,
						net.minecraft.world.level.LevelSettings.DifficultySettings.DEFAULT, true, net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
					mc.createWorldOpenFlows().createFreshLevel("landmark-dev", settings, net.minecraft.world.level.levelgen.WorldOptions.defaultWithRandomSeed(),
						net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions, mc.gui.screen());
					worldStage = 1;
					worldTicks = 0;
				}
			}
			case 1 -> {
				if (mc.level != null && mc.player != null && worldTicks > 200) {
					LandmarkClient.LOGGER.info("JMWORLD joined: dimension={} player at {},{}", mc.level.dimension().identifier(), (int) mc.player.getX(), (int) mc.player.getZ());
					String zip = System.getProperty("landmark.dev.importzip");
					if (zip != null) {
						try {
							state.store.importZip(java.nio.file.Path.of(zip));
						} catch (java.io.IOException e) {
							LandmarkClient.LOGGER.warn("dev import failed", e);
						}
					}
					Object api = ExternalWaypoints.journeyMapApi;
					LandmarkClient.LOGGER.info("JMWORLD plugin initialised (api present): {}", api != null);
					if (api != null) {
						try {
							DevJourneyMap.addTestWaypoints(api, "landmark");
						} catch (Throwable t) {
							LandmarkClient.LOGGER.warn("JMWORLD could not add waypoints through the API", t);
						}
					}
					worldStage = 2;
					worldTicks = 0;
				}
			}
			case 2 -> {
				if (worldTicks == 40) {
					state.reloadWaypoints(mc);
				}
				if (worldTicks == 100) {
					var wps = state.external.forDimension("minecraft:overworld");
					LandmarkClient.LOGGER.info("JMWORLD read {} waypoints: {}", wps.size(), wps.stream().map(w -> w.source() + ":" + w.name() + "@" + w.x() + "," + w.y() + "," + w.z() + " #" + Integer.toHexString(w.argb())).toList());
					mc.setScreenAndShow(new MapScreen());
					worldStage = 3;
					worldTicks = 0;
				}
			}
			case 3 -> {
				if (worldTicks == 40 && mc.gui.screen() instanceof MapScreen ms) {
					ms.view().setScale(1.0 / 8);
					ms.view().centerOn(-300, 150);
				}
				if (worldTicks == 80) {
					Screenshot.grab(mc, false);
				}
				if (worldTicks == 110) {
					mc.stop();
				}
			}
			default -> { }
		}
	}

	/** JourneyMap integration: is the plugin initialised, and are waypoints created through its API read back? */
	private static void jmScenario(net.minecraft.client.Minecraft mc, int f) {
		var shot = (Runnable) () -> Screenshot.grab(mc, false);
		LandmarkState state = LandmarkState.get();
		if (f == 60) {
			Object api = ExternalWaypoints.journeyMapApi;
			LandmarkClient.LOGGER.info("JMTEST plugin initialised (api present): {} | journeymap loaded: {}", api != null, net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("journeymap"));
			if (api != null) {
				try {
					DevJourneyMap.addTestWaypoints(api, "landmark");
				} catch (Throwable t) {
					LandmarkClient.LOGGER.warn("JMTEST could not add waypoints through the API", t);
				}
			}
			state.reloadWaypoints(mc);
		} else if (f == 100) {
			var wps = state.external.forDimension(DEMO_DIMENSION);
			LandmarkClient.LOGGER.info("JMTEST read {} waypoints for the overworld: {}", wps.size(), wps.stream().map(w -> w.source() + ":" + w.name() + "@" + w.x() + "," + w.z()).toList());
			mapScreen = mc.gui.screen();
			shot.run();
		} else if (f == 110) {
			mc.stop();
		}
	}

	private static HelpScreen helpScreen;
	private static ColorPickerScreen picker;
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
		for (int i = 0; i < lands.size(); i += OPEN_EVERY) {
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
				int pw = Math.max(150, Math.min(190, w.getGuiScaledWidth() / 3 + 24));
				double gx = pw + (w.getGuiScaledWidth() - pw) / 2.0;
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
			if (JM_WORLD) {
				jmWorldTick(mc);
				return;
			}
			if (framesOpen < 0 && mc.gui.screen() instanceof TitleScreen && ticks > 60) {
				devDimension = DEMO_DIMENSION;
				LandmarkState state = LandmarkState.get();
				String importZip = System.getProperty("landmark.dev.importzip");
				if (importZip != null) {
					try {
						state.store.importZip(java.nio.file.Path.of(importZip));
					} catch (java.io.IOException e) {
						LandmarkClient.LOGGER.warn("dev import failed", e);
					}
				}
				DimensionMap map = state.map(DEMO_DIMENSION);
				if (map != null) {
					List<String> names = new ArrayList<>();
					List<Land> lands = map.grid.lands();
					for (int i = 0; i < lands.size(); i += OPEN_EVERY) {
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
					int[] marks = {100, 130, 160, 190, 220};
					double[] scales = {1.0 / 128, 1.0 / 48, 1.0 / 24, 1.0 / 8, 1.0 / 3};
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
				if (SETTINGS) {
					settingsScenario(mc, framesOpen);
					return;
				}
				if (LAYOUT) {
					layoutScenario(mc, framesOpen);
					return;
				}
				if (SPAWNS) {
					spawnsScenario(mc, framesOpen);
					return;
				}
				if (WAYPOINTS) {
					waypointsScenario(mc, framesOpen);
					return;
				}
				if (JM_TEST) {
					jmScenario(mc, framesOpen);
					return;
				}
				if (PERF) {
					if (framesOpen == 290) {
						mc.stop();
					}
					return; // performance mode only measures; it does not run the screenshot script
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
