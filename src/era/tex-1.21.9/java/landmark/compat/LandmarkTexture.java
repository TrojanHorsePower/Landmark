package landmark.compat;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.function.Supplier;
import landmark.Branding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * A dynamic texture that knows its own identifier and whether it is drawn with smoothing ({@code linear}) or crisp
 * (nearest-neighbour, the default). Register it before drawing and release it when done. For 1.21.9 and 1.21.10.
 */
public final class LandmarkTexture extends DynamicTexture {
	private final ResourceLocation id;

	/** {@code path} identifies the texture within this mod's namespace, for example {@code tiles/overworld/3_4}. */
	public LandmarkTexture(Supplier<String> name, NativeImage image, String path, boolean linear) {
		super(name, image);
		this.id = ResourceLocation.fromNamespaceAndPath(Branding.MOD_ID, path);
		if (linear) {
			setFilter(true, false);
		}
	}

	public ResourceLocation id() {
		return id;
	}

	public void register(Minecraft mc) {
		mc.getTextureManager().register(id, this);
	}

	public void release(Minecraft mc) {
		mc.getTextureManager().release(id);
	}
}
