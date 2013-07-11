
package com.tigerknows.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import com.tigerknows.TKConfig;
import com.tigerknows.model.LocationQuery.TKCellLocation;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.telephony.NeighboringCellInfo;

public class LocationUpload {
    
    private static LocationUpload sInstance;

    public static LocationUpload getInstance(Context context) {
        if (null == sInstance) {
            sInstance = new LocationUpload(context);
        }
        return sInstance;
    }
    
    private Context context;
    
    private WifiManager wifiManager;

    private int mnc = -1;
    
    private int mcc = -1;
    
    private static final int CACHE_DATA_SIZE = 1024 * 20;
    
    private static final float UPLOAD_MIN_DISTANCE = 50f;
    
    private Object writeLock = new Object();
    
    private StringBuilder dataGps = null;
    
    private Calendar startTimeGps = Calendar.getInstance();
    
    private StringBuilder dataNetwork = null;
    
    private Calendar startTimeNetwork = Calendar.getInstance();
    
    public static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss");
    
    private Location locationGps = new Location(LocationManager.GPS_PROVIDER);
    
    private Location locationNetwork = new Location(LocationManager.NETWORK_PROVIDER);

    private LocationUpload(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager)this.context.getSystemService(Context.WIFI_SERVICE);
    }
    
    public void recordLocation(Location location) {
        synchronized (writeLock) {

            if (location == null || dataGps == null || dataNetwork == null) {
                return;
            }
            boolean notToday = false;
            boolean isGps = location.getProvider().equals(LocationManager.GPS_PROVIDER); 
            StringBuilder data;
            if (isGps) {
                if (locationGps.distanceTo(location) > UPLOAD_MIN_DISTANCE) {
                    locationGps = location;
                    data = dataGps;
                    Calendar calendar = Calendar.getInstance();
                    notToday = isNotToday(startTimeGps, calendar);
                } else {
                    return;
                }
            } else {
                
                if (locationNetwork.distanceTo(location) > UPLOAD_MIN_DISTANCE) {
                    locationNetwork = location;
                    data = dataNetwork;
                    Calendar calendar = Calendar.getInstance();
                    notToday = isNotToday(startTimeNetwork, calendar);
                } else {
                    return;
                }
            }

            mcc = TKConfig.getMCC();
            mnc = TKConfig.getMNC();
            writeToData(location, isGps, data, notToday);
        }
    }
    
    private boolean isNotToday(Calendar calendar1, Calendar calendar2) {
        if (calendar1.get(Calendar.YEAR) != calendar2.get(Calendar.YEAR)) {
            return true;
        } else if (calendar1.get(Calendar.MONTH) != calendar2.get(Calendar.MONTH)) {
            return true;
        } else if (calendar1.get(Calendar.DAY_OF_MONTH) != calendar2.get(Calendar.DAY_OF_MONTH)) {
            return true;
        }
        return false;
    }
    
    private void writeToData(Location location, boolean isGps, StringBuilder data, boolean notToday) {
        boolean overflow = (data.length() > CACHE_DATA_SIZE); 
        if (!overflow) {
            StringBuilder address = new StringBuilder();

            TKCellLocation tkCellLocation = TKConfig.getCellLocation();
            int lac = tkCellLocation.lac;
            int cid = tkCellLocation.cid;
            
            // 记录当前基站
            if (Utility.mccMncLacCidValid(mcc, mnc, lac, cid)) {
                address.append(String.format("%d.%d.%d.%d", mcc, mnc, lac, cid));
                address.append("@");
                address.append(Utility.asu2dbm(TKConfig.getSignalStrength()));
                address.append(";");
            }
            
            // 记录当前wifi队列
            if (wifiManager != null) {
                List<ScanResult> scanResultList = wifiManager.getScanResults();
                if (scanResultList != null) {
                    for (ScanResult scanResult : scanResultList) {
                        
                        String bssid = scanResult.BSSID;
                        address.append(bssid);
                        address.append("@");
                        address.append(scanResult.level);
                        address.append(";");
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
                        address.append(String.format("%d.%d.%d.%d", mcc, mnc, lac, cid));
                        address.append("@");
                        address.append(Utility.asu2dbm(neighboringCellInfo.getRssi()));
                        address.append(";");
                    }
                }
            }
            
            if (address.length() > 1) {
                if (data.length() > 0) {
                    data.append("|");
                }
                data.append(SIMPLE_DATE_FORMAT.format(Calendar.getInstance().getTime()));
                data.append(",");
                data.append(Utility.doubleKeep(location.getLatitude(), 6));
                data.append(",");
                data.append(Utility.doubleKeep(location.getLongitude(), 6));
                data.append(",");
                data.append(location.getAccuracy());
                data.append(","); 
                data.append(address.substring(0, address.length()-1));
            }

        }
            
        if (overflow || notToday) {
            uploadData(isGps, data.toString());
        }
    }
    
    private void uploadData(final boolean isGps, final String data) {
        // 异步上传定位数据
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                FeedbackUpload feedbackUpload = new FeedbackUpload(context);
                if (isGps) {
                    Hashtable<String, String> criteria = new Hashtable<String, String>();
                    criteria.put(FeedbackUpload.SERVER_PARAMETER_LOCATION, data);
                    feedbackUpload.setup(criteria);
                } else {
                    Hashtable<String, String> criteria = new Hashtable<String, String>();
                    criteria.put(FeedbackUpload.SERVER_PARAMETER_LOCATION_IN_ANDROID, data);
                    feedbackUpload.setup(criteria);
                }
                feedbackUpload.query();
                Response response = feedbackUpload.getResponse();
                if (response != null && response.getResponseCode() == Response.RESPONSE_CODE_OK) {
                    synchronized (writeLock) {
                        if (dataGps == null || dataNetwork == null) {
                            return;
                        }
                        if (isGps) {
                            startTimeGps = Calendar.getInstance();
                            dataGps = new StringBuilder();
                        } else {
                            startTimeNetwork = Calendar.getInstance();
                            dataNetwork = new StringBuilder();
                        }
                    }
                }
            }
        }).start();
    }
    
    public void onCreate() {
        synchronized (writeLock) {
            dataGps = new StringBuilder();
            dataNetwork = new StringBuilder();
            startTimeGps = Calendar.getInstance();
            startTimeNetwork = Calendar.getInstance();
            SharedPreferences sharedPreferences = context.getSharedPreferences("location", Context.MODE_WORLD_READABLE);
            String data = sharedPreferences.getString("gps", "");
            if (data.length() > 0) {
                dataGps.append(data);
                parseDate(startTimeGps, data);
            }
            data = sharedPreferences.getString("network", "");
            if (data.length() > 0) {
                dataNetwork.append(data);
                parseDate(startTimeNetwork, data);
            }
        }
    }
    
    private void parseDate(Calendar calendar, String str) {
        try {
            String[] date = str.substring(0, 10).split("-");
            calendar.set(Integer.parseInt(date[0]),
                    Integer.parseInt(date[1]),
                    Integer.parseInt(date[2]));
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    
    public void onDestroy() {
        synchronized (writeLock) {
            if (dataGps == null || dataNetwork == null) {
                return;
            }
            SharedPreferences writesettings = context.getSharedPreferences("location", Context.MODE_WORLD_WRITEABLE);
            writesettings.edit().putString("gps", dataGps.toString()).commit();
            writesettings.edit().putString("network", dataNetwork.toString()).commit();
            dataGps = null;
            dataNetwork = null;
        }
    }
}
