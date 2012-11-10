package com.decarta.android.map;

import com.decarta.android.exception.APIException;

/**
 * 
 * @author zsong
 *
 */
public abstract class Shape {
    public static final String MY_LOCATION = "my_location";
    public static final String TRAFFIC_SHAPE = "traffic";
    public static final String LINE_SHAPE = "line";
    public static final String HIGHLIGHT_SHAPE = "highlight";
	public static final int ZOOM_LEVEL=13;
	
	protected String name=null;
	protected boolean visible=true;
	
	public Shape(String shapeName) throws APIException{
		if(shapeName==null || !shapeName.matches("^[a-zA-Z_][a-zA-Z_0-9]*$")){
			throw new APIException("shapeName must be composed of [a-zA-Z_0-9] and begin with [a-zA-Z_]");
		}
		this.name=shapeName;
	}

	public String getName() {
		return name;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	
}
