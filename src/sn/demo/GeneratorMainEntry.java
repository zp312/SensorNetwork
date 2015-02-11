package sn.demo;

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
import java.util.Random;

import javax.imageio.ImageIO;

import com.jmatio.io.*;
import com.jmatio.types.*;

import sn.recover.SensorData;
import sn.recover.SensorInterval;
import sn.regiondetect.ComplexRegion;

public class GeneratorMainEntry {

	/**
	 * uniformly sample data point from positive intervals
	 * 
	 * @param gap
	 *            gap between sampled data point in pixel
	 * @return list of sampled points
	 */
	public static List<Point2D> getSampledSensorData(double gap, String fileName) {

		List<Point2D> pts = new ArrayList<Point2D>();
		
		File file = new File(fileName);

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			reader.readLine();
			while ((line = reader.readLine()) != null) {
				// caseFileCount = Integer.parseInt(nCase);
				String[] tokens = line.split(",");
				double x1 = Double.parseDouble(tokens[1]);
				double y1 = Double.parseDouble(tokens[2]);
				double x2 = Double.parseDouble(tokens[3]);
				double y2 = Double.parseDouble(tokens[4]);

				double KThreshold = 1e-10;
				//System.out.println("x1 "  +x1 + " y1 "  +y1 + " x2 "  +x2 + " y2 " + y2);
				// pts.add(interval.getStart());
				// pts.add(interval.getEnd());
				double k = Double.NaN;
				if (x2 - x1 != 0) {
					k = (y2 - y1) / (x2 - x1);
				}
				//System.out.println("k " + k);
				while (true) {
					if (Math.abs(x2 - x1) > KThreshold) {
						if (k == 0) { // horizontal lines
							if (x1 <= x2) {
								if (x1 + gap < x2) {
									pts.add(new Point2D.Double(x1 + gap, y1));
									x1 += gap;
								} else
									break;
							} else {
								if (x2 + gap < x1) {
									pts.add(new Point2D.Double(x2 + gap, y1));
									x2 += gap;
								} else
									break;
							}

						} else {
							double dx = gap / (Math.pow((k * k + 1), 0.5));
							double dy = k * dx;
							if (x1 <= x2) {
								if (x1 + dx < x2) {
									pts.add(new Point2D.Double((x1 + dx),
											(y1 + dy)));
									x1 += dx;
									y1 += dy;
									//System.out.println("adjusted x1 "  +x1 + " y1 "  +y1 + " x2 "  +x2 + " y2 " + y2);
								} else
									break;
							} else {
								if (x2 + dx < x1) {
									pts.add(new Point2D.Double((x2 + dx),
											(y2 + dy)));
									x2 += dx;
									y2 += dy;
								} else
									break;
							}
						}
					} else { // vertical lines
						if (y1 <= y2) {
							if (y1 + gap < y2) {
								pts.add(new Point2D.Double(x1, (y1 + gap)));
								y1 += gap;
							}

							else
								break;
						} else {
							if (y2 + gap < y1) {
								pts.add(new Point2D.Double(x1, (y2 + gap)));
								y2 += gap;
							} else
								break;
						}
					}
				}

			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("read file " + fileName + "failed");
		}

		return pts;
	}

