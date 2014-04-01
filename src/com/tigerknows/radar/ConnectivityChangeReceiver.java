package com.tigerknows.radar;

import java.util.Calendar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.service.PullService;
import com.tigerknows.service.download.AppService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.TextUtils;

/**
 * 推送服务的联网触发
 * @author xupeng
 * 联网触发的说明见PullService.java
 */

public class ConnectivityChangeReceiver extends BroadcastReceiver{

    // android 中网络变化时所发的Intent的名字
    private static final String netACTION="android.net.conn.CONNECTIVITY_CHANGE";
    
    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals(netACTION)) {
            // Intent中ConnectivityManager.EXTRA_NO_CONNECTIVITY这个关键字表示着当前是否连接上了网络
            // true 代表网络断开   false 代表网络没有断开
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            LogWrapper.d("NetCheckReceiver", "onReceive() noConnectivity="+noConnectivity);
            // 网络已连接
            if (noConnectivity == false) {
                // 推送的网络触发
                if (PullService.TRIGGER_MODE_NET.equals(PullService.getTriggerMode(context))) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(System.currentTimeMillis());
                    cal = Alarms.alarmAddMinutes(cal, TKConfig.PullServiceNetTriggerDelayTime);
                    Alarms.enableAlarm(context, cal, PullService.alarmAction);
                }
                if (!TextUtils.isEmpty(TKConfig.getPref(context, TKConfig.PREFS_APP_PUSH_DOWNLOAD_FINISHED, ""))){
                	//TODO: 添加下载完成之后的界面显示
                } else {
                    AppService.checkAndDown(context);
                }
            }
        }
    }
}


