/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.discover;


import java.util.List;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.BaseQuery;

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
    /**
     * Set to true when a query starts
     * Set to false when a query returns in onPostExecute
     * When new data is set to the same view, stopQuery will set this to false
     * and the new data operation can go.
     * This guarantees there is only one query on-going for a single description item
     * to remove duplicate download for the same description, to save Data and Time.
     */
    protected boolean mAsyncTaskExecuting = false;
    
    protected List<BaseQuery> mBaseQuerying;
    private BaseData mData;

    protected int mPosition;
    
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
        mAsyncTaskExecuting = false;
    }
    
    /**
     * 
     * @param data
     */
    public void setData(BaseData data, int position) {
        if (mData != data) {
            stopQuery();
        }
        mData = data;
        mPosition=position;
    }

    public void onResume() {
        mBodyScv.smoothScrollTo(0, 0);
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
        mBodyScv.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mParentFragment.updateNextPrevControls();
                    mParentFragment.scheduleDismissOnScreenControls();
                }
                return false;
            }
        });
    }
    
    public void viewMap() {
        mParentFragment.viewMap();
    }    

    public boolean onPostExecute(TKAsyncTask tkAsyncTask) {
    	mAsyncTaskExecuting = false;
        if (mBaseQuerying != tkAsyncTask.getBaseQueryList()) {
            return false;
        }
        return true;
    }

	public boolean onCancel(TKAsyncTask tkAsyncTask) {
    	mAsyncTaskExecuting = false;
        if (mBaseQuerying != tkAsyncTask.getBaseQueryList()) {
            return false;
        }
		return true;
	}
}
