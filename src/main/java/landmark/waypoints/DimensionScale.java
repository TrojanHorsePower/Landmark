package landmark.waypoints;

import org.jspecify.annotations.Nullable;

/**
 * The 8:1 coordinate ratio between the Overworld and the Nether. The End and any other dimension have no such ratio, so
 * nothing is converted to or from them.
 */
public final class DimensionScale {
	private DimensionScale() {}

	public static final String OVERWORLD = "minecraft:overworld";
	public static final String NETHER = "minecraft:the_nether";

	/** Whether coordinates can be converted between these two (different) dimensions. */
	public static boolean pairs(String from, String to) {
		return (from.equals(OVERWORLD) && to.equals(NETHER)) || (from.equals(NETHER) && to.equals(OVERWORLD));
	}

	/** The {x, z} in {@code to} for a position in {@code from}, or null when the two do not convert. */
	public static int @Nullable [] convert(int x, int z, String from, String to) {
		if (!pairs(from, to)) {
			return null;
		}
		if (from.equals(NETHER)) {
			return new int[] {x * 8, z * 8};
		}
		return new int[] {Math.floorDiv(x, 8), Math.floorDiv(z, 8)};
	}
}
