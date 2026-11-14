package net.froglux;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JList;
import javax.swing.JPanel;

// Lists current world and child nodes; allows user to add, select, copy, paste, and delete nodes
public class Explorer extends JPanel {
	private World world;

	private JList<Node> list;

	private static Explorer instance;

	// TODO: Create a list using JList that lists all objects present in world, clicking on it directs the Inspector to get value; top bar includes space to add child to currently selected item (world when no world selected, or node when world/node selected)
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
