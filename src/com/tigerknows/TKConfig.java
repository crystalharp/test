/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.decarta.CONFIG;
import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.LocationQuery.TKCellLocation;
import com.tigerknows.util.CommonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;

/**
 * @author Peng Wenyue
 */
public class TKConfig {
    
    private static final String TAG = "TKConfig";
    /**
     * server default configuration. This part store the default configuraiton when you want to
     * switch back in case configuration has been changed.
     */
    //api level configuration
    public static final boolean DRAW_BY_OPENGL_default=true;
    public static final boolean SNAP_TO_CLOSEST_ZOOMLEVEL_default=true;
    public static final int FADING_TIME_default=100;
    public static final int BORDER_default=0;
    public static final int TILE_THREAD_COUNT_default=5;
    
    public static boolean CACHE_BITMAP_TO_MEMORY = true;
    public static int CACHE_SIZE = 32;
    
    
    //application level configuration
    
    public static String LOCALE="US_EN";
    public static Position DEFAULT_POSITION=new Position(39.904156,116.397764);
    public static boolean ONLY_SHOW_ADDRESS_IN_GEOCODE_RESULT=false;

    public static final int ZOOM_LEVEL_DEFAULT = 5; // 200km
    public static final int ZOOM_LEVEL_CITY = 11; // 2km
    public static final int ZOOM_LEVEL_LOCATION = 14; // 200m
    public static final int ZOOM_LEVEL_POI = 15;
    
    //腾讯首发标签ANDTencent
    public static final String sSPREADER_TENCENT = "ANDqQFUnusual-SF";
    //魅族、联想、OPPO、MOTO不加切客链接
    public static final String[] sSPREADER_NOQIEKE = new String[] {"ANDhuawei", "ANDMotosj", "ANDmz", "ANDLianxiang", "ANDOpposj", "ANDSuoAi", "ANDwoshangdian"};
    
    public static final int NO_STORAGE_ERROR = -1;
    public static final int CANNOT_STAT_ERROR = -2;
    
    private static String sUSER_AGENT = "tigermap/1.50";
    private static int sHTTP_SOCKET_TIMEOUT = 60*1000;            // default to 1 min

    private static final String sMAP_DATA_FOLDER_NAME = "map"; 
    private static final String sRESOURCES_DATA_FOLDER_NAME = "res"; 
    private static final String sDATA_FILE_SUFFIXAL = ".dat"; 

    public static final String FILTER_FILE = "filter_%s_%d"; 
    public static final String SUGGEST_LEXCION_FILE_SW_INDEX = "sw2_%d_index"; 
    public static final String SUGGEST_LEXCION_FILE_SW_S = "sw2_%d_s"; 
    public static final String SUGGEST_LEXCION_FILE_SW_L = "sw2_%d_l"; 
    public static final String SUGGEST_LEXCION_FILE_DIFF_INDEX = "sw2_%d_diff_index"; 
    public static final String SUGGEST_LEXCION_FILE_DIFF_S = "sw2_%d_diff_s"; 
    public static final String SUGGEST_LEXCION_FILE_DIFF_L = "sw2_%d_diff_l"; 
    public static final String SUGGEST_LEXCION_FILE_TEMP = "%d"; //sw_cityId 
    public static final String SUGGEST_LEXCION_FILE = "sw2_%d.slf"; 
    
    public static final String MAP_REGION_METAFILE = "region%d.meta";
    
    public static boolean sLoadBalance = true;
    public static String sDOWNLOAD_MAP_URL = "http://%s/quantum/string";
    public static String sDOWNLOAD_SUGGEST_URL = "http://%s/suggest_lexicon";
    public static String sQUERY_URL = "http://%s/cormorant/local";
    public static String sLOCATION_URL = "http://%s/tk_locate_me";
    public static String sACCOUNT_MANAGE_URL = "http://%s/melon/user"; // http://192.168.11.174:8100/melon/user
    public static String sSMS_URL = "http://%s/sms";
    public static String LOGIN_URL = "http://%s/bootstrap/local";
    
