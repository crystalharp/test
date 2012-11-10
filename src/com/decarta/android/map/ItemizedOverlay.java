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

import com.decarta.CONFIG;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYDouble;
import com.decarta.android.util.XYInteger;
import com.decarta.android.util.XYZ;

public class ItemizedOverlay {
    public static String MY_LOCATION_OVERLAY = "my_location";
    public static String POI_OVERLAY = "poi";
    public static String TRAFFIC_OVERLAY = "traffic";
    public static String LINE_OVERLAY = "line";
    public static String PIN_OVERLAY = "pin";
    
	public static final int ZOOM_LEVEL=13;
	private static int GRANULARITY=20;
	private static int[][] GENERALIZE_ZOOM_LEVEL={
		{0,0},  //zoom 0
		{1,1},  //zoom 1
		{2,2},  //zoom 2
		{3,3},  //zoom 3
		{4,4},  //zoom 4
		{5,5},  //zoom 5
		{6,6},  //zoom 6
		{7,7},  //zoom 7
		{8,8},  //zoom 8
		{9,9},  //zoom 9
		{10,10}, //zoom 10
		{11,11}, //zoom 11
		{12,12}, //zoom 12
		{13,13}, //zoom 13
		{14,14}, //zoom 14
		{15,15}, //zoom 15
		{16,16}, //zoom 16
		{17,17}, //zoom 17
		{18,18}, //zoom 18
		{19,19}, //zoom 19
		{20,20}  //zoom 20
	};
	
	public static int TOUCH_RADIUS=100;
	//public static int SNAP_BUFFER=6;
	public static int SNAP_BUFFER=0;
	
	/**
	 * name as ID, should be unique and composed of [a-zA-Z_1-9] and begin with [a-zA-Z_]
	 */
	private String name;
	private ArrayList<OverlayItem> overlayItems = new ArrayList<OverlayItem>();
	
	private HashMap<XYZ,ArrayList<ArrayList<OverlayItem>>> pinIdxs[];
	
	//variables for clustering
	private boolean clustering=false;
	private ClusterTouchEventListener clusterTouchEventListener=null;
	//cluster size text will be drawn at clusterIcon topleft.x+clusterTextOffset.x, clusterIcon topleft.x.y+clusterTextOffset.y
	private XYInteger clusterTextOffset=new XYInteger(0,0);
	private int clusterBackgroundColor=Color.rgb(255, 0, 0);
	private int clusterTextColor=Color.rgb(255, 255, 255);
	private int clusterBorderColor=Color.rgb(255, 255, 255);
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
	
