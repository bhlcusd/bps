package net.froglux;

public class App {
	public static String VERSION = "v0.0.1";

	public static void main(String[] args) {
		Simulator sim = Simulator.getInstance();
		sim.run();
	}
}
