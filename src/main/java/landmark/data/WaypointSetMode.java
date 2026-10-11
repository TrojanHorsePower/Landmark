package landmark.data;

/** Which external waypoints the map draws. Stored in the config by {@link #id}; anything unrecognised means {@link #ACTIVE}. */
public enum WaypointSetMode {
	/** Only the set (Xaero's) or group (JourneyMap) the other mod is showing, as that mod does. */
	ACTIVE("active", "landmark.waypointsets.mode.active"),
	/** Every waypoint. */
	ALL("all", "landmark.waypointsets.mode.all"),
	/** Everything except the sets the player switched off. */
	PICK("pick", "landmark.waypointsets.mode.pick");

	public final String id;
	public final String translationKey;

	WaypointSetMode(String id, String translationKey) {
		this.id = id;
		this.translationKey = translationKey;
	}

	public static WaypointSetMode byId(String id) {
		for (WaypointSetMode m : values()) {
			if (m.id.equals(id)) {
				return m;
			}
		}
		return ACTIVE;
	}
}
