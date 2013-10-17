package com.tigerknows.model.test;

import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Comment;
import com.tigerknows.model.Coupon;
import com.tigerknows.model.DataQuery.AlternativeResponse;
import com.tigerknows.model.DataQuery.CouponResponse;
import com.tigerknows.model.DataQuery.CouponResponse.CouponList;
import com.tigerknows.model.DataQuery.FilterConfigResponse;
import com.tigerknows.model.DataQuery.DishResponse;
import com.tigerknows.model.DataQuery.PictureResponse;
import com.tigerknows.model.DataQuery.PictureResponse.PictureList;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.Dish;
import com.tigerknows.model.Hotel;
import com.tigerknows.model.Hotel.HotelTKDrawable;
import com.tigerknows.model.Hotel.RoomType;
import com.tigerknows.model.POI;
import com.tigerknows.model.POI.PresetTime;
import com.tigerknows.model.POI.Busstop;
import com.tigerknows.model.POI.SubwayExit;
import com.tigerknows.model.POI.SubwayPresetTime;
import com.tigerknows.model.PullMessage;
import com.tigerknows.model.Shangjia;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Yingxun;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.model.DataQuery.BaseList;
import com.tigerknows.model.DataQuery.DianyingResponse;
import com.tigerknows.model.DataQuery.DiscoverResponse;
import com.tigerknows.model.DataQuery.FendianResponse;
import com.tigerknows.model.DataQuery.FilterArea;
import com.tigerknows.model.DataQuery.FilterCategoryOrder;
import com.tigerknows.model.DataQuery.FilterOption;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.model.DataQuery.ShangjiaResponse;
import com.tigerknows.model.DataQuery.TuangouResponse;
import com.tigerknows.model.DataQuery.YanchuResponse;
import com.tigerknows.model.DataQuery.YingxunResponse;
import com.tigerknows.model.DataQuery.ZhanlanResponse;
import com.tigerknows.model.DataQuery.DianyingResponse.DianyingList;
import com.tigerknows.model.DataQuery.DiscoverResponse.DiscoverCategoryList;
import com.tigerknows.model.DataQuery.DiscoverResponse.DiscoverCategoryList.DiscoverCategory;
import com.tigerknows.model.DataQuery.FendianResponse.FendianList;
import com.tigerknows.model.DataQuery.DishResponse.DishList;
import com.tigerknows.model.DataQuery.POIResponse.POIList;
import com.tigerknows.model.DataQuery.ShangjiaResponse.ShangjiaList;
import com.tigerknows.model.DataQuery.TuangouResponse.TuangouList;
import com.tigerknows.model.DataQuery.YanchuResponse.YanchuList;
import com.tigerknows.model.DataQuery.YingxunResponse.YingxunList;
import com.tigerknows.model.DataQuery.ZhanlanResponse.ZhanlanList;
import com.tigerknows.model.POI.Description;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.model.PullMessage.Message;
import com.tigerknows.model.PullMessage.Message.PulledDynamicPOI;
import com.tigerknows.model.Yingxun.Changci;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XInt;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.text.TextUtils;

import java.util.List;

public class DataQueryTest {
    
    public static final String POI_UUID = "3539E497-33E3-1019-8020-E97938DD572F";

