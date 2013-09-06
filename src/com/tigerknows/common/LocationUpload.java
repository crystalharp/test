
package com.tigerknows.common;

import java.util.List;

import com.tigerknows.TKConfig;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.LocationQuery.TKCellLocation;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.telephony.NeighboringCellInfo;

public class LocationUpload extends LogUpload {
    
    /**
     * 保留小数点后6位数
     */
    static final long KEEP_VALUE = 1000000;
    
    /**
     * 保留小数点后6位数
     */
    static final int KEEP_ACCURACY = 6;
    
    private static LocationUpload sGpsInstance;

    public static LocationUpload getGpsInstance(Context context) {
        if (null == sGpsInstance) {
            sGpsInstance = new LocationUpload(context, "gps.location", FeedbackUpload.SERVER_PARAMETER_LOCATION);
        }
        return sGpsInstance;
    }
    
    private static LocationUpload sNetworkInstance;

    public static LocationUpload getNetworkInstance(Context context) {
        if (null == sNetworkInstance) {
            sNetworkInstance = new LocationUpload(context, "network.location", FeedbackUpload.SERVER_PARAMETER_LOCATION_IN_ANDROID);
        }
        return sNetworkInstance;
    }
    
    private WifiManager wifiManager;

    private int mnc = -1;
    
    private int mcc = -1;
    
    private static final float MIN_DISTANCE = 50f;

    private static final long LOGOUT_DISTANCE = 10000;
    
    private static final long LOGOUT_TIME = 900000;
    
    private Location lastLocation;
    
    private long sdx = 0;
    
    private long sdy = 0;
    
    private long sdt = 0;
    
    private LocationUpload(Context context, String logFileName, String serverParameterKey) {
        super(context, logFileName, serverParameterKey);
        this.wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        
        // 删除之前存储的定位信息，因为数据结构不同
        SharedPreferences writesettings = context.getSharedPreferences("location", Context.MODE_WORLD_WRITEABLE);
        writesettings.edit().clear().commit();

    }
    
    public void recordLocation(Location location) {
        synchronized (mLock) {
            if (mStringBuilder == null) {
                return;
            }
            
            if (location == null) {
                return;
            }
            
            if (lastLocation == null || lastLocation.distanceTo(location) > MIN_DISTANCE) {
                lastLocation = location;
                
                mcc = TKConfig.getMCC();
                mnc = TKConfig.getMNC();
                
                StringBuilder info = new StringBuilder();
                
                TKCellLocation tkCellLocation = TKConfig.getCellLocation();
                int lac = tkCellLocation.lac;
                int cid = tkCellLocation.cid;
                
                // 记录当前基站
                if (Utility.mccMncLacCidValid(mcc, mnc, lac, cid)) {
                    info.append(String.format("%d.%d.%d.%d", mcc, mnc, lac, cid));
                    info.append('@');
                    info.append(Utility.asu2dbm(TKConfig.getSignalStrength()));
                }
                
                // 记录当前wifi队列
                if (wifiManager != null) {
                    List<ScanResult> scanResultList = wifiManager.getScanResults();
                    if (scanResultList != null) {
                        for (ScanResult scanResult : scanResultList) {
                            if (info.length() > 0) {
                                info.append(';');
                            }
                            String bssid = scanResult.BSSID;
                            info.append(bssid);
                            info.append('@');
                            info.append(scanResult.level);
                        }
                    }
                }
                
                // 记录邻近基站队列
                List<NeighboringCellInfo> neighboringCellInfoList = TKConfig.getNeighboringCellList();
                if (neighboringCellInfoList != null) {
                    for (NeighboringCellInfo neighboringCellInfo : neighboringCellInfoList) {
                        lac = neighboringCellInfo.getLac();
                        cid = neighboringCellInfo.getCid();
                        if (Utility.lacCidValid(lac, cid)) {
                            if (info.length() > 0) {
                                info.append(';');
                            }
                            info.append(String.format("%d.%d.%d.%d", mcc, mnc, lac, cid));
                            info.append('@');
                            info.append(Utility.asu2dbm(neighboringCellInfo.getRssi()));
                        }
                    }
                }
                
                if (info.length() > 0) {
                    if (mLogFileLength > 0 || mStringBuilder.length() > 0) {
                        mStringBuilder.append('|');
                    }
                    long t = System.currentTimeMillis();
                    long x = (long)(Utility.doubleKeep(lastLocation.getLongitude(), KEEP_ACCURACY)*KEEP_VALUE);
                    long y = (long)(Utility.doubleKeep(lastLocation.getLatitude(), KEEP_ACCURACY)*KEEP_VALUE);
                    int changedsd = 0;
                    if (Math.abs(t - sdt) > LOGOUT_TIME) {
                        this.sdt = t;
                        mStringBuilder.append("sdt:");
                        mStringBuilder.append(this.sdt);
                        changedsd++;
                    }
                    if (Math.abs(this.sdx-x) > LOGOUT_DISTANCE) {
                        this.sdx = x;
                        if (changedsd > 0) {
                            mStringBuilder.append(',');
                        }
                        mStringBuilder.append("sdx:");
                        mStringBuilder.append(this.sdx);
                        changedsd++;
                    }
                    if (Math.abs(this.sdy-y) > LOGOUT_DISTANCE) {
                        this.sdy = y;
                        if (changedsd > 0) {
                            mStringBuilder.append(',');
                        }
                        mStringBuilder.append("sdy:");
                        mStringBuilder.append(this.sdy);
                        changedsd++;
                    }
                    if (changedsd > 0) {
                        mStringBuilder.append('|');
                    }
                    
                    mStringBuilder.append(t-this.sdt);
                    mStringBuilder.append(',');
                    mStringBuilder.append(x-this.sdx);
                    mStringBuilder.append(',');
                    mStringBuilder.append(y-this.sdy);
                    mStringBuilder.append(',');
                    mStringBuilder.append(((int) lastLocation.getAccuracy()));
                    mStringBuilder.append(','); 
                    mStringBuilder.append(info);
                    
                    tryUpload();
                }
            }
            
                
        }
    }
    
    public void onCreate() {
        synchronized (mLock) {
            super.onCreate();
            reset();
        }
    }
    
    protected void onLogOut() {
        synchronized (mLock) {
            super.onLogOut();
            if (mStringBuilder == null) {
                return;
            }
            reset();
            StringBuilder s = mStringBuilder;
            if (s.length() > 0 && s.charAt(0) == '|') {
                s.deleteCharAt(0);
            }
            mStringBuilder = new StringBuilder();
            mStringBuilder.append(s);
        }
    }
    
    protected void write() {
        synchronized (mLock) {
            super.write();
            reset();
        }
    }
    
    void reset() {
        sdt = 0;
        sdx = 0;
        sdy = 0;
    }
}
