package landmark.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClaimColorsTest {
	@Test
	void normalColoursAreStableTranslucentAndMuted() {
		int c = ClaimColors.normal("Sample");
		assertEquals(c, ClaimColors.normal("Sample"));
		assertEquals(0x66, c >>> 24);
		assertNotEquals(ClaimColors.normal("Sample"), ClaimColors.normal("Other"));
	}

	@Test
	void openFillIsMoreOpaqueThanAnyNormalColour() {
		assertTrue((ClaimColors.OPEN_FILL >>> 24) > (ClaimColors.normal("x") >>> 24));
	}
}
