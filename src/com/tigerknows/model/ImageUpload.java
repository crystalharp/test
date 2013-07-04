
package com.tigerknows.model;


import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.android.net.HttpManager;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.HttpUtils;

import org.apache.http.client.HttpClient;

import android.content.Context;

/**
 * 图片上传服务类
 * @author pengwenyue
 *
 */
public final class ImageUpload extends BaseQuery {

    public static final String SERVER_PARAMETER_MD5 = "md5";
    
    public ImageUpload(Context context) {
        super(context, API_TYPE_IMAGE_UPLOAD);
    }

    public void query() {
        statusCode = STATUS_CODE_NONE;
        
        if (criteria != null) {
            responseXMap = new XMap();
            try {
                addParameter(new String[] {SERVER_PARAMETER_MD5, SERVER_PARAMETER_PICTURE});
                
                HttpUtils.TKHttpClient.modifyRequestData(requestParameters);
                
                HttpClient client = HttpManager.getNewHttpClient();
                String url = String.format(TKConfig.getImageUploadUrl(), TKConfig.getImageUploadHost());
                HttpManager.openUrl(context, client, url, HttpManager.HTTPMETHOD_POST, requestParameters);
                statusCode = Response.RESPONSE_CODE_OK;
                responseXMap.put(Response.FIELD_RESPONSE_CODE, Response.RESPONSE_CODE_OK);
                
            } catch (Exception e) {
                responseXMap.put(Response.FIELD_RESPONSE_CODE, Response.RESPONSE_CODE_SERVER_ERROR);
                e.printStackTrace();
            }
            
            modifyResponseData();
            
            try {
                response = new Response(responseXMap);
            } catch (APIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
