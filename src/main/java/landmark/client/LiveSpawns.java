package landmark.client;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import landmark.data.OpenSpawns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Asks the server which lands have a public spawn, the same way the chat box does when the player types
 * {@code /lands spawn } (see CommandSuggestions.updateCommandInfo in the client): parse the text against the
 * command tree the server sent, then ask for completions, which the server answers for "ask server" arguments.
 */
public final class LiveSpawns {
	static final String QUERY = "/lands spawn ";
	static final long TIMEOUT_SECONDS = 5;

	private LiveSpawns() {}

	/** Must be called on the client thread. Fails with {@link IllegalStateException} if not connected to a server. */
	public static CompletableFuture<OpenSpawns> request(Minecraft mc) {
		ClientPacketListener connection = mc.getConnection();
		if (connection == null) {
			return CompletableFuture.failedFuture(new IllegalStateException("Not connected to a server"));
		}
		StringReader reader = new StringReader(QUERY);
		if (reader.canRead() && reader.peek() == '/') {
			reader.skip();
		}
		var parse = connection.getCommands().parse(reader, connection.getSuggestionsProvider());
		return connection.getCommands().getCompletionSuggestions(parse, QUERY.length())
			.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.thenApply(s -> OpenSpawns.fromSuggestionTexts(
				s.getList().stream().map(Suggestion::getText).toList(), System.currentTimeMillis()));
	}
}
