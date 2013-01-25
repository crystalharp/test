package com.decarta.android.map;

import static android.opengl.GLES10.GL_ALIASED_LINE_WIDTH_RANGE;
import static android.opengl.GLES10.GL_FLOAT;
import static android.opengl.GLES10.GL_LINE_STRIP;
import static android.opengl.GLES10.GL_TEXTURE_2D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.decarta.CONFIG;
import com.decarta.android.event.EventListener;
import com.decarta.android.event.EventSource;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYDouble;
import com.decarta.android.util.XYInteger;
import com.decarta.android.util.XYZ;

/**
 * used when draw polygon on map. subclass of Shape.
 * @author zsong
 *
 */
public class Polyline extends Shape implements EventSource{
	private static int GRANULARITY=10;
	private static int[][] GENERALIZE_ZOOM_LEVEL={
	// genLevel, idxLevel 这个表格依据什么制定
		{4,2},  //zoom 0
		{4,2},  //zoom 1
		{4,2},  //zoom 2
		{4,2},  //zoom 3
		{4,2},  //zoom 4
		{7,6},  //zoom 5
		{7,6},  //zoom 6
		{7,6},  //zoom 7, max 80 points
		{9,9},  //zoom 8, max 220 points
		{9,9},  //zoom 9, max 100 points
		{11,11}, //zoom 10, max 220 points
		{11,11}, //zoom 11, max 100 points
		{13,13}, //zoom 12, max 170 points
		{13,13}, //zoom 13, max 90 points
		{20,15}, //zoom 14, max 150 points
		{20,15}, //zoom 15
		{20,15}, //zoom 16
		{20,15}, //zoom 17
		{20,15}, //zoom 18
		{20,15}, //zoom 19
		{20,15}  //zoom 20
	};
	private static FloatBuffer vertexBuffer;
	
	static{
		ByteBuffer vbb2=ByteBuffer.allocateDirect(2*8*4);
		vbb2.order(ByteOrder.nativeOrder());
		vertexBuffer=vbb2.asFloatBuffer();
	}

	public int fillColor = 0xFF047EFB;
    public int strokeSize=6;
    public float opacity= 1.0f;
    	
    private List<Position> positions=null;
    private XYDouble[] mercXYs=null;
    /*
     * 在genLevel下, 经过过滤后的Position的index的集合
     * genLevel=20时, 不过滤
     */
    @SuppressWarnings("unchecked")
	private ArrayList<Short> generalizedPosIdxs[]=new ArrayList[21];
    /*
     * 在idxLevel下, 每一个tile下包含的Position的index
     */
    @SuppressWarnings("unchecked")
	private HashMap<XYZ, ArrayList<Short>> pointIdxs[]=new HashMap[21];
        
	private Map<Integer,ArrayList<EventListener>> eventListeners = new HashMap<Integer,ArrayList<EventListener>>();
	
	/*
     * 生成的三角形顶点连接次序
     */
    ShortBuffer indiceBuffer;
	
	private final String TAG = "Polyline";
	
	public Polyline(List<Position> positions, String polylineName) throws APIException{
		super(polylineName);
		setPositions(positions);
	}
	
