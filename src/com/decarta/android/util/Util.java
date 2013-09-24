/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.decarta.CONFIG;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.BoundingBox;
import com.decarta.android.location.Position;
import com.decarta.android.map.MapLayer.MapLayerProperty;
import com.decarta.android.map.MapLayer.MapLayerType;
import com.decarta.android.map.Tile;
import com.decarta.android.map.TileGridResponse;
import com.decarta.android.scale.Length;
import com.decarta.android.scale.Length.UOM;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.TypedValue;

public class Util {

	public static String[] llmaxList = {
			"0.000345636472797214,0.000343322753906250",
			"0.000691272945568983,0.000686645507812500",
			"0.001382546303032519,0.001373291015625000",
			"0.002765092605263539,0.002746582031250000",
			"0.005530185203987857,0.005493164062500000",
			"0.011060370355776452,0.010986328125000000",
			"0.022120740293895182,0.021972656250000000",
			"0.044241477246363230,0.043945312500000000",
			"0.088482927761462040,0.087890625000000000",
			"0.176965641673330230,0.175781250000000000",
			"0.353929573271679340,0.351562500000000000",
			"0.707845460801532700,0.703125000000000000",
			"1.415581451872543800,1.406250000000000000",
			"2.830287664051185000,2.812500000000000000",
			"5.653589942659626000,5.625000000000000000",
			"11.251819676168665000,11.250000000000000000",
			"22.076741328793200000,22.500000000000000000",
			"41.170427238429790000,45.000000000000000000",
			"66.653475896509040000,90.00000000000000000",
			"85.084059050110410000,180.00000000000000000",
			"89.787438015348100000,360.00000000000000000" };
	
	public static long MERC_X_MODS[]=new long[21];
	public static int INDEX_X_MODS[]=new int[21];
	public static void init(){
		for(int i=0;i<21;i++){
			MERC_X_MODS[i]=CONFIG.TILE_SIZE<<i;
			INDEX_X_MODS[i]=1<<i;
			//Log.d("Util","zoom,mercX,indexX:"+i+","+MERC_X_MODS[i]+","+INDEX_X_MODS[i]);
		}
    }
	
	public static double mercXMod(double x,int z){
		long mod = MERC_X_MODS[z];
		long mod2 = mod >> 1;
		if(x < mod2 && x >= -mod2) return x;
		return (((long)x + mod2) % mod + mod) % mod - mod2 + (x - (long)x);
	}
	
	public static int indexXMod(int x,int z){
		int mod = INDEX_X_MODS[z];
		int mod2 = mod >> 1;
		if(x < mod2 && x >= -mod2) return x;
		return ((x + mod2) % mod + mod) % mod - mod2;
	}

	public static final double EARTH_RADIUS_METERS = 6371000.000000;

	/**
	 * 
	 * @param positions
	 * 
	 */
	public static BoundingBox getBoundingBoxToFitPositions(ArrayList<Position> positions) {
		Position position;
		double minLat = positions.get(0).getLat();
		double maxLat = positions.get(0).getLat();
		double minLon = positions.get(0).getLon();
		double maxLon = positions.get(0).getLon();
		for (int i = 1; i < positions.size(); i++) {
			position = positions.get(i);
			if (position.getLon() > maxLon) {
				maxLon = position.getLon();
			} else if(position.getLon()<minLon){
				minLon = position.getLon();
			}
			if (position.getLat() > maxLat) {
				maxLat = position.getLat();
			} else if(position.getLat()<minLat){
				minLat = position.getLat();
			}
		}
		return new BoundingBox(new Position(minLat, minLon), new Position(
				maxLat, maxLon));
	}

	/**
	 * 
	 * @param screenX
	 *            (screen width)
	 * @param screenY
	 *            (screen height)
	 * @param positions
	 * 
	 */
	public static int getZoomLevelToFitPositions(int screenX, int screenY,
			ArrayList<Position> positions) throws APIException{
		
		return getZoomLevelToFitBoundingBox(screenX, screenY, getBoundingBoxToFitPositions(positions));
	}

