/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.tigerknows.MapDownload;
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
    public void onStart(final Intent intent, int startId) {
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                    Thread.sleep(10*1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                MapEngine mapEngine = MapEngine.getInstance();
                if (mapEngine.statsMapStart()) {
                    List<DownloadCity> downloadCityList = countDownloadCityList();
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
    private List<DownloadCity> countSavedDownloadCityList() {
        MapEngine mapEngine = MapEngine.getInstance();
        List<DownloadCity> list = new ArrayList<DownloadCity>();
        
        String downloadCityStr = TKConfig.getPref(getBaseContext(), TKConfig.PREFS_MAP_DOWNLOAD_CITYS, "");
        if (!TextUtils.isEmpty(downloadCityStr)) {
            try {
                String[] downloadCitysStrArr = downloadCityStr.split(";");
                
                for (int i = 0, size = downloadCitysStrArr.length; i < size; i++) {
                    String str = downloadCitysStrArr[i];
                    String[] downloadCityStrArr = str.split(",");
                    String cName = downloadCityStrArr[0];
                    DownloadCity downloadCity = null;
                    for(int j = list.size()-1; j >= 0; j--) {
                        DownloadCity downloadCity1 = list.get(j);
                        if (downloadCity1.getCityInfo().getCName().equals(cName)) {
                            downloadCity = downloadCity1;
                            break;
                        }
                    }
                    if (downloadCity == null) {
                        CityInfo cityInfo = mapEngine.getCityInfo(mapEngine.getCityid(cName));
                        if (cityInfo.isAvailably() && cityInfo.getId() > 0) {   // && cityInfo.getId() > 0 是排除全国概要
                            downloadCity = new DownloadCity(cityInfo);
                        }
                    }
                    
                    if (downloadCity != null) {
                        list.add(downloadCity);
                        downloadCity.isManual = 1;
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
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    // 统计手机上存在地图数据的城市名称列表
    private List<DownloadCity> countDownloadCityList() {
        MapEngine mapEngine = MapEngine.getInstance();
        List<DownloadCity> savedList = countSavedDownloadCityList();
        String mapPath = MapEngine.getInstance().getMapPath();
        if (!TextUtils.isEmpty(mapPath)) {
            File dataRootDir = new File(mapPath);
            File[] mapFiles = dataRootDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return true;
                }
            });
            if (mapFiles != null) {
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
                        int regionId = mapEngine.getRegionId(regionName);
                        RegionInfo regionInfo = mapEngine.getRegionInfo(regionId);
                        if (null != regionInfo) {
                            mapEngine.addLastRegionId(regionId, true);
                            String cName = regionInfo.getCCityName();
                            DownloadCity exist = null;
                            for (int x = savedList.size()-1; x >= 0; x--) {
                                DownloadCity downloadCity = savedList.get(x);
                                if (downloadCity.getCityInfo().getCName().equals(cName)) {
                                    exist = downloadCity;
                                    break;
                                }
                            }
                            
                            if (exist == null) {
                                CityInfo cityInfo = mapEngine.getCityInfo(mapEngine.getCityid(cName));
                                if (cityInfo.isAvailably() && cityInfo.getId() > 0) {   // && cityInfo.getId() > 0 是排除全国概要
                                    exist = new DownloadCity(cityInfo);
                                    savedList.add(exist);
                                }
                            }                        
                        }
                    }
                }
            }
        }
        
        List<Integer> list = new ArrayList<Integer>();
        for(int i = savedList.size()-1; i >= 0; i--) {
            DownloadCity downloadCity = savedList.get(i);
            list.addAll(downloadCity.getRegionIdList());
        }
        MapVersionQuery mapVersionQuery = new MapVersionQuery(getBaseContext());
        mapVersionQuery.setup(list);
        mapVersionQuery.query();
        HashMap<Integer, RegionDataInfo> regionVersionMap = mapVersionQuery.getRegionVersion();
        
        for(int i = savedList.size()-1; i >= 0; i--) {
            DownloadCity downloadCity = savedList.get(i);
            countDownloadCity(downloadCity, regionVersionMap);
        }
        
        List<DownloadCity> expandList = new ArrayList<DownloadCity>();
        for(int i = savedList.size()-1; i >= 0; i--) {
            DownloadCity downloadCity = savedList.get(i);
            MapDownload.addDownloadCity(getApplicationContext(), expandList, downloadCity, downloadCity.isManual == 1);
        }
        
        return expandList;
    }
    
    public static void countDownloadCity(DownloadCity downloadCity, HashMap<Integer, RegionDataInfo> regionVersionMap) {
        MapEngine mapEngine = MapEngine.getInstance();
        List<Integer> list = downloadCity.getRegionIdList();
        int totalSize = 0;
        int downloadedSize = 0;
        boolean maybeUpdate = false;
        for (int j = list.size()-1; j >= 0; j--) {
            int id = list.get(j);
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
                        e.printStackTrace();
                    }
                }
                }
            }
        }

        downloadCity.setTotalSize(totalSize);
        downloadCity.setDownloadedSize(downloadedSize);
        if (maybeUpdate) {
            downloadCity.setState(DownloadCity.MAYUPDATE);
        } else if (downloadCity.getPercent() >= MapDownload.PERCENT_COMPLETE) {
            downloadCity.setState(DownloadCity.COMPLETED);
        } else if (downloadCity.getState() == DownloadCity.MAYUPDATE) {
            downloadCity.setState(DownloadCity.STOPPED);
        }
    }
}