	/**
	 * 1.计算所有的Position在Tile的分布情况, 
	 * 2.过滤过多的Position
	 * @param zoomLevel
	 * @throws APIException
	 */
	private void generalizePoints(int zoomLevel) throws APIException{
        //Check the point index
        int genLevel=GENERALIZE_ZOOM_LEVEL[zoomLevel][0];
        int idxLevel=GENERALIZE_ZOOM_LEVEL[zoomLevel][1];
        double scale=Math.pow(2, idxLevel-ZOOM_LEVEL);
        
        if(genLevel==CONFIG.ZOOM_UPPER_BOUND){
			if(pointIdxs[idxLevel]==null){
				HashMap<XYZ, ArrayList<Short>> pointIdx=new HashMap<XYZ,ArrayList<Short>>();
		        
		        for(int i=0;i<positions.size();i++){
		        	XYDouble mercXY=new XYDouble(mercXYs[i].x*scale,mercXYs[i].y*scale);
					XYInteger ne = Util.mercXYToNE(mercXY);
					//String key=idxLevel+"_"+ne.x+"_"+ne.y;
					XYZ key=new XYZ(ne.x,ne.y,idxLevel);
		        	if(!pointIdx.containsKey(key)) pointIdx.put(key, new ArrayList<Short>());
		        	pointIdx.get(key).add((short)i);
		        	
		        }
		        pointIdxs[idxLevel]=pointIdx;
			}
		}else{
			if (this.generalizedPosIdxs[genLevel]==null){
	        	double zoomScale=(float)Math.pow(2, ZOOM_LEVEL-genLevel);
	        	double minDist=(GRANULARITY*GRANULARITY)*(zoomScale*zoomScale);
	        	
	            ArrayList<Short> genIdx=new ArrayList<Short>();
	            double lastX=0;
	            double lastY=0;
	            for (int i = 0; i < positions.size(); i++){
	               double dist=(mercXYs[i].x-lastX)*(mercXYs[i].x-lastX)+(mercXYs[i].y-lastY)*(mercXYs[i].y-lastY);
	               if(i==0 || dist>minDist || i==positions.size()-1){
	                	genIdx.add((short)i);
	                	lastX=mercXYs[i].x;
	    	        	lastY=mercXYs[i].y;
	               }
	            }
	            generalizedPosIdxs[genLevel]=genIdx;
	        }
			
			if(pointIdxs[idxLevel]==null){
				HashMap<XYZ, ArrayList<Short>> pointIdx=new HashMap<XYZ,ArrayList<Short>>();
		        
		        for(int i=0;i<generalizedPosIdxs[genLevel].size();i++){
		        	int idx=0x0000FFFF & generalizedPosIdxs[genLevel].get(i);
		        	XYDouble mercXY=new XYDouble(mercXYs[idx].x*scale,mercXYs[idx].y*scale);
					XYInteger ne = Util.mercXYToNE(mercXY);
					//String key=idxLevel+"_"+ne.x+"_"+ne.y;
					XYZ key=new XYZ(ne.x,ne.y,idxLevel);
		        	if(!pointIdx.containsKey(key)) pointIdx.put(key, new ArrayList<Short>());
		        	pointIdx.get(key).add((short)i);
		        	
		        }
		        pointIdxs[idxLevel]=pointIdx;
			}
		}
        
			
    }
	
	/**
	 * 1. 获取某一比例尺下显示的tile
	 * 2. 获取某一比例尺下显示的Position的index集合
	 * @param zoomLevel
	 * @param tiles
	 * @return
	 * @throws APIException
	 */
	public ArrayList<Short> getPointIdxs(int zoomLevel, ArrayList<Tile> tiles) throws APIException{
		generalizePoints(zoomLevel);
		
		int idxLevel=GENERALIZE_ZOOM_LEVEL[zoomLevel][1];
		
		ArrayList<Short> idx=new ArrayList<Short>();
		
		HashMap<XYZ,ArrayList<Short>> pointIdx=pointIdxs[idxLevel];
		
		List<XYZ> overlapTiles=new ArrayList<XYZ>();
		for(int i=0;i<tiles.size();i++){
			Tile tile=tiles.get(i);
			List<XYZ> xyzs=Util.findOverlapXYZs(tile.xyz, idxLevel);
			if(!overlapTiles.contains(xyzs.get(0))){
				overlapTiles.addAll(xyzs);
			}
			
		}
		for(int j=0;j<overlapTiles.size();j++){
			XYZ xyz=overlapTiles.get(j);
			//String key=xyz.z+"_"+xyz.x+"_"+xyz.y;
			XYZ key=xyz;
			if(pointIdx.containsKey(key)){
				idx.addAll(pointIdx.get(key));
			}
		}
		
		
		Collections.sort(idx);

        return idx;

    }
	
