package landmark.waypoints;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Reads the waypoints Xaero's Minimap saves on disk: {@code <game>/xaero/minimap/Multiplayer_<server>/dim%<n>/<world>.txt}, one
 * {@code waypoint:name:initials:x:y:z:color:disabled:type:set:...} line per waypoint. The format was taken from Xaero's own
 * reader (and cross-checked against an independent open-source converter); nothing is ever written back.
 */
public final class XaeroWaypoints {
	/** Xaero's colours by index: the 16 chat colours, then magenta, light blue, lime, pink and brown (those five are approximate). */
	private static final int[] COLORS = {
		0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
		0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF0000, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF,
		0xFFFF00FF, 0xFF55AAFF, 0xFF80FF00, 0xFFFF8CC8, 0xFF8B5A2B};

	private XaeroWaypoints() {}

	/** All waypoints of the folder for this server, across every dimension and set; disabled ones are left out. */
	public static List<ExternalWaypoint> load(Path gameDir, String serverAddress) {
		List<ExternalWaypoint> out = new ArrayList<>();
		for (Path root : roots(gameDir)) {
			try (Stream<Path> containers = Files.list(root)) {
				for (Path container : containers.filter(Files::isDirectory).toList()) {
					if (containerMatches(container.getFileName().toString(), serverAddress)) {
						loadContainer(container, out);
					}
				}
			} catch (IOException e) {
				// no such folder or unreadable: nothing to show
			}
		}
		return out;
	}

	/** Where Xaero keeps waypoints: the current location first, then the older ones. */
	static List<Path> roots(Path gameDir) {
		List<Path> roots = new ArrayList<>();
		for (String p : new String[] {"xaero/minimap", "XaeroWaypoints", "config/XaeroWaypoints"}) {
			Path candidate = gameDir.resolve(p);
			if (Files.isDirectory(candidate)) {
				roots.add(candidate);
			}
		}
		return roots;
	}

	private static void loadContainer(Path container, List<ExternalWaypoint> out) {
		for (Path dimDir : listSorted(container, Files::isDirectory)) {
			String dimension = dimensionOf(dimDir.getFileName().toString());
			if (dimension == null) {
				continue;
			}
			for (Path file : listSorted(dimDir, f -> f.getFileName().toString().endsWith(".txt"))) {
				// Each file stands alone: a broken or half-written one must never hide the others.
				try {
					out.addAll(parse(Files.readAllLines(file, StandardCharsets.UTF_8), dimension));
				} catch (IOException | RuntimeException e) {
					// skip this file
				}
			}
		}
	}

	/** The matching entries of a folder in a fixed order (directory listing order is not guaranteed); empty if unreadable. */
	private static List<Path> listSorted(Path dir, java.util.function.Predicate<Path> filter) {
		try (Stream<Path> entries = Files.list(dir)) {
			return entries.filter(filter).sorted().toList();
		} catch (IOException e) {
			return List.of();
		}
	}

	/** {@code dim%0} is the overworld, {@code dim%-1} the nether, {@code dim%1} the end, {@code dim%ns$path} is {@code ns:path}. */
	public static String dimensionOf(String folder) {
		if (!folder.startsWith("dim%")) {
			return null;
		}
		String id = folder.substring(4);
		switch (id) {
			case "0":
				return "minecraft:overworld";
			case "-1":
				return "minecraft:the_nether";
			case "1":
				return "minecraft:the_end";
			default:
				int dollar = id.indexOf('$');
				return dollar > 0 && dollar < id.length() - 1 ? id.substring(0, dollar) + ":" + id.substring(dollar + 1) : null;
		}
	}

	/**
	 * Whether a container folder is for this server. Xaero names it {@code Multiplayer_<address>} with a few characters escaped, and
	 * the address may or may not carry a port, so the host is compared case-insensitively and a port is ignored.
	 */
	public static boolean containerMatches(String folder, String serverAddress) {
		if (!folder.startsWith("Multiplayer_") || serverAddress == null || serverAddress.isBlank()) {
			return false;
		}
		String name = unescape(folder.substring("Multiplayer_".length())).toLowerCase(Locale.ROOT);
		return hostOf(name).equals(hostOf(serverAddress.strip().toLowerCase(Locale.ROOT)));
	}

	private static String unescape(String s) {
		return s.replace("%us%", "_").replace("%fs%", "/").replace("%bs%", "\\").replace("%lb%", "[").replace("%rb%", "]");
	}

	private static String hostOf(String address) {
		if (address.startsWith("[")) {
			int end = address.indexOf(']');
			return end > 0 ? address.substring(0, end + 1) : address;
		}
		int colon = address.lastIndexOf(':');
		return colon > 0 && address.indexOf(':') == colon ? address.substring(0, colon) : address;
	}

	/** Parses the lines of one waypoint file. Lines that are not waypoints, are malformed, or are disabled are skipped. */
	public static List<ExternalWaypoint> parse(List<String> lines, String dimension) {
		List<ExternalWaypoint> out = new ArrayList<>();
		for (String line : lines) {
			if (!line.regionMatches(true, 0, "waypoint:", 0, 9)) {
				continue;
			}
			String[] p = line.split(":", -1);
			if (p.length < 10) {
				continue;
			}
			try {
				if ("true".equals(p[7])) {
					continue; // switched off by the player in Xaero's
				}
				Integer y = "~".equals(p[4]) ? null : Integer.valueOf(Integer.parseInt(p[4]));
				int colorIndex = Integer.parseInt(p[6]);
				int argb = colorIndex >= 0 && colorIndex < COLORS.length ? COLORS[colorIndex] : 0xFFFFFFFF;
				out.add(new ExternalWaypoint(ExternalWaypoint.Source.XAERO, p[1].replace("§§", ":"), dimension,
					Integer.parseInt(p[3]), y, Integer.parseInt(p[5]), argb));
			} catch (NumberFormatException e) {
				// skip a damaged line
			}
		}
		return out;
	}
}
