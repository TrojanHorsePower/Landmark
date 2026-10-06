package landmark.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class OpenSpawnsTest {
	@Test
	void cleansSuggestionTexts() {
		var s = OpenSpawns.fromSuggestionTexts(List.of(" zeta", "Alpha", "", "alpha", "  ", "Beta"), 5L);
		assertEquals(List.of("Alpha", "Beta", "zeta"), s.names());
		assertEquals(5L, s.fetchedAtMillis());
	}

	@Test
	void joinSplitsKnownAndUnknown() {
		var index = new ClaimIndex(List.of(new Land("Sunny", "A", List.of(), 1, List.of())));
		var j = new OpenSpawns(List.of("sunny", "Ghost"), 0L).join(index);
		assertEquals(1, j.known().size());
		assertEquals(List.of("Ghost"), j.unknownNames());
	}
}
