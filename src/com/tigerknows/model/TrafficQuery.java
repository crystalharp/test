/*
 * @(#)BusTransfer1.java  上午10:27:54 2007-11-19 2007
 *
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd.
 * All rights reserved.
 *
 */

package com.tigerknows.model;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.map.CityInfo;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.Step;

/**
 * 
 * @author pengjunjie
 * @version
 * @see Change log:
 */
public final class TrafficQuery extends BaseQuery {

    // 1:公交换乘; 
    public static final int QUERY_TYPE_TRANSFER = Step.TYPE_TRANSFER;
    // 2:驾车导航;
    public static final int QUERY_TYPE_DRIVE = Step.TYPE_DRIVE;
    // 3:步行导航
    public static final int QUERY_TYPE_WALK = Step.TYPE_WALK;
    
    // 驾车类型，最少时间，最短距离，避免高速
    public static final int DRIVE_LEAST_TIME = 1;
    public static final int DRIVE_SHORTEST = 2;
    public static final int DRIVE_NO_HIGHWAY = 3;

    // bais 整数   false,只在驾车导航时提交     1:最少时间; 2:最短距离; 3:避免高速
    public static final String SERVER_PARAMETER_BIAS = "bias";
    
    /*
     * 每次默认的最大方案请求数目.
     */
    private static final int PAGE_SIZE = 6;

    private POI startPOI;

    private POI endPOI;

    private TrafficModel trafficModel;

    private int queryType;

    private boolean isPOIModified = false;
    
    public POI getEnd() {
        return this.endPOI;
    }

    public POI getStart() {
        return this.startPOI;
    }
    
    public int getQueryType() {
        return this.queryType;
    }
    
    public boolean isPOIModified() {
		return isPOIModified;
	}

	public TrafficModel getTrafficModel() {
        return trafficModel;
    }
    
    public void setTrafficModel(TrafficModel trafficModel) {
        this.trafficModel = trafficModel;
    }

    public TrafficQuery(Context context) {
        super(context, API_TYPE_TRAFFIC_QUERY, VERSION);
    }
    
    public void setup(POI startPOI, POI endPOI, int queryType, int targetViewId, String tipText) {
        if (!startPOI.equals(this.startPOI) || !endPOI.equals(this.endPOI) || queryType != this.queryType) {
            trafficModel = null;
        }        
        this.queryType = queryType;
        this.startPOI = startPOI;
        this.endPOI = endPOI;
        
        this.isPOIModified = false;
        this.targetViewId = targetViewId;
        this.tipText = tipText;
    }

    @Override
    protected void addCommonParameters() {
        super.addCommonParameters();

        if (Util.inChina(startPOI.getPosition())) {
            addParameter("sx", String.valueOf(startPOI.getPosition().getLon()));
            addParameter("sy", String.valueOf(startPOI.getPosition().getLat()));
        }
        String startName = startPOI.getName();
        if (!TextUtils.isEmpty(startName)) {
            addParameter("sn", startName);
        }

        if (Util.inChina(endPOI.getPosition())) {
            addParameter("ex", String.valueOf(endPOI.getPosition().getLon()));
            addParameter("ey", String.valueOf(endPOI.getPosition().getLat()));
        }
        String endName = endPOI.getName();
        if (!TextUtils.isEmpty(endName)) {
            addParameter("en", endName);
        }
        
        String type = "0";
        if (QUERY_TYPE_TRANSFER == queryType) {
            type = "1";
        } else if (QUERY_TYPE_DRIVE == queryType) {
            type = "2";
        } else if (QUERY_TYPE_WALK == queryType) {
            type = "3";
        }

        addParameter("type", type);
        addParameter(SERVER_PARAMETER_INDEX, String.valueOf(0));
        addParameter(SERVER_PARAMETER_SIZE, String.valueOf(PAGE_SIZE));
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
        trafficModel = new TrafficModel(responseXMap, queryType);

        /*
         * 服务端并没有返回起终点的经纬度坐标, 
         * 所以这里取Step列表的第一个Step的第一个坐标点作为起点坐标, 
         * 取Step列表的最后一个Step的最后一个坐标点作为终点坐标.
         * 
         * 这样的代价是代码实在是太丑了...
         */
        if (trafficModel != null && trafficModel.getType() == TrafficModel.TYPE_PROJECT && startPOI.getPosition() == null) {
        	if (trafficModel.getPlanList() != null && trafficModel.getPlanList().size() > 0 
        			&& trafficModel.getPlanList().get(0).getStepList() != null && trafficModel.getPlanList().get(0).getStepList().size() > 0
        			&& trafficModel.getPlanList().get(0).getStepList().get(0).getPositionList() != null
        			&& trafficModel.getPlanList().get(0).getStepList().get(0).getPositionList().size() > 0) {
	        	Position start = trafficModel.getPlanList().get(0).getStepList().get(0).getPositionList().get(0);
	        	startPOI.setPosition(start);
        	}
        }
        if (trafficModel != null && trafficModel.getType() == TrafficModel.TYPE_PROJECT && endPOI.getPosition() == null) {
        	if (trafficModel.getPlanList() != null && trafficModel.getPlanList().size() > 0 
        			&& trafficModel.getPlanList().get(0).getStepList() != null && trafficModel.getPlanList().get(0).getStepList().size() > 0) {
        		List<Step> steps = trafficModel.getPlanList().get(0).getStepList();
        		if (steps != null && steps.size() > 0 && steps.get(steps.size()-1).getPositionList() != null 
        				&& steps.get(steps.size()-1).getPositionList().size() > 0) {
	            	List<Position> positions = steps.get(steps.size()-1).getPositionList();
	            	Position end = positions.get(positions.size()-1);
	            	endPOI.setPosition(end);
        		}
        	}
        }
        
        /*
         * 过滤公交换乘步骤中"小于50米的步行步骤"
         */
        if (trafficModel != null && trafficModel.getType() == TrafficModel.TYPE_PROJECT &&
        		trafficModel.getPlanList() != null && trafficModel.getPlanList().size() > 0) {
        	for (Plan plan : trafficModel.getPlanList()) {
        		if (filterTooSmallLengthStepFromProjectPlan(plan) > 0) {
        		    plan.resetData();
        		}
        	}
        }
        
        /*
         * 若服务端返回了起终点POI名称, 则将返回的POI名称赋给客户端.
         * 但是注意服务端并不一定会返回起终点POI名称.
         */
        if (!TextUtils.isEmpty(trafficModel.getStartName())) {
        	startPOI.setName(trafficModel.getStartName());
        	isPOIModified = true;
        }
        if (!TextUtils.isEmpty(trafficModel.getEndName())) {
        	endPOI.setName(trafficModel.getEndName());
            isPOIModified = true;
        }
        
        trafficModel.setStart(startPOI);
        trafficModel.setEnd(endPOI);
        
        /*
         * 如果服务端返回的起终点为同一个点, 则将出现全程为0米的情况
         * 此时客户端将服务端返回的数据置为"无结果"的情况
         */
        if (POI.isPositionEqual(startPOI, endPOI)) {
        	trafficModel.setType(TrafficModel.TYPE_EMPTY);
        }
        
        /*
         *  如果服务端返回的驾车或步行方案的全程为0米
         *  此时客户端将服务端返回的数据置为"无结果"的情况
         */
        if ((trafficModel.getType() == TrafficModel.TYPE_PROJECT) 
        		&& (trafficModel.getPlanList().get(0).getType() != Plan.Step.TYPE_TRANSFER)
        		&& (trafficModel.getPlanList().get(0).getLength() == 0)) {
        	trafficModel.setType(TrafficModel.TYPE_EMPTY);
        }
    }
    
