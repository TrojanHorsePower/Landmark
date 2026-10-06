package landmark;

import landmark.client.LiveSpawns;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LandmarkClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Branding.MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("{} loaded", Branding.DISPLAY_NAME);
		// Temporary diagnostics: /landmark spawns requests the live open-spawn list and prints what came back.
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommands.literal(Branding.MOD_ID).then(ClientCommands.literal("spawns").executes(ctx -> {
				Minecraft mc = Minecraft.getInstance();
				long start = System.currentTimeMillis();
				LiveSpawns.request(mc).whenCompleteAsync((spawns, error) -> {
					Component msg = error != null
						? Component.literal("Live spawn request failed: " + error)
						: Component.literal(LiveSpawns.describe(spawns, System.currentTimeMillis() - start));
					if (mc.player != null) {
						mc.player.sendSystemMessage(msg);
					}
				}, mc);
				return 1;
			}))));
	}
}
