/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.location;


/**
 * Represents one line free form address
 */
public class FreeFormAddress extends Address {

	private static final long serialVersionUID = 1L;
	private String freeFormAddress;

	public FreeFormAddress(String freeFormAddress) {
		this.freeFormAddress = freeFormAddress;
	}
	
	@Override
	public String toString(){
		return  formatAddress();
	}

	public String getFreeFormAddress() {
		return freeFormAddress;
	}
	
	public void setFreeFormAddress(String freeFormAddress) {
		this.freeFormAddress = freeFormAddress;
	}
	
	@Override
	public String formatAddress() {
		// TODO Auto-generated method stub
		return freeFormAddress;
	}
}
