/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.map;

import java.util.LinkedList;

import com.decarta.CONFIG;
import com.decarta.Profile;
import com.decarta.android.map.TilesView.TileResponse;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.XYZ;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.TileDownload;
import com.tigerknows.map.MapView.DownloadEventListener;

/**
 * Thread class for loading tiles
 */
public class TileThread extends Thread {
	//private final static int FAST_LOAD_THREAD_MAX=5;
	
	private static boolean stop=false;
	private static Tile[] requestTiles=null;
	private MapEngine mapEngine=null;
	                    
	public static void startAllThreads(){
		requestTiles=new Tile[CONFIG.TILE_THREAD_COUNT];
		stop=false;
		LogWrapper.i("TileThread","startAllThreads, tile thread count:"+CONFIG.TILE_THREAD_COUNT);
	}
	public static void stopAllThreads(){
		stop=true;
	}
	
	private int sequence;
	private TilesView tilesView;
	private boolean blocked=true;
	
	public TileThread(int sequence,TilesView tilesView) {
		this.sequence=sequence;
		this.tilesView=tilesView;
		this.mapEngine = MapEngine.getInstance();
	}
	
	private void addToTileImages(Tile requestTile, TileResponse tileResponse, boolean loadFromDb){
		synchronized(tilesView.getDrawingLock()){
			if(!tilesView.getTileImages().containsKey(requestTile)){
				tilesView.getTileImages().put(requestTile, tileResponse);
				//Log.i("TileThread","addToTileImages image size,tileImages num,memory allocated,tile:"+bitmap.getRowBytes()*bitmap.getHeight()+","+tilesView.getTileImages().size()+","+android.os.Debug.getNativeHeapAllocatedSize()+","+requestTile.toString());
			}else{
				LogWrapper.i("TileThread","addToTileImages duplicate tile,hashCode:"+requestTile.toString()+","+requestTile.hashCode());
				
			}
			
		}
		
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
		LinkedList<Tile> tilesWaitForLoading=tilesView.getTilesWaitForLoading();
		
		while (true) {
			if(stop){
				LogWrapper.i("TileThread","thread "+sequence+" break");
				break;
			}
			Tile requestTile=null;
			
			/*if(mapActivity.networkType==ConnectivityManager.TYPE_WIFI && sequence>=FAST_LOAD_THREAD_MAX){
				synchronized(mapActivity.networkLock){
					try{
						Log.i("Thread","TileThread"+sequence+" before wait on networkLock");
						requestTiles[sequence]=null;
						mapActivity.networkLock.wait();
						Log.i("Thread","TileThread"+sequence+" after wait on networkLock");
						
					}catch(Exception e){
						
					}
				}
			}*/
			
			synchronized(tilesWaitForLoading){
				if(tilesWaitForLoading.isEmpty()){
					try{
						//Log.i("TileThread",""+sequence+" before tilesWaitForLoading.wait");
						blocked=true;
						tilesWaitForLoading.wait();
						//Log.i("TileThread",""+sequence+" after tilesWaitForLoading.wait");
						
					}catch(Exception e){
						
					}
				}
				blocked=false;
				
				if(tilesWaitForLoading.size()>0) {
					requestTile= tilesWaitForLoading.poll();
					//Log.i("TileThread","seq:"+this.sequence+",tile dist:"+ tile.distanceFromCenter+",number of tilesWaitForLoading:"+ tilesWaitForLoading.size());
				}
			}
			
			if(stop){
				LogWrapper.i("TileThread","thread "+sequence+" break");
				break;
			}
						
			if (requestTile!=null) {
				try {					
					int threadSeq=-1;
					for(int i=0;i<CONFIG.TILE_THREAD_COUNT;i++){
						if(requestTile.equals(requestTiles[i])){
							threadSeq=i;
							break;
						}
					}
					if(threadSeq!=-1){
						//Log.i("TileThread","thread "+sequence+" check thread "+threadSeq+" is requesting,rquestTile:"+requestTile.toString());
						continue;
					}
					requestTiles[sequence]=requestTile;
										
					synchronized(tilesView.getDrawingLock()){
						if(tilesView.getTileTextureRefs().containsKey(requestTile)){
							//Log.i("TileThread","check tileTextureRefs duplicate tile,hashCode:"+requestTile.toString()+","+requestTile.hashCode());
							continue;
						}
						if(tilesView.getTileImages().containsKey(requestTile)){
							//Log.i("TileThread","check tileImages duplicate tile,hashCode:"+requestTile.toString()+","+requestTile.hashCode());
							continue;
						}
					}
					
					//Log.i("TileThread" + this.sequence,"loading tile from network:" + Util.formatURL(url));
					try{
						TileResponse tileResponse = null;
                        XYZ xyz = requestTile.xyzTK;
                        long start=System.nanoTime();
                        tileResponse = mapEngine.getTileBuffer(xyz.x, xyz.y, xyz.z);
                        long loadTime=System.nanoTime()-start;    
                        Profile.tilesNetworkInc(loadTime);
					    
						if (tileResponse != null) {				
							if (tileResponse.bitmap == null) {
	                            if (tileResponse.lostTileInfos == null) {
	                                tilesView.refreshMap();
	                            } else {
	                                if (tilesView.getZoomingRecord().zoomCenterXY.x == 0 && tilesView.getZoomingRecord().zoomCenterXY.y == 0
	                                        && tilesView.getEasingRecord().direction.x == 0 && tilesView.getEasingRecord().direction.y == 0) {
	                                    LinkedList<TileDownload> tilesWaitForDownLoading=tilesView.getTilesWaitForDownloading();
	                                    synchronized (tilesWaitForDownLoading) {
	                                        int total = 0;
	                                        for(TileDownload tileDownload : tileResponse.lostTileInfos) {
	                                            tilesWaitForDownLoading.remove(tileDownload);
	                                            if (!DownloadThread.DownloadingTiles.contains(tileDownload)) {
	                                                tilesWaitForDownLoading.addLast(tileDownload);
	                                                total++;
	                                            }
	                                        }
	                                        if (total > 0) {
	                                            tilesView.noticeDownload(DownloadEventListener.STATE_DOWNLOADING);
	                                            tilesWaitForDownLoading.notifyAll();
	                                        }
	                                    }
	                                }
	                            }
							} else {
	                            addToTileImages(requestTile,tileResponse,false);
	                            tilesView.refreshMap();
							}
						}
					}catch(OutOfMemoryError e){
						LogWrapper.e("TileThread","getTile from network outOfMemoryError,heap size:"+android.os.Debug.getNativeHeapAllocatedSize());
						continue;
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}finally{
					requestTiles[sequence]=null;
				}
			}
		}
	}
}
