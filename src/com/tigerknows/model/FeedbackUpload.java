package com.tigerknows.model;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.util.Utility;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class FeedbackUpload extends BaseQuery {
    
    // fe string true 反馈内容
    public static final String SERVER_PARAMETER_FEEDBACK = "fe";

    // tr String false 用户行为日志，格式见用户行为跟踪
    public static final String SERVER_PARAMETER_ACTION_LOG = "tr";

    // lu String false 定位上传, 格式
    public static final String SERVER_PARAMETER_LOCATION = "lu";

    // lau String false android平台定位数据上传,格式同上
    public static final String SERVER_PARAMETER_LOCATION_IN_ANDROID = "lau";

    // md String false POI纠错日志，格式见POI纠错功能说明
    public static final String SERVER_PARAMETER_ERROR_RECOVERY = "md";

    // nc String false 添加商户
    public static final String SERVER_PARAMETER_ADD_MERCHANT = "nc";
    
    // sat String false 满意度评分
    public static final String SERVER_PARAMETER_SATISFY_RATE = "sat";
    
    // applist String false 应用列表
    public static final String SERVER_PARAMETER_APPLIST = "applist";
    
    public static final String SERVER_PARAMETER_POI_RANK = "poirank";

    // lip=时间，纬度坐标，经度坐标，精度范围
    public static final String SERVER_PARAMETER_LOCATION_IP = "lip";

    // display  string  false   启动展示内容
    public static final String SERVER_PARAMETER_DISPLAY = "display";
    
    // POI纠错页区分
    public static final String LOCAL_PARAMETER_POIERROR_IGNORE = "ignore";
    
    public FeedbackUpload(Context context) {
        super(context, API_TYPE_FEEDBACK_UPLOAD, VERSION);
    }

    @Override
    protected void checkRequestParameters() throws APIException {
        debugCheckParameters(new String[] {}, 
                new String[]{SERVER_PARAMETER_ACTION_LOG, DataQuery.SERVER_PARAMETER_POI_ID, 
                SERVER_PARAMETER_LOCATION, SERVER_PARAMETER_LOCATION_IN_ANDROID,
                SERVER_PARAMETER_ERROR_RECOVERY, SERVER_PARAMETER_ADD_MERCHANT,
                SERVER_PARAMETER_SATISFY_RATE, SERVER_PARAMETER_APPLIST, SERVER_PARAMETER_POI_RANK, 
                SERVER_PARAMETER_DATA_TYPE, SERVER_PARAMETER_SUB_DATA_TYPE, 
                SERVER_PARAMETER_REQUSET_SOURCE_TYPE, SERVER_PARAMETER_FEEDBACK,
                SERVER_PARAMETER_LOCATION_IP, SERVER_PARAMETER_DISPLAY});
    }
    
    @Override
    protected void addCommonParameters() {
        super.addCommonParameters();
        addSessionId();
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
        this.response = new Response(responseXMap);
        
        if (hasParameter(SERVER_PARAMETER_DISPLAY)) {
            synchronized (Globals.StartupDisplayLogFile) {
                File file = new File(Globals.StartupDisplayLogFile);
                try {
                    String uploadLog = getParameter(FeedbackUpload.SERVER_PARAMETER_DISPLAY);
                    String startupDisplayLog = Utility.readFile(new FileInputStream(file));
                    if (startupDisplayLog != null && uploadLog != null) {
                        startupDisplayLog = startupDisplayLog.replace(uploadLog, "");
                        Utility.writeFile(Globals.StartupDisplayLogFile, startupDisplayLog.getBytes(), true);
                    }
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    @Override
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
        if (hasParameter(FeedbackUpload.SERVER_PARAMETER_FEEDBACK)) {
            s.append(FeedbackUpload.SERVER_PARAMETER_FEEDBACK);
        } else if (hasParameter(FeedbackUpload.SERVER_PARAMETER_ACTION_LOG)) {
            s.append(FeedbackUpload.SERVER_PARAMETER_ACTION_LOG);
        } else if (hasParameter(FeedbackUpload.SERVER_PARAMETER_LOCATION)) {
            s.append(FeedbackUpload.SERVER_PARAMETER_LOCATION);
        } else if (hasParameter(FeedbackUpload.SERVER_PARAMETER_LOCATION_IN_ANDROID)) {
            s.append(FeedbackUpload.SERVER_PARAMETER_LOCATION_IN_ANDROID);
        } else if (hasParameter(FeedbackUpload.SERVER_PARAMETER_ERROR_RECOVERY)) {
            s.append(FeedbackUpload.SERVER_PARAMETER_ERROR_RECOVERY);
        } else if (hasParameter(FeedbackUpload.SERVER_PARAMETER_ADD_MERCHANT)) {
            s.append(FeedbackUpload.SERVER_PARAMETER_ADD_MERCHANT);
        } else if (hasParameter(FeedbackUpload.SERVER_PARAMETER_SATISFY_RATE)) {
            s.append(FeedbackUpload.SERVER_PARAMETER_SATISFY_RATE);
        } else if (hasParameter(FeedbackUpload.SERVER_PARAMETER_APPLIST)) {
            s.append(FeedbackUpload.SERVER_PARAMETER_APPLIST);
        } else if (hasParameter(FeedbackUpload.SERVER_PARAMETER_LOCATION_IP)) {
            s.append(FeedbackUpload.SERVER_PARAMETER_LOCATION_IP);
        } else if (hasParameter(FeedbackUpload.SERVER_PARAMETER_DISPLAY)) {
            s.append(FeedbackUpload.SERVER_PARAMETER_DISPLAY);
        }
        s.append('@');
        if (hasParameter(SERVER_PARAMETER_REQUSET_SOURCE_TYPE)) {
            s.append(getParameter(SERVER_PARAMETER_REQUSET_SOURCE_TYPE));
        }
        s.append('@');
        s.append('@');
        
        return s.toString();
    }
    
    /**
     * 目前用户在点击各种poi详情页面的时候会向服务器发起多个请求,导致没有办法精确统计用户进入详情页的行为,设计解决办法如下:
     * 在进入任何详情页面(包括poi,动态poi)的时候,向服务器发送额外发送一条请求,其中包含所有公共字段和下面几个关键字段
     * at=f,
     * dty=数据类型
     * subty=(如果有)
     * dsrc=发出请求的页面名称(使用行为日志定义的名称即可),
     * poirank=poi在列表中的序数,
     * poiid(dataid)=poi的uuid(动态poi情况下,只要用户点击的那一条poi的id,不需要其它预加载的数据)
     * 以上功能在用户进入普通poi详情时已经实现,现在需要在用户进入动态poi时也添加相关记录
     * http://192.168.11.147/wiki/index.php/%E6%95%B0%E6%8D%AE%E6%8C%96%E6%8E%98
     * 
     * @param context
     * @param dataType
     * @param subDataType
     * @param actionTag
     * @param rank
     * @param uuid
     * @param cityId
     * @return
     */
    public static void logEnterPOIDetailUI(Context context, String dataType, String subDataType, String actionTag, int rank, String uuid, int cityId) {
        final FeedbackUpload feedbackUpload = new FeedbackUpload(context);
        feedbackUpload.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, dataType);
        if (subDataType != null) {
            feedbackUpload.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, subDataType);
        }
        feedbackUpload.addParameter(DataQuery.SERVER_PARAMETER_REQUSET_SOURCE_TYPE, actionTag);
        feedbackUpload.addParameter(FeedbackUpload.SERVER_PARAMETER_POI_RANK, String.valueOf(rank));
        feedbackUpload.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, uuid);
        feedbackUpload.setCityId(cityId);
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                feedbackUpload.query();
            }
        }).start();
    }
}
