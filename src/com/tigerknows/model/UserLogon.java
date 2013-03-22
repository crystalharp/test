
package com.tigerknows.model;


import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.UserLogonModel.DomainName;

import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.text.TextUtils;

public final class UserLogon extends BaseQuery {

    private UserLogonModel userLogonModel;
    
    public UserLogonModel getUserLogonModel() {
        return userLogonModel;
    }

    public UserLogon(Context context) {
        super(context, API_TYPE_USER_LOGON);
    }

    @Override
    protected void createHttpClient() {
        super.createHttpClient();
        String url = String.format(TKConfig.LOGIN_URL, TKConfig.LOGIN_HOST[loginConnectTime%3]);
        httpClient.setURL(url);
    }

    @Override
    protected void makeRequestParameters() throws APIException {
        super.makeRequestParameters();
        addCommonParameters(requestParameters);
        // TODO 这个参数必须要，那怕是空？！
        requestParameters.add("fe", "");
    }

    @Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);

        userLogonModel = new UserLogonModel(responseXMap);

        DomainName domainName =  userLogonModel.getDomainName();
        if (domainName != null) {
            TKConfig.setDownloadHost(domainName.getDownload());
            TKConfig.setQueryHost(domainName.getNewQuery());
            TKConfig.setLocationHost(domainName.getLocation());
            TKConfig.setAccountManageHost(domainName.getAccountManage());
        }
        
        String uploadLog = userLogonModel.getUploadLog();
        if (!TextUtils.isEmpty(uploadLog)) {
            TKConfig.setUserActionTrack(context, uploadLog);
        }
    }
}
