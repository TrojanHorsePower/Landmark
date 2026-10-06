package landmark.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigAndAgesTest {
	@TempDir
	Path dir;

	@Test
	void missingOrBrokenConfigGivesDefaults() throws IOException {
		assertTrue(LandmarkConfig.load(dir.resolve("none.json")).confirmTeleport);
		Path bad = dir.resolve("bad.json");
		Files.writeString(bad, "{ not json");
		assertTrue(LandmarkConfig.load(bad).refreshOnOpen);
	}

	@Test
	void savedConfigRoundTripsAndClampsCacheSize() throws IOException {
		var c = new LandmarkConfig();
		c.confirmTeleport = false;
		c.tileCacheSize = 100000;
		Path f = dir.resolve("sub/landmark.json");
		c.save(f);
		var back = LandmarkConfig.load(f);
		assertEquals(false, back.confirmTeleport);
		assertEquals(512, back.tileCacheSize);
	}

	@Test
	void formatsAges() {
		assertEquals("just now", Ages.format(1000, 30_000));
		assertEquals("1 minute ago", Ages.format(0, 61_000));
		assertEquals("5 hours ago", Ages.format(0, 5 * 3_600_000L));
		assertEquals("3 days ago", Ages.format(0, 3 * 86_400_000L));
		assertEquals("just now", Ages.format(5000, 1000), "clock skew never goes negative");
	}
}
