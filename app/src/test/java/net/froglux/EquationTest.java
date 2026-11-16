package net.froglux;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class EquationTest {
	private static final double TOLERANCE = 1e-4;

	@AfterEach
	void reset() {
		// Do if needed, but not needed since static vars are not changed
	}

	@ParameterizedTest 
	@CsvSource({
		"'1+2*3', 7.0",
		"'(1+2)*3', 9.0",
		"'2+3*4^2', 50.0",
		"'10%4', 2.0",
		"'0.5*4', 2.0",
		"'-2^2', -4.0",
		"'(-2)^2', 4.0",
		"'2^3^2', 512.0",
		"'2^-3', 0.125",
		"'-3 + +2', -1.0"
	})
	void calculatesWithPrecedence(String eq, double expected) {
		assertEquals(expected, calculate(eq), TOLERANCE);
	}

	@Test
	void calculatesNestedParentheses() {
		assertEquals(12, calculate("2*(3+(4-1))"), TOLERANCE);
	}

	@Test
	void ignoresLeftSideOfAssignment() {
		assertEquals(7, calculate("y = x^2 + 3", new MathVariable("x", 2)), TOLERANCE);
	}

	@Test
	void returnsZeroForEmptyExpression() {
		assertEquals(0, calculate(""), TOLERANCE);
	}

	@Test 
	void substitutesVariables() {
		MathVariable x = new MathVariable("x", 3.5);
		MathVariable y = new MathVariable("y", 2.25);

		double result = calculate("x^2 + y", x, y);

		assertEquals(14.5, result, TOLERANCE);
	}

	@ParameterizedTest
	@ValueSource(strings = {"1 2", "1+", "*2", "(1", "1)"})
	void rejectsMalformedExpressions(String eq) {
		assertThrows(ArithmeticException.class, () -> calculate(eq));
	}

	@Test
	void rejectsUndefinedVariables() {
		ArithmeticException exception = assertThrows(
			ArithmeticException.class,
			() -> calculate("x + missing", new MathVariable("x", 1))
		);

		assertTrue(exception.getMessage().contains("missing"));
	}

	@Test 
	void differentiatesLinearFunction() {
		assertEquals(1.0, Equation.differentiate("x", 0, "x"), TOLERANCE);
	}

	@Test 
	void differentiatesQuadraticFunction() {
		assertEquals(4.0, Equation.differentiate("x^2", 2, "x"), TOLERANCE);
	}

	@Test 
	void differentiatesCubicFunction() {
		assertEquals(12.0, Equation.differentiate("x^3", 2, "x"), TOLERANCE);
	}

	@Test 
	void differentiatesSqrtFunction() {
		assertEquals(0.25, Equation.differentiate("x^0.5", 4, "x"), TOLERANCE);
	}

	@ParameterizedTest
	@CsvSource({
		"'3*x + 7', 5, 3.0",
		"'x^2', -2, -4.0",
		"'1/x', 2, -0.25",
		"'x^3 - 2*x', 1, 1.0",
		"'5', 3, 0.0"
	})
	void differentiatesAtPoints(String eq, double x, double expected) {
		assertEquals(expected, Equation.differentiate(eq, x, "x"), TOLERANCE);
	}

	@Test
	void differentiatesWithAdditionalVariables() {
		assertEquals(
			12,
			Equation.differentiate("x^2*y", 2, "x", new MathVariable("y", 3)),
			TOLERANCE
		);
		assertEquals(
			4,
			Equation.differentiate("x^2*y", 5, "y", new MathVariable("x", 2)),
			TOLERANCE
		);
	}

	@Test
	void returnsReusableDerivative() {
		DoubleUnaryOperator derivative = Equation.differentiate("x^2", "x");

		assertEquals(4, derivative.applyAsDouble(2), TOLERANCE);
		assertEquals(6, derivative.applyAsDouble(3), TOLERANCE);
	}

	@Test
	void differentiatesAssignment() {
		assertEquals(6, Equation.differentiate("y = x^2", 3, "x"), TOLERANCE);
	}

	@Test
	void differentiatesWithEulerConstant() {
		assertEquals(
			Math.E,
			Equation.differentiate("e*x", 2, "x", new MathVariable("e", Math.E)),
			TOLERANCE
		);
	}

	@Test
	void rejectsMissingDerivativeVariable() {
		assertThrows(ArithmeticException.class, () -> Equation.differentiate("x + z", 1, "x"));
	}

	@Test 
	void integratesLinearFunction() {
		assertEquals(6, Equation.integrate("3*x", "x", 0, 2), TOLERANCE);
	}

	@Test
	void integratesQuadraticFunction() {
		assertEquals(4, Equation.integrate("x^3", "x", 0, 2), TOLERANCE);
	}

	@ParameterizedTest
	@CsvSource({
		"'x', 0, 2, 2.0",
		"'x^2', 0, 3, 9.0",
		"'2*x+1', 1, 3, 10.0",
		"'5', -1, 4, 25.0",
		"'x', 2, 0, -2.0",
		"'x', -2, 0, -2.0",
		"'x', 1, 1, 0.0",
		"'1/x', 1, 2, 0.6931471805599453"
	})
	void integratesWithSimpsonsRule(String eq, double lower, double upper, double expected) {
		assertEquals(expected, Equation.integrate(eq, "x", lower, upper), TOLERANCE);
	}

	@Test
	void integratesWithAdditionalVariables() {
		assertEquals(
			6,
			Equation.integrate(
				"a*x + b",
				"x",
				0,
				2,
				new MathVariable("a", 2),
				new MathVariable("b", 1)
			),
			TOLERANCE
		);
	}

	@Test
	void returnsReusableIntegral() {
		DoubleBinaryOperator integral = Equation.integrate("x^2", "x");
		double whole = integral.applyAsDouble(0, 2);
		double parts = integral.applyAsDouble(0, 1) + integral.applyAsDouble(1, 2);

		assertEquals(8.0 / 3, whole, TOLERANCE);
		assertEquals(whole, parts, TOLERANCE);
	}

	@Test
	void integratesAssignment() {
		assertEquals(2, Equation.integrate("y = x", "x", 0, 2), TOLERANCE);
	}

	@Test
	void integratesEulerConstant() {
		assertEquals(
			Math.E,
			Equation.integrate("e", "x", 0, 1, new MathVariable("e", Math.E)),
			TOLERANCE
		);
	}

	@Test
	void rejectsMissingIntegralVariable() {
		assertThrows(ArithmeticException.class, () -> Equation.integrate("x + z", "x", 0, 1));
	}

	private static double calculate(String eq, MathVariable... args) {
		return Equation.calculate(Equation.tokenize(eq).toArray(String[]::new), args);
	}
}
