/*
 * @(#)NetSearcher.java  下午03:32:06 2007-11-27 2007
 *
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd.
 * All rights reserved.
 *
 */

package com.tigerknows.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.common.ActionLog;
import com.tigerknows.crypto.DataEncryptor;
import com.tigerknows.map.CityInfo;
import com.tigerknows.model.response.Appendix;
import com.tigerknows.model.response.DataPackage;
import com.tigerknows.model.response.DownloadDomainCake;
import com.tigerknows.model.response.MapVersionCake;
import com.tigerknows.model.response.PositionCake;
import com.tigerknows.model.response.ResponseCode;
import com.tigerknows.model.response.UUIDInfo;
import com.tigerknows.model.response.UpdateVersionData;
import com.tigerknows.model.response.UserActionTrackSwitch;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.Utility;
import com.tigerknows.util.HttpUtils;
import com.tigerknows.util.ParserUtil;
import com.tigerknows.util.ZLibUtils;
import com.tigerknows.util.HttpUtils.TKHttpClient.ProgressUpdate;

/**
 *本类是所有联网搜索类的公共父类.
 *
 * 使用方法：
 * 业务代码只需要给这个Query进行addParameter操作，setup后进行查询。
 * 
 * 继承自此类的查询类若需要添加该类的公共参数，只需要override掉addCommonPatameter，然后在继承的函数中
 * 调用以下父类的addCommonParameter。注意addCommonParameter的参数。
 * 
 * @author pengwenyue
 * @version
 * @see Change log:
 */
public abstract class BaseQuery {
    
    static final String TAG = "BaseQuery";
    
    // 交通查询
    public static final String API_TYPE_TRAFFIC_QUERY = "t";
    
    // 线路查询
    public static final String API_TYPE_BUSLINE_QUERY = "b";
    
    // 用户登录
    public static final String API_TYPE_BOOTSTRAP = "l";
    
    // 数据搜索
    public static final String API_TYPE_DATA_QUERY = "s";
    
    // 数据操作
    public static final String API_TYPE_DATA_OPERATION = "d";
    
    // 账号管理
    public static final String API_TYPE_ACCOUNT_MANAGE = "u";
    
    // 用户反馈
    public static final String API_TYPE_FEEDBACK_UPLOAD = "f";
    
    // 定位查询
    public static final String API_TYPE_LOCATION_QUERY = "lm";
    
    // 地图元数据下载
    public static final String API_TYPE_MAP_META_DOWNLOAD = "md";
    
    // 地图块数据下载
    public static final String API_TYPE_MAP_TILE_DOWNLOAD = "td";
    
    // 地图数据版本查询
    public static final String API_TYPE_MAP_VERSION_QUERY = "rq";
    
    // 联想词下载
    public static final String API_TYPE_SUGGEST_LEXICON_DOWNLOAD = "is";
    
    // 接口代理
    public static final String API_TYPE_PROXY = "proxy";
    
    // 酒店订单
    public static final String API_TYPE_HOTEL_ORDER = "hotelOrder";
    
    // 图片上传
    public static final String API_TYPE_FILE_UPLOAD = "fileUpload";
    
    // 文件下载
    public static final String API_TYPE_FILE_DOWNLOAD = "fileDownload";
    
    // 帮助
    public static final String API_TYPE_HELP = "help";
    
    // 消息通知
    public static final String API_TYPE_NOTICE = "notice";
    
    protected static final String VERSION = "13";
    
    // at string true api type，固定为d
    protected static final String SERVER_PARAMETER_API_TYPE = "at";

    // v string true version，固定为13
    protected static final String SERVER_PARAMETER_VERSION = "v";

    // c string true 选定城市ID
    public static final String SERVER_PARAMETER_CITY = "c";

    // dty string true 数据类型
    public static final String SERVER_PARAMETER_DATA_TYPE = "dty";

    // nf string true 所需字段, key的16进制数的拼接, 例如010a0b
    public static final String SERVER_PARAMETER_NEED_FIELD = "nf";

    // op string true operation code, 操作码
    public static final String SERVER_PARAMETER_OPERATION_CODE = "op";

    // i    int true    偏移量，第一次请求从0开始 
    public static final String SERVER_PARAMETER_INDEX = "i";
    
    // s   int true    该次请求返回结果数量
    public static final String SERVER_PARAMETER_SIZE = "s";
    
    // k   String  false   搜索关键字
    public static final String SERVER_PARAMETER_KEYWORD = "k";
    
    // clientuid   string  true    客户端uid, 每次全新安装生成一个uid 
    public static final String SERVER_PARAMETER_CLIENT_ID = "clientuid";

    // sessionid    string  false   登录用户sessionId 
    public static final String SERVER_PARAMETER_SESSION_ID = "sessionid";
    
    // x   Double  false   选定经度x
    public static final String SERVER_PARAMETER_LONGITUDE = "x";
    
    // y   Double  false   选定纬度y
    public static final String SERVER_PARAMETER_LATITUDE = "y";
    
    // lx   Double  false   定位经度x
    public static final String SERVER_PARAMETER_LOCATION_LONGITUDE = "lx";
    
    // ly   Double  false   定位纬度y
    public static final String SERVER_PARAMETER_LOCATION_LATITUDE = "ly";

    // lt  int false   local type 定位来源
    public static final String SERVER_PARAMETER_LOCATION_TYPE = "lt";
    
    // lc   int  false   定位城市Id
    public static final String SERVER_PARAMETER_LOCATION_CITY = "lc";
    
    // cx   Double  false   地图中心经度cx
    public static final String SERVER_PARAMETER_CENTER_LONGITUDE = "cx";
    
    // cy   Double  false   地图中心纬度cy
    public static final String SERVER_PARAMETER_CENTER_LATITUDE = "cy";

    // tel string true 手机号
    public static final String SERVER_PARAMETER_TELEPHONE = "tel";
    
    // pic string  true 图片们的宽高信息，格式为key0:width_height_[(0|1)+];key1:width_height_[(0|1)+];... 
    public static final String SERVER_PARAMETER_PICTURE = "pic";

