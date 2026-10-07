package landmark.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ColorAndPresetTest {
	@TempDir
	Path dir;

	@Test
	void parsesHexInTheSupportedForms() {
		assertEquals(0xFF1E90FF, ColorMath.parseHex("#1E90FF", false).getAsInt());
		assertEquals(0xFF1E90FF, ColorMath.parseHex("1e90ff", false).getAsInt(), "the # is optional and case does not matter");
		assertEquals(0xFFFFAA00, ColorMath.parseHex(" #fa0 ", false).getAsInt(), "three digits expand");
		assertEquals(0x801E90FF, ColorMath.parseHex("#1E90FF80", true).getAsInt());
	}

	@Test
	void rejectsBadHex() {
		for (String bad : new String[] {"", "#", "#12", "#GGGGGG", "#1234567", "red", "#1E90FF8"}) {
			assertTrue(ColorMath.parseHex(bad, true).isEmpty(), bad);
		}
		assertTrue(ColorMath.parseHex("#1E90FF80", false).isEmpty(), "opacity is not allowed for opaque colours");
	}

	@Test
	void formatsHex() {
		assertEquals("#1E90FF", ColorMath.format(0xFF1E90FF, false));
		assertEquals("#1E90FF80", ColorMath.format(0x801E90FF, true));
		assertEquals("#000000", ColorMath.format(0xFF000000, false));
	}

	@Test
	void hsvRoundTripsAndKnownColours() {
		for (int c : new int[] {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF1E90FF, 0xFF808080, 0xFFFFD34D, 0xFF000000, 0xFFFFFFFF}) {
			float[] h = ColorMath.toHsv(c);
			assertEquals(c, ColorMath.fromHsv(h[0], h[1], h[2]), ColorMath.format(c, false));
		}
		assertEquals(0xFFFF0000, ColorMath.fromHsv(0, 1, 1));
		assertEquals(0xFF00FF00, ColorMath.fromHsv(1f / 3, 1, 1));
		assertEquals(0xFF0000FF, ColorMath.fromHsv(2f / 3, 1, 1));
		assertEquals(0xFFFF0000, ColorMath.fromHsv(1, 1, 1), "hue wraps");
	}

	@Test
	void everyColourHasAUniqueIdAndAValidDefault() {
		var ids = new HashSet<String>();
		for (ColorKey k : ColorKey.values()) {
			assertTrue(ids.add(k.id), "duplicate id " + k.id);
			assertEquals(k, ColorKey.byId(k.id));
			assertTrue(k.hasAlpha || k.defaultArgb >>> 24 == 0xFF, k.id + " is opaque so it must not have alpha");
		}
	}

	@Test
	void overridesDefaultsAndResets() {
		var c = new LandmarkConfig();
		assertEquals(ColorKey.OPEN_OUTLINE.defaultArgb, c.color(ColorKey.OPEN_OUTLINE));
		c.setColor(ColorKey.OPEN_OUTLINE, 0xFF1E90FF);
		assertEquals(0xFF1E90FF, c.color(ColorKey.OPEN_OUTLINE));
		assertFalse(c.isDefaultColor(ColorKey.OPEN_OUTLINE));
		c.setColor(ColorKey.OPEN_OUTLINE, ColorKey.OPEN_OUTLINE.defaultArgb);
		assertTrue(c.colors.isEmpty(), "a colour equal to the default is not stored");
		c.setColor(ColorKey.OPEN_FILL, 0x40112233);
		c.setColor(ColorKey.PIN, 0x10ABCDEF);
		assertEquals(0xFFABCDEF, c.color(ColorKey.PIN), "opacity is ignored for opaque colours");
		c.resetAllColors();
		assertTrue(c.colors.isEmpty());
	}

	@Test
	void savedColoursAndTogglesRoundTrip() throws IOException {
		var c = new LandmarkConfig();
		c.setColor(ColorKey.OPEN_FILL, 0x80112233);
		c.setColor(ColorKey.TEXT_ERROR, 0xFF010203);
		c.saveSpawns = false;
		c.showPlayerMarker = false;
		c.setExportPreset(ExportPreset.ULTRA);
		Path f = dir.resolve("c.json");
		c.save(f);
		var back = LandmarkConfig.load(f);
		assertEquals(0x80112233, back.color(ColorKey.OPEN_FILL));
		assertEquals(0xFF010203, back.color(ColorKey.TEXT_ERROR));
		assertFalse(back.saveSpawns);
		assertFalse(back.showPlayerMarker);
		assertTrue(back.confirmTeleport);
		assertEquals(ExportPreset.ULTRA, back.exportPreset());
	}

	@Test
	void oldConfigFilesStillLoadWithDefaultsForNewSettings() throws IOException {
		Path f = dir.resolve("old.json");
		Files.writeString(f, "{\"confirmTeleport\": false, \"refreshOnOpen\": true, \"mapUrl\": \"https://example.invalid/\", \"tileCacheSize\": 48}", StandardCharsets.UTF_8);
		var c = LandmarkConfig.load(f);
		assertFalse(c.confirmTeleport);
		assertTrue(c.saveSpawns && c.showPlayerMarker);
		assertEquals(ExportPreset.LOW, c.exportPreset());
		assertTrue(c.colors.isEmpty());
	}

	@Test
	void junkInTheFileIsIgnoredNotFatal() throws IOException {
		Path f = dir.resolve("junk.json");
		Files.writeString(f, "{\"colors\": {\"open_fill\": \"not a colour\", \"made_up\": \"#FFFFFF\", \"pin\": \"#112233\"}, \"exportQuality\": \"bogus\"}", StandardCharsets.UTF_8);
		var c = LandmarkConfig.load(f);
		assertEquals(ColorKey.OPEN_FILL.defaultArgb, c.color(ColorKey.OPEN_FILL), "invalid hex falls back to the default");
		assertEquals(0xFF112233, c.color(ColorKey.PIN));
		assertFalse(c.colors.containsKey("made_up"));
		assertEquals(ExportPreset.LOW, c.exportPreset());
	}

	@Test
	void presetsMatchTheSpecification() {
		assertEquals(3, ExportPreset.LOW.tileFolder);
		assertEquals("small", ExportPreset.LOW.tileMode);
		assertEquals(3, ExportPreset.MEDIUM.tileFolder);
		assertEquals("full", ExportPreset.MEDIUM.tileMode);
		assertEquals(2, ExportPreset.HIGH.tileFolder);
		assertEquals("full", ExportPreset.HIGH.tileMode);
		assertEquals(1, ExportPreset.ULTRA.tileFolder);
		assertEquals("full", ExportPreset.ULTRA.tileMode);
		assertFalse(ExportPreset.LOW.isHeavy() || ExportPreset.MEDIUM.isHeavy());
		assertTrue(ExportPreset.HIGH.isHeavy() && ExportPreset.ULTRA.isHeavy());
		assertEquals(ExportPreset.LOW, ExportPreset.byId("nonsense"));
	}

	private static String bundledScript() throws IOException {
		try (var in = ColorAndPresetTest.class.getResourceAsStream("/assets/landmark/export.js")) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	void presetsChangeOnlyTheTwoSettingsInTheRealScript() throws IOException {
		String original = bundledScript();
		for (ExportPreset p : ExportPreset.values()) {
			String out = p.apply(original);
			assertTrue(out.contains("tileFolder: " + p.tileFolder), p.id);
			assertTrue(out.contains("tileMode: '" + p.tileMode + "'"), p.id);
			// everything else is byte-for-byte identical once those two values are put back
			String restored = ExportPreset.LOW.apply(out);
			assertEquals(ExportPreset.LOW.apply(original), restored, p.id);
			int differing = 0;
			String[] a = original.split("\n");
			String[] b = out.split("\n");
			assertEquals(a.length, b.length);
			for (int i = 0; i < a.length; i++) {
				if (!a[i].equals(b[i])) {
					differing++;
				}
			}
			assertTrue(differing <= 2, p.id + " changed " + differing + " lines");
		}
	}

	@Test
	void applyFailsLoudlyIfTheScriptLosesItsSettings() {
		assertThrows(IllegalStateException.class, () -> ExportPreset.HIGH.apply("const CONFIG = { world: 'world' };"));
	}

	@Test
	void picksTheFirstZipFromDroppedFiles() {
		assertEquals("b.ZIP", ImportFiles.firstZip(List.of(Path.of("a.txt"), Path.of("b.ZIP"), Path.of("c.zip"))).orElseThrow().getFileName().toString());
		assertTrue(ImportFiles.firstZip(List.of(Path.of("a.txt"), Path.of("dir"))).isEmpty());
		assertTrue(ImportFiles.firstZip(List.of()).isEmpty());
	}
}
