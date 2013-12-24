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
import com.tigerknows.map.CityInfo;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.Utility;

public final class BuslineQuery extends BaseQuery {

    private int startPos = 0;
    
    private boolean isTurnPage;
    
    private String keyword;
 
    private boolean isReturnTotal = true;
    
    private BuslineModel buslineModel;
    
    public String getKeyword() {
		return keyword;
	}


	public void setKeyword(String keyword) {
		this.keyword = keyword;
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
        
        addParameter(SERVER_PARAMETER_KEYWORD, keyword);
        addParameter(SERVER_PARAMETER_INDEX, String.valueOf(startPos));
        if (!hasParameter(SERVER_PARAMETER_SIZE)) {
            addParameter(SERVER_PARAMETER_SIZE, String.valueOf(TKConfig.getPageSize()));
        }

        if (isReturnTotal) {
            addParameter("w", "1");
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
