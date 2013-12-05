package com.decarta;


import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.regex.Pattern;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.common.AsyncImageLoader;
import com.tigerknows.common.ImageCache;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.Session;
import com.tigerknows.model.User;
import com.tigerknows.model.AccountManage.UserRespnose;
import com.tigerknows.util.Utility;
import com.tigerknows.util.XYInteger;

public class Globals {
    
	/**
	 * 暂无定位信息
	 */
    public static final int LOCATION_STATE_NONE = 0;
    
    /**
     * 打开软件之后第一次定位成功
     */
    public static final int LOCATION_STATE_FIRST_SUCCESS = 1;
    
    /**
     * 打开软件之后一直定位失败，没有成功过
     */
    public static final int LOCATION_STATE_AT_YET_FAILED = 2;
    
    /**
     * 定位失败
     */
    public static final int LOCATION_STATE_FAILED = 3;
    
    /**
     * 定位成功后，已经显示出切换城市对话框
     */
    public static final int LOCATION_STATE_SHOW_CHANGE_CITY_DIALOG = 4;
    
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
            cityInfo.setCName("北京");
            cityInfo.setCProvinceName("北京");
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
    /**
     * 当前数据网络的传输速度是否为快速
     */
    private static boolean sConnectionFast = false;
    private static HashMap<XYInteger, HashMap<Integer, XYInteger>> sScreenAdaptPic = new HashMap<XYInteger, HashMap<Integer, XYInteger>>();
    private static XYInteger sOptimalAdaptiveScreenSize = new XYInteger(480, 800);
    private static HashMap<Integer, XYInteger> sOptimalAdaptive = new HashMap<Integer, XYInteger>();

    public static String g_ClientUID = null; 
    
    public static boolean init = false;
    
    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     * @return The number of cores, or 1 if failed to get result
     */
    private static int getNumCores() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if(Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }      
        }

        try {
            //Get directory containing CPU info
        	File dir = new File("/sys/devices/system/cpu/");
        	//Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            LogWrapper.d("Globals", "CPU Count: "+files.length);
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch(Exception e) {
            //Print exception
        	LogWrapper.d("Globals", "CPU Count: Failed.");
            e.printStackTrace();
            //Default to return 1 core
            return 1;
        }
    }
    public static int g_cpuNum = getNumCores();
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
        pic.put(TKConfig.PICTURE_DISH_ALL, new XYInteger(144, 144));
        pic.put(TKConfig.PICTURE_DISH_RECOMMEND, new XYInteger(300, 162));
        pic.put(TKConfig.PICTURE_STARTUP_DISPLAY, new XYInteger(480, 800));
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
        sOptimalAdaptive.put(TKConfig.PICTURE_DISH_ALL, new XYInteger(96, 96));
        sOptimalAdaptive.put(TKConfig.PICTURE_DISH_RECOMMEND, new XYInteger(200, 108));
        sOptimalAdaptive.put(TKConfig.PICTURE_STARTUP_DISPLAY, new XYInteger(480, 800));
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
        pic.put(TKConfig.PICTURE_DISH_ALL, new XYInteger(64, 64));
        pic.put(TKConfig.PICTURE_DISH_RECOMMEND, new XYInteger(134, 72));
        pic.put(TKConfig.PICTURE_STARTUP_DISPLAY, new XYInteger(720, 1280));
        sScreenAdaptPic.put(new XYInteger(320, 480), pic);
    }
    
    /**
     * 重置当前所选城市、定位信息、图片尺寸
     * @param activity
     */
    public static void init(Activity activity) {
        
        initDataPath(activity);
        
        AsyncImageLoader.getInstance().onCreate(activity);
        
        if (init) {
            return;
        }
        init = true;
        
        Globals.g_Current_City_Info = null;
        Globals.g_My_Location_City_Info = null;
        Globals.g_My_Location = null;
        Globals.g_My_Location_State = LOCATION_STATE_NONE;
        
        Globals.readSessionAndUser(activity);
        Globals.setConnectionFast(Utility.isConnectionFast(activity));

        // 读取屏幕参数，如高宽、密度
        WindowManager winMan=(WindowManager)activity.getSystemService(Context.WINDOW_SERVICE);
        Display display=winMan.getDefaultDisplay();
        display.getMetrics(Globals.g_metrics);
        
        initOptimalAdaptiveScreenSize();
    }
    
    public static void initDataPath(Activity activity) {
        try {
            MapEngine.getInstance().initMapDataPath(activity);
            ImageCache.getInstance().init(activity);
        } catch (APIException e) {
            e.printStackTrace();
            Utility.showDialogAcitvity(activity, activity.getString(R.string.not_enough_space_and_please_clear));
            activity.finish();
        }
    }

    public static boolean isConnectionFast() {
        return sConnectionFast;
    }

    public static void setConnectionFast(boolean isConnectionFast) {
        Globals.sConnectionFast = isConnectionFast;
    }    
    
    private static void initOptimalAdaptiveScreenSize() {
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
            int lastZoomLevel = MapEngine.DEFAULT_CITY_LEVEL;//Integer.parseInt(TKConfig.getPref(context, TKConfig.PREFS_LAST_ZOOM_LEVEL, String.valueOf(TKConfig.ZOOM_LEVEL_DEFAULT)));

            Position lastPosition = new Position(lastLat, lastLon);
            if(Util.inChina(lastPosition)) {
                int cityId = MapEngine.getCityId(lastPosition);
                cityInfo = MapEngine.getCityInfo(cityId);
                cityInfo.setPosition(lastPosition);
                cityInfo.setLevel(lastZoomLevel);
            }
        }
        return cityInfo;
    }
}
