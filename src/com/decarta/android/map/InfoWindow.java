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
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.text.TextUtils;

import com.decarta.android.event.EventListener;
import com.decarta.android.event.EventSource;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYDouble;
import com.decarta.android.util.XYFloat;
import com.decarta.android.util.XYInteger;

/**
 * A Pin that gets attached to a Map object has a default, simple message for 
 * displaying any information associated with the Pin. To customize the window
 * in which the information associated with a pin is displayed in, use the 
 * InfoWindow class.
 */
public class InfoWindow implements com.decarta.android.event.EventSource{
	public enum TextAlign{LEFT,CENTER};
	
	public static final int ZOOM_LEVEL=13;
	
	public static int MAX_CHARS_PER_LINE_SIMPLE=30;
    public static int MAX_CHARS_PER_LINE_EXPEND=30;
    
    public static int MAX_CHARS_PER_LINE(int type) {
        if (TYPE_SIMPLE == type) {
            return MAX_CHARS_PER_LINE_SIMPLE;
        } else {
            return MAX_CHARS_PER_LINE_EXPEND;
        }
    }
    
	public static int BACKGROUND_COLOR_CLICKED=1;
    public static int BACKGROUND_COLOR_CLICKED_LEFT=2;
    public static int BACKGROUND_COLOR_CLICKED_RIGHT=3;
	public static int BACKGROUND_COLOR_UNCLICKED=0;
	
	public static int INFO_TRIANGLE_HEIGHT=10;
	public static int INFO_TRIANGLE_WIDTH=16;
	public static int INFO_TEXTOFFSET_VERTICAL=6;
	public static int INFO_TEXTOFFSET_HORIZONTAL=6;
    public static int INFO_EXPENDOFFSET_VERTICAL=6;
    public static int INFO_EXPENDOFFSET_HORIZONTAL=8;
	public static int INFO_ROUND_RADIUS=0;
	public static int INFO_BORDER_SIZE=2;
	public static int INFO_BORDER_COLOR=Color.DKGRAY;
	
	public static boolean INFO_BORDER_ANTIALIAS=false;
	public static boolean INFO_TEXT_ANTIALIAS=true;
	public static int INFO_TEXT_COLOR=Color.BLACK;
	public static int INFO_TEXT_SIZE=15;
	
	public static final int TYPE_SIMPLE = 0;
	public static final int TYPE_EXPEND = 1;
	
	private OverlayItem associatedOverlayItem;
	private boolean visible=false;
	private XYDouble mercXY;
	private Position position;
	private String message;
	//placed at position.x-offset.x, position.y-offset.y
	private XYFloat offset=new XYFloat(0f,0f);
	private RotationTilt offsetRotationTilt=new RotationTilt();
	private int backgroundColor=Color.rgb(230, 230, 230);
	
	private Map<Integer,ArrayList<EventListener>> eventListeners = new HashMap<Integer,ArrayList<EventListener>>();
	
	private TextAlign textAlign=TextAlign.CENTER;
	
	public int textureRef=0;
	public boolean texImageChanged=false;
	public int type = TYPE_SIMPLE;

	public XYInteger canvasSize=new XYInteger(0,0);
    
    public NinePatchDrawable infoWindowExpendNormalBg;
    public NinePatchDrawable infoWindowExpendLeftFocusedBg;
    public NinePatchDrawable infoWindowExpendRightFocusedBg;
    public NinePatchDrawable infoWindowSimpleNormalBg;
    public NinePatchDrawable infoWindowSimpleFocusedBg;
    public Drawable infoWindowLeftNormalDrw;
    public Drawable infoWindowRightNormalDrw;
    public Rect infoWindowSimpleNormalRect;
    public Rect infoWindowExpendRect;
    public Rect infoWindowLeftNormalRect;
	
	public InfoWindow(){
		
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

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
	    if (TextUtils.isEmpty(message)) {
	        return;
	    }
	    if (message.indexOf("\n") < 0 && message.length() > MAX_CHARS_PER_LINE(type)) {
            StringBuilder s = new StringBuilder();
            int length = message.length();
            int end;
	        for(int i = 0; i*MAX_CHARS_PER_LINE(type) < length; i++) {
	            end = (i+1)*MAX_CHARS_PER_LINE(type);
	            if (end > length) {
	                end = length;
	            }
	            s.append(message.substring(i*MAX_CHARS_PER_LINE(type), end));
	            s.append("\n");
	        }
	        message = s.toString();
	    }
		this.message = message;
		backgroundColor = BACKGROUND_COLOR_UNCLICKED;
		texImageChanged=true;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) throws APIException{
		try{
			mercXY=Util.posToMercPix(position, ZOOM_LEVEL);
			this.position=position;
		}catch(APIException e){
			mercXY=null;
			this.position=null;
			throw e;
		}
		
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public int getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
		texImageChanged=true;
	}

	public OverlayItem getAssociatedOverlayItem() {
		return associatedOverlayItem;
	}

	public void setAssociatedOverlayItem(OverlayItem associatedOverlyaItem) {
		this.associatedOverlayItem = associatedOverlyaItem;
	}
	
	public RotationTilt getOffsetRotationTilt() {
		return offsetRotationTilt;
	}
	
	
	//event related methods
	
	public TextAlign getTextAlign() {
		return textAlign;
	}

	public void setTextAlign(TextAlign textAlign) {
		this.textAlign = textAlign;
		texImageChanged=true;
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
	
	public void executeTouchListeners(Position position, int type){
		if(eventListeners.containsKey(com.decarta.android.event.EventType.TOUCH)){
			ArrayList<EventListener> listeners=eventListeners.get(com.decarta.android.event.EventType.TOUCH);
			for(int i=0;i<listeners.size();i++){
				((TouchEventListener)(listeners.get(i))).onTouchEvent(this, type);
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
		public void onTouchEvent(EventSource eventSource, int type);
	}

	public static boolean validateClickType(int clickType) {
		if (clickType == BACKGROUND_COLOR_CLICKED ||
				clickType == BACKGROUND_COLOR_CLICKED_LEFT ||
				clickType == BACKGROUND_COLOR_CLICKED_RIGHT) {
			return true;
		}
		return false;
	}
}
