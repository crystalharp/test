/*
 * Copyright (C) 2010 xupeng@tigerknows.com
 */

package com.tigerknows.service;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.PullMessage;
import com.tigerknows.model.Response;
import com.tigerknows.model.LocationQuery.LocationParameter;
import com.tigerknows.model.LocationQuery.TKCellLocation;
import com.tigerknows.model.LocationQuery.TKNeighboringCellInfo;
import com.tigerknows.model.LocationQuery.TKScanResult;
import com.tigerknows.model.PullMessage.Message;
import com.tigerknows.provider.LocationTable;
import com.tigerknows.radar.Alarms;
import com.tigerknows.radar.RadarReceiver;
import com.tigerknows.radar.TKNotificationManager;
import com.tigerknows.util.Utility;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author xupeng
 * 这个类名为Pull，实际上做法是随机找一个时间连接服务器，去查询服务器上有没有信息
 * 如果有的话查询出来发本地通知
 * 整个推送服务的机制分为以下几步：
 * 1.定时器初始化。
 *   这个部分在radar/AlarmInitReceiver.java中，它接收若干类型的广播intent启动，具体接受的
 *   广播类型在AndroidManifest.xml中，初始设计接受五种广播，分别是系统的启动，时间或时区变更，
 *   软件第一次启动，设置推送开启。初始化时获取TKConfig的prefs里面设置的下一次定时器时间，计算
 *   出定时器calendar并设置。
 *   变更1：时间或时区变更不再在定时器初始化中进行接收，因为这两个行为不会丢定时器，在PullService
 *   中进行处理即可，不需要重新设置定时器。它所接受的广播应该是那些会导致没有定时器或者定时器丢失的
 *   行为。时间或时区变更会导致的问题是过期的定时器会被立即触发而无法disable掉，所以在PullService
 *   中进行了检查，如果是和所设置的定时器不符的intent调用则推迟一小时再来。
 *   变更2：在PullService中并不能准确的检查是否是过期的定时器，于是决定采用相对时间来设置定时器，
 *   这样就不会受时间改变的影响。
 *   变更3：在radar/AlarmAdjustReceiver.java中添加了定时器时钟校准机制，使得定时器不受
 *   手机时钟修改的影响。具体做法见对应文件的注释。
 * 2.计算和设置定时器
 *   这个部分在radar/Alarms.java中。
 *   计算定时器有若干个函数，返回calendar对象。
 *   设置定时器统一使用enableAlarm函数，每次调用都要把要设置的时间写到TKConfig的prefs里。设置
 *   定时器之前要进行定时器检查，确保使用的定时器在当前时间之后。
 *   checkAlarm检查如果在当前时间之前则返回一个当前时间一小时之后的定时器。
 * 3.PullService被定时器调用
 *   RadarReceiver收到定时器的Intent调用，启动PullService
 *   PullService会获取当前定位信息并连接服务器查询是否有新的推送信息，如果有则从服务器获取下来发送
 *   本地推送信息。
 * 4.联网触发机制
 *   经过一段时间运营之后发现收不到推送消息的为数不少,且原因可能是网络不好.于是决定加入联网触发机制.
 *   联网触发作为一种模式和原有的定时器逻辑为互斥的关系,定时器触发失败(一天累计三次重试失败)后就会
 *   变为联网触发模式.
 *   联网触发模式下不再走定时器逻辑,重新启动之类的行为也不会初始化定时器,只是在每次联网成功时设置
 *   一个5分钟后的定时器
 *
 */
public class PullService extends Service {
    
    static final String TAG = "PullService";
    final static int MaxFail = 3;
    public final static int requestStartHour = 9;
    public final static int requestEndHour = 21;
    int fail = 0;
    int requsetIntervalDays;
    
    public final static int STAT_SUCCESS = 0;
    public final static int STAT_NULL = 1;
    public final static int STAT_NORMAL_FAIL = 2;
    public final static int STAT_RAND_TIME_NEXT_DAY = 3;
    
    public static PullAlarmAction alarmAction = new PullAlarmAction();
    
    public static String TRIGGER_MODE_NET = "net";
    public static String TRIGGER_MODE_ALARM = "alarm";