    // 返回结果中字符串字段的编码，参数值必须是java.lang.String.getBytes(String charset)支持的编码，否则整个查询结果为空；如果不提供该参数，默认编码为UTF-16LE 
    private static String sENCODING = System.getProperty("file.encoding");
    
    // mobile country code，国家码，在大陆地区为460
    private static int sMCC = -1;
    // mobile network code，移动网络识别码，用于识别不同运营商的网络，位长2位。中国移动GSM网为00，中国联通的GSM网为01，中国联通CDMA网为03。  
    private static int sMNC = -1;
    
    // 客户端软件版本
    private static String sCLIENT_SOFT_VERSION = "4.00.20121002A";
    // 客户端软件发布日期
    public static final String CLIENT_SOFT_VERSION = "4.00.alpha1";
    public static final String CLIENT_SOFT_RELEASE_DATE = "2012-10-02";
    // 地图数据版本
    private static String sMAP_DATA_VERSION = "2.50";
    // 联想词版本
    private static String sSUGGEST_WORD_VERSION = "1";
    // 客户端数据版本
    private static String sCLIENT_DATA_VERSION = "1.0.20100619";
    
    // 授权厂商手机产品ID，包括厂商品牌、型号、软件版本等信息，相同品牌不同型号手机不同??
    private static String sPHONE_KEY = "ANDTest"; 
    public static String sSPREADER = "TigerMapTest";
    public static String sCommentSource = "test";
    
    // IMSI(International Mobile Subscriber Identity)，国际移动用户标识号，是TD系统分给用户的唯一标识号，它存储在SIM卡、HLR/VLR中，最多由15个数字组成 
    private static String sIMSI = "0";
    // IMEI(International Mobile Equipment Identity)，国际移动设备身份码
    private static String sIMEI = "0";
    // 手机信号强度值
    private static int sSignalStrength = 0;

    /*
    si    SP收费相关参数，Android版本取固定值即可：si=5$5$5$5$5
    sg    SP收费相关参数，Android版本取固定值即可：sg=25
    sv    SP收费相关参数，Android版本取固定值即可：sv=1
    sc    短信中心号，比如北京移动为13800100500，借此可分析用户SIM卡所在省市
    */
    public static final String SI = "5$5$5$5$5";
    public static final String SG = "25";
    public static final String SV = "1";
    public static final String SERVICE_CENTER = "13800100500";
    
    // 客户端的OS版本，android版可获得手机系统版本（SDK版本号）
    private static final String sVERSION_OF_PLATFORM = String.valueOf(Build.VERSION.SDK_INT);
    
    private static String sUser_Action_Track = "on";

    private static String sDEFAULT_DOWNLOAD_HOST = "download.tigerknows.net";
    private static String sDEFAULT_QUERY_HOST = "search.tigerknows.net"; // 192.168.11.200:8080 local.tigerknows.net  good2012.gicp.net 192.168.11.174:8080 search.tigerknows.net 192.168.11.31:8080 211.151.98.76:8080
    private static String sDEFAULT_LOCATION_HOST = "position.tigerknows.net";
    private static String sDEFAULT_ACCOUNT_MANAGE_HOST = "user.tigerknows.net"; //192.168.11.174:8100 211.151.97.117:8080 user.tigerknows.net
    public static String[] LOGIN_HOST = new String[]{"init.tigerknows.net", "chshh.tigerknows.com", "csh.laohubaodian.net"};

    public static String sDYNAMIC_DOWNLOAD_HOST;
    public static String sDYNAMIC_QUERY_HOST;
    public static String sDYNAMIC_LOCATION_HOST;
    public static String sDYNAMIC_ACCOUNT_MANAGE_HOST;

    private static TelephonyManager sTelephonyManager;
    
    private static int sPage_Size = 20;
    
    public static int sMap_Padding = 40;
    
    public static int LON_LAT_DIVISOR = 100000;
    
    public static final String TIGERKNOWS_PREFS = "tigerknows_prefs";
    public static final String PREFS_USER_ACTION_TRACK = "prefs_user_action_track";
    public static final String PREFS_LAST_LON = "prefs_last_lon";
    public static final String PREFS_LAST_LAT = "prefs_last_lat";
    public static final String PREFS_LAST_ZOOM_LEVEL = "prefs_last_zoom_level";

