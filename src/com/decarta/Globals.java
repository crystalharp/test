package com.decarta;


import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.HashMap;

import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYInteger;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.common.AsyncImageLoader;
import com.tigerknows.common.ImageCache;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.Session;
import com.tigerknows.model.User;
import com.tigerknows.model.AccountManage.UserRespnose;
import com.tigerknows.util.Utility;

public class Globals {
    
	/**
	 * 暂无定位信息
	 */
    public static final int LOCATION_STATE_NONE = 0;
    
    /**
     * 打开软件来第一次定位成功
     */
    public static final int LOCATION_STATE_FIRST_SUCCESS = 1;
    
    /**
     * 定位失败
     */
    public static final int LOCATION_STATE_FAILED = 2;
    
    /**
     * 定位成功后，已经显示出切换城市对话框
     */
    public static final int LOCATION_STATE_SHOW_CHANGE_CITY_DIALOG = 3;
    
    /**
     * 屏幕相关参数
     */
	public static DisplayMetrics g_metrics=new DisplayMetrics();
	
	/**
	 * 当前所选城市信息
	 */
	private static CityInfo g_Current_City_Info = null;
    
    public static void setCurrentCityInfo(CityInfo cityInfo) {
        g_Current_City_Info = cityInfo;
    }
    
    public static CityInfo getCurrentCityInfo() {
        return getCurrentCityInfo(true);
    }
    
    public static CityInfo getCurrentCityInfo(boolean query) {
        CityInfo currentCityInfo = g_Current_City_Info;
        CityInfo hotelCityInfo = g_Hotel_City_Info;
        CityInfo cityInfo = null;
        if (query && hotelCityInfo != null && hotelCityInfo.isAvailably()) {
            cityInfo = hotelCityInfo;
        } else if (currentCityInfo != null && currentCityInfo.isAvailably()){
            cityInfo = currentCityInfo;
        } else {
            // "北京,beijing,39.90415599,116.397772995,11, 北京,beijing"
            cityInfo = new CityInfo();
            cityInfo.setCName("beijing");
            cityInfo.setCProvinceName("beijing");
            cityInfo.setEName("beijing");
            cityInfo.setEProvinceName("beijing");
            cityInfo.setId(1);
            cityInfo.setPosition(new Position(39.90415599,116.397772995));
            cityInfo.setLevel(11);
        }
        return cityInfo;
    }
    
    /**
     * 酒店查询的目标城市
     * 打开酒店页面时，默认设为当前所选城市
     * 用户在此页面切换酒店查询的目标城市时并不影响当前所选城市
     */
    private static CityInfo g_Hotel_City_Info = null;
    
    public static void setHotelCityInfo(CityInfo cityInfo) {
        g_Hotel_City_Info = cityInfo;
    }
    
    public static CityInfo getHotelCityInfo() {
        return g_Hotel_City_Info;
    }
    
    /**
     * 定位信息
     */
    public static Location g_My_Location = null;
    
    /**
     * 定位城市信息
     */
    public static CityInfo g_My_Location_City_Info = null;
    public static int g_My_Location_State = 0;
    public static BootstrapModel g_Bootstrap_Model = null;
    private static AsyncImageLoader sAsyncImageLoader = new AsyncImageLoader();
    private static ImageCache sImageCache = new ImageCache();
    /**
     * 当前数据网络的传输速度是否为快速
     */
    private static boolean sConnectionFast = false;
    private static HashMap<XYInteger, HashMap<Integer, XYInteger>> sScreenAdaptPic = new HashMap<XYInteger, HashMap<Integer, XYInteger>>();
    private static XYInteger sOptimalAdaptiveScreenSize = new XYInteger(480, 800);
    private static HashMap<Integer, XYInteger> sOptimalAdaptive = new HashMap<Integer, XYInteger>();

    public static String g_ClientUID = null; 
    
    // 用户登录后的SessionId
    public static String g_Session_Id = null;
    // 用户登录后的用户信息
    public static User g_User = null;
    
    private static Object userLock = new Object();
    
    public static int g_ApiVersion = android.os.Build.VERSION.SDK_INT;
    
