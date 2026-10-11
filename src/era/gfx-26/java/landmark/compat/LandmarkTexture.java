package landmark.compat;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.function.Supplier;
import landmark.Branding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * A dynamic texture that remembers its own identifier and whether it is drawn with smoothing ({@code linear}) or crisp
 * (nearest-neighbour, the default). Register it with the texture manager under {@link #id()}.
 */
public final class LandmarkTexture extends DynamicTexture {
	private final Identifier id;
	private final boolean linear;

	/** {@code path} identifies the texture within this mod's namespace, for example {@code tiles/overworld/3_4}. */
	public LandmarkTexture(Supplier<String> name, NativeImage image, String path, boolean linear) {
		super(name, image);
		this.id = Identifier.fromNamespaceAndPath(Branding.MOD_ID, path);
		this.linear = linear;
	}

	public void register(Minecraft mc) {
		mc.getTextureManager().register(id, this);
	}

	public void release(Minecraft mc) {
		mc.getTextureManager().release(id);
	}

	GpuSampler drawSampler() {
		return linear ? RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR) : getSampler();
	}
}
