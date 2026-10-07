package landmark.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Spawn positions the player has actually arrived at after a {@code /lands spawn}, kept per land and dimension.
 * They fill gaps when claim data is old or missing; they are the player's own observations, stored locally.
 */
public final class LearnedSpawns {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type LIST = new TypeToken<List<Spawn>>() {}.getType();

	public record Spawn(String land, String dimension, int x, int y, int z, long atMillis) {}

	private final Map<String, Spawn> spawns = new LinkedHashMap<>();

	private static String key(String land, String dimension) {
		return dimension + "|" + land.strip().toLowerCase(Locale.ROOT);
	}

	public void record(Spawn s) {
		spawns.put(key(s.land(), s.dimension()), s);
	}

	public Optional<Spawn> get(String land, String dimension) {
		return Optional.ofNullable(spawns.get(key(land, dimension)));
	}

	public List<Spawn> all(String dimension) {
		List<Spawn> out = new ArrayList<>();
		for (Spawn s : spawns.values()) {
			if (s.dimension().equals(dimension)) {
				out.add(s);
			}
		}
		return out;
	}

	public int size() {
		return spawns.size();
	}

	public void clear() {
		spawns.clear();
	}

	public static LearnedSpawns load(Path file) {
		LearnedSpawns r = new LearnedSpawns();
		try {
			List<Spawn> list = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), LIST);
			if (list != null) {
				for (Spawn s : list) {
					if (s != null && s.land() != null && s.dimension() != null) {
						r.record(s);
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			// missing or unreadable: start empty
		}
		return r;
	}

	public void save(Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(tmp, GSON.toJson(new ArrayList<>(spawns.values())), StandardCharsets.UTF_8);
		Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
	}
}
