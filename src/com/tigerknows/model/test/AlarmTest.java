package com.tigerknows.model.test;

import com.tigerknows.android.location.Position;
import com.tigerknows.model.Alarm;

import android.content.Context;
import android.provider.Settings;

public class AlarmTest {

    public static void launchTest(Context context) {
        Position position = new Position(39.99404, 116.31641);
        Alarm alarm = new Alarm(context);
        alarm.setName("中关园北站");
        alarm.setPosition(position);
        alarm.setRange(1000);
        alarm.setRingtone(Settings.System.DEFAULT_ALARM_ALERT_URI);
        alarm.setRingtoneName(Alarm.getRingtoneName(context, alarm.getRingtone()));
        alarm.setStatus(0);
        Alarm.writeAlarm(context, alarm);
        
        alarm = new Alarm(context);
        alarm.setName("清华西门");
        alarm.setPosition(position);
        alarm.setRange(1000);
        alarm.setRingtone(Settings.System.DEFAULT_ALARM_ALERT_URI);
        alarm.setRingtoneName(Alarm.getRingtoneName(context, alarm.getRingtone()));
        alarm.setStatus(0);
        Alarm.writeAlarm(context, alarm);
    }
}
