
package com.tigerknows.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.model.response.Appendix;
import com.tigerknows.model.response.PositionCake;
import com.tigerknows.model.response.ResponseCode;
import com.tigerknows.provider.LocationTable;
import com.tigerknows.service.TigerknowsLocationManager;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.ParserUtil;

import android.content.Context;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;

public class LocationQuery extends BaseQuery {
    
    static final String TAG = "LocationQuery";
    
    public static final String PROVIDER_ERROR = "tkerror";
    
    public static final String PROVIDER_DATABASE = "tkdatabase";
    
    static final float WIFI_RATE = 0.8f;
    
    protected static final String VERSION = "4";
    
    public static final int LOCATION_RESPONSE_CODE_NONE = 0;
    
    public static final int LOCATION_RESPONSE_CODE_SUCCEED = 200;

    public static final int LOCATION_RESPONSE_CODE_FAILED = 404;

    private static LocationQuery sInstance;

    public static LocationQuery getInstance(Context context) {
        if (null == sInstance) {
            sInstance = new LocationQuery(context);
        }
        return sInstance;
    }
    
    private Location location;

    private int locationResponseCode;

    private WifiManager wifiManager;
    
    private LocationParameter lastLocationParameter = new LocationParameter();
    
    private float rate = 0f;
    
    private HashMap<LocationParameter, Location> onlineLocationCache = new HashMap<LocationParameter, Location>();
    
    private HashMap<LocationParameter, Location> offlineLocationCache = new HashMap<LocationParameter, Location>();
    
    private LocationTable locationTable = null;
    
    private boolean readOfflineLocationCache = false;

    public void clearCache() {
        onlineLocationCache.clear();
        offlineLocationCache.clear();
    }