	/**
	 * 
	 * @param canvas
	 * @param topLeftXYf 左上角座标点
	 * @param zoomLevel
	 * @param mapLayerZoomlevel
	 * @param drawTiles 已被绘制的Tile块
	 * @throws APIException
	 */
	public void renderCanvas(Canvas canvas,XYDouble topLeftXYf, float zoomLevel,int mapLayerZoomlevel,ArrayList<Tile> drawTiles)throws APIException{
		ArrayList<Short> pointIdx=getPointIdxs(mapLayerZoomlevel, drawTiles);
		if(pointIdx.size()==0) return;
		
		//Paint pointP=new Paint();
		Paint shapeP=new Paint();
		shapeP.setColor(fillColor);
		shapeP.setStrokeWidth(strokeSize);
		shapeP.setAlpha(Math.round(opacity*255));
		shapeP.setStrokeCap(Paint.Cap.ROUND);
		float lastX=0;
		float lastY=0;
		boolean broken=false;
		
		int genLevel=GENERALIZE_ZOOM_LEVEL[mapLayerZoomlevel][0];
		
		if(genLevel!=CONFIG.ZOOM_UPPER_BOUND){
			//Log.i("Polyline","render posSize,genPosSize,pointIdxSize zoomLevel:"+positions.size()+","+generalizedPosIdxs[genLevel].size()+","+pointIdx.size()+","+mapLayerZoomlevel);
		}else{
			//Log.i("Polyline","render posSize,pointIdxSize zoomLevel:"+positions.size()+","+pointIdx.size()+","+mapLayerZoomlevel);
		}
		
		double zoomScale=(float)Math.pow(2,zoomLevel-Shape.ZOOM_LEVEL);
		for(int m=0;m<pointIdx.size();m++){
			double xx=0,yy=0;
			if(genLevel==CONFIG.ZOOM_UPPER_BOUND){
				xx=mercXYs[0x0000FFFF & pointIdx.get(m)].x;
				yy=mercXYs[0X0000FFFF & pointIdx.get(m)].y;
				
			}else{
				xx=mercXYs[0x0000FFFF & generalizedPosIdxs[genLevel].get(0x0000FFFF & pointIdx.get(m))].x;
				yy=mercXYs[0x0000FFFF & generalizedPosIdxs[genLevel].get(0X0000FFFF & pointIdx.get(m))].y;
			}
			float x=(float)(xx*zoomScale-topLeftXYf.x);
			float y=(float)(-yy*zoomScale+topLeftXYf.y);
			
			if((m==0 && pointIdx.get(m)!=0) || (m!=0 && broken) ){
				double preXX=0,preYY=0;
				if(genLevel==CONFIG.ZOOM_UPPER_BOUND){
					preXX=mercXYs[0x0000FFFF & pointIdx.get(m)-1].x;
					preYY=mercXYs[0x0000FFFF & pointIdx.get(m)-1].y;
				}else{
					preXX=mercXYs[0x0000FFFF & generalizedPosIdxs[genLevel].get(0x0000FFFF & pointIdx.get(m)-1)].x;
					preYY=mercXYs[0x0000FFFF & generalizedPosIdxs[genLevel].get(0x0000FFFF & pointIdx.get(m)-1)].y;
				}
				float preX=(float)(preXX*zoomScale-topLeftXYf.x);
				float preY=(float)(-preYY*zoomScale+topLeftXYf.y);
				canvas.drawLine(preX, preY, x, y, shapeP);
				//pointP.setColor(Color.BLACK);
				//canvas.drawCircle(preX, preY, 10, pointP);
			}else if(m!=0){
				canvas.drawLine(lastX, lastY, x, y, shapeP);
				
			}
			//pointP.setColor(Color.BLUE);
			//canvas.drawCircle(x, y, 10, pointP);
			
			broken=false;
			int genSize=(genLevel==CONFIG.ZOOM_UPPER_BOUND)?positions.size():generalizedPosIdxs[genLevel].size();
			if((m!=pointIdx.size()-1 && (0x0000FFFF & pointIdx.get(m))+1!=(0x0000FFFF & pointIdx.get(m+1)))
					|| (m==pointIdx.size()-1 && (0x0000FFFF & pointIdx.get(m))!=genSize-1)){
				double nextXX=0, nextYY=0;
				if(genLevel==CONFIG.ZOOM_UPPER_BOUND){
					nextXX=mercXYs[0x0000FFFF & pointIdx.get(m)+1].x;
					nextYY=mercXYs[0x0000FFFF & pointIdx.get(m)+1].y;
				}else{
					nextXX=mercXYs[0x0000FFFF & generalizedPosIdxs[genLevel].get(0x0000FFFF & pointIdx.get(m)+1)].x;
					nextYY=mercXYs[0x0000FFFF & generalizedPosIdxs[genLevel].get(0x0000FFFF & pointIdx.get(m)+1)].y;
				}
				float nextX=(float)(nextXX*zoomScale-topLeftXYf.x);
				float nextY=(float)(-nextYY*zoomScale+topLeftXYf.y);
				canvas.drawLine(x, y, nextX, nextY, shapeP);
				//pointP.setColor(Color.GREEN);
				//canvas.drawCircle(nextX, nextY, 10, pointP);
				
				broken=true;
			}
			
			lastX=x;
			lastY=y;
			
		}
	}

