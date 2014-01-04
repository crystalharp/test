package com.tigerknows.map;

import java.util.List;

import android.app.Activity;
import android.content.res.Resources;
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
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.Step;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.NavigationSplitJointRule;

public class TrafficOverlayHelper {

	private static final int DEFAULT_SHOW_STEP_ZOOMLEVEL = MapEngine.DEFAULT_CITY_LEVEL;
	
	private static final float HIGHLIGHT_POSITION_ACCURACY = 0.00001f;
	
	private static final String TAG = "TrafficOverlayHelper";
    
    public static ItemizedOverlay drawTrafficPlanListOverlay(Sphinx sphinx, List<Plan> list, int focusedIndex) {

        sphinx.clearMap();
        
        ItemizedOverlay itemizedOverlay = null;
        try {
            OverlayItem focusedOverlayItem = null;
            ItemizedOverlay overlay = new ItemizedOverlay(ItemizedOverlay.TRAFFIC_PLAN_LIST_OVERLAY);
            
            for(int i = 0, size = list.size(); i < size; i++) {
                Plan plan = list.get(i);
                RotationTilt rt = new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
                OverlayItem overlayItem = new OverlayItem(new Position(39, 116), Icon.getIcon(sphinx.getResources(), R.drawable.btn_bubble_b_normal, Icon.OFFSET_LOCATION_CENTER_BOTTOM), 
                        Icon.getIcon(sphinx.getResources(), R.drawable.btn_bubble_b_normal, Icon.OFFSET_LOCATION_CENTER_BOTTOM),
                        plan.getDescription(), rt);
                overlayItem.setAssociatedObject(plan);
                overlay.addOverlayItem(overlayItem);
                
                if (focusedIndex == i) {
                    overlayItem.isFoucsed = true;
                    focusedOverlayItem = overlayItem;
                }
            }
            
            sphinx.showInfoWindow(focusedOverlayItem);
            
            itemizedOverlay = overlay;
        } catch(APIException e) {
            e.printStackTrace();
        }
        
        return itemizedOverlay;
    }

