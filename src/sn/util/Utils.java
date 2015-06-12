package sn.util;

public class Utils {
	
	//get k value from two points
	public static double getK(double x1, double y1, double x2, double y2){
		if(x2 - x1 == 0)
			return Double.POSITIVE_INFINITY;
		return (y2-y1)/(x2-x1);
	}
	
	//get rotation angle
	public static double getRotationAngle(double k, double x1, double y1, double x2, double y2){
		double angle = Math.atan2(k*(x2-x1) - (y2 - y1), k*(y2-y1)+(x2-x1));
		return angle;
	}
}
