/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;

/**
 * @author Peng Wenyue
 */
public class HintActivity extends BaseActivity implements View.OnClickListener {
    
    public static final String EXTRA_KEY_LIST = "extra_key_list";
    
    public static final String EXTRA_LAYOUT_RES_ID_LIST = "extra_layout_res_id_list";

    private int[] mLayoutResIdList;
    
    private String[] mKeyList;
    
    private int mIndex = 0;
    
    private ViewGroup mRootView;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRootView = new LinearLayout(mThis);
        setContentView(mRootView);
        
        if (mIntent != null) {
            mKeyList = mIntent.getStringArrayExtra(EXTRA_KEY_LIST);
            mLayoutResIdList = mIntent.getIntArrayExtra(EXTRA_LAYOUT_RES_ID_LIST);
            mIndex = 0;
            
            showHint();
        } else {
            finish();
        }

        findViews();
        setListener();
    }
    
    void showHint() {
        if (mKeyList == null ||
                mLayoutResIdList == null ||
                mKeyList.length == 0 ||
                mKeyList.length != mLayoutResIdList.length ||
                mIndex+1 > mKeyList.length) {
            finish();
        } else {
            String key = mKeyList[mIndex];
            int resId = mLayoutResIdList[mIndex];
            mIndex++;
            TKConfig.setPref(mThis, key, "1");
            mRootView.removeAllViews();
            mLayoutInflater.inflate(resId, mRootView, true);
        }
    }

    protected void findViews() {
        super.findViews();
    }
    
    protected void setListener() {
        super.setListener();
        mRootView.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                showHint();
                return false;
            }
        });
    }

    @Override
    public void onClick(View view) {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {                
            case KeyEvent.KEYCODE_BACK:
                mActionLog.addAction(ActionLog.KeyCodeBack);
                showHint();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
}
