
package com.tigerknows.model;

import java.io.IOException;
import java.text.SimpleDateFormat;
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
import com.tigerknows.android.location.TKLocationManager;
import com.tigerknows.model.response.Appendix;
import com.tigerknows.model.response.PositionCake;
import com.tigerknows.model.response.ResponseCode;
import com.tigerknows.provider.LocationTable;
import com.tigerknows.service.TigerknowsLocationManager;
import com.tigerknows.util.Utility;
import com.tigerknows.util.ParserUtil;

import android.content.Context;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;

/**
 * 定位服务类，实现定位查询、缓存定位、重用定位、历史定位功能、避免重复的定位查询
 * 定位的缓存机制如下：
 * 首先遍历内存的缓存定位列表，如果有匹配的定位就返回，否则连接定位服务器进行查询，
 * 如果能查询到结果就返回，否则遍历史的缓存定位列表，
 * 如果有匹配就返回，否则返回空
 * @author pengwenyue
 *
 */
public class LocationQuery extends BaseQuery {
    
    static final String TAG = "LocationQuery";
    
    /**
     * 错误的定位数据来源
     */
    public static final String PROVIDER_ERROR = "tkerror";
    
    /**
     * 历史的定位数据来源（存储在数据库中）
     */
    public static final String PROVIDER_HISTORY = "tkhistory";
    
    /**
     * WIFI接入点列表的最低匹配率，大于此值才有效
     */
    static final float WIFI_MATCH_RATE_MIN = 0.8f;
    
    /**
     * 访问定位服务的版本 
     */
    static final String VERSION = "4";
    
    /**
     * 定位成功的状态响应码（注意此值不是网络状态响应）
     */
    static final int LOCATION_RESPONSE_CODE_SUCCEED = 200;

    /**
     * 定位失败的状态响应码，并且表示不用重试（注意此值不是网络状态响应）
     */
    static final int LOCATION_RESPONSE_CODE_FAILED = 404;
    
    public static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss");

    /**
     * 单实例模式
     */
    private static LocationQuery sInstance;

    /**
     * 单实例模式
     */
    public static LocationQuery getInstance(Context context) {
        if (null == sInstance) {
            sInstance = new LocationQuery(context);
        }
        return sInstance;
    }
    
    /**
     * 定位服务返回的定位信息
     */
    private Location location;

    /**
     * 查询定位服务提交的基站、WIFI、邻近基站信息
     */
    private LocationParameter locationParameter = null;
    
    /**
     * 定位服务返回的状态响应码（注意此值不是网络状态响应）
     */
    private int locationResponseCode;

    private WifiManager wifiManager;
    
    /**
     * 内存的定位信息缓存列表
     */
    private HashMap<LocationParameter, Location> onlineLocationCache = new HashMap<LocationParameter, Location>();
    
    /**
     * 历史的定位信息缓存列表
     */
    private HashMap<LocationParameter, Location> offlineLocationCache = new HashMap<LocationParameter, Location>();
    
    private LocationTable locationTable = null;

    /**
     * 清除所有缓存的定位信息
     */
    public void clearCache() {
        onlineLocationCache.clear();
        offlineLocationCache.clear();
    }

