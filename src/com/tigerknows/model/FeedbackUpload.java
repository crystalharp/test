package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.test.BaseQueryTest;

import android.content.Context;

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
    
    public static final String SERVER_PARAMETER_POI_RANK = "poirank";
    
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
                SERVER_PARAMETER_SATISFY_RATE, SERVER_PARAMETER_POI_RANK, 
                SERVER_PARAMETER_DATA_TYPE, SERVER_PARAMETER_SUB_DATA_TYPE, 
                SERVER_PARAMETER_REQUSET_SOURCE_TYPE, SERVER_PARAMETER_FEEDBACK});
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
    }
    
    protected void launchTest() {
        super.launchTest();
        responseXMap = BaseQueryTest.launchResponse();
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
        feedbackUpload.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_POI);
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
