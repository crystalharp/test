/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.tigerknows.R;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * @author Peng Wenyue
 */
public class HintActivity extends BaseActivity implements View.OnClickListener {
    
    public static final String LAYOUT_RES_ID = "layoutResID";

    private int mLayoutResID;
    
    private View mRootView;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (mIntent != null) {
            mLayoutResID = mIntent.getIntExtra(LAYOUT_RES_ID, R.id.view_invalid);
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
                finish();
                return false;
            }
        });
    }

    @Override
    public void onClick(View view) {
    }
}
