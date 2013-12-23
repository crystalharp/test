/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.discover;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.ItemizedOverlayHelper;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.POI;
import com.tigerknows.model.PullMessage.Message.PulledDynamicPOI;
import com.tigerknows.ui.discover.CycleViewPager.CyclePagerAdapter;
import com.tigerknows.widget.SpringbackListView.IPagerList;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class DianyingDetailFragment extends BaseDetailFragment
                                    implements CycleViewPager.CycleOnPageChangeListener.IRefreshViews {
    
    public DianyingDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    List<Dianying> mDataList;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mActionTag = ActionLog.DianyingDetail;
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        List<View> viewList = new ArrayList<View>();
        DianyingDetailView view;
        view = new DianyingDetailView(mSphinx, this);
        viewList.add(view);
        view = new DianyingDetailView(mSphinx, this);
        viewList.add(view);
        view = new DianyingDetailView(mSphinx, this);
        viewList.add(view);
        mCyclePagerAdapter = new CyclePagerAdapter(viewList);
        
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.dianying_detail);
    }
    
    public void setData(Dianying data) {
        int position = 0;
        for(int i = mDataList.size()-1; i >= 0; i--) {
            if (data == mDataList.get(i)) {
                position = i;
                break;
            }
        }
        setData(null, position, null);
    }
    
    public void setData(List<Dianying> dataList, int position, IPagerList iPagerList) {
        if (dataList == null) {
            dataList = mDataList;
        }
        mDataList = dataList;
        setData(dataList.size(), position, iPagerList);
        refreshViews(position);
        
        setViewsVisibility(View.VISIBLE);
    }

    public void setPulledDynamicPOI(PulledDynamicPOI dynamicPOI){
    	// There is only one item in the list when showing PulledDynamicPOI
    	position = 0;
        BaseDetailView baseDetailView = (BaseDetailView) mCyclePagerAdapter.viewList.get(position%mCyclePagerAdapter.viewList.size());
        ((DianyingDetailView)baseDetailView).setPulledDynamicPOI(dynamicPOI);
    }
    
    public void viewMap() {
        Dianying data = mDataList.get(mViewPager.getCurrentItem());
        List<POI> list = new ArrayList<POI>();
        POI poi = data.getPOI();
        list.add(poi);
        ItemizedOverlayHelper.drawPOIOverlay(mSphinx, list, 0);
        mSphinx.getResultMapFragment().setData(getString(R.string.dianying_ditu), ActionLog.ResultMapDianyingDetail);
        super.viewMap();
    }
    
    public void refreshViews(int position) {
        super.refreshViews(position);
        DianyingDetailView view;

        FeedbackUpload.logEnterPOIDetailUI(mSphinx,
            DataQuery.DATA_TYPE_DIANYING,
            null,
            mActionTag,
            position,
            mDataList.get(position).getUid(),
            MapEngine.getCityId(mDataList.get(position).getYingxun().getPosition()));
        
        view = (DianyingDetailView) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
        view.setData(mDataList.get(position), position);
        view.onResume();
        
        if (position - 1 >= 0) {
            view = (DianyingDetailView) mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
            view.setData(mDataList.get(position-1), position-1);
            view.onResume();
        }
        if (position + 1 < mDataList.size()) {
            view = (DianyingDetailView) mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
            view.setData(mDataList.get(position+1), position+1);
            view.onResume();
        }
    }    

}
