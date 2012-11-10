/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.map;

import com.decarta.CONFIG;
import com.decarta.Profile;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYDouble;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.MapWord;

/**
 * Thread class for loading tiles
 */
public class MapTextThread extends Thread {
	//private final static int FAST_LOAD_THREAD_MAX=5;
	
	private static boolean stop=false;
	private MapEngine mapEngine=null;
	public static long time = 0;
	                    
	public static void startThread(){
		stop=false;
		LogWrapper.i("MapTextThread","startAllThreads, tile thread count:"+CONFIG.TILE_THREAD_COUNT);
	}
	public static void stopThread(){
		stop=true;
	}
	
	private TilesView tilesView;
	private boolean blocked=true;
	
	public MapTextThread(TilesView tilesView) {
		this.tilesView=tilesView;
		this.mapEngine = MapEngine.getInstance();
	}
	
	public boolean isBlocked() {
		return blocked;
	}
	
	
	/**
	 * main entry method for loading tiles
	 */
	@Override
	public void run() {
		//MapActivity mapActivity=(MapActivity)(tilesView.getContext());
		
		while (true) {
			if(stop){
				LogWrapper.i("MapTextThread","thread break");
				break;
			}
			MapText mapText=tilesView.getMapText();
			
			/*if(mapActivity.networkType==ConnectivityManager.TYPE_WIFI && sequence>=FAST_LOAD_THREAD_MAX){
				synchronized(mapActivity.networkLock){
					try{
						Log.i("Thread","MapTextThread"+sequence+" before wait on networkLock");
						requestTiles[sequence]=null;
						mapActivity.networkLock.wait();
						Log.i("Thread","MapTextThread"+sequence+" after wait on networkLock");
						
					}catch(Exception e){
						
					}
				}
			}*/
			
			synchronized(mapText){
				if(mapText.screenTextGetting == false){
					try{
						//Log.i("MapTextThread",""+sequence+" before tilesWaitForLoading.wait");
						blocked=true;
						mapText.wait();
						//Log.i("MapTextThread",""+sequence+" after tilesWaitForLoading.wait");
						
					}catch(Exception e){
						
					}
				}
				blocked=false;
				
			}
			
			if(stop){
				LogWrapper.i("MapTextThread","thread break");
				break;
			}
						
			try {					
				//Log.i("MapTextThread" + this.sequence,"loading tile from network:" + Util.formatURL(url));
				try{
				    MapWord[] mapWords = null;
				    XYDouble mercXYGetting = null;
				    int zoomLevelGetting;
                    synchronized(tilesView.getDrawingLock()){
					    mercXYGetting = new XYDouble(mapText.mercXYGetting.x, mapText.mercXYGetting.y);
					    zoomLevelGetting = mapText.zoomLevelGetting;
                    }
                    if (mercXYGetting != null) {
    				    Position position = Util.mercPixToPos(mercXYGetting, zoomLevelGetting);
    				    if (Util.inChina(position)) {
                            mapWords = mapEngine.getScreenLabel(position, mapText.canvasSize.x, mapText.canvasSize.y, zoomLevelGetting);
    				    }
                        synchronized(tilesView.getDrawingLock()){
                            mapText.getMercXY().x = mercXYGetting.x;
                            mapText.getMercXY().y = mercXYGetting.y;
                            mapText.setZoomLevel(zoomLevelGetting);
                            mapText.setMapWords(mapWords);
                            mapText.screenTextGetting = false;
                            mapText.texImageChanged = true;
                            time = System.currentTimeMillis();
                        }
                        tilesView.refreshMap();
                    }
				}catch(OutOfMemoryError e){
					LogWrapper.e("MapTextThread","getTile from network outOfMemoryError,heap size:"+android.os.Debug.getNativeHeapAllocatedSize());
					continue;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
