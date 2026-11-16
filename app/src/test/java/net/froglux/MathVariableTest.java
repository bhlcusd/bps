package net.froglux;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MathVariableTest {
	@Test
	void storesNameValueAndMutability() {
		MathVariable variable = new MathVariable("x", 2.5);

		assertEquals("x", variable.getName());
		assertEquals(2.5, variable.get(), 1e-9);
		assertFalse(variable.isConstant());

		variable.set(7.5);
		assertEquals(7.5, variable.get(), 1e-9);
	}

	@Test
	void constantIgnoresUpdates() {
		MathVariable variable = new MathVariable("g", 9.81, true);

		assertTrue(variable.isConstant());
		assertEquals(9.81, variable.get(), 1e-9);

		variable.set(1);
		assertEquals(9.81, variable.get(), 1e-9);
	}

	@Test
	void clearRestoresConfiguredDefault() {
		MathVariable variable = new MathVariable("x", 5, 2, false);

		variable.set(9);
		assertEquals(9, variable.get(), 1e-9);

		variable.clear();
		assertEquals(2, variable.get(), 1e-9);
	}

	@Test
	void clearRestoresDefaultForConstant() {
		MathVariable variable = new MathVariable("g", 9.81, true);

		variable.set(1);
		variable.clear();

		assertEquals(9.81, variable.get(), 1e-9);
	}
}
