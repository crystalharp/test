package com.decarta.android.map;

import static android.opengl.GLES10.GL_FLOAT;
import static android.opengl.GLES10.GL_TRIANGLE_FAN;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import com.decarta.CONFIG;
import com.decarta.android.exception.APIException;
import com.decarta.android.scale.Length;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYDouble;
import com.tigerknows.android.location.Position;

/**
 * Circle to be shown on the map.
 * 
 *
 */
public class Circle extends Shape{
	private static int MAX_CIRCLE_VERTS=52;
	private static FloatBuffer CircleVertexBuffer;
	static{
		ByteBuffer vbb2=ByteBuffer.allocateDirect(MAX_CIRCLE_VERTS*2*4);
		vbb2.order(ByteOrder.nativeOrder());
		CircleVertexBuffer=vbb2.asFloatBuffer();
		CircleVertexBuffer.clear();
		float radient=(float)(Math.PI*2/(MAX_CIRCLE_VERTS-2));
		CircleVertexBuffer.put(0);
		CircleVertexBuffer.put(0);
		for(int i=0;i<MAX_CIRCLE_VERTS-2;i++){
			CircleVertexBuffer.put((float)Math.cos(i*radient));
			CircleVertexBuffer.put((float)-Math.sin(i*radient));
		}
		CircleVertexBuffer.put(1);
		CircleVertexBuffer.put(0);
	}
	
	private int fillColor=Color.BLUE;
    private float opacity= 0.05f;
    
    private Length radius=new Length(0,Length.UOM.M);
    private Position position=null;
    private XYDouble mercXY=null;
    
    /**
	 * 	Circle constructor. circleName should be unique among all shapes including circle and polyline.
	 */
    public Circle(Position position, Length radius, String circleName)throws APIException{
		super(circleName);
		setPosition(position);
		setRadius(radius);
		
		
	}
    
    public void renderGL(GL10 gl,XYDouble topLeftXYf, float zoomLevel){
		double shapeZoomScale=Math.pow(2,zoomLevel-Shape.ZOOM_LEVEL);
    	float x=(float)(mercXY.x*shapeZoomScale-topLeftXYf.x);
		float y=(float)(-mercXY.y*shapeZoomScale+topLeftXYf.y);
		
		float radiusN=(float)(radius.toMeters()/(Util.metersPerPixelAtZoom(CONFIG.TILE_SIZE, zoomLevel, position.getLat())));
		float red=((fillColor & 0x00ff0000)>>16)/(float)255;
        float green=((fillColor & 0x0000ff00)>>8)/(float)255;
        float blue=((fillColor & 0x000000ff))/(float)255;
		gl.glColor4f(red,green,blue,opacity);
		
		gl.glPushMatrix();
		gl.glTranslatef(x,y,0);
		gl.glScalef(radiusN, radiusN, 1);
    	gl.glVertexPointer(2, GL_FLOAT, 0, CircleVertexBuffer);
		CircleVertexBuffer.position(0);
		gl.glDrawArrays(GL_TRIANGLE_FAN, 0, MAX_CIRCLE_VERTS);
		gl.glPopMatrix();
	}
    
    public void renderCanvas(Canvas canvas,XYDouble topLeftXYf, float zoomLevel){
    	double shapeZoomScale=Math.pow(2,zoomLevel-Shape.ZOOM_LEVEL);
    	float x=(float)(mercXY.x*shapeZoomScale-topLeftXYf.x);
		float y=(float)(-mercXY.y*shapeZoomScale+topLeftXYf.y);
		
		float radiusN=(float)(radius.toMeters()/(Util.metersPerPixelAtZoom(CONFIG.TILE_SIZE, zoomLevel, position.getLat())));
		
		Paint accuracyP=new Paint();
		accuracyP.setColor(fillColor);
		accuracyP.setStyle(Style.FILL);
		accuracyP.setAlpha(Math.round(opacity*255));
		//accuracyP.setStrokeWidth(1);
		accuracyP.setAntiAlias(true);

		canvas.drawCircle(x, y, radiusN, accuracyP);
    }
	
    
	public XYDouble getMercXY() {
		return mercXY;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) throws APIException {
		try{
			mercXY=Util.posToMercPix(position, Shape.ZOOM_LEVEL);
			this.position=position;
		}catch(APIException e){
			mercXY=null;
			this.position=null;
			throw e;
		}
	}
	
	public int getFillColor() {
		return fillColor;
	}

	public void setFillColor(int fillColor) {
		this.fillColor = fillColor;
	}

	public float getOpacity() {
		return opacity;
	}

	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}

	public Length getRadius() {
		return radius;
	}

	public void setRadius(Length radius) {
		this.radius = radius;
	}
	
	
	
}
