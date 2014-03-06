/*
 * @(#)BusTransfer1.java  上午10:27:54 2007-11-19 2007
 *
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd.
 * All rights reserved.
 *
 */

package com.tigerknows.model;

import android.content.Context;

import java.io.IOException;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.Utility;

public final class BuslineQuery extends BaseQuery {
    
    // bty  整数  false   查询类别
    // 1: 搜索附近的公交站的列表
    // 2: 搜索附近的线路列表
    // 3: 按名字搜索公交站列表
    // 注: 此功能需要公共参数lx和ly，并且忽略参数c和k
    public static final String SERVER_PARAMETER_BUS_TYPE = "bty";

    public static final String BUS_TYPE_STATION = "1";
    public static final String BUS_TYPE_LINE = "2";
    public static final String BUS_TYPE_STATION_FOR_POSITION = "3";
    public static final String BUS_TYPE_LINE_FOR_POSITION = "4";

    private int startPos = 0;
    
    private boolean isTurnPage;
    
    private String keyword;
    
    private String type;
 
    private boolean isReturnTotal = true;
    
    private Position position;
    
    private BuslineModel buslineModel;
    
    public String getKeyword() {
		return keyword;
	}

    public String getType() {
        return type;
    }

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	
	public Position getPosition() {
	    return this.position;
	}
	
	public void setPosition(Position position) {
	    this.position = position;
	}
	
	public void setType(String type) {
	    this.type = type;
	}
	
	public boolean isTurnPage() {
		return isTurnPage;
	}

	public void setTurnPage(boolean isTurnPage) {
		this.isTurnPage = isTurnPage;
	}

	public BuslineModel getBuslineModel() {
        return buslineModel;
    }
    
    public void setBuslineModel(BuslineModel buslineModel) {
        this.buslineModel = buslineModel;
    }

    public BuslineQuery(Context context) {
        super(context, API_TYPE_BUSLINE_QUERY);
    }
    
    public void setup(String keyword, int startPos, boolean isReturnTotal, int targetViewId, String tipText) {

    	this.keyword = keyword;
        this.startPos = startPos;
        this.isTurnPage = (this.startPos > 0);
        this.needReconntection = false;
        this.isReturnTotal = isReturnTotal;        
        this.targetViewId = targetViewId;
        this.tipText = tipText;
    }
    
    @Override
    protected void checkRequestParameters() throws APIException {
    }
    
    @Override
    protected void addCommonParameters() {
        super.addCommonParameters();
        
        if (keyword != null) {
            addParameter(SERVER_PARAMETER_KEYWORD, keyword);
        }
        
        if (type != null) {
            addParameter(SERVER_PARAMETER_BUS_TYPE, type);
        }
        addParameter(SERVER_PARAMETER_INDEX, String.valueOf(startPos));
        if (!hasParameter(SERVER_PARAMETER_SIZE)) {
            addParameter(SERVER_PARAMETER_SIZE, String.valueOf(TKConfig.getPageSize()));
        }

        if (isReturnTotal) {
            addParameter("w", "1");
        }
        
        if (position != null) {
            addParameter(SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
            addParameter(SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
        }
    }

    @Override
    protected void createHttpClient() {
        super.createHttpClient();
        String url = String.format(TKConfig.getQueryUrl(), TKConfig.getQueryHost());
        httpClient.setURL(url);
    }

    @Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);
        buslineModel = new BuslineModel(responseXMap);
        try {
        if (TKConfig.SaveResponseData) {
            String path = TKConfig.getTestDataPath() + "buslinemodel";
            Utility.writeFile(path, ByteUtil.xobjectToByte(responseXMap), true);
        }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        LogWrapper.d("eric", "BuslineQuery query response:" + buslineModel);
    }
}
