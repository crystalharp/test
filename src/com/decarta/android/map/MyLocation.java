package com.decarta.android.map;

import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;

public class MyLocation extends OverlayItem {
    public static final int MODE_NONE = 0;
    public static final int MODE_NORMAL = 1;
    public static final int MODE_NAVIGATION = 2;
    public static final int MODE_ROTATION = 3;
    
    public static final double REFRESH_TIME = 1000000000*1.5;
    
    public int mode;
    public Icon focused;
    public long refreshTime;
    
    public MyLocation(Position position, Icon icon, Icon focused, String message, RotationTilt rotationTilt)
            throws APIException {
        super(position, icon, message, rotationTilt);
        this.focused = focused;
    }
}
