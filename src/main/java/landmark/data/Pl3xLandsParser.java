package landmark.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Pl3xMap marker layer ({@code tiles/<world>/markers/lands.json}) into {@link Land}s.
 * Land details only exist inside tooltip HTML, so that is parsed defensively: a marker that cannot be
 * understood is skipped, never fatal.
 */
public final class Pl3xLandsParser {
	private static final Pattern NAME = Pattern.compile("\\{land_color\\};\">(.*?)</span>", Pattern.DOTALL);
	private static final Pattern DESCRIPTION = Pattern.compile("<br\\s*/></span>(.*?)</div>", Pattern.DOTALL);
	private static final Pattern OWNER = Pattern.compile("^of (.+)\\.$", Pattern.DOTALL);
	private static final Pattern CHUNKS = Pattern.compile("Chunks: (\\d+)");
	private static final Pattern PLAYERS = Pattern.compile("Players \\(\\d+\\): ?(.*?)</li>", Pattern.DOTALL);

	private Pl3xLandsParser() {}

	public record Result(List<Land> lands, int skippedMarkers) {}

	public static Result parse(String json) {
		JsonArray markers = JsonParser.parseString(json).getAsJsonArray();
		Map<String, Builder> byLand = new LinkedHashMap<>();
		int skipped = 0;
		for (JsonElement el : markers) {
			try {
				if (!addMarker(el.getAsJsonObject(), byLand)) {
					skipped++;
				}
			} catch (RuntimeException e) {
				skipped++;
			}
		}
		List<Land> lands = new ArrayList<>();
		for (Builder b : byLand.values()) {
			lands.add(b.build());
		}
		return new Result(lands, skipped);
	}

	private static boolean addMarker(JsonObject marker, Map<String, Builder> byLand) {
		if (!"poly".equals(marker.get("type").getAsString())) {
			return false;
		}
		JsonObject data = marker.getAsJsonObject("data");
		String landId = data.get("key").getAsString().split("_", 2)[0];
		String tooltip = marker.getAsJsonObject("options").getAsJsonObject("tooltip").get("content").getAsString();
		Matcher nameMatcher = NAME.matcher(tooltip);
		if (!nameMatcher.find()) {
			return false;
		}
		List<int[]> rings = new ArrayList<>();
		for (JsonElement line : data.getAsJsonArray("polylines")) {
			JsonArray pts = line.getAsJsonObject().getAsJsonArray("points");
			int[] ring = new int[pts.size() * 2];
			for (int i = 0; i < pts.size(); i++) {
				ring[2 * i] = pts.get(i).getAsJsonObject().get("x").getAsInt();
				ring[2 * i + 1] = pts.get(i).getAsJsonObject().get("z").getAsInt();
			}
			rings.add(ring);
		}
		if (rings.isEmpty()) {
			return false;
		}
		Builder b = byLand.computeIfAbsent(landId, k -> new Builder(unescape(nameMatcher.group(1)).strip(), tooltip));
		b.polygons.add(new Polygon(rings));
		return true;
	}

	private static final class Builder {
		final String name;
		final String owner;
		final List<String> members = new ArrayList<>();
		final int chunks;
		final List<Polygon> polygons = new ArrayList<>();

		Builder(String name, String tooltip) {
			this.name = name;
			Matcher d = DESCRIPTION.matcher(tooltip);
			Matcher o = d.find() ? OWNER.matcher(unescape(d.group(1)).strip()) : null;
			this.owner = o != null && o.matches() ? o.group(1).strip() : null;
			Matcher c = CHUNKS.matcher(tooltip);
			this.chunks = c.find() ? Integer.parseInt(c.group(1)) : 0;
			Matcher p = PLAYERS.matcher(tooltip);
			if (p.find()) {
				for (String s : unescape(p.group(1)).split(",")) {
					if (!s.isBlank()) {
						members.add(s.strip());
					}
				}
			}
		}

		Land build() {
			return new Land(name, owner, members, chunks, polygons);
		}
	}

	private static String unescape(String s) {
		return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'");
	}
}
