package com.tigerknows.maps;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;

import com.decarta.Globals;
import com.decarta.android.event.EventSource;
import com.decarta.android.event.EventType;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.BoundingBox;
import com.decarta.android.location.Position;
import com.decarta.android.map.Icon;
import com.decarta.android.map.InfoWindow;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.MapView;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.map.Polyline;
import com.decarta.android.map.RotationTilt;
import com.decarta.android.map.RotationTilt.RotateReference;
import com.decarta.android.map.RotationTilt.TiltReference;
import com.decarta.android.map.Shape;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYInteger;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.BuslineModel.Station;
import com.tigerknows.model.POI;

public class BuslineOverlayHelper {

	private static final int DEFAULT_SHOW_STEP_ZOOMLEVEL = 14;

	private static String TAG = "BuslineOverlayHelper";

	
	/**
	 * 绘制线路图层
	 * 
	 * @param context
	 * @param mapView
	 * @param line
	 */
	public static void drawOverlay(final Context context, final Handler mainThreadHandler, final MapView mapView, Line line) {
		try {
			if (line != null) {
				mapView.getMapPreference().setRouteId(ItemizedOverlay.LINE_OVERLAY);
				
				// 线路所经过的路径
				Polyline routeLine = new Polyline(line.getPositionList(), Shape.LINE_SHAPE);
				mapView.addShape(routeLine);
				
				// 线路图层
				ItemizedOverlay overlay = new ItemizedOverlay(ItemizedOverlay.LINE_OVERLAY);
				
				// 站点集合
				List<POI> poiList = new ArrayList<POI>();
		        for(Station station : line.getStationList()) {
		            poiList.add(station.toPOI());
		        }
		        
		        RotationTilt rt = new RotationTilt(RotateReference.SCREEN, TiltReference.SCREEN);
				
		        // 绘制每一个站点图标及起点终点图标
		        Icon busStartIc = getStartIcon(context);
		        Icon busEndIc = getEndIcon(context);
		        Icon busStationIc = createIconByResId(context, R.drawable.icon_map_bus);
		        Icon busIc = null;
		        int i = 0;
		        final int startIndex = 0;
		        final int endIndex = poiList.size() - 1;
				for (POI poi : poiList) {
					if (i == startIndex) {
						busIc = busStartIc;
					} else if (i == endIndex) {
						busIc = busEndIc;
					} else {
						busIc = busStationIc;
					}
					i++;
					
					OverlayItem overlayItem = new OverlayItem(poi.getPosition(), busIc, context.getString(R.string.busline_map_bubble_content, i, poi.getName()), rt);
					
					overlayItem.setPreferZoomLevel(DEFAULT_SHOW_STEP_ZOOMLEVEL);
	                overlayItem.setInfoWindowType(InfoWindow.TYPE_SIMPLE);
	                
					addTouchEventListenerToOverlayItem(mainThreadHandler, mapView, overlayItem);
					overlay.addOverlayItem(overlayItem);
				}
				
				// 更新地图, 并显示"上下按钮"
				mapView.addOverlay(overlay);
				mainThreadHandler.sendEmptyMessage(Sphinx.PREVIOUS_NEXT_SHOW);
				
                mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 将图片资源文件转化为Icon对象
	 * 
	 * @param context
	 * @param resId
	 * @return
	 */
	private static Icon createIconByResId(Context context, int resId) {
		Options ops=new Options();
        ops.inScaled=false;
        
		Bitmap bmp=BitmapFactory.decodeResource(context.getResources(), resId, ops);
        Icon icon=new Icon(bmp, new XYInteger(bmp.getWidth(),bmp.getHeight()),
                new XYInteger(0,bmp.getHeight()));
        return icon;
	}
	
	/**
	 * 获取起点Icon
	 * 
	 * @param context
	 * @return
	 */
	private static Icon getStartIcon(Context context) {
		Options ops=new Options();
        ops.inScaled=false;
        
		Bitmap bmp=BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_start_pin, ops);
        Icon icon=new Icon(bmp, new XYInteger(bmp.getWidth(),bmp.getHeight()),
                new XYInteger(bmp.getWidth()/2,bmp.getHeight()));
        return icon;
	}
	
	/**
	 * 获取终点Icon
	 * 
	 * @param context
	 * @return
	 */
	private static Icon getEndIcon(Context context) {
		Options ops=new Options();
        ops.inScaled=false;
        
		Bitmap bmp=BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_end_pin, ops);
        Icon icon=new Icon(bmp, new XYInteger(bmp.getWidth(),bmp.getHeight()),
                new XYInteger(bmp.getWidth()/2,bmp.getHeight()));
        return icon;
	}
	
	/**
	 * 为每个OverlayItem附加点击事件,
	 * 点击后将弹出InfoWindow显示信息
	 * 
	 * @param mapView
	 * @param overlayItem
	 */
	private static void addTouchEventListenerToOverlayItem(final Handler mainThreadHandler, final MapView mapView, OverlayItem overlayItem) {
		try {
			overlayItem.addEventListener(EventType.TOUCH, new OverlayItem.TouchEventListener() {
			    @Override
			    public void onTouchEvent(EventSource eventSource) {
			        OverlayItem overlayItem=(OverlayItem) eventSource; 
			        overlayItem.getOwnerOverlay().focuseOverlayItemByPosition(overlayItem.getPosition());
			        panToPosition(mainThreadHandler, overlayItem.getPosition(), mapView);
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
	public static Position panToViewWholeOverlay(Line line, MapView mapview, Activity activity) {
		LogWrapper.d(TAG, "panToViewWholeOverlay");
		Position position = null;
		if (line == null) {
			return position;
		}
		
		mapview.getOverlaysByName(ItemizedOverlay.LINE_OVERLAY).isShowInPreferZoom = true;
		
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
//			int fitZoom = Util.getZoomLevelToFitBoundingBox(screenX, screenY, getStartIcon(activity).getSize().x/2, boundingBox);
			int fitZoom = Util.getZoomLevelToFitBoundingBox(screenX, screenY, getStartIcon(activity).getSize().x/2, 
					mapview.getPadding().top, boundingBox);

			LogWrapper.d(TAG, "get fitZoom: " + fitZoom);
			Position centerPosition = boundingBox.getCenterPosition();
			LogWrapper.d(TAG, "get centerPosition: " + centerPosition.getLon() + " " + centerPosition.getLat());
			mapview.zoomTo(fitZoom, centerPosition, -1, null);
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
	public static void panToPosition(final Handler mainThreadHandler, Position position, MapView mapview) {
//		mapview.zoomTo(DEFAULT_SHOW_STEP_ZOOMLEVEL, position);
		
		mapview.getOverlaysByName(ItemizedOverlay.LINE_OVERLAY).isShowInPreferZoom = true;
		
		// 将地图平移到某一坐标点
		mapview.getCurrentOverlay().focuseOverlayItemByPosition(position);
		mainThreadHandler.sendEmptyMessage(Sphinx.CENTER_SHOW_FOCUSED_OVERLAYITEM);
	}
}
