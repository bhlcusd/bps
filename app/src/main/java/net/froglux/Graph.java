package net.froglux;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;

// A simple graph that plots points with scalable axis and can conform to varying sizes
public class Graph extends JPanel {
	// Colors
	private static final Color PANEL_BACKGROUND = new Color(255, 255, 255);
	private static final Color PLOT_PACKGROUND = new Color(255, 255, 255);
	private static final Color GRID_COLOR = new Color(128, 128, 128);
	private static final Color AXIS_COLOR = new Color(0, 0, 0);
	private static final Color BORDER_COLOR = new Color(192, 192, 192);
	private static final Color LABEL_COLOR = new Color(64, 64, 64);
	private static final Color PLAYHEAD_COLOR = new Color(255, 0, 0);
	private static Color pointColor = new Color(0, 0, 0);
	private static Color lineColor = new Color(0, 0, 0);
	
	// Geo
	private static final int POINT_RADIUS = 4;
	private static final int PLAYHEAD_RADIUS = 6;
	private static final float PLAYHEAD_WIDTH = 1f;
	private static final float LINE_WIDTH = 1.5f;
	private static final float AXES_WIDTH = 1.5f;
	private static final float TICK_WIDTH = 1f;
	private static final float TICK_HEIGHT = 3f;
	private static final float BORDER_WIDTH = 1f;
	private static final int MAX_DOTS_PER_SERIES = 1000;

	// Viewport
	private static final int PADDING_TOP = 16;
	private static final int PADDING_BOTTOM = 36;
	private static final int PADDING_LEFT = 36;
	private static final int PADDING_RIGHT = 16;
	public static final GraphViewport DEFAULT_VIEWPORT = new GraphViewport(-10, 10, -10, 10, 1, 1);
	private static final double ZOOM_STEP = 1.1;
	private static final double MIN_VIEWPORT_SPAN = 1e-9;
	private static final double MAX_VIEWPORT_SPAN = 1e12;

	// Misc. options
	private static final int MAX_DECIMALS = 3;
	private static final double VALUE_CUTOFF = 1e-6;
	private static final int LABEL_PADDING = 2;
	private static final int LABEL_SPACING = 4;

	// Singleton
	private static Graph instance;

	// States
	private final List<Point> points = new ArrayList<>();
	private volatile GraphViewport viewport = DEFAULT_VIEWPORT;
	private volatile boolean showGrid = true;
	private volatile boolean connectPoints = false;

	// Series
	private List<GraphSeries> series = new ArrayList<>();
	private double playheadTime = Double.NaN;

	// Navigation
	private double panStartX;
	private double panStartY;
	private GraphViewport panOrigin;

	private Graph() {
		setBackground(PANEL_BACKGROUND);
		setOpaque(true);
		setToolTipText("Drag to pan; scroll to zoom");

		MouseAdapter navigation = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() != MouseEvent.BUTTON1) {
					return;
				}

				Rectangle2D plot = plotBounds();
				if (plot == null || !plot.contains(e.getPoint())) {
					return;
				}

				panStartX = e.getX();
				panStartY = e.getY();
				panOrigin = viewport;
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (panOrigin == null) {
					return;
				}

				Rectangle2D plot = plotBounds();
				if (plot == null) {
					return;
				}

				double dx = (e.getX() - panStartX) / plot.getWidth() * panOrigin.width();
				double dy = (e.getY() - panStartY) / plot.getHeight() * panOrigin.height();
				GraphViewport moved = panViewport(panOrigin, dx, dy);

