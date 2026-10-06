package landmark.data;

import org.jspecify.annotations.Nullable;

/**
 * Decides when a player has been teleported after asking for a land's spawn: a jump of at least {@value #MIN_JUMP}
 * blocks between two consecutive ticks (or a change of dimension) within {@value #WINDOW_TICKS} ticks of the request.
 * Normal movement, even elytra or falling, stays far below that per tick.
 */
public final class TeleportDetector {
	public static final double MIN_JUMP = 16;
	public static final int WINDOW_TICKS = 20 * 45;

	public record Arrival(String land, String dimension, int x, int y, int z) {}

	private final String land;
	private int ticksLeft = WINDOW_TICKS;
	private boolean havePrev;
	private double px;
	private double py;
	private double pz;
	private @Nullable String pdim;

	public TeleportDetector(String land) {
		this.land = land;
	}

	/** Feed the player's state once per tick. Returns the arrival once a teleport is seen, then keeps returning null. */
	public @Nullable Arrival tick(String dimension, double x, double y, double z) {
		if (ticksLeft <= 0) {
			return null;
		}
		ticksLeft--;
		Arrival result = null;
		if (havePrev) {
			double dx = x - px;
			double dy = y - py;
			double dz = z - pz;
			boolean jumped = dx * dx + dy * dy + dz * dz >= MIN_JUMP * MIN_JUMP;
			if (jumped || !dimension.equals(pdim)) {
				result = new Arrival(land, dimension, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
				ticksLeft = 0;
			}
		}
		havePrev = true;
		px = x;
		py = y;
		pz = z;
		pdim = dimension;
		return result;
	}

	public boolean finished() {
		return ticksLeft <= 0;
	}
}
