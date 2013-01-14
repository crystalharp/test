package com.decarta.android.map;

import static android.opengl.GLES10.GL_FLOAT;
import static android.opengl.GLES10.GL_TRIANGLE_FAN;

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
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.opengl.GLES10;
import android.opengl.GLUtils;

import com.decarta.Globals;
import com.decarta.android.event.EventListener;
import com.decarta.android.event.EventSource;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYFloat;
import com.decarta.android.util.XYInteger;

public class Compass implements EventSource{
	public enum PlaceLocation{TOP_LEFT,TOP_RIGHT,BOTTOM_LEFT,BOTTOM_RIGHT,CENTER};
	public static XYInteger DEF_OFFSET=new XYInteger(25,25);
	public static XYInteger DEF_SIZE=new XYInteger(10,40);
	public static PlaceLocation DEF_PLACE_LOCATION=PlaceLocation.TOP_LEFT;
	
	private FloatBuffer vertexN;
	private FloatBuffer vertexS;
	
	private int colorN=Color.RED;
	private int colorS=Color.BLUE;
	
	private XYInteger size=new XYInteger(0,0);
	private XYInteger offset=new XYInteger(0,0);
	private PlaceLocation placeLocation=null;
	private boolean visible=true;
	
	private Icon icon;
	
	private int textureRef=0;
	private static ByteBuffer TEXTURE_COORDS;
	private static FloatBuffer mVertexBuffer;
	
