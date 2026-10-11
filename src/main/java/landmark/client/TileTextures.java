package landmark.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import landmark.LandmarkClient;
import net.minecraft.client.Minecraft;
import landmark.compat.LandmarkTexture;
import org.jspecify.annotations.Nullable;

/** Lazily loads imported map tiles (PNG or JPEG) as GPU textures, keeping at most {@code maxEntries} of them. */
public final class TileTextures implements AutoCloseable {
	private static final int LOADS_PER_FRAME = 4;

		private final Minecraft mc;
	private final Path dir;
	private final String idPrefix;
	private int maxEntries;
	private final LinkedHashMap<Long, LandmarkTexture> cache = new LinkedHashMap<>(16, 0.75f, true);
	private final Set<Long> missing = new HashSet<>();
	private int loadedThisFrame;
	public int decodeFailures;

	public TileTextures(Minecraft mc, Path dir, String idPrefix, int maxEntries) {
		this.mc = mc;
		this.dir = dir;
		this.idPrefix = idPrefix;
		this.maxEntries = maxEntries;
	}

	/** Grows the cache so everything currently visible fits; shrinking is left to normal eviction. */
	public void ensureCapacity(int visible) {
		maxEntries = Math.max(maxEntries, Math.min(256, visible + 4));
	}

	public void newFrame() {
		loadedThisFrame = 0;
	}

	public @Nullable LandmarkTexture get(int tx, int tz) {
		long key = ((long) tx << 32) | (tz & 0xFFFFFFFFL);
		LandmarkTexture cached = cache.get(key);
		if (cached != null) {
			return cached;
		}
		if (missing.contains(key) || loadedThisFrame >= LOADS_PER_FRAME) {
			return null;
		}
		loadedThisFrame++;
		Path file = findFile(tx, tz);
		if (file == null) {
			missing.add(key);
			return null;
		}
		try {
			NativeImage image = TileDecoder.decode(Files.readAllBytes(file));
			LandmarkTexture texture = new LandmarkTexture(() -> "landmark tile " + tx + "_" + tz, image, idPrefix + "/" + tx + "_" + tz, true);
			texture.register(mc);
			texture.upload();
			cache.put(key, texture);
			evict();
			return texture;
		} catch (IOException | RuntimeException ex) {
			missing.add(key);
			decodeFailures++;
			LandmarkClient.LOGGER.warn("Could not load map tile {}", file.getFileName(), ex);
			return null;
		}
	}

	private @Nullable Path findFile(int tx, int tz) {
		for (String ext : new String[] {"jpg", "png"}) {
			Path p = dir.resolve(tx + "_" + tz + "." + ext);
			if (Files.isRegularFile(p)) {
				return p;
			}
		}
		return null;
	}

	private void evict() {
		Iterator<Map.Entry<Long, LandmarkTexture>> it = cache.entrySet().iterator();
		while (cache.size() > maxEntries && it.hasNext()) {
			LandmarkTexture old = it.next().getValue();
			it.remove();
			old.release(mc);
		}
	}

	@Override
	public void close() {
		for (LandmarkTexture t : cache.values()) {
			t.release(mc);
		}
		cache.clear();
		missing.clear();
	}
}
