/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.decarta.CONFIG;
import com.decarta.android.event.EventListener;
import com.decarta.android.event.EventSource;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.BoundingBox;
import com.decarta.android.location.Position;
import com.decarta.android.map.Compass.PlaceLocation;
import com.decarta.android.map.MapLayer.MapLayerType;
import com.decarta.android.map.TilesView.MapPreference;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYFloat;
import com.decarta.android.util.XYInteger;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.view.ZoomControls;

/**
 * Container for tiles view, map controls such as zoom controller
 */
public class MapView extends RelativeLayout implements
		com.decarta.android.event.EventSource {
	public enum MapType{STREET,SATELLITE,HYBRID};
	public static HashMap<MapType,ArrayList<MapLayerType>> MapType_MapLayer_Visibility=new HashMap<MapType,ArrayList<MapLayerType>>();
	public static int ZOOM_LEVEL=13;
	/**
	 * map tilt from 0 to -45 when tilt the top of the map to the back of screen, and the bottom 
	 * of map to the front of screen
	 */
	public static float MAP_TILT_MIN=-45;
	public static long PAN_TO_POSITION_TIME_DEF=300*1000000;//500 miniseconds
	public static int DIGITAL_ZOOMING_TIME_PER_LEVEL=300*1000000;
	
	
	static{
		for(int i=0;i<MapType.values().length;i++){
			MapType mapType=MapType.values()[i];
			ArrayList<MapLayerType> mapLayerVisible=new ArrayList<MapLayerType>();
			if(mapType.equals(MapType.STREET)){
				mapLayerVisible.add(MapLayerType.STREET);
			}else if(mapType.equals(MapType.SATELLITE)){
				mapLayerVisible.add(MapLayerType.SATELLITE);
			}else if(mapType.equals(MapType.HYBRID)){
				mapLayerVisible.add(MapLayerType.SATELLITE);
				mapLayerVisible.add(MapLayerType.TRANSPARENT);
			}
			
			MapType_MapLayer_Visibility.put(mapType, mapLayerVisible);
		}
		
	}
	
	private ZoomControls zoomControls;
	private TilesView tilesView;
	private Map<Integer, ArrayList<EventListener>> eventListeners = new HashMap<Integer, ArrayList<EventListener>>();
	private float xRotation = 0;

	/**
	 * initialize zoom controls, register listener for zoom controls, initialize
	 * tiles view
	 * 
	 * @param context
	 */
	public MapView(Context context) {
		super(context);
		init(context);
	}

	/**
	 * The constructor to setup the map and map controls.
	 * 
	 * @param context
	 *            the activity which set this view as the content view
	 * @param attrs
	 *            the parameter for the view
	 */
	public MapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public void init(Context context){
		if(this.isInEditMode()){
			LogWrapper.i("MapView", "init view isInEditMode:true");
			return; 
		}
		LogWrapper.i("MapView", "init view isInEditMode:false");
		
		Util.init();
        
		zoomControls = new ZoomControls(context);

		tilesView = new TilesView(context, this);
		tilesView.setId(R.id.view_invalid);
		addView(tilesView, android.view.ViewGroup.LayoutParams.FILL_PARENT,
				android.view.ViewGroup.LayoutParams.FILL_PARENT);

		zoomControls.setOnZoomInClickListener(new OnClickListener() {
			public void onClick(View view) {
				MapView.this.zoomIn();
			}
		});

		zoomControls.setOnZoomOutClickListener(new OnClickListener() {
			public void onClick(View view) {
				MapView.this.zoomOut();
			}
		});
		
		if(CONFIG.COMPASS_PLACE_LOCATION>-1 && CONFIG.COMPASS_PLACE_LOCATION<=4){
	        Icon icon=Icon.getIcon(getResources(), R.drawable.ic_compass);
    		Compass compass=new Compass(icon.getSize(), icon.getOffset(), PlaceLocation.TOP_LEFT, icon);
    		compass.setPlaceLocation(PlaceLocation.values()[CONFIG.COMPASS_PLACE_LOCATION]);
    		try{
    			compass.addEventListener(EventType.TOUCH, new Compass.TouchEventListener() {
    				
    				@Override
    				public void onTouchEvent(EventSource eventSource) {
    					// TODO Auto-generated method stub
    					try{
    					    if (xRotation == MapView.MAP_TILT_MIN) {
    						    rotateXToDegree(0);
    						    xRotation = 0;
    					    } else {
    				            rotateXToDegree(MapView.MAP_TILT_MIN);
                                xRotation = MapView.MAP_TILT_MIN;
    					    }
    					}catch(Exception e){
    						
    					}
    					//rotateZToDegree(0);
    				}
    			});
    		}catch(Exception e){
    			
    		}
    		tilesView.setCompass(compass);
    		
    	}
		
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}

	@Override
	protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
		// TODO Auto-generated method stub
		super.onLayout(arg0, arg1, arg2, arg3, arg4);

	}

	/**
	 * 
	 * @return zoomControls for zoom in and zoom out
	 */
	public ZoomControls getZoomControls() {
		return zoomControls;
	}
	
	

	/**
	 * 
	 * @return event listener specific to the map only, not for the pin, info
	 *         window
	 */
	public Map<Integer, ArrayList<EventListener>> getEventListeners() {
		return eventListeners;
	}

	/**
	 * defines the event type, now it supports 4 types: TOUCH, MOVEEND, ZOOMEND,
	 * DOUBLECLICK
	 * 
	 */
	public static class EventType extends com.decarta.android.event.EventType {
		public static int MOVEEND = 3;
		public static int ZOOMEND = 4;
		public static int DOUBLECLICK = 5;
		public static int LONGCLICK = 6;
		
		public static int ROTATEEND=7;
		public static int TILTEND=8;
        public static int DOWNLOAD = 10;

        public static int TOUCHDOWN = 11;
	}
	
	/**
	 * listener for touchdown event
	 */
	public interface TouchDownEventListener extends EventListener{
		public void onTouchDownEvent(EventSource eventSource, Position pos);
	}
	
	/**
	 * listener for touch event
	 */
	public interface TouchEventListener extends EventListener{
		public void onTouchEvent(EventSource eventSource, Position pos);
	}

	/**
	 * define the moveend listener of map
	 * 
	 */
	public interface MoveEndEventListener extends
			com.decarta.android.event.EventListener {
		public void onMoveEndEvent(MapView mapView, Position position);
	}
	
    public interface ClickEventListener extends
            com.decarta.android.event.EventListener {
        public void onClickEvent();
    }
    
    public interface DownloadEventListener extends
            com.decarta.android.event.EventListener {
        public static final int STATE_DOWNLOADING = 1;
        public static final int STATE_DOWNLOADED = 200;
        public static final int STATE_DOWNLOAD_ERROR = 400;
        public void onDownloadEvent(int state);
    }

	/**
	 * define zoomend listener of map
	 * 
	 */
	public interface ZoomEndEventListener extends
			com.decarta.android.event.EventListener {
		public void onZoomEndEvent(MapView mapView, int newZoomLevel);
	}

	/**
	 * define dobule click listener of map
	 * 
	 */
	public interface DoubleClickEventListener extends
			com.decarta.android.event.EventListener {
		public void onDoubleClickEvent(MapView mapView, Position position);
	}

	/**
	 * define long click listener of map
	 * 
	 */
	public interface LongClickEventListener extends
			com.decarta.android.event.EventListener {
		public void onLongClickEvent(MapView mapView, Position position);
	}
	
	public interface RotateEndEventListener extends EventListener{
		public void onRotateEndEvent(MapView mapView,float rotation);
	}
	
	public interface TiltEndEventListener extends EventListener{
		public void onTiltEndEvent(MapView mapView,float tilt);
	}

	/**
	 * implement the addEventListener method of EventSource interface
	 */
	@Override
	public void addEventListener(int eventType, EventListener listener) throws APIException{
		if (!isSupportedEventListener(eventType, listener)) {
			throw new APIException("not valid event type/listener pair.");
		}
		if (!eventListeners.containsKey(eventType)) {
			eventListeners.put(eventType, new ArrayList<EventListener>());
		}
		eventListeners.get(eventType).add(listener);

	}

	@Override
	public boolean isSupportedEventListener(int eventType,
			EventListener listener) {
		// TODO Auto-generated method stub
		if (eventType == com.decarta.android.event.EventType.TOUCH
				&& (listener instanceof TouchEventListener)
				|| eventType == EventType.MOVEEND
				&& (listener instanceof MoveEndEventListener)
                || eventType == EventType.DOWNLOAD
                && (listener instanceof DownloadEventListener)
				|| eventType == EventType.ZOOMEND
				&& (listener instanceof ZoomEndEventListener)
				|| eventType == EventType.DOUBLECLICK
				&& (listener instanceof DoubleClickEventListener)
				|| eventType == EventType.LONGCLICK
				&& (listener instanceof LongClickEventListener)
				|| eventType == EventType.ROTATEEND
				&& (listener instanceof RotateEndEventListener)
				|| eventType == EventType.TILTEND
				&& (listener instanceof TiltEndEventListener)
				|| eventType == EventType.TOUCHDOWN
				&& (listener instanceof TouchDownEventListener)
				)
			return true;
		else
			return false;
	}

	@Override
	public void removeAllEventListeners(int eventType) {
		// TODO Auto-generated method stub
		if (eventListeners.containsKey(eventType)) {
			eventListeners.get(eventType).clear();
		}
	}

	@Override
	public void removeEventListener(int eventType, EventListener listener) throws APIException{
		// TODO Auto-generated method stub
		if (!isSupportedEventListener(eventType, listener)) {
			throw new APIException("not valid event type/listener pair.");
		}
		if (eventListeners.containsKey(eventType)) {
			eventListeners.get(eventType).remove(listener);
		}
	}

	/**
	 * provide a simple way to call all the moveend listeners together
	 * 
	 * @param position
	 *            the position that the map center have moved to
	 */
	public void executeMoveEndListeners(Position position) {
		if (eventListeners.containsKey(MapView.EventType.MOVEEND)) {
			ArrayList<EventListener> listeners = eventListeners
					.get(MapView.EventType.MOVEEND);
			for (int i = 0; i < listeners.size(); i++) {
				((MoveEndEventListener) (listeners.get(i))).onMoveEndEvent(
						this, position);
			}
		}
	}
    
    public void executeDownloadListeners(int state) {
        if (eventListeners.containsKey(MapView.EventType.DOWNLOAD)) {
            ArrayList<EventListener> listeners = eventListeners
                    .get(MapView.EventType.DOWNLOAD);
            for (int i = 0; i < listeners.size(); i++) {
                ((DownloadEventListener) (listeners.get(i))).onDownloadEvent(state);
            }
        }
    }

	/**
	 * provide a simple way to call all the zoomend listeners together
	 * 
	 * @param newZoomLevel
	 *            the new zoom level the map has zoomed to
	 */
	public void executeZoomEndListeners(int newZoomLevel) {
		if (eventListeners.containsKey(MapView.EventType.ZOOMEND)) {
			ArrayList<EventListener> listeners = eventListeners
					.get(MapView.EventType.ZOOMEND);
			for (int i = 0; i < listeners.size(); i++) {
				((ZoomEndEventListener) (listeners.get(i))).onZoomEndEvent(
						this, newZoomLevel);
			}
		}
	}

	public void executeTouchDownListeners(Position position) {
		if (eventListeners.containsKey(MapView.EventType.TOUCHDOWN)) {
			ArrayList<EventListener> listeners = eventListeners.get(MapView.EventType.TOUCHDOWN);
			for (int i = 0; i < listeners.size(); i++) {
				((TouchDownEventListener) (listeners.get(i))).onTouchDownEvent(this,
						position);
			}
		}
	}
	
	public void executeTouchListeners(Position position) {
		if (eventListeners
				.containsKey(com.decarta.android.event.EventType.TOUCH)) {
			ArrayList<EventListener> listeners = eventListeners
					.get(com.decarta.android.event.EventType.TOUCH);
			for (int i = 0; i < listeners.size(); i++) {
				((TouchEventListener) (listeners.get(i))).onTouchEvent(this,
						position);
			}
		}
	}

	public void executeDoubleClickListeners(Position position) {
		if (eventListeners.containsKey(EventType.DOUBLECLICK)) {
			ArrayList<EventListener> listeners = eventListeners
					.get(EventType.DOUBLECLICK);
			for (int i = 0; i < listeners.size(); i++) {
				((DoubleClickEventListener) (listeners.get(i)))
						.onDoubleClickEvent(this, position);
			}
		}
	}

	public void executeLongClickListeners(Position position) {
		if (eventListeners.containsKey(EventType.LONGCLICK)) {
			ArrayList<EventListener> listeners = eventListeners
					.get(EventType.LONGCLICK);
			for (int i = 0; i < listeners.size(); i++) {
				((LongClickEventListener) (listeners.get(i))).onLongClickEvent(
						this, position);
			}
		}
	}
	
	public void executeRotateEndListeners(float rotation) {
		if (eventListeners.containsKey(MapView.EventType.ROTATEEND)) {
			ArrayList<EventListener> listeners = eventListeners
					.get(MapView.EventType.ROTATEEND);
			for (int i = 0; i < listeners.size(); i++) {
				((RotateEndEventListener) (listeners.get(i))).onRotateEndEvent(
						this,rotation);
			}
		}
	}
	
	public void executeTiltEndListeners(float tilt) {
		if(!CONFIG.DRAW_BY_OPENGL) return;
		if (eventListeners.containsKey(MapView.EventType.TILTEND)) {
			ArrayList<EventListener> listeners = eventListeners
					.get(MapView.EventType.TILTEND);
			for (int i = 0; i < listeners.size(); i++) {
				((TiltEndEventListener) (listeners.get(i))).onTiltEndEvent(
						this,tilt);
			}
		}
	}

	// All external called TilesView function wrapped here

	/**
	 * refresh the map view only, not refresht the controls such as zoom control
	 */
	public void refreshMap() {
		tilesView.refreshMap();
	}
	
	/**
	 * switch map between street map, satellite map and hybrid map
	 * @param mapType
	 */
	public void changeMapType(MapType mapType){
		tilesView.changeMapType(mapType);
	}
	
	public MapType getMapType(){
		return tilesView.getMapType();
	}
	
	public void rotateZToDegree(float zRotation){
		tilesView.rotateZToDegree(zRotation);
	}
    
    public void rotateLocationZToDegree(float zRotation){
        tilesView.tkRotateZToDegree(zRotation);
    }
	
	public void rotateXToDegree(float xRotation){
		tilesView.rotateXToDegree(xRotation);
	}
	
	/**
	 * convert the screen xy coordinate the position
	 * 
	 * @param pos
	 */
	public XYFloat positionToScreenXY(Position pos) {
	    try{
		return tilesView.positionToScreenXY(pos);
	    } catch (APIException e) {
            // TODO: handle exception
        }
	    return null;
	}

	/**
	 * set the current map zoom level. this function don't only set the zoom
	 * level. you need to call { centerOnPosition} or { renderMap} to
	 * reload the whole map
	 */
	public void setZoomLevel(int newLevel) throws APIException {
		tilesView.setZoomLevel(newLevel);
	}

	public void centerOnPosition(Position position, float zoomLevel,
			boolean clear) {
	    try {
		tilesView.centerOnPosition(position, zoomLevel, clear);
	    } catch (APIException e) {
            e.printStackTrace();
        }
	}

	/**
	 * compose web service request using the position and current zoom level,
	 * initialize/reset the tiles and render
	 * 
	 * @param position
	 * @throws APIException
	 */
	public void centerOnPosition(Position position) throws APIException {
//		tilesView.centerOnPosition(position);
	}

	/**
	 * compose web service request using the position and zoom level,
	 * initialize/reset the tiles and render
	 * 
	 * @param position
	 * @param zoomLevel
	 * @throws APIException
	 */
	public void centerOnPosition(Position position, float zoomLevel) {
	    try {
		tilesView.centerOnPosition(position, zoomLevel);
	    } catch (APIException e) {
            e.printStackTrace();
        }
	}

	/**
	 * pan to the specified position
	 * 
	 * @param position
	 */
	public void panToPosition(Position position) {
        try{
        tilesView.zoomTo((int)tilesView.getZoomLevel(), position, -1, null);
        }catch(APIException e){
            LogWrapper.w("MapView", "panToPosition exception:"+e.getMessage());
        }
	}
