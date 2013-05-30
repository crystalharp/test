package com.tigerknows.ui.traffic;

import android.text.TextUtils;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.POI;
import com.tigerknows.ui.traffic.TrafficViewSTT.State;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[控制显示地图中心及查询地图中心]]
 * @author linqingzu
 *
 */
public class TrafficQueryMapAndLocationHelper {

	private final int QUAN_GUO_ZOOM = 7;
	
    public boolean firstEnterNormalState = true;
	
	private TrafficQueryFragment mQueryFragment;
	
	private static final String TAG = "TrafficQueryMapCenterHelper";
	
	public TrafficQueryMapAndLocationHelper(TrafficQueryFragment queryFragment) {
		super();
		this.mQueryFragment = queryFragment;
	}

	/*
	 * 记录交通首页显示的地图中心点
	 * 改变的三个点:
	 * 1. 该页面首次被创建时
	 * 2. 切换城市时
	 * 3. 从全屏地图返回时(若返回时与离开时的城市不变)
	 */
	private CityInfo mCityInfo = new CityInfo();
	
	/*
	 * 记录交通输入页查询的地图中心点
	 * 
	 * 改变的点:
	 * 1. 该页面首次被创建时
	 * 2. 全屏地图时移动地图
	 * 3. 选点地图时移动地图
	 * 4. 从全屏地图返回时(若返回时与离开时的城市不变)
	 */
	private CityInfo mQueryCityInfo = new CityInfo();

	public CityInfo getQueryCityInfo() {
        return Globals.getCurrentCityInfo();
        //产品确定查询城市是系统设置城市，而不是地图所在城市。
//      return mQueryCityInfo;
	}
	
	public boolean isMyLocationLocateCurrentCity() {
    	int currentCityId = MapEngine.CITY_ID_INVALID;
    	int myPositionCityId = MapEngine.CITY_ID_INVALID - 1;
    	
    	if (Globals.g_My_Location_City_Info != null) {
    		myPositionCityId = Globals.g_My_Location_City_Info.getId();
    	}
    	
    	if (Globals.getCurrentCityInfo() != null) {
    		currentCityId = Globals.getCurrentCityInfo(false).getId();
    	}
    	
    	LogWrapper.d(TAG, "currentCityId: " + currentCityId + "myPositionCityId: " + myPositionCityId);
    	
    	return currentCityId == myPositionCityId;
	}
	
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
     * 得到当前地图中心点城市信息, 并设置当前地图中心点为交通首页显示中心点及交通查询中心点
     */
    public void getCurrentMapInfo() {
    	resetCurrentMapInfo(mQueryFragment.mSphinx.getMapView().getCenterPosition(), 
    			(int)mQueryFragment.mSphinx.getMapView().getZoomLevel());
    }

	public void resetCurrentMapInfo(Position position, int zoomLevel) {
    	if (mQueryFragment.mSphinx != null && mQueryFragment.mSphinx.getMapEngine() != null && Util.inChina(position)) {
        	mCityInfo.setPosition(position);
        	mCityInfo.setId(mQueryFragment.mSphinx.getMapEngine().getCityId(position));
        	mCityInfo.setLevel(zoomLevel);
        	
        	CityInfo cityInfo = mQueryFragment.mSphinx.getMapEngine().getCityInfo(mCityInfo.getId());
        	if (cityInfo != null)
        		mCityInfo.setCName(cityInfo.getCName());
        	
        	mQueryCityInfo = mCityInfo.clone();
        	mQueryFragment.setCurrentCity(mCityInfo.getCName());
        }
    }
    
    /**
     * 第一次进入交通首页时, 若得到了定位点, 且当前所选城市为定位城市, 将地图移动到定位点
     */
    public void centerOnMyLocation() {
    	LogWrapper.d(TAG, "centerOnMyLocation");
    	
    	if ((firstEnterNormalState == true) && isMyLocationLocateCurrentCity()
    			&& (mQueryFragment.mStateTransitionTable.getCurrentState() == TrafficViewSTT.State.Normal)) {
    		firstEnterNormalState = false;
    		Position myLocationPosition = null;
            CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
            if (myLocationCityInfo != null) {
                myLocationPosition = myLocationCityInfo.getPosition();
                mQueryFragment.mSphinx.getMapView().zoomTo(TKConfig.ZOOM_LEVEL_LOCATION, myLocationPosition);
                resetCurrentMapInfo(myLocationPosition, TKConfig.ZOOM_LEVEL_LOCATION);
            }
    	}
    }
    
