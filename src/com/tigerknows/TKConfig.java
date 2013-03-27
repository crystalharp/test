/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.decarta.CONFIG;
import com.decarta.Globals;
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
import android.content.SharedPreferences.Editor;
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
 * 配置应用程序中的通用参数
 * @author Peng Wenyue
 */
public class TKConfig {
    
    private static final String TAG = "TKConfig";
    
    /**
     * 是否开启缓存图片到内存
     */
    public static boolean CACHE_BITMAP_TO_MEMORY = true;
    
    /**
     * 缓存内存图片的最大数目
     */
    public static int CACHE_SIZE = 32;
    
    /**
     * 默认地图显示级别
     */
    public static final int ZOOM_LEVEL_DEFAULT = 5; // 200km
    
    /**
     * 点击定位按钮时显示地图的最低级别
     */
    public static final int ZOOM_LEVEL_LOCATION = 14; // 200m
    
    /**
     * 切换至单个POI时显示地图的最低级别
     */
    public static final int ZOOM_LEVEL_POI = 15;
    
    /**
     * 网络请求头中的User-Agent值
     */
    private static String sUSER_AGENT = "tigermap/1.50";
    
    /**
     * 网络超时值
     */
    private static int sHTTP_SOCKET_TIMEOUT = 60*1000;            // default to 1 min

    /**
     * 地图数据文件夹名称
     */
    private static final String MAP_DATA_FOLDER_NAME = "map"; 
    
    /**
     * 地图引擎资源文件夹名称
     */
    private static final String RESOURCES_DATA_FOLDER_NAME = "res"; 

    /**
     * 筛选项数据文件名格式
     * %s 数据类型，1为poi；2为团购==
     * %d 城市Id
     */
    public static final String FILTER_FILE = "filter_%s_%d"; 
    
    /**
     * 联想词库index文件名格式
     * %d 城市Id
     */
    public static final String SUGGEST_LEXCION_FILE_SW_INDEX = "sw2_%d_index"; 
    
    /**
     * 联想词库s文件名格式
     * %d 城市Id
     */
    public static final String SUGGEST_LEXCION_FILE_SW_S = "sw2_%d_s"; 
    
    /**
     * 联想词库l文件名格式
     * %d 城市Id
     */
    public static final String SUGGEST_LEXCION_FILE_SW_L = "sw2_%d_l"; 
    
    /**
     * 联想词库diff_index文件名格式
     * %d 城市Id
     */
    public static final String SUGGEST_LEXCION_FILE_DIFF_INDEX = "sw2_%d_diff_index"; 
    
    /**
     * 联想词库diff_s文件名格式
     * %d 城市Id
     */
    public static final String SUGGEST_LEXCION_FILE_DIFF_S = "sw2_%d_diff_s"; 
    
    /**
     * 联想词库diff_l文件名格式
     * %d 城市Id
     */
    public static final String SUGGEST_LEXCION_FILE_DIFF_L = "sw2_%d_diff_l"; 
    
    /**
     * 联想词库临时文件名格式
     * %d 城市Id
     */
    public static final String SUGGEST_LEXCION_FILE_TEMP = "%d"; 
    
    /**
     * Region无数据文件名格式
     */
    public static final String MAP_REGION_METAFILE = "region%d.meta";
    
    /**
     * 网络请求返回结果中的字符串字段的编码，参数值必须是java.lang.String.getBytes(String charset)支持的编码，否则整个查询结果为空；如果不提供该参数，默认编码为UTF-16LE
     */
    private static String sENCODING = System.getProperty("file.encoding"); // "utf-8"
    
    /**
     * mobile country code，国家码，在大陆地区为460
     */
    private static int sMCC = -1;

    /**
     * mobile network code，移动网络识别码，用于识别不同运营商的网络，位长2位。中国移动GSM网为00，中国联通的GSM网为01，中国联通CDMA网为03
     */
    private static int sMNC = -1;
    
    /**
     * 网络请求参数中的客户端软件版本
     */
    private static String sCLIENT_SOFT_VERSION = "4.30.20130326A";
    
    /**
     * 关于我们界面中的客户端软件发布版本
     */
    
    public static final String CLIENT_SOFT_VERSION = "4.30.alpha1";
    /**
     * 关于我们界面中的客户端软件发布日期
     */
    public static final String CLIENT_SOFT_RELEASE_DATE = "2013-03-26";
    
