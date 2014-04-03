/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.android.location;


import com.tigerknows.TKConfig;
import com.tigerknows.common.LocationUpload;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.TKCellLocation;
import com.tigerknows.service.TigerknowsLocationManager;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 定位管理类
 * 
 * @author pengwenyue
 *
 */
public class TKLocationManager {
    
    static final String TAG = "TKLocationManager";
    
    private static TKLocationManager sInstance = null;
    
    public static TKLocationManager getInstatce(Context context) {
        if (sInstance == null) {
            sInstance = new TKLocationManager(context);
        }
        
        return sInstance;
    }
    
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
    
    private Context context;
    private LocationManager locationManager;
    private LocationListener locationListenerGPS;
    private LocationListener locationListenerNetwork;
    private TigerknowsLocationManager networkLocationManager;
    private Location lastGpsLocation = null;
    private TKLocation lastGpsTKLocation = null;
    private TKLocation lastNetworkTKLocation = null;
    private TKLocation lastTigerknowsTKLocation = null;
    private LocationUpload gpsLocationUpload;
    private LocationUpload networkLocationUpload;
    private LocationQuery locationQuery;
    private ArrayList<TKLocationListener> locationListenerList = new ArrayList<TKLocationListener>();
    private Object locationChangeLock = new Object();
    private Location lastLocation;
    private int onCreateGPS = 0;
    private int onResumeGPS = 0;
    private int onCreateNetwork = 0;
    private int onResumeNetwork = 0;
    private int onCreateTigerknows = 0;
    private int onResumeTigerknows = 0;
    
    public LocationUpload getGPSLocationUpload() {
        return gpsLocationUpload;
    }
    
    public LocationUpload getNetworkLocationUpload() {
        return networkLocationUpload;
    }
    
    private TKLocationManager(Context context) {
        this.context = context;
        gpsLocationUpload = LocationUpload.getGpsInstance(context);
        networkLocationUpload = LocationUpload.getNetworkInstance(context);
        locationQuery = LocationQuery.getInstance(context);
        
        networkLocationManager = new TigerknowsLocationManager(context); 
        networkLocationManager.addLocationListener(new TigerknowsLocationListener());
        locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        
        locationListenerGPS = new AndroidLocationListener();
        locationListenerNetwork = new AndroidLocationListener();
    }
    
