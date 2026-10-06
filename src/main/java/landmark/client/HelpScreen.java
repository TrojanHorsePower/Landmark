package landmark.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import landmark.LandmarkClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Step-by-step instructions for exporting the map data, with buttons to open the website and copy the script. */
public class HelpScreen extends Screen {
	static final String SCRIPT_RESOURCE = "/assets/landmark/export.js";

	private final Screen parent;
	private Button copyButton;

	public HelpScreen(Screen parent) {
		super(Component.translatable("landmark.help.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int w = Math.min(width - 20, 360);
		int x = (width - w) / 2;
		int y = height - 28;
		int third = (w - 8) / 3;
		addRenderableWidget(Button.builder(Component.translatable("landmark.help.open"),
			ConfirmLinkScreen.confirmLink(this, LandmarkState.get().config.mapUrl)).bounds(x, y, third, 20).build());
		copyButton = addRenderableWidget(Button.builder(Component.translatable("landmark.help.copy"), b -> copyScript())
			.bounds(x + third + 4, y, third, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("landmark.help.back"), b -> onClose())
			.bounds(x + 2 * (third + 4), y, third, 20).build());
	}

	private void copyScript() {
		String script = loadScript();
		if (script == null) {
			copyButton.setMessage(Component.translatable("landmark.help.copyfail"));
			return;
		}
		minecraft.keyboardHandler.setClipboard(script);
		copyButton.setMessage(Component.translatable("landmark.help.copied"));
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
	public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
		g.fill(0, 0, width, height, 0xFF101418);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
		extractBackground(g, mx, my, partial);
		int w = Math.min(width - 20, 360);
		int x = (width - w) / 2;
		int y = 12;
		g.text(font, title, x, y, 0xFFFFD34D, false);
		y += 16;
		y = paragraph(g, "landmark.help.intro", x, y, w, 0xFF9AA5B0) + 6;
		for (String key : new String[] {"landmark.help.step1", "landmark.help.step2", "landmark.help.step3", "landmark.help.step4"}) {
			y = paragraph(g, key, x, y, w, 0xFFE6EAEE) + 5;
		}
		super.extractRenderState(g, mx, my, partial);
	}

	private int paragraph(GuiGraphicsExtractor g, String key, int x, int y, int w, int color) {
		for (var line : font.split(Component.translatable(key), w)) {
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
