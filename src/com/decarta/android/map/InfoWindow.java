/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.map;

import static android.opengl.GLES10.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES10.GL_TEXTURE_2D;
import static android.opengl.GLES10.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES10.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES10.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES10.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES10.GL_TRIANGLE_STRIP;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.opengl.GLES10;
import android.opengl.GLUtils;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;

import com.decarta.Globals;
import com.decarta.android.event.EventListener;
import com.decarta.android.event.EventSource;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYDouble;
import com.decarta.android.util.XYFloat;
import com.tigerknows.R;
import com.tigerknows.android.location.Position;

/**
 * A Pin that gets attached to a Map object has a default, simple message for 
 * displaying any information associated with the Pin. To customize the window
 * in which the information associated with a pin is displayed in, use the 
 * InfoWindow class.
 */
public class InfoWindow implements com.decarta.android.event.EventSource{
	public enum TextAlign{LEFT,CENTER};
	public enum InfoWindowType{TEXT,VIEWGROUP};
	
	public static final int ZOOM_LEVEL=13;
	
	public static int MAX_CHARS_PER_LINE=30;
	public static int BACKGROUND_COLOR_CLICKED=Color.rgb(245,245,220);
	public static int BACKGROUND_COLOR_UNCLICKED=Color.rgb(255, 255, 255);
	
	public static int INFO_TRIANGLE_HEIGHT=15;
	public static int INFO_TRIANGLE_WIDTH=16;
	public static int INFO_TEXTOFFSET_VERTICAL=5;
	public static int INFO_TEXTOFFSET_HORIZONTAL=5;
	public static int INFO_ROUND_RADIUS=0;
	public static int INFO_BORDER_SIZE=2;
	public static int INFO_BORDER_COLOR=0xc8c8c8;
	
	public static boolean INFO_BORDER_ANTIALIAS=false;
	public static boolean INFO_TEXT_ANTIALIAS=true;
	public static int INFO_TEXT_COLOR=Color.BLACK;
	public static int INFO_TEXT_SIZE=15;
	
	private static Paint infoWindowInnerP;
	private static Paint infoWindowBorderP;
	private static ByteBuffer TEXTURE_COORDS;
	private static FloatBuffer mVertexBuffer;
	
