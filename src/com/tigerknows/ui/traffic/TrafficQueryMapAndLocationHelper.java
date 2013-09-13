package com.tigerknows.ui.traffic;

import android.text.TextUtils;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.POI;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[控制显示地图中心及查询地图中心]]
 * @author linqingzu
 *
 */
public class TrafficQueryMapAndLocationHelper {

	private final int ZOOM_LEVEL_QUANGUO = 7;
	
	private TrafficQueryFragment mQueryFragment;
	
	private static final String TAG = "TrafficQueryMapCenterHelper";
	
	public TrafficQueryMapAndLocationHelper(TrafficQueryFragment queryFragment) {
		super();
		this.mQueryFragment = queryFragment;
	}

	/**
	 * 记录交通首页显示的地图中心点
	 * 改变的三个点:
	 * 1. 交通查询页面onResume()时
	 * 2. 从全屏地图状态返回时
	 * 3. 从地图选点状态返回时
	 */
	private CityInfo mMapCityInfo = new CityInfo();
	
	/**
	 * 到这里去的POI的所在城市
	 */
	public CityInfo mTargetCityInfo = null;

	public CityInfo getQueryCityInfo() {
	    if (mTargetCityInfo != null) {
	        return mTargetCityInfo;
	    } else {
            return Globals.getCurrentCityInfo();
	    }
	}
	
	/**
	 * 检查定位城市与查询城市是否一致
	 * @return
	 */
	public boolean isMyLocationLocateCurrentCity() {
    	int currentCityId = MapEngine.CITY_ID_INVALID;
    	int myPositionCityId = MapEngine.CITY_ID_INVALID - 1;
    	
    	if (Globals.g_My_Location_City_Info != null) {
    		myPositionCityId = Globals.g_My_Location_City_Info.getId();
    	}
    	
    	if (mTargetCityInfo != null) {
    	    currentCityId = mTargetCityInfo.getId();
        } else if (Globals.getCurrentCityInfo() != null) {
    		currentCityId = Globals.getCurrentCityInfo(false).getId();
    	}
    	
    	LogWrapper.d(TAG, "currentCityId: " + currentCityId + "myPositionCityId: " + myPositionCityId);
    	
    	return currentCityId == myPositionCityId;
	}
	
	/**
	 * 获取当前位置的POI
	 * @return
	 */
	public POI getMyLocation() {
    	
    	POI myLocationPOI = null;
    	Position myLocationPosition = null;
        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        if (myLocationCityInfo != null) {
            myLocationPosition = myLocationCityInfo.getPosition();
            if (Util.inChina(myLocationPosition)) {
                myLocationPOI = new POI();
                myLocationPOI.setName(mQueryFragment.mContext.getString(R.string.my_location));
                myLocationPOI.setPosition(myLocationPosition);
            }
        }
    	
    	return myLocationPOI;
    }
    
    /**
     * 设置地图的中心
     * 
     * 定位城市   当前城市或到这里去的POI所在城市   上一次地图城市   地图中心
     * 1   1   1   我的当前位置
     * 1   2   1   当前城市中心
     * 1   2   2   上一次地图中心
     * 0   1   1   上一次地图中心
     * 0   2   1   当前城市中心
     */
    public void resetMapCenter() {
    	
        mQueryFragment.mSphinx.resetLoactionButtonState();
        CityInfo currentCityInfo = Globals.getCurrentCityInfo();
    	CityInfo locationCityInfo = Globals.g_My_Location_City_Info;
    	CityInfo lastCityInfo = mMapCityInfo;
    	CityInfo targetCityInfo = mTargetCityInfo;
    	
    	int locationId = MapEngine.CITY_ID_INVALID;
        if (locationCityInfo != null) {
            locationId = locationCityInfo.getId();
        }
    	int currentId = currentCityInfo.getId();
    	int lastId = lastCityInfo.getId();

        int queryId;
        CityInfo queryCityInfo;
        if (targetCityInfo != null) {
            queryCityInfo = targetCityInfo;
            queryId = targetCityInfo.getId();
        } else {
            queryCityInfo = currentCityInfo;
            queryId = currentId;
        }
    	
    	CityInfo result = null;
        if (locationCityInfo != null) {
            if (locationId == queryId) {
                result = locationCityInfo;
            }
        }
        
        if (result == null) {
            if (queryId != lastId) {
                result = queryCityInfo;
            } else {
                result = lastCityInfo;
            }
        }
    	
        if (result.getId() == lastCityInfo.getId()) {
            mQueryFragment.mSphinx.getMapView().setZoomLevel(result.getLevel());
            mQueryFragment.mSphinx.getMapView().panToPosition(result.getPosition());
        } else {
            mQueryFragment.mSphinx.getMapView().centerOnPosition(result.getPosition(), result.getLevel());
        }
        
        mMapCityInfo = result.clone();
    }
    
    /**
     * 地图移动时根据当前地图中心点设置当前城市名
     * @param newCenter
     */
    public void onMapCenterChanged(CityInfo cityInfo) {
    	
    	int zoomLevel = (int)mQueryFragment.mSphinx.getMapView().getZoomLevel();
    	
    	/*
    	 * 显示"全国"
    	 */
    	if (zoomLevel <= ZOOM_LEVEL_QUANGUO) {
    		mQueryFragment.setCurrentCity(mQueryFragment.mContext.getString(R.string.quanguo));
    	} else {
        	if (cityInfo != null && !TextUtils.isEmpty(cityInfo.getCName())) {
        		mQueryFragment.setCurrentCity(cityInfo.getCName());	
        	}
        	else {
        		mQueryFragment.setCurrentCity(mQueryFragment.mSphinx.getString(R.string.location_unknown));
        	}
    	}

    }
	
	/**
	 * 返回交通首页时, 若当前地图中心点的城市 与 之前离开交通首页时的城市 不同
	 * 则将地图移动回 之前离开交通首页时地图的中心点及缩放级别
	 */
	public void checkMapCenterInCity() {
		int curCityId = mQueryFragment.mSphinx.getMapView().getCenterCityId();
    	if (mMapCityInfo.getId() != curCityId) {
    		mQueryFragment.mSphinx.getMapView().centerOnPosition(mMapCityInfo.getPosition(), mMapCityInfo.getLevel());
    	} else {
    		mMapCityInfo.setPosition(mQueryFragment.mSphinx.getMapView().getCenterPosition());
    		mMapCityInfo.setLevel((int)mQueryFragment.mSphinx.getMapView().getZoomLevel());
    	}
    	
    	mQueryFragment.mSphinx.resetLoactionButtonState();
	}
}
