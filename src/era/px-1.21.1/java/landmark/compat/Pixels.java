package landmark.compat;

import com.mojang.blaze3d.platform.NativeImage;

/** Reads and writes NativeImage pixels; the pixel methods are named and ordered differently between versions. */
public final class Pixels {
	private Pixels() {}

	/** Sets a pixel from an ARGB colour. */
	public static void setArgb(NativeImage image, int x, int y, int argb) {
		image.setPixelRGBA(x, y, swapRb(argb)); // setPixelRGBA takes ABGR
	}

	/** Sets a pixel from an ABGR colour (alpha, blue, green, red from the top byte down). */
	public static void setAbgr(NativeImage image, int x, int y, int abgr) {
		image.setPixelRGBA(x, y, abgr);
	}

	private static int swapRb(int c) {
		return (c & 0xFF00FF00) | ((c & 0xFF) << 16) | ((c >> 16) & 0xFF);
	}

	/** All pixels as ABGR, row by row. */
	public static int[] abgr(NativeImage image) {
		return image.getPixelsRGBA();
	}
}
