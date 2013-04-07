package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.service.LocationCollectionService;
import com.tigerknows.service.PullService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.util.Calendar;

/**
 * 
 * @author xupeng
 * 这里是定时器的发起点，所有的定时器初始化均由AlarmInitReceiver接受到广播来启动，
 * 所有定时器启动应该是发送广播来启动，而不要直接enableAlarm。
 * 它主要处理那些没有定时器的情况，也就是去处理可能会丢定时器的操作，比如关机，关掉推送开关。
 * 所接受的广播类型在AndroidManifest.xml中
 * 说明：现在接受媒体扫描完毕来代替开机启动完成，原因是装在SD卡中的程序在发送开机广播的时候
 * 并没有准备完成，无法接收，另外接收开机广播需要申请开机启动权限。
 * 
 */

public class AlarmInitReceiver extends BroadcastReceiver {
    
    static final String TAG = "AlarmInitReceiver";
    public static final String ACTION_ALARM_INIT = "com.tigerknows.action.AlarmInit";
    
    /**
     * Sets alarm on ACTION_BOOT_COMPLETED.  Resets alarm on
     * TIME_SET, TIMEZONE_CHANGED
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogWrapper.d(TAG, "onReceive() " + action);
        enableAlarm(context);
    }
    
    void disableAlarm(Context context) {
        Intent locationCollection = new Intent(LocationCollectorReceiver.ACTION_LOCATION_COLLECTION);
        Alarms.disableAlarm(context, locationCollection);
        Intent pullIntent = new Intent(RadarReceiver.ACTION_PULL);
        Alarms.disableAlarm(context, pullIntent);
    }
    
    void enableAlarm(Context context) {
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(System.currentTimeMillis());
        next = Alarms.alarmAddDays(next, 1);
        Calendar locationNext = null;
        Calendar pullNext = null;
        
        String locationAlarm = LocationCollectionService.alarmAction.getAbsAlarm(context);
        //如果是空，则用户是第一次使用，设置到一天以后
        if (TextUtils.isEmpty(locationAlarm)) {
            locationNext = next;
        } else {
            locationNext = Alarms.calculateAlarm(locationAlarm);
        }
        Alarms.enableAlarm(context, locationNext, LocationCollectionService.alarmAction);
        
        String pullAlarm = PullService.alarmAction.getAbsAlarm(context);
        if (TextUtils.isEmpty(pullAlarm)) {
            pullNext = next;
        } else {
            pullNext = Alarms.calculateAlarm(pullAlarm);
        }
        Alarms.enableAlarm(context, pullNext, PullService.alarmAction);
    }
}
