package landmark.data;

import java.util.List;

/**
 * Claims for one dimension at one point in time.
 *
 * @param dimension dimension id, e.g. {@code minecraft:overworld}
 * @param fetchedAtMillis when the data was exported, epoch millis
 * @param source human-readable origin shown to the player
 */
public record ClaimSnapshot(String dimension, long fetchedAtMillis, String source, List<Land> lands) {
	public ClaimSnapshot {
		lands = List.copyOf(lands);
	}
}
