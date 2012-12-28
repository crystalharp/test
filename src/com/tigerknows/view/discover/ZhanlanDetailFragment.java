/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.POI;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.view.SpringbackListView.IPagerList;
import com.tigerknows.view.discover.CycleViewPager.CyclePagerAdapter;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class ZhanlanDetailFragment extends BaseDetailFragment
                                   implements CycleViewPager.CycleOnPageChangeListener.IRefreshViews {
    
    public ZhanlanDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    List<Zhanlan> mDataList;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.ZhanlanXiangqing;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        List<View> viewList = new ArrayList<View>();
        ZhanlanDetailView view;
        view = new ZhanlanDetailView(mSphinx, this);
        viewList.add(view);
        view = new ZhanlanDetailView(mSphinx, this);
        viewList.add(view);
        view = new ZhanlanDetailView(mSphinx, this);
        viewList.add(view);
        mCyclePagerAdapter = new CyclePagerAdapter(viewList);
        
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.zhanlan_detail);
    }
    
    public void setData(Zhanlan data) {
        int position = 0;
        for(int i = mDataList.size()-1; i >= 0; i--) {
            if (data == mDataList.get(i)) {
                position = i;
                break;
            }
        }
        setData(null, position, null);
    }
    
    public void setData(List<Zhanlan> dataList, int position, IPagerList iPagerList) {
        if (dataList == null) {
            dataList = mDataList;
        }
        mDataList = dataList;
        setData(dataList.size(), position, iPagerList);
        ZhanlanDetailView view = (ZhanlanDetailView) mCyclePagerAdapter.viewList.get(mViewPager.getCurrentItem()%mCyclePagerAdapter.viewList.size());
        view.setData(mDataList.get(position));
        view.onResume();
    }
    
    public void viewMap() {
        Zhanlan data = mDataList.get(mViewPager.getCurrentItem());
        List<POI> list = new ArrayList<POI>();
        POI poi = data.getPOI();
        poi.setName(data.getPlaceName());
        poi.setAddress(data.getAddress());
        list.add(poi);
        mSphinx.showPOI(list, 0);
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.zhanlan_didian_ditu), ActionLog.MapZhanlanXiangqing);
        super.viewMap();
    }
    
    public void refreshViews(int position) {
        super.refreshViews(position);
        ZhanlanDetailView view;
        if (position - 1 >= 0) {
            view = (ZhanlanDetailView) mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
            view.setData(mDataList.get(position-1));
            view.onResume();
        }
        if (position + 1 < mDataList.size()) {
            view = (ZhanlanDetailView) mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
            view.setData(mDataList.get(position+1));
            view.onResume();
        }
    }
}
