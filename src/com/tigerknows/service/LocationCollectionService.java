package com.tigerknows.service;

import com.tigerknows.TKConfig;
import com.tigerknows.model.LocationQuery.LocationParameter;
import com.tigerknows.model.LocationQuery.TKNeighboringCellInfo;
import com.tigerknows.model.LocationQuery.TKScanResult;
import com.tigerknows.provider.LocationTable;
import com.tigerknows.radar.Alarms;

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

import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class LocationCollectionService extends Service {

    public static final String ACTION_LOCATION_COLLECTION_COMPLATE = "action.com.tigerknows.location.collection.complate";
    public static final int REQUEST_MIN_TIME = 10;
    public static final int REQUEST_MIN_DISTANCE = 10;
    static final long COLLECTION_INTERVAL = 6 * 1000;
    static final long COLLECTION_TIME = 150;

    private WifiManager wifiManager;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location lastGpsLocation = null;
    private LocationParameter lastLocationParameter = null;
    
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
        next.set(Calendar.HOUR, makeRandomHour());
        next.set(Calendar.MINUTE, makeRandomMinute());
        next.add(Calendar.DAY_OF_YEAR, 1);
        TKConfig.setPref(context,
                TKConfig.PREFS_RADAR_LOCATION_COLLECTION_ALARM, 
                Alarms.SIMPLE_DATE_FORMAT.format(next.getTime()));
        Intent name = new Intent(this, LocationCollectionService.class);
        Alarms.enableAlarm(context, next.getTimeInMillis(), name);
        stopService(name);
    }
    
    int makeRandomHour() {
        int hour = 6;
        Random ran =new Random(System.currentTimeMillis()); 
        hour += ran.nextInt(16);
        return hour;
    }
    
    int makeRandomMinute() {
        int minute = 0;
        Random ran =new Random(System.currentTimeMillis()); 
        minute = ran.nextInt(60);
        return minute;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}