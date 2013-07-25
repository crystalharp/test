package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.service.PullService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.text.TextUtils;

import java.util.Date;

/**
 * 
 * @author xupeng
 * 这个接收器会接受时钟/时区调整的广播，在手机时间调整的时候把配置中记录的定时器绝对时间调整为
 * 当前时间坐标系中的正确时间。
 * 每个需要校准的服务需要记录两个定时器来分别记录定时器的相对时间和绝对时间，每次时间调整的时候
 * 重新设置定时器绝对时间 = 系统当前绝对时间 + (定时器相对时间 - 系统当前相对时间)
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