    static {    	
    	HashMap<Integer, XYInteger> pic = new HashMap<Integer, XYInteger>();
    	pic.put(TKConfig.PICTURE_DISCOVER_HOME, new XYInteger(538, 478));
        pic.put(TKConfig.PICTURE_TUANGOU_LIST, new XYInteger(312, 180));
        pic.put(TKConfig.PICTURE_DIANYING_LIST, new XYInteger(162, 220));
        pic.put(TKConfig.PICTURE_TUANGOU_DETAIL, new XYInteger(688, 416));
        pic.put(TKConfig.PICTURE_TUANGOU_TAOCAN, new XYInteger(688, 0));
        pic.put(TKConfig.PICTURE_DIANYING_DETAIL, new XYInteger(312, 416));
        pic.put(TKConfig.PICTURE_HOTEL_LIST, new XYInteger(180, 180));
        pic.put(TKConfig.PICTURE_HOTEL_DETAIL, new XYInteger(150, 150));
        pic.put(TKConfig.PICTURE_COUPON_LIST, new XYInteger(132, 132));
        pic.put(TKConfig.PICTURE_COUPON_DETAIL, new XYInteger(700, 790));
        pic.put(TKConfig.PICTURE_COUPON_LOGO, new XYInteger(175, 37));
        pic.put(TKConfig.PICTURE_COUPON_HINT, new XYInteger(147, 147));
        pic.put(TKConfig.PICTURE_COUPON_QRIMG, new XYInteger(383, 383));
        pic.put(TKConfig.PICTURE_MORE_NOTICE, new XYInteger(688, 80));
    	sScreenAdaptPic.put(new XYInteger(800, 1280), pic);

        sOptimalAdaptive.put(TKConfig.PICTURE_DISCOVER_HOME, new XYInteger(322, 286));
        sOptimalAdaptive.put(TKConfig.PICTURE_TUANGOU_LIST, new XYInteger(188, 108));
        sOptimalAdaptive.put(TKConfig.PICTURE_DIANYING_LIST, new XYInteger(98, 132));
        sOptimalAdaptive.put(TKConfig.PICTURE_TUANGOU_DETAIL, new XYInteger(412, 250));
        sOptimalAdaptive.put(TKConfig.PICTURE_TUANGOU_TAOCAN, new XYInteger(412, 0));
        sOptimalAdaptive.put(TKConfig.PICTURE_DIANYING_DETAIL, new XYInteger(188, 250));
        sOptimalAdaptive.put(TKConfig.PICTURE_HOTEL_LIST, new XYInteger(108, 108));
        sOptimalAdaptive.put(TKConfig.PICTURE_HOTEL_DETAIL, new XYInteger(90, 90));
        sOptimalAdaptive.put(TKConfig.PICTURE_COUPON_LIST, new XYInteger(80, 80));
        sOptimalAdaptive.put(TKConfig.PICTURE_COUPON_DETAIL, new XYInteger(420, 474));
        sOptimalAdaptive.put(TKConfig.PICTURE_COUPON_LOGO, new XYInteger(105, 22));
        sOptimalAdaptive.put(TKConfig.PICTURE_COUPON_HINT, new XYInteger(88, 88));
        sOptimalAdaptive.put(TKConfig.PICTURE_COUPON_QRIMG, new XYInteger(230, 230));
        sOptimalAdaptive.put(TKConfig.PICTURE_MORE_NOTICE, new XYInteger(412, 48));
        sScreenAdaptPic.put(new XYInteger(480, 800), sOptimalAdaptive);
        
        pic = new HashMap<Integer, XYInteger>();
        pic.put(TKConfig.PICTURE_DISCOVER_HOME, new XYInteger(215, 191));
        pic.put(TKConfig.PICTURE_TUANGOU_LIST, new XYInteger(126, 72));
        pic.put(TKConfig.PICTURE_DIANYING_LIST, new XYInteger(66, 88));
        pic.put(TKConfig.PICTURE_TUANGOU_DETAIL, new XYInteger(276, 168));
        pic.put(TKConfig.PICTURE_TUANGOU_TAOCAN, new XYInteger(276, 0));
        pic.put(TKConfig.PICTURE_DIANYING_DETAIL, new XYInteger(126, 168));
        pic.put(TKConfig.PICTURE_HOTEL_LIST, new XYInteger(72, 72));
        pic.put(TKConfig.PICTURE_HOTEL_DETAIL, new XYInteger(60, 60));
        pic.put(TKConfig.PICTURE_COUPON_LIST, new XYInteger(52, 52));
        pic.put(TKConfig.PICTURE_COUPON_DETAIL, new XYInteger(280, 318));
        pic.put(TKConfig.PICTURE_COUPON_LOGO, new XYInteger(70, 15));
        pic.put(TKConfig.PICTURE_COUPON_HINT, new XYInteger(59, 59));
        pic.put(TKConfig.PICTURE_COUPON_QRIMG, new XYInteger(153, 153));
        pic.put(TKConfig.PICTURE_MORE_NOTICE, new XYInteger(276, 32));
        sScreenAdaptPic.put(new XYInteger(320, 480), pic);
    }
    
    /**
     * 重置当前所选城市、定位信息
     * @param context
     */
    public static void init(Context context) {
        Globals.g_Current_City_Info = null;
        Globals.g_My_Location_City_Info = null;
        Globals.g_My_Location = null;
        Globals.g_My_Location_State = LOCATION_STATE_NONE;
        
        Globals.readSessionAndUser(context);
        Globals.setConnectionFast(Utility.isConnectionFast(context));
    }

