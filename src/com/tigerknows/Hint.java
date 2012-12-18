/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.tigerknows.R;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * @author Peng Wenyue
 */
public class Hint extends BaseActivity implements View.OnClickListener {

    private int mLayoutResID;
    private Button mIknowBtn;
    
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
        mIknowBtn = (Button) findViewById(R.id.iknow_btn);
    }
    
    protected void setListener() {
        super.setListener();
        if (mIknowBtn != null) {
            mIknowBtn.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iknow_btn:
                finish();
                break;

            default:
                break;
        }
    }
}