				if (moved != null) {
					setViewport(moved);
					markManualView();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				panOrigin = null;
				setCursor(Cursor.getDefaultCursor());
			}
		};

		addMouseListener(navigation);
		addMouseMotionListener(navigation);
		addMouseWheelListener(this::zoom);
	}

	public static Graph getInstance() {
		if (instance == null) {
			instance = new Graph();
		}

		return instance;
	}

	public void refresh() {
		repaint();
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		Rectangle2D.Double plot = plotBounds();
		if (plot == null) {
			return;
		}

		double[] xTicks = tickValues(viewport.minX(), viewport.maxX(), viewport.xTick());
		double[] yTicks = tickValues(viewport.minY(), viewport.maxY(), viewport.yTick());

		g2d.setColor(PLOT_PACKGROUND);
		g2d.fill(plot);

		if (showGrid) {
			drawGrid(g2d, plot, xTicks, yTicks);
		} 

		drawAxes(g2d, plot, xTicks, yTicks);
		drawData(g2d, plot);
		drawPlayhead(g2d, plot);

		g2d.setColor(BORDER_COLOR);
		g2d.setStroke(new BasicStroke(BORDER_WIDTH));
		g2d.draw(plot);
		
		drawLabels(g2d, plot, xTicks, yTicks);
	}

	private void drawGrid(Graphics2D g2d, Rectangle2D plot, double[] xTicks, double[] yTicks) {
		g2d.setColor(GRID_COLOR);
		g2d.setStroke(new BasicStroke(AXES_WIDTH));

		for (double x : xTicks) {
			double px = pixelX(x, plot);
			g2d.draw(new Line2D.Double(px, plot.getMinY(), px, plot.getMaxY()));
		}

		for (double y : yTicks) {
			double py = pixelY(y, plot);
			g2d.draw(new Line2D.Double(plot.getMinX(), py, plot.getMaxX(), py));
		}
	}
	
	private void drawAxes(Graphics2D g2d, Rectangle2D plot, double[] xTicks, double[] yTicks) {
		double xAxisVal = Math.clamp(0, viewport.minX(), viewport.maxX());
		double yAxisVal = Math.clamp(0, viewport.minY(), viewport.maxY());

		double xAxisY = pixelY(yAxisVal, plot);
		double yAxisX = pixelX(xAxisVal, plot);

		g2d.setColor(AXIS_COLOR);
		g2d.setStroke(new BasicStroke(AXES_WIDTH));
		g2d.draw(new Line2D.Double(plot.getMinX(), xAxisY, plot.getMaxX(), xAxisY));
		g2d.draw(new Line2D.Double(yAxisX, plot.getMinY(), yAxisX, plot.getMaxY()));

		g2d.setStroke(new BasicStroke(TICK_WIDTH));
		for (double x : xTicks) {
			double px = pixelX(x, plot);
			g2d.draw(new Line2D.Double(px, xAxisY - TICK_HEIGHT, px, xAxisY + TICK_HEIGHT));
		}

		for (double y : yTicks) {
			double py = pixelY(y, plot);
			g2d.draw(new Line2D.Double(yAxisX - TICK_HEIGHT, py, yAxisX + TICK_HEIGHT, py));
		}
	}

	private void drawData(Graphics2D g2d, Rectangle2D plot) {
		for (GraphSeries s : series) {
			drawPointsAndLines(g2d, plot, s.points(), s.color(), s.color());
		}

		if (!points.isEmpty()) {
			drawPointsAndLines(g2d, plot, points, lineColor, pointColor);
		}
	}

	private void drawPointsAndLines(Graphics2D g2d, Rectangle2D plot, List<Point> data, Color line, Color dot) {
		if (data.isEmpty()) {
			return;
		}

		// Draw lines if applicable
		if (connectPoints && data.size() > 1) {
			g2d.setColor(line);
			g2d.setStroke(new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

			Path2D.Double path = new Path2D.Double();
			boolean first = true;
			for (Point point : data) {
				double px = pixelX(point.x(), plot);
				double py = pixelY(point.y(), plot);

				if (first) {
					path.moveTo(px, py);
					first = false;
				} else {
					path.lineTo(px, py);
				}
			}

			g2d.draw(path);
		}

		g2d.setColor(dot);
		double diameter = POINT_RADIUS * 2;
		int stride = Math.max(1, (int) Math.ceil(data.size() / (double) MAX_DOTS_PER_SERIES));

		for (int i = 0; i < data.size(); i += stride) {
			Point p = data.get(i);
			double px = pixelX(p.x(), plot);
			double py = pixelY(p.y(), plot);
			g2d.fill(new Ellipse2D.Double(px - POINT_RADIUS, py - POINT_RADIUS, diameter, diameter));
		}
	}

	private void drawPlayhead(Graphics2D g2d, Rectangle2D plot) {
		if (!Double.isFinite(playheadTime) || playheadTime < viewport.minX() || playheadTime > viewport.maxX()) {
			return;
		}

		double px = pixelX(playheadTime, plot);
		g2d.setColor(PLAYHEAD_COLOR);
		g2d.setStroke(new BasicStroke(PLAYHEAD_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4f, 4f}, 0));
		g2d.draw(new Line2D.Double(px, plot.getMinY(), px, plot.getMaxY()));

		for (GraphSeries s : series) {
			double val = s.valueAt(playheadTime);
			if (!Double.isFinite(val) || val < viewport.minY() || val > viewport.maxY()) {
				continue;
			}

			double py = pixelY(val, plot);
			
			g2d.setColor(Color.WHITE);
			g2d.fill(new Ellipse2D.Double(px - PLAYHEAD_RADIUS - 1, py - PLAYHEAD_RADIUS - 1, (PLAYHEAD_RADIUS + 1) * 2, (PLAYHEAD_RADIUS + 1) * 2));

			g2d.setColor(s.color());
			g2d.fill(new Ellipse2D.Double(px - PLAYHEAD_RADIUS, py - PLAYHEAD_RADIUS, PLAYHEAD_RADIUS * 2, PLAYHEAD_RADIUS * 2));
		}
	}

	private void drawLabels(Graphics2D g2d, Rectangle2D plot, double[] xTicks, double[] yTicks) {
		FontMetrics fm = g2d.getFontMetrics();
		g2d.setColor(LABEL_COLOR);

		DecimalFormat xFormat = createTickFormat(viewport.xTick());
		DecimalFormat yFormat = createTickFormat(viewport.yTick());

		int xBaseline = (int) Math.round(plot.getMaxY()) + fm.getAscent() + 8;
		int lastXRight = Integer.MIN_VALUE;
		for (double x : xTicks) {
			String text = formatTick(x, viewport.xTick(), xFormat);
			int textWidth = fm.stringWidth(text);
			int left = (int) Math.round(pixelX(x, plot)) - textWidth / 2;
			int drawLeft = (int) Math.clamp(left, LABEL_PADDING, getWidth() - textWidth - LABEL_PADDING);

			if (drawLeft <= lastXRight + LABEL_SPACING) {
				continue;
			}

			g2d.drawString(text, drawLeft, xBaseline);
			lastXRight = drawLeft + textWidth;
		}

		int yRight = (int) Math.round(plot.getMinX()) - 8;
		int lastYTop = Integer.MAX_VALUE;
		for (double y : yTicks) {
			String text = formatTick(y, viewport.yTick(), yFormat);
			int textWidth = fm.stringWidth(text);
			int py = (int) Math.round(pixelY(y, plot));
			int baseline = py + (fm.getAscent() - fm.getDescent()) / 2;
			int top = baseline - fm.getAscent();
			int bottom = baseline + fm.getDescent();

			if (bottom >= lastYTop - LABEL_PADDING) {
				continue;
			}

			g2d.drawString(text, yRight - textWidth, baseline);
			lastYTop = top;
		}
	} 

	private DecimalFormat createTickFormat(double tick) {
		int decimals = Math.max(0, BigDecimal.valueOf(tick).stripTrailingZeros().scale());
		decimals = Math.min(decimals, MAX_DECIMALS);

		DecimalFormat format = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ROOT));
		format.setGroupingUsed(false);
		format.setMinimumFractionDigits(0);
		format.setMaximumFractionDigits(decimals);
		return format;
	}

	private static String formatTick(double val, double tick, DecimalFormat format) {
		if (Math.abs(val) < tick * VALUE_CUTOFF) {
			val = 0;
		}

		return format.format(val);
	}

	private double[] tickValues(double min, double max, double tick) {
		double first = Math.ceil((min - tick * 1e-9) / tick) * tick; // Ensures starting tick is correct
		int count = (int) Math.floor((max - first) / tick + 1e-9) + 1;

		if (count <= 0) {
			return new double[0];
		}

		double[] arr = new double[count];
		for (int i = 0; i < count; i++) {
			arr[i] = first + i * tick;
		}

		return arr;
	}

	private double pixelX(double x, Rectangle2D plot) {
		return plot.getX() + (x - viewport.minX()) / viewport.width() * plot.getWidth();
	}

	private double pixelY(double y, Rectangle2D plot) {
		return plot.getMaxY() - (y - viewport.minY()) / viewport.height() * plot.getHeight();
	}

	private Rectangle2D.Double plotBounds() {
		int width = getWidth() - PADDING_LEFT - PADDING_RIGHT;
		int height = getHeight() - PADDING_TOP - PADDING_BOTTOM;

		if (width <= 1 || height <= 1) {
			return null;
		}

		return new Rectangle2D.Double(PADDING_LEFT, PADDING_TOP, width, height);
	}

	private void zoom(MouseWheelEvent e) {
		Rectangle2D plot = plotBounds();

		if (plot == null || !plot.contains(e.getPoint())) {
			return;
		}

		double rotation = e.getPreciseWheelRotation();
		if (!Double.isFinite(rotation) || rotation == 0) {
			return;
		}

		double anchorX = viewport.minX() + (e.getX() - plot.getMinX()) / plot.getWidth() * viewport.width();
		double anchorY = viewport.minY() + (plot.getMaxY() - e.getY()) / plot.getHeight() * viewport.height();
		double factor = Math.pow(ZOOM_STEP, -rotation);
		GraphViewport zoomed = zoomViewport(viewport, anchorX, anchorY, factor);

		if (zoomed != null) {
			setViewport(zoomed);
			markManualView();
		}

		e.consume();
	}

	static GraphViewport panViewport(GraphViewport viewport, double dx, double dy) {
		if (viewport == null || !Double.isFinite(dx) || !Double.isFinite(dy)) {
			return null;
		}

		double minX = viewport.minX() - dx;
		double maxX = viewport.maxX() - dx;
		double minY = viewport.minY() + dy;
		double maxY = viewport.maxY() + dy;

		if (!validRange(minX, maxX) || !validRange(minY, maxY)) {
			return null;
		}

		return viewport.withRange(minX, maxX, minY, maxY);
	}

	static GraphViewport zoomViewport(GraphViewport viewport, double anchorX, double anchorY, double factor) {
		if (viewport == null || !Double.isFinite(anchorX) || !Double.isFinite(anchorY)
			|| !Double.isFinite(factor) || factor <= 0) {
			return null;
		}

		double width = clampSpan(viewport.width() * factor, anchorX);
		double height = clampSpan(viewport.height() * factor, anchorY);

		if (!Double.isFinite(width) || !Double.isFinite(height)) {
			return null;
		}

		double ratioX = (anchorX - viewport.minX()) / viewport.width();
		double ratioY = (viewport.maxY() - anchorY) / viewport.height();
		double minX = anchorX - ratioX * width;
		double maxX = minX + width;
		double maxY = anchorY + ratioY * height;
		double minY = maxY - height;

		if (!validRange(minX, maxX) || !validRange(minY, maxY)) {
			return null;
		}

		return new GraphViewport(minX, maxX, minY, maxY, niceTick(width), niceTick(height));
	}

	private static double clampSpan(double span, double anchor) {
		if (!Double.isFinite(span) || span <= 0) {
			return Double.NaN;
		}

		double min = Math.max(MIN_VIEWPORT_SPAN, Math.abs(anchor) * 1e-12);
		return Math.clamp(span, min, MAX_VIEWPORT_SPAN);
	}

	private static boolean validRange(double min, double max) {
		return Double.isFinite(min) && Double.isFinite(max) && min < max;
	}

	private void markManualView() {
		World current = Simulator.getInstance().getWorld();

		if (current != null) {
			current.setAutoFit(false);
		}
	}

	public void setPoints(Point... points) {
		setPoints(List.of(points));
	}

	// Adds points to the local stored points list; null points are ignored
	public void setPoints(List<Point> points) {
		Objects.requireNonNull(points);

		List<Point> copy = new ArrayList<>(points.size());
		for (Point point : points) {
			copy.add(Objects.requireNonNull(point));
		}

		this.points.clear();
		this.points.addAll(copy);

		repaint();
	}

	public void addPoint(Point point) {
		Objects.requireNonNull(point);
		points.add(point);
		repaint();
	}

	public List<Point> getPoints() {
		return List.copyOf(points);
	}

	public void setViewport(double minX, double maxX, double minY, double maxY, double xTick, double yTick) {
		setViewport(new GraphViewport(minX, maxX, minY, maxY, xTick, yTick));
	}

	public void setRange(double minX, double maxX, double minY, double maxY) {
		setViewport(viewport.withRange(minX, maxX, minY, maxY));
	}

	public void setYRange(double minY, double maxY) {
		if (!Double.isFinite(minY) || !Double.isFinite(maxY) || minY >= maxY) {
			throw new IllegalArgumentException("Graph: invalid y range");
		}

		setViewport(viewport.minX(), viewport.maxX(), minY, maxY, viewport.xTick(), niceTick(maxY - minY));
	}

	public void setTicks(double xTick, double yTick) {
		setViewport(viewport.withTicks(xTick, yTick));
	}

	public void setViewport(GraphViewport viewport) {
		this.viewport = Objects.requireNonNull(viewport);
		repaint();
	}

	public GraphViewport getViewport() {
		return viewport;
	}

	public void setShowGrid(boolean showGrid) {
		this.showGrid = showGrid;
		repaint();
	}

	public boolean isShowingGrid() {
		return showGrid;
	}

	public void setConnectPoints(boolean connectPoints) {
		this.connectPoints = connectPoints;
		repaint();
	}

	public boolean isPointsConnected() {
		return connectPoints;
	}

	public void setSeries(List<GraphSeries> series) {
		Objects.requireNonNull(series);
		
		List<GraphSeries> copy = new ArrayList<>(series.size());
		for (GraphSeries s : series) {
			copy.add(Objects.requireNonNull(s));
		}

		this.series.clear();
		this.series.addAll(copy);
		repaint();
	}

	public void clearSeries() {
		series.clear();
		repaint();
	}

	public List<GraphSeries> getSeries() {
		return List.copyOf(series);
	}

	public void setPlaybackTime(double time) {
		this.playheadTime = time;
		repaint();
	}

	public void clearPlayhead() {
		this.playheadTime = Double.NaN;
		repaint();
	}

	public void autoFit() {
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (GraphSeries s : series) {
			for (Point p : s.points()) {
				if (!Double.isFinite(p.x()) || !Double.isFinite(p.y())) {
					continue;
				}

				minX = Math.min(minX, p.x());
				maxX = Math.max(maxX, p.x());
				minY = Math.min(minY, p.y());
				maxY = Math.max(maxY, p.y());
			}
		}

		for (Point p : points) {
			if (!Double.isFinite(p.x()) || !Double.isFinite(p.y())) {
				continue;
			}

			minX = Math.min(minX, p.x());
			maxX = Math.max(maxX, p.x());
			minY = Math.min(minY, p.y());
			maxY = Math.max(maxY, p.y());
		}

		if (!Double.isFinite(minX) || !Double.isFinite(minY)) {
			setViewport(DEFAULT_VIEWPORT);
			return;
		}

		// Expand a zero-width range so the viewport remains valid
		if (maxX - minX < 1e-12) {
			double center = (minX + maxX) / 2;
			double half = Math.max(0.5, Math.abs(center) * 0.05);
			minX = center - half;
			maxX = center + half;
		}

		// Expand a zero-height range so the viewport remains valid
		if (maxY - minY < 1e-12) {
			double center = (minY + maxY) / 2;
			double half = Math.max(0.5, Math.abs(center) * 0.05);
			minY = center - half;
			maxY = center + half;
		}

		double padX = (maxX - minX) * 0.05;
		double padY = (maxY - minY) * 0.05;

		double startX = minX - padX;
		double endX = maxX + padX;
		double startY = minY - padY;
		double endY = maxY + padY;

		setViewport(startX, endX, startY, endY, niceTick(endX - startX), niceTick(endY - startY));
	}

	private static double niceTick(double span) {
		if (span <= 0 || !Double.isFinite(span)) {
			return 1;
		}

		double rough = span / 10.0;
		double mag = Math.pow(10, Math.floor(Math.log10(rough)));
		double norm = rough / mag;

		if (norm <= 1) {
			return mag;
		}

		if (norm <= 2) {
			return 2 * mag;
		}

		if (norm <= 5) {
			return 5 * mag;
		}

		return 10 * mag;
	}
}

