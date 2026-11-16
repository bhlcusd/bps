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

	@Test
	void positionEquationDerivesVelocityAndAccelerationWithRespectToTime() {
		World world = new World(1, 1, 0, 5, -9.81);
		Node node = new Node(1, 1);
		node.setEquation(new MathFunction("(t - 3)^2", "t"), Node.FunctionType.POSITION);
		world.addNode(node);
		world.run();

		Result result = world.getResult(node);

		assertEquals(1, result.valueAt("x", 4), 1e-6);
		assertEquals(2, result.valueAt("v", 4), 1e-6);
		assertEquals(2, result.valueAt("a", 4), 1e-5);
		assertEquals(2, result.valueAt("a", 2), 1e-5);
	}

	@Test
	void velocityEquationDerivesAccelerationWithRespectToTime() {
		World world = new World(1, 1, 0, 5, -9.81);
		Node node = new Node(1, 1);
		node.setEquation(new MathFunction("t^2", "t"), Node.FunctionType.VELOCITY);
		world.addNode(node);
		world.run();

		Result result = world.getResult(node);

		assertEquals(16, result.valueAt("v", 4), 1e-6);
		assertEquals(8, result.valueAt("a", 4), 1e-6);
		assertEquals(64.0 / 3, result.valueAt("x", 4), 1e-4);
	}

	@Test
	void scrollModeDefaultsToSystemWithoutChangingRevision() {
		World world = new World(1, 0.01, 0, 10, -9.81);
		int revision = world.getRevision();

		assertEquals(World.ScrollMode.SYSTEM, world.getScrollMode());
		assertTrue(World.ScrollMode.SYSTEM.isNatural(true));
		assertFalse(World.ScrollMode.SYSTEM.isNatural(false));
		assertTrue(World.ScrollMode.NATURAL.isNatural(false));
		assertFalse(World.ScrollMode.TRADITIONAL.isNatural(true));

		world.setScrollMode(World.ScrollMode.TRADITIONAL);
		assertEquals(World.ScrollMode.TRADITIONAL, world.getScrollMode());
		assertEquals(revision, world.getRevision());
	}
}
