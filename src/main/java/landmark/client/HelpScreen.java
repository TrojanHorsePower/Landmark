package landmark.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import landmark.LandmarkClient;
import landmark.data.ColorKey;
import landmark.data.ExportPreset;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Step-by-step instructions for exporting the map data, with buttons to open the website and copy the script. A dropped zip is
 * imported right here: the result is shown on this page, so there is no need to go back to the map first.
 */
public class HelpScreen extends Screen {
	static final String SCRIPT_RESOURCE = "/assets/landmark/export.js";

	private final Screen parent;
	private final LandmarkState state = LandmarkState.get();
	private Button copyButton;

	public HelpScreen(Screen parent) {
		super(Component.translatable("landmark.help.title"));
		this.parent = parent;
	}

	private static int col(ColorKey key) {
		return Palette.get(key);
	}

	@Override
	protected void init() {
		int w = contentWidth();
		int x = contentLeft();
		int half = (w - 4) / 2;
		int row1 = height - 52;
		int row2 = height - 28;
		addRenderableWidget(Button.builder(Component.translatable("landmark.help.open"),
			ConfirmLinkScreen.confirmLink(this, state.config.mapUrl)).bounds(x, row1, half, 20).build());
		copyButton = addRenderableWidget(Button.builder(Component.translatable("landmark.help.copy"), b -> copyScript())
			.bounds(x + half + 4, row1, half, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("landmark.settings"),
			b -> minecraft.setScreenAndShow(new ConfigScreen(this))).bounds(x, row2, half, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("landmark.help.back"), b -> onClose())
			.bounds(x + half + 4, row2, half, 20).build());
	}

	/** Dev harness: presses Copy export script and returns what ended up on the clipboard. */
	String devCopy() {
		copyScript();
		return minecraft.keyboardHandler.getClipboard();
	}

	private void copyScript() {
		String script = loadScript();
		if (script == null) {
			copyButton.setMessage(Component.translatable("landmark.help.copyfail"));
			return;
		}
		minecraft.keyboardHandler.setClipboard(withPreset(script, state.config.exportPreset()));
		copyButton.setMessage(Component.translatable("landmark.help.copied"));
	}

	/** The script with the chosen export quality written in; the unmodified script if it can no longer be adjusted. */
	static String withPreset(String script, ExportPreset preset) {
		try {
			return preset.apply(script);
		} catch (IllegalStateException e) {
			LandmarkClient.LOGGER.warn("Could not apply the export quality preset: {}", e.getMessage());
			return script;
		}
	}

	static @Nullable String loadScript() {
		try (InputStream in = HelpScreen.class.getResourceAsStream(SCRIPT_RESOURCE)) {
			return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LandmarkClient.LOGGER.warn("Could not read the bundled export script", e);
			return null;
		}
	}

	@Override
	public void onFilesDrop(List<Path> paths) {
		state.handleDrop(minecraft, paths);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
		g.fill(0, 0, width, height, col(ColorKey.MAP_BACKGROUND));
	}

	/** Dev harness: scrolls the text. */
	void devScroll(double to) {
		scroll = to;
	}

	// ---- layout: the text scrolls when it does not fit above the status line and buttons ----

	private double scroll;
	private int contentHeight;
	private int viewTop;
	private int viewBottom;
	private boolean draggingBar;

	private int contentWidth() {
		return Math.min(width - 24, 360);
	}

	private int contentLeft() {
		return (width - contentWidth()) / 2;
	}

	/** How many lines of status text there are (0 to 2); needed to know where the scrolling area ends. */
	private List<net.minecraft.util.FormattedCharSequence> statusLines() {
		String note = state.notice;
		Component status = note != null ? Component.literal(note) : Component.translatable("landmark.help.drop");
		var lines = font.split(status, contentWidth());
		return lines.size() > 2 ? lines.subList(0, 2) : lines;
	}

