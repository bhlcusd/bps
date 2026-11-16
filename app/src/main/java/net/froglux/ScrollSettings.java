package net.froglux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

// Resolves the operating system's natural scrolling preference when possible
public class ScrollSettings {
	private static final long TIMEOUT_MS = 500;
	private static final String MAC_KEY = "com.apple.swipescrolldirection";

	private static Boolean systemNatural;

	private ScrollSettings() {}

	public static synchronized boolean systemNatural() {
		if (systemNatural == null) {
			systemNatural = detectSystemNatural();
		}

		return systemNatural;
	}

	public static Boolean parseMacDirection(String value) {
		if (value == null) {
			return null;
		}

		return switch (value.trim()) {
			case "1" -> Boolean.TRUE;
			case "0" -> Boolean.FALSE;
			default -> null;
		};
	}

	private static boolean detectSystemNatural() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

		if (!os.contains("mac")) {
			return false;
		}

		Boolean direction = readMacDirection("NSGlobalDomain");
		if (direction == null) {
			direction = readMacDirection("-g");
		}

		// macOS enables natural scrolling by default when the key is absent
		return direction == null || direction;
	}

	private static Boolean readMacDirection(String domain) {
		Process process = null;

		try {
			process = new ProcessBuilder("/usr/bin/defaults", "read", domain, MAC_KEY)
				.redirectErrorStream(true)
				.start();

			if (!process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
				return null;
			}

			String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			return parseMacDirection(output);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (IOException | SecurityException e) {
			return null;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroyForcibly();
			}
		}
	}
}
