/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.graphics.Color;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYDouble;
import com.decarta.android.util.XYInteger;

public class ItemizedOverlay {
	/**
	 * 
	 * define which corner of the text frame is used to calculate the offset.
	 * e.g., when top left corner of pin is {x,y}, text frame size is {10,20}, offset ={5,4}, relativeTo=BOTTOM_RIGHT, 
	 * then the left top corner of this text frame is at {x+5-10, y+4-20};
	 */
	public enum OffsetReference{TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT};
	
    public static String MY_LOCATION_OVERLAY = "my_location";
    public static String POI_OVERLAY = "poi";
    public static String TRAFFIC_OVERLAY = "traffic";
    public static String LINE_OVERLAY = "line";
    public static String PIN_OVERLAY = "pin";
    
	public static final int ZOOM_LEVEL=13;
	private static int GRANULARITY=20;
		
	public static int TOUCH_RADIUS=100;
	public static int SNAP_BUFFER=0;
	
	public static int OVERLAY_CLUSTER_BORDER_SIZE=2;
	public static int OVERLAY_CLUSTER_ROUND_RADIUS=8;
	public static int OVERLAY_CLUSTER_TEXT_SIZE=12;
	public static int OVERLAY_CLUSTER_TEXT_OFFSET_X=4;
	public static int OVERLAY_CLUSTER_TEXT_OFFSET_Y=4;
	public static boolean OVERLAY_CLUSTER_TEXT_ANTIALIAS=true;
	public static boolean OVERLAY_CLUSTER_BORDER_ANTIALIAS=true;
	public static boolean OVERLAY_CLUSTER_INNER_ANTIALIAS=false;
	/*
	 * 标志字段, 判断当前Overlay在"上一步","下一步", "点击步骤图标"
	 * 三种操作中将Overlay置于14级别
	 */
	public boolean isShowInPreferZoom = false;
	
	static double MIN_DIST[];
	static double ZOOM_SCALE[];
	static{
		ZOOM_SCALE=new double[21];
		MIN_DIST=new double[21];
		for(int i=0;i<21;i++){
			ZOOM_SCALE[i]=Math.pow(2, i-ZOOM_LEVEL);
			MIN_DIST[i]=(GRANULARITY*GRANULARITY)/(ZOOM_SCALE[i]*ZOOM_SCALE[i]);
		}
		
	}
	
	/**
	 * name as ID, should be unique and composed of [a-zA-Z_1-9] and begin with [a-zA-Z_]
	 */
	private String name;
	private ArrayList<OverlayItem> overlayItems = new ArrayList<OverlayItem>();
	
	private HashMap<XYInteger,ArrayList<Cluster>> pinIdxs[]=null;
	
	//variables for clustering
	private boolean clustering=false;
	private ClusterTouchEventListener clusterTouchEventListener=null;
	//cluster size text will be drawn at clusterIcon topleft.x+clusterTextOffset.x, clusterIcon topleft.x.y+clusterTextOffset.y
	private XYInteger clusterTextOffset=new XYInteger(0,0);
	private OffsetReference clusterTextOffsetRelativeTo=OffsetReference.TOP_LEFT;
	private int clusterBackgroundColor=Color.rgb(255, 0, 0);
	private int clusterTextColor=Color.rgb(255, 255, 255);
	private int clusterBorderColor=Color.rgb(255, 255, 255);
	
	private Object idxLock=new Object();
	
	/**
	 * constructure of ItemizedOverlay
	 * @param overlayName overlayName should be unique and composed of [a-zA-Z_0-9] and begin with [a-zA-Z_]
	 */
	public ItemizedOverlay(String overlayName) throws APIException{
		if(overlayName==null || !overlayName.matches("^[a-zA-Z_][a-zA-Z_0-9]*$")){
			throw new APIException("overlayName must be composed of [a-zA-Z_0-9] and begin with [a-zA-Z_]");
		}
		this.name=overlayName;
	}
	
	private class ClusterNE{
		Cluster cluster;
		XYInteger ne;
		ClusterNE(Cluster cluster, XYInteger ne){
			this.cluster=cluster;
			this.ne=ne;
		}
	}
	
