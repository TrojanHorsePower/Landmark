package landmark.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import landmark.LandmarkClient;
import landmark.waypoints.ExternalWaypoint;
import landmark.waypoints.XaeroWaypoints;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * Waypoints the player made in other map mods, for display on the map. JourneyMap's come through its API (when that mod is
 * installed); Xaero's Minimap's are read from its files on disk. Both are read-only and stay on this computer.
 */
public final class ExternalWaypoints {
	/** Set by {@link LandmarkJourneyMapPlugin} when JourneyMap is present. Typed as Object so this class loads without JourneyMap. */
	static volatile @Nullable Object journeyMapApi;

	private volatile List<ExternalWaypoint> xaero = List.of();
	private volatile List<ExternalWaypoint> journeyMap = List.of();
	private boolean loadedOnce;
	/** Bumped when the waypoint lists change. */
	public volatile int version;

	/** The name to show: Xaero's built-in names (such as its death point) are translation keys, with a readable fallback. */
	public static String displayName(String name) {
		if (!name.startsWith("gui.xaero_")) {
			return name;
		}
		if (net.minecraft.locale.Language.getInstance().has(name)) {
			return net.minecraft.network.chat.Component.translatable(name).getString();
		}
		String rest = name.substring("gui.xaero_".length());
		if (rest.equals("deathpoint")) {
			return "Death point";
		}
		rest = rest.replace('_', ' ');
		return rest.isEmpty() ? name : Character.toUpperCase(rest.charAt(0)) + rest.substring(1);
	}

	/** Waypoints to draw in this dimension. */
	public List<ExternalWaypoint> forDimension(String dimension) {
		List<ExternalWaypoint> out = new ArrayList<>();
		for (ExternalWaypoint w : journeyMap) {
			if (w.dimension().equals(dimension)) {
				out.add(w);
			}
		}
		for (ExternalWaypoint w : xaero) {
			if (w.dimension().equals(dimension)) {
				out.add(w);
			}
		}
		return out;
	}

	public boolean loadedOnce() {
		return loadedOnce;
	}

	/** One line for the settings screen saying what was found. */
	public String summary(String notInstalled, String format) {
		String x = !xaero.isEmpty() || FabricLoader.getInstance().isModLoaded("xaerominimap") ? String.valueOf(xaero.size()) : notInstalled;
		String j = journeyMapApi == null ? notInstalled : String.valueOf(journeyMap.size());
		return String.format(format, x, j);
	}

	/** Re-reads both sources. JourneyMap is quick and is read right here; Xaero's files are read off the client thread. */
	public void reload(Minecraft mc, Executor io) {
		loadedOnce = true;
		Object api = journeyMapApi;
		if (api != null) {
			try {
				journeyMap = JourneyMapReader.read(api);
			} catch (RuntimeException | LinkageError e) {
				LandmarkClient.LOGGER.warn("Could not read JourneyMap waypoints", e);
				journeyMap = List.of();
			}
		}
		String address = serverAddress(mc);
		if (address == null) {
			xaero = List.of();
			version++;
			return;
		}
		var gameDir = mc.gameDirectory.toPath();
		CompletableFuture.supplyAsync(() -> XaeroWaypoints.load(gameDir, address), io).whenCompleteAsync((list, error) -> {
			xaero = error != null || list == null ? List.of() : list;
			version++;
		}, mc);
	}

	/** The address the player joined with; null when not on a server (singleplayer has no server folder in Xaero's). */
	private static @Nullable String serverAddress(Minecraft mc) {
		String dev = System.getProperty("landmark.dev.serverAddress");
		if (dev != null) {
			return dev;
		}
		var server = mc.getCurrentServer();
		return server == null ? null : server.ip;
	}
}
