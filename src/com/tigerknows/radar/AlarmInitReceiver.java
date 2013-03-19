package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * 
 * @author xupeng
 * 这里是定时器的发起点，所有的定时器初始化均由AlarmInitReceiver接受到广播来启动，
 * 所有定时器启动应该是发送广播来启动，而不要直接enableAlarm。
 * 它所接受的广播类型在AndroidManifest.xml中
 */

public class AlarmInitReceiver extends BroadcastReceiver {
    
    static final String TAG = "AlarmInitReceiver";
    public static final String ACTION_ALARM_INIT = "com.tigerknows.action.AlarmInit";
    
    public static boolean IS_WAITING_NETWORK_CHANGE = false;
    
    /**
     * Sets alarm on ACTION_BOOT_COMPLETED.  Resets alarm on
     * TIME_SET, TIMEZONE_CHANGED
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogWrapper.d(TAG, "onReceive() " + action);
        
        // 开机
        if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)
                || action.equals(Intent.ACTION_TIME_CHANGED)) {
            // 以上两个action是改变时间引起的， 在这里需要取消之前的rtc注册定时唤醒，然后重新计算时间并注册
            disableAlarm(context);
        }
        enableAlarm(context);
    }
    
    void disableAlarm(Context context) {
        Intent locationCollection = new Intent(RadarReceiver.ACTION_LOCATION_COLLECTION);
        Alarms.disableAlarm(context, locationCollection);
        Intent pullIntent = new Intent(RadarReceiver.ACTION_PULL);
        Alarms.disableAlarm(context, pullIntent);
    }
    
    void enableAlarm(Context context) {
        String nextAlarm = TKConfig.getPref(context, TKConfig.PREFS_RADAR_LOCATION_COLLECTION_ALARM, "");
        Calendar next = Alarms.calculateAlarm(nextAlarm);
        Intent locationCollection = new Intent(RadarReceiver.ACTION_LOCATION_COLLECTION);
        Alarms.enableAlarm(context, next, locationCollection);
        
        nextAlarm = TKConfig.getPref(context, TKConfig.PREFS_RADAR_PULL_ALARM, "");
        next = Alarms.calculateAlarm(nextAlarm);
        Intent pullIntent = new Intent(RadarReceiver.ACTION_PULL);
        Alarms.enableAlarm(context, next, pullIntent);
    }
}
