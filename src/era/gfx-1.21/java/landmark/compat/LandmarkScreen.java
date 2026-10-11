package landmark.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class of Landmark's screens. The game calls different method names to draw a screen in different versions; this
 * maps them to {@link #drawBackground} and {@link #draw}. This is the implementation for 1.21.6 to 1.21.11.
 */
public abstract class LandmarkScreen extends Screen {
	protected LandmarkScreen(Component title) {
		super(title);
	}

	/** Draws the background; the default is the game's own. */
	protected void drawBackground(Gfx g, int mx, int my, float partial) {
		super.renderBackground(g.raw(), mx, my, partial);
	}

	/** Draws the screen; call {@link #drawWidgets} to draw its buttons and other widgets. */
	protected abstract void draw(Gfx g, int mx, int my, float partial);

	/** Draws the widgets that were added with addRenderableWidget. */
	protected final void drawWidgets(Gfx g, int mx, int my, float partial) {
		super.render(g.raw(), mx, my, partial);
	}

	@Override
	public final void renderBackground(GuiGraphics g, int mx, int my, float partial) {
		drawBackground(new Gfx(g), mx, my, partial);
	}

	@Override
	public final void render(GuiGraphics g, int mx, int my, float partial) {
		draw(new Gfx(g), mx, my, partial);
	}
}
