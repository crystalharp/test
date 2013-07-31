
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
                
                if (sdx == 0 || sdy == 0 || sdt == 0) {
                    sdx = (long)(Utility.doubleKeep(lastLocation.getLatitude(), 5)*100000);
                    sdy = (long)(Utility.doubleKeep(lastLocation.getLatitude(), 5)*100000);
                    sdt = System.currentTimeMillis();
                }
                
                mcc = TKConfig.getMCC();
                mnc = TKConfig.getMNC();
                
                StringBuilder info = new StringBuilder();
                
                TKCellLocation tkCellLocation = TKConfig.getCellLocation();
                int lac = tkCellLocation.lac;
                int cid = tkCellLocation.cid;
                
                // 记录当前基站
                if (Utility.mccMncLacCidValid(mcc, mnc, lac, cid)) {
                    info.append(String.format("%d.%d.%d.%d", mcc, mnc, lac, cid));
                    info.append("@");
                    info.append(Utility.asu2dbm(TKConfig.getSignalStrength()));
                }
                
                // 记录当前wifi队列
                if (wifiManager != null) {
                    List<ScanResult> scanResultList = wifiManager.getScanResults();
                    if (scanResultList != null) {
                        for (ScanResult scanResult : scanResultList) {
                            if (info.length() > 0) {
                                info.append(";");
                            }
                            String bssid = scanResult.BSSID;
                            info.append(bssid);
                            info.append("@");
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
                                info.append(";");
                            }
                            info.append(String.format("%d.%d.%d.%d", mcc, mnc, lac, cid));
                            info.append("@");
                            info.append(Utility.asu2dbm(neighboringCellInfo.getRssi()));
                        }
                    }
                }
                
                if (info.length() > 0) {
                    if (mLogFileLength > 0 || mStringBuilder.length() > 0) {
                        mStringBuilder.append("|");
                    }
                    long current = System.currentTimeMillis();
                    long sdx = (long)(Utility.doubleKeep(lastLocation.getLatitude(), 5)*100000);
                    long sdy = (long)(Utility.doubleKeep(lastLocation.getLongitude(), 5)*100000);
                    if (Math.abs(current - sdt) > LOGOUT_TIME ||
                            Math.abs(this.sdx-sdx) > LOGOUT_DISTANCE ||
                            Math.abs(this.sdy-sdy) > LOGOUT_DISTANCE) {
                        this.sdx = sdx;
                        this.sdy = sdy;
                        this.sdt = current;
                        mStringBuilder.append("sdt:");
                        mStringBuilder.append(this.sdt);
                        mStringBuilder.append(",sdx:");
                        mStringBuilder.append(this.sdx);
                        mStringBuilder.append(",sdy:");
                        mStringBuilder.append(this.sdy);
                        mStringBuilder.append("|");
                        mStringBuilder.append("0,0,0,");
                    } else {
                        mStringBuilder.append(current-sdt);
                        mStringBuilder.append(",");
                        mStringBuilder.append(sdx-this.sdx);
                        mStringBuilder.append(",");
                        mStringBuilder.append(sdy-this.sdy);
                        mStringBuilder.append(",");
                    }
                    mStringBuilder.append(((int) lastLocation.getAccuracy()));
                    mStringBuilder.append(","); 
                    mStringBuilder.append(info);
                    
                    tryUpload();
                }
            }
            
                
        }
    }
    
    protected void onLogOut() {
        super.onLogOut();
        sdx = 0;
        sdy = 0;
        sdt = 0;
    }
}
