package net.froglux;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphViewportTest {
	@Test
	void panShiftsRangesByDataDelta() {
		GraphViewport viewport = new GraphViewport(0, 10, -5, 5, 1, 1);
		GraphViewport moved = Graph.panViewport(viewport, 2, 3);

		assertNotNull(moved);
		assertEquals(-2, moved.minX(), 1e-9);
		assertEquals(8, moved.maxX(), 1e-9);
		assertEquals(-2, moved.minY(), 1e-9);
		assertEquals(8, moved.maxY(), 1e-9);
		assertEquals(1, moved.xTick(), 1e-9);
		assertEquals(1, moved.yTick(), 1e-9);
	}

	@Test
	void zoomKeepsAnchorAtSameRelativePosition() {
		GraphViewport viewport = new GraphViewport(0, 10, -5, 5, 1, 1);
		double anchorX = 4;
		double anchorY = 2;
		GraphViewport zoomed = Graph.zoomViewport(viewport, anchorX, anchorY, 0.5);

		assertNotNull(zoomed);
		assertEquals(5, zoomed.width(), 1e-9);
		assertEquals(5, zoomed.height(), 1e-9);

		double beforeX = (anchorX - viewport.minX()) / viewport.width();
		double afterX = (anchorX - zoomed.minX()) / zoomed.width();
		assertEquals(beforeX, afterX, 1e-9);

		double beforeY = (viewport.maxY() - anchorY) / viewport.height();
		double afterY = (zoomed.maxY() - anchorY) / zoomed.height();
		assertEquals(beforeY, afterY, 1e-9);
	}

	@Test
	void zoomClampsToValidSpan() {
		GraphViewport viewport = new GraphViewport(0, 10, -5, 5, 1, 1);
		GraphViewport zoomedIn = Graph.zoomViewport(viewport, 4, 2, 1e-20);
		GraphViewport zoomedOut = Graph.zoomViewport(viewport, 4, 2, 1e20);

		assertNotNull(zoomedIn);
		assertTrue(zoomedIn.width() >= 1e-9);
		assertTrue(zoomedIn.height() >= 1e-9);
		assertTrue(zoomedIn.xTick() > 0);
		assertTrue(zoomedIn.yTick() > 0);

		assertNotNull(zoomedOut);
		assertTrue(zoomedOut.width() <= 1e12);
		assertTrue(zoomedOut.height() <= 1e12);
		assertTrue(zoomedOut.xTick() > 0);
		assertTrue(zoomedOut.yTick() > 0);
	}
}