	private void generalizePins(int zoomLevel){
        //Check the point index
		if(pinIdxs!=null && pinIdxs[zoomLevel]!=null) return;
		if(pinIdxs==null){
			pinIdxs=new HashMap[21];
			for(int i=0;i<21;i++){
				pinIdxs[i]=null;
			}
		}
		
		//LogWrapper.i("ItemizedOverlay", "generalizePins zoomLevel:"+zoomLevel);
		
		double scale=ZOOM_SCALE[zoomLevel];
		double minDist=MIN_DIST[zoomLevel];
	    HashMap<XYInteger, ArrayList<Cluster>> pinIdx=new HashMap<XYInteger,ArrayList<Cluster>>();
		ArrayList<ClusterNE> clusters=new ArrayList<ClusterNE>();
    	
    	for (int i = 0; i < overlayItems.size(); i++) {
			OverlayItem pin = overlayItems.get(i);
			if (pin.getMercXY() == null) {
				continue;
			}
			
			XYInteger ne = Util.mercXYToNE(new XYDouble(pin.getMercXY().x*scale,pin.getMercXY().y*scale));
//			boolean included=false;
//			for(int j=0;j<clusters.size();j++){
//				if(clusters.get(j).cluster.clusterPins.size()==0){
//					LogWrapper.e("ItemizedOverlay","generalizePins cluster empty");
//				}
//				
//				if(!ne.equals(clusters.get(j).ne)){
//					continue;
//				}
//				
//				XYDouble refMercXY=clusters.get(j).cluster.refMercXY;
//				double dist = (pin.getMercXY().x - refMercXY.x)*(pin.getMercXY().x - refMercXY.x)+(pin.getMercXY().y - refMercXY.y)*(pin.getMercXY().y - refMercXY.y);
//				if(dist<minDist){
//					clusters.get(j).cluster.clusterPins.add(pin);
//					included=true;
//					break;
//				}
//				
//			}
//    		
//			if(included) continue;
			
			ArrayList<OverlayItem> newClusterPins=new ArrayList<OverlayItem>();
			newClusterPins.add(pin);//只有一个元素？
			XYDouble refMercXY=new XYDouble(pin.getMercXY().x,pin.getMercXY().y);
			clusters.add(new ClusterNE(new Cluster(refMercXY,newClusterPins),ne));
		}
	    		
	    for(int i=0;i<clusters.size();i++){
    		if(!pinIdx.containsKey(clusters.get(i).ne)){
    			pinIdx.put(clusters.get(i).ne, new ArrayList<Cluster>());
    		}
    		pinIdx.get(clusters.get(i).ne).add(clusters.get(i).cluster);
    	}
    	pinIdxs[zoomLevel]=pinIdx;
		
    	//LogWrapper.i("ItemizedOverlay","generalizePins zoomLevel,genLevel,indexLevel:"+zoomLevel+","+genLevel+","+idxLevel);
			
    }
	
	private void removeFromIndex(OverlayItem pin){
		if(pinIdxs==null) return;
		if(pin.getMercXY()==null) return;
		XYInteger ne20 = Util.mercXYToNE(new XYDouble(pin.getMercXY().x*ZOOM_SCALE[20],pin.getMercXY().y*ZOOM_SCALE[20]));
		for(int z=0;z<21;z++){
			if(pinIdxs[z]==null) continue;
			XYInteger ne=new XYInteger(ne20.x>>(20-z),ne20.y>>(20-z));
			ArrayList<Cluster> clusters=pinIdxs[z].get(ne);
			if(clusters==null){
				LogWrapper.e("ItemizedOverlay", "removeFromIndex zoom "+z+" cannot find owner clusters");
				continue;
			}
			Cluster cluster=null;
			for(int i=0;i<clusters.size();i++){
				XYDouble refMercXY=clusters.get(i).refMercXY;
				double dist = (pin.getMercXY().x - refMercXY.x)*(pin.getMercXY().x - refMercXY.x)+(pin.getMercXY().y - refMercXY.y)*(pin.getMercXY().y - refMercXY.y);
				if(dist<MIN_DIST[z]){
					cluster=clusters.get(i);
					break;
				}
			}
			
			if(cluster==null){
				LogWrapper.e("ItemizedOverlay", "removeFromIndex zoom "+z+" cannot find owner cluster");
				continue;
			}
			boolean removed=cluster.clusterPins.remove(pin);
			if(!removed){
				LogWrapper.e("ItemizedOverlay","removeFromIndex zoom "+z+" owner cluster do not contain pin");
				continue;
			}
			//LogWrapper.i("ItemizedOverlay", "removeFromIndex zoom "+z+" pin:"+pin.getMessage());
			if(cluster.clusterPins.size()==0){
				clusters.remove(cluster);
				if(clusters.size()==0){
					pinIdxs[z].remove(ne);
				}
			}
		}
	}
	
