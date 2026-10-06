package landmark.data;

/** Human-readable ages for the "how old is this data" notes. */
public final class Ages {
	private Ages() {}

	public static String format(long thenMillis, long nowMillis) {
		long s = Math.max(0, (nowMillis - thenMillis) / 1000);
		if (s < 60) {
			return "just now";
		}
		long m = s / 60;
		if (m < 60) {
			return m + (m == 1 ? " minute ago" : " minutes ago");
		}
		long h = m / 60;
		if (h < 24) {
			return h + (h == 1 ? " hour ago" : " hours ago");
		}
		long d = h / 24;
		return d + (d == 1 ? " day ago" : " days ago");
	}
}
