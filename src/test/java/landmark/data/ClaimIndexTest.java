package landmark.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClaimIndexTest {
	private static Land land(String name, String owner, String... members) {
		return new Land(name, owner, List.of(members), 1, List.of());
	}

	private final ClaimIndex index = new ClaimIndex(List.of(
		land("Sunny", "Alice", "Alice", "Bob"),
		land("SunnyDale", null, "Carol"),
		land("Moon", "Sunny_Fan"),
		land("Farm", "Dave", "Dave", "sunshine")));

	@Test
	void lookupIsCaseInsensitive() {
		assertEquals("Sunny", index.byName("  sUnNy ").orElseThrow().name());
		assertTrue(index.byName("nope").isEmpty());
	}

	@Test
	void searchRanksNameMatchesBeforePeople() {
		List<String> names = index.search("sunny", 10).stream().map(Land::name).toList();
		assertEquals(List.of("Sunny", "SunnyDale", "Moon"), names, "owner Sunny_Fan comes after name matches");
	}

	@Test
	void searchFindsByMemberAndRespectsLimit() {
		assertEquals(List.of("Farm"), index.search("sunshine", 10).stream().map(Land::name).toList());
		assertEquals(1, index.search("sunny", 1).size());
	}

	@Test
	void blankQueryMatchesNothing() {
		assertTrue(index.search("  ", 10).isEmpty());
	}
}
