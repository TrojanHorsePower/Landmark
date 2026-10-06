package landmark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BrandingTest {
	@Test
	void modIdMatchesFabricModJson() {
		assertEquals("landmark", Branding.MOD_ID);
	}
}
