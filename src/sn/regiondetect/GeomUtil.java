package sn.regiondetect;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GeomUtil {

	/**
	 * draw lines on regions
	 * 
	 * @param intersectLines
	 * @param regions
	 * @param l
	 * @return
	 * @throws Exception
	 */
	static public List<Line2D> lineRegion(List<Line2D> intersectLines,
			Region p, Line2D l) throws Exception {
		// System.out.print("line : " + lineCount + "\n");
		Set<Point2D> intersections = GeomUtil.getIntersections(p, l);

		List<Point2D> intersectionArray = new ArrayList<Point2D>();
		for (Iterator<Point2D> it = intersections.iterator(); it.hasNext();) {
			intersectionArray.add(it.next());
		}

		// sort vertically
		for (int i = 0; i < intersectionArray.size() - 1; i++) {
			for (int j = intersectionArray.size() - 1; j > 0; j--) {
				if (intersectionArray.get(j).getY() < intersectionArray.get(
						j - 1).getY()) {
					Point2D temPt = intersectionArray.get(j);
					intersectionArray.set(j, intersectionArray.get(j - 1));
					intersectionArray.set(j - 1, temPt);
				}
			}
		}

		// System.out.println("Intersection: " +
		// intersections.size());
		Point2D start = new Point2D.Double(), end = new Point2D.Double();
		boolean draw = false, hasStart = false, hasEnd = false;
		for (Iterator<Point2D> it = intersectionArray.iterator(); it.hasNext();) {
			Point2D point = it.next();
			if (!hasStart) {
				start = point;
				hasStart = true;
			}

			else if (hasStart && !hasEnd) {
				end = point;
				hasEnd = true;
			}

			if (hasStart && hasEnd) {
				draw = true;
				hasStart = false;
				hasEnd = false;
			}

			// boolean addLine = false;
			if (draw) {
				intersectLines = GeomUtil
						.extendLine(intersectLines, start, end);
				draw = false;
			}
		}
		return intersectLines;
	}

	static public List<Line2D> lineJumpHole(List<Line2D> intersectLines,
			Region p, Line2D l) throws Exception {
		Set<Point2D> intersections;
		intersections = GeomUtil.getIntersections(p, l);

		List<Point2D> intersectionArray = new ArrayList<Point2D>();
		for (Iterator<Point2D> it = intersections.iterator(); it.hasNext();) {
			intersectionArray.add(it.next());
		}

		// sort vertically
		for (int i = 0; i < intersectionArray.size() - 1; i++) {
			for (int j = intersectionArray.size() - 1; j > 0; j--) {
				if (intersectionArray.get(j).getY() < intersectionArray.get(
						j - 1).getY()) {
					Point2D temPt = intersectionArray.get(j);
					intersectionArray.set(j, intersectionArray.get(j - 1));
					intersectionArray.set(j - 1, temPt);
				}
			}
		}

		// System.out.println("Intersection: " +
		// intersections.size());
		Point2D start = new Point2D.Double(), end = new Point2D.Double();
		boolean drawHole = false, hasStart = false, hasEnd = false;
		for (Iterator<Point2D> it = intersectionArray.iterator(); it.hasNext();) {
			Point2D point = it.next();
			if (!hasStart) {
				start = point;
				hasStart = true;
			}

			else if (hasStart && !hasEnd) {
				end = point;
				hasEnd = true;
			}

			if (hasStart && hasEnd) {
				drawHole = true;
				hasStart = false;
				hasEnd = false;
			}

			// boolean addLine = false;
			if (drawHole) {
				for (int i = 0; i < intersectLines.size(); i++) {
					Line2D il = intersectLines.get(i);
					if (il.getY1() <= start.getY()
							&& il.getY2() >= start.getY()
							&& il.getY2() <= end.getY()) {
						il.setLine(il.getP1(), start);
					}

					else if (il.getY1() >= start.getY()
							&& il.getY1() <= end.getY()
							&& il.getY2() >= end.getY()) {
						il.setLine(end, il.getP2());
					} else if (il.getY1() >= start.getY()
							&& il.getY2() <= end.getY()) {
						intersectLines.remove(i);
						i--;
					} else if (il.getY1() <= start.getY()
							&& il.getY2() >= end.getY()) {
						Point2D temEnd = il.getP2();
						il.setLine(il.getP1(), start);
						// System.out.println(intersectLines.size());
						intersectLines.add(0, new Line2D.Double(end, temEnd));
						// System.out.println(intersectLines.size()
						// +" "
						// + lineC + "\n");
					}
				}
				drawHole = false;
			}
			// System.out.println("Intersection: " +
			// point.toString());

		}
		return intersectLines;

	}

	static public List<Line2D> extendLine(List<Line2D> intersectLines,
			Point2D start, Point2D end) {
		boolean addLine = false;
		for (Line2D il : intersectLines) {
			if (!il.intersectsLine(new Line2D.Double(start, end))) {
				addLine = true;
			}
			if (start.getY() <= il.getY1() && end.getY() >= il.getY1()
					&& end.getY() <= il.getY2()) {
				il.setLine(start, il.getP2());
				addLine = false;
				mergeLine(intersectLines);
				break;
			}

			else if (start.getY() >= il.getY1() && start.getY() <= il.getY2()
					&& end.getY() >= il.getY2()) {
				il.setLine(il.getP1(), end);
				addLine = false;
				mergeLine(intersectLines);
				break;
			}

			else if (il.getY1() >= start.getY() && il.getY2() <= end.getY()) {
				il.setLine(start, end);
				addLine = false;
				mergeLine(intersectLines);
				break;
			}

			else if (!(il.getY2() < start.getY() || il.getY1() > end.getY())) {
				addLine = false;
			}

		}

		if (intersectLines.isEmpty() || addLine) {
			intersectLines.add(new Line2D.Double(start, end));
			mergeLine(intersectLines);
			// System.out.print(addLine+"\n");
		}

		return intersectLines;
	}

	/**
	 * Merge overlapping lines
	 * 
	 * @param intersectLines
	 *            a list of lines with the same function
	 * @return
	 */
	static public List<Line2D> mergeLine(List<Line2D> intersectLines) {
		boolean merged = false;
		for (int i = 0; i < intersectLines.size();) {
			for (int j = i + 1; j < intersectLines.size(); j++) {
				Line2D l1 = intersectLines.get(i);
				Line2D l2 = intersectLines.get(j);
				if (l1.getY1() <= l2.getY1() && l1.getY2() >= l2.getY1()
						&& l1.getY2() <= l2.getY2()) {
					l2.setLine(l1.getP1(), l2.getP2());
					// mergeLine(intersectLines);
					intersectLines.remove(i);
					// System.out.print("remove : " + i + "\n");
					merged = true;
					break;
				}

				else if (l1.getY1() >= l2.getY1() && l1.getY1() <= l2.getY2()
						&& l1.getY2() >= l2.getY2()) {
					l2.setLine(l2.getP1(), l1.getP2());
					// mergeLine(intersectLines);
					intersectLines.remove(i);
					// System.out.print("remove : " + i + "\n");
					merged = true;
					break;
				}

				else if (l2.getY1() >= l1.getY1() && l2.getY2() <= l1.getY2()) {
					l2.setLine(l1);
					// mergeLine(intersectLines);
					intersectLines.remove(i);
					// System.out.print("remove : " + i + "\n");
					merged = true;
					break;
				}

				else if (l1.getY1() >= l2.getY1() && l1.getY2() <= l2.getY2()) {
					intersectLines.remove(i);
					merged = true;
					break;
				}
			}
			if (merged == true) {
				i = 0;
				merged = false;
			} else
				i++;
		}

		// if (merged) {
		// mergeLine(intersectLines);
		// }

		return intersectLines;
	}

	/**
	 * Find intersections of a polygon with a line viewed from
	 * http://stackoverflow.com/a/5185725
	 * 
	 * @param poly
	 * @param line
	 * @return
	 * @throws Exception
	 */
	public static Set<Point2D> getIntersections(final Polygon poly,
			final Line2D line) throws Exception {

		// Getting an iterator along the polygon path
		final PathIterator polyIt = poly.getPathIterator(null);

		// Double array with length 6 needed by iterator
		final double[] coords = new double[6];

		// First point (needed for closing polygon path)
		final double[] firstCoords = new double[2];

		// Previously visited point
		final double[] lastCoords = new double[2];

		// List to hold found intersections
		final Set<Point2D> intersections = new HashSet<Point2D>();

		// Getting the first coordinate pair
		polyIt.currentSegment(firstCoords);

		// Priming the previous coordinate pair
		lastCoords[0] = firstCoords[0];
		lastCoords[1] = firstCoords[1];
		polyIt.next();
		while (!polyIt.isDone()) {
			final int type = polyIt.currentSegment(coords);
			switch (type) {
			case PathIterator.SEG_LINETO: {
				final Line2D currentLine = new Line2D.Double(lastCoords[0],
						lastCoords[1], coords[0], coords[1]);
				if (currentLine.intersectsLine(line))
					intersections.add(getIntersection(currentLine, line));
				lastCoords[0] = coords[0];
				lastCoords[1] = coords[1];
				break;
			}
			case PathIterator.SEG_CLOSE: {
				final Line2D.Double currentLine = new Line2D.Double(coords[0],
						coords[1], firstCoords[0], firstCoords[1]);
				if (currentLine.intersectsLine(line))
					intersections.add(getIntersection(currentLine, line));
				break;
			}
			default: {
				throw new Exception("Unsupported PathIterator segment type.");
			}
			}
			polyIt.next();
		}
		return intersections;

	}

	public static Point2D getIntersection(final Line2D line1, final Line2D line2) {

		final double x1, y1, x2, y2, x3, y3, x4, y4;
		x1 = line1.getX1();
		y1 = line1.getY1();
		x2 = line1.getX2();
		y2 = line1.getY2();
		x3 = line2.getX1();
		y3 = line2.getY1();
		x4 = line2.getX2();
		y4 = line2.getY2();
		final double x = ((x2 - x1) * (x3 * y4 - x4 * y3) - (x4 - x3)
				* (x1 * y2 - x2 * y1))
				/ ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
		final double y = ((y3 - y4) * (x1 * y2 - x2 * y1) - (y1 - y2)
				* (x3 * y4 - x4 * y3))
				/ ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));

		return new Point2D.Double(x, y);

	}

	public static void drawLine(BufferedImage bi, Line2D il, int lineC,
			int subLineC, boolean showNum) {

		Graphics2D g2d = (Graphics2D) bi.createGraphics();
		if (showNum) {
			g2d.setFont(new Font("Helvetica", Font.PLAIN, 10));
			g2d.setColor(Color.ORANGE);
			g2d.drawString("s" + String.valueOf(lineC), (int) il.getX1(),
					(int) il.getY1());
			g2d.drawString(":" + String.valueOf(subLineC),
					(int) il.getX1() + 15, (int) il.getY1());
			g2d.setColor(Color.PINK);
			g2d.drawString("e" + String.valueOf(lineC), (int) il.getX2(),
					(int) il.getY2());
			g2d.drawString(":" + String.valueOf(subLineC),
					(int) il.getX2() + 15, (int) il.getY2());
		}
		g2d.setColor(Color.BLACK);
		g2d.drawLine((int) il.getX1(), (int) il.getY1(), (int) il.getX2(),
				(int) il.getY2());

	}

}