record GraphViewport(double minX, double maxX, double minY, double maxY, double xTick, double yTick) {
	public GraphViewport {
		if (minX >= maxX) {
			throw new IllegalArgumentException("GraphViewport: minX must be less than maxX");
		}

		if (minY >= maxY) {
			throw new IllegalArgumentException("GraphViewport: minY must be less than maxY");
		}

		if (xTick <= 0 || yTick <= 0) {
			throw new IllegalArgumentException("GraphViewport: tick sizes must be greater than 0");
		}
	}

	public GraphViewport withRange(double minX, double maxX, double minY, double maxY) {
		return new GraphViewport(minX, maxX, minY, maxY, xTick, yTick);
	}

	public GraphViewport withTicks(double xTick, double yTick) {
		return new GraphViewport(minX, maxX, minY, maxY, xTick, yTick);
	}

	public double width() {
		return maxX - minX;
	}

	public double height() {
		return maxY - minY;
	}
}

// Represents a series of points
record GraphSeries(String label, List<Point> points, Color color) {
	public GraphSeries {
		points = new ArrayList<>(points);
		points.sort((a, b) -> (int) (a.x() - b.x())); // Ensure points are in order by x-axis
		points = List.copyOf(points);
		Objects.requireNonNull(color);
	}

	// Linearly interpolates between points
	public double valueAt(double x) {
		if (x < points.getFirst().x() || x > points.getLast().x()) {
			return Double.NaN;
		}

		// Find two adjacent points and linearize between them; Linear search is fine here
		Point p1 = null;
		Point p2 = null;
		for (int i = 1; i < points.size(); i++) {
			if (x >= points.get(i - 1).x() && x <= points.get(i).x()) {
				p1 = points.get(i - 1);
				p2 = points.get(i);
				break;
			}
		}

		if (p1 == null || p2 == null) {
			return Double.NaN;
		}

		return p1.y() + (p2.y() - p1.y()) / (p2.x() - p1.x()) * (x - p1.x());
	}
}

