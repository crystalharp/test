package com.decarta.android.util;

public class XYDouble {
	public double x;
	public double y;

	public XYDouble(double x, double y) {
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
		if(!(o instanceof XYDouble)) return false;
		XYDouble i=(XYDouble)o;
		return i.x==this.x && i.y==this.y;
	}
}
