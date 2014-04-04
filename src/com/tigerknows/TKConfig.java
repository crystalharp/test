/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.decarta.CONFIG;
import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.android.app.TKApplication;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.TKCellLocation;
import com.tigerknows.util.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
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
import android.os.StatFs;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * 配置应用程序中的通用参数
 * @author Peng Wenyue
 */
public class TKConfig {
    
    private static final String TAG = "TKConfig";
    
    public static final int NO_STORAGE_ERROR = -1;
    public static final int CANNOT_STAT_ERROR = -2;
    
    /**
     * 是否开启缓存图片到内存
     */
    public static boolean CACHE_BITMAP_TO_MEMORY = true;
    
    /**
     * 缓存在内存的图片最大数目
     */
    public static int IMAGE_CACHE_SIZE_MEMORY = 32;
    
    /**
     * 缓存在扩展存储卡的图片最大数目
     */
    public static int IMAGE_CACHE_SIZE_SDCARD = 1024;
    
    /**
     * 默认地图显示级别
     */
    public static final int ZOOM_LEVEL_DEFAULT = 5; // 200km
    
    /**
     * 点击定位按钮时显示地图的最低级别
     */
    public static final int ZOOM_LEVEL_LOCATION = 15; // 100m
    
    /**
     * 切换至单个POI时显示地图的最低级别
     */
    public static final int ZOOM_LEVEL_POI = 15;
    
    /**
     * 网络请求头中的User-Agent值
     */
    private static String sUSER_AGENT = "tigerknows";
    
    /**
     * 网络超时值
     */
    private static int sHTTP_SOCKET_TIMEOUT = 30*1000;            // default to 1 min

    /**
     * 地图数据文件夹名称
     */
    private static final String MAP_DATA_FOLDER_NAME = "map"; 
    
    /**
     * 地图引擎资源文件夹名称
     */
    private static final String RESOURCES_DATA_FOLDER_NAME = "res"; 

    /**
     * 下载文件夹名称
     */
    public static final String DOWNLOAD_FOLDER_NAME = "download"; 

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
     * Region元数据文件名格式
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
    private static String sCLIENT_SOFT_VERSION = "5.30.20130723A";
    
    /**
     * 关于我们界面中的客户端软件发布版本
     */
    
    public static final String CLIENT_SOFT_VERSION = "5.30";
    /**
     * 关于我们界面中的客户端软件发布日期
     */
    public static final String CLIENT_SOFT_RELEASE_DATE = "2013-07-23";
    
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
     * 是否可以修改请求的数据
     */    
    public static boolean ModifyRequestData = false;
    
    /**
     * 是否可以修改返回的数据
     */    
    public static boolean ModifyResponseData = false;
    
    /**
     * 是否使用假数据
     */    
    public static int LaunchTest = 0;
    
    /**
     * 是否保存数据
     */    
    public static boolean SaveResponseData = false;
    
    /**
     * 是否显示测试选项对话框
     */
    public static boolean ShowTestOption = true;
    
    /**
     * 是否开启TalkingData数据统计
     */
    public static boolean ENABLE_TALKINGDATA = false;
    
