package sn.recover;

import java.awt.geom.Point2D;

import sn.util.LengthRel;
import sn.util.StarRelation;

public class ExtremePoint2D extends Point2D.Double {

	// public enum ExtremePtType {Normal,Void}

	// private ExtremePtType type;

	public ExtremePoint2D(Point2D pt) {
		super(pt.getX(), pt.getY());
		// this.type = type;
	}

	// public ExtremePtType getType(){
	// return this.type;
	// }

	/**
	 * calculate the qualitative angle change among 3 points
	 * 
	 * @param adjPt1
	 * @param adjPt2
	 * @param starRel
	 * @return
	 */
	public int getAngleDiff(Point2D adjPt1, Point2D adjPt2, StarRelation starRel) {
		int rel1 = starRel.GetStarRelation(adjPt1, this);

		int rel2 = starRel.GetStarRelation(this, adjPt2);

		int nRelations = starRel.getNRelations();
		assert rel1 != nRelations && rel2 != nRelations : "2 or more points are the same";

		int min = Math.min(rel1, rel2);
		int max = Math.max(rel1, rel2);
		int cycleUp = min + nRelations;
		int diffStar = Math.min(Math.abs(max - min), Math.abs(max - cycleUp));
		
		
		//test 
		System.out.println("x1 " + adjPt1.getX() + " y1 " + adjPt1.getY() + " x2 " + this.getX()+ " y2 " + this.getY() + " x3 " + adjPt2.getX() + " y3 " + adjPt2.getY());
		System.out.println("re1 1 " + rel1 + " rel2 " + rel2 + " diff " + diffStar);
		
		return diffStar;
	}

	/**
	 * return qualitative length relation between two edges connected by the
	 * point
	 */
	public int getLengthRel(Point2D adjPt1, Point2D adjPt2) {
		double l1 = this.distance(adjPt1);
		double l2 = this.distance(adjPt2);

		return LengthRel.getLengthRel(l1, l2);

	}

	/**
	 * naive method to calculate diff between two extreme points
	 * 
	 * @param angleDiff1
	 * @param angleDiff2
	 * @param nAngleRels
	 * @param lengthRel1
	 * @param lengthRel2
	 * @param nLengthRels
	 * @return
	 */
	public double getExtremePtDiff(int angleDiff1, int angleDiff2,
			int nAngleRels, int lengthRel1, int lengthRel2, int nLengthRels) {
		double diff = 0.;
		double wAngle = 0.5, wLength = 0.5;
		diff = wAngle * Math.abs((double) (angleDiff1 - angleDiff2))
				/ (double) nAngleRels + wLength
				* Math.abs((double) (lengthRel1 - lengthRel2))
				/ (double) nLengthRels;
		return diff;
	}
}
