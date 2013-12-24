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
import android.widget.Button;

/**
 * @author Peng Wenyue
 */
public class HomeBottomFragment extends BaseFragment implements View.OnClickListener {

    private Button mPOIBtn;

    private Button mTrafficBtn;

    private Button mMoreBtn;
    
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
        mPOIBtn = (Button) mRootView.findViewById(R.id.poi_btn);
        mTrafficBtn = (Button) mRootView.findViewById(R.id.traffic_btn);
        mMoreBtn = (Button) mRootView.findViewById(R.id.more_btn);
        mMoreImv = mRootView.findViewById(R.id.more_imv);
    }

    protected void setListener() {
        super.setListener();
        mPOIBtn.setOnClickListener(this);
        mTrafficBtn.setOnClickListener(this);
        mMoreBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.poi_btn) {
            mSphinx.getPOINearbyFragment().setData(mSphinx.getCenterPOI(), false);
            mSphinx.showView(R.id.view_poi_nearby_search);
        } else if (id == R.id.traffic_btn) {
            mSphinx.showView(R.id.view_traffic_home);
        } else if (id == R.id.more_btn) {
            mSphinx.showView(R.id.view_more_home);
        }
        
    }
    
}
