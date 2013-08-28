/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.exception;

/**
 * represents server side exceptions from DDS Web Services.
 */
public class APIException extends Exception {
	private static final long serialVersionUID = 1L;
	
	/**
	 * exception when the latitude is 90, and want to convert it to mercator y coord
	 */
	public static APIException INVALID_LATITUDE_90 = new APIException("invalid latitude for mercator mode: +-90");
	
	/**
	 * exception when latitude is close to 90, and when convert to mercator y coord, it's infinity
	 */
	public static APIException INVALID_MERCATOR_Y_INFINITY=new APIException("invalide latitude to screen coordinate: infinity");

    public static final String RESPONSE_DATA_IS_EMPTY ="response data is null";

    public static final String CRITERIA_IS_NULL ="criteria is null";
    
    private static final String MISSING_REQUEST_PARAMETER = "missing request parameter: %s";
	
	public APIException(String msg){
		super((msg==null)?"":msg);
	}
	
	private APIException(Throwable throwable){
		super(throwable.getMessage()==null ? throwable.getClass().getName() : throwable.getMessage() ,throwable);
	}

    public APIException(Exception cause) {
        super(cause);
    }

    public APIException(String msg, Exception cause) {
        super(msg, cause);
    }
    
	public static APIException wrapToAPIException(Throwable throwable){
		if(throwable instanceof APIException) return (APIException)throwable;
		else return new APIException(throwable);
	}
    
    public static APIException wrapToMissingRequestParameterException(String parameterName){
        return new APIException(String.format(MISSING_REQUEST_PARAMETER, parameterName));
    }
}
