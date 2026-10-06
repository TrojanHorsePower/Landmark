package landmark.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LearnedSpawnsTest {
	@TempDir
	Path dir;

	@Test
	void recordsPerLandAndDimensionCaseInsensitively() {
		var l = new LearnedSpawns();
		l.record(new LearnedSpawns.Spawn("Sunny", "minecraft:overworld", 1, 64, 2, 10));
		l.record(new LearnedSpawns.Spawn("sunny", "minecraft:overworld", 5, 70, 6, 20));
		l.record(new LearnedSpawns.Spawn("Sunny", "minecraft:the_nether", 9, 9, 9, 30));
		assertEquals(2, l.size());
		assertEquals(5, l.get("SUNNY", "minecraft:overworld").orElseThrow().x(), "newer observation replaces older");
		assertEquals(1, l.all("minecraft:the_nether").size());
		assertTrue(l.get("nope", "minecraft:overworld").isEmpty());
	}

	@Test
	void persistsAndSurvivesBrokenFiles() throws IOException {
		Path f = dir.resolve("x/learned.json");
		var l = new LearnedSpawns();
		l.record(new LearnedSpawns.Spawn("A", "minecraft:overworld", -3, 70, 4, 99));
		l.save(f);
		assertEquals(-3, LearnedSpawns.load(f).get("a", "minecraft:overworld").orElseThrow().x());
		Files.writeString(f, "{ not json");
		assertEquals(0, LearnedSpawns.load(f).size());
		assertEquals(0, LearnedSpawns.load(dir.resolve("missing.json")).size());
	}

	@Test
	void detectorIgnoresWalkingAndFiresOnJump() {
		var d = new TeleportDetector("Sunny");
		for (int i = 0; i < 100; i++) {
			assertNull(d.tick("minecraft:overworld", i * 0.3, 64, 0), "normal movement is not a teleport");
		}
		var a = d.tick("minecraft:overworld", 500.7, 80.2, -300.1);
		assertNotNull(a);
		assertEquals("Sunny", a.land());
		assertEquals(500, a.x());
		assertEquals(-301, a.z());
		assertNull(d.tick("minecraft:overworld", 900, 64, 900), "only the first jump counts");
		assertTrue(d.finished());
	}

	@Test
	void detectorFiresOnDimensionChangeAndExpires() {
		var d = new TeleportDetector("A");
		d.tick("minecraft:overworld", 0, 64, 0);
		assertNotNull(d.tick("minecraft:the_nether", 0, 64, 0));
		var e = new TeleportDetector("B");
		for (int i = 0; i < TeleportDetector.WINDOW_TICKS; i++) {
			e.tick("minecraft:overworld", 0, 64, 0);
		}
		assertNull(e.tick("minecraft:overworld", 1000, 64, 1000), "window has expired");
	}
}
