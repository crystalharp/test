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

import com.decarta.Globals;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.POI;
import com.tigerknows.model.Tuangou;
import com.tigerknows.view.SpringbackListView.IPagerList;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class TuangouDetailFragment extends BaseDetailFragment{
    
    public TuangouDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    boolean isRequsetBuy = false;
    
    private boolean isRequsetBuy() {
        boolean isRequsetBuy = this.isRequsetBuy;
        this.isRequsetBuy = false;
        return isRequsetBuy;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TuangouXiangqing;
        mMapFragmentTitle = mContext.getString(R.string.shanghu_ditu);
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
        mTitleBtn.setText(R.string.tuangou_detail);
        if (isRequsetBuy() && Globals.g_User != null) {
            ((TuangouDetailView) getDetailViewByPosition(mViewPager.getCurrentItem())).buy();
            return;
        }
    }

	@Override
	protected BaseDetailView newDetailView() {
		return new TuangouDetailView(mSphinx, this);
	}
    
    
}
