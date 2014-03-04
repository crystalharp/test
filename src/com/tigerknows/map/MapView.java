/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.tigerknows.map;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
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
import com.decarta.android.map.Compass;
import com.decarta.android.map.Icon;
import com.decarta.android.map.InfoWindow;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.map.Shape;
import com.decarta.android.map.TilesView;
import com.decarta.android.map.Compass.PlaceLocation;
import com.decarta.android.map.MapLayer.MapLayerType;
import com.decarta.android.map.TilesView.MapPreference;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYDouble;
import com.decarta.android.util.XYFloat;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.common.ActionLog;
import com.tigerknows.ui.InfoWindowFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.ZoomControls;

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
    Compass compass;
	private float xRotation = 0;

	private Sphinx sphinx;

    public void setSphinx(Sphinx sphinx) {
        this.sphinx = sphinx;
    }

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
		    compass=new Compass(icon.getSize(), icon.getOffset(), PlaceLocation.TOP_LEFT, icon);
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
		}

		tilesView = new TilesView(context, this);
		tilesView.setId(R.id.view_invalid);
		addView(tilesView, android.view.ViewGroup.LayoutParams.FILL_PARENT,
				android.view.ViewGroup.LayoutParams.FILL_PARENT);
        tilesView.setCompass(compass);

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
        public static int DRAWFRAME = 1;
		public static int SURFACECREATED = 2;
		public static int MOVEEND = 3;
		public static int ZOOMEND = 4;
		public static int DOUBLECLICK = 5;
		public static int LONGCLICK = 6;

		public static int ROTATEEND=7;
		public static int TILTEND=8;
        public static int DOWNLOAD = 10;

        public static int TOUCHUP = 11;
        public static int MULTITOUCHZOOM = 12;
        public static int CLICKPOI = 13;
        public static int UPDATEPOIPOSITION = 14; //地图上选中的poi修正了位置
        public static int TOUCHDOWN = 15;
        public static int TOUCHPIN = 16;
	}

    public interface DrawFrameEventListener extends EventListener{
        public void onDrawFrameEvent();
    }

	/**
	 * listener for touchdown event
	 */
	public interface SurfaceCreatedEventListener extends EventListener{
		public void onSurfaceCreatedEvent();
	}

	/**
	 * listener for touch event
	 */
	public interface TouchEventListener extends EventListener{
		public void onTouchEvent(EventSource eventSource, Position pos);
	}

    public interface ClickPOIEventListener extends EventListener{
        public void onClickPOIEvent(EventSource eventSource, Position pos, String name, Position touPosition);
    }

    public interface UpdatePoiPositionListener extends EventListener{
    	public void onUpdatePoiPosition(EventSource eventSource, Position pos, String name);
    }

	/**
	 * define the moveend listener of map
	 *
	 */
	public interface MoveEndEventListener extends
			com.decarta.android.event.EventListener {
		public void onMoveEndEvent(MapView mapView, Position position);
	}

    public interface DownloadEventListener extends
            com.decarta.android.event.EventListener {
        public static final int STATE_DOWNLOADING = 1;
        public static final int STATE_DOWNLOADED = 200;
        public static final int STATE_DOWNLOAD_ERROR = 400;
        public void onDownloadEvent(int state);
    }

    public interface MapEventListener extends
            com.decarta.android.event.EventListener {
        public void onShowedEvent();
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
     * define multi touch zoom listener of map
     *
     */
    public interface MultiTouchZoomEventListener extends
            com.decarta.android.event.EventListener {
        public void onMultiTouchZoomEvent(MapView mapView, float newZoomLevel);
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
				|| eventType == EventType.TOUCHUP
				&& (listener instanceof TouchEventListener)
                || eventType == EventType.MULTITOUCHZOOM
                && (listener instanceof MultiTouchZoomEventListener)
                || eventType == EventType.SURFACECREATED
                && (listener instanceof SurfaceCreatedEventListener)
                || eventType == EventType.DRAWFRAME
                && (listener instanceof DrawFrameEventListener)
                || eventType == EventType.CLICKPOI
                && (listener instanceof ClickPOIEventListener)
                || eventType == EventType.UPDATEPOIPOSITION
                && (listener instanceof UpdatePoiPositionListener)
                || eventType == EventType.TOUCHDOWN
                && (listener instanceof TouchEventListener)
                || eventType == EventType.TOUCHPIN
                && (listener instanceof TouchEventListener)
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

    public void executeClickPOIEventListener(Position position, String name, Position touchPosition) {
        if (eventListeners.containsKey(MapView.EventType.CLICKPOI)) {
            ArrayList<EventListener> listeners = eventListeners
                    .get(MapView.EventType.CLICKPOI);
            for (int i = 0; i < listeners.size(); i++) {
                ((ClickPOIEventListener) (listeners.get(i))).onClickPOIEvent(this, position, name, touchPosition);
            }
        }
    }

    public void executeUpdateMapPOIPostionEventListener(Position position, String name) {
        if (eventListeners.containsKey(MapView.EventType.UPDATEPOIPOSITION)) {
            ArrayList<EventListener> listeners = eventListeners
                    .get(MapView.EventType.UPDATEPOIPOSITION);
            for (int i = 0; i < listeners.size(); i++) {
                ((UpdatePoiPositionListener) (listeners.get(i))).onUpdatePoiPosition(this, position, name);
            }
        }
    }

    public void executeDrawFrameListeners() {
        if (eventListeners.containsKey(MapView.EventType.DRAWFRAME)) {
            ArrayList<EventListener> listeners = eventListeners
                    .get(MapView.EventType.DRAWFRAME);
            for (int i = 0; i < listeners.size(); i++) {
                ((DrawFrameEventListener) (listeners.get(i))).onDrawFrameEvent();
            }
        }
    }

	public void executeSurfaceCreatedListeners() {
		if (eventListeners.containsKey(MapView.EventType.SURFACECREATED)) {
			ArrayList<EventListener> listeners = eventListeners
					.get(MapView.EventType.SURFACECREATED);
			for (int i = 0; i < listeners.size(); i++) {
				((SurfaceCreatedEventListener) (listeners.get(i))).onSurfaceCreatedEvent();
			}
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

	/**
	 * provide a simple way to call all the multi touch listeners together
	 *
	 * @param newZoomLevel
	 *            the new zoom level the map has zoomed to
	 */
	public void executeMultiTouchZoomListeners(float newZoomLevel) {
		if (eventListeners.containsKey(MapView.EventType.MULTITOUCHZOOM)) {
			ArrayList<EventListener> listeners = eventListeners
					.get(MapView.EventType.MULTITOUCHZOOM);
			for (int i = 0; i < listeners.size(); i++) {
				((MultiTouchZoomEventListener) (listeners.get(i))).onMultiTouchZoomEvent(
						this, newZoomLevel);
			}
		}
	}

    public void executeTouchPinListeners(Position position) {
        if (eventListeners.containsKey(MapView.EventType.TOUCHPIN)) {
            ArrayList<EventListener> listeners = eventListeners.get(MapView.EventType.TOUCHPIN);
            for (int i = 0; i < listeners.size(); i++) {
                ((TouchEventListener) (listeners.get(i))).onTouchEvent(this,
                        position);
            }
        }
    }

	public void executeTouchUpListeners(Position position) {
		if (eventListeners.containsKey(MapView.EventType.TOUCHUP)) {
			ArrayList<EventListener> listeners = eventListeners.get(MapView.EventType.TOUCHUP);
			for (int i = 0; i < listeners.size(); i++) {
				((TouchEventListener) (listeners.get(i))).onTouchEvent(this,
						position);
			}
		}
	}

    public void executeTouchDownListeners(Position position) {
        if (eventListeners.containsKey(MapView.EventType.TOUCHDOWN)) {
            ArrayList<EventListener> listeners = eventListeners.get(MapView.EventType.TOUCHDOWN);
            for (int i = 0; i < listeners.size(); i++) {
                ((TouchEventListener) (listeners.get(i))).onTouchEvent(this,
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
//	public void setZoomLevel(int newLevel) throws APIException {
//		tilesView.setZoomLevel(newLevel);
//	}

	public void centerOnPosition(Position position, float zoomLevel,
			boolean refreshMap) {
	    try {
	    	tilesView.centerOnPosition(position, zoomLevel, refreshMap);
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
		tilesView.centerOnPosition(position);
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
        	tilesView.panToPosition(position, -1, null);
        }catch(APIException e){
            LogWrapper.w("MapView", "panToPosition exception:"+e.getMessage());
        }
	}

	/**
	 * pan to the specified position
	 *
	 * @param position
	 */
	public void panToPosition(Position position, long duration) {
        try {
        	tilesView.panToPosition(position,duration,null);
        } catch (APIException e) {
            e.printStackTrace();
        }
	}

	/**
	 * pan to the position inside duration time, and execute listener when the pan is done.
	 * @param position
	 * @param duration
	 * @param listener
	 * @throws APIException
	 */
	public void panToPosition(Position position, long duration, MoveEndEventListener listener) {
	    try {
	    	tilesView.panToPosition(position,duration,listener);
	    } catch (Exception e) {
            e.printStackTrace();
        }
	}

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
	public ItemizedOverlay getOverlaysByName(String overlayName) {
		return tilesView.getOverlaysByName(overlayName);
	}

    public ItemizedOverlay getCurrentOverlay(){
        for(int i=tilesView.getOverlays().size()-1;i>=0;i--){
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

	public void setZoomControlsState(float zoomLevel) {
		if (zoomLevel >= CONFIG.ZOOM_UPPER_BOUND) {
            zoomControls.setIsZoomInEnabled(false);
            zoomControls.setIsZoomOutEnabled(true);
        } else if (zoomLevel <= CONFIG.ZOOM_LOWER_BOUND) {
            zoomControls.setIsZoomInEnabled(true);
        	zoomControls.setIsZoomOutEnabled(false);
        } else {
        	zoomControls.setIsZoomInEnabled(true);
        	zoomControls.setIsZoomOutEnabled(true);
        }
	}

	public void setZoomLevel(int newZoomLevel){
		try{
			tilesView.setZoomLevel(newZoomLevel);
			setZoomControlsState(newZoomLevel);
		}catch(APIException e){
			LogWrapper.w("MapView", "zoomTo exception:"+e.getMessage());
		}
	}

	/**
	 * zoom the map to the new level. This can be used instead of setZoomLevel,
	 * which only set the zoom level and doesn't render the map again.
	 *
	 * @param newZoomLevel
	 */
	public void zoomTo(int newZoomLevel, ZoomEndEventListener listener) {
        try{
        	tilesView.zoomTo(newZoomLevel, null, -1, listener);
        } catch(APIException e) {
            LogWrapper.w("MapView", "zoomTo exception:"+e.getMessage());
        }
	}

	/**
	 * zoom in the map one level
	 */
	public void zoomIn() {
		try{
			tilesView.zoomTo(Math.round(getZoomLevel()) + 1, null, -1, null);
		}catch(APIException e){
			LogWrapper.w("MapView","zoomIn exception:"+e.getMessage());
		}
	}

	public void zoomIn(Position zoomCenter) {
        try{
        	tilesView.zoomTo(Math.round(getZoomLevel() + 1), zoomCenter, -1, null);
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

	// end of wrapped TilesView functions

    public int getCenterCityId() {
        Position position = getCenterPosition();
        int cityId = MapEngine.getCityId(position);
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
    public void snapMapView(final Activity activity, final SnapMap snapMap, Position position, final MapScene mapScene) {
        if (activity == null || snapMap == null || position == null) {
            return;
        }
        final boolean stopRefreshMyLocation = isStopRefreshMyLocation();
        setStopRefreshMyLocation(false);
        refreshMap();
        final Position centerPos = position.clone();
        View custom = activity.getLayoutInflater().inflate(R.layout.loading, null);
        TextView loadingTxv = (TextView)custom.findViewById(R.id.loading_txv);
        loadingTxv.setText(R.string.doing_and_wait);
        ActionLog.getInstance(activity).addAction(ActionLog.Dialog, activity.getString(R.string.doing_and_wait));

        final Dialog tipProgressDialog = Utility.showNormalDialog(activity, custom);
        tipProgressDialog.setCancelable(true);
        tipProgressDialog.setCanceledOnTouchOutside(false);
        tipProgressDialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                tilesView.isCancelSnap = true;
                synchronized (centerPos) {
                    centerPos.notifyAll();
                }
            }
        });
        tipProgressDialog.show();

        new Thread(new Runnable() {

            @Override
            public void run() {

                tilesView.requestSnap(centerPos);

                synchronized (centerPos) {
                    try {
                        centerPos.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        if (tipProgressDialog != null && tipProgressDialog.isShowing()) {
                            tipProgressDialog.dismiss();
                        }

                        tilesView.stopSnap();
                        restoreScene(mapScene);

                        if (tilesView.isCancelSnap == false) {
                            Bitmap bm = tilesView.getSnapBitmap();
                            String path = TKConfig.getSnapShotPath();
                            Uri uri = null;
                            if (bm != null && !TextUtils.isEmpty(path)) {
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                                uri = Utility.bitmap2Png(bm, simpleDateFormat.format(Calendar.getInstance().getTime())+".png", path);
                                if (bm.isRecycled() == false) {
                                    bm.recycle();
                                }
                                bm = null;
                            }
                            snapMap.finish(uri);
                        }
                        setStopRefreshMyLocation(stopRefreshMyLocation);
                    }
                });
            }
        }).start();

        // 此线程在60s后唤醒centerPos，如果此时快照地图还没有完成，则立即结束
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(15*1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                synchronized (centerPos) {
                    centerPos.notifyAll();
                }
            }
        }).start();
    }

    public void showOverlay(String overlayName, boolean top){
        tilesView.showOverlay(overlayName, top);
    }

    public boolean isStopRefreshMyLocation() {
        return tilesView.stopRefreshMyLocation;
    }

    public void setStopRefreshMyLocation(boolean stopRefreshMyLocation) {
        tilesView.stopRefreshMyLocation = stopRefreshMyLocation;

        if (stopRefreshMyLocation == false) {
            sphinx.updateMyLocation();
            tilesView.refreshMap();
        }
    }

    public MapScene getCurrentMapScene() {
        MapScene mapScene = new MapScene();
        mapScene.infoWindow = getInfoWindow().isVisible();
        mapScene.position = getCenterPosition();
        mapScene.zoomLevel = (int) getZoomLevel();
        List<ItemizedOverlay> itemizedOverlayList = new ArrayList<ItemizedOverlay>();
        itemizedOverlayList.addAll(tilesView.getOverlays());
        for (int i = itemizedOverlayList.size() - 1; i >= 0; i--) {
            if (itemizedOverlayList.get(i).getName().equals(ItemizedOverlay.MY_LOCATION_OVERLAY)) {
                itemizedOverlayList.remove(i);
            }
        }
        mapScene.itemizedOverlayList = itemizedOverlayList;
        List<Shape> shapeList = new ArrayList<Shape>();
        shapeList.addAll(tilesView.getShapes());
        for (int i = shapeList.size() - 1; i >= 0; i--) {
            if (shapeList.get(i).getName().equals(Shape.MY_LOCATION)) {
                shapeList.remove(i);
            }
        }
        mapScene.shapeList = shapeList;
        InfoWindowFragment infoWindowFragment = sphinx.getInfoWindowFragment();
        ItemizedOverlay itemizedOverlay = infoWindowFragment.getItemizedOverlay();
        ItemizedOverlay currentOverlay = getCurrentOverlay();
        if (itemizedOverlay != null &&
                currentOverlay != null &&
                itemizedOverlay.getName().equals(currentOverlay.getName())) {
            mapScene.fragmentId = infoWindowFragment.getOwerFragmentId();
            mapScene.overlayItem = itemizedOverlay.getItemByFocused();
        } else {
            mapScene.overlayItem = null;
        }
        return mapScene;
    }

    /**
     * 还原地图状态（中心点、Shape、OverlayItem）
     * @param mapScene
     */
    public void restoreScene(MapScene mapScene) {
        try {
            if (mapScene == null) {
                return;
            }

            clearMap();
            getInfoWindow().setVisible(mapScene.infoWindow);
            if (mapScene.shapeList != null) {
                tilesView.getShapes().addAll(mapScene.shapeList);
            }
            if (mapScene.itemizedOverlayList != null) {
                tilesView.getOverlays().addAll(mapScene.itemizedOverlayList);
            }
            showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
            if (mapScene.position != null) {
                centerOnPosition(mapScene.position, mapScene.zoomLevel);
            }
            sphinx.showInfoWindow(mapScene.fragmentId, mapScene.overlayItem, false);
        } catch (APIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static class MapScene {
    	public boolean infoWindow;
        public Position position;
        public int zoomLevel;
        public List<ItemizedOverlay> itemizedOverlayList;
        public List<Shape> shapeList;
        public OverlayItem overlayItem;
        public int fragmentId;
    }

    public boolean ensureThreadRunning() {
        return tilesView.ensureThreadsRunning();
    }

    public void clearTileTextures() {
    	tilesView.clearTileTextures();
    }

    public void pause() {
    	tilesView.pause();
    }

    public void resume() {
    	tilesView.resume();
    }

    public void zoomInAtPosition(Position position) {
    	tilesView.zoomInAtPosition(position);
    }

    public XYFloat mercXYToScreenXYConv(XYDouble mercXY, float zoomLevel) {
        return tilesView.mercXYToScreenXYConv(mercXY, zoomLevel);
    }
    
    public float getZRotation() {
        return tilesView.getMapMode().getzRotation();
    }
}