    /**
     * 是否在客户端检测发送请求的参数（各个Query中的checkParameter函数）
     */
    public static boolean CheckParameters = true;
    
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
        public void onSignalStrengthsChanged(SignalStrength paramSignalStrength) {
            CellLocation cellLocation = sTelephonyManager.getCellLocation();
            if (cellLocation instanceof CdmaCellLocation) {
                sSignalStrength = paramSignalStrength.getCdmaDbm();
            } else if (cellLocation instanceof GsmCellLocation) {
                sSignalStrength = paramSignalStrength.getGsmSignalStrength();
            }
            LogWrapper.d(TAG, "onSignalStrengthsChanged:"+sSignalStrength);
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
    public static boolean LoadBalance = true;
    
    /**
     * 下载地图服务访问URL路径
     */
    private static String sDOWNLOAD_MAP_URL = "http://%s/quantum/string";
    
    /**
     * 下载联想词服务访问URL路径
     */
    private static String sDOWNLOAD_SUGGEST_URL = "http://%s/suggest_lexicon";
    
    /**
     * 查询服务访问URL路径
     */
    private static String sQUERY_URL = "http://%s/cormorant/local";
    
    /**
     * 第三方接口代理服务访问URL路径
     */
    private static String sPROXY_URL = "http://%s//thirdparty/13";
    
    /**
     * 酒店订单服务访问URL路径
     */
    private static String sHOTEL_ORDER_URL = "http://%s//hotelorder/13";
    
    /**
     * 定位服务访问URL路径
     */
    private static String sLOCATION_URL = "http://%s/tk_locate_me";
    
    /**
     * 账户管理服务访问URL路径
     */
    private static String sACCOUNT_MANAGE_URL = "http://%s/melon/user"; // http://192.168.11.174:8100/melon/user
    
    /**
     * 软件登录服务访问URL路径
     */
    private static String sBOOTSTRAP_URL = "http://%s/bootstrap/local";
    
    /**
     * 图片上传服务访问URL路径
     */
    private static String sIMAGE_UPLOAD_URL = "http://%s/hornet/upload";

    /**
     * 消息通知服务访问URL路径
     */
    private static String sNOTICE_URL = "http://%s/notice/13";

    /**
     * 帮助服务访问URL路径
     */
    private static String sHELP_URL = "http://%s/help/13";

    /**
     * 文件下载服务访问URL路径
     */
    private static String sFILE_DOWNLOAD_URL = "http://%s/download/13";
    
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
     * 默认软件登录服务器的Host列表
     */
    private static String[] sBOOTSTRAP_HOST_LIST = new String[]{"init.tigerknows.net", "chshh.tigerknows.com", "csh.laohubaodian.net"};
    
    /** 
     * 默认图片上传服务器的Host
     */
    private static String sIMAGE_UPLOAD_HOST = "up.tigerknows.net";
    
    /**
     * 默认消息通知的Host
     */
    private static String sNOTICE_HOST = "notice.tigerknows.net";
    
    /**
     * 默认帮助服务的Host
     */
    private static String sHELP_HOST = "help.laohubaodian.net";
    
    /**
     * 默认文件下载服务的Host
     */
    private static String sFILE_DOWNLOAD_HOST = "down.tigerknows.net";
    
    /**
     * 默认第三方接口的Host
     */
    private static String sPROXY_QUERY_HOST = "search.tigerknows.net";
    
    /**
     * 默认酒店订单的Host
     */
    private static String sHOTEL_ORDER_HOST = "search.tigerknows.net";

    /**
     * 软件登录服务推送用于动态负载均衡的下载服务器Host
     */
    private static String sDYNAMIC_DOWNLOAD_HOST;
    
    /**
     * 软件登录服务推送用于动态负载均衡的查询服务器Host
     */
    private static String sDYNAMIC_QUERY_HOST;
    
    /**
     * 软件登录服务推送用于动态负载均衡的定位服务器Host
     */
    private static String sDYNAMIC_LOCATION_HOST;
    
    /**
     * 软件登录服务推送用于动态负载均衡的账户管理服务器Host
     */
    private static String sDYNAMIC_ACCOUNT_MANAGE_HOST;
    
    /**
     * 分页大小
     */
    private static int sPage_Size = 20;
    
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
     * 是否采用支付宝快捷支付逻辑
     */
    public static final String PREFS_CLIENT_GO_ALIPAY = "prefs_client_go_alipay";
    
    /**
     * 上次上传applist的时间戳
     */
    public static final String PREFS_LAST_UPLOAD_APPLIST = "prefs_last_upload_applist";
    
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
     * 显示过"点击指北针重置地图"的次数
     */
    public static final String PREFS_SHOW_RESET_MAP_TIP_TIMES = "prefs_show_reset_map_tip_times";

    /**
     * 当前软件版本
     */
    public static final String PREFS_VERSION_NAME = "prefs_version_name";
    
    /**
     * 是否为升级后使用本软件
     */
    public static final String PREFS_UPGRADE = "prefs_learn_5.8.2";    
    
    /**
     * 是否为第一次安装使用本软件
     */
    public static final String PREFS_FIRST_USE = "prefs_learn_5.8.1";    
    
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
     * 是否已显示POI主页界面的用户引导
     */
    public static final String PREFS_HINT_HOME_DRAG = "prefs_hint_home_drag";

    /**
     * 是否已显示POI主页界面的酒店预订用户引导
     */
    public static final String PREFS_HINT_POI_HOME_HOTEL = "prefs_hint_poi_home_hotel";

    /**
     * 是否已显示POI主页界面的指定点搜索的引导
     */
    public static final String PREFS_HINT_POI_HOME_LOCATION = "prefs_hint_poi_home_location";

    /**
     * 是否已显示酒店主页界面的用户引导
     */
    public static final String PREFS_HINT_HOTEL_HOME = "prefs_hint_hotel_home";
    
    /**
     * 是否已显示POI详情界面的用户引导
     */
    public static final String PREFS_HINT_POI_DETAIL = "prefs_hint_poi_detail";
    
    /**
     * 是否已显示POI详情界面的微信用户引导
     */
    public static final String PREFS_HINT_POI_DETAIL_WEIXIN = "prefs_hint_poi_detail_weixin";
    
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
     * 是否已显示搜索首页界面的酒店预订引导
     */
    public static final String PREFS_HINT_POI_HOME_HOTEL_RESERVE = "prefs_hint_poi_home_hotel_reserve";
    /**
     * 是否已显示搜索首页界面的酒店预订引导
     */
    public static final String PREFS_HOTEL_ORDER_COULD_ANOMALY_EXISTS = "prefs_hotel_order_could_anomaly_exists";
    
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
     * 推送在一天中的失败次数
     */
    public static final String PREFS_RADAR_PULL_FAILED_TIMES = "prefs_radar_pull_failed_times";
    
    /**
     * 推送的触发模式
     */
    public static final String PREFS_RADAR_PULL_TRIGGER_MODE = "prefs_radar_pull_trigger_mode";
    
    /**
     * 最近一次成功提交订单所保留的姓名
     */
    public static final String PREFS_HOTEL_LAST_BOOKNAME="prefs_hotel_last_bookname";
    
    /**
     * 最近一次成功提交订单所保留的手机号
     */
    public static final String PREFS_HOTEL_LAST_MOBILE="prefs_hotel_last_mobile";
    
    /**
     * 用户是否打开过满意度评分
     */
    public static final String PREFS_SATISFY_RATE_OPENED="prefs_satisfy_rate_opened";
    
    /**
     * 用户是否打开更多页面以便于看到活动通知栏
     */
    public static final String PREFS_MORE_OPENED="prefs_more_opened";
    
    // 以下三项用来获取一个比较准确的当前时间
    /**
     * ntp校时时记录的服务器时间
     */
    public static final String PREFS_RECORDED_NTP_TIME="prefs_recorded_ntp_time";
    
    /**
     * ntp校时时记录的手机系统绝对时间
     */
    public static final String PREFS_RECORDED_SYS_ABS_TIME="prefs_recorded_sys_abs_time";
    
    /**
     * ntp校时时记录的手机系统相对时间
     */
    public static final String PREFS_RECORDED_SYS_REL_TIME="prefs_recorded_sys_rel_time";
    
    /**
     * 地图工具箱
     */
    public static final String PREFS_MAP_TOOLS = "prefs_map_tools";
    
    /**
     * 地铁图
     */
    public static final String PREFS_SUBWAY_MAP = "prefs_subway_map";
    
    /**
     * 菜单
     */
    public static final String PREFS_DISH = "prefs_dish";
    
    /**
     * 自定义热门类别
     */
    public static final String PREFS_CUSTOM_CATEGORY = "prefs_custom_category";
    
    /**
     * 自定义热门类别是否收起
     */
    public static final String PREFS_CUSTOM_FOLD = "prefs_custom_fold";
    
    /**
     * 地图上是否显示放大缩小按钮
     */
    public static final String PREFS_SHOW_ZOOM_BUTTON = "prefs_show_zoom_button";
    
    /**
     * 上次点击的交通方式按钮id
     */
    public static final String PREFS_CHECKED_TRAFFIC_RADIOBUTTON = "prefs_checked_radiobutton";
    
    /**
     * 上次通知推送的app的时间
     */
    public static final String PREFS_APP_PUSH_NOTIFY = "prefs_app_push_notify";
    
    /**
     * 下载完app后弹出消息时间间隔t
     */
    public static final String PREFS_APP_PUSH_T = "prefs_app_push_t";
    
    public static final String PREFS_HINT_HOME = "PREFS_HINT_HOME";
    public static final String PREFS_HINT_NEARBY_SEARCH = "PREFS_HINT_NEARBY_SEARCH";
    public static final String PREFS_HINT_RESULT_MAP = "PREFS_HINT_RESULT_MAP";
    public static final String PREFS_HINT_TRAFFIC_PREFERENCE = "PREFS_HINT_TRAFFIC_PREFERENCE";
    
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
     * 酒店列表图片尺寸的Key
     */
    public static final int PICTURE_HOTEL_LIST = 7;
    
    /**
     * 酒店详情图片尺寸的Key
     */
    public static final int PICTURE_HOTEL_DETAIL = 8;
    
    /**
     * 优惠券列表图片尺寸的Key
     */
    public static final int PICTURE_COUPON_LIST = 9;
    
    /**
     * 优惠券详情图片尺寸的Key
     */
    public static final int PICTURE_COUPON_DETAIL = 10;
    
    /**
     * 优惠券Logo图片尺寸的Key
     */
    public static final int PICTURE_COUPON_LOGO = 11;
    
    /**
     * 优惠券提示图片尺寸的Key
     */
    public static final int PICTURE_COUPON_HINT = 12;
    
    /**
     * 优惠券二维码尺寸的Key
     */
    public static final int PICTURE_COUPON_QRIMG = 13;
    
    /**
     * 活动通知栏尺寸的Key
     */
    public static final int PICTURE_MORE_NOTICE = 14;
    
    /**
     * 全部菜品图片尺寸的Key
     */
    public static final int PICTURE_DISH_ALL = 15;
    
    /**
     * 推荐菜图片尺寸的Key
     */
    public static final int PICTURE_DISH_RECOMMEND = 16;
    
    /**
     * 启动图片尺寸的Key
     */
    public static final int PICTURE_STARTUP_DISPLAY = 17;

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
     * 推送在网络触发模式下检测到网络变化所设置定时器的延迟时间(单位:分钟)
     */
    public static int PullServiceNetTriggerDelayTime = 5;
    
    /**
     * 是否关闭地铁图数据有效性检测
     */
    public static boolean CloseSubwayDataCheck = false;
    
    /**
     * 是否使用太阳指南针的有效性检测
     */
    public static boolean UseSunCompassCheck = false;
    
    /**
     * 是否对应用推荐进行快速测试
     */
    public static boolean UseFastAppPush = false;
    
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
        
        String hasShortCut = getPref(context, PREFS_HAS_SHORT_CUT_PREFS);
        setPref(context, PREFS_HAS_SHORT_CUT_PREFS, "3");
        if (TextUtils.isEmpty(hasShortCut)) {
            createShortCut(context);
        } else if ("1".equals(hasShortCut)) {
            delShortcut(context, context.getString(R.string.old_app_name));
            createShortCut(context);
        } else if ("2".equals(hasShortCut)) {
            delShortcut(context, context.getString(R.string.app_name));
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
            MyPhoneStateListener myPhoneStateListener = new MyPhoneStateListener();
            sTelephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            
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
                    if (wifiInfo != null) {
                        sIMEI = wifiInfo.getMacAddress();
                    }
                }
            }
            
