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
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.Timer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Simulator implements ActionListener, KeyListener {
	private Map<Integer, World> worlds;
	private World world;

	private JFrame frame;
	private JLayeredPane container;
	private JPanel general;
	private JPanel modal;
	private Timer timer;

	private static Simulator instance;

	public static final int TARGET_FRAME_RATE = 60;
	public static final int SIMULATION_THREADS = 3;

	// Simulator contains references to worlds and acts as the main renderer for the program (relies on Swing components for everything except Graph window)
	private Simulator() {
		this.worlds = new HashMap<>();
		this.world = null;

		var screen = Toolkit.getDefaultToolkit().getScreenSize();

		this.frame = new JFrame();
		frame.setMinimumSize(new Dimension((int) Math.round(screen.getWidth() / 3.0) , (int) Math.round(screen.getHeight() / 3.0)));
		frame.setSize((int) Math.round(screen.getWidth() * 0.75) , (int) Math.round(screen.getHeight() * 0.75));
		frame.setTitle("bps " + App.VERSION);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.container = new JLayeredPane();
		container.setLayout(new OverlayLayout(container));
		frame.setContentPane(container);

		this.general = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.25;
		gbc.weighty = 0.5;
		gbc.gridheight = 1;
		general.add(Explorer.getInstance(), gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.25;
		gbc.weighty = 0.5;
		gbc.gridheight = 1;
		general.add(Inspector.getInstance(), gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0.75;
		gbc.weighty = 1;
		gbc.gridheight = 2;
		general.add(Graph.getInstance(), gbc);

		general.setAlignmentX(0.5f);
		general.setAlignmentY(0.5f);
		container.add(general, JLayeredPane.DEFAULT_LAYER);

		this.modal = new JPanel(new GridBagLayout()) {
			@Override 
			public void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g;
				g2d.setColor(new Color(0, 0, 0, 128));
				g2d.fillRect(0, 0, getWidth(), getHeight());
				g2d.dispose();
				super.paintComponent(g);
			}
		};
		modal.setOpaque(false);
		modal.setVisible(false);
		modal.setAlignmentX(0.5f);
		modal.setAlignmentY(0.5f);
		container.add(modal, JLayeredPane.MODAL_LAYER);

		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		frame.addKeyListener(this);

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
		Graph graph = Graph.getInstance();

		graph.setViewport(0, 10, -5, 5, 1, 1);
		graph.setShowGrid(true);
		graph.setConnectPoints(true);

		graph.setPoints(
			new Point(0, 0),
			new Point(1, 1),
			new Point(2, -2),
			new Point(3, 3)
		);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Explorer.getInstance().refresh();
		Inspector.getInstance().refresh();
		Graph.getInstance().refresh();
	}

	// Main keyboard listener

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyChar() == 'c') {
			this.modal.setVisible(!this.modal.isVisible());
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {}

	// Getters/setters

	public void setWorld(World world) {
		worlds.put(world.getId(), world);
	}

	public World getWorld() {
		return world;
	}

	public void clearWorlds() {
		worlds.clear();
	}

	public Map<Integer, World> getWorlds() {
		return worlds;
	}

	// Misc. methods

	// NOTE: There is one shared modal; any class can replace modal with own config using: modal.removeAll(); modal.revalidate(); modal.repaint();
	public void showModal() {
		this.modal.setVisible(true);
	}

	public void hideModal() {
		this.modal.setVisible(false);
	}

	public JPanel getModal() {
		return this.modal;
	}
}