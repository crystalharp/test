
package com.tigerknows.model;


import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.BootstrapModel.DomainName;

import android.content.Context;
import android.text.TextUtils;

public final class Bootstrap extends BaseQuery {

    private BootstrapModel bootstrapModel;
    
    public BootstrapModel getBootstrapModel() {
        return bootstrapModel;
    }

    public Bootstrap(Context context) {
        super(context, API_TYPE_BOOTSTRAP);
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

        bootstrapModel = new BootstrapModel(responseXMap);

        DomainName domainName =  bootstrapModel.getDomainName();
        if (domainName != null) {
            TKConfig.setDownloadHost(domainName.getDownload());
            TKConfig.setQueryHost(domainName.getNewQuery());
            TKConfig.setLocationHost(domainName.getLocation());
            TKConfig.setAccountManageHost(domainName.getAccountManage());
        }
        
        String uploadLog = bootstrapModel.getUploadLog();
        if (!TextUtils.isEmpty(uploadLog)) {
            TKConfig.setUserActionTrack(context, uploadLog);
        }
    }
}
