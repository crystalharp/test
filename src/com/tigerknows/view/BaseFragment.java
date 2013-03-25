/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import java.util.List;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.util.TKAsyncTask;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
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
    
    protected Sphinx mSphinx;
    
    protected Context mContext;
    
    protected ActionLog mActionLog;
    
    protected String mActionTag;
    
    protected LayoutInflater mLayoutInflater;
    
    protected View mRootView = null;
    
    protected TitleFragment mTitleFragment;
    
    protected MenuFragment mMenuFragment;
    
    protected TextView mTitleBtn;
    
    protected Button mLeftBtn;
    
    protected Button mRightBtn;
    
    public boolean isReLogin = false;
    
    protected List<BaseQuery> mBaseQuerying;
    
    protected TKAsyncTask mTkAsyncTasking;
    
    protected PopupWindow mPopupWindow;
    
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
    
    private View.OnClickListener mLeftTxvOnClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View arg0) {
            synchronized (mSphinx.mUILock) {
                if (!mSphinx.mUIProcessing) {
                	if (arg0.getVisibility() == View.VISIBLE) {
                	    mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "titleLeft");
	                	dismiss();
                	}
                }
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
        onCreateView(null, this, null);
        if (mRootView != null) {
            addView(mRootView);
        }
    }
    
    public void dismiss() {
        onPause();
        mSphinx.uiStackDismiss(getId());
        
        if (mTkAsyncTasking != null) {
            mTkAsyncTasking.stop();
        }
    }
    
    public void show() {    
        mSphinx.uiStackPush(getId());  
        onResume();
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
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onActivityCreated()"+mActionTag);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onActivityResult()"+mActionTag);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        return null;
    }

    public void onAttach(Activity activity) {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onAttach()"+mActionTag);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onConfigurationChanged()"+mActionTag);
        super.onConfigurationChanged(newConfig);
    }

    public boolean onContextItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onContextItemSelected()"+mActionTag);
        return false;
    }

    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onCreateAnimator()"+mActionTag);
        return null;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onCreateContextMenu()"+mActionTag);
    }

    public void onDestroy() {
        LogWrapper.d(TAG, "onDestroy()"+mActionTag);
        // TODO Auto-generated method stub
    }

    public void onDestroyOptionsMenu() {
        LogWrapper.d(TAG, "onDestroyOptionsMenu()"+mActionTag);
        // TODO Auto-generated method stub
    }

    public void onDestroyView() {
        LogWrapper.d(TAG, "onDestroyView()"+mActionTag);
        // TODO Auto-generated method stub
    }

    public void onDetach() {
        LogWrapper.d(TAG, "onDetach()"+mActionTag);
        // TODO Auto-generated method stub
    }

    public void onHiddenChanged(boolean hidden) {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onHiddenChanged()"+mActionTag);
    }

    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onInflate()"+mActionTag);
    }

    public void onLowMemory() {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onLowMemory()"+mActionTag);
    }

    public void onOptionsMenuClosed(Menu menu) {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onOptionsMenuClosed()"+mActionTag);
    }

    public void onPause() {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onPause()"+mActionTag);
        int id = getId();
        if (id != R.id.view_invalid
                && id != R.id.view_title
                && id != R.id.view_menu) {
            mSphinx.hideSoftInput();

        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
        LogWrapper.d(TAG, "onPrepareOptionsMenu()"+mActionTag);
        // TODO Auto-generated method stub
    }

    public void onResume() {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onResume()"+mActionTag);
        int id = getId();
        if (id != R.id.view_invalid
                && id != R.id.view_title
                && id != R.id.view_menu) { 
            if (!TextUtils.isEmpty(mActionTag)) {
                mActionLog.addAction(ActionLog.UI, mActionTag);
            }
            mSphinx.replace(this);   
            
            mTitleFragment = mSphinx.getTitleFragment();
            mMenuFragment = mSphinx.getMenuFragment();

            mTitleBtn = mTitleFragment.getTitleTxv();
            mLeftBtn = mTitleFragment.getLeftTxv();
            mRightBtn = mTitleFragment.getRightTxv();

            mTitleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            mTitleBtn.setText(null);
            mTitleBtn.setBackgroundDrawable(null);
            mTitleBtn.setOnClickListener(null);
            mLeftBtn.setVisibility(View.VISIBLE);
            mLeftBtn.setText(null);
            mLeftBtn.setBackgroundResource(R.drawable.btn_back);
            mLeftBtn.setOnClickListener(mLeftTxvOnClickListener);
            mRightBtn.setVisibility(View.VISIBLE);
            mRightBtn.setBackgroundDrawable(null);
            mRightBtn.setText(null);
            mRightBtn.setEnabled(true);
            
            mMenuFragment.hide();
        	mTitleFragment.display();
            Globals.getAsyncImageLoader().setViewToken(toString());
        	mSphinx.getMapView().setStopRefreshMyLocation(true);
        }
        
        if (mRootView != null) {
            mRootView.setOnTouchListener(new OnTouchListener() {
                
                @Override
                public boolean onTouch(View arg0, MotionEvent arg1) {
                    // TODO Auto-generated method stub
                    return true;
                }
            });
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onSaveInstanceState()"+mActionTag);
    }

    public void onStart() {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onStart()"+mActionTag);
    }

    public void onStop() {
        // TODO Auto-generated method stub
        LogWrapper.d(TAG, "onStop()"+mActionTag);
    }
    
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        LogWrapper.d(TAG, "onCreateOptionsMenu()"+mActionTag);
        // TODO Auto-generated method stub
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        LogWrapper.d(TAG, "onOptionsItemSelected()"+mActionTag);
        // TODO Auto-generated method stub
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
    
    public void dismissPopupWindow() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }
    
    public void setTkAsyncTasking(TKAsyncTask tkAsyncTask) {
    	this.mTkAsyncTasking = tkAsyncTask;
    }
    
    public void setBaseQuerying(List<BaseQuery> baseQuerying) {
    	this.mBaseQuerying = baseQuerying;
    }
}
