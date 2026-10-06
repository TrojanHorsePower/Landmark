package landmark.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The lands whose spawn is currently public, as listed by the server's own {@code /lands spawn} suggestions.
 * This is the source of truth for what can be teleported to; claim data only adds geometry and details.
 */
public record OpenSpawns(List<String> names, long fetchedAtMillis) {
	public OpenSpawns {
		names = List.copyOf(names);
	}

	/** Cleans raw suggestion texts: trimmed, blanks dropped, case-insensitive duplicates removed, sorted. */
	public static OpenSpawns fromSuggestionTexts(List<String> texts, long now) {
		Map<String, String> unique = new LinkedHashMap<>();
		for (String t : texts) {
			String s = t.strip();
			if (!s.isEmpty()) {
				unique.putIfAbsent(s.toLowerCase(Locale.ROOT), s);
			}
		}
		List<String> names = new ArrayList<>(unique.values());
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return new OpenSpawns(names, now);
	}

	/** Open-spawn lands we have claim data for, and names the server listed that the claim data does not know. */
	public record Joined(List<Land> known, List<String> unknownNames) {}

	public Joined join(ClaimIndex index) {
		List<Land> known = new ArrayList<>();
		List<String> unknown = new ArrayList<>();
		for (String n : names) {
			index.byName(n).ifPresentOrElse(known::add, () -> unknown.add(n));
		}
		return new Joined(known, unknown);
	}
}
