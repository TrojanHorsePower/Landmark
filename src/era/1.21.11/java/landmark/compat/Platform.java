package landmark.compat;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.File;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/** The few Minecraft calls whose names or signatures differ between versions; this is the Minecraft 1.21.11 implementation. */
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

	/** Registers a key binding in the mod's own category (it then shows up in Options > Controls). */
	public static KeyMapping registerKey(String name, int keyCode, String categoryPath) {
		return net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(new KeyMapping(name, keyCode,
			KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(landmark.Branding.MOD_ID, categoryPath))));
	}

	/** A cycling button builder with the given starting value. */
	public static <T> net.minecraft.client.gui.components.CycleButton.Builder<T> cycleBuilder(
		java.util.function.Function<T, net.minecraft.network.chat.Component> label, T initial) {
		return net.minecraft.client.gui.components.CycleButton.builder(label, initial);
	}

	/** Dev harness: saves a screenshot to the game's screenshots folder. */
	public static void screenshot(Minecraft mc) {
		Screenshot.grab(mc.gameDirectory, null, mc.getMainRenderTarget(), 1, c -> {});
	}

	/** Dev harness: settings for a fresh creative test world. */
	public static net.minecraft.world.level.LevelSettings devLevelSettings(String name) {
		return new net.minecraft.world.level.LevelSettings(name, net.minecraft.world.level.GameType.CREATIVE, false,
			net.minecraft.world.Difficulty.NORMAL, true,
			new net.minecraft.world.level.gamerules.GameRules(net.minecraft.world.level.WorldDataConfiguration.DEFAULT.enabledFeatures()),
			net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
	}

	/** The identifier of a level's dimension, for example {@code minecraft:overworld}. */
	public static String dimensionId(net.minecraft.world.level.Level level) {
		return level.dimension().identifier().toString();
	}

	/** Dev harness: the dimension key for an identifier string. */
	public static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimensionKey(String id) {
		return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse(id));
	}
}
