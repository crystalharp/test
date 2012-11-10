
package com.tigerknows.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.model.response.Appendix;
import com.tigerknows.model.response.PositionCake;
import com.tigerknows.model.response.ResponseCode;
import com.tigerknows.service.TigerknowsLocationManager;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.ParserUtil;

import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.telephony.NeighboringCellInfo;

public class LocationQuery extends BaseQuery {
    
    protected static final String VERSION = "4";
    
    private static final String EMPTY_STRING = "";
    
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
    
    private StringBuilder keyStr = new StringBuilder();
    
    private String lastMccMncLacCid = EMPTY_STRING;
    
    private List<String> keyList = new ArrayList<String>();
    
    private Comparator<NeighboringCellInfo> cellInfoLacCidComparator = new Comparator<NeighboringCellInfo>() {

        @Override
        public int compare(NeighboringCellInfo neighboringCellInfo1, NeighboringCellInfo neighboringCellInfo2) {
            int result = neighboringCellInfo1.getLac() - neighboringCellInfo2.getLac();
            if (result != 0) {
                return result;
            } 
            result = neighboringCellInfo1.getCid() - neighboringCellInfo2.getCid();
            return result;
        };
    };
    
    private Comparator<ScanResult> scanResultBSSIDComparator = new Comparator<ScanResult>() {

        @Override
        public int compare(ScanResult scanResult1, ScanResult scanResult2) {
            return scanResult1.BSSID.compareTo(scanResult2.BSSID);
        };
    };
    
    private int mnc = -1;
    
    private int mcc = -1;
    
    private int lac = -1;
    
    private int cid = -1;
    
    private HashMap<String, Location> locationCache = new HashMap<String, Location>();
    
    public HashMap<String, Location> getLocationCache() {
        return locationCache;
    }

    public int getResponseCode() {
        return locationResponseCode;
    }

    private LocationQuery(Context context) {
        super(context, API_TYPE_LOCATION_QUERY, VERSION);
        this.wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    }

    public void onCreate() {
        lastMccMncLacCid = EMPTY_STRING;
        keyList.clear();
        locationCache.clear();
    }

    @Override
    public void query() {
        location = null;
        locationResponseCode = LOCATION_RESPONSE_CODE_NONE;
        super.query();
        if (locationResponseCode == LOCATION_RESPONSE_CODE_FAILED || locationResponseCode == LOCATION_RESPONSE_CODE_SUCCEED) {
            LogWrapper.i("LocationQuery", "query():location="+location);

            if (location == null) {
                location = new Location("error");
            }
            
            locationCache.put(keyStr.toString(), location);
        }
    }

    @Override
    protected void makeRequestParameters() throws APIException {
        super.makeRequestParameters();
        addCommonParameters(requestParameters, MapEngine.CITY_ID_INVALID, true);
        
        if (wifiManager != null) {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (scanResults != null) {
                for (ScanResult sr : scanResults) {
                    requestParameters.add(new BasicNameValuePair("wifi_mac[]", sr.BSSID));
                    requestParameters.add(new BasicNameValuePair("wifi_ss[]", String.valueOf(sr.level)));
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
                    requestParameters.add(new BasicNameValuePair("n8b_lac[]", String.valueOf(lac)));
                    requestParameters.add(new BasicNameValuePair("n8b_ci[]", String.valueOf(cid)));
                    requestParameters.add(new BasicNameValuePair("n8b_ss[]", String.valueOf(CommonUtils.asu2dbm(cellInfo.getRssi()))));
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
//                parameters.add(new BasicNameValuePair("n8b_lac[]", String.valueOf(String.valueOf(cid >>> 16))));
//                parameters.add(new BasicNameValuePair("n8b_ci[]", String.valueOf(cid & 0xffff)));
//                parameters.add(new BasicNameValuePair("n8b_ss[]", String.valueOf(neighboringCellInfo.getRssi())));
//            }
//        }
        requestParameters.add(new BasicNameValuePair("radio_type", TKConfig.getRadioType()));
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
    public Location getLocation() {
        synchronized (keyList) {
        mcc = TKConfig.getMCC();
        mnc = TKConfig.getMNC();
        
        int[] cellLocation = TKConfig.getCellLocation();
        lac = cellLocation[0];
        cid = cellLocation[1];
        
        keyStr.delete(0, keyStr.length());
        
        String mccMncLacCid = EMPTY_STRING;
        if (CommonUtils.mccMncLacCidValid(mcc, mnc, lac, cid)) {
            mccMncLacCid = String.format("%d.%d.%d.%d", mcc, mnc, lac, cid);
            keyStr.append(mccMncLacCid);
        }
        keyStr.append(';');

        keyList.clear();
        List<NeighboringCellInfo> neighboringCellInfo = TKConfig.getNeighboringCellList();
        if (neighboringCellInfo != null) {
            int lac,cid;
            Collections.sort(neighboringCellInfo, cellInfoLacCidComparator);
            for (NeighboringCellInfo cellInfo : neighboringCellInfo) {
                lac = cellInfo.getLac();
                cid = cellInfo.getCid();
                if (CommonUtils.lacCidValid(lac, cid)) {
                    keyStr.append(lac);
                    keyStr.append('.');
                    keyStr.append(cid);
                    keyStr.append(';');
                    keyList.add(lac + "." + cid);
                }
            }
        }
        
        if (wifiManager != null) {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (scanResults != null) {

                Collections.sort(scanResults, scanResultBSSIDComparator);
                for (ScanResult scanResult : scanResults) {
                    keyStr.append(scanResult.BSSID);
                    keyStr.append(';');
                    keyList.add(scanResult.BSSID);
                }
            }
        }

        LogWrapper.i("LocationQuery", "getLocation() keyStr:"+keyStr.toString());
        Location location = locationCache.get(keyStr.toString());
        
        if (location == null &&
                lastMccMncLacCid.equals(mccMncLacCid) &&
                keyList.size() > 0) {
            float maxRoate = 0.8f;
            Iterator<Map.Entry<String, Location>> iter = locationCache.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, Location> entry = (Map.Entry<String, Location>)iter.next();
                String key = entry.getKey();
                int exist = 0;
                float rate; 
                for(String str : keyList) {
                    if (key.indexOf(";"+str+";") > -1) {
                        exist++;
                    }
                }
                if (exist > 0) {
                    rate = (float)exist/keyList.size();
                    LogWrapper.i("LocationQuery", "getLocation() rate:"+rate);
                    if (rate > maxRoate) {
                        maxRoate = rate;
                        location = entry.getValue();
                    }
                }
            }
        }
        
        lastMccMncLacCid = mccMncLacCid;
        
        time++;
        if (location == null) {
            queryTime++;
            query();
            location = this.location;
        }
        
        LogWrapper.i("LocationQuery", "getLocation() time:"+time+", queryTime:"+queryTime);
        
        return location;
            
        }
    }
}