	/**
	 * 绘制交通图层
	 * @param context
	 * @param mapView
	 * @param plan
	 * @param type
	 */
	public static void drawOverlay(Sphinx sphinx, Plan plan) {
	    
	    sphinx.clearMap();
	    
		try {
	        if(plan!=null){
	            MapView mapView = sphinx.getMapView();
	        	mapView.getMapPreference().setRouteId(ItemizedOverlay.TRAFFIC_OVERLAY); 
	            
	        	// 线路所经过的路径
	        	Polyline routeLine = new Polyline(plan.getRouteGeometry(), Shape.TRAFFIC_SHAPE);
	            mapView.addShape(routeLine);
	            
	            // 交通图层
	            ItemizedOverlay overlay = new ItemizedOverlay(ItemizedOverlay.TRAFFIC_OVERLAY);

	            RotationTilt rt = new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
	            
	            List<Step> steps = plan.getStepList();
	            List<CharSequence> strings = NavigationSplitJointRule.splitJoint(sphinx, plan);
	            
	            Resources resources = sphinx.getResources();
                Icon start = Icon.getIcon(resources, R.drawable.icon_start_pin, Icon.OFFSET_LOCATION_CENTER_BOTTOM);
                Icon end = Icon.getIcon(resources, R.drawable.icon_end_pin, Icon.OFFSET_LOCATION_CENTER_BOTTOM);
	            Icon busIc = Icon.getIcon(resources, R.drawable.icon_map_bus);
	    	    Icon driveIc = Icon.getIcon(resources, R.drawable.icon_map_drive);
	    	    Icon walkIc = Icon.getIcon(resources, R.drawable.icon_map_walk);
	            
	    	    // 添加起点item. 包括终点图标, 起点文本:"起点"
	    	    OverlayItem overlayItem = new OverlayItem(steps.get(0).getPositionList().get(0), start, start, 
            			sphinx.getString(R.string.start), rt);
	    	    overlayItem.setAssociatedObject(new Step(new XMap()));
	            addTouchEventListenerToOverlayItem(sphinx, mapView, overlayItem);
                overlayItem.setPreferZoomLevel(DEFAULT_SHOW_STEP_ZOOMLEVEL);
                overlay.addOverlayItem(overlayItem);
	            for(int i = 0; i < steps.size(); i++){
	                
	                // 起点图标为startIc
	                if (i == 0) {
	                	overlayItem = new OverlayItem(steps.get(i).getPositionList().get(0), start, start, 
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
	                	
	                	overlayItem = new OverlayItem(steps.get(i).getPositionList().get(0), icon, icon, 
	                			strings.get(i).toString(), rt);
	                }
	                overlayItem.setPreferZoomLevel(DEFAULT_SHOW_STEP_ZOOMLEVEL);
	                overlayItem.setAssociatedObject(steps.get(i));
	                
	                // 设置每一个item的点击事件
	                addTouchEventListenerToOverlayItem(sphinx, mapView, overlayItem);
	                overlay.addOverlayItem(overlayItem);
	            }
	            
	            // 添加终点item. 包括终点图标, 终点文本:"到达终点"
	            Step lastStep = steps.get(steps.size()-1);
	            Position endPos = lastStep.getPositionList().get(lastStep.getPositionList().size()-1);
	            overlayItem = new OverlayItem(endPos, end, end, 
	            		sphinx.getString(R.string.traffic_goto_end_station), rt);
	    	    overlayItem.setAssociatedObject(new Step(new XMap()));
	            addTouchEventListenerToOverlayItem(sphinx, mapView, overlayItem);
	            overlayItem.setPreferZoomLevel(DEFAULT_SHOW_STEP_ZOOMLEVEL);
                overlay.addOverlayItem(overlayItem);
                
                mapView.addOverlay(overlay);

                mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
                
                sphinx.getCenterTokenView().setVisibility(View.INVISIBLE);
                sphinx.getMapToolsView().setVisibility(View.VISIBLE);
                sphinx.getMapCleanBtn().setVisibility(View.VISIBLE);
                sphinx.getLocationView().setVisibility(View.VISIBLE);
	        }
	    }catch(Exception e){
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
                    String tag = sphinx.getResultMapFragment().mActionTag;
                    if (ActionLog.TrafficTransferListMap.equals(tag) ||
                            ActionLog.TrafficDriveListMap.equals(tag) ||
                            ActionLog.TrafficWalkListMap.equals(tag)) {
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
	public static Position panToViewWholeOverlay(Plan plan, MapView mapview, Activity activity) {
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
            Rect rect = mapview.getPadding();
		    Icon start = Icon.getIcon(activity.getResources(), R.drawable.icon_start_pin, Icon.OFFSET_LOCATION_CENTER_BOTTOM);
		    int fitZoom = Util.getZoomLevelToFitBoundingBox(screenX, screenY, rect, boundingBox);
			LogWrapper.d(TAG, "get fitZoom: " + fitZoom);
			Position centerPosition = boundingBox.getCenterPosition();
			LogWrapper.d(TAG, "get centerPosition: " + centerPosition.getLon() + " " + centerPosition.getLat());
//			mapview.zoomTo(fitZoom, centerPosition);
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
	public static void panToPosition(final Sphinx sphinx, OverlayItem overlayItem, MapView mapview) {
//		mapview.zoomTo(DEFAULT_SHOW_STEP_ZOOMLEVEL, position);
		
		mapview.getOverlaysByName(ItemizedOverlay.TRAFFIC_OVERLAY).isShowInPreferZoom = true;
		
		// 将地图平移到某一坐标点
		mapview.getCurrentOverlay().focuseOverlayItem(overlayItem);
		ItemizedOverlayHelper.centerShowCurrentOverlayFocusedItem(sphinx);
	}
	public static void panToPosition(final Sphinx sphinx, int position, MapView mapview) {
//		mapview.zoomTo(DEFAULT_SHOW_STEP_ZOOMLEVEL, position);
		
		mapview.getOverlaysByName(ItemizedOverlay.TRAFFIC_OVERLAY).isShowInPreferZoom = true;
		
		// 将地图平移到某一坐标点
		mapview.getCurrentOverlay().focuseOverlayItem(position);
		ItemizedOverlayHelper.centerShowCurrentOverlayFocusedItem(sphinx);
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
    		OverlayItem focusedItem = mapview.getCurrentOverlay().getItemByFocused();
    		OverlayItem nextItem = mapview.getCurrentOverlay().getNextItem(focusedItem);
    		
    		if (focusedItem != null
    		        && nextItem != null) {
    		    Position focusedStepPosition = focusedItem.getPosition();
	    		Position nextStepPosition = nextItem.getPosition();
	    		
	    		Polyline trafficPolygon = (Polyline)mapview.getShapesByName(Shape.TRAFFIC_SHAPE);
	    		List<Position> wholeLinePositions = trafficPolygon.getPositions();
	    		int stepStartIndex = -1;
	    		int stepEndIndex = -1;
	    		for(int i = 0, size = wholeLinePositions.size(); i < size; i++) {
	    		    Position pos = wholeLinePositions.get(i);
	    		    if (Math.abs(pos.getLat()-focusedStepPosition.getLat()) < HIGHLIGHT_POSITION_ACCURACY
                            && Math.abs(pos.getLon()-focusedStepPosition.getLon()) < HIGHLIGHT_POSITION_ACCURACY
                            && stepStartIndex == -1) {
	    		        stepStartIndex = i;
	    		    } 
	    		    if (Math.abs(pos.getLat()-nextStepPosition.getLat()) < HIGHLIGHT_POSITION_ACCURACY
                            && Math.abs(pos.getLon()-nextStepPosition.getLon()) < HIGHLIGHT_POSITION_ACCURACY
                            && stepEndIndex == -1) {
	    		        stepEndIndex = i;
	    		    }
	    		}
	    		if (stepStartIndex < stepEndIndex) {
	    			List<Position> stepPositions = wholeLinePositions.subList(stepStartIndex, stepEndIndex + 1);
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
