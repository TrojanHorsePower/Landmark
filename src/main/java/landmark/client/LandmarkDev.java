package landmark.client;

/** Public entry point for the dev-only harness; does nothing unless explicitly enabled. */
public final class LandmarkDev {
	private LandmarkDev() {}

	public static void install() {
		DevHarness.install();
	}
}
