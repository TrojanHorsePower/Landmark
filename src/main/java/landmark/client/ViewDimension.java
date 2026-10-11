package landmark.client;

import java.util.List;
import landmark.data.LandmarkConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Which dimension the map (and the waypoint set list) shows: Auto follows the player, otherwise one of the three vanilla
 * dimensions pinned in the config. Shared by the map screen and the waypoint sets screen so both always agree.
 */
final class ViewDimension {
	private ViewDimension() {}

	static final List<String> CYCLE = List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");

	/** The dimension the player is in ("" with no world). */
	static String player(Minecraft mc) {
		var level = mc.level;
		return level == null ? DevHarness.devDimension : landmark.compat.Platform.dimensionId(level);
	}

	/** The pinned dimension, or null to follow the player. */
	static @Nullable String chosen(LandmarkConfig config) {
		String d = config.viewDimension;
		return d != null && CYCLE.contains(d) ? d : null;
	}

	/** The dimension being viewed right now. */
	static String resolve(LandmarkConfig config, String playerDimension) {
		String chosen = chosen(config);
		return chosen != null ? chosen : playerDimension;
	}

	/** Auto → Overworld → Nether → The End → Auto; stores the next value in the config (the caller saves it). */
	static void cycle(LandmarkConfig config) {
		String chosen = chosen(config);
		int i = chosen == null ? -1 : CYCLE.indexOf(chosen);
		config.viewDimension = i + 1 < CYCLE.size() ? CYCLE.get(i + 1) : "";
	}

	static Component name(String dim) {
		String key = switch (dim) {
			case "minecraft:overworld" -> "landmark.dimension.overworld";
			case "minecraft:the_nether" -> "landmark.dimension.nether";
			case "minecraft:the_end" -> "landmark.dimension.end";
			default -> null;
		};
		return key != null ? Component.translatable(key) : Component.literal(dim.substring(dim.indexOf(':') + 1));
	}

	/** "Map: Auto (The Nether)" or "Map: The End". */
	static Component label(LandmarkConfig config, String viewed) {
		String chosen = chosen(config);
		return chosen == null
			? Component.translatable("landmark.dimension.auto", name(viewed))
			: Component.translatable("landmark.dimension", name(chosen));
	}
}
