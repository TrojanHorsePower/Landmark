package landmark.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class of Landmark's screens. The game calls different method names to draw a screen and to deliver input in
 * different versions; this maps them to {@link #drawBackground}, {@link #draw} and the {@code on...} methods.
 * This is the implementation for Minecraft 1.21.6 to 1.21.8.
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

	// ---- input ----

	protected boolean onMouseClicked(Mouse e, boolean doubleClick) {
		return superMouseClicked(e, doubleClick);
	}

	protected boolean onMouseDragged(Mouse e, double dx, double dy) {
		return superMouseDragged(e, dx, dy);
	}

	protected boolean onMouseReleased(Mouse e) {
		return superMouseReleased(e);
	}

	protected boolean onKeyPressed(KeyPress e) {
		return superKeyPressed(e);
	}

	protected final boolean superMouseClicked(Mouse e, boolean doubleClick) {
		return super.mouseClicked(e.x(), e.y(), e.button());
	}

	protected final boolean superMouseDragged(Mouse e, double dx, double dy) {
		return super.mouseDragged(e.x(), e.y(), e.button(), dx, dy);
	}

	protected final boolean superMouseReleased(Mouse e) {
		return super.mouseReleased(e.x(), e.y(), e.button());
	}

	protected final boolean superKeyPressed(KeyPress e) {
		return super.keyPressed(e.key(), e.scancode(), e.modifiers());
	}

	@Override
	public final boolean mouseClicked(double x, double y, int button) {
		return onMouseClicked(new Mouse(x, y, button), false);
	}

	@Override
	public final boolean mouseDragged(double x, double y, int button, double dx, double dy) {
		return onMouseDragged(new Mouse(x, y, button), dx, dy);
	}

	@Override
	public final boolean mouseReleased(double x, double y, int button) {
		return onMouseReleased(new Mouse(x, y, button));
	}

	@Override
	public final boolean keyPressed(int key, int scancode, int modifiers) {
		return onKeyPressed(new KeyPress(key, scancode, modifiers));
	}
}
