package com.decarta.android.map;

import com.decarta.android.exception.APIException;
import com.tigerknows.android.location.Position;

public class MyLocation extends OverlayItem {
    public static final int MODE_NONE = 0;
    public static final int MODE_NORMAL = 1;
    public static final int MODE_NAVIGATION = 2;
    public static final int MODE_ROTATION = 3;
    
    public static final double REFRESH_TIME = 1000000000*1.5;
    
    public int mode = MODE_NORMAL;
    public long refreshTime;
    public Icon faceToNormal;
    public Icon faceToFocused;
    private Position position;
    
    public MyLocation(Position position, Icon icon, Icon iconFocused, Icon faceToNormal, Icon faceToFocused, String message, RotationTilt rotationTilt)
            throws APIException {
        super(position, icon, iconFocused, message, rotationTilt);
        this.faceToNormal = faceToNormal;
        this.faceToFocused = faceToFocused;
    }
    
    @Override
    public void setPosition(Position position) throws APIException {
        this.position = null;
        super.setPosition(position);
        if (getMercXY() != null) {
            this.position = position;
        }
    }
    
    @Override
    public Position getPosition() {
        return position;
    }
}