    /**
	 * 过滤公交换乘Step中"小于50米的步行Step"
	 * 服务端可能返回"从**步行0米到**"
	 * 
	 * Notice: 若最后一个Step是步行Step, 不进行过滤
	 * 
	 * @param plan
	 */
    public static int filterTooSmallLengthStepFromProjectPlan(Plan plan) {
        int filter = 0;
		if(plan.getType() == TrafficQuery.QUERY_TYPE_TRANSFER) {
            List<Step> skipSteps = new ArrayList<Step>();

            Step step = null;
            for(int i = 1; i < plan.getStepList().size()-1; i++) {
            	step = plan.getStepList().get(i);
            	if(step.getType() == Step.TYPE_WALK && step.getDistance() <= Plan.TRANSFER_WALK_MIN_DISTANCE)
            		skipSteps.add(step);
            }
            
            filter = skipSteps.size();
            if (filter > 0) {
                for(Step s : skipSteps){
                    if(plan.getStepList().contains(s))
                        plan.getStepList().remove(s);
                }
            }
        }
		return filter;
	}
    
    //用来给方案序号转换数字和汉字
    public static String numToStr(Context mContext, int num) {
        String[] numTransTable = mContext.getResources().getStringArray(R.array.num_to_str);
        String[] baseTable = mContext.getResources().getStringArray(R.array.num_base_to_str);
    	int oldNum = num;
        
    	String result = "";
    	
    	//这个限制只是因为方案不会太多，如果过了100，可以在arrays.xml里增加num_base_to_str的值，然后把这个限制增加
    	if (num > 99) {
    		return String.valueOf(num);
    	}
    	
    	for (int i = 0; num != 0; i++) {
    	    result = numTransTable[num % 10] + baseTable[i] + result;
    	    num = num / 10;
    	}

    	//在10到19之间，十位数不需要写出来
    	if (oldNum >= 10 && oldNum < 20) {
    	    result = result.substring(1);
    	}
    	
    	return result;
    }

    @Override
    protected void checkRequestParameters() throws APIException {
    }

    @Override
    protected String getActionTag() {
        StringBuilder s = new StringBuilder();
        s.append(apiType);
        s.append('@');
        
        s.append(queryType);
        s.append('@');
        s.append('@');
        s.append('@');
        if (hasParameter(SERVER_PARAMETER_REQUSET_SOURCE_TYPE)) {
            s.append(getParameter(SERVER_PARAMETER_REQUSET_SOURCE_TYPE));
        }
        s.append('@');
        s.append('@');
        
        return s.toString();
    }
}
