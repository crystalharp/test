/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.map.LocalRegionDataInfo;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.map.MapEngine.RegionInfo;
import com.tigerknows.map.MapEngine.RegionMetaVersion;
import com.tigerknows.model.MapVersionQuery;
import com.tigerknows.model.MapVersionQuery.ServerRegionDataInfo;
import com.tigerknows.ui.more.MapDownloadActivity;
import com.tigerknows.ui.more.MoreHomeFragment;
import com.tigerknows.ui.more.MapDownloadActivity.DownloadCity;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author pengwenyue
 * 
 * 统计城市的地图信息（大小、版本）及状态（等待、正在下载、可更新），
 * 在onCreate()创建一个线程用于统计指定城市的地图信息及状态，
 * 在onStart()时为每次统计当前城市、出现在下载列表中的所有城市或未出现在下载列表中但是tigermap/map文件夹下已经存在地图数据文件的城市的地图信息及状态分别创建一个新线程
 * 统计结束时都通过广播通知监听者
 * ACTION_STATS_CURRENT_DOWNLOAD_CITY              统计当前城市的地图信息及状态
 * ACTION_STATS_DOWNLOAD_CITY_LIST                 统计出现在下载列表中的所有城市的地图信息及状态
 * ACTION_STATS_DOWNLOAD_CITY                      统计指定城市的地图信息及状态
 * ACTION_STATS_DOWNLOAD_CITY_LIST_FOR_EXIST_MAP   统计未出现在下载列表中但是tigermap/map文件夹下已经存在地图数据文件的城市的地图信息及状态
 *
 */
public class MapStatsService extends Service {
    
    static final String TAG = "MapStatsService";
    
    /*
     * 统计当前城市的地图信息及状态
     */
    public static final String ACTION_STATS_CURRENT_DOWNLOAD_CITY = "action.com.tigerknows.stats.current.download.city";
    
    /*
     * 统计当前城市的地图信息及状态完成
     */
    public static final String ACTION_STATS_CURRENT_DOWNLOAD_CITY_COMPLATE = "action.com.tigerknows.stats.current.download.city.complate";
    
    /*
     * 统计出现在下载列表中的所有城市的地图信息及状态
     */
    public static final String ACTION_STATS_DOWNLOAD_CITY_LIST = "action.com.tigerknows.stats.download.city.list";
    
    /*
     * 统计出现在下载列表中的所有城市的地图信息及状态完成
     */
    public static final String ACTION_STATS_DOWNLOAD_CITY_LIST_COMPLATE = "action.com.tigerknows.stats.download.city.list.complate";
    
    /*
     * 统计指定城市的地图信息及状态
     */
    public static final String ACTION_STATS_DOWNLOAD_CITY = "action.com.tigerknows.stats.download.city";
    
    /*
     * 统计指定城市的地图信息及状态完成
     */
    public static final String ACTION_STATS_DOWNLOAD_CITY_COMPLATE = "action.com.tigerknows.stats.download.city.complate";
    
    /*
     * 统计未出现在下载列表中但是tigermap/map文件夹下已经存在地图数据文件的城市的地图信息及状态
     */
    public static final String ACTION_STATS_DOWNLOAD_CITY_LIST_FOR_EXIST_MAP = "action.com.tigerknows.stats.download.city.list.for.exist.map";
    
    /*
     * 统计未出现在下载列表中但是tigermap/map文件夹下已经存在地图数据文件的城市的地图信息及状态完成
     */
    public static final String ACTION_STATS_DOWNLOAD_CITY_LIST_FOR_EXIST_MAP_COMPLATE = "action.com.tigerknows.stats.download.city.list.for.exist.map.complate";
    
    public static final String EXTRA_DOWNLOAD_CITY_LIST = "extra_download_city_list";
    
    public static final String EXTRA_DOWNLOAD_CITY = "extra_download_city";
    
    /*
     * 延迟30s后再统计当前城市的地图信息及状态
     */
    static final int STATS_CURRENT_CITY_DELAY_TIME = 30000;
    
    /*
     * 查询全国所有城市的最新Region信息的时间间隔
     */
    static final int QUERY_MAP_VERSION_ERROR_SLEEP_TIME = 15000;
    
    /*
     * 最近查询全国所有城市的最新Region信息的时间截
     */
    private static long lastQueryServerRegionDataInfoTime;
    