    // 自定义 某个 response error code 时对应错误字符串的时候使用的前缀
	public static final String RESPONSE_CODE_ERROR_MSG_PREFIX = "resp_code_err_msg";
	
	// 定义如果response为空的时候的字符串消息
	public static final String RESPONSE_NULL_ERROR_MSG= "resp_null_err_msg";

	// dsrc	 string	 false	 data request source，该请求的来源（指客户端的不同“频道”
    public static final String SERVER_PARAMETER_REQUSET_SOURCE_TYPE = "dsrc";

    // dsrc=dpmsg，表示雷达频道，通过解析推送消息获得动态poi的uid之后，根据uid取完整的动态poi 
    public static final String REQUSET_SOURCE_TYPE_PULLED_DYNAMIC_POI = "dpmsg";

    // dsrc=weixin，表示从附件栏启动客户端，仅在引导服务(at=l, Bootstrap)时使用
    public static final String REQUSET_SOURCE_TYPE_WEIXIN = "weixin";

    // dsrc=cinemadetail，表示点击POI中的电影院列表进入电影详情页
    public static final String REQUSET_SOURCE_TYPE_POI_TO_CINEMA = "cinemadetail";

    // cstatus  string  false   上传客户端状态信息,字段为cstatus,打开时为0(可以省略),关闭时为1
    public static final String SERVER_PARAMETER_CLIENT_STATUS = "cstatus";
    
    public static final String CLIENT_STATUS_START = "0";
    
    public static final String CLIENT_STATUS_STOP = "1";
    
    public static String sClentStatus = CLIENT_STATUS_START;
    
    // subty   int     false   子数据类型，当dty=1时，可有取值，取值范围{0,1} 
    public static final String SERVER_PARAMETER_SUB_DATA_TYPE = "subty";

    // checkin     String  true    入住酒店时间，格式"yyyy-MM-dd"
    public static final String SERVER_PARAMETER_CHECKIN = "checkin";
    
    // checkout    String  true    离开酒店时间，格式"yyyy-MM-dd" 
    public static final String SERVER_PARAMETER_CHECKOUT = "checkout";
    
    // hs  string  false   表示支持的酒店第三方商家，取值范围{elong, all}，默认elong，all表示支持所有的
    public static final String SERVER_PARAMETER_HOTEL_SOURCE = "hs";
    
    // uuid     string  true    用于唯一标识联网请求的uuid或uuid_offset 
    public static final String SERVER_PARAMETER_UUID = "uuid";
    
    // refdty   true    相关数据的类型，现在支持1（poi）和24（菜品）
    public static final String SERVER_PARAMETER_REF_DATA_TYPE = "refdty";
    
    // refid    false   数据的id
    public static final String SERVER_PARAMETER_REF_ID = "refid";
    
    /**
     * 检查是否为推送动态POI的查询
     * @return
     */
    public boolean isPulledDynamicPOIRequest() {
        boolean result = false;
        if (requestParameters.containsKey(SERVER_PARAMETER_REQUSET_SOURCE_TYPE)) {
            String sourceType = requestParameters.getValue(SERVER_PARAMETER_REQUSET_SOURCE_TYPE);
            if (REQUSET_SOURCE_TYPE_PULLED_DYNAMIC_POI.equals(sourceType)) {
                result = true;
            }
        }
        return result;
    }
    
    // 数据类型:
    // POI 1
    public static final String DATA_TYPE_POI = "1";

    // 团购 2
    public static final String DATA_TYPE_TUANGOU = "2";

    // 影讯 3
    public static final String DATA_TYPE_YINGXUN = "3";

    // 电影? 4
    public static final String DATA_TYPE_DIANYING = "4";

    // 备选 5
    public static final String DATA_TYPE_ALTERNATIVE = "5";

    // 反向定位poi
    public static final String DATA_TYPE_GEOCODER = "6";

    // 优惠 7
    public static final String DATA_TYPE_YOUHUI = "7";

    // 订单 8
    public static final String DATA_TYPE_DINGDAN = "8";

    // 团购券 9
    public static final String DATA_TYPE_TUANGOUJUAN = "9";

    // 团购商家 10
    public static final String DATA_TYPE_TUANGOUSHANGJIA = "10";

    // 点评 11
    public static final String DATA_TYPE_DIANPING = "11";

    // 用户 12
    public static final String DATA_TYPE_USER = "12";

    // 演出 13
    public static final String DATA_TYPE_YANCHU = "13";

    // 展览  14
    public static final String DATA_TYPE_ZHANLAN = "14";

    // 团购商 15
    public static final String DATA_TYPE_SHANGJIA = "15";

    // 团购分店 16
    public static final String DATA_TYPE_FENDIAN = "16";
    
    // 消息推送 18
    public static final String DATA_TYPE_PULL_MESSAGE = "18";
    
    // 用户调研 21
    public static final String DATA_TYPE_DIAOYAN = "21";
    
    // 优惠券 22
    public static final String DATA_TYPE_COUPON = "22";
    
    // 默认筛选项等配置文件 23
    public static final String DATA_TYPE_FILTER = "23";
    
    // 菜品 24
    public static final String DATA_TYPE_DISH = "24";
    
    // 图片 25
    public static final String DATA_TYPE_PICTURE = "25";
    
    // 酒店商家 26
    public static final String DATA_TYPE_HOTELVENDOR = "26";

    // 发现首页动态数据统计 100
    public static final String DATA_TYPE_DISCOVER = "100";

    // 数据子类型:
    // POI 0
    public static final String SUB_DATA_TYPE_POI = "0";
    
    // 酒店 1
    public static final String SUB_DATA_TYPE_HOTEL = "1";
    
    public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    public static final int STATUS_CODE_MISSING_REQUEST_PARAMETER = -1;

    public static final int STATUS_CODE_NONE = 0;

    public static final int STATUS_CODE_NETWORK_OK = 200;

