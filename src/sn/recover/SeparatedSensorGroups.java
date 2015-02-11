package sn.recover;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sn.debug.ShowDebugImage;
import sn.regiondetect.ComplexRegion;
import sn.util.StarRelation;

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
	private List<SensorGroup> positiveIntervalGroups;
	private List<SensorGroup> negativeIntervalGroups;
	private List<int[]> mergablePositiveGroupPairs;
	private List<int[]> mergableNegativeGroupPairs;

	public SeparatedSensorGroups(SensorData sd) {
		this.positiveIntervals = sd.getPositiveIntervals();
		this.negativeIntervals = sd.getNegativeIntervals();
		this.positiveIntervalMap = this
				.mapIntervalsByID(this.positiveIntervals);
		this.negativeIntervalMap = this
				.mapIntervalsByID(this.negativeIntervals);
		this.positiveIntervalGroups = findSensorGroups(true);
		this.negativeIntervalGroups = findSensorGroups(false);
		this.mergablePositiveGroupPairs = this.findMergablePairs(true);
		this.mergableNegativeGroupPairs = this.findMergablePairs(false);
	}

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

	private List<SensorGroup> findSensorGroups(boolean positive) {
		// Intervals which has not been assigned to a group
		List<SensorInterval> unclassifiedIntervals;
		if (positive) {
			unclassifiedIntervals = this.positiveIntervals;
		} else {

			unclassifiedIntervals = this.negativeIntervals;
		}
		// Intervals which has been assigned to a group but
		// their adjacent intervals are not yet searched
		List<SensorInterval> ongoingIntervals;

		// intervals assigned to a specific group
		HashMap<Integer, List<SensorInterval>> intervalGroup;

		// A list of all positive sensor groups for the given data
		List<SensorGroup> sensorGroups = new ArrayList<SensorGroup>();

		if (positive) {
			if (this.positiveIntervals.isEmpty())
				return null;
		} else {
			if (this.negativeIntervals.isEmpty())
				return null;
		}
		int groupID = 0;
		while (!unclassifiedIntervals.isEmpty()) {
			intervalGroup = new HashMap<Integer, List<SensorInterval>>();
			ongoingIntervals = new ArrayList<SensorInterval>();

			ongoingIntervals.add(unclassifiedIntervals.get(0));
			unclassifiedIntervals.remove(0);
			// List<SensorInterval> unclassifiedIntervalsUpdated = new
			// ArrayList<SensorInterval>();

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

				boolean pairAdded = false;

				for (int key : intervalMap1.keySet()) {
					if (pairAdded)
						break;
					if (intervalMap2.containsKey(key)) {
						List<SensorInterval> intervals1 = intervalMap1.get(key);
						List<SensorInterval> intervals2 = intervalMap2.get(key);

						List<Point2D[]> gapIntervals = findGapIntervals(
								intervals1, intervals2);

						List<SensorInterval> intervals;
						if (positive) {
							assert (this.positiveIntervalMap.containsKey(key)) : "inconsistent sensor data from id : "
									+ key;
							intervals = this.positiveIntervalMap.get(key);
						} else {
							assert (this.negativeIntervalMap.containsKey(key)) : "inconsistent sensor data from id : "
									+ key;
							intervals = this.negativeIntervalMap.get(key);

						}
						for (Point2D[] ptArray : gapIntervals) {
							boolean interIntervalFound = false;
							for (SensorInterval interval : intervals) {
								if (ptArray[0].getY() < interval.getEnd()
										.getY()
										&& ptArray[1].getY() < interval
												.getEnd().getY()) {
									interIntervalFound = true;
								}
							}
							if (!interIntervalFound) {
								int[] pair = { sg1.getID(), sg2.getID() };
								mergablePairs.add(pair);
								pairAdded = true;

								System.out.println(groupType
										+ "mergable pair " + pair[0] + " "
										+ pair[1]);
								break;
							}
						}
					}

					// TODO if(intervalMap2.containsKey(key+-1))
				}
			}
		}

		System.out.println(groupType + "mergable pair total amount : "
				+ mergablePairs.size());
		return mergablePairs;
	}

	/**
	 * 
	 * @return
	 */
	private List<Point2D[]> findGapIntervals(List<SensorInterval> list1,
			List<SensorInterval> list2) {
		List<Point2D[]> gapIntervals = new ArrayList<Point2D[]>();

		Collections.sort(list1, new YComparator());
		Collections.sort(list2, new YComparator());
		Collections.reverse(list2);

		for (SensorInterval si1 : list1) {
			double lastY = Double.NaN;
			for (SensorInterval si2 : list2) {
				if (si1.getEnd().getY() > si2.getEnd().getY()) {
					if (si2.getEnd().getY() == lastY)
						break;
					Point2D[] interval = { si2.getEnd(), si1.getEnd() };
					gapIntervals.add(interval);
					lastY = si2.getEnd().getY();
					break;
				}
			}
		}

		Collections.reverse(list1);
		Collections.reverse(list2);

		for (SensorInterval si2 : list1) {
			double lastY = Double.NaN;
			for (SensorInterval si1 : list2) {
				if (si2.getEnd().getY() > si1.getEnd().getY()) {
					if (si1.getEnd().getY() == lastY)
						break;
					Point2D[] interval = { si1.getEnd(), si2.getEnd() };
					gapIntervals.add(interval);
					lastY = si1.getEnd().getY();
					break;
				}
			}
		}

		return gapIntervals;
	}

	// Compare the Y between two points.
	private class YComparator implements Comparator<SensorInterval> {
		@Override
		public int compare(SensorInterval si1, SensorInterval si2) {
			return (new Double(si1.getStart().getY())).compareTo(new Double(si2
					.getStart().getY()));
		}
	}

	// Compare the size.
	private class sizeComparator implements Comparator<SensorGroup> {
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
			HashMap<Integer, Integer> mergeAddressMap) {

		if (mergeAddressMap.containsKey(id1)) {
			if (mergeAddressMap.containsKey(id2)) {
				int rootID1 = mergeAddressMap.get(id1);
				int rootID2 = mergeAddressMap.get(id2);
				mergeRegions(rootID1, rootID2, mergeAddressMap);
			}

			else {
				int rootID1 = mergeAddressMap.get(id1);
				mergeAddressMap.put(id2, rootID1);
			}

		}

		else if (mergeAddressMap.containsKey(id2)) {
			int rootID2 = mergeAddressMap.get(id2);
			mergeAddressMap.put(id1, rootID2);
		}

		else {
			mergeAddressMap.put(id2, id1);
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
							rootIntervalMap.put(k, candidateIntervalMap.get(k));
						}
					}

				} else {

					SensorGroup oldSg = ssg.getPositiveIntervalGroups().get(
							rootID);

					HashMap<Integer, List<SensorInterval>> sensorIntervalMap = new HashMap<Integer, List<SensorInterval>>();
					sensorIntervalMap.putAll(oldSg.getSensorIntervals());

					SensorGroup newSg = new SensorGroup(sensorIntervalMap,
							rootID);
					mergedGroupMap.put(rootID, newSg);

				}
			}

			else {
				SensorGroup oldSg = ssg.getPositiveIntervalGroups().get(id);

				HashMap<Integer, List<SensorInterval>> sensorIntervalMap = new HashMap<Integer, List<SensorInterval>>();
				sensorIntervalMap.putAll(oldSg.getSensorIntervals());

				SensorGroup newSg = new SensorGroup(sensorIntervalMap, id);
				mergedGroupMap.put(id, newSg);
			}
		}
		System.out.println("merged map size " + mergedGroupMap.size()
				+ " original ssg size "
				+ ssg.getPositiveIntervalGroups().size());
		return mergedGroupMap;
	}

	private double calculateDirectionChainDis(
			HashMap<Integer, SensorGroup> map1,
			HashMap<Integer, SensorGroup> map2) {

		double diff = 0.;

		List<SensorGroup> sgList1 = new ArrayList<SensorGroup>(map1.values());
		List<SensorGroup> sgList2 = new ArrayList<SensorGroup>(map2.values());

		Collections.sort(sgList1, new sizeComparator());
		Collections.sort(sgList2, new sizeComparator());

		double totalSize1 = 0.;
		double totalSize2 = 0.;

		int[] starRelations1 = new int[sgList1.size()];
		int[] starRelations2 = new int[sgList2.size()];
		int[] directionDiff1;
		int[] directionDiff2;

		StarRelation star = new StarRelation(12);

		// get star relations and total size of all sensor groups
		for (int i = 0; i < sgList1.size() - 1; i++) {
			SensorGroup sg1 = sgList1.get(i);
			SensorGroup sg2 = sgList1.get(i + 1);
			starRelations1[i] = star.GetStarRelation(sg1.getCentrePoint(),
					sg2.getCentrePoint());
			totalSize1 += sg1.getSize();
		}

		// if (sgList1.size() > 0)
		totalSize1 += sgList1.get(sgList1.size() - 1).getSize();

		for (int i = 0; i < sgList2.size() - 1; i++) {
			SensorGroup sg1 = sgList2.get(i);
			SensorGroup sg2 = sgList2.get(i + 1);
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
			double diff1, diff2;
			double err;

			if (i >= directionDiff1.length)
				diff1 = 0;
			else
				diff1 = directionDiff1[i]
						* (sgList1.get(i).getSize() / totalSize1);
			if (i >= directionDiff2.length)
				diff2 = 0;
			else
				diff2 = directionDiff2[i]
						* (sgList1.get(i).getSize() / totalSize2);
			err = Math.abs(diff1 - diff2);
			diff += err;
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

	private void performInitialMatch() {

	}

	public void addIntervalsToGraphic(Graphics2D g2d,
		List<SensorGroup> sensorGroups, int xOffset, int yOffset) {
		// Draw components
		int nGroups = sensorGroups.size();
		for (int i = 0; i < nGroups; i++) {
			SensorGroup curGroup = sensorGroups.get(i);
			// set a random color for each new group
			int R = (int) (Math.random() * 256);
			int G = (int) (Math.random() * 256);
			int B = (int) (Math.random() * 256);
			Color randomColor = new Color(R, G, B);
			g2d.setColor(randomColor);
			// draw group id
			// SensorInterval si = siList.get(0);
			g2d.drawString(String.valueOf(curGroup.getID()), (int)curGroup.getCentrePoint().getX()
			 + xOffset, (int)curGroup.getCentrePoint().getY() + yOffset);
			for (int key : curGroup.getSensorIntervals().keySet()) {
				//g2d.setColor(randomColor);
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

	public static int getBit(int i, int position) {
		return (i >> position) & 1;
	}

	public void initialMatch(SeparatedSensorGroups ssg) {
		int n = this.mergablePositiveGroupPairs.size();
		// iterate over all 2^n possibilities of merging interval groups
		for (int i = 0; i < Math.pow(2, n); i++) {
			// Represents the selected merging sensor groups, <id,List<merged
			// ids>>
			// HashMap<Integer, List<Integer>> mergeMap1 = new HashMap<Integer,
			// List<Integer>>();
			HashMap<Integer, Integer> mergeAddressMap1 = new HashMap<Integer, Integer>();
			for (int j = 0; j < n; j++) {
				if (getBit(i, j) == 1) {
					int[] mergablePair = this.mergablePositiveGroupPairs.get(j);
					int id1 = mergablePair[0];
					int id2 = mergablePair[1];
					setMergeInfo(id1, id2, mergeAddressMap1);
				}
			}

			int m = ssg.mergablePositiveGroupPairs.size();
			for (int p = 0; p < Math.pow(2, m); p++) {
				// HashMap<Integer, List<Integer>> mergeMap2 = new
				// HashMap<Integer, List<Integer>>();
				HashMap<Integer, Integer> mergeAddressMap2 = new HashMap<Integer, Integer>();

				for (int j = 0; j < m; j++) {
					if (getBit(i, j) == 1) {
						int[] mergablePair = ssg.mergablePositiveGroupPairs
								.get(j);
						int id1 = mergablePair[0];
						int id2 = mergablePair[1];

						setMergeInfo(id1, id2, mergeAddressMap2);
					}
				}

				HashMap<Integer, SensorGroup> map1 = constructMergedGroup(
						mergeAddressMap1, this);
				HashMap<Integer, SensorGroup> map2 = constructMergedGroup(
						mergeAddressMap2, ssg);
				double diff = calculateDirectionChainDis(map1, map2);
				System.out.println(diff + " times i " + i + " p " + p);
			}
		}

	}

	// possible initial rotation factors in radians
	public List<Double> getInitialRotationFactor(SeparatedSensorGroups sgs) {

		return null;
	}

	public List<Point2D> getInitialTranslationalFactor(SeparatedSensorGroups sgs) {

		return null;
	}

	public List<Double> getInitialScaleFactor(SeparatedSensorGroups sgs) {

		return null;
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
	public static void testDrawIntervalGroups(String filename) throws Exception {
		ShowDebugImage frame = null;
		int width = 1000;
		int height = 1000;

		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);

		SensorData positiveData = new SensorData(
				filename, 800, 600);
		// SensorData negativeData = new
		// SensorData("data/test7-negativeDataNorm-0", 800, 600);
		SeparatedSensorGroups ssg1 = new SeparatedSensorGroups(positiveData);
		ssg1.addIntervalsToGraphic(g2d, ssg1.getPositiveIntervalGroups(), 100,
				200);
		// if (showNeg) {
		// negativeData.addIntervalsToGraphic(g2d,
		// negativeData.getPositiveIntervals(), false, Color.BLACK);
		// }
		frame = new ShowDebugImage("Regions with intervals", img);
		frame.refresh(img);
	}

	public static void main(String[] args) {
//		SensorData sd1 = new SensorData("data/test1-positiveDataNorm-0", 800,
//				600);
//		SensorData sd2 = new SensorData("data/test1-positiveDataNorm-1", 800,
//				600);
//
//		SeparatedSensorGroups ssg1 = new SeparatedSensorGroups(sd1);
//		SeparatedSensorGroups ssg2 = new SeparatedSensorGroups(sd2);
//
//		List<SensorGroup> posiSgs = ssg1.positiveIntervalGroups;
//
//		System.out.println("pos size " + posiSgs.size());
//
//		ssg1.initialMatch(ssg2);
		try {
			testDrawIntervalGroups("data/test1-positiveDataNorm-0");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
