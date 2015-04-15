package sn.recover;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import javax.imageio.ImageIO;

import sn.debug.ShowDebugImage;
import sn.util.GrahamScan;
import sn.util.StarRelation;

import sn.demo.GeneratorMainEntry;

//Class for representing the approximative separated sensor interval groups 
//by only inspecting a single set of parallel sensor data
public class SeparatedSensorGroups {
	// list of positive intervals
	private List<SensorInterval> positiveIntervals;
	// list of negative intervals
	private List<SensorInterval> negativeIntervals;
	// hashmap of positive intervals, sensor id as the key
	private HashMap<Integer, List<SensorInterval>> positiveIntervalMap;
	// hashmap of negative intervals, sensor id as the key
	private HashMap<Integer, List<SensorInterval>> negativeIntervalMap;
	// a list of all sensor groups with positive intervals(exclude
	// positiveIntervalGroupAll)
	private List<SensorGroup> positiveIntervalGroups;
	// a list of all sensor groups with negative intervals(exclude
	// negativeIntervalGroupAll)
	private List<SensorGroup> negativeIntervalGroups;
	// sensor group contains all positive intervals
	private SensorGroup positiveIntervalGroupAll;
	// sensor group contains all negative intervals
	private SensorGroup negativeIntervalGroupAll;
	// a list of id pairs of all possibly mergeable positive sensor group
	private List<int[]> mergablePositiveGroupPairs;
	// a list of id pairs of all possibly mergeable megative sensor group
	private List<int[]> mergableNegativeGroupPairs;
	// gap between the sensors
	private double sensorGap;

	/**
	 * construction function
	 * 
	 * @param sd
	 *            raw sensor data
	 */
	public SeparatedSensorGroups(SensorData sd) {
		this.sensorGap = sd.getSensorGap();
		this.positiveIntervals = sd.getPositiveIntervals();
		this.negativeIntervals = sd.getNegativeIntervals();
		this.positiveIntervalMap = this
				.mapIntervalsByID(this.positiveIntervals);
		this.negativeIntervalMap = this
				.mapIntervalsByID(this.negativeIntervals);

		// find all positive interval groups ignoring groups with single
		// intervals
		this.positiveIntervalGroups = findSensorGroups(this.positiveIntervals,
				true);

		// find all negative interval groups ignoring groups with single
		// intervals
		this.negativeIntervalGroups = findSensorGroups(this.negativeIntervals,
				true);

		this.mergablePositiveGroupPairs = this.findMergablePairs(true);
		this.mergableNegativeGroupPairs = this.findMergablePairs(false);

		// set the gruop with all intervals with id = -1
		this.positiveIntervalGroupAll = new SensorGroup(
				this.positiveIntervalMap, -1);
		this.negativeIntervalGroupAll = new SensorGroup(
				this.negativeIntervalMap, -1);

	}

	/**
	 * Reorganize sensor intervals as a hashmap take sensor id as the key
	 * 
	 * @param siList
	 *            a list of all sensor intervals in the sensor data
	 * @return
	 */
	private HashMap<Integer, List<SensorInterval>> mapIntervalsByID(
			List<SensorInterval> siList) {
		HashMap<Integer, List<SensorInterval>> map = new HashMap<Integer, List<SensorInterval>>();
		for (SensorInterval si : siList) {
			if (map.containsKey(si.getSensorID())) {
				map.get(si.getSensorID()).add(si);
			} else {
				List<SensorInterval> newList = new ArrayList<SensorInterval>();
				newList.add(si);
				map.put(si.getSensorID(), newList);
			}
		}
		return map;
	}

	/**
	 * 
	 * @param positive
	 *            if true, looking for positive interval group
	 * @param ignoreSingleInterval
	 *            if true, ignore group with only one snesor interval
	 * @return
	 */
	private List<SensorGroup> findSensorGroups(List<SensorInterval> intervals,
			boolean ignoreSingleInterval) {
		// Intervals which has not been assigned to a group
		List<SensorInterval> unclassifiedIntervals = new ArrayList<SensorInterval>();

		unclassifiedIntervals.addAll(intervals);

		// Intervals which has been assigned to a group but
		// their adjacent intervals are not yet searched
		List<SensorInterval> ongoingIntervals;

		// intervals assigned to a specific group
		HashMap<Integer, List<SensorInterval>> intervalGroup;

		// A list of all positive sensor groups for the given data
		List<SensorGroup> sensorGroups = new ArrayList<SensorGroup>();

		if (intervals.isEmpty())
			return sensorGroups;

		int groupID = 0;
		while (!unclassifiedIntervals.isEmpty()) {
			intervalGroup = new HashMap<Integer, List<SensorInterval>>();
			ongoingIntervals = new ArrayList<SensorInterval>();

			ongoingIntervals.add(unclassifiedIntervals.get(0));
			unclassifiedIntervals.remove(0);

			List<Integer> removeIndex = new ArrayList<Integer>();

			while (!ongoingIntervals.isEmpty()) {
				SensorInterval queriedSi = ongoingIntervals.get(0);
				for (int i = 0; i < unclassifiedIntervals.size(); i++) {
					SensorInterval matchedSi = unclassifiedIntervals.get(i);
					if (Math.abs(matchedSi.getSensorID()
							- queriedSi.getSensorID()) == 1) {
						double y1s = matchedSi.getStart().getY();
						double y1e = matchedSi.getEnd().getY();
						double y2s = queriedSi.getStart().getY();
						double y2e = queriedSi.getEnd().getY();
						if ((y1s <= y2s && y1e >= y2e)
								|| (y1s <= y2e && y1e >= y2e)
								|| (y2s <= y1s && y2e >= y1e)
								|| (y2s <= y1e && y2e >= y1e)) {
							ongoingIntervals.add(matchedSi);
							removeIndex.add(i);
						}
					}
				}
				if (intervalGroup.containsKey(queriedSi.getSensorID())) {
					if (!intervalGroup.get(queriedSi.getSensorID()).contains(
							queriedSi))
						intervalGroup.get(queriedSi.getSensorID()).add(
								queriedSi);

				} else {
					List<SensorInterval> intervalFromNewSensor = new ArrayList<SensorInterval>();
					intervalFromNewSensor.add(queriedSi);
					intervalGroup.put(queriedSi.getSensorID(),
							intervalFromNewSensor);
				}

				ongoingIntervals.remove(0);
				Collections.sort(removeIndex);
				Collections.reverse(removeIndex);
				for (int i = 0; i < removeIndex.size(); i++) {
					// System.out.println("removing " + removeIndex.get(i));
					unclassifiedIntervals.remove((int) removeIndex.get(i));
				}
				removeIndex.clear();

			}

			SensorGroup sg = new SensorGroup(intervalGroup, groupID);
			if (ignoreSingleInterval) {
				int intervalCount = 0;
				for (List<SensorInterval> l : sg.getSensorIntervals().values()) {
					intervalCount += l.size();
				}
				if (intervalCount <= 1)
					continue;
			}
			sensorGroups.add(sg);
			groupID++;
			// System.out.println("group id " + groupID + " sg size "
			// + sg.getSensorIntervals().size());
		}

		return sensorGroups;
	}

	private List<int[]> findMergablePairs(boolean positive) {
		List<int[]> mergablePairs = new ArrayList<int[]>();

		List<SensorGroup> intervalGroups;
		if (positive) {
			intervalGroups = this.positiveIntervalGroups;
		}

		else {
			intervalGroups = this.negativeIntervalGroups;

		}

		String groupType;
		if (positive) {
			groupType = "positive ";
		} else {
			groupType = "negative ";
		}

		for (int i = 0; i < intervalGroups.size() - 1; i++) {
			for (int j = i + 1; j < intervalGroups.size(); j++) {
				SensorGroup sg1 = intervalGroups.get(i);
				SensorGroup sg2 = intervalGroups.get(j);

				HashMap<Integer, List<SensorInterval>> intervalMap1 = sg1
						.getSensorIntervals();
				HashMap<Integer, List<SensorInterval>> intervalMap2 = sg2
						.getSensorIntervals();

				for (int key : intervalMap1.keySet()) {
					if (intervalMap2.containsKey(key)) {
						List<SensorInterval> list1 = intervalMap1.get(key);
						List<SensorInterval> list2 = intervalMap2.get(key);
						double minDis = Double.POSITIVE_INFINITY;

						for (SensorInterval si1 : list1) {
							for (SensorInterval si2 : list2) {
								if (si1.getStart().getY() > si2.getEnd().getY()) {
									minDis = Math.min(si1.getStart().getY()
											- si2.getEnd().getY(), minDis);
								} else {
									minDis = Math.min(si2.getStart().getY()
											- si1.getEnd().getY(), minDis);
								}
							}
						}
						if (minDis < this.sensorGap) {
							int[] pair = { sg1.getID(), sg2.getID() };
							mergablePairs.add(pair);
							// System.out.println(groupType + "mergable pair "
							// + pair[0] + " " + pair[1]);
							break;
						}
					}

					else if (intervalMap2.containsKey(key + 1)) {
						List<SensorInterval> list1 = intervalMap1.get(key);
						List<SensorInterval> list2 = intervalMap2.get(key + 1);
						double minDis = Double.POSITIVE_INFINITY;

						for (SensorInterval si1 : list1) {
							for (SensorInterval si2 : list2) {
								if (si1.getStart().getY() > si2.getEnd().getY()) {
									minDis = Math.min(si1.getStart().getY()
											- si2.getEnd().getY(), minDis);
								} else {
									minDis = Math.min(si2.getStart().getY()
											- si1.getEnd().getY(), minDis);
								}
							}
						}
						if (minDis < this.sensorGap) {
							int[] pair = { sg1.getID(), sg2.getID() };
							mergablePairs.add(pair);
							// System.out.println(groupType + "mergable pair "
							// + pair[0] + " " + pair[1]);
							break;
						}
					}

					else if (intervalMap2.containsKey(key - 1)) {
						List<SensorInterval> list1 = intervalMap1.get(key);
						List<SensorInterval> list2 = intervalMap2.get(key - 1);
						double minDis = Double.POSITIVE_INFINITY;

						for (SensorInterval si1 : list1) {
							for (SensorInterval si2 : list2) {
								if (si1.getStart().getY() > si2.getEnd().getY()) {
									minDis = Math.min(si1.getStart().getY()
											- si2.getEnd().getY(), minDis);
								} else {
									minDis = Math.min(si2.getStart().getY()
											- si1.getEnd().getY(), minDis);
								}
							}
						}
						if (minDis < this.sensorGap) {
							int[] pair = { sg1.getID(), sg2.getID() };
							mergablePairs.add(pair);
							// System.out.println(groupType + "mergable pair "
							// + pair[0] + " " + pair[1]);
							break;
						}
					}
				}
			}
		}

		// System.out.println(groupType + "mergable pair total amount : "
		// + mergablePairs.size());
		return mergablePairs;
	}

	// Compare the Y between two points.
	private static class YComparator implements Comparator<SensorInterval> {
		@Override
		public int compare(SensorInterval si1, SensorInterval si2) {
			return (new Double(si1.getEnd().getY())).compareTo(new Double(si2
					.getEnd().getY()));
		}
	}

	// Compare the size.
	private static class sizeComparator implements Comparator<SensorGroup> {
		@Override
		public int compare(SensorGroup sg1, SensorGroup sg2) {
			return (new Double(sg1.getSize())).compareTo(new Double(sg2
					.getSize()));
		}
	}

	private void mergeRegions(int id1, int id2,
			HashMap<Integer, Integer> mergeAddressMap) {
		for (int key : mergeAddressMap.keySet()) {
			if (mergeAddressMap.get(key) == id2)
				mergeAddressMap.put(key, id1);
		}
		mergeAddressMap.put(id2, id1);
	}

	private void setMergeInfo(int id1, int id2,
			HashMap<Integer, Integer> mergeAddressMap,
			HashMap<Integer, List<Integer>> mergeMap) {

		if (mergeAddressMap.containsKey(id1)) {
			if (mergeAddressMap.containsKey(id2)) {
				int rootID1 = mergeAddressMap.get(id1);
				int rootID2 = mergeAddressMap.get(id2);
				if (rootID1 != rootID2) {
					mergeRegions(rootID1, rootID2, mergeAddressMap);

					List<Integer> list1 = mergeMap.get(rootID1);
					List<Integer> list2 = mergeMap.get(rootID2);
					list1.addAll(list2);
					list1.add(rootID2);
					mergeMap.remove(list2);
				}
			}

			else {
				int rootID1 = mergeAddressMap.get(id1);
				mergeAddressMap.put(id2, rootID1);
				List<Integer> list1 = mergeMap.get(rootID1);
				list1.add(id2);
			}

		}

		else if (mergeAddressMap.containsKey(id2)) {
			int rootID2 = mergeAddressMap.get(id2);
			mergeAddressMap.put(id1, rootID2);
			List<Integer> list2 = mergeMap.get(rootID2);
			list2.add(id1);
		}

		else {
			mergeAddressMap.put(id2, id1);
			List<Integer> list = new ArrayList<Integer>();
			// list.add(id1);
			list.add(id2);
			mergeMap.put(id1, list);
		}
	}

