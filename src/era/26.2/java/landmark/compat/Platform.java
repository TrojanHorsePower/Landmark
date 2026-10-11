package landmark.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/** The few Minecraft calls whose names or signatures differ between versions; this is the Minecraft 26.2 implementation. */
public final class Platform {
	private Platform() {}

	/** The screen that is open, or null. */
	public static @Nullable Screen screen(Minecraft mc) {
		return mc.gui.screen();
	}

	/** Whether this key binding is currently bound to the given key. */
	public static boolean matches(KeyMapping mapping, InputConstants.Key key) {
		return mapping.matches(key);
	}

	/** Dev harness: saves a screenshot to the game's screenshots folder. */
	public static void screenshot(Minecraft mc) {
		Screenshot.grab(mc, false);
	}
}
