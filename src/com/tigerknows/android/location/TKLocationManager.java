/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.android.location;


import com.tigerknows.TKConfig;
import com.tigerknows.common.LocationUpload;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.LocationQuery.TKCellLocation;
import com.tigerknows.service.TigerknowsLocationManager;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 定位管理类
 * 
 * @author pengwenyue
 *
 */
public class TKLocationManager {
    
    static final String TAG = "TKLocationManager";
    
    public static final String GPS_COLLECTION_PROVIDER = "tk_gps_collection";
    
    /**
     * 若GPS定位信息的精度大于此值则视为无效的定位信息
     */
    public static final int GPS_AVAILABLY_ACCURACY = 2 * 1000;
    
    /**
     * 若最近一次有效地定位信息的时间截至今大于此值则视为过期
     */
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
    private LocationUpload gpsLocationUpload;
    private LocationUpload networkLocationUpload;
    private LocationQuery locationQuery;
    private ArrayList<TKLocationListener> locationListenerList = new ArrayList<TKLocationListener>();
    private Object locationChangeLock = new Object();
    private List<GpsSatellite> mSatelliteList = new ArrayList<GpsSatellite>();
    private Location lastLocation;
    
    public LocationUpload getGPSLocationUpload() {
        return gpsLocationUpload;
    }
    
    public LocationUpload getNetworkLocationUpload() {
        return networkLocationUpload;
    }
    
    public TKLocationManager(Context context) {
        gpsLocationUpload = LocationUpload.getGpsInstance(context);
        networkLocationUpload = LocationUpload.getNetworkInstance(context);
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
            locationManager.addGpsStatusListener(new GpsStatus.Listener() {
				@Override
				public void onGpsStatusChanged(int event) {
					// TODO Auto-generated method stub
					if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS){
						GpsStatus status = locationManager.getGpsStatus(null);
						if(status != null){
							int maxSatellites = status.getMaxSatellites();
							Iterator<GpsSatellite> it = status.getSatellites().iterator();
							mSatelliteList.clear();
							int count = 0;
							while (it.hasNext() && count <= maxSatellites){
								mSatelliteList.add(it.next());							
								count++;
							}
						}
					}
				}
			});
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
            lastLocation = null;
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
        gpsLocationUpload.onCreate();
        networkLocationUpload.onCreate();
        locationQuery.onCreate();
    }
    
    public void onDestroy() {
        gpsLocationUpload.onDestroy();
        networkLocationUpload.onDestroy();
        locationQuery.onDestory();
    }
    
    private void locationChanged(int locationType) {
        if (UnallowedLocation) {
            return;
        }
        synchronized (locationChangeLock) {

            Location location = null;
            
            // 优先顺序：gps，tigerknows,network
            TKLocation lastGpsTKLocation = this.lastGpsTKLocation;
            TKLocation lastTigerknowsTKLocation = this.lastTigerknowsTKLocation;
            TKLocation lastNetworkTKLocation = this.lastNetworkTKLocation;
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

            if (lastLocation != location ||
                    lastLocation == null ||
                    location == null ||
                    !(Math.abs(lastLocation.getLatitude()-location.getLatitude()) < 0.00001d &&
                            Math.abs(lastLocation.getLongitude()-location.getLongitude()) < 0.00001d &&
                            Math.abs(lastLocation.getAccuracy()-location.getAccuracy()) < 0.00001f &&
                            Math.abs(lastLocation.getSpeed()-location.getSpeed()) < 0.00001f &&
                            Math.abs(lastLocation.getAltitude()-location.getAltitude()) < 0.00001d)) {
                lastLocation = location;
                for(TKLocationListener listener : locationListenerList) {
                    listener.onLocationChanged(location);
                }
            }
        }
    }
    
    public void addLocationListener(TKLocationListener listener) {
        synchronized (locationChangeLock) {
            lastLocation = null;
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
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER) && location.getAccuracy() < GPS_AVAILABLY_ACCURACY) { 
                lastGpsTKLocation = new TKLocation(location, lac, cid, System.currentTimeMillis());   
                locationChanged(LOCATION_GPS);
                gpsLocationUpload.recordLocation(location);
            } else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) { 
                lastNetworkTKLocation = new TKLocation(location, lac, cid, -1); 
                TKLocation lastTigerknowsTKLocation = TKLocationManager.this.lastTigerknowsTKLocation;
                if (lastTigerknowsTKLocation == null || lastTigerknowsTKLocation.location.getProvider().equals("error")) {
                    locationChanged(LOCATION_NETWORK);
                    networkLocationUpload.recordLocation(location);
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
    
    public int getSatelliteSize(){
    	return mSatelliteList.size();
    }
}