            if (sIMEI == null) {
                sIMEI = "null";
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
        
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(context.getPackageName(), context.getPackageName() + ".LauncherActivity"));
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);//快捷方式的动作
        
        context.sendBroadcast(shortcutIntent);//发送广播
    }
    
    /**  
     * 删除程序的快捷方式  
     */  
    private static void delShortcut(Context context, String appName){   
        Intent shortcut = new Intent("com.android.launcher.action.UNINSTALL_SHORTCUT");   
               
        //快捷方式的名称   
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, appName);   
               
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(context.getPackageName(), context.getPackageName() + ".Sphinx"));
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);//快捷方式的动作  
               
        context.sendBroadcast(shortcut);   
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
    
    public static String getConnectivityType(Context context) {
        String result = "none";
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                int type = networkInfo.getType();
                if (type == ConnectivityManager.TYPE_WIFI) {
                    return "WIFI";
                } else {
                    result = getRadioType();
                }
            }
        }
        
        return result;
    }
    public static int getPhoneType() {
        if (sTelephonyManager != null) {
            return sTelephonyManager.getPhoneType();
        }
        return TelephonyManager.PHONE_TYPE_NONE;
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
            path = getSavePath()+MAP_DATA_FOLDER_NAME+ "/";
            File file = new File(path);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    LogWrapper.e(TAG, "getDataPath() Unable to create new folder: " + path);
                }
            }
        } else {
            path = TKApplication.getInstance().getDir(RESOURCES_DATA_FOLDER_NAME, Context.MODE_PRIVATE).toString()+"/";
        }
        return path;
    }

    /**
     * 获取存储数据的文件夹路径
     * @return
     */
    public static String getSavePath() {
        String path = null;
        // 检查扩展存储卡
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            path = externalStorageDirectory.getAbsolutePath() + "/tigermap/";
            File file = new File(path);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    LogWrapper.e(TAG, "getSavePath() Unable to create new folder: " + path);
                    path = null;
                }
            }
        }
        
        // 使用应用程序数据空间
        if (path == null) {
            path = TKApplication.getInstance().getCacheDir().getAbsolutePath()+"/";
            File file = new File(path);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    LogWrapper.e(TAG, "getSavePath() Unable to create new folder: " + path);
                }
            }
        }
        return path;
    }
    
    /**
     * 获取快照文件存储路径
     * @return
     */
    public static String getSnapShotPath() {
        String path = getSavePath()+ "snapshot/";
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                LogWrapper.e(TAG, "getSnapPath() Unable to create new folder: " + path);
            }
        }
        return path;
    }
    
    /**
     * 获取测试数据文件存储路径
     * @return
     */
    public static String getTestDataPath() {
        String path = getSavePath()+ "testdata/";
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                LogWrapper.e(TAG, "getTestDataPath() Unable to create new folder: " + path);
            }
        }
        return path;
    }
    
    private static boolean hasExternalStorage(boolean requireWriteAccess) {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            if (requireWriteAccess) {
                boolean writable = checkExternalStorageFsWritable();
                return writable;
            } else {
                return true;
            }
        } else if (!requireWriteAccess
                && Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private static boolean checkExternalStorageFsWritable() {
        // Create a temporary file to see whether a volume is really writeable.
        // It's important not to put it in the root directory which may have a
        // limit on the number of files.
        String directoryName =
                Environment.getExternalStorageDirectory().toString() + "/tigermap";
        File directory = new File(directoryName);
        if (!directory.isDirectory()) {
            if (!directory.mkdirs()) {
                return false;
            }
        }
        File f = new File(directoryName, ".probe");
        try {
            // Remove stale file if any
            if (f.exists()) {
                f.delete();
            }
            if (!f.createNewFile()) {
                return false;
            }
            f.delete();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
    
    private static int calculateExternalStorageRemaining() {
        try {
            if (!hasExternalStorage(true)) {
                return NO_STORAGE_ERROR;
            } else {
                String storageDirectory =
                        Environment.getExternalStorageDirectory().toString();
                StatFs stat = new StatFs(storageDirectory);
                final int MIN_BYTES = 2 * 1024 * 1024;
                float remaining = ((float) stat.getAvailableBlocks()
                        * (float) stat.getBlockSize()) / MIN_BYTES;
                return (int) remaining;
            }
        } catch (Exception ex) {
            // if we can't stat the filesystem then we don't know how many
            // pictures are remaining.  it might be zero but just leave it
            // blank since we really don't know.
            return CANNOT_STAT_ERROR;
        }
    }

    public static int checkStorageSize(final Activity activity) {
        final int remaining = calculateExternalStorageRemaining();
        if (activity != null && remaining >= 0 && remaining <= 8) {
            activity.runOnUiThread(new Runnable() {
    
                @Override
                public void run() {
                    Toast.makeText(activity, R.string.sd_not_enough_space, 10*1000).show();
                }
            });
        }
        return remaining;
    }
    
    /**
     * 获取引导服务访问的URL
     * @return
     */
    public static String getBootstarpUrl() {
        return sBOOTSTRAP_URL;
    }
    
    /**
     * 获取图片上传服务访问的URL
     * @return
     */
    public static String getImageUploadUrl() {
        return sIMAGE_UPLOAD_URL;
    }
    
    /**
     * 获取消息通知服务的URL
     * @return
     */
    public static String getNoticeUrl() {
    	return sNOTICE_URL;
    }
    
    /**
     * 获取消息通知服务的Host
     * @return
     */
    public static String getNoticeHost() {
    	return sNOTICE_HOST;
    }
    
    /**
     * 获取帮助服务的URL
     * @return
     */
    public static String getHelpUrl() {
        return sHELP_URL;
    }
    
    /**
     * 获取帮助服务的Host
     * @return
     */
    public static String getHelpHost() {
        return sHELP_HOST;
    }
    
    /**
     * 获取文件下载服务的URL
     * @return
     */
    public static String getFileDownloadUrl() {
        return sFILE_DOWNLOAD_URL;
    }
    
    /**
     * 获取文件下载服务的Host
     * @return
     */
    public static String getFileDownloadHost() {
        return sFILE_DOWNLOAD_HOST;
    }
    
    /**
     * 获取引导服务器Host列表
     * @return
     */
    public static String[] getBootStrapHostList() {
        return sBOOTSTRAP_HOST_LIST;
    }
    
    /**
     * 获取图片上传服务器Host
     * @return
     */
    public static String getImageUploadHost() {
        return sIMAGE_UPLOAD_HOST;
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
     * 获取第三方接口代理服务访问的URL
     * @return
     */
    public static String getProxyUrl() {
        return sPROXY_URL;
    }
    
    /**
     * 获取酒店订单服务访问的URL
     * @return
     */
    public static String getHotelOrderUrl() {
        return sHOTEL_ORDER_URL;
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
     * 获取动态负载均衡的定位服务器Host
     * @return
     */
    public static String getDynamicLocationHost() {
        return sDYNAMIC_LOCATION_HOST;
    }
    
    /**
     * 设置动态负载均衡的定位服务器Host
     * @param host
     */
    public static void setDynamicLocationHost(String host) {
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
     * 获取动态负载均衡的查询服务器Host
     * @return
     */
    public static String getDynamicQueryHost() {
        return sDYNAMIC_QUERY_HOST;
    }
    
    /**
     * 设置动态负载均衡的查询服务器Host
     * @param host
     */
    public static void setDynamicQueryHost(String host) {
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
     * 获取动态负载均衡的账户管理服务器Host
     * @return
     */
    public static String getDynamicAccountManageHost() {
        return sDYNAMIC_ACCOUNT_MANAGE_HOST;
    }
    
    /**
     * 设置动态负载均衡的账户管理服务器Host
     * @param host
     */
    public static void setDynamicAccountManageHost(String host) {
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
     * 获取第三方接口Host，如果有动态负载均衡的查询服务器Host则返回该值
     */
    public static String getProxyQueryHost(){
//    	if (!TextUtils.isEmpty(sDYNAMIC_QUERY_HOST)){
//    		return sDYNAMIC_QUERY_HOST;
//    	}
    	return sPROXY_QUERY_HOST;
    }
    
    /**
     * 获取酒店订单Host，如果有动态负载均衡的查询服务器Host则返回该值
     */
    public static String getHotelOrderHost(){
    	if (!TextUtils.isEmpty(sDYNAMIC_QUERY_HOST)){
    		return sDYNAMIC_QUERY_HOST;
    	}
    	return sHOTEL_ORDER_HOST;
    }
    
    /**
     * 获取动态负载均衡的下载服务器Host
     * @return
     */
    public static String getDynamicDownloadHost() {
        return sDYNAMIC_DOWNLOAD_HOST;
    }
    
    /**
     * 设置动态负载均衡的下载服务器Host
     * @param host
     */
    public static void setDynamicDownloadHost(String host) {
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
    	if(TextUtils.isEmpty(sIMEI))return "0";
        return sIMEI;
    }
    
    /**
     * 获取网络请求头中的User-Agent值
     * @return
     */
    public static String getUserAgent() {
        return sCLIENT_SOFT_VERSION;
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
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_PRIVATE);
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
        LogWrapper.d("prefs", "set pref " + name + ":" + value);
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_PRIVATE);
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
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_PRIVATE);
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
     * 判断其Pref的值是否为1
     * @param context
     * @param name
     * @return
     */
    public static boolean isPref(Context context, String name) {
        boolean result = false;
        if (context == null || TextUtils.isEmpty(name)) {
            return result;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_PRIVATE);
        try {
            String value = sharedPreferences.getString(name, null);
            if ("1".equals(value)) {
                result = true;
            } else {
                result = false;
            }
        } catch (Exception e) {
            
        }
        return result;
    }

    /**
     * get preference
     * @param 名称
     * @defaultValue 默认值
     */
    public static String reversePref(Context context, String name) {
        String value = null;
        if (context == null || TextUtils.isEmpty(name)) {
            return value;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(TKConfig.TIGERKNOWS_PREFS, Context.MODE_PRIVATE);
        try {
            value = sharedPreferences.getString(name, null);
            if ("1".equals(value)) {
                value = "";
            } else {
                value = "1";
            }
            Editor editor = sharedPreferences.edit();
            editor.putString(name, value).commit();
        } catch (Exception e) {
            Editor editor = sharedPreferences.edit();
            editor.remove(name).commit();
            editor.putString(name, value).commit();
        }
        return value;
    }
    
    /**
     * 上传图片的最大高/宽
     */
    public static int Photo_Max_Width_Height = 800;
    
    /**
     * 图片的压缩比率
     */
    public static int Photo_Compress_Ratio = 80;
    
    /**
     * 读取配置文件(地图数据文件存储路径/config.txt)
     * 设置参数
     */
    public static void readConfig() {
        String mapPath = TKConfig.getDataPath(true);
        if (TextUtils.isEmpty(mapPath) || ShowTestOption == false) {
            return;
        }
        File file = new File(mapPath+"config.txt");
        if (file.exists()) {
            try {
            	// Warning: 这一段只能从尾部新增代码，不可删除或从中间插入
                FileInputStream fis = new FileInputStream(file);
                String text = Utility.readFile(fis);
                fis.close();
                int start = text.indexOf("downloadHost=");
                int end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "downloadHost=".length();
                    TKConfig.sDEFAULT_DOWNLOAD_HOST = text.substring(start, end);
                }
                start = text.indexOf("queryHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "queryHost=".length();
                    TKConfig.sDEFAULT_QUERY_HOST = text.substring(start, end);
                }
                start = text.indexOf("locationHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "locationHost=".length();
                    TKConfig.sDEFAULT_LOCATION_HOST = text.substring(start, end);
                }
                start = text.indexOf("accountManageHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "accountManageHost=".length();
                    TKConfig.sDEFAULT_ACCOUNT_MANAGE_HOST = text.substring(start, end);
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
                    TKConfig.LoadBalance = text.substring(start, end).equals("true");
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
                start = text.indexOf("bootstrapUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "bootstrapUrl=".length();
                    sBOOTSTRAP_URL = text.substring(start, end);
                }
                start = text.indexOf("bootstrapHostList=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "bootstrapHostList=".length();
                    sBOOTSTRAP_HOST_LIST = text.substring(start, end).split(",");
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
                start = text.indexOf("proxyUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "proxyUrl=".length();
                    TKConfig.sPROXY_URL = text.substring(start, end);
                }
                start = text.indexOf("hotelOrderUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "hotelOrderUrl=".length();
                    TKConfig.sHOTEL_ORDER_URL = text.substring(start, end);
                }
                start = text.indexOf("imageUploadHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "imageUploadHost=".length();
                    TKConfig.sIMAGE_UPLOAD_HOST = text.substring(start, end);
                }
                start = text.indexOf("imageUploadUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "imageUploadUrl=".length();
                    TKConfig.sIMAGE_UPLOAD_URL = text.substring(start, end);
                }
                start = text.indexOf("Photo_Max_Width_Height=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "Photo_Max_Width_Height=".length();
                    TKConfig.Photo_Max_Width_Height = Integer.valueOf(text.substring(start, end));
                }
                start = text.indexOf("Photo_Compress_Ratio=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "Photo_Compress_Ratio=".length();
                    TKConfig.Photo_Compress_Ratio = Integer.valueOf(text.substring(start, end));
                }
                start = text.indexOf("noticeUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "noticeUrl=".length();
                    TKConfig.sNOTICE_URL = text.substring(start, end);
                }
                start = text.indexOf("noticeHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "noticeHost=".length();
                    TKConfig.sNOTICE_HOST = text.substring(start, end);
                }
                start = text.indexOf("helpUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "helpUrl=".length();
                    TKConfig.sHELP_URL = text.substring(start, end);
                }
                start = text.indexOf("helpHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "helpHost=".length();
                    TKConfig.sHELP_HOST = text.substring(start, end);
                }
                start = text.indexOf("fileDownloadUrl=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "fileDownloadUrl=".length();
                    TKConfig.sFILE_DOWNLOAD_URL = text.substring(start, end);
                }
                start = text.indexOf("fileDownloadHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "fileDownloadHost=".length();
                    TKConfig.sFILE_DOWNLOAD_HOST = text.substring(start, end);
                }
                start = text.indexOf("proxyQueryHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "proxyQueryHost=".length();
                    TKConfig.sPROXY_QUERY_HOST = text.substring(start, end);
                }
                start = text.indexOf("hotelOrderHost=");
                end = text.indexOf(";", start);
                if (start > -1 && end > -1) {
                    start += "hotelOrderHost=".length();
                    TKConfig.sHOTEL_ORDER_HOST = text.substring(start, end);
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
    
    /**
     * 获取记录地铁数据version的prefs标识
     */
    public static String getSubwayMapVersionPrefs(int cityId) {
        String cityEName = MapEngine.getCityInfo(cityId).getEName();
        return "prefs_subwaymap_version_" + cityEName;
    }
    
    /**
     * 获取记录地铁数据完整性验证码的prefs标识
     */
    public static String getSubwayMapSizePrefs(int cityId) {
        String cityEName = MapEngine.getCityInfo(cityId).getEName();
        return "prefs_subwaymap_size_" + cityEName;
    }
    
    /**
     * 获取地铁数据是否是升级来的prefs标识
     * @param cityId
     * @return
     */
    public static String getSubwayMapUpdatedPrefs(int cityId) {
        String cityEName = MapEngine.getCityInfo(cityId).getEName();
        return "prefs_subwaymap_updated_" + cityEName;
    }
}