	/**
	 * 
	 * @param screenX
	 *            (screen width)
	 * @param screenY
	 *            (screen height)
	 * @param boundingBox
	 *            to fit
	 * 
	 */
	public static int getZoomLevelToFitBoundingBox(int screenX, int screenY,BoundingBox boundingBox) throws APIException {
		screenX = Math.abs(screenX / 2);
		screenY = Math.abs(screenY / 2);
		int fitZoom = CONFIG.ZOOM_LOWER_BOUND;
		for (int gxZoom = CONFIG.ZOOM_UPPER_BOUND; gxZoom >= CONFIG.ZOOM_LOWER_BOUND; --gxZoom) {

			double scale = Util.radsPerPixelAtZoom(CONFIG.TILE_SIZE, gxZoom);

			double pixelsY = Util.lat2pix(boundingBox.getCenterPosition().getLat(), scale);
			double pixelsX = Util.lon2pix(boundingBox.getCenterPosition().getLon(), scale);

			double maxlat = Util.pix2lat((int) pixelsY + screenY, scale);
			double maxlon = Util.pix2lon((int) pixelsX + screenX, scale);

			double minlat = Util.pix2lat((int) pixelsY - screenY, scale);
			double minlon = Util.pix2lon((int) pixelsX - screenX, scale);

			BoundingBox gxbbox = new BoundingBox(new Position(minlat, minlon),new Position(maxlat, maxlon));

			if (gxbbox.contains(boundingBox.getMinPosition()) && gxbbox.contains(boundingBox.getMaxPosition())) {
				fitZoom = gxZoom;
				break;
			}
		}
		return fitZoom;
	}


	public static TileGridResponse handlePortrayMapRequest(XYDouble centerXY, int gxZoom) throws APIException{
		TileGridResponse resp = new TileGridResponse();
//	    correctToTKXY(centerXY, gxZoom);

		long pixX = Math.round(centerXY.x);
		long pixY = Math.round(centerXY.y);
		if(pixY>Integer.MAX_VALUE || pixY<Integer.MIN_VALUE){
			throw APIException.INVALID_MERCATOR_Y_INFINITY;
		}
		float offsetPixX = (CONFIG.TILE_SIZE + ((int)pixX % CONFIG.TILE_SIZE)) % CONFIG.TILE_SIZE - CONFIG.TILE_SIZE / 2;
		float offsetPixY = (CONFIG.TILE_SIZE + ((int)pixY % CONFIG.TILE_SIZE)) % CONFIG.TILE_SIZE - CONFIG.TILE_SIZE / 2;
		int fixedSeedColumnIdx = (int) Math.floor((double) pixX / CONFIG.TILE_SIZE);
		int fixedSeedRowIdx = (int) Math.floor((double) pixY / CONFIG.TILE_SIZE);
		resp.setFixedGridPixelOffset(new XYFloat(-offsetPixX, offsetPixY));
		resp.centerXYZ = new XYZ(fixedSeedColumnIdx, fixedSeedRowIdx, gxZoom);
		resp.centerXY=new XYDouble(centerXY.x,centerXY.y);
		
		double radius=handleRadius(centerXY, gxZoom);
		resp.setRadiusY(new Length(radius,UOM.M));
		return resp;
	}
	
	public static double handleRadius(XYDouble centerXY,	int gxZoom){
		double scale = Util.radsPerPixelAtZoom(CONFIG.TILE_SIZE, gxZoom);
		double radius = CONFIG.TILE_SIZE / 2 * scale * Util.EARTH_RADIUS_METERS;
		radius *= Math.cos(pix2lat(centerXY.y, scale) * Math.PI / 180);
		return radius;
	}
	
	public static XYInteger mercXYToNE(XYDouble mercXY){
		int e = (int) Math.floor(mercXY.x / CONFIG.TILE_SIZE);
		int n = (int) Math.floor(mercXY.y / CONFIG.TILE_SIZE);
		return new XYInteger(e,n);
	}
	