    /*
     * 全国所有城市的最新Region信息(大小、版本等等)
     */
    private static HashMap<Integer, ServerRegionDataInfo> serverRegionDataInfoMap = null;
    public static HashMap<Integer, ServerRegionDataInfo> getServerRegionDataInfoMap() {
        return serverRegionDataInfoMap;
    }
    
    Context context;
    List<DownloadCity> downloadCityList = new ArrayList<DownloadCity>(); // 待统计的城市队列
    boolean stop = false; // 停止统计
    DownloadCity downloadCity = null; // 当前统计城市
    
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        // 此线程用于统计指定城市的地图信息及状态
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                    while (true) {
                        // 在Service被停止时会退出while
                        if(stop){
                            LogWrapper.i(TAG,"thread break");
                            break;
                        }
                        synchronized (downloadCityList) {
                            if (downloadCityList.isEmpty()) {
                                try {
                                    LogWrapper.i(TAG,"thread wait");
                                    downloadCityList.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        
                        if (downloadCityList.isEmpty()) {
                        	continue;
                        }
                        downloadCity = downloadCityList.remove(0); // 取出最先插入的城市
                        
                        statsDownloadCity(downloadCity, queryServerRegionDataInfoMapInternally(context));
                        
                        // 统计结束时广播通知监听者
                        Intent intent = new Intent(ACTION_STATS_DOWNLOAD_CITY_COMPLATE);
                        intent.putExtra(EXTRA_DOWNLOAD_CITY, downloadCity);
                        sendBroadcast(intent);
                    }
            }
        }).start();
    }
    
    /*
     * 查询所有城市的最新Region信息（大小、版本等等）
     * 当serverRegionDataInfoMap为空且距上次查询时间超过15s才能查询，这样做是为了避免频繁网络查询
     */
    public static HashMap<Integer, ServerRegionDataInfo> queryServerRegionDataInfoMapInternally(Context context) {
        long currentTime = System.currentTimeMillis();
        
        if (serverRegionDataInfoMap == null
                && currentTime-lastQueryServerRegionDataInfoTime > QUERY_MAP_VERSION_ERROR_SLEEP_TIME) {   // 距上次查询时间超过15s
            lastQueryServerRegionDataInfoTime = currentTime;
            
            // 遍历全国所有城市的所有Region
            List<CityInfo> allCityInfoList = MapEngine.getAllProvinceCityList(context);
            List<Integer> allRegionIdList = new ArrayList<Integer>();
            for(int i = allCityInfoList.size()-1; i >= 0; i--) {
                CityInfo cityInfo1 = allCityInfoList.get(i);
                List<CityInfo> childCityInfoList = cityInfo1.getCityList();
                if (childCityInfoList.size() > 1) {
                    for(int ii = childCityInfoList.size()-1; ii >= 0; ii--) {
                        CityInfo cityInfo2 = childCityInfoList.get(ii);
                        allRegionIdList.addAll(MapEngine.getRegionIdList(cityInfo2.getCName()));
                    }
                } else {
                    allRegionIdList.addAll(MapEngine.getRegionIdList(cityInfo1.getCName()));
                }
            }
            
            // 查询服务器上最新的Region信息
            MapVersionQuery mapVersionQuery = new MapVersionQuery(context);
            mapVersionQuery.setup(allRegionIdList);
            mapVersionQuery.query();
            serverRegionDataInfoMap = mapVersionQuery.getServerRegionDataInfoMap();
        }
        return serverRegionDataInfoMap;
    }
    
    /*
     * 查询指定城市的最新Region信息（大小、版本等等）
     */
    public static HashMap<Integer, ServerRegionDataInfo> queryServerRegionDataInfoMapInternally(Context context, CityInfo cityInfo) {
        List<Integer> allRegionIdList = new ArrayList<Integer>();
        allRegionIdList.addAll(MapEngine.getRegionIdList(cityInfo.getCName()));
        
        // 查询服务器上最新的Region信息
        MapVersionQuery mapVersionQuery = new MapVersionQuery(context);
        mapVersionQuery.setup(allRegionIdList);
        mapVersionQuery.query();
        HashMap<Integer, ServerRegionDataInfo> serverRegionDataInfoMap = mapVersionQuery.getServerRegionDataInfoMap();
        return serverRegionDataInfoMap;
    }

    @Override
    public void onStart(final Intent intent, int startId) {

        if (intent != null) {
            if (ACTION_STATS_DOWNLOAD_CITY.equals(intent.getAction())) {
            	if (intent.hasExtra(EXTRA_DOWNLOAD_CITY_LIST)) {
                    // 将指定城市列表加入待统计城市队列
                	ArrayList<DownloadCity> list = intent.getParcelableArrayListExtra(EXTRA_DOWNLOAD_CITY_LIST);
                    synchronized (downloadCityList) {
                		downloadCityList.addAll(list);
                		downloadCityList.notifyAll();
    				}
            	} else if (intent.hasExtra(EXTRA_DOWNLOAD_CITY)) {

                    
                    // 将指定城市加入待统计城市队列
                    DownloadCity downloadCity = intent.getParcelableExtra(EXTRA_DOWNLOAD_CITY);
                    synchronized (downloadCityList) {
                      if (downloadCityList.contains(downloadCity) == false) {
                            downloadCityList.add(downloadCity);
                            downloadCityList.notifyAll();
                      }
                    }
            	}
            } else {
                
                // 每次统计当前城市、出现在下载列表中的所有城市或出现在下载列表中但是tigermap/map文件夹下已经存在地图数据文件的城市都创建一个新线程
                new Thread(new Runnable() {
                	
                	@Override
                	public void run() {
                	    Intent broadcast = null;
                	    String action = intent.getAction();
                	    // 统计当前城市
                        if (ACTION_STATS_CURRENT_DOWNLOAD_CITY.equals(action)) {
                            try {
                                Thread.sleep(STATS_CURRENT_CITY_DELAY_TIME);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            
                            DownloadCity downloadCity = statsCurrentCity();
                            
                            MoreHomeFragment.CurrentDownloadCity = downloadCity;
                            broadcast = new Intent(ACTION_STATS_CURRENT_DOWNLOAD_CITY_COMPLATE);
                            sendBroadcast(broadcast);
                            
                        // 统计出现在下载列表中但是tigermap/map文件夹下已经存在地图数据文件的城市
                        } else if (ACTION_STATS_DOWNLOAD_CITY_LIST_FOR_EXIST_MAP.equals(action)) {
                            ArrayList<DownloadCity> existMapCityList = null;
                            if (intent.hasExtra(EXTRA_DOWNLOAD_CITY_LIST)) {
                                existMapCityList = intent.getParcelableArrayListExtra(EXTRA_DOWNLOAD_CITY_LIST);
                            }
                            ArrayList<DownloadCity> list = statsDownloadCityListForExistData(existMapCityList);
                		    
                            broadcast = new Intent(ACTION_STATS_DOWNLOAD_CITY_LIST_FOR_EXIST_MAP_COMPLATE);
                            broadcast.putParcelableArrayListExtra(EXTRA_DOWNLOAD_CITY_LIST, list);
                            sendBroadcast(broadcast);
                            
                        // 统计出现在下载列表中的所有城市
                		} else if(ACTION_STATS_DOWNLOAD_CITY_LIST.equals(action)){
                		    ArrayList<DownloadCity> downloadCityList = (ArrayList<DownloadCity>) statsDownloadCityList();
                		    
                    		broadcast = new Intent(ACTION_STATS_DOWNLOAD_CITY_LIST_COMPLATE);
                    		broadcast.putParcelableArrayListExtra(EXTRA_DOWNLOAD_CITY_LIST, downloadCityList);
                    		sendBroadcast(broadcast);
                		}
                	}
                }).start();
            }
        }
        
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 在Service被停止时会退出统计指定城市的线程
        stop = true;
        synchronized (downloadCityList) {
		    downloadCityList.notifyAll();	
		}
        serverRegionDataInfoMap = null;
    }

    /* 
     * 统计出现在下载列表中的所有城市的地图信息及状态
     */
    private List<DownloadCity> statsDownloadCityList() {
        List<DownloadCity> list = new ArrayList<DownloadCity>();
        
        String downloadCityStr = TKConfig.getPref(getBaseContext(), TKConfig.PREFS_MAP_DOWNLOAD_CITYS, "");
        if (!TextUtils.isEmpty(downloadCityStr)) {
            try {
                /*
                 * 数据结构如下:
                 * 城市中文名,地图数据总大小,已经下载数据的大小,状态;城市中文名,地图数据总大小,已经下载数据的大小,状态
                 * es:
                 * 北京,561200,562400,3;广州,485655,385521,1
                 */
                String[] downloadCitysStrArr = downloadCityStr.split(";");
                
                for (int i = downloadCitysStrArr.length-1; i >= 0; i--) {
                    String str = downloadCitysStrArr[i];
                    String[] downloadCityStrArr = str.split(",");
                    String cName = downloadCityStrArr[0];
                    DownloadCity downloadCity = null;
                    
                    // 检查在下载列表中是否存在
                    for(int j = list.size()-1; j >= 0; j--) {
                        DownloadCity downloadCity1 = list.get(j);
                        if (downloadCity1.cityInfo.getCName().equals(cName)) {
                            downloadCity = downloadCity1;
                            break;
                        }
                    }
                    
                    if (downloadCity == null) {
                        CityInfo cityInfo = MapEngine.getCityInfo(MapEngine.getCityid(cName));
                        if(cityInfo == null)
                        	continue;
                        if (cityInfo.isAvailably()) {
                            downloadCity = new DownloadCity(cityInfo);
                            list.add(downloadCity);
                            downloadCity.totalSize = Integer.parseInt(downloadCityStrArr[1]);
                            downloadCity.downloadedSize = Integer.parseInt(downloadCityStrArr[2]);
                            downloadCity.state = Integer.parseInt(downloadCityStrArr[3]);
                            
                            // 在用户退出下载地图界面再进入时
                            // v4.20要求之前正在下载和等待的状态保持不变
                            // v4.30要求之前正在下载和等待的状态全部设置为暂停状态
                            if (downloadCity.state == DownloadCity.STATE_WAITING
                                    || downloadCity.state == DownloadCity.STATE_DOWNLOADING) {
                                downloadCity.state = DownloadCity.STATE_STOPPED;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 生成用于ExpandListView的列表
        List<DownloadCity> expandList = new ArrayList<DownloadCity>();
        for(int i = list.size()-1; i >= 0; i--) {
            DownloadCity downloadCity = list.get(i);
            MapDownloadActivity.addDownloadCity(getApplicationContext(), expandList, downloadCity, true);
        }
        return expandList;
    }
    
    /*
     * 统计当前城市的地图信息及状态
     */
    private DownloadCity statsCurrentCity() {
        CityInfo cityInfo = Globals.getCurrentCityInfo(false);
        if (cityInfo != null) {
            MapVersionQuery mapVersionQuery = new MapVersionQuery(getBaseContext());
            mapVersionQuery.setup(MapEngine.getRegionIdList(cityInfo.getCName()));
            mapVersionQuery.query();
            HashMap<Integer, ServerRegionDataInfo> regionVersionMap = mapVersionQuery.getServerRegionDataInfoMap();
            DownloadCity downloadCity = new DownloadCity(cityInfo);
            statsDownloadCity(downloadCity, regionVersionMap);
            return downloadCity;
        }
        return null;
    }
    
    /*
     * 统计未出现在下载列表中但是tigermap/map文件夹下已经存在地图数据文件的城市的地图信息及状态
     * @manualAddList 出现在下载列表中的城市列表
     */
    ArrayList<DownloadCity> statsDownloadCityListForExistData(List<DownloadCity> downloadCityList) {
        ArrayList<DownloadCity> list = new ArrayList<DownloadCity>();
        MapEngine mapEngine = MapEngine.getInstance();
        String mapPath = mapEngine.getMapPath();
        if (!TextUtils.isEmpty(mapPath)) {
            File dataRootDir = new File(mapPath);
            File[] mapFiles = dataRootDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return true;
                }
            });
            if (mapFiles != null) {
                // 遍历tigermap/map文件夹下的所有*.dat文件，通过文件名获取相应的Region名字，再得到RegionInfo
                for (int i = mapFiles.length-1; i >= 0; i--) {
                    File mapFile = mapFiles[i];
                    File[] regionFiles = mapFile.listFiles();
                    if (regionFiles == null) {
                        continue;
                    }
                    for (int j = regionFiles.length-1; j >= 0; j--) {
                        File regionFile = regionFiles[j];
                        String regionFileName = regionFile.getName();
                        if (!regionFileName.endsWith(".dat")) {
                            continue;
                        }
                        
                        String regionName = regionFileName.replace(".dat", "");
                        int regionId = MapEngine.getRegionId(regionName);
                        RegionInfo regionInfo = MapEngine.getRegionInfo(regionId);
                        if (null != regionInfo) {
                            String cName = regionInfo.getCCityName();
                            DownloadCity exist = null;
                            
                            // 检查在下载列表中是否存在
                            if (downloadCityList != null) {
                                for (int x = downloadCityList.size()-1; x >= 0; x--) {
                                    DownloadCity downloadCity = downloadCityList.get(x);
                                    if (downloadCity.cityInfo.getCName().equals(cName)) {
                                        exist = downloadCity;
                                        break;
                                    }
                                }
                            }

                            if (exist == null) {
                                // 检查在 list中是否存在
                                for (int x = list.size()-1; x >= 0; x--) {
                                    DownloadCity downloadCity = list.get(x);
                                    if (downloadCity.cityInfo.getCName().equals(cName)) {
                                        exist = downloadCity;
                                        break;
                                    }
                                }
                            }

                            // 都不存在时才创建一个DownloadCity
                            if (exist == null) {
                                CityInfo cityInfo = MapEngine.getCityInfo(MapEngine.getCityid(cName));
                                if(cityInfo == null)
                                	continue;
                                if (cityInfo.isAvailably()) {
                                    exist = new DownloadCity(cityInfo);
                                    statsDownloadCity(exist, queryServerRegionDataInfoMapInternally(context));
                                    list.add(exist);
                                }
                            }                        
                        }
                    }
                }
            }
        }
        return list;
    }
    
    /*
     * 统计指定下载城市的地图信息及状态
     * @downloadCity
     * @serverRegionDataInfoMap 服务器端Region的地图信息
     */
    public static void statsDownloadCity(DownloadCity downloadCity, HashMap<Integer, ServerRegionDataInfo> serverRegionDataInfoMap) {
        List<Integer> list = MapEngine.getRegionIdList(downloadCity.cityInfo.getCName());
        int totalSize = 0;
        int downloadedSize = 0;
        boolean maybeUpgrade = false;
        
        for (int j = list.size()-1; j >= 0; j--) {
            int id = list.get(j);
            LocalRegionDataInfo regionMapInfo = MapEngine.getLocalRegionDataInfo(id);
            if (regionMapInfo != null) {
                totalSize += regionMapInfo.getTotalSize();
                downloadedSize += regionMapInfo.getDownloadedSize();
            }
            
            // 检查是否可升级
            if (serverRegionDataInfoMap != null && serverRegionDataInfoMap.containsKey(id)) {
                RegionMetaVersion regionMetaVersion = MapEngine.getRegionMetaVersion(id);
                ServerRegionDataInfo serverRegionDataInfo = serverRegionDataInfoMap.get(id); 
                if (regionMetaVersion != null && serverRegionDataInfo != null) {
                    String localVersion = regionMetaVersion.toString();
                    String serverVersion = serverRegionDataInfo.getRegionVersion();
                    
                    if (TextUtils.isEmpty(localVersion) == false
                            && TextUtils.isEmpty(localVersion) == false
                            && localVersion.equalsIgnoreCase(RegionMetaVersion.NONE) == false
                            && serverVersion.equalsIgnoreCase(localVersion) == false) {
                    	maybeUpgrade = true;
                    }
                }
                if(regionMapInfo == null && serverRegionDataInfo != null) {
                	totalSize += serverRegionDataInfo.getTotalSize();
                }
            }
        }

        downloadCity.totalSize = totalSize;
        downloadCity.downloadedSize = downloadedSize;
        
        
        if (maybeUpgrade) {
            downloadCity.state = DownloadCity.STATE_CAN_BE_UPGRADE;
        } else if (downloadCity.getPercent() >= MapDownloadActivity.PERCENT_COMPLETE) {
    		downloadCity.state = DownloadCity.STATE_COMPLETED;
        } else if (downloadCity.state == DownloadCity.STATE_COMPLETED) {
           	downloadCity.state = DownloadCity.STATE_STOPPED;
        }
        
    }
}
