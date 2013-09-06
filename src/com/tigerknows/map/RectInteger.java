/**
 * 
 */
package com.tigerknows.map;

import com.decarta.android.util.*;

/**
 * @author chenming
 *
 */
public class RectInteger {
    public int left;
    public int right;
    public int top;
    public int bottom;
    
    public RectInteger() {
    	left = 0;
    	right = 0;
    	top = 0;
    	bottom = 0;
    }
    
    public RectInteger(int l, int r, int t, int b) {
    	left = l;
    	right = r;
    	top = t;
    	bottom = b;
    }
    
    public boolean isPointInRectI(XYInteger point) {
        if (point.x < left || point.x > right || point.x < top || point.y > bottom) {
            return false;
        }
        else {
            return true;
        }
    }
    
    public boolean isPointInRectF(XYFloat point) {
        if (point.x < left || point.x > right || point.x < top || point.y > bottom) {
            return false;
        }
        else {
            return true;
        }
    }
}
