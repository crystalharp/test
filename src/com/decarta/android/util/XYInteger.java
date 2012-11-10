/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.util;
/**
 * Simple data structure for 2D X/Y coordinates.  Usually used to represent positions
 * on screen.
 * */
public class XYInteger {
	public int x;
	public int y;
	public XYInteger(int x, int y){
		this.x=x;
		this.y=y;
	}
	@Override
	public String toString(){
		return "x:"+x+"|y:"+y;
	}
		
	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		if(!(o instanceof XYInteger)) return false;
		XYInteger i=(XYInteger)o;
		return i.x==this.x && i.y==this.y;
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		int hash = 3;
		hash = 29 * hash + x;
		hash = 29 * hash + y;
		return hash;
	}
}
