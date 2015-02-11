package sn.recover;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import sn.util.GrahamScan;

//Class for representing a group of sensor intervals
//For each interval in this group, there exists one or more intervals in this group which is 
//produced by its adjacent sensor and overlaps with it. 
public class SensorGroup {
	private HashMap<Integer,List<SensorInterval>> sensorIntervals;
	private int id;
	private int sizeRanking;
	private double size;
	private List<Point2D> convexHull;
	private Point2D centroid;

	// private List<Integer> mergableGroupIDList;

	public SensorGroup(HashMap<Integer,List<SensorInterval>> siMap, int id) {
		this.sensorIntervals = siMap;
		this.id = id;
		this.size = 0.;

		List<Point2D> ptList = new ArrayList<Point2D>();
		

		for(List<SensorInterval> siList: siMap.values()){
			for (int i = 0; i < siList.size(); i++) {
				SensorInterval si = siList.get(i);
				ptList.add(si.getStart());
				ptList.add(si.getEnd());
			}
		}
		Point2D[] pts = new Point2D[ptList.size()];
		ptList.toArray(pts);
		
		//System.out.println("n points " + pts.length);
		
		GrahamScan gs = new GrahamScan(pts);
		this.convexHull = new ArrayList<Point2D>((Stack<Point2D>) gs.hull());
		calculateApproximativeSize(this.convexHull);
		calculateCentroid(this.convexHull);
		this.size = Math.abs(this.size);
		
	}

	private void calculateApproximativeSize(List<Point2D> extremePts) {
		
		//if there are only 2 points, take length as size
		if(extremePts.size() == 2){	
			Point2D pt1 = extremePts.get(0);
			Point2D pt2 = extremePts.get(1);
			this.size = Math.sqrt(Math.pow(pt1.getX() - pt2.getX(),2) + Math.pow(pt1.getY() - pt2.getY(),2));
			return;
		}
		
		//if less than one point, size is 0
		if(extremePts.size() <= 1){
			
			this.size = 0.;
			return;
		}
		
		this.size = 0.;

		for (int i = 0; i < extremePts.size() ; i++) {
			int index;
			if(i == extremePts.size()-1)
				index = 0;	
			else
				index = i+1;
			this.size += extremePts.get(i).getX()
					* extremePts.get(index).getY() - extremePts.get(i).getY()
					* extremePts.get(index).getX();
		}
	}

	private void calculateCentroid(List<Point2D> extremePts) {
		
		//if there are only 2 points, take length as size
		if(extremePts.size() == 2){	
			Point2D pt1 = extremePts.get(0);
			Point2D pt2 = extremePts.get(1);
			this.centroid = new Point2D.Double((pt1.getX() + pt2.getX())/2, (pt1.getY() + pt2.getY())/2);
			return;
		}
		
		//if less than one point, size is 0
		if(extremePts.size() == 1){
			
			this.centroid = extremePts.get(0);
			return;
		}
		
		double centreX = 0., centreY = 0.;
		for (int i = 0; i < extremePts.size(); i++) {
			int index;
			if(i == extremePts.size()-1)
				index = 0;
			else
				index = i+1;
			
			centreX += (extremePts.get(i).getX() * extremePts.get(index).getY() - extremePts
					.get(i).getY() * extremePts.get(index).getX())
					* (extremePts.get(i).getX() + extremePts.get(index).getX());
			centreY += (extremePts.get(i).getX() * extremePts.get(index).getY() - extremePts
					.get(i).getY() * extremePts.get(index).getX())
					* (extremePts.get(i).getY() + extremePts.get(index).getY());
		}
		centreX = centreX/(3 * this.size);
		centreY = centreY/(3 * this.size);
		this.centroid = new Point2D.Double(centreX, centreY);
	}
	
	public List<Point2D> getConvexHull() {
		return this.convexHull;
	}

	public double getApproximativeSize() {
		return this.size;
	}

	public Point2D getCentrePoint() {

		return this.centroid;
	}
	
	public int getID() {

		return this.id;
	}
	
	public double getSize() {

		return this.size;
	}
	
	public HashMap<Integer,List<SensorInterval>> getSensorIntervals(){
		return this.sensorIntervals;
	}
	
	public static void main(String[] args){
		Line2D line1 = new Line2D.Double(new Point2D.Double(-1,2), new Point2D.Double(-1,0));
		SensorInterval si1 = new SensorInterval(0, line1);
		Line2D line2 = new Line2D.Double(new Point2D.Double(1,2), new Point2D.Double(1,0));
		SensorInterval si2 = new SensorInterval(1, line2);
		
		List<SensorInterval> siList1 = new ArrayList<SensorInterval>();
		siList1.add(si1);
		
		List<SensorInterval> siList2 = new ArrayList<SensorInterval>();
		siList2.add(si2);
		
		HashMap<Integer,List<SensorInterval>> siMap = new HashMap<Integer,List<SensorInterval>>();
		siMap.put(0,siList1);
		siMap.put(1,siList2);
		
		SensorGroup sg = new SensorGroup(siMap,0);
		
		for(int i = 0 ; i < sg.getConvexHull().size(); i ++){
			
			System.out.println("hull pt " + sg.getConvexHull().get(i).getX() + " " + sg.getConvexHull().get(i).getY());
		}
		
		System.out.println("size " + sg.getSize() + " centroid " + sg.getCentrePoint());
		
	}
}
