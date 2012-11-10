/*
 * @(#)NetSearcher.java  下午03:32:06 2007-11-27 2007
 *
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd.
 * All rights reserved.
 *
 */

package com.tigerknows.model;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.model.response.Appendix;
import com.tigerknows.model.response.DataPackage;
import com.tigerknows.model.response.DownloadDomainCake;
import com.tigerknows.model.response.MapVersionCake;
import com.tigerknows.model.response.PositionCake;
import com.tigerknows.model.response.ResponseCode;
import com.tigerknows.model.response.UUIDInfo;
import com.tigerknows.model.response.UpdateVersionData;
import com.tigerknows.model.response.UserActionTrackSwitch;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.DataEncryptor;
import com.tigerknows.util.HttpUtils;
import com.tigerknows.util.ParserUtil;
import com.tigerknows.util.TKLZDecode;
import com.tigerknows.util.HttpUtils.TKHttpClient.ProgressUpdate;

/**
 *本类是所有联网搜索类的公共父类.
 * 
 * @author pengwenyue
 * @version
 * @see Change log:
 */
public abstract class BaseQuery {
    
    public static boolean Test = false;

    // 交通查询
    public static final String API_TYPE_TRAFFIC_QUERY = "t";
    
    // 线路查询
    public static final String API_TYPE_BUSLINE_QUERY = "b";
    
    // 用户登录
    public static final String API_TYPE_USER_LOGON = "l";
    
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
    public static final String SERVER_PARAMETER_NEED_FEILD = "nf";

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

    // lc  int false   定位城市
    public static final String SERVER_PARAMETER_LOCATION_CITY = "lc";

    // lx  Double  false   定位经度x
    public static final String SERVER_PARAMETER_LOCATION_LONGITUDE = "lx";

    // ly  Double  false   定位纬度y
    public static final String SERVER_PARAMETER_LOCATION_LATITUDE = "ly";

    // lt  int false   local type 定位来源
    public static final String SERVER_PARAMETER_LOCATION_TYPE = "lt";

    // tel string true 手机号
    public static final String SERVER_PARAMETER_TELEPHONE = "tel";
    
    // pic string  true 图片们的宽高信息，格式为key0:width_height_[(0|1)+];key1:width_height_[(0|1)+];... 
    public static final String SERVER_PARAMETER_PICTURE = "pic";

    
    // 数据类型:
    // POI 1
    public static final String DATA_TYPE_POI = "1";

    // 团购 2
    public static final String DATA_TYPE_TUANGOU = "2";

    // 影讯 3
    public static final String DATA_TYPE_YINGXUN = "3";

    // 电影? 4
    public static final String DATA_TYPE_DIANYING = "4";

    // 活动 5
    public static final String DATA_TYPE_HUODONG = "5";

    // 打折商场 6
    public static final String DATA_TYPE_DAZHESHANGCHANG = "6";

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

    // 发现首页动态数据统计 100
    public static final String DATA_TYPE_DISCOVER = "100";
    
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
    
    private static List<NameValuePair> sCommonParameters;
    
    public static void initCommonParameters() {
        
        sCommonParameters = new ArrayList<NameValuePair>();
        sCommonParameters.add(new BasicNameValuePair("dv", "1"));
        sCommonParameters.add(new BasicNameValuePair("e", TKConfig.getIMEI()));

        sCommonParameters.add(new BasicNameValuePair("m", TKConfig.getSpreader()));
        sCommonParameters.add(new BasicNameValuePair("sc", TKConfig.SERVICE_CENTER));
        sCommonParameters.add(new BasicNameValuePair("sg", TKConfig.SG));
        sCommonParameters.add(new BasicNameValuePair("si", TKConfig.SI));
        sCommonParameters.add(new BasicNameValuePair("sv", TKConfig.SV));
        sCommonParameters.add(new BasicNameValuePair("ec", TKConfig.getEncoding()));
        sCommonParameters.add(new BasicNameValuePair("pk", TKConfig.getPhoneKey()));

        sCommonParameters.add(new BasicNameValuePair("vs", TKConfig.getClientSoftVersion()));
        sCommonParameters.add(new BasicNameValuePair("vd", TKConfig.getClientDataVersion()));
        sCommonParameters.add(new BasicNameValuePair("vp", TKConfig.getVersionOfPlatform()));
    }
    
