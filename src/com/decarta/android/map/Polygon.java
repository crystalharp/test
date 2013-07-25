package com.decarta.android.map;

import static android.opengl.GLES10.GL_FLOAT;
import static android.opengl.GLES10.GL_TEXTURE_2D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;

import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYDouble;

/**
 * 多边形类
 */
public class Polygon extends Shape{
	/*
	 * 该多边形在地图上经过的经纬坐标点
	 */
	private List<Position> positions=null;
	/*
	 * 该多边形在地图上经过的墨卡托坐标点
	 */
	private XYDouble mercXYs[]=null;
	/*
	 * 绘制该多边形的颜色值
	 */
	public int fillColor=0xFF047EFB; 
	/*
	 * 绘制该多边形的颜色透明度
	 */
    public float opacity= 0.6f;
    /*
     * 绘制该多边形的线宽
     */
    public int strokeSize=6;
    
    private static final String TAG = "Polygon";
    
    public Polygon(List<Position> positions,String polygonName) throws APIException{
		super(polygonName);
		setPositions(positions);
	}
	
	public void setPositions(List<Position> positions) throws APIException {
		this.positions = positions;
		if(positions!=null && positions.size()>0){
			mercXYs=new XYDouble[positions.size()];
			try{
				for(int m=0;m<positions.size();m++){
					mercXYs[m]=Util.posToMercPix(positions.get(m),Shape.ZOOM_LEVEL);
				}
			}catch(APIException e){
				positions=null;
				mercXYs=null;
				throw e;
			}
			
		}
	}
	
	public void renderGL(GL10 gl,XYDouble topLeftXY, float zoomLevel){
		if(mercXYs.length<2) return;
		double zoomScale=Math.pow(2,zoomLevel-Shape.ZOOM_LEVEL);
		
		float red=((fillColor & 0x00ff0000)>>16)/(float)255;
        float green=((fillColor & 0x0000ff00)>>8)/(float)255;
        float blue=((fillColor & 0x000000ff))/(float)255;
        gl.glDisable(GL_TEXTURE_2D);
        gl.glColor4f(red, green, blue, opacity);
        
        ByteBuffer vbb2=ByteBuffer.allocateDirect(2*mercXYs.length*4);
		vbb2.order(ByteOrder.nativeOrder());
		FloatBuffer vertexBuffer=vbb2.asFloatBuffer();
		for(int i=0;i<mercXYs.length;i++){
			XYDouble merc=mercXYs[i];
			float x=(float)(merc.x*zoomScale-topLeftXY.x);
			float y=(float)(-merc.y*zoomScale+topLeftXY.y);
			vertexBuffer.put(x);
			vertexBuffer.put(y);
		}
		vertexBuffer.position(0);
		gl.glVertexPointer(2,GL_FLOAT, 0, vertexBuffer);
		gl.glLineWidth(strokeSize);
        gl.glDrawArrays(GL10.GL_LINE_STRIP,0,mercXYs.length);
	}
	
	public void renderCanvas(Canvas canvas,XYDouble topLeftXY,float zoomLevel){
		if(mercXYs.length<2) return;
		double zoomScale=Math.pow(2,zoomLevel-Shape.ZOOM_LEVEL);
		
		Path path=new Path();
		for(int i=0;i<mercXYs.length;i++){
			XYDouble merc=mercXYs[i];
			float x=(float)(merc.x*zoomScale-topLeftXY.x);
			float y=(float)(-merc.y*zoomScale+topLeftXY.y);
			if(i==0) path.moveTo(x, y);
			else path.lineTo(x, y);
		}
		
		Paint paint=new Paint();
		paint.setColor(fillColor);
		paint.setStrokeWidth(strokeSize);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStyle(Style.STROKE);
		paint.setAlpha(Math.round(opacity*255));
		paint.setAntiAlias(true);
		
		canvas.drawPath(path, paint);
	}

	public List<Position> getPositions() {
		return positions;
	}

	public void setFillColor(int fillColor) {
		this.fillColor = fillColor;
	}
	
}
