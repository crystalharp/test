/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.text.TextUtils;

import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.RegionMetaVersion;
import com.tigerknows.model.response.Appendix;
import com.tigerknows.model.response.DataPackage;
import com.tigerknows.model.response.ResponseCode;
import com.tigerknows.util.ParserUtil;
import com.tigerknows.util.HttpUtils.TKHttpClient;

/**
 * @author Peng Wenyue
 */
public class MapMetaFileDownload extends BaseQuery {
    
    protected static final String VERSION = "1";

    private MapEngine mapEngine;
    
    public MapMetaFileDownload(Context context, MapEngine mapEngine) {
        super(context, API_TYPE_MAP_META_DOWNLOAD, VERSION);
        this.mapEngine = mapEngine;
        this.requestParameters = new ListRequestParameters();
    }
    
    private int mRegionId;
    
    public void setup(int regionId) {
        mRegionId = regionId;
    } 
    
    long lastTime = -1;
    /**
          参数名 类型  必选  意义
    at  string  true    api type，固定为md
    v   integer true    服务版本
    rid integer true    区域id 
    off integer false   偏移量，默认为0，如果大于meta数据长度报错
    vd  string off>0时必选 数据版本
    release:http://download.tigerknows.net/quantum/string?at=md&v=1&rid=109&off=6000&vd=2.0.2010.11.18
            http://download.tigerknows.net/quantum/string?at=md&v=1&rid=109
    test:http://192.168.11.119:8080/quantum/string?at=md&v=1&rid=109&off=0&vd=2.0.2010.11.18
    */
    @Override
    public synchronized void query() {
        isStop = false;
        this.requestParameters.clear();
        super.query();
        
        long currentTime = System.currentTimeMillis();
        if (statusCode != STATUS_CODE_NETWORK_OK && statusCode > STATUS_CODE_NONE) {
            httpClient = null;
            if (lastTime > 0 && currentTime - lastTime > KEEP_ALIVE_TIME) {
                statusCode = STATUS_CODE_NETWORK_OK;
            }
        }
        lastTime = currentTime;
    }
    
    @Override
    protected void addMyLocationParameters() {
        
    }

    @Override
    protected void addCommonParameters() {
        addParameter(SERVER_PARAMETER_API_TYPE, apiType);
        addParameter(SERVER_PARAMETER_VERSION, version);
        addParameter("rid", String.valueOf(mRegionId));
        addParameter("vs", TKConfig.getClientSoftVersion());
        String mapPath = TKConfig.getDataPath(true);
        if (TextUtils.isEmpty(mapPath)) {
            return;
        }
        File metaFile = new File(mapPath + String.format(TKConfig.MAP_REGION_METAFILE, mRegionId));
        if (metaFile.exists()) {
            long off = metaFile.length();
            if (off > 30) {
                addParameter("off", String.valueOf(off));
                String dataVersion = "";
                RegionMetaVersion version = mapEngine.getRegionMetaVersion(mRegionId);
                if (null != version) {
                    dataVersion = version.toString();
                }
                addParameter("vd", dataVersion);
            } else {
                metaFile.delete();
            }
        }
    }

    @Override
    protected void createHttpClient() {
        if (httpClient == null) {
            httpClient = new TKHttpClient();
            httpClient.setKeepAlive(true);
        }
        String url = String.format(TKConfig.getDownloadMapUrl(), TKConfig.getDownloadHost());
        httpClient.setURL(url);
        httpClient.setParameters(requestParameters);
        httpClient.setProgressUpdate(progressUpdate);
    }

    @Override
    protected void translateResponseV12(ParserUtil util) throws IOException {
        super.translateResponseV12(util);
        DataPackage dataPackage = null;
        ResponseCode responseCode = null;
        for (Appendix appendix : appendice) {
            if (appendix.type == Appendix.TYPE_DATA_PACKAGE) {
                dataPackage = (DataPackage) appendix;
            } else if (appendix.type == Appendix.TYPE_RESPONSE_CODE) {
                responseCode = (ResponseCode) appendix;
            }
        }
        if (responseCode != null) {
            String mapPath = mapEngine.getMapPath();
            if (TextUtils.isEmpty(mapPath)) {
                return;
            }
            File regionMetaFile = new File(mapPath + String.format(TKConfig.MAP_REGION_METAFILE, mRegionId));
            if (responseCode.getResponseCode() == ResponseCode.MAP_META_OK && dataPackage != null) {
                ParserUtil parseUtil = dataPackage.getData();
                int length = parseUtil.availableDataleng();

                if (length > 0) {
                    FileOutputStream fout = new FileOutputStream(regionMetaFile, true);
                    fout.write(parseUtil.getData(), parseUtil.getStart(), length);
                    fout.close();
                    if (httpClient.isReceivedAllData()) {
                        mapEngine.initRegion(mRegionId);
                    }
                }
            } else if (responseCode.getResponseCode() == ResponseCode.MAP_META_NEW_REVISION) {
                if (regionMetaFile.exists()) {
                    regionMetaFile.delete();
                }
            }
        }
    }

    public int getRegionId() {
        return mRegionId;
    }

    @Override
    protected void checkRequestParameters() throws APIException {
        // TODO Auto-generated method stub
        
    }
}