// Shows controls for running nodes and scrubbing through the resulting motion
class Toolbar extends JPanel {
	private enum State {
		IDLE,
		RUNNING,
		CURRENT,
		STALE
	}

	private static final int PLAYBACK_DELAY_MS = Math.max(1, 1000 / Simulator.TARGET_FRAME_RATE);
	private static final int AUTO_RUN_DELAY_MS = 250;
	private static final int TOOLBAR_HEIGHT = 48;
	private static final int STATUS_WIDTH = 170;

	private final JButton runButton;
	private final JButton playButton;
	private final JButton focusButton;
	private final JButton homeButton;
	private final Timeline timeline;
	private final JLabel statusLabel;
	private final Timer playbackTimer;
	private final Timer autoRunTimer;

	private State state;
	private World world;
	private long syncedRevision;
	private long pendingRevision;
	private long failedRevision;
	private boolean autoRefreshArmed;
	private double playhead;
	private long lastTickNanos;
	private SwingWorker<Report, Void> worker;

	private static Toolbar instance;

	private Toolbar() {
		setLayout(new BorderLayout(6, 0));
		setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		setPreferredSize(new Dimension(0, TOOLBAR_HEIGHT));

		this.runButton = new JButton("Run");
		runButton.setEnabled(false);
		runButton.setToolTipText("Compile and run all nodes");

		this.playButton = new JButton("Play");
		playButton.setEnabled(false);

		this.focusButton = new JButton("Focus");
		focusButton.setEnabled(false);
		focusButton.setToolTipText("Fit graph and re-enable automatic focus");
		
		this.homeButton = new JButton("Home");
		homeButton.setToolTipText("Normalizes axis to the default view");

		this.timeline = new Timeline();

		this.statusLabel = new JLabel("No world");
		statusLabel.setPreferredSize(new Dimension(STATUS_WIDTH, 20));

		JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		controls.add(runButton);
		controls.add(playButton);

		JPanel info = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		info.add(statusLabel);
		info.add(focusButton);
		info.add(homeButton);

		add(controls, BorderLayout.WEST);
		add(timeline, BorderLayout.CENTER);
		add(info, BorderLayout.EAST);

		runButton.addActionListener(e -> run());
		playButton.addActionListener(e -> togglePlayback());
		focusButton.addActionListener(e -> focus());
		homeButton.addActionListener(e -> home());
		timeline.setListener(this::seek);

		this.playbackTimer = new Timer(PLAYBACK_DELAY_MS, e -> tick());
		playbackTimer.setCoalesce(true);

		this.autoRunTimer = new Timer(AUTO_RUN_DELAY_MS, e -> startAutoRun());
		autoRunTimer.setRepeats(false);
		autoRunTimer.setCoalesce(true);

		this.state = State.IDLE;
		this.world = null;
		this.syncedRevision = -1;
		this.pendingRevision = -1;
		this.failedRevision = -1;
		this.autoRefreshArmed = false;
		this.playhead = 0;
	}

