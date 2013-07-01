package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/**
 * 
 * @author xupeng
 * 这个类提供了定时调用intent的服务。它和radar/AlarmAdjustReceiver.java一起提供了
 * 不受手机时间修改影响的定时器服务。
 * 使用这个类需要实现AlarmAction接口，具体函数为存储该服务的定时器绝对时间和相对时间;
 * 读绝对时间;读相对时间;获取定时器调用的intent对象。
 * 定时器时间的设置完全使用Calendar类来进行传递，并且提供了一些Alarm计算方法，特别根据
 * 服务的特点添加了生成下一天某时间段中随机时间的方法。
 * 
 * 具体例子参见PullService.java的实现。
 */
public class Alarms {
    
    static final String TAG = "Alarms";
    
    public static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    
    /**
     * 定时器的传递使用calendar类，绝对时间，设置定时器时使用相对时间来设置，
     * 原因是使用绝对时间的话如果将时间调整到已设定时器之后的时间，则会立即触发所设置的定时器。
     * 调用该方法的服务需要实现AlarmAction接口，以进行获取intent和保存定时器时间操作。
     * 同时保存绝对时间和相对时间的原因是为了在手机时间变化的时候进行绝对时间校准，具体方法
     * 在radar/AlarmAdjustReceiver.java里
     */
    public static void enableAlarm(Context context, Calendar alarmCal, AlarmAction action) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        alarmCal = checkAlarm(alarmCal);
        //checkAlarm可以保证alarmCal在当前之间之后
        long timeGap = alarmCal.getTimeInMillis() - System.currentTimeMillis();
        long timer = SystemClock.elapsedRealtime() + timeGap;
        
        action.saveAlarm(context, Alarms.SIMPLE_DATE_FORMAT.format(alarmCal.getTimeInMillis()), String.valueOf(timer));
        Intent intent = action.getIntent();
        LogWrapper.d(TAG, intent.getAction() + " enableAlarm atTime " + alarmCal.getTime().toLocaleString());
        
        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, timer, sender);
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
    
    public static Calendar alarmAddDays(Calendar next, int addDays) {
        
        if (addDays > 0 && next != null) {
            next.add(Calendar.DAY_OF_YEAR, addDays);
        }
        
        return next;
    }
    
    public static Calendar alarmAddMinutes(Calendar next, int addMinutes) {
        
        if (addMinutes > 0 && next != null) {
            next.add(Calendar.MINUTE, addMinutes);
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
            now = alarmAddMinutes(now, TKConfig.AlarmCheckDelayTime);
//            now.add(Calendar.MINUTE, TKConfig.AlarmCheckDelayTime);
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
    
    public interface AlarmAction {
        void saveAlarm(Context context, String absAlarm, String relAlarm);
        String getAbsAlarm(Context context);
        String getRelAlarm(Context context);
        Intent getIntent();
    }
}
