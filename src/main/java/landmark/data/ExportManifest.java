package landmark.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/** The {@code manifest.json} of an export zip. See docs/export-format.md. */
public record ExportManifest(int format, String world, String dimension, String source, long exportedAtMillis, @Nullable Tiles tiles) {
	public static final int SUPPORTED_FORMAT = 1;
	private static final Pattern DIMENSION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	/** Zoomed-out map tiles: each tile is {@code pixelSize} square and covers {@code blocksPerTile} blocks per side. */
	public record Tiles(int blocksPerTile, int pixelSize) {}

	public static ExportManifest parse(String json) throws ImportException {
		JsonObject o;
		try {
			o = JsonParser.parseString(json).getAsJsonObject();
		} catch (RuntimeException e) {
			throw new ImportException("manifest.json is not valid JSON");
		}
		try {
			int format = o.get("format").getAsInt();
			if (format != SUPPORTED_FORMAT) {
				throw new ImportException("Unsupported export format " + format + " (this version reads " + SUPPORTED_FORMAT + ")");
			}
			String dimension = o.get("dimension").getAsString();
			if (!DIMENSION.matcher(dimension).matches()) {
				throw new ImportException("Invalid dimension id in manifest");
			}
			Tiles tiles = null;
			if (o.has("tiles") && o.get("tiles").isJsonObject()) {
				JsonObject t = o.getAsJsonObject("tiles");
				int bpt = t.get("blocksPerTile").getAsInt();
				int px = t.get("pixelSize").getAsInt();
				if (bpt < 16 || bpt > 1 << 20 || Integer.bitCount(bpt) != 1 || px < 16 || px > 4096) {
					throw new ImportException("Invalid tile settings in manifest");
				}
				tiles = new Tiles(bpt, px);
			}
			return new ExportManifest(format, o.get("world").getAsString(), dimension, clean(o.get("source").getAsString()),
				o.get("exportedAt").getAsLong(), tiles);
		} catch (ImportException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ImportException("manifest.json is missing required fields");
		}
	}

	/** Display-safe: no control characters, bounded length. */
	private static String clean(String s) {
		String c = s.replaceAll("\\p{Cntrl}", "");
		return c.length() > 120 ? c.substring(0, 120) : c;
	}
}
