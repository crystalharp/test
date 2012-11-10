/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.map;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.decarta.CONFIG;
import com.decarta.android.map.MapView.DownloadEventListener;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.TileDownload;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.MapMetaFileDownload;
import com.tigerknows.model.MapTileDataDownload;
import com.tigerknows.model.Response;

/**
 * Thread class for loading tiles
 */
public class DownloadThread extends Thread implements MapTileDataDownload.FillMapTile {
	//private final static int FAST_LOAD_THREAD_MAX=5;
	
    private static final int NETWORK_ERROR_WAIT_TIME = 20*1000;
	private static boolean stop=false;
	private MapEngine mapEngine=null;
    private MapMetaFileDownload mapMetaFileDownload;
    private MapTileDataDownload mapTileDataDownload;
    public static LinkedList<TileDownload> DownloadingTiles=new LinkedList<TileDownload>();
	                    
	public static void startAllThreads(){
		stop=false;
		LogWrapper.i("DownloadThread","startAllThreads, tile thread count:"+CONFIG.TILE_THREAD_COUNT);
		DownloadingTiles.clear();
	}
	public static void stopAllThreads(){
		stop=true;
	}
	
	private int sequence;
	private TilesView tilesView;
	private boolean blocked=true;
	private List<TileDownload> downloadingTiles = new ArrayList<TileDownload>();
	
