package landmark.data;

import java.util.OptionalInt;
import java.util.regex.Pattern;

/** Hex parsing/formatting and RGB/HSV conversion for the colour settings. Colours are ARGB ints. */
public final class ColorMath {
	private static final Pattern HEX = Pattern.compile("#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})");

	private ColorMath() {}

	/**
	 * Parses {@code #RGB}, {@code #RRGGBB} or (when {@code allowAlpha}) {@code #RRGGBBAA}; the leading {@code #} is optional.
	 * Without an alpha part the colour is fully opaque.
	 */
	public static OptionalInt parseHex(String text, boolean allowAlpha) {
		var m = HEX.matcher(text.strip());
		if (!m.matches()) {
			return OptionalInt.empty();
		}
		String h = m.group(1);
		if (h.length() == 3) {
			h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
		}
		if (h.length() == 8 && !allowAlpha) {
			return OptionalInt.empty();
		}
		int rgb = Integer.parseInt(h.substring(0, 6), 16);
		int alpha = h.length() == 8 ? Integer.parseInt(h.substring(6, 8), 16) : 0xFF;
		return OptionalInt.of(alpha << 24 | rgb);
	}

	/** {@code #RRGGBB}, or {@code #RRGGBBAA} when {@code withAlpha}. Upper case. */
	public static String format(int argb, boolean withAlpha) {
		String rgb = String.format("#%06X", argb & 0xFFFFFF);
		return withAlpha ? rgb + String.format("%02X", argb >>> 24) : rgb;
	}

	/** Returns {hue, saturation, value}, each 0..1, for the RGB part of {@code argb}. */
	public static float[] toHsv(int argb) {
		float r = (argb >> 16 & 255) / 255f;
		float g = (argb >> 8 & 255) / 255f;
		float b = (argb & 255) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float d = max - min;
		float h;
		if (d == 0) {
			h = 0;
		} else if (max == r) {
			h = ((g - b) / d % 6 + 6) % 6 / 6f;
		} else if (max == g) {
			h = ((b - r) / d + 2) / 6f;
		} else {
			h = ((r - g) / d + 4) / 6f;
		}
		return new float[] {h, max == 0 ? 0 : d / max, max};
	}

	/** Opaque ARGB for the given hue, saturation and value (each 0..1; hue wraps). */
	public static int fromHsv(float h, float s, float v) {
		h = ((h % 1f) + 1f) % 1f * 6f;
		float c = v * s;
		float x = c * (1 - Math.abs(h % 2 - 1));
		float m = v - c;
		float r;
		float g;
		float b;
		switch ((int) h) {
			case 0 -> { r = c; g = x; b = 0; }
			case 1 -> { r = x; g = c; b = 0; }
			case 2 -> { r = 0; g = c; b = x; }
			case 3 -> { r = 0; g = x; b = c; }
			case 4 -> { r = x; g = 0; b = c; }
			default -> { r = c; g = 0; b = x; }
		}
		return 0xFF000000 | Math.round((r + m) * 255) << 16 | Math.round((g + m) * 255) << 8 | Math.round((b + m) * 255);
	}

	public static int withAlpha(int argb, int alpha) {
		return (alpha & 255) << 24 | argb & 0xFFFFFF;
	}
}
