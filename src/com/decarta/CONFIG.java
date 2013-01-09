/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta;

/**
 * Global static CONFIG class 
 * <br/>
 * <br/>
 * <b>You must configure this class with your user credentials for using deCarta web services</b>
 */
public class CONFIG {
	
	/**
	 * TILE_SIZE sets tile image size 
	 * default is 256 x 256 pixels
	 */
	public static int TILE_SIZE=256;
	
	/**
	 * TILE_THREAD_COUNT for tile network thread pool 
	 * default 6
	 */
	public static int TILE_THREAD_COUNT=1;
    
    /**
     * To maximum number of tiles be cached. Oldest tiles will be removed when cached 
     * tiles exceed this limit. If set it to 0, then no cached will be used.
     */
    public static int CACHE_SIZE=1024;
	
	/**
	 * sets the upper and lower bounds of the zoom controller
	 * possible range : 0-20. Bigger number means more ability to zoom in.
	 */
	public static int ZOOM_LOWER_BOUND=5;
	public static int ZOOM_UPPER_BOUND=18;
    public static int ZOOM_JUMP=9;
	
	/**
	 * enable if after pinch zooming, slide to closest integer zoom level
	 */
	public static boolean SNAP_TO_CLOSEST_ZOOMLEVEL=true;
	
	/**
	 * DRAW_BY_OPENGL sets whether to use OpenGL (or Canvas)
	 * for the rendering of the map view. OpenGL rendering will only benefit performance with Graphic 
	 * Processing Units (GPUs)
	 */
	public static boolean DRAW_BY_OPENGL=true;
	/**
	 * specify fading time for map effect of fading in tiles when they load, default to 100 ms
	 */
	public static int FADING_TIME=200;
    public static int TEXT_FADING_TIME=1600;
	
	/**
	 * enable tilt
	 */
	public static boolean ENABLE_TILT=false;
	/**
	 * enable rotate
	 */
	public static boolean ENABLE_ROTATE=false;
	/**
	 * place compass on the corner. The compass's real screen position is decided by the compass's offset
	 * relative to the corner.
	 * -1: no compass, 0: top-left, 1: top-right, 2: bottom-left, 3: bottom-right
	 */
	public static int COMPASS_PLACE_LOCATION=4;
	
	/**
	 * BORDER set the width of border between tiles. This is only for debug purpose.
	 */
	public static int BORDER=0;
	
	/**
	 * Set Log level
	 * 0: no log
	 * 1: error
	 * 2: warn
	 * 3: info
	 * 4: debug
	 */
	public static int LOG_LEVEL=4; 
    
    /**
     * number of messages should be saved in log
     */
    public static int LOG_SIZE=200;
		
	/**
	 * log easing DECELERATE_RATE means speed will become 0.8 times of the previous speed 
	 * every 33 miniseconds, which is approximately one frame.
	 * valid value is (0,1). The bigger, the longer it will ease.
	 */
	public static double DECELERATE_RATE=0.8;
	
	/**
	 * background color for canvas mode
	 */
	public static int BACKGROUND_COLOR_CANVAS=0xfff4f3ef;
	/**
	 * background color for opengl mode
	 */
	public static int BACKGROUND_COLOR_OPENGL=0xfff4f3ef;
	/**
	 * background grid line color
	 */
	public static int BACKGROUND_GRID_COLOR=0xfff4f3ef;
	
    public static final String IMAGE_FORMAT="PNG";
	
    public static final double TK_MIN_LAT = 4;
    public static final double TK_MAX_LAT = 56;
    public static final double TK_MIN_LON = 72;
    public static final double TK_MAX_LON = 140;
}