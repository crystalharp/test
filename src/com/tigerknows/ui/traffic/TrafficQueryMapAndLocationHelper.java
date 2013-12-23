package com.tigerknows.ui.traffic;

import android.text.TextUtils;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.android.location.Position;
import com.tigerknows.map.CityInfo;
import com.tigerknows.map.MapEngine;
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
                myLocationPOI.setName(mQueryFragment.getString(R.string.my_location));
                myLocationPOI.setPosition(myLocationPosition);
            }
        }
    	
    	return myLocationPOI;
    }
}
