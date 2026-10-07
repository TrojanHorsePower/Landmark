package landmark.map;

import landmark.data.ColorMath;

/** Per-land colours for claims that are not highlighted. */
public final class ClaimColors {
	private ClaimColors() {}

	/**
	 * A translucent tint that varies per land so neighbours can be told apart. The land's name picks the hue; the saturation,
	 * brightness and opacity come from {@code tintArgb} (the "other claims" colour in the settings).
	 */
	public static int normal(String landName, int tintArgb) {
		float hue = Math.floorMod(landName.hashCode() * 0x9E3779B1, 360) / 360f;
		float[] tint = ColorMath.toHsv(tintArgb);
		return ColorMath.withAlpha(ColorMath.fromHsv(hue, tint[1], tint[2]), tintArgb >>> 24);
	}
}
