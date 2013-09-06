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
	public int hashCode() {
		// TODO Auto-generated method stub
		int hash = 7;
		hash = 97 * hash + (int) (Double.doubleToLongBits(this.y) ^ (Double.doubleToLongBits(this.y) >>> 32));
		hash = 97 * hash + (int) (Double.doubleToLongBits(this.x) ^ (Double.doubleToLongBits(this.x) >>> 32));
		return hash;
	}
	
	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		if(!(o instanceof XYDouble)) return false;
		XYDouble i=(XYDouble)o;
		return i.x==this.x && i.y==this.y;
	}
}
