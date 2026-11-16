package net.froglux;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

// Lists all worlds and their child nodes; allows user to add, select, and delete worlds and nodes
public class Explorer extends JPanel {
	private final DefaultListModel<Object> model;
	private final JList<Object> list;
	private final JButton addButton;
	private final JButton deleteButton;
	private final JLabel targetLabel;
	private final JPopupMenu addMenu;
	private final JMenuItem addWorldItem;
	private final JMenuItem addNodeItem;

	private static Explorer instance;

	private Explorer() {
		setLayout(new BorderLayout());

		this.model = new DefaultListModel<>();
		this.list = new JList<>(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new ExplorerRenderer());

		this.addButton = new JButton("Add");
		this.deleteButton = new JButton("Delete");
		this.deleteButton.setEnabled(false);
		this.targetLabel = new JLabel("No World Selected", SwingConstants.CENTER);

		this.addMenu = new JPopupMenu();
		this.addWorldItem = new JMenuItem("World");
		this.addNodeItem = new JMenuItem("Node");
		addMenu.add(addWorldItem);
		addMenu.add(addNodeItem);

		addButton.addActionListener(e -> {
			updateAddMenu();
			addMenu.show(addButton, 0, addButton.getHeight());
		});
		addWorldItem.addActionListener(e -> addWorld());
		addNodeItem.addActionListener(e -> addNode());
		deleteButton.addActionListener(e -> delete());

		list.addListSelectionListener(e -> {
			if (e.getValueIsAdjusting()) {
				return;
			}

			World world = parentWorld(list.getSelectedValue());
			if (world != null) {
				Simulator.getInstance().setWorld(world);
			}

			updateToolbar();
		});

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
		Simulator sim = Simulator.getInstance();
		List<Object> items = buildItems(sim.getWorlds().values());

		if (sameItems(items)) {
			return;
		}

		Object selected = list.getSelectedValue();

		model.clear();
		for (Object item : items) {
			model.addElement(item);
		}

		World active = sim.getWorld();

		if (selected != null && items.contains(selected)) {
			list.setSelectedValue(selected, false);
		} else if (active != null && items.contains(active)) {
			list.setSelectedValue(active, false);
		} else if (!items.isEmpty()) {
			list.setSelectedIndex(0);
		} else {
			list.clearSelection();
		}
	}

	static List<Object> buildItems(Collection<World> worlds) {
		List<World> sorted = new ArrayList<>(worlds);
		sorted.sort(Comparator.comparingInt(World::getId));

		List<Object> items = new ArrayList<>();
		for (World world : sorted) {
			items.add(world);
			items.addAll(world.getNodes());
		}

		return items;
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

		updateAddMenu();
		deleteButton.setEnabled(sel != null);

		String target;
		if (sel instanceof Node node && node.getWorld() != null) {
			target = "Selected: Node #" + node.getId();
		} else if (sel instanceof World world) {
			target = "Selected: World #" + world.getId();
		} else if (curr != null) {
			target = "Selected none (World #" + curr.getId() + ")";
		} else {
			target = "No World";
		}

		if (!target.equals(targetLabel.getText())) {
			targetLabel.setText(target);
		}
	}

	private void updateAddMenu() {
		addNodeItem.setVisible(parentWorld(list.getSelectedValue()) != null);
	}

	private void addWorld() {
		Simulator sim = Simulator.getInstance();
		int id = nextWorldId(sim.getWorlds().values());

		World created = new World(id, World.DEFAULT_TICK, World.DEFAULT_START, World.DEFAULT_END, World.DEFAULT_GRAVITY);
		sim.setWorld(created);

		refresh();
		select(created);
	}

	private void addNode() {
		World parent = parentWorld(list.getSelectedValue());
		if (parent == null) {
			return;
		}

		Node node = new Node(nextNodeId(parent), World.DEFAULT_MASS);
		node.setEquation(new MathFunction("0", "t"), Node.FunctionType.POSITION);
		parent.addNode(node);

		Simulator.getInstance().setWorld(parent);
		refresh();
		select(node);
	}

	private void delete() {
		Object sel = list.getSelectedValue();
		Simulator sim = Simulator.getInstance();

		if (sel instanceof Node node) {
			World parent = node.getWorld();
			if (parent != null) {
				parent.removeNode(node);
			}

			refresh();
			if (parent != null) {
				select(parent);
			}
			return;
		}

		if (sel instanceof World world) {
			boolean active = sim.getWorld() == world;
			sim.removeWorld(world);

			if (active) {
				World fallback = firstWorld(sim.getWorlds().values());
				if (fallback != null) {
					sim.setWorld(fallback);
				}
			}

			refresh();

			World fallback = sim.getWorld();
			if (fallback != null) {
				select(fallback);
			}
			return;
		}

		refresh();
	}

	public static World parentWorld(Object selected) {
		if (selected instanceof Node node) {
			return node.getWorld();
		}

		if (selected instanceof World world) {
			return world;
		}

		return null;
	}

	public static int nextWorldId(Collection<World> worlds) {
		return worlds.stream()
			.mapToInt(World::getId)
			.max()
			.orElse(0) + 1;
	}