    public static final String PREFS_SETTING_LOCATION = "setting_location";
    public static final String PREFS_VERSION_NAME = "prefs_version_name";
    public static final String PREFS_HAS_SHORT_CUT_PREFS = "prefs_has_short_cut";
    public static final String PREFS_UPGRADE = "prefs_learn_4.00";    
    public static final String PREFS_FIRST_USE = "prefs_learn_3.10";    
    public static final String PREFS_SHOW_LOCATION_SETTINGS_TIP = "show_location_settings_tip";    
    public static final String PREFS_SHOW_UPGRADE_MAP_TIP = "show_upgrade_map_tip";    
    public static final String PREFS_SHOW_UPGRADE_COMMENT_TIP = "show_upgrade_comment_tip";    

    public static final String PREFS_WEIBO_TOKEN = "weibo_token";
    public static final String PREFS_WEIBO_SECRET = "weibo_secret";
    public static final String PREFS_WEIBO_SCREEN_NAME = "weibo_screen_name";
    public static final String PREFS_WEIBO_EXPIRE_IN = "weibo_expire_deadline";
    
    public static final String PREFS_PHONENUM = "prefs_phonenum";
    
    public static final String PREFS_ERROR_RECOVERY = "error_recovery";
    public static final String PREFS_MAP_DOWNLOAD_CITYS = "map_download_citys_v2";
    public static final String PREFS_ACQUIRE_WAKELOCK = "acquire_wakelock";
    public static final String PREFS_CLIENT_UID = "prefs_client_uid";
    
    public static final String PREFS_HISTORY_WORD_POI = "history_word_poi_%d";
    public static final String PREFS_HISTORY_WORD_TRAFFIC = "history_word_traffic_%d";
    public static final String PREFS_HISTORY_WORD_BUSLINE = "history_word_busline_%d";
    
    public static final String PREFS_LAST_REGION_ID_LIST = "last_region_id_list";
    
    public static final String PREFS_HINT_POI_DETAIL = "prefs_hint_poi_detail";
    public static final String PREFS_HINT_DISCOVER_HOME = "prefs_discover";
    public static final String PREFS_HINT_POI_LIST = "prefs_hint_poi_list";
    public static final String PREFS_HINT_DISCOVER_TUANGOU_LIST = "prefs_hint_discover_tuangou_list";
    public static final String PREFS_HINT_DISCOVER_TUANGOU_DINGDAN = "prefs_hint_discover_tuangou_dingdan";
    public static final String PREFS_HINT_LOCATION = "prefs_hint_location";

    public static final int PICTURE_DISCOVER_HOME = 1;
    public static final int PICTURE_TUANGOU_LIST = 2;
    public static final int PICTURE_DIANYING_LIST = 3;
    public static final int PICTURE_TUANGOU_DETAIL = 4;
    public static final int PICTURE_TUANGOU_TAOCAN = 5;
    public static final int PICTURE_DIANYING_DETAIL = 6;

    public static final int COLOR_BLACK_DARK = 0xff323232;
    public static final int COLOR_BLACK_LIGHT = 0xff969696;
    public static final int COLOR_ORANGE = 0xffff6c00;
    
    public static int getPageSize() {
        return sPage_Size;
    }

    public static int getSignalStrength() {
        return sSignalStrength;
    }

    public static void init(Context context) {
        LogWrapper.i(TAG, "init()");

        String clientUID = TKConfig.getPref(context, TKConfig.PREFS_CLIENT_UID);
        if (TextUtils.isEmpty(clientUID)) {
            clientUID = UUID.randomUUID().toString();
            TKConfig.setPref(context, TKConfig.PREFS_CLIENT_UID, clientUID);
        }
        Globals.g_ClientUID = clientUID;
        
        sPHONE_KEY = (Build.MODEL + "|" + Build.DISPLAY + "|" + Build.PRODUCT).replace(",", "").replace("=", "");
        sTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        sSignalStrength = 0;
        sTelephonyManager.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_SIGNAL_STRENGTH);
        sIMEI = sTelephonyManager.getDeviceId();        
        sIMSI = sTelephonyManager.getSubscriberId();

