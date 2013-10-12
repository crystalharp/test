package com.decarta.android.map;

import com.decarta.CONFIG;
import com.tigerknows.map.MapView;
import com.tigerknows.util.XYInteger;

public class MapMode{
	private static int PERSPECTIVE_DEGREE=15;
	private static float MAX_PERSPECTIVE_RATIO=0.5f;
	private static float Y_SAME_HEIGHT=-1/4f;
	private static int DELAY_ANGLE=15;
	
	public int nearZ=1024;
	public int middleZ=2048;
	public int farZ=middleZ;
	
	public float xRotationEnd=0;
	public long xRotationEndTime=0;
	public boolean xRotating=false;
	
	public float zRotationEnd=0;
	public long zRotationEndTime=0;
	public boolean zRotating=false;
	
	private float xRotation=0;
	private float zRotation=0;
	public float scale=middleZ/(float)nearZ;
	private float cosX=1;
	private float sinX=0;
	private float cosZ=1;
	private float sinZ=0;
	
	public float displaySizeConvXR=0;
	public float displaySizeConvXL=0;
	public float displaySizeConvYT=0;
	public float displaySizeConvYB=0;
	public int gridSizeConvXR=0;
	public int gridSizeConvXL=0;
	public int gridSizeConvYT=0;
	public int gridSizeConvYB=0;
	
	public void resetXEasing(){
		xRotationEnd=0;
		xRotationEndTime=0;
		xRotating=false;
		
	}
	
	public void resetZEasing(){
		zRotationEnd=0;
		zRotationEndTime=0;
		zRotating=false;
	
	}
	
	public void setXRotation(float xRotationS,XYInteger displaySize){
		if(!CONFIG.ENABLE_TILT) return;
		
		if(!CONFIG.DRAW_BY_OPENGL) return;
		
		if(xRotationS>0){
			xRotationS=0;
		}
		else if(xRotationS<MapView.MAP_TILT_MIN){
			xRotationS=MapView.MAP_TILT_MIN;
		}
		this.xRotation=xRotationS;
		cosX=(float)Math.cos(this.xRotation*Math.PI/180);
		sinX=(float)Math.sin(this.xRotation*Math.PI/180);
		configViewSize(displaySize);
	}
	
	public void setZRotation(float zRotationS,XYInteger displaySize){
		if(!CONFIG.ENABLE_ROTATE) return;
		
		zRotationS=((zRotationS+180)%360+360)%360-180;
		this.zRotation=zRotationS;
		cosZ=(float)Math.cos(this.zRotation*Math.PI/180);
		sinZ=(float)Math.sin(this.zRotation*Math.PI/180);
		configViewSize(displaySize);
	}
	
	public void configViewDepth(XYInteger displaySize){
		if(!CONFIG.ENABLE_TILT) return;
		
		if(!CONFIG.DRAW_BY_OPENGL) return;
		
		float perspectiveTan=(float)Math.tan(PERSPECTIVE_DEGREE*Math.PI/180);
		float maxPTan=displaySize.x/(float)displaySize.y*MAX_PERSPECTIVE_RATIO;
		perspectiveTan=Math.min(perspectiveTan,maxPTan);
		
		double cosX=Math.cos(MapView.MAP_TILT_MIN*Math.PI/180);
		double sinX=Math.sin(MapView.MAP_TILT_MIN*Math.PI/180);
		
		nearZ=(int)Math.ceil(displaySize.x/2f*(-sinX)/(perspectiveTan*cosX));
		int mz=(int)Math.ceil(nearZ+displaySize.y/2*(-sinX)/cosX);
		middleZ=nearZ*2;
		while(middleZ<mz){
			middleZ+=nearZ;
		}
		
		farZ=(int)Math.ceil((middleZ*displaySize.y/2f)/(nearZ*cosX-displaySize.y/2f*(-sinX))*(-sinX))+middleZ;
		
		//Log.d("TilesView","viewDepth nearZ,middleZ,farZ,tanP:"+nearZ+","+middleZ+","+farZ+","+perspectiveTan);
	}
	
