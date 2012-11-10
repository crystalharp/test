package com.decarta.android.map;

import com.decarta.CONFIG;


public class RotationTilt {
	public enum RotateReference{SCREEN,MAP};
	public enum TiltReference{SCREEN,MAP};
	
	private RotateReference rotateRelativeTo=null;
	private float rotation=0;
	private TiltReference tiltRelativeTo=null;
	private float tilt=0;
	
	private float cosR=1;
	private float sinR=0;
	private float cosT=1;
	private float sinT=0;
	
	/**
	 * default constructor rotateRelativeTo default to RotateReference.SCREEN
	 * tiltRelativeTo default to TiltReference.SCREEN
	 */
	public RotationTilt(){
		this(RotateReference.SCREEN, TiltReference.SCREEN);
	}
	
	public RotationTilt(RotateReference rotateRelativeTo, TiltReference tiltRelativeTo){
		this.rotateRelativeTo=rotateRelativeTo;
		this.tiltRelativeTo=tiltRelativeTo;
	}

	public float getRotation() {
		return rotation;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
		this.cosR=(float)Math.cos(this.rotation*Math.PI/180);
		this.sinR=(float)Math.sin(this.rotation*Math.PI/180);
	}

	public float getTilt() {
		return tilt;
	}

	public void setTilt(float tilt) {
		if(!CONFIG.DRAW_BY_OPENGL) return;
		this.tilt = tilt;
		this.cosT=(float)Math.cos(this.tilt*Math.PI/180);
		this.sinT=(float)Math.sin(this.tilt*Math.PI/180);
	}

	public RotateReference getRotateRelativeTo() {
		return rotateRelativeTo;
	}

	public TiltReference getTiltRelativeTo() {
		return tiltRelativeTo;
	}

	public float getCosR() {
		return cosR;
	}

	public float getSinR() {
		return sinR;
	}

	public float getCosT() {
		return cosT;
	}

	public float getSinT() {
		return sinT;
	}
	
	
}
