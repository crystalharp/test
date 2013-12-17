/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.location.Position;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapView;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.POI;
import com.tigerknows.ui.poi.InputSearchFragment;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author Peng Wenyue
 */
@SuppressLint("ValidFragment")
public class HomeFragment extends BaseFragment implements View.OnClickListener {

    public HomeFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    static final String TAG = "HomeFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mBottomFrament = new HomeBottomFragment(mSphinx);
        mBottomFrament.onCreate(null);
        mBottomFrament.mRootView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mBottomFramentHeight = mBottomFrament.mRootView.getMeasuredHeight();
        
        mActionTag = ActionLog.Home;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()" + mActionTag);

        mRootView = mLayoutInflater.inflate(R.layout.home, container, false);

        findViews();
        setListener();

        return mRootView;
    }

    protected void setListener() {
        
    }
    
    Runnable mSetMapViewPadding = new Runnable() {
        
        @Override
        public void run() {
            MapView mapView = mSphinx.getMapView();
            Rect rect = mapView.getPadding();
            rect.bottom = mBottomFrament.getBottom() - mBottomFrament.getTop();
            mapView.refreshMap();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        
        mTitleBtn.setBackgroundResource(R.drawable.textfield);
        mTitleBtn.setOnClickListener(this);
        mTitleBtn.setHint(R.string.find_poi);
        
        mLeftBtn.setBackgroundDrawable(null);
        
        mTitleFragment.mRootView.setBackgroundDrawable(null);
        
        MapView mapView = mSphinx.getMapView();
        Rect rect = mapView.getPadding();
        rect.bottom = mBottomFramentHeight;
        mapView.setStopRefreshMyLocation(false);
    }

    public DataQuery getDataQuery(String keyWord) {
        POI requestPOI = mSphinx.getPOI();
        DataQuery poiQuery = new DataQuery(mContext);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        Position position = requestPOI.getPosition();
        if (position != null) {
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
        }
        
        if (keyWord != null) {
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, keyWord);
        }

        return poiQuery;
    }

    @Override
    public void onClick(View v) {
        mSphinx.getPOIQueryFragment().reset();
        mSphinx.getPOIQueryFragment().setMode(InputSearchFragment.MODE_POI);
        mSphinx.showView(R.id.view_poi_input_search);
    }
}
