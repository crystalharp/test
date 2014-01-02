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
    
    public int mTitleBtnPaddingLeft;
    
    public int mTitleBtnPaddingRight;

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
        layoutParams.height = Utility.dip2px(mSphinx, 40);

        mLeftBtn.setVisibility(View.GONE);
        mRightBtn.setVisibility(View.GONE);
        
        mTitleFragment.mRootView.setBackgroundDrawable(null);
        
        mSphinx.clearMap();
        MapView mapView = mSphinx.getMapView();
        mapView.setStopRefreshMyLocation(false);
        
        mSphinx.getCenterTokenView().setVisibility(View.INVISIBLE);
        mSphinx.getMapToolsView().setVisibility(View.VISIBLE);
        mSphinx.getMapCleanBtn().setVisibility(View.INVISIBLE);
        mSphinx.getLocationView().setVisibility(View.VISIBLE);
    }

    public DataQuery getDataQuery(String keyWord) {
        DataQuery poiQuery = new DataQuery(mContext);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");

        
        // 我的定位显示在屏幕可视区域，并且比例尺小于或等于1千米时，则请求中包含lx和ly且不包含cx和cy，否则请求参数中包含cx和cy
        MapView mapView = mSphinx.getMapView();
        Position position = mapView.getCenterPosition();
        float zoomLevle = mapView.getZoomLevel();
        if (mSphinx.positionInScreen(position)
                && zoomLevle >= 12) { // 12级别是1千米
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_CENTER_LONGITUDE, String.valueOf(position.getLon()));
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_CENTER_LATITUDE, String.valueOf(position.getLat()));
        }
        
        if (keyWord != null) {
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, keyWord);
        }

        return poiQuery;
    }

    @Override
    public void onClick(View v) {
        mBottomFrament = mSphinx.getHomeBottomFragment();
        mSphinx.getInputSearchFragment().setData(null,
                InputSearchFragment.MODE_POI);
        mSphinx.showView(R.id.view_poi_input_search);
    }
}
