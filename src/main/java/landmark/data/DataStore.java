package landmark.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Where imported data lives on disk, one folder per dimension: {@code <root>/<dimension>/claims.bin},
 * {@code tiles.json} and {@code tiles/}. Nothing here depends on Minecraft.
 */
public final class DataStore {
	private final Path root;

	public DataStore(Path root) {
		this.root = root;
	}

	/** Loaded state for one dimension. {@code tiles} is null when no map tiles were imported. */
	public record Stored(ClaimSnapshot snapshot, ExportManifest.@Nullable Tiles tiles, Path tileDir) {}

	public Path dimensionDir(String dimension) {
		return root.resolve(dimension.replace(':', '_').replace('/', '_'));
	}

	public Optional<Stored> load(String dimension) {
		Path dir = dimensionDir(dimension);
		Path claims = dir.resolve("claims.bin");
		if (!Files.isRegularFile(claims)) {
			return Optional.empty();
		}
		try {
			ClaimSnapshot snapshot = SnapshotCodec.decode(Files.readAllBytes(claims));
			ExportManifest.Tiles tiles = null;
			Path meta = dir.resolve("tiles.json");
			if (Files.isRegularFile(meta)) {
				JsonObject o = JsonParser.parseString(Files.readString(meta, StandardCharsets.UTF_8)).getAsJsonObject();
				tiles = new ExportManifest.Tiles(o.get("blocksPerTile").getAsInt(), o.get("pixelSize").getAsInt());
			}
			return Optional.of(new Stored(snapshot, tiles, dir.resolve("tiles")));
		} catch (IOException | RuntimeException e) {
			return Optional.empty(); // unreadable data is treated as absent; the player can import again
		}
	}

	/** Imports an export zip and stores the result under its dimension. The previous data is replaced only on success. */
	public ExportImporter.Result importZip(Path zip) throws IOException {
		Path staging = root.resolve(".import-staging");
		ExportImporter.deleteRecursively(staging);
		Files.createDirectories(staging);
		try {
			ExportImporter.Result r = ExportImporter.importZip(zip, staging.resolve("tiles"));
			Files.write(staging.resolve("claims.bin"), SnapshotCodec.encode(r.snapshot()));
			if (r.manifest().tiles() != null && r.tileCount() > 0) {
				JsonObject o = new JsonObject();
				o.addProperty("blocksPerTile", r.manifest().tiles().blocksPerTile());
				o.addProperty("pixelSize", r.manifest().tiles().pixelSize());
				Files.writeString(staging.resolve("tiles.json"), o.toString(), StandardCharsets.UTF_8);
			}
			Path target = dimensionDir(r.snapshot().dimension());
			ExportImporter.deleteRecursively(target);
			Files.createDirectories(target.getParent());
			Files.move(staging, target);
			return r;
		} finally {
			ExportImporter.deleteRecursively(staging);
		}
	}
}
