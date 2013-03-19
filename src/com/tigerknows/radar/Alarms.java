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
    
    public static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    
    /**
     * Sets alert in AlarmManger.  This is what will
     * actually launch the pull data service when the alarm triggers.
     *
     * @param alarm Alarm.
     * @param atTimeInMillis milliseconds since epoch
     */
    public static void enableAlarm(Context context, Calendar alarmCal, Intent intent) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        alarmCal = checkAlarm(alarmCal);
        LogWrapper.d(TAG, "enableAlarm atTime " + alarmCal.getTime().toLocaleString());
        
        TKConfig.setPref(context,
                TKConfig.PREFS_RADAR_PULL_ALARM, 
                Alarms.SIMPLE_DATE_FORMAT.format(alarmCal.getTimeInMillis()));

        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        am.set(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), sender);
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
    
    public static Calendar calculateAlarm(String alarm) {
        long currentTimeMillis = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentTimeMillis-1000);
        Date date = new Date(currentTimeMillis-1000);
        if (TextUtils.isEmpty(alarm) == false) {
            try {
                date = SIMPLE_DATE_FORMAT.parse(alarm); 
                c.setTimeInMillis(date.getTime());
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
        
        return c;
    }
    
    public static Calendar alarmAddDays(Calendar next, long addDays) {
        
        if (addDays > 0 && next != null) {
            next.add(Calendar.DAY_OF_YEAR, (int)addDays);
        }
        
        return next;
    }
    
    public static Calendar alarmAddHours(Calendar next, int addHours) {
        
        if (addHours > 0 && next != null) {
            next.add(Calendar.HOUR_OF_DAY, (int)addHours);
        }
        
        return next;
    }
    
    /**
     * 检查返回的时间不要迟于当下。
     */
    private static Calendar checkAlarm(Calendar next){
        long currentTimeMillis = System.currentTimeMillis();
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(currentTimeMillis);
        
        if (next == null) {
            next = now;
        }
        LogWrapper.d(TAG, "before check, alarm is: " + next.getTime().toLocaleString());
        
        if (next.compareTo(now) <= 0) {
            now.add(Calendar.HOUR_OF_DAY, 1);
            next = now;
        }
        return next;
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
        if (cal != null) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.SECOND, second);
        }
        return cal;
    }
}