//	
//	/**
//	 * pan to the specified position
//	 * 
//	 * @param position
//	 */
//	public void panToPosition(Position position, long duration) {
//        try {
//		tilesView.panToPosition(position,duration,null);
//        } catch (APIException e) {
//            e.printStackTrace();
//        }
//	}
//	
//	/**
//	 * pan to the position inside duration time, and execute listener when the pan is done.
//	 * @param position
//	 * @param duration
//	 * @param listener
//	 * @throws APIException
//	 */
//	public void panToPosition(Position position, long duration, MoveEndEventListener listener) {
//	    try {
//		tilesView.panToPosition(position,duration,listener);
//	    } catch (Exception e) {
//            e.printStackTrace();
//        }
//	}

	/**
	 * clear the routeId, overlays and info window
	 * 
	 * @throws APIException
	 */
	public void clearMap() throws APIException {
		tilesView.clearMap();
	}

	/**
	 * get the current zoom level
	 * 
	 */
	public float getZoomLevel() {
		return tilesView.getZoomLevel();
	}

	public XYInteger getDisplaySize() {
		return tilesView.getDisplaySize();
	}

	/**
	 * get the current center position
	 * 
	 */
	public Position getCenterPosition() {
		return tilesView.getCenterPosition();
	}
	
	/**
	 * get the info window
	 * 
	 */
	public InfoWindow getInfoWindow() {
		return tilesView.getInfoWindow();
	}

	public MapPreference getMapPreference() {
		return tilesView.getMapPreference();
	}
	
	/**
	 * delete all overlays
	 */
	public void deleteOverlays() {
		tilesView.getOverlays().clear();
	}
	
	/**
	 * delete one overlay
	 * @param overlay
	 */
	public void deleteOverlay(ItemizedOverlay overlay){
		tilesView.getOverlays().remove(overlay);
	}
	
	/**
	 * delete overlay by it's sequence in overlays array
	 * @param i
	 */
	public void deleteOverlay(int i){
		tilesView.getOverlays().remove(i);
	}

	/**
	 * remove overlay by it's unique name
	 * @param overlayName
	 * @return the removed overlay
	 */
	public ItemizedOverlay deleteOverlaysByName(String overlayName) {
		for(int i=tilesView.getOverlays().size()-1;i>=0;i--){
			if(tilesView.getOverlays().get(i).getName().equals(overlayName)){
				return tilesView.getOverlays().remove(i);
			}
		}
		return null;
	}
	
    public Shape deleteShapeByName(String shapeName) {
        for(int i=tilesView.getShapes().size()-1;i>=0;i--){
            if(tilesView.getShapes().get(i).getName().equals(shapeName)){
                return tilesView.getShapes().remove(i);
            }
        }
        return null;
    }

	/**
	 * add pin group/overlay to the map
	 */
	public void addOverlay(ItemizedOverlay overlay) throws APIException{
		List<ItemizedOverlay> overlays=tilesView.getOverlays();
		for(int i=0;i<overlays.size();i++){
			if(overlay.getName().equals(overlays.get(i).getName())){
				throw new APIException("duplicate overlay name, it should be unique");
			}
		}
		tilesView.getOverlays().add(overlay);
	}
	
	/**
	 * get pin group/overlay by unique name
	 * @param overlayName
	 */
	public ItemizedOverlay getOverlaysByName(String overlayName){
		for(int i=0;i<tilesView.getOverlays().size();i++){
			ItemizedOverlay overlay=tilesView.getOverlays().get(i);
			if(overlay.getName().equals(overlayName)){
				return overlay;
			}
		}
		return null;
	}
	
    public ItemizedOverlay getCurrentOverlay(){
        for(int i=0;i<tilesView.getOverlays().size();i++){
            ItemizedOverlay overlay=tilesView.getOverlays().get(i);
            if(!overlay.getName().equals(ItemizedOverlay.MY_LOCATION_OVERLAY)){
                return overlay;
            }
        }
        return null;
    }
    
    public Shape getCurrentShape() {
        for(int i=0;i<tilesView.getShapes().size();i++){
            Shape overlay=tilesView.getShapes().get(i);
            if(!overlay.getName().equals(Shape.MY_LOCATION)){
                return overlay;
            }
        }
        return null;
    }
	
	public int getOverlaysSize(){
		return tilesView.getOverlays().size();
	}
	
	/**
	 * get overlay by it's sequence in the overlays array
	 * @param i
	 */
	public ItemizedOverlay getOverlay(int i){
		return tilesView.getOverlays().get(i);
	}
	
	public int getShapesSize(){
		return tilesView.getShapes().size();
		
	}
	
	/**
	 * get shape according to it's sequence in the shapes array
	 * @param i
	 */
	public Shape getShape(int i){
		return tilesView.getShapes().get(i);
	}
	
	/**
	 * get shape by it's unique name
	 * @param shapeName
	 */
	public Shape getShapesByName(String shapeName){
		for(int i=0;i<tilesView.getShapes().size();i++){
			if(tilesView.getShapes().get(i).getName().equals(shapeName)){
				return tilesView.getShapes().get(i);
			}
			
		}
		return null;
	}
	
	/**
	 * add shape to shapes
	 * @param shape
	 * @throws APIException
	 */
	public void addShape(Shape shape) throws APIException{
		List<Shape> shapes=tilesView.getShapes();
		for(int i=0;i<shapes.size();i++){
			if(shape.getName().equals(shapes.get(i).getName())){
				throw new APIException("duplicate shape name, it should be unique");
			}
		}
		tilesView.getShapes().add(shape);
	}
	
	/**
	 * remove all of the shapes
	 */
	public void removeShapes(){
		tilesView.getShapes().clear();
	}
	/**
	 * remove shape by it's sequence in the shapes array
	 * @param i
	 * @return removed shape
	 */
	public Shape removeShape(int i){
		if(i>=tilesView.getShapes().size()) return null;
		return tilesView.getShapes().remove(i);
	}
	/**
	 * remove one shape
	 * @param shape
	 */
	public void removeShape(Shape shape){
		tilesView.getShapes().remove(shape);
	}
	/**
	 * remove shape by it's unique name
	 * @param shapeName
	 * @return removed shape
	 */
	public Shape removeShapesByName(String shapeName){
		for(int i=tilesView.getShapes().size()-1;i>=0;i--){
			if(tilesView.getShapes().get(i).getName().equals(shapeName)){
				return tilesView.getShapes().remove(i);
			}
			
		}
		return null;
	}

	/**
	 * get the distance between current center to the top border of screen
	 * 
	 * @return vertical radius
	 */
	public double getRadiusY() {
		return tilesView.getRadiusY();
	}

	/**
	 * get the distance between current center to the left border of screen
	 * 
	 * @return horizontal radius
	 */
	public double getRadiusX() {
		return tilesView.getRadiusX();
	}
	
	public Compass getCompass(){
		return tilesView.getCompass();
	}

	/**
	 * convert screen coordinate to position
	 * 
	 * @param screenXY
	 */
	public Position screenXYToPos(XYFloat screenXY){
		return tilesView.screenXYToPos(screenXY);
	}

	/**
	 * move the map to left and above pixels specified. Negative number means
	 * opposite direction.
	 * 
	 * @param left
	 * @param top
	 * 
	 */
	public void moveView(float left, float top) {
		tilesView.moveView(left, top);
	}

	/**
	 * zoom the map to the new level. This can be used instead of setZoomLevel,
	 * which only set the zoom leve and doesn't render the map again.
	 * 
	 * @param newZoomLevel
	 */
	public void zoomTo(int newZoomLevel){
		try{
			tilesView.zoomTo(newZoomLevel, null, -1, null);
		}catch(APIException e){
			LogWrapper.w("MapView", "zoomTo exception:"+e.getMessage());
		}
	}
	
	/**
	 * zoom the map to newZoomLevel at zoomCenter
	 * @param newZoomLevel
	 * @param zoomCenter
	 * @throws APIException
	 */
	public void zoomTo(int newZoomLevel, Position zoomCenter) {
        try{
		tilesView.zoomTo(newZoomLevel, zoomCenter, -1, null);
        }catch(APIException e){
            LogWrapper.w("MapView", "zoomTo exception:"+e.getMessage());
        }
		
	}
	
	/**
	 * zoom the map to newZoomLevel at zoomCenter with ZoomEnd listener
	 * @param newZoomLevel
	 * @param zoomCenter
	 * @param duration
	 * @param listener
	 * @throws APIException
	 */
	public void zoomTo(int newZoomLevel, Position zoomCenter, long duration, ZoomEndEventListener listener) {
        try{
		tilesView.zoomTo(newZoomLevel, zoomCenter, duration, listener);
        }catch(APIException e){
            LogWrapper.w("MapView", "zoomTo exception:"+e.getMessage());
        }
	}

	/**
	 * zoom in the map one level
	 */
	public void zoomIn(){
		try{
			tilesView.zoomTo(Math.round(getZoomLevel())+1, null, -1, null);
		}catch(APIException e){
			LogWrapper.w("MapView","zoomIn exception:"+e.getMessage());
		}
	}

	public void zoomIn(Position zoomCenter) {
        try{
		tilesView.zoomTo(Math.round(getZoomLevel()+1),zoomCenter,-1,null);
        }catch(APIException e){
            LogWrapper.w("MapView","zoomIn exception:"+e.getMessage());
        }
	}

	/**
	 * zoom in the map one level
	 */
	public void zoomOut(){
		try{
			tilesView.zoomTo(Math.round(getZoomLevel())-1, null, -1, null);
		}catch(APIException e){
			LogWrapper.w("MapView","zoomOut exception:"+e.getMessage());
		}
	}

	public void zoomOut(Position zoomCenter) throws APIException {
		tilesView.zoomTo(Math.round(getZoomLevel()-1),zoomCenter,-1,null);
	}
	
	/**
     * returns the current boundingbox for the viewable area of map on the screen, useful for clipping
     * @return BoundingBox
     */
    public BoundingBox getScreenBoundingBox() throws APIException{
    	return tilesView.getScreenBoundingBox();
    }

	// end of wrapped TilesView functions

    public int getCenterCityId() {
        Position position = getCenterPosition();
        int cityId = MapEngine.getInstance().getCityId(position);
        return cityId;
    }
    
    public Rect getPadding() {
        return tilesView.getPadding();
    }
	
    public RectF getInfoWindowRecF() {
    	return tilesView.getInfoWindow().getInfoWindowRecF();
    }
    
    public interface SnapMap {
        public void finish(Uri uri);
    }
    
    /**
     * 快照地图，完成后还原地图状态
     * @param activity
     * @param snapMap
     * @param position
     * @param mapScene
     */
    public void snapMapView(final Activity activity, final SnapMap snapMap, final Position position, final MapScene mapScene) {
        if (activity == null || snapMap == null || position == null) {
            return;
        }
        View custom = activity.getLayoutInflater().inflate(R.layout.loading, null);
        TextView loadingTxv = (TextView)custom.findViewById(R.id.loading_txv);
        loadingTxv.setText(R.string.doing_and_wait);
        ActionLog.getInstance(activity).addAction(ActionLog.DIALOG, loadingTxv);
        
        final Dialog tipProgressDialog = CommonUtils.showNormalDialog(activity, custom);
        tipProgressDialog.setCancelable(true);
        tipProgressDialog.setCanceledOnTouchOutside(false);
        tipProgressDialog.setOnDismissListener(new OnDismissListener() {
            
            @Override
            public void onDismiss(DialogInterface dialog) {
                tilesView.resetSnap();
            }
        });
        tipProgressDialog.show();
        
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                tilesView.requestSnap(position);
                int waitTimes = 0;
                // 等待快照完成，最长等待时间为60s
                while (waitTimes < 30 && tipProgressDialog.isShowing() && tilesView.isSnap()) {
                    try {
                        Thread.sleep(2*1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    waitTimes++;
                }
                
                activity.runOnUiThread(new Runnable() {
                    
                    @Override
                    public void run() {
                        if (tipProgressDialog != null && tipProgressDialog.isShowing()) {
                            tipProgressDialog.dismiss();
                        }            
                        
                        restoreScene(mapScene);
                        
                        Bitmap bm = tilesView.getSnapBitmap();
                        String mapPath = TKConfig.getDataPath(true);
                        Uri uri = null;
                        if (bm != null && !TextUtils.isEmpty(mapPath)) {
                            uri = CommonUtils.bitmap2Png(bm, "mapsnap.png", mapPath);
                            if (bm.isRecycled() == false) {
                                bm.recycle();
                            }
                            bm = null;
                        }
                        snapMap.finish(uri);
                    }
                });
            }
        }).start();
    }
    
    public void showOverlay(String overlayName, boolean top){
        tilesView.showOverlay(overlayName, top);
    }
    
    public MapText getMapText() {
        return tilesView.getMapText();
    }
    
    public void setStopRefreshMyLocation(boolean stopRefreshMyLocation) {
        tilesView.stopRefreshMyLocation = stopRefreshMyLocation;
    }
    
    public MapScene getCurrentMapScene() {
        MapScene mapScene = new MapScene();
        mapScene.position = getCenterPosition();
        mapScene.zoomLevel = (int) getZoomLevel();
        mapScene.itemizedOverlay = getCurrentOverlay();
        mapScene.shape = getCurrentShape();
        return mapScene;
    }
    
    /**
     * 还原地图状态（中心点、Shape、OverlayItem）
     * @param mapScene
     */
    public void restoreScene(MapScene mapScene) {
        try {
            clearMap();
            if (mapScene == null) {
                return;
            }
            if (mapScene.shape != null) {
                addShape(mapScene.shape);
            }
            if (mapScene.itemizedOverlay != null) {
                addOverlay(mapScene.itemizedOverlay);
            }
            showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
            if (mapScene.position != null) {
                centerOnPosition(mapScene.position, mapScene.zoomLevel);
            }
        } catch (APIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static class MapScene {
        public Position position;
        public int zoomLevel;
        public ItemizedOverlay itemizedOverlay;
        public Shape shape;
    }
}
