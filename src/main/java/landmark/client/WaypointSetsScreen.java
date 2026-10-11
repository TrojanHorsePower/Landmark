package landmark.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import landmark.data.ColorKey;
import landmark.data.WaypointSetMode;
import landmark.waypoints.ExternalWaypoint;
import landmark.waypoints.WaypointSets;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Chooses which Xaero's Minimap sets and JourneyMap groups are drawn on the map. The default follows the other mod (only its
 * active set); ticking or unticking a set switches to a custom choice, which keeps new sets visible until they are unticked.
 */
public class WaypointSetsScreen extends Screen {
	private static final int ROW_H = 14;

	private record Row(boolean header, String text, WaypointSets.Group group) {}

	private final Screen parent;
	private Button dimensionButton;
	private final LandmarkState state = LandmarkState.get();
	private List<Row> rows = List.of();
	private int scroll;
	private int listTop;
	private int listBottom;
	private CycleButton<WaypointSetMode> modeButton;

	public WaypointSetsScreen(Screen parent) {
		super(Component.translatable("landmark.waypointsets.title"));
		this.parent = parent;
	}

	/** The dimension whose sets are listed: the same one the map shows (and the same Auto / pinned setting). */
	private String dimension() {
		return ViewDimension.resolve(state.config, ViewDimension.player(minecraft));
	}

	private static int col(ColorKey key) {
		return Palette.get(key);
	}

	private int contentWidth() {
		return Math.min(width - 24, 340);
	}

	private int contentLeft() {
		return (width - contentWidth()) / 2;
	}

	@Override
	protected void init() {
		int w = contentWidth();
		int x = contentLeft();
		dimensionButton = addRenderableWidget(Button.builder(Component.empty(), b -> cycleDimension()).bounds(x, 24, w, 20).build());
		modeButton = addRenderableWidget(CycleButton.<WaypointSetMode>builder(m -> Component.translatable(m.translationKey), state.config.waypointMode())
			.withValues(WaypointSetMode.values())
			.create(x, 48, w, 20, Component.translatable("landmark.waypointsets.show"), (b, v) -> {
				state.config.waypointSets = v.id;
				state.saveConfig();
			}));
		addRenderableWidget(CycleButton.onOffBuilder(state.config.crossDimensionWaypoints)
			.create(x, 72, w, 20, Component.translatable("landmark.waypointsets.cross"), (b, v) -> {
				state.config.crossDimensionWaypoints = v;
				state.saveConfig();
				scroll = 0;
				buildRows();
			}));
		int third = (w - 8) / 3;
		int y = height - 26;
		addRenderableWidget(Button.builder(Component.translatable("landmark.waypointsets.showall"), b -> setAll(false)).bounds(x, y, third, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("landmark.waypointsets.hideall"), b -> setAll(true)).bounds(x + third + 4, y, third, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("landmark.picker.done"), b -> onClose()).bounds(x + 2 * (third + 4), y, w - 2 * (third + 4), 20).build());
		listTop = 100;
		listBottom = Math.max(listTop + ROW_H, height - 32);
		buildRows();
	}

	private void cycleDimension() {
		ViewDimension.cycle(state.config);
		state.saveConfig();
		scroll = 0;
		buildRows();
	}

	private void buildRows() {
		// Only the sets that have waypoints in the viewed dimension are listed.
		var groups = state.external.groups(dimension()).stream().filter(g -> g.count() > 0).toList();
		boolean twoSources = groups.stream().map(WaypointSets.Group::source).distinct().count() > 1;
		List<Row> out = new ArrayList<>();
		ExternalWaypoint.Source last = null;
		for (var g : groups) {
			if (twoSources && g.source() != last) {
				out.add(new Row(true, g.source().displayName, null));
			}
			last = g.source();
			out.add(new Row(false, "", g));
		}
		rows = out;
	}

	/** Whether this group is drawn right now, as the checkbox shows it (for the viewed dimension). */
	private boolean shownNow(WaypointSets.Group g) {
		return switch (state.config.waypointMode()) {
			case ACTIVE -> g.active();
			case ALL -> true;
			case PICK -> !state.config.hiddenWaypointSets.contains(g.key());
		};
	}

	/** Ticks or unticks one group, first turning the current view (active set or all) into a custom choice. */
	private void toggle(WaypointSets.Group g) {
		var cfg = state.config;
		if (cfg.waypointMode() != WaypointSetMode.PICK) {
			Set<String> hidden = new HashSet<>();
			for (var other : state.external.groups(dimension())) {
				boolean shown = cfg.waypointMode() == WaypointSetMode.ALL || other.active() || (other.count() == 0 && other.activeAnywhere());
				if (!shown) {
					hidden.add(other.key());
				}
			}
			cfg.hiddenWaypointSets = new ArrayList<>(hidden);
			cfg.waypointSets = WaypointSetMode.PICK.id;
		}
		if (!cfg.hiddenWaypointSets.remove(g.key())) {
			cfg.hiddenWaypointSets.add(g.key());
		}
		state.saveConfig();
		modeButton.setValue(WaypointSetMode.PICK);
	}

