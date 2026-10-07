package landmark.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Every label the settings screen can show must exist in the language file. */
class LangCoverageTest {
	private static JsonObject lang() throws IOException {
		try (var in = LangCoverageTest.class.getResourceAsStream("/assets/landmark/lang/en_us.json")) {
			return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	@Test
	void everyColourPresetAndGroupHasText() throws IOException {
		JsonObject lang = lang();
		List<String> missing = new ArrayList<>();
		for (ColorKey k : ColorKey.values()) {
			if (!lang.has(k.translationKey())) {
				missing.add(k.translationKey());
			}
		}
		for (ColorKey.Group g : ColorKey.Group.values()) {
			if (!lang.has(g.translationKey)) {
				missing.add(g.translationKey);
			}
		}
		for (ExportPreset p : ExportPreset.values()) {
			for (String key : new String[] {p.translationKey(), "landmark.export.desc." + p.id}) {
				if (!lang.has(key)) {
					missing.add(key);
				}
			}
		}
		assertTrue(missing.isEmpty(), "missing translations: " + missing);
	}

	@Test
	void theHeavyWarningSaysWhatTheSpecificationRequires() throws IOException {
		String text = lang().get("landmark.export.warning").getAsString();
		assertTrue(text.contains("larger file sizes") && text.contains("longer export times"), text);
	}
}
