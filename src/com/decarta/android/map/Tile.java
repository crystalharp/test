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
	
	public XYZ xyz=new XYZ(0,0,0);
	public int distanceFromCenter=0;
    
    public Tile(MapLayerProperty mapTypeProperty){
    	this.mapLayerProperty=mapTypeProperty;
    }
	@Override
    public int compareTo(Tile another) {
		if(distanceFromCenter<another.distanceFromCenter) return -1;
    	else if(distanceFromCenter>another.distanceFromCenter) return 1;
    	else return 0;
    	
	}	
	
	public MapLayerProperty getMapTypeProperty() {
		return mapLayerProperty;
	}
	
	@Override
	public String toString() {
		return xyz.x+"_"+xyz.y+"_"+xyz.z+"_"+mapLayerProperty.mapLayerType;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o==null) return false;
		if(o instanceof Tile){
			Tile t=(Tile)o;
			return xyz.equals(t.xyz) && mapLayerProperty.equals(t.mapLayerProperty);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int hash = 29 * xyz.hashCode() + mapLayerProperty.hashCode();
		return hash;
	}
}