    protected static void addCommonParameters(List<NameValuePair> parameters, int cityId, boolean isLocateMe) {
        parameters.addAll(sCommonParameters);

        if (cityId < MapEngine.CITY_ID_BEIJING && isLocateMe == false) {
            cityId = MapEngine.CITY_ID_BEIJING;
        }
        parameters.add(new BasicNameValuePair("c", String.valueOf(cityId)));
        
        parameters.add(new BasicNameValuePair("d", TKConfig.getIMSI()));
        boolean simAvailably = true;
        int[] cellInfo = TKConfig.getCellLocation();
        int mcc = TKConfig.getMCC();
        int mnc = TKConfig.getMNC();
        int lac = cellInfo[0];
        int cid = cellInfo[1];
        if (isLocateMe && !CommonUtils.mccMncLacCidValid(mcc, mnc, lac, cid)) {
            simAvailably = false;
        }
        
        if (simAvailably) {
            parameters.add(new BasicNameValuePair("mcc", String.valueOf(mcc)));
            parameters.add(new BasicNameValuePair("mnc", String.valueOf(mnc)));
            parameters.add(new BasicNameValuePair("lac", String.valueOf(lac)));
            parameters.add(new BasicNameValuePair("ci", String.valueOf(cid)));
            parameters.add(new BasicNameValuePair("ss", String.valueOf(TKConfig.getSignalStrength())));
        }
    }
    
    protected static void addCommonParameters(List<NameValuePair> parameters) {
        addCommonParameters(parameters, MapEngine.CITY_ID_BEIJING, false);
    }
    
    protected static void addCommonParameters(List<NameValuePair> parameters, int cityId) {
        addCommonParameters(parameters, cityId, false);
    }
    
    protected int loginConnectTime = 0;

    protected Context context;
    
    protected String apiType;
    
    protected String version;
    
    protected int cityId = MapEngine.CITY_ID_BEIJING;

    protected int sourceViewId = -1;
    
    protected int targetViewId = -1;
    
    protected Hashtable<String, String> criteria = null;
    
    protected String tipText = null;
    
    protected List<NameValuePair> requestParameters = new ArrayList<NameValuePair>();

    protected HttpUtils.TKHttpClient httpClient;

    protected boolean isTranslatePart = false;

    protected boolean needReconntection = false;

    protected boolean isStop = false;

    protected int statusCode = STATUS_CODE_NONE;
    
    protected List<Appendix> appendice = new ArrayList<Appendix>();

    protected ProgressUpdate progressUpdate;

    protected boolean compress = false;
    
    protected XMap responseXMap;
    
    protected Response response;
    
    public BaseQuery(Context context, String apiType) {
        this(context, apiType, VERSION);
    }
    
    public BaseQuery(Context context, String apiType, String version) {
        this(context, apiType, version, true);
    }
    