	private HashMap<Integer, SensorGroup> constructMergedGroup(
			HashMap<Integer, Integer> mergeAddressMap, SeparatedSensorGroups ssg) {
		HashMap<Integer, SensorGroup> mergedGroupMap = new HashMap<Integer, SensorGroup>();
		List<SensorGroup> sgList = ssg.getPositiveIntervalGroups();

		for (SensorGroup sg : sgList) {
			int id = sg.getID();
			if (mergeAddressMap.containsKey(id)) {
				int rootID = mergeAddressMap.get(id);
				if (mergedGroupMap.containsKey(rootID)) {
					SensorGroup rootSg = mergedGroupMap.get(rootID);
					SensorGroup candidateSg = ssg.getPositiveIntervalGroups()
							.get(id);
					HashMap<Integer, List<SensorInterval>> rootIntervalMap = rootSg
							.getSensorIntervals();
					HashMap<Integer, List<SensorInterval>> candidateIntervalMap = candidateSg
							.getSensorIntervals();

					for (int k : candidateIntervalMap.keySet()) {
						if (rootIntervalMap.containsKey(k)) {
							rootIntervalMap.get(k).addAll(
									candidateIntervalMap.get(k));
						} else {
							List<SensorInterval> newList = new ArrayList<SensorInterval>();
							newList.addAll(candidateIntervalMap.get(k));
							rootIntervalMap.put(k, newList);
						}
					}
					rootSg.updateGroupInfo();
				} else {

					SensorGroup oldSg = ssg.getPositiveIntervalGroups().get(
							rootID);

					HashMap<Integer, List<SensorInterval>> sensorIntervalMap = new HashMap<Integer, List<SensorInterval>>();
					for (int key : oldSg.getSensorIntervals().keySet()) {
						List<SensorInterval> copyList = new ArrayList<SensorInterval>();
						copyList.addAll(oldSg.getSensorIntervals().get(key));
						sensorIntervalMap.put(key, copyList);
					}

					SensorGroup newSg = new SensorGroup(sensorIntervalMap,
							rootID);
					mergedGroupMap.put(rootID, newSg);

				}
			}

			else {
				SensorGroup oldSg = ssg.getPositiveIntervalGroups().get(id);

				HashMap<Integer, List<SensorInterval>> sensorIntervalMap = new HashMap<Integer, List<SensorInterval>>();

				for (int key : oldSg.getSensorIntervals().keySet()) {
					List<SensorInterval> copyList = new ArrayList<SensorInterval>();
					copyList.addAll(oldSg.getSensorIntervals().get(key));
					sensorIntervalMap.put(key, copyList);
				}

				// for(List<SensorInterval> siList:
				// oldSg.getSensorIntervals().values()){
				// System.out.println("old sg size " + siList.size() + " id " +
				// id );
				// }

				SensorGroup newSg = new SensorGroup(sensorIntervalMap, id);
				mergedGroupMap.put(id, newSg);
			}
		}
		// System.out.println("merged map size " + mergedGroupMap.size()
		// + " original ssg size "
		// + ssg.getPositiveIntervalGroups().size());
		return mergedGroupMap;
	}

	/**
	 * calculate the dis-similarity between two size directional chains
	 * 
	 * @param map1
	 * @param map2
	 * @return
	 */
	private double calculateDirectionChainDis(
			HashMap<Integer, SensorGroup> map1,
			HashMap<Integer, SensorGroup> map2) {

		double diff = 0.;

		List<SensorGroup> sgList1 = new ArrayList<SensorGroup>(map1.values());
		List<SensorGroup> sgList2 = new ArrayList<SensorGroup>(map2.values());

		Collections.sort(sgList1, new sizeComparator());
		Collections.sort(sgList2, new sizeComparator());
		Collections.reverse(sgList1);
		Collections.reverse(sgList2);

		double totalSize1 = 0.;
		double totalSize2 = 0.;

		int[] starRelations1 = new int[sgList1.size()];
		int[] starRelations2 = new int[sgList2.size()];
		int[] directionDiff1;
		int[] directionDiff2;

		StarRelation star = new StarRelation(12);

		// get star relations and total size of all sensor groups
		for (int i = 0; i < sgList1.size(); i++) {
			SensorGroup sg1 = sgList1.get(i);
			SensorGroup sg2;
			if (i == sgList1.size() - 1)
				sg2 = sgList1.get(0);
			else
				sg2 = sgList1.get(i + 1);
			starRelations1[i] = star.GetStarRelation(sg1.getCentrePoint(),
					sg2.getCentrePoint());
			totalSize1 += sg1.getSize();
		}

		// if (sgList1.size() > 0)
		totalSize1 += sgList1.get(sgList1.size() - 1).getSize();

		for (int i = 0; i < sgList2.size(); i++) {
			SensorGroup sg1 = sgList2.get(i);
			SensorGroup sg2;
			if (i == sgList2.size() - 1)
				sg2 = sgList2.get(0);
			else
				sg2 = sgList2.get(i + 1);
			starRelations2[i] = star.GetStarRelation(sg1.getCentrePoint(),
					sg2.getCentrePoint());
			totalSize2 += sg1.getSize();
		}

		if (sgList2.size() > 0)
			totalSize2 += sgList2.get(sgList2.size() - 1).getSize();

		if (starRelations1.length <= 1) {

			directionDiff1 = new int[1];
			directionDiff1[0] = 0;
		} else {
			directionDiff1 = getStarDiff(starRelations1, star.getNRelations());
		}

		if (starRelations2.length <= 1) {

			directionDiff2 = new int[1];
			directionDiff2[0] = 0;
		} else {
			directionDiff2 = getStarDiff(starRelations2, star.getNRelations());
		}
		for (int i = 0; i < Math.max(directionDiff1.length,
				directionDiff2.length); i++) {
			double diff1, diff2, sizeRatio1, sizeRatio2;
			double err;

			if (i >= directionDiff1.length) {
				diff1 = 0;
				sizeRatio1 = 0;
			} else {
				diff1 = directionDiff1[i];
				sizeRatio1 = (sgList1.get(i).getSize() / totalSize1);
			}
			if (i >= directionDiff2.length) {
				sizeRatio2 = 0;
				diff2 = 0;
			} else {
				diff2 = directionDiff2[i];
				sizeRatio2 = (sgList2.get(i).getSize() / totalSize2);
			}
			err = Math.abs(diff1 - diff2) * Math.max(sizeRatio1, sizeRatio2);
			diff += (err / star.getNRelations() + Math.abs(sizeRatio1
					- sizeRatio2));
		}

		return diff;
	}

	private int[] getStarDiff(int[] starRelations, int nRelations) {

		int[] directionDiff = new int[starRelations.length - 1];

		int lastNonEqualStar = -1;
		for (int i = 0; i < directionDiff.length; i++) {

			if (starRelations[i] != nRelations) {
				lastNonEqualStar = starRelations[i];
			}

			if (starRelations[i + 1] == nRelations) {
				directionDiff[i] = 0;
				continue;
			}
			if (starRelations[i] == nRelations) {// equal relation
				if (i == 0) {
					directionDiff[i] = 0;
					continue;
				}

				else {

					int min = Math.min(lastNonEqualStar, starRelations[i + 1]);
					int max = Math.max(lastNonEqualStar, starRelations[i + 1]);
					int cycleUp = min + nRelations;
					int diffStar = Math.min(Math.abs(max - min),
							Math.abs(max - cycleUp));
					directionDiff[i] = diffStar;
					continue;

				}
			}

			else {
				int min = Math.min(starRelations[i], starRelations[i + 1]);
				int max = Math.max(starRelations[i], starRelations[i + 1]);
				int cycleUp = min + nRelations;
				int diffStar = Math.min(Math.abs(max - min),
						Math.abs(max - cycleUp));
				directionDiff[i] = diffStar;
				continue;
			}

		}
		return directionDiff;
	}

	public static void addSensorGroupsToGraphic(Graphics2D g2d,
			List<SensorGroup> sensorGroups, int xOffset, int yOffset,
			boolean drawHull) {
		// Draw components
		int nGroups = sensorGroups.size();
		for (int i = 0; i < nGroups; i++) {

			// set a random color for each new group
			int R = (int) (Math.random() * 256);
			int G = (int) (Math.random() * 256);
			int B = (int) (Math.random() * 256);
			Color randomColor = new Color(R, G, B);
			g2d.setColor(randomColor);

			SensorGroup curGroup = sensorGroups.get(i);

			if (drawHull) {
				List<Point2D> hullPts = curGroup.getConvexHull();
				if (hullPts.size() > 1) {
					Path2D hullPath = new Path2D.Double();
					hullPath.moveTo(hullPts.get(0).getX() + xOffset, hullPts
							.get(0).getY() + yOffset);
					for (Point2D pt : hullPts) {
						hullPath.lineTo(pt.getX() + xOffset, pt.getY()
								+ yOffset);
					}
					hullPath.lineTo(hullPts.get(0).getX() + xOffset, hullPts
							.get(0).getY() + yOffset);
					hullPath.closePath();
					g2d.draw(hullPath);
				}
			}
			// draw group id
			// SensorInterval si = siList.get(0);
			// Font stringFont = new Font("SansSerif", Font.PLAIN, 18);
			// g2d.setFont(stringFont);
			// g2d.drawString(String.valueOf(curGroup.getID()), (int) curGroup
			// .getCentrePoint().getX() + xOffset, (int) curGroup
			// .getCentrePoint().getY() + yOffset);
			for (int key : curGroup.getSensorIntervals().keySet()) {
				// g2d.setColor(randomColor);
				List<SensorInterval> siList = curGroup.getSensorIntervals()
						.get(key);

				// draw sensor id
				// SensorInterval si = siList.get(0);
				// g2d.drawString(String.valueOf(key), (int)si.getStart().getX()
				// + xOffset, (int)si.getStart().getY() + yOffset);

				for (SensorInterval curInterval : siList) {
					Path2D path = new Path2D.Double();
					path.moveTo(curInterval.getStart().getX() + xOffset,
							curInterval.getStart().getY() + yOffset);
					path.lineTo(curInterval.getEnd().getX() + xOffset,
							curInterval.getEnd().getY() + yOffset);
					path.closePath();
					g2d.draw(path);
				}
			}
		}
	}

	public static void addIntervalsToGraphic(Graphics2D g2d,
			List<SensorInterval> sensorIntervals, int xOffset, int yOffset) {
		// set a random color for each new group
		int R = (int) (Math.random() * 256);
		int G = (int) (Math.random() * 256);
		int B = (int) (Math.random() * 256);
		Color randomColor = new Color(R, G, B);
		g2d.setColor(randomColor);

		int nIntervals = sensorIntervals.size();
		for (int i = 0; i < nIntervals; i++) {
			SensorInterval curInterval = sensorIntervals.get(i);
			Path2D path = new Path2D.Double();
			path.moveTo(curInterval.getStart().getX() + xOffset, curInterval
					.getStart().getY() + yOffset);
			path.lineTo(curInterval.getEnd().getX() + xOffset, curInterval
					.getEnd().getY() + yOffset);
			path.closePath();
			g2d.draw(path);
		}
	}

	public static void addHullToGraphic(Graphics2D g2d,
			List<ExtremePoint2D> hull, int xOffset, int yOffset) {

		Color lineColor = Color.RED;

		Color spotColor = Color.DARK_GRAY;
		// g2d.setColor(randomLineColor);

		int nPts = hull.size();
		Path2D path = new Path2D.Double();
		path.moveTo(hull.get(0).getX() + xOffset, hull.get(0).getY() + yOffset);
		for (int i = 1; i < nPts + 1; i++) {
			Point2D pt;
			if (i == nPts) {
				pt = hull.get(0);
			} else
				pt = hull.get(i);
			g2d.setColor(spotColor);
			g2d.fillRect((int) pt.getX() + xOffset, (int) pt.getY() + yOffset,
					5, 5);

			path.lineTo(pt.getX() + xOffset, pt.getY() + yOffset);
			// path.lineTo(ptNext.getX() + xOffset, ptNext.getY() + yOffset);
			// path.closePath();

		}
		g2d.setColor(lineColor);
		g2d.draw(path);
		path.closePath();
	}

