package landmark.map;

import java.awt.Color;

/** ARGB colours for claim cells. Open-spawn claims are a saturated amber so they stand out from the muted rest. */
public final class ClaimColors {
	public static final int OPEN_FILL = 0xB8FFB000;
	public static final int OPEN_OUTLINE = 0xFFFFD34D;
	public static final int HOVER_OUTLINE = 0xFFFFFFFF;
	public static final int SELECTED_OUTLINE = 0xFF4DE1FF;

	private ClaimColors() {}

	/** A muted, translucent tint that varies per land so neighbours can be told apart. */
	public static int normal(String landName) {
		float hue = (Math.floorMod(landName.hashCode() * 0x9E3779B1, 360)) / 360f;
		int rgb = Color.HSBtoRGB(hue, 0.28f, 0.72f) & 0x00FFFFFF;
		return 0x66000000 | rgb;
	}
}
