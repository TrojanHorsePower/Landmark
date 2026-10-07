package landmark.waypoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XaeroWaypointsTest {
	@TempDir
	Path dir;

	// lines in the exact shape Xaero writes (names and places are made up)
	private static final List<String> FILE = List.of(
		"#",
		"#waypoint:name:initials:x:y:z:color:disabled:type:set:rotate_on_tp:tp_yaw:visibility_type:destination",
		"#",
		"sets:gui.xaero_default",
		"waypoint:My Base:M:120:64:-340:12:false:0:gui.xaero_default:false:0:0:false",
		"waypoint:Mine§§shaft:S:-8:~:900:4:false:0:gui.xaero_default:false:0:0:false",
		"waypoint:Hidden:H:1:2:3:1:true:0:gui.xaero_default:false:0:0:false",
		"waypoint:gui.xaero_deathpoint:D:5:70:5:0:false:1:gui.xaero_default:false:0:0:false",
		"waypoint:too:short:1:2",
		"waypoint:Broken:B:abc:64:0:1:false:0:gui.xaero_default:false:0:0:false",
		"slime_chunk_seed:12345");

	@Test
	void parsesWaypointLinesAndSkipsEverythingElse() {
		var wps = XaeroWaypoints.parse(FILE, "minecraft:overworld");
		assertEquals(3, wps.size(), "disabled, malformed and non-waypoint lines are skipped");
		var base = wps.get(0);
		assertEquals("My Base", base.name());
		assertEquals(120, base.x());
		assertEquals(64, base.y());
		assertEquals(-340, base.z());
		assertEquals(0xFFFF0000, base.argb(), "colour index 12 is red");
		assertEquals(ExternalWaypoint.Source.XAERO, base.source());
		assertEquals("minecraft:overworld", base.dimension());
	}

	@Test
	void decodesEscapedColonsAndUnknownHeights() {
		var shaft = XaeroWaypoints.parse(FILE, "minecraft:overworld").get(1);
		assertEquals("Mine:shaft", shaft.name());
		assertNull(shaft.y(), "a ~ means Xaero has no height for it");
		assertEquals(900, shaft.z());
	}

	@Test
	void mapsDimensionFolders() {
		assertEquals("minecraft:overworld", XaeroWaypoints.dimensionOf("dim%0"));
		assertEquals("minecraft:the_nether", XaeroWaypoints.dimensionOf("dim%-1"));
		assertEquals("minecraft:the_end", XaeroWaypoints.dimensionOf("dim%1"));
		assertEquals("somepack:caves", XaeroWaypoints.dimensionOf("dim%somepack$caves"));
		assertNull(XaeroWaypoints.dimensionOf("backup"));
		assertNull(XaeroWaypoints.dimensionOf("dim%"));
	}

	@Test
	void matchesTheServerFolderLoosely() {
		assertTrue(XaeroWaypoints.containerMatches("Multiplayer_play.example.invalid", "play.example.invalid"));
		assertTrue(XaeroWaypoints.containerMatches("Multiplayer_Play.Example.Invalid", "play.example.invalid"), "case does not matter");
		assertTrue(XaeroWaypoints.containerMatches("Multiplayer_play.example.invalid", "play.example.invalid:25565"), "a port is ignored");
		assertTrue(XaeroWaypoints.containerMatches("Multiplayer_play.example.invalid:25570", "play.example.invalid:25565"));
		assertTrue(XaeroWaypoints.containerMatches("Multiplayer_my%us%server.example.invalid", "my_server.example.invalid"), "escaped characters");
		assertFalse(XaeroWaypoints.containerMatches("Multiplayer_other.example.invalid", "play.example.invalid"));
		assertFalse(XaeroWaypoints.containerMatches("Realms_123", "play.example.invalid"));
		assertFalse(XaeroWaypoints.containerMatches("Multiplayer_play.example.invalid", ""));
		assertFalse(XaeroWaypoints.containerMatches("Multiplayer_play.example.invalid", null));
	}

	private Path writeFile(String root, String container, String dim, List<String> lines) throws IOException {
		Path d = dir.resolve(root).resolve(container).resolve(dim);
		Files.createDirectories(d);
		Path f = d.resolve("mw$default_1.txt");
		Files.write(f, lines, StandardCharsets.UTF_8);
		return f;
	}

	@Test
	void loadsFromTheCurrentFolderLayoutForTheRightServerOnly() throws IOException {
		writeFile("xaero/minimap", "Multiplayer_play.example.invalid", "dim%0", FILE);
		writeFile("xaero/minimap", "Multiplayer_play.example.invalid", "dim%-1", List.of("waypoint:Portal:P:15:70:-3:9:false:0:gui.xaero_default:false:0:0:false"));
		writeFile("xaero/minimap", "Multiplayer_elsewhere.example.invalid", "dim%0", List.of("waypoint:Not mine:N:1:1:1:1:false:0:s:false:0:0:false"));
		var wps = XaeroWaypoints.load(dir, "play.example.invalid");
		assertEquals(4, wps.size());
		assertTrue(wps.stream().noneMatch(w -> w.name().equals("Not mine")), "another server's waypoints must never show");
		var portal = wps.stream().filter(w -> w.name().equals("Portal")).findFirst().orElseThrow();
		assertEquals("minecraft:the_nether", portal.dimension());
	}

	@Test
	void olderFolderLayoutStillWorks() throws IOException {
		writeFile("XaeroWaypoints", "Multiplayer_play.example.invalid", "dim%0", List.of("waypoint:Old:O:1:2:3:2:false:0:s:false:0:0:false"));
		assertEquals(1, XaeroWaypoints.load(dir, "play.example.invalid").size());
	}

	@Test
	void noFolderMeansNoWaypointsNotAnError() {
		assertTrue(XaeroWaypoints.load(dir, "play.example.invalid").isEmpty());
	}

	@Test
	void aCorruptFileDoesNotHideTheGoodOnes() throws IOException {
		Path d = dir.resolve("xaero/minimap/Multiplayer_play.example.invalid/dim%0");
		Files.createDirectories(d);
		Files.write(d.resolve("a.txt"), new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 1, 2});
		Files.write(d.resolve("b.txt"), List.of("waypoint:Fine:F:1:2:3:1:false:0:s:false:0:0:false"), StandardCharsets.UTF_8);
		assertTrue(XaeroWaypoints.load(dir, "play.example.invalid").stream().anyMatch(w -> w.name().equals("Fine")));
	}
}