    public static final int STATUS_CODE_DATA_EMPTY = 1000;

    public static final int STATUS_CODE_CANCEL = 1001;

    public static final int STATUS_CODE_DECODE_ERROR = 1401;
    
    public static final int STATUS_CODE_BYTE_TO_XMAP_ERROR= 1402;
    
    public static final int STATUS_CODE_TRANSLATE_ERROR = 1404;

    public static final int STATUS_CODE_RESPONSE_EMPTY = 2000;

    private static final int RECONNECTION_WAIT_TIMES = 15 * 1000;

    public static final int KEEP_ALIVE_TIME = 30 * 1000;
    
    private static IRequestParameters sCommonParameters;
    
    /**
     * 设置网络请求中的公共参数
     */
    public static void initCommonParameters() {
        
        sCommonParameters = new MapRequestParameters();
        
        // 下列参数都是固定值，兼容旧版本服务的公共请求参数约定
        sCommonParameters.add("dv", "1");
        sCommonParameters.add("sc", "13800100500");
        sCommonParameters.add("sg", "25");
        sCommonParameters.add("si", "5$5$5$5$5");
        sCommonParameters.add("sv", "1");
        sCommonParameters.add("vd", "1.0.20100619");
        
        sCommonParameters.add("m", TKConfig.getSpreader());
        sCommonParameters.add("ec", TKConfig.getEncoding());
        sCommonParameters.add("pk", TKConfig.getPhoneKey());
        sCommonParameters.add("vs", TKConfig.getClientSoftVersion());
        sCommonParameters.add("vp", TKConfig.getVersionOfPlatform());
    }
    
    protected void addCommonParameters() {
        addCommonParameters(this.cityId);
    }
    
    /**
     * 添加Query的公共参数，继承自此类的Query可以override这个函数
     */
    protected void addCommonParameters(int cityId) {
        CityInfo cityInfo = Globals.getCurrentCityInfo(context);
        if (cityId == CityInfo.CITY_ID_INVALID) {
            cityId = cityInfo.getId();
        }
        requestParameters.add(sCommonParameters);

        requestParameters.add("c", String.valueOf(cityId));
        requestParameters.add("e", TKConfig.getIMEI());
        requestParameters.add("d", TKConfig.getIMSI());
        TKCellLocation tkCellLocation = TKConfig.getCellLocation();
        int mcc = TKConfig.getMCC();
        int mnc = TKConfig.getMNC();
        int lac = tkCellLocation.lac;
        int cid = tkCellLocation.cid;

        requestParameters.add("mcc", String.valueOf(mcc));
        requestParameters.add("mnc", String.valueOf(mnc));
        requestParameters.add("lac", String.valueOf(lac));
        requestParameters.add("ci", String.valueOf(cid));
        requestParameters.add("ss", String.valueOf(TKConfig.getSignalStrength()));
        
        //服务器的约定，这些类别的请求不能含有at
        if (API_TYPE_PROXY.equals(apiType) == false 
                && API_TYPE_HOTEL_ORDER.equals(apiType) == false 
                && API_TYPE_FILE_UPLOAD.equals(apiType) == false 
                && API_TYPE_NOTICE.equals(apiType) == false
                && API_TYPE_HELP.equals(apiType) == false) {
            requestParameters.add(SERVER_PARAMETER_API_TYPE, apiType);
            requestParameters.add(SERVER_PARAMETER_VERSION, version);
        }

        addUUIDParameter();
        requestParameters.add(SERVER_PARAMETER_CLIENT_STATUS, sClentStatus);
        
        addMyLocationParameters();
    }
    
    /**
     * 引导服务进行网络连接的最大重试次数
     */
    static final int BOOTSTRAP_RETRY_TIME_MAX = 3;
    
    /**
     * 引导服务进行网络连接的重试次数
     */
    protected int bootstrapRetryTime = 0;

    protected Context context;
    
    protected String apiType;
    
    protected String version;
    
    protected String actionTag;
    
    protected int cityId = CityInfo.CITY_ID_INVALID;

    protected int sourceViewId = -1;
    
    protected int targetViewId = -1;
    
    private IRequestParameters checkParameter = null;
    
    protected String tipText = null;
    
    protected IRequestParameters requestParameters = new MapRequestParameters();
    
    //用于该Query的本地控制参数
    private IRequestParameters localParameters = new MapRequestParameters();
    
    protected HttpUtils.TKHttpClient httpClient;

    protected boolean isTranslatePart = false;

    protected boolean needReconntection = false;

    protected boolean isStop = false;

    protected int statusCode = STATUS_CODE_NONE;
    
    protected List<Appendix> appendice = new ArrayList<Appendix>();

    protected ProgressUpdate progressUpdate;

    protected XMap responseXMap;
    
    protected Response response;
    
    public BaseQuery(Context context, String apiType) {
        this(context, apiType, VERSION);
    }
    
    public BaseQuery(Context context, String apiType, String version) {
        this.context = context;
        this.apiType = apiType;
        this.version = version;
    }
    
    public BaseQuery(BaseQuery query) {
        this.context = query.context;
        this.apiType = query.apiType;
        this.version = query.version;
        this.targetViewId = query.targetViewId;
        this.sourceViewId = query.sourceViewId;
        this.tipText = query.tipText;
        this.needReconntection = query.needReconntection;
        this.requestParameters = query.requestParameters.clone();
        this.cityId = query.cityId;
    }
    
    public int getTargetViewId() {
        return targetViewId;
    }
    
    public int getSourceViewId() {
        return sourceViewId;
    }
    
    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public String getAPIType() {
        return this.apiType;
    }
    
    public void setTipText(String tipText) {
		this.tipText = tipText;
	}

    public String getTipText() {
        return tipText;
    }

    public boolean isStop() {
        return isStop;
    }
    
    public void setResponse(Response response) {
        this.response = response;
    }
    
    public Response getResponse() {
        return response;
    }
    
    public XMap getResponseXMap() {
        return responseXMap;
    }

