package com.tigerknows.radar;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.common.LocationUpload;
import com.tigerknows.common.LogUpload;
import com.tigerknows.service.PullService;
import com.tigerknows.service.download.AppService;
import com.weibo.sdk.android.util.Utility;

/**
 * 推送服务的联网触发
 * @author xupeng
 * 联网触发的说明见PullService.java
 */

public class ConnectivityChangeReceiver extends BroadcastReceiver{

    // android 中网络变化时所发的Intent的名字
    private static final String netACTION="android.net.conn.CONNECTIVITY_CHANGE";
    
    @Override
    public void onReceive(final Context context, Intent intent) {

        if(intent.getAction().equals(netACTION)) {
            // Intent中ConnectivityManager.EXTRA_NO_CONNECTIVITY这个关键字表示着当前是否连接上了网络
            // true 代表网络断开   false 代表网络没有断开
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            LogWrapper.d("NetCheckReceiver", "onReceive() noConnectivity="+noConnectivity);
            // 网络已连接
            if (noConnectivity == false) {
                // 日志上传
                new Thread(new Runnable() {
                    
                    @Override
                    public void run() {
                        LogUpload.upload(context,
                                ActionLog.getInstance(context), 
                                LocationUpload.getGpsInstance(context),
                                LocationUpload.getNetworkInstance(context),
                                LocationUpload.getNetworkTrackInstance(context));
                        
                    }
                }).start();
                // 消息推送的网络触发
                if (PullService.TRIGGER_MODE_NET.equals(PullService.getTriggerMode(context))) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(System.currentTimeMillis());
                    cal = Alarms.alarmAddMinutes(cal, TKConfig.PullServiceNetTriggerDelayTime);
                    Alarms.enableAlarm(context, cal, PullService.alarmAction);
                }
                // 推送app的网络触发
                if (Utility.isWifi(context)){
                    if (AppPushNotify.checkNotification(context) != 0){
                        AppService.checkAndDown(context);
                    }
                } else {
                    // 网络变化后如果不是wifi就停止下载
                    AppService.stopDownload(context);
                }
            } else {
                // 网络断了就马上停止下载
                AppService.stopDownload(context);
            }
        }
    }
    



    

}


