package landmark.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import landmark.data.Ages;
import landmark.data.ColorKey;
import landmark.data.Land;
import landmark.data.OpenSpawns;
import landmark.data.Polygon;
import landmark.map.ChunkOwnerGrid;
import landmark.map.MapView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The map screen. Works as a ladder: with claim data it shows the full map; without it (or in a dimension that has none)
 * it is a searchable list of the lands whose spawn the server currently lists; with neither it still lets the player
 * type a land name and teleport.
 */
public class MapScreen extends Screen {
	private static final int PANEL_W = 190;
	private static final int ROW_H = 11;
	private static final int STATUS_H = 34;
	/** Lands smaller than this on screen (both ways) are outlined as a dot or box instead of edge by edge. */
	private static final int SMALL_LAND_PX = 12;
	/** Other (non-open) lands smaller than this on screen get no outline; their fill is still drawn. */
	private static final int OTHER_MIN_PX = 12;

	private static int col(ColorKey key) {
		return Palette.get(key);
	}

	private record Entry(String name, @Nullable Land land, boolean open, boolean typed) {}

	private final LandmarkState state = LandmarkState.get();
	private final MapView view = new MapView();
	private EditBox search;
	private Button refreshButton;
	private Button fitButton;
	private Button teleportButton;
	private Button helpButton;
	private Button claimsButton;
	private Button settingsButton;
	private List<Entry> entries = List.of();
	private int listScroll;
	private @Nullable String selectedName;

	private String dimension = "";
	private @Nullable DimensionMap map;
	private @Nullable ClaimLayer claims;
	private @Nullable TileTextures tiles;
	private @Nullable TileOverview overview;
	private int builtDataVersion = -1;
	private int builtSpawnsVersion = -1;
	private int builtPaletteVersion = -1;
	private Set<Integer> openIdx = Set.of();
	private boolean viewInitialised;
	private boolean layersBuilt;

	/** Nanoseconds spent building the last frame's draw commands; read by the dev harness. */
	long lastExtractNanos;

	MapView view() {
		return view;
	}

	/** Dev harness: types into the search box. */
	void devSearch(String query) {
		search.setValue(query);
	}

	/** Dev harness: presses the Hide claims button. */
	void devToggleClaims() {
		toggleClaims();
	}

	/** Dev harness: selects the first list entry, as a click would. */
	void devSelectFirst() {
		if (!entries.isEmpty()) {
			select(entries.get(0));
		}
	}

	private double pressX;
	private double pressY;
	private boolean pressedInMap;
	private double dragged;

	public MapScreen() {
		super(Component.translatable("landmark.screen.title"));
	}

	@Override
	protected void init() {
		search = new EditBox(font, 4, 4, PANEL_W - 8, 16, Component.translatable("landmark.search"));
		search.setHint(Component.translatable("landmark.search.hint"));
		search.setMaxLength(48);
		search.setResponder(s -> {
			listScroll = 0;
			rebuildEntries();
		});
		addRenderableWidget(search);
		refreshButton = addRenderableWidget(Button.builder(Component.translatable("landmark.refresh"), b -> refresh())
			.bounds(4, 24, 91, 18).build());
		fitButton = addRenderableWidget(Button.builder(Component.translatable("landmark.fit"), b -> fitToSpawns())
			.bounds(99, 24, 87, 18).build());
		claimsButton = addRenderableWidget(Button.builder(Component.translatable("landmark.claims.hide"), b -> toggleClaims())
			.bounds(4, 46, 91, 18).build());
		settingsButton = addRenderableWidget(Button.builder(Component.translatable("landmark.settings"),
			b -> minecraft.setScreenAndShow(new ConfigScreen(this))).bounds(99, 46, 87, 18).build());
		teleportButton = addRenderableWidget(Button.builder(Component.translatable("landmark.teleport"), b -> teleportToSelection())
			.bounds(4, height - 22, PANEL_W - 8, 18).build());
		helpButton = addRenderableWidget(Button.builder(Component.translatable("landmark.help.button"),
			b -> minecraft.setScreenAndShow(new HelpScreen(this))).bounds(width - 102, 4, 98, 16).build());
		setInitialFocus(search);
		if (state.config.refreshOnOpen && state.openSpawns == null) {
			refresh();
		}
		rebuildEntries();
	}

	private void refresh() {
		state.refreshSpawns(minecraft);
	}

	// ---- data ----

