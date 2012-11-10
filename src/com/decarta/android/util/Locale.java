/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.util;

import java.io.Serializable;

import com.decarta.android.location.FreeFormAddress;

/**
 * A Locale object represents a specific geographical, political, or cultural
 * region. An operation that requires a Locale to perform its task is called
 * locale-sensitive and uses the Locale to tailor information for the user.
 * Currently, Locales can be attached to a {@link FreeFormAddress} object to aid
 * in producing better results with a {@link Geocoder#geocode}.
 * <p>
 * The current valid pairs of language and country codes are as follows:
 * </p>
 * <ul>
 * <li>Language Code: "DE"; Country Code: "AT"</li>
 * <ul>
 * <li>for German in Austria</li>
 * </ul>
 * <li>Language Code: "EN"; Country Code: "CA"</li>
 * <ul>
 * <li>for English in Canada</li>
 * </ul>
 * <li>Language Code: "FR"; Country Code: "CA"</li>
 * <ul>
 * <li>for French in Canada</li>
 * </ul>
 * <li>Language Code: "DE"; Country Code: "DE"</li>
 * <ul>
 * <li>for German in Germany</li>
 * </ul>
 * <li>Language Code: "ES"; Country Code: "ES"</li>
 * <ul>
 * <li>for Spanish in Spain</li>
 * </ul>
 * <li>Language Code: "FR"; Country Code: "FR"</li>
 * <ul>
 * <li>for French in France</li>
 * </ul>
 * <li>Language Code: "EN"; Country Code: "GB"</li>
 * <ul>
 * <li>for English in Great Britain</li>
 * </ul>
 * <li>Language Code: "EN"; Country Code: "IE"</li>
 * <ul>
 * <li>for English in Ireland</li>
 * </ul>
 * <li>Language Code: "IT"; Country Code: "IT"</li>
 * <ul>
 * <li>for Italian in Italy</li>
 * </ul>
 * <li>Language Code: "EN"; Country Code: "US"</li>
 * <ul>
 * <li>for English in the United States</li>
 * </ul>
 * </ul>
 * <p>
 * These pairs of language can be used to create a valid locale for use with the
 * Geocoder.
 * </p>
 */
public class Locale implements Serializable {

	private static final long serialVersionUID = 1L;
	private String countryCode;
	private String LanguageCode;

	public Locale(String countryCode, String LanguageCode) {
		this.countryCode = countryCode;
		this.LanguageCode = LanguageCode;
	}

	public String getLanguageCode() {
		return LanguageCode;
	}

	public void setLanguageCode(String LanguageCode) {
		this.LanguageCode = LanguageCode;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

}
