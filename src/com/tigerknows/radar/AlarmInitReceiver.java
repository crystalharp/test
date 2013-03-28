package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.service.LocationCollectionService;
import com.tigerknows.service.PullService;

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
        String locationAlarm = LocationCollectionService.alarmAction.getAbsAlarm(context);
        Calendar locationNext = Alarms.calculateAlarm(locationAlarm);
        Alarms.enableAlarm(context, locationNext, LocationCollectionService.alarmAction);
        
        String pullAlarm = PullService.alarmAction.getAbsAlarm(context);
        Calendar pullNext = Alarms.calculateAlarm(pullAlarm);
        Alarms.enableAlarm(context, pullNext, PullService.alarmAction);
    }
}
