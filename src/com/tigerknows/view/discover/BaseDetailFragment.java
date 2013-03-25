/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;


import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.PullMessage.Message.PulledDynamicPOI;
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
    
    protected int position = 0;
    
    protected ViewPager mViewPager = null;
    
    protected CycleViewPager.CyclePagerAdapter mCyclePagerAdapter;
    
    protected CycleViewPager.CycleOnPageChangeListener mCycleOnPageChangeListener;

    private View mNextImageView;
    private View mPrevImageView;
    
    private final Animation mHideNextImageViewAnimation =
        new AlphaAnimation(1F, 0F);
    private final Animation mHidePrevImageViewAnimation =
            new AlphaAnimation(1F, 0F);
    private final Animation mShowNextImageViewAnimation =
            new AlphaAnimation(0F, 1F);
    private final Animation mShowPrevImageViewAnimation =
            new AlphaAnimation(0F, 1F);

    private final Runnable mDismissOnScreenControlRunner = new Runnable() {
        public void run() {
            hideOnScreenControls();
        }
    };
    
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
        mRightBtn.setBackgroundResource(R.drawable.btn_view_map);
        mRightBtn.setOnClickListener(this);   

        if (isReLogin()) {
            return;
        }
        
        mPrevImageView.setVisibility(View.GONE);
        mPrevImageView.setVisibility(View.GONE);
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
    }

    protected void findViews() {
        mViewPager = (ViewPager) mRootView.findViewById(R.id.view_pager);
        mViewPager.setAdapter(mCyclePagerAdapter);
        mNextImageView = mRootView.findViewById(R.id.next_imv);
        mPrevImageView = mRootView.findViewById(R.id.prev_imv);
    }

    protected void setListener() {
        mCycleOnPageChangeListener = new CycleOnPageChangeListener(mContext, this, this);
        mViewPager.setOnPageChangeListener(mCycleOnPageChangeListener);
    }

    public void onClick(View view) {
        switch (view.getId()) {                
            case R.id.right_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "titleRight");
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
    
    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        if (tkAsyncTask.getBaseQuery().isPulledDynamicPOIRequest()) {
            dismiss();
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

    public void hideOnScreenControls() {
        if (mNextImageView.getVisibility() == View.VISIBLE) {
            Animation a = mHideNextImageViewAnimation;
            a.setDuration(500);
            mNextImageView.startAnimation(a);
            mNextImageView.setVisibility(View.INVISIBLE);
        }

        if (mPrevImageView.getVisibility() == View.VISIBLE) {
            Animation a = mHidePrevImageViewAnimation;
            a.setDuration(500);
            mPrevImageView.startAnimation(a);
            mPrevImageView.setVisibility(View.INVISIBLE);
        }

    }
    
    public void updateNextPrevControls() {
        int currentPosition = mViewPager.getCurrentItem();
        boolean showPrev = currentPosition > 0;
        boolean showNext = currentPosition < mCyclePagerAdapter.count - 1;

        boolean prevIsVisible = mPrevImageView.getVisibility() == View.VISIBLE;
        boolean nextIsVisible = mNextImageView.getVisibility() == View.VISIBLE;

        if (showPrev && !prevIsVisible) {
            Animation a = mShowPrevImageViewAnimation;
            a.setDuration(500);
            mPrevImageView.startAnimation(a);
            mPrevImageView.setVisibility(View.VISIBLE);
        } else if (!showPrev && prevIsVisible) {
            Animation a = mHidePrevImageViewAnimation;
            a.setDuration(500);
            mPrevImageView.startAnimation(a);
            mPrevImageView.setVisibility(View.GONE);
        }

        if (showNext && !nextIsVisible) {
            Animation a = mShowNextImageViewAnimation;
            a.setDuration(500);
            mNextImageView.startAnimation(a);
            mNextImageView.setVisibility(View.VISIBLE);
        } else if (!showNext && nextIsVisible) {
            Animation a = mHideNextImageViewAnimation;
            a.setDuration(500);
            mNextImageView.startAnimation(a);
            mNextImageView.setVisibility(View.GONE);
        }
    }

    public void scheduleDismissOnScreenControls() {
        mSphinx.getHandler().removeCallbacks(mDismissOnScreenControlRunner);
        mSphinx.getHandler().postDelayed(mDismissOnScreenControlRunner, 2000);
    }

    public void setPulledDynamicPOI(PulledDynamicPOI dynamicPOI){
    }
    
    void setViewsVisibility(int visibility){
        for (View view : mCyclePagerAdapter.viewList) {
            view.setVisibility(visibility);
        }
    }
}
