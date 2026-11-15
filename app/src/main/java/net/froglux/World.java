package net.froglux;

/*
 * This file contains all classes related to storing info about Nodes and Worlds.
 * 
 * Use in conjunction with Simulator.java and Equation.java
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Represents a workplace ("world") where nodes are constrained by the same timer and constants
class World {
	public static final double DEFAULT_TICK = 0.01;
	public static final double DEFAULT_START = 0;
	public static final double DEFAULT_END = 10;
	public static final double DEFAULT_GRAVITY = -9.81;
	public static final double DEFAULT_MASS = 1;

	// Identifier to search and sort by 
	private int id;

	// World "constants" - can only be changed before a new simulation runs
	private double tick; // time delta
	private double start, end; // Start and end times in sec
	private double gravity;

	// World variables
	private double time;

	// Nodes
	private List<Node> nodes;

	// Results
	private List<Result> results;
	
	public World(int id, double tick, double start, double end, double gravity) {
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
		results.clear(); // Remove previous results

		try (ExecutorService pool = Executors.newFixedThreadPool(Simulator.SIMULATION_THREADS)) {
			for (Node node : nodes) {
				pool.execute(node); // NOTE: node's run() should automatically push to results list
			}
		}

		return results;
	}
	
	// When modifying nodes, do not directly modify nodes, use the modifier methods
	public void addNode(Node node) {
		node.setWorld(this);
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

	public double getStart() {
		return start;
	}

	public boolean setEnd(double end) {
		if (end < start) {
			return false;
		}

		this.end = end;
		return true;
	}

	public double getEnd() {
		return end;
	}

	public boolean setTick(double tick) {
		if (tick <= 0) {
			return false;
		}

		this.tick = tick;
		return true;
	}

	public double getTick() {
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

record Result(Node node, Map<String, List<Double>> results, List<Double> times) {}

// A node represents an object affected by the physics of the world it's played in.
class Node implements Runnable {
	enum FunctionType {
		POSITION,
		VELOCITY,
		ACCELERATION
	}

	private int id;
	private double mass;
	private double pos;
	private double vel;
	private double acc;

	private MathFunction equation; // Valid equation of specific FunctionType
	private FunctionType type;

	private World world; // Set externally by World object
	
	public Node(int id, double mass) {
		this.id = id;
		this.mass = mass;
		this.pos = 0;
		this.vel = 0;
		this.acc = 0;
		this.world = null;
	}

	// Called by a ThreadExecutorPool thread to calculate the position, velocity, and acceleration for this node
	public Result calculate() {
		Result result = new Result(this, new HashMap<>(), new ArrayList<>());
		
		if (world == null || equation == null || type == null) {
			return result;
		}

		switch (type) {
			case POSITION -> {
				// Calculate all position points
				List<Double> positions = new ArrayList<>();
				for (double i = world.getStart(); i < world.getEnd(); i += world.getTick()) {
					pos = equation.calculate(getVariables(i));
					positions.add(pos);
					result.times().add(i);
				}

				// Evaluate for end point
				if (!result.times().contains(world.getEnd())) {
					pos = equation.calculate(getVariables(world.getEnd()));
					positions.add(pos);
					result.times().add(world.getEnd());
				}

				// Calculate dy/dx btwn pos points
				List<Double> velocities = new ArrayList<>();
				for (double x : positions) {
					velocities.add(equation.derivative(x));
				}

				// Calculate dy/dx btwn vel points
				List<Double> accelerations = new ArrayList<>();
				for (double v : velocities) {
					accelerations.add(equation.derivative(v));
				}

				// Add to result
				var map = result.results();
				map.put("x", positions);
				map.put("v", velocities);
				map.put("a", accelerations);
			}
			case VELOCITY -> {
				// Calculate all vel points
				List<Double> velocities = new ArrayList<>();
				for (double i = world.getStart(); i < world.getEnd(); i += world.getTick()) {
					vel = equation.calculate(getVariables(i));
					velocities.add(vel);
					result.times().add(i);
				}

				// Integrate to find pos points
				List<Double> positions = new ArrayList<>();
				for (double t : result.times()) {
					positions.add(equation.integrate(world.getStart(), t));
				}

				// Calculate dy/dx btwn vel points
				List<Double> accelerations = new ArrayList<>();
				for (double v : velocities) {
					accelerations.add(equation.derivative(v));
				}

				var map = result.results();
				map.put("x", positions);
				map.put("v", velocities);
				map.put("a", accelerations);
			}
			case ACCELERATION -> {
				// Calculate all acc points
				List<Double> accelerations = new ArrayList<>();
				for (double i = world.getStart(); i < world.getEnd(); i += world.getTick()) {
					acc = equation.calculate(getVariables(i));
					accelerations.add(acc);
					result.times().add(i);
				}

				// Integrate to find vel points
				List<Double> velocities = new ArrayList<>();
				for (double t : result.times()) {
					velocities.add(equation.integrate(world.getStart(), t));
				}

				// Integrate to find pos points
				List<Double> positions = new ArrayList<>();
				for (int i = 0; i < velocities.size(); i++) {
					positions.add(Equation.integratePoints(velocities.subList(0, i).toArray(Point[]::new)));
				}

				var map = result.results();
				map.put("x", positions);
				map.put("v", velocities);
				map.put("a", accelerations);
			}
		}

		return result;
	}

	// Returns an array containing all variable values as MathVariables
	public MathVariable[] getVariables(double time) {
		return new MathVariable[]{
			new MathVariable("t", time),
			new MathVariable("m", mass),
			new MathVariable("x", pos),
			new MathVariable("v", vel),
			new MathVariable("a", acc)
		};
	}

	public int getId() {
		return id;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public World getWorld() {
		return world;
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

	public double getPos() {
		return pos;
	}

	public double getVel() {
		return vel;
	}

	public double getAcc() {
		return acc;
	}

	public void setEquation(MathFunction equation, FunctionType type) {
		this.equation = equation;
		this.type = type;
	} 

	public void setEquation(String eq, String respective, FunctionType type) {
		this.equation = new MathFunction(eq, respective);
		this.type = type;
	}

	public MathFunction getEquation() {
		return equation;
	}

	public FunctionType getType() {
		return type;
	}

	// Called by parent World's ThreadPoolExecutor and calculates the constants from World to generate (end - start) / tick values
	@Override
	public void run() {
		world.getResults().add(calculate());
	}
}
