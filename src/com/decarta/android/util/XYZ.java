package com.decarta.android.util;

public class XYZ {
	public int x = 0;
	public int y = 0;
	public int z = 0;

	public XYZ(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof XYZ) {
			XYZ xyz = (XYZ) o;
			return x == xyz.x && y == xyz.y && z == xyz.z;
		}
		return false;
	}

	@Override
	public String toString() {
		return x + "_" + y + "_" + z;
	}
	
	public XYZ clone(){
		return new XYZ(x,y,z);
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		int hash = 3;
		hash = 29 * hash + x;
		hash = 29 * hash + y;
		hash = 29 * hash + z;
		return hash;
	}
}
