package landmark.map;

/** Pan/zoom state: which world position is at the middle of the viewport, and how many screen pixels one block is. */
public final class MapView {
	public static final double MIN_SCALE = 1.0 / 128;
	public static final double MAX_SCALE = 4.0;

	private double centerX;
	private double centerZ;
	private double scale = 1.0 / 8;
	private int width = 1;
	private int height = 1;

	public void setViewport(int width, int height) {
		this.width = Math.max(1, width);
		this.height = Math.max(1, height);
	}

	public double centerX() {
		return centerX;
	}

	public double centerZ() {
		return centerZ;
	}

	public double scale() {
		return scale;
	}

	public void centerOn(double x, double z) {
		centerX = x;
		centerZ = z;
	}

	public void setScale(double s) {
		scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
	}

	public double screenToWorldX(double sx) {
		return centerX + (sx - width / 2.0) / scale;
	}

	public double screenToWorldZ(double sz) {
		return centerZ + (sz - height / 2.0) / scale;
	}

	public double worldToScreenX(double wx) {
		return (wx - centerX) * scale + width / 2.0;
	}

	public double worldToScreenZ(double wz) {
		return (wz - centerZ) * scale + height / 2.0;
	}

	public void panByPixels(double dx, double dz) {
		centerX -= dx / scale;
		centerZ -= dz / scale;
	}

	/** Zooms by {@code factor} keeping the world point under ({@code sx},{@code sz}) fixed on screen. */
	public void zoomAt(double factor, double sx, double sz) {
		double wx = screenToWorldX(sx);
		double wz = screenToWorldZ(sz);
		setScale(scale * factor);
		centerX = wx - (sx - width / 2.0) / scale;
		centerZ = wz - (sz - height / 2.0) / scale;
	}

	/** Fits the block rectangle into the viewport with a margin (fraction of the viewport). */
	public void fit(double minX, double minZ, double maxX, double maxZ, double margin) {
		double w = Math.max(1, maxX - minX);
		double h = Math.max(1, maxZ - minZ);
		setScale(Math.min(width / w, height / h) * (1 - margin));
		centerOn((minX + maxX) / 2, (minZ + maxZ) / 2);
	}
}
