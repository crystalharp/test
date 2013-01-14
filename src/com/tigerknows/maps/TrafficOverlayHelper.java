package com.tigerknows.maps;

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
import com.decarta.android.map.Polygon;
import com.decarta.android.map.Polyline;
import com.decarta.android.map.RotationTilt;
import com.decarta.android.map.RotationTilt.RotateReference;
import com.decarta.android.map.RotationTilt.TiltReference;
import com.decarta.android.map.Shape;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYFloat;
import com.decarta.android.util.XYInteger;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.Step;
import com.tigerknows.util.NavigationSplitJointRule;

public class TrafficOverlayHelper {

	private static final int DEFAULT_SHOW_STEP_ZOOMLEVEL = 14;
	
	private static final String TAG = "TrafficOverlayHelper";

	/**
	 * 绘制交通图层
	 * @param context
	 * @param mapView
	 * @param plan
	 * @param type
	 */
	public static void drawOverlay(final Context context, final Handler mainThreadHandler, 
			final MapView mapView, Plan plan, final int type) {
		try {
	        if(plan!=null){
	        	mapView.getMapPreference().setRouteId(ItemizedOverlay.TRAFFIC_OVERLAY); 
	            
	        	// 线路所经过的路径
	        	Polyline routeLine = new Polyline(plan.getRouteGeometry(), Shape.TRAFFIC_SHAPE);
	            mapView.addShape(routeLine);
	            
	            // 交通图层
	            ItemizedOverlay overlay = new ItemizedOverlay(ItemizedOverlay.TRAFFIC_OVERLAY);

	            RotationTilt rt = new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
	            
	            List<Step> steps = plan.getStepList();
	            List<CharSequence> strings = NavigationSplitJointRule.splitJoint(context, type, plan);
	            
	            Icon start = getStartIcon(context);
	            Icon end = getEndIcon(context);
	            Icon busIc = createIconByResId(context, R.drawable.icon_map_bus);
	    	    Icon driveIc = createIconByResId(context, R.drawable.icon_map_drive);
	    	    Icon walkIc = createIconByResId(context, R.drawable.icon_map_walk);
	            
	            for(int i = 0; i < steps.size(); i++){
	                OverlayItem overlayItem = null;
	                
	                // 起点图标为startIc
	                if (i == 0) {
	                	overlayItem = new OverlayItem(steps.get(i).getPositionList().get(0), start, 
	                			strings.get(i).toString(), rt);
	                } else {
	                	Icon icon = null;
	                	switch(steps.get(i).getType()){
	                    case Step.TYPE_TRANSFER:
	                        icon = busIc;
	                        break;
	                    case Step.TYPE_DRIVE:
	                        icon =  driveIc;
	                        break;
	                    case Step.TYPE_WALK:
	                        icon =  walkIc;
	                        break;
	                    default:
	                    }
	                	
	                	overlayItem = new OverlayItem(steps.get(i).getPositionList().get(0), icon, 
	                			strings.get(i).toString(), rt);
	                }
	                overlayItem.setPreferZoomLevel(DEFAULT_SHOW_STEP_ZOOMLEVEL);
	                
	                // 设置每一个item的点击事件
	                addTouchEventListenerToOverlayItem(mainThreadHandler, mapView, overlayItem);
	                overlay.addOverlayItem(overlayItem);
	            }
	            
	            // 添加终点item. 包括终点图标, 终点文本:"到达终点"
	            Step lastStep = steps.get(steps.size()-1);
	            Position endPos = lastStep.getPositionList().get(lastStep.getPositionList().size()-1);
	            OverlayItem overlayItem = new OverlayItem(endPos, end, 
	            		context.getString(R.string.traffic_goto_end_station), rt);
	            addTouchEventListenerToOverlayItem(mainThreadHandler, mapView, overlayItem);
	            overlayItem.setPreferZoomLevel(DEFAULT_SHOW_STEP_ZOOMLEVEL);
                overlay.addOverlayItem(overlayItem);
                
                // 更新地图, 并显示"上下按钮"
                mapView.addOverlay(overlay);
	            mainThreadHandler.sendEmptyMessage(Sphinx.PREVIOUS_NEXT_SHOW);

                mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
	        }
	    }catch(Exception e){
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
	 * 根据Step类型获取相应Icon
	 * 步行Icon, 驾车Icon, 公交Icon
	 * 
	 * @param context
	 * @param step
	 * @return
	 */
	private static Icon getIconByType(Context context, Step step) {
	    Icon busIc = createIconByResId(context, R.drawable.icon_map_bus);
	    Icon driveIc = createIconByResId(context, R.drawable.icon_map_drive);
	    Icon walkIc = createIconByResId(context, R.drawable.icon_map_walk);
	
	    Icon icon = null;
        
        switch(step.getType()){
        
        case Step.TYPE_TRANSFER:
            icon = busIc;
            break;
        case Step.TYPE_DRIVE:
            icon =  driveIc;
            break;
        case Step.TYPE_WALK:
            icon =  walkIc;
            break;
        default:
        }

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
			        overlayItem.getOwnerOverlay().focuseOverlayItem(overlayItem);
			    	panToPosition(mainThreadHandler, overlayItem, mapView);
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
	public static Position panToViewWholeOverlay(Plan plan, MapView mapview, Activity activity) {
		LogWrapper.d(TAG, "panToViewWholeOverlay");
		Position position = null;
		if (plan == null) {
			return position;
		}
		
		mapview.getOverlaysByName(ItemizedOverlay.TRAFFIC_OVERLAY).isShowInPreferZoom = true;
		
		List<Position> positions = plan.getRouteGeometry();
		
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
//			mapview.zoomTo(fitZoom, centerPosition);
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
	public static void panToPosition(final Handler mainThreadHandler, OverlayItem overlayItem, MapView mapview) {
//		mapview.zoomTo(DEFAULT_SHOW_STEP_ZOOMLEVEL, position);
		
		mapview.getOverlaysByName(ItemizedOverlay.TRAFFIC_OVERLAY).isShowInPreferZoom = true;
		
		// 将地图平移到某一坐标点
		mapview.getCurrentOverlay().focuseOverlayItem(overlayItem);
		mainThreadHandler.sendEmptyMessage(Sphinx.CENTER_SHOW_FOCUSED_OVERLAYITEM);
	}
	public static void panToPosition(final Handler mainThreadHandler, int position, MapView mapview) {
//		mapview.zoomTo(DEFAULT_SHOW_STEP_ZOOMLEVEL, position);
		
		mapview.getOverlaysByName(ItemizedOverlay.TRAFFIC_OVERLAY).isShowInPreferZoom = true;
		
		// 将地图平移到某一坐标点
		mapview.getCurrentOverlay().focuseOverlayItem(position);
		mainThreadHandler.sendEmptyMessage(Sphinx.CENTER_SHOW_FOCUSED_OVERLAYITEM);
	}
	
	/**
	 * 高亮两个OverlayItem中间的路径
	 * @param mapview
	 */
	public static void highlightNextTwoOverlayItem(MapView mapview) {
		mapview.removeShapesByName(Shape.HIGHLIGHT_SHAPE);
		ItemizedOverlay itemizedOverlay = mapview.getCurrentOverlay();
		if (itemizedOverlay.getName().equals(ItemizedOverlay.TRAFFIC_OVERLAY)) {
        	// 高亮选中的Step
        	Position position = itemizedOverlay.getItemByFocused().getPosition();
    		LogWrapper.d(TAG, "position: " + position);
    		OverlayItem focusedItem = mapview.getCurrentOverlay().getItemByFocused();
    		OverlayItem nextItem = mapview.getCurrentOverlay().getNextItem(focusedItem);
    		
    		if (nextItem != null) {
	    		Position nextStepPosition = nextItem.getPosition();
	    		
	    		Polyline trafficPolygon = (Polyline)mapview.getShapesByName(Shape.TRAFFIC_SHAPE);
	    		List<Position> wholeLinePositions = trafficPolygon.getPositions();
	    		LogWrapper.d(TAG, "wholeLinePositions: " + wholeLinePositions);
	    		int stepStartIndex = wholeLinePositions.indexOf(position);
	    		int stepEndIndex = wholeLinePositions.indexOf(nextStepPosition);
	    		LogWrapper.d(TAG, "stepStartIndex: " + stepStartIndex);
	    		LogWrapper.d(TAG, "stepEndIndex: " + stepEndIndex);
	    		if (stepStartIndex >= 0 && stepStartIndex < stepEndIndex && stepEndIndex <= wholeLinePositions.size()) {
	    			List<Position> stepPositions = wholeLinePositions.subList(stepStartIndex, stepEndIndex + 1);
		    		LogWrapper.d(TAG, "stepPositions: " + stepPositions);
		    		try {
		    			Polyline routeLine = new Polyline(stepPositions, Shape.HIGHLIGHT_SHAPE);
		    			routeLine.setFillColor(0xFFAA0000);
		    			
		    			mapview.addShape(routeLine);
		    			mapview.refreshMap();
		    		} catch (Exception e) {
		    			// TODO Auto-generated catch block
		    			e.printStackTrace();
		    		}
	    		}
    		}
        }
	}

}
