package landmark.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Lookup and search over a set of lands. Names are unique on the server, so lookups are by lower-cased name. */
public final class ClaimIndex {
	private final List<Land> lands;
	private final Map<String, Land> byName = new HashMap<>();

	public ClaimIndex(Collection<Land> lands) {
		this.lands = List.copyOf(lands);
		for (Land l : this.lands) {
			byName.putIfAbsent(key(l.name()), l);
		}
	}

	public static ClaimIndex empty() {
		return new ClaimIndex(List.of());
	}

	public List<Land> all() {
		return lands;
	}

	public Optional<Land> byName(String name) {
		return Optional.ofNullable(byName.get(key(name)));
	}

	/** Best matches first: exact name, name prefix, name substring, then owner/member matches. Blank query matches nothing. */
	public List<Land> search(String query, int limit) {
		String q = key(query);
		if (q.isEmpty()) {
			return List.of();
		}
		List<Land> exact = new ArrayList<>();
		List<Land> prefix = new ArrayList<>();
		List<Land> contains = new ArrayList<>();
		List<Land> people = new ArrayList<>();
		for (Land l : lands) {
			String n = key(l.name());
			if (n.equals(q)) {
				exact.add(l);
			} else if (n.startsWith(q)) {
				prefix.add(l);
			} else if (n.contains(q)) {
				contains.add(l);
			} else if (matchesPerson(l, q)) {
				people.add(l);
			}
		}
		List<Land> out = new ArrayList<>(exact);
		out.addAll(prefix);
		out.addAll(contains);
		out.addAll(people);
		return out.size() > limit ? List.copyOf(out.subList(0, limit)) : List.copyOf(out);
	}

	private static boolean matchesPerson(Land l, String q) {
		if (l.owner() != null && key(l.owner()).contains(q)) {
			return true;
		}
		return l.members().stream().anyMatch(m -> key(m).contains(q));
	}

	private static String key(String s) {
		return s.strip().toLowerCase(Locale.ROOT);
	}
}