	public static int getBit(int i, int position) {
		return (i >> position) & 1;
	}

	public void setNegativeSensorIntervals(List<SensorInterval> negSiList) {
		this.negativeIntervals = negSiList;
		this.negativeIntervalMap = this
				.mapIntervalsByID(this.negativeIntervals);
		this.negativeIntervalGroups = findSensorGroups(this.negativeIntervals,
				true);
		this.mergableNegativeGroupPairs = this.findMergablePairs(false);

	}

	public List<List<HashMap<Integer, SensorGroup>>> initialMatch(
			SeparatedSensorGroups ssg) {
		int n = this.mergablePositiveGroupPairs.size();

		List<List<HashMap<Integer, Integer>>> singleOrDualMerges = new ArrayList<List<HashMap<Integer, Integer>>>();
		List<List<HashMap<Integer, Integer>>> bestMerges = new ArrayList<List<HashMap<Integer, Integer>>>();
		List<Double> minErrors = new ArrayList<Double>();
		List<List<HashMap<Integer, SensorGroup>>> mergedComponentsMaps = new ArrayList<List<HashMap<Integer, SensorGroup>>>();

		// iterate over all 2^n possibilities of merging interval groups
		for (int i = 0; i < Math.pow(2, n); i++) {
			// Represents the selected merging sensor groups, <id,List<merged
			// ids>>
			// HashMap<Integer, List<Integer>> mergeMap1 = new HashMap<Integer,
			// List<Integer>>();
			boolean conflict1 = false;
			HashMap<Integer, Integer> mergeAddressMap1 = new HashMap<Integer, Integer>();
			HashMap<Integer, List<Integer>> mergeMap1 = new HashMap<Integer, List<Integer>>();
			List<Integer> nonMergePairs1 = new ArrayList<Integer>();

			for (int j = 0; j < n; j++) {
				if (getBit(i, j) == 1) {
					int[] mergablePair = this.mergablePositiveGroupPairs.get(j);
					int id1 = mergablePair[0];
					int id2 = mergablePair[1];
					setMergeInfo(id1, id2, mergeAddressMap1, mergeMap1);
				} else {
					nonMergePairs1.add(j);
				}
			}

			for (int k : mergeMap1.keySet()) {
				if (conflict1)
					break;
				List<Integer> mergedComponents = mergeMap1.get(k);
				for (int index : nonMergePairs1) {
					int[] pair = this.mergablePositiveGroupPairs.get(index);
					int id1 = pair[0];
					int id2 = pair[1];
					if ((id1 == k || mergedComponents.contains(id1))
							&& (id2 == k || mergedComponents.contains(id2))) {
						conflict1 = true;
						break;
					}
				}
			}
			if (conflict1)
				continue;

			int m = ssg.mergablePositiveGroupPairs.size();
			for (int p = 0; p < Math.pow(2, m); p++) {

				boolean conflict2 = false;
				// HashMap<Integer, List<Integer>> mergeMap2 = new
				// HashMap<Integer, List<Integer>>();
				HashMap<Integer, Integer> mergeAddressMap2 = new HashMap<Integer, Integer>();
				HashMap<Integer, List<Integer>> mergeMap2 = new HashMap<Integer, List<Integer>>();
				List<Integer> nonMergePairs2 = new ArrayList<Integer>();
				for (int q = 0; q < m; q++) {
					if (getBit(p, q) == 1) {
						int[] mergablePair = ssg.mergablePositiveGroupPairs
								.get(q);
						int id1 = mergablePair[0];
						int id2 = mergablePair[1];

						setMergeInfo(id1, id2, mergeAddressMap2, mergeMap2);

					} else {
						nonMergePairs2.add(q);
					}
				}

				for (int k : mergeMap2.keySet()) {
					if (conflict2)
						break;
					List<Integer> mergedComponents = mergeMap2.get(k);
					for (int index : nonMergePairs2) {
						int[] pair = ssg.mergablePositiveGroupPairs.get(index);
						int id1 = pair[0];
						int id2 = pair[1];
						if ((id1 == k || mergedComponents.contains(id1))
								&& (id2 == k || mergedComponents.contains(id2))) {
							conflict2 = true;
							break;
						}
					}
				}
				if (conflict2) {
					// if(p%10000 == 0)
					// System.out.println("ignore conflict merge, times p " +
					// p);
					continue;
				}

				HashMap<Integer, SensorGroup> map1 = constructMergedGroup(
						mergeAddressMap1, this);

				HashMap<Integer, SensorGroup> map2 = constructMergedGroup(
						mergeAddressMap2, ssg);

				double diff = calculateDirectionChainDis(map1, map2);

				List<HashMap<Integer, Integer>> goodMerges = new ArrayList<HashMap<Integer, Integer>>();
				goodMerges.add(mergeAddressMap1);
				goodMerges.add(mergeAddressMap2);

				List<HashMap<Integer, SensorGroup>> mergedComponent = new ArrayList<HashMap<Integer, SensorGroup>>();
				mergedComponent.add(map1);
				mergedComponent.add(map2);

				if (map1.size() <= 2 && map2.size() <= 2) {
					singleOrDualMerges.add(goodMerges);
				}

				else {
					if (minErrors.size() < 5) {
						minErrors.add(diff);
						bestMerges.add(goodMerges);
						mergedComponentsMaps.add(mergedComponent);
					}

					for (int t = 0; t < minErrors.size(); t++) {
						if (diff < minErrors.get(t)) {
							minErrors.add(t, diff);
							bestMerges.add(t, goodMerges);
							mergedComponentsMaps.add(t, mergedComponent);
							break;
						}
					}
					if (minErrors.size() > 5) {
						minErrors.remove(minErrors.size() - 1);
						bestMerges.remove(bestMerges.size() - 1);
						mergedComponentsMaps.remove(mergedComponent.size() - 1);
					}
				}
				// for (int x : mergeAddressMap1.keySet()) {
				// System.out.println("Data1 : Merge " + x + " to "
				// + mergeAddressMap1.get(x));
				// }
				// for (int x : mergeAddressMap2.keySet()) {
				// System.out.println("Data2 : Merge " + x + " to "
				// + mergeAddressMap2.get(x));
				// }

				// if(p%100000 == 0)
				System.out.println(diff + " c1 " + i + " c2 " + p);
			}

		}

		// for (int i = 0; i < bestMerges.size(); i++) {
		// HashMap<Integer, Integer> map1 = bestMerges.get(i).get(0);
		// HashMap<Integer, Integer> map2 = bestMerges.get(i).get(1);
		// System.out.println("min err is " + minErrors.get(i));
		// for (int j : map1.keySet()) {
		// System.out.println("Data1 : Merge " + j + " to " + map1.get(j));
		// }
		// for (int j : map2.keySet()) {
		// System.out.println("Data2 : Merge " + j + " to " + map2.get(j));
		// }
		// }
		return mergedComponentsMaps;
	}

	/**
	 * 
	 * @return List<{rotation(radian),scale,translationX,translationY}>
	 */
	private List<Double[]> getInitialFactors(
			List<List<HashMap<Integer, SensorGroup>>> mergedComponentsMaps,
			Point2D anchory) {
		List<Double[]> factorList = new ArrayList<Double[]>();

		for (int i = 0; i < mergedComponentsMaps.size(); i++) {
			HashMap<Integer, SensorGroup> map1 = mergedComponentsMaps.get(i)
					.get(0);
			HashMap<Integer, SensorGroup> map2 = mergedComponentsMaps.get(i)
					.get(1);

			List<SensorGroup> sgList1 = new ArrayList<SensorGroup>(
					map1.values());
			List<SensorGroup> sgList2 = new ArrayList<SensorGroup>(
					map2.values());

			Collections.sort(sgList1, new sizeComparator());
			Collections.sort(sgList2, new sizeComparator());
			Collections.reverse(sgList1);
			Collections.reverse(sgList2);

			double rotationAngle = Double.NaN;
			double scale = Double.NaN;
			double translationX = Double.NaN;
			double translationY = Double.NaN;

			rotationAngle = this.getInitialRotationAngle(sgList1, sgList2);
			scale = this.getInitialScale(sgList1, sgList2);
			double[] translation = this.getInitialTranslation(sgList1, sgList2,
					scale, rotationAngle, anchory);
			translationX = translation[0];
			translationY = translation[1];

			Double[] factors = { rotationAngle, scale, translationX,
					translationY };
			factorList.add(factors);
		}
		return factorList;
	}

	private Double[] getFactor(List<SensorGroup> sgList1,
			List<SensorGroup> sgList2, Point2D anchory) {

		Collections.sort(sgList1, new sizeComparator());
		Collections.sort(sgList2, new sizeComparator());
		Collections.reverse(sgList1);
		Collections.reverse(sgList2);

		double rotationAngle = Double.NaN;
		double scale = Double.NaN;
		double translationX = Double.NaN;
		double translationY = Double.NaN;

		rotationAngle = this.getInitialRotationAngle(sgList1, sgList2);
		scale = this.getInitialScale(sgList1, sgList2);
		double[] translation = this.getInitialTranslation(sgList1, sgList2,
				scale, rotationAngle, anchory);
		translationX = translation[0];
		translationY = translation[1];

		Double[] factors = { rotationAngle, scale, translationX, translationY };

		return factors;
	}

	// rotate sgList2 to sgList1
	private double getInitialRotationAngle(List<SensorGroup> sgList1,
			List<SensorGroup> sgList2) {
		double rotationAngle;
		if (sgList1.size() > 1 && sgList2.size() > 1) {
			Point2D pt11 = sgList1.get(0).getCentrePoint();
			Point2D pt12 = sgList1.get(1).getCentrePoint();
			Point2D pt21 = sgList2.get(0).getCentrePoint();
			Point2D pt22 = sgList2.get(1).getCentrePoint();

			double angle1,angle2;
			
			double x1 = pt11.getX();
			double x2 = pt12.getX();
			double y1 = pt11.getY();
			double y2 = pt12.getY();
			//System.out.println("p11 " + x1 + "," + y1 + " p12 " + x2 + ","  +y2);
			if(pt12.getX() != pt11.getX()){
				
				angle1 = Math.atan(((y2 - y1)/(x2 - x1)));
				if (x1 > x2)
					// due to the different coordinate system
					angle1 = 1.5 * Math.PI + angle1;
				else if (x1 < x2)
					// due to the different coordinate system
					angle1 = 0.5 * Math.PI + angle1;
			}
			
			else{
				if (y1 <= y2)
					angle1 = 0;
				else
					angle1 = Math.PI;
			}
			
			x1 = pt21.getX();
			x2 = pt22.getX();
			y1 = pt21.getY();
			y2 = pt22.getY();
			//System.out.println("p21 " + x1 + "," + y1 + " p22 " + x2 + ","  +y2);
			if(pt22.getX() != pt21.getX()){

				angle2 = Math.atan(((y2 - y1)/(x2 - x1)));
				if (x1 > x2)
					// due to the different coordinate system
					angle2 = 1.5 * Math.PI + angle2;
				else if (x1 < x2)
					// due to the different coordinate system
					angle2 = 0.5 * Math.PI + angle2;
				
			}
			
			else{
				if (y1 <= y2)
					angle2 = 0;
				else
					angle2 = Math.PI;
			}
			
			rotationAngle = angle1 - angle2;
			
			//System.out.println("angle1 " + angle1 + " angle2 " + angle2  + " diff " + rotationAngle);
		}

		else {
			rotationAngle = 0.;
		}

		return rotationAngle;
	}

	private double getInitialScale(List<SensorGroup> sgList1,
			List<SensorGroup> sgList2) {
		double scale = 1.;

		SensorGroup sg1 = sgList1.get(0);
		SensorGroup sg2 = sgList2.get(0);

		scale = Math.sqrt(sg1.getSize() / sg2.getSize());

		return scale;
	}