    /**
     * 手机产品信息，包括品牌、型号、软件版本等信息
     * es:
     * Build.MODEL + "|" + Build.DISPLAY + "|" + Build.PRODUCT
     */
    private static String sPHONE_KEY = "Tigerknows"; 
    
    /**
     * 市场推广渠道标签
     */
    public static String sSPREADER = "TigerMapTest";
    
    /**
     * 腾讯标签
     */
    public static final String SPREADER_TENCENT = "ANDqQFUnusual-SF";
    
    /**
     * 点评来源，显示和发表点评时的限制条件
     * user为正式
     * test为测试
     */
    public static String sCommentSource = "test";
    
    /**
     * IMSI(International Mobile Subscriber Identity)，国际移动用户标识号，是TD系统分给用户的唯一标识号，它存储在SIM卡、HLR/VLR中，最多由15个数字组成
     * 当前数据网络连接的是wifi时，则在IMSI后加上后缀@wifi
     */
    private static String sIMSI = "0";
    
    /**
     * IMEI(International Mobile Equipment Identity)，国际移动设备身份码
     */
    private static String sIMEI = "0";
    
    /**
     * 手机信号强度值
     */
    private static int sSignalStrength = Integer.MAX_VALUE;

    /**
     * TelephonyManager
     */
    private static TelephonyManager sTelephonyManager;
    
    /**
     * 监听手机信号改变事件
     * @author pengwenyue
     *
     */
    private static class MyPhoneStateListener extends PhoneStateListener {
        
        @Override
        public void onSignalStrengthChanged(int asu) {
            sSignalStrength = asu;
        }
    }
    
    /**
     * 当前手机系统版本
     */
    private static final String sVERSION_OF_PLATFORM = String.valueOf(Build.VERSION.SDK_INT);
    
    /**
     * 是否开启用户行为日志上传
     * 默认开启
     */
    private static String sUser_Action_Track = "on";

    /**
     * 是否接受软件登录服务推送用于动态负载均衡的Host
     */
    public static boolean sLoadBalance = true;
    
    /**
     * 下载地图服务访问URL路径
     */
    public static String sDOWNLOAD_MAP_URL = "http://%s/quantum/string";
    
    /**
     * 下载联想词服务访问URL路径
     */
    public static String sDOWNLOAD_SUGGEST_URL = "http://%s/suggest_lexicon";
    
    /**
     * 查询服务访问URL路径
     */
    public static String sQUERY_URL = "http://%s/cormorant/local";
    
    /**
     * 定位服务访问URL路径
     */
    public static String sLOCATION_URL = "http://%s/tk_locate_me";
    
    /**
     * 账户管理服务访问URL路径
     */
    public static String sACCOUNT_MANAGE_URL = "http://%s/melon/user"; // http://192.168.11.174:8100/melon/user
    
    /**
     * 软件登录服务访问URL路径
     */
    public static String LOGIN_URL = "http://%s/bootstrap/local";

    /**
     * 默认下载服务器Host
     */
    private static String sDEFAULT_DOWNLOAD_HOST = "download.tigerknows.net";
    
    /**
     * 默认查询服务器Host
     */
    private static String sDEFAULT_QUERY_HOST = "search.tigerknows.net"; // 192.168.11.200:8080 local.tigerknows.net  good2012.gicp.net 192.168.11.174:8080 search.tigerknows.net 192.168.11.31:8080 211.151.98.76:8080
    
    /**
     * 默认定位服务器Host
     */
    private static String sDEFAULT_LOCATION_HOST = "position.tigerknows.net";
    
    /**
     * 默认账户管理服务器Host
     */
    private static String sDEFAULT_ACCOUNT_MANAGE_HOST = "user.tigerknows.net"; //192.168.11.174:8100 211.151.97.117:8080 user.tigerknows.net
    
    /** 
     * 默认软件登录服务器的Host
     */
    public static String[] BOOTSTRAP_HOST = new String[]{"init.tigerknows.net", "chshh.tigerknows.com", "csh.laohubaodian.net"};

    /**
     * 软件登录服务推送用于动态负载均衡的下载服务器Host
     */
    public static String sDYNAMIC_DOWNLOAD_HOST;
    
    /**
     * 软件登录服务推送用于动态负载均衡的查询服务器Host
     */
    public static String sDYNAMIC_QUERY_HOST;
    