	private void addToIndex(OverlayItem pin){
		if(pinIdxs==null) return;
		if(pin.getMercXY()==null) return;
		XYInteger ne20 = Util.mercXYToNE(new XYDouble(pin.getMercXY().x*ZOOM_SCALE[20],pin.getMercXY().y*ZOOM_SCALE[20]));
		for(int z=0;z<21;z++){
			if(pinIdxs[z]==null) continue;
			XYInteger ne=new XYInteger(ne20.x>>(20-z),ne20.y>>(20-z));
			if(!pinIdxs[z].containsKey(ne)){
				pinIdxs[z].put(ne, new ArrayList<Cluster>());
			}
			ArrayList<Cluster> clusters=pinIdxs[z].get(ne);
			boolean included=false;
			for(int j=0;j<clusters.size();j++){
				if(clusters.get(j).clusterPins.size()==0){
					LogWrapper.e("ItemizedOverlay","addToIndex cluster empty");
				}
				XYDouble refMercXY=clusters.get(j).refMercXY;
				double dist = (pin.getMercXY().x - refMercXY.x)*(pin.getMercXY().x - refMercXY.x)+(pin.getMercXY().y - refMercXY.y)*(pin.getMercXY().y - refMercXY.y);
				if(dist<MIN_DIST[z]){
					clusters.get(j).clusterPins.add(pin);
					included=true;
					break;
				}
				
			}
    		
			if(included) continue;
			
			ArrayList<OverlayItem> newClusterPins=new ArrayList<OverlayItem>();
			newClusterPins.add(pin);
			XYDouble refMercXY=new XYDouble(pin.getMercXY().x,pin.getMercXY().y);
			clusters.add(new Cluster(refMercXY,newClusterPins));
		}
	}
	
	void changePinPos(OverlayItem pin, XYDouble oldMercXY){
		synchronized(idxLock){
			if(pinIdxs==null) return;
			XYInteger ne20 = pin.getMercXY()==null?null:Util.mercXYToNE(new XYDouble(pin.getMercXY().x*ZOOM_SCALE[20],pin.getMercXY().y*ZOOM_SCALE[20]));
			XYInteger oldne20=oldMercXY==null?null:Util.mercXYToNE(new XYDouble(oldMercXY.x*ZOOM_SCALE[20],oldMercXY.y*ZOOM_SCALE[20]));
			for(int z=0;z<21;z++){
				if(pinIdxs[z]==null) continue;
				
				ArrayList<Cluster> clusters=null;
				Cluster cluster=null;
				XYInteger ne=ne20==null?null:new XYInteger(ne20.x>>(20-z),ne20.y>>(20-z));
				ArrayList<Cluster> oldClusters=null;
				Cluster oldCluster=null;
				XYInteger oldne=oldne20==null?null:new XYInteger(oldne20.x>>(20-z),oldne20.y>>(20-z));
				
				if(oldMercXY!=null){
					oldClusters=pinIdxs[z].get(oldne);
					if(oldClusters==null){
						LogWrapper.e("ItemizedOverlay","changePinPos zoom "+z+" cannot find oldClusters");
					}else{
						for(int j=0;j<oldClusters.size();j++){
							if(oldClusters.get(j).clusterPins.size()==0){
								LogWrapper.e("ItemizedOverlay","changePinPos zoom "+z+" oldClusters cluster empty");
								continue;
							}
							XYDouble refMercXY=oldClusters.get(j).refMercXY;
							double dist = (oldMercXY.x - refMercXY.x)*(oldMercXY.x - refMercXY.x)+(oldMercXY.y - refMercXY.y)*(oldMercXY.y - refMercXY.y);
							if(dist<MIN_DIST[z]){
								oldCluster=oldClusters.get(j);
								break;
							}
							
						}
					}
					if(oldCluster==null){
						LogWrapper.e("ItemizedOverlay","changePinPos zoom "+z+" cannot find oldCluster");
					}
				}
				
				if(pin.getMercXY()!=null){
					if(!pinIdxs[z].containsKey(ne)){
						pinIdxs[z].put(ne, new ArrayList<Cluster>());
					}
					clusters=pinIdxs[z].get(ne);
					boolean included=false;
					for(int j=0;j<clusters.size();j++){
						if(clusters.get(j).clusterPins.size()==0){
							LogWrapper.e("ItemizedOverlay","changePinPos zoom "+z+" cluster empty");
						}
						XYDouble refMercXY=clusters.get(j).refMercXY;
						double dist = (pin.getMercXY().x - refMercXY.x)*(pin.getMercXY().x - refMercXY.x)+(pin.getMercXY().y - refMercXY.y)*(pin.getMercXY().y - refMercXY.y);
						if(dist<MIN_DIST[z]){
							cluster=clusters.get(j);
							if(cluster!=oldCluster){
								cluster.clusterPins.add(pin);
							}
							included=true;
							break;
						}
						
					}
					
					if(included) continue;
					ArrayList<OverlayItem> newClusterPins=new ArrayList<OverlayItem>();
					newClusterPins.add(pin);
					XYDouble refMercXY=new XYDouble(pin.getMercXY().x,pin.getMercXY().y);
					cluster=new Cluster(refMercXY,newClusterPins);
					clusters.add(cluster);
					
				}
				
				if(oldCluster!=null && oldCluster!=cluster){
					boolean removed=oldCluster.clusterPins.remove(pin);
					if(!removed){
						LogWrapper.e("ItemizedOverlay","changePinPos zoom "+z+" oldCluster do not contain pin");
						continue;
					}
					if(oldCluster.clusterPins.size()==0){
						oldClusters.remove(oldCluster);
						if(oldClusters.size()==0){
							pinIdxs[z].remove(oldne);
						}
					}
					
				}
			}
		}
	}
		
