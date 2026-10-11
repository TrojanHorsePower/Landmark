package landmark.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/**
 * What the screens draw with. A thin wrapper over the game's graphics object, because its method names and its texture
 * drawing differ between Minecraft versions; this is the implementation for 1.21.6 to 1.21.11.
 */
public final class Gfx {
	private final GuiGraphics g;

	Gfx(GuiGraphics g) {
		this.g = g;
	}

	GuiGraphics raw() {
		return g;
	}

	public void fill(int x0, int y0, int x1, int y1, int argb) {
		g.fill(x0, y0, x1, y1, argb);
	}

	public void fillGradient(int x0, int y0, int x1, int y1, int topArgb, int bottomArgb) {
		g.fillGradient(x0, y0, x1, y1, topArgb, bottomArgb);
	}

	public void outline(int x, int y, int w, int h, int argb) {
		g.fill(x, y, x + w, y + 1, argb);
		g.fill(x, y + h - 1, x + w, y + h, argb);
		g.fill(x, y + 1, x + 1, y + h - 1, argb);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
	}

	public void text(Font font, String text, int x, int y, int argb, boolean shadow) {
		g.drawString(font, text, x, y, argb, shadow);
	}

	public void text(Font font, Component text, int x, int y, int argb, boolean shadow) {
		g.drawString(font, text, x, y, argb, shadow);
	}

	public void text(Font font, FormattedCharSequence text, int x, int y, int argb, boolean shadow) {
		g.drawString(font, text, x, y, argb, shadow);
	}

	public void centeredText(Font font, Component text, int x, int y, int argb) {
		g.drawCenteredString(font, text, x, y, argb);
	}

	public void textWithWordWrap(Font font, FormattedText text, int x, int y, int width, int argb) {
		g.drawWordWrap(font, text, x, y, width, argb);
	}

	public void enableScissor(int x0, int y0, int x1, int y1) {
		g.enableScissor(x0, y0, x1, y1);
	}

	public void disableScissor() {
		g.disableScissor();
	}

	/** Draws the whole texture stretched over the given rectangle. */
	public void blit(LandmarkTexture t, int x0, int y0, int x1, int y1) {
		g.blit(t.id(), x0, y0, x1, y1, 0f, 1f, 0f, 1f);
	}

	/** Draws a widget (button, edit box ...) that is not part of the screen's own widget list. */
	public void widget(AbstractWidget w, int mx, int my, float partial) {
		w.render(g, mx, my, partial);
	}
}