        sUser_Action_Track = getPref(context, PREFS_USER_ACTION_TRACK, "on");
        
        // 手机上没有插入SIM卡，则不能读取到IMSI
        sMCC = -1;
        sMNC = -1;
        /** 获取SIM卡的IMSI码   
         * SIM卡唯一标识：IMSI 国际移动用户识别码（IMSI：International Mobile Subscriber Identification Number）是区别移动用户的标志，   
         * 储存在SIM卡中，可用于区别移动用户的有效信息。IMSI由MCC、MNC、MSIN组成，其中MCC为移动国家号码，由3位数字组成，   
         * 唯一地识别移动客户所属的国家，我国为460；MNC为网络id，由2位数字组成，   
         * 用于识别移动客户所归属的移动网络，中国移动为00，中国联通为01,中国电信为03；MSIN为移动客户识别码，采用等长11位数字构成。   
         * 唯一地识别国内GSM移动通信网中移动客户。所以要区分是移动还是联通，只需取得SIM卡中的MNC字段即可   
        */   
        if (!TextUtils.isEmpty(sIMSI)){
            try {
                sMCC = Integer.parseInt(sIMSI.substring(0, 3));
                sMNC = Integer.parseInt(sIMSI.substring(3, 5));
            } catch (Exception e) {
                LogWrapper.e(TAG, "init() Can't get MCC and MNC from SIM card");
            }
        }
        if (sMCC == -1 || sMNC == -1) {
            String networkOperator = sTelephonyManager.getNetworkOperator();
            if (!TextUtils.isEmpty(networkOperator)) {
                try {
                    sMCC = Integer.parseInt(networkOperator.substring(0, 3));
                    sMNC = Integer.parseInt(networkOperator.substring(3, 5));
                } catch (Exception e) {
                    LogWrapper.e(TAG, "init() Can't get MCC and MNC from SIM card");
                }
            }
        }
        