    public void stop() {
        isStop = true;
        if (httpClient != null) {
            httpClient.stop();
        }
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setProgressUpdate(ProgressUpdate progressUpdate) {
        this.progressUpdate = progressUpdate;
    }
    
    public interface ReportState {
        public void onReportStateCode(int stateCode);
    }
    
    public void setup(int sourceViewId, int targetViewId) {
        setup(sourceViewId, targetViewId, null);
    }
    
    public void setup(int sourceViewId, int targetViewId, String tipText) {
        setup(sourceViewId, targetViewId, tipText, false);
    }
    
    public void setup(int sourceViewId, int targetViewId, String tipText, boolean needReconntection) {
        this.sourceViewId = sourceViewId;
        this.targetViewId = targetViewId;
        this.tipText = tipText;
        this.needReconntection = needReconntection; 
    }

    public void query() {
        statusCode = STATUS_CODE_NONE;
        appendice.clear();
        
        addCommonParameters();
        
        if (TKConfig.CheckParameters) {
            try {
                checkRequestParameters();
            } catch (APIException e) {
                e.printStackTrace();
                statusCode = STATUS_CODE_MISSING_REQUEST_PARAMETER;
                return;
            }
        }
        
        createHttpClient();
        execute();
    }
    
    /**
     * 这个是添加位置参数的函数，但是有些特殊情况，比如说翻页所需要提交的位置
     * 和当前定位并不一样，那些查询可以Override掉这个函数。
     */
    protected void addMyLocationParameters() {
        final CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        if (myLocationCityInfo != null) {
            final Position myLocationPosition = myLocationCityInfo.getPosition();
            if (myLocationPosition != null) {
                requestParameters.add(SERVER_PARAMETER_LOCATION_CITY, String.valueOf(myLocationCityInfo.getId()));
                requestParameters.add(SERVER_PARAMETER_LOCATION_LONGITUDE, String.valueOf(myLocationPosition.getLon()));
                requestParameters.add(SERVER_PARAMETER_LOCATION_LATITUDE, String.valueOf(myLocationPosition.getLat()));
                requestParameters.add(SERVER_PARAMETER_LOCATION_TYPE, String.valueOf(myLocationPosition.getType()));
            }
        }
    }
    
    protected void addUUIDParameter() {
        String uuid = UUID.randomUUID().toString();
        requestParameters.add(SERVER_PARAMETER_UUID, uuid);
    }
    
    String[] CommonEssentialKeys = new String[]{"c", "e", "d", "m", "vs", "pk", "clientuid", "uuid"};
    /**
     * 放在OptionalKeys里面的key也可以在某些query中放在EssentialKey中作为必要的key来检查，不影响可选key
     * 这样原来的addSession(false)可以不用再理会，而addSession(true)在自己的必选key中加入即可。
     */
    String[] CommonOptionalKeys = new String[]{"lc", "lx", "ly", "lt", "mcc", "mnc", "lac", 
            "ci", "ss", "at", "v", "info", "dsrc", "ddst", "dv", "sc", "sg", "si", "sv", "vd", 
            "ec", "vp", SERVER_PARAMETER_CLIENT_STATUS, SERVER_PARAMETER_SESSION_ID,
            SERVER_PARAMETER_CENTER_LONGITUDE, SERVER_PARAMETER_CENTER_LATITUDE};
     
    /**
     * 这个函数用来检测参数,顺便可以兼容以前添加公共参数的行为.
     * @throws APIException
     */
    abstract protected void checkRequestParameters() throws APIException;
    
    protected void createHttpClient() {
        createHttpClient(true);
    }
    
    protected void createHttpClient(boolean needEncrypt) {
        httpClient = new HttpUtils.TKHttpClient();
        httpClient.setIsEncrypt(needEncrypt);
        httpClient.setParameters(requestParameters);
    }
    
    protected void launchTest() {
        responseXMap = BaseQueryTest.launchResponse();
    }
    
    private void execute() {
        boolean isFirstConnection = true;

        while ((isFirstConnection || needReconntection) && !isStop) {
            try {
                httpClient.setApiType(getActionTag(), (isFirstConnection == false));
                if (TKConfig.LaunchTest == 1) {
                    if (apiType.equals(API_TYPE_DATA_QUERY)
                            || apiType.equals(API_TYPE_DATA_OPERATION)
                            || apiType.equals(API_TYPE_ACCOUNT_MANAGE)
                            || apiType.equals(API_TYPE_PROXY)
                            || apiType.equals(API_TYPE_HOTEL_ORDER)
                            || apiType.equals(API_TYPE_NOTICE)
                            || apiType.equals(API_TYPE_FEEDBACK_UPLOAD)
                            || apiType.equals(API_TYPE_FILE_UPLOAD)
                            || apiType.equals(API_TYPE_FILE_DOWNLOAD)
                            || apiType.equals(API_TYPE_BOOTSTRAP)) {
                        launchTest();
                        if (responseXMap != null) {
                            byte[] data = ByteUtil.xobjectToByte(responseXMap);
                            data = ZLibUtils.compress(data);
                            DataEncryptor.getInstance().encrypt(data);
                            httpClient.launchTest(data, STATUS_CODE_NETWORK_OK);
                        }
                    } 
                } else if (TKConfig.LaunchTest == 2) {
                    File file = new File(TKConfig.getTestDataPath()+getActionTag());
                    if (file.exists()) {
                        FileInputStream fis = null;
                        try {
                            fis = new FileInputStream(file);
                            byte[] data = Utility.readFileToByte(fis);
                            httpClient.launchTest(data, STATUS_CODE_NETWORK_OK);
                        } catch (Exception e) {
                            file.delete();
                            e.printStackTrace();
                        } finally {
                            if (null != fis) {
                                try {
                                    fis.close();
                                } catch (IOException e) {
                                    // Ignore
                                    LogWrapper.e(TAG, "excute() IOException caught while closing stream");
                                }
                            }
                        }
                    }
                } else {
                    httpClient.execute(context);
                }
                statusCode = httpClient.getStatusCode();
                if (statusCode == STATUS_CODE_NETWORK_OK) {
                    byte[] data = httpClient.getData();
                    if (TKConfig.SaveResponseData) {
                        Utility.writeFile(TKConfig.getTestDataPath() + getActionTag(), data, true);
                    }
                    if (data != null) {
                        LogWrapper.i(TAG, "execute():at="+apiType+", responseLength="+data.length);
                    } else {
                        LogWrapper.i(TAG, "execute():at="+apiType+", responseLength="+data+", stop="+isStop);
                    }
                    translate(data);
                    break;
                }
            } catch (IOException e) {
                // 当前出现异常，如果当前连接的是服务器推送的动态负载均衡Host，则将此动态负载均衡Host置空，这样就会使用预置的Host
                if (API_TYPE_MAP_META_DOWNLOAD.equals(apiType) ||
                        API_TYPE_MAP_TILE_DOWNLOAD.equals(apiType) ||
                        API_TYPE_MAP_VERSION_QUERY.equals(apiType) ||
                        API_TYPE_SUGGEST_LEXICON_DOWNLOAD.equals(apiType)) {
                    if (TKConfig.getDynamicDownloadHost() != null) {
                        TKConfig.setDynamicDownloadHost(null);
                        createHttpClient();
                        // 下面此代码需要注释，因为地图下载服务在下次httpClient.execute(context)时没有重新生成最新的请求参数，
                        // 会造成将下载的地图数据写入到地图文件中的错误位置，从而引起地图引擎崩溃
//                        continue;
                    }
                } else if (API_TYPE_BUSLINE_QUERY.equals(apiType) ||
                        API_TYPE_TRAFFIC_QUERY.equals(apiType) ||
                        API_TYPE_DATA_QUERY.equals(apiType) ||
                        API_TYPE_DATA_OPERATION.equals(apiType) ||
                        API_TYPE_PROXY.equals(apiType) ||
                        API_TYPE_HOTEL_ORDER.equals(apiType)) {
                    if (TKConfig.getDynamicQueryHost() != null) {
                        TKConfig.setDynamicQueryHost(null);
                        createHttpClient();
                        continue;
                    }
                } else if (API_TYPE_LOCATION_QUERY.equals(apiType)) {
                    if (TKConfig.getDynamicLocationHost() != null) {
                        TKConfig.setDynamicLocationHost(null);
                        createHttpClient();
                        continue;
                    }
                } else if (API_TYPE_ACCOUNT_MANAGE.equals(apiType)) {
                    if (TKConfig.getDynamicAccountManageHost() != null) {
                        TKConfig.setDynamicAccountManageHost(null);
                        createHttpClient();
                        continue;
                    }
                } else if (API_TYPE_BOOTSTRAP.equals(apiType)) {
                    bootstrapRetryTime++;
                    try {
                        Thread.sleep(6*1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    if (bootstrapRetryTime < BOOTSTRAP_RETRY_TIME_MAX) {
                        createHttpClient();
                        continue;
                    }
                }
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (needReconntection && !isStop) {
                try {
                    Thread.sleep(RECONNECTION_WAIT_TIMES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            
            isFirstConnection = false;
        }

        LogWrapper.i(TAG, "execute():at="+apiType+", statusCode="+statusCode+", isStop="+isStop);
    }

    protected void translate(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        if ((isStop == false
                || isTranslatePart)) {
            try {
                if (API_TYPE_MAP_META_DOWNLOAD.equals(apiType)
                        || API_TYPE_MAP_TILE_DOWNLOAD.equals(apiType)
                        || API_TYPE_MAP_VERSION_QUERY.equals(apiType)
                        || API_TYPE_SUGGEST_LEXICON_DOWNLOAD.equals(apiType)
                        || API_TYPE_LOCATION_QUERY.equals(apiType))  {
                    ParserUtil util = new ParserUtil(data, TKConfig.getEncoding());
                    translateResponseV12(util);
                } else {
                    translateResponse(data);
                    if (response != null) {
                        ActionLog.getInstance(context).addAction(ActionLog.Response, apiType, response.getResponseCode(), response.getDescription());
                    }
                    LogWrapper.d(TAG, "translate():at="+apiType+", response="+response);
                }
            } catch (APIException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void translateResponseV12(ParserUtil util) throws IOException {
        int itemsNum = util.readIntFromTwoBytes();

        for (int i = 0; i < itemsNum; i++) {
            int type = util.readIntFromTwoBytes();
            int length = 0;
            // 当是原始数据包，在收数据时有可能被停止，
            // 那么后续的数据长度就不一定为此处解释出的长度，
            // 实际应该要比解释出的长度要小
            if (type >= Appendix.TYPE_DATA_PACKAGE) {
                length = util.readIntFromFourBytes();
                if (length > util.availableDataleng()) {
                    length = util.availableDataleng();
                }
            } else {
                length = util.readIntFromTwoBytes();
            }
            int index = util.getStart();

            Appendix appendix = null;
            switch (type) {
                case Appendix.TYPE_UUID:
                    appendix = new UUIDInfo();
                    appendix.parse(util);
                    break;
                case Appendix.TYPE_UPDATE_VERSION_DATA:
                    appendix = new UpdateVersionData();
                    appendix.parse(util);
                    appendix.type = Appendix.TYPE_UPDATE_VERSION_DATA;
                    break;
                case Appendix.TYPE_RESPONSE_CODE:
                    appendix = new ResponseCode();
                    appendix.parse(util);
                    appendix.type = Appendix.TYPE_RESPONSE_CODE;
                    break;
                case Appendix.TYPE_DATA_PACKAGE:
                    appendix = new DataPackage();
                    appendix.parse(util);
                    appendix.type = Appendix.TYPE_DATA_PACKAGE;
                    break;
                case Appendix.TYPE_DOWNLOAD_DOMAIN:
                    appendix = new DownloadDomainCake();
                    appendix.parse(util);
                    appendix.type = Appendix.TYPE_DOWNLOAD_DOMAIN;
                    break;
                case Appendix.TYPE_POSITION:
                    appendix = new PositionCake();
                    appendix.parse(util);
                    appendix.type = Appendix.TYPE_POSITION;
                    break;
                case Appendix.TYPE_MAP_VERSION:
                    appendix = new MapVersionCake(length);
                    appendix.parse(util);
                    appendix.type = Appendix.TYPE_MAP_VERSION;
                    break;
                case Appendix.TYPE_USER_ACTION_TRACK_SWITCH:
                    appendix = new UserActionTrackSwitch();
                    appendix.parse(util);
                    appendix.type = Appendix.TYPE_USER_ACTION_TRACK_SWITCH;
                    break;
                default:
                    break;
            }
            
            if (appendix != null) {
                appendice.add(appendix);
            }

            if (type < Appendix.TYPE_DATA_PACKAGE) {
                int remainBytes = length - (util.getStart() - index);
                util.advance(remainBytes);
            }
        }
    }
    
    protected void translateResponse(byte[] data) throws APIException {
        try {
            // 解密数据
            DataEncryptor.getInstance().decrypt(data);
            // 解压数据
            try {
                data = ZLibUtils.decompress(data);
            } catch (Exception cause) {
                throw new APIException("decode data error");
            }
            try {
                responseXMap = (XMap) ByteUtil.byteToXObject(data);

                modifyResponseData();
            } catch (Exception cause) {
                throw new APIException("byte to xmap error");
            }
            LogWrapper.d(TAG, "translateResponse():at="+apiType+", responseXMap="+responseXMap);
        } catch (APIException cause) {
            throw cause;
        } catch (Exception cause) {
            throw new APIException(cause);
        }
    }
    
    protected void modifyResponseData() {
        final Activity activity = BaseQueryTest.getActivity();
        if (TKConfig.ModifyResponseData && activity != null && responseXMap != null) {
            activity.runOnUiThread(new Runnable() {
                
                @Override
                public void run() {
                    ViewGroup viewGroup = BaseQueryTest.getViewByXObject(activity, (byte)0, responseXMap, 0);
                    
                    ScrollView scrollView = new ScrollView(activity);
                    scrollView.addView(viewGroup);
                    Dialog dialog = Utility.showNormalDialog(activity, scrollView);
                    dialog.setOnDismissListener(new OnDismissListener() {
                        
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            responseXMap.put((byte)250, 250);
                        }
                    });
                }
            });
            while (true) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (responseXMap.containsKey((byte)250)) {
                    break;
                }
            }
        }
    }
    
    public void copyLocationParameter(BaseQuery query) {
        if (query == null) {
            return;
        }
        if (query.hasParameter(DataQuery.SERVER_PARAMETER_LOCATION_CITY)
                && query.hasParameter(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE)
                && query.hasParameter(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE)
                && query.hasParameter(DataQuery.SERVER_PARAMETER_LOCATION_TYPE)) {
            addParameter(DataQuery.SERVER_PARAMETER_LOCATION_CITY, query.getParameter(SERVER_PARAMETER_LOCATION_CITY));
            addParameter(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE, query.getParameter(SERVER_PARAMETER_LOCATION_LONGITUDE));
            addParameter(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE, query.getParameter(SERVER_PARAMETER_LOCATION_LATITUDE));
            addParameter(DataQuery.SERVER_PARAMETER_LOCATION_TYPE, query.getParameter(SERVER_PARAMETER_LOCATION_TYPE));
        }
        if (query.hasParameter(DataQuery.SERVER_PARAMETER_CITY)) {
            addParameter(DataQuery.SERVER_PARAMETER_CITY, query.getParameter(SERVER_PARAMETER_CITY));
        }
        if (query.hasParameter(DataQuery.SERVER_PARAMETER_LONGITUDE)
                && query.hasParameter(DataQuery.SERVER_PARAMETER_LATITUDE)) {
            addParameter(DataQuery.SERVER_PARAMETER_LONGITUDE, query.getParameter(DataQuery.SERVER_PARAMETER_LONGITUDE));
            addParameter(DataQuery.SERVER_PARAMETER_LATITUDE, query.getParameter(DataQuery.SERVER_PARAMETER_LATITUDE));
        }
        if (query.hasParameter(DataQuery.SERVER_PARAMETER_FILTER)) {
            addParameter(DataQuery.SERVER_PARAMETER_FILTER, query.getParameter(DataQuery.SERVER_PARAMETER_FILTER));
        }
    }
    
    public final void addLocalParameter(String key, String value) {
        if (key != null && !key.equals("")) {
            localParameters.add(key, value);
        }
    }
    
    public final String getLocalParameter(String key) {
        if (key != null && !key.equals("")) {
            return localParameters.getValue(key);
        }
        return null;
    }
    
    public final boolean hasLocalParameter(String key) {
        if (key != null && !key.equals("")) {
            return localParameters.containsKey(key);
        }
        return false;
    }
    /**
     * 以下是此次查询的相关参数操作接口，使用这些函数来添加参数，不与实际参数类产生交集。
     * @param key
     * @param value
     */
    public final void addParameter(String key, String value) {
        if (key != null && !key.equals("")) {
            requestParameters.add(key, value);
        }
    }
    
    public final void removeParameter(String key) {
        if (key != null && !key.equals("")) {
            requestParameters.remove(key);
        }
    }
    
    public final void setParameter(String key, String value) {
        if (key != null && !key.equals("")) {
            requestParameters.add(key, value);
        }
    }
    
    public final String getParameter(String key) {
        if (key != null) {
            return requestParameters.getValue(key);
        }
        return null;
    }
    
    public final void delParameter(String key) {
        if (key != null) {
            requestParameters.remove(key);
        }
    }
    
    public final boolean hasParameter(String key) {
        if (key != null) {
            return requestParameters.containsKey(key);
        }
        return false;
    }
    
    /**
     * 返回该query的参数对象
     * @return
     */
    public final IRequestParameters getParameters() {
        return requestParameters;
    }
    
    /**
     * 添加SessionId和ClientId
     * @param need
     * @throws APIException
     */
    //FIXME:去掉这个参数，在别的地方添加上检测的key. 注：还没写关于clientid的检查
    void addSessionId() {
        String sessionId = Globals.g_Session_Id;
        if (!TextUtils.isEmpty(sessionId)) {
            requestParameters.add(SERVER_PARAMETER_SESSION_ID, sessionId);
        } else {
            requestParameters.remove(SERVER_PARAMETER_SESSION_ID);
        }
        
        String clientId = Globals.g_ClientUID;
        if (!TextUtils.isEmpty(clientId)) {
            requestParameters.add(SERVER_PARAMETER_CLIENT_ID, clientId);
        } else {
            requestParameters.remove(SERVER_PARAMETER_CLIENT_ID);
        }
    }
        
    /**
     * 检查一个参数是否存在，不存在则抛出异常
     * @param key
     * @throws APIException
     */
    private final void debugCheckParameter(String key) throws APIException {
        if (checkParameter != null && checkParameter.containsKey(key)) {
            String result = checkParameter.getValue(key);
            if (result == null) {
                throw APIException.wrapToMissingRequestParameterException(key);
            }
        } else {
            throw APIException.wrapToMissingRequestParameterException(key);
        }
    }
    
    /**
     * 检查一个请求的参数，不许比必选参数少，不许比必选+可选参数多
     * 调用方法：
     * checkParameters(new String[]{"key1", "key2"}, new String[]{"key5", "key6"})
     * @param essentialKeys 必选参数列表
     * @param optionalKeys 可选参数列表
     * @throws APIException
     */
    protected void debugCheckParameters(String[] essentialKeys, String[] optionalKeys, boolean checkCommonParameters) throws APIException{
        checkParameter = requestParameters.clone();
        for (int i = 0; essentialKeys != null && i < essentialKeys.length; i++) {
            debugCheckParameter(essentialKeys[i]);
            checkParameter.remove(essentialKeys[i]);
        }
        if (checkCommonParameters) {
            for (int i = 0; i < CommonEssentialKeys.length; i++) {
                debugCheckParameter(CommonEssentialKeys[i]);
                checkParameter.remove(CommonEssentialKeys[i]);
            }
        }
        
        for (int i = 0; optionalKeys != null && i < optionalKeys.length; i++) {
            checkParameter.remove(optionalKeys[i]);
        }
        if (checkCommonParameters) {
            for (int i = 0; i < CommonOptionalKeys.length; i++) {
                checkParameter.remove(CommonOptionalKeys[i]);
            }
        }
        
        if (!checkParameter.isEmpty()) {
            String unwantedParameters = checkParameter.getPostParam();
            throw APIException.wrapToUnwantedRequestParameterException(unwantedParameters);
        }
    }
    
    /**
     * 检查一个请求的参数，只检查必选参数，无可选参数。
     * @param essentialKeys
     * @throws APIException
     */
    protected void debugCheckParameters(String[] essentialKeys) throws APIException {
        debugCheckParameters(essentialKeys, null);
    }
    
    protected void debugCheckParameters(String[] essentialKeys, String[] optionalKeys) throws APIException {
        debugCheckParameters(essentialKeys, optionalKeys, true);
    }
    
    public interface IRequestParameters {
        
        public void add(String key, String value);
        
        public void add(String key, int value);
        
        public void add(String key, long value);
        
        public void remove(String key);
        
        public void remove(int location);
        
        public String getKey(int location);
        
        public String getValue(String key);
        
        public String getValue(int location);
        
        public int size();
        
        public void add(IRequestParameters parameters);
        
        public void clear();
        
        public boolean isEmpty();
        
        public boolean containsKey(String key);
        
        public String getPostParam();
        
        public String getEncodedPostParam(String enc);
        
        public IRequestParameters clone();
    }
    
    public static class MapRequestParameters implements IRequestParameters, Cloneable{
        LinkedHashMap<String, String> parameters;
        public static final String PARAMETER_SEPARATOR = "&";
        public static final String NAME_VALUE_SEPARATOR = "=";
        
        public MapRequestParameters() {
            parameters = new LinkedHashMap<String, String>();
        }
        
        public MapRequestParameters(LinkedHashMap<String, String> parameters) {
            this.parameters = parameters; 
        }
        
        public String getValue(String key) {
            return parameters.get(key);
        }
        
        public void add(String key, String value) {
            parameters.put(key, value);
        }
        
        public boolean containsKey(String key) {
            return parameters.containsKey(key);
        }
        
        public void remove(String key) {
            parameters.remove(key);
        }
        
        public boolean isEmpty() {
            return parameters.isEmpty();
        }
        
        @SuppressWarnings("unchecked")
        public IRequestParameters clone() {
            return new MapRequestParameters((LinkedHashMap<String, String>) parameters.clone());
        }
        
        public String getPostParam() {
            Set<String> keys;
            StringBuilder buf = new StringBuilder();
            keys = parameters.keySet();
            for (String key : keys) {
                buf.append(key).append(NAME_VALUE_SEPARATOR).append(parameters.get(key));
                buf.append(PARAMETER_SEPARATOR);
            }
            
            return buf.substring(0, buf.length() - 1);
        }
        
        public String getEncodedPostParam(String enc) {
            Set<String> keys;
            StringBuilder buf = new StringBuilder();
            keys = parameters.keySet();
            for (String key : keys) {
                try {
                    buf.append(URLEncoder.encode(key, enc)).append(NAME_VALUE_SEPARATOR).append(URLEncoder.encode(parameters.get(key)));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                buf.append(PARAMETER_SEPARATOR);
            }
            
            return buf.substring(0, buf.length() - 1);
        }

        @Override
        public void add(String key, int value) {
            parameters.put(key, String.valueOf(value));
        }

        @Override
        public void add(String key, long value) {
            parameters.put(key, String.valueOf(value));
        }

        @Override
        public int size() {
            return parameters.size();
        }

        @Override
        public void add(IRequestParameters parameters) {
            for(int i = 0, size = parameters.size(); i < size; i++){
                this.add(parameters.getKey(i), parameters.getValue(i));
            }
        }

        @Override
        public void clear() {
            parameters.clear();
        }

        @Override
        public void remove(int location) {
            int index = 0;
            Iterator<Entry<String, String>> k = parameters.entrySet().iterator();
            for (; k.hasNext(); ) {
                Entry<String, String> e = k.next();
                if (index == location) {
                    parameters.remove(e.getKey());
                    return;
                }
                index++;
            }
        }

        @Override
        public String getKey(int location) {
            int index = 0;
            Iterator<Entry<String, String>> k = parameters.entrySet().iterator();
            for (; k.hasNext(); ) {
                Entry<String, String> e = k.next();
                if (index == location) {
                    return e.getKey();
                }
                index++;
            }
            return null;
        }

        @Override
        public String getValue(int location) {
            int index = 0;
            Iterator<Entry<String, String>> k = parameters.entrySet().iterator();
            for (; k.hasNext(); ) {
                Entry<String, String> e = k.next();
                if (index == location) {
                    return e.getValue();
                }
                index++;
            }
            return null;
        }
    }
    
    public static class ListRequestParameters implements IRequestParameters, Cloneable{
        public static final String PARAMETER_SEPARATOR = "&";
        public static final String NAME_VALUE_SEPARATOR = "=";

        private ArrayList<String> mKeys = new ArrayList<String>();
        private ArrayList<String> mValues=new ArrayList<String>();
        
        public ListRequestParameters(){
            
        }
        
        public ListRequestParameters(ArrayList<String> keys, ArrayList<String> values){
            this.mKeys.addAll(keys);
            this.mValues.addAll(values);
        }
        
        public void add(String key, String value){
            if(!TextUtils.isEmpty(key)&&!TextUtils.isEmpty(value)){
                this.mKeys.add(key);
                this.mValues.add(value);
            }
           
        }
        
        public void add(String key, int value){
            this.mKeys.add(key);
            this.mValues.add(String.valueOf(value));
        }
        public void add(String key, long value){
            this.mKeys.add(key);
            this.mValues.add(String.valueOf(value));
        }
        
        public void remove(String key){
            int firstIndex=this.mKeys.indexOf(key);
            if(firstIndex>=0){
                this.mKeys.remove(firstIndex);
                this.mValues.remove(firstIndex);
            }
          
        }
        
        private int getLocation(String key){
            if(this.mKeys.contains(key)){
                return this.mKeys.indexOf(key);
            }
            return -1;
        }        
        
        public String getValue(String key){
            int index=getLocation(key);
            if(index>=0 && index < this.mKeys.size()){
                return  this.mValues.get(index);
            }
            else{
                return null;
            }
        }        
        
        public String getValue(int location){
            if(location>=0 && location < this.mKeys.size()){
                String rlt = this.mValues.get(location);
                return rlt;
            }
            else{
                return null;
            }
            
        }
        
        public String getKey(int location){
            if(location >= 0 && location < this.mKeys.size()){
                return this.mKeys.get(location);
            }
            return "";
        }
        
        public int size(){
            return this.mKeys.size();
        }
        
        public void add(IRequestParameters parameters){
            for (int loc = 0, size = parameters.size(); loc < size; loc++) {
                this.add(parameters.getKey(loc), parameters.getValue(loc));
            }
        }
        
        public void clear(){
            this.mKeys.clear();
            this.mValues.clear();
        }
        
        public boolean isEmpty() {
            return this.mKeys.isEmpty() || this.mValues.isEmpty();
        }
        
        public ListRequestParameters clone() {
            return new ListRequestParameters(this.mKeys, this.mValues);
        }
        
        public boolean containsKey(String key) {
            return this.mKeys.contains(key);
        }
        
        public String getPostParam() {
            return buildQueryString(false, null);
        }
        
        public String getEncodedPostParam(String enc) {
            return buildQueryString(true, enc);
        }
        
        private String buildQueryString(boolean encoded, String enc) {
            StringBuilder buf = new StringBuilder();
            boolean first = true;
            for (int loc = 0; loc < this.mKeys.size(); loc++) {
                String key = this.mKeys.get(loc);
                if (first) {
                    first = false;
                } else {
                    buf.append(PARAMETER_SEPARATOR);
                }
                try {
                    String value = this.mValues.get(loc);
                    if (encoded) {
                        buf.append(URLEncoder.encode(key, enc)).append(NAME_VALUE_SEPARATOR).append(URLEncoder.encode(value, enc));
                    } else {
                        buf.append(key).append(NAME_VALUE_SEPARATOR).append(value);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            
            return buf.toString();
        }

        @Override
        public void remove(int location) {
            if(location<mKeys.size()){
                mKeys.remove(location);
                this.mValues.remove(location);
            }
        }
    }
    
    protected String getActionTag() {
        StringBuilder s = new StringBuilder();
        s.append(apiType);
        s.append('@');
        
        if (hasParameter(SERVER_PARAMETER_DATA_TYPE)) {
            s.append(getParameter(SERVER_PARAMETER_DATA_TYPE));
        }
        s.append('@');
        if (hasParameter(SERVER_PARAMETER_SUB_DATA_TYPE)) {
            s.append(getParameter(SERVER_PARAMETER_SUB_DATA_TYPE));
        }
        s.append('@');
        s.append('@');
        if (hasParameter(SERVER_PARAMETER_REQUSET_SOURCE_TYPE)) {
            s.append(getParameter(SERVER_PARAMETER_REQUSET_SOURCE_TYPE));
        }
        s.append('@');
        if (hasParameter(SERVER_PARAMETER_OPERATION_CODE)) {
            s.append(getParameter(SERVER_PARAMETER_OPERATION_CODE));
        }
        s.append('@');
        
        return s.toString();
    }
}
