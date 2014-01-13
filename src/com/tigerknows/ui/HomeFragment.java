/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapView;
import com.tigerknows.ui.poi.InputSearchFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author Peng Wenyue
 */
public class HomeFragment extends BaseFragment implements View.OnClickListener {

    public HomeFragment(Sphinx sphinx) {
        super(sphinx);
    }

    static final String TAG = "HomeFragment";
    
    private ItemizedOverlay mItemizedOverlay;
    
    private OverlayItem mOverlayItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.Home;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()" + mActionTag);

        mRootView = mLayoutInflater.inflate(R.layout.home, container, false);
        
        mBottomFragment = mSphinx.getHomeBottomFragment();

        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    protected void setListener() {
        // 不拦截触摸事件
        mSphinx.getTitleView().getChildAt(0).findViewById(R.id.title_btn).setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        mLeftBtn.setVisibility(View.GONE);
        
        mSphinx.clearMap();
        mBottomFragment = mSphinx.getHomeBottomFragment();
        mSphinx.replaceBottomUI(this);
        MapView mapView = mSphinx.getMapView();
        mapView.setStopRefreshMyLocation(false);
        
        mSphinx.getCenterTokenView().setVisibility(View.INVISIBLE);
        mSphinx.getMapToolsView().setVisibility(View.VISIBLE);
        mSphinx.getMapCleanBtn().setVisibility(View.INVISIBLE);
        mSphinx.getLocationView().setVisibility(View.VISIBLE);
        
        try {
            mSphinx.showInfoWindow(getId(), mOverlayItem);
            if (mItemizedOverlay != null) {
                mSphinx.getMapView().addOverlay(mItemizedOverlay);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onPause() {
        mItemizedOverlay = mSphinx.getMapView().getCurrentOverlay();
        InfoWindowFragment infoWindowFragment = mSphinx.getInfoWindowFragment();
        ItemizedOverlay itemizedOverlay = infoWindowFragment.getItemizedOverlay();
        if (mSphinx.getBottomFragment() == infoWindowFragment &&
                infoWindowFragment.getOwerFragmentId() == getId() &&
                itemizedOverlay != null) {
            mOverlayItem = itemizedOverlay.get(0);
        } else {
            mOverlayItem = null;
        }
        super.onPause();
    }
    
    public void reset() {
        if (mItemizedOverlay != null) {
            mSphinx.getMapView().deleteOverlaysByName(mItemizedOverlay.getName());
        }
        mItemizedOverlay = null;
        mOverlayItem = null;
    }

    @Override
    public void onClick(View v) {
    	mActionLog.addAction(mActionTag + ActionLog.TitleCenterButton);
        mBottomFragment = mSphinx.getHomeBottomFragment();
        mSphinx.getInputSearchFragment().setData(mSphinx.buildDataQuery(),
        		null,
                InputSearchFragment.MODE_POI);
        mSphinx.showView(R.id.view_poi_input_search);
    }
}