	public void drawLine(GL10 gl, float pixAx, float pixAy, float pixBx, float pixBy){
		double Elength = Math.sqrt((pixBx-pixAx)*(pixBx-pixAx)+(pixBy-pixAy)*(pixBy-pixAy));
		float pixEx = (float)((strokeSize/2)*(pixBx-pixAx)/Elength);
		float pixEy = (float)((strokeSize/2)*(pixBy-pixAy)/Elength);
		// 计算与E向量垂直且长度相等的向量N, S
		float pixNx = pixEy;
		float pixNy = -pixEx;
		float pixSx = -pixEy;
		float pixSy = pixEx;
		// 计算A点西南方点的坐标 向量A+(S-E)
		float pixASWx = pixAx + (pixSx - pixEx);
		float pixASWy = pixAy + (pixSy - pixEy);
		vertexBuffer.put(pixASWx);
		vertexBuffer.put(pixASWy);
		// 计算A点西北方点的坐标 向量A+(N-E)
		float pixANWx = pixAx + (pixNx - pixEx);
		float pixANWy = pixAy + (pixNy - pixEy);
		vertexBuffer.put(pixANWx);
		vertexBuffer.put(pixANWy);
		// 计算A点正南方点的坐标 向量A+S
		float pixASx = pixAx + pixSx;
		float pixASy = pixAy + pixSy;
		vertexBuffer.put(pixASx);
		vertexBuffer.put(pixASy);
		// 计算A点正北方点的坐标 向量A+N
		float pixANx = pixAx + pixNx;
		float pixANy = pixAy + pixNy;
		vertexBuffer.put(pixANx);
		vertexBuffer.put(pixANy);

		// 计算B点正南方点的坐标 向量B+S
		float pixBSx = pixBx + pixSx;
		float pixBSy = pixBy + pixSy;
		vertexBuffer.put(pixBSx);
		vertexBuffer.put(pixBSy);
		// 计算B点正北方点的坐标 向量B+N
		float pixBNx = pixBx + pixNx;
		float pixBNy = pixBy + pixNy;
		vertexBuffer.put(pixBNx);
		vertexBuffer.put(pixBNy);
		// 计算B点东南方点的坐标 向量B+(S+E)
		float pixBSEx = pixBx + (pixSx + pixEx);
		float pixBSEy = pixBy + (pixSy + pixEy);
		vertexBuffer.put(pixBSEx);
		vertexBuffer.put(pixBSEy);
		// 计算B点东北方点的坐标 向量B+(N+E)
		float pixBNEx = pixBx + (pixNx + pixEx);
		float pixBNEy = pixBy + (pixNy + pixEy);
		vertexBuffer.put(pixBNEx);
		vertexBuffer.put(pixBNEy);
		vertexBuffer.position(0);
		gl.glDrawElements(GL10.GL_TRIANGLES, 18, GL10.GL_UNSIGNED_SHORT, indiceBuffer);

		vertexBuffer.clear();
	}
	
