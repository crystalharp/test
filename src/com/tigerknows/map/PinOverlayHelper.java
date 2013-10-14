package com.tigerknows.map;

import android.os.Handler;

import com.decarta.android.event.EventSource;
import com.decarta.android.event.EventType;
import com.decarta.android.map.Icon;
import com.decarta.android.map.InfoWindow;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.map.RotationTilt;
import com.decarta.android.map.RotationTilt.RotateReference;
import com.decarta.android.map.RotationTilt.TiltReference;
import com.decarta.android.util.XYFloat;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.location.Position;
import com.tigerknows.model.POI;

public class PinOverlayHelper {

	static final String TAG = "PinOverlayHelper";
	
	public static void drawSelectPointOverlay(final Sphinx sphinx, Handler mainThreadHandler, final MapView mapView, boolean isStart, String name, Position position) {
		
		try {
	        if(name != null && position != null){
	        	// 单击选点图层
	            POI poi = new POI();
                poi.setSourceType(POI.SOURCE_TYPE_CLICK_SELECT_POINT);
                poi.setName(name);
                poi.setPosition(position);
                
	            String message = sphinx.getString(isStart ? R.string.select_where_as_start : R.string.select_where_as_end, name);
	            ItemizedOverlay overlay = new ItemizedOverlay(ItemizedOverlay.PIN_OVERLAY);
	            RotationTilt rt = new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
	            OverlayItem overlayItem = new OverlayItem(poi.getPosition(), Icon.getIcon(sphinx.getResources(), R.drawable.btn_bubble_b_normal, Icon.OFFSET_LOCATION_CENTER_BOTTOM), 
	            		message, rt);
	            overlayItem.setAssociatedObject(poi);
		        
	            overlayItem.addEventListener(EventType.TOUCH, new OverlayItem.TouchEventListener() {
				    @Override
				    public void onTouchEvent(EventSource eventSource) {
				        OverlayItem overlayItem=(OverlayItem) eventSource;
				        sphinx.showInfoWindow(overlayItem);
				    }
				});
	            overlayItem.isFoucsed = true;
	            overlay.addOverlayItem(overlayItem);
	            mapView.addOverlay(overlay);

	            mainThreadHandler.sendEmptyMessage(Sphinx.ADJUST_SHOW_FOCUSED_OVERLAYITEM);
                mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
	        }
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void drawLongClickOverlay(final Sphinx sphinx, Handler mainThreadHandler, final MapView mapView, final String name, final Position position) {
		try {
	        if(position != null){
	        	// 长按选点图层
	            POI poi = new POI();
	            poi.setSourceType(POI.SOURCE_TYPE_LONG_CLICK_SELECT_POINT);
	            poi.setName(name);
                poi.setPosition(position);
                
	            ItemizedOverlay overlay = new ItemizedOverlay(ItemizedOverlay.PIN_OVERLAY);
	            RotationTilt rt = new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
	            OverlayItem overlayItem = new OverlayItem(position, Icon.getIcon(sphinx.getResources(), R.drawable.btn_bubble_b_normal, Icon.OFFSET_LOCATION_CENTER_BOTTOM), 
	            		name, rt);
		        overlayItem.setAssociatedObject(poi);
		        
	            overlayItem.addEventListener(EventType.TOUCH, new OverlayItem.TouchEventListener() {
				    @Override
				    public void onTouchEvent(EventSource eventSource) {
				        OverlayItem overlayItem=(OverlayItem) eventSource;
				        try{
	                        sphinx.showInfoWindow(overlayItem);
				        }catch(Exception e){
				            e.printStackTrace();
				        }
				        
				        mapView.getInfoWindow().setOffset(new XYFloat(0f,(float)(overlayItem.getIcon().getOffset().y)),
				                overlayItem.getRotationTilt());
				        mapView.getInfoWindow().setTextAlign(InfoWindow.TextAlign.CENTER);
				        mapView.getInfoWindow().setVisible(true);
				    }
				});
	            overlayItem.isFoucsed = true;
	            overlay.addOverlayItem(overlayItem);
	            mapView.addOverlay(overlay);
	            
	            mainThreadHandler.sendEmptyMessage(Sphinx.ADJUST_SHOW_FOCUSED_OVERLAYITEM);
                mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
	        }
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
