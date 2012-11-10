/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.tigerknows.service.ILocationListener;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;

/**
 * 
 * @author pengwenyue
 *
 */
public class TigerknowsLocationManager {

    static final String TAG = "TigerknowsLocationManager";
    public static final String TIGERKNOWS_PROVIDER = "Tigerknows";

    private Context mContext;
    
    public TigerknowsLocationManager(Context context) {
        mContext = context;
    }
    
    private ILocationBinder mLocationService;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            mLocationService = ILocationBinder.Stub.asInterface(service);
            try {
                mLocationService.registerCallback(mLocationCallback);
            } catch (RemoteException e) {
                
            }
        }
        
        public void onServiceDisconnected(ComponentName className) {
            mLocationService = null;
        }
    };
    
    private ArrayList<LocationListener> mLocationListener = new ArrayList<LocationListener>();
    
    private ILocationListener mLocationCallback = new ILocationListener.Stub() {
        
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) throws RemoteException {
            for(LocationListener locationListener : mLocationListener) {
                locationListener.onStatusChanged(provider, status, extras);
            }
        }
        
        @Override
        public void onProviderEnabled(String provider) throws RemoteException {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public void onProviderDisabled(String provider) throws RemoteException {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public void onLocationChanged(Location location) throws RemoteException {
            for(LocationListener locationListener : mLocationListener) {
                locationListener.onLocationChanged(location);
            }
        }
    };

    public void addLocationListener(LocationListener listener) {
        if (null != listener) {
           mLocationListener.add(listener);
        }
    }
    
    public void bindService() {
        Intent intent = new Intent(mContext, LocationService.class);
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE); 
    }

    public void unbindService() {
        mContext.unbindService(mConnection);
    }
}
