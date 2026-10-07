package landmark.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import landmark.data.ColorKey;
import landmark.data.ColorMath;
import org.junit.jupiter.api.Test;

class ClaimColorsTest {
	private static final int TINT = ColorKey.CLAIM_TINT.defaultArgb;

	@Test
	void normalColoursAreStableTranslucentAndVaryPerLand() {
		int c = ClaimColors.normal("Sample", TINT);
		assertEquals(c, ClaimColors.normal("Sample", TINT));
		assertEquals(0x66, c >>> 24);
		assertNotEquals(ClaimColors.normal("Sample", TINT), ClaimColors.normal("Other", TINT));
	}

	@Test
	void theTintControlsSaturationBrightnessAndOpacity() {
		int dull = ClaimColors.normal("Sample", 0x66B88484);
		int vivid = ClaimColors.normal("Sample", 0xCCFF0000);
		assertEquals(0xCC, vivid >>> 24);
		assertEquals(1f, ColorMath.toHsv(vivid)[1], 0.01);
		assertNotEquals(dull, vivid);
		assertEquals(ColorMath.toHsv(dull)[0], ColorMath.toHsv(vivid)[0], 0.01, "the hue still comes from the land's name");
	}

	@Test
	void defaultTintKeepsTheOriginalLook() {
		float[] hsv = ColorMath.toHsv(ClaimColors.normal("x", TINT));
		assertEquals(0.28f, hsv[1], 0.02);
		assertEquals(0.72f, hsv[2], 0.02);
	}
}
