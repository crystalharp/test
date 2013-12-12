/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.android.app.TKActivity;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

/**
 * @author Peng Wenyue
 */
public class BaseActivity extends TKActivity implements TKAsyncTask.EventListener {
    
    public static String SOURCE_VIEW_ID = "SOURCE_VIEW_ID";
    
    public static String SOURCE_USER_HOME = "SOURCE_USER_HOME";
    
    static final String TAG = "BaseActivity";
    
    protected int mSourceViewId;
    
    protected boolean mSourceUserHome = false;
    
    protected Button mTitleBtn;
    
    protected Button mLeftBtn;
    
    protected Button mRightBtn;
    
    protected Button mRight2Btn;
    
    protected int mId;
    
    protected boolean isReLogin = false;
    
    protected DialogInterface.OnClickListener mCancelLoginListener = new DialogInterface.OnClickListener() {
        
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            isReLogin();
        }
    };
    
    protected boolean isReLogin() {
        boolean isRelogin = this.isReLogin;
        this.isReLogin = false;
        if (isRelogin) {
            if (mBaseQuerying != null) {
            	for(int i = 0, size = mBaseQuerying.size(); i < size; i++) {
	                mBaseQuerying.get(i).setResponse(null);
            	}
            	queryStart(mBaseQuerying);
            }
        }
        return isRelogin;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        getViewId(mIntent);
        
        mHandler = new Handler();
    }
    
    protected void findViews() {
        mTitleBtn = (Button) findViewById(R.id.title_btn);
        mLeftBtn = (Button) findViewById(R.id.left_btn);
        mRightBtn = (Button) findViewById(R.id.right_btn);
        mRight2Btn = (Button) findViewById(R.id.right2_btn);
    }
    
    protected void setListener() {
        if (mLeftBtn != null) {
            mLeftBtn.setOnClickListener(new View.OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton);
                    finish();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogWrapper.d(TAG, "onResume()");
    }

    @Override
    protected void onPause() {
        LogWrapper.d(TAG, "onPause()");
        super.onPause();
    }
    
    @Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    protected void onNewIntent(Intent newIntent) {
        // TODO Auto-generated method stub
        super.onNewIntent(newIntent);
        setIntent(newIntent);
        mIntent = getIntent();
        getViewId(mIntent);
    }
    
    protected void getViewId(Intent intent) {
        mSourceViewId = intent.getIntExtra(SOURCE_VIEW_ID, R.id.view_invalid);
        mSourceUserHome = intent.getBooleanExtra(SOURCE_USER_HOME, false);
    }
    
    protected void putViewId(Intent intent) {
        intent.putExtra(SOURCE_VIEW_ID, mId);
        intent.putExtra(SOURCE_USER_HOME, mSourceUserHome);
    }

    @Override
    public void finish() {
        hideSoftInput();
        super.finish();
        LogWrapper.d(TAG, "finish()");
    }

    @Override
    //为了防止万一程序被销毁的风险，这个方法可以保证重要数据的正确性
    //不写这个方法并不意味着一定出错，但是一旦遇到了一些非常奇怪的数据问题的时候
    //可以看看是不是由于某些重要的数据没有保存，在程序被销毁时被重置
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
      super.onSaveInstanceState(savedInstanceState);
      LogWrapper.d(TAG, "onSaveInstanceState()");

    }  

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      LogWrapper.d(TAG, "onRestoreInstanceState()");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {     
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        putViewId(intent);
        super.startActivityForResult(intent, requestCode);
    }
        
    @Override
    public void startActivity(Intent intent) {
        putViewId(intent);
        super.startActivity(intent);
    }
}
