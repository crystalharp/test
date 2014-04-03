
package com.tigerknows.common;

import java.util.List;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.LocationQuery.LocationParameter;
import com.tigerknows.model.LocationQuery.TKNeighboringCellInfo;
import com.tigerknows.model.LocationQuery.TKScanResult;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

/**
 * 定位日志记录上传类
 * @author pengwenyue
 *
 */
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
    
    private static LocationUpload sNetworkTrackInstance;

    public static LocationUpload getNetworkTrackInstance(Context context) {
        if (null == sNetworkTrackInstance) {
            sNetworkTrackInstance = new LocationUpload(context, "network.track", FeedbackUpload.SERVER_PARAMETER_LOCATION);
        }
        return sNetworkTrackInstance;
    }
    
    private int mnc = -1;
    
    private int mcc = -1;
    
    private static final float MIN_DISTANCE = 50f;

    private static final long LOGOUT_DISTANCE = 10000;
    
    private static final long LOGOUT_TIME = 900000;
    
    private Location lastLocation;
    
    private long sdx = 0;
    
    private long sdy = 0;
    
    private long sdt = 0;
    
    private LocationQuery locationQuery;
    
    private LocationUpload(Context context, String logFileName, String serverParameterKey) {
        super(context, logFileName, serverParameterKey);
        
        // 删除之前存储的定位信息，因为数据结构不同
        SharedPreferences writesettings = context.getSharedPreferences("location", Context.MODE_PRIVATE);
        writesettings.edit().clear().commit();
        
        locationQuery = LocationQuery.getInstance(context);

    }
    
    public void update(Location location) {

        synchronized (mLock) {
            if (location == null) {
                return;
            }
            
            if (lastLocation == null ||
                    lastLocation.distanceTo(location) > MIN_DISTANCE) {
                record(location, locationQuery.makeLocationParameter());
            }
        }
    }
    
    public void record(Location location, LocationParameter locationParameter) {
        LogWrapper.d(TAG, "record():" + location.getProvider() + "," + locationParameter);
        synchronized (mLock) {
            if (mStringBuilder == null) {
                return;
            }
            
            lastLocation = location;
            
            mcc = locationParameter.mcc;
            mnc = locationParameter.mnc;
            
            StringBuilder info = new StringBuilder();
            
            int lac = locationParameter.tkCellLocation.lac;
            int cid = locationParameter.tkCellLocation.cid;
            
            // 记录当前基站
            if (Utility.mccMncLacCidValid(mcc, mnc, lac, cid)) {
                info.append(String.format("%d.%d.%d.%d", mcc, mnc, lac, cid));
                info.append('@');
                info.append(Utility.asu2dbm(TKConfig.getSignalStrength()));
            }
            
            // 记录当前wifi队列
            List<TKScanResult> scanResultList = locationParameter.wifiList;
            if (scanResultList != null) {
                for (TKScanResult scanResult : scanResultList) {
                    if (info.length() > 0) {
                        info.append(';');
                    }
                    String bssid = scanResult.BSSID;
                    info.append(bssid);
                    info.append('@');
                    info.append(scanResult.level);
                }
            }
            
            // 记录邻近基站队列
            List<TKNeighboringCellInfo> neighboringCellInfoList = locationParameter.neighboringCellInfoList;
            if (neighboringCellInfoList != null) {
                for (TKNeighboringCellInfo neighboringCellInfo : neighboringCellInfoList) {
                    lac = neighboringCellInfo.lac;
                    cid = neighboringCellInfo.cid;
                    if (Utility.lacCidValid(lac, cid)) {
                        if (info.length() > 0) {
                            info.append(';');
                        }
                        info.append(String.format("%d.%d.%d.%d", mcc, mnc, lac, cid));
                        info.append('@');
                        info.append(Utility.asu2dbm(neighboringCellInfo.rssi));
                    }
                }
            }
            
            if (info.length() > 0) {
                long length = (mUploading ? mLogFileLength + mTmpLogFileLength : mLogFileLength);
                if (length > 0 || mStringBuilder.length() > 0) {
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
                
                writeAndUpload();
            }
            
        }
    }

    @Override
    public void onCreate() {
        synchronized (mLock) {
            if (onCreate) {
                return;
            }
            super.onCreate();
            reset();
        }
    }

    @Override
    protected boolean write(boolean temp) {
        synchronized (mLock) {
            reset();
            return super.write(temp);
        }
    }
    
    private void reset() {
        synchronized (mLock) {
            sdt = 0;
            sdx = 0;
            sdy = 0;
        }
    }
}
