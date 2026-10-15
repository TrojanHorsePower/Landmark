package landmark.compat;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * A mouse press, drag or release, in GUI coordinates; the same shape in every Minecraft version. {@code button} is in
 * the numbering of the running version (26.3 counts from 1, earlier versions from 0), so use {@link #isLeft()} rather
 * than comparing it with a number.
 */
public record Mouse(double x, double y, int button) {
	public boolean isLeft() {
		return button == InputConstants.MOUSE_BUTTON_LEFT;
	}
}
