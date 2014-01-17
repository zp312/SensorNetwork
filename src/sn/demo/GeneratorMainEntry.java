package sn.demo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import sn.debug.ShowDebugImage;
import sn.regiondetect.*;

public class GeneratorMainEntry {

	public static void generateCases(int width, int height, double lineAngle,
			int lineGap, int caseFileCount) throws Exception {
		generateCases(width, height, lineAngle, lineGap, caseFileCount, null,
				false);

	}

	public static void generateCases(int width, int height, double lineAngle,
			int lineGap, int caseFileCount, ShowDebugImage frame,
			boolean showDebugImg) throws Exception {
		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		RegionGenerator rg = new RegionGenerator();
		Region[] regions = rg.generateRegions(width, height);

		AffineTransform rotateRec = new AffineTransform();
		rotateRec.rotate(lineAngle, width/2, height/2);
		
		
		
		String filename = String.format("data/test%04d.log", caseFileCount);
		System.out.println("saving log to " + filename);
		BufferedWriter output = new BufferedWriter(new FileWriter(filename));
		output.write("Line Angle " + lineAngle + "\n");
		output.write("Line Gap " + lineGap + "\n");
		// construct a line generator
		ParallelLineGenerator plg = new ParallelLineGenerator(lineAngle,
				lineGap, height, width);

		// generate parallel lines
		List<Line2D> lines = plg.generateParallelLines();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);

		for (int i = 0; i < regions.length; i++) {
			output.write("Region" + i + "\n");
			output.write(regions[i].toString());
			if (!regions[i].isHole()) {
				g2d.setColor(Color.CYAN);
				//GeneralPath p = GeomUtil.getRoundedGeneralPath(regions[i]);
				g2d.fill(regions[i].getShape());
			} else {
				g2d.setColor(Color.WHITE);
				//GeneralPath p = GeomUtil.getRoundedGeneralPath(regions[i]);
				g2d.fill(regions[i].getShape());
			}
		}
		output.close();

		// Save case image without lines
		filename = String.format("data/test%04d-noline.png", caseFileCount);
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}

		g2d.setColor(Color.BLACK);

		int lineC = 0;
		int totalLineC = 0;

		/* TODO Output line coordinates here */
		filename = String.format("data/test%04d-linecoord", caseFileCount);
		System.out.println("saving line coordinates to " + filename);
		BufferedWriter outLineCoord = new BufferedWriter(new FileWriter(filename));
		
		filename = String.format("data/test%04d-linecoordnorm", caseFileCount);		
		BufferedWriter outLineCoordNorm = new BufferedWriter(new FileWriter(filename));

		for (Line2D l : lines) {
			List<Line2D> intersectLines = new ArrayList<Line2D>();
			for (Region p : regions) {
				if (!p.isHole()) {
					intersectLines = GeomUtil.lineRegion(intersectLines, p, l);
				} else {
					intersectLines = GeomUtil
							.lineJumpHole(intersectLines, p, l);
				}
			}

			int iLineC = 0;
			for (Line2D il : intersectLines) {

				// set to false to hide line IDs
				GeomUtil.drawLine(img, il, lineC, iLineC, false);
				outLineCoord.write("Sensor" + lineC + " [" + il.getX1() + ","
						+ il.getY1() + "] ");
				outLineCoord.write("[" + il.getX2() + "," + il.getY2() + "]\n");
				
				Point2D normedPt1 = rotateRec.transform(il.getP1(), null);
				Point2D normedPt2 = rotateRec.transform(il.getP2(), null);
				//GeomUtil.drawLine(img, new Line2D.Double(normedPt1.getX(), normedPt1.getY(), normedPt2.getX(), normedPt2.getY()), lineC, iLineC, false);

				outLineCoordNorm.write("Sensor" + lineC + " [" + normedPt1.getX() + ","
						+ normedPt1.getY()+ "] ");
				outLineCoordNorm.write("[" + normedPt1.getX()+ ","
						+ normedPt2.getY() + "]\n");
				
				
				iLineC++;
				totalLineC++;
			}

			lineC++;

		}

		outLineCoord.close();
		outLineCoordNorm.close();

		if (showDebugImg) {
			if (frame == null) {
				frame = new ShowDebugImage("Regions", img);
				System.out.println("create frame");
			} else {
				frame.refresh(img);
				System.out.println("refresh frame");
			}
		}
		// Save case image with lines
		filename = String.format("data/test%04d-line.png", caseFileCount);
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}
	}

	public static void main(String args[]) throws Exception {

		int nCases = 10;

		int width = 800; // width of canvas
		int height = 600; // height of canvas
		double lineAngle = Math.PI / 50; // angle of lines
		int lineGap = 20; // Gap between lines (uniform)

		boolean showDebugImg = false;

		if (args.length == 0) {
			System.out.println("Generating " + nCases + " cases by default");
		}

		if (args.length == 1) {
			try {
				nCases = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.err
						.println("ERROR: Argument Type Cannot Be Parsed To Integer");
				System.exit(1);
			}
		}

		else if (args.length > 1) {
			System.err.println("USAGE: java -jar test.jar <nCases>");
			System.exit(1);
		}

		ShowDebugImage frame = null;
		if (showDebugImg) {
			BufferedImage img = new BufferedImage(width, height,
					BufferedImage.TYPE_4BYTE_ABGR);
			frame = new ShowDebugImage("Regions", img);
		}
		// Read file name count if exists
		int caseFileCount = 0;
		File file = new File("data/CaseCount.ini");
		if (file.exists()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				String nCase = null;
				if ((nCase = reader.readLine()) != null) {
					caseFileCount = Integer.parseInt(nCase);
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (file.createNewFile()) {
				System.out.println("created data/CaseCount.ini");
				BufferedWriter output = new BufferedWriter(new FileWriter(file));
				output.write("0");
				output.close();
			} else {
				System.err.println("failed to created data/CaseCount.ini");
			}
		}

		for (int i = 0; i < nCases; i++) {
			System.out.println("=====================\nGenerating Case "
					+ (i + 1) + "/" + nCases);
			generateCases(width, height, lineAngle, lineGap, caseFileCount,
					frame, showDebugImg);
			caseFileCount++;
		}

		BufferedWriter output = new BufferedWriter(new FileWriter(file));
		output.write(String.valueOf(caseFileCount));
		output.close();

	}
}
