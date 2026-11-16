package net.froglux;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

// Lists current world and child nodes; allows user to add, select, copy, paste, and delete nodes
public class Explorer extends JPanel {
	private final DefaultListModel<Object> model;
	private final JList<Object> list;
	private final JButton addButton;
	private final JButton deleteButton;
	private final JLabel targetLabel;

	private static Explorer instance;

	// TODO: Create a list using JList that lists all objects present in world, clicking on it directs the Inspector to get value; top bar includes space to add child to currently selected item (world when no world selected, or node when world/node selected)
	private Explorer() {
		setLayout(new BorderLayout());

		this.model = new DefaultListModel<>();
		this.list = new JList<>(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new ExplorerRenderer());

		this.addButton = new JButton("Add World");
		this.deleteButton = new JButton("Delete");
		this.deleteButton.setEnabled(false);
		this.targetLabel = new JLabel("No World Selected", SwingConstants.CENTER);

		addButton.addActionListener(e -> add());
		deleteButton.addActionListener(e -> delete());

		JPanel actions = new JPanel(new GridLayout(1, 2, 4, 0));
		actions.add(addButton);
		actions.add(deleteButton);

		JPanel toolbar = new JPanel(new BorderLayout(0, 2));
		toolbar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		toolbar.add(actions, BorderLayout.NORTH);
		toolbar.add(targetLabel, BorderLayout.SOUTH);

		add(toolbar, BorderLayout.NORTH);
		add(new JScrollPane(list), BorderLayout.CENTER);

		list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "delete");
		list.getActionMap().put("delete", new AbstractAction() {
			@Override 
			public void actionPerformed(ActionEvent e) {
				delete();
			}
		});
	}

	public static Explorer getInstance() {
		if (instance == null) {
			instance = new Explorer();
		}

		return instance;
	}

	public void refresh() {
		syncModel();
		updateToolbar();
	}

	public Object getSelected() {
		return list.getSelectedValue();
	}

	private void syncModel() {
		World current = Simulator.getInstance().getWorld();

		List<Object> items = new ArrayList<>();
		if (current != null) {
			items.add(current);
			items.addAll(current.getNodes());
		}

		if (sameItems(items)) {
			return;
		}

		Object selected = list.getSelectedValue();

		model.clear();
		for (Object item : items) {
			model.addElement(item);
		}

		if (selected != null && items.contains(selected)) {
			list.setSelectedValue(selected, false);
		} else if (!items.isEmpty()) {
			list.setSelectedIndex(0);
		}
	}

	private boolean sameItems(List<Object> items) {
		if (model.getSize() != items.size()) {
			return false;
		}

		for (int i = 0; i < items.size(); i++) {
			if (model.getElementAt(i) != items.get(i)) {
				return false;
			}
		}

		return true;
	}

	private void updateToolbar() {
		World curr = Simulator.getInstance().getWorld();
		Object sel = list.getSelectedValue();

		String text = curr == null ? "Add World" : "Add Node";
		if (!text.equals(addButton.getToolTipText())) {
			addButton.setText(text);
		}

		deleteButton.setEnabled(sel != null);

		String target;
		if (curr == null) {
			target = "No World";
		} else if (sel instanceof Node node && node.getWorld() != null) {
			target = "Selected: Node #" + node.getId();
		} else if (sel instanceof World world) {
			target = "Selected: World #" + world.getId();
		} else {
			target = "Selected none (World #" + curr.getId() + ")";
		}

		if (!target.equals(targetLabel.getText())) {
			targetLabel.setText(target);
		}
	}

	private void add() {
		Simulator sim = Simulator.getInstance();
		World curr = sim.getWorld();

		if (curr == null) {
			int id = sim.getWorlds().keySet().stream()
				.mapToInt(Integer::intValue)
				.max()
				.orElse(0) + 1;
			
				World created = new World(id, World.DEFAULT_TICK, World.DEFAULT_START, World.DEFAULT_END, World.DEFAULT_GRAVITY);
				sim.setWorld(created);

				refresh();
				select(created);
				return;
		}

		Object sel = list.getSelectedValue();
		World parent = curr;

		if (sel instanceof Node node && node.getWorld() != null) {
			parent = node.getWorld();
		}

		int id = parent.getNodes().stream()
			.mapToInt(Node::getId)
			.max()
			.orElse(0) + 1;
		
		Node node = new Node(id, World.DEFAULT_MASS);
		node.setEquation(new MathFunction("0", "t"), Node.FunctionType.POSITION);
		parent.addNode(node);

		refresh();
		select(node);
	}

	private void delete() {
		Object sel = list.getSelectedValue();

		if (sel instanceof Node node) {
			World parent = node.getWorld();
			if (parent != null) {
				parent.removeNode(node);
			}
		} else if (sel instanceof World world) {
			Simulator.getInstance().removeWorld(world);
		}

		refresh();
	}

	private boolean select(Object item) {
		for (int i = 0; i < model.size(); i++) {
			if (model.getElementAt(i) == item) {
				list.setSelectedIndex(i);
				list.ensureIndexIsVisible(i);
				return true;
			}
		}

		return false;
	}

	private static class ExplorerRenderer extends DefaultListCellRenderer {
		private static final Border WORLD_BORDER = BorderFactory.createEmptyBorder(2, 4, 2, 4);
		private static final Border NODE_BORDER = BorderFactory.createEmptyBorder(2, 24, 2, 4);

		@Override 
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value instanceof World world) {
				setText("World #" + world.getId());
				setFont(getFont().deriveFont(Font.BOLD));
				setBorder(WORLD_BORDER);
			} else if (value instanceof Node node) {
				setText("Node #" + node.getId());
				setFont(getFont().deriveFont(Font.PLAIN));
				setBorder(NODE_BORDER);
			}

			return this;
		}
	}
}

