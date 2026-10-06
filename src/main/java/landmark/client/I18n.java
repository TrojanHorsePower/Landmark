package landmark.client;

import net.minecraft.network.chat.Component;

/** Small helper: translated text with arguments as a plain string. */
final class I18n {
	private I18n() {}

	static String tr(String key, Object... args) {
		return Component.translatable(key, args).getString();
	}
}
