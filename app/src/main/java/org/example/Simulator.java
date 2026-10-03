package org.example;

/*
 * `bps` is a simple physics simulator built on Java and Swing. It relies on a global timer (ticks)
 * that acts as dt. It can simulate both basic (algebraic) and advanced (calculus) physics. Position,
 * velocity, and acceleration can be in terms of t, x, v, and t (but not itself). 
 */

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import java.util.Stack;

public class Simulator {
	private JFrame frame;
	private JPanel panel;

	public Simulator() {
		this.frame = new JFrame();
		frame.setSize(800, 450);
		frame.setTitle("bps");
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		this.panel = new JPanel();

		frame.add(panel);
	}

	public void run() {
		
	}
}

class World {
	private int id;

	// World "constants" - can only be changed before a new simulation runs
	private double gravity;

	// World variables
	private double time;
	
	public World(int id) {
		this.id = id;
	}
}

// A node represents an object affected by the physics of the world it's played in.
class Node {
	private int id;
	private double mass;

	private String posEq, velEq, accEq; // Valid equations as defined by Equations class

	private double pos, vel, acc;
	
	public Node(int id, double mass) {
		this.id = id;
		this.mass = mass;
	}

	// Called for every tick of World timer, for best performance, consider using a ThreadExecutorPool to stack operations for each time increment,
	// and then place into list and sort by time.
	public void calculate() {

	}
}

class Equation {
	public enum IntegrationType {
		LEFT,
		MIDPOINT,
		RIGHT,
		TRAPEZOIDAL
	}

	// Number of segments for integration (larger = more accurate, but slower)
	public static final int INTEGRATION_SEGS = 1000;

	// Implementation taken from SimpleCalc
	public static double solve(String eq) throws ArithmeticException {
		Stack<Double> operands = new Stack<>();
		Stack<String> operator = new Stack<>();

		return 0;
	}

	public static double rSum(String eq, double lower, double upper, int segs, IntegrationType type) {
		double[] xSegs = new double[segs + 1];
		for (int i = 0; i < segs; i++) {
			
		}

		double[] valSegs = new double[xSegs.length];
		for (int i = 0; i < valSegs.length; i++) {
			
		}

		return 0;
	}

	// Creates 
	public static double integrate(String eq, double lower, double upper) {
		return 0;
	}
}