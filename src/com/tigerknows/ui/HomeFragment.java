/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
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

    protected void setListener() {
        
    }

    @Override
    public void onResume() {
        super.onResume();
        
        mTitleBtn.setBackgroundResource(R.drawable.edt_home);
        mTitleBtn.setOnClickListener(this);
        mTitleBtn.setHint(R.string.find_poi);
        mTitleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        
        if (mSearchDrawable == null) {
            mSearchDrawable = mSphinx.getResources().getDrawable(R.drawable.ic_home_search);
        }
        
        mSearchDrawable.setBounds(0, 0, mSearchDrawable.getIntrinsicWidth(), mSearchDrawable.getIntrinsicHeight());
        mTitleBtn.setCompoundDrawables(mSearchDrawable, null, null, null);
        mTitleBtn.setCompoundDrawablePadding(mTitleBtnPaddingRight);
        mTitleBtn.setPadding(mTitleBtnPaddingLeft, mTitleBtnPaddingRight, mTitleBtnPaddingLeft, mTitleBtnPaddingRight);
        mTitleBtn.getLayoutParams().height = Utility.dip2px(mSphinx, 40);

        mLeftBtn.setVisibility(View.GONE);
        mRightBtn.setVisibility(View.GONE);
        
        mTitleFragment.mRootView.setBackgroundDrawable(null);
        
        MapView mapView = mSphinx.getMapView();
        mapView.setStopRefreshMyLocation(false);
        
        mSphinx.getCenterTokenView().setVisibility(View.INVISIBLE);
        mSphinx.getMapToolsBtn().setVisibility(View.VISIBLE);
        mSphinx.getMapCleanBtn().setVisibility(View.INVISIBLE);
        mSphinx.getLocationView().setVisibility(View.VISIBLE);
    }

    public DataQuery getDataQuery(String keyWord) {
        DataQuery poiQuery = new DataQuery(mContext);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        
        if (keyWord != null) {
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, keyWord);
        }

        return poiQuery;
    }

    @Override
    public void onClick(View v) {
        mBottomFrament = mSphinx.getHomeBottomFragment();
        mSphinx.getPOIQueryFragment().reset();
        mSphinx.getPOIQueryFragment().setMode(InputSearchFragment.MODE_POI);
        mSphinx.showView(R.id.view_poi_input_search);
    }
}