	@SuppressWarnings("unchecked")
	private void generalizePins(){
        //Check the point index
		if(pinIdxs!=null) return;
		pinIdxs=new HashMap[21];
		for(int i=0;i<21;i++){
			pinIdxs[i]=null;
		}
		
		ArrayList<ArrayList<OverlayItem>> clustersArray[]=new ArrayList[21];
		for(int zoomLevel=CONFIG.ZOOM_UPPER_BOUND;zoomLevel>=0;zoomLevel--){
			if(zoomLevel>CONFIG.ZOOM_UPPER_BOUND || zoomLevel<CONFIG.ZOOM_LOWER_BOUND) continue;
			
			int genLevel=GENERALIZE_ZOOM_LEVEL[zoomLevel][0];
			int idxLevel=GENERALIZE_ZOOM_LEVEL[zoomLevel][1];
	        if(pinIdxs[idxLevel]!=null) continue;
	        
			double zoomScale=Math.pow(2, ZOOM_LEVEL-genLevel);
			double scale=Math.pow(2, idxLevel-ZOOM_LEVEL);
		    double minDist=(GRANULARITY*GRANULARITY)*(zoomScale*zoomScale);
        	
        	clustersArray[zoomLevel]=new ArrayList<ArrayList<OverlayItem>>();
            if(zoomLevel==CONFIG.ZOOM_UPPER_BOUND){
            	for (int i = 0; i < overlayItems.size(); i++) {
    				OverlayItem pin = overlayItems.get(i);
    				if (pin.getMercXY() == null) {
    					continue;
    				}
    				
    				if(clustersArray[zoomLevel].size()==0){
            			ArrayList<OverlayItem> clusterL=new ArrayList<OverlayItem>();
    					clusterL.add(pin);
    					clustersArray[zoomLevel].add(clusterL);
    					continue;
            		}
            		
//            		boolean included=false;
//            		for(int j=0;j<clustersArray[zoomLevel].size();j++){
//    					ArrayList<OverlayItem> clusterL=clustersArray[zoomLevel].get(j);
//    					if(clusterL.size()==0){
//    						LogWrapper.e("ItemizedOverlay","generalizePins cluster empty");
//    						continue;
//    					}
//    					OverlayItem pinL=clusterL.get(0);
//    					double dist = (pin.getMercXY().x - pinL.getMercXY().x)*(pin.getMercXY().x - pinL.getMercXY().x)+(pin.getMercXY().y - pinL.getMercXY().y)*(pin.getMercXY().y - pinL.getMercXY().y);
//    					if(dist<minDist){
//    						clusterL.add(pin);
//    						included=true;
//    						break;
//    					}
//    					
//    				}
//    				if(included) continue;
    				
    				ArrayList<OverlayItem> clusterL=new ArrayList<OverlayItem>();
    				clusterL.add(pin);
    				clustersArray[zoomLevel].add(clusterL);
    			}
            }else{
            	for(int i=0;i<clustersArray[zoomLevel+1].size();i++){
            		ArrayList<OverlayItem> cluster=clustersArray[zoomLevel+1].get(i);
//            		OverlayItem pin=cluster.get(0);
            		if(clustersArray[zoomLevel].size()==0){
            			ArrayList<OverlayItem> clusterL=new ArrayList<OverlayItem>();
    					clusterL.addAll(cluster);
    					clustersArray[zoomLevel].add(clusterL);
    					continue;
            		}
            		
//            		boolean included=false;
//            		for(int j=0;j<clustersArray[zoomLevel].size();j++){
//    					ArrayList<OverlayItem> clusterL=clustersArray[zoomLevel].get(j);
//    					if(clusterL.size()==0){
//    						LogWrapper.e("ItemizedOverlay","generalizePins cluster empty");
//    						continue;
//    					}
//    					OverlayItem pinL=clusterL.get(0);
//    					double dist = (pin.getMercXY().x - pinL.getMercXY().x)*(pin.getMercXY().x - pinL.getMercXY().x)+(pin.getMercXY().y - pinL.getMercXY().y)*(pin.getMercXY().y - pinL.getMercXY().y);
//    					if(dist<minDist){
//    						clusterL.addAll(cluster);
//    						included=true;
//    						break;
//    					}
//    					
//    				}
//    				if(included) continue;
    				
    				ArrayList<OverlayItem> clusterL=new ArrayList<OverlayItem>();
    				clusterL.addAll(cluster);
    				clustersArray[zoomLevel].add(clusterL);
            	}
            }
            
            HashMap<XYZ, ArrayList<ArrayList<OverlayItem>>> pinIdx=new HashMap<XYZ,ArrayList<ArrayList<OverlayItem>>>();
        	for(int i=0;i<clustersArray[zoomLevel].size();i++){
        		ArrayList<OverlayItem> cluster=clustersArray[zoomLevel].get(i);
				if(cluster.size()==0){
					LogWrapper.e("ItemizedOverlay","generalizePins cluster empty");
					continue;
				}
				OverlayItem pin=cluster.get(0);
				
				XYDouble mercXY=new XYDouble(pin.getMercXY().x*scale,pin.getMercXY().y*scale);
				XYInteger ne = Util.mercXYToNE(mercXY);
				XYZ key=new XYZ(ne.x,ne.y,idxLevel);
				if (!pinIdx.containsKey(key)){
					pinIdx.put(key, new ArrayList<ArrayList<OverlayItem>>());
				}
				pinIdx.get(key).add(cluster);
				key = key.clone();
//				key.y -= 1;
                if (!pinIdx.containsKey(key)){
                    pinIdx.put(key, new ArrayList<ArrayList<OverlayItem>>());
                }
                pinIdx.get(key).add(cluster);
        	}
        				
            pinIdxs[idxLevel]=pinIdx;
            //LogWrapper.i("ItemizedOverlay","generalizePins zoomLevel,genLevel,indexLevel:"+zoomLevel+","+genLevel+","+idxLevel);
		}
    		
    }
		
