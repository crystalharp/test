/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;


import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.SpringbackListView;
import com.tigerknows.view.SpringbackListView.IPagerList;
import com.tigerknows.view.discover.CycleViewPager.CycleOnPageChangeListener;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class BaseDetailFragment extends DiscoverBaseFragment implements View.OnClickListener,
    SpringbackListView.IPagerListCallBack, CycleViewPager.CycleOnPageChangeListener.IRefreshViews {
    
    public BaseDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    protected int position = -1;
    
    protected ViewPager mViewPager = null;
    
    protected CycleViewPager.CyclePagerAdapter mCyclePagerAdapter;
    
    protected CycleViewPager.CycleOnPageChangeListener mCycleOnPageChangeListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = mLayoutInflater.inflate(R.layout.view_pager, container, false);
        
        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mRightImv.setImageResource(R.drawable.ic_view_map);
        mRightBtn.getLayoutParams().width = Util.dip2px(Globals.g_metrics.density, 72);
        mRightBtn.setOnClickListener(this);   

        if (isReLogin()) {
            return;
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        for(int i = mCyclePagerAdapter.viewList.size()-1; i >= 0; i--) {
            BaseDetailView view = (BaseDetailView) mCyclePagerAdapter.viewList.get(i);
            view.dismiss();
        }
    }
    
    protected void setData(int count, int position, IPagerList iPagerList) {
        if (iPagerList == null) {
            iPagerList = this.mCycleOnPageChangeListener.iPagerList;
        }
        
        mCyclePagerAdapter.count = count;
        
        mCycleOnPageChangeListener.iPagerList = iPagerList;
        mCycleOnPageChangeListener.isPageTurning = false;
        mCycleOnPageChangeListener.count = mCyclePagerAdapter.count;
        mCyclePagerAdapter.notifyDataSetChanged();
        
        mViewPager.setCurrentItem(position);
        this.position = -1;
        refreshViews(position);
    }

    protected void findViews() {
        mViewPager = (ViewPager) mRootView.findViewById(R.id.view_pager);
        mViewPager.setAdapter(mCyclePagerAdapter);
    }

    protected void setListener() {
        mCycleOnPageChangeListener = new CycleOnPageChangeListener(this, this);
        mViewPager.setOnPageChangeListener(mCycleOnPageChangeListener);
    }

    public void onClick(View view) {
        switch (view.getId()) {                
            case R.id.right_btn:
                mActionLog.addAction(ActionLog.Title_Right_Button, mActionTag);
                viewMap();
                break;
        }
    }
    
    public void viewMap() {
        mSphinx.showView(R.id.view_result_map);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        for(int i = mCyclePagerAdapter.viewList.size()-1; i >= 0; i--) {
            BaseDetailView view = (BaseDetailView) mCyclePagerAdapter.viewList.get(i);
            if (view.onPostExecute(tkAsyncTask)) {
                break;
            }
        }
    }
    
    public void refreshViews(int position) {
        if (this.position == position) {
            return;
        }
        this.position = position;
    }

    @Override
    public void turnPageEnd(boolean isHeader, int size) {
        if (size > mCyclePagerAdapter.count) {
            mCyclePagerAdapter.count = size;
            mCycleOnPageChangeListener.count = mCyclePagerAdapter.count;
            mCyclePagerAdapter.notifyDataSetChanged();
            mViewPager.setCurrentItem(mViewPager.getCurrentItem()+1, true);
        }
        mCycleOnPageChangeListener.isPageTurning = false;        
    }
}
