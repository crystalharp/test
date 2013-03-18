package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class Alarms {
    
    static final String TAG = "Alarms";
    
    public static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static Calendar calculateAlarm(String alarm) {
        long currentTimeMillis = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentTimeMillis-1000);
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
                e.printStackTrace();
            }
        }

        return calculateAlarm(c, 0);
    }
    
    /**
     * 给定一个calendar和推迟的天数，返回一个计算好的calendar。
     * 被其他同名函数所调用，检查返回的时间不要迟于当下。
     */
    public static Calendar calculateAlarm(Calendar next, long addDays) {

        if (addDays > 0) {
            next.add(Calendar.DAY_OF_YEAR, (int)addDays);
        }
        
        long currentTimeMillis = System.currentTimeMillis();
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(currentTimeMillis);
        
        if (next.before(now)) {
            now.add(Calendar.HOUR_OF_DAY, 1);
            next = now;
        }
        LogWrapper.d(TAG, "calculateAlarm() " + next);
        return next;
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
        
        TKConfig.setPref(context,
                TKConfig.PREFS_RADAR_PULL_ALARM, 
                Alarms.SIMPLE_DATE_FORMAT.format(atTimeInMillis));

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
    
    /**
     *  return a random time from startHour to endHour in such form: 12:33:46
     */
    public static String makeRandomTime(int startHour, int endHour) {
        String time = "";
        Random rand = new Random(System.currentTimeMillis());
        int hour = startHour + rand.nextInt(endHour - startHour);
        int minute = rand.nextInt(60);
        int second = rand.nextInt(60);
        time += hour + ":" + minute + ":" + second;
        return time;
    }
    
    public static Calendar calculateRandomAlarmInNextDay(Calendar cal, int startHour, int endHour) {
        Random rand = new Random(System.currentTimeMillis());
        int hour = startHour + rand.nextInt(endHour - startHour);
        int minute = rand.nextInt(60);
        int second = rand.nextInt(60);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.SECOND, second);
        return cal;
    }
}