	public static void mercXYToNE(XYDouble mercXY, XYInteger result){
		if(mercXY == null || result == null)
			return;
		result.x = (int) Math.floor(mercXY.x / CONFIG.TILE_SIZE);
		result.y = (int) Math.floor(mercXY.y / CONFIG.TILE_SIZE);
	}
	
	/**
     * convert from position to mercator pixels based on current zoom level
     * @param pos
     * 
     */
    public static XYDouble posToMercPix ( final Position pos, float zoomLevel ) throws APIException{
        double scale =  Util.radsPerPixelAtZoom(CONFIG.TILE_SIZE, zoomLevel);
        double y = Util.lat2pix(pos.getLat(), scale);
        double x = Util.lon2pix(pos.getLon(), scale);
        return new XYDouble(x, y );
    }

    /**
     * convert from mercator pixels to position based on current zoom level
     * @param pix
     * 
     */
    public static Position mercPixToPos( XYDouble pix ,float zoomLevel) {
    	double scale =  Util.radsPerPixelAtZoom(CONFIG.TILE_SIZE, zoomLevel);
    	double lat = Util.pix2lat( pix.y, scale );
        double lon = Util.pix2lon( pix.x, scale );
        return new Position( lat, lon );
    }
	/**
	 * This function will return the Mercator Pixel value for a given latitude
	 * and scale level.
	 */
	public static double lat2pix(double lat, double scale) throws APIException{
		if(lat==90 || lat==-90) throw APIException.INVALID_LATITUDE_90;
		double radLat = (lat * Math.PI) / 180;
//		double ecc = 0.08181919084262157;
		double sinPhi = Math.sin(radLat);
//		double eSinPhi = ecc * sinPhi;
		double retVal = Math.log(((1.0 + sinPhi) / (1.0 - sinPhi))) / 2.0;
		if(Double.isInfinite(retVal) || Double.isInfinite(retVal/scale)){
			throw APIException.INVALID_MERCATOR_Y_INFINITY;
		}
		return (double)(retVal / scale);

	}

	/**
	 * This function will return the Mercator Pixel value for a given longitude
	 * and scale level.
	 */
	public static double lon2pix(double lon, double scale) {
		return (double)(((lon / 180) * Math.PI) / scale);
	}

	/**
	 * Compute the radians per pixel at the zoom level.
	 */
	public static double radsPerPixelAtZoom(int tileSize, float gxZoom) {
		return 2 * Math.PI / (tileSize*Math.pow(2,gxZoom));
	}

	public static double metersPerPixelAtZoom(int tileSize, float gxZoom, double lat) {
		double radsPerPixel = radsPerPixelAtZoom(tileSize, gxZoom);
		return radsPerPixel * EARTH_RADIUS_METERS * Math.cos(lat * Math.PI / 180);
	}
	
	/**
	 * This function will return the longitude for a given Mercator Pixel and
	 * scale level.
	 */
	public static double pix2lon(double x, double scale) {
		return (x * scale) * 180 / Math.PI;
	}

	/**
	 * This function will return the latitude for a given Mercator Pixel and
	 * scale level.
	 */
	public static double pix2lat(double y, double scale) {
//		double phiEpsilon = 1E-7;
//		double phiMaxIter = 12;
		double t = Math.pow(Math.E, -y * scale);
		double prevPhi = Util.mercatorUnproject(t);
//		double newPhi = Util.findRadPhi(prevPhi, t);
//		double iterCount = 0;
//		while (iterCount < phiMaxIter && Math.abs(prevPhi - newPhi) > phiEpsilon) {
//			prevPhi = newPhi;
//			newPhi = Util.findRadPhi(prevPhi, t);
//			iterCount++;
//		}
		return prevPhi * 180 / Math.PI;
	}