	public void renderGL(GL10 gl,XYDouble topLeftXYf, float zoomLevel,int mapLayerZoomlevel,ArrayList<Tile> drawTiles)throws APIException{
		ArrayList<Short> pointIdx=getPointIdxs(mapLayerZoomlevel, drawTiles);
		if(pointIdx.size()==0) return;
		
		//long start=System.nanoTime();
		
		float lastX=0;
		float lastY=0;
		boolean broken=false;
		
		int genLevel=GENERALIZE_ZOOM_LEVEL[mapLayerZoomlevel][0];
		
		if(genLevel!=CONFIG.ZOOM_UPPER_BOUND){
			//Log.i("Polyline","render posSize,genPosSize,pointIdxSize zoomLevel:"+positions.size()+","+generalizedPosIdxs[genLevel].size()+","+pointIdx.size()+","+mapLayerZoomlevel);
		}else{
			//Log.i("Polyline","render posSize,pointIdxSize zoomLevel:"+positions.size()+","+pointIdx.size()+","+mapLayerZoomlevel);
		}
		
		gl.glDepthMask(false);
        gl.glDisable(GL_TEXTURE_2D);
        gl.glVertexPointer(2, GL_FLOAT, 0, vertexBuffer);
        float red=((fillColor & 0x00ff0000)>>16)/(float)255;
        float green=((fillColor & 0x0000ff00)>>8)/(float)255;
        float blue=((fillColor & 0x000000ff))/(float)255;
        gl.glColor4f(red, green, blue, opacity);
        int widths[]=new int[2];
        gl.glGetIntegerv(GL_ALIASED_LINE_WIDTH_RANGE, widths, 0);
        int width=this.strokeSize;
        if(widths[1]>0 && width>widths[1]){
        	width=widths[1];
        }
        gl.glLineWidth(width);
		gl.glPointSize(width/2f);
		//Log.i("Polyline","renderGL red,green,blue,opacity,line width,point size:"+red+","+green+","+blue+","+opacity+","+width+","+width/2f);
        
		double zoomScale=(float)Math.pow(2,zoomLevel-Shape.ZOOM_LEVEL);
		for(int m=0;m<pointIdx.size();m++){
			double xx=0,yy=0;
			if(genLevel==CONFIG.ZOOM_UPPER_BOUND){
				xx=mercXYs[0x0000FFFF & pointIdx.get(m)].x;
				yy=mercXYs[0X0000FFFF & pointIdx.get(m)].y;
				
			}else{
				xx=mercXYs[0x0000FFFF & generalizedPosIdxs[genLevel].get(0x0000FFFF & pointIdx.get(m))].x;
				yy=mercXYs[0x0000FFFF & generalizedPosIdxs[genLevel].get(0X0000FFFF & pointIdx.get(m))].y;
			}
			float x=(float)(xx*zoomScale-topLeftXYf.x);
			float y=(float)(-yy*zoomScale+topLeftXYf.y);
			
			if((m==0 && pointIdx.get(m)!=0) || (m!=0 && broken) ){
				double preXX=0,preYY=0;
				if(genLevel==CONFIG.ZOOM_UPPER_BOUND){
					preXX=mercXYs[0x0000FFFF & pointIdx.get(m)-1].x;
					preYY=mercXYs[0x0000FFFF & pointIdx.get(m)-1].y;
				}else{
					preXX=mercXYs[0x0000FFFF & generalizedPosIdxs[genLevel].get(0x0000FFFF & pointIdx.get(m)-1)].x;
					preYY=mercXYs[0x0000FFFF & generalizedPosIdxs[genLevel].get(0x0000FFFF & pointIdx.get(m)-1)].y;
				}
				float preX=(float)(preXX*zoomScale-topLeftXYf.x);
				float preY=(float)(-preYY*zoomScale+topLeftXYf.y);
				drawLine(gl,preX, preY, x, y);
				
			}else if(m!=0){
				drawLine(gl,lastX, lastY, x, y);
				
			}
			
			broken=false;
			int genSize=(genLevel==CONFIG.ZOOM_UPPER_BOUND)?positions.size():generalizedPosIdxs[genLevel].size();
			if((m!=pointIdx.size()-1 && (0x0000FFFF & pointIdx.get(m))+1!=(0x0000FFFF & pointIdx.get(m+1)))
					|| (m==pointIdx.size()-1 && (0x0000FFFF & pointIdx.get(m))!=genSize-1)){
				double nextXX=0, nextYY=0;
				if(genLevel==CONFIG.ZOOM_UPPER_BOUND){
					nextXX=mercXYs[0x0000FFFF & pointIdx.get(m)+1].x;
					nextYY=mercXYs[0x0000FFFF & pointIdx.get(m)+1].y;
				}else{
					nextXX=mercXYs[0x0000FFFF & generalizedPosIdxs[genLevel].get(0x0000FFFF & pointIdx.get(m)+1)].x;
					nextYY=mercXYs[0x0000FFFF & generalizedPosIdxs[genLevel].get(0x0000FFFF & pointIdx.get(m)+1)].y;
				}
				float nextX=(float)(nextXX*zoomScale-topLeftXYf.x);
				float nextY=(float)(-nextYY*zoomScale+topLeftXYf.y);
				drawLine(gl,x, y, nextX, nextY);
				
				broken=true;
			}
			
			lastX=x;
			lastY=y;
			
		}

		indiceBuffer.clear();
        gl.glColor4f(1, 1, 1, 1);
        gl.glLineWidth(1);
        gl.glPointSize(1);
        gl.glDepthMask(true);
        
        //Log.i("Polyline","draw points, time:"+pointIdx.size()+","+(System.nanoTime()-start)/1E6);
	}
	
	
		
