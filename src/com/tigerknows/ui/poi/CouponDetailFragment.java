/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.Coupon;
import com.tigerknows.ui.discover.BaseDetailFragment;
import com.tigerknows.ui.discover.CycleViewPager;
import com.tigerknows.ui.discover.CycleViewPager.CyclePagerAdapter;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class CouponDetailFragment extends BaseDetailFragment implements View.OnClickListener, CycleViewPager.CycleOnPageChangeListener.IRefreshViews {
    
    public CouponDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private List<Coupon> mDataList = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TuangouDetail;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        // Create the views used in the ViewPager
        List<View> viewList = new ArrayList<View>();
        CouponDetailView view;
        view = new CouponDetailView(mSphinx, this);
        viewList.add(view);
        view = new CouponDetailView(mSphinx, this);
        viewList.add(view);
        view = new CouponDetailView(mSphinx, this);
        viewList.add(view);
        mCyclePagerAdapter = new CyclePagerAdapter(viewList);
        
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.coupon_detail);
        mRightBtn.setVisibility(View.INVISIBLE);
    }
    
    public void setData(Coupon data) {
        int position = 0;
        for(int i = mDataList.size()-1; i >= 0; i--) {
            if (data == mDataList.get(i)) {
                position = i;
                break;
            }
        }
        setData(null, position);
    }
    
    public void setData(List<Coupon> dataList, int position) {
        if (dataList == null) {
            dataList = mDataList;
        }
        
        this.mDataList = dataList;
        setData(dataList.size(), position, null);
        refreshViews(position);
        setViewsVisibility(View.VISIBLE);
    }
    
    public void refreshViews(int position) {
        super.refreshViews(position);
        CouponDetailView view;

        view = (CouponDetailView) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
        view.setData(mDataList.get(position), position);
        view.onResume();
        
        if (position - 1 >= 0) {
            view = (CouponDetailView) mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
            view.setData(mDataList.get(position-1), position-1);
            view.onResume();
        }
        if (position + 1 < mDataList.size()) {
            view = (CouponDetailView) mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
            view.setData(mDataList.get(position+1), position+1);
            view.onResume();
        }
    }
}