    /**
     * 软件登录服务推送用于动态负载均衡的定位服务器Host
     */
    public static String sDYNAMIC_LOCATION_HOST;
    
    /**
     * 软件登录服务推送用于动态负载均衡的账户管理服务器Host
     */
    public static String sDYNAMIC_ACCOUNT_MANAGE_HOST;
    
    /**
     * 分页大小
     */
    private static int sPage_Size = 20;
    
    /**
     * 地图上显示Shape和OverlayItem的Padding
     */
    public static int sMap_Padding = 56;
    
    /**
     * 服务器返回的经纬度都是乘以此数值后的结果，所以需要除以此常量才得到实际的值
     */
    public static int LON_LAT_DIVISOR = 100000;
    
    /**
     * Preferences文件名称
     * 存储应用程序中使用的参数设置，key/value
     */
    public static final String TIGERKNOWS_PREFS = "tigerknows_prefs";
    
    /**
     * 是否上传用户行为日志
     */
    public static final String PREFS_USER_ACTION_TRACK = "prefs_user_action_track";
    
    /**
     * 离开软件时地图上的经度，且此经度一定在选择城市范围内
     */
    public static final String PREFS_LAST_LON = "prefs_last_lon";
    
    /**
     * 离开软件时地图上的纬度，且此纬度一定在选择城市范围内
     */
    public static final String PREFS_LAST_LAT = "prefs_last_lat";
    
    /**
     * 离开软件时地图上的比例尺大小，且此时经纬度一定在选择城市范围内
     */
    public static final String PREFS_LAST_ZOOM_LEVEL = "prefs_last_zoom_level";

    /**
     * 当前软件版本
     */
    public static final String PREFS_VERSION_NAME = "prefs_version_name";
    
    /**
     * 是否为升级后使用本软件
     */
    public static final String PREFS_UPGRADE = "prefs_learn_4.20";    
    
    /**
     * 是否为第一次安装使用本软件
     */
    public static final String PREFS_FIRST_USE = "prefs_learn_3.10";    
    
    /**
     * 是否已创建快捷方式
     */
    public static final String PREFS_HAS_SHORT_CUT_PREFS = "prefs_has_short_cut";
    
    /**
     * 能否弹出提醒用户进行定位设置的对话框
     */
    public static final String PREFS_SHOW_LOCATION_SETTINGS_TIP = "show_location_settings_tip"; 

    /**
     * 能否在一周内在更多界面显示提醒用户地图升级的消息
     */
    public static final String PREFS_SHOW_UPGRADE_MAP_TIP = "show_upgrade_map_tip";    
    
    /**
     * 能否在更多界面显示提醒用户点评的消息
     */
    public static final String PREFS_SHOW_UPGRADE_COMMENT_TIP = "show_upgrade_comment_tip";    
    
    /**
     * 用户在账户管理相关界面输入的电话号码
     */
    public static final String PREFS_PHONENUM = "prefs_phonenum";
    
    /**
     * 下载列表中的所有城市的地图信息及状态
     * 数据结构如下:
     * 城市中文名,地图数据总大小,已经下载数据的大小,状态;城市中文名,地图数据总大小,已经下载数据的大小,状态
     * es:
     * 北京,561200,562400,3;广州,485655,385521,1
     */
    public static final String PREFS_MAP_DOWNLOAD_CITYS = "map_download_citys_v2";
    
    /**
     * 是否开启屏幕常亮
     */
    public static final String PREFS_ACQUIRE_WAKELOCK = "acquire_wakelock";
    
    /**
     * 客户端Id
     * 用户清除软件数据时会通过UUID.randomUUID()自动生成
     */
    public static final String PREFS_CLIENT_UID = "prefs_client_uid";
    
    /**
     * 用于POI搜索的历史词
     */
    public static final String PREFS_HISTORY_WORD_POI = "history_word_poi_%d";
    
    /**
     * 用于交通查询的历史词
     */
    public static final String PREFS_HISTORY_WORD_TRAFFIC = "history_word_traffic_%d";
    
    /**
     * 用于线路查询的历史词
     */
    public static final String PREFS_HISTORY_WORD_BUSLINE = "history_word_busline_%d";
    
    /**
     * 最近浏览过的RegionId列表
     */
    public static final String PREFS_LAST_REGION_ID_LIST = "last_region_id_list";
    
