package org.example;

/*
 * `bps` is a simple physics simulator built on Java and Swing. It relies on a global timer (ticks)
 * that acts as dt. It can simulate both basic (algebraic) and advanced (calculus) physics. Position,
 * velocity, and acceleration can be in terms of t, x, v, and t (but not itself). 
 */

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import java.awt.Graphics;
import java.awt.Graphics2D;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

public class Simulator {
	private List<World> worlds;
	private World world;

	private JFrame frame;
	private JPanel panel;

	public Simulator() {
		this.worlds = new ArrayList<>();
		this.world = null;

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
		System.out.println(Equation.tokenize("x = 23 / (log(26.23^23 * 23.1283 / .2388))"));
	}
}

// Lists current world and child nodes; allows user to add, select, copy, paste, and delete nodes
class Explorer extends JPanel {

}

// Lists the properties of a node, provides settings similar to Godot, with a label and an input value (number, string, boolean (checkbox), or dropdown)
class Inspector extends JPanel {
	public Inspector() {

	}
}

class Graph extends JPanel {
	// Grid tick distance
	private int xInc, yInc;

	// Flags
	private boolean grid;

	public Graph() {

	}

	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;

		
	}
}

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
	public List<Node> nodes;
	
	public World(int id, int tick, double start, double end, double gravity) {
		this.id = id;
		this.tick = tick;
		this.start = start;
		this.end = end;
		this.gravity = gravity;
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
	// and then place into a list and sort by time. Or, to reduce memory/processing overhead, allocate arrays and access from there.
	public void calculate() {

	}
}

// Represents a variable that has a name and stores a value (equivalent to a Java double)
class MathVariable {
	private double val;
	
	// "Locked" after creation
	private String name;
	private double defaultVal;
	private boolean constant;

	public MathVariable(String name, double val, double defaultVal, boolean constant) {
		this.name = name;
		this.val = val;
		this.defaultVal = defaultVal;
		this.constant = constant;
	}

	public MathVariable(String name, double val, boolean constant) {
		this(name, val, val, constant);
	}

	public MathVariable(String name, double val) {
		this(name, val, val, false);
	}

	public void set(double val) {
		if (!constant) {
			this.val = val;
		}
	}

	public double get() {
		return val;
	}

	public void clear() {
		this.val = defaultVal;
	}

	public String getName() {
		return name;
	}

	public boolean isConstant() {
		return constant;
	}
}

// Represents a "pre-compiled" equation that skips tokenization, enforces stricter rules on function calls and is reusable
class MathFunction {
	private String equation;
	private String[] tokenized;
	private MathVariable[] args;

	public MathFunction(String equation) {
		this.equation = equation;
		this.tokenized = Equation.tokenize(equation).toArray(String[]::new);
	}

	// Attempt to automatically assign values to required arguments by position
	public MathVariable[] autoAssign(double... args) {
		if (args.length < this.args.length) {
			throw new ArithmeticException(String.format("Expected %d arguments, got %d", this.args.length, args.length));
		} else {
			for (int i = 0; i < this.args.length; i++) {
				this.args[i].set(args[i]);
			}
		}

		return this.args;
	}

	public double calculate(double... args) throws ArithmeticException {
		return calculate(autoAssign(args));
	}

	public double calculate(MathVariable... args) throws ArithmeticException {
		if (args.length < this.args.length) {
			throw new ArithmeticException(String.format("Expected %d arguments, got %d", this.args.length, args.length));
		} else {
			// Try to match by name; to make it easier for prototyping, using linear search (to fix use Arrays.binarySearch())

			List<MathVariable> argsRem = Arrays.stream(args).toList();
			int count = 0;

			for (int i = 0; i < this.args.length; i++) {
				for (int j = 0; j < argsRem.size(); j++) {
					if (argsRem.get(j).getName().equals(this.args[i].getName())) {
						this.args[i].set(argsRem.remove(j).get());
						count++;
						break;
					}
				}
			}

			// Auto assign the rest
			for (int i = count; i < this.args.length; i++) {
				this.args[i].set(argsRem.get(i).get());
			}
		}

		return calculate();
	}

