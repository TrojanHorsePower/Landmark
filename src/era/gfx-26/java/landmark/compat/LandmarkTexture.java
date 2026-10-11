package landmark.compat;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * A dynamic texture that remembers its own identifier and whether it is drawn with smoothing ({@code linear}) or crisp
 * (nearest-neighbour, the default). Register it with the texture manager under {@link #id()}.
 */
public final class LandmarkTexture extends DynamicTexture {
	private final Identifier id;
	private final boolean linear;

	public LandmarkTexture(Supplier<String> name, NativeImage image, Identifier id, boolean linear) {
		super(name, image);
		this.id = id;
		this.linear = linear;
	}

	public Identifier id() {
		return id;
	}

	GpuSampler drawSampler() {
		return linear ? RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR) : getSampler();
	}
}
