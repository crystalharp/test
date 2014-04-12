/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.android.app.TKService;
import com.tigerknows.android.location.TKLocationListener;
import com.tigerknows.android.location.TKLocationManager;
import com.tigerknows.common.LocationUpload;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.LocationQuery.LocationParameter;
import com.tigerknows.util.Utility;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * 收集GPS、网络基站、邻近基站、WIFI列表信息的服务
 * @author pengwenyue
 */
public class CollectService extends TKService {
    
    static final String TAG = "CollectService";
    
    public static final long RETRY_INTERVAL = 2 * 1000;
    
    public static final long RETRY_CHECK_LOCATION_INTERVAL = 16 * 1000;
    
    private static final int CACHE_SIZE = 256;
    
    private static final String EXTRA_PAUSE_LOCATION_MANAGER = "EXTRA_PAUSE_LOCATION_MANAGER";
    
    private Context mContext;
    
    private LocationQuery mLocationQuery;
    
    private int mCheckTime = 0;
    
    private int mRecordTime = 0;
    
    private boolean mStop;
    
    private long mLastCheckLocationTime;
    
    private LinkedHashMap<LocationParameter, Location> mLocationCache = new LinkedHashMap<LocationParameter, Location>(CACHE_SIZE * 2, 0.75f, true);
    
    private Location mLocation = new Location(LocationManager.NETWORK_PROVIDER);
    
    private LocationUpload mLocationUpload;
    
    private ActivityManager mActivityManager;
    
    private List<String> mHomePackageNames = new ArrayList<String>();
    
    private String mCurrentAppName;
    
    private LocationManager mLocationManager;
    
    private GpsStatus.Listener mGpsStatusListener;
    
    private TKLocationManager mTKLocationManager;
    
    private TKLocationListener mLocationListener = new TKLocationListener() {
        
        @Override
        public void onLocationChanged(Location location) {
            // do no thing
        }
    };
    
    private BroadcastReceiver mBroadcastReceiver;
    
    private boolean mTKLocationManagerOnCreate = false;
    
    private boolean mAddGpsStatusListener = false;
    
    private static boolean sCanListenerGps = true;
    