	private static double mercatorUnproject(double t) {
		return (Math.PI / 2) - 2 * Math.atan(t);
	}

	public static double findRadPhi(double phi, double t) {
		double ecc = 0.08181919084262157;
		double eSinPhi = ecc * Math.sin(phi);
		return (Math.PI / 2) - (2 * Math.atan(t	* Math.pow((1 - eSinPhi) / (1 + eSinPhi), ecc / 2)));
	}
	
	public static List<XYZ> findOverlapXYZs(XYZ xyz, int zoomLevel){
		ArrayList<XYZ> xyzs = new ArrayList<XYZ>();
		if(zoomLevel < xyz.z){
			float scale=((float)1)/(1<<(xyz.z-zoomLevel));
			xyzs.add(new XYZ((int)Math.floor(xyz.x*scale),(int)Math.floor(xyz.y*scale),zoomLevel));
		} else {
			int expand = 1<<(zoomLevel-xyz.z);
			if(expand > 1) {
				int debug = 0;
				++debug;
			}
			for(int x = xyz.x * expand, boundx = (xyz.x + 1) * expand; x < boundx; ++x){
				for(int y = xyz.y * expand, boundy = (xyz.y+1) * expand; y < boundy; ++y){
					xyzs.add(new XYZ(x, y, zoomLevel));
				}
			}
		}
		return xyzs;
		
	}
	
	public static List<XYInteger> findOverlapXYs(XYZ xyz, int zoomLevel){
		ArrayList<XYInteger> xys=new ArrayList<XYInteger>();
		if(zoomLevel<xyz.z){
			float scale=((float)1)/(1<<(xyz.z-zoomLevel));
			xys.add(new XYInteger((int)Math.floor(xyz.x*scale),(int)Math.floor(xyz.y*scale)));
		}else{
			int expand=1<<(zoomLevel-xyz.z);
			for(int x=xyz.x*expand;x<(xyz.x+1)*expand;x++){
				for(int y=xyz.y*expand;y<(xyz.y+1)*expand;y++){
					xys.add(new XYInteger(x,y));
				}
			}
		}
		return xys;
		
	}
	
	public static int getPower2(float size){
		int r=1;
		while(r<size){
			r = (r << 1);
		}
		return r;
	}
		
	public static ArrayList<Tile> getTouchTiles(XYDouble mercXY,int z, double radius){
		ArrayList<Tile> tiles=new ArrayList<Tile>();
		int er=(int)Math.floor((mercXY.x+radius)/CONFIG.TILE_SIZE);
		int el=(int)Math.floor((mercXY.x-radius)/CONFIG.TILE_SIZE);
		int nt=(int)Math.floor((mercXY.y+radius)/CONFIG.TILE_SIZE);
		int nb=(int)Math.floor((mercXY.y-radius)/CONFIG.TILE_SIZE);
		for(int i=nb;i<=nt;i++){
			for(int j=el;j<=er;j++){
				Tile tile=new Tile(MapLayerProperty.getInstance(MapLayerType.STREET));
				tile.xyz.x=j;
				tile.xyz.y=i;
				tile.xyz.z=z;
				tiles.add(tile);
				
			}
		}
		
		return tiles;
	}
	
	public static BoundingBox getTileBoundingBox(Tile tile){
		double mercX=tile.xyz.x*CONFIG.TILE_SIZE;
		double mercY=tile.xyz.y*CONFIG.TILE_SIZE;
		Position minPos=mercPixToPos(new XYDouble(mercX,mercY), tile.xyz.z);
		mercX+=CONFIG.TILE_SIZE;
		mercY+=CONFIG.TILE_SIZE;
		Position maxPos=mercPixToPos(new XYDouble(mercX,mercY), tile.xyz.z);
		return new BoundingBox(minPos,maxPos);
	}
	
