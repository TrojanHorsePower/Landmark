package landmark.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BundledScriptTest {
	@Test
	void exportScriptIsBundledForTheHelpScreen() throws IOException {
		try (var in = getClass().getResourceAsStream("/assets/landmark/export.js")) {
			assertNotNull(in, "tools/export.js must be packaged into the jar");
			String s = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(s.contains("landmark-export.zip") && s.contains("buildZip"));
		}
	}

	@Test
	void defaultMapUrlIsHttps() {
		assertTrue(new LandmarkConfig().mapUrl.startsWith("https://"));
	}
}
