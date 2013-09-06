package com.decarta.android.map;

import java.util.HashMap;

import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.XYDouble;
import com.decarta.android.util.XYFloat;
import com.decarta.android.util.XYZ;

/**
 * tiles container, also store tiles related parameter.
 */
public class MapLayer{
	public enum MapLayerType{STREET,SATELLITE,TRANSPARENT};
	public static HashMap<MapLayerType,Float> MapLayerType_ImageSizeFactor = new HashMap<MapLayerType,Float>();
	static{
		MapLayerType_ImageSizeFactor.put(MapLayerType.TRANSPARENT, 2f);//transparent层的factor为2，其他为1
	}
	
	private MapLayerProperty mapLayerProperty;
	
	public boolean cleared=true;
	public float mainLayerDrawPercent=0;
	public boolean visible=false;

    public XYZ centerXYZTK=new XYZ(0,0,-1);
	public XYZ centerXYZ=new XYZ(0,0,-1);
	public XYDouble centerXY=null;
	public XYFloat centerDelta=new XYFloat(0,0);
	public float zoomLayerDrawPercent=0;
	
	public MapLayer(MapLayerProperty mapLayerProperty){
		this.mapLayerProperty=mapLayerProperty;
	}
		
	public MapLayerProperty getMapLayerProperty() {
		return mapLayerProperty;
	}

	public static class MapLayerProperty{
		private static HashMap<MapLayerType,MapLayerProperty> MapLayerType_MapLayerProperty=new HashMap<MapLayerType,MapLayerProperty>();
		static{
			for(int i=0;i<MapLayerType.values().length;i++){
				MapLayerType t=MapLayerType.values()[i];
				MapLayerProperty p=new MapLayerProperty(t);
				MapLayerType_MapLayerProperty.put(t, p);
				if(MapLayerType_ImageSizeFactor.containsKey(t)){
					p.tileImageSizeFactor=MapLayerType_ImageSizeFactor.get(t);
				}
				
				LogWrapper.i("MapLayerProperty","mapLayerType,image size factor,hashCode:"+t+","+p.tileImageSizeFactor+","+p.hashCode());
			}
						
		}
		
		
		public static MapLayerProperty getInstance(MapLayerType mapLayerType){
			return MapLayerType_MapLayerProperty.get(mapLayerType);
		}
		private MapLayerProperty(MapLayerType mapLayerType){
			this.mapLayerType=mapLayerType;
		}
		
		public MapLayerType mapLayerType;
		public float tileImageSizeFactor=1;
		public int priority=0;
			
	}
	
	public Tile createTile(){
		return new Tile(this.mapLayerProperty);
	}
}
	
