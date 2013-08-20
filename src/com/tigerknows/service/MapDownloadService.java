/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.map.LocalRegionDataInfo;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.TileDownload;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.map.MapEngine.RegionMetaVersion;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.MapMetaFileDownload;
import com.tigerknows.model.MapTileDataDownload;
import com.tigerknows.model.MapVersionQuery.ServerRegionDataInfo;
import com.tigerknows.ui.more.MapDownloadActivity;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author pengwenyue
 *
 * 下载城市地图
 * 在onCreate()时创建一个线程用于下载城市地图，下载过程中广播通知监听者进度及完成情况
 */
public class MapDownloadService extends Service implements MapTileDataDownload.ITileDownload {
    
    static final String TAG = "MapDownloadService";
    
    /**
     * 广播当前下载城市的进度情况
     */
    public static final String ACTION_MAP_DOWNLOAD_PROGRESS = "action.com.tigerknows.map.download.progress";
    
    /**
     * 通知监听者进度的时间间隔
     */
    static final int REFRESH_PROGRESS_INTERVAL = 2000;
    
    /**
     * 网络出错时的重试时间间隔
     */
    static final int NETWORK_FAILURE_RETRY_INTERVAL =15000;
    
    public static final String EXTRA_CITY_INFO = "extra_city_info";
    
    public static final String EXTRA_OPERATION_CODE = "extra_operation_code";
    
    /**
     * 添加某个下载城市
     */
    public static final String OPERATION_CODE_ADD = "add";
    
    /**
     * 删除某个下载城市
     */
    public static final String OPERATION_CODE_REMOVE = "remove";
    
    /**
     * 清空下载城市列表
     */
    public static final String OPERATION_CODE_CLEAR = "clear";
    
    public static final String EXTRA_TOTAL_SIZE = "extra_total_size";
    
    public static final String EXTRA_DOWNLOAD_SIZE = "extra_download_size";
    
    Context context;
    public static List<CityInfo> CityInfoList = new ArrayList<CityInfo>(); // 下载城市队列
    boolean isStopAll = false; // 停止全部
    CityInfo currentCityInfo = null; // 当前下载城市
    int writeSize = 0; // 临时写入的大小
    int totalSize = 0; // 总大小
    int downloadedSize = 0; // 已下载的大小
    long lastRefreshTime; // 最近通知监听者的时间截
    boolean isStopCurrentCity = false; // 停止当前下载城市
    MapEngine mapEngine = null;
    private MapMetaFileDownload mapMetaFileDownload; // 下载元数据
    private MapTileDataDownload mapTileDataDownload; // 下载Tile数据
    
