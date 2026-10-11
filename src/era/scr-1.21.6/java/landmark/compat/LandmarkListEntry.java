package landmark.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/**
 * A row of a {@link ContainerObjectSelectionList}. Maps the game's per-version draw method to {@link #draw} and provides
 * the getContentX() family that newer versions have on list entries (Minecraft 1.21.6 to 1.21.8).
 */
public abstract class LandmarkListEntry<E extends ContainerObjectSelectionList.Entry<E>> extends ContainerObjectSelectionList.Entry<E> {
	private int contentX;
	private int contentY;
	private int contentWidth;
	private int contentHeight;

	protected abstract void draw(Gfx g, int mx, int my, boolean hovered, float partial);

	@Override
	public final void render(GuiGraphics g, int index, int top, int left, int width, int height, int mx, int my, boolean hovered, float partial) {
		contentX = left;
		contentY = top;
		contentWidth = width - 8; // matches where the newer versions put the right edge of a row
		contentHeight = height;
		draw(new Gfx(g), mx, my, hovered, partial);
	}

	public int getContentX() {
		return contentX;
	}

	public int getContentY() {
		return contentY;
	}

	public int getContentWidth() {
		return contentWidth;
	}

	public int getContentHeight() {
		return contentHeight;
	}

	public int getContentRight() {
		return contentX + contentWidth;
	}

	public int getContentBottom() {
		return contentY + contentHeight;
	}

	public int getContentYMiddle() {
		return contentY + contentHeight / 2;
	}

	public int getContentXMiddle() {
		return contentX + contentWidth / 2;
	}
}
