package landmark.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** User settings, stored as pretty JSON. Missing or broken files fall back to defaults. */
public final class LandmarkConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Ask before running {@code /lands spawn}. */
	public boolean confirmTeleport = true;
	/** Request the open-spawn list whenever the map opens. */
	public boolean refreshOnOpen = true;
	/** The web map players export from; opened by the in-game help screen. Change it for another server. */
	public String mapUrl = "https://map.chillzone.cc/";
	/** Maximum number of map tiles kept as GPU textures at once. */
	public int tileCacheSize = 48;

	public static LandmarkConfig load(Path file) {
		try {
			LandmarkConfig c = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), LandmarkConfig.class);
			if (c == null) {
				return new LandmarkConfig();
			}
			c.tileCacheSize = Math.max(8, Math.min(512, c.tileCacheSize));
			if (c.mapUrl == null || !c.mapUrl.startsWith("https://")) {
				c.mapUrl = new LandmarkConfig().mapUrl;
			}
			return c;
		} catch (IOException | RuntimeException e) {
			return new LandmarkConfig();
		}
	}

	public void save(Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, GSON.toJson(this), StandardCharsets.UTF_8);
	}
}