	public static boolean coveredByTiles(XYZ xyz, int blX, int blY, int trX, int trY, int z, HashSet<XYZ> xyzs){
		boolean covered=true;
		if(z<=xyz.z){
			float scale=((float)1)/(1<<(xyz.z-z));
			xyz.x=Util.indexXMod(xyz.x, xyz.z);
			XYZ coverXYZ=new XYZ((int)Math.floor(xyz.x*scale),(int)Math.floor(xyz.y*scale),z);
			covered= xyzs.contains(coverXYZ);
		}else{
			int expand=1<<(z-xyz.z);
			int xmin=xyz.x*expand;
			int xmax=(xyz.x+1)*expand-1;
			int ymin=xyz.y*expand;
			int ymax=(xyz.y+1)*expand-1;
			if(xmin<blX) xmin=blX;
			if(xmax>trX) xmax=trX;
			if(ymin<blY) ymin=blY;
			if(ymax>trY) ymax=trY;
			
			if(xmin>xmax || ymin>ymax){
				LogWrapper.e("Util", "coveredByTiles xyz invisible,blX,blY,trX,trY,z:"+xyz.toString()+","+blX+","+blY+","+trX+","+trY+","+z);
				return true;
			}
			for(int i=ymin;i<=ymax;i++){
				for(int j=xmin;j<=xmax;j++){
					XYZ coverXYZ=new XYZ(j,i,z);
					coverXYZ.x=Util.indexXMod(coverXYZ.x,coverXYZ.z);
					if(!xyzs.contains(coverXYZ)){
						covered= false;
						break;
					}
				}
				if(!covered) break;
			}
			
		}
		return covered;
			
	}

    public static int getZoomLevelToFitPositions(int screenX, int screenY, int padding, ArrayList<Position> positions, Position screenCenter) throws APIException {
        if (positions == null || positions.isEmpty()) {
            throw new APIException("positions is null");
        }
		BoundingBox boundingBox = null;
        if (screenCenter != null) {
            double maxLatDelta = 0;
            double maxLonDelta = 0;
    		for (Position position: positions) {
    			if (Math.abs(position.lat - screenCenter.lat) > maxLatDelta) {
    				maxLatDelta = Math.abs(position.lat - screenCenter.lat);
    			} 
    			if(Math.abs(position.lon - screenCenter.lon) > maxLonDelta) {
    				maxLonDelta = Math.abs(position.lon - screenCenter.lon);
    			}
    		}
            Position max = new Position(screenCenter.lat + maxLatDelta, screenCenter.lon + maxLonDelta);
            Position min = new Position(screenCenter.lat - maxLatDelta, screenCenter.lon - maxLonDelta);
            boundingBox = new BoundingBox(max, min);
        }
        else {
            boundingBox = getBoundingBoxToFitPositions(positions);
        }
        
        if (boundingBox != null) {
            return Util.getZoomLevelToFitBoundingBox(screenX, screenY, padding, boundingBox);
        } else {
            throw new APIException("boundingBox is null");
        }
    }
    /**
     * 
     * 画交通图层时, 需要考虑
     * 1. 让图标也全部显示在屏幕内
     * 2. 交通图层不会被title盖住
     * 
     * @param screenX
     *            (screen width)
     * @param screenY
     *            (screen height)
     * @param halfIconSize
     *            (half of icon size)
     * @param boundingBox
     *            to fit
     * 
     */
    public static int getZoomLevelToFitBoundingBox(int screenX, int screenY, int padding, BoundingBox boundingBox) throws APIException {
//    	LogWrapper.d("centerdebug", "fit box sx: " + screenX + ", sy: " + screenY);
        int fitZoom = CONFIG.ZOOM_LOWER_BOUND;
        for (int gxZoom = CONFIG.ZOOM_UPPER_BOUND; gxZoom >= CONFIG.ZOOM_LOWER_BOUND; --gxZoom) {

            double scale = Util.radsPerPixelAtZoom(CONFIG.TILE_SIZE, gxZoom);
            double maxPixelY = Util.lat2pix(boundingBox.getMaxPosition().getLat(), scale);
            double maxPixelX = Util.lon2pix(boundingBox.getMaxPosition().getLon(), scale);
            double minPixelX = Util.lon2pix(boundingBox.getMinPosition().getLon(), scale);
            boundingBox.setMaxPosition(
            		new Position(Util.pix2lat(maxPixelY + padding, scale), Util.pix2lon(maxPixelX + (padding>>1), scale)));
            boundingBox.setMinPosition(
            		new Position(boundingBox.getMinPosition().getLat(), Util.pix2lon(minPixelX - (padding>>1), scale)));

            Position centerPosition = boundingBox.getCenterPosition();
            double pixelsY = Util.lat2pix(centerPosition.getLat(), scale);
            double pixelsX = Util.lon2pix(centerPosition.getLon(), scale);
            double maxlat = Util.pix2lat(pixelsY + (screenY>>1), scale);
            double maxlon = Util.pix2lon(pixelsX + (screenX>>1), scale);
            double minlat = Util.pix2lat(pixelsY - (screenY>>1), scale);
            double minlon = Util.pix2lon(pixelsX - (screenX>>1), scale);
            BoundingBox gxbbox = new BoundingBox(new Position(minlat, minlon),new Position(maxlat, maxlon));

            if (gxbbox.contains(boundingBox.getMinPosition()) && gxbbox.contains(boundingBox.getMaxPosition())) {
                fitZoom = gxZoom;
                break;
            } 
        }
        
        if (fitZoom == CONFIG.ZOOM_JUMP) {
            fitZoom = CONFIG.ZOOM_JUMP - 1;
        }
        
        return fitZoom;
    }
    
