/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.map;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedList;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

import com.decarta.CONFIG;
import com.decarta.Profile;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.XYZ;
import com.tigerknows.map.Ca;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.TileDownload;
import com.tigerknows.map.TileResponse;
import com.tigerknows.map.MapView.DownloadEventListener;
import com.tigerknows.map.label.Label;
import com.tigerknows.map.TileInfo;
import com.tigerknows.map.Ca;

/**
 * Thread class for loading tiles
 */
public class TileThread extends Thread {
	//private final static int FAST_LOAD_THREAD_MAX=5;
	
	private static boolean stop=false;
	private static Tile[] requestTiles=null;
	private MapEngine mapEngine=null;
	                    
	public static void startAllThreads(){
		if(requestTiles == null) {
			requestTiles=new Tile[CONFIG.TILE_THREAD_COUNT];
		}
		stop=false;
		LogWrapper.i("TileThread","startAllThreads, tile thread count:"+CONFIG.TILE_THREAD_COUNT);
	}
	public static void stopAllThreads(){
		stop=true;
	}
    
    public static boolean isStop() {
        return stop;
    }
	
	private int sequence;
	TilesView tilesView;
	private boolean blocked=true;
	
	public TileThread(int sequence,TilesView tilesView) {
		this.sequence=sequence;
		this.tilesView=tilesView;
		this.mapEngine = MapEngine.getInstance();
	}
	
	private void addToTileInfos(Tile requestTile, TileInfo tileInfo, boolean loadFromDb){
		synchronized(tilesView.getDrawingLock()){
			if(!tilesView.getTileInfos().containsKey(requestTile)){
				tilesView.getTileInfos().put(requestTile, tileInfo);
			} else {
				LogWrapper.i("TileThread","addToTileInfos duplicate tile,hashCode:"+requestTile.toString()+","+requestTile.hashCode());
			}
		}
	}
	
	public boolean isBlocked() {
		return blocked;
	}
	public static final int RENDER_MODE = 1;
	/**
	 * main entry method for loading tiles
	 */
	@Override
	public void run() {
		//MapActivity mapActivity=(MapActivity)(tilesView.getContext());
		LinkedList<Tile> tilesWaitForLoading=tilesView.getTilesWaitForLoading();
		int matrixSize = Ca.tk_get_matrix_size(CONFIG.TILE_SIZE);
		int[] bitmapIntBuffer = new int[matrixSize];
		Ca.tk_init_context(bitmapIntBuffer, MapEngine.TILE_SIZE_BIT, RENDER_MODE);
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
						if(tilesView.getTileInfos().containsKey(requestTile)){
							//Log.i("TileThread","check tileTextureRefs duplicate tile,hashCode:"+requestTile.toString()+","+requestTile.hashCode());
							continue;
						}
					}
					
					//Log.i("TileThread" + this.sequence,"loading tile from network:" + Util.formatURL(url));
					try{
                        XYZ xyz = requestTile.xyz;
                        long start=System.nanoTime();
                        Label[] labels = Ca.tk_render_tile(xyz.x, xyz.y, xyz.z);
                        long loadTime=System.nanoTime()-start;
                        Profile.tilesNetworkInc(loadTime);
                        if (labels != null) {
                            start=System.nanoTime();
                            Bitmap bmp565 = Bitmap.createBitmap(CONFIG.TILE_SIZE, CONFIG.TILE_SIZE, Config.RGB_565);
                            bmp565.setPixels(bitmapIntBuffer, 0, CONFIG.TILE_SIZE, 0, 0, CONFIG.TILE_SIZE, CONFIG.TILE_SIZE);
                            
//                            File myCaptureFile = new File(MapEngine.getInstance().getMapPath() + "test" + xyz + ".jpg");
//                          BufferedOutputStream bos = new BufferedOutputStream(
//                                                                   new FileOutputStream(myCaptureFile));
//                          bmp565.compress(Bitmap.CompressFormat.JPEG, 80, bos);
//                          bos.flush();
//                          bos.close();
//                            bmp565.compress(format, quality, stream)
                            loadTime = System.nanoTime()-start;
                            Profile.decodeByteArrayInc(loadTime);
                            
                            TileInfo tileInfo = new TileInfo();
							tileInfo.bitmap = bmp565;
							tileInfo.labels = labels;
                            addToTileInfos(requestTile, tileInfo, false);
                            tilesView.refreshMap();
                        } else {
                        	TileDownload[] lostTileInfos = Ca.tk_get_lost_tile_info();
                        	if (tilesView.getZoomingRecord().zoomCenterXY.x == 0 && tilesView.getZoomingRecord().zoomCenterXY.y == 0
                                    && tilesView.getEasingRecord().direction.x == 0 && tilesView.getEasingRecord().direction.y == 0) {
                                LinkedList<TileDownload> tilesWaitForDownLoading=tilesView.getTilesWaitForDownloading();
                                synchronized (tilesWaitForDownLoading) {
                                    int total = 0;
                                    for(TileDownload tileDownload : lostTileInfos) {
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
					} catch(OutOfMemoryError e) {
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
		Ca.tk_fini_context();
	}
}
