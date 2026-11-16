package net.froglux;

import java.awt.Color;

/*
 * This file contains all classes related to storing info about Nodes and Worlds.
 * 
 * Use in conjunction with Simulator.java and Equation.java
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Represents a workplace ("world") where nodes are constrained by the same timer and constants
class World {
	public static final double DEFAULT_TICK = 0.01;
	public static final double DEFAULT_START = 0;
	public static final double DEFAULT_END = 10;
	public static final double DEFAULT_GRAVITY = -9.81;
	public static final double DEFAULT_MASS = 1;
	public static final double DEFAULT_Y_MIN = -10;
	public static final double DEFAULT_Y_MAX = 10;

	// Identifier to search and sort by 
	private int id;

	// World "constants" - can only be changed before a new simulation runs
	private double tick; // time delta
	private double start, end; // Start and end times in sec
	private double gravity;

	// World variables
	private double time;

	// Graph view
	private double yMin, yMax;
	private boolean autoFit;

	// Nodes
	private List<Node> nodes;

	// Results
	private List<Result> results;

	// Revision tracker
	private int revision;
	
	public World(int id, double tick, double start, double end, double gravity) {
		this.id = id;
		this.tick = tick;
		this.start = start;
		this.end = end;
		this.gravity = gravity;
		this.revision = 0;

		this.yMin = DEFAULT_Y_MIN;
		this.yMax = DEFAULT_Y_MAX;
		this.autoFit = true;

		this.nodes = new ArrayList<>();
		this.results = new ArrayList<>();
	}

	// Generates complete values for all nodes for the given time period - each value will have (start - end) / tick values
	public Report run() {
		List<Node> snapshot = List.copyOf(nodes);
		List<Result> results = new ArrayList<>(snapshot.size());
		List<String> errors = new ArrayList<>();

		try (ExecutorService pool = Executors.newFixedThreadPool(Simulator.SIMULATION_THREADS)) {
			List<Future<Result>> futures = snapshot.stream()
				.map(pool::submit)
				.toList();
			
			for (int i = 0; i < futures.size(); i++) {
				try {
					results.add(futures.get(i).get());
				} catch (ExecutionException e) {
					errors.add("Node #" + snapshot.get(i).getId() + ": " + e.getCause().getMessage());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}

		this.results = results;
		return new Report(results, errors);
	}
	
	// When modifying nodes, do not directly modify nodes, use the modifier methods
	public void addNode(Node node) {
		node.setWorld(this);
		nodes.add(node);
		touch();
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

		touch();
		return true;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public List<Result> getResults() {
		return results;
	}

	public Result getResult(Node node) {
		for (Result result : results) {
			if (result.node() == node) {
				return result;
			}
		}

		return null;
	}
	
	public int getId() {
		return id;
	}

	public boolean setStart(double start) {
		if (start > end) {
			return false;
		}

		this.start = start;
		touch();
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
		touch();
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
		touch();
		return true;
	}

	public double getTick() {
		return tick;
	}

	public void setGravity(double gravity) {
		this.gravity = gravity;
		touch();
	}

	public double getGravity() {
		return gravity;
	}

	public boolean setYRange(double yMin, double yMax) {
		if (!Double.isFinite(yMin) || !Double.isFinite(yMax) || yMin >= yMax) {
			return false;
		}

		this.yMin = yMin;
		this.yMax = yMax;
		this.autoFit = false;
		return true;
	}

	public double getYMin() {
		return yMin;
	}

	public double getYMax() {
		return yMax;
	}

	public boolean isAutoFit() {
		return autoFit;
	}

	public void setAutoFit(boolean autoFit) {
		this.autoFit = autoFit;
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

	public int getRevision() {
		return revision;
	}

	public void touch() {
		revision++;
	}
}

record Report(List<Result> results, List<String> errors) {
	public Report {
		results = List.copyOf(results);
		errors = List.copyOf(errors);
	}
}

record Result(Node node, Map<String, List<Double>> results, List<Double> times) {
	public List<Double> valuesFor(Node.FunctionType type) {
		if (type == null) {
			return List.of();
		}

		return switch (type) {
			case POSITION -> results.getOrDefault("x", List.of());
			case VELOCITY -> results.getOrDefault("v", List.of());
			case ACCELERATION -> results.getOrDefault("a", List.of());
		};
	}

	public double valueAt(String key, double time) {
		List<Double> values = results.get(key);

		if (values == null || values.isEmpty() || times.isEmpty() || !Double.isFinite(time)) {
			return Double.NaN;
		}

		int size = Math.min(values.size(), times.size());
		if (size == 0) {
			return Double.NaN;
		}

		if (time <= times.get(0)) {
			return values.get(0);
		}

		if (time >= times.get(size - 1)) {
			return values.get(size - 1);
		}

		int lo = 0;
		int hi = size - 1;

		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			if (times.get(mid) < time) {
				lo = mid + 1;
			} else {
				hi = mid - 1;
			}
		}

		int right = lo;
		int left = right - 1;
		double t0 = times.get(left);
		double t1 = times.get(right);
		double v0 = values.get(left);
		double v1 = values.get(right);

		if (t1 == t0) {
			return v1;
		}

		double ratio = (time - t0) / (t1 - t0);
		return v0 + ratio * (v1 - v0);
	}
}

// A node represents an object affected by the physics of the world it's played in.
class Node implements Callable<Result> {
	enum FunctionType {
		POSITION,
		VELOCITY,
		ACCELERATION
	}

	private static final Color[] PALETTE = {
		Color.BLACK,
		Color.BLUE,
		Color.CYAN,
		Color.DARK_GRAY,
		Color.GRAY,
		Color.GREEN,
		Color.LIGHT_GRAY,
		Color.MAGENTA,
		Color.ORANGE,
		Color.PINK,
		Color.RED,
		Color.WHITE,
		Color.YELLOW
	};

	private int id;
	private double mass;
	private double pos;
	private double vel;
	private double acc;
	private Color color;

	private MathFunction equation; // Valid equation of specific FunctionType
	private FunctionType type;

	private World world; // Set externally by World object
	
	public Node(int id, double mass) {
		this.id = id;
		this.mass = mass;
		this.pos = 0;
		this.vel = 0;
		this.acc = 0;
		this.color = PALETTE[id % PALETTE.length];
		this.world = null;
	}

	// Called by a ThreadExecutorPool thread to calculate the position, velocity, and acceleration for this node
	public Result calculate() {
		Result result = new Result(this, new HashMap<>(), new ArrayList<>());
		
		if (world == null || equation == null || type == null) {
			return result;
		}

		List<Double> times = buildTimes();
		result.times().addAll(times);

		List<Double> positions = new ArrayList<>();
		List<Double> velocities = new ArrayList<>();
		List<Double> accelerations = new ArrayList<>();

		switch (type) {
			case POSITION -> {
				// Calculate all position points
				for (double t : times) {
					pos = equation.calculate(getVariables(t));
					positions.add(pos);
				}

				// Calculate dy/dx btwn pos points
				for (double x : positions) {
					velocities.add(equation.derivative(x));
				}

				// Calculate dy/dx btwn vel points
				for (double v : velocities) {
					accelerations.add(equation.derivative(v));
				}
			}
			case VELOCITY -> {
				// Calculate all vel points
				for (double t : times) {
					vel = equation.calculate(getVariables(t));
					velocities.add(vel);
				}

				// Integrate to find pos points
				for (double t : times) {
					positions.add(equation.integrate(world.getStart(), t));
				}

				// Calculate dy/dx btwn vel points
				for (double v : velocities) {
					accelerations.add(equation.derivative(v));
				}
			}
			case ACCELERATION -> {
				// Calculate all acc points
				for (double t : times) {
					acc = equation.calculate(getVariables(t));
					accelerations.add(acc);
				}

				// Integrate to find vel points
				for (double t : times) {
					velocities.add(equation.integrate(world.getStart(), t));
				}

				// Integrate to find pos points
				double x = 0;
				positions.add(x);
				for (int i = 1; i < times.size(); i++) {
					x += (times.get(i) - times.get(i - 1)) * (velocities.get(i - 1) + velocities.get(i)) / 2.0;
					positions.add(x);
				}
			}
		}

		// Add to result
		var map = result.results();
		map.put("x", positions);
		map.put("v", velocities);
		map.put("a", accelerations);

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

	public List<Double> buildTimes() {
		List<Double> times = new ArrayList<>();
		for (double t = world.getStart(); t < world.getEnd(); t += world.getTick()) {
			times.add(t);
		}

		if (times.isEmpty() || Double.compare(times.get(times.size() - 1), world.getEnd()) != 0) {
			times.add(world.getEnd());
		}

		return times;
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
		changed();
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

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		if (color != null) {
			this.color = color;
			changed();
		}
	}

	public void setEquation(MathFunction equation, FunctionType type) {
		this.equation = equation;
		this.type = type;
		changed();
	} 

	public void setEquation(String eq, String respective, FunctionType type) {
		this.equation = new MathFunction(eq, respective);
		this.type = type;
		changed();
	}

	public MathFunction getEquation() {
		return equation;
	}

	public FunctionType getType() {
		return type;
	}

	private void changed() {
		if (world != null) {
			world.touch();
		}
	}

	@Override
	public Result call() throws Exception {
		return calculate();
	}
}
