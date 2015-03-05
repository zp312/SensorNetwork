package sn.util;

public class LengthRel {
	public static int getLengthRel(double l1, double l2){
		if(l1/l2 <= 0.4){//much shorter
			return 1;
		}
		else if(l1/l2 <= 0.6){//half length
			return 2;
		}
		else if(l1/l2 <= 0.9){//a bit shorter
			return 3;
		}
		
		else if(l1/l2 <= 1.1){//similar length
			return 4;
		}
		
		else if(l1/l2 <= 1.9){//a bit longer
			return 5;
		}
		else if(l1/l2 <= 2.1){//double length
			return 6;
		}
		else//much longer
			return 7;
	};
}
