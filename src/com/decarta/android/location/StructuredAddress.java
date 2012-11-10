/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.location;


/**
 * A structured address is an address broken into its logical parts
 */
public class StructuredAddress extends Address {

	private static final long serialVersionUID = 1L;
	private String buildingNumber;
	private String street;
	private String countrySubdivision;
	private String countrySecondarySubdivision;
	private String municipality;
	private String postalCode;
	private String municipalitySubdivision;
	private String postedSpeedLimit;
	
	public StructuredAddress(){
		
	}
	public StructuredAddress(String buildingNumber, String street, String countrySubdivision, String countrySecondarySubdivision,String municipality, String postalCode, String municipalitySubdivision){
		this.buildingNumber=buildingNumber;
		this.street=street;
		this.countrySubdivision=countrySubdivision;
		this.countrySecondarySubdivision=countrySecondarySubdivision;
		this.municipality=municipality;
		this.postalCode=postalCode;
		this.municipalitySubdivision=municipalitySubdivision;
		
	}
	
	@Override
	public String toString() {
		//return nvl(buildingNumber) + " " + nvl(street) + " " + nvl(municipality) + " " + nvl(countrySubdivision) + " " + nvl(postalCode);
		return formatAddress(" ", " ");
	}
	
	/**
	 * format address using lineDelim between street and city, fldDelim between city, state
	 * @param lineDelim
	 * @param fldDelim
	 * @return
	 */
	public String formatAddress(String lineDelim, String fldDelim){
		if(fldDelim==null) fldDelim=" ";
		StringBuilder str=new StringBuilder();
		if(valid(street)){
			if(valid(buildingNumber)){
				str.append(buildingNumber);
				str.append(" ");
			}
			str.append(street);
			if(valid(municipalitySubdivision) || valid(municipality) || valid(countrySubdivision) || valid(postalCode)){
				str.append(lineDelim);
			}
		}
		
		String strPre="";
		if(valid(municipalitySubdivision)){
			str.append(municipalitySubdivision);
			str.append(fldDelim);
			strPre=municipalitySubdivision;
		}
		
		if(valid(municipality) && !municipality.equalsIgnoreCase(strPre)){
			str.append(municipality);
			str.append(fldDelim);
			strPre=municipality;
		}
		
		if(valid(countrySecondarySubdivision) 
				&& !valid(municipality)
				&& !countrySecondarySubdivision.equalsIgnoreCase(strPre)){
			str.append(countrySecondarySubdivision);
			str.append(fldDelim);
			strPre=countrySecondarySubdivision;
		}
		
		if(valid(countrySubdivision) && !countrySubdivision.equalsIgnoreCase(strPre)){
			str.append(countrySubdivision);
			str.append(fldDelim);
			strPre=countrySubdivision;
		}
		
		if(valid(postalCode)){
			str.append(postalCode);
		}
		
		if(strPre.endsWith(fldDelim)){
			return strPre.substring(0, strPre.lastIndexOf(fldDelim));
		}
		
		return str.toString();
	}
	
	public String formatAddress(String delim){
		//return nvl(buildingNumber) + " " + nvl(street) + delim + nvl(municipality) + " " + nvl(countrySubdivision) + " " + nvl(postalCode);
		return formatAddress(delim, " ");
	}
	
	@Override
	public String formatAddress() {
		// TODO Auto-generated method stub
		
		return formatAddress("\n", " ");
	}
	
	private boolean valid(String in){
		return in!=null && in.length()>0;
	}
	
	/** 
	Address number for this location.
	 */
	public String getBuildingNumber() {
		return buildingNumber;
	}
	/** 
	Address number for this location.
	 */
	public void setBuildingNumber(String buildingNumber) {
		this.buildingNumber = buildingNumber;
	}
	/** 
	County (or equivalent) for this location.
	 */
	public String getCountrySecondarySubdivision() {
		return countrySecondarySubdivision;
	}
	/** 
	County (or equivalent) for this location.
	 */
	public void setCountrySecondarySubdivision(String countrySecondarySubdivision) {
		this.countrySecondarySubdivision = countrySecondarySubdivision;
	}
	/**
	Sub-country administrative division (ie the state, province, or region) for this location.
	 */
	public String getCountrySubdivision() {
		return countrySubdivision;
	}
	/**
	Sub-country administrative division (ie the state, province, or region) for this location.
	 */
	public void setCountrySubdivision(String countrySubdivision) {
		this.countrySubdivision = countrySubdivision;
	}
	/** 
	City, town, village, or equivalent for this location.
	 */
	public String getMunicipality() {
		return municipality;
	}
	/** 
	City, town, village, or equivalent for this location.
	 */
	public void setMunicipality(String municipality) {
		this.municipality = municipality;
	}
	/**
	Recognized neighborhood, borough, or equivalent for this location.
	 */
	public String getMunicipalitySubdivision() {
		return municipalitySubdivision;
	}
	/**
	Recognized neighborhood, borough, or equivalent for this location.
	 */
	public void setMunicipalitySubdivision(String municipalitySubdivision) {
		this.municipalitySubdivision = municipalitySubdivision;
	}
	/** 
	Postal code, postcode, ZIP code, or equivalent numerical code for this location.
	 */
	public String getPostalCode() {
		return postalCode;
	}
	/** 
	Postal code, postcode, ZIP code, or equivalent numerical code for this location.
	 */
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
	/**
	Name and designation of the street (ie Main St) for this location.
	 */
	public String getStreet() {
		return street;
	}
	/**
	Name and designation of the street (ie Main St) for this location.
	 */
	public void setStreet(String street) {
		this.street = street;
	}
	
	public boolean isCompleteAddress(){
		return buildingNumber!=null && !buildingNumber.equals("")
		&& street!=null && !street.equals("")
		&& municipality !=null && !municipality.equals("")
		&& countrySubdivision!=null && !countrySubdivision.equals("")
		;
		
	}
	
	public void setPostedSpeedLimit(String postedSpeed) {
		this.postedSpeedLimit = postedSpeed;
	}
	public String getPostedSpeedLimit() {
		return postedSpeedLimit;
	}
	
	
}
