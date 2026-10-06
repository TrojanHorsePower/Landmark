package landmark.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Decodes a map tile. NativeImage.read only accepts PNG (it validates the PNG signature), so JPEG tiles go through
 * LWJGL's stb_image directly, which the game already bundles.
 */
final class TileDecoder {
	private TileDecoder() {}

	static NativeImage decode(byte[] data) throws IOException {
		if (data.length > 8 && (data[0] & 0xFF) == 0x89 && data[1] == 'P') {
			return NativeImage.read(data);
		}
		ByteBuffer in = MemoryUtil.memAlloc(data.length);
		try {
			in.put(data).flip();
			try (MemoryStack stack = MemoryStack.stackPush()) {
				IntBuffer w = stack.mallocInt(1);
				IntBuffer h = stack.mallocInt(1);
				IntBuffer channels = stack.mallocInt(1);
				ByteBuffer pixels = STBImage.stbi_load_from_memory(in, w, h, channels, 4);
				if (pixels == null) {
					throw new IOException("Could not decode image: " + STBImage.stbi_failure_reason());
				}
				try {
					int width = w.get(0);
					int height = h.get(0);
					NativeImage image = new NativeImage(width, height, false);
					pixels.order(ByteOrder.LITTLE_ENDIAN); // R,G,B,A in memory is 0xAABBGGRR as an int, i.e. ABGR
					for (int y = 0; y < height; y++) {
						for (int x = 0; x < width; x++) {
							image.setPixelABGR(x, y, pixels.getInt((y * width + x) * 4));
						}
					}
					return image;
				} finally {
					STBImage.stbi_image_free(pixels);
				}
			}
		} finally {
			MemoryUtil.memFree(in);
		}
	}
}
