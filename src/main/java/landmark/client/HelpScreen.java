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
		int w = Math.min(width - 20, 360);
		int x = (width - w) / 2;
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

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
		extractBackground(g, mx, my, partial);
		int w = Math.min(width - 20, 360);
		int x = (width - w) / 2;
		int y = 12;
		g.text(font, title, x, y, col(ColorKey.LIST_OPEN), false);
		y += 16;
		y = paragraph(g, Component.translatable("landmark.help.intro"), x, y, w, col(ColorKey.TEXT_STATUS)) + 6;
		for (String key : new String[] {"landmark.help.step1", "landmark.help.step2", "landmark.help.step3", "landmark.help.step4"}) {
			y = paragraph(g, Component.translatable(key), x, y, w, col(ColorKey.TEXT_BODY)) + 5;
		}
		ExportPreset preset = state.config.exportPreset();
		y = paragraph(g, Component.translatable("landmark.help.quality", Component.translatable(preset.translationKey())), x, y + 2, w, col(ColorKey.TEXT_STATUS));
		if (preset.isHeavy()) {
			paragraph(g, Component.translatable("landmark.export.warning"), x, y, w, col(ColorKey.LIST_OPEN));
		}
		// The import result is shown here, because the map is not on screen while this page is open.
		String note = state.notice;
		Component status = note != null ? Component.literal(note) : Component.translatable("landmark.help.drop");
		int color = note == null ? col(ColorKey.TEXT_DIM) : state.noticeIsError ? col(ColorKey.TEXT_ERROR) : col(ColorKey.TEXT_INFO);
		var lines = font.split(status, w);
		int statusY = height - 52 - 6 - Math.min(2, lines.size()) * (font.lineHeight + 1);
		for (int i = 0; i < Math.min(2, lines.size()); i++) {
			g.text(font, lines.get(i), x, statusY + i * (font.lineHeight + 1), color, false);
		}
		super.extractRenderState(g, mx, my, partial);
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