        if (sIMEI == null) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String macAddress = wifiInfo == null ? sSPREADER : wifiInfo.getMacAddress();
                sIMEI = macAddress;
            } else {
                sIMEI = "null";
            }
        }
        
        if (sIMSI == null) {
            sIMSI = "@wifi";
        } else {
            ConnectivityManager conMan = (ConnectivityManager)context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            // wifi
            State wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();

            if (wifi == State.CONNECTED || wifi == State.CONNECTING) {
                // wifi
                sIMSI += "@wifi";
            }

        }

        String versionNum = getPref(context, PREFS_VERSION_NAME, "");
        if (!sCLIENT_SOFT_VERSION.equals(versionNum)) {

            int versionCode = 0;
            try {
                versionCode = Integer.parseInt(versionNum.substring(0, 4).replace(".", ""));
            } catch (Exception e) {
            }

            if (versionCode < 300) {
                removePref(context, PREFS_ACQUIRE_WAKELOCK);
                removePref(context, PREFS_HAS_SHORT_CUT_PREFS);
                removePref(context, PREFS_LAST_ZOOM_LEVEL);
                removePref(context, PREFS_USER_ACTION_TRACK);
                SharedPreferences sharedPreferences = context.getSharedPreferences(TIGERKNOWS_PREFS, Context.MODE_WORLD_READABLE);
                boolean hasShort = sharedPreferences.getBoolean(PREFS_HAS_SHORT_CUT_PREFS, false);
                if (hasShort) {
                    setPref(context, PREFS_HAS_SHORT_CUT_PREFS, "1");
                }
            }
        }
        boolean notShort = TextUtils.isEmpty(getPref(context, PREFS_HAS_SHORT_CUT_PREFS));
        setPref(context, PREFS_HAS_SHORT_CUT_PREFS, "1");
        if (notShort) {
            createShortCut(context);
        } 
        try {
            MapEngine.getInstance().setup(context);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        BaseQuery.initCommonParameters();
    }
    
    private static void createShortCut(Context context) {
        Intent shortcutIntent=new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        String appName = context.getString(R.string.app_name);
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, appName);//快捷方式的标题
        Parcelable icon=Intent.ShortcutIconResource.fromContext(context, R.drawable.icon); //获取快捷键的图标
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);//快捷方式的图标
        shortcutIntent.putExtra("duplicate", false);
        
        Intent sphnixIntent = new Intent(Intent.ACTION_MAIN);
        sphnixIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        sphnixIntent.setComponent(new ComponentName(context.getPackageName(), context.getPackageName() + ".Sphinx"));
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, sphnixIntent);//快捷方式的动作
        
        context.sendBroadcast(shortcutIntent);//发送广播
    }
    
    public static TKCellLocation getCellLocation() {
        int phoneType = TelephonyManager.PHONE_TYPE_NONE;
        int lac = -1;
        int cid = -1;
        CellLocation cellLocation = sTelephonyManager.getCellLocation();
        if (cellLocation != null) {
            if (cellLocation instanceof GsmCellLocation) {
                GsmCellLocation gsmCellLocation = (GsmCellLocation)cellLocation;
                phoneType = TelephonyManager.PHONE_TYPE_GSM;
                lac = gsmCellLocation.getLac();
                cid = gsmCellLocation.getCid();
            } else if (cellLocation instanceof CdmaCellLocation) {
                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation)cellLocation;
                phoneType = TelephonyManager.PHONE_TYPE_CDMA;
                int sid = cdmaCellLocation.getSystemId();
                lac = (sid << 16) + cdmaCellLocation.getNetworkId();
                cid = cdmaCellLocation.getBaseStationId();
            }
        }
        
        return new TKCellLocation(phoneType, lac, cid);
    }
    
    public static List<NeighboringCellInfo> getNeighboringCellList() {
        return sTelephonyManager.getNeighboringCellInfo();
    }
    
    public static String getRadioType() {
        String radioType;
        switch (sTelephonyManager.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                radioType = "GPRS";
                break;
            case TelephonyManager.NETWORK_TYPE_CDMA:
                radioType = "CDMA";
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                radioType = "UMTS";
                break;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                radioType = "xRTT";
                break;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                radioType = "EDGE";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                radioType = "EVD0";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                radioType = "EVDA";
                break;
            default:
                radioType = "UNKN";
                break;
        }
        return radioType;
    }
    
    public static int getNetworkType() {
        return sTelephonyManager.getNetworkType();
    }
    
    private static class MyPhoneStateListener extends PhoneStateListener {
        
        @Override
        public void onSignalStrengthChanged(int asu) {
            sSignalStrength = asu;
        }
    }
    
    public static String getDataFileSuffixal() {
        return sDATA_FILE_SUFFIXAL;
    }
    
    public static String getDataPath(boolean isMapData) {
        String path = null;
        if (isMapData) {
            String status = Environment.getExternalStorageState();
            if (status.equals(Environment.MEDIA_MOUNTED)) {
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                path = externalStorageDirectory.getAbsolutePath() + "/tigermap/" +sMAP_DATA_FOLDER_NAME + "/";
                File file = new File(path);
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        LogWrapper.e(TAG, "getDataPath() Unable to create new folder: " + path);
                        path = null;
                    }
                }
            }
            
            if (path == null) {
                path = TKApplication.getInstance().getCacheDir().getAbsolutePath()+"/"+sMAP_DATA_FOLDER_NAME+ "/";
                File file = new File(path);
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        LogWrapper.e(TAG, "getDataPath() Unable to create new folder: " + path);
                    }
                }
            }
        } else {
            path = TKApplication.getInstance().getDir(sRESOURCES_DATA_FOLDER_NAME, Context.MODE_PRIVATE).toString()+"/";
        }
        return path;
    }
    
    public static String getDownloadMapUrl() {
        return sDOWNLOAD_MAP_URL;
    }
    
    public static String getDownloadSuggestUrl() {
        return sDOWNLOAD_SUGGEST_URL;
    }
    
    public static String getQueryUrl() {
        return sQUERY_URL;
    }
    
    public static String getLocationUrl() {
        return sLOCATION_URL;
    }
    
    public static String getAccountManageUrl() {
        return sACCOUNT_MANAGE_URL;
    }
    
    public static String getSMSUrl() {
        return sSMS_URL;
    }
    
    public static String getLocationHost() {
        if (!TextUtils.isEmpty(sDYNAMIC_LOCATION_HOST)) {
            return sDYNAMIC_LOCATION_HOST;
        }
        return sDEFAULT_LOCATION_HOST;
    }
    
    public static void setLocationHost(String host) {
        if (sLoadBalance == false) {
            return;
        }
        sDYNAMIC_LOCATION_HOST = host;
    }
    
    public static String getQueryHost() {
        if (!TextUtils.isEmpty(sDYNAMIC_QUERY_HOST)) {
            return sDYNAMIC_QUERY_HOST;
        }
        return sDEFAULT_QUERY_HOST;
    }
    
    public static void setQueryHost(String host) {
        if (sLoadBalance == false) {
            return;
        }
        sDYNAMIC_QUERY_HOST = host;
    }
    
    public static String getAccountManageHost() {
        if (!TextUtils.isEmpty(sDYNAMIC_ACCOUNT_MANAGE_HOST)) {
            return sDYNAMIC_ACCOUNT_MANAGE_HOST;
        }
        return sDEFAULT_ACCOUNT_MANAGE_HOST;
    }
    
    public static void setAccountManageHost(String host) {
        if (sLoadBalance == false) {
            return;
        }
        sDYNAMIC_ACCOUNT_MANAGE_HOST = host;
    }
    
    public static String getDownloadHost() {
        if (!TextUtils.isEmpty(sDYNAMIC_DOWNLOAD_HOST)) {
            return sDYNAMIC_DOWNLOAD_HOST;
        }
        return sDEFAULT_DOWNLOAD_HOST;
    }
    
    public static void setDownloadHost(String host) {
        if (sLoadBalance == false) {
            return;
        }
        sDYNAMIC_DOWNLOAD_HOST = host;
    }

    public static String getEncoding() {
        return sENCODING;
    }
    
    public static int getMCC() {
        return sMCC;
    }
    
    public static int getMNC() {
        return sMNC;
    }
    
    public static String getClientSoftVersion() {
        return sCLIENT_SOFT_VERSION;
    }

    public static String getMapDataVersion() {
        return sMAP_DATA_VERSION;
    }
    
    public static String getSuggestWordVersion() {
        return sSUGGEST_WORD_VERSION;
    }
    
    public static void setSuggestWordVerson(String suggestWordVersion) {
        sSUGGEST_WORD_VERSION = suggestWordVersion;
    }
    
    public static String getClientDataVersion() {
        return sCLIENT_DATA_VERSION;
    }

    public static String getSpreader() {
        return sSPREADER;
    }

    public static String getPhoneKey() {
        return sPHONE_KEY;
    }

    public static String getVersionOfPlatform() {
        return sVERSION_OF_PLATFORM;
    }
    
    public static String getIMSI() {
        return sIMSI;
    }

    public static String getIMEI() {
        return sIMEI;
    }
    
    public static String getUserAgent() {
        return sUSER_AGENT;
    }

    public static int getHttpSocketTimeout() {
        return sHTTP_SOCKET_TIMEOUT;
    }

    public static void setUserActionTrack(Context context, String userActionTrack) {
        if (TextUtils.isEmpty(userActionTrack)) {
            return;
        }
        sUser_Action_Track = userActionTrack;
        setPref(context, PREFS_USER_ACTION_TRACK, userActionTrack);
    }

    public static String getUserActionTrack() {
        return sUser_Action_Track;
    }
    
    public static void removePref(Context context, String name) {
        if (context == null || TextUtils.isEmpty(name)) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_WORLD_WRITEABLE);
        sharedPreferences.edit().remove(name).commit();
    }

    /**
     * set preference
     * @param url
     * @param tile
     */
    public static void setPref(Context context, String name, String value) {
        if (context == null || TextUtils.isEmpty(name)) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_WORLD_WRITEABLE);
        sharedPreferences.edit().putString(name, value).commit();
    }

    /**
     * get preference
     * @param name
     */
    public static String getPref(Context context, String name) {
        return getPref(context, name, null);
    }

    /**
     * get preference
     * @param name
     */
    public static String getPref(Context context, String name, String defaultValue) {
        if (context == null || TextUtils.isEmpty(name)) {
            return defaultValue;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_WORLD_READABLE);
        return sharedPreferences.getString(name, defaultValue);
    }
    
    public static void readConfig() {
        sMap_Padding = (int)(56*Globals.g_metrics.density);
        String mapPath = TKConfig.getDataPath(true);
        if (TextUtils.isEmpty(mapPath)) {
            return;
        }
        File file = new File(mapPath+"config.txt");
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                String text = CommonUtils.readFile(fis);
                fis.close();
                int start = text.indexOf("downloadHost=");
                int end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "downloadHost=".length();
                    TKConfig.sDYNAMIC_DOWNLOAD_HOST = text.substring(start, end);
                }
                start = text.indexOf("queryHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "queryHost=".length();
                    TKConfig.sDYNAMIC_QUERY_HOST = text.substring(start, end);
                }
                start = text.indexOf("locationHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "locationHost=".length();
                    TKConfig.sDYNAMIC_LOCATION_HOST = text.substring(start, end);
                }
                start = text.indexOf("accountManageHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "accountManageHost=".length();
                    TKConfig.sDYNAMIC_ACCOUNT_MANAGE_HOST = text.substring(start, end);
                }
                start = text.indexOf("mapDownloadUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "mapDownloadUrl=".length();
                    TKConfig.sDOWNLOAD_MAP_URL = text.substring(start, end);
                }
                start = text.indexOf("suggestDownloadUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "suggestDownloadUrl=".length();
                    TKConfig.sDOWNLOAD_SUGGEST_URL = text.substring(start, end);
                }
                start = text.indexOf("queryUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "queryUrl=".length();
                    TKConfig.sQUERY_URL = text.substring(start, end);
                }
                start = text.indexOf("locationUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "locationUrl=".length();
                    TKConfig.sLOCATION_URL = text.substring(start, end);
                }
                start = text.indexOf("accountManageUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "accountManageUrl=".length();
                    TKConfig.sACCOUNT_MANAGE_URL = text.substring(start, end);
                }
                start = text.indexOf("loadBalance=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "loadBalance=".length();
                    TKConfig.sLoadBalance = text.substring(start, end).equals("true");
                }
                start = text.indexOf("spreader=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "spreader=".length();
                    TKConfig.sSPREADER = text.substring(start, end);
                }
                start = text.indexOf("clientSoftVersion=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "clientSoftVersion=".length();
                    TKConfig.sCLIENT_SOFT_VERSION = text.substring(start, end);
                }
                start = text.indexOf("commentSource=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "commentSource=".length();
                    TKConfig.sCommentSource = text.substring(start, end);
                }
                start = text.indexOf("logLevel=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "logLevel=".length();
                    CONFIG.LOG_LEVEL = Integer.parseInt(text.substring(start, end));
                }
                start = text.indexOf("loginUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "loginUrl=".length();
                    LOGIN_URL = text.substring(start, end);
                }
                start = text.indexOf("loginHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "loginHost=".length();
                    LOGIN_HOST = text.substring(start, end).split(",");
                }
                start = text.indexOf("pageSize=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "pageSize=".length();
                    sPage_Size = Integer.parseInt(text.substring(start, end));
                }
                BaseQuery.initCommonParameters();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void updateIMSI(ConnectivityManager connectivityManager) {
        if (connectivityManager == null) {
            return;
        }
        // mobile
        NetworkInfo mobileNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        // wifi
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    
        if (mobileNetworkInfo != null) {
            
            if (mobileNetworkInfo.getState() == State.CONNECTED || mobileNetworkInfo.getState() == State.CONNECTING) {
                // mobile
                if (sIMSI.contains("@wifi")) {
                    sIMSI = sIMSI.replace("@wifi", "");
                }
            }
        }
        
        if (wifiNetworkInfo != null) {
            
            if (wifiNetworkInfo.getState() == State.CONNECTED || wifiNetworkInfo.getState() == State.CONNECTING) {
                // wifi
                if (!sIMSI.contains("@wifi")) {
                    sIMSI += "@wifi";
                }
            }
        }
        
    }
}
