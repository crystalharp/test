/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.location;

import java.io.Serializable;

/**
 * The BoundingBox class marks a rectangular geography. The extents are marked 
 * by two {@link Position}s located at the upper-right (maximum) and lower-left 
 * (minimum) locations. <br /><br /> 
 */
public class BoundingBox implements Serializable{

	private static final long serialVersionUID = 1L;
	private Position minPosition;
	private Position maxPosition;

	public BoundingBox() {
	}

	public BoundingBox(Position min, Position max) {
		this.minPosition = min;
		this.maxPosition = max;
	}

	/**  The upper-right corner of the bounding box 
	 */
	public Position getMaxPosition() {
		return maxPosition;
	}

	/**  The upper-right corner of the bounding box 
	 */
	public void setMaxPosition(Position maxPosition) {
		this.maxPosition = maxPosition;
	}

	/** The lower-left corner of the bounding box */
	public Position getMinPosition() {
		return minPosition;
	}

	/** The lower-left corner of the bounding box */
	public void setMinPosition(Position minPosition) {
		this.minPosition = minPosition;
	}

	/**
	 * Retrieve the center position of the bounding box.
	 * @return Position calculated center point of the BoundingBox
	 */
	public Position getCenterPosition() {
		double centerLat = this.maxPosition.getLat() - 
			((this.maxPosition.getLat() - this.minPosition.getLat()) / 2);
		double centerLon = this.maxPosition.getLon() - 
			((this.maxPosition.getLon() - this.minPosition.getLon()) / 2);
		return new Position(centerLat, centerLon);
	}

	/**
	 * Check if a given Position is within the BoundingBox
	 * @param pos required coordinate to test
	 * @return boolean true if located within the BoundingBox, false if located outside of the BoundingBox
	 */
	public boolean contains(Position pos) {
		if (pos.getLat() > this.minPosition.getLat() &&
			   pos.getLon() > this.minPosition.getLon() &&
			   pos.getLat() < this.maxPosition.getLat() &&
			   pos.getLon() < this.maxPosition.getLon()) {
			return true;
		} else {
			return false;
		}
	}
	@Override
	public String toString(){
		return " min: "+minPosition.toString()+" max: "+maxPosition.toString();
	}
}
