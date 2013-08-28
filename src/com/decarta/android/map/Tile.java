/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.map;

import com.decarta.android.map.MapLayer.MapLayerProperty;
import com.decarta.android.util.XYZ;

public class Tile implements Comparable<Tile>{
	
	private MapLayerProperty mapLayerProperty=null; 
	
    public XYZ xyzTK=new XYZ(0,0,0);
	public XYZ xyz=new XYZ(0,0,0);
	public int distanceFromCenter=0;
    
    public Tile(MapLayerProperty mapTypeProperty){
    	this.mapLayerProperty=mapTypeProperty;
    }
	@Override
    public int compareTo(Tile another) {
    	// TODO Auto-generated method stub
		if(distanceFromCenter<another.distanceFromCenter) return -1;
    	else if(distanceFromCenter>another.distanceFromCenter) return 1;
    	else return 0;
    	
	}	
	
	public MapLayerProperty getMapTypeProperty() {
		return mapLayerProperty;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return xyz.x+"_"+xyz.y+"_"+xyz.z+"_"+mapLayerProperty.mapLayerType+"_"+xyzTK.x+"_"+xyzTK.y+"_"+xyzTK.z;
	}
	
	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		if(o==null) return false;
		if(o instanceof Tile){
			Tile t=(Tile)o;
			return xyz.equals(t.xyz) && mapLayerProperty.equals(t.mapLayerProperty) && xyzTK.equals(t.xyzTK);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		int hash = 29 * xyz.hashCode() + mapLayerProperty.hashCode() + 29 * xyzTK.hashCode();
		return hash;
	}
}