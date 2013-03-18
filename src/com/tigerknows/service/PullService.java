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
import com.tigerknows.model.BaseQuery;
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
import android.text.TextUtils;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

/**
 * 
 * @author pengwenyue
 * 这个类名为Pull，实际上做法是随机找一个时间连接服务器，去查询服务器上有没有信息
 * 如果有的话查询出来发本地通知
 *
 */
public class PullService extends Service {
    
    static final String TAG = "PullService";
    static int fail = 0;
    final static int MaxFail = 3;
    final static int requestStartHour = 9;
    final static int requestEndHour = 21;

    @Override
    public void onCreate() {
        super.onCreate();
        LogWrapper.d(TAG, "onCreate()");
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                TKConfig.readConfig();
                long currentTimeMillis = System.currentTimeMillis();

                Calendar requestCal = Calendar.getInstance();
                requestCal.setTimeInMillis(currentTimeMillis);
                Calendar next = (Calendar) requestCal.clone();
                //如果不在请求时间范围内，肯定是请求失败被推迟的定时器，推迟到第二天的随机时间再请求
                if (requestCal.getTime().getHours() > requestEndHour ||requestCal.getTime().getHours() < requestStartHour) {
                    next = Alarms.calculateRandomAlarmInNextDay(requestCal, requestStartHour, requestEndHour);
                    exitService(next);
                }
                //TODO:普通失败，推迟一天
                next.add(Calendar.MINUTE, 1);
//                next.add(Calendar.HOUR_OF_DAY, 1);
                
                Context context = getApplicationContext();
                
                // 获取当前城市
                CityInfo currentCityInfo = Globals.getLastCityInfo(context);
                
                LogWrapper.d(TAG, "currentCityInfo="+currentCityInfo);
                // 安装后从来没有使用过老虎宝典的情况
                if (currentCityInfo == null || currentCityInfo.isAvailably() == false) {
                    exitService(next);
                    return;
                }
                
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

                LogWrapper.d(TAG, "locationCityInfo="+locationCityInfo);
                if (locationCityInfo != null
                        && locationCityInfo.isAvailably()
                        && currentCityInfo.getId() == locationCityInfo.getId()) {    
                    // TODO: 去服务器上拉数据
                    DataQuery dataQuery = new DataQuery(context);
                    Hashtable<String, String> criteria = new Hashtable<String, String>();
                    String messageIdList = TKConfig.getPref(context, TKConfig.PREFS_RADAR_RECORD_MESSAGE_ID_LIST, "");
                    String lastSucceedTime = TKConfig.getPref(context, TKConfig.PREFS_RADAR_RECORD_LAST_SUCCEED_TIME, "");
                    criteria.put(BaseQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_PULL_MESSAGE);
                    criteria.put(DataQuery.SERVER_PARAMETER_LOCATION_CITY, String.valueOf(locationCityInfo.getId()));
                    criteria.put(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
                    criteria.put(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
                    criteria.put(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE, String.valueOf(position.getLon()));
                    criteria.put(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE, String.valueOf(position.getLat()));
                    criteria.put(DataQuery.SERVER_PARAMETER_MESSAGE_ID_LIST, messageIdList);
                    if (!TextUtils.isEmpty(lastSucceedTime)) {
                        criteria.put(DataQuery.SERVER_PARAMETER_LAST_PULL_DATE, lastSucceedTime);
                    }
                    dataQuery.setup(criteria, currentCityInfo.getId());
                    dataQuery.query();
                    PullMessage pullMessage = dataQuery.getPullMessage();
                    if (pullMessage != null) {
                        fail = 0;
                        TKConfig.setPref(context, TKConfig.PREFS_RADAR_RECORD_LAST_SUCCEED_TIME, 
                                Alarms.SIMPLE_DATE_FORMAT.format(requestCal.getTime()));
                        next = recordPullMessage(context, pullMessage, requestCal);
                        LogWrapper.d(TAG, "pull succeeded, fail = " + fail);
                    } else {
                        fail += 1;
                    }
                }
                
                // 定时下一个唤醒
                exitService(next);
            }
        }).start();
    }

    public Calendar recordPullMessage(Context context, PullMessage pullMessage, Calendar requestCal) {
        
        long recordMessageUpperLimit = pullMessage.getRecordMessageUpperLimit();
        long requsetIntervalDays = pullMessage.getRequsetIntervalDays();
        TKConfig.setPref(context, TKConfig.PREFS_RADAR_RECORD_MESSAGE_UPPER_LIMIT, String.valueOf(pullMessage.getRecordMessageUpperLimit()));
        String messageIdList = TKConfig.getPref(context, TKConfig.PREFS_RADAR_RECORD_MESSAGE_ID_LIST, "");
        String[] list = messageIdList.split("_");
        StringBuilder s = new StringBuilder();
        List<Message> messageList = pullMessage.getMessageList();
        
        //由于产品还没想好怎么处理多条数据，目前message只显示列表中第一条。
        Message message = messageList.get(0);
        if (message != null) {
            s.append(message.getId());
            TKNotificationManager.notify(context, message);
        }
        int length = list.length;
        long limit = recordMessageUpperLimit - 1;
        for(int i = 0; i < limit && i < length; i++) {
            s.append("_");
            s.append(list[i]);
        }
        TKConfig.setPref(context, TKConfig.PREFS_RADAR_RECORD_MESSAGE_ID_LIST, s.toString());
        
        return Alarms.calculateAlarm(requestCal, requsetIntervalDays);
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
        if (fail >= MaxFail || next == null) {
            fail = 0;
            next = Alarms.calculateRandomAlarmInNextDay(next, requestStartHour, requestEndHour);
        }
        
        Context context = getApplicationContext();
        Intent intent = new Intent(RadarReceiver.ACTION_PULL);
        Alarms.disableAlarm(context, intent);
        Alarms.enableAlarm(context, next.getTimeInMillis(), intent);
        if (fail > 0) {
            LogWrapper.d(TAG, "Pull failed " + fail + " times, next alarm:" + next.getTime().toLocaleString());
        } else {
            LogWrapper.d(TAG, "Pull succeeded, next alarm:" + next.getTime().toLocaleString());
        }
        
        Intent name = new Intent(context, PullService.class);
        stopService(name);
    }
}
