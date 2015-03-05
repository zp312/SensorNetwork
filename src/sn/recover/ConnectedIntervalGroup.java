package sn.recover;

import java.util.HashMap;
import java.util.List;

//After a match between two sets of sensor data, 
//ConnectedIntervalGroup is a group of sensor 
//intervals which connected with each other 
//by intersection points
public class ConnectedIntervalGroup {
	private int id;
	private int layer;
	private List<List<SensorInterval>> connectedIntervalList;
	private int parentId;
	private HashMap<Integer,ConnectedIntervalGroup> descendantMap;
	
	public ConnectedIntervalGroup(int id, List<List<SensorInterval>> connectedIntervalList){
		this.id = id;
		this.connectedIntervalList = connectedIntervalList;
		this.parentId = -1;
		this.descendantMap = null;
		this.layer = -1;
	} 
	
	public List<List<SensorInterval>> getComponentList(){
		return this.connectedIntervalList;
	}
	
	public void setLayer(int l){
		this.layer = l;
	}
	
	public void setParent(int id){
		this.parentId = id;
	}
	

	public int getLayer(){
		return this.layer;
	}
}
