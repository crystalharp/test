/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.model;

import java.io.IOException;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

import android.content.Context;

import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.TileDownload;
import com.tigerknows.model.response.Appendix;
import com.tigerknows.model.response.DataPackage;
import com.tigerknows.model.response.ResponseCode;
import com.tigerknows.util.HttpUtils;
import com.tigerknows.util.ParserUtil;
import com.tigerknows.util.HttpUtils.TKHttpClient;
import com.tigerknows.util.HttpUtils.TKHttpClient.RealTimeRecive;

/**
 * @author zhou wentao
 */
public class MapTileDataDownload extends BaseQuery {
    
    protected static final String VERSION = "1";

    public interface ITileDownload {
        public int fillMapTile(List<TileDownload> tileInfos, int rid, byte[] data, int start);
        public void upgradeRegion(int rid);
    }
    
    private List<TileDownload> tileDownloads;
    private int rid;

    private ITileDownload iTileDownload;
    
    private HttpUtils.TKHttpClient.RealTimeRecive realTimeRecive = new RealTimeRecive() {
        
        @Override
        public void reciveData(byte[] data) {
            if (isStop) {
                return;
            }
            if (appendice.isEmpty()) {
               translate(data);
            } else {
                if (data != null && data.length > 0 && iTileDownload != null) {
                    iTileDownload.fillMapTile(tileDownloads, rid, data, 0);
                }
            }
        }
    };
    
    public MapTileDataDownload(Context context, MapEngine mapEngine) {
        super(context, API_TYPE_MAP_TILE_DOWNLOAD, VERSION);
        this.isTranslatePart = true;
    }
    
    public void setFillMapTile(ITileDownload iTileDownload) {
        this.iTileDownload = iTileDownload;
    }
    
    public void setup(List<TileDownload> tileDownloadList, int rid) {
        this.tileDownloads = tileDownloadList;
        this.rid = rid;
    }

    long lastTime = -1;
    /**
          参数名称 类型  必选  意义
    at  string  true    api type，固定为td
    v   integer true    服务版本
    rid integer true    区域id
    vd  string  true    数据版本
    off intArray    true    文件的偏移量
    len intArray    true    下载文件长度 
    http://192.168.11.119:8080/quantum/string?at=td&rid=233&v=1&vd=2.0.2010.11.18&off=294377&len=842
    http://download.tigerknows.net/quantum/string?at=td&rid=233&v=1&vd=2.0.2010.11.18&off=294377&len=842
    */
    @Override
    public synchronized void query() {
        isStop = false;
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
    protected void makeRequestParameters() throws APIException {
        super.makeRequestParameters();
        requestParameters.add(new BasicNameValuePair("rid", String.valueOf(rid)));
        requestParameters.add(new BasicNameValuePair("vs", TKConfig.getClientSoftVersion()));
        String version = null;
        int count = 0;
        
        for(TileDownload tileDownload:tileDownloads) {
            if (tileDownload.getRid() == rid && tileDownload.getLength() > 0) {
                if (count == 0) {
                    version = tileDownload.getVersion();
                }
                String currentVersion = tileDownload.getVersion();
                if ((version != null && version.equals(currentVersion)) || (version == null && currentVersion == null)) {
                    count++;
                    requestParameters.add(new BasicNameValuePair("off", String.valueOf(tileDownload.getOffset())));
                    requestParameters.add(new BasicNameValuePair("len", String.valueOf(tileDownload.getLength())));
                }
            }
        }
        if (count <= 0) {
            throw APIException.wrapToMissingRequestParameterException("off,len");
        }
        if (version != null) {
            requestParameters.add(new BasicNameValuePair("vd", version));
        } else {
            throw APIException.wrapToMissingRequestParameterException("vd");
        }
    }

    @Override
    protected void createHttpClient() {
        if (httpClient == null) {
            httpClient = new TKHttpClient();
            httpClient.setKeepAlive(true);
        }
        httpClient.setApiType(apiType);
        String url = String.format(TKConfig.getDownloadMapUrl(), TKConfig.getDownloadHost());
        httpClient.setURL(url);
        httpClient.setParameters(requestParameters);
        
        httpClient.setRealTimeRecive(realTimeRecive);
        httpClient.setProgressUpdate(progressUpdate);
    }

    @Override
    protected void translateResponseV12(ParserUtil util) throws IOException {
        super.translateResponseV12(util);
        DataPackage dataPackage = null;
        ResponseCode responseCode = null;
        for(Appendix appendix:appendice) {
            if (appendix.type == Appendix.TYPE_DATA_PACKAGE) {
                dataPackage = (DataPackage)appendix;
            } else if (appendix.type == Appendix.TYPE_RESPONSE_CODE) {
                responseCode = (ResponseCode)appendix;
            }
        }
        
        if (responseCode != null) {
            if (responseCode.getResponseCode() == ResponseCode.MAP_TILE_OK && dataPackage != null) {
                
                ParserUtil parseUtil = dataPackage.getData();
                byte[] data = parseUtil.getData();
                if (data != null && data.length > 0 && iTileDownload != null) {
                    int ret = iTileDownload.fillMapTile(tileDownloads, rid, data, parseUtil.getStart());
                    if (ret != 0) {
                        stop();
                    }
                } else {
                    stop();
                }
            } else if (responseCode.getResponseCode() == ResponseCode.MAP_TILE_NEW_REVISION) {
                iTileDownload.upgradeRegion(rid);
                stop();
            } else {
                stop();
            }
        }
    }    
}
