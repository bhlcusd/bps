package net.froglux;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MathFunctionTest {
	private static final double TOLERANCE = 1e-4;

	@Test
	void calculatesWithPositionalArguments() {
		MathFunction function = new MathFunction("x^2 + y", "x");

		assertEquals(7, function.calculate(2, 3), TOLERANCE);
		assertEquals(21, function.calculate(4, 5), TOLERANCE);
	}

	@Test
	void assignsPositionalArgumentsInEquationOrder() {
		MathFunction function = new MathFunction("x - y", "x");

		assertEquals(2, function.calculate(5, 3), TOLERANCE);
	}

	@Test
	void calculatesWithNamedVariables() {
		MathFunction function = new MathFunction("x^2 + y", "x");

		assertEquals(
			13,
			function.calculate(new MathVariable("x", 3), new MathVariable("y", 4)),
			TOLERANCE
		);
	}

	@Test
	void usesBuiltInConstants() {
		MathFunction function = new MathFunction("e*x + pi", "x");

		assertEquals(Math.E + Math.PI, function.calculate(1), TOLERANCE);
		assertEquals(Math.E, function.derivative(2), TOLERANCE);
		assertEquals(Math.E / 2 + Math.PI, function.integrate(0, 1), TOLERANCE);
	}

	@Test
	void extractsRequiredVariables() {
		MathFunction function = new MathFunction("unused = x + y + pi + e", "x");
		String[] tokens = Equation.tokenize("ignored = x + y + pi + e").toArray(String[]::new);

		assertArrayEquals(new String[] {"x", "y"}, function.extractRequired(tokens));
	}

	@Test
	void includesConstantsAndDefaultsInCreatedArgs() {
		Map<String, MathVariable> args = new MathFunction("x + y", "x").createArgs();

		assertEquals(0, args.get("x").get(), TOLERANCE);
		assertEquals(0, args.get("y").get(), TOLERANCE);
		assertEquals(Math.E, args.get("e").get(), TOLERANCE);
		assertEquals(Math.PI, args.get("pi").get(), TOLERANCE);
	}

	@Test
	void autoAssignsArgumentsByPosition() {
		MathFunction function = new MathFunction("x^2 + y", "x");
		Map<String, MathVariable> assigned = function.autoAssign(2, 3);

		assertEquals(2, assigned.get("x").get(), TOLERANCE);
		assertEquals(3, assigned.get("y").get(), TOLERANCE);
		assertEquals(4, function.derivative(2), TOLERANCE);
	}

	@Test
	void reusesDerivativeAndIntegralAfterAssignments() {
		MathFunction function = new MathFunction("x^2", "x");

		assertEquals(6, function.derivative(3), TOLERANCE);
		assertEquals(8.0 / 3, function.integrate(0, 2), TOLERANCE);

		function.calculate(4);

		assertEquals(8, function.derivative(4), TOLERANCE);
		assertEquals(64.0 / 3, function.integrate(0, 4), TOLERANCE);
	}

	@Test
	void calculatesSecondDerivative() {
		MathFunction function = new MathFunction("(x - 3)^2", "x");

		assertEquals(2, function.secondDerivative(4), TOLERANCE);
		assertEquals(2, function.secondDerivative(2), TOLERANCE);
	}

	@Test
	void supportsEquationAssignments() {
		MathFunction function = new MathFunction("y = x^2", "x");

		assertEquals(9, function.calculate(3), TOLERANCE);
	}

	@Test
	void handlesConstantExpressions() {
		MathFunction function = new MathFunction("pi + 2", "x");

		assertEquals(Math.PI + 2, function.calculate(new double[0]), TOLERANCE);
		assertEquals(0, function.derivative(5), TOLERANCE);
		assertEquals(Math.PI + 2, function.integrate(0, 1), TOLERANCE);
	}

	@Test
	void throwsWhenRequiredArgumentsAreMissing() {
		MathFunction function = new MathFunction("x + y", "x");

		ArithmeticException calculateException = assertThrows(
			ArithmeticException.class,
			() -> function.calculate(1)
		);
		assertTrue(calculateException.getMessage().contains("Expected 2"));

		ArithmeticException autoAssignException = assertThrows(
			ArithmeticException.class,
			() -> function.autoAssign(1)
		);
		assertTrue(autoAssignException.getMessage().contains("Expected 2"));
	}

	@Test
	void evalHelperEvaluatesExpressions() {
		assertEquals(10, MathFunction.eval("x^2 + 1", 3), TOLERANCE);
		assertEquals(7, MathFunction.eval("x^2 + y", 2, 3), TOLERANCE);
	}

	@Test
	void exposesEquation() {
		assertEquals("x^2 + 1", new MathFunction("x^2 + 1", "x").getEquation());
	}
}