    private LocationQuery(Context context) {
        super(context, API_TYPE_LOCATION_QUERY, VERSION);
        this.wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public void query() {
        synchronized (this) {
            location = null;
            locationResponseCode = 0;
            super.query();
            if (locationResponseCode == LOCATION_RESPONSE_CODE_FAILED || locationResponseCode == LOCATION_RESPONSE_CODE_SUCCEED) {
                LogWrapper.i("LocationQuery", "query():location="+location);
                
                if (locationParameter == null) {
                    locationParameter = makeLocationParameter();
                }
                
                if (location == null) {
                    location = new Location(PROVIDER_ERROR);
                } else {
                    checkLocationTable();
                    locationTable.write(locationParameter, location);
                    offlineLocationCache.put(locationParameter, location);
                }
                
                onlineLocationCache.put(locationParameter, location);
            }
        }
    }

    @Override
    protected void makeRequestParameters() throws APIException {
        super.makeRequestParameters();
        addCommonParameters(requestParameters, Globals.getCurrentCityInfo(false).getId(), true);
        
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
                if (Utility.lacCidValid(lac, cid)) {
                    requestParameters.add("n8b_lac[]", String.valueOf(lac));
                    requestParameters.add("n8b_ci[]", String.valueOf(cid));
                    requestParameters.add("n8b_ss[]", String.valueOf(Utility.asu2dbm(cellInfo.getRssi())));
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
        String url = String.format(TKConfig.getLocationUrl(), TKConfig.getLocationHost());
        httpClient.setURL(url);
    }

    @Override
    protected void translateResponseV12(ParserUtil util) throws IOException {
        super.translateResponseV12(util);
        try {
            for (int i = appendice.size()-1; i >= 0; i--) {
                Appendix appendix = appendice.get(i);
                if (appendix.type == Appendix.TYPE_RESPONSE_CODE && appendix instanceof ResponseCode) {
                    locationResponseCode = ((ResponseCode)appendix).getResponseCode();
                    break;
                }
            }

            if (locationResponseCode == LOCATION_RESPONSE_CODE_SUCCEED) {
                for (int i = appendice.size()-1; i >= 0; i--) {
                    Appendix appendix = appendice.get(i);
                    if (appendix.type == Appendix.TYPE_POSITION && appendix instanceof PositionCake) {
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
    
    /**
     * 生成定位的绑定参数信息
     * @return
     */
    LocationParameter makeLocationParameter() {
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
        return locationParameter;
    }
    
    /**
     * 获取当前定位信息
     * @return
     */
    public Location getLocation() {
        if (TKLocationManager.UnallowedLocation) {
            return null;
        }
        synchronized (this) {
            LocationParameter locationParameter = makeLocationParameter();
            
            if (Utility.mccMncLacCidValid(locationParameter.mcc,
                    locationParameter.mnc,
                    locationParameter.tkCellLocation.lac,
                    locationParameter.tkCellLocation.cid) == false &&
                    (locationParameter.wifiList == null || locationParameter.wifiList.size() == 0)) {
                return null;
            }
    
            Location location = null;
            location = queryCache(locationParameter, onlineLocationCache, true);
            
            time++;
            if (location == null) {
                queryTime++;
                this.locationParameter = locationParameter; 
                query();
                location = this.location;
            }
    
            
            if (location == null || PROVIDER_ERROR.equals(location.getProvider())) {
                checkLocationTable();
                if (offlineLocationCache.isEmpty()) {
                    locationTable.read(offlineLocationCache, LocationTable.Provider_List_Cache);
                }
                location = queryCache(locationParameter, offlineLocationCache, false);
            }
            
            LogWrapper.i("LocationQuery", "getLocation() time:"+time+", queryTime:"+queryTime);
            
            return location;
        }
    }
    
    /**
     * 检查locationTable是否初始化，否则初始化locationTable
     */
    private void checkLocationTable() {
        if (locationTable == null) {
            locationTable = new LocationTable(context);
        } else if (locationTable.isOpen() == false) {
            locationTable.open();
        }
    }
    
    /**
     * 遍历缓存定位列表，找出匹配率最高的定位信息
     * @param locationParameter
     * @param cache
     * @param needWholeMatch
     * @return
     */
    private Location queryCache(LocationParameter locationParameter, HashMap<LocationParameter, Location> cache, boolean needWholeMatch) {
        Location location = null;
        if (cache == null || cache.isEmpty()) {
            return location;
        }
        boolean mccMncLacCidValid = Utility.mccMncLacCidValid(locationParameter.mcc, locationParameter.mnc, locationParameter.tkCellLocation.lac, locationParameter.tkCellLocation.cid);
        float cellInfoRate = 0f;
        float wifiMatchRate = 0f;
        float totalRate = 0f;
        Iterator<Map.Entry<LocationParameter, Location>> iter = cache.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<LocationParameter, Location> entry = (Map.Entry<LocationParameter, Location>)iter.next();
            LocationParameter key = entry.getKey();
            Location value = entry.getValue();
            if (value != null && PROVIDER_ERROR.equals(value.getProvider()) == false) {
                cellInfoRate = 0f;
                wifiMatchRate = 0f;
                if (mccMncLacCidValid) {
                    if (locationParameter.equalsCellInfo(key)) {
                        cellInfoRate = 1f;
                    } else {
                        cellInfoRate = -1f;
                    }
                }
                wifiMatchRate = locationParameter.calculateWifiMatchRate(key);
                if (wifiMatchRate < WIFI_MATCH_RATE_MIN) {
                    wifiMatchRate = 0;
                }
                
                float rate = (cellInfoRate+wifiMatchRate);
                if (rate > 0 && rate > totalRate) {
                    totalRate = rate;
                    location = value;
                }
            }
        }
        
        LogWrapper.d(TAG, "totalRate="+totalRate);
        if (needWholeMatch) {
            if (mccMncLacCidValid) {
                if ((locationParameter.wifiList == null || locationParameter.wifiList.size() == 0)) {
                    if (totalRate >= 1.0) {
                        return location;
                    }
                } else if (totalRate >= 2.0) {
                    return location;
                }
            } else {
                if ((locationParameter.wifiList == null || locationParameter.wifiList.size() == 0)) {
                    return null;
                } else if (totalRate >= 1.0) {
                    return location;
                }
            }
        } else {
            if (totalRate > 0) {
                return location;
            }
        }
        return null;
    }
    
    /**
     * 初始化
     */
    public void onCreate() {
    }
    
    /**
     * 释放资源
     */
    public void onDestory() {
        if (locationTable != null && locationTable.isOpen()) {
            locationTable.optimize(LocationTable.Provider_List_Cache);
            locationTable.close();
        }
    }
    
    /**
     * 定位的绑定参数信息，包括MNC、MCC、基站、WIFI接入点列表、邻近基站列表、时间戳信息
     * @author pengwenyue
     *
     */
    public static class LocationParameter {
        
        public int mnc = -1;
        public int mcc = -1;
        
        /**
         * 基站信息
         */
        public TKCellLocation tkCellLocation = null;
        
        /**
         * 邻近基站列表信息
         */
        public List<TKNeighboringCellInfo> neighboringCellInfoList = new ArrayList<TKNeighboringCellInfo>();
        
        /**
         * WIFI接入点列表信息
         */
        public List<TKScanResult> wifiList = new ArrayList<TKScanResult>();
        
        /**
         * 时间戳
         */
        public String time;
        
        private volatile int hashCode = 0;
        
        public LocationParameter() {
            time = SIMPLE_DATE_FORMAT.format(Calendar.getInstance().getTime());
        }
        
        /**
         * 比较基站及邻近基站列表信息是否相同
         * @param other
         * @return
         */
        public boolean equalsCellInfo(LocationParameter other) {
            if (other == null) {
                return false;
            }
            // 比较基站
            if (this.mnc == other.mnc 
                    && this.mcc == other.mcc 
                    && ((this.tkCellLocation != null && this.tkCellLocation.equals(other.tkCellLocation)) ||
                            (this.tkCellLocation == null && other.tkCellLocation == null))) {
                
                // 比较邻近基站列表
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
        
        /**
         * 计算WIFI接入点列表的匹配率
         * @param other
         * @return
         */
        public float calculateWifiMatchRate(LocationParameter other) {
            if (other == null) {
                return 0;
            }
            if (wifiList != null && other.wifiList != null) {
                int size = wifiList.size();
                if (size == 0 || other.wifiList.size() == 0) {
                    return 0;
                }
                float rate = 0;
                int match = matchWifi(wifiList, other.wifiList);
                rate = ((float)match)/size;
                return rate;
            }
            return 0;
        }
        
        /**
         * 计算两个WIFI接入点列表存在相同WIFI接入点的总数
         * @param list1
         * @param list2
         * @return
         */
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
                        if (calculateWifiMatchRate(other) == 1) {
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
                s.append(neighboringCellInfo.toString());
                if (i > 0) {
                    s.append(";");
                }
            }
            return s.toString();
        }
        
        public String getWifiString() {
            StringBuilder s = new StringBuilder();
            for(int i = wifiList.size()-1; i >= 0; i--) {
                s.append(wifiList.get(i).toString());
                if (i > 0) {
                    s.append(";");
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
    
    /**
     * 基站信息类
     * @author pengwenyue
     *
     */
    public static class TKCellLocation {
        public int phoneType = TelephonyManager.PHONE_TYPE_NONE;
        public int lac = -1;
        public int cid = -1;
        public int signalStrength = Integer.MAX_VALUE;
        private volatile int hashCode = 0;
        
        public TKCellLocation(int phoneType, int lac, int cid, int signalStrength) {
            this.phoneType = phoneType;
            this.lac = lac;
            this.cid = cid;
            this.signalStrength = signalStrength;
        }
        
        public TKCellLocation(String str) {
            if (str != null) {
                String[] arr = str.split(",");
                try {
                    phoneType = Integer.parseInt(arr[0]);
                    lac = Integer.parseInt(arr[1]);
                    cid = Integer.parseInt(arr[2]);
                    signalStrength = Integer.parseInt(arr[3]);
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
            return phoneType+","+lac+","+cid+","+signalStrength;
        }
        
    }
    
    /**
     * 邻近基站信息类
     * @author pengwenyue
     *
     */
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
    
    /**
     * WIFI接点入信息类
     * @author pengwenyue
     *
     */
    public static class TKScanResult {
        public String BSSID;
        public int level = Integer.MAX_VALUE;
        private volatile int hashCode = 0;
        
        public TKScanResult(ScanResult scanResult) {
            this(scanResult.BSSID, scanResult.level);
        }
        
        public TKScanResult(String str) {
            String[] arr = str.split(",");
            try {
                BSSID = arr[0];
                level = Integer.parseInt(arr[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
    }
}