	static{
		infoWindowBorderP=new Paint();
		infoWindowBorderP.setColor(InfoWindow.INFO_BORDER_COLOR);
		infoWindowBorderP.setStyle(Style.STROKE);
    	infoWindowBorderP.setStrokeWidth(InfoWindow.INFO_BORDER_SIZE);
    	infoWindowBorderP.setAntiAlias(InfoWindow.INFO_BORDER_ANTIALIAS);
    	
    	ByteBuffer tbb = ByteBuffer.allocateDirect(1 * 2 * 4);
		tbb.order(ByteOrder.nativeOrder());
		TEXTURE_COORDS = tbb;
		TEXTURE_COORDS.put((byte)0);
		TEXTURE_COORDS.put((byte)0);
		TEXTURE_COORDS.put((byte)0);
		TEXTURE_COORDS.put((byte)1);
		TEXTURE_COORDS.put((byte)1);
		TEXTURE_COORDS.put((byte)0);
		TEXTURE_COORDS.put((byte)1);
		TEXTURE_COORDS.put((byte)1);
		
		ByteBuffer vbb = ByteBuffer.allocateDirect(4 * 2 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer=vbb.asFloatBuffer();
	}
	
	
	
	/******************************* instance variable ****************************************************/
	private OverlayItem associatedOverlayItem;
	private boolean visible=false;
	private XYDouble mercXY;
	private Position position;
	private String message;
	private ViewGroup viewGroup;
	private InfoWindowType type=InfoWindowType.TEXT;
	//placed at position.x-offset.x, position.y-offset.y
	private XYFloat offset=new XYFloat(0f,0f);
	private RotationTilt offsetRotationTilt=new RotationTilt();
	private int backgroundColor=Color.rgb(230, 230, 230);
	
	private int textureRef=0;
	private boolean changed=false;
	private boolean isRectGot = false;
	private RectF rect=new RectF();
	
	private Map<Integer,ArrayList<EventListener>> eventListeners = new HashMap<Integer,ArrayList<EventListener>>();
	private TextAlign textAlign=TextAlign.CENTER;
	
	
	
	/**************************************** public methods*******************************************************/
	public InfoWindow(){
	    measureMaxCharsPerLine();
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
		if(message==null){
			LogWrapper.e("InfoWindow", "setMessage null");
		}
		if (message.contains("\n")) {
		    this.message = message;
		} else {
    		this.message = null;
    		for(int start=0, length = message.length(); start < length;) {
    		    int remainder = (length-start);
    		    int l = remainder > MAX_CHARS_PER_LINE ? MAX_CHARS_PER_LINE : remainder;
    		    if (this.message == null) {
    		        this.message = message.substring(start, start+l);
    		    } else {
    		        this.message += "\n" + message.substring(start, start+l);
    		    }
    		    start += l;
    		}
		}
		changed=true;
		type=InfoWindowType.TEXT;
		setBackgroundColor(InfoWindow.BACKGROUND_COLOR_UNCLICKED);
	}
	
	public ViewGroup getViewGroup() {
		return viewGroup;
	}

	public void setViewGroup(ViewGroup viewGroup) {
		if(viewGroup==null){
			LogWrapper.w("InfoWindow", "setViewGroup null");
		}
		this.viewGroup = viewGroup;
		changed=true;
		type=InfoWindowType.VIEWGROUP;
        setBackgroundColor(InfoWindow.BACKGROUND_COLOR_UNCLICKED);
	}

	public InfoWindowType getType() {
		return type;
	}
	
//	public void clearTextureRef(GL10 gl){
//		if(textureRef!=0){
//			IntBuffer textureRefBuf=IntBuffer.allocate(1);
//			textureRefBuf.clear();
//			textureRefBuf.put(0,textureRef);
//			textureRefBuf.position(0);
//			gl.glDeleteTextures(1, textureRefBuf);
//			
//			LogWrapper.i("InfoWindow","remove texture:"+textureRef);
//			textureRef=0;
//			changed=true;
//		}
//	}
	public void clearTextureRef(){
		if(textureRef!=0){
			IntBuffer textureRefBuf=IntBuffer.allocate(1);
			textureRefBuf.clear();
			textureRefBuf.put(0,textureRef);
			textureRefBuf.position(0);
			GLES10.glDeleteTextures(1, textureRefBuf);
			
			LogWrapper.i("InfoWindow","remove texture:"+textureRef);
			textureRef=0;
			changed=true;
		}
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
		changed=true;
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
	
	public RectF getRect(){
		return rect;
	}
	
	public TextAlign getTextAlign() {
		return textAlign;
	}

	public void setTextAlign(TextAlign textAlign) {
		this.textAlign = textAlign;
		changed=true;
	}

	public XYDouble getMercXY() {
		return mercXY;
	}

	
	/************************************************ event related methods *********************************************/
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
	
	public void executeTouchListeners(MotionEvent ev){
		if(type.equals(InfoWindowType.TEXT) && eventListeners.containsKey(com.decarta.android.event.EventType.TOUCH)){
			ArrayList<EventListener> listeners=eventListeners.get(com.decarta.android.event.EventType.TOUCH);
			for(int i=0;i<listeners.size();i++){
				((TouchEventListener)(listeners.get(i))).onTouchEvent(this);
			}
		}
		else if(type.equals(InfoWindowType.VIEWGROUP)){
			viewGroup.dispatchTouchEvent(ev);
		}
	}
	
	public void dispatchTouchEvent(MotionEvent ev){
		if(type.equals(InfoWindowType.VIEWGROUP)){
			viewGroup.dispatchTouchEvent(ev);
		}
	}
	
	/**
	 *listener for touch event
	 */
	public interface TouchEventListener extends EventListener{
		public void onTouchEvent(EventSource eventSource);
	}
	
	
	
	/************************ internal used only in API ***************************/
	/**
     * calculate the outer containing rectangular of the info window from the screen coordinate
     * @param infoWindow
     * @param screenXY the screen coordinate of the anchor point of the info window
     */
    public RectF getInfoWindowRecF(){
        if (mercXY == null) {
            return new RectF();
        }
        synchronized (mercXY) {
    	XYFloat screenXY=new XYFloat(0,0);
    	
    	if(type.equals(InfoWindow.InfoWindowType.VIEWGROUP)){
    		if(viewGroup==null){
    			return new RectF();
    		}
    		
    		ViewGroup ivg=viewGroup;
            if (backgroundColor == BACKGROUND_COLOR_CLICKED) {
                ivg.setBackgroundResource(R.drawable.btn_bubble_focused);
            } else {
                ivg.setBackgroundResource(R.drawable.btn_bubble_normal);
            }
    		ivg.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
    		int infoWindowWidth=ivg.getMeasuredWidth() + (InfoWindow.INFO_BORDER_SIZE << 1);
    		int infoWindowHeight=ivg.getMeasuredHeight() + (InfoWindow.INFO_BORDER_SIZE << 1);
    		
    		RectF infoWindowRect = new RectF(screenXY.x - (infoWindowWidth>>1),
    				screenXY.y - infoWindowHeight, screenXY.x + (infoWindowWidth>>1), screenXY.y);
//                    screenXY.y-infoWindowHeight-InfoWindow.INFO_TRIANGLE_HEIGHT,screenXY.x+infoWindowWidth/2,screenXY.y-InfoWindow.INFO_TRIANGLE_HEIGHT);
    		return infoWindowRect;
    	}
    	else{
    		if(message==null){
    			return new RectF();
    		}
    		
    		Paint paint=getTextPaint(textAlign);
    		float textOffsetX=InfoWindow.INFO_TEXTOFFSET_HORIZONTAL*Globals.g_metrics.density;
    		float textOffsetY=InfoWindow.INFO_TEXTOFFSET_VERTICAL*Globals.g_metrics.density;
    		String msg=message;
    		String[] msgs=msg.trim().split("\n");
    		float maxLength=0;
    		String msg_line="";
    		for(int i=0;i<msgs.length;i++){
    			msg_line=msgs[i];
    			if(msg_line.length()>InfoWindow.MAX_CHARS_PER_LINE) msg_line=msg_line.substring(0,InfoWindow.MAX_CHARS_PER_LINE-3)+"...";
    			float length=paint.measureText(msg_line);
    			if(length>maxLength) maxLength=length;
    		}
    		float infoWindowWidth=maxLength+2*textOffsetX;
    		float infoWindowHeight=(-paint.ascent()+paint.descent())*msgs.length+2*textOffsetY;
    		
    		RectF infoWindowRect=new RectF(screenXY.x-infoWindowWidth/2,
    				screenXY.y-infoWindowHeight-InfoWindow.INFO_TRIANGLE_HEIGHT,screenXY.x+infoWindowWidth/2,screenXY.y-InfoWindow.INFO_TRIANGLE_HEIGHT);
    		return infoWindowRect;
    	}
        
        }
    }
	
    /**
	 * instantiate once and use forever
	 * @return Paint for info window text drawing
	 */
	private Paint getTextPaint(InfoWindow.TextAlign textAlign){
		Paint textP=new Paint();
		textP.setColor(InfoWindow.INFO_TEXT_COLOR);
    	if(textAlign==InfoWindow.TextAlign.LEFT){
    		textP.setTextAlign(Align.LEFT);
    	}else{
    		textP.setTextAlign(Align.CENTER);
	    }
		textP.setTextSize(InfoWindow.INFO_TEXT_SIZE*Globals.g_metrics.density);
		textP.setAntiAlias(InfoWindow.INFO_TEXT_ANTIALIAS);
			
    	return textP;
	}
	
	/**
	 * draw info window to canvas. This method is also used in opengl drawing, because otherwise
	 * we need to create a font bitmap buffer to write any text, which should be very painful.
	 * @param canvas
	 * @param infoWindow
	 * @param screenXY
	 */
    public void drawInfoWindow(Canvas canvas, XYFloat screenXY){
    	if(changed && !isRectGot){
    		rect = getInfoWindowRecF();
    	}
    	RectF infoWindowRect = new RectF(rect);
    	infoWindowRect.offset(screenXY.x, screenXY.y);
    	
    	float roundRadius=InfoWindow.INFO_ROUND_RADIUS*Globals.g_metrics.density;
		
		if(type.equals(InfoWindow.InfoWindowType.VIEWGROUP) && viewGroup!=null){
			Bitmap.Config config = Bitmap.Config.ARGB_8888;
			int width = (int)(infoWindowRect.width()-2*InfoWindow.INFO_BORDER_SIZE);
			int height = (int)(infoWindowRect.height()-2*InfoWindow.INFO_BORDER_SIZE);
//			LogWrapper.i("infoWindowBug", "bgBitmap width: " + width + ", height: " + height);
			Bitmap vgBitmap=Bitmap.createBitmap(width, height, config);
			Canvas infoImageCanvas= new Canvas(vgBitmap);
			
			ViewGroup ivg=viewGroup;
			if(changed){
			    if (backgroundColor == BACKGROUND_COLOR_CLICKED) {
			        ivg.setBackgroundResource(R.drawable.btn_bubble_focused);
			    } else {
			        ivg.setBackgroundResource(R.drawable.btn_bubble_normal);
			    }
				ivg.layout(0, 0, width, height);
			}
			ivg.draw(infoImageCanvas);
			Paint paint=new Paint();
			paint.setAntiAlias(true);
//			LogWrapper.i("infoWindowBug", "drawBitmap left: " + (infoWindowRect.left+InfoWindow.INFO_BORDER_SIZE) + 
//					", top: " + (infoWindowRect.top+InfoWindow.INFO_BORDER_SIZE));
			canvas.drawBitmap(vgBitmap,infoWindowRect.left+InfoWindow.INFO_BORDER_SIZE,infoWindowRect.top+InfoWindow.INFO_BORDER_SIZE,paint);
		}
		else if(type.equals(InfoWindow.InfoWindowType.TEXT) && message!=null){
		    canvas.drawRoundRect(infoWindowRect, roundRadius, roundRadius, getInfoWindowInnerPaint());
		    canvas.drawRoundRect(infoWindowRect, roundRadius, roundRadius, infoWindowBorderP);
			float textOffsetY=InfoWindow.INFO_TEXTOFFSET_VERTICAL*Globals.g_metrics.density;
	    	float textOffsetX=InfoWindow.INFO_TEXTOFFSET_HORIZONTAL*Globals.g_metrics.density;
	    	
			Paint paint=getTextPaint(textAlign);
			float textSizeY=-paint.ascent()+paint.descent();
			String[] msgs=message.trim().split("\n");
			for(int i=0;i<msgs.length;i++){
				String msg_line=msgs[i];
				if(msg_line.length()>InfoWindow.MAX_CHARS_PER_LINE){
					msg_line=msg_line.substring(0,InfoWindow.MAX_CHARS_PER_LINE-3)+"...";
				}
				if(textAlign==InfoWindow.TextAlign.LEFT){
					canvas.drawText(msg_line,infoWindowRect.left+textOffsetX,infoWindowRect.top+textOffsetY+(i+0.8f)*(textSizeY),paint);
					
				}else if(textAlign==InfoWindow.TextAlign.CENTER){
					canvas.drawText(msg_line,(infoWindowRect.left+infoWindowRect.right)/2,infoWindowRect.top+textOffsetY+(i+0.8f)*(textSizeY),paint);
				}
			}
			int x = (int)infoWindowRect.left+(int)((infoWindowRect.right-infoWindowRect.left)/2);
			int y = (int)infoWindowRect.bottom-1;
			
			// Points of the triangle pointer on info window
			
			Point triangleTL = new Point(x-InfoWindow.INFO_TRIANGLE_WIDTH/2,y);        
			Point triangleTR = new Point(x+InfoWindow.INFO_TRIANGLE_WIDTH/2,y);
			Point triangleBC = new Point(x,y+InfoWindow.INFO_TRIANGLE_HEIGHT+1);
			
			// triangle background
			Path triangleBGPath = new Path();
			triangleBGPath.setFillType(Path.FillType.EVEN_ODD);
			triangleBGPath.moveTo(triangleTL.x,triangleTL.y);
			triangleBGPath.lineTo(triangleTR.x,triangleTR.y);
			triangleBGPath.lineTo(triangleBC.x,triangleBC.y);
			triangleBGPath.lineTo(triangleTL.x,triangleTL.y);
			triangleBGPath.close();
			canvas.drawPath(triangleBGPath, getInfoWindowInnerPaint());
			
			// triangle border
			Path triangleBorderPath = new Path();
			triangleBorderPath.setFillType(Path.FillType.EVEN_ODD);	    
			triangleBorderPath.moveTo(triangleTL.x,triangleTL.y+1);	
			triangleBorderPath.lineTo(triangleBC.x,triangleBC.y);
			triangleBorderPath.lineTo(triangleTR.x,triangleTR.y+1);
			canvas.drawPath(triangleBorderPath, infoWindowBorderP);
		}
		
    	
	    changed=false;
    } 
    
    public void drawInfoWindowOpenGL(GL10 gl, XYFloat screenXY){
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl.glTexCoordPointer(2,GL10.GL_BYTE,0,TEXTURE_COORDS);
    	
    	gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        
        if(textureRef==0){
			//infoWindow.textureRef=Util.getNextTexRef();
			IntBuffer bf=IntBuffer.allocate(1);
			gl.glGenTextures(1, bf);
			textureRef=bf.get(0);
			//Log.d("TilesView","infoWindow.textureRef:"+infoWindow.textureRef);
		}
		gl.glBindTexture(GL_TEXTURE_2D, textureRef);
		
		if(changed){
			rect=getInfoWindowRecF();
			isRectGot = true;
    	}
//		LogWrapper.i("infoWindowBug", "rect width: " + rect.width() + ", height: " + rect.height());
		int infoWindowCanvasSizeX=Util.getPower2(rect.width());
		int infoWindowCanvasSizeY=Util.getPower2(rect.height()+InfoWindow.INFO_TRIANGLE_HEIGHT);
		float originX=infoWindowCanvasSizeX/2f;
		float originY=infoWindowCanvasSizeY;
		float x=screenXY.x-infoWindowCanvasSizeX/2f;
		float y=screenXY.y-infoWindowCanvasSizeY;
//		LogWrapper.i("infoWindowBug", "infoBitmap width: " + infoWindowCanvasSizeX + ", height: " + infoWindowCanvasSizeY + 
//				"originX: " + originX + ", originY: " + originY);
		if(changed){
			//Log.i("MapRender","infoWindow bind texture image start");
			Bitmap.Config config = Bitmap.Config.ARGB_8888;
			Bitmap infoBitmap=Bitmap.createBitmap(infoWindowCanvasSizeX, infoWindowCanvasSizeY, config);
			Canvas infoImageCanvas= new Canvas(infoBitmap);
    		
			infoBitmap.eraseColor(0);
			drawInfoWindow(infoImageCanvas, new XYFloat(originX,originY));
			
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			GLUtils.texImage2D(GL_TEXTURE_2D, 0, infoBitmap, 0);
			changed=false;
		}
	
		mVertexBuffer.clear();
		mVertexBuffer.put(x);
		mVertexBuffer.put(y);
		//mVertexBuffer.put(0);
		mVertexBuffer.put(x);
		mVertexBuffer.put(y+infoWindowCanvasSizeY);
		//mVertexBuffer.put(0);
		mVertexBuffer.put(x+infoWindowCanvasSizeX);
		mVertexBuffer.put(y);
		//mVertexBuffer.put(0);
		mVertexBuffer.put(x+infoWindowCanvasSizeX);
		mVertexBuffer.put(y+infoWindowCanvasSizeY);
		//mVertexBuffer.put(0);
		mVertexBuffer.position(0);
		
		TEXTURE_COORDS.position(0);
		
		gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
//		LogWrapper.i("infoWindowBug", "texture width: " + infoWindowCanvasSizeX + ", height: " + infoWindowCanvasSizeY);
		gl.glDisable(GL10.GL_BLEND);
		isRectGot = false;
	}
    
    /**
	 * instantiate once and use forever
	 * @return Paint for info window text drawing
	 */
	private Paint getInfoWindowInnerPaint(){
		if(infoWindowInnerP==null){
			infoWindowInnerP=new Paint();
		}
		infoWindowInnerP.setColor(backgroundColor);
    	return infoWindowInnerP;
	}
	
	private static void measureMaxCharsPerLine() {
	    StringBuilder s = new StringBuilder();
	    int charsNum = 0;
	    float length = 0;
	    Paint paint = new Paint();
	    paint.setTextSize(InfoWindow.INFO_TEXT_SIZE*Globals.g_metrics.density);
	    for(;;) {
	        charsNum++;
            s.append("a0");
            length = paint.measureText(s.toString());
            int widthPixels = Globals.g_metrics.widthPixels;
            if (length >= widthPixels - 2*(INFO_TEXTOFFSET_HORIZONTAL)) {
                break;
            }
        }
	    MAX_CHARS_PER_LINE = charsNum;
	}
}