	private double[] getInitialTranslation(List<SensorGroup> sgList1,
			List<SensorGroup> sgList2, double scale, double rotateAngle,
			Point2D anchory) {
		double[] translation = new double[2];

		SensorGroup sg1 = sgList1.get(0);
		SensorGroup sg2 = sgList2.get(0);

		Point2D centre2 = sg2.getCentrePoint();

		AffineTransform rotate = new AffineTransform();
		rotate.rotate(rotateAngle, anchory.getX(), anchory.getY());
		centre2 = rotate.transform(centre2, null);

		translation[0] = sg1.getCentrePoint().getX() - centre2.getX() * scale;
		translation[1] = sg1.getCentrePoint().getY() - centre2.getY() * scale;

		return translation;
	}

	private List<Point2D> getIntersections(List<SensorInterval> intervals1,
			List<SensorInterval> intervals2) {
		List<Point2D> intersections = new ArrayList<Point2D>();

		for (SensorInterval si1 : intervals1) {
			for (SensorInterval si2 : intervals2) {
				if (si1.intersects(si2)) {
					intersections.add(getIntersectionPoint(si1.getInterval(),
							si2.getInterval()));
				}
			}
		}

		return intersections;
	}

	/**
	 * 
	 * @param siList
	 * @return
	 */
	private boolean isBounding(List<SensorInterval> siList) {
		boolean leftBounding = true;
		boolean rightBounding = true;
		boolean topBounding = true;
		boolean downBounding = true;

		for (SensorInterval si1 : siList) {
			for (SensorInterval si2 : this.getNegativeIntervals()) {
				if (si2.getStart().getY() < si1.getStart().getY())
					topBounding = false;
				if (si2.getEnd().getY() > si1.getEnd().getY())
					downBounding = false;
				if (si2.getStart().getX() < si1.getStart().getX())
					leftBounding = false;
				if (si2.getEnd().getX() > si1.getEnd().getX())
					rightBounding = false;
			}
			if (leftBounding || rightBounding || topBounding || downBounding) {
				return true;
			}

			else {
				leftBounding = true;
				rightBounding = true;
				topBounding = true;
				downBounding = true;
			}
		}
		return false;
	}

	private boolean setLayer(int curLayer, ConnectedIntervalGroup knownCig,
			ConnectedIntervalGroup unknownCig) {
		List<SensorInterval> unknownSiList = unknownCig.getComponentList().get(
				0);
		List<SensorInterval> knownSiList = knownCig.getComponentList().get(0);
		for (SensorInterval knownSi : knownSiList) {
			for (SensorInterval unknownSi : unknownSiList) {
				Point2D s1 = knownSi.getStart();
				Point2D e1 = knownSi.getEnd();
				Point2D s2 = unknownSi.getStart();
				Point2D e2 = unknownSi.getEnd();

				if (e1.distance(s2) <= 1 || e2.distance(s1) <= 1) {
					unknownCig.setLayer(curLayer + 1);
					return true;
				}
			}

		}
		return false;
	}

	/**
	 * remove sensor interval groups with only one interval or only short
	 * intervals
	 */
	private List<SensorGroup> getFilteredGroups() {
		List<SensorGroup> posIntervalGroups = this.getPositiveIntervalGroups();
		List<SensorGroup> filteredGroups = new ArrayList<SensorGroup>();
		double threshold = 5.;

		Iterator<SensorGroup> it = posIntervalGroups.iterator();
		while (it.hasNext()) {
			SensorGroup sg = it.next();
			HashMap<Integer, List<SensorInterval>> siMap = sg
					.getSensorIntervals();

			// boolean hasMultiIntervals = false;
			if (siMap.size() <= 1) {// remove group with only one interval
				continue;
			}

			// remove group with only short intervals
			// boolean shortIntervals = true;
			for (int key : siMap.keySet()) {
				for (SensorInterval si : siMap.get(key)) {
					if (si.getStart().distance(si.getEnd()) >= threshold) {
						filteredGroups.add(sg);
						break;
					}
				}
			}

		}
		return filteredGroups;
	}

	/**
	 * use graham scan to get convex hull of the whole region
	 * 
	 * @param intervalGroups
	 * @return
	 */
	private List<ExtremePoint2D> getConvexShape(List<SensorGroup> intervalGroups) {
		List<Point2D> shapePts = new ArrayList<Point2D>();
		List<ExtremePoint2D> extremePtList = new ArrayList<ExtremePoint2D>();
		for (SensorGroup sg : intervalGroups) {
			HashMap<Integer, List<SensorInterval>> siMap = sg
					.getSensorIntervals();
			for (int siId : siMap.keySet()) {
				List<SensorInterval> siList = siMap.get(siId);
				for (SensorInterval si : siList) {
					shapePts.add(si.getStart());
					shapePts.add(si.getEnd());
				}
			}
		}

		Point2D[] pts = new Point2D[shapePts.size()];
		shapePts.toArray(pts);
		GrahamScan gs = new GrahamScan(pts);
		shapePts = new ArrayList<Point2D>((Stack<Point2D>) gs.hull());

		for (Point2D pt : shapePts) {

			extremePtList.add(new ExtremePoint2D(pt));
		}
		return extremePtList;
	}

	private List<ExtremePoint2D> getSignificantInflexion(
			List<ExtremePoint2D> extremePtList, int nPtRequired,
			StarRelation starRel) {
		List<ExtremePoint2D> inflexions = new ArrayList<ExtremePoint2D>();

		int[] angleDiffArray = new int[extremePtList.size()];
		int smallestIndexInInflexions = 0;// record the index of point with
											// smallest angle change in the
											// inflexions list
		int smallestIndexInExtremePts = 0;

		for (int i = 0; i < extremePtList.size(); i++) {
			boolean ptAdded = false;
			ExtremePoint2D adjPt1, adjPt2;// adjacent point1

			if (i == 0) {
				adjPt1 = extremePtList.get(extremePtList.size() - 1);
				adjPt2 = extremePtList.get(i + 1);

			}

			else if (i == extremePtList.size() - 1) {
				adjPt1 = extremePtList.get(i - 1);
				adjPt2 = extremePtList.get(0);
			}

			else {
				adjPt1 = extremePtList.get(i - 1);
				adjPt2 = extremePtList.get(i + 1);
			}
			angleDiffArray[i] = extremePtList.get(i).getAngleDiff(adjPt1,
					adjPt2, starRel);

			// update smallest index and append extreme pt if the
			// inflexions list is not full
			if (inflexions.size() < nPtRequired) {
				inflexions.add(extremePtList.get(i));
				ptAdded = true;
			}

			// remove the point with smallest angle diff and append the point
			// with larger diff
			else {
				if (angleDiffArray[i] >= angleDiffArray[smallestIndexInExtremePts]) {
					inflexions.remove(smallestIndexInInflexions);
					inflexions.add(extremePtList.get(i));
					ptAdded = true;
				}
			}

			// update smallest index
			if (ptAdded) {
				int tem = i;// tem index
				int compIndex = i;// index compares with i
				for (ExtremePoint2D pt : inflexions) {
					compIndex = extremePtList.indexOf(pt);
					if (angleDiffArray[tem] > angleDiffArray[compIndex]) {
						tem = compIndex;
					}
				}
				smallestIndexInInflexions = inflexions.indexOf(extremePtList
						.get(tem));
				smallestIndexInExtremePts = extremePtList.indexOf(inflexions
						.get(smallestIndexInInflexions));
			}
		}

		// test

		for (int i = 0; i < inflexions.size(); i++) {
			int index = extremePtList.indexOf(inflexions.get(i));
			System.out.println("largest diff : " + angleDiffArray[index]);
		}

		Arrays.sort(angleDiffArray);
		for (int d : angleDiffArray) {
			System.out.println("all diff " + d);
		}

		return inflexions;
	}

	/**
	 * 
	 * @param shape1
	 * @param shape2
	 * @return position in shape2 that matches position 0 in shape 1
	 */

	private int matchShape(List<ExtremePoint2D> shape1,
			List<ExtremePoint2D> shape2) {
		int matchedPos = 0;

		return matchedPos;
	}

	// dilate the sensor intervals with a given gap
	private HashMap<Integer, List<SensorInterval>> getIntervalMapAfterDilation(
			HashMap<Integer, List<SensorInterval>> intervalMap, double radius,
			double sensorGap) {
		HashMap<Integer, List<SensorInterval>> dilationIntervalMap = new HashMap<Integer, List<SensorInterval>>();
		for (int k : intervalMap.keySet()) {
			for (SensorInterval si : intervalMap.get(k)) {
				double startY = si.getStart().getY();
				double endY = si.getEnd().getY();

				// assume intervals are vertically normalized
				double x = (si.getStart().getX() + si.getEnd().getX()) / 2;

				// first extend the original interval
				SensorInterval extendedSi = new SensorInterval(
						si.getSensorID(),
						new Point2D.Double(x, startY - radius),
						new Point2D.Double(x, endY + radius));

				// add the extended interval into candidate list
				if (dilationIntervalMap.containsKey(k)) {
					dilationIntervalMap.get(k).add(extendedSi);
				} else {
					List<SensorInterval> intervalList = new ArrayList<SensorInterval>();
					intervalList.add(extendedSi);
					dilationIntervalMap.put(k, intervalList);
				}

				// Extend adjacent intervals
				int nAdjConsidered = (int) (radius / sensorGap);
				for (int i = 1; i <= nAdjConsidered; i++) {
					double adjStartY = startY
							- Math.sqrt(radius * radius - i * sensorGap
									* sensorGap) / 2;
					double adjEndY = endY
							+ Math.sqrt(radius * radius - i * sensorGap
									* sensorGap) / 2;

					SensorInterval extendedAdjSi1 = new SensorInterval(k - i,
							new Point2D.Double(x - sensorGap, adjStartY),
							new Point2D.Double(x - sensorGap, adjEndY));
					SensorInterval extendedAdjSi2 = new SensorInterval(k + i,
							new Point2D.Double(x + sensorGap, adjStartY),
							new Point2D.Double(x + sensorGap, adjEndY));

					// add two extended adjacent intervals into candidate
					// interval map
					if (dilationIntervalMap.containsKey(k - i)) {
						dilationIntervalMap.get(k - i).add(extendedAdjSi1);
					} else {
						List<SensorInterval> intervalList = new ArrayList<SensorInterval>();
						intervalList.add(extendedAdjSi1);
						dilationIntervalMap.put(k - i, intervalList);
					}

					if (dilationIntervalMap.containsKey(k + i)) {
						dilationIntervalMap.get(k + i).add(extendedAdjSi2);
					} else {
						List<SensorInterval> intervalList = new ArrayList<SensorInterval>();
						intervalList.add(extendedAdjSi2);
						dilationIntervalMap.put(k + i, intervalList);
					}

				}

			}
		}

		// merge overlapping intervals
		for (int k : dilationIntervalMap.keySet()) {
			List<SensorInterval> siList = dilationIntervalMap.get(k);
			Collections.sort(siList, new YComparator());

			// the index indicates the first interval that has not been tested
			int testingIndex = 0;
			while (siList.size() > 1 && testingIndex < siList.size() - 1) {

				double startY1 = siList.get(testingIndex).getStart().getY();
				double startY2 = siList.get(testingIndex + 1).getStart().getY();
				double endY1 = siList.get(testingIndex).getEnd().getY();
				double endY2 = siList.get(testingIndex + 1).getEnd().getY();
				double x = (siList.get(testingIndex).getStart().getX() + siList
						.get(testingIndex).getEnd().getX()) / 2;

				// if overlapping occurs,merge two intervals
				if (startY2 <= endY1) {
					double mergedEndY = Math.max(endY1, endY2);
					SensorInterval mergedInterval = new SensorInterval(k,
							new Point2D.Double(x, startY1), new Point2D.Double(
									x, mergedEndY));
					siList.set(testingIndex, mergedInterval);
					siList.remove(testingIndex + 1);
				} else {

					testingIndex++;
				}
			}
		}

		return dilationIntervalMap;
	}

