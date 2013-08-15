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
        debugCheckParameters(new String[] {SERVER_PARAMETER_FEEDBACK}, 
                new String[]{SERVER_PARAMETER_ACTION_LOG,
                SERVER_PARAMETER_LOCATION, SERVER_PARAMETER_LOCATION_IN_ANDROID,
                SERVER_PARAMETER_ERROR_RECOVERY, SERVER_PARAMETER_ADD_MERCHANT,
                SERVER_PARAMETER_SATISFY_RATE, SERVER_PARAMETER_POI_RANK, 
                SERVER_PARAMETER_DATA_TYPE, SERVER_PARAMETER_SUB_DATA_TYPE, 
                SERVER_PARAMETER_REQUSET_SOURCE_TYPE});
    }
    
    @Override
    protected void addCommonParameters() {
        super.addCommonParameters(cityId);
        if (!hasParameter(SERVER_PARAMETER_FEEDBACK)) {
            addParameter(SERVER_PARAMETER_FEEDBACK, "");
        }
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
}
