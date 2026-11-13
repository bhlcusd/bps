package net.froglux;

/*
 * `bps` is a simple physics simulator built on Java and Swing. It relies on a global timer (ticks)
 * that acts as dt. It can simulate both basic (algebraic) and advanced (calculus) physics. Position,
 * velocity, and acceleration can be in terms of t, x, v, and t (but not itself). 
 * 
 * Uses the Singleton pattern to maintain a single object for UI components (easily pass around contexts).
 * 
 * This file contains all classes related to rendering.
 */

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.Timer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;

public class Simulator implements ActionListener {
	private List<World> worlds;
	private World world;

	private JFrame frame;
	private Timer timer;

	private static Simulator instance;

	public static final int TARGET_FRAME_RATE = 60;

	// Simulator contains references to worlds and acts as the main renderer for the program (relies on Swing components for everything except Graph window)
	private Simulator() {
		this.worlds = new ArrayList<>();
		this.world = null;

		var screen = Toolkit.getDefaultToolkit().getScreenSize();

		this.frame = new JFrame();
		frame.setMinimumSize(new Dimension((int) Math.round(screen.getWidth() / 3.0) , (int) Math.round(screen.getHeight() / 3.0)));
		frame.setSize((int) Math.round(screen.getWidth() * 0.75) , (int) Math.round(screen.getHeight() * 0.75));
		frame.setTitle("bps " + App.VERSION);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.25;
		gbc.weighty = 0.5;
		gbc.gridheight = 1;
		frame.add(Explorer.getInstance(), gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.25;
		gbc.weighty = 0.5;
		gbc.gridheight = 1;
		frame.add(Inspector.getInstance(), gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0.75;
		gbc.weighty = 1;
		gbc.gridheight = 2;
		frame.add(Graph.getInstance(), gbc);

		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		this.timer = new Timer((int) Math.round(1000.0 / TARGET_FRAME_RATE), this);
		timer.start();
	}

	public static Simulator getInstance() {
		if (instance == null) {
			instance = new Simulator();
		}

		return instance;
	}

	public void run() {
		// TODO: Do something on execution - window should already be created
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Explorer.getInstance().refresh();
		Inspector.getInstance().refresh();
		Graph.getInstance().refresh();
	}
}

// Lists current world and child nodes; allows user to add, select, copy, paste, and delete nodes
class Explorer extends JPanel {
	private World world;

	private JList<Node> list;

	private static Explorer instance;

	// TODO: Create a list using JList that lists all objects present in world, clicking on it directs the Inspector to get value
	private Explorer() {
		this.setBackground(Color.RED);
		this.world = null;
	}

	public static Explorer getInstance() {
		if (instance == null) {
			instance = new Explorer();
		}

		return instance;
	}

	public void refresh() {

	}
}

// Lists the properties of a node, provides settings similar to Godot, with a label and an input value (number, string, boolean (checkbox), or dropdown)
class Inspector extends JPanel {
	private static Inspector instance;

	// TODO: Create a grid with key: value/input pairs (similar to Godot)
	private Inspector() {
		this.setBackground(Color.BLUE);
		this.setLayout(new GridLayout(0, 2));
	}

	public static Inspector getInstance() {
		if (instance == null) {
			instance = new Inspector();
		}

		return instance;
	}

	public void refresh() {
		
	}
}

// A simple graph that plots points with scalable axis and can conform to varying sizes
class Graph extends JPanel {
	// Grid tick distance
	private int xInc, yInc;

	// Flags
	private boolean grid;

	private static Graph instance;

	private Graph() {
		this.setBackground(Color.GREEN);
	}

	public static Graph getInstance() {
		if (instance == null) {
			instance = new Graph();
		}

		return instance;
	}

	public void refresh() {
		
	}

	// TODO: Draw graph view with lines and plot points by getting processed points from world
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;

		
	}
}