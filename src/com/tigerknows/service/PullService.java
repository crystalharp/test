/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.PullMessage;
import com.tigerknows.model.PullMessage.Message;
import com.tigerknows.radar.Alarms;
import com.tigerknows.radar.RadarReceiver;
import com.tigerknows.radar.TKNotificationManager;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.Random;

/**
 * 
 * @author pengwenyue
 *
 */
public class PullService extends Service {
    
    static final String TAG = "PullService";

    @Override
    public void onCreate() {
        super.onCreate();
        LogWrapper.d(TAG, "onCreate()");
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                Calendar next = null;
                
                Context context = getApplicationContext();
                
                // 获取当前城市
                CityInfo currentCityInfo = Globals.getLastCityInfo(context);
                
                // 安装后从来没有使用过老虎宝典的情况
                if (currentCityInfo == null) {
                    exitService(next);
                    return;
                }
                
                long currentTimeMillis = System.currentTimeMillis();
                next = Calendar.getInstance();
                next.setTimeInMillis(currentTimeMillis);
                next.add(Calendar.HOUR_OF_DAY, 1);
                
                // 获取定位城市
                CityInfo locationCityInfo = null;
                LocationQuery locationQuery = LocationQuery.getInstance(context);
                Location location = locationQuery.getLocation();
                Position position = null;
                if (location != null) {
                    MapEngine mapEngine = MapEngine.getInstance();
                    try {
                        mapEngine.initMapDataPath(context, false);
                        position = mapEngine.latlonTransform(new Position(location.getLatitude(), location.getLongitude()));
                        int cityId = mapEngine.getCityId(position);
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
                    DataQuery dataQuery = new DataQuery(context);
                    Hashtable<String, String> criteria = new Hashtable<String, String>();
                    String messageIdList = TKConfig.getPref(context, TKConfig.PREFS_RADAR_RECORD_MESSAGE_ID_LIST, "");
                    criteria.put(DataQuery.SERVER_PARAMETER_MESSAGE_ID_LIST, messageIdList);
                    criteria.put(DataQuery.SERVER_PARAMETER_CITY_ID_FOR_PULL_MESSAGE, String.valueOf(currentCityInfo.getId()));
                    criteria.put(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
                    criteria.put(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
                    dataQuery.setup(criteria, currentCityInfo.getId());
                    dataQuery.query();
                    PullMessage pullMessage = dataQuery.getPullMessage();
                    if (pullMessage != null) {
                        recordPullMessage(context, pullMessage);
                        next = null;
                        TKNotificationManager.notify(context, pullMessage.getMessage());
                    }
                }
                
                // 定时下一个唤醒
                exitService(next);
            }
        }).start();
    }

    public void recordPullMessage(Context context, PullMessage pullMessage) {
        long recordMessageUpperLimit = pullMessage.getRecordMessageUpperLimit();
        String nextRequestDate = pullMessage.getNextRequsetDate();
        TKConfig.setPref(context, TKConfig.PREFS_RADAR_PULL_ALARM, nextRequestDate);
        TKConfig.setPref(context, TKConfig.PREFS_RADAR_RECORD_MESSAGE_UPPER_LIMIT, String.valueOf(pullMessage.getRecordMessageUpperLimit()));
        String messageIdList = TKConfig.getPref(context, TKConfig.PREFS_RADAR_RECORD_MESSAGE_ID_LIST, "");
        String[] list = messageIdList.split("_");
        StringBuilder s = new StringBuilder();
        Message message = pullMessage.getMessage();
        if (message != null) {
            s.append(message.getId());
        }
        int length = list.length;
        long limit = recordMessageUpperLimit - 1;
        for(int i = 0; i < limit && i < length; i++) {
            s.append("_");
            s.append(list[i]);
        }
        TKConfig.setPref(context, TKConfig.PREFS_RADAR_RECORD_MESSAGE_ID_LIST, s.toString());
        
        Intent pullIntent = new Intent(RadarReceiver.ACTION_PULL);
        Alarms.disableAlarm(context, pullIntent);
        
        Calendar next = Alarms.calculateAlarm(nextRequestDate+" " + makeRandomHour() + ":" + makeRandom(60) + ":" + makeRandom(60));
        Alarms.enableAlarm(context, next.getTimeInMillis(), pullIntent);
    }
    
    int makeRandomHour() {
        int hour = 6;
        Random ran =new Random(System.currentTimeMillis()); 
        hour += ran.nextInt(16);
        return hour;
    }
    
    int makeRandom(int n) {
        int value = 0;
        Random ran =new Random(System.currentTimeMillis()); 
        value = ran.nextInt(n);
        return value;
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
        Context context = getApplicationContext();
        if (next != null) {
            TKConfig.setPref(context,
                    TKConfig.PREFS_RADAR_PULL_ALARM, 
                    Alarms.SIMPLE_DATE_FORMAT.format(next.getTime()));
            Intent intent = new Intent(RadarReceiver.ACTION_PULL);
            Alarms.enableAlarm(context, next.getTimeInMillis(), intent);
        }
        Intent name = new Intent(context, PullService.class);
        stopService(name);
    }
}
