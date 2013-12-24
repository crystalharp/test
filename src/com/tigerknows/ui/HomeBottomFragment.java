/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.tigerknows.R;
import com.tigerknows.Sphinx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author Peng Wenyue
 */
public class HomeBottomFragment extends BaseFragment implements View.OnClickListener {

    private View mPOIView;

    private View mTrafficView;

    private View mMoreView;
    
    private View mMoreImv;
    
    public HomeBottomFragment(Sphinx sphinx) {
        super(sphinx);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.home_bottom, container, false);
        
        findViews();
        setListener();
        
        return mRootView;
    }
    
    protected void findViews() {
        mPOIView = mRootView.findViewById(R.id.poi_view);
        mTrafficView = mRootView.findViewById(R.id.traffic_view);
        mMoreView = mRootView.findViewById(R.id.more_view);
        mMoreImv = mRootView.findViewById(R.id.more_imv);
    }

    protected void setListener() {
        super.setListener();
        mPOIView.setOnClickListener(this);
        mTrafficView.setOnClickListener(this);
        mMoreView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.poi_view) {
            mSphinx.getPOINearbyFragment().setData(mSphinx.getCenterPOI());
            mSphinx.showView(R.id.view_poi_nearby_search);
        } else if (id == R.id.traffic_view) {
            mSphinx.showView(R.id.view_traffic_home);
        } else if (id == R.id.more_view) {
            mMoreImv.setVisibility(View.GONE);
            mSphinx.showView(R.id.view_more_home);
        }
        
    }
    
    public View getMoreImv() {
        return mMoreImv;
    }
    
}
