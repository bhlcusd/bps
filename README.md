<div align="center">
	<h1>bps</h1>
	<picture>
		<img src="./resources/demo/demo.gif" alt="bps Demo Clip">
	</picture>
	<h3>A basic physics simulator written in Java.</h3>
</div>

> [!NOTE]
> bps is currently in alpha. Some features may not be complete! Check out the change log [here.](./CHANGELOG.md)

## Quick Start
* Using JAR (Recommended):
	1. Download your favorite JRE (Java Runtime Environment) or JDK (Java Development Kit); use the [official builds](https://jdk.java.net/) or use a package manager like [brew](https://formulae.brew.sh/formula/openjdk).
	2. Download the latest `.jar` file from [Releases](https://github.com/bhlcusd/bps/releases/latest).
	3. Open up the terminal and navigate to where the `.jar` is located.
	4. Execute: `java -jar bps-[version].jar`. Replace `[version]` with the file's version.
	5. Enjoy bps!

* Using OS installer (not tested by developer):
	1. Download the latest installer for your OS from [Releases](https://github.com/bhlcusd/bps/releases/latest).
	2. Open the installer and follow the instructions shown.
	3. Launch and enjoy bps!

## Features
 - [Explorer](./app/src/main/java/net/froglux/Explorer.java): Select or create a world or node; selecting a world or node will highlight its info inside the Inspector
 - [Inspector](./app/src/main/java/net/froglux/Explorer.java): Reveal a world's or node's values; worlds and nodes have different properties
 - [Graph](./app/src/main/java/net/froglux/Graph.java): A zoomable and panable graph that plots points with axes and labels
 - [Toolbar](./app/src/main/java/net/froglux/Graph.java): A simple toolbar that contains buttons to run the world/nodes, play/pause a simulation, or focus on the graph or return to the default "home" view

## Technicals
 * Mostly native Java: The only dependencies used are for testing, but are not required for development of the program. The entire program is written in pure Java and Swing, so there is technically no need for Gradle, but it helps with building and testing functions.
 * Relatively fast: [Equation](./app/src/main/java/net/froglux/Equation.java) contains many convenience methods for parsing text equations. It uses a two stack system to parse operands and operators together. There are also methods that help with integration and differentiation. The functions can be converted into Java DoubleUnaryOperator and DoubleBinaryOperator, both of which are versions of the built-in Java Function<> class. This allows functions to essentially input their own t-value and extract the value quickly.
 * Simple GUI: The GUI, although dated by its look and feel, is quite simple. The Inspector was modeled after Godot's inspector, with a simple two column approach that shows object information in a concise manner. The Graph is also quite intuitive, with optional natural scrolling enabled by default (depending on system configuration) for macOS.

## License
bps is licensed under the [GNU General Public License Version 3](./LICENSE).