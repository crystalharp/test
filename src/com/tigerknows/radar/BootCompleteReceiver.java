package com.tigerknows.radar;

import com.tigerknows.TKConfig;
import com.tigerknows.service.PullService;
import com.tigerknows.util.CalendarUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class BootCompleteReceiver extends BroadcastReceiver {

    static final String TAG = "BootCompleteReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {

        // 初始化推送的定时器
        if (PullService.TRIGGER_MODE_ALARM.equals(PullService.getTriggerMode(context))) {
            TKConfig.readConfig();
            AlarmInitReceiver.enableAlarm(context);
        }
        
        // 调整自定义的时钟
        String absTime = TKConfig.getPref(context, TKConfig.PREFS_RECORDED_SYS_ABS_TIME, null);
        if (absTime != null) {
            long recTime = Long.parseLong(absTime);
            long correctRelTime = CalendarUtil.getRelTimeByAbsTime(recTime);
            TKConfig.setPref(context, TKConfig.PREFS_RECORDED_SYS_REL_TIME, String.valueOf(correctRelTime));
            Log.d(TAG, "correctRelTime:" + correctRelTime);
            Log.d(TAG, "currentRelTime:" + SystemClock.elapsedRealtime());
        }
    }

}
