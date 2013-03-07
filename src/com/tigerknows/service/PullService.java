/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.tigerknows.TKConfig;
import com.tigerknows.radar.Alarms;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.Calendar;

/**
 * 
 * @author pengwenyue
 *
 */
public class PullService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                // TODO: 去服务器上拉数据
                
                // 定时下一个唤醒
                long currentTimeMillis = System.currentTimeMillis();
                Calendar next = Calendar.getInstance();
                next.setTimeInMillis(currentTimeMillis);
                stopService(next);
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    void stopService(Calendar next) {
        TKConfig.setPref(getApplicationContext(),
                TKConfig.PREFS_RADAR_PULL_ALARM, 
                Alarms.SIMPLE_DATE_FORMAT.format(next.getTime()));
        Intent name = new Intent(this, PullService.class);
        stopService(name);
    }
}
