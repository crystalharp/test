package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.service.LocationCollectionService;
import com.tigerknows.service.PullService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class AlarmInitReceiver extends BroadcastReceiver {
    
    static final String TAG = "AlarmInitReceiver";
    
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
            Intent locationCollection = new Intent(context, LocationCollectionService.class);
            Alarms.disableAlarm(context, locationCollection);
            Intent pullIntent = new Intent(context, PullService.class);
            Alarms.disableAlarm(context, pullIntent);
        }
        initAlarm(context);
    }
    
    void initAlarm(Context context) {
        String nextAlarm = TKConfig.getPref(context, TKConfig.PREFS_RADAR_LOCATION_COLLECTION_ALARM, "");
        Calendar next = Alarms.calculateAlarm(nextAlarm);
        Intent locationCollection = new Intent(context, LocationCollectionService.class);
        Alarms.enableAlarm(context, next.getTimeInMillis(), locationCollection);
        
        nextAlarm = TKConfig.getPref(context, TKConfig.PREFS_RADAR_PULL_ALARM, "");
        next = Alarms.calculateAlarm(nextAlarm);
        Intent pullIntent = new Intent(context, PullService.class);
        Alarms.enableAlarm(context, next.getTimeInMillis(), pullIntent);
    }
}
