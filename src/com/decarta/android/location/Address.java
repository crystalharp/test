/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.location;

import java.io.Serializable;

import com.decarta.android.util.Locale;

/**
 * Super class for Address Types.
 */
public abstract class Address implements Serializable{
	
	private static final long serialVersionUID = 1L;
	protected Locale locale = new Locale("US","EN");
	
	public Address(){}
	
	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}
	
	public abstract String formatAddress();
	
	@Override
	public abstract String toString();

}