	private void syncData() {
		var level = minecraft.level;
		String dim = level == null ? DevHarness.devDimension : level.dimension().identifier().toString();
		boolean dimChanged = !dim.equals(dimension);
		boolean dataChanged = builtDataVersion != state.dataVersion;
		// Layers are freed whenever another screen replaces this one (help, confirm), so rebuild them on return too.
		if (!layersBuilt || dimChanged || dataChanged) {
			closeLayers();
			layersBuilt = true;
			dimension = dim;
			map = dim.isEmpty() ? null : state.map(dim);
			builtDataVersion = state.dataVersion;
			builtSpawnsVersion = -1;
			if (map != null) {
				String prefix = dim.replace(':', '_').replace('/', '_');
				claims = new ClaimLayer(minecraft, map, "claims/" + prefix);
				if (map.stored.tiles() != null) {
					tiles = new TileTextures(minecraft, map.stored.tileDir(), "tiles/" + prefix, state.config.tileCacheSize);
					overview = new TileOverview(minecraft, map.stored.tileDir(), "overview/" + prefix);
					overview.start();
				}
			}
			if (dimChanged || dataChanged) {
				viewInitialised = false;
			}
		}
		if (builtPaletteVersion != Palette.version) {
			builtPaletteVersion = Palette.version;
			builtSpawnsVersion = -1; // repaint the claim textures with the new colours
		}
		if (builtSpawnsVersion != state.spawnsVersion) {
			builtSpawnsVersion = state.spawnsVersion;
			openIdx = map == null ? Set.of() : state.openIndices(map);
			if (claims != null) {
				claims.recolor(openIdx);
			}
			rebuildEntries();
		}
		if (!viewInitialised) {
			viewInitialised = true;
			view.setViewport(mapWidth(), mapHeight());
			var p = minecraft.player;
			if (map != null && p != null && inClaimBounds(p.getX(), p.getZ())) {
				view.centerOn(p.getX(), p.getZ());
				view.setScale(1.0 / 4);
			} else {
				fitToSpawns();
			}
		}
	}

	private boolean inClaimBounds(double x, double z) {
		return Math.abs(x) < 30000 && Math.abs(z) < 30000;
	}

	private void closeLayers() {
		layersBuilt = false;
		if (claims != null) {
			claims.close();
			claims = null;
		}
		if (tiles != null) {
			tiles.close();
			tiles = null;
		}
		if (overview != null) {
			overview.close();
			overview = null;
		}
	}

	@Override
	public void removed() {
		closeLayers();
	}

	private boolean isOpen(String name) {
		OpenSpawns s = state.openSpawns;
		if (s == null) {
			return false;
		}
		String n = name.strip().toLowerCase(Locale.ROOT);
		return s.names().stream().anyMatch(o -> o.toLowerCase(Locale.ROOT).equals(n));
	}

	private void rebuildEntries() {
		if (search == null) {
			return;
		}
		String q = search.getValue().strip();
		List<Entry> out = new ArrayList<>();
		OpenSpawns spawns = state.openSpawns;
		if (q.isEmpty()) {
			if (spawns != null) {
				for (String n : spawns.names()) {
					out.add(new Entry(n, map == null ? null : map.index.byName(n).orElse(null), true, false));
				}
			}
		} else {
			List<Entry> open = new ArrayList<>();
			List<Entry> closed = new ArrayList<>();
			java.util.Set<String> seen = new java.util.HashSet<>();
			if (map != null) {
				for (Land l : map.index.search(q, 200)) {
					boolean o = isOpen(l.name());
					(o ? open : closed).add(new Entry(l.name(), l, o, false));
					seen.add(l.name().toLowerCase(Locale.ROOT));
				}
			}
			if (spawns != null) {
				String lq = q.toLowerCase(Locale.ROOT);
				for (String n : spawns.names()) {
					if (n.toLowerCase(Locale.ROOT).contains(lq) && seen.add(n.toLowerCase(Locale.ROOT))) {
						open.add(new Entry(n, null, true, false));
					}
				}
			}
			out.addAll(open);
			out.addAll(closed);
			if (out.stream().noneMatch(e -> e.name().equalsIgnoreCase(q))) {
				out.add(new Entry(q, null, false, true));
			}
		}
		entries = out;
	}

	// ---- layout ----

	private int mapWidth() {
		return Math.max(1, width - PANEL_W);
	}

