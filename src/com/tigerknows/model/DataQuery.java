
package com.tigerknows.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.DataQuery.AlternativeResponse.Alternative;
import com.tigerknows.model.DataQuery.DiscoverResponse.DiscoverConfigList;
import com.tigerknows.model.DataQuery.DiscoverResponse.DiscoverCategoryList.DiscoverCategory;
import com.tigerknows.model.DataQuery.DiscoverResponse.DiscoverConfigList.DiscoverConfig;
import com.tigerknows.model.test.DataQueryTest;
import com.tigerknows.model.xobject.ByteReader;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XInt;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.HanziUtil;
import com.tigerknows.util.Utility;
import com.weibo.sdk.android.WeiboParameters;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public final class DataQuery extends BaseQuery {
    
    // ts   String  true    时间戳，形如"yyyy/MM/dd HH:mm:ss" 
    public static final String SERVER_PARAMETER_TIME_STAMP = "ts";
    
    // info  string  false   info目前的取值有： tagsearch POI搜索时是通过点击首页进行的搜索（非输入搜索） networkpush 因为网络可用触发的push请求 
    public static final String SERVER_PARAMETER_INFO = "info";
    
    // x   Double  false   选定经度x
    public static final String SERVER_PARAMETER_LONGITUDE = "x";
    
    // y   Double  false   选定纬度y
    public static final String SERVER_PARAMETER_LATITUDE = "y";

    // flt String  false   筛选选项，格式为key:id;key:id;key:id(key是对应筛选项在xmap中的key，目前有11,12,13)
    public static final String SERVER_PARAMETER_FILTER = "flt";

    // flt_s    String  false   筛选选项字符串，格式为key:str;key:str;key:str(key同上，str指的是筛选选项对应的字符串，且如有父级也需提交，如"我的当前位置_附近xx米") 
    public static final String SERVER_PARAMETER_FILTER_STRING = "flt_s";
    
    // cfv string  false   城市地片筛选版本号，每个城市不同
    public static final String SERVER_PARAMETER_CITY_FILTER_VERSION = "cfv";
    
    // nfv string  false   分类和排序筛选版本号，全国共用一份
    public static final String SERVER_PARAMETER_NATION_FILTER_VERSION = "nfv";
    
    // cmv string  true    comment version 请求的评论版本 
    public static final String SERVER_PARAMETER_COMMENT_VERSION = "cmv";

    // bias string  false   是否对搜索结果有特殊要求，在“我要点评”请求中bias='1' 
    public static final String SERVER_PARAMETER_BIAS = "bias";
    
    // ref string  true    取"USER"(表示在个人中心获取用户相关点评)或"POI"(在poi详情页获取poi相关点评)
    public static final String SERVER_PARAMETER_REFER = "ref";
    
    // poiid   string  false   评论所属poi的id，通过POI详情查看点评时必需
    public static final String SERVER_PARAMETER_POI_ID = "poiid";
    
    // lx   Double  false   定位经度x
    public static final String SERVER_PARAMETER_LOCATION_LONGITUDE = "lx";
    
    // ly   Double  false   定位纬度y
    public static final String SERVER_PARAMETER_LOCATION_LATITUDE = "ly";
    
    // lc   int  false   定位城市Id
    public static final String SERVER_PARAMETER_LOCATION_CITY = "lc";

    // time string false 参考时间点
    public static final String SERVER_PARAMETER_TIME = "time";

    // direction int false 参考方向，取值范围 { before, after } 
    public static final String SERVER_PARAMETER_DIRECTION = "direction";

    // idlist   string  false   目前只支持poiid,由id组成的字符串，以','分隔。提交该参数意味着主要是关心id们的属性，其他非公共参数不需要提交了
    public static final String SERVER_PARAMETER_ID_LIST = "idlist";
    
    // tguid string true 相关联的团购的uid
    public static final String SERVER_PARAMETER_TUANGOU_UUID = "tguid";

    // mvuid    string  true    相关联的电影的uid 
    public static final String SERVER_PARAMETER_DIANYING_UUID = "mvuid";

    // dbv string true 索引目录为种子生成的uid，服务器发现不同或未提交会下发，有的话一定要提交 
    public static final String SERVER_PARAMETER_DISCOVER_BASEINDEX_VERSION = "dbv";
    
    // spts string true 当前客户端所能支持的动态poi的数据类型
    public static final String SERVER_PARAMETER_DISCOVER_SUPPORT_DATATYPE = "spt";
    
    // cdv string true 当前客户端缓存的所有城市能够支持的动态poi类型的数据的版本 
    public static final String SERVER_PARAMETER_DISCOVER_POI_VERSION = "cdv";
    
    // ids string false 团购商家id列表  
    public static final String SERVER_PARAMETER_SHANGJIA_IDS = "ids";
    
    // lastsuc String false 上次接收到消息的时间,客户端首次请求时此项为空
    public static final String SERVER_PARAMETER_LAST_PULL_DATE = "lastsuc";
    
    // msgIds String false 客户端已收到的消息id，多个id之间用_分隔。如"1000_32762_33658"。首次请求时此项为空。 
    public static final String SERVER_PARAMETER_MESSAGE_ID_LIST = "msgIds";
    
    // appendaction    string  false   目前支持nosearch（表示不做搜索，只返回筛选项） 
    public static final String SERVER_PARAMETER_APPENDACTION = "appendaction";
    
    // configinfo   String  true    JSON字符串，key标识config的类型，value标识版本号,具体定义见默认筛选项参数定义
    public static final String SERVER_PARAMETER_CONFIGINFO = "configinfo";
    
    // 评论版本 
    public static final String COMMENT_VERSION = "1";
    
    public static final String INFO_TYPE_TAG = "tagsearch";
    
    public static final String INFO_TYPE_NETWORK_PUSH = "networkpush";  
    
    public static final String INFO_CATE_FLT = "cate_flt";
    
    public static final String FILTER_TYPE_AREA = "11";    
    
    public static final String FILTER_TYPE_CATEGORY = "12";    
    
    public static final String FILTER_TYPE_ORDER = "13";    
    
    public static final String APPENDACTION_NOSEARCH = "nosearch";
    
    private static final SimpleDateFormat TIME_STAMP_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    
    // ref string  true    取"USER"(表示在个人中心获取用户相关点评)或"POI"(在poi详情页获取poi相关点评)
    public static final String REFER_USER = "USER";
    public static final String REFER_POI = "POI";

    // direction int false 参考方向，取值范围 { before, after } 
    public static final String DIRECTION_BEFORE = "before";
    public static final String DIRECTION_AFTER = "after";
    
    // bias string  false   是否对搜索结果有特殊要求，在“我要点评”请求中bias='1' 
    public static final String BIAS_MY_COMMENT = "1";
    
    private boolean isTurnPage = false;
    
    // POI Request Begin
    private POI poi;
    // POI Request End
    
    // POI Response Begin
    private static Object Filter_Lock = new Object();
    
    private static FilterCategoryOrder Filter_Category_Order_POI;
    
    private static FilterCategoryOrder Filter_Category_Order_Hotel;
    
    public static FilterCategoryOrder getHotelFilterCategoryOrder() {
        return Filter_Category_Order_Hotel;
    }
    
    public static FilterCategoryOrder getPOIFilterCategoryOrder() {
        return Filter_Category_Order_POI;
    }
    
    private static FilterArea Quanguo_Filter_Area;
    
    public static FilterArea getQuanguoFilterArea(Context context) {
        if (Quanguo_Filter_Area == null) {
//            XMap xmap = new XMap();
//            xmap.put(FilterArea.FIELD_VERSION, "0.0.0");
//            XArray<XMap> xArray = new XArray<XMap>();
//            String[] presetName = context.getResources().getStringArray(R.array.preset_filter_area);
//            for(int i = 0; i < presetName.length; i++) {
//                XMap fitler = new XMap();
//                fitler.put(FilterOption.FIELD_NAME, presetName[i]);
//                if (i == 0) {
//                    fitler.put(FilterOption.FIELD_PARENT, -1);
//                } else if (i == 1) {
//                    fitler.put(FilterOption.FIELD_PARENT, -1);
//                } else if (i > 1 && i < 6) {
//                    fitler.put(FilterOption.FIELD_PARENT, 1);
//                } else if (i > 5 && i < 11) {
//                    fitler.put(FilterOption.FIELD_PARENT, 6);
//                }
//            }
//            xmap.put(FilterArea.FIELD_LIST, xArray);
//            try {
//                Quanguo_Filter_Area = new FilterArea(xmap);
//            } catch (APIException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
            
        }
        return Quanguo_Filter_Area;
    }
    
    /**
     * POI、发现、酒店搜索都共用区域筛选数据
     */
    private static FilterArea Filter_Area;
    
    public static FilterArea getFilterArea() {
        return Filter_Area;
    }
    
    private static FilterCategoryOrder Filter_Category_Order_Tuangou;
    
    private static FilterCategoryOrder Filter_Category_Order_Dianying;
    
    private static FilterCategoryOrder Filter_Category_Order_Yanchu;
    
    private static FilterCategoryOrder Filter_Category_Order_Zhanlan;
    
    private static DiscoverConfigList Discover_Config_List;
    
    private static String Discover_Database_Version;
    
    private List<Filter> filterList = new ArrayList<Filter>();
    // POI Response End
    
    public static DiscoverConfigList getDiscoverConfigList() {
        return Discover_Config_List;
    }
    
    public boolean isTurnPage() {
        return isTurnPage;
    }
    
    public POI getPOI() {
        return poi;
    }

    public List<Filter> getFilterList() {
        return filterList;
    }

    public DataQuery(Context context) {
        super(context, API_TYPE_DATA_QUERY);
    }
    
    public void setup(Hashtable<String, String> criteria, int cityId, int sourceViewId, int targetViewId, String tipText, boolean isTurnpage, boolean needReconntection, POI poi) {
        super.setup(criteria, cityId, sourceViewId, targetViewId, tipText);
        this.poi = poi;
        this.cityId = cityId;
        this.isTurnPage = isTurnpage;
        this.needReconntection = needReconntection;
        initStaticField(this.criteria.get(SERVER_PARAMETER_DATA_TYPE), this.criteria.get(SERVER_PARAMETER_SUB_DATA_TYPE), this.cityId);
    }
    
    public static boolean checkDiscoveryCity(int cityId) {
        boolean exist = false;
        initDiscoverConfigList();
        if (Discover_Config_List != null) {
            List<DiscoverConfig> list = Discover_Config_List.getList();
            for(int i = list.size()-1; i >= 0; i--) {
                if (list.get(i).seqId == cityId) {
                    exist = true;
                    break;
                }
            }
        }
        return exist;
    }
    
    private static void initDiscoverConfigList() {
        synchronized (Filter_Lock) {
            try {
                if (Discover_Config_List == null) {
                    String path = TKConfig.getDataPath(false) + "discoverConfigList.xml";
                    File file = new File(path);
                    if (file.exists()) {
                        FileInputStream fis = new FileInputStream(file);
                        try {
                            Discover_Config_List = DiscoverResponse.parseDiscoverConfigList(fis);
                        } finally {
                            if (null != fis) {
                                try {
                                    fis.close();
                                } catch (IOException e) {
                                    // Ignore
                                    LogWrapper.e("POIQuery", "setup() IOException caught while closing stream");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
    
    public static void initStaticField(String dataType, String subDataType, int cityId) {
        initStaticField(dataType, subDataType, cityId, null);
    }
    
    public static void initStaticField(String dataType, String subDataType, int cityId, Context context) {
        try {
            synchronized (Filter_Lock) {
                FilterArea filterDataArea = null;
                FilterCategoryOrder filterDataCategoryOrder = null;
                String filterFileKey = dataType;
                if (DATA_TYPE_POI.equals(dataType)) {
                	if (cityId == MapEngine.SW_ID_QUANGUO) {
                		filterDataArea = Quanguo_Filter_Area;
                	} else {
                        filterDataArea = Filter_Area;
                	}
                    if (SUB_DATA_TYPE_POI.equals(subDataType)) {
                        filterDataCategoryOrder = Filter_Category_Order_POI;
                    } else if (SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                        filterFileKey += ("_" + subDataType);
                        filterDataCategoryOrder = Filter_Category_Order_Hotel;
                    }
                } else if (DATA_TYPE_TUANGOU.equals(dataType)) {
                    filterDataArea = Filter_Area;
                    filterDataCategoryOrder = Filter_Category_Order_Tuangou;
                } else if (DATA_TYPE_DIANYING.equals(dataType)) {
                    filterDataArea = Filter_Area;
                    filterDataCategoryOrder = Filter_Category_Order_Dianying;
                } else if (DATA_TYPE_YANCHU.equals(dataType)) {
                    filterDataArea = Filter_Area;
                    filterDataCategoryOrder = Filter_Category_Order_Yanchu;
                } else if (DATA_TYPE_ZHANLAN.equals(dataType)) {
                    filterDataArea = Filter_Area;
                    filterDataCategoryOrder = Filter_Category_Order_Zhanlan;
                } else if (DATA_TYPE_DISCOVER.equals(dataType)) {
                    initDiscoverConfigList();
                    
                    if (TextUtils.isEmpty(Discover_Database_Version)) {
                        Discover_Database_Version = Utility.readFile(new FileInputStream(TKConfig.getDataPath(true) + "discoverDatabaseVersion"));
                    }
                }
                
                if (filterDataCategoryOrder == null) {
                    String name = String.format(TKConfig.FILTER_FILE, filterFileKey, MapEngine.SW_ID_QUANGUO);
                    String path = MapEngine.cityId2Floder(MapEngine.SW_ID_QUANGUO) + name;
                    File file = new File(path);

                    // 硬编码酒店的分类筛选项
                    if (file.exists() == false) {
                        if (DATA_TYPE_POI.equals(dataType) &&
                                SUB_DATA_TYPE_HOTEL.equals(subDataType) &&
                                context != null) {
                            AssetManager am = context.getAssets();
                            String mapPath = TKConfig.getDataPath(true);
                            Utility.unZipFile(am, "tigermap.zip", mapPath, name);
                        }
                    }
                    
                    if (file.exists()) {
                        FileInputStream fis = new FileInputStream(file);
                        try {
                            XMap xmap = (XMap)XMap.readFrom(new ByteReader(fis, TKConfig.getEncoding()));
                            filterDataCategoryOrder = new FilterCategoryOrder(xmap);
                            if (filterDataCategoryOrder.getVersion() == null ||
                                    filterDataCategoryOrder.getCategoryFilterOption().isEmpty() ||
                                    filterDataCategoryOrder.getOrderFilterOption().isEmpty()) {
                                filterDataCategoryOrder = null;
                            }
                        } finally {
                            if (null != fis) {
                                try {
                                    fis.close();
                                } catch (IOException e) {
                                    // Ignore
                                    LogWrapper.e("POIQuery", "setup() IOException caught while closing stream");
                                }
                            }
                        }
                    }
                }
                
                if (filterDataArea == null || filterDataArea.cityId != cityId){
                    filterDataArea = null;
                    String path = MapEngine.cityId2Floder(cityId) + String.format(TKConfig.FILTER_FILE, DATA_TYPE_POI, cityId);
                    File file = new File(path);

                    // 硬编码部分区域筛选项（全部区域、我的当前位置、指定位置）
                    if (file.exists() == false) {
                        String quanguo = String.format(TKConfig.FILTER_FILE, "0", MapEngine.SW_ID_QUANGUO);
                        path = MapEngine.cityId2Floder(MapEngine.SW_ID_QUANGUO) + quanguo;
                        file = new File(path);
                        if (context != null && file.exists() == false) {
                            AssetManager am = context.getAssets();
                            String mapPath = TKConfig.getDataPath(true);
                            Utility.unZipFile(am, "tigermap.zip", mapPath, quanguo);
                        }
                    }
                    
                    if (file.exists()) {
                        FileInputStream fis = new FileInputStream(file);
                        try {
                            XMap xmap = (XMap)XMap.readFrom(new ByteReader(fis, TKConfig.getEncoding()));
                            filterDataArea = new FilterArea(xmap);
                            if (filterDataArea.getVersion() == null ||
                                    filterDataArea.getAreaFilterOption().isEmpty()) {
                                filterDataArea = null;
                            } else {
                                filterDataArea.cityId = cityId;
                            }
                            // 生成客户端预置的区域筛选项数据文件
//                            String resetPath = MapEngine.cityId2Floder(9999) + String.format(TKConfig.FILTER_FILE, "0", 9999);
//                            if (filterDataArea != null) {
//                                if (!TextUtils.isEmpty(resetPath)) {
//                                    XMap resetXMap = filterDataArea.getPresetData();
//                                    byte[] resetData = ByteUtil.xobjectToByte(resetXMap);
//                                    
//                                    Utility.writeFile(resetPath, resetData, true);
//                                }
//                            }
                        } finally {
                            if (null != fis) {
                                try {
                                    fis.close();
                                } catch (IOException e) {
                                    // Ignore
                                    LogWrapper.e("POIQuery", "setup() IOException caught while closing stream");
                                }
                            }
                        }
                    }
                }
                
                if (DATA_TYPE_POI.equals(dataType)) {
                	if (cityId == MapEngine.SW_ID_QUANGUO) {
                        Quanguo_Filter_Area = filterDataArea;
                	} else {
                		Filter_Area = filterDataArea;
                	}
                    if (SUB_DATA_TYPE_POI.equals(subDataType)) {
                        Filter_Category_Order_POI = filterDataCategoryOrder;
                    } else if (SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                        Filter_Category_Order_Hotel = filterDataCategoryOrder;   
                    }
                } else if (DATA_TYPE_TUANGOU.equals(dataType)) {
                    Filter_Area = filterDataArea;
                    Filter_Category_Order_Tuangou = filterDataCategoryOrder;
                } else if (DATA_TYPE_DIANYING.equals(dataType)) {
                    Filter_Area = filterDataArea;
                    Filter_Category_Order_Dianying = filterDataCategoryOrder;
                } else if (DATA_TYPE_YANCHU.equals(dataType)) {
                    Filter_Area = filterDataArea;
                    Filter_Category_Order_Yanchu = filterDataCategoryOrder;
                } else if (DATA_TYPE_ZHANLAN.equals(dataType)) {
                    Filter_Area = filterDataArea;
                    Filter_Category_Order_Zhanlan = filterDataCategoryOrder;
                }
            }
        } catch (Exception e) {
            LogWrapper.e("POIQuery", "setup() IOException caught while read filter file");
        }
    }
    
    @Override
    protected void addMyLocationParameters() {
        if (isTurnPage == false) {
            String dataType = this.criteria.get(SERVER_PARAMETER_DATA_TYPE);
            if (DATA_TYPE_FENDIAN.equals(dataType) || DATA_TYPE_YINGXUN.equals(dataType) ||
                    (SUB_DATA_TYPE_HOTEL.equals(this.criteria.get(SERVER_PARAMETER_SUB_DATA_TYPE)) && criteria.containsKey(SERVER_PARAMETER_LOCATION_CITY))) {
                if (criteria.containsKey(SERVER_PARAMETER_LOCATION_CITY)
                        && criteria.containsKey(SERVER_PARAMETER_LOCATION_LONGITUDE)
                        && criteria.containsKey(SERVER_PARAMETER_LOCATION_LATITUDE)) {
                    requestParameters.add(SERVER_PARAMETER_LOCATION_CITY, criteria.get(SERVER_PARAMETER_LOCATION_CITY));
                    requestParameters.add(SERVER_PARAMETER_LOCATION_LONGITUDE, criteria.get(SERVER_PARAMETER_LOCATION_LONGITUDE));
                    requestParameters.add(SERVER_PARAMETER_LOCATION_LATITUDE, criteria.get(SERVER_PARAMETER_LOCATION_LATITUDE));
                }
            } else {
                super.addMyLocationParameters();
            }
        } else {
            if (criteria.containsKey(SERVER_PARAMETER_LOCATION_CITY)
                    && criteria.containsKey(SERVER_PARAMETER_LOCATION_LONGITUDE)
                    && criteria.containsKey(SERVER_PARAMETER_LOCATION_LATITUDE)) {
                requestParameters.add(SERVER_PARAMETER_LOCATION_CITY, criteria.get(SERVER_PARAMETER_LOCATION_CITY));
                requestParameters.add(SERVER_PARAMETER_LOCATION_LONGITUDE, criteria.get(SERVER_PARAMETER_LOCATION_LONGITUDE));
                requestParameters.add(SERVER_PARAMETER_LOCATION_LATITUDE, criteria.get(SERVER_PARAMETER_LOCATION_LATITUDE));
            }
        }
    }
    
    @Override
    protected void addUUIDParameter() {
        if (isTurnPage == false) {
            super.addUUIDParameter();
        } else {
            if (criteria.containsKey(SERVER_PARAMETER_UUID) && criteria.containsKey(SERVER_PARAMETER_INDEX)) {
                String index = criteria.get(SERVER_PARAMETER_INDEX);
                String uuid = criteria.get(SERVER_PARAMETER_UUID);
                requestParameters.add(SERVER_PARAMETER_UUID, uuid+"_"+index);
            }
        }
    }

    @Override
    protected void makeRequestParameters() throws APIException {
        super.makeRequestParameters();
        addCommonParameters(requestParameters, cityId);
        
        if (criteria == null) {
            throw new APIException(APIException.CRITERIA_IS_NULL);
        }
        
        // 默认分页数目为TKConfig.getPageSize()
        String pageSize = addParameter(SERVER_PARAMETER_SIZE, false);
        if (pageSize == null) {
            requestParameters.add(SERVER_PARAMETER_SIZE, String.valueOf(TKConfig.getPageSize()));
        }
        
        String dataType = addParameter(SERVER_PARAMETER_DATA_TYPE);
        if (DATA_TYPE_POI.equals(dataType)) {     
            String subDataType = addParameter(SERVER_PARAMETER_SUB_DATA_TYPE);
            if (SUB_DATA_TYPE_POI.equals(subDataType)) {
                String idList = addParameter(SERVER_PARAMETER_ID_LIST, false);
                if (idList == null) {
                    requestParameters.add(SERVER_PARAMETER_NEED_FIELD, POI.NEED_FIELD);
                    requestParameters.add(SERVER_PARAMETER_COMMENT_VERSION, COMMENT_VERSION);
                    String bias = addParameter(SERVER_PARAMETER_BIAS, false);
                    if (bias == null) {
                        addParameter(new String[]{SERVER_PARAMETER_KEYWORD});
                        addParameter(new String[]{SERVER_PARAMETER_INFO}, false);
                    }
                    addParameter(SERVER_PARAMETER_POI_ID, false);
                }
            } else if (SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                String appendaction = addParameter(SERVER_PARAMETER_APPENDACTION, false);
                if (appendaction == null) {
                    requestParameters.add(SERVER_PARAMETER_NEED_FIELD, POI.NEED_FIELD+Hotel.NEED_FILED_LIST);
                    requestParameters.add(SERVER_PARAMETER_COMMENT_VERSION, COMMENT_VERSION);
                    requestParameters.add(SERVER_PARAMETER_PICTURE, 
                            Util.byteToHexString(Hotel.FIELD_IMAGE_THUMB)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_HOTEL_LIST)+"_[11000000000000000000]" + ";" +
                            Util.byteToHexString(Hotel.FIELD_IMAGE_LIST)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_HOTEL_DETAIL)+"_[00000000000000000000]");
                }
                addParameter(new String[]{SERVER_PARAMETER_CHECKIN, SERVER_PARAMETER_CHECKOUT});
            }
            
            String cfv = null;
            if (Filter_Area != null && Filter_Area.cityId == cityId) {
                cfv = Filter_Area.version;
            }
            String nfv = null;
            if (Filter_Category_Order_POI != null) {
                nfv = Filter_Category_Order_POI.version;
            }
            addFilterParameters(criteria, requestParameters, cfv, nfv);

        } else if (DATA_TYPE_DISCOVER.equals(dataType)) { 
            requestParameters.add(SERVER_PARAMETER_NEED_FIELD, DiscoverCategory.NEED_FILED);
            String pic = "";
            String cdv = "";
            if (Discover_Config_List != null) {
                cdv = Discover_Config_List.getVersion();
                List<DiscoverConfig> list = Discover_Config_List.getList();
                if (list != null) {
                    for(int i = 0, size = list.size(); i < size; i++) {
                        DiscoverConfig discoverConfig = list.get(i);
                        if (discoverConfig.getSeqId() == cityId) {
                            List<Long> values = discoverConfig.getList();
                            for(int j = 0, count = values.size(); j < count; j++) {
                                long value = values.get(j);
                                if (DATA_TYPE_TUANGOU.equals(String.valueOf(value))) {
                                    pic += "0";
                                } else if (DATA_TYPE_DIANYING.equals(String.valueOf(value))) {
                                    pic += "0";
                                } else if (DATA_TYPE_YANCHU.equals(String.valueOf(value))) {
                                    pic += "0";
                                } else if (DATA_TYPE_ZHANLAN.equals(String.valueOf(value))) {
                                    pic += "0";
                                }
                            }
                            break;
                        }
                    }
                }
            }
            requestParameters.add(SERVER_PARAMETER_DISCOVER_SUPPORT_DATATYPE, 
                    DATA_TYPE_TUANGOU+":"+DATA_TYPE_DIANYING+":"+DATA_TYPE_ZHANLAN+":"+DATA_TYPE_YANCHU);
            if (TextUtils.isEmpty(cdv) == false) {
                requestParameters.add(SERVER_PARAMETER_DISCOVER_POI_VERSION, cdv);
            }
            if (TextUtils.isEmpty(pic) == false) {
                requestParameters.add(SERVER_PARAMETER_PICTURE, 
                        Util.byteToHexString(DiscoverCategory.FIELD_DATA)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DISCOVER_HOME)+"_["+pic+"]");
            }
        } else if (DATA_TYPE_TUANGOU.equals(dataType)) { 
            addParameter(new String[]{SERVER_PARAMETER_INFO}, false);
            requestParameters.add(SERVER_PARAMETER_NEED_FIELD, Tuangou.NEED_FIELD);
            requestParameters.add(SERVER_PARAMETER_PICTURE, 
                    Util.byteToHexString(Tuangou.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_LIST)+"_[11000000000000000000]" + ";" +
                    Util.byteToHexString(Tuangou.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_DETAIL)+"_[00000000000000000000]");
            addDiscoverCategoryParameters(requestParameters);
            String cfv = null;
            if (Filter_Area != null && Filter_Area.cityId == cityId) {
                cfv = Filter_Area.version;
            }
            String nfv = null;
            if (Filter_Category_Order_Tuangou != null) {
                nfv = Filter_Category_Order_Tuangou.version;
            }
            addFilterParameters(criteria, requestParameters, cfv, nfv);
        } else if (DATA_TYPE_FENDIAN.equals(dataType)) { 
            requestParameters.add(SERVER_PARAMETER_NEED_FIELD, Fendian.NEED_FIELD);
            addDiscoverCategoryParameters(requestParameters);
            addParameter(SERVER_PARAMETER_TUANGOU_UUID);
            String cfv = null;
            if (Filter_Area != null && Filter_Area.cityId == cityId) {
                cfv = Filter_Area.version;
            }
            String nfv = null;
            if (Filter_Category_Order_Tuangou != null) {
                nfv = Filter_Category_Order_Tuangou.version;
            }
            addFilterParameters(criteria, requestParameters, cfv, nfv);
        } else if (DATA_TYPE_DIANYING.equals(dataType)) { 
            addParameter(new String[]{SERVER_PARAMETER_DIANYING_UUID, SERVER_PARAMETER_POI_ID}, false);
            requestParameters.add(SERVER_PARAMETER_NEED_FIELD, Dianying.NEED_FIELD);
            requestParameters.add(SERVER_PARAMETER_PICTURE, 
                    Util.byteToHexString(Dianying.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[11000000000000000000]" + ";" +
                    Util.byteToHexString(Dianying.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[00000000000000000000]");
            addDiscoverCategoryParameters(requestParameters);
            String cfv = null;
            if (Filter_Area != null && Filter_Area.cityId == cityId) {
                cfv = Filter_Area.version;
            }
            String nfv = null;
            if (Filter_Category_Order_Dianying != null) {
                nfv = Filter_Category_Order_Dianying.version;
            }
            addFilterParameters(criteria, requestParameters, cfv, nfv);
        } else if (DATA_TYPE_YINGXUN.equals(dataType)) { 
            requestParameters.add(SERVER_PARAMETER_NEED_FIELD, Yingxun.NEED_FIELD);
            addDiscoverCategoryParameters(requestParameters);
            addParameter(SERVER_PARAMETER_DIANYING_UUID);
            String cfv = null;
            if (Filter_Area != null && Filter_Area.cityId == cityId) {
                cfv = Filter_Area.version;
            }
            String nfv = null;
            if (Filter_Category_Order_Dianying != null) {
                nfv = Filter_Category_Order_Dianying.version;
            }
            addFilterParameters(criteria, requestParameters, cfv, nfv);
        } else if (DATA_TYPE_YANCHU.equals(dataType)) { 
            requestParameters.add(SERVER_PARAMETER_NEED_FIELD, Yanchu.NEED_FIELD);
            requestParameters.add(SERVER_PARAMETER_PICTURE, 
                    Util.byteToHexString(Yanchu.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[11000000000000000000]" + ";" +
                    Util.byteToHexString(Yanchu.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[00000000000000000000]");
            addDiscoverCategoryParameters(requestParameters);
            String cfv = null;
            if (Filter_Area != null && Filter_Area.cityId == cityId) {
                cfv = Filter_Area.version;
            }
            String nfv = null;
            if (Filter_Category_Order_Yanchu != null) {
                nfv = Filter_Category_Order_Yanchu.version;
            }
            addFilterParameters(criteria, requestParameters, cfv, nfv);
        } else if (DATA_TYPE_SHANGJIA.equals(dataType)) { 
            requestParameters.add(SERVER_PARAMETER_NEED_FIELD, TextUtils.isEmpty(Globals.g_Session_Id) ? Shangjia.NEED_FIELD_NO_LOGON : Shangjia.NEED_FIELD);
        } else if (DATA_TYPE_ZHANLAN.equals(dataType)) { 
            requestParameters.add(SERVER_PARAMETER_NEED_FIELD, Zhanlan.NEED_FIELD);
            requestParameters.add(SERVER_PARAMETER_PICTURE, 
                    Util.byteToHexString(Zhanlan.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[11000000000000000000]" + ";" +
                    Util.byteToHexString(Zhanlan.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[00000000000000000000]");
            addDiscoverCategoryParameters(requestParameters);
            String cfv = null;
            if (Filter_Area != null && Filter_Area.cityId == cityId) {
                cfv = Filter_Area.version;
            }
            String nfv = null;
            if (Filter_Category_Order_Zhanlan != null) {
                nfv = Filter_Category_Order_Zhanlan.version;
            }
            addFilterParameters(criteria, requestParameters, cfv, nfv);
        } else if (DATA_TYPE_DIANPING.equals(dataType)) {
            requestParameters.add(SERVER_PARAMETER_NEED_FIELD, Comment.NEED_FIELD);
            requestParameters.add(SERVER_PARAMETER_COMMENT_VERSION, COMMENT_VERSION);
            addParameter(new String[]{SERVER_PARAMETER_REFER});
            addParameter(new String[]{SERVER_PARAMETER_TIME, SERVER_PARAMETER_DIRECTION, SERVER_PARAMETER_POI_ID}, false);
        } else if (DATA_TYPE_PULL_MESSAGE.equals(dataType)) {
            addParameter(new String[]{SERVER_PARAMETER_LOCATION_CITY, SERVER_PARAMETER_LONGITUDE, SERVER_PARAMETER_LATITUDE, SERVER_PARAMETER_LOCATION_LONGITUDE, SERVER_PARAMETER_LOCATION_LATITUDE});
            addParameter(new String[]{SERVER_PARAMETER_MESSAGE_ID_LIST, SERVER_PARAMETER_LAST_PULL_DATE}, false);
        } else if (DATA_TYPE_ALTERNATIVE.equals(dataType)) {
            requestParameters.add(SERVER_PARAMETER_NEED_FIELD, Alternative.NEED_FIELD);
            addParameter(new String[]{SERVER_PARAMETER_KEYWORD});
        } else if (DATA_TYPE_COUPON.equals(dataType)) {
        	addParameter(SERVER_PARAMETER_POI_ID);
        	addParameter(SERVER_PARAMETER_NEED_FIELD);
        } else if (DATA_TYPE_FILTER.equals(dataType)) {
            addParameter(SERVER_PARAMETER_CONFIGINFO);
            
            String cfv = null;
            if (Filter_Area != null && Filter_Area.cityId == cityId) {
                cfv = Filter_Area.version;
            }
            String nfv = null;
            if (Filter_Category_Order_POI != null) {
                nfv = Filter_Category_Order_POI.version;
            }
            addFilterParameters(criteria, requestParameters, cfv, nfv, false);

        } else {
            throw APIException.wrapToMissingRequestParameterException("invalid data type.");
        }

        requestParameters.add(SERVER_PARAMETER_TIME_STAMP, TIME_STAMP_FORMAT.format(Calendar.getInstance().getTime()));
        addParameter(new String[]{SERVER_PARAMETER_LONGITUDE, SERVER_PARAMETER_LATITUDE}, false);
        
        addSessionId(false);
    }
    
    private void addDiscoverCategoryParameters(WeiboParameters requestParameters) {
        if (Discover_Config_List != null) {
            requestParameters.add(SERVER_PARAMETER_DISCOVER_POI_VERSION, Discover_Config_List.getVersion());
        }
        if (TextUtils.isEmpty(Discover_Database_Version) == false) {
            requestParameters.add(SERVER_PARAMETER_DISCOVER_BASEINDEX_VERSION, Discover_Database_Version);
        }
    }
    
    private void addFilterParameters(Hashtable<String, String> criteria, WeiboParameters requestParameters, String cfv, String nfv) throws APIException {
        addFilterParameters(criteria, requestParameters, cfv, nfv, true);
    }
    
    private void addFilterParameters(Hashtable<String, String> criteria, WeiboParameters requestParameters, String cfv, String nfv, boolean needIndex) throws APIException {
        if (criteria.containsKey(SERVER_PARAMETER_FILTER)) {
            requestParameters.add(SERVER_PARAMETER_FILTER, criteria.get(SERVER_PARAMETER_FILTER));
        }
        if (criteria.containsKey(SERVER_PARAMETER_FILTER_STRING)) {
            requestParameters.add(SERVER_PARAMETER_FILTER_STRING, criteria.get(SERVER_PARAMETER_FILTER_STRING));
        }
        if (TextUtils.isEmpty(cfv) == false) {
            requestParameters.add(SERVER_PARAMETER_CITY_FILTER_VERSION, cfv);
        }
        if (TextUtils.isEmpty(nfv) == false) {
            requestParameters.add(SERVER_PARAMETER_NATION_FILTER_VERSION, nfv);
        }
        if (criteria.containsKey(SERVER_PARAMETER_INDEX)) {
            requestParameters.add(SERVER_PARAMETER_INDEX, criteria.get(SERVER_PARAMETER_INDEX));
        } else if (needIndex) {
            throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_INDEX);
        }
    }

    @Override
    protected void createHttpClient() {
        super.createHttpClient();
        String url = String.format(TKConfig.getQueryUrl(), TKConfig.getQueryHost());
        httpClient.setURL(url);
    }

    @Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);
        String dataType = this.criteria.get(SERVER_PARAMETER_DATA_TYPE);
        String subDataType = this.criteria.get(SERVER_PARAMETER_SUB_DATA_TYPE);
        if (DATA_TYPE_POI.equals(dataType)) {

            FilterResponse filterResponse = null;
            if (SUB_DATA_TYPE_POI.equals(subDataType)) {
                filterResponse = new POIResponse(responseXMap);
            } else if (SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                filterResponse = new POIResponse(responseXMap);
            }
            translateFilter(filterResponse, dataType, subDataType, filterList);
            this.response = filterResponse;
        } else if (DATA_TYPE_TUANGOU.equals(dataType)) {
            TuangouResponse response = new TuangouResponse(responseXMap);
            translateFilter(response, dataType, subDataType, filterList);
            this.response = response;
        } else if (DATA_TYPE_FENDIAN.equals(dataType)) {
            FendianResponse response = new FendianResponse(responseXMap);
            this.response = response;
        } else if (DATA_TYPE_DIANYING.equals(dataType)) {
            DianyingResponse response = new DianyingResponse(responseXMap);
            translateFilter(response, dataType, subDataType, filterList);
            this.response = response;
        } else if (DATA_TYPE_YINGXUN.equals(dataType)) {
            YingxunResponse response = new YingxunResponse(responseXMap);
            this.response = response;
        } else if (DATA_TYPE_YANCHU.equals(dataType)) {
            YanchuResponse response = new YanchuResponse(responseXMap);
            translateFilter(response, dataType, subDataType, filterList);
            this.response = response;
        } else if (DATA_TYPE_ZHANLAN.equals(dataType)) {
            ZhanlanResponse response = new ZhanlanResponse(responseXMap);
            translateFilter(response, dataType, subDataType, filterList);
            this.response = response;
        } else if (DATA_TYPE_SHANGJIA.equals(dataType)) {
            ShangjiaResponse response = new ShangjiaResponse(responseXMap);
            this.response = response;
        } else if (DATA_TYPE_DIANPING.equals(dataType)) {
            CommentResponse response = new CommentResponse(responseXMap);
            this.response = response;
        } else if (DATA_TYPE_DISCOVER.equals(dataType)) {
            DiscoverResponse response = new DiscoverResponse(responseXMap);
            this.response = response;
        } else if (DATA_TYPE_PULL_MESSAGE.equals(dataType)) {
            PullMessage pullMessage = new PullMessage(responseXMap);
            this.response = pullMessage;
        } else if (DATA_TYPE_ALTERNATIVE.equals(dataType)) {
            this.response = new AlternativeResponse(responseXMap);
        } else if (DATA_TYPE_COUPON.equals(dataType)) {
            this.response = new CouponResponse(responseXMap);
        } else if (DATA_TYPE_FILTER.equals(dataType)) {
            FilterConfigResponse response = new FilterConfigResponse(responseXMap);
            translateFilter(response.getFilterResponse(), dataType, subDataType, filterList);
            this.response = response;
        }
    }
    
    private void translateFilter(FilterResponse baseResponse, String dataType, String subDataType, List<Filter> filterList) throws APIException {
        filterList.clear();
        synchronized (Filter_Lock) {
            try {      
                FilterArea staticFilterDataArea = null;
                FilterCategoryOrder staticFilterDataCategoryOrder = null;
                String filterFileKey = dataType;
                if (DATA_TYPE_POI.equals(dataType)) {
                    staticFilterDataArea = Filter_Area;
                    if (SUB_DATA_TYPE_POI.equals(subDataType)) {
                        staticFilterDataCategoryOrder = Filter_Category_Order_POI;
                    } else if (SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                        filterFileKey += ("_" + subDataType);
                        staticFilterDataCategoryOrder = Filter_Category_Order_Hotel;
                    }
                } else if (DATA_TYPE_TUANGOU.equals(dataType)) {
                    staticFilterDataArea = Filter_Area;
                    staticFilterDataCategoryOrder = Filter_Category_Order_Tuangou;
                } else if (DATA_TYPE_DIANYING.equals(dataType)) {
                    staticFilterDataArea = Filter_Area;
                    staticFilterDataCategoryOrder = Filter_Category_Order_Dianying;
                } else if (DATA_TYPE_YANCHU.equals(dataType)) {
                    staticFilterDataArea = Filter_Area;
                    staticFilterDataCategoryOrder = Filter_Category_Order_Yanchu;
                } else if (DATA_TYPE_ZHANLAN.equals(dataType)) {
                    staticFilterDataArea = Filter_Area;
                    staticFilterDataCategoryOrder = Filter_Category_Order_Zhanlan;
                } else if (DATA_TYPE_FILTER.equals(dataType)) {
                    staticFilterDataArea = Filter_Area;
                }
                // 将城市区域筛选数据写入相应文件夹
                FilterArea filterDataArea = baseResponse.getFilterDataArea();
                String path = MapEngine.cityId2Floder(cityId) + String.format(TKConfig.FILTER_FILE, DATA_TYPE_POI, cityId);
                if (filterDataArea != null) {
                    staticFilterDataArea = filterDataArea;
                    staticFilterDataArea.cityId = cityId;
                    if (!TextUtils.isEmpty(path)) {
                        XMap filter = filterDataArea.getData();
                        byte[] data = ByteUtil.xobjectToByte(filter);
                        
                        Utility.writeFile(path, data, true);
                    }
                }
                
                if (staticFilterDataArea != null && staticFilterDataArea.cityId == cityId) {
                    List<Long> list = baseResponse.getFilterAreaIndex();
                    if (list != null && list.size() > 0) {
                        filterList.add(makeFilterResponse(context, list, staticFilterDataArea.getVersion(), staticFilterDataArea.getAreaFilterOption(), FilterArea.FIELD_LIST));
                    }
                }
                
                // 将分类排序筛选数据写入全国文件夹
                FilterCategoryOrder filterDataCategoryOrder = baseResponse.getFilterDataCategoryOrder();
                path = MapEngine.cityId2Floder(MapEngine.SW_ID_QUANGUO) + String.format(TKConfig.FILTER_FILE, filterFileKey, MapEngine.SW_ID_QUANGUO);
                if (filterDataCategoryOrder != null) {
                    staticFilterDataCategoryOrder = filterDataCategoryOrder;
                    if (!TextUtils.isEmpty(path)) {
                        
                        XMap filter = filterDataCategoryOrder.getData();
                        byte[] data = ByteUtil.xobjectToByte(filter);
                        
                        Utility.writeFile(path, data, true);
                    }
                }
                
                if (staticFilterDataCategoryOrder != null) {
        
                    List<Long> list = baseResponse.getFilterCategoryIndex();
                    if (list != null && list.size() > 0) {
                        filterList.add(makeFilterResponse(context, list, staticFilterDataCategoryOrder.getVersion(), staticFilterDataCategoryOrder.getCategoryFilterOption(), FilterCategoryOrder.FIELD_LIST_CATEGORY, BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType) == false));
                    }
                    list = baseResponse.getFilterOrderIndex();
                    if (list != null && list.size() > 0) {
                        filterList.add(makeFilterResponse(context, list, staticFilterDataCategoryOrder.getVersion(), staticFilterDataCategoryOrder.getOrderFilterOption(), FilterCategoryOrder.FIELD_LIST_ORDER));
                    }
                }  

                
                if (DATA_TYPE_POI.equals(dataType)) {
                    Filter_Area = staticFilterDataArea;
                    if (SUB_DATA_TYPE_POI.equals(subDataType)) {
                        Filter_Category_Order_POI = staticFilterDataCategoryOrder;
                    } else if (SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                        Filter_Category_Order_Hotel = staticFilterDataCategoryOrder;
                    }
                } else if (DATA_TYPE_TUANGOU.equals(dataType)) {
                    Filter_Area = staticFilterDataArea;
                    Filter_Category_Order_Tuangou = staticFilterDataCategoryOrder;
                } else if (DATA_TYPE_DIANYING.equals(dataType)) {
                    Filter_Area = staticFilterDataArea;
                    Filter_Category_Order_Dianying = staticFilterDataCategoryOrder;
                } else if (DATA_TYPE_YANCHU.equals(dataType)) {
                    Filter_Area = staticFilterDataArea;
                    Filter_Category_Order_Yanchu = staticFilterDataCategoryOrder;
                } else if (DATA_TYPE_ZHANLAN.equals(dataType)) {
                    Filter_Area = staticFilterDataArea;
                    Filter_Category_Order_Zhanlan = staticFilterDataCategoryOrder;
                } else if (DATA_TYPE_FILTER.equals(dataType)) {
                    Filter_Area = staticFilterDataArea;
                }
            } catch (Exception e) {
                throw new APIException(e);
            }
        }
    }
    
    public static String makeFilterRequest(List<Filter> filterList) {
        return makeFilterRequest(filterList, Byte.MIN_VALUE);
    }
    
    public static String makeFilterRequest(List<Filter> filterList, byte key) {
        StringBuilder s = new StringBuilder();
        if (filterList == null) {
            return s.toString();
        }
        
        for(int i = 0, size = filterList.size(); i < size; i++) {
            Filter filter = filterList.get(i);
            if (filter.key != key) {
                int id = -1;
                List<Filter> childrenFilterList1 = filter.getChidrenFilterList();
                for(Filter childrenFilter1 : childrenFilterList1) {
                    if (childrenFilter1.isSelected()) {
                        id = childrenFilter1.getFilterOption().getId();
                        break;
                    } else {
                        List<Filter> childrenFilterList2 = childrenFilter1.getChidrenFilterList();
                        if (childrenFilterList2 != null) {
                            for(Filter childrenFilter2 : childrenFilterList2) {
                                if (childrenFilter2.isSelected()) {
                                    id = childrenFilter2.getFilterOption().getId();
                                    break;
                                }
                            }
                            if (id >= 0) {
                                break;
                            }
                        }
                    }
                }
                
                if (id >= 0) {
                    if (s.length() > 0) {
                        s.append(';');
                    }
                    s.append(Util.byteToHexString(filter.getKey()));
                    s.append(':');
                    s.append(id);
                }
            }
        }

        return s.toString();
    }

    public static Filter makeFilterResponse(Context context, List<Long> indexList, String version, List<FilterOption> filterOptionList, byte key) {
        return makeFilterResponse(context, indexList, version, filterOptionList, key, true);
    }
    
    public static Filter makeFilterResponse(Context context, List<Long> indexList, String version, List<FilterOption> filterOptionList, byte key, boolean addAllAnyone) {

        Filter filter = new Filter();
        filter.version = version;
        filter.key = key;
        boolean isAreaFilter = (key == POIResponse.FIELD_FILTER_AREA_INDEX);
        if (indexList != null && filterOptionList != null) {
            Filter parentFilter = null;            
            FilterOption filterOption;
            
            long selectedId = indexList.get(0);
            
            Filter allArea = null;
            // 从1开始，是因为第0个值，是选中项的ID，特殊用途。
            for(int i = 1, size = indexList.size(); i < size; i++) {
                long index = indexList.get(i);
                if (index < filterOptionList.size()) {
                    filterOption = filterOptionList.get((int)index);
    
                    Filter tempFilter = new Filter();
                    tempFilter.filterOption = filterOption;
                    
                    if (selectedId == filterOption.getId()) {
                        tempFilter.selected = true;
                    }
                    
                    if (filterOption.getParent() == -1 || filterOption.getParent() == -2) {
                        filter.chidrenFilterList.add(tempFilter);
                        parentFilter = tempFilter;
                    } else if (parentFilter != null){
                        parentFilter.chidrenFilterList.add(tempFilter);
                    }
                    
                    if (isAreaFilter && tempFilter.getFilterOption().getId() == 0) {
                    	allArea = tempFilter;
                    }
                }
            }
            
            // 将子筛选项，加入到全部区域下边。
            if (allArea != null) {
            	List<Filter> list = allArea.getChidrenFilterList();
            	List<Filter> chidrenFilterList = filter.getChidrenFilterList();
                for(Filter chidrenFilter : chidrenFilterList) {
                    if (chidrenFilter.getChidrenFilterList().size() > 0 && chidrenFilter.getFilterOption().id > 10) {
                    	list.addAll(chidrenFilter.getChidrenFilterList());
                    }
                }
                sortFilterList(list);
                
                FilterOption dupAllAreaFilterOpt = new FilterOption();
                dupAllAreaFilterOpt.setName(allArea.getFilterOption().getName());
                
                int id = allArea.getFilterOption().getId();
                dupAllAreaFilterOpt.setId(id);
                
                Filter filter1 = new Filter();
                filter1.filterOption = dupAllAreaFilterOpt;
                filter1.selected = allArea.selected;
                allArea.selected = false;
                list.add(0, filter1);
                
            }
            
            if (addAllAnyone) {
                // 增加全部
                List<Filter> chidrenFilterList = filter.getChidrenFilterList();
                for(Filter chidrenFilter : chidrenFilterList) {
                    if (chidrenFilter.getFilterOption().getId() != 0 &&
                    		chidrenFilter.getChidrenFilterList().size() > 0 &&
                            chidrenFilter.getFilterOption().getParent() == -1) {
                        FilterOption filterOption1 = new FilterOption();
                        filterOption1.setName(context.getString(R.string.all_anyone, ""));
                        
                        int id = chidrenFilter.getFilterOption().getId();
                        filterOption1.setId(id);
                        filterOption1.setParent(id);
                        
                        Filter filter1 = new Filter();
                        filter1.filterOption = filterOption1;
                        filter1.selected = chidrenFilter.selected;
                        chidrenFilter.getChidrenFilterList().add(0, filter1);
                        chidrenFilter.selected = false;
                    }
                }
            }
        }
        return filter;
    }

    /**
     * 按照拼音对Filter列表进行排序
     * @param filters
     */
    private static void sortFilterList(List<Filter> filters){
    	Collections.sort(filters, new Comparator<Filter>() {

			@Override
			public int compare(Filter lhs, Filter rhs) {
				if(lhs.getFilterOption().firstChar==0){
					lhs.getFilterOption().firstChar = HanziUtil.getFirstPinYinChar(lhs.getFilterOption().getName());
				}
				if(rhs.getFilterOption().firstChar==0){
					rhs.getFilterOption().firstChar = HanziUtil.getFirstPinYinChar(rhs.getFilterOption().getName());
				}
				return lhs.getFilterOption().firstChar-rhs.getFilterOption().firstChar;
			}
		});
    }
    public static class Filter {        
        byte key;
        boolean selected = false;
        String version;
        FilterOption filterOption;

        List<Filter> chidrenFilterList = new ArrayList<Filter>();
        
        public byte getKey() {
            return key;
        }

        public void setKey(byte key) {
            this.key = key;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public String getVersion() {
            return version;
        }

        public FilterOption getFilterOption() {
            return filterOption;
        }

        public List<Filter> getChidrenFilterList() {
            return chidrenFilterList;
        }

        public Filter clone() {
            Filter filter = new Filter();
            filter.key = key;
            filter.selected = selected;
            filter.version = version;
            if (filterOption != null) {
                filter.filterOption = filterOption.clone();
            }
            for(Filter childrenFilter : chidrenFilterList) {
                filter.chidrenFilterList.add(childrenFilter.clone());
            }
            return filter;
        }
        
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (object instanceof Filter) {
                Filter other = (Filter) object;
                if (this.key == other.key 
//                        && this.selected == other.selected
                        && ((this.version != null && this.version.equals(other.version))
                                || (this.version == null && other.version == null))
                        && ((this.filterOption != null && this.filterOption.equals(other.filterOption))
                                || (this.filterOption == null && other.filterOption == null))) {
                    if (this.chidrenFilterList.size() == other.chidrenFilterList.size()) {
                        for(int i = this.chidrenFilterList.size()-1; i >= 0; i--) {
                            if (this.chidrenFilterList.get(i).equals(other.chidrenFilterList.get(i)) == false) {
                                return false;   
                            }
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static class FilterOption extends XMapData {
        // 0x01 x_string name
        public static final byte FIELD_NAME = 0x01;

        // 0x02 x_int parent，-1表示没有parent
        public static final byte FIELD_PARENT = 0x02;

        private int id;

        private String name;

        private int parent;
        
        public char firstChar = 0;

        public FilterOption() {
        }

        public FilterOption(XMap data) throws APIException {
            super(data);

            name = getStringFromData(FIELD_NAME);
            parent = (int)getLongFromData(FIELD_PARENT);
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getParent() {
            return parent;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setParent(int parent) {
            this.parent = parent;
        }

        public FilterOption clone() {
            FilterOption filterOption = new FilterOption();
            filterOption.id = id;
            filterOption.name = name;
            filterOption.parent = parent;
            return filterOption;
        }
        
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            
            if (object instanceof FilterOption) {
                FilterOption other = (FilterOption) object;
                if (this.id == other.id 
                        && this.parent == other.parent 
                        && ((this.name != null && this.name.equals(other.name)) 
                                || (this.name == null && other.name == null))) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            return "FilterOption[id=" + id + " ,name=" + name + " ,parent=" + parent + "]";
        }
    }

    public static class FilterArea extends XMapData {

        // 0x01 x_string 区域筛选项版本
        public static final byte FIELD_VERSION = 0x01;

        // 0x11 x_array<x_map> 区域筛选项
        public static final byte FIELD_LIST = 0x11;

        private String version;

        private List<FilterOption> areaFilterOption;

        public int cityId;

        @SuppressWarnings("unchecked")
        public FilterArea(XMap data) throws APIException {
            super(data);

            version = getStringFromData(FIELD_VERSION);

            areaFilterOption = new ArrayList<FilterOption>();
            if (this.data.containsKey(FIELD_LIST)) {
                XArray<XMap> xArray = this.data.getXArray(FIELD_LIST);
                for (int i = 0, size = xArray.size(); i < size; i++) {
                    FilterOption filterOption = new FilterOption(xArray.get(i));
                    filterOption.id = i;
                    areaFilterOption.add(filterOption);
                }
            }
        }
        
        /**
         * 生成客户端预置的区域筛选项数据
         * @return
         */
        @SuppressWarnings("unchecked")
		public XMap getPresetData() {
        	XMap xmap = new XMap();
        	xmap.put(FIELD_VERSION, "0.0.0");
        	if (this.data.containsKey(FIELD_LIST)) {
        		XArray<XMap> xArray1 = new XArray<XMap>();
                XArray<XMap> xArray = this.data.getXArray(FIELD_LIST);
                for (int i = 0; i < 11; i++) {
                	xArray1.add(xArray.get(i));
                }
                xmap.put(FIELD_LIST, xArray1);
            }
        	return xmap;
        }

        public XMap getData() {
            return data;
        }

        public String getVersion() {
            return version;
        }

        public List<FilterOption> getAreaFilterOption() {
            return areaFilterOption;
        }

    }

    public static class FilterCategoryOrder extends XMapData {

        // 0x01 x_string 分类和排序筛选项版本
        public static final byte FIELD_VERSION = 0x01;

        // 0x12 x_array<x_map> 分类筛选项
        public static final byte FIELD_LIST_CATEGORY = 0x12;

        // 0x13 x_array<x_map> 排序筛选项
        public static final byte FIELD_LIST_ORDER = 0x13;

        private String version;

        private List<FilterOption> categoryFilterOption;

        private List<FilterOption> orderFilterOption;

        @SuppressWarnings("unchecked")
        public FilterCategoryOrder(XMap data) throws APIException {
            super(data);

            version = getStringFromData(FIELD_VERSION);

            categoryFilterOption = new ArrayList<FilterOption>();
            if (this.data.containsKey(FIELD_LIST_CATEGORY)) {
                XArray<XMap> xArray = this.data.getXArray(FIELD_LIST_CATEGORY);
                for (int i = 0, size = xArray.size(); i < size; i++) {
                    FilterOption filterOption = new FilterOption(xArray.get(i));
                    filterOption.id = i;
                    categoryFilterOption.add(filterOption);
                }
            }

            orderFilterOption = new ArrayList<FilterOption>();
            if (this.data.containsKey(FIELD_LIST_ORDER)) {
                XArray<XMap> xArray = this.data.getXArray(FIELD_LIST_ORDER);
                for (int i = 0, size = xArray.size(); i < size; i++) {
                    FilterOption filterOption = new FilterOption(xArray.get(i));
                    filterOption.id = i;
                    orderFilterOption.add(filterOption);
                }
            }
        }

        public XMap getData() {
            return data;
        }

        public String getVersion() {
            return version;
        }

        public List<FilterOption> getCategoryFilterOption() {
            return categoryFilterOption;
        }

        public List<FilterOption> getOrderFilterOption() {
            return orderFilterOption;
        }

    }
    
    public static class BaseList extends XMapData {

        // 0x01 x_int 总数 
        public static final byte FIELD_TOTAL = 0x01;

        // 0x03 x_string 信息提示
        public static final byte FIELD_MESSAGE = 0x03;

        protected long total;

        protected String message;

        public BaseList(XMap data) throws APIException {
            super(data);

            this.total = getLongFromData(FIELD_TOTAL);
            this.message = getStringFromData(FIELD_MESSAGE);
        }

        public long getTotal() {
            return total;
        }

        public String getMessage() {
            return message;
        }        
    }

    public static class FilterResponse extends Response {
        
        // 0x11 x_array<x_int> 区域筛选下标（第一个是选中的）
        public static final byte FIELD_FILTER_AREA_INDEX = 0x11;

        // 0x12 x_array<x_int> 分类筛选下标（第一个是选中的）
        public static final byte FIELD_FILTER_CATEGORY_INDEX = 0x12;

        // 0x13 x_array<x_int> 排序筛选下标（第一个是选中的）
        public static final byte FIELD_FILTER_ORDER_INDEX = 0x13;

        // 0x21 x_map 城市地片筛选数据
        public static final byte FIELD_FILTER_AREA = 0x21;

        // 0x22 x_map 分类和排序筛选数据
        public static final byte FIELD_FILTER_CATEGORY_ORDER = 0x22;

        protected List<Long> filterAreaIndex;

        protected List<Long> filterCategoryIndex;

        protected List<Long> filterOrderIndex;

        protected FilterArea filterDataArea;

        protected FilterCategoryOrder filterDataCategoryOrder;

        public List<Long> getFilterAreaIndex() {
            return filterAreaIndex;
        }

        public List<Long> getFilterCategoryIndex() {
            return filterCategoryIndex;
        }

        public List<Long> getFilterOrderIndex() {
            return filterOrderIndex;
        }

        public FilterArea getFilterDataArea() {
            return filterDataArea;
        }

        public FilterCategoryOrder getFilterDataCategoryOrder() {
            return filterDataCategoryOrder;
        }

        @SuppressWarnings("unchecked")
        public FilterResponse(XMap data) throws APIException {
            super(data);

            if (this.data.containsKey(FIELD_FILTER_AREA_INDEX)) {
                filterAreaIndex = this.data.getXArray(FIELD_FILTER_AREA_INDEX).toIntList();
            }
            if (this.data.containsKey(FIELD_FILTER_CATEGORY_INDEX)) {
                filterCategoryIndex = this.data.getXArray(FIELD_FILTER_CATEGORY_INDEX).toIntList();
            }
            if (this.data.containsKey(FIELD_FILTER_ORDER_INDEX)) {
                filterOrderIndex = this.data.getXArray(FIELD_FILTER_ORDER_INDEX).toIntList();
            }
            if (this.data.containsKey(FIELD_FILTER_AREA)) {
                this.filterDataArea = new FilterArea(this.data.getXMap(FIELD_FILTER_AREA));
            }
            if (this.data.containsKey(FIELD_FILTER_CATEGORY_ORDER)) {
                this.filterDataCategoryOrder = new FilterCategoryOrder(this.data.getXMap(FIELD_FILTER_CATEGORY_ORDER));
            }
        }
    }

    public static class DiscoverCategoreResponse extends FilterResponse {
        
        // 0x03 x_string    dbv，即database version，数据版本号（以索引目录为种子生成的uuid） 
        public static final byte FIELD_DATABASE_VERSION = 0x03;

        protected String databaseVersion;

        public String getDatabaseVersion() {
            return databaseVersion;
        }
        
        public DiscoverCategoreResponse(XMap data) throws APIException {
            super(data);

            databaseVersion = getStringFromData(FIELD_DATABASE_VERSION);
            if (databaseVersion != null) {
                String path = TKConfig.getDataPath(true) + "discoverDatabaseVersion";
                try {
                    Utility.writeFile(path, databaseVersion.getBytes(TKConfig.getEncoding()), true);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Discover_Database_Version = databaseVersion;
            }
        }
    }

    public static class POIResponse extends FilterResponse {
        // 0x02 x_map A类poi结果
        public static final byte FIELD_A_POI_LIST = 0x02;

        // 0x03 x_map B类poi结果（格式同上）
        public static final byte FIELD_B_POI_LIST = 0x03;
        
        // 0x23 x_array<x_int>
        // 与请求idlist相关，反映id们属性的字段，用整数表示属性，第1个bit为1表示无效，第2bit为1表示不存在，第3bit为1表示金戳，第4bit为1表示银戳(从低位开始)
        private static final byte FIELD_ID_LIST = 0x23;

        private POIList aPOIList;

        private POIList bPOIList;

        private List<Long> idList;

        public POIList getAPOIList() {
            return aPOIList;
        }

        public void setAPOIList(POIList poiList) {
            this.aPOIList = poiList;
        }

        public POIList getBPOIList() {
            return bPOIList;
        }

        public void setBPOIList(POIList poiList) {
            this.bPOIList = poiList;
        }
        
        public List<Long> getIdList() {
            return idList;
        }

        @SuppressWarnings("unchecked")
        public POIResponse(XMap data) throws APIException {
            super(data);

            if (this.data.containsKey(FIELD_A_POI_LIST)) {
                this.aPOIList = new POIList(this.data.getXMap(FIELD_A_POI_LIST), FIELD_A_POI_LIST);
            }
            if (this.data.containsKey(FIELD_B_POI_LIST)) {
                this.bPOIList = new POIList(this.data.getXMap(FIELD_B_POI_LIST), FIELD_B_POI_LIST);
            }
            if (this.data.containsKey(FIELD_ID_LIST)) {
                this.idList = this.data.getXArray(FIELD_ID_LIST).toIntList();
            }
        }

        public static class POIList extends BaseList {

            // 0x02 x_array<x_map> poi列表
            public static final byte FIELD_LIST = 0x02;

            // 0x04 | x_string | 短信息提示
            public static final byte FIELD_SHORT_MESSAGE = 0x04;

            private List<POI> list;

            private int resultType;
            
            private String shortMessage;

            public String getShortMessage() {
                return shortMessage;
            }

            public List<POI> getList() {
                return list;
            }

            public void setList(List<POI> list) {
                this.list = list;
            }

            public POIList(XMap data, int resultType) throws APIException {
                super(data);
                this.resultType = resultType;

                this.list = getListFromData(FIELD_LIST, POI.Initializer);
                if (this.list != null) {
                    for (int i = 0, size = list.size(); i < size; i++) {
                        list.get(i).setResultType(this.resultType);
                    }
                }
                this.shortMessage = getStringFromData(FIELD_SHORT_MESSAGE);
            }

        }
    }

    public static class DiscoverResult extends BaseList {
        // 0x04 x_int 附近团购数量
        public static final byte FIELD_TOTAL_NEARBY = 0x04;

        // 0x05 x_int 全市团购数量
        public static final byte FIELD_TOTAL_CITY = 0x05;

        protected long totalNearby;

        protected long totalCity;

        public DiscoverResult(XMap data) throws APIException {
            super(data);

            this.totalNearby = getLongFromData(FIELD_TOTAL_NEARBY);
            this.totalCity = getLongFromData(FIELD_TOTAL_CITY);
        }

        public long getTotalNearby() {
            return totalNearby;
        }

        public long getTotalCity() {
            return totalCity;
        }
    }
    
    public static class TuangouResponse extends DiscoverCategoreResponse {
        // 0x02    x_map   团购结果
        public static final byte FIELD_LIST = 0x02;

        private TuangouList list;

        public TuangouResponse(XMap data) throws APIException {
            super(data);

            if (this.data.containsKey(FIELD_LIST)) {
                this.list = new TuangouList(this.data.getXMap(FIELD_LIST));
            }
        }

        public TuangouList getList() {
            return list;
        }

        public static class TuangouList extends BaseList {

            // 0x02 x_array<x_map> 列表
            public static final byte FIELD_LIST = 0x02;

            private List<Tuangou> list;

            public TuangouList(XMap data) throws APIException {
                super(data);

                this.list = getListFromData(FIELD_LIST, Tuangou.Initializer);
            }

            public List<Tuangou> getList() {
                return list;
            }
        }
    }

    public static class FendianResponse extends Response {
        // 0x02    x_map   团购分店结果
        public static final byte FIELD_LIST = 0x02;

        private FendianList list;

        public FendianResponse(XMap data) throws APIException {
            super(data);

            if (this.data.containsKey(FIELD_LIST)) {
                this.list = new FendianList(this.data.getXMap(FIELD_LIST));
            }
        }

        public FendianList getList() {
            return list;
        }

        public static class FendianList extends BaseList {

            // 0x02 x_array<x_map> 团购分店列表 
            public static final byte FIELD_LIST = 0x02;

            private List<Fendian> list;

            public FendianList(XMap data) throws APIException {
                super(data);

                this.list = getListFromData(FIELD_LIST, Fendian.Initializer);
            }

            public List<Fendian> getList() {
                return list;
            }
        }
    }
    
    public static class DianyingResponse extends DiscoverCategoreResponse {
        // 0x02    x_map   影片结果
        public static final byte FIELD_LIST = 0x02;

        private DianyingList list;

        public DianyingResponse(XMap data) throws APIException {
            super(data);

            if (this.data.containsKey(FIELD_LIST)) {
                this.list = new DianyingList(this.data.getXMap(FIELD_LIST));
            }
        }

        public DianyingList getList() {
            return list;
        }

        public static class DianyingList extends DiscoverResult {

            // 0x02 x_array<x_map> 列表
            public static final byte FIELD_LIST = 0x02;

            private List<Dianying> list;

            public DianyingList(XMap data) throws APIException {
                super(data);

                this.list = getListFromData(FIELD_LIST, Dianying.Initializer);
            }

            public List<Dianying> getList() {
                return list;
            }
        }
    }

    public static class YingxunResponse extends Response {
        // 0x02 x_map   影讯结果
        public static final byte FIELD_LIST = 0x02;

        private YingxunList list;

        public YingxunResponse(XMap data) throws APIException {
            super(data);

            if (this.data.containsKey(FIELD_LIST)) {
                this.list = new YingxunList(this.data.getXMap(FIELD_LIST));
            }
        }

        public YingxunList getList() {
            return list;
        }

        public static class YingxunList extends BaseList {

            // 0x02 x_array<x_map> 列表
            public static final byte FIELD_LIST = 0x02;

            private List<Yingxun> list;

            public YingxunList(XMap data) throws APIException {
                super(data);

                list = getListFromData(FIELD_LIST, Yingxun.Initializer);
            }

            public List<Yingxun> getList() {
                return list;
            }
        }
    }

    public static class YanchuResponse extends DiscoverCategoreResponse {
        // 0x02    x_map   演出结果
        public static final byte FIELD_LIST = 0x02;

        private YanchuList list;

        public YanchuResponse(XMap data) throws APIException {
            super(data);

            if (this.data.containsKey(FIELD_LIST)) {
                this.list = new YanchuList(this.data.getXMap(FIELD_LIST));
            }
        }

        public YanchuList getList() {
            return list;
        }

        public static class YanchuList extends DiscoverResult {

            // 0x02 x_array<x_map> 列表
            public static final byte FIELD_LIST = 0x02;

            private List<Yanchu> list;

            public YanchuList(XMap data) throws APIException {
                super(data);

                this.list = getListFromData(FIELD_LIST, Yanchu.Initializer);
            }

            public List<Yanchu> getList() {
                return list;
            }
        }
    }

    public static class ZhanlanResponse extends DiscoverCategoreResponse {
        // 0x02    x_map   展览结果
        public static final byte FIELD_LIST = 0x02;

        private ZhanlanList list;

        public ZhanlanResponse(XMap data) throws APIException {
            super(data);

            if (this.data.containsKey(FIELD_LIST)) {
                this.list = new ZhanlanList(this.data.getXMap(FIELD_LIST));
            }
        }

        public ZhanlanList getList() {
            return list;
        }

        public static class ZhanlanList extends DiscoverResult {

            // 0x02 x_array<x_map> 列表
            public static final byte FIELD_LIST = 0x02;
            
            private List<Zhanlan> list;

            public ZhanlanList(XMap data) throws APIException {
                super(data);

                this.list = getListFromData(FIELD_LIST, Zhanlan.Initializer);
            }

            public List<Zhanlan> getList() {
                return list;
            }
        }
    }
    
    public static class CommentResponse extends Response {
        // 0x02 x_map   点评结果
        public static final byte FIELD_COMMENT_LIST = 0x02;

        private CommentList list;

        public CommentResponse(XMap data) throws APIException {
            super(data);
            
            if (this.data.containsKey(FIELD_COMMENT_LIST)) {
                this.list = new CommentList(this.data.getXMap(FIELD_COMMENT_LIST));
            }
        }

        public CommentList getList() {
            return list;
        }

        public void setList(CommentList list) {
            this.list = list;
        }
        
        public XMap getData() {
            if (data == null) {
                data = super.getData();
                if (this.list != null) {
                    data.put(FIELD_COMMENT_LIST, this.list.getData());
                }
            }
            return data;
        }
        
        public static class CommentList extends BaseList {
            
            // 0x02 x_array<x_map>  poi列表 
            public static final byte FIELD_LIST = 0x02;
            
            private List<Comment> list;

            public void setTotal(int total) {
                this.total = total;
            }

            public List<Comment> getList() {
                return list;
            }

            public void setList(List<Comment> list) {
                this.list = list;
                XArray<XMap> xarray = new XArray<XMap>();
                if (this.list != null) {
                    for(Comment comment : this.list) {
                        xarray.add(comment.getData());
                    }
                }
                data.put(FIELD_LIST, xarray);
            }

            public CommentList(XMap data) throws APIException {
                super(data);
                
                list = getListFromData(FIELD_LIST, Comment.Initializer);
                this.total = 0;
            }     
            
            public XMap getData() {
                if (data == null) {
                    data = new XMap();
                    if (list != null) {
                        XArray<XMap> xarray = new XArray<XMap>();
                        for(Comment comment : list) {
                            xarray.add(comment.getData());
                        }
                        data.put(FIELD_LIST, xarray);
                    }
                    if (TextUtils.isEmpty(message)) {
                       data.put(FIELD_MESSAGE, message);
                    }
                }
                return data;
            }
        }
    }
    
    public static class CouponResponse extends Response {
        // 0x02 x_map   点评结果
        public static final byte FIELD_COUPON_LIST = 0x02;

        private CouponList list;

        public CouponResponse(XMap data) throws APIException {
            super(data);
            
            if (this.data.containsKey(FIELD_COUPON_LIST)) {
                this.list = new CouponList(this.data.getXMap(FIELD_COUPON_LIST));
            }
        }

        public CouponList getList() {
            return list;
        }

        public void setList(CouponList list) {
            this.list = list;
        }
        
        public XMap getData() {
            if (data == null) {
                data = super.getData();
                if (this.list != null) {
                    data.put(FIELD_COUPON_LIST, this.list.getData());
                }
            }
            return data;
        }
        
        public static class CouponList extends XMapData{
            
        	// 0x00 x_int 优惠券数量
        	public static final byte FIELD_TOTAL = 0x00;
        	
            // 0x01 x_array<x_map>  poi列表 
            public static final byte FIELD_LIST = 0x01;
            
            private long total;
            private List<Coupon> list;

            public long getTotal() {
                return total;
            }

            public List<Coupon> getList() {
                return list;
            }

            public CouponList(XMap data) throws APIException {
                super(data);
                
                list = getListFromData(FIELD_LIST, Coupon.Initializer);
                this.total = 0;
            }     
            
        }
    }
    
    public static class FilterConfigResponse extends Response {
        // 0x02     x_map<x_map>    配置文件的返回结果
        public static final byte FIELD_RESULT = 0x02;
        
        // 0x00    x_map   根据城市id获取的地片筛选项 
        public static final byte FIELD_AREA_FILTER = 0x0;
        
        private FilterResponse filterResponse;

        public FilterConfigResponse(XMap data) throws APIException {
            super(data);
            
            if (this.data.containsKey(FIELD_RESULT)) {
                XMap xmap = this.data.getXMap(FIELD_RESULT);
                if (xmap.containsKey(FIELD_AREA_FILTER)) {
                    XMap filterArea = xmap.getXMap(FIELD_AREA_FILTER);
                    xmap.remove(FIELD_AREA_FILTER);
                    xmap.put(FilterResponse.FIELD_FILTER_AREA, filterArea);
                    filterResponse = new FilterResponse(xmap);
                }
            }
        }

        public FilterResponse getFilterResponse() {
            return filterResponse;
        }
    }
    
    public static class ShangjiaResponse extends Response {
        // 0x02 x_map   商家结果
        public static final byte FIELD_LIST = 0x02;

        private ShangjiaList list;

        public ShangjiaResponse(XMap data) throws APIException {
            super(data);
            
            if (this.data.containsKey(FIELD_LIST)) {
                this.list = new ShangjiaList(this.data.getXMap(FIELD_LIST));
            }
        }
        
        public ShangjiaList getList() {
            return list;
        }
        
        public static class ShangjiaList extends BaseList {
            
            // 0x02 x_array<x_map>  poi列表 
            public static final byte FIELD_LIST = 0x02;
            
            private List<Shangjia> list;

            public ShangjiaList(XMap data) throws APIException {
                super(data);
                
                list = getListFromData(FIELD_LIST, Shangjia.Initializer);
                if (list != null) {
                    Shangjia.writeShangjiaList();
                }                
            }
            
            public List<Shangjia> getList() {
                return list;
            }
        }
    }
    
    public static class DiscoverResponse extends Response {
        // 0x02 x_map   首页结果
        public static final byte FIELD_LIST = 0x02;
        
        // 0x03 x_map 各城市所能支持的发现数据类型配置表?
        public static final byte FIELD_CONFIG = 0x03;
        
        private DiscoverCategoryList discoverCategoryList;
        private DiscoverConfigList configList;

        public DiscoverResponse(XMap data) throws APIException {
            super(data);

            if (this.data.containsKey(FIELD_LIST)) {
                discoverCategoryList = new DiscoverCategoryList(this.data.getXMap(FIELD_LIST));
            }

            String config = getStringFromData(FIELD_CONFIG);
            if (config != null) {
                byte[] bytes = config.getBytes();
                configList = parseDiscoverConfigList(new ByteArrayInputStream(bytes));
                if (configList != null) {
                    String path = TKConfig.getDataPath(false) + "discoverConfigList.xml";
                    Utility.writeFile(path, bytes, true);
                    Discover_Config_List = configList;
                }
            }
        }
        
        public static DiscoverConfigList parseDiscoverConfigList(InputStream inputStream) {
            SAXParserFactory factory=SAXParserFactory.newInstance();
            try {
                SAXParser parser=factory.newSAXParser();
                //获取事件源
                XMLReader xmlReader=parser.getXMLReader();
                //设置处理器
                ConfigHandler handler=new ConfigHandler();
                xmlReader.setContentHandler(handler);
                //解析xml文档
                xmlReader.parse(new InputSource(inputStream));
                return handler.discoverConfigList;
            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        
        private static class ConfigHandler extends DefaultHandler {

            DiscoverConfigList discoverConfigList = null;
            DiscoverConfig discoverConfig = null;
            boolean dp = false;
            
            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                super.endElement(uri, localName, qName);
                String tagName=localName.length()!=0?localName:qName;
                tagName=tagName.toLowerCase().trim();
                if(tagName.equals("dp")){
                    dp = false;
                }
            }

            @Override
            public void startElement(String uri, String localName, String qName,
                    Attributes attributes) throws SAXException {
                super.startElement(uri, localName, qName, attributes);
                String tagName=localName.length()!=0?localName:qName;
                tagName=tagName.toLowerCase().trim();
                if(tagName.equals("citylist")){
                    discoverConfigList = new DiscoverConfigList();
                    discoverConfigList.setVersion(attributes.getValue("version"));
                } else if(tagName.equals("city")){
                    discoverConfig = new DiscoverConfig();
                    discoverConfig.setSeqId(Long.parseLong(attributes.getValue("seq_id")));
                    discoverConfigList.getList().add(discoverConfig);
                } else if(tagName.equals("dp")){
                    dp = true;
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                if (dp) {
                    String value = new String(ch,start,length);
                    if (TextUtils.isEmpty(value) == false) {
                        discoverConfig.getList().add(Long.parseLong(value));
                    }
                }
            }

        }
        
        public DiscoverCategoryList getDiscoverCategoryList() {
            return discoverCategoryList;
        }
        
        public DiscoverConfigList getConfigList() {
            return configList;
        }
        
        public static class DiscoverCategoryList extends XMapData{
            // 0x01 x_array<x_map> x_array<单个动态poi>
            public static final byte FIELD_LIST = 0x01;
            
            // 0x02 x_string 信息提示
            public static final byte FIELD_MESSAGE = 0x02;
            
            private String message;
            private List<DiscoverCategory> list;

            public DiscoverCategoryList(XMap data) throws APIException {
                super(data);
                
                list = getListFromData(FIELD_LIST, DiscoverCategory.Initializer);
                this.message = getStringFromData(FIELD_MESSAGE);
            }
            
            public String getMessage() {
                return message;
            }

            public List<DiscoverCategory> getList() {
                return list;
            }

            public static class DiscoverCategory extends XMapData implements Parcelable {
                
                public static final String NEED_FILED = "01020304";
                    
                // 0x01 x_int 该动态poi的类型
                public static final byte FIELD_TYPE = 0x01;

                // 0x02 x_int 此类型动态poi的全市数量
                public static final byte FIELD_NUM_CITY = 0x02;

                // 0x03 x_int 此类型动态poi的附近数量
                public static final byte FIELD_NUM_NEARBY = 0x03;

                // 0x04 x_map 团购|展览|演出|电影等的首页图片
                public static final byte FIELD_DATA = 0x04;
                
                private long type;
                private long numCity;
                private long numNearby;
                private TKDrawable tkDrawable;
                private volatile int hashCode = 0;

                public DiscoverCategory(long type) {
                    this.type = type;
                }

                public DiscoverCategory(XMap data) throws APIException {
                    super(data);
                    init(data);
                }
                
                public void init(XMap data) throws APIException {
                    this.data = data;
                    if (this.data == null) {
                        throw new APIException(APIException.RESPONSE_DATA_IS_EMPTY);
                    }
                    this.type = getLongFromData(FIELD_TYPE, this.type);
                    this.numCity = getLongFromData(FIELD_NUM_CITY, 0);
                    this.numNearby = getLongFromData(FIELD_NUM_NEARBY, 0);
                    if (this.data.containsKey(FIELD_DATA)) {
                        this.tkDrawable = new TKDrawable(this.data.getXMap(FIELD_DATA));
                    } else {
                        this.tkDrawable = null;
                    }
                }

                public String getType() {
                    return String.valueOf(this.type);
                }

                public long getNumCity() {
                    return numCity;
                }

                public long getNumNearby() {
                    return numNearby;
                }

                public TKDrawable getTKDrawable() {
                    return tkDrawable;
                }

                public static final Parcelable.Creator<DiscoverCategory> CREATOR
                        = new Parcelable.Creator<DiscoverCategory>() {
                    public DiscoverCategory createFromParcel(Parcel in) {
                        return new DiscoverCategory(in);
                    }

                    public DiscoverCategory[] newArray(int size) {
                        return new DiscoverCategory[size];
                    }
                };

                
                private DiscoverCategory(Parcel in) {
                    type = in.readLong();
                    numCity = in.readLong();
                    numNearby = in.readLong();
                    tkDrawable = (TKDrawable)in.readParcelable(TKDrawable.class.getClassLoader());
                }
                
                @Override
                public int describeContents() {
                    // TODO Auto-generated method stub
                    return 0;
                }

                @Override
                public void writeToParcel(Parcel out, int flags) {
                    out.writeLong(type);
                    out.writeLong(numCity);
                    out.writeLong(numNearby);
                    
                    if (tkDrawable != null)
                        out.writeParcelable(tkDrawable, flags);
                    else
                        out.writeParcelable(null, flags);
                }
                
                @Override
                public boolean equals(Object object) {
                    if (object == null) {
                        return false;
                    }
                    if (object instanceof DiscoverCategory) {
                        DiscoverCategory other = (DiscoverCategory) object;
                        return (this.type == other.type);
                    }
                    return false;
                }
                
                public int hashCode() {
                    if(hashCode == 0) {
                        int result = 17;
                        result = 37*result + ((int)(type ^ (type >>> 32)));
                    }
                    return hashCode;
                }

                public static XMapInitializer<DiscoverCategory> Initializer = new XMapInitializer<DiscoverCategory>() {

                    @Override
                    public DiscoverCategory init(XMap data) throws APIException {
                        return new DiscoverCategory(data);
                    }
                };
            }
        }
        
        public static class DiscoverConfigList extends XMapData {
            // 0x01 x_string    城市所能支持的发现数据类型的配置表版本 
            public static final byte FIELD_VERSION = 0x01;
            
            // 0x11 x_array<x_map>  x_array<某城市所能支持的数据类型> 
            public static final byte FIELD_LIST = 0x11;
            
            public String version;
            
            private List<DiscoverConfig> list = new ArrayList<DiscoverConfig>();
            
            public DiscoverConfigList() {
            }

            public DiscoverConfigList(XMap data) throws APIException {
                super(data);

                version = getStringFromData(FIELD_VERSION);
                
                list = getListFromData(FIELD_LIST, DiscoverConfig.Initializer);
                if (list == null) {
                    list = new ArrayList<DiscoverConfig>();
                }
            }
            
            public String getVersion() {
                return version;
            }
            
            public void setVersion(String version) {
                this.version = version;
            }

            public List<DiscoverConfig> getList() {
                return list;
            }
            
            public void setList(List<DiscoverConfig> list) {
                this.list = list;
            }

            public static class DiscoverConfig extends XMapData {
                // 团购 2 x_array[0]中，bit[0]==1表示支持，==0表示不支持
                // 电影 4 x_array[0]，bit[1]==1表示支持，==0表示不支持
                // 演出 13 x_array[0]，bit[2]==1表示支持，==0表示不支持
                // 展览 14 x_array[0]，bit[3]==1表示支持，==0表示不支持
                
                // 0x01 x_int 某城市的编号 seq_id
                public static final byte FIELD_SEQ_ID = 0x01;
                
                // 0x11 x_array<x_int> x_array<发现数据类型>
                public static final byte FIELD_LIST = 0x11;
                
                public static final int SUPPORT_TUANGOU = 1;
                public static final int SUPPORT_DIANYING = 2;
                public static final int SUPPORT_YANCHU = 4;
                public static final int SUPPORT_ZHANLAN = 8;
                
                private long seqId;
                
                private List<Long> list = new ArrayList<Long>();
                
                public DiscoverConfig() {
                    
                }

                @SuppressWarnings("unchecked")
                public DiscoverConfig(XMap data) throws APIException {
                    super(data);

                    seqId = getLongFromData(FIELD_SEQ_ID);
                    
                    list = new ArrayList<Long>();
                    if (this.data.containsKey(FIELD_LIST)) {
                        XArray<XInt> xarray = (XArray<XInt>)this.data.getXArray(FIELD_LIST);
                        if (xarray != null) {
                            for(int i = 0; i < xarray.size(); i++) {
                                list.add(xarray.get(i).getValue());
                            }
                        }
                    }                
                }
                
                public long getSeqId() {
                    return seqId;
                }
                
                public void setSeqId(long seqId) {
                    this.seqId = seqId;
                }
                
                public List<Long> getList() {
                    return list;
                }

                public void setList(List<Long> list) {
                    this.list = list;
                }

                public static XMapInitializer<DiscoverConfig> Initializer = new XMapInitializer<DiscoverConfig>() {

                    @Override
                    public DiscoverConfig init(XMap data) throws APIException {
                        return new DiscoverConfig(data);
                    }
                };
                
            }
        }
    }

    public static class AlternativeResponse extends FilterResponse {
        
        // 0x02     x_map   关键字匹配得到的候选点结果
        public static final byte FIELD_LIST = 0x02;
        
        // 0x03    x_int   关键字命中的地片筛选下标 
        public static final byte FIELD_POSITION = 0x03;

        protected long position = Long.MIN_VALUE;
        
        protected AlternativeList list;

        public long getPosition() {
            return position;
        }
        
        public AlternativeList getList() {
            return list;
        }
        
        public AlternativeResponse(XMap data) throws APIException {
            super(data);

            this.list = getObjectFromData(FIELD_LIST, AlternativeList.Initializer);
            this.position = getLongFromData(FIELD_POSITION, Long.MIN_VALUE);
        }

        public static class AlternativeList extends BaseList {

            // 0x02 x_array<x_map> 列表
            public static final byte FIELD_LIST = 0x02;

            private List<Alternative> list;

            public List<Alternative> getList() {
                return list;
            }

            public void setList(List<Alternative> list) {
                this.list = list;
            }

            public AlternativeList(XMap data) throws APIException {
                super(data);

                this.list = getListFromData(FIELD_LIST, Alternative.Initializer);
            }

            static XMapInitializer<AlternativeList> Initializer = new XMapInitializer<AlternativeList>() {

                @Override
                public AlternativeList init(XMap data) throws APIException {
                    return new AlternativeList(data);
                }
            };
        }
        
        public static class Alternative extends XMapData {
            // 0x01    x_string    uuid,全局唯一
            public static final byte FIELD_UUID = 0x01;
            // 0x03    x_int   所在经度, 是普通的度数 × 10万
            public static final byte FIELD_LONGITUDE = 0x03;
            // 0x04    x_int   所在纬度, 是普通的度数 × 10万
            public static final byte FIELD_LATITUDE = 0x04;
            // 0x05    x_string    名称
            public static final byte FIELD_NAME = 0x05;
            // 0x09    x_string    地址 
            public static final byte FIELD_ADDRESS = 0x09;
            
            public static final String NEED_FIELD = "0103040509";
            
            protected String uuid;
            protected Position position;
            protected String name;
            protected String address;
            
            public String getUuid() {
                return uuid;
            }

            public Position getPosition() {
                return position;
            }

            public String getName() {
                return name;
            }

            public String getAddress() {
                return address;
            }
            
            public POI toPOI() {
                POI poi = new POI();
                poi.setUUID(uuid);
                poi.setName(name);
                poi.setAddress(address);
                poi.setPosition(position);
                return poi;
            }

            public Alternative(XMap data) throws APIException {
                super(data);

                uuid = getStringFromData(FIELD_UUID);
                position = getPositionFromData(FIELD_LONGITUDE, FIELD_LATITUDE);
                name = getStringFromData(FIELD_NAME);
                address = getStringFromData(FIELD_ADDRESS);
            }

            static XMapInitializer<Alternative> Initializer = new XMapInitializer<Alternative>() {

                @Override
                public Alternative init(XMap data) throws APIException {
                    return new Alternative(data);
                }
            };
        }
    }
    
    protected void launchTest() {
        super.launchTest();
        String dataType = this.criteria.get(SERVER_PARAMETER_DATA_TYPE);
        if (DATA_TYPE_POI.equals(dataType)) {
            String subDataType = this.criteria.get(SERVER_PARAMETER_SUB_DATA_TYPE);
            if (SUB_DATA_TYPE_POI.equals(subDataType)) {
                responseXMap = DataQueryTest.launchPOIResponse(168, "launchPOIResponse");
            } if (SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                responseXMap = DataQueryTest.launchHotelPOIResponse(168, "launchHotelPOIResponse");
            }
        } else if (DATA_TYPE_TUANGOU.equals(dataType)) {
            responseXMap = DataQueryTest.launchTuangouResponse(context, 168, "launchTuangouResponse");
        } else if (DATA_TYPE_FENDIAN.equals(dataType)) {
            responseXMap = DataQueryTest.launchFendianResponse(168, "launchFendianResponse");
        } else if (DATA_TYPE_DIANYING.equals(dataType)) {
            responseXMap = DataQueryTest.launchDianyingResponse(context, 168, "launchDianyingResponse");
        } else if (DATA_TYPE_YINGXUN.equals(dataType)) {
            responseXMap = DataQueryTest.launchYingxunResponse(168, "launchYingxunResponse");
        } else if (DATA_TYPE_YANCHU.equals(dataType)) {
            responseXMap = DataQueryTest.launchYanchuResponse(context, 168, "launchYanchuResponse");
        } else if (DATA_TYPE_ZHANLAN.equals(dataType)) {
            responseXMap = DataQueryTest.launchZhanlanResponse(context, 168, "launchZhanlanResponse");
        } else if (DATA_TYPE_SHANGJIA.equals(dataType)) {
            responseXMap = DataQueryTest.launchShangjiaResponse(context, 16);
        } else if (DATA_TYPE_DIANPING.equals(dataType)) {
            responseXMap = DataQueryTest.launchDianpingResponse(168, "launchDianpingResponse");
        } else if (DATA_TYPE_DISCOVER.equals(dataType)) {
            responseXMap = DataQueryTest.launchDiscoverResponse(context, "launchDiscoverResponse", 
                    DiscoverConfig.SUPPORT_TUANGOU+DiscoverConfig.SUPPORT_DIANYING+DiscoverConfig.SUPPORT_YANCHU+DiscoverConfig.SUPPORT_ZHANLAN);
        } else if (DATA_TYPE_PULL_MESSAGE.equals(dataType)) {
            responseXMap = DataQueryTest.launchPullMessage();
        } else if (DATA_TYPE_ALTERNATIVE.equals(dataType)) {
            responseXMap = DataQueryTest.launchAlternativeResponse(168, "launchAlternativeResponse");
        } else if (DATA_TYPE_COUPON.equals(dataType)){
        	responseXMap = DataQueryTest.launchCouponResponse(168, "launchCouponResponse");
        } else if (DATA_TYPE_FILTER.equals(dataType)){
            responseXMap = DataQueryTest.launchFilterConfigResponse();
        }
    }
}
