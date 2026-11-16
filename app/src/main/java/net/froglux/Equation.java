package net.froglux;

/*
 * This file contains all classes related to tokenizing, parsing, and calculating equations.
 * 
 * Use in conjunction with Simulator.java and World.java
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;

record Point(double x, double y) {}

public class Equation {
	public enum DifferentiationType {
		LOWER,
		UPPER,
		MIDDLE
	}

	public enum IntegrationType {
		LEFT,
		MIDPOINT,
		RIGHT,
		TRAPEZOIDAL
	}

	// Delta x for differentiation
	public static double DIFFERENTIATION_DELTA = 1e-5;

	// Distance between each riemann sum (dx)
	public static double INTEGRATION_DELTA = 1e-2;
	public static double INTEGRATION_SEGMENTS = 1 / INTEGRATION_DELTA;

	// Implementation derived from SimpleCalc
	public static List<String> tokenize(String eq) {
		List<String> tokens = new ArrayList<>();

		int index = 0;

		while (index < eq.length()) {
			char curr = eq.charAt(index);
			index++;

			String token = "";

			if (curr == '+' || curr == '-') {
				token += curr;
				tokens.add(token);
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
		}

		return tokens;
	}

	// Evaluates the expression or, for an equation, assigns value to a variable. All variables needed should be provided, otherwise an ArithmeticException will occur.
	public static double calculate(String[] tokens, MathVariable... args) throws ArithmeticException {
		Stack<Double> valueStack = new Stack<>();
		Stack<String> operatorStack = new Stack<>();

		boolean expectOperand = true;

		// Ignore left side
		int start = 0;
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("=")) {
				start = i + 1;
			}
		}
		
		for (int i = start; i < tokens.length; i++) {
			String token = tokens[i];

			if (isNumber(token)) {
				valueStack.push(Double.parseDouble(token));
				expectOperand = false;
			} else if (!isSyntax(token)) { // Catch variables
				boolean substitute = false;

				// Iterate until variable that matches token is found
				for (MathVariable variable : args) {
					if (variable.getName().equals(token)) {
						valueStack.push(variable.get());
						substitute = true;
						break;
					}
				}

				// Use 0 for undefined variables
				if (!substitute) {
					throw new ArithmeticException("calculate: Undefined variable: " + token);
				}

				expectOperand = false;
			} else if (token.equals("(")) {
				operatorStack.push(token);
				expectOperand = true;
			} else if (token.equals(")")) { // Solve value in parenthesis
				// Calculate until "(" is reached
				while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
					compute(valueStack, operatorStack);
				}
					
				if (operatorStack.isEmpty()) {
					throw new ArithmeticException("calculate: Mismatched ')'");
				}

				// Pop ")"
				operatorStack.pop();
				expectOperand = false;
			} else {
				// A +/- is a unary operator
				if (expectOperand) {
					if (token.equals("+")) {
						continue;
					}

					if (token.equals("-")) {
						operatorStack.push("u-");
						continue;
					}

					throw new ArithmeticException("calculate: Unexpected operator: " + token);
				}

				// Parse until lower precedent or no operators left
				while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(") && !hasPrecedence(operatorStack.peek(), token)) {
					compute(valueStack, operatorStack);
				}

				operatorStack.push(token);
				expectOperand = true;
			}
		}
		
		// Calculate to collapse stack
		while (!operatorStack.isEmpty()) {
			if (operatorStack.peek().equals("(")) {
				throw new ArithmeticException("calculate: Mismatched '('");
			}

			compute(valueStack, operatorStack);
		}
		
		// Return the last value (result) in the stack
		if (valueStack.isEmpty()) {
			return 0;
		}

		if (valueStack.size() != 1) {
			throw new ArithmeticException("calculate: invalid expression");
		}

		return valueStack.pop();
	}

	private static void compute(Stack<Double> valueStack, Stack<String> operatorStack) {
		String operator = operatorStack.pop();
		if (operator.equals("u-")) {
			if (valueStack.isEmpty()) {
				throw new ArithmeticException("computer: missing operand for unary '-'");
			}

			valueStack.push(-valueStack.pop());
			return;
		}

		if (valueStack.size() < 2) {
			throw new ArithmeticException("compute: missing operand for '" + operator + "'");
		}

		double valRight = valueStack.pop();
		double valLeft = valueStack.pop();
						
		valueStack.push(compute(valLeft, operator, valRight));
	}

	private static double compute(double left, String operator, double right) throws ArithmeticException {
		double rtnVal = 0;
		
		switch (operator) {
			case "+":
				rtnVal = left + right;
				break;
			case "-":
				rtnVal = left - right;
				break;
			case "*":
				rtnVal = left * right;
				break;
			case "/":
				rtnVal = left / right;
				break;
			case "%":
				rtnVal = left % right;
				break;
			case "^":
				rtnVal = Math.pow(left, right);
				break;
			default:
				throw new ArithmeticException("compute: invalid operator: " + operator);
		}
		
		return rtnVal;
	}

	private static boolean hasPrecedence(String op1, String op2) {
		if (op2.equals("(") || op2.equals(")")) {
			return false;
		}

		if (op1.equals("^") && op2.equals("^")) {
			return true;
		}

		return getOperatorWeight(op1) < getOperatorWeight(op2);
	}

	private static int getOperatorWeight(String op) {
		return switch (op) {
			case "+", "-" -> 1;
			case "*", "/", "%" -> 2;
			case "u-" -> 3;
			case "^" -> 4;
			default -> 0;
		};
	}

	// Returns the differentiation by substituting into the h -> 0 method; respective = variable to take derivative respective to
	public static double differentiate(String eq, double val, String respective, MathVariable... args) {
		return differentiate(eq, respective, args).applyAsDouble(val);
	} 

	// Returns the differentiation using the h -> 0 method; respective = variable to take derivative respective to
	public static DoubleUnaryOperator differentiate(String eq, String respective, MathVariable... args) {
		return val -> {
			String[] tokens = Equation.tokenize(eq).toArray(String[]::new);
			Map<String, MathVariable> argsMap = getArgsMap(args);

			argsMap.put(respective, new MathVariable(respective, val + DIFFERENTIATION_DELTA));
			double upper = Equation.calculate(tokens, argsMap.values().toArray(MathVariable[]::new));

			argsMap.put(respective, new MathVariable(respective, val - DIFFERENTIATION_DELTA));
			double lower = Equation.calculate(tokens, argsMap.values().toArray(MathVariable[]::new));

			return (upper - lower) / (2 * DIFFERENTIATION_DELTA);
		};
	}

	// Evaluates a definite integral using the given Riemann Sum type
	public static double rSum(String eq, double lower, double upper, int segs, String respective, IntegrationType type, MathVariable... args) {
		if (segs < 1) {
			throw new ArithmeticException("rSum: segs < 1");
		}

		Map<String, MathVariable> argsMap = getArgsMap(args);

		// Calculates the values at each segments' endpoints (+ 1 since there is an extra endpoint to account for)
		double delta = (upper - lower) / segs;
		double refX = lower;

		String[] tokenized = Equation.tokenize(eq).toArray(String[]::new);

		// Calculates the y-value for each segment
		double[] valSegs = new double[segs + 1];
		for (int i = 0; i < valSegs.length; i++) {
			argsMap.put(respective, new MathVariable(respective, refX));
			valSegs[i] = Equation.calculate(tokenized, argsMap.entrySet().stream().map(x -> x.getValue()).toArray(MathVariable[]::new));
			refX += delta;
		}

		double sum = 0;

		switch (type) {
			case LEFT:
				for (int i = 0; i < valSegs.length - 1; i++) {
					sum += delta * valSegs[i];
				}
				break;
			case MIDPOINT:
				for (int i = 1; i < valSegs.length; i++) {
					double mid = lower + (i + 0.5) * delta;
					argsMap.put(respective, new MathVariable(respective, mid));
					sum += delta * Equation.calculate(tokenized, argsMap.values().toArray(MathVariable[]::new));
				}
				break;
			case RIGHT:
				for (int i = 1; i < valSegs.length; i++) {
					sum += delta * valSegs[i];
				}
				break;
			case TRAPEZOIDAL:
				for (int i = 1; i < valSegs.length; i++) {
					sum += delta / 2 * (valSegs[i - 1] + valSegs[i]);
				}
				break;
		}

		return sum;
	}

	// Returns the result of the definite integral with dx = INTEGRATION_DELTA. Currently uses midpoint riemann sums, but should be analyzed to use the most efficient method
	public static double rSum(String eq, double lower, double upper, String respective, MathVariable... args) {
		if (lower == upper) {
			return 0;
		}

		int segs = (int) Math.round(Math.abs(upper - lower) / INTEGRATION_DELTA);
		return rSum(eq, lower, upper, Math.max(1, segs), respective, IntegrationType.MIDPOINT, args);
	}

	public static double integrate(String eq, String respective, double lower, double upper, MathVariable... args) {
		return integrate(eq, respective, args).applyAsDouble(lower, upper);
	}

	// Returns a DoubleBinaryOperator that accepts (lower, upper) bounds - uses Simpson's Rule; respective - string of variable in equation to calculate relative to (use t for time and x for general)
	public static DoubleBinaryOperator integrate(String eq, String respective, MathVariable... args) {
		INTEGRATION_SEGMENTS = 1 / INTEGRATION_DELTA;
		return (lower, upper) -> {
			String[] tokens = Equation.tokenize(eq).toArray(String[]::new);
			Map<String, MathVariable> argsMap = getArgsMap(args);

			int segs = Math.max(2, (int) Math.round(INTEGRATION_SEGMENTS));
			if (segs % 2 != 0) {
				segs++;
			}
			double h = (upper - lower) / segs;
			argsMap.put(respective, new MathVariable(respective, lower));
			double sum = Equation.calculate(tokens, argsMap.values().toArray(MathVariable[]::new));

			for (int i = 1; i < segs; i++) {
				argsMap.put(respective, new MathVariable(respective, lower + i * h));

				double val = Equation.calculate(tokens, argsMap.values().toArray(MathVariable[]::new));
				sum += (i % 2 == 0 ? 2 : 4) * val;
			}

			argsMap.put(respective, new MathVariable(respective, upper));
			sum += Equation.calculate(tokens, argsMap.values().toArray(MathVariable[]::new));

			return sum * h / 3;
		};
	}

	// Simplified methods that accepts multiple point arguments
	public static double integratePoints(Point... points) {
		return integratePoints(List.of(points));
	}

	// Finds the "area under the graph" for the given points using the trapezoidal riemann sum
	public static double integratePoints(List<Point> points) {
		if (points == null || points.size() < 2) {
			return 0;
		}

		points = new ArrayList<>(points);
		points.sort((a, b) -> Double.compare(a.x(), b.x()));

		double sum = 0;
		for (int i = 1; i < points.size(); i++) {
			sum += (points.get(i).x() - points.get(i - 1).x()) / 2 * (points.get(i - 1).y() + points.get(i).y());
		}

		return sum;
	}

	public static boolean isNumber(String val) {
		if (val == null || val.length() == 0) {
			return false;
		}

		try {
			Double.parseDouble(val);
		} catch (NumberFormatException e) {
			return false;
		}

		return true;
	}

	// Returns true if the args provided has all the values given; false otherwise
	public static boolean hasArguments(MathVariable[] args, String... values) {
		List<String> names = Arrays.stream(args).map(x -> x.getName()).toList();
		
		for (String val : values) {
			if (!names.contains(val)) {
				return false;
			}
		}

		return true;
	}

	public static Map<String, MathVariable> getArgsMap(MathVariable... args) {
		if (args == null) {
			return new HashMap<>();
		}

		return Arrays.stream(args).collect(Collectors.toMap(x -> x.getName(), x -> x, (a, b) -> b));
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

// Represents a "pre-compiled" equation that skips tokenization, enforces stricter rules on function calls, and is reusable
class MathFunction {
	// Derived from user input
	private String equation;
	private String[] tokenized;
	private Map<String, MathVariable> args;
	private String[] required;
	private String respective;

	// Stored functions (none for integral since it needs to sum up areas)
	private DoubleUnaryOperator derivative;
	private DoubleBinaryOperator integral;

	// Creates a MathFunction with an equation and args (enforced by name)
	public MathFunction(String equation, String respective, MathVariable... args) {
		this.equation = equation;
		this.respective = respective;
		this.tokenized = Equation.tokenize(equation).toArray(String[]::new);
		this.required = extractRequired(tokenized);
		this.args = createArgs(args);
	}

	// Creates a MathFunction with an equation and some number of args (not enforced by name)
	public MathFunction(String equation, String respective) {
		this.equation = equation;
		this.respective = respective;
		this.tokenized = Equation.tokenize(equation).toArray(String[]::new);
		this.required = extractRequired(tokenized);
		this.args = createArgs();
	}

	// Searches string for variables, irrespective of values (e and pi are automatically injected into args)
	public String[] extractRequired(String[] tokens) {
		List<String> list = new ArrayList<>();
		int start = 0;
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("=")) {
				start = i + 1;
			}
		}

		for (int i = start; i < tokens.length; i++) {
			String token = tokens[i];

			if (token.matches("[a-zA-Z]+") && !token.equals("pi") && !token.equals("e") && !list.contains(token)) {
				list.add(token);
			}
		}

		return list.toArray(String[]::new);
	}

	// Creates, assigns, and returns a map of MathVariables that include e, pi, and required MathVariables with default values, and fills in values if provided
	public Map<String, MathVariable> createArgs(MathVariable... args) {
		this.args = new HashMap<>();
		this.args.put("pi", new MathVariable("pi", Math.PI));
		this.args.put("e", new MathVariable("e", Math.E));

		for (String name : required) {
			this.args.putIfAbsent(name, new MathVariable(name, 0));
		}

		if (args != null) {
			Arrays.stream(args).forEach(x -> this.args.put(x.getName(), x));
		}

		refreshFunctions();
		return this.args;
	}

	private void refreshFunctions() {
		MathVariable[] currArgs = this.args.values().toArray(MathVariable[]::new);
		this.derivative = Equation.differentiate(equation, respective, currArgs);
		this.integral = Equation.integrate(equation, respective, currArgs);
	}

	// Evaluates the function with the given positional arguments
	// NOTE: All arguments for MathFunction are positional, it does not depend on the name of each variable
	// NOTE: This function assumes that args is valid
	private double calculate() throws ArithmeticException {
		return Equation.calculate(tokenized, args.values().toArray(MathVariable[]::new));
	}

	// Uses auto assigned variables, fills by order to order of map
	public double calculate(double... args) throws ArithmeticException {
		autoAssign(args);
		return calculate();
	}

	// Tries to best assign given variables to map, otherwise defaults to autoAssign()
	public double calculate(MathVariable... args) throws ArithmeticException {
		createArgs(args);
		return calculate();
	}

	// Finds the derivative of the function at the point using 
	public double derivative(double val) {
		return derivative.applyAsDouble(val);
	}

	// Finds the integration of the function using Simpson's Rule
	public double integrate(double lower, double upper) {
		return integral.applyAsDouble(lower, upper);
	}

	// Attempt to automatically assign values to required arguments by position
	public Map<String, MathVariable> autoAssign(double... args) {
		if (args.length < required.length) {
			throw new ArithmeticException(String.format("Expected %d arguments, got %d", required.length, args.length));
		}

		for (int i = 0; i < required.length; i++) {
			this.args.put(required[i], new MathVariable(required[i], args[i]));
		}

		refreshFunctions();
		return this.args;
	}

	public String getEquation() {
		return equation;
	}

	// Convenience method to skip object creation; relies on auto assigning values
	public static double eval(String eq, double... args) throws ArithmeticException {
		MathFunction func = new MathFunction(eq, "x");
		return func.calculate(args);
	}
}