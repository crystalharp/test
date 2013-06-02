/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.tigerknows.model.LocationQuery;

import android.content.Intent;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

/**
 * 
 * @author pengwenyue
 *
 */
public class LocationService extends TKNetworkService {
    
    public static final long RETRY_INTERVAL = 6 * 1000;
    
    static final String TAG = "LocationService";
    
    private LocationQuery locationQuery;
    
    public static Location LOCATION_FOR_TEST;
    
    @Override
    public void onCreate() {
        super.onCreate();
        locationQuery = LocationQuery.getInstance(getBaseContext());
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    void onLocationChanged(Location location) {
        int N = 0;
        while ((N = callbacks.beginBroadcast()) < 1) {
            // 等待监听者
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < N; i++) {
            try {
                callbacks.getBroadcastItem(i).onLocationChanged(location);
            } catch (RemoteException e) {
                // TODO
            }
        }
        callbacks.finishBroadcast();
    }
    
    void onStatusChanged(String provider, int status, Bundle extras) {
        int N = 0;
        while ((N = callbacks.beginBroadcast()) < 1) {
            // 等待监听者
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < N; i++) {
            try {
                callbacks.getBroadcastItem(i).onStatusChanged(provider, status, extras);
            } catch (RemoteException e) {
                // TODO
            }
        }
        callbacks.finishBroadcast();
    }

    private final ILocationBinder.Stub binder = new ILocationBinder.Stub() {

        @Override
        public void stopRunningTask() throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean isTaskRunning() throws RemoteException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void registerCallback(ILocationListener cb) throws RemoteException {
            if (null != cb)
                callbacks.register(cb);
        }

        @Override
        public void unregisterCallback(ILocationListener cb) throws RemoteException {
            if (null != cb)
                callbacks.unregister(cb);
        }
    };

    final RemoteCallbackList<ILocationListener> callbacks = new RemoteCallbackList<ILocationListener>();

    @Override
    public void doWork() {
        if (locationQuery == null) {
            onStatusChanged(TigerknowsLocationManager.TIGERKNOWS_PROVIDER, LocationProvider.OUT_OF_SERVICE, null);
            return;
        }
        //TODO:获取当前位置信息
        Location location = locationQuery.getLocation();
        
        if (LOCATION_FOR_TEST != null) {
            location = LOCATION_FOR_TEST;
        }
        if (location != null) {
            onLocationChanged(location);
        } else {
            onStatusChanged(TigerknowsLocationManager.TIGERKNOWS_PROVIDER, LocationProvider.TEMPORARILY_UNAVAILABLE, null);
        }
        
        try {
            Thread.sleep(RETRY_INTERVAL);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
