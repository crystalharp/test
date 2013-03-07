/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;


import com.tigerknows.TKConfig;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.LocationUpload;
import com.tigerknows.model.LocationQuery.TKCellLocation;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author pengwenyue
 *
 */
public class TKLocationManager {
    
    static final String TAG = "TKLocationManager";
    
    public static final String GPS_COLLECTION_PROVIDER = "tk_gps_collection";
    
    public static final int GPS_TIME_OUT = 60000;
    public static final int REQUEST_MIN_TIME = 10;
    public static final int REQUEST_MIN_DISTANCE = 10;
    
    private static final int LOCATION_GPS = 0;
    private static final int LOCATION_NETWORK = 1;
    private static final int LOCATION_TIGERKNOWS = 2;
    
    public static boolean UnallowedLocation = false;
    
    private LocationManager locationManager;
    private LocationListener locationListener;
    private TigerknowsLocationManager networkLocationManager;
    private TKLocation lastGpsTKLocation = null;
    private TKLocation lastNetworkTKLocation = null;
    private TKLocation lastTigerknowsTKLocation = null;
    private LocationUpload locationUpload;
    private LocationQuery locationQuery;
    private ArrayList<TKLocationListener> locationListenerList = new ArrayList<TKLocationListener>();
    private Object locationChangeLock = new Object();
    
    public TKLocationManager(Context context) {
        locationUpload = LocationUpload.getInstance(context);
        locationQuery = LocationQuery.getInstance(context);
        
        networkLocationManager = new TigerknowsLocationManager(context); 
        networkLocationManager.addLocationListener(new TigerknowsLocationListener());
        locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
    }
    
    public void prepareLocation() {
        List<String> providers = locationManager.getAllProviders();
        locationListener = new AndroidLocationListener();
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, REQUEST_MIN_TIME, REQUEST_MIN_DISTANCE, locationListener);
        }
        if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, REQUEST_MIN_TIME, REQUEST_MIN_DISTANCE, locationListener);
        }
        networkLocationManager.bindService();
    }
    
    public void prepareLocation(long minTime, float minInstance) {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            List<String> providers = locationManager.getAllProviders();
            if (providers.contains(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minInstance, locationListener);
            }
            if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minInstance, locationListener);
            }
        }
    }
    
    public void removeUpdates() {
        synchronized (locationChangeLock) {
            locationListenerList.clear();
        }
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        
        if (networkLocationManager != null) {
            networkLocationManager.unbindService();
        }
    }
    
    public void onCreate() {
        locationUpload.onCreate();
        locationQuery.onCreate();
    }
    
    public void onDestroy() {
        locationUpload.onDestroy();
        locationQuery.onDestory();
    }
    
    private void locationChanged(int locationType) {
        if (UnallowedLocation) {
            return;
        }
        synchronized (locationChangeLock) {

            Location location = null;
            
            // 优先顺序：gps，tigerknows,network
            if (locationType == LOCATION_GPS) {
                if (lastGpsTKLocation != null) {
                    location = lastGpsTKLocation.location;
                }
            } else if (locationType == LOCATION_TIGERKNOWS || locationType == LOCATION_NETWORK) {
                
                TKCellLocation tkCellLocation = TKConfig.getCellLocation();
                int lac = tkCellLocation.lac;
                int cid = tkCellLocation.cid;
                if (lastGpsTKLocation != null 
                            && ((lac > 0 
                                    && cid > 0
                                    && lac == lastGpsTKLocation.lac
                                    && cid == lastGpsTKLocation.cid)
                                || (lac == -1 
                                    && cid == -1
                                    && Math.abs(lastGpsTKLocation.time - System.currentTimeMillis()) < TKLocationManager.GPS_TIME_OUT))) {
                    
                    // 优先取用gps定位信息
                    location = lastGpsTKLocation.location;
                } else {
                    if (lastTigerknowsTKLocation != null && !lastTigerknowsTKLocation.location.getProvider().equals("error")) {
                            location = lastTigerknowsTKLocation.location;
                    } else if (lastNetworkTKLocation != null
                            && lac == lastNetworkTKLocation.lac
                            && cid == lastNetworkTKLocation.cid) {
                        location = lastNetworkTKLocation.location;
                    }
                }
            }

            for(TKLocationListener listener : locationListenerList) {
                listener.onLocationChanged(location);
            }
        }
    }
    
    public interface TKLocationListener {
        public void onLocationChanged(Location location);
    }
    
    public void addLocationListener(TKLocationListener listener) {
        synchronized (locationChangeLock) {
            locationListenerList.add(listener);
        }
    }

    public class AndroidLocationListener implements LocationListener {
   
        @Override
        public void onLocationChanged(Location location) {
            if (location == null) {
                return;
            }

            TKCellLocation tkCellLocation = TKConfig.getCellLocation();
            int lac = tkCellLocation.lac;
            int cid = tkCellLocation.cid;
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) { 
                lastGpsTKLocation = new TKLocation(location, lac, cid, System.currentTimeMillis());   
                locationChanged(LOCATION_GPS);
                locationUpload.recordLocation(location);
            } else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) { 
                lastNetworkTKLocation = new TKLocation(location, lac, cid, -1); 
                if (lastTigerknowsTKLocation == null || lastTigerknowsTKLocation.location.getProvider().equals("error")) {
                    locationChanged(LOCATION_NETWORK);
                    locationUpload.recordLocation(location);
                }
            }
        }
        @Override
        public void onProviderDisabled(String provider) {
        }
        @Override
        public void onProviderEnabled(String provider) {
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    public class TigerknowsLocationListener implements LocationListener {
   
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                lastTigerknowsTKLocation = new TKLocation(location);
            } else {
                lastTigerknowsTKLocation = null;
            }
            locationChanged(LOCATION_TIGERKNOWS);
        }
        @Override
        public void onProviderDisabled(String provider) {
        }
        @Override
        public void onProviderEnabled(String provider) {
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if(status != LocationProvider.AVAILABLE) {
                lastTigerknowsTKLocation = null;
            }
            locationChanged(LOCATION_TIGERKNOWS);
        }
    }
    
    public static class TKLocation {
        Location location;
        
        int lac = -1;
        int cid = -1;
        long time = 0;
        
        public TKLocation(Location location) {
            this(location , -1, -1, 0);
        }
        
        public TKLocation(Location location, int lac, int cid, long time) {
            this.location = location;
            this.lac = lac;
            this.cid = cid;
            this.time = time;
        }
        
        
        public String toString() {
            return "TKLocation[lac=" + lac + ", cid=" + cid + ", time=" + time + ", location=" + location+"]";
        }
    }
}
