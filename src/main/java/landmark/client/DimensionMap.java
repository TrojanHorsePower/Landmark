package landmark.client;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import landmark.data.ClaimIndex;
import landmark.data.DataStore;
import landmark.data.Land;
import landmark.map.ChunkOwnerGrid;

/** Everything derived from one dimension's stored claim data. */
public final class DimensionMap {
	public final DataStore.Stored stored;
	public final ClaimIndex index;
	public final ChunkOwnerGrid grid;
	private final Map<String, Integer> landIndexByName = new HashMap<>();

	public DimensionMap(DataStore.Stored stored) {
		this.stored = stored;
		this.index = new ClaimIndex(stored.snapshot().lands());
		this.grid = new ChunkOwnerGrid(stored.snapshot().lands());
		for (int i = 0; i < grid.lands().size(); i++) {
			landIndexByName.putIfAbsent(grid.lands().get(i).name().strip().toLowerCase(Locale.ROOT), i);
		}
	}

	public Land land(int i) {
		return grid.lands().get(i);
	}

	/** Index of the land with this name in {@code grid.lands()}, or -1. */
	public int indexOf(String name) {
		return landIndexByName.getOrDefault(name.strip().toLowerCase(Locale.ROOT), -1);
	}
}