    /**
     * 是否已显示POI详情界面的用户引导
     */
    public static final String PREFS_HINT_POI_DETAIL = "prefs_hint_poi_detail";
    
    /**
     * 是否已显示发现首页界面的用户引导
     */
    public static final String PREFS_HINT_DISCOVER_HOME = "prefs_discover";
    
    /**
     * 是否已显示POI结果列表界面的用户引导
     */
    public static final String PREFS_HINT_POI_LIST = "prefs_hint_poi_list";
    
    /**
     * 是否已显示团购列表界面的用户引导（筛选项和订单）
     */
    public static final String PREFS_HINT_DISCOVER_TUANGOU_LIST = "prefs_hint_discover_tuangou_list";
    
    /**
     * 是否已显示团购列表界面的用户引导（订单）
     */
    public static final String PREFS_HINT_DISCOVER_TUANGOU_DINGDAN = "prefs_hint_discover_tuangou_dingdan";
    
    /**
     * 是否已显示地图界面的用户引导
     */
    public static final String PREFS_HINT_LOCATION = "prefs_hint_location";
    
    /**
     * 推送服务绝对启动时间
     */
    public static final String PREFS_RADAR_PULL_ALARM_ABSOLUTE = "prefs_radar_pull_alarm_absolute";
    
    /**
     * 推送服务相对启动时间
     */
    public static final String PREFS_RADAR_PULL_ALARM_RELETIVE = "prefs_radar_pull_alarm_reletive";
    
    /**
     * 定位收集服务绝对启动时间
     */
    public static final String PREFS_RADAR_LOCATION_COLLECTION_ALARM_ABSOLUTE = "prefs_radar_location_collection_alarm_absolute";
    
    /**
     * 定位收集服务相对启动时间
     */
    public static final String PREFS_RADAR_LOCATION_COLLECTION_ALARM_RELETIVE = "prefs_radar_location_collection_alarm_reletive";
    
    /**
     * 已收到的推送消息Id的列表
     */
    public static final String PREFS_RADAR_RECORD_MESSAGE_ID_LIST = "prefs_radar_record_message_id_list";
    
    /**
     * 上次推送服务成功的时间
     */
    public static final String PREFS_RADAR_RECORD_LAST_SUCCEED_TIME = "prefs_radar_record_last_succeed_time";
    
    /**
     * 在客户端存储推送消息数目的最大值
     */
    public static final String PREFS_RADAR_RECORD_MESSAGE_UPPER_LIMIT = "prefs_radar_record_message_upper_limit";
    
    /**
     * 推送和定位收集服务的开关
     */
    public static final String PREFS_RADAR_PULL_SERVICE_SWITCH = "prefs_radar_pull_service_switch";

    
    /**
     * 发现分类图片尺寸的Key
     */
    public static final int PICTURE_DISCOVER_HOME = 1;
    
    /**
     * 团购列表图片尺寸的Key
     */
    public static final int PICTURE_TUANGOU_LIST = 2;
    
    /**
     * 电影列表图片尺寸的Key
     */
    public static final int PICTURE_DIANYING_LIST = 3;
    
    /**
     * 团购影详情图片尺寸的Key
     */
    public static final int PICTURE_TUANGOU_DETAIL = 4;
    
    /**
     * 团购套餐详情图片尺寸的Key
     */
    public static final int PICTURE_TUANGOU_TAOCAN = 5;
    
    /**
     * 电影详情图片尺寸的Key
     */
    public static final int PICTURE_DIANYING_DETAIL = 6;

    /**
     * 黑色
     */
    public static final int COLOR_BLACK_DARK = 0xff323232;
    
    /**
     * 亮黑色
     */
    public static final int COLOR_BLACK_LIGHT = 0xff969696;
    
    /**
     * 橙色
     */
    public static final int COLOR_ORANGE = 0xffff6c00;
    
    /**
     * 推送服务失败后重试的时间间隔（单位：分钟）
     */
    public static int PullServiceFailedRetryTime=60;
    
    /**
     * 无效定时器被重置的延迟时间（单位：分钟）
     */
    public static int AlarmCheckDelayTime=60;

