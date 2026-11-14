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
import java.util.stream.Collectors;

record Point(double x, double y) {}

// TODO: Fix evaluation errors for equation
public class Equation {
	public enum IntegrationType {
		LEFT,
		MIDPOINT,
		RIGHT,
		TRAPEZOIDAL
	}

	// The name property of MathVariable to interpret as equivalent to the x-axis
	public static String xName = "t";

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
				if (lastToken.length() == 0 || (isSyntax(lastToken) && !lastToken.equals(")"))) {
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
					
					tokens.add(token);
				} else { // Add as normal operator
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

			// Only add non-whitespace as valid prev token
			if (token.strip().length() > 0) {
				lastToken = token;
			}
		}

		return tokens;
	}

	// Evaluates the expression or, for an equation, assigns value to a variable. All variables needed should be provided, otherwise an ArithmeticException will occur.
	public static double calculate(String[] tokens, MathVariable... args) throws ArithmeticException {
		Stack<Double> valueStack = new Stack<>();
		Stack<String> operatorStack = new Stack<>();
		
		for (String token : tokens) {
			if (isNumber(token)) {
				valueStack.push(Double.parseDouble(token));
			} else if (!isSyntax(token)) { // Catch variables
				boolean substitute = false;

				// Iterate until variable that matches token is found
				for (MathVariable variable : args) {
					// No need to check repeating names
					if (variable.getName().equals(token)) {
						valueStack.push(variable.get());
						substitute = true;
					}
				}

				// Use 0 for undefined variables
				if (!substitute) {
					valueStack.push(0.0);
				}
			} else if (token.equals("(")) {
				operatorStack.push(token);
			} else if (token.equals(")")) { // Solve value in parenthesis
				// Calculate until "(" is reached
				while (!operatorStack.peek().equals("(")) {
					compute(valueStack, operatorStack);
				}
					
				// Pop "(" since it's not needed
				if (!operatorStack.isEmpty()) {
					operatorStack.pop();
				}
			} else {
				// Until precedent is lower or no more operations left; Solve before adding low precedence operator 
				while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(") && !hasPrecedence(operatorStack.peek(), token)) {
					compute(valueStack, operatorStack);
				}
					
				// Push the current operator onto the stack
				operatorStack.push(token);
			}
		}
		
		// Calculate to collapse stack
		while (!operatorStack.isEmpty()) {
			compute(valueStack, operatorStack);
		}
		
		// Return the last value (result) in the stack
		return valueStack.isEmpty() ? 0 : valueStack.pop();
	}

	private static void compute(Stack<Double> valueStack, Stack<String> operatorStack) {
		double valRight = valueStack.pop();
		double valLeft = valueStack.pop();
		String operator = operatorStack.pop();
						
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

		int weight1 = 0;
		if (op1.equals("+") || op1.equals("-")) {
			weight1 = 1;
		}
		if (op1.equals("*") || op1.equals("/") || op1.equals("%")) {
			weight1 = 2;
		}
		if (op1.equals("^")) {
			weight1 = 3;
		}

		int weight2 = 0;
		if (op2.equals("+") || op2.equals("-")) {
			weight2 = 1;
		}
		if (op2.equals("*") || op2.equals("/") || op2.equals("%")) {
			weight2 = 2;
		}
		if (op2.equals("^")) {
			weight2 = 3;
		}

		return weight1 < weight2;
	}

	public static double rSum(String eq, double lower, double upper, int segs, IntegrationType type, MathVariable... args) {
		Map<String, MathVariable> argsMap = getArgsMap(args);

		// Calculates the values at each segments' endpoints (+ 1 since there is an extra endpoint to account for)
		double delta = (upper - lower) / segs;
		double refX = lower;

		String[] tokenized = Equation.tokenize(eq).toArray(String[]::new);

		// TODO: Eventually make this more efficient; consider adding a new calculate() method that accepts a map of [name], [MathVariable]
		// Calculates the y-value for each segment
		double[] valSegs = new double[segs + 1];
		for (int i = 0; i < valSegs.length; i++) {
			argsMap.put("x", new MathVariable("x", refX));
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
					double avg = (valSegs[i - 1] + valSegs[i]) / 2;
					sum += delta * avg;
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

	// Returns the result of the definite integral with dx = INTEGRATION_DELTA. Currently uses trapezoidal riemann sums, but should be analyzed to use the most efficient method
	public static double integrate(String eq, double lower, double upper, MathVariable... args) {
		return rSum(eq, lower, upper, (int) Math.round((upper - lower) / INTEGRATION_DELTA), IntegrationType.MIDPOINT, args);
	}

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
		return Arrays.stream(args).collect(Collectors.toMap(x -> x.getName(), x -> x));
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
	private String[] tokenized;
	private Map<String, MathVariable> args;
	private String[] required;

	// Creates a MathFunction with an equation and args (enforced by name)
	public MathFunction(String equation, MathVariable... args) {
		this.tokenized = Equation.tokenize(equation).toArray(String[]::new);
		this.required = extractRequired(tokenized);
		this.args = createArgs(args);
	}

	// Creates a MathFunction with an equation and some number of args (not enforced by name)
	public MathFunction(String equation) {
		this.tokenized = Equation.tokenize(equation).toArray(String[]::new);
		this.required = extractRequired(tokenized);
		this.args = createArgs();
	}

	// Searches string for variables, irrespective of values (e and pi are automatically injected into args)
	public String[] extractRequired(String[] tokens) {
		List<String> list = new ArrayList<>();

		for (String token : tokens) {
			if (token.matches("[a-zA-Z]+")) {
				list.add(token);
			}
		}

		return list.stream().toArray(String[]::new);
	}

	// Creates, assigns, and returns a map of MathVariables that include e, pi, and required MathVariables with default values, and fills in values if provided
	public Map<String, MathVariable> createArgs(MathVariable... args) {
		this.args = new HashMap<>();
		this.args.put("pi", new MathVariable("pi", Math.PI));
		this.args.put("e", new MathVariable("e", Math.E));
		Arrays.stream(required).forEach(x -> this.args.put(x, new MathVariable(x, 0)));

		if (args == null || args.length == 0) {
			return this.args;
		}

		Arrays.stream(args).forEach(x -> this.args.put(x.getName(), x));

		return this.args;
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

		return Equation.calculate(Arrays.stream(tokenized, index + 1, tokenized.length).toArray(String[]::new), args.values().toArray(MathVariable[]::new));
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

	// Attempt to automatically assign values to required arguments by position
	public Map<String, MathVariable> autoAssign(double... args) {
		if (args.length < required.length) {
			throw new ArithmeticException(String.format("Expected %d arguments, got %d", required.length, args.length));
		} else {
			var items = this.args.keySet().toArray(String[]::new);
			
			for (int i = 0; i < args.length && i < required.length && i < items.length; i++) {
				this.args.put(items[i], new MathVariable(items[i], args[i]));
			}
		}

		return this.args;
	}

	// Convenience method to skip object creation; relies on auto assigning values
	public static double eval(String eq, double... args) throws ArithmeticException {
		MathFunction func = new MathFunction(eq);
		return func.calculate(args);
	}
}