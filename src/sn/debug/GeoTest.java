package sn.debug;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GeoTest {

	public static GeneralPath getRoundedGeneralPath(Polygon polygon) {
		List<int[]> l = new ArrayList<int[]>();
		for (int i = 0; i < polygon.npoints; i++) {
			l.add(new int[] { polygon.xpoints[i], polygon.ypoints[i] });
		}
		return getRoundedGeneralPath(l);
	}

	public static GeneralPath getRoundedGeneralPath(List<int[]> l) {
		List<Point> list = new ArrayList<Point>();
		for (int[] point : l) {
			list.add(new Point(point[0], point[1]));
		}
		return getRoundedGeneralPathFromPoints(list);
	}

	public static GeneralPath getRoundedGeneralPathFromPoints(List<Point> l) {
		l.add(l.get(0));
		l.add(l.get(1));
		GeneralPath p = new GeneralPath();
		Point begin = calculatePoint(l.get(l.size()-1),l.get(0));
		p.moveTo(begin.x,begin.y);
		for (int pointIndex = 1; pointIndex < l.size() - 1; pointIndex++) {
//			Point p1 = l.get(pointIndex - 1);
//			Point p2 = l.get(pointIndex);
//			Point p3 = l.get(pointIndex + 1);
//			Point mPoint = calculatePoint(p1, p2);
//			p.lineTo(mPoint.x, mPoint.y);
//			mPoint = calculatePoint(p3, p2);
//			p.curveTo(p2.x, p2.y, p2.x, p2.y, mPoint.x, mPoint.y);
			Point p1 = l.get(pointIndex - 1);
			Point p2 = l.get(pointIndex);
			Point p3 = l.get(pointIndex + 1);
			Point m1 = calculatePoint(p1, p2);
			p.lineTo(m1.x, m1.y);
			Point m2 = calculatePoint(p3, p2);
			p.curveTo(p2.x, p2.y, p2.x, p2.y, m2.x, m2.y);
		}
		return p;
	}

	private static Point calculatePoint(Point p1, Point p2) {
		double arcSize = 0.4;
		double d1 = Math.sqrt(Math.pow(p1.x - p2.x, 2)
				+ Math.pow(p1.y - p2.y, 2));
		double per = arcSize;
		double d_x = (p1.x - p2.x) * per;
		double d_y = (p1.y - p2.y) * per;
		int xx = (int) (p2.x + d_x);
		int yy = (int) (p2.y + d_y);
		return new Point(xx, yy);
	}

	public static void main(String args[]) {
		JFrame f = new JFrame("Rounded Corner Demo");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel contentPane = new JPanel() {
			@Override
			protected void paintComponent(Graphics grphcs) {
				Graphics2D g2d = (Graphics2D) grphcs;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);

				GradientPaint gp = new GradientPaint(0, 0, getBackground()
						.brighter().brighter(), 0, getHeight(), getBackground()
						.darker().darker());

				g2d.setPaint(gp);
				g2d.fillRect(0, 0, getWidth(), getHeight());
				int[][] a = { { 50, 50 }, { 100, 100 }, { 50, 150 }, { 0, 100 }};
				GeneralPath p = getRoundedGeneralPath(Arrays.asList(a));
				g2d.setColor(Color.red);
				g2d.draw(p);
				super.paintComponent(grphcs);
			}
		};
		contentPane.setOpaque(false);
		f.setContentPane(contentPane);
		contentPane.add(new JLabel("test"));
		f.setSize(200, 200);
		f.setVisible(true);
	}

}