    private void prepareLocation(String provider, LocationListener locationListener) {
        List<String> providers = locationManager.getAllProviders();
        if (providers.contains(provider)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, REQUEST_MIN_TIME, REQUEST_MIN_DISTANCE, locationListener);
        }
    }
    
    private void removeUpdates(String provider, LocationListener locationListener) {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
    
    public void onCreate() {
        onCreate(true, true, true);
    }
    
    public void onCreate(boolean gps, boolean network, boolean tigerknows) {
        synchronized (locationChangeLock) {
            if (gps) {
                if (onCreateGPS == 0) {
                    gpsLocationUpload.onCreate();
                }
                onCreateGPS++;
            }

            if (network) {
                if (onCreateNetwork == 0) {
                    networkLocationUpload.onCreate();
                }
                onCreateNetwork++;
            }
            
            if (tigerknows) {
                if (onCreateTigerknows == 0) {
                    locationQuery.onCreate();
                }
                onCreateTigerknows++;
            }
        }
    }
    
    public void onResume(TKLocationListener listener) {
        onResume(listener, true, true, true);
    }
    
    public void onResume(TKLocationListener listener, boolean gps, boolean network, boolean tigerknows) {
        synchronized (locationChangeLock) {

            if (gps) {
                if (onResumeGPS == 0) {
                    prepareLocation(LocationManager.GPS_PROVIDER, locationListenerGPS);
                }
                onResumeGPS++;
            }
            

            if (network) {
                if (onResumeNetwork == 0) {
                    prepareLocation(LocationManager.NETWORK_PROVIDER, locationListenerNetwork);
                }
                onResumeNetwork++;
            }

            if (tigerknows) {
                if (onResumeTigerknows == 0) {
                    networkLocationManager.bindService();
                    locationQuery.onResume();
                }
                onResumeTigerknows++;
            }
            
            lastLocation = null;
            if (listener != null) {
                locationListenerList.remove(listener);
                locationListenerList.add(listener);
            }
        }
        
    }
    
    public void onPause(TKLocationListener listener) {
        onPause(listener, true, true, true);
    }
    
    public void onPause(TKLocationListener listener, boolean gps, boolean network, boolean tigerknows) {
        synchronized (locationChangeLock) {
            locationListenerList.remove(listener);

            if (gps) {
                onResumeGPS--;
                if (onResumeGPS == 0) {
                    removeUpdates(LocationManager.GPS_PROVIDER, locationListenerGPS);
                }
                if (onResumeGPS < 0) {
                    onResumeGPS = 0;
                }
            }

            if (network) {
                onResumeNetwork--;
                if (onResumeNetwork == 0) {
                    removeUpdates(LocationManager.NETWORK_PROVIDER, locationListenerNetwork);
                }
                if (onResumeNetwork < 0) {
                    onResumeNetwork = 0;
                }
            }

            if (tigerknows) {
                onResumeTigerknows--;
                if (onResumeTigerknows == 0) {
                    networkLocationManager.unbindService();
                    locationQuery.onPause();
                }
                if (onResumeTigerknows < 0) {
                    onResumeTigerknows = 0;
                }
            }
        }
    }

    
    public void onDestroy() {
        onDestroy(true, true, true);
    }
    
    public void onDestroy(boolean gps, boolean network, boolean tigerknows) {
        synchronized (locationChangeLock) {
            if (gps) {
                onCreateGPS--;
                if (onCreateGPS == 0) {
                    gpsLocationUpload.onDestroy();
                }
                if (onCreateGPS < 0) {
                    onCreateGPS = 0;
                }
            }

            if (network) {
                onCreateNetwork--;
                if (onCreateNetwork == 0) {
                    networkLocationUpload.onDestroy();
                }
                if (onCreateNetwork < 0) {
                    onCreateNetwork = 0;
                }
            }

            if (tigerknows) {
                onCreateTigerknows--;
                if (onCreateTigerknows == 0) {
                    locationQuery.onDestory();
                }
                if (onCreateTigerknows < 0) {
                    onCreateTigerknows = 0;
                }
            }
        }
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
                                || Math.abs(System.currentTimeMillis() - lastGpsTKLocation.time) < TKLocationManager.GPS_TIME_OUT)) {
                    
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
                Location lastGpsLocation = TKLocationManager.this.lastGpsLocation;
                if (lastGpsLocation == null || lastGpsLocation.distanceTo(location) > 5000) {
                    TKLocationManager.this.lastGpsLocation = location;
                } else if (lastGpsLocation.distanceTo(location) > 500) {
                    TKLocationManager.this.lastGpsLocation = location;
                    final FeedbackUpload feedbackUpload = new FeedbackUpload(context);
                    // lip=2013-11-25:23:47:04,39.160395,106.700049,12.0
                    String lip = LocationQuery.SIMPLE_DATE_FORMAT.format(Calendar.getInstance().getTime()) + 
                            "," + location.getLongitude() + 
                            "," + location.getLatitude() + 
                            "," + location.getAccuracy();
                    feedbackUpload.addParameter(FeedbackUpload.SERVER_PARAMETER_LOCATION_IP, lip);
                    new Thread(new Runnable() {
                        
                        @Override
                        public void run() {
                            feedbackUpload.query();
                        }
                    }).start();
                }
                lastGpsTKLocation = new TKLocation(location, lac, cid, System.currentTimeMillis());   
                locationChanged(LOCATION_GPS);
                gpsLocationUpload.update(location);
            } else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) { 
                lastNetworkTKLocation = new TKLocation(location, lac, cid, -1); 
                TKLocation lastTigerknowsTKLocation = TKLocationManager.this.lastTigerknowsTKLocation;
                if (lastTigerknowsTKLocation == null || lastTigerknowsTKLocation.location.getProvider().equals("error")) {
                    locationChanged(LOCATION_NETWORK);
                    networkLocationUpload.update(location);
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
