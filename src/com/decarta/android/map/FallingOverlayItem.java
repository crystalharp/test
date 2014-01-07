package com.decarta.android.map;

import javax.microedition.khronos.opengles.GL10;

import com.decarta.android.exception.APIException;
import com.decarta.android.map.RotationTilt.RotateReference;
import com.decarta.android.map.RotationTilt.TiltReference;
import com.tigerknows.android.location.Position;

public class FallingOverlayItem extends OverlayItem {
	private long animationDuration = 200;//毫秒
	private boolean firstTimeDraw = true;
	private long animationEndTime;
	float endY;

	private boolean animating = false;
	public boolean isAnimating() {
		return animating;
	}


	public FallingOverlayItem(Position position, Icon icon, Icon iconFocused, String message, RotationTilt rotationTilt)
			throws APIException {
		super(position, icon, iconFocused, message, rotationTilt);
	}

	public void drawPin(double scale, double topLeftXf, double topleftYf, MapMode mapMode, GL10 gl) {
		if (firstTimeDraw) {
			animating = true;
			animationEndTime = System.currentTimeMillis() + animationDuration;
			firstTimeDraw = false;
			endY = (float) (-getMercXY().y
					* scale + topleftYf);
		}
		float x = (float) (getMercXY().x
				* scale - topLeftXf);

	    float fallingDistancePercent = 1 - (float)(animationEndTime - System.currentTimeMillis()) / animationDuration;
	    if (fallingDistancePercent >= 1) {
	        fallingDistancePercent = 1;
	        animating = false;
	    }
		float y;
		if (animating) {
			y = endY * fallingDistancePercent;
		} else {
			y = (float)(-getMercXY().y * scale + topleftYf);
		}
		RotationTilt rt = getRotationTilt();
		float zRot = rt.getRotation();
		if (rt.getRotateRelativeTo().equals(
				RotateReference.MAP)) {
			zRot += mapMode.getzRotation();
		}
		float xRot = rt.getTilt();
		if (rt.getTiltRelativeTo().equals(
				TiltReference.SCREEN)) {
			xRot -= mapMode.getxRotation();
		}
		gl.glTranslatef(x, y, 0);
		gl.glRotatef(-mapMode.getzRotation(), 0, 0, 1);
		gl.glRotatef(xRot, 1, 0, 0);
		gl.glRotatef(zRot, 0, 0, 1);
	}
}
