package com.decarta.android.map;

import com.tigerknows.android.location.Position;
import com.decarta.android.exception.APIException;
import com.decarta.android.map.MapMode;
import com.decarta.android.map.RotationTilt.RotateReference;
import com.decarta.android.map.RotationTilt.TiltReference;
import javax.microedition.khronos.opengles.GL10;

public class PopupOverlayItem extends OverlayItem {
	private long animationDuration = 300;//毫秒
	private float currScale = 0;

	private boolean firstTimeDraw = true;
	private long animationEndTime;

	private boolean animating = false;

	public boolean isAnimating() {
		return animating;
	}

	public PopupOverlayItem(Position position, Icon icon, Icon iconFocused, String message, RotationTilt rotationTilt)
			throws APIException {
		super(position, icon, iconFocused, message, rotationTilt);

	}

	public PopupOverlayItem(OverlayItem item) throws APIException {
		super(item.getPosition(), item.getIcon(), item.getIconFocused(), item.getMessage(), item.getRotationTilt());
	}

	public void drawPin(double scale, double topLeftXf, double topleftYf, MapMode mapMode, GL10 gl) {
		if (firstTimeDraw) {
			animating = true;
			animationEndTime = System.currentTimeMillis() + animationDuration;
			firstTimeDraw = false;
		}
		float x = (float) (getMercXY().x
				* scale - topLeftXf);
		float y = (float) (-getMercXY().y
				* scale + topleftYf);

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

		if (animating) {
			currScale = 1 - (float)(animationEndTime - System.currentTimeMillis()) / animationDuration;
		    if (currScale >= 1) {
		    	currScale = 1;
		    	animating = false;
		    }
		} else {
			currScale = 1;
		}
		gl.glScalef(currScale, currScale, currScale);
	}
}
