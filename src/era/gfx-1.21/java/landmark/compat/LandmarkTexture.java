package landmark.compat;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * A dynamic texture that remembers its own identifier and whether it is drawn with smoothing ({@code linear}) or crisp
 * (nearest-neighbour, the default). Register it with the texture manager under {@link #id()}.
 */
public final class LandmarkTexture extends DynamicTexture {
	private final Identifier id;

	public LandmarkTexture(Supplier<String> name, NativeImage image, Identifier id, boolean linear) {
		super(name, image);
		this.id = id;
		if (linear) {
			// the game draws a texture registered under an identifier with the texture's own sampler
			this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		}
	}

	public Identifier id() {
		return id;
	}
}
