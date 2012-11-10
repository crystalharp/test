package com.decarta.android.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Rect;

import com.decarta.Globals;
import com.decarta.android.event.EventListener;
import com.decarta.android.event.EventSource;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.XYFloat;
import com.decarta.android.util.XYInteger;

public class Compass implements EventSource{
	public enum PlaceLocation{TOP_LEFT,TOP_RIGHT,BOTTOM_LEFT,BOTTOM_RIGHT};
	public static PlaceLocation DEF_PLACE_LOCATION=PlaceLocation.TOP_LEFT;
	
	private PlaceLocation placeLocation=null;
	private boolean visible=true;
	private Icon icon;
	public int textureRef = 0;
	
	private Map<Integer, ArrayList<EventListener>> eventListeners = new HashMap<Integer, ArrayList<EventListener>>();
	
	public Compass(Icon icon){
		this(icon,DEF_PLACE_LOCATION);
	}
	
	/**
	 * Compass to be show on the map
	 * @param size width and height of the compass
	 * @param offset define screen position of the compass center, calculated relative to the placeLocation
	 * @param placeLocation define which corner of the screen should place the compass. The real screen position is also related to offset.
	 */
	public Compass(Icon icon, PlaceLocation placeLocation){
	    this.icon = icon;
		this.placeLocation=placeLocation;
	}
	
	public boolean snapTo(XYFloat screenXY, XYInteger displaySize, Rect padding){
		float buffer=10;
		
		XYInteger xy=getScreenXY(displaySize);
		
		if(xy.x-icon.getSize().x/2-buffer<screenXY.x && xy.x+icon.getSize().x/2+buffer>screenXY.x 
				&& xy.y-icon.getSize().y/2-buffer+(padding.top)<screenXY.y && xy.y+icon.getSize().y/2+buffer+(padding.top)>screenXY.y){
			return true;
		}
		return false;
	}
	
	public XYInteger getScreenXY(XYInteger displaySize){
	       float density=Globals.g_metrics.density;
	        float x=icon.getOffset().x*density;
	        float y=icon.getOffset().y;
	        if(placeLocation.equals(PlaceLocation.TOP_RIGHT)){
	            x=displaySize.x-icon.getOffset().x*density;
	        }else if(placeLocation.equals(PlaceLocation.BOTTOM_LEFT)){
	            y=displaySize.y-icon.getOffset().y;
	        }else if(placeLocation.equals(PlaceLocation.BOTTOM_RIGHT)){
	            x=displaySize.x-icon.getOffset().x*density;
	            y=displaySize.y-icon.getOffset().y;
		}
		return new XYInteger((int)x,(int)y);
	}

	public PlaceLocation getPlaceLocation() {
		return placeLocation;
	}

	public void setPlaceLocation(PlaceLocation placeLocation) {
		this.placeLocation = placeLocation;
	}

	public XYInteger getOffset() {
		return icon.getOffset();
	}
	
	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	@Override
	public void addEventListener(int eventType, EventListener listener) throws APIException{
		if(!isSupportedEventListener(eventType, listener)){
			throw new APIException("not valid event type/listener pair.");
		}
		if(!eventListeners.containsKey(eventType)){
			eventListeners.put(eventType, new ArrayList<EventListener>());
		}
		eventListeners.get(eventType).add(listener);
		
	}
	
	@Override
	public boolean isSupportedEventListener(int eventType, EventListener listener) {
		// TODO Auto-generated method stub
		if(
				eventType==com.decarta.android.event.EventType.TOUCH && (listener instanceof TouchEventListener)
				
		)
		return true;
		else return false;
	}
	
	@Override
	public void removeAllEventListeners(int eventType) {
		// TODO Auto-generated method stub
		if(eventListeners.containsKey(eventType)){
			eventListeners.get(eventType).clear();
		}
	}
	
	@Override
	public void removeEventListener(int eventType, EventListener listener) throws APIException {
		// TODO Auto-generated method stub
		if(!isSupportedEventListener(eventType, listener)){
			throw new APIException("not valid event type/listener pair.");
		}
		if(eventListeners.containsKey(eventType)){
			eventListeners.get(eventType).remove(listener);
		}
	}
	
	public void executeTouchListeners(){
		if(eventListeners.containsKey(com.decarta.android.event.EventType.TOUCH)){
			ArrayList<EventListener> listeners=eventListeners.get(com.decarta.android.event.EventType.TOUCH);
			for(int i=0;i<listeners.size();i++){
				((TouchEventListener)(listeners.get(i))).onTouchEvent(this);
			}
		}
	}
	
	
	/**
	 *listener for touch event
	 */
	public interface TouchEventListener extends EventListener{
		public void onTouchEvent(EventSource eventSource);
	}

	public Icon getIcon() {
	    return icon;
	}
}