	static {
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
	
	private Map<Integer, ArrayList<EventListener>> eventListeners = new HashMap<Integer, ArrayList<EventListener>>();
	
	private Compass(){
		this(DEF_SIZE,DEF_OFFSET,DEF_PLACE_LOCATION);
	}
	
	private Compass(XYInteger size, XYInteger offset, PlaceLocation placeLocation){
		this(size, offset, placeLocation, null);
	}
	
	/**
	 * Compass to be show on the map
	 * @param size width and height of the compass
	 * @param offset define screen position of the compass center, calculated relative to the placeLocation
	 * @param placeLocation define which corner of the screen should place the compass. The real screen position is also related to offset.
	 */
	public Compass(XYInteger size, XYInteger offset, PlaceLocation placeLocation, Icon icon){
		this.size.x=size.x;
		this.size.y=size.y;
		this.offset.x=offset.x;
		this.offset.y=offset.y;
		this.placeLocation=placeLocation;
		this.icon = icon;
		
		setVertexs();
	}
	
	private void setVertexs(){
		ByteBuffer vbb1=ByteBuffer.allocateDirect(3*2*4);
		vbb1.order(ByteOrder.nativeOrder());
		vertexN=vbb1.asFloatBuffer();
		vertexN.put(-size.x/2);
		vertexN.put(0);
		vertexN.put(size.x/2);
		vertexN.put(0);
		vertexN.put(0);
		vertexN.put(-size.y/2);
		
		
		ByteBuffer vbb2=ByteBuffer.allocateDirect(3*2*4);
		vbb2.order(ByteOrder.nativeOrder());
		vertexS=vbb2.asFloatBuffer();
		vertexS.put(-size.x/2);
		vertexS.put(0);
		vertexS.put(0);
		vertexS.put(size.y/2);
		vertexS.put(size.x/2);
		vertexS.put(0);
	}
	
	public void renderGL(GL10 gl){
		float density=Globals.g_metrics.density;
		gl.glScalef(density, density, 1);
		gl.glVertexPointer(2, GL_FLOAT, 0, vertexN);
		vertexN.position(0);
		gl.glColor4f(((colorN & 0x00ff0000)>>16)/(float)255, ((colorN & 0x0000ff00)>>8)/(float)255, ((colorN & 0x000000ff))/(float)255, 1);
		gl.glDrawArrays(GL_TRIANGLE_FAN, 0, 3);
		
		gl.glVertexPointer(2, GL_FLOAT, 0, vertexS);
		vertexS.position(0);
		gl.glColor4f(((colorS & 0x00ff0000)>>16)/(float)255, ((colorS & 0x0000ff00)>>8)/(float)255, ((colorS & 0x000000ff))/(float)255, 1);
		gl.glDrawArrays(GL_TRIANGLE_FAN, 0, 3);
	}
	
	public void renderCanvas(Canvas canvas){
		float density=Globals.g_metrics.density;
		canvas.scale(density, density);
		Paint innerN=new Paint();
		innerN.setStyle(Style.FILL_AND_STROKE);
		innerN.setStrokeWidth(1);
		innerN.setColor(colorN);
		innerN.setAntiAlias(true);
		Paint innerS=new Paint();
		innerS.setStyle(Style.FILL_AND_STROKE);
		innerS.setStrokeWidth(1);
		innerS.setColor(colorS);
		innerS.setAntiAlias(true);
		
		Path pathN=new Path();
		pathN.moveTo(-size.x/2, 0);
		pathN.lineTo(0, -size.y/2);
		pathN.lineTo(size.x/2, 0);
		pathN.close();
		canvas.drawPath(pathN, innerN);
		
		Path pathS=new Path();
		pathS.moveTo(-size.x/2, 0);
		pathS.lineTo(0, size.y/2);
		pathS.lineTo(size.x/2, 0);
		pathS.close();
		canvas.drawPath(pathS, innerS);
	}
	
	public boolean snapTo(XYFloat screenXY, XYInteger displaySize, Rect padding){
		float density=Globals.g_metrics.density;
		float buffer=10*density;
		
		XYInteger xy=getScreenXY(displaySize);
		
		if(xy.x-size.x/2*density-buffer<screenXY.x && xy.x+size.x/2*density+buffer>screenXY.x 
				&& xy.y-size.y/2*density-buffer+padding.top<screenXY.y && xy.y+size.y/2*density+buffer+padding.top>screenXY.y){
			return true;
		}
		return false;
	}
	
	public XYInteger getScreenXY(XYInteger displaySize){
		float density=Globals.g_metrics.density;
		float x=offset.x*density;
		float y=offset.y*density;
		if(placeLocation.equals(PlaceLocation.TOP_RIGHT)){
			x=displaySize.x-offset.x*density;
		}else if(placeLocation.equals(PlaceLocation.BOTTOM_LEFT)){
			y=displaySize.y-offset.y*density;
		}else if(placeLocation.equals(PlaceLocation.BOTTOM_RIGHT)){
			x=displaySize.x-offset.x*density;
			y=displaySize.y-offset.y*density;
		} else if (placeLocation.equals(PlaceLocation.CENTER)) {
		    x=displaySize.x/2;
            y=displaySize.y/2;
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
		return offset;
	}

	/**
	 * offset is the relative x,y from the PlaceLocation diagonally.
	 * @param offset
	 */
	public void setOffset(XYInteger offset) {
		this.offset = offset;
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

	public void drawCompassOpenGL(GL10 gl){
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl.glTexCoordPointer(2,GL10.GL_BYTE,0,TEXTURE_COORDS);
    	
    	gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        
		int bmSizeX=Util.getPower2(size.x);
        int bmSizeY=Util.getPower2(size.y);
        if(textureRef==0){
            IntBuffer bf=IntBuffer.allocate(1);
            gl.glGenTextures(1, bf);
            textureRef=bf.get(0);
            gl.glBindTexture(GLES10.GL_TEXTURE_2D, textureRef);
            try{
                Bitmap.Config config=Config.ARGB_8888;
                Bitmap bm=Bitmap.createBitmap(bmSizeX,bmSizeY,config);
                Canvas canvas=new Canvas(bm);
                bm.eraseColor(0);
                
                canvas.drawBitmap(icon.getImage(), null, new RectF(0, 0,size.x,size.y), null);
                
                gl.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
                gl.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
                gl.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE);
                gl.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE);
                GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, bm, 0);
            }catch(Exception e){
                clearTextureRef(gl);
            }
        }
        gl.glBindTexture(GLES10.GL_TEXTURE_2D, textureRef);
        
        XYInteger xy = new XYInteger(0, 0);
        xy.x-=offset.x;
        xy.y-=offset.y;
        mVertexBuffer.clear();
        mVertexBuffer.put(xy.x);
        mVertexBuffer.put(xy.y);
        mVertexBuffer.put(xy.x);
        mVertexBuffer.put(xy.y+bmSizeY);
        mVertexBuffer.put(xy.x+bmSizeX);
        mVertexBuffer.put(xy.y);
        mVertexBuffer.put(xy.x+bmSizeX);
        mVertexBuffer.put(xy.y+bmSizeY);
        mVertexBuffer.position(0);
        TEXTURE_COORDS.position(0);
        gl.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, 4);
        gl.glDisable(GL10.GL_BLEND);
	}

	public void clearTextureRef(GL10 gl){
		if(textureRef!=0){
			IntBuffer textureRefBuf=IntBuffer.allocate(1);
			textureRefBuf.clear();
			textureRefBuf.put(0,textureRef);
			textureRefBuf.position(0);
			gl.glDeleteTextures(1, textureRefBuf);
			
			LogWrapper.i("Compass","remove texture:"+textureRef);
			textureRef=0;
		}
	}
}