	public static int nextNodeId(World world) {
		return world.getNodes().stream()
			.mapToInt(Node::getId)
			.max()
			.orElse(0) + 1;
	}

	public static World firstWorld(Collection<World> worlds) {
		return worlds.stream()
			.min(Comparator.comparingInt(World::getId))
			.orElse(null);
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
		addReadOnly("Id", () -> String.valueOf(world.getId()));
		addNumber("Start", world::getStart, world::setStart);
		addNumber("End", world::getEnd, world::setEnd);
		addNumber("Tick", world::getTick, world::setTick);
		addNumber("Gravity", world::getGravity, val -> {
			world.setGravity(val);
			return true;
		});
		addNumber("Min Y", world::getYMin, val -> setYRange(world, val, world.getYMax()));
		addNumber("Max Y", world::getYMax, val -> setYRange(world, world.getYMin(), val));
		addReadOnly("Autofocus", () -> world.isAutoFit() ? "On" : "Manual");
		addScrollMode(world);
		addReadOnly("Time", () -> format(world.getTime()));
		addReadOnly("Nodes", () -> String.valueOf(world.getNodes().size()));
	}

	private void addScrollMode(World world) {
		JComboBox<World.ScrollMode> combo = new JComboBox<>(World.ScrollMode.values());
		combo.setSelectedItem(world.getScrollMode());
		combo.addActionListener(e -> {
			world.setScrollMode((World.ScrollMode) combo.getSelectedItem());
			updateScrollTooltip(combo, world);
		});

		updateScrollTooltip(combo, world);
		addRow("Scroll Mode", combo);
	}

	private static void updateScrollTooltip(JComboBox<World.ScrollMode> combo, World world) {
		String tooltip = switch (world.getScrollMode()) {
			case SYSTEM -> "Using OS setting: " + (ScrollSettings.systemNatural() ? "Natural" : "Traditional");
			case NATURAL -> "Scroll down to zoom in";
			case TRADITIONAL -> "Scroll up to zoom in";
		};

		combo.setToolTipText(tooltip);
	}

	private static boolean setYRange(World world, double min, double max) {
		if (!world.setYRange(min, max)) {
			return false;
		}

		Graph.getInstance().setYRange(world.getYMin(), world.getYMax());
		return true;
	}

	private void buildNode(Node node) {
		addReadOnly("Id", () -> String.valueOf(node.getId()));
		addReadOnly("World Id", () -> node.getWorld() == null ? "None" : String.valueOf(node.getWorld().getId()));
		addColor(node);
		addNumber("Mass", node::getMass, node::setMass);
		addType(node);
		addEquation(node);
		addReadOnly("Position", () -> format(valueAt(node, "x")));
		addReadOnly("Velocity", () -> format(valueAt(node, "v")));
		addReadOnly("Acceleration", () -> format(valueAt(node, "a")));
	}

	private void addColor(Node node) {
		JPanel swatch = new JPanel();
		swatch.setBackground(node.getColor());
		swatch.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0)));
		swatch.setToolTipText(String.format("#%06X", node.getColor().getRGB() & 0xFFFFFF));

		JPanel holder = new JPanel(new BorderLayout());
		holder.setOpaque(false);
		holder.add(swatch, BorderLayout.CENTER);

		addRow("Color", holder);
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
		JTextField field = new JTextField(value.get());
		field.setEditable(false);
		liveValues.put(name, field);
		addRow(name, field);
	}

	private void addType(Node node) {
		JComboBox<Node.FunctionType> combo = new JComboBox<>(Node.FunctionType.values());
		combo.setSelectedItem(node.getType());
		combo.addActionListener(e -> node.setEquation(node.getEquation(), (Node.FunctionType) combo.getSelectedItem()));
		addRow("Equation Type", combo);
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
					MathFunction eq = new MathFunction(text, "t");
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
		addRow("Equation", field);
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
		label.setFont(label.getFont().deriveFont(14.0f));
		form.add(label);
		form.add(component);
	}

	private void updateLiveValues() {
		if (target instanceof World world) {
			setLive("Id", String.valueOf(world.getId()));
			setLive("Autofocus", world.isAutoFit() ? "On" : "Manual");
			setLive("Time", format(world.getTime()));
			setLive("Nodes", String.valueOf(world.getNodes().size()));
		} else if (target instanceof Node node) {
			setLive("Id", String.valueOf(node.getId()));
			setLive("World", node.getWorld() == null ? "None" : "#" + node.getWorld().getId());
			setLive("Position", format(valueAt(node, "x")));
			setLive("Velocity", format(valueAt(node, "v")));
			setLive("Acceleration", format(valueAt(node, "a")));
		}
	}

	private static double valueAt(Node node, String key) {
		World world = node.getWorld();

		if (world != null) {
			Result result = world.getResult(node);
			if (result != null) {
				double value = result.valueAt(key, world.getTime());
				if (Double.isFinite(value)) {
					return value;
				}
			}
		}

		return switch (key) {
			case "x" -> node.getPos();
			case "v" -> node.getVel();
			case "a" -> node.getAcc();
			default -> Double.NaN;
		};
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
