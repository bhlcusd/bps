package net.froglux;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WorldTest {
	@Test
	void interpolatesResultValuesAtTime() {
		Node node = new Node(1, 1);
		Result result = new Result(
			node,
			Map.of("x", List.of(0.0, 10.0), "v", List.of(2.0, 4.0)),
			List.of(0.0, 2.0)
		);

		assertEquals(5.0, result.valueAt("x", 1.0), 1e-9);
		assertEquals(3.0, result.valueAt("v", 1.0), 1e-9);
		assertEquals(0.0, result.valueAt("x", -1.0), 1e-9);
		assertEquals(10.0, result.valueAt("x", 3.0), 1e-9);
		assertTrue(Double.isNaN(result.valueAt("a", 1.0)));
	}

	@Test
	void manualYRangeDisablesAutoFitWithoutChangingRevision() {
		World world = new World(1, 0.01, 0, 10, -9.81);
		int revision = world.getRevision();

		assertTrue(world.setYRange(-5, 5));
		assertEquals(-5, world.getYMin(), 1e-9);
		assertEquals(5, world.getYMax(), 1e-9);
		assertFalse(world.isAutoFit());
		assertEquals(revision, world.getRevision());

		assertFalse(world.setYRange(5, 5));
		assertFalse(world.setYRange(5, -5));
	}

	@Test
	void autoFitCanBeReenabled() {
		World world = new World(1, 0.01, 0, 10, -9.81);
		world.setYRange(-5, 5);

		world.setAutoFit(true);
		assertTrue(world.isAutoFit());
	}

	@Test
	void findsResultForNodeAfterRun() {
		World world = new World(1, 0.01, 0, 10, -9.81);
		Node node = new Node(1, 1);
		world.addNode(node);

		assertNull(world.getResult(node));
		world.run();
		assertNotNull(world.getResult(node));
	}
}