    public BaseQuery(Context context, String apiType, String version, boolean compress) {
        this.context = context;
        this.apiType = apiType;
        this.version = version;
        this.compress = compress;
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
    
    public Hashtable<String, String> getCriteria() {
        return criteria;
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

    public void setup(Hashtable<String, String> criteria) {
        setup(criteria, -1, -1, -1);
    }

    public void setup(Hashtable<String, String> criteria, int cityId) {
        setup(criteria, cityId, -1, -1);
    }
    
    public void setup(Hashtable<String, String> criteria, int cityId, int sourceViewId, int targetViewId) {
        setup(criteria, cityId, sourceViewId, targetViewId, null);
    }
    
    public void setup(Hashtable<String, String> criteria, int cityId, int sourceViewId, int targetViewId, String tipText) {
        setup(criteria, cityId, sourceViewId, targetViewId, tipText, false);
    }
    
    public void setup(Hashtable<String, String> criteria, int cityId, int sourceViewId, int targetViewId, String tipText, boolean needReconntection) {
        this.criteria = criteria;
        this.cityId = cityId;
        this.sourceViewId = sourceViewId;
        this.targetViewId = targetViewId;
        this.tipText = tipText;
        this.needReconntection = needReconntection; 
    }

    public void query() {
        statusCode = STATUS_CODE_NONE;
        appendice.clear();
        try {
            makeRequestParameters();
        } catch (APIException e) {
            e.printStackTrace();
            statusCode = STATUS_CODE_MISSING_REQUEST_PARAMETER;
            return;
        }
        
        createHttpClient();
        execute();
    }
    
    protected void addMyLocationParameters() {
        final CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        if (myLocationCityInfo != null) {
            final Position myLocationPosition = myLocationCityInfo.getPosition();
            if (myLocationPosition != null) {
                if (criteria != null) {
                    criteria.put(DataQuery.SERVER_PARAMETER_LOCATION_CITY, String.valueOf(myLocationCityInfo.getId()));
                    criteria.put(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE, String.valueOf(myLocationPosition.getLon()));
                    criteria.put(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE, String.valueOf(myLocationPosition.getLat()));
                }
                requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_LOCATION_CITY, String.valueOf(myLocationCityInfo.getId())));
                requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_LOCATION_LONGITUDE, String.valueOf(myLocationPosition.getLon())));
                requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_LOCATION_LATITUDE, String.valueOf(myLocationPosition.getLat())));
                requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_LOCATION_TYPE, String.valueOf(myLocationPosition.getType())));
            }
        }
    }
    
    protected void makeRequestParameters() throws APIException {
        requestParameters.clear();
        requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_API_TYPE, apiType));
        requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_VERSION, version));
        addMyLocationParameters();
    }
    
    protected void createHttpClient() {
        httpClient = new HttpUtils.TKHttpClient();
        httpClient.setApiType(apiType);
        httpClient.setIsEncrypt(true);
        httpClient.setParameters(requestParameters);
    }
    
    protected void launchTest() {
    }
    
    private void execute() {
        boolean isFirstConnection = true;

        while ((isFirstConnection || needReconntection) && !isStop) {
            try {
                if (Test && (apiType.equals(API_TYPE_BUSLINE_QUERY) == false &&
                        apiType.equals(API_TYPE_LOCATION_QUERY) == false &&
                        apiType.equals(API_TYPE_MAP_META_DOWNLOAD) == false &&
                        apiType.equals(API_TYPE_MAP_TILE_DOWNLOAD) == false &&
                        apiType.equals(API_TYPE_MAP_VERSION_QUERY) == false &&
                        apiType.equals(API_TYPE_SUGGEST_LEXICON_DOWNLOAD) == false &&
                        apiType.equals(API_TYPE_TRAFFIC_QUERY) == false &&
                        apiType.equals(API_TYPE_USER_LOGON) == false)) {
                    LogWrapper.d("BaseQuery", "execute() requestParameters="+requestParameters.toString());
                    launchTest();
                    httpClient.launchTest(ByteUtil.xobjectToByte(responseXMap), STATUS_CODE_NETWORK_OK);
                } else {
                    httpClient.execute(context);
                }
                statusCode = httpClient.getStatusCode();
                if (statusCode == STATUS_CODE_NETWORK_OK) {
                    byte[] data = httpClient.getData();
                    if (data != null) {
                        LogWrapper.i("BaseQuery", "execute():at="+apiType+", response="+data.length);
                    } else {
                        LogWrapper.i("BaseQuery", "execute():at="+apiType+", response="+data+", stop="+isStop);
                    }
                    translate(data);
                    break;
                }
            } catch (IOException e) {
                if (API_TYPE_MAP_META_DOWNLOAD.equals(apiType) ||
                        API_TYPE_MAP_TILE_DOWNLOAD.equals(apiType) ||
                        API_TYPE_MAP_VERSION_QUERY.equals(apiType) ||
                        API_TYPE_SUGGEST_LEXICON_DOWNLOAD.equals(apiType)) {
                    if (TKConfig.sDYNAMIC_DOWNLOAD_HOST != null) {
                        TKConfig.sDYNAMIC_DOWNLOAD_HOST = null;
                        createHttpClient();
                        continue;
                    }
                } else if (API_TYPE_BUSLINE_QUERY.equals(apiType) ||
                        API_TYPE_TRAFFIC_QUERY.equals(apiType) ||
                        API_TYPE_DATA_QUERY.equals(apiType) ||
                        API_TYPE_DATA_OPERATION.equals(apiType)) {
                    if (TKConfig.sDYNAMIC_QUERY_HOST != null) {
                        TKConfig.sDYNAMIC_QUERY_HOST = null;
                        createHttpClient();
                        continue;
                    }
                } else if (API_TYPE_LOCATION_QUERY.equals(apiType)) {
                    if (TKConfig.sDYNAMIC_LOCATION_HOST != null) {
                        TKConfig.sDYNAMIC_LOCATION_HOST = null;
                        createHttpClient();
                        continue;
                    }
                } else if (API_TYPE_ACCOUNT_MANAGE.equals(apiType)) {
                    if (TKConfig.sDYNAMIC_ACCOUNT_MANAGE_HOST != null) {
                        TKConfig.sDYNAMIC_ACCOUNT_MANAGE_HOST = null;
                        createHttpClient();
                        continue;
                    }
                } else if (API_TYPE_USER_LOGON.equals(apiType)) {
                    loginConnectTime++;
                    if (loginConnectTime < 3) {
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

        LogWrapper.i("BaseQuery", "execute():at="+apiType+", statusCode="+statusCode+", isStop="+isStop);
    }

    protected void translate(byte[] data) {
        if (data == null || data.length == 0) {
            statusCode = STATUS_CODE_DATA_EMPTY;
            return;
        }
        if ((isStop == false || isTranslatePart)) {
            try {
                if (API_TYPE_MAP_META_DOWNLOAD.equals(apiType) ||
                        API_TYPE_MAP_TILE_DOWNLOAD.equals(apiType) ||
                        API_TYPE_MAP_VERSION_QUERY.equals(apiType) ||
                        API_TYPE_SUGGEST_LEXICON_DOWNLOAD.equals(apiType) ||
                        API_TYPE_LOCATION_QUERY.equals(apiType))  {
                    ParserUtil util = new ParserUtil(data, TKConfig.getEncoding());
                    translateResponseV12(util);
                    statusCode = STATUS_CODE_RESPONSE_EMPTY + Response.RESPONSE_CODE_OK;
                } else {
                    translateResponse(data);
                    LogWrapper.d("BaseQuery", "translate():at="+apiType+", response="+response);
                    if (response != null) {
                        statusCode += response.getResponseCode();
                    }
                }
            } catch (APIException e) {
                e.printStackTrace();
                int statusCode = e.getStatusCode();
                if (statusCode > 0) {
                    this.statusCode = statusCode;
                }
            } catch (Exception e) {
                e.printStackTrace();
                statusCode = STATUS_CODE_TRANSLATE_ERROR;
            }
        } else {
            statusCode = STATUS_CODE_CANCEL;
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
            if (Test == false || 
                    apiType.equals(API_TYPE_BUSLINE_QUERY) ||
                    apiType.equals(API_TYPE_TRAFFIC_QUERY) ||
                    apiType.equals(API_TYPE_USER_LOGON)) {
            // 解密数据
            data = DataEncryptor.decrypt(data);
            // 解压数据
            if (compress) {
                try {
                    data = TKLZDecode.decode(data, 0);
                } catch (Exception cause) {
                    throw new APIException("decode data error", STATUS_CODE_DECODE_ERROR);
                }
            }
            }
            try {
                responseXMap = (XMap) ByteUtil.byteToXObject(data);
            } catch (Exception cause) {
                throw new APIException("byte to xmap error", STATUS_CODE_BYTE_TO_XMAP_ERROR);
            }
            LogWrapper.d("BaseQuery", "translateResponse():at="+apiType+", responseXMap="+responseXMap);
            statusCode = STATUS_CODE_RESPONSE_EMPTY;
        } catch (APIException cause) {
            throw cause;
        } catch (Exception cause) {
            throw new APIException(cause);
        }
    }

    public static boolean checkStatus(Activity activity, BaseQuery baseQuery) {
        int statusCode = baseQuery.getStatusCode();
        if (statusCode < BaseQuery.STATUS_CODE_NONE) {
            // 缺少参数
            if (activity != null)
                Toast.makeText(activity, "缺少参数", Toast.LENGTH_LONG).show();
            return false;
        } else if (statusCode != BaseQuery.STATUS_CODE_NETWORK_OK && statusCode < BaseQuery.STATUS_CODE_DATA_EMPTY) {
            // 网络出错
            if (activity != null)
                Toast.makeText(activity, "网络出错", Toast.LENGTH_LONG).show();
            return false;
        } else if (statusCode == BaseQuery.STATUS_CODE_DATA_EMPTY) {
            // 服务器返回的0个字节的数据
            if (activity != null)
                Toast.makeText(activity, "服务器返回的0个字节的数据", Toast.LENGTH_LONG).show();
            return false;
        } else if (statusCode > BaseQuery.STATUS_CODE_DATA_EMPTY && statusCode < BaseQuery.STATUS_CODE_RESPONSE_EMPTY) {
            // 解压解密服务器返回的数据时出错
            if (activity != null)
                Toast.makeText(activity, "解压解密服务器返回的数据时出错", Toast.LENGTH_LONG).show();
            return false;
        } else if (statusCode == BaseQuery.STATUS_CODE_RESPONSE_EMPTY) {
            // 解释服务器返回的数据时出错
            if (activity != null)
                Toast.makeText(activity, "解释服务器返回的数据时出错", Toast.LENGTH_LONG).show();
            return false;
        } else {
            // 解释完服务器返回的数据
            return true;
        }
    }
    
    public static void passLocationToCriteria(Hashtable<String, String> source, Hashtable<String, String> target) {
        if (source == null || target == null) {
            return;
        }
        if (source.containsKey(DataQuery.SERVER_PARAMETER_LOCATION_CITY)
                && source.containsKey(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE)
                && source.containsKey(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE)) {
            target.put(DataQuery.SERVER_PARAMETER_LOCATION_CITY, source.get(SERVER_PARAMETER_LOCATION_CITY));
            target.put(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE, source.get(SERVER_PARAMETER_LOCATION_LONGITUDE));
            target.put(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE, source.get(SERVER_PARAMETER_LOCATION_LATITUDE));
        }
        if (source.containsKey(DataQuery.SERVER_PARAMETER_CITY)) {
            target.put(DataQuery.SERVER_PARAMETER_CITY, source.get(SERVER_PARAMETER_CITY));
        }
        if (source.containsKey(DataQuery.SERVER_PARAMETER_LONGITUDE)
                && source.containsKey(DataQuery.SERVER_PARAMETER_LATITUDE)) {
            target.put(DataQuery.SERVER_PARAMETER_LONGITUDE, source.get(DataQuery.SERVER_PARAMETER_LONGITUDE));
            target.put(DataQuery.SERVER_PARAMETER_LATITUDE, source.get(DataQuery.SERVER_PARAMETER_LATITUDE));
        }
        if (source.containsKey(DataQuery.SERVER_PARAMETER_FILTER)) {
            target.put(DataQuery.SERVER_PARAMETER_FILTER, source.get(DataQuery.SERVER_PARAMETER_FILTER));
        }
    }
}