	/**
	 * 
	 * @param intervalMap
	 * @param radius
	 * @return
	 */
	private HashMap<Integer, List<SensorInterval>> getIntervalMapAfterErosion(
			HashMap<Integer, List<SensorInterval>> intervalMap, double radius) {
		HashMap<Integer, List<SensorInterval>> erosionIntervalMap = new HashMap<Integer, List<SensorInterval>>();

		for (int k : intervalMap.keySet()) {

			List<SensorInterval> eroatedIntevals = new ArrayList<SensorInterval>();

			for (SensorInterval si : intervalMap.get(k)) {
				// if si length < 2 * radius, ignore short si
				if (si.getStart().distance(si.getEnd()) < 2 * radius)
					continue;

				// ignore bounding interval
				if (!intervalMap.containsKey(k - 1))
					continue;
				if (!intervalMap.containsKey(k + 1))
					continue;

				boolean startFound = false, endFound = false;

				double startY = si.getStart().getY();
				double endY = si.getEnd().getY();

				// assume intervals are vertically normalized
				// deal with small diff between two ends
				double x = (si.getStart().getX() + si.getEnd().getX()) / 2;

				double eroatedStartY = Double.NaN, eroatedEndY = Double.NaN;

				// start with +radius to ignore bounding
				for (double y = startY + radius; y < endY - radius; y += 1) {

					// init distance to left and right adjacent interval to 0
					int nLeftAdj = 1, nRightAdj = 1;

					Point2D centre = new Point2D.Double(x, y);

					double xLeftDis = 0., xRightDis = 0.;

					boolean leftTestPassed = true, rightTestPassed = true;

					// find the left nearest interval with distance to the
					// circle centre greater than radius
					while (xLeftDis < radius && leftTestPassed) {
						//
						if (!intervalMap.containsKey(k - nLeftAdj)) {
							if (startFound) {
								eroatedEndY = y - 1;
								endFound = true;
							}
							leftTestPassed = false;
							break;
						}

						// perform erosion, assume intervals are vertically
						// normalized
						for (SensorInterval siAdj : intervalMap.get(k
								- nLeftAdj)) {
							xLeftDis = siAdj.getInterval().ptLineDist(centre);
							if (!(siAdj.getStart().getY() < startY)) {
								Line2D lineSeg = new Line2D.Double(
										si.getStart(), siAdj.getStart());
								// part of the circle falls out of the component
								if (lineSeg.ptLineDist(centre) < radius) {
									leftTestPassed = false;
								}

								else {
									leftTestPassed = true;
								}
							}

							else {
								leftTestPassed = true;
							}

							if (!(siAdj.getEnd().getY() > endY)) {
								Line2D lineSeg = new Line2D.Double(si.getEnd(),
										siAdj.getEnd());
								// part of the circle falls out of the component
								if (lineSeg.ptLineDist(centre) < radius) {
									leftTestPassed = false;
								}

								else {
									leftTestPassed = true;
									break;
								}
							}

							else {
								leftTestPassed = true;
								break;
							}
						}

						nLeftAdj++;
					}

					// find the right nearest interval with distance to the
					// circle centre greater than radius
					while (xRightDis < radius && rightTestPassed
							&& leftTestPassed) {

						if (!intervalMap.containsKey(k + nRightAdj)) {
							if (startFound) {
								eroatedEndY = y - 1;
								endFound = true;
							}
							rightTestPassed = false;
							break;
						}

						// perform erosion, assume intervals are vertically
						// normalized
						for (SensorInterval siAdj : intervalMap.get(k
								+ nRightAdj)) {
							xRightDis = siAdj.getInterval().ptLineDist(centre);
							if (!(siAdj.getStart().getY() < startY)) {
								Line2D lineSeg = new Line2D.Double(
										si.getStart(), siAdj.getStart());
								// part of the circle falls out of the component
								if (lineSeg.ptLineDist(centre) < radius) {
									rightTestPassed = false;
								}

								else {
									rightTestPassed = true;
								}
							}

							else {
								rightTestPassed = true;
							}

							if (!(siAdj.getEnd().getY() > endY)) {
								Line2D lineSeg = new Line2D.Double(si.getEnd(),
										siAdj.getEnd());
								// part of the circle falls out of the component
								if (lineSeg.ptLineDist(centre) < radius) {
									rightTestPassed = false;
								}

								else {
									rightTestPassed = true;
									break;
								}
							}

							else {
								rightTestPassed = true;
								break;
							}
						}

						nRightAdj++;
					}

					if (!startFound && leftTestPassed && rightTestPassed) {
						eroatedStartY = y;
						startFound = true;
					}

					else if (startFound && !endFound
							&& (!leftTestPassed || !rightTestPassed)) {
						eroatedEndY = y - 1;
						endFound = true;
					}

					// if start point found, end point not found and reached the
					// end of the interval
					// set the end of the interval as end of eroated interval
					else if (startFound && !endFound && (y + 1 > endY - radius)) {
						eroatedEndY = endY;
						endFound = true;
					}

					if (startFound && endFound) {

						Point2D s = new Point2D.Double(x, eroatedStartY);
						Point2D e = new Point2D.Double(x, eroatedEndY);

						eroatedIntevals.add(new SensorInterval(k, s, e));

						eroatedStartY = Double.NaN;
						eroatedEndY = Double.NaN;

						startFound = false;
						endFound = false;
					}

				}
			}
			erosionIntervalMap.put(k, eroatedIntevals);
		}

		return erosionIntervalMap;
	}

	private Double[] optimizeInitMatch(Double[] factors, Point2D anchory,
			List<SensorInterval> posSiList1, List<SensorInterval> posSiList2,
			List<SensorInterval> negSiList1, List<SensorInterval> negSiList2) {
		Random r = new Random();
		int[] searchDirection = this.resetSearch();

		double err = getMatchError(posSiList1, negSiList1, posSiList2,
				negSiList2, factors, anchory);

		if(Double.isNaN(err)){
			System.err.println("no intersections found, please check if two measurements are parallel");
			return factors;
		}
		
		double temErr = err;
		while (true) {

			boolean hasZero = false;

			int nZeros = 0;
			for (int i = 0; i < searchDirection.length; i++) {
				if (searchDirection[i] == 0) {
					hasZero = true;
					nZeros++;
				}
			}
			System.out.println("opting " + nZeros);
			if (!hasZero)
				break;

			int searchType = r.nextInt(nZeros);
			int curType = -1;
			for (int i = 0; i < searchDirection.length; i++) {
				if (searchDirection[i] == 0) {
					curType++;
					if (curType == searchType) {
						searchType = i;
						break;
					}
				}
			}

			System.out.println("opt " + searchType);

			// shrink
			if (searchType == 0 && searchDirection[0] != 1) {
				temErr = err;
				double originalScale = factors[1];
				while (temErr == err) {
					factors[1] -= 0.01;
					temErr = getMatchError(posSiList1, negSiList1, posSiList2,
							negSiList2, factors, anchory);
					
					if(Double.isNaN(temErr)){
						System.err.println("no intersections found, please check if two measurements are parallel");
						return factors;
					}
					
					if (temErr < err) {
						searchDirection = this.resetSearch();
						err = temErr;
						break;
					} else if (temErr > err) {
						searchDirection[0] = 1;
						factors[1] = originalScale;
						break;
					}
				}
			}

			// enlarge
			else if (searchType == 1 && searchDirection[1] != 1) {
				temErr = err;
				double originalScale = factors[1];
				while (temErr == err) {
					factors[1] += 0.01;
					temErr = getMatchError(posSiList1, negSiList1, posSiList2,
							negSiList2, factors, anchory);
					
					if(Double.isNaN(temErr)){
						System.err.println("no intersections found, please check if two measurements are parallel");
						return factors;
					}
					
					
					if (temErr < err) {
						searchDirection = this.resetSearch();
						err = temErr;
						break;
					} else if (temErr > err) {
						searchDirection[1] = 1;
						factors[1] = originalScale;
						break;
					}
				}
			}

			// rotate anti-clockwise
			else if (searchType == 2 && searchDirection[2] != 1) {
				temErr = err;
				double originalRotate = factors[0];
				while (temErr == err) {
					factors[0] -= 0.01;
					temErr = getMatchError(posSiList1, negSiList1, posSiList2,
							negSiList2, factors, anchory);
					
					if(Double.isNaN(temErr)){
						System.err.println("no intersections found, please check if two measurements are parallel");
						return factors;
					}
					
					if (temErr < err) {
						searchDirection = this.resetSearch();
						err = temErr;
						break;
					} else if (temErr > err) {
						searchDirection[2] = 1;
						factors[0] = originalRotate;
						break;
					}
				}
			}

			// rotate clockwise
			else if (searchType == 3 && searchDirection[3] != 1) {
				temErr = err;
				double originalRotate = factors[0];
				while (temErr == err) {
					factors[0] += 0.01;
					temErr = getMatchError(posSiList1, negSiList1, posSiList2,
							negSiList2, factors, anchory);
					
					if(Double.isNaN(temErr)){
						System.err.println("no intersections found, please check if two measurements are parallel");
						return factors;
					}
					
					if (temErr < err) {
						searchDirection = this.resetSearch();
						err = temErr;
						break;
					} else if (temErr > err) {
						searchDirection[3] = 1;
						factors[0] = originalRotate;
						break;
					}
				}
			}

			// translate left
			else if (searchType == 4 && searchDirection[4] != 1) {// up
				temErr = err;
				double originalTranslateX = factors[2];
				while (temErr == err) {
					factors[2] -= 1;
					temErr = getMatchError(posSiList1, negSiList1, posSiList2,
							negSiList2, factors, anchory);
					
					if(Double.isNaN(temErr)){
						System.err.println("no intersections found, please check if two measurements are parallel");
						return factors;
					}
					
					if (temErr < err) {
						searchDirection = this.resetSearch();
						err = temErr;
						break;
					} else if (temErr > err) {
						searchDirection[4] = 1;
						factors[2] = originalTranslateX;
						break;
					}
				}
			}

			// translate right
			else if (searchType == 5 && searchDirection[5] != 1) {// down
				temErr = err;
				double originalTranslateX = factors[2];
				while (temErr == err) {
					factors[2] += 1;
					temErr = getMatchError(posSiList1, negSiList1, posSiList2,
							negSiList2, factors, anchory);
					
					if(Double.isNaN(temErr)){
						System.err.println("no intersections found, please check if two measurements are parallel");
						return factors;
					}
					
					if (temErr < err) {
						searchDirection = this.resetSearch();
						err = temErr;
						break;
					} else if (temErr > err) {
						searchDirection[5] = 1;
						factors[2] = originalTranslateX;
						break;
					}
				}
			}

			// translate up
			else if (searchType == 6 && searchDirection[6] != 1) {// left
				temErr = err;
				double originalTranslateY = factors[3];
				while (temErr == err) {
					factors[3] -= 1;
					temErr = getMatchError(posSiList1, negSiList1, posSiList2,
							negSiList2, factors, anchory);
					
					if(Double.isNaN(temErr)){
						System.err.println("no intersections found, please check if two measurements are parallel");
						return factors;
					}
					
					if (temErr < err) {
						searchDirection = this.resetSearch();
						err = temErr;
						break;
					} else if (temErr > err) {
						searchDirection[6] = 1;
						factors[3] = originalTranslateY;
						break;
					}
				}
			}

			// translate down
			else if (searchType == 7 && searchDirection[7] != 1) {// right
				temErr = err;
				double originalTranslateY = factors[3];
				while (temErr == err) {
					factors[3] += 1;
					temErr = getMatchError(posSiList1, negSiList1, posSiList2,
							negSiList2, factors, anchory);
					
					if(Double.isNaN(temErr)){
						System.err.println("no intersections found, please check if two measurements are parallel");
						return factors;
					}
					
					if (temErr < err) {
						searchDirection = this.resetSearch();
						err = temErr;
						break;
					} else if (temErr > err) {
						searchDirection[7] = 1;
						factors[3] = originalTranslateY;
						break;
					}
				}
			}
		}
		return factors;
	}

	private int[] resetSearch() {
		int[] searchDirection = new int[8];

		for (int i = 0; i < 8; i++) {
			searchDirection[i] = 0;
		}
		return searchDirection;
	}

	public static Point2D getIntersectionPoint(Line2D line1, Line2D line2) {
		if (!line1.intersectsLine(line2))
			return null;
		double px = line1.getX1(), py = line1.getY1(), rx = line1.getX2() - px, ry = line1
				.getY2() - py;
		double qx = line2.getX1(), qy = line2.getY1(), sx = line2.getX2() - qx, sy = line2
				.getY2() - qy;

		double det = sx * ry - sy * rx;
		if (det == 0) {
			return null;
		} else {
			double z = (sx * (qy - py) + sy * (px - qx)) / det;
			if (z == 0 || z == 1)
				return null; // intersection at end point!
			return new Point2D.Double((px + z * rx), (py + z * ry));
		}
	} // end intersection line-line

