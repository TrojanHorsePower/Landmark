package landmark.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataStoreTest {
	@TempDir
	Path dir;

	private Path sampleZip() throws IOException {
		Path zip = dir.resolve("sample.zip");
		try (var in = getClass().getResourceAsStream("/export-sample.zip")) {
			Files.copy(in, zip, StandardCopyOption.REPLACE_EXISTING);
		}
		return zip;
	}

	@Test
	void importThenLoadRoundTrips() throws IOException {
		var store = new DataStore(dir.resolve("data"));
		assertTrue(store.load("minecraft:overworld").isEmpty());
		store.importZip(sampleZip());
		var stored = store.load("minecraft:overworld").orElseThrow();
		assertEquals(3, stored.snapshot().lands().size());
		assertEquals(4096, stored.tiles().blocksPerTile());
		assertTrue(Files.exists(stored.tileDir().resolve("0_0.png")));
		assertFalse(Files.exists(dir.resolve("data/.import-staging")), "staging is cleaned up");
	}

	@Test
	void otherDimensionsStayEmpty() throws IOException {
		var store = new DataStore(dir.resolve("data"));
		store.importZip(sampleZip());
		assertTrue(store.load("minecraft:the_nether").isEmpty());
	}

	@Test
	void failedImportKeepsExistingData() throws IOException {
		var store = new DataStore(dir.resolve("data"));
		store.importZip(sampleZip());
		Path bad = dir.resolve("bad.zip");
		Files.writeString(bad, "nope");
		try {
			store.importZip(bad);
		} catch (ImportException expected) {
			// fine
		}
		assertTrue(store.load("minecraft:overworld").isPresent());
	}

	@Test
	void corruptStoredDataCountsAsMissing() throws IOException {
		var store = new DataStore(dir.resolve("data"));
		Path d = store.dimensionDir("minecraft:overworld");
		Files.createDirectories(d);
		Files.writeString(d.resolve("claims.bin"), "garbage");
		assertTrue(store.load("minecraft:overworld").isEmpty());
	}
}
