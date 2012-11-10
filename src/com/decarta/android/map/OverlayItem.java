/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.decarta.android.event.EventListener;
import com.decarta.android.event.EventSource;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.map.InfoWindow.TextAlign;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYDouble;
import com.tigerknows.model.POI;

/**
 * A Pin is an abstract term for a dynamically created point, handled by the 
 * client, and placed on a Map. Pins must be located at a specific Position 
 * (latitude and longitude), are represented by an Icon, and can by default 
 * display information in a the InfoWindow associated with a Map.
 */
public class OverlayItem implements com.decarta.android.event.EventSource{
	/*
	 * 在该Item位置要绘制的图标
	 */
	protected Icon icon;
	/*
	 * 此Item所在墨卡托坐标点
	 */
	protected XYDouble mercXY= null;
	/*
	 * 点击此Item时显示的文本
	 */
	protected String message;
	/*
	 * 是否可见
	 */
	protected boolean visible=true;
	/*
	 * 事件池
	 */
	protected Map<Integer,ArrayList<EventListener>> eventListeners = new HashMap<Integer,ArrayList<EventListener>>();
	/*
	 * 此Item所在Overlay
	 */
	protected ItemizedOverlay ownerOverlay=null;
	/*
	 * X, Z轴的旋转
	 */
	private RotationTilt rotationTilt=null;//new RotationTilt(RotateReference.MAP,TiltReference.MAP);
	/*
	 * 此Item所在经纬度
	 */
	private Position position;
	// position name
	private String name;
	/*
	 * 此Item是否被选中
	 */
	public boolean isFoucsed = false;
	/*
	 * 相关联的对象???
	 */
	private Object associatedObject=null;
	
	private int preferZoomLevel = -1;
	
	private int infoWindowType = -1;
    
    private TextAlign textAlign=TextAlign.CENTER;
	
	private OverlayItem(Position position, Icon icon, String message) throws APIException{
		this.setPosition(position);
		this.icon=icon;
		this.message=message;
	}
	
	public OverlayItem(Position position, Icon icon, String message,RotationTilt rotationTilt)throws APIException{
		this(position,icon,message);
		
		this.rotationTilt=rotationTilt;
	}
	
	public ItemizedOverlay getOwnerOverlay() {
		return ownerOverlay;
	}

	public void setOwnerOverlay(ItemizedOverlay ownerOverlay) {
		this.ownerOverlay = ownerOverlay;
	}
	
	/**
	 * text message associated with this pin
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * text message associated with this pin
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	
	/**
	 * get associatedObject
	 * @return
	 */
	public Object getAssociatedObject() {
		return associatedObject;
	}
	
	/**
	 * set associatedObject
	 * @param associatedObject
	 */
	public void setAssociatedObject(Object associatedObject) {
		this.associatedObject = associatedObject;
	}

	public XYDouble getMercXY() {
		return mercXY;
		
	}
	
	/**
	 * get Icon associated with this Pin
	 * 
	 */
	public Icon getIcon() {
		return icon;
	}
	/**
	 * set Icon associated with this Pin
	 */
	public void setIcon(Icon icon) {
		this.icon = icon;
		
	}
	/**
	 * Position of pin
	 */
	public Position getPosition() {
		return position;
	}
	
	/**
	 * Position of pin
	 */
	public void setPosition(Position position) throws APIException {
		try{
			if(position==null){
				if(this.position!=null){
					this.position=null;
					mercXY=null;
					if(ownerOverlay!=null) ownerOverlay.resetPinIdxs();
				}
			}
			else if(!position.equals(this.position)){
				mercXY=Util.posToMercPix(position, ItemizedOverlay.ZOOM_LEVEL);
				this.position=position;
				if(ownerOverlay!=null) ownerOverlay.resetPinIdxs();
			}
		}catch(APIException e){
			mercXY=null;
			this.position=null;
			throw e;
		}
	}
	
	public int getPreferZoomLevel() {
		return preferZoomLevel;
	}

	public void setPreferZoomLevel(int preferZoomLevel) {
		// TODO: 判断有效的ZOOMLEVEL
		if (preferZoomLevel < 0) {
			return;
		}
		this.preferZoomLevel = preferZoomLevel;
	}

	public int getInfoWindowType() {
		return infoWindowType;
	}

	public void setInfoWindowType(int infoWindowType) {
		this.infoWindowType = infoWindowType;
	}

	public boolean hasPreferZoomLevel() {
		return preferZoomLevel != -1;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if(!(obj instanceof OverlayItem)){
			return false;
		}
		OverlayItem other = (OverlayItem) obj;
		if (this.position != other.position && (this.position == null || !this.position.equals(other.position))) {
			return false;
		}
		if (this.icon != other.icon && (this.icon == null || !this.icon.equals(other.icon))) {
			return false;
		}
		if (this.message != other.message && (this.message == null || !this.message.equals(other.message))) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 89 * hash + (this.position != null ? this.position.hashCode() : 0);
		hash = 89 * hash + (this.icon != null ? this.icon.hashCode() : 0);
		return hash;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public RotationTilt getRotationTilt() {
		return rotationTilt;
	}

	//Event registration related functions
	
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
	
	public void setName(String name) {
		this.name = name;
	    LogWrapper.d("OverlayItem", "InfoWindow.setName name:" + name);
	}
	
	public POI getPOI() {
	    POI poi = new POI();
	    poi.setPosition(position);
	    LogWrapper.d("OverlayItem", "InfoWindow.getPOI name:" + name);
	    poi.setName(name);
	    return poi;
	}
    
    public TextAlign getTextAlign() {
        return textAlign;
    }

    public void setTextAlign(TextAlign textAlign) {
        this.textAlign = textAlign;
    }
}
