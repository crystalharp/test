package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class DynamicPOIViewTemplate extends POIDetailFragment.DynamicPOIView {
    
    List<DynamicPOIViewBlock> mBlockList = new ArrayList<DynamicPOIViewBlock>();
    DynamicPOIViewBlock mViewBlock;
    BaseQuery mBaseQuery;
    
    View mRootView;
    View mTitleView;
    TextView mTitleTxv;
    TextView mTitleRightTxv;
    LinearLayout mListView;
    View mMoreView;
    TextView mMoreTxv;
    
    public DynamicPOIViewTemplate(POIDetailFragment poiFragment, LayoutInflater inflater){
        mPOIDetailFragment = poiFragment;
        mSphinx = mPOIDetailFragment.mSphinx;
        mInflater = inflater;

        mRootView = mInflater.inflate(R.layout.poi_dynamic_template, null);
        mTitleView = mRootView.findViewById(R.id.title_view);
        mTitleTxv = (TextView) mTitleView.findViewById(R.id.title_txv);
        mTitleRightTxv = (TextView) mTitleView.findViewById(R.id.title_right_txv);
        mListView = (LinearLayout) mRootView.findViewById(R.id.list_view);
        mMoreView = mRootView.findViewById(R.id.more_view);
        mMoreTxv = (TextView) mMoreView.findViewById(R.id.more_txv);
    }

    @Override
    public void refresh() {
        mViewBlock.refresh();
    }
    
    @Override
    public List<DynamicPOIViewBlock> getViewList() {
        mBlockList.clear();
        mBlockList.add(mViewBlock);
        return mBlockList;
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        // TODO Auto-generated method stub
        
    }
}
