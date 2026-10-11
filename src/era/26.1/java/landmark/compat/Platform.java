package landmark.compat;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.File;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/** The few Minecraft calls whose names or signatures differ between versions; this is the Minecraft 26.1 implementation. */
public final class Platform {
	private Platform() {}

	/** The screen that is open, or null. */
	public static @Nullable Screen screen(Minecraft mc) {
		return mc.screen;
	}

	/** Whether this key binding is currently bound to the given key. */
	public static boolean matches(KeyMapping mapping, InputConstants.Key key) {
		return mapping.saveString().equals(key.getName());
	}

	/** Registers a key binding (it then shows up in Options > Controls). */
	public static KeyMapping registerKey(KeyMapping mapping) {
		return net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(mapping);
	}

	/** Dev harness: saves a screenshot to the game's screenshots folder. */
	public static void screenshot(Minecraft mc) {
		Screenshot.grab(mc.gameDirectory, null, mc.getMainRenderTarget(), 1, c -> {});
	}

	/** Dev harness: settings for a fresh creative test world. */
	public static net.minecraft.world.level.LevelSettings devLevelSettings(String name) {
		return new net.minecraft.world.level.LevelSettings(name, net.minecraft.world.level.GameType.CREATIVE,
			net.minecraft.world.level.LevelSettings.DifficultySettings.DEFAULT, true, net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
	}
}
