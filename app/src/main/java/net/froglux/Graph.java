package net.froglux;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

import javax.swing.JPanel;

// A simple graph that plots points with scalable axis and can conform to varying sizes
public class Graph extends JPanel {
	// Colors
	private static final Color PANEL_BACKGROUND = new Color(255, 255, 255);
	private static final Color PLOT_PACKGROUND = new Color(255, 255, 255);
	private static final Color GRID_COLOR = new Color(128, 128, 128);
	private static final Color AXIS_COLOR = new Color(0, 0, 0);
	private static final Color BORDER_COLOR = new Color(192, 192, 192);
	private static final Color LABEL_COLOR = new Color(64, 64, 64);
	private static Color pointColor = new Color(0, 0, 0);
	private static Color lineColor = new Color(0, 0, 0);
	
	// Geo
	private static final int POINT_RADIUS = 4;
	private static final float LINE_WIDTH = 1.5f;
	private static final float AXES_WIDTH = 1.5f;
	private static final float TICK_WIDTH = 1f;
	private static final float TICK_HEIGHT = 3f;
	private static final float BORDER_WIDTH = 1f;

	// Viewport
	private static final int PADDING_TOP = 16;
	private static final int PADDING_BOTTOM = 36;
	private static final int PADDING_LEFT = 36;
	private static final int PADDING_RIGHT = 16;
	private static final GraphViewport DEFAULT_VIEWPORT = new GraphViewport(-10, 10, -10, 10, 1, 1);

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

	private Graph() {
		setBackground(PANEL_BACKGROUND);
		setOpaque(true);
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

	// TODO: Draw graph view with lines and plot points by getting processed points from world
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int width = getWidth() - PADDING_LEFT - PADDING_RIGHT;
		int height = getHeight() - PADDING_TOP - PADDING_BOTTOM;
		if (width <= 1 || height <= 1) {
			return;
		}

		Rectangle2D.Double plot = new Rectangle2D.Double(PADDING_LEFT, PADDING_TOP, width, height);
		double[] xTicks = tickValues(viewport.minX(), viewport.maxX(), viewport.xTick());
		double[] yTicks = tickValues(viewport.minY(), viewport.maxY(), viewport.yTick());

		g2d.setColor(PLOT_PACKGROUND);
		g2d.fill(plot);

		if (showGrid) {
			drawGrid(g2d, plot, xTicks, yTicks);
		} 

		drawAxes(g2d, plot, xTicks, yTicks);
		drawData(g2d, plot);

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
		if (points.isEmpty()) {
			return;
		}

		if (connectPoints && points.size() > 1) {
			g2d.setColor(lineColor);
			g2d.setStroke(new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

			Path2D.Double path = new Path2D.Double();
			boolean first = true;
			for (Point point : points) {
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

		g2d.setColor(pointColor);
		double diameter = POINT_RADIUS * 2;
		for (Point point : points) {
			double px = pixelX(point.x(), plot);
			double py = pixelY(point.y(), plot);
			g2d.fill(new Ellipse2D.Double(px - POINT_RADIUS, py - POINT_RADIUS, diameter, diameter));
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

// Shows buttons for user to click on to play/pause, jump to time
class Toolbar extends JPanel {

}
