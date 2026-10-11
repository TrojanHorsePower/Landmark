package landmark.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/** A row of a {@link ContainerObjectSelectionList}; maps the game's per-version draw method to {@link #draw}. */
public abstract class LandmarkListEntry<E extends ContainerObjectSelectionList.Entry<E>> extends ContainerObjectSelectionList.Entry<E> {
	protected abstract void draw(Gfx g, int mx, int my, boolean hovered, float partial);

	@Override
	public final void renderContent(GuiGraphics g, int mx, int my, boolean hovered, float partial) {
		draw(new Gfx(g), mx, my, hovered, partial);
	}
}
