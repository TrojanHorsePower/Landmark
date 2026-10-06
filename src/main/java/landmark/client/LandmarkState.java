package landmark.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import landmark.LandmarkClient;
import landmark.data.DataStore;
import landmark.data.LandmarkConfig;
import landmark.data.OpenSpawns;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/** Client-thread state shared by the screens: stored claim data, the live open-spawn list, and status notes. */
public final class LandmarkState {
	private static @Nullable LandmarkState instance;

	public final DataStore store;
	public final LandmarkConfig config;
	private final Path configFile;
	private final Map<String, @Nullable DimensionMap> maps = new HashMap<>();
	private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "landmark-io");
		t.setDaemon(true);
		return t;
	});

	public @Nullable OpenSpawns openSpawns;
	public boolean refreshing;
	public @Nullable String notice;
	public boolean noticeIsError;
	/** Bumped whenever stored claim data changes or open spawns change, so cached textures know to rebuild. */
	public int dataVersion;
	public int spawnsVersion;

	private LandmarkState(Path gameDir, Path configDir) {
		this.store = new DataStore(gameDir.resolve("landmark"));
		this.configFile = configDir.resolve("landmark.json");
		this.config = LandmarkConfig.load(configFile);
	}

	public static LandmarkState get() {
		if (instance == null) {
			throw new IllegalStateException("Landmark not initialised");
		}
		return instance;
	}

	public static void init(Path gameDir, Path configDir) {
		instance = new LandmarkState(gameDir, configDir);
	}

	public @Nullable DimensionMap map(String dimension) {
		return maps.computeIfAbsent(dimension, d -> store.load(d).map(DimensionMap::new).orElse(null));
	}

	public Set<Integer> openIndices(DimensionMap map) {
		Set<Integer> out = new HashSet<>();
		if (openSpawns != null) {
			for (String n : openSpawns.names()) {
				int i = map.indexOf(n);
				if (i >= 0) {
					out.add(i);
				}
			}
		}
		return out;
	}

	public void setNotice(@Nullable String text, boolean error) {
		notice = text;
		noticeIsError = error;
	}

	public void saveConfig() {
		try {
			config.save(configFile);
		} catch (IOException e) {
			LandmarkClient.LOGGER.warn("Could not save config", e);
		}
	}

	/** Asks the server for the open-spawn list. Keeps the previous list if the request fails. */
	public void refreshSpawns(Minecraft mc) {
		if (refreshing) {
			return;
		}
		refreshing = true;
		LiveSpawns.request(mc).whenCompleteAsync((spawns, error) -> {
			refreshing = false;
			if (error != null) {
				Throwable cause = error.getCause() != null ? error.getCause() : error;
				setNotice("Could not load open spawns: " + cause.getMessage(), true);
			} else {
				openSpawns = spawns;
				spawnsVersion++;
				setNotice(null, false);
			}
		}, mc);
	}

	/** Imports an export zip off-thread, then updates state on the client thread. */
	public CompletableFuture<Void> importZip(Minecraft mc, Path zip) {
		setNotice("Importing " + zip.getFileName() + "...", false);
		return CompletableFuture.supplyAsync(() -> {
			try {
				return store.importZip(zip);
			} catch (IOException e) {
				throw new java.util.concurrent.CompletionException(e);
			}
		}, io).handleAsync((result, error) -> {
			if (error != null) {
				Throwable cause = error.getCause() != null ? error.getCause() : error;
				setNotice("Import failed: " + cause.getMessage(), true);
			} else {
				maps.clear();
				dataVersion++;
				String dim = result.snapshot().dimension();
				setNotice("Imported " + result.snapshot().lands().size() + " lands for " + dim + " with " + result.tileCount()
					+ " map tiles" + (result.warnings().isEmpty() ? "" : " (" + String.join("; ", result.warnings()) + ")"), false);
			}
			return null;
		}, mc);
	}
}