	public ArrayList<ArrayList<OverlayItem>> getVisiblePins(int zoomLevel, ArrayList<Tile> tiles) {
		ArrayList<ArrayList<OverlayItem>> clusters=new ArrayList<ArrayList<OverlayItem>>();
		
		ArrayList<XYInteger> overlapXYs=new ArrayList<XYInteger>();
		for(int i=0;i<tiles.size();i++){
			Tile tile=tiles.get(i);
			List<XYInteger> xys=Util.findOverlapXYs(tile.xyz, zoomLevel);
			if(!overlapXYs.contains(xys.get(0))){
				overlapXYs.addAll(xys);
			}
			
		}
		
		synchronized(idxLock){
			generalizePins(zoomLevel);
			HashMap<XYInteger,ArrayList<Cluster>> pinIdx=pinIdxs[zoomLevel];
			for(int j=0;j<overlapXYs.size();j++){
				XYInteger xy=overlapXYs.get(j);
				if(pinIdx.containsKey(xy)){
					ArrayList<Cluster> clustersL=pinIdx.get(xy);
					for(int k=0;k<clustersL.size();k++){
						clusters.add(clustersL.get(k).clusterPins);
					}
				}
			}
			
		}
		return clusters;

    }
	
	public int size(){
		synchronized(idxLock){
			return overlayItems.size();
		}
	}
	
	public OverlayItem get(int i){
		synchronized(idxLock){
			if(i<overlayItems.size() && i>=0)
				return overlayItems.get(i);
			else return null;
		}
		
	}
	
	
	
	public boolean addOverlayItem(OverlayItem overlayItem){
		synchronized(idxLock){
			if(overlayItems.add(overlayItem)){
				overlayItem.setOwnerOverlay(this);
				addToIndex(overlayItem);
				return true;
			}
			else return false;
		}
	}
	
	/**
	 * remove all pins of this overlay
	 */
	public void clear(){
		synchronized(idxLock){
			overlayItems.clear();
			resetPinIdxs();
		}
	}
	
	/**
	 * remove the overlay item. 
	 * @param location
	 */
	public OverlayItem remove(int location){
		synchronized(idxLock){
			if(location<overlayItems.size() && location>=0){
				OverlayItem pin=overlayItems.remove(location);
				if(pin!=null){
					removeFromIndex(pin);
				}
				return pin;
			}else return null;
			
		}
	}
	
	/**
	 * remove the overlay item. 
	 * @param overlayItem
	 */
	public boolean remove(OverlayItem overlayItem){
		synchronized(idxLock){
			if(overlayItems.remove(overlayItem)){
				removeFromIndex(overlayItem);
				return true;
			}else return false;
			
		}
	}
	
	public String getName() {
		return name;
	}
	
	public void resetPinIdxs(){
		pinIdxs=null;
	}
	
	
	
	/**
	 * Define touch event listener of pin cluster
	 * When draw the cluster pin, it uses the cluster's first pin for position, rotation tilt and icon.
	 * 
	 */
	public interface ClusterTouchEventListener extends
			com.decarta.android.event.EventListener {
		public void onTouchEvent(ItemizedOverlay overlay, ArrayList<OverlayItem> cluster);
	}


	/**
	 * if the overlay enable clustering 
	 * @return
	 */
	public boolean isClustering() {
		return clustering;
	}
	
	/**
	 * set if the overlay enable clustering
	 * @param clustering
	 */
	public void setClustering(boolean clustering) {
		this.clustering = clustering;
	}

	/**
	 * get the on touch event listener for cluster
	 * @return
	 */
	public ClusterTouchEventListener getClusterTouchEventListener() {
		return clusterTouchEventListener;
	}