	// Evaluates the function with the given positional arguments
	// NOTE: All arguments for MathFunction are positional, it does not depend on the name of each variable
	// NOTE: This function assumes that args is valid
	private double calculate() throws ArithmeticException {
		// Ignore everything before the "=" sign
		int index = 0;

		while (!tokenized[index].equals("=")) {
			index++;
		}

		return Equation.calculate(Arrays.stream(tokenized, index + 1, tokenized.length).toArray(String[]::new), args);
	}

	// Convenience method to skip object creation
	public static double eval(String eq, double... args) throws ArithmeticException {
		MathFunction func = new MathFunction(eq);
		return func.calculate(func.autoAssign(args));
	}
}

class Equation {
	public enum IntegrationType {
		LEFT,
		MIDPOINT,
		RIGHT,
		TRAPEZOIDAL
	}

	// Distance between each riemann sum (dx)
	public static double INTEGRATION_DELTA = 0.01;

	// Implementation derived from SimpleCalc
	public static List<String> tokenize(String eq) {
		List<String> tokens = new ArrayList<>();

		String lastToken = "";
		int index = 0;

		while (index < eq.length()) {
			char curr = eq.charAt(index);
			index++;

			String token = "";

			if (curr == '+' || curr == '-') {
				// Handle unary operator
				if (lastToken.length() == 0 || isSyntax(lastToken)) {
					token += curr;

					if (Character.isDigit(eq.charAt(index))) {
						while (index < eq.length() && (Character.isDigit(eq.charAt(index)) || eq.charAt(index) == '.')) {
							token += eq.charAt(index);
							index++;
						}
					} else {
						while (index < eq.length() && Character.isLetter(eq.charAt(index))) {
							token += eq.charAt(index);
							index++;
						}
					}
				} else { // Add as operator
					token += curr;
					tokens.add(token);
				}
			} else if (Character.isDigit(curr) || curr == '.') {
				token += curr;

				while (index < eq.length() && (Character.isDigit(eq.charAt(index)) || eq.charAt(index) == '.')) {
					token += eq.charAt(index);
					index++;
				}

				tokens.add(token);
			} else if (Character.isLetter(curr)) {
				token += curr;

				while (index < eq.length() && Character.isLetter(eq.charAt(index))) {
					token += eq.charAt(index);
					index++;
				}

				tokens.add(token);
			} else if (isSyntax(curr)) {
				token += curr;
				tokens.add(token);
			}

			lastToken = token;
		}

		return tokens;
	}

	// Evaluates the expression or, for an equation, assigns value to a variable. All variables needed should be provided, otherwise an ArithmeticException will occur.
	public static double calculate(String[] tokens, MathVariable... args) throws ArithmeticException {
		
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

	// Returns the result of the definite integral with dx = INTEGRATION_DELTA. Currently uses trapezoidal riemann sums, but should be analyzed to use the most efficient method
	public static double integrate(String eq, double lower, double upper) {
		return rSum(eq, lower, upper, (int) Math.round((upper - lower) / INTEGRATION_DELTA), IntegrationType.TRAPEZOIDAL);
	}

	// Checks whether the current string is an operator. An operator is defined as being able to transform two numeric or numeric-like values and returns a result.
	public static boolean isOperator(String val) {
		return val.equals("+") || val.equals("-") ||
			val.equals("*") || val.equals("/") ||
			val.equals("^") || val.equals("%"); 
	}

	public static boolean isOperator(char val) {
		return isOperator("" + val);
	}

	// Checks whether the current string is defined under mathematical syntax. Syntax includes operators and any other additional characters needed to perform mathematical operations.
	public static boolean isSyntax(String val) {
		return isOperator(val) || val.equals("=") || 
			val.equals("(") || val.equals(")");
	}

	public static boolean isSyntax(char val) {
		return isSyntax("" + val);
	}
}