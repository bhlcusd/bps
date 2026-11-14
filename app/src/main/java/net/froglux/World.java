package net.froglux;

/*
 * This file contains all classes related to storing info about Nodes and Worlds.
 * 
 * Use in conjunction with Simulator.java and Equation.java
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Represents a workplace ("world") where nodes are constrained by the same timer and constants
class World {
	// Identifier to search and sort by
	private int id;

	// World "constants" - can only be changed before a new simulation runs
	private int tick; // delay between variable "snapshots" in ms
	private double start, end; // Start and end times in sec
	private double gravity;

	// World variables
	private double time;

	// Nodes
	private List<Node> nodes;

	// Results
	private List<Result> results;
	
	public World(int id, int tick, double start, double end, double gravity) {
		this.id = id;
		this.tick = tick;
		this.start = start;
		this.end = end;
		this.gravity = gravity;

		this.nodes = new ArrayList<>();
		this.results = new ArrayList<>();
	}

	// Generates complete values for all nodes for the given time period - each value will have (start - end) / tick values
	public List<Result> run() {
		nodes.clear(); // Remove previous results

		try (ExecutorService pool = Executors.newFixedThreadPool(Simulator.SIMULATION_THREADS)) {
			for (Node node : nodes) {
				pool.execute(node); // NOTE: node's run() should automatically push to results list
			}
		}

		return results;
	}
	
	// When modifying nodes, do not directly modify nodes, use the modifier methods
	public void addNode(Node node) {
		nodes.add(node);
	}

	public boolean removeNode(Node node) {
		int index = nodes.indexOf(node);
		if (index == -1) {
			return false;
		}

		return removeNode(index);
	}

	public boolean removeNode(int index) {
		try {
			Node node = nodes.get(index);
			node.setWorld(null);
			nodes.remove(index);
		} catch (IndexOutOfBoundsException _) {
			return false;
		}

		return true;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public List<Result> getResults() {
		return results;
	}
	
	public int getId() {
		return id;
	}

	public boolean setStart(double start) {
		if (start > end) {
			return false;
		}

		this.start = start;
		return true;
	}

	public boolean setEnd(double end) {
		if (end < start) {
			return false;
		}

		this.end = end;
		return true;
	}

	public boolean setTick(int tick) {
		if (tick <= 0) {
			return false;
		}

		this.tick = tick;
		return true;
	}

	public int getTick() {
		return tick;
	}

	public void setGravity(double gravity) {
		this.gravity = gravity;
	}

	public double getGravity() {
		return gravity;
	}

	public boolean setTime(double time) {
		if (time < start || time > end) {
			return false;
		}

		this.time = time;
		return true;
	}

	public double getTime() {
		return time;
	}
}

record Result(Node node, Map<String, List<Double>> results) {}

// A node represents an object affected by the physics of the world it's played in.
class Node implements Runnable {
	private int id;
	private double mass;

	private MathFunction posEq, velEq, accEq; // Valid equations as defined by Equations class

	private World world; // Set externally by World object
	
	public Node(int id, double mass) {
		this.id = id;
		this.mass = mass;
		this.world = null;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	// Called by a ThreadExecutorPool thread to calculate the position, velocity, and acceleration for this node
	public Result calculate() {
		Result result = new Result(this, null);
		
		if (world == null) {
			return result;
		}

		if (posEq != null) {
			// Calculate all position points
			// Calculate dy/dx btwn pos points
			// Calculate dy/dx btwn vel points
		} else if (velEq != null) {
			// Calculate all vel points
			// Integrate to find pos points
			// Calculate dy/dx btwn vel points
		} else if (accEq != null) {
			// Calculate all acc points
			// Integrate to find vel points
			// Integrate to find pos points
		}

		return result;
	}

	public int getId() {
		return id;
	}

	public boolean setMass(double mass) {
		if (mass <= 0) {
			return false;
		}

		this.mass = mass;
		return true;
	}

	public double getMass() {
		return mass;
	}

	// Called by parent World's ThreadPoolExecutor and calculates the constants from World to generate (end - start) / tick values
	@Override
	public void run() {
		world.getResults().add(calculate());
	}
}