    @Override
    public void onCreate() {
        super.onCreate();
        CityInfoList.clear();
        context = getApplicationContext();
        mapEngine = MapEngine.getInstance();
        mapMetaFileDownload = new MapMetaFileDownload(context, mapEngine);
        mapTileDataDownload = new MapTileDataDownload(context, mapEngine);
        mapTileDataDownload.setFillMapTile(this);

        new Thread(new Runnable() {
            
            @Override
            public void run() {
                    while (true) {
                        if(isStopAll){
                            LogWrapper.i(TAG,"thread break");
                            break;
                        }
                        synchronized (CityInfoList) {
                            if (CityInfoList.isEmpty()) {
                                try {
                                    LogWrapper.i(TAG,"thread wait");
                                    CityInfoList.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (CityInfoList.isEmpty()) {
                            continue;
                        }
                        currentCityInfo = CityInfoList.get(0);
                        LogWrapper.d(TAG, "currentCityInfo="+currentCityInfo);
                        totalSize = 0;
                        downloadedSize = 0;
                        writeSize = 0;
                        isStopCurrentCity = false;
                        int statusCode = BaseQuery.STATUS_CODE_NETWORK_OK;
                        
                        // 查询所有城市的所有Region的数据信息
                        HashMap<Integer, ServerRegionDataInfo> allServerRegionDataInfoMap = MapStatsService.queryServerRegionDataInfoMapInternally(context);
                        
                        HashMap<Integer, ServerRegionDataInfo> serverRegionDataInfoMap = MapStatsService.queryServerRegionDataInfoMapInternally(context, currentCityInfo);
                        
                        if (serverRegionDataInfoMap == null) {
                            serverRegionDataInfoMap = allServerRegionDataInfoMap;
                        } else {
                            if (allServerRegionDataInfoMap != null) {
                                allServerRegionDataInfoMap.putAll(serverRegionDataInfoMap);
                            }
                        }
                        
                        List<Integer> regionIdList = mapEngine.getRegionIdList(currentCityInfo.getCName());
                        for (int i = 0, size = regionIdList.size(); i < size; i++) {
                            int regionId = regionIdList.get(i);
                            LogWrapper.d(TAG, "count regionId="+regionId);
                            
                            LocalRegionDataInfo localRegionDataInfo = mapEngine.getLocalRegionDataInfo(regionId);
                            // 如果本地没有Region元数据则去下载并生成Region数据文件
                            while (isStopAll == false
                                    && isStopCurrentCity == false
                                    && localRegionDataInfo == null) {
                                localRegionDataInfo = downloadMetaData(regionId);     
                            }
                            
                            ServerRegionDataInfo severRegionDataInfo = null;
                            if (serverRegionDataInfoMap != null && serverRegionDataInfoMap.containsKey(regionId)) {
                                severRegionDataInfo = serverRegionDataInfoMap.get(regionId);
                                if (localRegionDataInfo != null && severRegionDataInfo != null) {
                                    RegionMetaVersion regionMetaVersion = mapEngine.getRegionMetaVersion(regionId);
                                    if (null != regionMetaVersion) {
                                        String localVersion = regionMetaVersion.toString();
                                        String serverVersion = severRegionDataInfo.getRegionVersion();
                                        List<TileDownload> lostDatas = localRegionDataInfo.getLostDatas();
                                        
                                        // 更新地图数据文件
                                        if ((lostDatas != null && lostDatas.size() > 0 && lostDatas.get(0).getOffset() == 0)
                                                || (serverVersion != null && !serverVersion.equalsIgnoreCase(localVersion))) {
                                            
                                            mapEngine.removeRegion(regionId);
                                            localRegionDataInfo = null;
                                            while (isStopAll == false
                                                    && isStopCurrentCity == false
                                                    && localRegionDataInfo == null) {
                                                localRegionDataInfo = downloadMetaData(regionId);     
                                            }
                                        }
                                    }
                                }
                            }

                            if (localRegionDataInfo != null){
                                totalSize += localRegionDataInfo.getTotalSize();
                                downloadedSize += localRegionDataInfo.getDownloadedSize();
                            }
            
                        }
                        
                        //下载Region数据
                        statusCode = downloadRegionList(regionIdList);
                        
                        refreshProgress(true);
                        if (totalSize > 0
                                && downloadedSize > 0
                                && downloadedSize/(float)totalSize >= MapDownloadActivity.PERCENT_COMPLETE) {
                            synchronized (CityInfoList) {
                                CityInfoList.remove(currentCityInfo);
                            }
                        }
                        
                        if (statusCode != BaseQuery.STATUS_CODE_NETWORK_OK) {
                            try {
                                Thread.sleep(NETWORK_FAILURE_RETRY_INTERVAL);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                    LogWrapper.d(TAG, "thread exit");
                }
            }).start();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 在Service被停止时会停止下载并退出下载地图的线程
        isStopAll = true;
        mapMetaFileDownload.stop();
        mapTileDataDownload.stop();
        synchronized (CityInfoList) {
            CityInfoList.clear();
            CityInfoList.notifyAll();
        }
    }

    @Override
    public void onStart(final Intent intent, int startId) {
        if (intent != null) {
            if (intent.hasExtra(EXTRA_OPERATION_CODE)) {
                synchronized (CityInfoList) {
                    String operationCode = intent.getStringExtra(EXTRA_OPERATION_CODE);
                    LogWrapper.d(TAG, "onStart() operationCode="+operationCode);
                    if (OPERATION_CODE_ADD.equals(operationCode)) {
                        if (intent.hasExtra(EXTRA_CITY_INFO)) {
                            CityInfo cityInfo = intent.getParcelableExtra(EXTRA_CITY_INFO);
                            CityInfoList.remove(cityInfo);
                            CityInfoList.add(cityInfo);
                            CityInfoList.notifyAll();
                        }
                    } else if (OPERATION_CODE_REMOVE.equals(operationCode)) {
                        if (intent.hasExtra(EXTRA_CITY_INFO)) {
                            CityInfo cityInfo = intent.getParcelableExtra(EXTRA_CITY_INFO);
                            CityInfoList.remove(cityInfo);
                            CityInfo thisCityInfo = this.currentCityInfo;
                            if (thisCityInfo != null && thisCityInfo.getId() == cityInfo.getId()) {
                                mapMetaFileDownload.stop();
                                mapTileDataDownload.stop();
                                isStopCurrentCity = true;
                            }
                        }
                    } else if (OPERATION_CODE_CLEAR.equals(operationCode)) {
                        CityInfoList.clear();
                        mapMetaFileDownload.stop();
                        mapTileDataDownload.stop();
                        isStopCurrentCity = true;
                    }
                }
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.tigerknows.model.MapTileDataDownload.ITileDownload#fillMapTile(java.util.List, int, byte[], int)
     */
    @Override
    public int fillMapTile(List<TileDownload> tileDownloadList, int rid, byte[] data, int start) {
        if (tileDownloadList == null || tileDownloadList.isEmpty() || data == null) {
            return -1;
        }
        
        int remainDataLenth = data.length - start;
        int size = tileDownloadList.size();
        for(int i = 0; i < size; i++) {
            refreshProgress(false);
            if (isStopAll || isStopCurrentCity) {
                return -1;
            }
            TileDownload tileInfo = tileDownloadList.get(i);
            if (remainDataLenth <= 0) {
                break;
            } 
            int tileLen = tileInfo.getLength();
            if (tileLen < 1 || rid != tileInfo.getRid()) {
                continue;
            }
            if (tileLen <= remainDataLenth) {
                byte[] dest = new byte[tileLen];
                System.arraycopy(data, start, dest, 0, tileLen);
                int ret = mapEngine.writeRegion(tileInfo.getRid(), tileInfo.getOffset(), dest, tileInfo.getVersion());
                if (ret != 0) {
                    return -1;
                }
                writeSize += tileLen; // 增加临时写入大小
                //let the tile be empty
                tileInfo.setLength(-1);
                start += tileLen;
                remainDataLenth -= tileLen;
            } else {
                byte[] dest = new byte[remainDataLenth];
                System.arraycopy(data, start, dest, 0, remainDataLenth);
                int ret = mapEngine.writeRegion(tileInfo.getRid(), tileInfo.getOffset(), dest, tileInfo.getVersion());
                if (ret != 0) {
                    return -1;
                }
                writeSize += remainDataLenth; // 增加临时写入大小
                tileInfo.setOffset(tileInfo.getOffset() + remainDataLenth);
                tileInfo.setLength(tileLen - remainDataLenth); 
                remainDataLenth = 0;
            }
        }
        return 0;
    }
    
    
    /*
     * 广播通知监听者进度情况
     * @force 是否强制发送更新广播
     */
    void refreshProgress(boolean force) {
        long currentTimeMillis = System.currentTimeMillis();
        if (force || (currentTimeMillis-lastRefreshTime > REFRESH_PROGRESS_INTERVAL)) {
            // 更新时间截、已下载大小及临时写入大小
            lastRefreshTime = currentTimeMillis;
            downloadedSize += writeSize;
            writeSize = 0;
            LogWrapper.d(TAG, "refreshProgress() downloadedSize,totalSize="+downloadedSize+","+totalSize);
            Intent intent = new Intent(ACTION_MAP_DOWNLOAD_PROGRESS);
            intent.putExtra(EXTRA_CITY_INFO, currentCityInfo);
            intent.putExtra(EXTRA_TOTAL_SIZE, totalSize);
            intent.putExtra(EXTRA_DOWNLOAD_SIZE, downloadedSize);
            sendBroadcast(intent);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.tigerknows.model.MapTileDataDownload.ITileDownload#upgradeRegion(int)
     */
    @Override
    public void upgradeRegion(int rid) {
        mapEngine.removeRegion(rid);
    }
    
    /*
     * 下载指定RegionId的元数据
     */
    LocalRegionDataInfo downloadMetaData(int regionId) {
        LogWrapper.d(TAG, "download meta regionId="+regionId);
        mapMetaFileDownload.setup(regionId);
        mapMetaFileDownload.query();
        LocalRegionDataInfo regionMapInfo = mapEngine.getLocalRegionDataInfo(regionId);
        return regionMapInfo;
    }
    
    /*
     * 下载RegionList所有的Tile数据
     */
    int downloadRegionList(List<Integer> regionIdList) {
        int statusCode =  BaseQuery.STATUS_CODE_NETWORK_OK;
         //下载tile数据
        for (int i = 0, size = regionIdList.size(); i < size; i++) {
            if (isStopAll || isStopCurrentCity) {
                break;
            }
            int regionId = regionIdList.get(i);
            LogWrapper.d(TAG, "download tile regionId="+regionId);
            LocalRegionDataInfo regionMapInfo = mapEngine.getLocalRegionDataInfo(regionId);
            while (regionMapInfo != null && regionMapInfo.getLostDataNum() > 0) {
                if (isStopAll || isStopCurrentCity) {
                    break;
                }
                List<TileDownload> lostDatas = regionMapInfo.getLostDatas();
                statusCode = downloadTileData(lostDatas);
                regionMapInfo = mapEngine.getLocalRegionDataInfo(regionId);
            }
        }
        return statusCode;
    }
    
    /*
     * 下载Tile数据
     */
    int downloadTileData(List<TileDownload> lostDatas) {
        int statusCode =  BaseQuery.STATUS_CODE_NETWORK_OK;
        if (lostDatas == null) {
            return statusCode;
        }
        // download tile
        for (TileDownload tileInfo : lostDatas) {
            if (isStopAll || isStopCurrentCity) {
                break;
            }
            if (tileInfo.getLength() <= 0) {
                // when len is 0, this tile has been downloaded
                continue;
            }
            mapTileDataDownload.setup(lostDatas, tileInfo.getRid());
            mapTileDataDownload.query();
            if (mapTileDataDownload.isStop() == false) {
                statusCode = mapTileDataDownload.getStatusCode();
            }
        }
        return statusCode;
    }
}