	public static Toolbar getInstance() {
		if (instance == null) {
			instance = new Toolbar();
		}

		return instance;
	}

	public void refresh() {
		World current = Simulator.getInstance().getWorld();

		if (current != world) {
			reset(current);
			return;
		}

		if (current == null) {
			updateControls();
			return;
		}

		if (current.getNodes().isEmpty()) {
			if (state != State.IDLE || !Graph.getInstance().getSeries().isEmpty()) {
				cancelPendingRun();
				state = State.IDLE;
				syncedRevision = current.getRevision();
				failedRevision = -1;
				Graph.getInstance().clearSeries();
				Graph.getInstance().clearPlayhead();
				playButton.setText("Play");
				setStatus("No nodes");
			}

			updateControls();
			return;
		}

		long revision = current.getRevision();

		if (state == State.RUNNING) {
			if (revision != pendingRevision) {
				pendingRevision = revision;

				if (worker != null) {
					worker.cancel(true);
				}
			}

			updateControls();
			return;
		}

		if (revision != syncedRevision && revision != pendingRevision && revision != failedRevision) {
			if (autoRefreshArmed) {
				scheduleAutoRun(revision);
			} else if (state == State.IDLE) {
				syncedRevision = revision;
				setStatus("Ready");
			}
		}

		updateControls();
	}