	private void setAll(boolean hide) {
		var cfg = state.config;
		if (hide) {
			cfg.hiddenWaypointSets = new ArrayList<>();
			for (var g : state.external.groups(dimension())) {
				cfg.hiddenWaypointSets.add(g.key());
			}
			cfg.waypointSets = WaypointSetMode.PICK.id;
		} else {
			cfg.hiddenWaypointSets = new ArrayList<>();
			cfg.waypointSets = WaypointSetMode.ALL.id;
		}
		state.saveConfig();
		modeButton.setValue(cfg.waypointMode());
	}

	private int visibleRows() {
		return Math.max(1, (listBottom - listTop) / ROW_H);
	}

	private int maxScroll() {
		return Math.max(0, rows.size() - visibleRows());
	}

	private String fit(String text, int room) {
		if (font.width(text) <= room) {
			return text;
		}
		return font.plainSubstrByWidth(text, Math.max(0, room - font.width("\u2026"))) + "\u2026";
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
		g.fill(0, 0, width, height, col(ColorKey.MAP_BACKGROUND));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
		extractBackground(g, mx, my, partial);
		int x = contentLeft();
		int w = contentWidth();
		g.text(font, title, x, 8, col(ColorKey.LIST_OPEN), false);
		dimensionButton.setMessage(ViewDimension.label(state.config, dimension()));
		if (rows.isEmpty()) {
			g.text(font, fit(Component.translatable("landmark.waypointsets.none").getString(), w), x, listTop + 4, col(ColorKey.TEXT_MUTED), false);
		}
		scroll = Math.max(0, Math.min(scroll, maxScroll()));
		g.enableScissor(0, listTop, width, listBottom);
		for (int r = 0; r < visibleRows() && scroll + r < rows.size(); r++) {
			Row row = rows.get(scroll + r);
			int y = listTop + r * ROW_H;
			if (row.header()) {
				g.text(font, fit(row.text(), w), x, y + 3, col(ColorKey.TEXT_STATUS), false);
				continue;
			}
			var grp = row.group();
			boolean hover = mx >= x && mx < x + w && my >= y && my < y + ROW_H;
			if (hover) {
				g.fill(x - 2, y, x + w + 2, y + ROW_H, col(ColorKey.ROW_HOVER));
			}
			boolean on = shownNow(grp);
			int bx = x + 2;
			int by = y + 3;
			g.fill(bx, by, bx + 8, by + 8, col(ColorKey.TEXT_MUTED));
			g.fill(bx + 1, by + 1, bx + 7, by + 7, col(ColorKey.MAP_BACKGROUND));
			if (on) {
				g.fill(bx + 2, by + 2, bx + 6, by + 6, col(ColorKey.LIST_OPEN));
			}
			String count = String.valueOf(grp.count());
			int countW = font.width(count);
			String name = ExternalWaypoints.displayName(grp.name());
			if (name.isEmpty()) {
				name = Component.translatable("landmark.waypointsets.ungrouped").getString();
			}
			if (grp.active()) {
				name += " (" + Component.translatable("landmark.waypointsets.active").getString() + ")";
			}
			int room = w - 16 - countW - 8;
			int color = grp.count() == 0 ? col(ColorKey.TEXT_DIM) : on ? col(ColorKey.TEXT_PRIMARY) : col(ColorKey.TEXT_MUTED);
			g.text(font, fit(name, room), x + 16, y + 3, color, false);
			g.text(font, count, x + w - countW - 2, y + 3, col(ColorKey.TEXT_DIM), false);
		}
		g.disableScissor();
		if (maxScroll() > 0) {
			int track = listBottom - listTop;
			int barH = Math.max(12, track * visibleRows() / rows.size());
			int barY = listTop + (int) ((track - barH) * (scroll / (double) maxScroll()));
			g.fill(x + w + 4, listTop, x + w + 7, listBottom, 0x40FFFFFF);
			g.fill(x + w + 4, barY, x + w + 7, barY + barH, col(ColorKey.TEXT_STATUS));
		}
		super.extractRenderState(g, mx, my, partial);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		if (super.mouseClicked(e, doubleClick)) {
			return true;
		}
		int x = contentLeft();
		if (e.button() == 0 && e.x() >= x && e.x() < x + contentWidth() && e.y() >= listTop && e.y() < listBottom) {
			int idx = scroll + (int) ((e.y() - listTop) / ROW_H);
			if (idx < rows.size() && !rows.get(idx).header()) {
				toggle(rows.get(idx).group());
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (maxScroll() > 0) {
			scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY) * 2));
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (maxScroll() > 0 && (e.isUp() || e.isDown())) {
			scroll = Math.max(0, Math.min(maxScroll(), scroll + (e.isDown() ? 1 : -1)));
			return true;
		}
		return super.keyPressed(e);
	}

	/** Dev harness: presses the dimension button. */
	void devCycleDimension() {
		cycleDimension();
	}

	/** Dev harness: the listed sets as "name=count". */
	String devRows() {
		return rows.stream().filter(r -> !r.header()).map(r -> r.group().name() + "=" + r.group().count()).toList().toString();
	}

	/** Dev harness: unticks or ticks the nth listed group, as a click would. */
	void devToggle(int groupIndex) {
		int n = 0;
		for (Row r : rows) {
			if (!r.header() && n++ == groupIndex) {
				toggle(r.group());
				return;
			}
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