	public void configViewSize(XYInteger displaySize){
		float yHeight=Y_SAME_HEIGHT*displaySize.y;
		scale=(float)(middleZ/(nearZ*cosX+(yHeight*(-sinX))));
		    		
		double yT=(middleZ*(-displaySize.y/2f))/(nearZ*scale*cosX-(-displaySize.y/2f)*scale*sinX);
		double xT=((displaySize.x/2f)*middleZ+(displaySize.x/2f)*scale*yT*sinX)/(nearZ*scale);
		double dT=Math.sqrt(xT*xT+yT*yT);
		
		double yB=(middleZ*(displaySize.y/2f))/(nearZ*scale*cosX-(displaySize.y/2f)*scale*sinX);
		double xB=((displaySize.x/2f)*middleZ+(displaySize.x/2f)*scale*yB*sinX)/(nearZ*scale);
		double dB=Math.sqrt(xB*xB+yB*yB);
		
		float cosTmZ=(float)(xT/dT*cosZ+yT/dT*sinZ);
		float cosTpZ=(float)(xT/dT*cosZ-yT/dT*sinZ);
		float sinTmZ=(float)(yT/dT*cosZ-xT/dT*sinZ);
		float sinTpZ=(float)(yT/dT*cosZ+xT/dT*sinZ);
		float convX1T=(float)Math.abs(dT*cosTmZ);
		float convX2T=(float)Math.abs(dT*cosTpZ);
		float convY1T=(float)Math.abs(dT*sinTpZ);
		float convY2T=(float)Math.abs(dT*sinTmZ);
		
		float cosBmZ=(float)(xB/dB*cosZ+yB/dB*sinZ);
		float cosBpZ=(float)(xB/dB*cosZ-yB/dB*sinZ);
		float sinBmZ=(float)(yB/dB*cosZ-xB/dB*sinZ);
		float sinBpZ=(float)(yB/dB*cosZ+xB/dB*sinZ);
		float convX1B=(float)Math.abs(dB*cosBmZ);
		float convX2B=(float)Math.abs(dB*cosBpZ);
		float convY1B=(float)Math.abs(dB*sinBpZ);
		float convY2B=(float)Math.abs(dB*sinBmZ);
		
		if(zRotation>=DELAY_ANGLE && zRotation<=180-DELAY_ANGLE){
			displaySizeConvXR=Math.max(convX1B, convX2B);
		}else{
			displaySizeConvXR=Math.max(convX1T, convX2T);
		}
		if(zRotation>=-(180-DELAY_ANGLE) && zRotation<=-DELAY_ANGLE){
			displaySizeConvXL=Math.max(convX1B, convX2B);
		}else{
			displaySizeConvXL=Math.max(convX1T, convX2T);
		}
		
		if(zRotation>=-(90-DELAY_ANGLE) && zRotation<=90-DELAY_ANGLE){
			displaySizeConvYB=Math.max(convY1B, convY2B);
		}else{
			displaySizeConvYB=Math.max(convY1T, convY2T);
		}
		if(zRotation>=(90+DELAY_ANGLE) && zRotation<=180 
				|| zRotation>=-180 && zRotation<-(90+DELAY_ANGLE)){
			displaySizeConvYT=Math.max(convY1B, convY2B);
		}else{
			displaySizeConvYT=Math.max(convY1T, convY2T);
		}
			
		gridSizeConvXR=(int) Math.ceil(displaySizeConvXR/CONFIG.TILE_SIZE) + 1;
		gridSizeConvXL=(int) Math.ceil(displaySizeConvXL/CONFIG.TILE_SIZE) + 1;
		gridSizeConvYB=(int) Math.ceil(displaySizeConvYB/CONFIG.TILE_SIZE) + 1;
		gridSizeConvYT=(int) Math.ceil(displaySizeConvYT/CONFIG.TILE_SIZE) + 1;
		
		//Log.d("MapMode","configViewSize scale,sizeXR,sizeXL,sizeYB,sizeYT,zRotation,xRoation:"+scale+","+(int)displaySizeConvXR+","+(int)displaySizeConvXL+","+(int)displaySizeConvYB+","+(int)displaySizeConvYT+","+(int)zRotation+","+(int)xRotation);
		//Log.i("MapMode","configViewSize gridXR,gridXL,gridYB,gridYT,zRotation,xRoation:"+gridSizeConvXR+","+gridSizeConvXL+","+gridSizeConvYB+","+gridSizeConvYT+","+(int)zRotation+","+(int)xRotation);
	}

	public float getxRotation() {
		return xRotation;
	}

	public float getzRotation() {
		return zRotation;
	}

	public float getCosX() {
		return cosX;
	}

	public float getSinX() {
		return sinX;
	}

	public float getCosZ() {
		return cosZ;
	}

	public float getSinZ() {
		return sinZ;
	}

	
}
