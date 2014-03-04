package com.tigerknows.map;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;

import com.decarta.Globals;
import com.decarta.android.event.EventSource;
import com.decarta.android.event.EventType;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.BoundingBox;
import com.decarta.android.map.Icon;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.map.Polyline;
import com.decarta.android.map.RotationTilt;
import com.decarta.android.map.RotationTilt.RotateReference;
import com.decarta.android.map.RotationTilt.TiltReference;
import com.decarta.android.map.Shape;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.android.location.Position;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.BuslineModel.Station;
import com.tigerknows.model.POI;

public class BuslineOverlayHelper {

	private static final int DEFAULT_SHOW_STEP_ZOOMLEVEL = MapEngine.DEFAULT_CITY_LEVEL;

	private static String TAG = "BuslineOverlayHelper";

	
	/**
	 * 绘制线路图层
	 * 
	 * @param context
	 * @param mapView
	 * @param line
	 */
	public static void drawOverlay(final Sphinx sphinx, final MapView mapView, Line line) {
	    
		try {
			if (line != null) {
			    ItemizedOverlay itemizedOverlay = mapView.getOverlaysByName(ItemizedOverlay.BUSLINE_OVERLAY);
			    if (itemizedOverlay != null) {
			        Object o = itemizedOverlay.get(0).getAssociatedObject();
			        if (o == line) {
			            return;
			        }
			    }
		        
		        sphinx.clearMap();
			    
				mapView.getMapPreference().setRouteId(ItemizedOverlay.BUSLINE_OVERLAY);
				
				// 线路所经过的路径
				Polyline routeLine = new Polyline(line.getPositionList(), Shape.LINE_SHAPE);
				mapView.addShape(routeLine);
				
				// 线路图层
				ItemizedOverlay overlay = new ItemizedOverlay(ItemizedOverlay.BUSLINE_OVERLAY);
				
				// 站点集合
				List<POI> poiList = new ArrayList<POI>();
		        for(Station station : line.getStationList()) {
		            poiList.add(station.toPOI());
		        }
		        
		        RotationTilt rt = new RotationTilt(RotateReference.SCREEN, TiltReference.SCREEN);
                
                OverlayItem overlayItem = new OverlayItem(new Position(39, 116), Icon.getIcon(sphinx.getResources(), R.drawable.btn_bubble_b_normal, Icon.OFFSET_LOCATION_CENTER_BOTTOM), 
                        Icon.getIcon(sphinx.getResources(), R.drawable.btn_bubble_b_normal, Icon.OFFSET_LOCATION_CENTER_BOTTOM),
                        line.getName(), rt);
                overlayItem.setAssociatedObject(line);
                overlay.addOverlayItem(overlayItem);
				
		        // 绘制每一个站点图标及起点终点图标
		        Icon busStartIc = Icon.getIcon(sphinx.getResources(), R.drawable.icon_start_pin, Icon.OFFSET_LOCATION_CENTER_BOTTOM);
                Icon busEndIc = Icon.getIcon(sphinx.getResources(), R.drawable.icon_end_pin, Icon.OFFSET_LOCATION_CENTER_BOTTOM);
		        Icon busStationIc = Icon.getIcon(sphinx.getResources(), R.drawable.icon_map_bus);
                Icon busStationIcFocused = Icon.getIcon(sphinx.getResources(), R.drawable.icon_map_bus_focused);
		        Icon busIc = null;
		        Icon busIcFocused = null;
		        int i = 0;
		        final int startIndex = 0;
		        final int endIndex = poiList.size() - 1;

				for (POI poi : poiList) {
					if (i == startIndex) {
						busIc = busStartIc;
                        busIcFocused = busStartIc;
					} else if (i == endIndex) {
						busIc = busEndIc;
                        busIcFocused = busEndIc;
					} else {
						busIc = busStationIc;
						busIcFocused = busStationIcFocused;
					}
					i++;
					
					overlayItem = new OverlayItem(poi.getPosition(), busIc, busIcFocused, sphinx.getString(R.string.busline_map_bubble_content, i, poi.getName()), rt);
					
					overlayItem.setPreferZoomLevel(DEFAULT_SHOW_STEP_ZOOMLEVEL);
	                
					addTouchEventListenerToOverlayItem(sphinx, mapView, overlayItem);
					overlay.addOverlayItem(overlayItem);
				}
				
				// 更新地图, 并显示"上下按钮"
				mapView.addOverlay(overlay);
				
                mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
                
                sphinx.getCenterTokenView().setVisibility(View.INVISIBLE);
                sphinx.getMapToolsView().setVisibility(View.VISIBLE);
                sphinx.getMapCleanBtn().setVisibility(View.VISIBLE);
                sphinx.getLocationView().setVisibility(View.VISIBLE);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 为每个OverlayItem附加点击事件,
	 * 点击后将弹出InfoWindow显示信息
	 * 
	 * @param mapView
	 * @param overlayItem
	 */
	private static void addTouchEventListenerToOverlayItem(final Sphinx sphinx, final MapView mapView, OverlayItem overlayItem) {
		try {
			overlayItem.addEventListener(EventType.TOUCH, new OverlayItem.TouchEventListener() {
			    @Override
			    public void onTouchEvent(EventSource eventSource) {
                    if (sphinx.getTouchMode().equals(TouchMode.MEASURE_DISTANCE)) {
                        return;
                    }
			        OverlayItem overlayItem=(OverlayItem) eventSource; 
			        overlayItem.getOwnerOverlay().focuseOverlayItem(overlayItem);
			        panToPosition(sphinx, overlayItem, mapView);
			    }
			});
		} catch (APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 将地图缩放至可以显示完整的交通路径, 并平移到交通路径中心点
	 */
	public static Position panToViewWholeOverlay(Line line, Sphinx sphinx) {
		LogWrapper.d(TAG, "panToViewWholeOverlay");
		Position position = null;
		if (line == null) {
			return position;
		}
		MapView mapview = sphinx.getMapView();
		mapview.getOverlaysByName(ItemizedOverlay.BUSLINE_OVERLAY).isShowInPreferZoom = true;
		
		List<Position> positions = new ArrayList<Position>();
		for (Station s : line.getStationList()) {
			positions.add(s.getPosition());
		}
		
		/*
		 * 获取交通路径的左上角及右下角的经纬度坐标
		 */
		Position position1;
		double minLat = (positions.get(0)).getLat();
		double maxLat = (positions.get(0)).getLat();
		double minLon = (positions.get(0)).getLon();
		double maxLon = (positions.get(0)).getLon();
		for (int i = 1; i < positions.size(); i++) {
			position1 = positions.get(i);
			if (position1.getLon() > maxLon) {
				maxLon = position1.getLon();
			} else if (position1.getLon() < minLon) {
				minLon = position1.getLon();
			}
			if (position1.getLat() > maxLat) {
				maxLat = position1.getLat();
			} else if (position1.getLat() < minLat){
				minLat = position1.getLat();
			}
		}
		
		/*
		 * 创建一个可以包含整个交通路径的BoundingBox
		 */
		BoundingBox boundingBox = new BoundingBox(
				new Position(minLat, minLon), new Position(maxLat, maxLon));
		
		/*
		 * 获取屏幕的宽度及高度的像素点
		 */
		int screenX = Globals.g_metrics.widthPixels;
		int screenY = Globals.g_metrics.heightPixels;
		LogWrapper.d(TAG, "screenX: " + screenX + " screenY: " + screenY);
		
		/*
		 * 获取可以完整显示整个交通路径的zoomlevel及交通路径的中心点
		 * 缩放到该zoomlevel, 并平移至中心点
		 */
		try {
		    Rect rect = mapview.getPadding();
		    Icon start = Icon.getIcon(sphinx.getResources(), R.drawable.icon_start_pin, Icon.OFFSET_LOCATION_CENTER_BOTTOM);
		    int fitZoom = Util.getZoomLevelToFitBoundingBox(screenX, screenY, 
		            rect, boundingBox);
			LogWrapper.d(TAG, "get fitZoom: " + fitZoom);
			Position centerPosition = boundingBox.getCenterPosition();
			LogWrapper.d(TAG, "get centerPosition: " + centerPosition.getLon() + " " + centerPosition.getLat());
			mapview.setZoomLevel(fitZoom);
			mapview.panToPosition(centerPosition);
			position = centerPosition;
		} catch (APIException e) {
			e.printStackTrace();
		}
		return position;
	}
	
	/**
	 * 将地图平移到某一坐标点, 并缩放至某一级别
	 * @param mapview
	 * @param positon
	 */
	public static void panToPosition(Sphinx sphinx, OverlayItem overlayItem, MapView mapview) {
//		mapview.zoomTo(DEFAULT_SHOW_STEP_ZOOMLEVEL, position);
		
//		mapview.getOverlaysByName(ItemizedOverlay.BUSLINE_OVERLAY).isShowInPreferZoom = true;
		
		// 将地图平移到某一坐标点
		mapview.getCurrentOverlay().focuseOverlayItem(overlayItem);
		ItemizedOverlayHelper.centerShowCurrentOverlayFocusedItem(sphinx);
	}
	public static void panToPosition(Sphinx sphinx, int position, MapView mapview) {
//		mapview.zoomTo(DEFAULT_SHOW_STEP_ZOOMLEVEL, position);
		
		mapview.getOverlaysByName(ItemizedOverlay.BUSLINE_OVERLAY).isShowInPreferZoom = true;
		
		// 将地图平移到某一坐标点
		mapview.getCurrentOverlay().focuseOverlayItem(position);
        ItemizedOverlayHelper.centerShowCurrentOverlayFocusedItem(sphinx);
	}
}
