/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.tigerknows.android.app.TKService;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.location.TKLocationListener;
import com.tigerknows.android.location.TKLocationManager;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.Alarm;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;


/**
 * 监听定位改变事件，通知公交闹铃
 * @author pengwenyue
 */
public class AlarmService extends TKService {
    
    static final String TAG = "PositionAlarmService";
    
    public static final String EXTRA_RECHECK = "EXTRA_RECHECK";
    
    private TKLocationManager mTKLocationManager;
    
    private MyLocationListener mLocationListener;
    
    private Position mLastPosition;

    /**
     * 定位改变事件的监听器
     * @author pengwenyue
     *
     */
    public class MyLocationListener implements TKLocationListener {

        @Override
        public void onLocationChanged(Location location) {  
            if (location != null) {
                Position position = new Position(location.getLatitude(), location.getLongitude());
                MapEngine mapEngine = MapEngine.getInstance();
                mLastPosition = mapEngine.latlonTransform(position);
                
                Alarm.showAlarm(getApplicationContext(), mLastPosition);
            }
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();

        mLocationListener = new MyLocationListener();
        mTKLocationManager = TKLocationManager.getInstatce(getApplicationContext());
        
        mTKLocationManager.onCreate();
        mTKLocationManager.onStart(mLocationListener);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        
        if (intent != null && intent.getBooleanExtra(EXTRA_RECHECK, false)) {
            Alarm.showAlarm(getApplicationContext(), mLastPosition);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        mTKLocationManager.onStop(mLocationListener);
        mTKLocationManager.onDestroy();
        super.onDestroy();
    }
    
    public static void start(Context context, boolean recheck) {
        Intent intent = new Intent(context, AlarmService.class);
        intent.putExtra(EXTRA_RECHECK, recheck);
        context.startService(intent);
    }
    
    public static void stop(Context context) {
        Intent intent = new Intent(context, AlarmService.class);
        context.stopService(intent);
    }
}
