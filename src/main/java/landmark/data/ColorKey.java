package landmark.data;

/**
 * Every colour the mod draws with, its default, and where it appears in the settings. The display name of a colour is the
 * translation key {@code landmark.color.<id>}.
 */
public enum ColorKey {
	// Map
	MAP_BACKGROUND("map_background", Group.MAP, 0xFF101418, false),
	OPEN_FILL("open_fill", Group.MAP, 0xB8FFB000, true),
	OPEN_OUTLINE("open_outline", Group.MAP, 0xFFFFD34D, false),
	HOVER_OUTLINE("hover_outline", Group.MAP, 0xFFFFFFFF, false),
	SELECTED_OUTLINE("selected_outline", Group.MAP, 0xFF4DE1FF, false),
	/** Every claim that is not an open-spawn land. */
	OTHER_LANDS("other_lands", Group.MAP, 0x668C9BB0, true),
	PIN("pin", Group.MAP, 0xFF40E0FF, false),
	PIN_BORDER("pin_border", Group.MAP, 0xFF000000, false),
	PLAYER("player", Group.MAP, 0xFFFF3B30, false),
	PLAYER_BORDER("player_border", Group.MAP, 0xFF000000, false),
	// Side panel and list
	PANEL_BACKGROUND("panel_background", Group.PANEL, 0xFF1B2128, false),
	PANEL_BORDER("panel_border", Group.PANEL, 0xFF000000, false),
	ROW_SELECTED("row_selected", Group.PANEL, 0xFF2C4A5C, false),
	ROW_HOVER("row_hover", Group.PANEL, 0xFF2A323B, false),
	LIST_OPEN("list_open", Group.PANEL, 0xFFFFD34D, false),
	LIST_CLOSED("list_closed", Group.PANEL, 0xFF8C96A0, false),
	// Text
	TEXT_PRIMARY("text_primary", Group.TEXT, 0xFFFFFFFF, false),
	TEXT_BODY("text_body", Group.TEXT, 0xFFE6EAEE, false),
	TEXT_STATUS("text_status", Group.TEXT, 0xFF9AA5B0, false),
	TEXT_MUTED("text_muted", Group.TEXT, 0xFFAAAAAA, false),
	TEXT_DIM("text_dim", Group.TEXT, 0xFF6E7781, false),
	TEXT_INFO("text_info", Group.TEXT, 0xFF9AD0FF, false),
	TEXT_ERROR("text_error", Group.TEXT, 0xFFFF6B6B, false),
	// Status bar and tooltip
	STATUS_BACKGROUND("status_background", Group.OVERLAY, 0xE0101418, true),
	TOOLTIP_BACKGROUND("tooltip_background", Group.OVERLAY, 0xE0000000, true),
	TOOLTIP_TEXT("tooltip_text", Group.OVERLAY, 0xFFB8C0C8, false);

	public enum Group {
		MAP("landmark.colors.group.map"),
		PANEL("landmark.colors.group.panel"),
		TEXT("landmark.colors.group.text"),
		OVERLAY("landmark.colors.group.overlay");

		public final String translationKey;

		Group(String translationKey) {
			this.translationKey = translationKey;
		}
	}

	public final String id;
	public final Group group;
	public final int defaultArgb;
	/** Whether the opacity can be changed; otherwise the colour is always fully opaque. */
	public final boolean hasAlpha;

	ColorKey(String id, Group group, int defaultArgb, boolean hasAlpha) {
		this.id = id;
		this.group = group;
		this.defaultArgb = defaultArgb;
		this.hasAlpha = hasAlpha;
	}

	public String translationKey() {
		return "landmark.color." + id;
	}

	public static ColorKey byId(String id) {
		for (ColorKey k : values()) {
			if (k.id.equals(id)) {
				return k;
			}
		}
		return null;
	}
}
