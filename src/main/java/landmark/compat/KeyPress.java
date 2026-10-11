package landmark.compat;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/** A key press, with GLFW key codes; the same shape in every Minecraft version. */
public record KeyPress(int key, int scancode, int modifiers) {
	public boolean isEscape() {
		return key == GLFW.GLFW_KEY_ESCAPE;
	}

	public boolean isUp() {
		return key == GLFW.GLFW_KEY_UP;
	}

	public boolean isDown() {
		return key == GLFW.GLFW_KEY_DOWN;
	}

	/** Enter on the main keyboard or the keypad. */
	public boolean isConfirmation() {
		return key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
	}

	/** The key this press stands for, as a key binding stores it. */
	public InputConstants.Key toKey() {
		return key == GLFW.GLFW_KEY_UNKNOWN ? InputConstants.Type.SCANCODE.getOrCreate(scancode) : InputConstants.Type.KEYSYM.getOrCreate(key);
	}
}
