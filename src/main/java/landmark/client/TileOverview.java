package landmark.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import landmark.LandmarkClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Small copies of every imported tile (128px, or 64px when there are very many tiles, as with the Ultra export preset), decoded
 * once in the background. Used when zoomed far out, where full-size tiles would be both wasteful and too numerous to keep cached
 * at once.
 */
public final class TileOverview implements AutoCloseable {
	/** More tiles than this get the smaller copies, to bound memory. */
	private static final int MANY_TILES = 800;
	private static final int UPLOADS_PER_FRAME = 4;
	private static final Pattern NAME = Pattern.compile("(-?\\d+)_(-?\\d+)\\.(png|jpg)");

	private record Ready(int tx, int tz, int[] abgr) {}

	private record Entry(Identifier id, DynamicTexture texture) {}

	private final Minecraft mc;
	private final Path dir;
	private final String idPrefix;
	private final Queue<Ready> done = new ConcurrentLinkedQueue<>();
	private final Map<Long, Entry> textures = new HashMap<>();
	private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "landmark-overview");
		t.setDaemon(true);
		return t;
	});
	private volatile boolean closed;
	private int total;
	private volatile int size = 128;

	public TileOverview(Minecraft mc, Path dir, String idPrefix) {
		this.mc = mc;
		this.dir = dir;
		this.idPrefix = idPrefix;
	}

	public void start() {
		List<Path> files = new ArrayList<>();
		try (Stream<Path> s = Files.list(dir)) {
			s.filter(p -> NAME.matcher(p.getFileName().toString()).matches()).forEach(files::add);
		} catch (IOException e) {
			return;
		}
		total = files.size();
		size = total > MANY_TILES ? 64 : 128;
		final int copySize = size;
		for (Path p : files) {
			worker.submit(() -> {
				if (closed) {
					return;
				}
				Matcher m = NAME.matcher(p.getFileName().toString());
				if (!m.matches()) {
					return;
				}
				try {
					NativeImage image = TileDecoder.decode(Files.readAllBytes(p));
					try {
						done.add(new Ready(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), downscale(image, copySize)));
					} finally {
						image.close();
					}
				} catch (IOException | RuntimeException e) {
					LandmarkClient.LOGGER.warn("Could not build overview of {}", p.getFileName(), e);
				}
			});
		}
	}

	/** Pixel size of the copies: tiles on screen smaller than about 1.5 times this are drawn from them. */
	public int size() {
		return size;
	}

	/** Box-filters the image down (or, for a small image, up) to {@code size} x {@code size}. Pixels are ABGR ints. */
	static int[] downscale(NativeImage image, int size) {
		int w = image.getWidth();
		int h = image.getHeight();
		int[] src = image.getPixelsABGR();
		int[] out = new int[size * size];
		for (int y = 0; y < size; y++) {
			int y0 = y * h / size;
			int y1 = Math.max(y0 + 1, (y + 1) * h / size);
			for (int x = 0; x < size; x++) {
				int x0 = x * w / size;
				int x1 = Math.max(x0 + 1, (x + 1) * w / size);
				long a = 0;
				long b = 0;
				long g = 0;
				long r = 0;
				int n = 0;
				for (int yy = y0; yy < Math.min(h, y1); yy++) {
					for (int xx = x0; xx < Math.min(w, x1); xx++) {
						int p = src[yy * w + xx];
						a += p >>> 24;
						b += (p >> 16) & 255;
						g += (p >> 8) & 255;
						r += p & 255;
						n++;
					}
				}
				out[y * size + x] = n == 0 ? 0 : (int) (a / n) << 24 | (int) (b / n) << 16 | (int) (g / n) << 8 | (int) (r / n);
			}
		}
		return out;
	}

	/** Call once per frame on the client thread: turns finished background work into textures. */
	public void pump() {
		for (int i = 0; i < UPLOADS_PER_FRAME; i++) {
			Ready r = done.poll();
			if (r == null) {
				return;
			}
			int n = size;
			NativeImage image = new NativeImage(n, n, false);
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					image.setPixelABGR(x, y, r.abgr()[y * n + x]);
				}
			}
			Identifier id = Identifier.fromNamespaceAndPath(LandmarkClient.MOD_ID_FOR_ASSETS, idPrefix + "/" + r.tx() + "_" + r.tz());
			DynamicTexture t = new DynamicTexture(() -> "landmark overview " + r.tx() + "_" + r.tz(), image);
			mc.getTextureManager().register(id, t);
			t.upload();
			textures.put(key(r.tx(), r.tz()), new Entry(id, t));
		}
	}

	public @Nullable DynamicTexture get(int tx, int tz) {
		Entry e = textures.get(key(tx, tz));
		return e == null ? null : e.texture();
	}

	public boolean complete() {
		return textures.size() >= total;
	}

	private static long key(int tx, int tz) {
		return ((long) tx << 32) | (tz & 0xFFFFFFFFL);
	}

	@Override
	public void close() {
		closed = true;
		worker.shutdownNow();
		for (Entry e : textures.values()) {
			mc.getTextureManager().release(e.id());
		}
		textures.clear();
		done.clear();
	}
}