    public static XYDouble xyzToMercPix(XYZ xyz) {
        double x = xyz.x * CONFIG.TILE_SIZE;
        double y =xyz.y * CONFIG.TILE_SIZE;
        XYDouble mercPix = new XYDouble(x, y);
        return mercPix;
    }

    /**
     * 为当前屏幕可见Tiles加上屏幕上方及屏幕下方的Tile
     * (为绕过BUG而写, 后续当解决BUG并去除)
     * 
     * @param origs 当前屏幕可见Tiles的Tile坐标
     * @return
     */
    public static List<XYZ> getVerticalLargerOverlapTiles(List<XYZ> origs) {
        if (origs == null || origs.size() == 0) {
            return null;
        }
        
        ArrayList<XYZ> xyzs=new ArrayList<XYZ>();
        xyzs.addAll(origs);
        
        int minY = origs.get(0).y;
        int maxY = origs.get(0).y;
        int minX = origs.get(0).x;
        int maxX = origs.get(0).x;
        
        for (XYZ xyz : origs) {
            if (xyz.y < minY) {
                minY = xyz.y;
            }
            if (xyz.y > maxY) {
                maxY = xyz.y;
            }
            
            if (xyz.x < minX) {
                minX = xyz.x;
            }
            if (xyz.x > maxX) {
                maxX = xyz.x;
            }
        }
        
        for (XYZ xyz : origs) {
            if (xyz.y == minY) {
                xyzs.add(new XYZ(xyz.x, xyz.y - 1, xyz.z));
            }
            if (xyz.y == maxY) {
                xyzs.add(new XYZ(xyz.x, xyz.y + 1, xyz.z));
            }
            
            if (xyz.x == minX) {
                xyzs.add(new XYZ(xyz.x - 1, xyz.y, xyz.z));
            }
            if (xyz.x == maxX) {
                xyzs.add(new XYZ(xyz.x + 1, xyz.y, xyz.z));
            }
        }
        return xyzs;
    }
    
