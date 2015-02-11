package sn.util;

import java.awt.geom.Point2D;
import java.io.ObjectInputStream.GetField;

public class StarRelation {
	private double diff;//angle gap between two region in degree
	private int nRelations;
	
	public StarRelation(int nRegions) {
		this.diff = (double)2*Math.PI/(double)nRegions;
		this.nRelations = 2 * nRegions;
	}
	
	public int getNRelations(){
		return this.nRelations;
	}
	
	public int GetStarRelation(Point2D pt1, Point2D pt2){
		if(pt1.getX() != pt2.getX()){
			double angle = Math.atan((pt1.getY()-pt2.getY())/(pt2.getX()-pt1.getX()));
			if(pt1.getX() > pt2.getX())
				angle += Math.PI;
			return (int)Math.ceil(angle/this.diff) + (int)Math.floor(angle/this.diff);
		}
			
		else{
			if(pt1.getY() == pt2.getY()){
				return this.nRelations;//equal
			}
			else if(pt1.getY() > pt2.getY()){
				return 0;
			}
			else{
				return (int)Math.ceil(Math.PI/this.diff) + (int)Math.floor(Math.PI/this.diff);
			}
		}
		
	}
	
	public static void main(String args[]){
		
		Point2D pt1 = new Point2D.Double(1, 1);
		Point2D pt2 = new Point2D.Double(2, 0);
		StarRelation star = new StarRelation(12);
		int relation = star.GetStarRelation(pt1, pt2);
		System.out.println(relation);
	}
}

