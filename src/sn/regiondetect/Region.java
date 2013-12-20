package sn.regiondetect;

import java.awt.Point;
import java.awt.Polygon;

public class Region extends Polygon {
	protected Region _next;
	protected Region _prev;
	protected int _layer;
	protected int _radius;
	protected Point _centre;
	protected boolean _exceedBoundary;
	
	public Region(int[] x, int[] y, int nPts, int radius, int layer , Point centre,boolean exceed){
		super(x, y, nPts);
		_next = null;
		_prev = null;
		_radius = radius;
		_layer = layer;
		_centre = centre;
		_exceedBoundary = exceed;
	}
	
	public boolean isHole(){
		if(_layer%2 == 0)
			return false;
		else
			return true;
	}
	
	public boolean isHead(){
		if(_prev == null)
			return true;
		else 
			return false;
	}
	
	public boolean isExceedBound(){
		return _exceedBoundary;
	}
	
	public Region getPrev(){
		return _prev;
	}
	
	public Region getNext(){
		return _next;
	}
	
	public int getRadius(){
		return _radius;
	}
	
	public Point getCenter(){
		return _centre;
	}
	
	public int getLayer(){
		return _layer;
	}
	
	public void setPrev(Region prev){
		_prev = prev;
	}
	
	public void setNext(Region next){
		_next = next;
	}
	
	@Override
	public String toString(){
		String regionInfo = "";
		int[] x = this.xpoints;
		int[] y = this.ypoints;
		
		regionInfo += ("layer="+_layer+"\n");
		regionInfo += ("Bounding Points\n");
		for(int i = 0; i < x.length; i++){
			regionInfo += ("["+x[i]+","+y[i]+"]\n");
		}
			
		return regionInfo;
	}
	
}