	public HashMap<Integer, ConnectedIntervalGroup> findConnectedIntervals(
			List<SensorInterval> siList1, List<SensorInterval> siList2) {
		HashMap<Integer, ConnectedIntervalGroup> connectedIntervalGroupMap = new HashMap<Integer, ConnectedIntervalGroup>();

		List<SensorInterval> copySiList1 = new ArrayList<SensorInterval>();
		List<SensorInterval> copySiList2 = new ArrayList<SensorInterval>();
		copySiList1.addAll(siList1);
		copySiList2.addAll(siList2);

		int id = 0;

		while (copySiList1.size() != 0 || copySiList2.size() != 0) {
			List<List<SensorInterval>> connectedIntervals = new ArrayList<List<SensorInterval>>();
			List<SensorInterval> connectedIntervalsFromList1 = new ArrayList<SensorInterval>();
			List<SensorInterval> connectedIntervalsFromList2 = new ArrayList<SensorInterval>();
			List<SensorInterval> ongoingList;

			SensorInterval si;
			boolean fromList1 = true;
			if (copySiList1.size() > 0) {
				if (copySiList2.isEmpty()) {
					for (SensorInterval si1 : copySiList1) {
						connectedIntervalsFromList1.add(si1);
					}
					break;
				}

				si = copySiList1.get(0);
				fromList1 = true;
			} else {
				for (SensorInterval si2 : copySiList2) {
					connectedIntervalsFromList2.add(si2);
				}
				break;
			}

			List<SensorInterval> tester = new ArrayList<SensorInterval>();
			tester.add(si);
			while (!tester.isEmpty()) {
				if (fromList1) {
					ongoingList = getIntersectIntervals(copySiList2, tester);
					connectedIntervalsFromList1.addAll(tester);

					for (SensorInterval sitester : tester) {
						copySiList1.remove(sitester);
					}

					tester = ongoingList;

					fromList1 = false;
				}

				else {
					ongoingList = getIntersectIntervals(copySiList1, tester);
					connectedIntervalsFromList2.addAll(tester);

					for (SensorInterval sitester : tester) {
						copySiList2.remove(sitester);
					}

					tester = ongoingList;
					fromList1 = true;
				}
			}

			if (connectedIntervalsFromList1.size()
					+ connectedIntervalsFromList2.size() < 3)
				continue;
			connectedIntervals.add(connectedIntervalsFromList1);
			connectedIntervals.add(connectedIntervalsFromList2);
			ConnectedIntervalGroup cig = new ConnectedIntervalGroup(id,
					connectedIntervals);

			connectedIntervalGroupMap.put(id, cig);
			id++;
		}

		return connectedIntervalGroupMap;
	}

	public List<SensorInterval> getIntersectIntervals(
			List<SensorInterval> candidateList, List<SensorInterval> testerList) {
		List<SensorInterval> dest = new ArrayList<SensorInterval>();

		boolean intervalAdded = false;
		for (SensorInterval si1 : candidateList) {
			if (intervalAdded) {
				intervalAdded = false;
				continue;
			}

			for (SensorInterval si2 : testerList) {
				if (si1.intersects(si2)) {
					dest.add(si1);
					intervalAdded = true;
					break;
				}
			}
		}

		return dest;
	}

	public void setLayerInfo(
			HashMap<Integer, ConnectedIntervalGroup> posiCigMap,
			HashMap<Integer, ConnectedIntervalGroup> negaCigMap) {
		int curLayer = 0;
		// boolean settingPositive = true;
		List<SensorInterval> knownIntervals = new ArrayList<SensorInterval>();
		// find base layer
		for (int key : negaCigMap.keySet()) {
			ConnectedIntervalGroup negaCig = negaCigMap.get(key);
			List<SensorInterval> normIntervalList = negaCig.getComponentList()
					.get(0);
			if (this.isBounding(normIntervalList)) {
				negaCig.setLayer(0);// set to base
				knownIntervals.addAll(normIntervalList);
			}
		}

		boolean newLayerFound = true;
		while (newLayerFound) {
			newLayerFound = false;
			if (curLayer % 2 == 1) {
				for (int key1 : negaCigMap.keySet()) {
					ConnectedIntervalGroup negaCig = negaCigMap.get(key1);
					for (int key2 : posiCigMap.keySet()) {
						ConnectedIntervalGroup posiCig = posiCigMap.get(key2);
						if (posiCig.getLayer() == curLayer) {
							if (negaCig.getLayer() == -1) {
								if (this.setLayer(curLayer, posiCig, negaCig)) {
									newLayerFound = true;
								}
							}
						}
					}
				}
			}

			else {
				for (int key1 : posiCigMap.keySet()) {
					ConnectedIntervalGroup posiCig = posiCigMap.get(key1);
					for (int key2 : negaCigMap.keySet()) {
						ConnectedIntervalGroup negaCig = negaCigMap.get(key2);
						if (negaCig.getLayer() == curLayer) {
							if (posiCig.getLayer() == -1) {
								if (this.setLayer(curLayer, negaCig, posiCig)) {
									newLayerFound = true;
								}
							}
						}
					}
				}

			}
			curLayer++;
		}
	}

	public static SensorInterval transformInterval(SensorInterval si,
			Double[] factors, Point2D anchory) {

		double rotationAngle = factors[0];
		double scale = factors[1];
		double translationX = factors[2];
		double translationY = factors[3];

		Point2D pt1 = si.getStart();
		Point2D pt2 = si.getEnd();

		AffineTransform rotate = new AffineTransform();
		rotate.rotate(rotationAngle, anchory.getX(), anchory.getY());
		pt1 = rotate.transform(si.getStart(), null);
		pt2 = rotate.transform(si.getEnd(), null);
		pt1.setLocation(pt1.getX() * scale, pt1.getY() * scale);
		pt2.setLocation(pt2.getX() * scale, pt2.getY() * scale);
		pt1.setLocation(pt1.getX() + translationX, pt1.getY() + translationY);
		pt2.setLocation(pt2.getX() + translationX, pt2.getY() + translationY);

		SensorInterval siTransformed = new SensorInterval(si.getSensorID(),
				pt1, pt2);

		return siTransformed;
	}

	public double getMatchError(List<SensorInterval> positiveIntervals1,
			List<SensorInterval> negativeIntervals1,
			List<SensorInterval> positiveIntervals2,
			List<SensorInterval> negativeIntervals2, Double[] factors,
			Point2D anchory) {
		double error = 0;
		List<SensorInterval> transformedPositiveIntervals2 = new ArrayList<SensorInterval>();
		List<SensorInterval> transformedNegativeIntervals2 = new ArrayList<SensorInterval>();

		for (SensorInterval si : positiveIntervals2) {
			SensorInterval siTransformed = transformInterval(si, factors,
					anchory);
			transformedPositiveIntervals2.add(siTransformed);
		}

		for (SensorInterval si : negativeIntervals2) {
			SensorInterval siTransformed = transformInterval(si, factors,
					anchory);
			transformedNegativeIntervals2.add(siTransformed);
		}

		int nPositiveIntersections = getIntersections(positiveIntervals1,
				transformedPositiveIntervals2).size();
		int nPosiNegaIntersections = getIntersections(positiveIntervals1,
				transformedNegativeIntervals2).size();
		int nNegaPosiIntersections = getIntersections(negativeIntervals1,
				transformedPositiveIntervals2).size();

		error = ((double) nPosiNegaIntersections + (double) nNegaPosiIntersections)
				/ (double) ((double) nPosiNegaIntersections
						+ (double) nNegaPosiIntersections + (double) nPositiveIntersections);

		System.out.println("nPositiveIntersections " + nPositiveIntersections
				+ " nPosiNegaIntersections " + nPosiNegaIntersections
				+ " nNegaPosiIntersections " + nNegaPosiIntersections
				+ " error " + error);

		// optimizeMergedData();

		return error;

	}

	public List<SensorInterval> getPositiveIntervals() {

		return this.positiveIntervals;
	}

	public List<SensorInterval> getNegativeIntervals() {

		return this.negativeIntervals;
	}

	public List<SensorGroup> getPositiveIntervalGroups() {
		return this.positiveIntervalGroups;

	}

	public List<SensorGroup> getNegativeIntervalGroups() {
		return this.negativeIntervalGroups;

	}

	/**
	 * Draw interval groups
	 * 
	 * @param showNeg
	 *            if show negative sensor intervals
	 * @throws Exception
	 */
	public static void testDrawIntervalGroups(List<SensorGroup> sg, String name)
			throws Exception {
		int width = 1000;
		int height = 1000;

		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);

		addSensorGroupsToGraphic(g2d, sg, 100, 200, true);

		// if (showNeg) {
		// negativeData.addIntervalsToGraphic(g2d,
		// negativeData.getPositiveIntervals(), false, Color.BLACK);
		// }

