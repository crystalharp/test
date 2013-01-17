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
import com.tigerknows.model.BaseData;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.POI;
import com.tigerknows.view.SpringbackListView.IPagerList;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class DianyingDetailFragment extends BaseDetailFragment{
    
    public DianyingDetailFragment(Sphinx sphinx) {
        super(sphinx);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.DianyingXiangqing;
        mMapFragmentTitle = mContext.getString(R.string.dianying_ditu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mCyclePagerAdapter = new DetailViewPagerAdapter(this);
        
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.dianying_detail);
    }

	@Override
	protected BaseDetailView newDetailView() {
		return new DianyingDetailView(mSphinx, this);
	}
    
}