	public ArrayList<ArrayList<OverlayItem>> getVisiblePins(int zoomLevel, ArrayList<Tile> tiles) {
		ArrayList<ArrayList<OverlayItem>> clusters=new ArrayList<ArrayList<OverlayItem>>();
		
		generalizePins();
		int idxLevel=GENERALIZE_ZOOM_LEVEL[zoomLevel][1];
		HashMap<XYZ,ArrayList<ArrayList<OverlayItem>>> pinIdx=pinIdxs[idxLevel];
		
		List<XYZ> overlapTiles=new ArrayList<XYZ>();
		for(int i=0;i<tiles.size();i++){
			Tile tile=tiles.get(i);
			List<XYZ> xyzs=Util.findOverlapTiles(tile.xyz, idxLevel);
			if(!overlapTiles.contains(xyzs.get(0))){
				overlapTiles.addAll(xyzs);
			}
			
		}
		
		/*
		 * 地图上点的坐标系计算仍有问题, 当前暂时绕过
		 */
		overlapTiles = Util.getVerticalLargerOverlapTiles(overlapTiles);
		
		for(int j=0;j<overlapTiles.size();j++){
			XYZ xyz=overlapTiles.get(j);
			//String key=xyz.z+"_"+xyz.x+"_"+xyz.y;
			XYZ key=xyz;
			if(pinIdx != null && key != null && pinIdx.containsKey(key)){
				clusters.addAll(pinIdx.get(key));
			}
		}
		return clusters;

    }
	
	public int size(){
		return overlayItems.size();
	}
	
	public OverlayItem get(int i){
		return overlayItems.get(i);
	}
	
	public OverlayItem getItemByFocused() {
        int size = overlayItems.size();
        for(int i = 0; i < size; i++) {
            if (overlayItems.get(i).isFoucsed) {
                return overlayItems.get(i);
            }
        }
        
        return null;
	}
	
	public void addOverlayItem(OverlayItem overlayItem){
		overlayItems.add(overlayItem);
		overlayItem.setOwnerOverlay(this);
		resetPinIdxs();
	}
	
	/**
	 * remove all pins of this overlay
	 */
	public void clear(){
		overlayItems.clear();
		resetPinIdxs();
	}
	
	/**
	 * remove the overlay item. 
	 * @param location
	 */
	public OverlayItem remove(int location){
		resetPinIdxs();
		return overlayItems.remove(location);
	}
	
	/**
	 * remove the overlay item. 
	 * @param overlayItem
	 */
	public boolean remove(OverlayItem overlayItem){
		resetPinIdxs();
		return overlayItems.remove(overlayItem);
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

    public OverlayItem switchItem(boolean next) {
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
    
    public void focuseOverlayItemByPosition(Position focusedPosition) {
    	if (!Util.inChina(focusedPosition)) {
    		return;
    	}
    	
    	// clear focus status
    	if (getItemByFocused() != null) {
    		getItemByFocused().isFoucsed = false;
    	}
    	
    	// set focus status to item equals to focusPosition
    	for (OverlayItem item : overlayItems) {
    		if (item.getPosition().equals(focusedPosition)) {
    			item.isFoucsed = true;
    			break;
    		}
    	}
    }
    
    public OverlayItem getNextItem(OverlayItem overlayItem) {
    	
    	int index = overlayItems.indexOf(overlayItem);
    	if (index >= 0 && index < overlayItems.size()-1) {
    		return overlayItems.get(index+1);
    	}
        
        return null;
    }

}