	private void run() {
		startRun(false);
	}

	private void scheduleAutoRun(long revision) {
		if (!autoRefreshArmed || revision == syncedRevision || revision == failedRevision) {
			return;
		}

		if (worker != null) {
			pendingRevision = revision;
			return;
		}

		pendingRevision = revision;
		state = State.STALE;
		setStatus("Updating...");
		autoRunTimer.restart();
		updateControls();
	}

	private void startAutoRun() {
		autoRunTimer.stop();

		World current = Simulator.getInstance().getWorld();
		if (!autoRefreshArmed || current == null || current != world || current.getNodes().isEmpty()) {
			return;
		}

		long revision = current.getRevision();
		if (revision == syncedRevision || revision == failedRevision) {
			pendingRevision = -1;
			return;
		}

		if (worker != null) {
			pendingRevision = revision;
			return;
		}

		startRun(true);
	}

	private void startRun(boolean automatic) {
		World target = Simulator.getInstance().getWorld();

		if (target == null || target.getNodes().isEmpty()) {
			setStatus(target == null ? "No world" : "No nodes");
			return;
		}

		if (worker != null) {
			pendingRevision = target.getRevision();
			return;
		}

		long revision = target.getRevision();

		if (!automatic) {
			autoRefreshArmed = true;
			failedRevision = -1;
			autoRunTimer.stop();
		}

		stopPlayback();

		state = State.RUNNING;
		world = target;
		pendingRevision = revision;

		timeline.setEnabled(false);
		playButton.setEnabled(false);
		runButton.setEnabled(false);
		setStatus(automatic ? "Updating..." : "Running...");

		worker = new SwingWorker<Report, Void>() {
			@Override
			protected Report doInBackground() {
				return target.run();
			}

			@Override
			protected void done() {
				try {
					if (isCancelled()) {
						return;
					}

					Report report = get();

					if (Simulator.getInstance().getWorld() != target || target.getRevision() != revision) {
						return;
					}

					display(report, target);
				} catch (Exception e) {
					if (!isCancelled()) {
						failRun(revision, e);
					}
				} finally {
					if (worker == this) {
						worker = null;
						finishRun(target);
					}
				}
			}
		};

		worker.execute();
	}

