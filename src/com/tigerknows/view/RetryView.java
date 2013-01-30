/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tigerknows.view;

import com.tigerknows.ActionLog;
import com.tigerknows.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;


public class RetryView extends LinearLayout {
    
    public interface CallBack {
        public void retry();
    }
    
    private TextView mTextTxv;
    
    CallBack mCallBack;
        
    public RetryView(Context context) {
        this(context, null);
    }

    public RetryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.retry_view, this, // we are the parent
                true);
        
        findViews();
        setListener();
        
        setBackgroundColor(0x00000000);
    }

    protected void findViews() {
        mTextTxv = (TextView) findViewById(R.id.text_txv);
    }
    
    protected void setListener() {
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCallBack != null) {
            ActionLog.getInstance(getContext()).addAction(ActionLog.RETRY);
            mCallBack.retry();
        }
        return true;
    }
    
    @Override
    public boolean hasFocus() {
        if (mTextTxv == null) {
            return false;
        }
        return mTextTxv.hasFocus();
    }
    
    public void setCallBack(CallBack callBack) {
        mCallBack = callBack;
    }
    
    public void setText(int resid) {
        mTextTxv.setText(resid);
    }
    
    public void setText(String text) {
        mTextTxv.setText(text);
    }
}
