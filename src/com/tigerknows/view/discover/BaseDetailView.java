/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;


import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.util.TKAsyncTask;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class BaseDetailView extends LinearLayout {
    
    protected ScrollView mBodyScv;
    
    protected View mLoadingView;
    
    protected Runnable mLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            mSphinx.getHandler().removeCallbacks(mActualLoadedDrawableRun);
            mSphinx.getHandler().post(mActualLoadedDrawableRun);
        }
    };
    
    protected Runnable mActualLoadedDrawableRun = null;
    
    protected Sphinx mSphinx;
    protected BaseDetailFragment mParentFragment;
    protected String mActionTag;
    protected ActionLog mActionLog;
    protected TKAsyncTask mTKAsyncTasking;
    protected BaseQuery mBaseQuerying;
    private BaseData mData;
    
    public BaseDetailView(Sphinx sphinx, BaseDetailFragment parentFragment, int layoutResId) {
        super(sphinx);
        mSphinx = sphinx;
        mParentFragment = parentFragment;
        mActionLog = ActionLog.getInstance(mSphinx);
        
        mSphinx.getLayoutInflater().inflate(layoutResId, this, true);
        findViews(); // TODO: 为什么在这里调用了findView()，在子类的构造函数还需要再调用一次?!?
        setListener();
    }
    
    protected void stopQuery() {
        if (mTKAsyncTasking != null) {
            mTKAsyncTasking.stop();
            mTKAsyncTasking = null;
        }
        if (mBaseQuerying != null) {
            mBaseQuerying = null;
        }
    }
    
    public void setData(BaseData data) {
        if (mData != data) {
            stopQuery();
        }
        mData = data;
    }

    public void onResume() {
        mBodyScv.scrollTo(0, 0);
        refreshDrawable();
    }

    public void dismiss() {
        stopQuery();
    }
    
    protected void refreshDrawable() {
    }
    
    protected void refreshDescription(boolean query) {
    }
    
    protected void findViews() {
        mBodyScv = (ScrollView) findViewById(R.id.body_scv);
        mLoadingView = findViewById(R.id.loading_view);
    }

    protected void setListener() {
    }
    
    public void viewMap() {
        mParentFragment.viewMap();
    }    

    public boolean onPostExecute(TKAsyncTask tkAsyncTask) {
        if (mBaseQuerying != tkAsyncTask.getBaseQuery()) {
            return false;
        }
        return true;
    }
}