	private void failRun(long revision, Exception e) {
		failedRevision = revision;
		pendingRevision = -1;
		state = State.STALE;

		String message = e.getMessage();
		setStatus(message == null ? "Update failed" : "Update failed: " + message);
	}

	private void finishRun(World target) {
		World current = Simulator.getInstance().getWorld();

		if (current != target) {
			reset(current);
			return;
		}

		if (current != null && !current.getNodes().isEmpty() && autoRefreshArmed) {
			long revision = current.getRevision();

			if (revision != syncedRevision && revision != failedRevision) {
				scheduleAutoRun(revision);
			}
		}

		updateControls();
	}

	private void display(Report report, World target) {
		List<GraphSeries> plotted = new ArrayList<>();

		for (Result result : report.results()) {
			Node node = result.node();
			List<Double> values = result.valuesFor(node.getType());
			List<Double> times = result.times();

			if (values.isEmpty() || times.isEmpty()) {
				continue;
			}

			int size = Math.min(values.size(), times.size());
			List<Point> points = new ArrayList<>(size);

			for (int i = 0; i < size; i++) {
				double t = times.get(i);
				double value = values.get(i);

				if (Double.isFinite(t) && Double.isFinite(value)) {
					points.add(new Point(t, value));
				}
			}

			if (points.isEmpty()) {
				continue;
			}

			String label = "Node #" + node.getId() + " (" + keyFor(node.getType()) + ")";
			plotted.add(new GraphSeries(label, points, node.getColor()));
		}

		Graph graph = Graph.getInstance();
		double start = target.getStart();
		double end = target.getEnd();
		double previousPlayhead = playhead;
		boolean hasData = !plotted.isEmpty();

		if (hasData) {
			graph.setSeries(plotted);
			graph.setConnectPoints(true);

			if (target.isAutoFit()) {
				graph.autoFit();
			}
		} else {
			graph.clearSeries();
			graph.clearPlayhead();
		}

		timeline.setRange(start, end);

		playhead = Math.clamp(previousPlayhead, start, end);
		timeline.setTime(playhead);
		applyPlayhead();

		timeline.setEnabled(hasData && end > start);
		playButton.setText("Play");
		playButton.setEnabled(hasData && end > start);

		state = hasData ? State.CURRENT : State.IDLE;
		syncedRevision = target.getRevision();
		pendingRevision = -1;
		failedRevision = -1;

		String summary = plotted.isEmpty() ? "No plottable data" : "Done: " + plotted.size() + " node" + (plotted.size() == 1 ? "" : "s");

		if (!report.errors().isEmpty()) {
			summary += " (" + report.errors().size() + " error" + (report.errors().size() == 1 ? "" : "s") + ")";
		}

		setStatus(summary);

		if (!report.errors().isEmpty()) {
			statusLabel.setToolTipText(String.join("\n", report.errors()));
		}

		updateControls();
	}

	private void cancelPendingRun() {
		autoRunTimer.stop();

		if (worker != null) {
			worker.cancel(true);
			worker = null;
		}

		pendingRevision = -1;
	}

	private void reset(World current) {
		stopPlayback();
		cancelPendingRun();

		world = current;
		state = State.IDLE;
		syncedRevision = current == null ? -1 : current.getRevision();
		failedRevision = -1;
		autoRefreshArmed = false;
		playhead = current == null ? 0 : current.getStart();

		Graph.getInstance().clearSeries();
		Graph.getInstance().clearPlayhead();

		if (current != null && !current.isAutoFit()) {
			Graph.getInstance().setYRange(current.getYMin(), current.getYMax());
		}

		timeline.setRange(0, 1);
		timeline.setTime(0);
		timeline.setEnabled(false);
		playButton.setText("Play");

		if (current == null) {
			setStatus("No world");
		} else if (current.getNodes().isEmpty()) {
			setStatus("No nodes");
		} else {
			setStatus("Ready");
		}

		updateControls();
	}

	private void updateControls() {
		boolean hasNodes = world != null && !world.getNodes().isEmpty();
		boolean current = state == State.CURRENT;
		boolean validRange = world != null && world.getEnd() > world.getStart();

		runButton.setEnabled(hasNodes && state != State.RUNNING);
		focusButton.setEnabled(world != null && state != State.RUNNING);
		timeline.setEnabled(current && validRange);
		playButton.setEnabled(current && validRange);
	}

	private void focus() {
		World current = Simulator.getInstance().getWorld();

		if (current != null) {
			current.setAutoFit(true);
		}

		Graph.getInstance().autoFit();
	}

	private void home() {
		Graph.getInstance().setViewport(Graph.DEFAULT_VIEWPORT);
	}

	private void togglePlayback() {
		if (state != State.CURRENT) {
			return;
		}

		if (playbackTimer.isRunning()) {
			stopPlayback();
			return;
		}

		if (world == null || world.getEnd() <= world.getStart()) {
			return;
		}

		if (playhead >= timeline.getMax()) {
			playhead = timeline.getMin();
			applyPlayhead();
		}

		lastTickNanos = System.nanoTime();
		playbackTimer.start();
		playButton.setText("Pause");
	}