	private int maxScroll() {
		return Math.max(0, contentHeight - (viewBottom - viewTop));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
		extractBackground(g, mx, my, partial);
		int w = contentWidth();
		int x = contentLeft();
		g.text(font, title, x, 8, col(ColorKey.LIST_OPEN), false);

		var status = statusLines();
		int lineH = font.lineHeight + 1;
		int statusY = height - 52 - 4 - status.size() * lineH;
		viewTop = 8 + font.lineHeight + 6;
		viewBottom = Math.max(viewTop + 20, statusY - 4);

		// Lay the text out once to learn its height, then draw it shifted by the scroll offset and clipped to the view.
		scroll = Math.max(0, Math.min(scroll, maxScroll()));
		int y = viewTop - (int) scroll;
		int startY = y;
		g.enableScissor(0, viewTop, width, viewBottom);
		y = paragraph(g, Component.translatable("landmark.help.intro"), x, y, w, col(ColorKey.TEXT_STATUS)) + 6;
		for (String key : new String[] {"landmark.help.step1", "landmark.help.step2", "landmark.help.step3", "landmark.help.step4"}) {
			y = paragraph(g, Component.translatable(key), x, y, w, col(ColorKey.TEXT_BODY)) + 5;
		}
		ExportPreset preset = state.config.exportPreset();
		y = paragraph(g, Component.translatable("landmark.help.quality", Component.translatable(preset.translationKey())), x, y + 2, w, col(ColorKey.TEXT_STATUS));
		if (preset.isHeavy()) {
			y = paragraph(g, Component.translatable("landmark.export.warning"), x, y, w, col(ColorKey.LIST_OPEN));
		}
		g.disableScissor();
		contentHeight = y - startY;

		// scrollbar, only when there is something to scroll to
		if (maxScroll() > 0) {
			int barX = x + w + 4;
			int track = viewBottom - viewTop;
			int barH = Math.max(12, track * track / contentHeight);
			int barY = viewTop + (int) ((track - barH) * (scroll / maxScroll()));
			g.fill(barX, viewTop, barX + 3, viewBottom, 0x40FFFFFF);
			g.fill(barX, barY, barX + 3, barY + barH, col(ColorKey.TEXT_STATUS));
		}

		// The import result is shown here, because the map is not on screen while this page is open.
		boolean isNote = state.notice != null;
		int color = !isNote ? col(ColorKey.TEXT_DIM) : state.noticeIsError ? col(ColorKey.TEXT_ERROR) : col(ColorKey.TEXT_INFO);
		for (int i = 0; i < status.size(); i++) {
			g.text(font, status.get(i), x, statusY + i * lineH, color, false);
		}
		super.extractRenderState(g, mx, my, partial);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (maxScroll() > 0) {
			scroll = Math.max(0, Math.min(maxScroll(), scroll - scrollY * 14));
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent e) {
		if (maxScroll() > 0) {
			int step = e.isUp() ? -14 : e.isDown() ? 14 : e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP ? -(viewBottom - viewTop)
				: e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN ? viewBottom - viewTop : 0;
			if (step != 0) {
				scroll = Math.max(0, Math.min(maxScroll(), scroll + step));
				return true;
			}
		}
		return super.keyPressed(e);
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e, boolean doubleClick) {
		if (maxScroll() > 0 && e.button() == 0 && e.x() >= contentLeft() + contentWidth() && e.x() <= contentLeft() + contentWidth() + 12
			&& e.y() >= viewTop && e.y() <= viewBottom) {
			draggingBar = true;
			dragBar(e.y());
			return true;
		}
		return super.mouseClicked(e, doubleClick);
	}

	@Override
	public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent e, double dx, double dy) {
		if (draggingBar) {
			dragBar(e.y());
			return true;
		}
		return super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent e) {
		draggingBar = false;
		return super.mouseReleased(e);
	}

	private void dragBar(double my) {
		double f = (my - viewTop) / Math.max(1, viewBottom - viewTop);
		scroll = Math.max(0, Math.min(maxScroll(), f * maxScroll()));
	}

	private int paragraph(GuiGraphicsExtractor g, Component text, int x, int y, int w, int color) {
		for (var line : font.split(text, w)) {
			g.text(font, line, x, y, color, false);
			y += font.lineHeight + 1;
		}
		return y;
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