    /*
    int tk_get_tile_xy(double lat, double lon, int zoom, int *min_tile_x, int *min_tile_y) {
        int temp_x;
        int temp_y;
        struct envelope el;
    
        if (zoom < MIN_Z || zoom > MAX_Z) {
            LOG_INFO("Zoom level must be between 5 and 18: %d\n", zoom);
            return -1;
        }
        if (lon < (MIN_LON) || lon > (MAX_LON)) {
            LOG_INFO("longitude is out of range: %f\n", lon);
            return -1;
        }
        if (lat < (MIN_LAT) || lat > (MAX_LAT)) {
            LOG_INFO("latitude is out of range: %f\n", lat);
            return -1;
        }
    
        temp_x = tk_lon_to_x(lon);
        temp_y = tk_lat_to_y(lat);
    
        if (temp_x < 0 || temp_y < 0) {
            return -1;
        }
        if (zoom <= 16) {
            *min_tile_x = temp_x >> (8 + TK_BASE_LEVEL_A - zoom);
            *min_tile_y = temp_y >> (8 + TK_BASE_LEVEL_A - zoom);
        } else {
            *min_tile_x = temp_x >> (8 + (TK_BASE_LEVEL_A - zoom));  
            *min_tile_y = temp_y >> (8 + (TK_BASE_LEVEL_A - zoom));
        }
    }


    static unsigned int tk_lon_to_x(double lon) 
    {
        return (int)floor(TK_ABS(-(lon + 180) / 360 * (1 << GSCALE_FACTOR) * TILE_SIZE));
    }
    
    static unsigned int tk_lat_to_y(double lat)
    {
        double lat_in_rad, y_temp, y;
    
        lat_in_rad = lat * (MATH_PI) / 180;
        y_temp = (sin(lat_in_rad) + 1) / -(sin(lat_in_rad) - 1);
        y = -(log(y_temp) / (MATH_PI * 2) - 1) * (1 << (GSCALE_FACTOR - 1)) * TILE_SIZE;
    
        return (int)floor(y);
    }
    */
//
//    private static final int GSCALE_FACTOR = 16;
//
//    private static int tk_lon_to_x(double lon) {
//        return (int)Math.floor(Math.abs(-(lon + 180) / 360 * (1 << GSCALE_FACTOR)
//                * CONFIG.TILE_SIZE));
//    }
//
//    private static int tk_lat_to_y(double lat) {
//        double lat_in_rad, y_temp, y;
//
//        lat_in_rad = lat * (Math.PI) / 180;
//        y_temp = (Math.sin(lat_in_rad) + 1) / -(Math.sin(lat_in_rad) - 1);
//        y = -(Math.log(y_temp) / (Math.PI * 2) - 1) * (1 << (GSCALE_FACTOR - 1)) * CONFIG.TILE_SIZE;
//
//        return (int)Math.floor(y);
//    }

//    private static final double MIN_LAT = -85.5;
//    private static final double MAX_LAT = 85.5;
//    private static final double MIN_LON = -180.0;
//    private static final double MAX_LON = 180.0;
//
//    private static final int TK_BASE_LEVEL_A = 16;
//
//    public static XYZ tk_get_tile_xy(double lat, double lon, int zoom) {
//        int temp_x;
//        int temp_y;
//
//        if (zoom < MIN_Z || zoom > MAX_Z) {
//            LogWrapper.d("Util", "Zoom level must be between 5 and 18: " + zoom);
//            return null;
//        }
//        if (lon < (TK_MIN_LON) || lon > (TK_MAX_LON)) {
//            LogWrapper.d("Util", "longitude is out of range:: " + lon);
//            return null;
//        }
//        if (lat < (TK_MIN_LAT) || lat > (TK_MAX_LAT)) {
//            LogWrapper.d("Util", "latitude is out of range:: " + lat);
//            return null;
//        }
//
//        temp_x = tk_lon_to_x(lon);
//        temp_y = tk_lat_to_y(lat);
//
//        if (temp_x < 0 || temp_y < 0) {
//            return null;
//        }
//        XYZ xyz = new XYZ(-1, -1, zoom);
//        if (zoom <= 16) {
//            xyz.x = temp_x >> (8 + TK_BASE_LEVEL_A - zoom);
//            xyz.y = temp_y >> (8 + TK_BASE_LEVEL_A - zoom);
//        } else {
//            xyz.x = temp_x >> (8 + (TK_BASE_LEVEL_A - zoom));
//            xyz.y = temp_y >> (8 + (TK_BASE_LEVEL_A - zoom));
//        }
//
//        return xyz;
//    }
    