    @Override
    public void onCreate() {
        super.onCreate();
        LogWrapper.d(TAG, "onCreate()");
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                TKConfig.readConfig();
                long currentTimeMillis = System.currentTimeMillis();
                requsetIntervalDays = 1;
                
                Context context = getApplicationContext();
                
                try {
                    String s = TKConfig.getPref(context, TKConfig.PREFS_RADAR_PULL_FAILED_TIMES, "0");
                    fail = Integer.parseInt(s);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Calendar requestCal = Calendar.getInstance();
                requestCal.setTimeInMillis(currentTimeMillis);
                //如果不在请求时间范围内，肯定是请求失败被推迟的定时器，推迟到第二天的随机时间再请求
                if (requestCal.getTime().getHours() >= requestEndHour ||requestCal.getTime().getHours() < requestStartHour) {
                    exitService(STAT_RAND_TIME_NEXT_DAY, requestCal);
                    return;
                }
                
                // 获取当前城市
                CityInfo currentCityInfo = Globals.getLastCityInfo(getApplicationContext());
                
                LogWrapper.d(TAG, "currentCityInfo="+currentCityInfo);
                // 安装后从来没有使用过老虎宝典的情况
                if (currentCityInfo == null || currentCityInfo.isAvailably() == false) {
                    exitService(STAT_NULL, requestCal);
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
                        mapEngine.initMapDataPath(getApplicationContext());
                        position = mapEngine.latlonTransform(new Position(location.getLatitude(), location.getLongitude()));
                        int cityId = mapEngine.getCityId(position);
                        locationCityInfo = mapEngine.getCityInfo(cityId);
                    } catch (APIException e) {
                        e.printStackTrace();
                    }
                } else {
                    exitService(STAT_NORMAL_FAIL, requestCal);
                    return;
                }
                LogWrapper.d(TAG, "locationCityInfo="+locationCityInfo);

                if (locationCityInfo.isAvailably()
                        && currentCityInfo.getId() == locationCityInfo.getId()) {    
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
                    if (!TextUtils.isEmpty(messageIdList)) {
                        criteria.put(DataQuery.SERVER_PARAMETER_MESSAGE_ID_LIST, messageIdList);
                    }
                    if (!TextUtils.isEmpty(lastSucceedTime)) {
                        criteria.put(DataQuery.SERVER_PARAMETER_LAST_PULL_DATE, lastSucceedTime);
                    }
                    LogWrapper.d(TAG, criteria.toString());
                    dataQuery.setup(criteria, currentCityInfo.getId());
                    dataQuery.query();
                    Response response = dataQuery.getResponse();
                    if (response != null && response.getResponseCode() == 200 && response instanceof PullMessage) {
                        PullMessage pullMessage = (PullMessage) response;
                        processPullMessage(context, pullMessage, requestCal);
                        //推送成功,设置为定时器触发模式
                        TKConfig.setPref(context, TKConfig.PREFS_RADAR_PULL_TRIGGER_MODE, TRIGGER_MODE_ALARM);
                        exitService(STAT_SUCCESS, requestCal);
                        return;
                    }
                } else {
                    //定位城市与设置城市不符，则跳到第二天的随机时间
                    exitService(STAT_RAND_TIME_NEXT_DAY, requestCal);
                    return;
                }
                
                // 退出并设置下一个定时器
                exitService(STAT_NORMAL_FAIL, requestCal);
            }
        }).start();
    }

    public void processPullMessage(Context context, PullMessage pullMessage, Calendar requestCal) {
        
        long recordMessageUpperLimit = pullMessage.getRecordMessageUpperLimit();
        requsetIntervalDays = pullMessage.getRequsetIntervalDays();
        LogWrapper.d(TAG, "interval day:" + requsetIntervalDays);
        TKConfig.setPref(context, TKConfig.PREFS_RADAR_RECORD_MESSAGE_UPPER_LIMIT, String.valueOf(pullMessage.getRecordMessageUpperLimit()));
        String messageIdList = TKConfig.getPref(context, TKConfig.PREFS_RADAR_RECORD_MESSAGE_ID_LIST, "");
        StringBuilder s = new StringBuilder();
        List<Message> messageList = pullMessage.getMessageList();

        //由于产品还没想好怎么处理多条数据，目前message只显示列表中第一条。
        if (messageList != null && messageList.size() > 0) {
            Message message = messageList.get(0);
            s.append(message.getId());
            //如果这次的消息有id，则添加上此id，并写入配置文件
            if (s.length() > 0) {
                s.append("_");
                //如果配置文件中有msgIdList，则处理一下这个id列表，重新拼成字符串。
                if (!TextUtils.isEmpty(messageIdList)) {
                    String[] list = messageIdList.split("_");
                    long limit = recordMessageUpperLimit - 1;
                    limit = Math.min(limit, list.length);
                    for (int i = 0; i < limit; i++){
                        s.append(list[i]);
                        s.append("_");
                    }
                }
                //按照上面的代码生成字符串会在结尾带有下划线，下面一句去掉结尾的下划线
                messageIdList = s.substring(0, s.length()-1);
                LogWrapper.d(TAG, "msgidList:" + messageIdList);
                
                TKConfig.setPref(context, TKConfig.PREFS_RADAR_RECORD_LAST_SUCCEED_TIME, 
                        Alarms.SIMPLE_DATE_FORMAT.format(requestCal.getTime()));
                TKConfig.setPref(context, TKConfig.PREFS_RADAR_RECORD_MESSAGE_ID_LIST, messageIdList);
            }
            TKNotificationManager.notify(context, message);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    void exitService(int status, Calendar now) {
        LogWrapper.d(TAG, "failed times = " + fail);
        
        Context context = getApplicationContext();
        
        if (getTriggerMode(context).equals(TRIGGER_MODE_NET)) {
            //如果是网络触发模式,则不设置定时器,只把失败次数归零
            LogWrapper.d(TAG, "failed in net trigger mode, do not set Alarm.");
            fail = 0;
        } else {
            //如果是正常定时器触发模式
            Calendar next = (Calendar) now.clone();
            switch (status) {
            case STAT_NORMAL_FAIL:
                fail += 1; 
                next.add(Calendar.MINUTE, TKConfig.PullServiceFailedRetryTime);
                break;
            case STAT_RAND_TIME_NEXT_DAY:
                fail = 0;
                next = Alarms.calculateRandomAlarmInNextDay(next, requestStartHour, requestEndHour);
                break;
            case STAT_SUCCESS:
                fail = 0;
                next = Alarms.alarmAddDays(now, requsetIntervalDays);
                break;
            default:
                break;
            }
            if (fail >= MaxFail) {
                LogWrapper.d(TAG, "failed too many times, change to net trigger mode.");
                TKConfig.setPref(context, TKConfig.PREFS_RADAR_PULL_TRIGGER_MODE, TRIGGER_MODE_NET);
                fail = 0;
            } else {
                LogWrapper.d(TAG, "next Alarm: " + next.getTime().toLocaleString());
                Alarms.enableAlarm(context, next, alarmAction);
            }
        } 
            
        TKConfig.setPref(context, TKConfig.PREFS_RADAR_PULL_FAILED_TIMES, String.valueOf(fail));
        
        Intent name = new Intent(context, PullService.class);
        stopService(name);
    }

    public static class PullAlarmAction implements Alarms.AlarmAction {
        
        @Override
        public void saveAlarm(Context context, String absAlarm, String relAlarm) {
            if (absAlarm != null && !TextUtils.isEmpty(absAlarm)) {
                TKConfig.setPref(context, TKConfig.PREFS_RADAR_PULL_ALARM_ABSOLUTE, absAlarm);
            }
            if (relAlarm != null && !TextUtils.isEmpty(relAlarm)) {
                TKConfig.setPref(context, TKConfig.PREFS_RADAR_PULL_ALARM_RELETIVE, relAlarm);
            }
        }

        @Override
        public Intent getIntent() {
            return new Intent(RadarReceiver.ACTION_PULL);
        }

        @Override
        public String getAbsAlarm(Context context) {
            return TKConfig.getPref(context, TKConfig.PREFS_RADAR_PULL_ALARM_ABSOLUTE, "");
        }

        @Override
        public String getRelAlarm(Context context) {
            return TKConfig.getPref(context, TKConfig.PREFS_RADAR_PULL_ALARM_RELETIVE, "");
        }
    }
    
    final public static String getTriggerMode(Context context) {
        return TKConfig.getPref(context, TKConfig.PREFS_RADAR_PULL_TRIGGER_MODE, TRIGGER_MODE_ALARM);
    }
}
