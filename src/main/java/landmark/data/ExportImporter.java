package landmark.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Reads an export zip (see docs/export-format.md) produced by tools/export.js.
 * The zip is untrusted input: entry names are matched against fixed patterns (never used as paths),
 * and sizes are enforced while reading rather than trusted from headers.
 */
public final class ExportImporter {
	static final int MAX_ENTRIES = 5_000;
	static final int MAX_MANIFEST_BYTES = 64 * 1024;
	static final int MAX_LANDS_BYTES = 64 * 1024 * 1024;
	static final int MAX_TILE_BYTES = 4 * 1024 * 1024;
	static final long MAX_TOTAL_BYTES = 3L * 1024 * 1024 * 1024;

	private static final Pattern TILE_NAME = Pattern.compile("tiles/(-?\\d{1,6})_(-?\\d{1,6})\\.(png|jpg)");

	private ExportImporter() {}

	public record Result(ClaimSnapshot snapshot, ExportManifest manifest, int tileCount, List<String> warnings) {}

	/**
	 * Imports {@code zip}. Tiles are written to {@code tileDir} as {@code <x>_<z>.<png|jpg>}, replacing its previous
	 * contents only if the whole import succeeds. Tiles are skipped when the manifest declares none.
	 */
	public static Result importZip(Path zip, Path tileDir) throws IOException {
		Path tmp = tileDir.resolveSibling(tileDir.getFileName() + ".importing");
		deleteRecursively(tmp);
		try (ZipFile zf = new ZipFile(zip.toFile())) {
			if (zf.size() > MAX_ENTRIES) {
				throw new ImportException("Zip has too many files");
			}
			ZipEntry manifestEntry = zf.getEntry("manifest.json");
			ZipEntry landsEntry = zf.getEntry("lands.json");
			if (manifestEntry == null || landsEntry == null) {
				throw new ImportException("Not a Landmark export (manifest.json or lands.json missing)");
			}
			long[] total = {0};
			ExportManifest manifest = ExportManifest.parse(readLimited(zf, manifestEntry, MAX_MANIFEST_BYTES, total));
			Pl3xLandsParser.Result parsed = Pl3xLandsParser.parse(readLimited(zf, landsEntry, MAX_LANDS_BYTES, total));
			if (parsed.lands().isEmpty()) {
				throw new ImportException("The export contains no lands");
			}
			List<String> warnings = new ArrayList<>();
			if (parsed.skippedMarkers() > 0) {
				warnings.add(parsed.skippedMarkers() + " map shapes could not be read and were skipped");
			}

			int tiles = 0;
			int ignored = 0;
			Enumeration<? extends ZipEntry> entries = zf.entries();
			while (entries.hasMoreElements()) {
				ZipEntry e = entries.nextElement();
				String name = e.getName();
				if (e.isDirectory() || name.equals("manifest.json") || name.equals("lands.json")) {
					continue;
				}
				Matcher m = TILE_NAME.matcher(name);
				if (!m.matches() || manifest.tiles() == null) {
					ignored++;
					continue;
				}
				byte[] data = readBytesLimited(zf, e, MAX_TILE_BYTES, total);
				if (!looksLikeImage(data, m.group(3))) {
					ignored++;
					continue;
				}
				Files.createDirectories(tmp);
				Files.write(tmp.resolve(m.group(1) + "_" + m.group(2) + "." + m.group(3)), data);
				tiles++;
			}
			if (ignored > 0) {
				warnings.add(ignored + " unrecognised or invalid files in the zip were ignored");
			}
			deleteRecursively(tileDir);
			if (tiles > 0) {
				Files.move(tmp, tileDir);
			}
			ClaimSnapshot snapshot = new ClaimSnapshot(manifest.dimension(), manifest.exportedAtMillis(), manifest.source(), parsed.lands());
			return new Result(snapshot, manifest, tiles, warnings);
		} catch (ZipException e) {
			throw new ImportException("That file is not a valid zip");
		} finally {
			deleteRecursively(tmp);
		}
	}

	static boolean looksLikeImage(byte[] d, String ext) {
		if (ext.equals("png")) {
			return d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G';
		}
		return d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF;
	}

	private static String readLimited(ZipFile zf, ZipEntry e, int max, long[] total) throws IOException {
		return new String(readBytesLimited(zf, e, max, total), StandardCharsets.UTF_8);
	}

	private static byte[] readBytesLimited(ZipFile zf, ZipEntry e, int max, long[] total) throws IOException {
		try (InputStream in = zf.getInputStream(e)) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) > 0) {
				out.write(buf, 0, n);
				total[0] += n;
				if (out.size() > max) {
					throw new ImportException("A file in the zip is too large");
				}
				if (total[0] > MAX_TOTAL_BYTES) {
					throw new ImportException("The zip is too large");
				}
			}
			return out.toByteArray();
		}
	}

	static void deleteRecursively(Path p) throws IOException {
		if (!Files.exists(p)) {
			return;
		}
		try (Stream<Path> s = Files.walk(p)) {
			for (Path q : s.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(q);
			}
		}
	}
}
