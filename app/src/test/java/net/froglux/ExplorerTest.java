package net.froglux;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExplorerTest {
	@Test
	void resolvesParentWorldForWorldsNodesAndEmptySelection() {
		World world = new World(1, 0.01, 0, 10, -9.81);
		Node node = new Node(1, 1);
		world.addNode(node);

		assertSame(world, Explorer.parentWorld(world));
		assertSame(world, Explorer.parentWorld(node));
		assertNull(Explorer.parentWorld(null));
	}

	@Test
	void resolvesNoParentForDetachedNode() {
		assertNull(Explorer.parentWorld(new Node(1, 1)));
	}

	@Test
	void buildsItemsSortedByWorldWithNodesNested() {
		World higher = new World(2, 0.01, 0, 10, -9.81);
		World lower = new World(1, 0.01, 0, 10, -9.81);
		Node first = new Node(1, 1);
		Node second = new Node(2, 1);
		Node third = new Node(1, 1);

		higher.addNode(first);
		higher.addNode(second);
		lower.addNode(third);

		List<Object> items = Explorer.buildItems(List.of(higher, lower));

		assertEquals(5, items.size());
		assertSame(lower, items.get(0));
		assertSame(third, items.get(1));
		assertSame(higher, items.get(2));
		assertSame(first, items.get(3));
		assertSame(second, items.get(4));
	}

	@Test
	void allocatesNextWorldIdFromHighestExistingId() {
		assertEquals(1, Explorer.nextWorldId(List.of()));

		World first = new World(1, 0.01, 0, 10, -9.81);
		World third = new World(3, 0.01, 0, 10, -9.81);
		assertEquals(4, Explorer.nextWorldId(List.of(first, third)));
	}

	@Test
	void allocatesNextNodeIdWithinParentWorld() {
		World world = new World(1, 0.01, 0, 10, -9.81);
		assertEquals(1, Explorer.nextNodeId(world));

		world.addNode(new Node(2, 1));
		world.addNode(new Node(5, 1));
		assertEquals(6, Explorer.nextNodeId(world));
	}

	@Test
	void selectsLowestWorldIdAsDeletionFallback() {
		World first = new World(1, 0.01, 0, 10, -9.81);
		World second = new World(2, 0.01, 0, 10, -9.81);

		assertSame(first, Explorer.firstWorld(List.of(second, first)));
		assertNull(Explorer.firstWorld(List.of()));
	}
}
