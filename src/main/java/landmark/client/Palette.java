package landmark.client;

import landmark.data.ColorKey;
import landmark.data.LandmarkConfig;

/** The colours currently in use, read from the settings. Everything the mod draws asks here instead of hard-coding a colour. */
public final class Palette {
	private static final int[] VALUES = new int[ColorKey.values().length];

	/** Bumped on every change, so screens and cached textures can tell when to repaint. */
	public static int version;

	static {
		for (ColorKey k : ColorKey.values()) {
			VALUES[k.ordinal()] = k.defaultArgb;
		}
	}

	private Palette() {}

	public static int get(ColorKey key) {
		return VALUES[key.ordinal()];
	}

	/** Re-reads every colour from the config. */
	public static void reload(LandmarkConfig config) {
		for (ColorKey k : ColorKey.values()) {
			VALUES[k.ordinal()] = config.color(k);
		}
		version++;
	}
}
