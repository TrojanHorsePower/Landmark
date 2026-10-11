package landmark.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/**
 * Base class of Landmark's screens. The game calls different method names to draw a screen and to deliver input in
 * different versions; this maps them to {@link #drawBackground}, {@link #draw} and the {@code on...} methods.
 * This is the implementation for Minecraft 26.1 and later.
 */
public abstract class LandmarkScreen extends Screen {
	protected LandmarkScreen(Component title) {
		super(title);
	}

	/** Draws the background; the default is the game's own. */
	protected void drawBackground(Gfx g, int mx, int my, float partial) {
		super.extractBackground(g.raw(), mx, my, partial);
	}

	/** Draws the screen; call {@link #drawWidgets} to draw its buttons and other widgets. */
	protected abstract void draw(Gfx g, int mx, int my, float partial);

	/** Draws the widgets that were added with addRenderableWidget. */
	protected final void drawWidgets(Gfx g, int mx, int my, float partial) {
		super.extractRenderState(g.raw(), mx, my, partial);
	}

	@Override
	public final void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
		drawBackground(new Gfx(g), mx, my, partial);
	}

	@Override
	public final void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
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
		return super.mouseClicked(event(e), doubleClick);
	}

	protected final boolean superMouseDragged(Mouse e, double dx, double dy) {
		return super.mouseDragged(event(e), dx, dy);
	}

	protected final boolean superMouseReleased(Mouse e) {
		return super.mouseReleased(event(e));
	}

	protected final boolean superKeyPressed(KeyPress e) {
		return super.keyPressed(new KeyEvent(e.key(), e.scancode(), e.modifiers()));
	}

	private static MouseButtonEvent event(Mouse e) {
		return new MouseButtonEvent(e.x(), e.y(), new MouseButtonInfo(e.button(), 0));
	}

	@Override
	public final boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		return onMouseClicked(new Mouse(e.x(), e.y(), e.button()), doubleClick);
	}

	@Override
	public final boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		return onMouseDragged(new Mouse(e.x(), e.y(), e.button()), dx, dy);
	}

	@Override
	public final boolean mouseReleased(MouseButtonEvent e) {
		return onMouseReleased(new Mouse(e.x(), e.y(), e.button()));
	}

	@Override
	public final boolean keyPressed(KeyEvent e) {
		return onKeyPressed(new KeyPress(e.key(), e.scancode(), e.modifiers()));
	}
}