    public static AsyncImageLoader getAsyncImageLoader() {
        return sAsyncImageLoader;
    }

    public static ImageCache getImageCache() {
        return sImageCache;
    }

    public static boolean isConnectionFast() {
        return sConnectionFast;
    }

    public static void setConnectionFast(boolean isConnectionFast) {
        Globals.sConnectionFast = isConnectionFast;
    }    
    
    public static void initOptimalAdaptiveScreenSize() {
        int diff320 = Math.abs(Globals.g_metrics.widthPixels-320);
        int diff480 = Math.abs(Globals.g_metrics.widthPixels-480);
        int diff800 = Math.abs(Globals.g_metrics.widthPixels-800);
        int min = Math.min(Math.min(diff320, diff480), diff800);
        if (diff320 == min) {
            sOptimalAdaptiveScreenSize.x = 320;
            sOptimalAdaptiveScreenSize.y = 480;
        } else if (diff480 == min) {
            sOptimalAdaptiveScreenSize.x = 480;
            sOptimalAdaptiveScreenSize.y = 800;
        } else {
            sOptimalAdaptiveScreenSize.x = 800;
            sOptimalAdaptiveScreenSize.y = 1280;
        }
        sOptimalAdaptive = sScreenAdaptPic.get(sOptimalAdaptiveScreenSize);
    }
    
    public static String getPicWidthHeight(int key) {
        XYInteger xyInteger = sOptimalAdaptive.get(key);
        return xyInteger.x+"_"+xyInteger.y;
    }
    
    /**
     * 读取会话信息（SessionId）和用户信息（UserId、昵称）
     * @param context
     */
    public static void readSessionAndUser(Context context) {
        synchronized (userLock) {
            if (g_Session_Id != null && g_User != null) {
                return;
            }
            Session session = Session.loadDefault(context);
            if (session != null && session.isValid()) {
                g_Session_Id = session.getSessionId();
            }
            g_User = User.loadDefault(context);
            if (TextUtils.isEmpty(g_Session_Id) || g_User == null) {
                g_Session_Id = null;
                g_User = null;
            }       
        }
    }
    
    /**
     * 存储会话信息（SessionId）和用户信息（UserId、昵称）
     * @param context
     * @param userResponse
     */
    public static void storeSessionAndUser(Context context, UserRespnose userResponse) {
        synchronized (userLock) {
            Session session = new Session(userResponse.getSessionId(), userResponse.getTimeout());
            session.store(context);
            Globals.g_Session_Id = session.getSessionId();
            
            String nickName = (TextUtils.isEmpty(userResponse.getNickname())) ? context.getString(R.string.default_nick_name) : userResponse.getNickname();
            User user = new User(nickName, userResponse.getUserId());
            user.store(context);
            Globals.g_User = user;
            
            if (TextUtils.isEmpty(g_Session_Id) || g_User == null) {
                g_Session_Id = null;
                g_User = null;
            } 
        }
    }
    
    /**
     * 清除会话信息（SessionId）和用户信息（UserId、昵称）
     * @param context
     */
    public static void clearSessionAndUser(Context context) {
        synchronized (userLock) {
            Session session = Session.loadDefault(context);
            if (session != null) {
                session.clear(context);
            }
            Globals.g_Session_Id = null;
            
            User user = User.loadDefault(context);
            if (user != null) {
                user.clear(context);
            }
            Globals.g_User = null;
        }
    }
        
    /**
     * 获取最近所选的城市信息
     * @param context
     * @return 当第一次安装使用时，因为还没有选择某个城市所以返回无效的城市信息
     */
    public static CityInfo getLastCityInfo(Context context) {
        CityInfo cityInfo = g_Current_City_Info;
        if (cityInfo == null || cityInfo.isAvailably() == false) {
            double lastLon = Double.parseDouble(TKConfig.getPref(context, TKConfig.PREFS_LAST_LON, "361"));
            double lastLat = Double.parseDouble(TKConfig.getPref(context, TKConfig.PREFS_LAST_LAT, "361"));
            int lastZoomLevel = Integer.parseInt(TKConfig.getPref(context, TKConfig.PREFS_LAST_ZOOM_LEVEL, String.valueOf(TKConfig.ZOOM_LEVEL_DEFAULT)));

            Position lastPosition = new Position(lastLat, lastLon);
            if(Util.inChina(lastPosition)) {
                MapEngine mapEngine = MapEngine.getInstance();
                try {
                    mapEngine.initMapDataPath(context);
                    int cityId = mapEngine.getCityId(lastPosition);
                    cityInfo = mapEngine.getCityInfo(cityId);
                    cityInfo.setPosition(lastPosition);
                    cityInfo.setLevel(lastZoomLevel);
                } catch (APIException e) {
                    e.printStackTrace();
                }
            }
        }
        return cityInfo;
    }
}
