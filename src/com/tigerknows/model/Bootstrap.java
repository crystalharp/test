
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

    // firstlogin  string  false   表示客户端是否为下载后第一次启动， 仅在引导服务(at=l, Bootstrap)时使用
    public static final String SERVER_PARAMETER_FIRST_LOGIN = "firstlogin";
    
    // 0时为本机第一次启动
    public static final String FIRST_LOGIN_NEW = "0";
    
    // 1时为软件升级后第一次启动
    public static final String FIRST_LOGIN_UPGRADE = "1";

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
        String url = String.format(TKConfig.getBootstarpUrl(), bootStrapHostList[bootstrapRetryTime%bootStrapHostList.length]);
        httpClient.setURL(url);
    }

    @Override
    protected void checkRequestParameters() throws APIException {
    }
    
    @Override
    protected void addCommonParameters() {
        super.addCommonParameters();
        addParameter("fe", "");
    }

    @Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);

        bootstrapModel = new BootstrapModel(responseXMap);

        DomainName domainName =  bootstrapModel.getDomainName();
        if (domainName != null) {
            if (TKConfig.LoadBalance) {
                TKConfig.setDynamicDownloadHost(domainName.getDownload());
                TKConfig.setDynamicQueryHost(domainName.getNewQuery());
                TKConfig.setDynamicLocationHost(domainName.getLocation());
                TKConfig.setDynamicAccountManageHost(domainName.getAccountManage());
            }
        }
        
        String uploadLog = bootstrapModel.getUploadLog();
        if (!TextUtils.isEmpty(uploadLog)) {
            TKConfig.setUserActionTrack(context, uploadLog);
        }
    }
}
