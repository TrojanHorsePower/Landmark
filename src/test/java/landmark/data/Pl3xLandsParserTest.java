package landmark.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class Pl3xLandsParserTest {
	public static String sample() throws IOException {
		try (var in = Pl3xLandsParserTest.class.getResourceAsStream("/pl3x-lands-sample.json")) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	void mergesPartsOfOneLandAndSkipsUnusableMarkers() throws IOException {
		var result = Pl3xLandsParser.parse(sample());
		assertEquals(3, result.lands().size());
		assertEquals(2, result.skippedMarkers());
		Land land = result.lands().get(0);
		assertEquals("Sample Land", land.name());
		assertEquals(2, land.polygons().size());
		assertEquals(2, land.polygons().get(0).rings().size(), "outline plus one hole");
		assertEquals(12, land.chunks());
	}

	@Test
	void readsOwnerAndMembers() throws IOException {
		Land land = Pl3xLandsParser.parse(sample()).lands().get(0);
		assertEquals("ExampleOwner", land.owner());
		assertEquals(java.util.List.of("ExampleOwner", "ExampleFriend"), land.members());
	}

	@Test
	void customDescriptionMeansUnknownOwner() throws IOException {
		Land land = Pl3xLandsParser.parse(sample()).lands().get(1);
		assertEquals("Fake_Farm", land.name());
		assertNull(land.owner());
	}

	@Test
	void readsTheNewerTooltipStyle() {
		String json = "[{\"type\":\"poly\",\"data\":{\"key\":\"ID1_world_0\",\"polylines\":[{\"points\":"
				+ "[{\"x\":0,\"z\":0},{\"x\":16,\"z\":0},{\"x\":16,\"z\":16},{\"x\":0,\"z\":16}]}]},\"options\":{\"tooltip\":"
				+ "{\"content\":\"<div><span style=\\\"font-size: 200%;\\\"><span style=\\\"color: <v:land_color>;\\\">Sample Land</span>\\n</span>"
				+ "of ExampleOwner.</div><ul><li>Chunks: 5</li><li>Players (2): ExampleOwner, ExampleFriend</li></ul>\\n\"}}}]";
		var result = Pl3xLandsParser.parse(json);
		assertEquals(0, result.skippedMarkers());
		Land land = result.lands().get(0);
		assertEquals("Sample Land", land.name());
		assertEquals("ExampleOwner", land.owner());
		assertEquals(5, land.chunks());
		assertEquals(java.util.List.of("ExampleOwner", "ExampleFriend"), land.members());
	}

	@Test
	void unescapesHtmlEntitiesInNames() throws IOException {
		assertEquals("Tom's & Co", Pl3xLandsParser.parse(sample()).lands().get(2).name());
	}

	@Test
	void computesBounds() throws IOException {
		Land land = Pl3xLandsParser.parse(sample()).lands().get(0);
		assertArrayEquals(new int[] {0, 0, 192, 64}, land.bounds());
	}
}
