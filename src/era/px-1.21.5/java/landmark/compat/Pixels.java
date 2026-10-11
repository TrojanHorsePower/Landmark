package landmark.compat;

import com.mojang.blaze3d.platform.NativeImage;

/** Reads and writes NativeImage pixels; the pixel methods are named and ordered differently between versions. */
public final class Pixels {
	private Pixels() {}

	/** Sets a pixel from an ARGB colour. */
	public static void setArgb(NativeImage image, int x, int y, int argb) {
		image.setPixel(x, y, argb);
	}

	/** Sets a pixel from an ABGR colour (alpha, blue, green, red from the top byte down). */
	public static void setAbgr(NativeImage image, int x, int y, int abgr) {
		image.setPixelABGR(x, y, abgr);
	}

	/** All pixels as ABGR, row by row. */
	public static int[] abgr(NativeImage image) {
		return image.getPixelsABGR();
	}
}
