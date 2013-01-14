package com.tigerknows.maps;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.tigerknows.view.TrafficQueryFragment;

public class PinOverlayHelper {

	private static final String TAG = "PinOverlayHelper";
	
	public static void drawSelectPointOverlay(Context context, Handler mainThreadHandler, final MapView mapView, boolean isStart, String name, Position position) {
		
//		String positionName = mSphinx.getMapEngine().getPOIName(position);
//		if (TextUtils.isEmpty(positionName)) {
//			positionName = context.getString(R.string.select_has_point);
//		}
		try {
	        if(name != null && position != null){
	        	// 单击选点图层
	            POI poi = new POI();
                poi.setSourceType(POI.SOURCE_TYPE_CLICK_SELECT_POINT);
                poi.setName(name);
                poi.setPosition(position);
                
	            String message = context.getString(isStart ? R.string.select_where_as_start : R.string.select_where_as_end, name);
	            ItemizedOverlay overlay = new ItemizedOverlay(ItemizedOverlay.PIN_OVERLAY);
	            RotationTilt rt = new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
	            OverlayItem overlayItem = new OverlayItem(poi.getPosition(), createIconByResId(context, R.drawable.icon_longclick_bubble), 
	            		message, rt);
	            overlayItem.setAssociatedObject(poi);
		        
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
	
	public static void drawLongClickOverlay(final Context context, Handler mainThreadHandler, final MapView mapView, final String name, final Position position) {
		try {
	        if(position != null){
	        	// 长按选点图层
	            POI poi = new POI();
	            poi.setSourceType(POI.SOURCE_TYPE_LONG_CLICK_SELECT_POINT);
	            poi.setName(name);
                poi.setPosition(position);
                
	            ItemizedOverlay overlay = new ItemizedOverlay(ItemizedOverlay.PIN_OVERLAY);
	            RotationTilt rt = new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
	            OverlayItem overlayItem = new OverlayItem(position, createIconByResId(context, R.drawable.icon_longclick_bubble), 
	            		name, rt);
		        overlayItem.setAssociatedObject(poi);
		        
	            overlayItem.addEventListener(EventType.TOUCH, new OverlayItem.TouchEventListener() {
				    @Override
				    public void onTouchEvent(EventSource eventSource) {
				        OverlayItem overlayItem=(OverlayItem) eventSource;
				        try{
				            mapView.getInfoWindow().setPosition(overlayItem.getPosition());
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
