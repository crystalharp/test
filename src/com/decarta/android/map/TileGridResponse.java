/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.map;

import com.decarta.android.scale.Length;
import com.decarta.android.util.XYDouble;
import com.decarta.android.util.XYFloat;
import com.decarta.android.util.XYZ;
import com.tigerknows.android.location.Position;
 
public class TileGridResponse {
	private Length radiusY;
	private Position centerPosition;
	private Position tileGridCenterPosition;
	private XYFloat fixedGridPixelOffset=new XYFloat(0f,0f);
	private String seedTileURL=null;

    public XYZ centerXYZTK=null;
	public XYZ centerXYZ=null;
	public XYDouble centerXY=null;
	
	public Position getCenterPosition() {
		return centerPosition;
	}
	public void setCenterPosition(Position centerPosition) {
		this.centerPosition = centerPosition;
	}
	public XYFloat getFixedGridPixelOffset() {
		return fixedGridPixelOffset;
	}
	public void setFixedGridPixelOffset(XYFloat move) {
		this.fixedGridPixelOffset = move;
	}
	public Length getRadiusY() {
		return radiusY;
	}
	public void setRadiusY(Length radiusY) {
		this.radiusY = radiusY;
	}
	public String getSeedTileURL() {
		return seedTileURL;
	}
	public void setSeedTileURL(String seedTileURL) {
		this.seedTileURL = seedTileURL;
	}
	public Position getTileGridCenterPosition() {
		return tileGridCenterPosition;
	}
	public void setTileGridCenterPosition(Position tileGridCenterPosition) {
		this.tileGridCenterPosition = tileGridCenterPosition;
	}
	
}
