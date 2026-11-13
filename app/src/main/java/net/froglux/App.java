package net.froglux;

public class App {
	public static final String VERSION = resolveVersion();

	public static void main(String[] args) {
		Simulator sim = Simulator.getInstance();
		sim.run();
	}

	private static String resolveVersion() {
		Package pkg = App.class.getPackage();
		String v = (pkg == null) ? null : pkg.getImplementationVersion();
		return (v == null || v.isBlank()) ? "v0.0.0-dev" : "v" + v;
	}
}
