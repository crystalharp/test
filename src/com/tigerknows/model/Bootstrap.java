
package com.tigerknows.model;


import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.BootstrapModel.DomainName;

import android.content.Context;
import android.text.TextUtils;

/**
 * 引导服务类
 * @author pengwenyue
 *
 */
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
        String[] bootStrapHostList = TKConfig.getBootStrapHostList();
        String url = String.format(TKConfig.getBootstarpUrl(), bootStrapHostList[loginConnectTime%bootStrapHostList.length]);
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
            TKConfig.setDynamicDownloadHost(domainName.getDownload());
            TKConfig.setDynamicQueryHost(domainName.getNewQuery());
            TKConfig.setDynamicLocationHost(domainName.getLocation());
            TKConfig.setDynamicAccountManageHost(domainName.getAccountManage());
        }
        
        String uploadLog = bootstrapModel.getUploadLog();
        if (!TextUtils.isEmpty(uploadLog)) {
            TKConfig.setUserActionTrack(context, uploadLog);
        }
    }
}
