package net.froglux;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScrollSettingsTest {
	@Test
	void parsesMacScrollDirection() {
		assertEquals(Boolean.TRUE, ScrollSettings.parseMacDirection("1"));
		assertEquals(Boolean.TRUE, ScrollSettings.parseMacDirection(" 1\n"));
		assertEquals(Boolean.FALSE, ScrollSettings.parseMacDirection("0"));
		assertNull(ScrollSettings.parseMacDirection(""));
		assertNull(ScrollSettings.parseMacDirection("Error: key not found"));
		assertNull(ScrollSettings.parseMacDirection(null));
	}
}
