package landmark.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportImporterTest {
	@TempDir
	Path dir;

	private static final String MANIFEST = "{\"format\":1,\"world\":\"world\",\"dimension\":\"minecraft:overworld\","
		+ "\"source\":\"https://map.example.invalid\",\"exportedAt\":1700000000000,"
		+ "\"tiles\":{\"blocksPerTile\":4096,\"pixelSize\":512}}";
	private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};

	private Path zip(Map<String, byte[]> files) throws IOException {
		Path p = dir.resolve("t" + files.hashCode() + ".zip");
		try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(p))) {
			for (var e : files.entrySet()) {
				z.putNextEntry(new ZipEntry(e.getKey()));
				z.write(e.getValue());
				z.closeEntry();
			}
		}
		return p;
	}

	private static Map<String, byte[]> valid() throws IOException {
		Map<String, byte[]> m = new LinkedHashMap<>();
		m.put("manifest.json", MANIFEST.getBytes(StandardCharsets.UTF_8));
		m.put("lands.json", Pl3xLandsParserTest.sample().getBytes(StandardCharsets.UTF_8));
		return m;
	}

	@Test
	void importsRealExportWriterOutput() throws IOException {
		Path zip = dir.resolve("sample.zip");
		try (var in = getClass().getResourceAsStream("/export-sample.zip")) {
			Files.copy(in, zip, StandardCopyOption.REPLACE_EXISTING);
		}
		var r = ExportImporter.importZip(zip, dir.resolve("tiles"));
		assertEquals(3, r.snapshot().lands().size());
		assertEquals("minecraft:overworld", r.snapshot().dimension());
		assertEquals(1700000000000L, r.snapshot().fetchedAtMillis());
		assertEquals(2, r.tileCount());
		assertTrue(Files.exists(dir.resolve("tiles/0_0.png")));
		assertTrue(Files.exists(dir.resolve("tiles/-1_0.png")));
	}

	@Test
	void ignoresBadNamesAndFakeImagesWithoutTouchingDisk() throws IOException {
		var f = valid();
		f.put("tiles/../../evil.png", PNG);
		f.put("tiles/1_1.png", "not an image".getBytes(StandardCharsets.UTF_8));
		f.put("tiles/2_2.jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0});
		var r = ExportImporter.importZip(zip(f), dir.resolve("tiles"));
		assertEquals(1, r.tileCount());
		assertTrue(Files.exists(dir.resolve("tiles/2_2.jpg")));
		assertFalse(Files.exists(dir.resolve("evil.png")));
		assertFalse(r.warnings().isEmpty());
	}

	@Test
	void claimsOnlyExportWorksWithoutTiles() throws IOException {
		var f = valid();
		f.put("manifest.json", MANIFEST.replaceAll(",\"tiles\":\\{[^}]*}", "").getBytes(StandardCharsets.UTF_8));
		var r = ExportImporter.importZip(zip(f), dir.resolve("tiles"));
		assertEquals(0, r.tileCount());
		assertFalse(Files.exists(dir.resolve("tiles")));
	}

	@Test
	void failedImportKeepsPreviousTiles() throws IOException {
		Files.createDirectories(dir.resolve("tiles"));
		Files.write(dir.resolve("tiles/9_9.png"), PNG);
		var f = valid();
		f.put("lands.json", "[]".getBytes(StandardCharsets.UTF_8));
		assertThrows(ImportException.class, () -> ExportImporter.importZip(zip(f), dir.resolve("tiles")));
		assertTrue(Files.exists(dir.resolve("tiles/9_9.png")));
	}

	@Test
	void rejectsMissingManifest() throws IOException {
		var f = valid();
		f.remove("manifest.json");
		assertThrows(ImportException.class, () -> ExportImporter.importZip(zip(f), dir.resolve("tiles")));
	}

	@Test
	void rejectsUnsupportedFormatAndBadDimension() throws IOException {
		var f = valid();
		f.put("manifest.json", MANIFEST.replace("\"format\":1", "\"format\":2").getBytes(StandardCharsets.UTF_8));
		assertThrows(ImportException.class, () -> ExportImporter.importZip(zip(f), dir.resolve("a")));
		f.put("manifest.json", MANIFEST.replace("minecraft:overworld", "../x").getBytes(StandardCharsets.UTF_8));
		assertThrows(ImportException.class, () -> ExportImporter.importZip(zip(f), dir.resolve("b")));
	}

	@Test
	void rejectsOversizedTile() throws IOException {
		var f = valid();
		byte[] big = new byte[ExportImporter.MAX_TILE_BYTES + 1];
		System.arraycopy(PNG, 0, big, 0, PNG.length);
		f.put("tiles/0_0.png", big);
		assertThrows(ImportException.class, () -> ExportImporter.importZip(zip(f), dir.resolve("tiles")));
	}

	@Test
	void importsMoreThanFiveThousandTiles() throws IOException {
		// The real Chillzone map at Ultra quality is about 6,400 tiles.
		var f = valid();
		for (int i = 0; i < 6_000; i++) {
			f.put("tiles/" + i + "_0.png", PNG);
		}
		var r = ExportImporter.importZip(zip(f), dir.resolve("many"));
		assertEquals(6_000, r.tileCount());
	}

	@Test
	void rejectsAbsurdlyManyFiles() throws IOException {
		var f = valid();
		for (int i = 0; i <= ExportImporter.MAX_ENTRIES; i++) {
			f.put("junk/" + i, new byte[0]);
		}
		assertThrows(ImportException.class, () -> ExportImporter.importZip(zip(f), dir.resolve("junk")));
	}

	@Test
	void rejectsNonZip() throws IOException {
		Path p = dir.resolve("x.zip");
		try (OutputStream o = Files.newOutputStream(p)) {
			o.write("hello".getBytes(StandardCharsets.UTF_8));
		}
		assertThrows(ImportException.class, () -> ExportImporter.importZip(p, dir.resolve("tiles")));
	}
}