    /**
     * 初始化ClientUid、共用的网络请求参数（imsi、imsi、mcc、mnc）
     * @param context
     */
    public static void init(Context context) {
        LogWrapper.i(TAG, "init()");

        String clientUID = TKConfig.getPref(context, TKConfig.PREFS_CLIENT_UID);
        if (TextUtils.isEmpty(clientUID)) {   // 为空则生成一个ClientUID
            clientUID = UUID.randomUUID().toString();
            TKConfig.setPref(context, TKConfig.PREFS_CLIENT_UID, clientUID);
        }
        Globals.g_ClientUID = clientUID;
        
        sPHONE_KEY = (Build.MODEL + "|" + Build.DISPLAY + "|" + Build.PRODUCT).replace(",", "").replace("=", "");
        
        getTelephonyInfo(context);
        BaseQuery.initCommonParameters();

        sUser_Action_Track = getPref(context, PREFS_USER_ACTION_TRACK, "on");
        
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
        
    }
    
    public static void getTelephonyInfo(Context context) {

        sTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        sSignalStrength = Integer.MAX_VALUE;
        
        if (sTelephonyManager != null) {
            sTelephonyManager.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_SIGNAL_STRENGTH);
            
            sIMEI = sTelephonyManager.getDeviceId();        
            sIMSI = sTelephonyManager.getSubscriberId();
            
            ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            updateIMSI(connectivityManager);
            
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
                    sMCC = -1;
                    sMNC = -1;
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
                        sMCC = -1;
                        sMNC = -1;
                        LogWrapper.e(TAG, "init() Can't get MCC and MNC from getNetworkOperator()");
                    }
                }
            }
            
            // 如果IMEI为空则将其Wifi的MacAddress作为IMEI，此种情况出现在部分不是移动数字电话的设备上
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
        }
    }

    /**
     * 创建应用程序快捷方式
     */
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
    
    /**
     * 获取当前基站信息
     * @return
     */
    public static TKCellLocation getCellLocation() {
        int phoneType = TelephonyManager.PHONE_TYPE_NONE;
        int lac = -1;
        int cid = -1;
        if (sTelephonyManager != null) {
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
        }
        
        return new TKCellLocation(phoneType, lac, cid, sSignalStrength);
    }
    
    /**
     * 获取邻近基站信息
     * @return
     */
    public static List<NeighboringCellInfo> getNeighboringCellList() {
        List<NeighboringCellInfo> list = null;
        if (sTelephonyManager != null) {
            list = sTelephonyManager.getNeighboringCellInfo();
        }
        return list;
    }
    
    /**
     * 获取设备连接的数据网络类型
     * @return
     */
    public static String getRadioType() {
        String radioType = "UNKN";
        if (sTelephonyManager != null) {
            int networkType = sTelephonyManager.getNetworkType(); 
            switch (networkType) {
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
                    radioType = "UNKN"+networkType;
                    break;
            }
        }
        return radioType;
    }
    
    /**
     * 获取数据存储文件路径
     * @param isMapData ture，地图数据文件路径（优先使用扩展存储卡，其次使用应用程序数据空间）；false，地图引擎资源文件路径
     * @return
     */
    public static String getDataPath(boolean isMapData) {
        String path = null;
        if (isMapData) {
            // 检查扩展存储卡
            String status = Environment.getExternalStorageState();
            if (status.equals(Environment.MEDIA_MOUNTED)) {
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                path = externalStorageDirectory.getAbsolutePath() + "/tigermap/" +MAP_DATA_FOLDER_NAME + "/";
                File file = new File(path);
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        LogWrapper.e(TAG, "getDataPath() Unable to create new folder: " + path);
                        path = null;
                    }
                }
            }
            
            // 使用应用程序数据空间
            if (path == null) {
                path = TKApplication.getInstance().getCacheDir().getAbsolutePath()+"/"+MAP_DATA_FOLDER_NAME+ "/";
                File file = new File(path);
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        LogWrapper.e(TAG, "getDataPath() Unable to create new folder: " + path);
                    }
                }
            }
        } else {
            path = TKApplication.getInstance().getDir(RESOURCES_DATA_FOLDER_NAME, Context.MODE_PRIVATE).toString()+"/";
        }
        return path;
    }
    
    /**
     * 获取下载地图服务访问的URL
     * @return
     */
    public static String getDownloadMapUrl() {
        return sDOWNLOAD_MAP_URL;
    }
    
    /**
     * 获取下载联想词服务访问的URL
     * @return
     */
    public static String getDownloadSuggestUrl() {
        return sDOWNLOAD_SUGGEST_URL;
    }
    
    /**
     * 获取查询服务访问的URL
     * @return
     */
    public static String getQueryUrl() {
        return sQUERY_URL;
    }
    
    /**
     * 获取定位服务访问的URL
     * @return
     */
    public static String getLocationUrl() {
        return sLOCATION_URL;
    }
    
    /**
     * 获取账户管理服务访问的URL
     * @return
     */
    public static String getAccountManageUrl() {
        return sACCOUNT_MANAGE_URL;
    }
    
    /**
     * 获取定位服务器Host，如果有动态负载均衡的定位服务器Host则返回该值
     * @return
     */
    public static String getLocationHost() {
        if (!TextUtils.isEmpty(sDYNAMIC_LOCATION_HOST)) {
            return sDYNAMIC_LOCATION_HOST;
        }
        return sDEFAULT_LOCATION_HOST;
    }
    
    /**
     * 设置动态负载均衡的定位服务器Host
     * @param host
     */
    public static void setDynamicLocationHost(String host) {
        if (sLoadBalance == false) {
            return;
        }
        sDYNAMIC_LOCATION_HOST = host;
    }
    
    /**
     * 获取查询服务器Host，如果有动态负载均衡的查询服务器Host则返回该值
     * @return
     */
    public static String getQueryHost() {
        if (!TextUtils.isEmpty(sDYNAMIC_QUERY_HOST)) {
            return sDYNAMIC_QUERY_HOST;
        }
        return sDEFAULT_QUERY_HOST;
    }
    
    /**
     * 设置动态负载均衡的查询服务器Host
     * @param host
     */
    public static void setDynamicQueryHost(String host) {
        if (sLoadBalance == false) {
            return;
        }
        sDYNAMIC_QUERY_HOST = host;
    }
    
    /**
     * 获取账户管理服务器Host，如果有动态负载均衡的账户管理服务器Host则返回该值
     * @return
     */
    public static String getAccountManageHost() {
        if (!TextUtils.isEmpty(sDYNAMIC_ACCOUNT_MANAGE_HOST)) {
            return sDYNAMIC_ACCOUNT_MANAGE_HOST;
        }
        return sDEFAULT_ACCOUNT_MANAGE_HOST;
    }
    
    /**
     * 设置动态负载均衡的账户管理服务器Host
     * @param host
     */
    public static void setDynamicAccountManageHost(String host) {
        if (sLoadBalance == false) {
            return;
        }
        sDYNAMIC_ACCOUNT_MANAGE_HOST = host;
    }
    
    /**
     * 获取下载服务器Host，如果有动态负载均衡的下载服务器Host则返回该值
     * @return
     */
    public static String getDownloadHost() {
        if (!TextUtils.isEmpty(sDYNAMIC_DOWNLOAD_HOST)) {
            return sDYNAMIC_DOWNLOAD_HOST;
        }
        return sDEFAULT_DOWNLOAD_HOST;
    }
    
    /**
     * 设置动态负载均衡的下载服务器Host
     * @param host
     */
    public static void setDynamicDownloadHost(String host) {
        if (sLoadBalance == false) {
            return;
        }
        sDYNAMIC_DOWNLOAD_HOST = host;
    }

    /**
     * 获取网络请求返回结果中的字符编码
     * @return
     */
    public static String getEncoding() {
        return sENCODING;
    }
    
    /**
     * get MCC
     * @return
     */
    public static int getMCC() {
        return sMCC;
    }
    
    /**
     * get MNC
     * @return
     */
    public static int getMNC() {
        return sMNC;
    }
    
    /**
     * 获取网络请求参数中的客户端软件版本
     * @return
     */
    public static String getClientSoftVersion() {
        return sCLIENT_SOFT_VERSION;
    }

    /**
     * 获取市场推广渠道标签
     * @return
     */
    public static String getSpreader() {
        return sSPREADER;
    }

    /**
     * 获取手机产品信息，包括品牌、型号、软件版本等信息
     * @return
     */
    public static String getPhoneKey() {
        return sPHONE_KEY;
    }

    /**
     * 获取当前手机系统版本
     * @return
     */
    public static String getVersionOfPlatform() {
        return sVERSION_OF_PLATFORM;
    }
    
    /**
     * get IMSI
     * @return
     */
    public static String getIMSI() {
        return sIMSI;
    }

    /**
     * get IMEI
     * @return
     */
    public static String getIMEI() {
        return sIMEI;
    }
    
    /**
     * 获取网络请求头中的User-Agent值
     * @return
     */
    public static String getUserAgent() {
        return sUSER_AGENT;
    }

    /**
     * 获取网络超时值
     * @return
     */
    public static int getHttpSocketTimeout() {
        return sHTTP_SOCKET_TIMEOUT;
    }

    /**
     * 设置开关上传用户行为日志
     * @param context
     * @param userActionTrack
     */
    public static void setUserActionTrack(Context context, String userActionTrack) {
        if (TextUtils.isEmpty(userActionTrack)) {
            return;
        }
        sUser_Action_Track = userActionTrack;
        setPref(context, PREFS_USER_ACTION_TRACK, userActionTrack);
    }

    /**
     * 是否开启用户行为日志上传
     */
    public static String getUserActionTrack() {
        return sUser_Action_Track;
    }
    
    /**
     * 获取分页大小
     */
    public static int getPageSize() {
        return sPage_Size;
    }
    
    /**
     * 获取手机信号强度值
     */
    public static int getSignalStrength() {
        return sSignalStrength;
    }
    
    /**
     * remove preference
     * @param context
     * @param name 名称
     */
    public static void removePref(Context context, String name) {
        if (context == null || TextUtils.isEmpty(name)) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_WORLD_WRITEABLE);
        sharedPreferences.edit().remove(name).commit();
    }

    /**
     * set preference
     * @param name 名称
     * @param value 值
     */
    public static void setPref(Context context, String name, String value) {
        if (context == null || TextUtils.isEmpty(name)) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_WORLD_WRITEABLE);
        try {
            sharedPreferences.edit().putString(name, value).commit();
        } catch (Exception e) {
            Editor editor = sharedPreferences.edit();
            editor.remove(name).commit();
            editor.putString(name, value).commit();
        }
    }

    /**
     * get preference
     * @param name 名称
     */
    public static String getPref(Context context, String name) {
        return getPref(context, name, null);
    }

    /**
     * get preference
     * @param 名称
     * @defaultValue 默认值
     */
    public static String getPref(Context context, String name, String defaultValue) {
        String value = defaultValue;
        if (context == null || TextUtils.isEmpty(name)) {
            return value;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_WORLD_READABLE);
        try {
            value = sharedPreferences.getString(name, defaultValue);
        } catch (Exception e) {
            Editor editor = sharedPreferences.edit();
            editor.remove(name).commit();
            editor.putString(name, value).commit();
        }
        return value;
    }
    
    /**
     * 读取配置文件(地图数据文件存储路径/config.txt)
     * 设置参数
     */
    public static void readConfig() {
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
                    BOOTSTRAP_HOST = text.substring(start, end).split(",");
                }
                start = text.indexOf("pageSize=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "pageSize=".length();
                    sPage_Size = Integer.parseInt(text.substring(start, end));
                }
                start = text.indexOf("pullServiceFailedRetryTime=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "pullServiceFailedRetryTime=".length();
                    PullServiceFailedRetryTime = Integer.parseInt(text.substring(start, end));
                }
                start = text.indexOf("alarmCheckDelayTime=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "alarmCheckDelayTime=".length();
                    AlarmCheckDelayTime = Integer.parseInt(text.substring(start, end));
                }
                
                BaseQuery.initCommonParameters();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 更新IMSI
     * @param connectivityManager
     */
    public static void updateIMSI(ConnectivityManager connectivityManager) {
        final String wifi = "@wifi";
        // 如果IMSI为空则使用固定值@wifi
        if (TextUtils.isEmpty(sIMSI)) {
            sIMSI = wifi;
            return;
        }
        
        if (connectivityManager == null) {
            return;
        }
        
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // 如果当前使用wifi网络则将IMSI加上后缀@wifi
                if (networkInfo.getState() == State.CONNECTED || networkInfo.getState() == State.CONNECTING) {
                    if (sIMSI.length() > 5
                            && sIMSI.lastIndexOf(wifi)+5 != sIMSI.length()) {
                        sIMSI += wifi;
                        return;
                    }
                }
            
            }
        }
        
        // 如果当前使用mobile网络则从IMSI中删除后缀@wifi
        if (sIMSI.length() > 5
                && sIMSI.lastIndexOf(wifi)+5 == sIMSI.length()) {
            sIMSI = sIMSI.replace(wifi, "");
        }
    }
}