	public DownloadThread(int sequence,TilesView tilesView) {
		this.sequence=sequence;
		this.tilesView=tilesView;
		this.mapEngine = MapEngine.getInstance();
        this.mapMetaFileDownload = new MapMetaFileDownload(tilesView.getContext(), mapEngine);
        this.mapTileDataDownload = new MapTileDataDownload(tilesView.getContext(), mapEngine);
        this.mapTileDataDownload.setFillMapTile(this);
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
		LinkedList<TileDownload> tilesWaitForDownLoading=tilesView.getTilesWaitForDownloading();
		
		while (true) {
			if(stop){
				LogWrapper.i("DownloadThread","thread "+sequence+" break");
				break;
			}
			TileDownload requestTile=null;
			
			/*if(mapActivity.networkType==ConnectivityManager.TYPE_WIFI && sequence>=FAST_LOAD_THREAD_MAX){
				synchronized(mapActivity.networkLock){
					try{
						Log.i("Thread","DownloadThread"+sequence+" before wait on networkLock");
						requestTiles[sequence]=null;
						mapActivity.networkLock.wait();
						Log.i("Thread","DownloadThread"+sequence+" after wait on networkLock");
						
					}catch(Exception e){
						
					}
				}
			}*/
			
			synchronized(tilesWaitForDownLoading){
				if(tilesWaitForDownLoading.isEmpty()){
					try{
						//Log.i("DownloadThread",""+sequence+" before tilesWaitForLoading.wait");
                        tilesView.noticeDownload(DownloadEventListener.STATE_DOWNLOADED);
						blocked=true;
						tilesWaitForDownLoading.wait();
						//Log.i("DownloadThread",""+sequence+" after tilesWaitForLoading.wait");
						
					}catch(Exception e){
						
					}
				}
				blocked=false;
				
				if(tilesWaitForDownLoading.size()>0) {

                    synchronized (tilesWaitForDownLoading) {
                        int rid = -1;
                        for(int i = tilesWaitForDownLoading.size()-1; i >= 0; i--) {
                            requestTile= tilesWaitForDownLoading.get(i);
                            if (!DownloadingTiles.contains(requestTile)) {
                                if (requestTile.getLength() > 0) {
                                    if (downloadingTiles.isEmpty()) {
                                        rid = requestTile.getRid();
                                        downloadingTiles.add(requestTile);
                                        DownloadingTiles.add(requestTile);
                                        tilesWaitForDownLoading.remove(requestTile);
                                    } else if (requestTile.getRid() == rid) {
                                        downloadingTiles.add(requestTile);
                                        DownloadingTiles.add(requestTile);
                                        tilesWaitForDownLoading.remove(requestTile);
                                    }
                                } else if (requestTile.getLength() == 0 && requestTile.getOffset() == 0 && downloadingTiles.isEmpty()){
                                    downloadingTiles.add(requestTile);
                                    DownloadingTiles.add(requestTile);
                                    tilesWaitForDownLoading.remove(requestTile);
                                    break;
                                }
                            }
                        }
                    }
					//Log.i("DownloadThread","seq:"+this.sequence+",tile dist:"+ tile.distanceFromCenter+",number of tilesWaitForLoading:"+ tilesWaitForLoading.size());
				}
			}
			
			if(stop){
				LogWrapper.i("DownloadThread","thread "+sequence+" break");
				break;
			}
						
			if (!downloadingTiles.isEmpty()) {
				try {
					//Log.i("DownloadThread" + this.sequence,"loading tile from network:" + Util.formatURL(url));
					try{
                            int size = downloadingTiles.size();
                            LogWrapper.d("DownloadThread", "downloadTiles.size()="+downloadingTiles.size());
                            if (size > 0) {
                                try {
                                    int statusCode = 0;
                                    if (downloadingTiles.get(0).getLength() == 0 && downloadingTiles.get(0).getOffset() == 0) {
                                        statusCode = downloadMetaData(downloadingTiles.get(0).getRid());
                                    } else if (downloadingTiles.get(0).getLength() > 0) {
                                        statusCode = downloadTiles(downloadingTiles);
                                    }
                                    if (statusCode != (BaseQuery.STATUS_CODE_RESPONSE_EMPTY + Response.RESPONSE_CODE_OK) && statusCode != BaseQuery.STATUS_CODE_DATA_EMPTY) {
                                        tilesView.noticeDownload(DownloadEventListener.STATE_DOWNLOAD_ERROR);
                                        try {
                                            Thread.sleep(NETWORK_ERROR_WAIT_TIME);
                                        } catch (InterruptedException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }finally{
                                    
                                }
                                synchronized (tilesWaitForDownLoading) {
                                    for (TileDownload tileDownload : downloadingTiles) {
                                        DownloadingTiles.remove(tileDownload);
                                        tilesWaitForDownLoading.remove(tileDownload);
                                        LogWrapper.d("DownloadThread", "downloadingTiles.remove(tileDownload)="+tileDownload);
                                    }
                                }
                                tilesView.refreshMap();
                            }
					}catch(OutOfMemoryError e){
						LogWrapper.e("DownloadThread","getTile from network outOfMemoryError,heap size:"+android.os.Debug.getNativeHeapAllocatedSize());
						continue;
					}
					
				} catch (Exception e) {
					e.printStackTrace();
					
				}finally{
				    downloadingTiles.clear();
				}
			}
		}
	}

    private int downloadMetaData(int regionId) {
        LogWrapper.d("DownloadThread", "downloadMetaData() regionId=" + regionId);
        mapMetaFileDownload.setup(regionId);
        mapMetaFileDownload.query();
        int statusCode = mapMetaFileDownload.getStatusCode();
        return statusCode;
    }

    private int downloadTiles(List<TileDownload> tileDownloads) {
        int statusCode = 0;
        if (tileDownloads == null || tileDownloads.size() == 0) {
            return statusCode;
        }
        LogWrapper.d("DownloadThread", "downloadTileData() rid=" + tileDownloads.get(0).getRid());
        mapTileDataDownload.setup(tileDownloads, tileDownloads.get(0).getRid());
        mapTileDataDownload.query();
        statusCode = mapTileDataDownload.getStatusCode();
        return statusCode;
    }
    
    @Override
    public void fillMapTile(List<TileDownload> tileDownloads, int rid, byte[] data, boolean upgrade) {
//        LogWrapper.d("DownloadThread", "fillMapTile() tileInfos=" + tileDownloads[0].getRid()+", size="+data.length);
        if (tileDownloads == null) {
            return;
        }
        
        if (data == null) {
            if (upgrade) {
                downloadMetaData(tileDownloads.get(0).getRid());
            }
            return;
        }

        int start = 0;
        int remainDataLenth = data.length - start;
        while (remainDataLenth > 0) {
            for (TileDownload tileInfo : tileDownloads) {
                int tileLen = tileInfo.getLength();
                if (tileLen < 1 || rid != tileInfo.getRid()) {
                    continue;
                }
                if (tileLen <= remainDataLenth) {
                    byte[] dest = new byte[tileLen];
                    System.arraycopy(data, start, dest, 0, tileLen);
                    mapEngine.writeRegion(tileInfo.getRid(), tileInfo.getOffset(), dest);
                    // let the tile be empty
                    tileInfo.setLength(-1);
                    start += tileLen;
                    remainDataLenth -= tileLen;
                } else {
                    byte[] dest = new byte[remainDataLenth];
                    System.arraycopy(data, start, dest, 0, remainDataLenth);
                    mapEngine.writeRegion(tileInfo.getRid(), tileInfo.getOffset(), dest);
                    tileInfo.setOffset(tileInfo.getOffset() + remainDataLenth);
                    tileInfo.setLength(tileLen - remainDataLenth);
                    remainDataLenth = 0;
                }
            }
        }
    }
}
