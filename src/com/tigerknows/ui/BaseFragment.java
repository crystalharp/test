/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import java.util.List;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.android.widget.TKEditText;
import com.tigerknows.common.ActionLog;
import com.tigerknows.common.AsyncImageLoader;
import com.tigerknows.model.BaseQuery;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * @author Peng Wenyue
 */
public class BaseFragment extends LinearLayout {

    protected static final String TAG = "BaseFragment";
    
    public Sphinx mSphinx;
    
    public Context mContext;
    
    public ActionLog mActionLog;
    
    public String mActionTag;
    
    protected LayoutInflater mLayoutInflater;
    
    public View mRootView = null;
    
    public TitleFragment mTitleFragment = null;
    
    public BaseFragment mBottomFrament = null;
    
    /**
     * 此是标题栏中间的ViewGroup，可以用自定义的View替换此ViewGroup的内容
     */
    public ViewGroup mTitleView;
    
    public TKEditText mKeywordEdt;
    
    public Button mTitleBtn;
    
    public Button mLeftBtn;
    
    public Button mRightBtn;
    
    /**
     * 最顶的View，其高度为0
     */
    public View mSkylineView;
    
    public boolean isReLogin = false;
    
    protected List<BaseQuery> mBaseQuerying;
    
    protected TKAsyncTask mTkAsyncTasking;
    
    protected PopupWindow mPopupWindow;
    
    public boolean mDismissed = true;
    
    public int mHeight;
    
    public PopupWindow getPopupWindow() {
        return mPopupWindow;
    }

    public void setPopupWindow(PopupWindow popupWindow) {
        this.mPopupWindow = popupWindow;
    }

