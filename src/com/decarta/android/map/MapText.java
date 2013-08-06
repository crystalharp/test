/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;

import com.decarta.android.event.EventListener;
import com.decarta.android.event.EventSource;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.XYDouble;
import com.decarta.android.util.XYFloat;
import com.decarta.android.util.XYInteger;
import com.tigerknows.map.MapWord;

/**
 * A Pin that gets attached to a Map object has a default, simple message for 
 * displaying any information associated with the Pin. To customize the window
 * in which the information associated with a pin is displayed in, use the 
 * InfoWindow class.
 */
public class MapText implements com.decarta.android.event.EventSource{
	public enum TextAlign{LEFT,CENTER};
	
	private boolean visible=true;
	private XYDouble mercXY = new XYDouble(0, 0);
	//placed at position.x-offset.x, position.y-offset.y
	private XYFloat offset=new XYFloat(0f,0f);
	private RotationTilt offsetRotationTilt=new RotationTilt();
    private int zoomLevel = 0;
	
	private Map<Integer,ArrayList<EventListener>> eventListeners = new HashMap<Integer,ArrayList<EventListener>>();
	
	public int textureRef=0;
    public boolean texImageChanged=false;
	public boolean screenTextGetting=false;
    public XYDouble mercXYGetting = new XYDouble(0, 0);
    public int zoomLevelGetting=0;
    public long lastTime = 0;

	private MapWord[] mapWords = null;
    public XYInteger canvasSize=new XYInteger(0,0);
    public Bitmap bitmap = null;
    public boolean refresh = false;
	
	public MapText(){
		
	}
	
	public void setMapWords(MapWord[] mapWords) {
	    if (mapWords != null) {
            int offsetX = canvasSize.x/2;
            int offsetY = canvasSize.y/2;
            for(MapWord mapWord : mapWords) {
                mapWord.mercXY = new XYDouble(mercXY.x+mapWord.getX()-offsetX, mercXY.y-mapWord.getY()+offsetY);
                if (mapWord.icon.index >= 0) {
                    mapWord.icon.mercXY = new XYDouble(mercXY.x+mapWord.icon.x-offsetX, mercXY.y-mapWord.icon.y+offsetY); 
                }
            }
        }
	    this.mapWords = mapWords;
	}
	
	public MapWord[] getMapWords() {
	    return mapWords;
	}
    
    public void setZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
    }
    
    public int getZoomLevel() {
        return zoomLevel;
    }
	
	public XYFloat getOffset() {
		return offset;
	}
	
	/**
	 * @param offset info window's center bottom point is position.x-offset.x, position.y-offset.y
	 * @param offsetRotationTilt rotation and tilt of the offset line. When draw the info window, we need to calculate start
	 * from the info window position, go through the offset, then we get the coordinate of the center bottom
	 * point as the info window arrow pointed at. The offset is normally caused by the pin between the infowindow and the
	 * pin's position.
	 */
	public void setOffset(XYFloat offset, RotationTilt offsetRotationTilt) {
		this.offset = offset;
		this.offsetRotationTilt=offsetRotationTilt;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public RotationTilt getOffsetRotationTilt() {
		return offsetRotationTilt;
	}
	
	
	//event related methods

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
				//|| eventType==EventType.TOUCHDOWN && (listener instanceof TouchDownEventListener)
				//|| eventType==EventType.TOUCHUP && (listener instanceof TouchUpEventListener)
				
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
	
	public void executeTouchListeners(Position position){
		if(eventListeners.containsKey(com.decarta.android.event.EventType.TOUCH)){
			ArrayList<EventListener> listeners=eventListeners.get(com.decarta.android.event.EventType.TOUCH);
			for(int i=0;i<listeners.size();i++){
				((TouchEventListener)(listeners.get(i))).onTouchEvent(this);
			}
		}
	}

	public XYDouble getMercXY() {
		return mercXY;
	}

	
	/**
	 *listener for touch event
	 */
	public interface TouchEventListener extends EventListener{
		public void onTouchEvent(EventSource eventSource);
	}
}