// Lists the properties of a node, provides settings similar to Godot, with a label and an input value (number, string, boolean (checkbox), or dropdown)
class Inspector extends JPanel {
	private static final Border INVALID_BORDER = BorderFactory.createLineBorder(Color.RED, 1);
	private static final DecimalFormat NUMBER_FORMAT = createDecimalFormat();

	private final JPanel form;
	private final Map<String, JComponent> liveValues;
	private final JLabel label;

	private Object target;
	private boolean built;

	private static Inspector instance;

	private Inspector() {
		setLayout(new BorderLayout());

		this.form = new PropertyForm();
		form.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		this.liveValues = new HashMap<>();

		JPanel header = new JPanel();
		this.label = new JLabel("Inspector", SwingConstants.CENTER);
		header.add(this.label);

		add(header, BorderLayout.NORTH);
		add(new JScrollPane(form), BorderLayout.CENTER);
	}

	private static DecimalFormat createDecimalFormat() {
		DecimalFormat format = new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(Locale.ROOT));
		format.setGroupingUsed(false);
		return format;
	}

	public static Inspector getInstance() {
		if (instance == null) {
			instance = new Inspector();
		}

		return instance;
	}

	public void refresh() {
		Object sel = Explorer.getInstance().getSelected();
		if (sel instanceof Node node && node.getWorld() == null) {
			sel = null;
		}

		if (!built || sel != target) {
			target = sel;
			built = true;
			rebuild();
		}

		updateLiveValues();
	}

	private void rebuild() {
		form.removeAll();
		liveValues.clear();

		label.setText("Inspector");

		if (target instanceof World world) {
			buildWorld(world);
		} else if (target instanceof Node node) {
			buildNode(node);
		} else {
			label.setText("Select a World or Node");
		}

		form.revalidate();
		form.repaint();
	}

	private void buildWorld(World world) {
		addReadOnly("id", () -> String.valueOf(world.getId()));
		addNumber("start", world::getStart, world::setStart);
		addNumber("end", world::getEnd, world::setEnd);
		addNumber("tick", world::getTick, world::setTick);
		addNumber("gravity", world::getGravity, val -> {
			world.setGravity(val);
			return true;
		});
		addReadOnly("time", () -> format(world.getTime()));
		addReadOnly("nodes", () -> String.valueOf(world.getNodes().size()));
	}

	private void buildNode(Node node) {
		addReadOnly("id", () -> String.valueOf(node.getId()));
		addReadOnly("world id", () -> node.getWorld() == null ? "none" : String.valueOf(node.getWorld().getId()));
		addColor(node);
		addNumber("mass", node::getMass, node::setMass);
		addType(node);
		addEquation(node);
		addReadOnly("x", () -> format(node.getPos()));
		addReadOnly("v", () -> format(node.getVel()));
		addReadOnly("a", () -> format(node.getAcc()));
	}

	private void addColor(Node node) {
		JPanel swatch = new JPanel();
		swatch.setBackground(node.getColor());
		swatch.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0)));
		swatch.setToolTipText(String.format("#%06X", node.getColor().getRGB() & 0xFFFFFF));

		JPanel holder = new JPanel(new BorderLayout());
		holder.setOpaque(false);
		holder.add(swatch, BorderLayout.CENTER);

		addRow("color", holder);
	}

	private void addNumber(String name, DoubleSupplier getter, DoublePredicate setter) {
		JTextField field = new JTextField(format(getter.getAsDouble()));
		Border normal = field.getBorder();

		Runnable commit = () -> {
			try {
				double value = Double.parseDouble(field.getText().trim());
				if (!Double.isFinite(value) || !setter.test(value)) {
					throw new NumberFormatException();
				}

				field.setText(format(getter.getAsDouble()));
				field.setBorder(normal);
				field.setToolTipText(null);
			} catch (NumberFormatException e) {
				field.setBorder(INVALID_BORDER);
				field.setToolTipText("Invalid Value");
			}
		};

		bindCommit(field, commit);
		addRow(name, field);
	}

	private void addReadOnly(String name, Supplier<String> value) {
		// JLabel label = new JLabel(value.get());
		// label.setForeground(new Color(64, 64, 64));
		JTextField field = new JTextField(value.get());
		field.setEditable(false);
		liveValues.put(name, field);
		addRow(name, field);
	}

	private void addType(Node node) {
		JComboBox<Node.FunctionType> combo = new JComboBox<>(Node.FunctionType.values());
		combo.setSelectedItem(node.getType());
		combo.addActionListener(e -> node.setEquation(node.getEquation(), (Node.FunctionType) combo.getSelectedItem()));
		addRow("type", combo);
	}

	private void addEquation(Node node) {
		JTextField field = new JTextField(node.getEquation() == null ? "" : node.getEquation().getEquation());
		Border normal = field.getBorder();

		Runnable commit = () -> {
			String text = field.getText().trim();

			try {
				if (text.isEmpty()) {
					node.setEquation(null, null);
				} else {
					MathFunction eq = new MathFunction(text, "t"); // TODO: Change depending on what function type it is
					eq.calculate(node.getVariables(0)); // Validate expr
					node.setEquation(eq, node.getType());
				}

				field.setBorder(normal);
				field.setToolTipText(null);
			} catch (ArithmeticException e) {
				field.setBorder(INVALID_BORDER);
				field.setToolTipText(e.getMessage());
			}
		};

		bindCommit(field, commit);
		addRow("equation", field);
	}

	private void bindCommit(JTextField field, Runnable commit) {
		field.addActionListener(e -> commit.run());
		field.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				commit.run();
			}
		});
	}

	private void addRow(String name, JComponent component) {
		JLabel label = new JLabel(name + ":");
		label.setVerticalAlignment(SwingConstants.CENTER);
		form.add(label);
		form.add(component);
	}

	private void updateLiveValues() {
		if (target instanceof World world) {
			setLive("id", String.valueOf(world.getId()));
			setLive("time", format(world.getTime()));
			setLive("nodes", String.valueOf(world.getNodes().size()));
		} else if (target instanceof Node node) {
			setLive("id", String.valueOf(node.getId()));
			setLive("world", node.getWorld() == null ? "none" : "#" + node.getWorld().getId());
			setLive("x", format(node.getPos()));
			setLive("v", format(node.getVel()));
			setLive("a", format(node.getAcc()));
		}
	}

	private void setLive(String name, String val) {
		JComponent comp = liveValues.get(name);

		if (comp == null) {
			return;
		}

		if (comp instanceof JLabel label && !val.equals(label.getText())) {
			label.setText(val);
		} else if (comp instanceof JTextField field && !val.equals(field.getText())) {
			field.setText(val);
		}
	}

	private static String format(double val) {
		return NUMBER_FORMAT.format(val);
	}

	private static class PropertyForm extends JPanel implements Scrollable {
		private PropertyForm() {
			super(new GridLayout(0, 2, 4, 4));
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}
}