		BufferedImage scaledImg = ShowDebugImage.resize(img, 800, 800);
		showImg(scaledImg, name);
	}

	/**
	 * Save interval groups into file
	 * 
	 * @throws Exception
	 */
	public static void testSaveIntervalGroups(List<SensorGroup> sg, String name)
			throws Exception {
		int width = 1000;
		int height = 1000;

		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);

		addSensorGroupsToGraphic(g2d, sg, 100, 200, true);

		// if (showNeg) {
		// negativeData.addIntervalsToGraphic(g2d,
		// negativeData.getPositiveIntervals(), false, Color.BLACK);
		// }

		BufferedImage scaledImg = ShowDebugImage.resize(img, 800, 800);
		try {
			ImageIO.write(scaledImg, "png", new File(name));
			System.out.println("saved matched sensor data image to " + name);
		} catch (IOException e) {
			System.err.println("failed to save image " + name);
			e.printStackTrace();
		}
	}

	public static void testDrawIntervals(List<SensorInterval> siList,
			String name) throws Exception {
		int width = 1000;
		int height = 1000;

		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);

		addIntervalsToGraphic(g2d, siList, 100, 200);

		// if (showNeg) {
		// negativeData.addIntervalsToGraphic(g2d,
		// negativeData.getPositiveIntervals(), false, Color.BLACK);
		// }

		BufferedImage scaledImg = ShowDebugImage.resize(img, 500, 500);
		showImg(scaledImg, name);
		// frame.refresh(scaledImg);
	}

	public static void testDrawConflicts(List<List<SensorInterval>> siLists,
			List<Point2D> intersections, String name) throws Exception {
		int width = 1000;
		int height = 1000;

		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);

		int n = 0;
		g2d.setStroke(new BasicStroke(3));
		for (List<SensorInterval> siList : siLists) {
			if (n >= 2)
				g2d.setStroke(new BasicStroke(1));

			addIntervalsToGraphic(g2d, siList, 100, 200);
			n++;
		}
		Font stringFont = new Font("SansSerif", Font.PLAIN, 28);
		g2d.setColor(Color.RED);
		g2d.setStroke(new BasicStroke(10));
		g2d.setFont(stringFont);
		for (Point2D pt : intersections) {
			g2d.drawString("X", (int) pt.getX() + 100, (int) pt.getY() + 200);

		}

		String fileName = name;
		try {
			ImageIO.write(img, "png", new File(fileName));
			System.out
					.println("saved matched sensor data image to " + fileName);
		} catch (IOException e) {
			System.err.println("failed to save image " + fileName);
			e.printStackTrace();
		}

	}

	public static void showImg(BufferedImage img, String name) {
		ShowDebugImage frame = null;
		frame = new ShowDebugImage(name, img);
		frame.refresh(img);
	}

	public static void saveMatchedPicture(String fileName,
			List<SensorGroup> sgList1, List<SensorGroup> sgList2,
			Double[] factors, Point2D anchory) {

		double rotationAngle = factors[0];
		double scale = factors[1];
		double translationX = factors[2];
		double translationY = factors[3];

		// System.out.println("rotation " + rotationAngle + " scale " + scale
		// + " translation " + translationX + " " + translationY);

		int width = 1000;
		int height = 1000;

		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);

		addSensorGroupsToGraphic(g2d, sgList1, 100, 200, false);

		List<SensorGroup> sgListTransformed = new ArrayList<SensorGroup>();

		for (SensorGroup sg : sgList2) {
			HashMap<Integer, List<SensorInterval>> siMapTransformed = new HashMap<Integer, List<SensorInterval>>();
			for (int k : sg.getSensorIntervals().keySet()) {
				List<SensorInterval> siList = sg.getSensorIntervals().get(k);
				List<SensorInterval> siListTransformed = new ArrayList<SensorInterval>();
				for (SensorInterval si : siList) {
					Point2D pt1 = si.getStart();
					Point2D pt2 = si.getEnd();

					AffineTransform rotate = new AffineTransform();
					rotate.rotate(rotationAngle, anchory.getX(), anchory.getY());
					pt1 = rotate.transform(si.getStart(), null);
					pt2 = rotate.transform(si.getEnd(), null);
					pt1.setLocation(pt1.getX() * scale, pt1.getY() * scale);
					pt2.setLocation(pt2.getX() * scale, pt2.getY() * scale);
					pt1.setLocation(pt1.getX() + translationX, pt1.getY()
							+ translationY);
					pt2.setLocation(pt2.getX() + translationX, pt2.getY()
							+ translationY);

					SensorInterval siTransformed = new SensorInterval(
							si.getSensorID(), pt1, pt2);
					siListTransformed.add(siTransformed);
				}
				siMapTransformed.put(k, siListTransformed);
			}
			SensorGroup sgTransformed = new SensorGroup(siMapTransformed,
					sg.getID());
			sgListTransformed.add(sgTransformed);
		}

		addSensorGroupsToGraphic(g2d, sgListTransformed, 100, 200, false);

		try {
			ImageIO.write(img, "png", new File(fileName));
			System.out
					.println("saved matched sensor data image to " + fileName);
		} catch (IOException e) {
			System.err.println("failed to save image " + fileName);
			e.printStackTrace();
		}

	}

	public static void saveConvexShape(String fileName,
			List<ExtremePoint2D> hull, List<SensorGroup> sgList) {

		// System.out.println("rotation " + rotationAngle + " scale " + scale
		// + " translation " + translationX + " " + translationY);

		int width = 1000;
		int height = 1000;

		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);
		addSensorGroupsToGraphic(g2d, sgList, 100, 200, false);
		addHullToGraphic(g2d, hull, 100, 200);

		try {
			ImageIO.write(img, "png", new File(fileName));
			System.out
					.println("saved matched sensor data image to " + fileName);
		} catch (IOException e) {
			System.err.println("failed to save image " + fileName);
			e.printStackTrace();
		}

	}

	/**
	 * test the first matching strategy
	 * 
	 * @throws Exception
	 */
	public static void testStg1() throws Exception {

		int totalNComponents = 0;
		double angleDiff = 0;
		double scaleTruthDiff = 0;

		double err = 0.;

		for (int n = 222; n < 323; n++) {
			String dataFileHeader = String.format(
					"data_ijcai_paper/test%d-positiveDataNorm-", n);

			String dataFileName = String.format(dataFileHeader + "%d", 8);

			SensorData sd1 = new SensorData(dataFileName, 800, 600);

			String negativeDataFileHeader = String.format(
					"data_ijcai_paper/test%d-negativeDataNorm-", n);
			String negativeDataFileName = String.format(negativeDataFileHeader
					+ "%d", 8);

			SensorData negSd1 = new SensorData(negativeDataFileName, 800, 600);

			dataFileName = String.format(dataFileHeader + "%d", 9);
			negativeDataFileName = String.format(negativeDataFileHeader + "%d",
					9);

			SensorData sd2 = new SensorData(dataFileName, 800, 600);
			SensorData negSd2 = new SensorData(negativeDataFileName, 800, 600);

			SeparatedSensorGroups ssg1 = new SeparatedSensorGroups(sd1);
			ssg1.setNegativeSensorIntervals(negSd1.getPositiveIntervals());

			SeparatedSensorGroups ssg2 = new SeparatedSensorGroups(sd2);
			ssg2.setNegativeSensorIntervals(negSd2.getPositiveIntervals());
			List<List<HashMap<Integer, SensorGroup>>> mergedComponentsMaps = ssg1
					.initialMatch(ssg2);

			Point2D anchory = ssg2.positiveIntervalGroupAll.getCentrePoint();

			List<Double[]> factorList = ssg1.getInitialFactors(
					mergedComponentsMaps, anchory);

			File dir = new File("results");
			if (!dir.exists()) {
				dir.mkdir();
			}

			double[] matchErrorArray = new double[factorList.size()];
			double minErr = Double.MAX_VALUE;
			int minErrIndex = -1;

			for (int i = 0; i < factorList.size(); i++) {
				String fileName = String.format("results/test%d-ini-%d.png", n,
						i);
				List<SensorGroup> sgList1 = new ArrayList<SensorGroup>(
						mergedComponentsMaps.get(i).get(0).values());
				List<SensorGroup> sgList2 = new ArrayList<SensorGroup>(
						mergedComponentsMaps.get(i).get(1).values());

				Collections.sort(sgList1, new sizeComparator());
				Collections.sort(sgList2, new sizeComparator());
				Collections.reverse(sgList1);
				Collections.reverse(sgList2);

				matchErrorArray[i] = ssg1
						.getMatchError(ssg1.getPositiveIntervals(),
								ssg1.getNegativeIntervals(),
								ssg2.getPositiveIntervals(),
								ssg2.getNegativeIntervals(), factorList.get(i),
								anchory);
				if (matchErrorArray[i] < minErr) {
					minErr = matchErrorArray[i];
					minErrIndex = i;
				}

				// System.out.println("Error is " + matchErrorArray[i]);

				saveMatchedPicture(fileName, sgList1, sgList2,
						factorList.get(i), anchory);
			}
			if (factorList.size() < 1 || minErrIndex == -1)
				continue;

			Double[] factors = factorList.get(minErrIndex);

			factors = ssg1.optimizeInitMatch(factors, anchory,
					ssg1.getPositiveIntervals(), ssg2.getPositiveIntervals(),
					ssg1.getNegativeIntervals(), ssg2.getNegativeIntervals());

			minErr = ssg1.getMatchError(ssg1.getPositiveIntervals(),
					ssg1.getNegativeIntervals(), ssg2.getPositiveIntervals(),
					ssg2.getNegativeIntervals(), factors, anchory);

			List<SensorInterval> transformedPositiveSiList = new ArrayList<SensorInterval>();
			List<SensorInterval> transformedNegativeSiList = new ArrayList<SensorInterval>();

			for (SensorInterval si : ssg2.getPositiveIntervals()) {
				SensorInterval transformedSi = transformInterval(si, factors,
						anchory);
				transformedPositiveSiList.add(transformedSi);
			}

			for (SensorInterval si : ssg2.getNegativeIntervals()) {
				SensorInterval transformedSi = transformInterval(si, factors,
						anchory);
				transformedNegativeSiList.add(transformedSi);
			}

			HashMap<Integer, ConnectedIntervalGroup> posiCigMap = ssg1
					.findConnectedIntervals(ssg1.getPositiveIntervals(),
							transformedPositiveSiList);
			HashMap<Integer, ConnectedIntervalGroup> negaCigMap = ssg1
					.findConnectedIntervals(ssg1.getNegativeIntervals(),
							transformedNegativeSiList);
			// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//			List<Point2D> sampledPts = GeneratorMainEntry.getSampledSensorData(
//					5, transformedPositiveSiList);
//
//			String converterFileName = String.format("test%d-sample-0bini", n);
//
//			File convertedDir = new File("results/convertedData");
//			if (!convertedDir.exists()) {
//				convertedDir.mkdir();
//			}
//
//			File convertedDataFile = new File(
//					converterFileName);
//			if (convertedDataFile.exists()) {
//
//			} else {
//				if (convertedDataFile.createNewFile()) {
//					BufferedWriter output = new BufferedWriter(new FileWriter(
//							convertedDataFile));
//
//					for (Point2D pt : sampledPts) {
//						output.write("   " + pt.getX() + "   " + pt.getY()
//								+ "\n");
//					}
//					output.close();
//				} else {
//					System.err.println("failed to created " + converterFileName);
//				}
//			}

			// ///////////////////////////////////////////////////////////////////////////////////////////////
			// ///////////////////////////////////////////////////////////////////////////////////////////////
			System.out.println("n components " + posiCigMap.size());

			File file = new File("results/componentCount.txt");
			if (file.exists()) {

			} else {
				if (file.createNewFile()) {
					System.out.println("created results/componentCount.txt");

				} else {
					System.err.println("failed to created data/CaseCount.ini");
				}
			}

			BufferedWriter output;
			try {
				output = new BufferedWriter(new FileWriter(file, true));

				output.write("case " + n + " number of component "
						+ posiCigMap.size() + "\n");
				output.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			totalNComponents += posiCigMap.size();
			err += minErr;
			dataFileHeader = String.format(
					"data_ijcai_paper/test%d-positiveData-", n);

			dataFileName = String.format(dataFileHeader + "%d", 8);

			SensorData sdtruth1 = new SensorData(dataFileName, 800, 600);

			dataFileName = String.format(dataFileHeader + "%d", 9);

			SensorData sdtruth2 = new SensorData(dataFileName, 800, 600);

			// if (pt11.getX() > pt12.getX())
			// angle1 = Math.PI + angle1;
			// else if (pt11.getY() < pt12.getY())
			// angle1 = 2 * Math.PI + angle1;
			// if (pt21.getX() > pt22.getX())
			// angle2 = Math.PI + angle2;
			// else if (pt21.getY() < pt22.getY())
			// angle2 = 2 * Math.PI + angle2;

			double angleDiffTruth = sdtruth1.getAngle() - sdtruth2.getAngle();
			double scaleTruth = 1.;

			AffineTransform rotate = new AffineTransform();
			rotate.rotate(factors[0], anchory.getX(), anchory.getY());

			// while (Math.abs(factors[0]) > Math.PI) {
			// if (factors[0] > 0)
			// factors[0] -= Math.PI;
			// else
			// factors[0] += Math.PI;
			// }

			angleDiff += Math.abs(factors[0] - angleDiffTruth);
			scaleTruthDiff += Math.abs(scaleTruth - factors[1]);

		}
		System.out.println("err  " + err/100);
		System.out.println("angle diff  " + angleDiff/100);
		System.out.println("scaleTruthDiff  " + scaleTruthDiff/100);
	}

	/**
	 * match with shape of convex hull
	 * 
	 * @throws Exception
	 */
	public static void testShapeMatchStg() throws Exception {

		for (int n = 2; n < 11; n++) {
			String dataFileHeader = String.format(
					"data/test%d-positiveDataNorm-", n);

			String dataFileName = String.format(dataFileHeader + "%d", 0);

			SensorData sd1 = new SensorData(dataFileName, 800, 600);

			String negativeDataFileHeader = String.format(
					"data/test%d-negativeDataNorm-", n);
			String negativeDataFileName = String.format(negativeDataFileHeader
					+ "%d", 0);

			SensorData negSd1 = new SensorData(negativeDataFileName, 800, 600);

			dataFileName = String.format(dataFileHeader + "%d", 1);
			negativeDataFileName = String.format(negativeDataFileHeader + "%d",
					1);

			SensorData sd2 = new SensorData(dataFileName, 800, 600);
			SensorData negSd2 = new SensorData(negativeDataFileName, 800, 600);

			SeparatedSensorGroups ssg1 = new SeparatedSensorGroups(sd1);
			ssg1.setNegativeSensorIntervals(negSd1.getPositiveIntervals());

			SeparatedSensorGroups ssg2 = new SeparatedSensorGroups(sd2);
			ssg2.setNegativeSensorIntervals(negSd2.getPositiveIntervals());

			List<SensorGroup> filteredGroups1 = ssg1.getFilteredGroups();
			List<SensorGroup> filteredGroups2 = ssg2.getFilteredGroups();

			File dir = new File("results");
			if (!dir.exists()) {
				dir.mkdir();
			}

			StarRelation starRel = new StarRelation(96);

			// get lists of extreme points represent the convex hull
			List<ExtremePoint2D> convexShape1 = ssg1
					.getConvexShape(filteredGroups1);
			List<ExtremePoint2D> convexShape2 = ssg2
					.getConvexShape(filteredGroups2);

			List<ExtremePoint2D> significantShape1 = ssg1
					.getSignificantInflexion(convexShape1, 6, starRel);

			List<ExtremePoint2D> significantShape2 = ssg2
					.getSignificantInflexion(convexShape2, 6, starRel);

			String fileName = String.format(
					"results/test%d-convexShape-%d.png", n, 0);

			saveConvexShape(fileName, convexShape1,
					ssg1.getPositiveIntervalGroups());

			fileName = String.format("results/test%d-convexShape-%d.png", n, 1);
			saveConvexShape(fileName, convexShape2,
					ssg2.getPositiveIntervalGroups());

			fileName = String.format("results/test%d-sigShape-%d.png", n, 0);

			saveConvexShape(fileName, significantShape1,
					ssg1.getPositiveIntervalGroups());

			fileName = String.format("results/test%d-sigShape-%d.png", n, 1);
			saveConvexShape(fileName, significantShape2,
					ssg2.getPositiveIntervalGroups());

		}

	}

	// test erosion
	public static void testErosion() throws Exception {
		for (int n = 222; n < 323; n++) {
			String dataFileHeader = String.format(
					"data_ijcai_paper/test%d-positiveDataNorm-", n);

			String dataFileName = String.format(dataFileHeader + "%d", 0);

			SensorData sd1 = new SensorData(dataFileName, 800, 600);

			String negativeDataFileHeader = String.format(
					"data_ijcai_paper/test%d-negativeDataNorm-", n);
			String negativeDataFileName = String.format(negativeDataFileHeader
					+ "%d", 0);

			SensorData negSd1 = new SensorData(negativeDataFileName, 800, 600);

			dataFileName = String.format(dataFileHeader + "%d", 1);
			negativeDataFileName = String.format(negativeDataFileHeader + "%d",
					1);

			SensorData sd2 = new SensorData(dataFileName, 800, 600);
			SensorData negSd2 = new SensorData(negativeDataFileName, 800, 600);

			SeparatedSensorGroups ssg1 = new SeparatedSensorGroups(sd1);
			ssg1.setNegativeSensorIntervals(negSd1.getPositiveIntervals());

			SeparatedSensorGroups ssg2 = new SeparatedSensorGroups(sd2);
			ssg2.setNegativeSensorIntervals(negSd2.getPositiveIntervals());

			File dir = new File("results/erosion");
			if (!dir.exists()) {
				dir.mkdir();
			}

			List<SensorGroup> sensorGroups1 = ssg1.getPositiveIntervalGroups();
			List<SensorInterval> erotatedIntervalsList1 = new ArrayList<SensorInterval>();
			List<SensorGroup> erotatedSensorGroups1 = new ArrayList<SensorGroup>();
			for (SensorGroup sg : sensorGroups1) {
				HashMap<Integer, List<SensorInterval>> erotatedIntervalMap = ssg1
						.getIntervalMapAfterErosion(sg.getSensorIntervals(), 15);
				for (int k : erotatedIntervalMap.keySet()) {
					for (SensorInterval si : erotatedIntervalMap.get(k)) {
						erotatedIntervalsList1.add(si);
					}
				}
				erotatedSensorGroups1 = ssg1.findSensorGroups(
						erotatedIntervalsList1, true);
			}

			List<SensorGroup> sensorGroups2 = ssg2.getPositiveIntervalGroups();
			List<SensorInterval> erotatedIntervalsList2 = new ArrayList<SensorInterval>();
			List<SensorGroup> erotatedSensorGroups2 = new ArrayList<SensorGroup>();
			for (SensorGroup sg : sensorGroups2) {
				HashMap<Integer, List<SensorInterval>> erotatedIntervalMap = ssg2
						.getIntervalMapAfterErosion(sg.getSensorIntervals(), 15);
				for (int k : erotatedIntervalMap.keySet()) {
					for (SensorInterval si : erotatedIntervalMap.get(k)) {
						erotatedIntervalsList2.add(si);
					}
				}
				erotatedSensorGroups2 = ssg2.findSensorGroups(
						erotatedIntervalsList2, true);
			}

			String fileName = String.format(
					"results/erosion/test%d-erosionShape-%d.png", n, 0);

			testSaveIntervalGroups(erotatedSensorGroups1, fileName);
			fileName = String.format(
					"results/erosion/test%d-erosionShape-%d.png", n, 1);
			testSaveIntervalGroups(erotatedSensorGroups2, fileName);

			fileName = String.format(
					"results/erosion/test%d-originalShape-%d.png", n, 0);

			testSaveIntervalGroups(sensorGroups1, fileName);
			fileName = String.format(
					"results/erosion/test%d-originalShape-%d.png", n, 1);
			testSaveIntervalGroups(sensorGroups2, fileName);

		}
	}

	// test dilation
	public static void testDilation() throws Exception {
		for (int n = 222; n < 323; n++) {
			String dataFileHeader = String.format(
					"data_ijcai_paper/test%d-positiveDataNorm-", n);

			String dataFileName = String.format(dataFileHeader + "%d", 0);

			SensorData sd1 = new SensorData(dataFileName, 800, 600);

			String negativeDataFileHeader = String.format(
					"data_ijcai_paper/test%d-negativeDataNorm-", n);
			String negativeDataFileName = String.format(negativeDataFileHeader
					+ "%d", 0);

			SensorData negSd1 = new SensorData(negativeDataFileName, 800, 600);

			dataFileName = String.format(dataFileHeader + "%d", 1);
			negativeDataFileName = String.format(negativeDataFileHeader + "%d",
					1);

			SensorData sd2 = new SensorData(dataFileName, 800, 600);
			SensorData negSd2 = new SensorData(negativeDataFileName, 800, 600);

			SeparatedSensorGroups ssg1 = new SeparatedSensorGroups(sd1);
			ssg1.setNegativeSensorIntervals(negSd1.getPositiveIntervals());

			SeparatedSensorGroups ssg2 = new SeparatedSensorGroups(sd2);
			ssg2.setNegativeSensorIntervals(negSd2.getPositiveIntervals());

			File dir = new File("results/dilation");
			if (!dir.exists()) {
				dir.mkdir();
			}

			SensorGroup sensorGroup1 = ssg1.positiveIntervalGroupAll;
			List<SensorInterval> dilatedIntervalsList1 = new ArrayList<SensorInterval>();
			List<SensorGroup> dilatedSensorGroups1 = new ArrayList<SensorGroup>();

			HashMap<Integer, List<SensorInterval>> dilatedIntervalMap1 = ssg1
					.getIntervalMapAfterDilation(
							sensorGroup1.getSensorIntervals(), 15,
							sd1.getSensorGap());
			for (int k : dilatedIntervalMap1.keySet()) {
				for (SensorInterval si : dilatedIntervalMap1.get(k)) {
					dilatedIntervalsList1.add(si);
				}
			}
			dilatedSensorGroups1 = ssg1.findSensorGroups(dilatedIntervalsList1,
					true);

			SensorGroup sensorGroup2 = ssg2.positiveIntervalGroupAll;
			List<SensorInterval> dilatedIntervalsList2 = new ArrayList<SensorInterval>();
			List<SensorGroup> dilatedSensorGroups2 = new ArrayList<SensorGroup>();
			HashMap<Integer, List<SensorInterval>> dilatedIntervalMap2 = ssg2
					.getIntervalMapAfterDilation(
							sensorGroup2.getSensorIntervals(), 15,
							sd2.getSensorGap());
			for (int k : dilatedIntervalMap2.keySet()) {
				for (SensorInterval si : dilatedIntervalMap2.get(k)) {
					dilatedIntervalsList2.add(si);
				}
			}
			dilatedSensorGroups2 = ssg2.findSensorGroups(dilatedIntervalsList2,
					true);

			String fileName = String.format(
					"results/dilation/test%d-dilationShape-%d.png", n, 0);

			testSaveIntervalGroups(dilatedSensorGroups1, fileName);
			fileName = String.format(
					"results/dilation/test%d-dilationShape-%d.png", n, 1);
			testSaveIntervalGroups(dilatedSensorGroups2, fileName);

			fileName = String.format(
					"results/dilation/test%d-originalShape-%d.png", n, 0);

			testSaveIntervalGroups(ssg1.getPositiveIntervalGroups(), fileName);
			fileName = String.format(
					"results/dilation/test%d-originalShape-%d.png", n, 1);
			testSaveIntervalGroups(ssg2.getPositiveIntervalGroups(), fileName);

		}
	}

	public static void matchWithDilation() throws Exception {

		double totalErr = 0.;
		int nSingleComponentCase = 0;
		for (int n = 222; n < 323; n++) {

			String dataFileHeader = String.format(
					"data_ijcai_paper/test%d-positiveDataNorm-", n);

			String dataFileName = String.format(dataFileHeader + "%d", 8);

			SensorData sd1 = new SensorData(dataFileName, 800, 600);

			String negativeDataFileHeader = String.format(
					"data_ijcai_paper/test%d-negativeDataNorm-", n);
			String negativeDataFileName = String.format(negativeDataFileHeader
					+ "%d", 8);

			SensorData negSd1 = new SensorData(negativeDataFileName, 800, 600);

			dataFileName = String.format(dataFileHeader + "%d", 9);
			negativeDataFileName = String.format(negativeDataFileHeader + "%d",
					9);

			SensorData sd2 = new SensorData(dataFileName, 800, 600);
			SensorData negSd2 = new SensorData(negativeDataFileName, 800, 600);

			SeparatedSensorGroups ssg1 = new SeparatedSensorGroups(sd1);
			ssg1.setNegativeSensorIntervals(negSd1.getPositiveIntervals());

			SeparatedSensorGroups ssg2 = new SeparatedSensorGroups(sd2);
			ssg2.setNegativeSensorIntervals(negSd2.getPositiveIntervals());

			Point2D anchory = ssg2.positiveIntervalGroupAll.getCentrePoint();

			File dir = new File("results");
			if (!dir.exists()) {
				dir.mkdir();
			}

			SensorGroup sensorGroup1 = ssg1.positiveIntervalGroupAll;
			List<SensorInterval> dilatedIntervalsList1 = new ArrayList<SensorInterval>();
			List<SensorGroup> dilatedSensorGroups1 = new ArrayList<SensorGroup>();

			HashMap<Integer, List<SensorInterval>> dilatedIntervalMap1 = ssg1
					.getIntervalMapAfterDilation(
							sensorGroup1.getSensorIntervals(), 15,
							sd1.getSensorGap());
			for (int k : dilatedIntervalMap1.keySet()) {
				for (SensorInterval si : dilatedIntervalMap1.get(k)) {
					dilatedIntervalsList1.add(si);
				}
			}
			dilatedSensorGroups1 = ssg1.findSensorGroups(dilatedIntervalsList1,
					true);

			SensorGroup sensorGroup2 = ssg2.positiveIntervalGroupAll;
			List<SensorInterval> dilatedIntervalsList2 = new ArrayList<SensorInterval>();
			List<SensorGroup> dilatedSensorGroups2 = new ArrayList<SensorGroup>();
			HashMap<Integer, List<SensorInterval>> dilatedIntervalMap2 = ssg2
					.getIntervalMapAfterDilation(
							sensorGroup2.getSensorIntervals(), 15,
							sd2.getSensorGap());
			for (int k : dilatedIntervalMap2.keySet()) {
				for (SensorInterval si : dilatedIntervalMap2.get(k)) {
					dilatedIntervalsList2.add(si);
				}
			}
			dilatedSensorGroups2 = ssg2.findSensorGroups(dilatedIntervalsList2,
					true);

			// double diff = calculateDirectionChainDis(map1, map2);

			List<SensorGroup> sgList1 = dilatedSensorGroups1;
			List<SensorGroup> sgList2 = dilatedSensorGroups2;

			Collections.sort(sgList1, new sizeComparator());
			Collections.sort(sgList2, new sizeComparator());
			Collections.reverse(sgList1);
			Collections.reverse(sgList2);

			Double[] factors = ssg1.getFactor(sgList1, sgList2, anchory);

			double err = ssg1.getMatchError(ssg1.getPositiveIntervals(),
					ssg1.getNegativeIntervals(), ssg2.getPositiveIntervals(),
					ssg2.getNegativeIntervals(), factors, anchory);
			if (Double.isNaN(err)) {
				System.out.println("NaN err");
				nSingleComponentCase++;
			} else
				totalErr += err;
			// System.out.println("Error is " + err + " Total Error is " +
			// totalErr);

			String fileName = String.format("results/test%d-ini-%d.png", n, 0);

			saveMatchedPicture(fileName, sgList1, sgList2, factors, anchory);

		}
		// System.out.println("Total Error is " + totalErr);
		System.out.println("Mean Error is " + totalErr
				/ (101 - nSingleComponentCase));
	}

	public static void generatePtRegConfigFile(String fileName){
		File dir = new File("configs");
		if (!dir.exists()) {
			dir.mkdir();
		}
		
		
		
	}
	
	public static void main(String[] args) throws Exception {
		long begintime = System.currentTimeMillis();
		testStg1();
		// testShapeMatchStg();
		// testErosion();
		// testDilation();
		// matchWithDilation();
		long endtime = System.currentTimeMillis();
		long costTime = (endtime - begintime);
		System.out.println("time consumed " + costTime + "ms");
	}
}