    /**
     * 地图移动时根据当前地图中心点设置当前城市名, 并设置交通查询中心点
     * @param newCenter
     */
    public void onMapCenterChanged(CityInfo cityInfo) {
    	
    	Position newCenter = mQueryFragment.mSphinx.getMapView().getCenterPosition();
    	int zoomLevel = (int)mQueryFragment.mSphinx.getMapView().getZoomLevel();
    	
    	/*
    	 * 显示"全国"
    	 */
    	if (zoomLevel > 0 && zoomLevel <= QUAN_GUO_ZOOM) {
    		mQueryFragment.setCurrentCity(mQueryFragment.mContext.getString(R.string.quanguo));
    		LogWrapper.d("eirc", "TrafficQueryView  onMapCenterChanged() zoom="+zoomLevel);
    	} else {
        	if (cityInfo != null && !TextUtils.isEmpty(cityInfo.getCName())) {
        		mQueryFragment.setCurrentCity(cityInfo.getCName());	
        		LogWrapper.d("eirc", " currentCity="+cityInfo.getCName());
        	}
    	}
    	
    	State curState = mQueryFragment.mStateTransitionTable.getCurrentState();
    	if (curState == TrafficViewSTT.State.Normal
    			|| curState == TrafficViewSTT.State.Map 
    			|| curState == TrafficViewSTT.State.SelectPoint) {
    		mQueryCityInfo.setId(mQueryFragment.mSphinx.getMapView().getCenterCityId());
    		mQueryCityInfo.setPosition(newCenter);
    		mQueryCityInfo.setLevel(zoomLevel);
    	}

    }
    
    /**
     * 得到某城市中心坐标点
     * Util Method
     * @param cityInfo
     * @return
     */
	private Position getCityCenterPosition(CityInfo cityInfo) {
    	return cityInfo.getPosition();
    }
    
    private boolean isEqualsToMapCenter(CityInfo cityInfo) {
    	Position center = getCityCenterPosition(cityInfo);
    	
    	if (mQueryFragment.mSphinx != null && mQueryFragment.mSphinx.getMapEngine() != null 
        		&& Util.inChina(center)
        		&& ((center != null && !center.equals(mQueryFragment.mSphinx.getMapView().getCenterPosition()))
        		|| (int)mQueryFragment.mSphinx.getMapView().getZoomLevel() != cityInfo.getLevel())) {
    		return false;
    	}
    	
    	return true;
    }
    
    public void showNormalStateMap() {
		if (!isEqualsToMapCenter(mCityInfo)) {
			mQueryFragment.mSphinx.getMapView().zoomTo(mCityInfo.getLevel(), getCityCenterPosition(mCityInfo));
			mQueryFragment.mSphinx.resetLoactionButtonState();
    	}
		
		mQueryCityInfo = mCityInfo.clone();
		mQueryFragment.mSphinx.resetLoactionButtonState();
	}
	
	/**
	 * 返回交通首页时, 若当前地图中心点的城市 与 之前离开交通首页时的城市 不同
	 * 则将地图移动回 之前离开交通首页时地图的中心点及缩放级别
	 */
	public void resetNormalStateMap() {
		int curCityId = mQueryFragment.mSphinx.getMapView().getCenterCityId();
    	if (mCityInfo.getId() != curCityId) {
    		LogWrapper.d(TAG, "Return from city " + curCityId + " to " + mCityInfo.getId());
    		mQueryFragment.mSphinx.getMapView().zoomTo(mCityInfo.getLevel(), getCityCenterPosition(mCityInfo));
    		mQueryFragment.mSphinx.resetLoactionButtonState();
    	} else {
    		LogWrapper.d(TAG, "Stay same city");
    		mCityInfo.setPosition(mQueryFragment.mSphinx.getMapView().getCenterPosition());
    		mCityInfo.setLevel((int)mQueryFragment.mSphinx.getMapView().getZoomLevel());
    	}
    	
    	mQueryCityInfo = mCityInfo.clone();
    	mQueryFragment.mSphinx.resetLoactionButtonState();
	}
	
	public void resetMapStateMap() {
	    //FIXME：这个是什么时候会调用到？
		if (!isEqualsToMapCenter(mQueryCityInfo)) {
			mQueryFragment.mSphinx.getMapView().zoomTo(mQueryCityInfo.getLevel(), getCityCenterPosition(mQueryCityInfo));
			mQueryFragment.mSphinx.resetLoactionButtonState();
    	}
	}
}
