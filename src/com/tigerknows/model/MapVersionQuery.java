/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;

import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.response.Appendix;
import com.tigerknows.model.response.MapVersionCake;
import com.tigerknows.model.response.ResponseCode;
import com.tigerknows.util.ParserUtil;

/**
 * @author Peng Wenyue
 */
public class MapVersionQuery extends BaseQuery {
    
    public static final String VERSION = "2";
    
    private List<Integer> regionIdList;
    private HashMap<Integer, RegionDataInfo> regionDataInfos = null;

    public MapVersionQuery(Context context) {
        super(context, API_TYPE_MAP_VERSION_QUERY, VERSION);
    }
    
    public void setup(List<Integer> regionIdList) {
        this.regionIdList = regionIdList;
    }
    
    public HashMap<Integer, RegionDataInfo> getRegionVersion() {
        return regionDataInfos;
    }
    
    /**
          参数名称 类型  必选    意义o
    at  string  true    api type，固定为rq
    v   integer true    服务版本,固定为1
    rid intArray    true    区域id 
    release:http://download.tigerknows.net/quantum/string?at=rq&v=1&rid=109&rid=110
    test:http://192.168.11.119:8080/quantum/string?at=rq&v=2&rid=109
    test:http://spiderman.tigerknows.net:80/quantum/string?at=rq&v=1&rid=109&rid=110
    */
    @Override
    public synchronized void query() {
        isStop = false;
        super.query();
    }
    
    @Override
    protected void addMyLocationParameters() {
        
    }

    @Override
    protected void makeRequestParameters() throws APIException {
        super.makeRequestParameters();
        requestParameters.add(new BasicNameValuePair("vs", TKConfig.getClientSoftVersion()));
        for (Integer regionId : regionIdList) {
            requestParameters.add(new BasicNameValuePair("rid", String.valueOf(regionId)));
        }
    }

    @Override
    protected void createHttpClient() {
        super.createHttpClient();
        httpClient.setIsEncrypt(false);
        String url = String.format(TKConfig.getDownloadMapUrl(), TKConfig.getDownloadHost());
        httpClient.setURL(url);
    }

    @Override
    protected void translateResponseV12(ParserUtil util) throws IOException {
        super.translateResponseV12(util);
        MapVersionCake mapVersionCake = null;
        ResponseCode responseCode = null;
        for (Appendix appendix : appendice) {
            if (appendix.type == Appendix.TYPE_MAP_VERSION) {
                mapVersionCake = (MapVersionCake) appendix;
            } else if (appendix.type == Appendix.TYPE_RESPONSE_CODE) {
                responseCode = (ResponseCode) appendix;
            }
        }
        if (responseCode != null && 
                mapVersionCake != null &&
                responseCode.getResponseCode() == ResponseCode.MAP_REGION_VERSION_OK) {
            regionDataInfos = mapVersionCake.getRegionVersionMap();
        }
    }

    public static class RegionDataInfo {
        private int mRegionId;
        private String mRegionVersion;
        private int mTotalSize;
        private String mCheckSum;
        
        public RegionDataInfo(final String regionVersion, final int totalSize) {
            this.mRegionVersion = regionVersion;
            this.mTotalSize = totalSize;
        }

        public int getRegionId() {
            return mRegionId;
        }
        public void setRegionId(int regionId) {
            this.mRegionId = regionId;
        }

        public String getRegionVersion() {
            return mRegionVersion;
        }
        public void setRegionVersion(String regionVersion) {
            this.mRegionVersion = regionVersion;
        }

        public int getTotalSize() {
            return mTotalSize;
        }
        public void setTotalSize(int totalSize) {
            this.mTotalSize = totalSize;
        }

        public String getCheckSum() {
            return mCheckSum;
        }
        public void setCheckSum(String checkSum) {
            this.mCheckSum = checkSum;
        }
        
        @Override
        public String toString() {
            return "mRegionVersion:" + mRegionVersion + " mTotalSize:" + mTotalSize;
        }
    }
}
