package landmark.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import landmark.LandmarkClient;
import landmark.map.ChunkOwnerGrid;
import landmark.map.ClaimColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * One chunk-resolution texture per {@value ChunkOwnerGrid#TILE_CELLS}-chunk tile of the owner grid. The textures use
 * nearest-neighbour sampling (DynamicTexture's default), so each chunk stays a crisp square at any zoom.
 */
public final class ClaimLayer implements AutoCloseable {
	public record Tile(int tx, int tz, Identifier id, DynamicTexture texture, int[] cells) {}

	private final Minecraft mc;
	private final DimensionMap map;
	private final int[] normalColors;
	public final List<Tile> tiles = new ArrayList<>();
	private final Map<Long, Tile> byKey = new HashMap<>();

	public ClaimLayer(Minecraft mc, DimensionMap map, String idPrefix) {
		this.mc = mc;
		this.map = map;
		this.normalColors = new int[map.grid.lands().size()];
		for (int i = 0; i < normalColors.length; i++) {
			normalColors[i] = ClaimColors.normal(map.grid.lands().get(i).name());
		}
		map.grid.forEachTile((tx, tz, cells) -> {
			NativeImage image = new NativeImage(ChunkOwnerGrid.TILE_CELLS, ChunkOwnerGrid.TILE_CELLS, true);
			DynamicTexture texture = new DynamicTexture(() -> "landmark claims " + tx + "_" + tz, image);
			Identifier id = Identifier.fromNamespaceAndPath(LandmarkClient.MOD_ID_FOR_ASSETS, idPrefix + "/" + tx + "_" + tz);
			mc.getTextureManager().register(id, texture);
			Tile t = new Tile(tx, tz, id, texture, cells);
			tiles.add(t);
			byKey.put(((long) tx << 32) | (tz & 0xFFFFFFFFL), t);
		});
	}

	/** Repaints every tile; {@code open} holds the indices of lands with a public spawn. */
	public void recolor(Set<Integer> open) {
		for (Tile t : tiles) {
			NativeImage image = t.texture().getPixels();
			if (image == null) {
				continue;
			}
			int n = ChunkOwnerGrid.TILE_CELLS;
			for (int z = 0; z < n; z++) {
				for (int x = 0; x < n; x++) {
					int v = t.cells()[z * n + x];
					int argb = v == 0 ? 0 : open.contains(v - 1) ? ClaimColors.OPEN_FILL : normalColors[v - 1];
					image.setPixel(x, z, argb);
				}
			}
			t.texture().upload();
		}
	}

	@Override
	public void close() {
		for (Tile t : tiles) {
			mc.getTextureManager().release(t.id());
		}
		tiles.clear();
		byKey.clear();
	}
}
