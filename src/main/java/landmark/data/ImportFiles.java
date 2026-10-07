package landmark.data;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Picks what to import from files dropped on the game window. */
public final class ImportFiles {
	private ImportFiles() {}

	public static Optional<Path> firstZip(List<Path> dropped) {
		return dropped.stream().filter(p -> p.getFileName() != null && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")).findFirst();
	}
}
