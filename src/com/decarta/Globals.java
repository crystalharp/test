package com.decarta;


import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.HashMap;

import com.decarta.android.util.XYInteger;
import com.tigerknows.ImageCache;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.model.UserLogonModel;
import com.tigerknows.model.AccountManage.UserRespnose;
import com.tigerknows.util.AsyncImageLoader;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.view.user.Session;
import com.tigerknows.view.user.User;

public class Globals {
    
    public static final int LOCATION_STATE_NONE = 0;
    public static final int LOCATION_STATE_FIRST_SUCCESS = 1;
    public static final int LOCATION_STATE_SHOW_CHANGE_CITY_DIALOG = 2;
    
	public static DisplayMetrics g_metrics=new DisplayMetrics();
    public static CityInfo g_Current_City_Info = new CityInfo();
    public static Location g_My_Location = null;
    public static CityInfo g_My_Location_City_Info = null;
    public static int g_My_Location_State = 0;
    public static UserLogonModel g_User_Logon_Model = null;
    private static AsyncImageLoader sAsyncImageLoader = new AsyncImageLoader();
    private static ImageCache sImageCache = new ImageCache();
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
    	sScreenAdaptPic.put(new XYInteger(800, 1280), pic);

        sOptimalAdaptive.put(TKConfig.PICTURE_DISCOVER_HOME, new XYInteger(322, 286));
        sOptimalAdaptive.put(TKConfig.PICTURE_TUANGOU_LIST, new XYInteger(188, 108));
        sOptimalAdaptive.put(TKConfig.PICTURE_DIANYING_LIST, new XYInteger(98, 132));
        sOptimalAdaptive.put(TKConfig.PICTURE_TUANGOU_DETAIL, new XYInteger(412, 250));
        sOptimalAdaptive.put(TKConfig.PICTURE_TUANGOU_TAOCAN, new XYInteger(412, 0));
        sOptimalAdaptive.put(TKConfig.PICTURE_DIANYING_DETAIL, new XYInteger(188, 250));
        sScreenAdaptPic.put(new XYInteger(480, 800), sOptimalAdaptive);
        
        pic = new HashMap<Integer, XYInteger>();
        pic.put(TKConfig.PICTURE_DISCOVER_HOME, new XYInteger(215, 191));
        pic.put(TKConfig.PICTURE_TUANGOU_LIST, new XYInteger(126, 72));
        pic.put(TKConfig.PICTURE_DIANYING_LIST, new XYInteger(66, 88));
        pic.put(TKConfig.PICTURE_TUANGOU_DETAIL, new XYInteger(276, 168));
        pic.put(TKConfig.PICTURE_TUANGOU_TAOCAN, new XYInteger(276, 0));
        pic.put(TKConfig.PICTURE_DIANYING_DETAIL, new XYInteger(126, 168));
        sScreenAdaptPic.put(new XYInteger(320, 480), pic);
    }
    
    public static void init(Context context) {
        Globals.g_Current_City_Info = null;
        Globals.g_My_Location_City_Info = null;
        Globals.g_My_Location = null;
        Globals.g_My_Location_State = LOCATION_STATE_NONE;
        
        Globals.readSessionAndUser(context);
        Globals.setConnectionFast(CommonUtils.isConnectionFast(context));
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
    
    public static int getCurrentCityId() {
        int cityId = MapEngine.CITY_ID_INVALID;
        CityInfo cityInfo = g_Current_City_Info;
        if (cityInfo != null) {
            cityId = cityInfo.getId(); 
        }
        return cityId;
    }
}
