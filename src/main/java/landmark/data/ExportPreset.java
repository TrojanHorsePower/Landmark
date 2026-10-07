package landmark.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Export quality presets. They only change {@code tileFolder} and {@code tileMode} in the CONFIG block of the export script
 * (tools/export.js); everything else in the script is identical for every preset.
 */
public enum ExportPreset {
	LOW("low", 3, "small"),
	MEDIUM("medium", 3, "full"),
	HIGH("high", 2, "full"),
	ULTRA("ultra", 1, "full");

	private static final Pattern FOLDER = Pattern.compile("(tileFolder:\\s*)\\d+");
	private static final Pattern MODE = Pattern.compile("(tileMode:\\s*')[a-z]+(')");

	public final String id;
	public final int tileFolder;
	public final String tileMode;

	ExportPreset(String id, int tileFolder, String tileMode) {
		this.id = id;
		this.tileFolder = tileFolder;
		this.tileMode = tileMode;
	}

	public String translationKey() {
		return "landmark.export.preset." + id;
	}

	/** Larger files on disk and a longer export. */
	public boolean isHeavy() {
		return this == HIGH || this == ULTRA;
	}

	public static ExportPreset byId(String id) {
		for (ExportPreset p : values()) {
			if (p.id.equals(id)) {
				return p;
			}
		}
		return LOW;
	}

	/** Returns the script with this preset's values written into its CONFIG block. Fails if the script no longer has them. */
	public String apply(String script) {
		String out = replaceOnce(FOLDER, script, "$1" + tileFolder, "tileFolder");
		return replaceOnce(MODE, out, "$1" + tileMode + "$2", "tileMode");
	}

	private static String replaceOnce(Pattern p, String text, String replacement, String what) {
		Matcher m = p.matcher(text);
		int n = 0;
		while (m.find()) {
			n++;
		}
		if (n != 1) {
			throw new IllegalStateException("Expected exactly one '" + what + "' setting in the export script, found " + n);
		}
		return p.matcher(text).replaceFirst(replacement);
	}
}