	public static void main(String args[]) throws Exception {

		Random r = new Random();

		int nCases = 10;

		int width = 800; // width of canvas
		int height = 600; // height of canvas

		int lineGap = 10; // Gap between lines (uniform)
		int lineSet = 2; // indicates number of sets of parallel lines to be
							// drawn

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			String[] msg = arg.split("=");

			if (msg.length < 2) {
				System.err.println("ERROR: Parameter " + msg[0]
						+ " has no value!");
				System.out.println("Expect:	<Parameter>=<Value>");
				System.exit(-1);
			}

			if (msg[0].equals("width") || msg[0].equals("Width")
					|| msg[0].equals("WIDTH")) {
				try {
					width = Integer.parseInt(msg[1]);
				} catch (Exception e) {
					System.err
							.println("ERROR: Illigal value type for parameter "
									+ msg[0]);
					System.out.println("Expect:	<Integer>");
					System.exit(-1);
				}

			}

			else if (msg[0].equals("height") || msg[0].equals("Height")
					|| msg[0].equals("HEIGHT")) {
				try {
					height = Integer.parseInt(msg[1]);
				} catch (Exception e) {
					System.err
							.println("ERROR: Illigal value type for parameter "
									+ msg[0]);
					System.out.println("Expect:	<Integer>");
					System.exit(-1);
				}
			}

			else if (msg[0].equals("nCases")) {
				try {
					nCases = Integer.parseInt(msg[1]);
				} catch (Exception e) {
					System.err
							.println("ERROR: Illigal value type for parameter "
									+ msg[0]);
					System.out.println("Expect:	<Integer>");
					System.exit(-1);
				}
			}

			else if (msg[0].equals("gap") || msg[0].equals("Gap")
					|| msg[0].equals("GAP")) {
				try {
					lineGap = Integer.parseInt(msg[1]);
				} catch (Exception e) {
					System.err
							.println("ERROR: Illigal value type for parameter "
									+ msg[0]);
					System.out.println("Expect:	<Integer>");
					System.exit(-1);
				}
			}

			else if (msg[0].equals("nSensorSets")) {
				try {
					lineSet = Integer.parseInt(msg[1]);
				} catch (Exception e) {
					System.err
							.println("ERROR: Illigal value type for parameter "
									+ msg[0]);
					System.out.println("Expect:	<Integer>");
					System.exit(-1);
				}
			}

			else {
				System.err.println("ERROR: Illegal paremeter " + msg[0]);
				System.out
						.println("USAGE: java -jar RegionGenerator.jar parameters...");
				System.out.println("parameters:");
				System.out.println("	width=<Integer>");
				System.out.println("	height=<Integer>");
				System.out.println("	nCases=<Integer>");
				System.out.println("	nSensorSets=<Integer>");
				System.out.println("	gap=<Integer>");
				System.exit(-1);
			}

		}
		// Read file name count if exists
		int caseFileCount = 0;
		File dir = new File("data");
		if (!dir.exists()) {
			dir.mkdir();
		}
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
			caseFileCount++;
			System.out.println("=====================\nGenerating Case "
					+ (i + 1) + "/" + nCases);
			ComplexRegion complexRegion = new ComplexRegion(width, height);
			SensorData d = null;
			String fileHead = String.format("data/test%d", caseFileCount);
			String fileName;
			BufferedImage regionWithLines = complexRegion.drawRegion();// save
																		// region
																		// with
																		// all
																		// sensor
																		// data

			// for .mat file sample pts
			ArrayList matList = new ArrayList();

			double[] lineAngle = new double[lineSet];// angle of lines
			for (int n = 0; n < lineSet; n++) {
				if (r.nextBoolean())
					lineAngle[n] = r.nextDouble() * Math.PI / 2.1;
				else
					lineAngle[n] = r.nextDouble() * Math.PI * (1 - 1 / 1.9)
							+ Math.PI / 1.9;

				d = new SensorData(complexRegion, lineGap, lineAngle[n],
						complexRegion.getWidth(), complexRegion.getHeight());
				fileName = String.format(fileHead + "-positiveInterval-%d.png",
						n);
				BufferedImage img = d.drawPositiveIntervals(
						complexRegion.drawRegion(), false);
				if (n == 0) {
					regionWithLines = img;
				} else {
					regionWithLines = d.drawPositiveIntervals(regionWithLines,
							false);
				}
				// Write to file
				System.out.println("saving image to " + fileName);
				try {
					ImageIO.write(img, "png", new File(fileName));
				} catch (IOException e) {
					System.err.println("failed to save image " + fileName);
					e.printStackTrace();
				}

				String positiveFileName, negativeFileName;
				positiveFileName = String.format(fileHead + "-positiveData-%d",
						n);
				negativeFileName = String.format(fileHead + "-negativeData-%d",
						n);
				d.writeIntervalsToFile(positiveFileName, negativeFileName,
						false);

				positiveFileName = String.format(fileHead
						+ "-positiveDataNorm-%d", n);
				negativeFileName = String.format(fileHead
						+ "-negativeDataNorm-%d", n);
				d.writeIntervalsToFile(positiveFileName, negativeFileName, true);

				List<Point2D> samplePts = getSampledSensorData(5,positiveFileName);

				
				System.out.println("================Sampled Data ==================");
				// save sampled pts to .mat file
				double[] pts = new double[samplePts.size() * 2];
				for (int j = 0; j < samplePts.size(); j++) {
					Point2D pt = samplePts.get(j);
					pts[j] = pt.getX();
					pts[j + samplePts.size()] = pt.getY();
					//System.out.println("x " +  pt.getX() + " y "  + pt.getY());
				}
				String dataName = String.format("X%d", n);

				MLDouble mlDouble = new MLDouble(dataName, pts,
						samplePts.size());

				matList.add(mlDouble);

			}
			
			fileName = String.format("CPD2/data/test%d-sample.mat", caseFileCount);
			new MatFileWriter(fileName, matList);

			fileName = String.format(fileHead + "-positiveInterval-all.png");

			System.out.println("saving image to " + fileName);
			try {
				ImageIO.write(regionWithLines, "png", new File(fileName));
			} catch (IOException e) {
				System.err.println("failed to save image " + fileName);
				e.printStackTrace();
			}

			fileName = fileHead;
			complexRegion.saveRegion(i, fileName + ".rgn", true);

		}

		BufferedWriter output = new BufferedWriter(new FileWriter(file));
		output.write(String.valueOf(caseFileCount));
		output.close();

	}
}