    @Override
    public void onCreate() {
        LogWrapper.d(TAG, "onCreate");
        super.onCreate();
        
        mContext = getApplicationContext();
        
        mStop = false;
        mLocation.setLatitude(99.99999d);
        mLocation.setLongitude(99.99999d);
        mLocationQuery = LocationQuery.getInstance(mContext);
        mLocationUpload = LocationUpload.getNetworkTrackInstance(mContext);
        mLocationUpload.onCreate();

        mActivityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        initHomeAppList();
        mLocationManager =(LocationManager)getSystemService(Context.LOCATION_SERVICE);
        // 监听GPS定位请求的事件
        mGpsStatusListener = new GpsStatus.Listener() {
            public void onGpsStatusChanged(int event) {
                switch (event) {

//                    // 第一次定位
//                    case GpsStatus.GPS_EVENT_FIRST_FIX:
//                        LogWrapper.i(TAG, "GPS_EVENT_FIRST_FIX");
//                        break;
//
//                    // 卫星状态改变
//                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
//                        LogWrapper.i(TAG, "GPS_EVENT_SATELLITE_STATUS");
//                        // 获取当前状态
//                        GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
//                        // 获取卫星颗数的默认最大值
//                        int maxSatellites = gpsStatus.getMaxSatellites();
//                        // 创建一个迭代器保存所有卫星
//                        Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
//                        int count = 0;
//                        while (iters.hasNext() && count <= maxSatellites) {
//                            GpsSatellite s = iters.next();
//                            count++;
//                        }
//                        LogWrapper.i(TAG, "搜索到：" + count + "颗卫星");
//                        break;

                    // 定位启动
                    case GpsStatus.GPS_EVENT_STARTED:
                        LogWrapper.i(TAG, "GPS_EVENT_STARTED");
                        mCurrentAppName = getCurrentAppName();
                        resumeLocationManager();
                        break;

                    // 定位结束
                    case GpsStatus.GPS_EVENT_STOPPED:
                        LogWrapper.i(TAG, "GPS_EVENT_STOPPED");
                        mCurrentAppName = null;
                        pauseLocationManager();
                        break;
                }
            };
        };

        mTKLocationManager = TKLocationManager.getInstatce(getApplicationContext());
        
        // 通过反射功能，监听GPS定位开关的广播
        try {
            Class classLocationManager = Class.forName("android.location.LocationManager");
            final Field extraGpsEnabled = classLocationManager.getField("EXTRA_GPS_ENABLED");
            final Field gpsEnabledChanged = classLocationManager.getField("GPS_ENABLED_CHANGE_ACTION");
            if (classLocationManager != null && extraGpsEnabled != null && gpsEnabledChanged != null) {
                mBroadcastReceiver = new BroadcastReceiver() {
                    
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        try {
                            boolean enabled = intent.getBooleanExtra((String)extraGpsEnabled.get(null), false);
                            LogWrapper.d(TAG, "enabled:"+enabled);
                            if (enabled) {
//                                addGpsStatusListener();
//                                resumeLocationManager();
                            } else {
//                                removeGpsStatusListener();
//                                pauseLocationManager();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                registerReceiver(mBroadcastReceiver, new IntentFilter((String) gpsEnabledChanged.get(null)));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
//        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            addGpsStatusListener();
//        }
        
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                while (mStop == false) {
                    try {
                        Thread.sleep(RETRY_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!TKConfig.isSwitch(BootstrapModel.FIELD_COLLECT_NETWORK_INFO) &&
                            !TKConfig.isSwitch(BootstrapModel.FIELD_COLLECT_GPS_INFO)) {
                        stopSelf();
                    }
                    
                    long time = System.currentTimeMillis();
                    if (time - mLastCheckLocationTime > RETRY_CHECK_LOCATION_INTERVAL) {
                        checkLocationParameter();
                        mLastCheckLocationTime = time;
                    }
                    
                    // 若当前应用是系统桌面则撤消对GPS的监听（此判断是因为高德地图（或类似应用）也与我们做相同的事情，导致死锁最后都不撤消对GPS的监听）
                    if (mTKLocationManagerOnCreate) {
                        String currentAppName = getCurrentAppName();
                        boolean inAndroidHomeApp = inAndroidHomeApp(currentAppName);
                        LogWrapper.d(TAG, "inAndroidHomeApp="+inAndroidHomeApp);
                        if (inAndroidHomeApp || 
                                mCurrentAppName == null ||
                                !mCurrentAppName.equals(currentAppName)) {
                            pauseLocationManager();
                        }
                    }
                    
                }
                
            }
        }).start();
    }
    
    private void addGpsStatusListener() {
        LogWrapper.d(TAG, "addGpsStatusListener");
        synchronized (mLocationManager) {
            if (mAddGpsStatusListener == false) {
                mLocationManager.addGpsStatusListener(mGpsStatusListener);
            }
            mAddGpsStatusListener = true;
        }
    }
    
    private void removeGpsStatusListener() {
        LogWrapper.d(TAG, "removeGpsStatusListener");
        synchronized (mLocationManager) {
            if (mAddGpsStatusListener) {
                mLocationManager.removeGpsStatusListener(mGpsStatusListener);
            }
            mAddGpsStatusListener = false;
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getBooleanExtra(EXTRA_PAUSE_LOCATION_MANAGER, false)) {
                pauseLocationManager();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
    
    public static void pauseLocationManager(Context context) {
        LogWrapper.d(TAG, "static pauseLocationManager");
        sCanListenerGps = false;
        Intent service = new Intent(context, CollectService.class);
        service.putExtra(EXTRA_PAUSE_LOCATION_MANAGER, true);
        context.startService(service);
    }
    
    public static void resumeLocationManager(Context context) {
        LogWrapper.d(TAG, "static resumeLocationManager");
        sCanListenerGps = true;
    }
    
    private void resumeLocationManager() {
        LogWrapper.d(TAG, "resumeLocationManager");
        if (sCanListenerGps == false) {
            return;
        }
        if (!TKConfig.isSwitch(BootstrapModel.FIELD_COLLECT_GPS_INFO)) {
            return;
        }
        synchronized (mTKLocationManager) {
            if (mTKLocationManagerOnCreate == false) {
                mTKLocationManager.onCreate(true, false, false);
                mTKLocationManager.onResume(mLocationListener, true, false, false);
            }
            mTKLocationManagerOnCreate = true;
        }
    }

    /**
     * 撤消对GPS定位的监听
     */
    private void pauseLocationManager() {
        LogWrapper.d(TAG, "pauseLocationManager");
        synchronized (mTKLocationManager) {
            if (mTKLocationManagerOnCreate) {
                mTKLocationManager.onPause(mLocationListener, true, false, false);
                mTKLocationManager.onDestroy(true, false, false);
            }
            mTKLocationManagerOnCreate = false;
        }
    }

    @Override
    public void onDestroy() {
        LogWrapper.d(TAG, "onDestroy");
        mStop = true;
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
        removeGpsStatusListener();
        pauseLocationManager();
        mLocationUpload.onDestroy();
        super.onDestroy();
    }
    
    /**
     * 初始化属于桌面的应用的应用包名称列表
     */
    private void initHomeAppList() {
        mHomePackageNames.clear();
        PackageManager packageManager = this.getPackageManager();
        //属性
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for(ResolveInfo ri : resolveInfo){
            String packageName = ri.activityInfo.packageName;
            mHomePackageNames.add(packageName);
            LogWrapper.d(TAG, "homeApp=" + packageName);
        }
    }
    
    /**
     * 判断当前界面是否是桌面
     */
    private boolean inAndroidHomeApp(String appName){
        return mHomePackageNames.contains(appName);
    }
    
    /**
     * 获取当前应用的名称
     * @return
     */
    private String getCurrentAppName(){
        List<RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
        if (rti != null && rti.size() > 0) {
            return rti.get(0).topActivity.getPackageName();
        } else {
            return null;
        }
    }
    
    /**
     * 遍历缓存定位列表，记录当前网络基站、邻近基站、WIFI列表信息
     */
    private void checkLocationParameter() {
        if (!TKConfig.isSwitch(BootstrapModel.FIELD_COLLECT_NETWORK_INFO)) {
            return;
        }
        mLocationQuery.startScanWifi();
        
        LocationParameter locationParameter = mLocationQuery.makeLocationParameter();
        
        if (Utility.mccMncLacCidValid(locationParameter.mcc,
                                      locationParameter.mnc,
                                      locationParameter.tkCellLocation.lac,
                                      locationParameter.tkCellLocation.cid) == false &&
               (locationParameter.wifiList == null ||
                   locationParameter.wifiList.size() == 0)) {
            return;
        }

        Location location = LocationQuery.queryCache(locationParameter, mLocationCache, false);
        
        mCheckTime++;
        if (location == null) {
            mRecordTime++;
            mLocationUpload.record(mLocation, locationParameter);
            mLocationCache.put(locationParameter, mLocation);
        }
        LogWrapper.i(TAG, "checkLocationParameter() mCheckTime:"+ mCheckTime + ", mRecordTime:"+mRecordTime);
    }
}
