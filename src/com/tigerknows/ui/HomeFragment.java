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
import com.tigerknows.util.Utility;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * @author Peng Wenyue
 */
public class HomeFragment extends BaseFragment implements View.OnClickListener {

    public HomeFragment(Sphinx sphinx) {
        super(sphinx);
    }

    static final String TAG = "HomeFragment";
    
    private Drawable mSearchDrawable;
    
    private int mTitleBtnPaddingLeft;
    
    private int mTitleBtnPaddingRight;
    
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
        
        mBottomFrament = mSphinx.getHomeBottomFragment();

        findViews();
        setListener();
        
        mTitleBtnPaddingLeft = Utility.dip2px(mSphinx, 16);
        mTitleBtnPaddingRight = Utility.dip2px(mSphinx, 8);

        return mRootView;
    }

    @Override
    protected void setListener() {
        // 不拦截触摸事件
    }

    @Override
    public void onResume() {
        super.onResume();
        reset();
        mTitleBtn.setBackgroundResource(R.drawable.edt_home);
        mTitleBtn.setOnClickListener(this);
        mTitleBtn.setHint(R.string.find_poi);
        mTitleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        RelativeLayout.LayoutParams layoutParams = (android.widget.RelativeLayout.LayoutParams) mTitleBtn.getLayoutParams();
        layoutParams.leftMargin = mTitleBtnPaddingRight;
        layoutParams.topMargin = mTitleBtnPaddingLeft;
        layoutParams.rightMargin = mTitleBtnPaddingRight;
        layoutParams.bottomMargin = mTitleBtnPaddingLeft;
        
        if (mSearchDrawable == null) {
            mSearchDrawable = mSphinx.getResources().getDrawable(R.drawable.ic_home_search);
        }
        
        mSearchDrawable.setBounds(0, 0, mSearchDrawable.getIntrinsicWidth(), mSearchDrawable.getIntrinsicHeight());
        mTitleBtn.setCompoundDrawables(mSearchDrawable, null, null, null);
        mTitleBtn.setCompoundDrawablePadding(mTitleBtnPaddingRight);
        mTitleBtn.setPadding(mTitleBtnPaddingLeft, mTitleBtnPaddingRight, mTitleBtnPaddingLeft, mTitleBtnPaddingRight);
        layoutParams.height = Utility.dip2px(mSphinx, 48);

        mLeftBtn.setVisibility(View.GONE);
        
        mTitleFragment.mRootView.setBackgroundDrawable(null);
        
        mSphinx.clearMap();
        mBottomFrament = mSphinx.getHomeBottomFragment();
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
        if (infoWindowFragment.getOwerFragmentId() == getId() && itemizedOverlay != null) {
            mOverlayItem = itemizedOverlay.getItemByFocused();
        } else {
            mOverlayItem = null;
        }
        super.onPause();
    }
    
    public void reset() {
        mItemizedOverlay = null;
        mOverlayItem = null;
    }

    @Override
    public void onClick(View v) {
    	mActionLog.addAction(mActionTag + ActionLog.TitleCenterButton);
        mBottomFrament = mSphinx.getHomeBottomFragment();
        mSphinx.getInputSearchFragment().setData(mSphinx.buildDataQuery(),
        		null,
                InputSearchFragment.MODE_POI);
        mSphinx.showView(R.id.view_poi_input_search);
    }
}