    public DialogInterface.OnClickListener mCancelLoginListener = new DialogInterface.OnClickListener() {
        
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            isReLogin();
        }
    };
    
    public boolean isReLogin() {
        boolean isRelogin = this.isReLogin;
        this.isReLogin = false;
        if (isRelogin) {
            if (mBaseQuerying != null) {
            	for(int i = 0, size = mBaseQuerying.size(); i < size; i++) {
	                mBaseQuerying.get(i).setResponse(null);
            	}
                mSphinx.queryStart(mBaseQuerying);
            }
        }
        return isRelogin;
    }
    
    private View.OnClickListener mLeftBtnOnClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View arg0) {
            synchronized (mSphinx.mUILock) {
        	    mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton);
            	dismiss();
            }
        }
    };
    
    public BaseFragment(Sphinx sphinx) {
        super(sphinx);
        setId(R.id.view_invalid);
        mSphinx = sphinx;
    }
    
    public void onCreate(Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreate()"+mActionTag);
        mContext = mSphinx.getBaseContext();
        mActionLog = ActionLog.getInstance(mContext);
        mLayoutInflater = mSphinx.getLayoutInflater();
        
        int id = getId();
        if (id != R.id.view_invalid) {
            mTitleFragment = mSphinx.getTitleFragment();            
            mTitleView = mTitleFragment.mTitleView;
            mTitleBtn = mTitleFragment.mTitleBtn;
            mKeywordEdt = mTitleFragment.mKeywordEdt;
            mLeftBtn = mTitleFragment.mLeftBtn;
            mRightBtn = mTitleFragment.mRightBtn;
            mSkylineView = mTitleFragment.mSkylineView;
        }
        
        onCreateView(null, this, null);
        if (mRootView != null) {
            mRootView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mHeight = mRootView.getMeasuredHeight();
            addView(mRootView);
        }
    }
    
    public void dismiss() {
        onPause();
        mSphinx.uiStackDismiss(getId());
        
        if (mTkAsyncTasking != null) {
            mTkAsyncTasking.stop();
        }
        mDismissed = true;
    }
    
    public void show() {    
        mSphinx.uiStackPush(getId());  
        onResume();
        mDismissed = false;
    }
    
    public void hide() {    
        this.setVisibility(View.GONE);
        onPause();
    }
    
    public void display() {  
        this.setVisibility(View.VISIBLE);
        onResume();
    }
    
    public void onActivityCreated(Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onActivityCreated()"+mActionTag);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogWrapper.d(TAG, "onActivityResult()"+mActionTag);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        return null;
    }
    
    protected void findViews() {
    }

    protected void setListener() {
        setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    public void onAttach(Activity activity) {
        LogWrapper.d(TAG, "onAttach()"+mActionTag);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LogWrapper.d(TAG, "onConfigurationChanged()"+mActionTag);
        super.onConfigurationChanged(newConfig);
    }

    public boolean onContextItemSelected(MenuItem item) {
        LogWrapper.d(TAG, "onContextItemSelected()"+mActionTag);
        return false;
    }

    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        LogWrapper.d(TAG, "onCreateAnimator()"+mActionTag);
        return null;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        LogWrapper.d(TAG, "onCreateContextMenu()"+mActionTag);
    }

    public void onDestroy() {
        LogWrapper.d(TAG, "onDestroy()"+mActionTag);
    }

    public void onDestroyOptionsMenu() {
        LogWrapper.d(TAG, "onDestroyOptionsMenu()"+mActionTag);
    }

    public void onDestroyView() {
        LogWrapper.d(TAG, "onDestroyView()"+mActionTag);
    }

    public void onDetach() {
        LogWrapper.d(TAG, "onDetach()"+mActionTag);
    }

    public void onHiddenChanged(boolean hidden) {
        LogWrapper.d(TAG, "onHiddenChanged()"+mActionTag);
    }

    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onInflate()"+mActionTag);
    }

    public void onLowMemory() {
        LogWrapper.d(TAG, "onLowMemory()"+mActionTag);
    }

    public void onOptionsMenuClosed(Menu menu) {
        LogWrapper.d(TAG, "onOptionsMenuClosed()"+mActionTag);
    }

    public void onPause() {
        LogWrapper.d(TAG, "onPause()"+mActionTag);
        int id = getId();
        if (id != R.id.view_invalid && id != R.id.view_infowindow) {
            dismissPopupWindow();
            mSphinx.hideSoftInput();
            mSphinx.resetLoactionButtonState();
            if (!TextUtils.isEmpty(mActionTag)) {
                mActionLog.addAction(mActionTag + ActionLog.Dismiss);
            }
        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
        LogWrapper.d(TAG, "onPrepareOptionsMenu()"+mActionTag);
    }

    public void onResume() {
        LogWrapper.d(TAG, "onResume()"+mActionTag);
        int id = getId();
        if (id != R.id.view_invalid && id != R.id.view_infowindow) { 
            if (!TextUtils.isEmpty(mActionTag)) {
                mActionLog.addAction(mActionTag);
            }
            
            if (R.id.view_home == getId()) {
                mSphinx.getTitleView().getChildAt(0).setVisibility(View.VISIBLE);
                mSphinx.getTitleView().getChildAt(1).setVisibility(View.GONE);
            } else {
                mSphinx.getTitleView().getChildAt(0).setVisibility(View.GONE);
                mSphinx.getTitleView().getChildAt(1).setVisibility(View.VISIBLE);
            }
            mSphinx.replaceBodyUI(this);
            mSphinx.replaceBottomUI(this);
            mTitleFragment.reset();
            mTitleFragment.mLeftBtn.setOnClickListener(mLeftBtnOnClickListener);
            
            AsyncImageLoader.getInstance().setViewToken(toString());
        	mSphinx.getMapView().setStopRefreshMyLocation(true);
        }
        correctUIStack();
    }
    
    public void correctUIStack(){
    	// DO NOT DELETE virtual method
    }
    
    public void onSaveInstanceState(Bundle outState) {
        LogWrapper.d(TAG, "onSaveInstanceState()"+mActionTag);
    }

    public void onStart() {
        LogWrapper.d(TAG, "onStart()"+mActionTag);
    }

    public void onStop() {
        LogWrapper.d(TAG, "onStop()"+mActionTag);
    }
    
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        LogWrapper.d(TAG, "onCreateOptionsMenu()"+mActionTag);
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        LogWrapper.d(TAG, "onOptionsItemSelected()"+mActionTag);
        return false;
    }
    
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return false;
    }

    public void onCancelled(TKAsyncTask tkAsyncTask) {
        mTkAsyncTasking = null;
    }

    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        mTkAsyncTasking = null;
    }
    
    public boolean isShowing() {
        return mSphinx.uiStackPeek() == getId();
    }
    
    Runnable dismissPopupWindowTask = new Runnable() {
		@Override
		public void run() {
            mPopupWindow.dismiss();
		}
	};
    
    public void dismissPopupWindow() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
        	mRootView.post(dismissPopupWindowTask);
        }
    }
    
    public void setTkAsyncTasking(TKAsyncTask tkAsyncTask) {
    	this.mTkAsyncTasking = tkAsyncTask;
    }
    
    public void setBaseQuerying(List<BaseQuery> baseQuerying) {
    	this.mBaseQuerying = baseQuerying;
    }
    
    public String getLogTag() throws Exception{
    	throw new Exception("getTag must be overrided by subclasss to use log*!");
    }

    public void logI(String msg){
    	try {
			LogWrapper.i(getLogTag(), msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public void logD(String msg){
    	try {
			LogWrapper.d(getLogTag(), msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public void logW(String msg){
    	try {
			LogWrapper.w(getLogTag(), msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public void logE(String msg){
    	try {
			LogWrapper.e(getLogTag(), msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public final String getString(int resId) {
        return mContext.getString(resId);
    }
    
    public final String getString(int resId, Object... formatArgs) {
        return mContext.getString(resId, formatArgs);
    }
    
    public static final void setTxvText(TextView txv, String s) {
        if (TextUtils.isEmpty(s)) {
            txv.setVisibility(View.GONE);
        } else {
            txv.setVisibility(View.VISIBLE);
            txv.setText(s);
        }
    }
}
