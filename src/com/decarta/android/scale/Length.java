/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.scale;

import java.io.Serializable;

public class Length implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static enum UOM implements Serializable {
		M, KM, MI;
		public static UOM getInstance(String uom) {
			if ("KM".equals(uom))
				return KM;
			else if ("MI".equals(uom))
				return MI;
			else if ("M".equals(uom))
				return M;
			else
				return M;
		}

		@Override
		public String toString() {
			if (this.equals(MI)) {
				return "MI";
			} else if (this.equals(KM)) {
				return "KM";
			} else {
				return "M";
			}
		}
	};

	private double distance;
	private UOM uom;

	public Length(double distance, UOM uom) {
		this.distance = distance;
		this.uom = uom;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public UOM getUom() {
		return uom;
	}

	public void setUom(UOM uom) {
		this.uom = uom;
	}

	public double toMeters() {
		if (uom.equals(UOM.M))
			return distance;
		else if (uom.equals(UOM.KM))
			return distance * 1000;
		else if (uom.equals(UOM.MI))
			return distance * 1609.344;
		else
			return 0;
	}

	@Override
	public String toString() {
		return distance + uom.toString();
	}

}