	public List<Position> getPositions() {
		return positions;
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
		
		// 生成三角形顶点连接次序
        ByteBuffer ibb2 = ByteBuffer.allocateDirect(18*2);
        ibb2.order(ByteOrder.nativeOrder());
		indiceBuffer = ibb2.asShortBuffer();
		indiceBuffer.put((short) (0)); indiceBuffer.put((short) (2)); indiceBuffer.put((short) (1)); 
		indiceBuffer.put((short) (2)); indiceBuffer.put((short) (3)); indiceBuffer.put((short) (1)); 
		indiceBuffer.put((short) (2)); indiceBuffer.put((short) (4)); indiceBuffer.put((short) (3)); 
		indiceBuffer.put((short) (4)); indiceBuffer.put((short) (5)); indiceBuffer.put((short) (3)); 
		indiceBuffer.put((short) (4)); indiceBuffer.put((short) (6)); indiceBuffer.put((short) (5)); 
		indiceBuffer.put((short) (6)); indiceBuffer.put((short) (7)); indiceBuffer.put((short) (5)); 
		indiceBuffer.position(0);
	}

	public XYDouble[] getMercXYs() {
		return mercXYs;
	}
	
	public void setFillColor(int fillColor) {
		this.fillColor = fillColor;
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
	
	public void executeTouchListeners(Position position){
		if(eventListeners.containsKey(com.decarta.android.event.EventType.TOUCH)){
			ArrayList<EventListener> listeners=eventListeners.get(com.decarta.android.event.EventType.TOUCH);
			for(int i=0;i<listeners.size();i++){
				((TouchEventListener)(listeners.get(i))).onTouchEvent(this, position);
			}
		}
	}
	
	/**
	 *listener for touch event
	 */
	public interface TouchEventListener extends EventListener{
		public void onTouchEvent(EventSource eventSource, Position pos);
	}
}
