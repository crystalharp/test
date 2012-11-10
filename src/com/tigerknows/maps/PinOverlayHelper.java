package com.tigerknows.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;

import com.decarta.android.event.EventSource;
import com.decarta.android.event.EventType;
import com.decarta.android.location.Position;
import com.decarta.android.map.Icon;
import com.decarta.android.map.InfoWindow;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.MapView;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.map.RotationTilt;
import com.decarta.android.map.RotationTilt.RotateReference;
import com.decarta.android.map.RotationTilt.TiltReference;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.XYFloat;
import com.decarta.android.util.XYInteger;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.POI;

public class PinOverlayHelper {

	private static final String TAG = "PinOverlayHelper";
	
	public static void drawSelectPointOverlay(Context context, Handler mainThreadHandler, final MapView mapView, final POI poi, String message) {
		
//		String positionName = mSphinx.getMapEngine().getPOIName(position);
//		if (TextUtils.isEmpty(positionName)) {
//			positionName = context.getString(R.string.select_has_point);
//		}
//		String message = context.getString(touchMode.equals(TouchMode.CHOOSE_ROUTING_END_POINT) 
//				? R.string.select_where_as_start : R.string.select_where_as_end, positionName);
		try {
	        if(poi != null){
	        	// 单击选点图层
	            ItemizedOverlay overlay = new ItemizedOverlay(ItemizedOverlay.PIN_OVERLAY);
	            RotationTilt rt = new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
	            OverlayItem overlayItem = new OverlayItem(poi.getPosition(), createIconByResId(context, R.drawable.icon_longclick_bubble), 
	            		message, rt);
		        overlayItem.setInfoWindowType(InfoWindow.TYPE_SIMPLE);
		        overlayItem.setName(poi.getName());
		        
	            overlayItem.addEventListener(EventType.TOUCH, new OverlayItem.TouchEventListener() {
				    @Override
				    public void onTouchEvent(EventSource eventSource) {
				        OverlayItem overlayItem=(OverlayItem) eventSource;
				        mapView.getInfoWindow().setAssociatedOverlayItem(overlayItem);
				        mapView.getInfoWindow().setMessage(overlayItem.getMessage());
				        try{
				            mapView.getInfoWindow().setPosition(overlayItem.getPosition());
				        }catch(Exception e){
				            e.printStackTrace();
				        }
				        
				        mapView.getInfoWindow().setOffset(new XYFloat(0f,(float)(overlayItem.getIcon().getOffset().y)),
				                overlayItem.getRotationTilt());
				        mapView.getInfoWindow().type = overlayItem.getInfoWindowType();
				        mapView.getInfoWindow().setTextAlign(InfoWindow.TextAlign.CENTER);
				        mapView.getInfoWindow().setVisible(true);
				    }
				});
	            overlayItem.isFoucsed = true;
	            overlay.addOverlayItem(overlayItem);
	            mapView.addOverlay(overlay);

	            LogWrapper.d(TAG, "drawSelectPointOverlay: " + overlayItem.getInfoWindowType());
	            
	            mainThreadHandler.sendEmptyMessage(Sphinx.ADJUST_SHOW_FOCUSED_OVERLAYITEM);
                mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
	        }
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void drawLongClickOverlay(Context context, Handler mainThreadHandler, final MapView mapView, final Position position, final String name) {
		try {
	        if(position != null){
	        	// 长按选点图层
	            ItemizedOverlay overlay = new ItemizedOverlay(ItemizedOverlay.PIN_OVERLAY);
	            RotationTilt rt = new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
	            OverlayItem overlayItem = new OverlayItem(position, createIconByResId(context, R.drawable.icon_longclick_bubble), 
	            		name, rt);
		        overlayItem.setInfoWindowType(InfoWindow.TYPE_EXPEND);
		        overlayItem.setName(overlayItem.getMessage());
		        
	            overlayItem.addEventListener(EventType.TOUCH, new OverlayItem.TouchEventListener() {
				    @Override
				    public void onTouchEvent(EventSource eventSource) {
				        OverlayItem overlayItem=(OverlayItem) eventSource;
				        mapView.getInfoWindow().setAssociatedOverlayItem(overlayItem);
				        mapView.getInfoWindow().setMessage(overlayItem.getMessage());
				        try{
				            mapView.getInfoWindow().setPosition(overlayItem.getPosition());
				        }catch(Exception e){
				            e.printStackTrace();
				        }
				        
				        mapView.getInfoWindow().setOffset(new XYFloat(0f,(float)(overlayItem.getIcon().getOffset().y)),
				                overlayItem.getRotationTilt());
				        mapView.getInfoWindow().type = overlayItem.getInfoWindowType();
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
                new XYInteger(bmp.getWidth()/2,bmp.getHeight()));
        return icon;
	}
}