    public static boolean inChina(XYDouble pix, float zoomLevel) {
        Position position = mercPixToPos(pix, zoomLevel);
        return inChina(position);
    }
    
    public static boolean inChina(Position position) {
        boolean inChina = true;
        if (position == null || 
                position.getLat() < CONFIG.TK_MIN_LAT || 
                position.getLat() > CONFIG.TK_MAX_LAT ||
                position.getLon() < CONFIG.TK_MIN_LON ||
                position.getLon() > CONFIG.TK_MAX_LON) {
            inChina = false;
        }
        return inChina;
    }
    
    /**
     * @param z
     * @throws APIException 
     * @throws IllegalArgumentException
     */
    private static void checkZ(int z) throws APIException{
        if (z < CONFIG.ZOOM_LOWER_BOUND || z > CONFIG.ZOOM_UPPER_BOUND) 
            throw new APIException("invalid zoomLevel:"+z+", must between "+CONFIG.ZOOM_LOWER_BOUND+"-"+CONFIG.ZOOM_UPPER_BOUND);
    }
    
    /**
     * @param tileNumber
     * @param zoomLevel
     * @throws APIException 
     * @throws IllegalArgumentException
     */
    private static void checkXYZ(int tileNumber, int zoomLevel) throws APIException{
        checkZ(zoomLevel);    
        
        if (tileNumber < 0 || tileNumber > (1 << zoomLevel)) 
            throw new APIException("invalid tilex/y:"+tileNumber+", must between "+1+"-"+(2^zoomLevel));       
    }
    
    /**
     * @param lon
     * @param zoomLevel
     * @throws APIException 
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("unused")
	private static void checkLon(double lon, int zoomLevel) throws APIException{
        checkZ(zoomLevel);
        
        if (lon < CONFIG.TK_MIN_LON || lon > CONFIG.TK_MAX_LON) {
            throw new APIException("invalid lon:"+lon+", must between "+CONFIG.TK_MIN_LON+"-"+CONFIG.TK_MAX_LON);
        }
    }
   
    /**
     * @param lat
     * @param zoomLevel
     * @throws APIException 
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("unused")
	private static void checkLat(double lat, int zoomLevel) throws APIException{
        checkZ(zoomLevel);
        
        if(lat < CONFIG.TK_MIN_LAT || lat > CONFIG.TK_MAX_LAT)
            throw new APIException("invalid lat:"+lat+", must between "+CONFIG.TK_MIN_LAT+"-"+CONFIG.TK_MAX_LAT);
    }

    public static void writeBitmap2Uri(Context context, Uri uri, Bitmap bitmap) {
        OutputStream outputStream = null;
        try {
            outputStream = context.getContentResolver().openOutputStream(uri);
            bitmap.compress(CompressFormat.JPEG, 100, outputStream);
        } catch (IOException ex) {
            // ignore exception
        } finally {
            closeSilently(outputStream);
        }
    }

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }

    public static void closeSilently(ParcelFileDescriptor c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }
    
    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */ 
    public static int dip2px(float density, float dpValue) { 
        return (int) (dpValue * density + 0.5f); 
    } 
    
    public static Bitmap decodeResource(Resources resources, int id) {
        TypedValue value = new TypedValue();
        resources.openRawResource(id, value);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inTargetDensity = value.density;
        return BitmapFactory.decodeResource(resources, id, opts);
    }
    
    //将指定byte以16进制的形式打印到控制台  
    public static String byteToHexString(byte b) {     
        String hex = Integer.toHexString(b & 0xFF);   
        if (hex.length() == 1) {   
           hex = '0' + hex;   
        }
        return hex;
    }  

}
