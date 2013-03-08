/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.radar.Alarms;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

import java.util.Calendar;

/**
 * 
 * @author pengwenyue
 *
 */
public class PullService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                long currentTimeMillis = System.currentTimeMillis();
                Calendar now = Calendar.getInstance();
                now.setTimeInMillis(currentTimeMillis);
                
                Context context = getApplicationContext();
                
                // 获取当前城市
                CityInfo currentCityInfo = Globals.getLastCityInfo(context);
                // 安装后从来没有使用过老虎宝典的情况
                if (currentCityInfo == null) {
                    exitService(null);
                    return;
                }
                
                // 获取定位城市
                CityInfo locationCityInfo = null;
                LocationQuery locationQuery = LocationQuery.getInstance(context);
                Location location = locationQuery.getLocation();
                if (location != null) {
                    MapEngine mapEngine = MapEngine.getInstance();
                    try {
                        mapEngine.initMapDataPath(context, false);
                        int cityId = mapEngine.getCityId(new Position(location.getLatitude(), location.getLongitude()));
                        locationCityInfo = mapEngine.getCityInfo(cityId);
                    } catch (APIException exception) {
                        exception.printStackTrace();
                    }
                }
                
                if (currentCityInfo != null
                        && locationCityInfo != null
                        && currentCityInfo.isAvailably()
                        && locationCityInfo.isAvailably()
                        && currentCityInfo.getId() == locationCityInfo.getId()) {    
                    // TODO: 去服务器上拉数据
                    
                } else {
                    
                }
                
                // 定时下一个唤醒
                Calendar next = Calendar.getInstance();
                exitService(next);
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
    
    void exitService(Calendar next) {
        Intent name = new Intent(this, PullService.class);
        if (next != null) {
            Context context = getApplicationContext();
            TKConfig.setPref(context,
                    TKConfig.PREFS_RADAR_PULL_ALARM, 
                    Alarms.SIMPLE_DATE_FORMAT.format(next.getTime()));
            Alarms.enableAlarm(context, next.getTimeInMillis(), name);
        }
        stopService(name);
    }
}
