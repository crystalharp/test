/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.tigerknows.R;

import android.os.Bundle;
import android.view.View;

/**
 * @author Peng Wenyue
 */
public class Hint extends BaseActivity implements View.OnClickListener {

    private View mRootView;
    private int mLayoutResID;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.HitComment;
        
        if (mIntent != null) {
            mLayoutResID = mIntent.getIntExtra("layoutResID", R.id.view_invalid);
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
        mRootView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.root_view:
                finish();
                break;

            default:
                break;
        }
    }
}
