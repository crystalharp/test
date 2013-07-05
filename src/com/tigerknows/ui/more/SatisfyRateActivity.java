/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.more;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.tigerknows.R;
import com.tigerknows.common.ActionLog;
import com.tigerknows.ui.BaseActivity;

/**
 * @author Feng TianXiao
 */
public class SatisfyRateActivity extends BaseActivity implements View.OnClickListener {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mActionTag = ActionLog.Feedback;

        setContentView(R.layout.more_satisfy_rate);
        findViews();
        setListener();    
    }
    
    protected void findViews() {
    	super.findViews();
    }
    
    protected void setListener() {
    	super.setListener();
    }
    
    @Override
    public void onClick(View view) {
        switch (view.getId()) {

        default:
            break;
    }
}
}