    private LocationQuery(Context context) {
        super(context, API_TYPE_LOCATION_QUERY, VERSION);
        this.wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public synchronized void query() {
        location = null;
        locationResponseCode = LOCATION_RESPONSE_CODE_NONE;
        super.query();
        if (locationResponseCode == LOCATION_RESPONSE_CODE_FAILED || locationResponseCode == LOCATION_RESPONSE_CODE_SUCCEED) {
            LogWrapper.i("LocationQuery", "query():location="+location);

            if (location == null) {
                location = new Location(PROVIDER_ERROR);
            } else {
                checkLocationTable();
                locationTable.write(lastLocationParameter, location);
            }
            
            onlineLocationCache.put(lastLocationParameter, location);
        }
    }
    
    boolean isTest = false;
    public synchronized Location queryLocation(boolean isTest) {
        this.isTest = isTest;
        super.query();
        return location;
    }

    @Override
    protected void makeRequestParameters() throws APIException {
        super.makeRequestParameters();
        addCommonParameters(requestParameters, Globals.getCurrentCityId(), true);
        
        if (wifiManager != null) {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (scanResults != null) {
                for (ScanResult sr : scanResults) {
                    requestParameters.add("wifi_mac[]", sr.BSSID);
                    requestParameters.add("wifi_ss[]", String.valueOf(sr.level));
                }
            }
        }

        List<NeighboringCellInfo> list = TKConfig.getNeighboringCellList();
        if (list != null) {
            int lac,cid;
            for (NeighboringCellInfo cellInfo : list) {
                lac = cellInfo.getLac();
                cid = cellInfo.getCid();
                if (CommonUtils.lacCidValid(lac, cid)) {
                    requestParameters.add("n8b_lac[]", String.valueOf(lac));
                    requestParameters.add("n8b_ci[]", String.valueOf(cid));
                    requestParameters.add("n8b_ss[]", String.valueOf(CommonUtils.asu2dbm(cellInfo.getRssi())));
                }
            }
        }
//        List<NeighboringCellInfo> list = TigerknowsConfig.getNeighboringCellList();
//        if (list != null
//                && (Build.VERSION.SDK.equals("3") || Build.VERSION.SDK.equals("4"))
//                && (TigerknowsConfig.getNetworkType() == TelephonyManager.NETWORK_TYPE_GPRS
//                        || TigerknowsConfig.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE || TigerknowsConfig
//                        .getNetworkType() == TelephonyManager.NETWORK_TYPE_CDMA)) {
//            int cid;
//            for (NeighboringCellInfo neighboringCellInfo : list) {
//                cid = neighboringCellInfo.getCid();
//                parameters.add("n8b_lac[]", String.valueOf(String.valueOf(cid >>> 16))));
//                parameters.add("n8b_ci[]", String.valueOf(cid & 0xffff)));
//                parameters.add("n8b_ss[]", String.valueOf(neighboringCellInfo.getRssi())));
//            }
//        }
        requestParameters.add("radio_type", TKConfig.getRadioType());
    }

    @Override
    protected void createHttpClient() {
        super.createHttpClient();
        httpClient.setIsEncrypt(false);
        String url = String.format(TKConfig.getLocationUrl(), isTest ? TKConfig.getLocationHostForTest() : TKConfig.getLocationHost());
        httpClient.setURL(url);
    }

    @Override
    protected void translateResponseV12(ParserUtil util) throws IOException {
        super.translateResponseV12(util);
        try {
            for (Appendix appendix : appendice) {
                if (appendix.type == Appendix.TYPE_RESPONSE_CODE) {
                    locationResponseCode = ((ResponseCode)appendix).getResponseCode();
                    break;
                }
            }

            if (locationResponseCode == LOCATION_RESPONSE_CODE_SUCCEED) {
                for (Appendix appendix : appendice) {
                    if (appendix.type == Appendix.TYPE_POSITION) {
                        PositionCake positionCake = (PositionCake)appendix;
                        location = new Location(TigerknowsLocationManager.TIGERKNOWS_PROVIDER);
                        location.setLatitude(positionCake.getLat() / 1000000.0d);
                        location.setLongitude(positionCake.getLon() / 1000000.0d);
                        location.setAccuracy(positionCake.getAccuracy());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    int time = 0;
    int queryTime = 0;
    public synchronized Location getLocation() {
        isTest = false;
        synchronized (this) {
        LocationParameter locationParameter = new LocationParameter();
        locationParameter.mcc = TKConfig.getMCC();
        locationParameter.mnc = TKConfig.getMNC();
        locationParameter.tkCellLocation = TKConfig.getCellLocation();
        List<NeighboringCellInfo> neighboringCellList = TKConfig.getNeighboringCellList();
        if (neighboringCellList != null) {
            for(int i = neighboringCellList.size() - 1; i >= 0; i--) {
                locationParameter.neighboringCellInfoList.add(new TKNeighboringCellInfo(neighboringCellList.get(i)));
            }
        }
        if (wifiManager != null) {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (scanResults != null) {
                for(int i = scanResults.size() - 1; i >= 0; i--) {
                    ScanResult scanResult = scanResults.get(i);
                    locationParameter.wifiList.add(new TKScanResult(scanResult));
                }
            }
        }

        Location location = null;
        rate = 0f;
        location = queryCache(locationParameter, onlineLocationCache, true);
        
        time++;
        if (location == null) {
            queryTime++;
            lastLocationParameter = locationParameter; 
            query();
            location = this.location;
        }

        
        if (location == null || PROVIDER_ERROR.equals(location.getProvider())) {
            checkLocationTable();
            if (readOfflineLocationCache == false) {
                locationTable.read(offlineLocationCache, LocationTable.PROVIDER_TIGERKNOWS, LocationTable.PROVIDER_NETWORK);
                readOfflineLocationCache = true;
            }
            location = queryCache(locationParameter, offlineLocationCache, false);
        }
        
        LogWrapper.i("LocationQuery", "getLocation() time:"+time+", queryTime:"+queryTime);
        
        return location;
        }
    }
    
    private void checkLocationTable() {
        if (locationTable == null) {
            locationTable = new LocationTable(context);
        } else if (locationTable.isOpen() == false) {
            locationTable.open();
        }
    }
    
    private Location queryCache(LocationParameter locationParameter, HashMap<LocationParameter, Location> cache, boolean mustEqualsWifi) {
        Location location = null;
        if (cache == null || cache.isEmpty()) {
            return location;
        }
        Iterator<Map.Entry<LocationParameter, Location>> iter = cache.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<LocationParameter, Location> entry = (Map.Entry<LocationParameter, Location>)iter.next();
            LocationParameter key = entry.getKey();
            Location value = entry.getValue();
            if (value != null && PROVIDER_ERROR.equals(value.getProvider()) == false) {
                if (locationParameter.equalsCellInfo(key)) {
                    if (location == null && mustEqualsWifi == false) {
                        location = value; 
                    }
                    float temp = locationParameter.equalsWifi(key);
                    if (temp >= WIFI_RATE && temp >= rate) {
                        if (value != null) {
                            rate = temp;
                            location = value;
                        }
                    }
                    if (temp >= 1f) {
                        break;
                    }
                }
            }
        }
        return location;
    }
    
    public void onCreate() {
    }
    
    public void onDestory() {
        if (locationTable != null && locationTable.isOpen()) {
            locationTable.optimize(LocationTable.PROVIDER_TIGERKNOWS, LocationTable.PROVIDER_NETWORK);
            locationTable.close();
        }
    }
    
    public static class LocationParameter {
        
        public int mnc = -1;
        public int mcc = -1;
        public TKCellLocation tkCellLocation = null;
        public List<TKNeighboringCellInfo> neighboringCellInfoList = new ArrayList<TKNeighboringCellInfo>();
        public List<TKScanResult> wifiList = new ArrayList<TKScanResult>();
        private volatile int hashCode = 0;
        public String time;
        
        public LocationParameter() {
            time = LocationUpload.SIMPLE_DATE_FORMAT.format(Calendar.getInstance().getTime());
        }
        
        public boolean equalsCellInfo(LocationParameter other) {
            if (other == null) {
                return false;
            }
            if (this.mnc == other.mnc 
                    && this.mcc == other.mcc 
                    && ((this.tkCellLocation != null && this.tkCellLocation.equals(other.tkCellLocation)) ||
                            (this.tkCellLocation == null && other.tkCellLocation == null))) {
                if (neighboringCellInfoList != null 
                        && other.neighboringCellInfoList != null 
                        && neighboringCellInfoList.size() == other.neighboringCellInfoList.size()) {
                    for(int i = neighboringCellInfoList.size()-1; i >= 0; i--) {
                        TKNeighboringCellInfo neighboringCellInfo = neighboringCellInfoList.get(i);
                        TKNeighboringCellInfo otherNeighboringCellInfo = other.neighboringCellInfoList.get(i);
                        if (neighboringCellInfo.lac != otherNeighboringCellInfo.lac
                                || neighboringCellInfo.cid != neighboringCellInfo.cid) {
                            return false;
                        }
                    }
                    return true;
                } else if (neighboringCellInfoList == null && other.neighboringCellInfoList == null) {
                    return true;
                }
            }
            return false;
        }
        
        public float equalsWifi(LocationParameter other) {
            if (other == null) {
                return 0;
            }
            if (wifiList != null && other.wifiList != null) {
                if (wifiList.size() == 0 && other.wifiList.size() == 0) {
                    return 1;
                }
                float rate = 0;
                int size = wifiList.size();
                if (size == 0) {
                    rate = 1;
                } else {
                    int match = matchWifi(wifiList, other.wifiList);
                    rate = ((float)match)/size;
//                    LogWrapper.d("LocationQuery", "match="+match);
//                    LogWrapper.d("LocationQuery", "size="+size);
//                    LogWrapper.d("LocationQuery", "scanResult    ="+wifiList);
//                    LogWrapper.d("LocationQuery", "other.wifiList="+other.wifiList);
                }
//                LogWrapper.d("LocationQuery", "rate="+rate);
                return rate;
            } else if (neighboringCellInfoList == null && other.neighboringCellInfoList == null) {
                return 1;
            }
            return 0;
        }
        
        private int matchWifi(List<TKScanResult> list1, List<TKScanResult> list2) {
            int match = 0;
            if (list1 == null || list2 == null) {
                return match;
            }
            int size = list1.size();
            int otherSize = list2.size();
            for(int i = size-1; i >= 0; i--) {
                String BSSID = list1.get(i).BSSID;
                for(int j = otherSize-1; j >= 0; j--) {
                    String otherBSSID = list2.get(j).BSSID;
                    if (BSSID.equals(otherBSSID)) {
                        match++;
                        break;
                    }
                }
            }
            return match;
        }
        
        public void reset() {
            mnc = -1;
            mcc = -1;
            tkCellLocation = null;
            neighboringCellInfoList = null;
            wifiList = null;
        }
        
        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (object instanceof LocationParameter) {
                LocationParameter other = (LocationParameter) object;
                if (mnc == other.mnc &&
                        mcc == other.mcc &&
                        ((tkCellLocation != null && tkCellLocation.equals(other.tkCellLocation)) || 
                                (tkCellLocation == null && other.tkCellLocation == null))) {
                    if (equalsCellInfo(other)) {
                        if (equalsWifi(other) == 1) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            if (hashCode == 0) {
                int result = 17;
                result = 37*result + mnc;
                result = 37*result + mcc;
                if (tkCellLocation != null) {
                    result = result + tkCellLocation.hashCode();
                }
                for(int i = neighboringCellInfoList.size()-1; i >= 0; i--) {
                    TKNeighboringCellInfo neighboringCellInfo = neighboringCellInfoList.get(i);
                    result = neighboringCellInfo.hashCode();
                }
                for(int i = wifiList.size()-1; i >= 0; i--) {
                    String bssid = wifiList.get(i).BSSID;
                    result = result + bssid.hashCode();
                }
                hashCode = result;
            }
            return hashCode;
        }
        
        public String getNeighboringCellInfoString() {
            StringBuilder s = new StringBuilder();
            for(int i = neighboringCellInfoList.size()-1; i >= 0; i--) {
                TKNeighboringCellInfo neighboringCellInfo = neighboringCellInfoList.get(i);
                s.append(neighboringCellInfo.lac);
                s.append(",");
                s.append(neighboringCellInfo.cid);
                if (i > 0) {
                    s.append(";");
                }
            }
            return s.toString();
        }
        
        public String getWifiString() {
            StringBuilder s = new StringBuilder();
            for(int i = wifiList.size()-1; i >= 0; i--) {
                String bssid = wifiList.get(i).BSSID;
                s.append(bssid);
                if (i > 0) {
                    s.append(",");
                }
            }
            return s.toString();
        }
        
        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append("mnc=");
            s.append(mnc);
            s.append(", mcc=");
            s.append(mcc);
            s.append(", tkCellLocation=[");
            s.append(tkCellLocation);
            s.append("], neighboringCellInfoList=[");
            s.append(getNeighboringCellInfoString());
            s.append("], wifiList=[");
            s.append(getWifiString());
            s.append("]");
            return s.toString();
        }
    }
    
    public static class TKCellLocation {
        public int phoneType = TelephonyManager.PHONE_TYPE_NONE;
        public int lac = -1;
        public int cid = -1;
        private volatile int hashCode = 0;
        
        public TKCellLocation(int phoneType, int lac, int cid) {
            this.phoneType = phoneType;
            this.lac = lac;
            this.cid = cid;
        }
        
        public TKCellLocation(String str) {
            if (str != null) {
                String[] arr = str.split(",");
                try {
                    phoneType = Integer.parseInt(arr[0]);
                    lac = Integer.parseInt(arr[1]);
                    cid = Integer.parseInt(arr[2]);
                } catch (Exception e) {
                    e.printStackTrace();
                    phoneType = TelephonyManager.PHONE_TYPE_NONE;
                    lac = -1;
                    cid = -1;
                }
            }
        }
        
        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (object instanceof TKCellLocation) {
                TKCellLocation other = (TKCellLocation) object;
                if (phoneType == other.phoneType
                        && lac == other.lac
                        && cid == other.cid) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            if (hashCode == 0) {
                int result = 17;
                result = 37*result + phoneType;
                result = 37*result + lac;
                result = 37*result + cid;
                hashCode = result;
            }
            return hashCode;
        }
        
        public String toString() {
            return phoneType+","+lac+","+cid;
        }
        
    }
    
    public static class TKNeighboringCellInfo {
        public int lac;
        public int cid;
        public int rssi;
        private volatile int hashCode = 0;
        
        public TKNeighboringCellInfo(int lac, int cid, int rssi) {
            this.lac = lac;
            this.cid = cid;
            this.rssi = rssi;
        }
        
        public TKNeighboringCellInfo(NeighboringCellInfo neighboringCellInfo) {
            this.lac = neighboringCellInfo.getLac();
            this.cid = neighboringCellInfo.getCid();
            this.rssi = neighboringCellInfo.getRssi();
        }
        
        public TKNeighboringCellInfo(String str) {
            if (str != null) {
                String[] arr = str.split(",");
                try {
                    lac = Integer.parseInt(arr[0]);
                    cid = Integer.parseInt(arr[1]);
                    rssi = Integer.parseInt(arr[2]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (object instanceof TKNeighboringCellInfo) {
                TKNeighboringCellInfo other = (TKNeighboringCellInfo) object;
                if (lac == other.lac
                        && cid == other.cid) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            if (hashCode == 0) {
                int result = 17;
                result = 37*result + lac;
                result = 37*result + cid;
                hashCode = result;
            }
            return hashCode;
        }
        
        public String toString() {
            return lac+","+cid+","+rssi;
        }
    }
    
    public static class TKScanResult {
        public String BSSID;
        public int level;
        private volatile int hashCode = 0;
        
        public TKScanResult(ScanResult scanResult) {
            this(scanResult.BSSID, scanResult.level);
        }
        
        public TKScanResult(String BSSID) {
            this(BSSID, 0);
        }
        
        public TKScanResult(String BSSID, int level) {
            this.BSSID = BSSID;
            this.level = level;
        }
        
        @Override
        public int hashCode() {
            if (hashCode == 0) {
                int result = BSSID.hashCode();
                hashCode = result;
            }
            return hashCode;
        }
        
        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (object instanceof TKScanResult) {
                TKScanResult other = (TKScanResult) object;
                if ((BSSID != null && BSSID.equals(other.BSSID)) ||
                        (BSSID == null && other.BSSID == null)) {
                    return true;
                }
            }
            return false;
        }
        
        public String toString() {
            return BSSID+","+level;
        }
        
        public static TKScanResult parse(String str) {
            TKScanResult tkScanResult = null;
            if (str != null) {
                String[] arr = str.split(",");
                tkScanResult = new TKScanResult(arr[0]);
                try {
                    tkScanResult.level = Integer.parseInt(arr[1]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return tkScanResult;
        }
    }
}
