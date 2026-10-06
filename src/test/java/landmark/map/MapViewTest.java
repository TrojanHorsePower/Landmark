package landmark.map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MapViewTest {
	private static MapView view() {
		var v = new MapView();
		v.setViewport(800, 600);
		v.centerOn(1000, -500);
		v.setScale(0.5);
		return v;
	}

	@Test
	void roundTripsBetweenScreenAndWorld() {
		var v = view();
		assertEquals(1000, v.screenToWorldX(400), 1e-9);
		assertEquals(-500, v.screenToWorldZ(300), 1e-9);
		assertEquals(123.0, v.screenToWorldX(v.worldToScreenX(123.0)), 1e-9);
	}

	@Test
	void zoomKeepsPointUnderCursorFixed() {
		var v = view();
		double wx = v.screenToWorldX(100), wz = v.screenToWorldZ(500);
		v.zoomAt(2, 100, 500);
		assertEquals(1.0, v.scale(), 1e-9);
		assertEquals(wx, v.screenToWorldX(100), 1e-9);
		assertEquals(wz, v.screenToWorldZ(500), 1e-9);
	}

	@Test
	void scaleIsClampedAndPanFollowsTheMouse() {
		var v = view();
		v.setScale(1000);
		assertEquals(MapView.MAX_SCALE, v.scale());
		v.setScale(0.5);
		v.panByPixels(50, -20);
		assertEquals(900, v.centerX(), 1e-9);
		assertEquals(-460, v.centerZ(), 1e-9);
	}

	@Test
	void fitShowsWholeRectangle() {
		var v = view();
		v.fit(-1000, -1000, 1000, 1000, 0.1);
		assertEquals(0, v.centerX(), 1e-9);
		assertEquals(600 / 2000.0 * 0.9, v.scale(), 1e-9);
	}
}