	private int mapHeight() {
		return Math.max(1, height - STATUS_H);
	}

	private boolean inMap(double x, double y) {
		return x >= PANEL_W && y < height - STATUS_H;
	}

	private int listTop() {
		return 70;
	}

	private int listBottom() {
		return height - 48;
	}

	private int visibleRows() {
		return Math.max(1, (listBottom() - listTop()) / ROW_H);
	}

	// ---- rendering ----

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
		g.fill(0, 0, width, height, col(ColorKey.MAP_BACKGROUND));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
		long t0 = System.nanoTime();
		syncData();
		view.setViewport(mapWidth(), mapHeight());
		extractBackground(g, mx, my, partial);
		if (map != null) {
			drawMap(g, mx, my);
		} else {
			drawNoMap(g);
		}
		g.fill(0, 0, PANEL_W, height, col(ColorKey.PANEL_BACKGROUND));
		g.fill(PANEL_W - 1, 0, PANEL_W, height, col(ColorKey.PANEL_BORDER));
		drawList(g, mx, my);
		updateButtons();
		super.extractRenderState(g, mx, my, partial);
		drawStatus(g);
		if (map != null && inMap(mx, my)) {
			drawHoverTooltip(g, mx, my);
		}
		lastExtractNanos = System.nanoTime() - t0;
	}

	private void updateButtons() {
		refreshButton.setMessage(Component.translatable(state.refreshing ? "landmark.refreshing" : "landmark.refresh"));
		refreshButton.active = !state.refreshing;
		fitButton.active = map != null;
		claimsButton.active = map != null;
		claimsButton.setMessage(Component.translatable(state.hideClaims ? "landmark.claims.show" : "landmark.claims.hide"));
		String target = teleportTarget();
		teleportButton.active = target != null;
		teleportButton.setMessage(target == null ? Component.translatable("landmark.teleport")
			: Component.translatable("landmark.teleport.to", target));
	}

	private void drawNoMap(GuiGraphicsExtractor g) {
		int cx = PANEL_W + mapWidth() / 2;
		int cy = mapHeight() / 2;
		String dim = dimension.isEmpty() ? "" : dimension;
		g.centeredText(font, Component.translatable(dim.equals("minecraft:overworld") || dim.isEmpty() ? "landmark.nomap" : "landmark.nomap.dimension", dim), cx, cy - 12, col(ColorKey.TEXT_PRIMARY));
		g.centeredText(font, Component.translatable("landmark.nomap.hint"), cx, cy + 2, col(ColorKey.TEXT_MUTED));
	}

	private void drawMap(GuiGraphicsExtractor g, int mx, int my) {
		int x0 = PANEL_W;
		int y0 = 0;
		int x1 = width;
		int y1 = height - STATUS_H;
		g.enableScissor(x0, y0, x1, y1);
		int mapX = x0;
		view.setViewport(mapWidth(), mapHeight());
		double minWX = view.screenToWorldX(0);
		double maxWX = view.screenToWorldX(mapWidth());
		double minWZ = view.screenToWorldZ(0);
		double maxWZ = view.screenToWorldZ(mapHeight());

		if (tiles != null && map != null && map.stored.tiles() != null) {
			int bpt = map.stored.tiles().blocksPerTile();
			GpuSampler linear = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
			int tx0 = Math.floorDiv((int) minWX, bpt);
			int tx1 = Math.floorDiv((int) maxWX, bpt);
			int tz0 = Math.floorDiv((int) minWZ, bpt);
			int tz1 = Math.floorDiv((int) maxWZ, bpt);
			// Far out, a tile is only a few hundred pixels wide: use the small preloaded copies instead of
			// full-size tiles, which would be too many to keep cached and would be reloaded every frame.
			boolean far = overview != null && bpt * view.scale() <= overview.size() * 1.5;
			if (far) {
				overview.pump();
			} else {
				tiles.ensureCapacity((tx1 - tx0 + 1) * (tz1 - tz0 + 1));
				tiles.newFrame();
			}
			for (int tx = tx0; tx <= tx1; tx++) {
				for (int tz = tz0; tz <= tz1; tz++) {
					DynamicTexture t = far ? overview.get(tx, tz) : tiles.get(tx, tz);
					if (t != null) {
						int sx0 = mapX + (int) Math.floor(view.worldToScreenX((double) tx * bpt));
						int sz0 = (int) Math.floor(view.worldToScreenZ((double) tz * bpt));
						int sx1 = mapX + (int) Math.ceil(view.worldToScreenX((double) (tx + 1) * bpt));
						int sz1 = (int) Math.ceil(view.worldToScreenZ((double) (tz + 1) * bpt));
						g.blit(t.getTextureView(), linear, sx0, sz0, sx1, sz1, 0f, 1f, 0f, 1f);
					}
				}
			}
		}

		boolean showClaims = !state.hideClaims;
		if (claims != null && showClaims) {
			int span = ChunkOwnerGrid.TILE_CELLS * ChunkOwnerGrid.CELL;
			for (ClaimLayer.Tile t : claims.tiles) {
				double wx0 = (double) t.tx() * span;
				double wz0 = (double) t.tz() * span;
				if (wx0 > maxWX || wx0 + span < minWX || wz0 > maxWZ || wz0 + span < minWZ) {
					continue;
				}
				int sx0 = mapX + (int) Math.floor(view.worldToScreenX(wx0));
				int sz0 = (int) Math.floor(view.worldToScreenZ(wz0));
				int sx1 = mapX + (int) Math.ceil(view.worldToScreenX(wx0 + span));
				int sz1 = (int) Math.ceil(view.worldToScreenZ(wz0 + span));
				g.blit(t.texture().getTextureView(), t.texture().getSampler(), sx0, sz0, sx1, sz1, 0f, 1f, 0f, 1f);
			}
		}

		if (map != null && showClaims) {
			// Optional (off by default): every other land gets a thin outline too. There are thousands of them, so this can be
			// slow when zoomed out; parts too small to see are skipped to limit the cost.
			int otherOutline = col(ColorKey.OTHER_OUTLINE);
			if (state.config.outlineOtherLands && otherOutline >>> 24 != 0) {
				var lands = map.grid.lands();
				for (int i = 0; i < lands.size(); i++) {
					if (!openIdx.contains(i)) {
						drawOutline(g, lands.get(i), otherOutline, 1, OTHER_MIN_PX, mapX, minWX, maxWX, minWZ, maxWZ);
					}
				}
			}
			int openOutline = col(ColorKey.OPEN_OUTLINE);
			for (int i : openIdx) {
				drawOutline(g, map.land(i), openOutline, 1, 0, mapX, minWX, maxWX, minWZ, maxWZ);
			}
			Land selected = selectedName == null ? null : map.index.byName(selectedName).orElse(null);
			if (selected != null) {
				drawOutline(g, selected, col(ColorKey.SELECTED_OUTLINE), 2, 0, mapX, minWX, maxWX, minWZ, maxWZ);
			}
			if (inMap(mx, my)) {
				int hi = landAt(mx, my);
				if (hi >= 0) {
					drawOutline(g, map.land(hi), col(ColorKey.HOVER_OUTLINE), 2, 0, mapX, minWX, maxWX, minWZ, maxWZ);
				}
			}
		}

		for (var sp : showClaims ? state.learned.all(dimension) : List.<landmark.data.LearnedSpawns.Spawn>of()) {
			if (isOpen(sp.land()) || sp.land().equalsIgnoreCase(selectedName)) {
				int px = mapX + (int) view.worldToScreenX(sp.x() + 0.5);
				int pz = (int) view.worldToScreenZ(sp.z() + 0.5);
				g.fill(px - 3, pz - 3, px + 4, pz + 4, col(ColorKey.PIN_BORDER));
				g.fill(px - 2, pz - 2, px + 3, pz + 3, col(ColorKey.PIN));
			}
		}
		var p = minecraft.player;
		if (p != null && state.config.showPlayerMarker) {
			int px = mapX + (int) view.worldToScreenX(p.getX());
			int pz = (int) view.worldToScreenZ(p.getZ());
			g.fill(px - 3, pz - 3, px + 4, pz + 4, col(ColorKey.PLAYER_BORDER));
			g.fill(px - 2, pz - 2, px + 3, pz + 3, col(ColorKey.PLAYER));
		}
		g.disableScissor();
	}

	/**
	 * Outlines a land. The size checks are done per part (polygon), not for the land as a whole: a land can have several parts far
	 * apart, and its combined box would otherwise make every edge of every tiny part get drawn. {@code minPx}: parts smaller than
	 * this on screen (both ways) are not outlined at all.
	 */
	private void drawOutline(GuiGraphicsExtractor g, Land land, int color, int thickness, int minPx, int mapX,
		double minWX, double maxWX, double minWZ, double maxWZ) {
		int lo = thickness / 2;
		int hi = thickness - lo;
		for (Polygon poly : land.polygons()) {
			int[] b = poly.bounds();
			if (b[0] > maxWX || b[2] < minWX || b[1] > maxWZ || b[3] < minWZ) {
				continue;
			}
			int bx0 = mapX + (int) Math.round(view.worldToScreenX(b[0]));
			int bz0 = (int) Math.round(view.worldToScreenZ(b[1]));
			int bx1 = mapX + (int) Math.round(view.worldToScreenX(b[2]));
			int bz1 = (int) Math.round(view.worldToScreenZ(b[3]));
			if (bx1 - bx0 < minPx && bz1 - bz0 < minPx) {
				continue;
			}
			// Level of detail: every edge is a draw call, which is far too many when zoomed out.
			if (bx1 - bx0 < SMALL_LAND_PX && bz1 - bz0 < SMALL_LAND_PX) {
				if (bx1 - bx0 < 4 && bz1 - bz0 < 4) {
					int cx = (bx0 + bx1) / 2;
					int cz = (bz0 + bz1) / 2;
					g.fill(cx - 1 - lo, cz - 1 - lo, cx + 2 + hi, cz + 2 + hi, color);
				} else {
					g.fill(bx0 - lo, bz0 - lo, bx1 + hi, bz0 + hi, color);
					g.fill(bx0 - lo, bz1 - lo, bx1 + hi, bz1 + hi, color);
					g.fill(bx0 - lo, bz0 - lo, bx0 + hi, bz1 + hi, color);
					g.fill(bx1 - lo, bz0 - lo, bx1 + hi, bz1 + hi, color);
				}
				continue;
			}
			for (int[] ring : poly.rings()) {
				int n = ring.length / 2;
				for (int i = 0; i < n; i++) {
					int j = (i + 1) % n;
					int ax = mapX + (int) Math.round(view.worldToScreenX(ring[2 * i]));
					int az = (int) Math.round(view.worldToScreenZ(ring[2 * i + 1]));
					int bx = mapX + (int) Math.round(view.worldToScreenX(ring[2 * j]));
					int bz = (int) Math.round(view.worldToScreenZ(ring[2 * j + 1]));
					if (ax == bx && az == bz) {
						continue;
					}
					if (az == bz) {
						g.fill(Math.min(ax, bx) - lo, az - lo, Math.max(ax, bx) + hi, az + hi, color);
					} else if (ax == bx) {
						g.fill(ax - lo, Math.min(az, bz) - lo, ax + hi, Math.max(az, bz) + hi, color);
					}
				}
			}
		}
	}

	private void drawList(GuiGraphicsExtractor g, int mx, int my) {
		int top = listTop();
		int rows = visibleRows();
		if (entries.isEmpty()) {
			String key = state.openSpawns == null ? (state.refreshing ? "landmark.list.loading" : "landmark.list.none") : "landmark.list.empty";
			g.textWithWordWrap(font, Component.translatable(key), 6, top + 4, PANEL_W - 12, col(ColorKey.TEXT_MUTED));
			return;
		}
		listScroll = Math.max(0, Math.min(listScroll, Math.max(0, entries.size() - rows)));
		for (int r = 0; r < rows && listScroll + r < entries.size(); r++) {
			Entry e = entries.get(listScroll + r);
			int y = top + r * ROW_H;
			boolean hover = mx >= 2 && mx < PANEL_W - 2 && my >= y && my < y + ROW_H;
			boolean selected = e.name().equalsIgnoreCase(selectedName);
			if (selected) {
				g.fill(2, y, PANEL_W - 2, y + ROW_H, col(ColorKey.ROW_SELECTED));
			} else if (hover) {
				g.fill(2, y, PANEL_W - 2, y + ROW_H, col(ColorKey.ROW_HOVER));
			}
			int color = e.typed() ? col(ColorKey.TEXT_INFO) : e.open() ? col(ColorKey.LIST_OPEN) : col(ColorKey.LIST_CLOSED);
			String label = e.typed() ? "> " + e.name() : e.name();
			g.text(font, font.plainSubstrByWidth(label, PANEL_W - 14), 6, y + 2, color, false);
		}
		if (entries.size() > rows) {
			g.text(font, (listScroll + 1) + "-" + Math.min(entries.size(), listScroll + rows) + "/" + entries.size(), 6, listBottom() + 2, col(ColorKey.TEXT_DIM), false);
		}
	}

	private void drawStatus(GuiGraphicsExtractor g) {
		int y = height - STATUS_H;
		g.fill(PANEL_W, y, width, height, col(ColorKey.STATUS_BACKGROUND));
		long now = System.currentTimeMillis();
		int room = mapWidth() - 8;
		String claimsText;
		if (map != null) {
			var snap = map.stored.snapshot();
			claimsText = I18n.tr("landmark.status.claims", snap.lands().size(), Ages.format(snap.fetchedAtMillis(), now), snap.source());
		} else {
			claimsText = I18n.tr("landmark.status.noclaims");
		}
		OpenSpawns s = state.openSpawns;
		String spawnText = s == null ? I18n.tr("landmark.status.nospawns")
			: I18n.tr("landmark.status.spawns", s.names().size(), Ages.format(s.fetchedAtMillis(), now));
		g.text(font, font.plainSubstrByWidth(claimsText, room), PANEL_W + 4, y + 3, col(ColorKey.TEXT_STATUS), false);
		g.text(font, font.plainSubstrByWidth(spawnText, room), PANEL_W + 4, y + 13, col(ColorKey.TEXT_STATUS), false);
		String note = state.notice;
		if (note == null && tiles != null && tiles.decodeFailures > 0) {
			note = I18n.tr("landmark.status.tilefail", tiles.decodeFailures);
		}
		if (note != null) {
			g.text(font, font.plainSubstrByWidth(note, room), PANEL_W + 4, y + 23, state.noticeIsError ? col(ColorKey.TEXT_ERROR) : col(ColorKey.TEXT_INFO), false);
		} else {
			g.text(font, font.plainSubstrByWidth(I18n.tr("landmark.status.dropHint"), room), PANEL_W + 4, y + 23, col(ColorKey.TEXT_DIM), false);
		}
	}

	private void drawHoverTooltip(GuiGraphicsExtractor g, int mx, int my) {
		int hi = landAt(mx, my);
		if (hi < 0) {
			return;
		}
		Land l = map.land(hi);
		List<String> lines = new ArrayList<>();
		lines.add(l.name());
		if (l.owner() != null) {
			lines.add(I18n.tr("landmark.tip.owner", l.owner()));
		}
		List<String> others = l.members().stream().filter(m -> !m.equalsIgnoreCase(l.owner())).toList();
		if (!others.isEmpty()) {
			String shown = String.join(", ", others.subList(0, Math.min(6, others.size())));
			lines.add(I18n.tr("landmark.tip.members", others.size() > 6 ? shown + " +" + (others.size() - 6) : shown));
		}
		lines.add(I18n.tr("landmark.tip.chunks", l.chunks()));
		lines.add(openIdx.contains(hi) ? I18n.tr("landmark.tip.open") : I18n.tr("landmark.tip.closed"));
		state.learned.get(l.name(), dimension).ifPresent(sp -> lines.add(I18n.tr("landmark.tip.learned", sp.x(), sp.y(), sp.z())));
		int w = 0;
		for (String s : lines) {
			w = Math.max(w, font.width(s));
		}
		int x = Math.min(mx + 10, width - w - 8);
		int y = Math.min(my + 10, height - lines.size() * 10 - 12);
		g.fill(x - 3, y - 3, x + w + 3, y + lines.size() * 10 + 1, col(ColorKey.TOOLTIP_BACKGROUND));
		for (int i = 0; i < lines.size(); i++) {
			g.text(font, lines.get(i), x, y + i * 10, i == 0 ? col(ColorKey.TEXT_PRIMARY) : lines.get(i).equals(I18n.tr("landmark.tip.open")) ? col(ColorKey.LIST_OPEN) : col(ColorKey.TOOLTIP_TEXT), false);
		}
	}

	// ---- input ----

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		if (super.mouseClicked(e, doubleClick)) {
			return true;
		}
		if (e.button() != 0) {
			return false;
		}
		if (inMap(e.x(), e.y()) && map != null) {
			pressedInMap = true;
			pressX = e.x();
			pressY = e.y();
			dragged = 0;
			return true;
		}
		if (e.x() < PANEL_W && e.y() >= listTop() && e.y() < listTop() + visibleRows() * ROW_H) {
			int idx = listScroll + (int) ((e.y() - listTop()) / ROW_H);
			if (idx < entries.size()) {
				Entry en = entries.get(idx);
				select(en);
				if (doubleClick && (en.open() || en.typed())) {
					teleportToSelection();
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (pressedInMap) {
			dragged += Math.abs(dx) + Math.abs(dy);
			view.panByPixels(dx, dy);
			return true;
		}
		return super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		if (pressedInMap) {
			pressedInMap = false;
			if (dragged < 4 && map != null) {
				int hi = landAt(e.x(), e.y());
				if (hi >= 0) {
					Land l = map.land(hi);
					boolean open = openIdx.contains(hi);
					selectedName = l.name();
					if (open) {
						teleportToSelection();
					}
				}
			}
			return true;
		}
		return super.mouseReleased(e);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (inMap(x, y) && map != null) {
			view.zoomAt(Math.pow(1.25, scrollY), x - PANEL_W, y);
			return true;
		}
		if (x < PANEL_W) {
			listScroll -= (int) Math.signum(scrollY) * 3;
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (e.isConfirmation() && search.isFocused()) {
			if (!entries.isEmpty()) {
				select(entries.get(0));
			}
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public void onFilesDrop(List<Path> paths) {
		state.handleDrop(minecraft, paths);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---- actions ----

	private void toggleClaims() {
		state.hideClaims = !state.hideClaims;
	}

	/** Index of the land under the screen position, or -1; nothing is under the cursor while claims are hidden. */
	private int landAt(double sx, double sy) {
		if (map == null || state.hideClaims) {
			return -1;
		}
		return map.grid.landIndexAt((int) Math.floor(view.screenToWorldX(sx - PANEL_W)), (int) Math.floor(view.screenToWorldZ(sy)));
	}

	private void select(Entry en) {
		selectedName = en.name();
		if (en.land() == null) {
			state.learned.get(en.name(), dimension).ifPresent(sp -> {
				view.centerOn(sp.x(), sp.z());
				view.setScale(Math.max(view.scale(), 1.0 / 2));
			});
		}
		if (en.land() != null && map != null) {
			int[] b = en.land().bounds();
			if (b != null) {
				view.fit(b[0], b[1], b[2], b[3], 0.55);
				view.setScale(Math.min(view.scale(), 0.5));
			}
		}
	}

	private void fitToSpawns() {
		if (map == null) {
			return;
		}
		double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
		boolean any = false;
		List<Land> lands = openIdx.isEmpty() ? map.grid.lands() : openIdx.stream().map(map::land).toList();
		for (Land l : lands) {
			int[] b = l.bounds();
			if (b != null) {
				any = true;
				minX = Math.min(minX, b[0]);
				minZ = Math.min(minZ, b[1]);
				maxX = Math.max(maxX, b[2]);
				maxZ = Math.max(maxZ, b[3]);
			}
		}
		if (any) {
			view.setViewport(mapWidth(), mapHeight());
			view.fit(minX, minZ, maxX, maxZ, 0.05);
		}
	}

	/** The name the Teleport button would use: the selected open land, or the typed name; null if neither applies. */
	private @Nullable String teleportTarget() {
		if (selectedName != null) {
			Entry e = entries.stream().filter(x -> x.name().equalsIgnoreCase(selectedName)).findFirst().orElse(null);
			if (e != null && (e.open() || e.typed())) {
				return e.name();
			}
			if (e == null && isOpen(selectedName)) {
				return selectedName;
			}
			return null;
		}
		String q = search == null ? "" : search.getValue().strip();
		return q.isEmpty() ? null : q;
	}

	private void teleportToSelection() {
		String target = teleportTarget();
		if (target == null) {
			return;
		}
		if (!state.config.confirmTeleport) {
			go(target);
			return;
		}
		minecraft.setScreenAndShow(new ConfirmScreen(yes -> {
			if (yes) {
				go(target);
			} else {
				minecraft.setScreenAndShow(this);
			}
		}, Component.translatable("landmark.confirm.title"), Component.translatable("landmark.confirm.message", target)));
	}

	private void go(String name) {
		state.beginWatching(name);
		minecraft.setScreenAndShow(null);
		var connection = minecraft.getConnection();
		if (connection != null) {
			connection.sendCommand("lands spawn " + name);
		}
	}
}
