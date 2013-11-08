
package com.tigerknows.model;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.android.net.HttpManager;
import com.tigerknows.crypto.DataEncryptor;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.HttpUtils;
import com.tigerknows.util.Utility;
import com.tigerknows.util.ZLibUtils;
import com.weibo.sdk.android.WeiboParameters;

import org.apache.http.client.HttpClient;

import android.content.Context;

/**
 * 文件上传服务类
 * 
 * @author pengwenyue
 */
public final class FileUpload extends BaseQuery {

    public static final String SERVER_PARAMETER_FILE_TYPE = "type";

    public static final String SERVER_PARAMETER_CHECKSUM = "checksum";

    public static final String SERVER_PARAMETER_FILENAME = "filename";

    public static final String SERVER_PARAMETER_PARAMS = "params";

    public static final String SERVER_PARAMETER_UPFILE = "upfile";

    // image 图片
    public static final String FILE_TYPE_IMAGE = "image";

    // text 文本文件
    public static final String FILE_TYPE_TEXT = "text";

    // video 视频
    public static final String FILE_TYPE_VIDEO = "video";

    // office 办公兼容类别文档(word/ppt/excel等)
    public static final String FILE_TYPE_OFFICE = "office";

    public FileUpload(Context context) {
        super(context, API_TYPE_FILE_UPLOAD);
    }

    public void query() {
        statusCode = STATUS_CODE_NONE;

        try {
            addSessionId();
            addCommonParameters(Globals.getCurrentCityInfo().getId(), false);
            
            checkRequestParameters();

            HttpUtils.TKHttpClient.modifyRequestData(getParameters());

            String postParam = getParameters().getPostParam();
            byte[] data = postParam.getBytes("UTF-8");
            data = ZLibUtils.compress(data);
            DataEncryptor.getInstance().encrypt(data);

            WeiboParameters parameters = new WeiboParameters();
            parameters.add(SERVER_PARAMETER_PARAMS, Utility.toHexString(data));
            parameters.add(SERVER_PARAMETER_UPFILE,
                    getParameter(SERVER_PARAMETER_UPFILE));
            parameters.add(SERVER_PARAMETER_FILENAME,
                    getParameter(SERVER_PARAMETER_FILENAME));

            HttpClient client = HttpManager.getNewHttpClient();
            String url = String.format(TKConfig.getImageUploadUrl(),
                    TKConfig.getImageUploadHost());

            LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", url="+url);
            LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", parameters="+postParam+", TKConfig.getEncoding()="+TKConfig.getEncoding());

            byte result[] = null;
            try {
                result = HttpManager.openUrl(context, client, url,
                        HttpManager.HTTPMETHOD_POST, parameters, getActionTag(), getParameter(SERVER_PARAMETER_UUID));
                statusCode = STATUS_CODE_NETWORK_OK;
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (TKConfig.LaunchTest == 1) {
                launchTest();
                result = ByteUtil.xobjectToByte(responseXMap);
                statusCode = STATUS_CODE_NETWORK_OK;
            }

            translate(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);
        this.response = new Response(responseXMap);
    }

    @Override
    protected void checkRequestParameters() throws APIException {
        String[] ekeys = new String[]{SERVER_PARAMETER_FILE_TYPE, SERVER_PARAMETER_REF_DATA_TYPE, SERVER_PARAMETER_CHECKSUM, SERVER_PARAMETER_FILENAME, SERVER_PARAMETER_UPFILE};
        String[] okeys = new String[]{SERVER_PARAMETER_REF_ID};
        debugCheckParameters(ekeys, okeys);
        
    }
}
