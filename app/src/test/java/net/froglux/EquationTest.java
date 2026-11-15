package net.froglux;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
	})
	void calculatesWithPrecedence(String eq, double expected) {
		double actual = Equation.calculate(Equation.tokenize(eq).toArray(String[]::new));
		assertEquals(expected, actual, TOLERANCE);
	}

	@Test 
	void substitutesVariables() {
		MathVariable x = new MathVariable("x", Math.random() * 20);
		MathVariable y = new MathVariable("y", Math.random() * 20);

		double result = Equation.calculate(Equation.tokenize("x^2 + y").toArray(String[]::new), x, y);

		assertEquals(Math.pow(x.get(), 2) + y.get(), result, TOLERANCE);
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

	@Test 
	void integratesLinearFunction() {
		assertEquals(6, Equation.integrate("3*x", "x", 0, 2), TOLERANCE);
	}

	@Test
	void integratesQuadraticFunction() {
		assertEquals(4, Equation.integrate("x^3", "x", 0, 2), TOLERANCE);
	}
}
