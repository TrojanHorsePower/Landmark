package landmark.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

/** User settings, stored as pretty JSON. Missing or broken files fall back to defaults, and unknown keys are ignored. */
public final class LandmarkConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Ask before running {@code /lands spawn}. */
	public boolean confirmTeleport = true;
	/** Remember where you arrive after a teleport and show a pin there. */
	public boolean saveSpawns = true;
	/** Outline every land that is not open-spawn. Off by default: with thousands of lands it can slow the map down. */
	public boolean outlineOtherLands = false;
	/** Show waypoints made in Xaero's Minimap and JourneyMap (if installed) on the map. */
	public boolean showExternalWaypoints = true;
	/** Show your own position on the map. */
	public boolean showPlayerMarker = true;
	/** Request the open-spawn list whenever the map opens. */
	public boolean refreshOnOpen = true;
	/** Export quality preset id (low, medium, high, ultra); decides the tile detail and format in the copied export script. */
	public String exportQuality = ExportPreset.LOW.id;
	/** The web map players export from; opened by the in-game help screen. Change it for another server. */
	public String mapUrl = "https://map.chillzone.cc/";
	/** Maximum number of map tiles kept as GPU textures at once. */
	public int tileCacheSize = 48;
	/** Colour overrides by {@link ColorKey#id}, as hex strings. Colours that are not listed use their default. */
	public Map<String, String> colors = new LinkedHashMap<>();

	public ExportPreset exportPreset() {
		return ExportPreset.byId(exportQuality);
	}

	public void setExportPreset(ExportPreset preset) {
		exportQuality = preset.id;
	}

	/** The colour to draw with: the saved override if it is valid, otherwise the default. */
	public int color(ColorKey key) {
		String hex = colors == null ? null : colors.get(key.id);
		if (hex != null) {
			OptionalInt parsed = ColorMath.parseHex(hex, key.hasAlpha);
			if (parsed.isPresent()) {
				return parsed.getAsInt();
			}
		}
		return key.defaultArgb;
	}

	/** Saves a colour; one equal to the default is not stored, so a future change of the default reaches the user. */
	public void setColor(ColorKey key, int argb) {
		int value = key.hasAlpha ? argb : ColorMath.withAlpha(argb, 255);
		if (value == key.defaultArgb) {
			colors.remove(key.id);
		} else {
			colors.put(key.id, ColorMath.format(value, key.hasAlpha));
		}
	}

	public boolean isDefaultColor(ColorKey key) {
		return !colors.containsKey(key.id) || color(key) == key.defaultArgb;
	}

	public void resetColor(ColorKey key) {
		colors.remove(key.id);
	}

	public void resetAllColors() {
		colors.clear();
	}

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
			if (c.colors == null) {
				c.colors = new LinkedHashMap<>();
			}
			c.colors.keySet().removeIf(id -> ColorKey.byId(id) == null);
			c.exportQuality = ExportPreset.byId(c.exportQuality).id;
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
