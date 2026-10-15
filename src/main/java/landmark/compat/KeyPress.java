package landmark.compat;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * A key press. {@code key} is in the key space of the running Minecraft version, so compare it with the
 * {@code InputConstants.KEY_*} constants, never with GLFW or SDL ones (Minecraft 26.3 moved from GLFW to SDL).
 */
public record KeyPress(int key, int scancode, int modifiers) {
	public boolean isEscape() {
		return key == InputConstants.KEY_ESCAPE;
	}

	public boolean isUp() {
		return key == InputConstants.KEY_UP;
	}

	public boolean isDown() {
		return key == InputConstants.KEY_DOWN;
	}

	/** Enter on the main keyboard or the keypad. */
	public boolean isConfirmation() {
		return key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER;
	}

	/** The key this press stands for, as a key binding stores it. */
	public InputConstants.Key toKey() {
		return Platform.keyOf(key, scancode);
	}
}
