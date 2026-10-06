package landmark.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Compact on-disk / over-the-wire form of a {@link ClaimSnapshot}: gzip'd JSON, coordinates divided by a
 * common scale (16 when every coordinate is chunk-aligned) and delta-encoded per ring.
 */
public final class SnapshotCodec {
	public static final int VERSION = 1;

	private SnapshotCodec() {}

	public static byte[] encode(ClaimSnapshot s) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (OutputStream gz = new GZIPOutputStream(out)) {
			gz.write(toJson(s).toString().getBytes(StandardCharsets.UTF_8));
		}
		return out.toByteArray();
	}

	public static ClaimSnapshot decode(byte[] data) throws IOException {
		try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(data));
			InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			return fromJson(JsonParser.parseReader(r).getAsJsonObject());
		} catch (RuntimeException e) {
			throw new IOException("Malformed snapshot: " + e.getMessage(), e);
		}
	}

	static JsonObject toJson(ClaimSnapshot s) {
		int scale = commonScale(s);
		JsonObject o = new JsonObject();
		o.addProperty("v", VERSION);
		o.addProperty("dimension", s.dimension());
		o.addProperty("fetchedAt", s.fetchedAtMillis());
		o.addProperty("source", s.source());
		o.addProperty("scale", scale);
		JsonArray lands = new JsonArray();
		for (Land l : s.lands()) {
			JsonObject lo = new JsonObject();
			lo.addProperty("n", l.name());
			if (l.owner() != null) {
				lo.addProperty("o", l.owner());
			}
			JsonArray members = new JsonArray();
			l.members().forEach(members::add);
			lo.add("m", members);
			lo.addProperty("c", l.chunks());
			JsonArray polys = new JsonArray();
			for (Polygon p : l.polygons()) {
				JsonArray rings = new JsonArray();
				for (int[] ring : p.rings()) {
					JsonArray flat = new JsonArray();
					int px = 0;
					int pz = 0;
					for (int i = 0; i + 1 < ring.length; i += 2) {
						int x = ring[i] / scale;
						int z = ring[i + 1] / scale;
						flat.add(x - px);
						flat.add(z - pz);
						px = x;
						pz = z;
					}
					rings.add(flat);
				}
				polys.add(rings);
			}
			lo.add("p", polys);
			lands.add(lo);
		}
		o.add("lands", lands);
		return o;
	}

	static ClaimSnapshot fromJson(JsonObject o) throws IOException {
		if (o.get("v").getAsInt() != VERSION) {
			throw new IOException("Unsupported snapshot version " + o.get("v"));
		}
		int scale = o.get("scale").getAsInt();
		List<Land> lands = new ArrayList<>();
		for (JsonElement le : o.getAsJsonArray("lands")) {
			JsonObject lo = le.getAsJsonObject();
			List<String> members = new ArrayList<>();
			lo.getAsJsonArray("m").forEach(m -> members.add(m.getAsString()));
			List<Polygon> polys = new ArrayList<>();
			for (JsonElement pe : lo.getAsJsonArray("p")) {
				List<int[]> rings = new ArrayList<>();
				for (JsonElement re : pe.getAsJsonArray()) {
					JsonArray flat = re.getAsJsonArray();
					int[] ring = new int[flat.size()];
					int px = 0;
					int pz = 0;
					for (int i = 0; i + 1 < ring.length; i += 2) {
						px += flat.get(i).getAsInt();
						pz += flat.get(i + 1).getAsInt();
						ring[i] = px * scale;
						ring[i + 1] = pz * scale;
					}
					rings.add(ring);
				}
				polys.add(new Polygon(rings));
			}
			lands.add(new Land(lo.get("n").getAsString(), lo.has("o") ? lo.get("o").getAsString() : null, members,
				lo.get("c").getAsInt(), polys));
		}
		return new ClaimSnapshot(o.get("dimension").getAsString(), o.get("fetchedAt").getAsLong(), o.get("source").getAsString(), lands);
	}

	private static int commonScale(ClaimSnapshot s) {
		for (Land l : s.lands()) {
			for (Polygon p : l.polygons()) {
				for (int[] ring : p.rings()) {
					for (int v : ring) {
						if (Math.floorMod(v, 16) != 0) {
							return 1;
						}
					}
				}
			}
		}
		return 16;
	}
}