    public static XMap launchDiscoverResponse(Context context, String message, int supportType) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(DiscoverResponse.FIELD_LIST, launchDiscoverCategoryList(context, message));
        data.put(DiscoverResponse.FIELD_CONFIG, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        		"<citylist version=\"1.0.0\">" +
        		"    <city seq_id=\"1\">" +
        		"        <dp>2</dp>" +
        		"        <dp>4</dp>" +
        		"        <dp>13</dp>" +
        		"        <dp>14</dp>" +
        		"    </city>" +
        		"    <city seq_id=\"2\">" +
        		"        <dp>2</dp>" +
        		"        <dp>4</dp>" +
        		"        <dp>13</dp>" +
        		"        <dp>14</dp>" +
        		"    </city>" +
        		"    <city seq_id=\"3\">" +
        		"        <dp>2</dp>" +
        		"        <dp>4</dp>" +
        		"        <dp>13</dp>" +
        		"        <dp>14</dp>" +
        		"    </city>" +
        		"    <city seq_id=\"4\">" +
        		"        <dp>2</dp>" +
        		"        <dp>4</dp>" +
        		"        <dp>13</dp>" +
        		"        <dp>14</dp>" +
        		"    </city>" +
        		"</citylist> ");
        return data;
    }
    
    protected static XMap launchDiscoverCategoryList(Context context, String message) {
        XMap data = new XMap();
        XArray<XMap> list = new XArray<XMap>();
        list.add(launchDiscoverCategory(context, Integer.parseInt(BaseQuery.DATA_TYPE_TUANGOU)));
        list.add(launchDiscoverCategory(context, Integer.parseInt(BaseQuery.DATA_TYPE_DIANYING)));
        list.add(launchDiscoverCategory(context, Integer.parseInt(BaseQuery.DATA_TYPE_YANCHU)));
        list.add(launchDiscoverCategory(context, Integer.parseInt(BaseQuery.DATA_TYPE_ZHANLAN)));
        data.put(DiscoverCategoryList.FIELD_LIST, list);
        data.put(DiscoverCategoryList.FIELD_MESSAGE, message);
        return data;
    }
    
    protected static XMap launchDiscoverCategory(Context context, int type) {
        XMap data = new XMap();
        data.put(DiscoverCategory.FIELD_TYPE, type);
        data.put(DiscoverCategory.FIELD_NUM_CITY, 1680);
        data.put(DiscoverCategory.FIELD_NUM_NEARBY, 168);
        data.put(DiscoverCategory.FIELD_DATA, launchTKDrawable(context));
        return data;
    }
    
    protected static XMap launchTKDrawable(Context context) {
        XMap data = new XMap();
        data.put(TKDrawable.FIELD_URL, "http://wm2e16.263.net/resource/images/58/226/tigerknows.com/domain_logo.gif");
//        data.put(TKDrawable.FIELD_DATA, launchDrawable(context, R.drawable.icon));
        return data;
    }
    
    protected static XMap launchDiscoverConfigList(String version, Context context, int supportType) {
        XMap data = new XMap();
        data.put(DiscoverResponse.DiscoverConfigList.FIELD_VERSION, version);
        XArray<XMap> list = new XArray<XMap>();
        
        List<CityInfo> allCityInfoList = MapEngine.getAllProvinceCityList(context);  
        for(CityInfo cityInfo : allCityInfoList) {
            List<CityInfo> cityInfoList = cityInfo.getCityList();
            for(CityInfo cityInfoChild : cityInfoList) {
                XMap config = launchDiscoverConfig(cityInfoChild.getId(), supportType);
                list.add(config);
            }
        }
        
        data.put(DiscoverResponse.DiscoverConfigList.FIELD_LIST, list);
        return data;
    }
    
    protected static XMap launchDiscoverConfig(int seq_id, int supportType) {
        XMap data = new XMap();
        data.put(DiscoverResponse.DiscoverConfigList.DiscoverConfig.FIELD_SEQ_ID, seq_id);
        XArray<XInt> list = new XArray<XInt>();
        list.add(XInt.valueOf(supportType));
        data.put(DiscoverResponse.DiscoverConfigList.DiscoverConfig.FIELD_LIST, list);
        return data;
    }

    public static XMap launchTuangouResponse(Context context, int total, String message) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(TuangouResponse.FIELD_LIST, launchTuangouList(context, total, message, TKConfig.getPageSize()));
        data.put(TuangouResponse.FIELD_DATABASE_VERSION, "1.0");
        data.put(TuangouResponse.FIELD_FILTER_AREA_INDEX, launchFilterIndex(1, 168));
        data.put(TuangouResponse.FIELD_FILTER_CATEGORY_INDEX, launchFilterIndex(2, 168));
        data.put(TuangouResponse.FIELD_FILTER_ORDER_INDEX, launchFilterIndex(3, 168));
        data.put(TuangouResponse.FIELD_FILTER_AREA, launchFilterArea());
        data.put(TuangouResponse.FIELD_FILTER_CATEGORY_ORDER, launchFilterCategoryOrder());
        return data;
    }

    public static XMap launchFendianResponse(int total, String message) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(FendianResponse.FIELD_LIST, launchFendianList(total, message, TKConfig.getPageSize()));
        return data;
    }

    protected static XMap launchFendianList(int total, String message, int pageSize) {
        XMap data = new XMap();
        launchBaseList(data, total, message);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < pageSize; i ++) {
            list.add(launchFendian("fendian"+i));
        }
        data.put(FendianList.FIELD_LIST, list);
        return data;
    }

    protected static XMap launchFendian(String name) {
        return launchFendian(name, null);
    }

    protected static XMap launchFendian(String name, XMap data) {
        if (data == null) {
            data = new XMap();
        }
        data.put(Fendian.FIELD_UID, "FIELD_UID");
        data.put(Fendian.FIELD_BRC_ID, 168);
        data.put(Fendian.FIELD_TUAN_ID, "FIELD_TUAN_ID");
        data.put(Fendian.FIELD_LINK_UID, "FIELD_LINK_UID");
        data.put(Fendian.FIELD_ADMINNAME, "FIELD_ADMINNAME");
        data.put(Fendian.FIELD_AREANAME, "FIELD_AREANAME");
        data.put(Fendian.FIELD_X, ((int) (116.397*100000)));
        data.put(Fendian.FIELD_Y, ((int) (39.904*100000)));
        data.put(Fendian.FIELD_PLACE_NAME, name);
        data.put(Fendian.FIELD_PLACE_PHONE, "16899168 16899168 16899168");
        data.put(Fendian.FIELD_OPEN_TIME, "FIELD_OPEN_TIME");
        data.put(Fendian.FIELD_ADDRESS, "FIELD_ADDRESS");
        data.put(Fendian.FIELD_DISTANCE, "12Km");
        return data;
    }

    public static XMap launchShangjiaResponse(Context context, int total) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(ShangjiaResponse.FIELD_LIST, launchShangjiaList(context, total));
        return data;
    }

    protected static XMap launchShangjiaList(Context context, int total) {
        XMap data = new XMap();
        data.put(ShangjiaList.FIELD_TOTAL, total);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0; i < total; i++) {
            list.add(launchShangjia(context, "shangjia"+i));
        }
        data.put(ShangjiaList.FIELD_LIST, list);
        return data;
    }

    protected static XMap launchShangjia(Context context, String message) {
        return launchShangjia(context, message, null);
    }

    protected static XMap launchShangjia(Context context, String message, XMap data) {
        if (data == null) {
            data = new XMap();
        } 
        data.put(Shangjia.FIELD_SOURCE, Shangjia.SOURCE_TEST);
        data.put(Shangjia.FIELD_NAME, "test");
        data.put(Shangjia.FIELD_SERVICE_TEL, "16899168");
        data.put(Shangjia.FIELD_MARKER, Utility.getDrawableResource(context, R.drawable.icon));
        data.put(Shangjia.FIELD_URL, "http://www.baidu.com");
        data.put(Shangjia.FIELD_MESSAGE, message);
        data.put(Shangjia.FIELD_LOGO, Utility.getDrawableResource(context, R.drawable.icon));
        data.put(Shangjia.FIELD_REFUND_SERVICE, "FIELD_REFUND_SERVICE");
        return data;
    }

    protected static void launchBaseList(XMap data, int total, String message) {
        data.put(BaseList.FIELD_TOTAL, total);
        data.put(BaseList.FIELD_MESSAGE, message);
    }

    protected static XMap launchTuangouList(Context context, int total, String message, int size) {
        XMap data = new XMap();
        launchBaseList(data, total, message);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < size; i ++) {
            list.add(launchTuangou(context, "tuangou"+i+"`1234567890-=  qwertyuiop[]asdfghjkl;'zxcvbnm,." +
            		"`1234567890-=  qwertyuiop[]asdfghjkl;'zxcvbnm,." +
            		"`1234567890-=  qwertyuiop[]asdfghjkl;'zxcvbnm,.", 
            		null, 
            		null,
                    false));
        }
        data.put(TuangouList.FIELD_LIST, list);
        return data;
    }

    protected static XMap launchTuangou(Context context, String name, String noticed, String contentText, boolean contextPic) {
        XMap data = new XMap();
        data.put(Tuangou.FIELD_UID, "FIELD_UID");
        data.put(Tuangou.FIELD_NAME, name);
        data.put(Tuangou.FIELD_TAG, "FIELD_TAG");
        data.put(Tuangou.FIELD_CITY_ID, "FIELD_CITY_ID");
        data.put(Tuangou.FIELD_PICTURES, launchTKDrawable(context));
        data.put(Tuangou.FIELD_PICTURES_DETAIL, launchTKDrawable(context));
        data.put(Tuangou.FIELD_DESCRIPTION, "FIELD_DESCRIPTION");
        data.put(Tuangou.FIELD_START_TIME, "FIELD_START_TIME");
        data.put(Tuangou.FIELD_END_TIME, "FIELD_END_TIME");
        data.put(Tuangou.FIELD_PRICE, "168");
        data.put(Tuangou.FIELD_ORG_PRICE, "68");
        data.put(Tuangou.FIELD_DISCOUNT, "7.8");
        if (!TextUtils.isEmpty(noticed)) {
            data.put(Tuangou.FIELD_NOTICED, noticed);
        }
        data.put(Tuangou.FIELD_BUYER_NUM, 168);
        data.put(Tuangou.FIELD_REFUND, "not"+context.getString(R.string.tuangou_no_refund));
        if (!TextUtils.isEmpty(contentText)) {
            data.put(Tuangou.FIELD_CONTENT_TEXT, contentText);
        }
        if (contextPic) {
            data.put(Tuangou.FIELD_CONTENT_PIC, launchTKDrawable(context));
        }
        data.put(Tuangou.FIELD_SOURCE, Shangjia.SOURCE_TEST);
        data.put(Tuangou.FIELD_SHORT_DESC, "FIELD_SHORT_DESC");
        data.put(Tuangou.FIELD_DEADLINE, "FIELD_DEADLINE");
        data.put(Tuangou.FIELD_GOODS_ID, "FIELD_GOODS_ID");
        data.put(Tuangou.FIELD_BRANCH_NUM, 168);
        launchFendian(name+"fendian", data);
        return data;
    }
    
    protected static XArray<XInt> launchFilterIndex(int selectIndex, int size) {
        XArray<XInt> list = new XArray<XInt>();
        list.add(XInt.valueOf(selectIndex));
        for(int i = 0; i < size; i++) {
            list.add(XInt.valueOf(i));
        }
        return list;
    }
    
    public static XMap launchFilterConfigResponse() {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        XMap configResult = new XMap();
        configResult.put(FilterConfigResponse.FIELD_AREA_FILTER, launchFilterArea());
        data.put(FilterConfigResponse.FIELD_RESULT, configResult);
        return data;
    }

    protected static XMap launchFilterArea() {
        XMap data = new XMap();
        data.put(FilterArea.FIELD_VERSION, "1.0");
        XArray<XMap> list = new XArray<XMap>();
        for(int i=0; i < 168; i++) {
            XMap filterOption = null;
            if (i > 0 && i % 3 == 0) {
                filterOption = launchFilterOption("area"+i, i/3);
            } else {
                filterOption = launchFilterOption("area"+i, -1);
            }
            list.add(filterOption);
        }
        data.put(FilterArea.FIELD_LIST, list);
        return data;
    }
    
    protected static XMap launchFilterOption(String name, int parent) {
        XMap data = new XMap();
        data.put(FilterOption.FIELD_NAME, name);
        data.put(FilterOption.FIELD_PARENT, parent);
        return data;
    }

    protected static XMap launchFilterCategoryOrder() {
        XMap data = new XMap();
        data.put(FilterCategoryOrder.FIELD_VERSION, "1.0");
        XArray<XMap> list = new XArray<XMap>();
        for(int i=0; i < 168; i++) {
            XMap filterOption = null;
            if (i > 0 && i % 3 == 0) {
                filterOption = launchFilterOption("category"+i, i/3);
            } else {
                filterOption = launchFilterOption("category"+i, -1);
            }
            list.add(filterOption);
        }
        data.put(FilterCategoryOrder.FIELD_LIST_CATEGORY, list);
        list = new XArray<XMap>();
        for(int i=0; i < 168; i++) {
            XMap filterOption = null;
            if (i > 0 && i % 3 == 0) {
                filterOption = launchFilterOption("order"+i, i/3);
            } else {
                filterOption = launchFilterOption("order"+i, -1);
            }
            list.add(filterOption);
        }
        data.put(FilterCategoryOrder.FIELD_LIST_ORDER, list);
        return data;
    }

    public static XMap launchDianyingResponse(Context context, int total, String message) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(DianyingResponse.FIELD_LIST, launchDianyingList(context, total, message, TKConfig.getPageSize()));
        data.put(DianyingResponse.FIELD_DATABASE_VERSION, "1.0");
        data.put(DianyingResponse.FIELD_FILTER_AREA_INDEX, launchFilterIndex(1, 168));
        data.put(DianyingResponse.FIELD_FILTER_CATEGORY_INDEX, launchFilterIndex(2, 168));
        data.put(DianyingResponse.FIELD_FILTER_ORDER_INDEX, launchFilterIndex(3, 168));
        data.put(DianyingResponse.FIELD_FILTER_AREA, launchFilterArea());
        data.put(DianyingResponse.FIELD_FILTER_CATEGORY_ORDER, launchFilterCategoryOrder());
        return data;
    }

    protected static XMap launchDianyingList(Context context, int total, String message, int size) {
        XMap data = new XMap();
        launchBaseList(data, total, message);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < size; i ++) {
            list.add(launchDianying(context, "dianying"+i, null));
        }
        data.put(DianyingList.FIELD_LIST, list);
        return data;
    }

    protected static XMap launchDianying(Context context, String name, String description) {
        XMap data = new XMap();
        data.put(Dianying.FIELD_UID, "FIELD_UID");
        data.put(Dianying.FIELD_NAME, name);
        data.put(Dianying.FIELD_ALIAS, "FIELD_ALIAS");
        data.put(Dianying.FIELD_TAG, "FIELD_TAG");
        data.put(Dianying.FIELD_PICTURES, launchTKDrawable(context));
        data.put(Dianying.FIELD_PICTURES_DETAIL, launchTKDrawable(context));
        if (!TextUtils.isEmpty(description)) {
            data.put(Dianying.FIELD_DESCRIPTION, "FIELD_DESCRIPTION");
        }
        data.put(Dianying.FIELD_START_TIME, "FIELD_START_TIME");
        data.put(Dianying.FIELD_LENGTH, "FIELD_LENGTH");
        data.put(Dianying.FIELD_LANGUAGE, "FIELD_LANGUAGE");
        data.put(Dianying.FIELD_RANK, 3);
        data.put(Dianying.FIELD_COUNTRY, "FIELD_COUNTRY");
        data.put(Dianying.FIELD_DIRECTOR, "FIELD_DIRECTOR");
        data.put(Dianying.FIELD_MAIN_ACTOR, "FIELD_MAIN_ACTOR");
        data.put(Dianying.FIELD_NUM, 168);
        launchYingxun(name, data);
        return data;
    }


    protected static XMap launchYingxun(String name) {
        return launchYingxun(name, null);
    }

    protected static XMap launchYingxun(String name, XMap data) {
        if (data == null) {
            data = new XMap();
        }
        data.put(Yingxun.FIELD_UID, "FIELD_UID");
        data.put(Yingxun.FIELD_CITY_ID, "1");
        data.put(Yingxun.FIELD_LINK_UID, "FIELD_LINK_UID");
        data.put(Yingxun.FIELD_MOVIE_UID, "FIELD_MOVIE_UID");
        data.put(Yingxun.FIELD_ADMINNAME, "FIELD_ADMINNAME");
        data.put(Yingxun.FIELD_AREANAME, "FIELD_AREANAME");
        data.put(Yingxun.FIELD_X, ((int) (116.397*100000)));
        data.put(Yingxun.FIELD_Y, ((int) (39.904*100000)));
        data.put(Yingxun.FIELD_NAME, name);
        data.put(Yingxun.FIELD_ADDRESS, "FIELD_ADDRESS");
        data.put(Yingxun.FIELD_PHONE, "FIELD_PHONE");
        data.put(Yingxun.FIELD_DISTANCE, "FIELD_DISTANCE");
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0; i < 24; i++) {
            list.add(launchChangci((i+1)+":00", i+Changci.OPTION_FILTER));
        }
        data.put(Yingxun.FIELD_CHANGCI_LIST, list);
        return data;
    }

    protected static XMap launchChangci(String time, int day) {
        XMap data = new XMap();
        data.put(Changci.FIELD_UID, "FIELD_UID");
        data.put(Changci.FIELD_YINGX_UID, "FIELD_YINGX_UID");
        data.put(Changci.FIELD_START_TIME, time);
        data.put(Changci.FIELD_VERSION, "3DMAX");
        data.put(Changci.FIELD_OPTION, day);
        return data;
    }

    public static XMap launchYingxunResponse(int total, String message) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(YingxunResponse.FIELD_LIST, launchYingxunList(total, message, TKConfig.getPageSize()));
        return data;
    }

    protected static XMap launchYingxunList(int total, String message, int pageSize) {
        XMap data = new XMap();
        launchBaseList(data, total, message);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < pageSize; i ++) {
            list.add(launchYingxun("yingxun"+i));
        }
        data.put(YingxunList.FIELD_LIST, list);
        return data;
    }

    protected static XMap launchZhanlan(Context context, String name, String description) {
        XMap data = new XMap();
        data.put(Zhanlan.FIELD_UID, "FIELD_UID");
        data.put(Zhanlan.FIELD_NAME, name);
        data.put(Zhanlan.FIELD_CITY_ID, "1");
        data.put(Zhanlan.FIELD_PICTURES, launchTKDrawable(context));
        data.put(Zhanlan.FIELD_PICTURES_DETAIL, launchTKDrawable(context));
        if (!TextUtils.isEmpty(description)) {
            data.put(Zhanlan.FIELD_DESCRIPTION, description);
        }
        data.put(Zhanlan.FIELD_LINK_UID, "FIELD_LINK_UID");
        data.put(Zhanlan.FIELD_X, ((int) (116.397*100000)));
        data.put(Zhanlan.FIELD_Y, ((int) (39.904*100000)));
        data.put(Zhanlan.FIELD_TIME_INFO, "FIELD_TIME_INFO");
        data.put(Zhanlan.FIELD_TIME_DESC, "FIELD_TIME_DESC");
        data.put(Zhanlan.FIELD_PRICE, "FIELD_PRICE");
        data.put(Zhanlan.FIELD_CONTACT_TEL, "FIELD_CONTACT_TEL");
        data.put(Zhanlan.FIELD_PLACE_NAME, "FIELD_PLACE_NAME");
        data.put(Zhanlan.FIELD_ADDRESS, "FIELD_ADDRESS");
        data.put(Zhanlan.FIELD_HOT, 3);
        data.put(Zhanlan.FIELD_URL, BaseQueryTest.URL);
        data.put(Zhanlan.FIELD_ADMINNAME, "FIELD_ADMINNAME");
        data.put(Zhanlan.FIELD_AREANAME, "FIELD_AREANAME");
        data.put(Zhanlan.FIELD_SOURCE, "FIELD_SOURCE");
        data.put(Zhanlan.FIELD_DISTANCE, "FIELD_DISTANCE");
        return data;
    }

    protected static XMap launchYanchu(Context context, String name, String descrption) {
        XMap data = new XMap();
        data.put(Yanchu.FIELD_UID, "FIELD_UID");
        data.put(Yanchu.FIELD_NAME, name);
        data.put(Yanchu.FIELD_CITY_ID, "1");
        data.put(Yanchu.FIELD_PICTURES, launchTKDrawable(context));
        data.put(Yanchu.FIELD_PICTURES_DETAIL, launchTKDrawable(context));
        if (!TextUtils.isEmpty(descrption)) {
            data.put(Yanchu.FIELD_DESCRIPTION, descrption);
        }
        data.put(Yanchu.FIELD_LINK_UID, "FIELD_LINK_UID");
        data.put(Yanchu.FIELD_X, ((int) (116.397*100000)));
        data.put(Yanchu.FIELD_Y, ((int) (39.904*100000)));
        data.put(Yanchu.FIELD_TIME_INFO, "FIELD_TIME_INFO");
        data.put(Yanchu.FIELD_TIME_DESC, "FIELD_TIME_DESC");
        data.put(Yanchu.FIELD_PRICE, "FIELD_PRICE");
        data.put(Yanchu.FIELD_CONTACT_TEL, "FIELD_CONTACT_TEL");
        data.put(Yanchu.FIELD_PLACE_NAME, "FIELD_PLACE_NAME");
        data.put(Yanchu.FIELD_ADDRESS, "FIELD_ADDRESS");
        data.put(Yanchu.FIELD_HOT, 3);
        data.put(Yanchu.FIELD_URL, BaseQueryTest.URL);
        data.put(Yanchu.FIELD_ADMINNAME, "FIELD_ADMINNAME");
        data.put(Yanchu.FIELD_AREANAME, "FIELD_AREANAME");
        data.put(Yanchu.FIELD_SOURCE, "FIELD_SOURCE");
        data.put(Yanchu.FIELD_ORDER_TEL, "16899168");
        data.put(Yanchu.FIELD_DISTANCE, "15km");
        return data;
    }

    public static XMap launchYanchuResponse(Context context, int total, String message) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(YanchuResponse.FIELD_LIST, launchYanchuList(context, total, message, TKConfig.getPageSize()));
        data.put(YanchuResponse.FIELD_DATABASE_VERSION, "1.0");
        data.put(YanchuResponse.FIELD_FILTER_AREA_INDEX, launchFilterIndex(1, 168));
        data.put(YanchuResponse.FIELD_FILTER_CATEGORY_INDEX, launchFilterIndex(2, 168));
        data.put(YanchuResponse.FIELD_FILTER_ORDER_INDEX, launchFilterIndex(3, 168));
        data.put(YanchuResponse.FIELD_FILTER_AREA, launchFilterArea());
        data.put(YanchuResponse.FIELD_FILTER_CATEGORY_ORDER, launchFilterCategoryOrder());
        return data;
    }

    public static XMap launchYanchuList(Context context, int total, String message, int size) {
        XMap data = new XMap();
        launchBaseList(data, total, message);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < size; i ++) {
            list.add(launchYanchu(context, "Yanchu"+i, null));
        }
        data.put(YanchuList.FIELD_LIST, list);
        return data;
    }

    public static XMap launchZhanlanResponse(Context context, int total, String message) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(ZhanlanResponse.FIELD_LIST, launchZhanlanList(context, total, message, TKConfig.getPageSize()));
        data.put(ZhanlanResponse.FIELD_DATABASE_VERSION, "1.0");
        data.put(ZhanlanResponse.FIELD_FILTER_AREA_INDEX, launchFilterIndex(1, 168));
        data.put(ZhanlanResponse.FIELD_FILTER_CATEGORY_INDEX, launchFilterIndex(2, 168));
        data.put(ZhanlanResponse.FIELD_FILTER_ORDER_INDEX, launchFilterIndex(3, 168));
        data.put(ZhanlanResponse.FIELD_FILTER_AREA, launchFilterArea());
        data.put(ZhanlanResponse.FIELD_FILTER_CATEGORY_ORDER, launchFilterCategoryOrder());
        return data;
    }

    public static XMap launchZhanlanList(Context context, int total, String message, int size) {
        XMap data = new XMap();
        launchBaseList(data, total, message);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < size; i ++) {
            list.add(launchZhanlan(context, "Zhanlan"+i, null));
        }
        data.put(ZhanlanList.FIELD_LIST, list);
        return data;
    }

    public static XMap launchPOIResponse(int total, String message) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(POIResponse.FIELD_B_POI_LIST, launchPOIList(total, message, TKConfig.getPageSize()));
        data.put(POIResponse.FIELD_FILTER_AREA_INDEX, launchFilterIndex(1, 168));
        data.put(POIResponse.FIELD_FILTER_CATEGORY_INDEX, launchFilterIndex(2, 168));
        data.put(POIResponse.FIELD_FILTER_ORDER_INDEX, launchFilterIndex(3, 168));
        data.put(POIResponse.FIELD_FILTER_AREA, launchFilterArea());
        data.put(POIResponse.FIELD_FILTER_CATEGORY_ORDER, launchFilterCategoryOrder());
        return data;
    }

    public static XMap launchPOIList(int total, String message, int size) {
        XMap data = new XMap();
        launchBaseList(data, total, message);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < size; i ++) {
            list.add(launchPOI("POI"+i+"POIPOIPOIPOIPOIPOIPOIPOIPOIPOIPOIPOIPOIPOIPOIPOIPOI"));
        }
        data.put(POIList.FIELD_LIST, list);
        return data;
    }

    protected static XMap launchPOI(String name) {
        XMap data = new XMap();
        data.put(POI.FIELD_UUID, POI_UUID);
        data.put(POI.FIELD_NAME, name);
        data.put(POI.FIELD_TYPE, 1);
        data.put(POI.FIELD_LONGITUDE, ((int) (116.397*100000)));
        data.put(POI.FIELD_LATITUDE, ((int) (39.904*100000)));
        data.put(POI.FIELD_DESCRIPTION, launchDynamicDescription());
        data.put(POI.FIELD_TELEPHONE, "FIELD_TELEPHONE");
        data.put(POI.FIELD_RESERVE_TEL, "FIELD_RESERVE_TEL");
        data.put(POI.FIELD_ADDRESS, "FIELD_ADDRESS");
        data.put(POI.FIELD_URL, "FIELD_URL");
        data.put(POI.FIELD_TO_CENTER_DISTANCE, "12km");
        data.put(POI.FIELD_COMMENT_PATTERN, POI.COMMENT_PATTERN_FOOD);
        data.put(POI.FIELD_ATTRIBUTE, 1);
        data.put(POI.FIELD_STATUS, 1);
        data.put(POI.FIELD_LAST_COMMENT, launchDianping("FIELD_LAST_COMMENT"));
        XArray<XMap> xarray = new XArray<XMap>();
        for(int i = 0; i < 16; i ++) {
            xarray.add(launchDynamicPOI(i));
        }
        xarray.add(launchDynamicPOI(Integer.parseInt(BaseQuery.DATA_TYPE_COUPON)));
        xarray.add(launchDynamicPOI(Integer.parseInt(DynamicPOI.TYPE_HOTEL)));
        data.put(POI.FIELD_DYNAMIC_POI, xarray);
        data.put(POI.FIELD_PRICE, "168");
        return data;
    }

    protected static XMap launchDynamicDescription() {
        XMap data = new XMap();
        data.put(Description.FIELD_GRADE, 4);
        data.put(Description.FIELD_SERVICE, "FIELD_SERVICE");
        data.put(Description.FIELD_ENVIRONMENT, "FIELD_ENVIRONMENT");
        data.put(Description.FIELD_PER_CAPITA, 168);
        data.put(Description.FIELD_MOODS, "FIELD_MOODS");
        data.put(Description.FIELD_STAR, "FIELD_STAR");
        data.put(Description.FIELD_SUBWAY_EXITS, launchSubwayExit());
        data.put(Description.FIELD_SUBWAY_PRESET_TIMES, launchSubwayPresetTime());
        data.put(Description.FIELD_LINE, "FIELD_LINE");
        return data;
    }
    
    protected static XArray<XMap> launchSubwayExit() {
        XArray<XMap> dataList = new XArray<XMap>();
        for (int i = 0; i < 4; i++) {
            XMap subwayExit = new XMap();
            subwayExit.put(SubwayExit.FEILD_SUBWAY_EXIT, "Exit " + i);
            subwayExit.put(SubwayExit.FEILD_LANDMARK, "Landmark " + i);
            XArray<XMap> stationList = new XArray<XMap>();
//            for (int j = 0; j < 3; j++) {
                XMap station = new XMap();
                station.put(Busstop.FEILD_STATION, "中关园北站");
                stationList.add(station);
//            }
            subwayExit.put(SubwayExit.FEILD_BUSSTOP, stationList);
            dataList.add(subwayExit);
        }
        
        return dataList;
    }
    
    protected static XArray<XMap> launchSubwayPresetTime() {
        XArray<XMap> dataList = new XArray<XMap>();
        for (int i = 0; i < 2; i++) {
            XMap subwayPresetTime = new XMap();
            subwayPresetTime.put(SubwayPresetTime.FEILD_SUBWAY_NAME, "Line " + i);
            XArray<XMap> presetTimeList = new XArray<XMap>();
            for (int j = 0; j < 3; j++) {
                XMap presetTime = new XMap();
                presetTime.put(PresetTime.FEILD_DIRECTION, "Direction");
                presetTime.put(PresetTime.FEILD_END_TIME, "EndTime");
                presetTime.put(PresetTime.FEILD_START_TIME, "StartTime");
                presetTimeList.add(presetTime);
            }
            subwayPresetTime.put(SubwayPresetTime.FEILD_PRESET_TIME, presetTimeList);
            dataList.add(subwayPresetTime);
        }
        
        return dataList;
    }

    protected static XMap launchDynamicPOI(int type) {
        XMap data = new XMap();
        data.put(DynamicPOI.FIELD_TYPE, type);
        data.put(DynamicPOI.FIELD_MASTER_UID, "FIELD_MASTER_UID");
        data.put(DynamicPOI.FIELD_SUMMARY, "FIELD_SUMMARY");
        data.put(DynamicPOI.FIELD_SLAVE_UID, "FIELD_SLAVE_UID");
        return data;
    }

    public static XMap launchHotelPOIResponse(int total, String message) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(POIResponse.FIELD_B_POI_LIST, launchHotelPOIList(total, message, TKConfig.getPageSize()));
        data.put(POIResponse.FIELD_FILTER_AREA_INDEX, launchFilterIndex(1, 168));
        data.put(POIResponse.FIELD_FILTER_CATEGORY_INDEX, launchFilterIndex(2, 168));
        data.put(POIResponse.FIELD_FILTER_ORDER_INDEX, launchFilterIndex(3, 168));
        data.put(POIResponse.FIELD_FILTER_AREA, launchFilterArea());
        data.put(POIResponse.FIELD_FILTER_CATEGORY_ORDER, launchFilterCategoryOrder());
        return data;
    }

    public static XMap launchHotelPOIList(int total, String message, int size) {
        XMap data = new XMap();
        launchBaseList(data, total, message);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < size; i ++) {
            list.add(launchHotelPOI("Hotel"+i+"HotelHotelHotelHotelHotelHotelHotel", i % 2));
        }
        data.put(POIList.FIELD_LIST, list);
        return data;
    }

    public static XMap launchHotelPOI(String name, int canReserve) {
        XMap data = launchPOI(name);
        data.put(Hotel.FIELD_UUID, "FIELD_UUID");
        data.put(Hotel.FIELD_SOURCE, "FIELD_SOURCE");
        data.put(Hotel.FIELD_BRAND, 8);
        data.put(Hotel.FIELD_IMAGE_THUMB, "FIELD_IMAGE_THUMB");
        XArray<XMap> xarray = new XArray<XMap>();
        for(int i = 0; i < 16; i ++) {
            xarray.add(launchHotelTKDrawable(i));
        }
        data.put(Hotel.FIELD_IMAGE_LIST, xarray);
        xarray = new XArray<XMap>();
        for(int i = 0; i < 16; i ++) {
            xarray.add(launchRoomType(i));
        }
        data.put(Hotel.FIELD_ROOM_TYPE_LIST, xarray);
        data.put(Hotel.FIELD_SERVICE, "FIELD_SERVICE");
        data.put(Hotel.FIELD_CAN_RESERVE, canReserve);
        return data;
    }

    protected static XMap launchHotelTKDrawable(int order) {
        XMap data = new XMap();
        data.put(HotelTKDrawable.FIELD_NAME, "FIELD_NAME"+order);
        data.put(HotelTKDrawable.FIELD_URL, BaseQueryTest.PIC_URL);
        return data;
    }

    public static XMap launchRoomType(int order) {
        XMap data = new XMap();
        data.put(RoomType.FIELD_ROOM_ID, "168");
        data.put(RoomType.FIELD_RATEPLAN_ID, "168");
        data.put(RoomType.FIELD_ROOM_TYPE, "FIELD_ROOM_TYPE"+order);
        data.put(RoomType.FIELD_FLOOR, "FIELD_FLOOR"+order);
        data.put(RoomType.FIELD_BED_TYPE, "FIELD_BED_TYPE");
        data.put(RoomType.FIELD_NET_SERVICE, "FIELD_NET_SERVICE");
        data.put(RoomType.FIELD_BREAKFAST, "FIELD_BREAKFAST");
        data.put(RoomType.FIELD_PRICE, 168+order*100+"");
        data.put(RoomType.FIELD_CAN_RESERVE, 1);
        data.put(RoomType.FIELD_NEED_GUARANTEE, order%2);
        data.put(RoomType.FIELD_SUBTITLE, "FIELD_SUBTITLE");
        data.put(RoomType.FIELD_VENDOR_ID, order%3 + 2000);
        data.put(RoomType.FIELD_VENDOR_NAME, "FIELD_VENDOR_" + order%3);
        return data;
    }

    public static XMap launchDianpingResponse(int total, String message) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(FendianResponse.FIELD_LIST, launchDianpingList(total, message, TKConfig.getPageSize()));
        return data;
    }

    protected static XMap launchDianpingList(int total, String message, int pageSize) {
        XMap data = new XMap();
        launchBaseList(data, total, message);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < pageSize; i ++) {
            list.add(launchDianping("fendian"+i));
        }
        data.put(FendianList.FIELD_LIST, list);
        return data;
    }

    protected static XMap launchDianping(String content) {
        XMap data = new XMap();
        data.put(Comment.FIELD_UID, "FIELD_UID");
        data.put(Comment.FIELD_CONTENT, content);
        data.put(Comment.FIELD_USER, AccountManageTest.SESSION_ID);
        data.put(Comment.FIELD_TIME, "FIELD_TIME FIELD_TIME");
        data.put(Comment.FIELD_GRADE, 4);
        data.put(Comment.FIELD_PUID, POI_UUID);
        data.put(Comment.FIELD_AVG, 168);
        data.put(Comment.FIELD_TASTE, 4);
        data.put(Comment.FIELD_QOS, 8);
        data.put(Comment.FIELD_RECOMMEND, "FIELD_RECOMMEND,FIELD_RECOMMEN,FIELD_RECOMMEND");
        data.put(Comment.FIELD_LEVEL, 8);
        data.put(Comment.FIELD_EFFECT, 8);
        data.put(Comment.FIELD_RESTAIR, "FIELD_RESTAIR");
        data.put(Comment.FIELD_POI_STATUS, 1);
        data.put(Comment.FIELD_PATTERN, POI.COMMENT_PATTERN_FOOD);
        data.put(Comment.FIELD_POI_NAME, "FIELD_POI_NAME");
        data.put(Comment.FIELD_USER_ID, 168);
        data.put(Comment.FIELD_POI_CITY_ID, "1");
        data.put(Comment.FIELD_URL, "FIELD_URL");
        data.put(Comment.FIELD_CLIENT_UID, "FIELD_CLIENT_UID");
        data.put(Comment.FIELD_SOURCE, "test");
        return data;
    }

    public static XMap launchPullMessage() {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(PullMessage.FIELD_REQUEST_INTERVAL_DAYS, 1);
        data.put(PullMessage.FIELD_RECORD_MESSAGE_UPPER_LIMIT, 12);
        XArray<XMap> xArray = new XArray<XMap>();
        xArray.add(launchMessage());
        data.put(PullMessage.FIELD_MESSAGE, xArray);
        return data;
    }
    
    protected static XMap launchMessage() {
        XMap data = new XMap();
        data.put(Message.FIELD_MESSAGE_ID, 10);
        data.put(Message.FIELD_CITY_ID, "1");
        data.put(Message.FIELD_EXPIRY_DATE, "2013-01-03");
        data.put(Message.FIELD_MSG_TYPE, Message.TYPE_FILM);
        data.put(Message.FIELD_POI_INFO, launchPulledDynamicPOI());
        return data;
    }
    
    protected static XMap launchPulledDynamicPOI() {
        XMap data = new XMap();
        data.put(PulledDynamicPOI.FIELD_MASTER_POI_TYPE, Integer.parseInt(BaseQuery.DATA_TYPE_DIANYING));
        data.put(PulledDynamicPOI.FIELD_MASTER_POI_UID, "FIELD_MASTER_POI_UID");
        data.put(PulledDynamicPOI.FIELD_SLAVE_POI_TYPE, Integer.parseInt(BaseQuery.DATA_TYPE_YINGXUN));
        data.put(PulledDynamicPOI.FIELD_SLAVE_POI_UID, "FIELD_SLAVE_POI_UID");
        data.put(PulledDynamicPOI.FIELD_POI_DESCRIPTION, "FIELD_POI_DESCRIPTION");
        return data;
    }

    public static XMap launchAlternativeResponse(int total, String message) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(AlternativeResponse.FIELD_LIST, launchPOIList(total, message, TKConfig.getPageSize()));
        data.put(AlternativeResponse.FIELD_FILTER_AREA, launchFilterArea());
        data.put(AlternativeResponse.FIELD_POSITION, 16);
        return data;
    }

    public static XMap launchAlternativeList(int total, String message, int size) {
        XMap data = new XMap();
        launchBaseList(data, total, message);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < size; i ++) {
            list.add(launchPOI(("Alternative"+i+"AlternativeAlternativeAlternative")));
        }
        data.put(AlternativeResponse.FIELD_LIST, list);
        return data;
    }

    public static XMap launchCouponResponse(int total, String message) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(CouponResponse.FIELD_COUPON_LIST, launchCouponList(total, message, TKConfig.getPageSize()));
        return data;
    }

    protected static XMap launchCouponList(int total, String message, int pageSize) {
        XMap data = new XMap();
        launchBaseList(data, total, message);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < pageSize; i ++) {
            list.add(launchYingxun("coupon"+i));
        }
        data.put(CouponList.FIELD_LIST, list);
        return data;
    }

    protected static XMap launchCoupon() {
        XMap data = new XMap();
        data.put(Coupon.FIELD_UID, "FIELD_UID");
        data.put(Coupon.FIELD_DESCRIPTION, "FIELD_DESCRIPTION");
        data.put(Coupon.FIELD_BRIEF_PIC, BaseQueryTest.PIC_URL);
        data.put(Coupon.FIELD_HOT, 123);
        data.put(Coupon.FIELD_LIST_NAME, "FIELD_LIST_NAME");
        data.put(Coupon.FIELD_DETAIL_PIC, BaseQueryTest.PIC_URL);
        data.put(Coupon.FIELD_DETAIL, "FIELD_DETAIL,FIELD_DETAIL,FIELD_DETAIL");
        data.put(Coupon.FIELD_2D_CODE, BaseQueryTest.PIC_URL);
        data.put(Coupon.FIELD_LOGO, BaseQueryTest.PIC_URL);
        data.put(Coupon.FIELD_HINT_PIC, "FIELD_HINT_PIC");
        data.put(Coupon.FIELD_REMARK, "FIELD_REMARK");
        return data;
    }

    public static XMap launchDishResponse(int total) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(DishResponse.FIELD_LIST, launchDishList(total));
        return data;
    }

    protected static XMap launchDishList(int total) {
        XMap data = new XMap();
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < total; i ++) {
            list.add(launchDish(i));
        }
        data.put(DishList.FIELD_DISH_LIST, list);
        data.put(DishList.FIELD_CATEGORY_LIST, "[{\"sift_id\":1, \"sift_name\":\"菜单项1\", \"classsfication_list\":[{\"classfication_id\":1, \"classfication_name\":\"特色烤鱼1\", \"dishes_list\":[1,2,3]}]},{\"sift_id\":1, \"sift_name\":\"菜单项2\", \"classsfication_list\":[{\"classfication_id\":1, \"classfication_name\":\"特色烤鱼2\", \"dishes_list\":[4,5,6]}]}]");
        return data;
    }

    protected static XMap launchDish(int id) {
        XMap data = new XMap();
        data.put(Dish.FIELD_DISH_ID, id);
        data.put(Dish.FIELD_LINK_UID, "FIELD_LINK_UID");
        data.put(Dish.FIELD_CITY_ID, "1");
        data.put(Dish.FIELD_FOOD_NAME, id+"FIELD_FOOD_NAME");
        data.put(Dish.FIELD_PRICE, "FIELD_PRICE;FIELD_PRICE;FIELD_PRICE");
        data.put(Dish.FIELD_PRICE_JSON, "[]");
        data.put(Dish.FIELD_DEFAULT_PICTURE, launchHotelTKDrawable(1));
        data.put(Dish.FIELD_FOOD_PICTURES, 1);
        data.put(Dish.FIELD_HIT_COUNT, 1);
        return data;
    }

    public static XMap launchPictureResponse(int total) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(PictureResponse.FIELD_LIST, launchPictureList(total));
        return data;
    }

    protected static XMap launchPictureList(int total) {
        XMap data = new XMap();
        data.put(PictureList.FIELD_TOTAL, total);
        XArray<XMap> list = new XArray<XMap>();
        for(int i = 0 ; i < total; i ++) {
            list.add(launchHotelTKDrawable(i));
        }
        data.put(PictureList.FIELD_LIST, list);
        return data;
    }
}
