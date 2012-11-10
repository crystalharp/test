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
import com.tigerknows.maps.MapEngine.RegionMetaVersion;
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

    public interface FillMapTile {
        public void fillMapTile(List<TileDownload> tileInfos, int rid, byte[] data, boolean downloadRegionMeta);
    }
    
    private List<TileDownload> tileDownloads;
    private int rid;

    private FillMapTile fillMapTile;
    
    private boolean isTranslated;
    
    private MapEngine mapEngine;
    
    private HttpUtils.TKHttpClient.RealTimeRecive mRealTimeRecive = new RealTimeRecive() {
        
        @Override
        public void reciveData(byte[] data) {
            if (appendice.isEmpty() || appendice.size() <= 0) {
               translate(data);
               isTranslated = true;
            } else {
                if (data != null && data.length > 0 && fillMapTile != null) {
                    fillMapTile.fillMapTile(tileDownloads, rid, data, false);
                }
            }
        }
    };
    
    public MapTileDataDownload(Context context, MapEngine mapEngine) {
        super(context, API_TYPE_MAP_TILE_DOWNLOAD, VERSION);
        this.mapEngine = mapEngine;
        this.isTranslatePart = true;
    }
    
    public void setFillMapTile(FillMapTile fillMapTile) {
        this.fillMapTile = fillMapTile;
    }
    
    public void setup(List<TileDownload> tileDownloads, int rid) {
        this.tileDownloads = tileDownloads;
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
        isTranslated = false;
        super.query();
        long currentTime = System.currentTimeMillis();
        if (statusCode != STATUS_CODE_DATA_EMPTY && statusCode > STATUS_CODE_NONE && statusCode < STATUS_CODE_DATA_EMPTY) {
            httpClient = null;
            if (lastTime > 0 && currentTime - lastTime > KEEP_ALIVE_TIME) {
                statusCode = STATUS_CODE_DATA_EMPTY;
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
        String version = "";
        RegionMetaVersion dataVersion = mapEngine.getRegionVersion(rid);
        if (null != dataVersion) {
            version= dataVersion.toString();
        }
        requestParameters.add(new BasicNameValuePair("vd", version));
        int count = 0;
        for(TileDownload tileDownload:tileDownloads) {
            if (tileDownload.getRid() == rid && tileDownload.getLength() > 0) {
                count++;
                requestParameters.add(new BasicNameValuePair("off", String.valueOf(tileDownload.getOffset())));
                requestParameters.add(new BasicNameValuePair("len", String.valueOf(tileDownload.getLength())));
            }
        }
        if (count <= 0) {
            throw APIException.wrapToMissingRequestParameterException("off,len");
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
        
        httpClient.setRealTimeRecive(mRealTimeRecive);
        httpClient.setProgressUpdate(progressUpdate);
    }

    @Override
    protected void translateResponseV12(ParserUtil util) throws IOException {
        super.translateResponseV12(util);
        if (isTranslated) {
            return;
        }
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
//                datas = parseUtil.getLeaveData(parseUtil.getStart());
                int length = parseUtil.availableDataleng();
                byte[] datas = new byte[length];
                System.arraycopy(parseUtil.getData(), parseUtil.getStart(), datas, 0, length);
                if (datas != null && datas.length > 0 && fillMapTile != null) {
                    fillMapTile.fillMapTile(tileDownloads, rid, datas, false);
                }
            } else if (responseCode.getResponseCode() == ResponseCode.MAP_TILE_NEW_REVISION) {
                fillMapTile.fillMapTile(tileDownloads, rid, null, true);
            }
        }
    }    
}
