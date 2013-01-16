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
        mMapFragmentTitle = mContext.getString(R.string.zhanlan_didian_ditu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mCyclePagerAdapter = new CyclePagerAdapter(this);
        
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.zhanlan_detail);
    }

	@Override
	protected BaseDetailView newDetailView() {
		return new ZhanlanDetailView(mSphinx, this);
	}    

}
