package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.service.LocationCollectionService;
import com.tigerknows.service.PullService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.text.TextUtils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * 
 * @author xupeng
 * 
 */

public class AlarmAdjustReceiver extends BroadcastReceiver {
    
    static final String TAG = "AlarmAdjustReceiver";
    
    /**
     * Resets alarm on TIME_SET, TIMEZONE_CHANGED
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String pullAlarmAbsolute = PullService.alarmAction.getAbsAlarm(context);
        String pullAlarmRelative = PullService.alarmAction.getRelAlarm(context);
        if (!TextUtils.isEmpty(pullAlarmAbsolute)) {
            LogWrapper.d(TAG, "before Adjust, pullAlarm is:" + pullAlarmAbsolute);
            pullAlarmAbsolute = adjustAbsTimebyRelTime(pullAlarmRelative);
            LogWrapper.d(TAG, "after Adjust, pullAlarm is:" + pullAlarmAbsolute);
            PullService.alarmAction.saveAlarm(context, pullAlarmAbsolute, null);
        }

        String locationAlarmAbsolute = LocationCollectionService.alarmAction.getAbsAlarm(context);
        String locationAlarmRelative = LocationCollectionService.alarmAction.getRelAlarm(context);
        if (!TextUtils.isEmpty(locationAlarmAbsolute)) {
            LogWrapper.d(TAG, "before Adjust, locationAlarm is:" + locationAlarmAbsolute);
            locationAlarmAbsolute = adjustAbsTimebyRelTime(locationAlarmRelative);
            LogWrapper.d(TAG, "after Adjust, locationAlarm is:" + locationAlarmAbsolute);
            LocationCollectionService.alarmAction.saveAlarm(context, locationAlarmAbsolute, null);
        }
    }
    
    String adjustAbsTimebyRelTime(String relAlarm){
        Date absAlarmDate = new Date();
        long relTime;
        try {
            relTime = Long.parseLong(relAlarm);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
        long correctTime = relTime - SystemClock.elapsedRealtime() + System.currentTimeMillis();
        absAlarmDate.setTime(correctTime);
        return Alarms.SIMPLE_DATE_FORMAT.format(absAlarmDate);
    }
}
