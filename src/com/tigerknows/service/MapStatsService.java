/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.tigerknows.TKConfig;
import com.tigerknows.MapDownload.DownloadCity;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.RegionMapInfo;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.maps.MapEngine.RegionInfo;
import com.tigerknows.maps.MapEngine.RegionMetaVersion;
import com.tigerknows.model.MapVersionQuery;
import com.tigerknows.model.MapVersionQuery.RegionDataInfo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author pengwenyue
 *
 */
public class MapStatsService extends Service {
    
    public static final String ACTION_MAP_STATS_COMPLATE = "action_map_stats_complate";

    
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                MapEngine mapEngine = MapEngine.getInstance();
                if (mapEngine.statsMapStart()) {
                    List<DownloadCity> downloadCityList = countMapCityList();
                    mapEngine.statsMapEnd(downloadCityList, true);
                }
                Intent intent = new Intent(ACTION_MAP_STATS_COMPLATE);
                sendBroadcast(intent);
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
    }

    // 统计以前有下载操作的城市
    private List<DownloadCity> countDownloadCityList() {
        MapEngine mapEngine = MapEngine.getInstance();
        List<DownloadCity> addedCityList = new ArrayList<DownloadCity>();
        
        String downloadCityStr = TKConfig.getPref(getBaseContext(), TKConfig.PREFS_MAP_DOWNLOAD_CITYS, "");
        if (!TextUtils.isEmpty(downloadCityStr)) {
            try {
            String[] downloadCitysStrArr = downloadCityStr.split(";");
            
            for (String str : downloadCitysStrArr) {
                String[] downloadCityStrArr = str.split(",");
                String cName = downloadCityStrArr[0];
                DownloadCity downloadCity = null;
                for(DownloadCity downloadCity1 : addedCityList) {
                    if (downloadCity1.getCityInfo().getCName().equals(cName)) {
                        downloadCity = downloadCity1;
                        break;
                    }
                }
                if (downloadCity == null) {
                    CityInfo cityInfo = mapEngine.getCityInfo(mapEngine.getCityid(cName));
                    downloadCity = new DownloadCity(cityInfo);
                    
                    addedCityList.add(downloadCity);
                }
                
                downloadCity.setTotalSize(0);
                downloadCity.setDownloadedSize(0);

                int state = Integer.parseInt(downloadCityStrArr[3]);
                // 如果以前是正在下载，那么需要将它设为等待下载状态
                if (state == DownloadCity.DOWNLOADING) {
                    state = DownloadCity.WAITING;
                }
                
                if (state == DownloadCity.WAITING || state == DownloadCity.STOPPED) {
                    downloadCity.setState(state);
                }
            }
            } catch (Exception e) {
            }
        }
        return addedCityList;
    }

    // 统计手机上存在地图数据的城市名称列表
    private List<DownloadCity> countMapCityList() {
        MapEngine mapEngine = MapEngine.getInstance();
        List<DownloadCity> addedMapCityList = countDownloadCityList();
        String dataRoot = MapEngine.getInstance().getDataRoot();
        if (!TextUtils.isEmpty(dataRoot)) {
            File dataRootDir = new File(dataRoot);
            File[] mapFiles = dataRootDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return true;
                }
            });
            if (mapFiles == null) {
                return addedMapCityList;
            }
            for (File mapFile : mapFiles) {
                File[] regionFiles = mapFile.listFiles();
                if (regionFiles == null) {
                    continue;
                }
                for (File regionFile : regionFiles) {
                    String regionFileName = regionFile.getName();
                    if (!regionFileName.endsWith(".dat")) {
                        continue;
                    }
                    String regionName = regionFileName.replace(".dat", "");
                    int regionId = mapEngine.getRegionId(regionName);
                    RegionInfo regionInfo = mapEngine.getRegionInfo(regionId);
                    if (null != regionInfo) {
                        String cName = regionInfo.getCCityName();
                        DownloadCity downloadCity = null;
                        for (DownloadCity downloadCity1 : addedMapCityList) {
                            if (downloadCity1.getCityInfo().getCName().equals(cName)) {
                                downloadCity = downloadCity1;
                                break;
                            }
                        }
                        
                        if (downloadCity == null) {
                            CityInfo cityInfo = mapEngine.getCityInfo(mapEngine.getCityid(cName));
                            downloadCity = new DownloadCity(cityInfo);
                                                    
                            addedMapCityList.add(downloadCity);
                        }                        
                    }
                }
            }
        }
        
        List<Integer> list = new ArrayList<Integer>();
        for(DownloadCity downloadCity : addedMapCityList) {
            list.addAll(downloadCity.getRegionIdList());
        }
        MapVersionQuery mapVersionQuery = new MapVersionQuery(getBaseContext());
        mapVersionQuery.setup(list);
        mapVersionQuery.query();
        HashMap<Integer, RegionDataInfo> regionVersionMap = mapVersionQuery.getRegionVersion();
        
        for(DownloadCity downloadCity : addedMapCityList) {
            countDownloadCity(downloadCity, regionVersionMap);
        }

        return addedMapCityList;
    }
    
    public static void countDownloadCity(DownloadCity downloadCity, HashMap<Integer, RegionDataInfo> regionVersionMap) {
        MapEngine mapEngine = MapEngine.getInstance();
        List<Integer> list = downloadCity.getRegionIdList();
        int totalSize = 0;
        int downloadedSize = 0;
        boolean maybeUpdate = false;
        for (int id : list) {
            RegionMapInfo regionMapInfo = mapEngine.getRegionStat(id);
            if (regionMapInfo != null) {
                totalSize += regionMapInfo.getTotalSize();
                downloadedSize += regionMapInfo.getDownloadedSize();
            }
            
            // 查询地图数据版本是否可升级
            String serverDataVersion = "";
            if (regionVersionMap != null && regionVersionMap.containsKey(id)) {
                RegionMetaVersion regionMetaVersion = mapEngine.getRegionVersion(id);
                if (regionMetaVersion != null) {
                String phoneDataVersion = regionMetaVersion.toString();
                serverDataVersion = regionVersionMap.get(id).getRegionVersion();
                if (phoneDataVersion != null && !phoneDataVersion.equalsIgnoreCase(serverDataVersion) && !phoneDataVersion.equalsIgnoreCase(RegionMetaVersion.INVALD_VERSION)) {
                    String[] phoneVersion = phoneDataVersion.replace(".", ",").split(",");
                    String[] serverVersion = serverDataVersion.replace(".", ",").split(",");
//                    LogWrapper.d("MapDownloadDialog", "countExistMapCityList() cityName="+downloadCity.getCityInfo().getCName()+", phoneDataVersion="+phoneDataVersion+", serverDataVersion="+serverDataVersion);
                    try {
                        for(int i = 0; i < phoneVersion.length; i++) {                                    
                            if (Integer.parseInt(phoneVersion[i]) < Integer.parseInt(serverVersion[i])) {
                                maybeUpdate = true;
                                break;
                            } else if (Integer.parseInt(phoneVersion[i]) > Integer.parseInt(serverVersion[i])) {
                                break;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
                }
            }
        }

        downloadCity.setTotalSize(totalSize);
        downloadCity.setDownloadedSize(downloadedSize);
        if (maybeUpdate) {
            downloadCity.setState(DownloadCity.MAYUPDATE);
        } else if (downloadCity.getDownloadedSize() >= downloadCity.getTotalSize()-1024 && downloadCity.getDownloadedSize() > 1024) {
            downloadCity.setState(DownloadCity.COMPLETED);
        } else if (downloadCity.getState() == DownloadCity.MAYUPDATE) {
            downloadCity.setState(DownloadCity.STOPPED);
        }
    }
}
