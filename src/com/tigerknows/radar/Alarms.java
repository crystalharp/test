package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Alarms {
    
    static final String TAG = "Alarms";
    
    public static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static Calendar calculateAlarm(String alarm) {
        long currentTimeMillis = System.currentTimeMillis();
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(currentTimeMillis);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentTimeMillis);
        Date date = new Date(currentTimeMillis-1000);
        if (TextUtils.isEmpty(alarm) == false) {
            try {
                date = SIMPLE_DATE_FORMAT.parse(alarm); 
                c.set(date.getYear(),
                        date.getMonth(),
                        date.getDate(),
                        date.getHours(),
                        date.getMinutes(),
                        date.getSeconds());
            } catch (Exception e) {
                // TODO: handle exception
            }
        }

        if (c.before(now)) {
            now.add(Calendar.HOUR_OF_DAY, 1);
            c = now;
        }
        LogWrapper.d(TAG, "calculateAlarm() " + c);
        return c;
    }
    
    /**
     * Given an alarm in hours and minutes, return a time suitable for
     * setting in AlarmManager.
     * @param hour Always in 24 hour 0-23
     * @param minute 0-59
     * @param daysOfYear 0-31
     */
    public static Calendar calculateAlarm(int hour, int minute, int addDays) {

        // start with now
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int nowHour = c.get(Calendar.HOUR_OF_DAY);
        int nowMinute = c.get(Calendar.MINUTE);

        // if alarm is behind current time, advance one day
        if (hour < nowHour  ||
            hour == nowHour && minute <= nowMinute) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        if (addDays > 0) c.add(Calendar.DAY_OF_YEAR, addDays);
        return c;
    }
    
    /**
     * Sets alert in AlarmManger.  This is what will
     * actually launch the pull data service when the alarm triggers.
     *
     * @param alarm Alarm.
     * @param atTimeInMillis milliseconds since epoch
     */
    public static void enableAlarm(Context context, final long atTimeInMillis, Intent intent) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        LogWrapper.d(TAG, "enableAlarm() atTime " + atTimeInMillis);

        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
    }

    /**
     * Disables alert in AlarmManger.
     */
    public static void disableAlarm(Context context, Intent intent) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancel(sender);
    }
}
