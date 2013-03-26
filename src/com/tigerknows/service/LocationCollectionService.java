package com.tigerknows.service;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.model.LocationQuery.LocationParameter;
import com.tigerknows.model.LocationQuery.TKNeighboringCellInfo;
import com.tigerknows.model.LocationQuery.TKScanResult;
import com.tigerknows.provider.LocationTable;
import com.tigerknows.radar.Alarms;
import com.tigerknows.radar.Alarms.AlarmAction;
import com.tigerknows.radar.LocationCollectorReceiver;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.NeighboringCellInfo;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.List;

public class LocationCollectionService extends Service {
    
    static final String TAG = "LocationCollectionService";

    public static final String ACTION_LOCATION_COLLECTION_COMPLATE = "action.com.tigerknows.location.collection.complate";
    public static final int REQUEST_MIN_TIME = 10;
    public static final int REQUEST_MIN_DISTANCE = 10;
    static final long COLLECTION_INTERVAL = 6 * 1000;
    static final long COLLECTION_TIME = 150;
    final static int requestStartHour = 7;
    final static int requestEndHour = 20;

    private WifiManager wifiManager;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location lastGpsLocation = null;
    private LocationParameter lastLocationParameter = null;
    
    public static LocationAlarmAction alarmAction = new LocationAlarmAction();
    
    void requestLocationUpdates() {
        lastGpsLocation = null;
        lastLocationParameter = null;
        if (locationManager != null) {
            if (locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
            locationListener = new LocationListener() {
                
                @Override
                public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
                    // TODO Auto-generated method stub
                    
                }
                
                @Override
                public void onProviderEnabled(String arg0) {
                    // TODO Auto-generated method stub
                    
                }
                
                @Override
                public void onProviderDisabled(String arg0) {
                    // TODO Auto-generated method stub
                    
                }
                
                @Override
                public void onLocationChanged(Location location) {
                    lastGpsLocation = location;
                    lastLocationParameter = new LocationParameter();
                    lastLocationParameter.mcc = TKConfig.getMCC();
                    lastLocationParameter.mnc = TKConfig.getMNC();
                    lastLocationParameter.tkCellLocation = TKConfig.getCellLocation();
                    List<NeighboringCellInfo> neighboringCellList = TKConfig.getNeighboringCellList();
                    if (neighboringCellList != null) {
                        for(int i = neighboringCellList.size() - 1; i >= 0; i--) {
                            lastLocationParameter.neighboringCellInfoList.add(new TKNeighboringCellInfo(neighboringCellList.get(i)));
                        }
                    }
                    if (wifiManager != null) {
                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        if (scanResults != null) {
                            for(int i = scanResults.size() - 1; i >= 0; i--) {
                                ScanResult scanResult = scanResults.get(i);
                                lastLocationParameter.wifiList.add(new TKScanResult(scanResult));
                            }
                        }
                    }
                    LocationTable locationTable = new LocationTable(getBaseContext());
                    lastGpsLocation.setProvider(TKLocationManager.GPS_COLLECTION_PROVIDER);
                    locationTable.write(lastLocationParameter, lastGpsLocation);
                }
            };
            List<String> providers = locationManager.getAllProviders();
            if (providers != null
                    && providers.contains(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, REQUEST_MIN_TIME, REQUEST_MIN_DISTANCE, locationListener);
            }
        }
    }
    
    void removeUpdates() {
        if (locationManager != null) {
            if (locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        LogWrapper.d(TAG, "onCreate()");
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        
        requestLocationUpdates();
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                int i = 0;
                while (lastGpsLocation == null && i < COLLECTION_TIME) {
                    try {
                        Thread.sleep(COLLECTION_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    i++;
                }
                removeUpdates();
                exitService();
            }
        }).start();
    }
    
    void exitService() {
        Context context = getApplicationContext();
        long currentTimeMillis = System.currentTimeMillis();
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(currentTimeMillis);
        next = Alarms.calculateRandomAlarmInNextDay(next, requestStartHour, requestEndHour);
        Alarms.enableAlarm(context, next, alarmAction);
        Intent name = new Intent(context, LocationCollectionService.class);
        stopService(name);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
    
    public static class LocationAlarmAction implements AlarmAction {

        @Override
        public void saveAlarm(Context context, String absAlarm, String relAlarm) {
            if (absAlarm != null && !TextUtils.isEmpty(absAlarm)) {
                TKConfig.setPref(context, TKConfig.PREFS_RADAR_LOCATION_COLLECTION_ALARM_ABSOLUTE, absAlarm);
            }
            if (relAlarm != null && !TextUtils.isEmpty(relAlarm)) {
                TKConfig.setPref(context, TKConfig.PREFS_RADAR_LOCATION_COLLECTION_ALARM_RELETIVE, relAlarm);
            }
        }

        @Override
        public Intent getIntent() {
            return new Intent(LocationCollectorReceiver.ACTION_LOCATION_COLLECTION);
        }

        @Override
        public String getAbsAlarm(Context context) {
            return TKConfig.getPref(context, TKConfig.PREFS_RADAR_LOCATION_COLLECTION_ALARM_ABSOLUTE, "");
        }

        @Override
        public String getRelAlarm(Context context) {
            return TKConfig.getPref(context, TKConfig.PREFS_RADAR_LOCATION_COLLECTION_ALARM_RELETIVE, "");
        }
        
    }
}