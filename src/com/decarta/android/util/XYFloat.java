/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.util;

/**
 * Simple data structure for 2D X/Y coordinates. Usually used to represent
 * positions on screen.
 * */
public class XYFloat {
	public float x;
	public float y;

	public XYFloat(float x, float y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "x:" + x + "|y:" + y;
	}
	
	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		if(!(o instanceof XYFloat)) return false;
		XYFloat i=(XYFloat)o;
		return i.x==this.x && i.y==this.y;
	}
}
