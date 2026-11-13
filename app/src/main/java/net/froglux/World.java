package net.froglux;

/*
 * This file contains all classes related to storing info about Nodes and Worlds.
 * 
 * Use in conjunction with Simulator.java and Equation.java
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

// TODO: Rewrite world and node, seperate it from the simulation itself - node and world really should be info classes only and should not have timers or runners involved (info is only fetched from them)

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

	// Timer & Runner - computations happen here
	private Timer timer;
	private WorldRunner runner;
	
	public World(int id, int tick, double start, double end, double gravity) {
		this.id = id;
		this.tick = tick;
		this.start = start;
		this.end = end;
		this.gravity = gravity;

		this.nodes = new ArrayList<>();
		this.timer = new Timer("world-" + id, false);
		this.runner = new WorldRunner(this);
	}

	// This should be called to simulate for the Nodes from start to end and produce node values; calculates the values for each node, creating a Map<Node, Physics>
	public void run() {

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

	public WorldRunner getRunner() {
		return runner;
	}
	
	public int getId() {
		return id;
	}

	public boolean setTick(int tick) {
		if (tick <= 0) {
			return false;
		}

		this.tick = tick;

		return true;
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

// Each world should only have ONE associated WorldRunner (use the World's getRunner()).
class WorldRunner extends TimerTask {
	private World world; // Reference to quickly fetch values

	public WorldRunner(World world) {
		this.world = world;
	}

	@Override
	public void run() {
		
	}

}

// A node represents an object affected by the physics of the world it's played in.
class Node {
	private int id;
	private double mass;

	private Equation posEq, velEq, accEq; // Valid equations as defined by Equations class

	private MathVariable pos, vel, acc; // "Current" values of the node at the given world timer

	private World world; // Set externally by World object
	
	public Node(int id, double mass) {
		this.id = id;
		this.mass = mass;
		this.world = null;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	// Called for every tick of World timer, for best performance, consider using a ThreadExecutorPool to stack operations for each time increment,
	// and then place into a list and sort by time. Or, to reduce memory/processing overhead, allocate arrays and access from there.
	public void calculate() {
		if (world == null) {
			return;
		}
	}
}