	private void stopPlayback() {
		playbackTimer.stop();
		playButton.setText("Play");
	}

	private void tick() {
		if (state != State.CURRENT) {
			stopPlayback();
			return;
		}

		long now = System.nanoTime();
		double delta = (now - lastTickNanos) / 1e9;
		lastTickNanos = now;

		double max = timeline.getMax();
		playhead = Math.min(max, playhead + delta);
		applyPlayhead();

		if (playhead >= max) {
			stopPlayback();
		}
	}

	private void seek(double time) {
		if (state != State.CURRENT) {
			return;
		}

		stopPlayback();
		playhead = Math.clamp(time, timeline.getMin(), timeline.getMax());
		applyPlayhead();
	}

	private void applyPlayhead() {
		timeline.setTime(playhead);
		Graph.getInstance().setPlaybackTime(playhead);

		if (world != null) {
			world.setTime(playhead);
		}
	}

	private void setStatus(String text) {
		statusLabel.setText(text);
		statusLabel.setToolTipText(text);
	}

	private static String keyFor(Node.FunctionType type) {
		if (type == null) {
			return "-";
		}

		return switch (type) {
			case POSITION -> "x";
			case VELOCITY -> "v";
			case ACCELERATION -> "a";
		};
	}
}

// Horizontal scrub control used by Toolbar
class Timeline extends JComponent {
	private static final Color TRACK = new Color(190, 190, 190);
	private static final Color TRACK_FILL = new Color(70, 130, 200);
	private static final Color KNOB = new Color(30, 90, 160);
	private static final Color TEXT = new Color(64, 64, 64);
	private static final Color TEXT_DISABLED = new Color(150, 150, 150);
	private static final int SIDE_PADDING = 8;
	private static final int TRACK_HEIGHT = 4;
	private static final int KNOB_RADIUS = 5;

	private double min = 0;
	private double max = 1;
	private double time = 0;
	private Consumer<Double> listener = t -> {};

	Timeline() {
		setPreferredSize(new Dimension(320, 36));
		setMinimumSize(new Dimension(120, 30));
		setOpaque(false);
		setEnabled(false);

		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				seek(e);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				seek(e);
			}
		};

		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	public void setListener(Consumer<Double> listener) {
		this.listener = listener == null ? t -> {} : listener;
	}

	public void setRange(double min, double max) {
		if (!Double.isFinite(min) || !Double.isFinite(max) || max <= min) {
			this.min = Double.isFinite(min) ? min : 0;
			this.max = this.min + 1;
			this.time = this.min;
		} else {
			this.min = min;
			this.max = max;
			this.time = Math.clamp(this.time, min, max);
		}

		repaint();
	}

	public void setTime(double time) {
		this.time = Math.clamp(time, min, max);
		repaint();
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		repaint();
	}

	private void seek(MouseEvent e) {
		if (!isEnabled() || getWidth() <= SIDE_PADDING * 2) {
			return;
		}

		double x = Math.clamp(e.getX(), SIDE_PADDING, getWidth() - SIDE_PADDING);
		double ratio = (x - SIDE_PADDING) / (double) (getWidth() - SIDE_PADDING * 2);
		double value = min + ratio * (max - min);

		setTime(value);
		listener.accept(value);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int left = SIDE_PADDING;
		int right = getWidth() - SIDE_PADDING;
		if (right <= left) {
			return;
		}

		int trackY = Math.max(2, getHeight() / 2 - TRACK_HEIGHT / 2);

		g2d.setColor(TRACK);
		g2d.fillRoundRect(left, trackY, right - left, TRACK_HEIGHT, TRACK_HEIGHT, TRACK_HEIGHT);

		double ratio = (max - min) <= 0 ? 0 : (time - min) / (max - min);
		int knobX = left + (int) Math.round(ratio * (right - left));

		if (isEnabled()) {
			g2d.setColor(TRACK_FILL);
			g2d.fillRoundRect(left, trackY, Math.max(0, knobX - left), TRACK_HEIGHT, TRACK_HEIGHT, TRACK_HEIGHT);
		}

		g2d.setColor(isEnabled() ? KNOB : TRACK);
		g2d.fillOval(knobX - KNOB_RADIUS, trackY + TRACK_HEIGHT / 2 - KNOB_RADIUS, KNOB_RADIUS * 2, KNOB_RADIUS * 2);

		g2d.setColor(isEnabled() ? TEXT : TEXT_DISABLED);
		FontMetrics fm = g2d.getFontMetrics();
		String minText = format(min);
		String maxText = format(max);
		int baseline = getHeight() - 4;

		g2d.drawString(minText, left, baseline);
		g2d.drawString(maxText, right - fm.stringWidth(maxText), baseline);
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}
}
