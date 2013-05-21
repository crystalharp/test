/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.tigerknows.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * @author Peng Wenyue
 */
public class Hint extends BaseActivity implements View.OnClickListener {
    
    public static final String LAYOUT_RES_ID = "layoutResID";
    
    public static final String NEXT_KEY = "nextKey";
    
    public static final String NEXT_LAYOUT_RES_ID = "nextLayoutResID";

    private int mLayoutResID;
    
    private int mNextResID = R.id.view_invalid;
    
    private String mNextKey;
    
    private View mRootView;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (mIntent != null) {
            mLayoutResID = mIntent.getIntExtra(LAYOUT_RES_ID, R.id.view_invalid);
            mNextKey = mIntent.getStringExtra(NEXT_KEY);
            mNextResID = mIntent.getIntExtra(NEXT_LAYOUT_RES_ID, R.id.view_invalid);
            if (mLayoutResID == R.id.view_invalid) {
                finish();
            } else {
                setContentView(mLayoutResID);
            }
        } else {
            finish();
        }

        findViews();
        setListener();
    }

    protected void findViews() {
        super.findViews();
        mRootView = findViewById(R.id.root_view);
    }
    
    protected void setListener() {
        super.setListener();
        mRootView.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                Intent data = new Intent();
                data.putExtra(NEXT_KEY, mNextKey);
                data.putExtra(NEXT_LAYOUT_RES_ID, mNextResID);
                setResult(RESULT_OK, data);
                finish();
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
                Intent data = new Intent();
                data.putExtra(NEXT_KEY, mNextKey);
                data.putExtra(NEXT_LAYOUT_RES_ID, mNextResID);
                setResult(RESULT_OK, data);
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
}