	/**
	 * set the on touch event listener for cluster
	 * @param clusterTouchEventListener
	 */
	public void setClusterTouchEventListener(
			ClusterTouchEventListener clusterTouchEventListener) {
		this.clusterTouchEventListener = clusterTouchEventListener;
	}
	
	public XYInteger getClusterTextOffset() {
		return clusterTextOffset;
	}
	
	/**
	 * Set the cluster number text offset.
	 * number will be drawn at clusterIcon topleft.x+clusterTextOffset.x, clusterIcon topleft.y+clusterTextOffset.y
	 * @param clusterTextOffset
	 */
	public void setClusterTextOffset(XYInteger clusterTextOffset) {
		this.clusterTextOffset = clusterTextOffset;
	}
	
	public OffsetReference getClusterTextOffsetRelativeTo() {
		return clusterTextOffsetRelativeTo;
	}
	
	/**
	 * 
	 * define which corner of the text frame is used to calculate the offset.
	 * e.g., when top left corner of pin is {x,y}, text frame size is {10,20}, offset ={5,4}, relativeTo=BOTTOM_RIGHT, 
	 * then the left top corner of this text frame is at {x+5-10, y+4-20};
	 */
	public void setClusterTextOffsetRelativeTo(
			OffsetReference clusterTextOffsetRelativeTo) {
		this.clusterTextOffsetRelativeTo = clusterTextOffsetRelativeTo;
	}

	public int getClusterBackgroundColor() {
		return clusterBackgroundColor;
	}

	public void setClusterBackgroundColor(int clusterBackgroundColor) {
		this.clusterBackgroundColor = clusterBackgroundColor;
	}

	public int getClusterTextColor() {
		return clusterTextColor;
	}

	public void setClusterTextColor(int clusterTextColor) {
		this.clusterTextColor = clusterTextColor;
	}

	public int getClusterBorderColor() {
		return clusterBorderColor;
	}

	public void setClusterBorderColor(int clusterBorderColor) {
		this.clusterBorderColor = clusterBorderColor;
	}


	public OverlayItem getItemByFocused() {
        synchronized(idxLock){
        int size = overlayItems.size();
        for(int i = 0; i < size; i++) {
            if (overlayItems.get(i).isFoucsed) {
                return overlayItems.get(i);
            }
        }
        
        return null;
        }
	}

    public OverlayItem switchItem(boolean next) {
        synchronized(idxLock){
        OverlayItem overlayItem = null;
        int index = -1;
        int size = overlayItems.size();
        for(int i = 0; i < size; i++) {
            if (overlayItems.get(i).isFoucsed) {
                if (next) {
                    index = i + 1;
                } else {
                    index = i - 1;
                }
//                index %= size;
                index = (index + size)%size;
                break;
            }
        }
        
        if (index == -1) {
            index = 0;
        }
        
        for(int i = 0; i < size; i++) {
            if (index == i) {
                overlayItem = overlayItems.get(i);
                overlayItem.isFoucsed = true;                
            } else {
                overlayItems.get(i).isFoucsed = false;
            }
        }
        
        return overlayItem;
        }
    }
    
    public void focuseOverlayItem(int focusedPosition) {
        synchronized(idxLock){
    	
    	// set focus status to item equals to focusPosition
    	for (int i = overlayItems.size()-1; i >= 0; i--) {
    		OverlayItem overlayItem = overlayItems.get(i);
    		if (i == focusedPosition) {
    			overlayItem.isFoucsed = true;
    		} else {
    			overlayItem.isFoucsed = false;
    		}
    	}
        }
    }
    
    public void focuseOverlayItem(OverlayItem focusedOverlayItem) {
        synchronized(idxLock){
    	
        	for (int i = overlayItems.size()-1; i >= 0; i--) {
        		OverlayItem overlayItem = overlayItems.get(i);
        		if (overlayItem.equals(focusedOverlayItem)) {
        			overlayItem.isFoucsed = true;
        		} else {
        			overlayItem.isFoucsed = false;
        		}
        	}
        }
    }
    
    public OverlayItem getNextItem(OverlayItem overlayItem) {
        synchronized(idxLock){
    	
    	int index = overlayItems.indexOf(overlayItem);
    	if (index >= 0 && index < overlayItems.size()-1) {
    		return overlayItems.get(index+1);
    	}
        
        return null;
        }
    }

}

class Cluster{
	XYDouble refMercXY;
	ArrayList<OverlayItem> clusterPins;
	Cluster(XYDouble refMercXY, ArrayList<OverlayItem> clusterPins){
		this.refMercXY=refMercXY;
		this.clusterPins=clusterPins;
	}
}
