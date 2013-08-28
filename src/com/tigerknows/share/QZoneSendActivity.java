/*
 * Copyright 2011 Sina.
 *
 * Licensed under the Apache License and Weibo License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.open.weibo.com
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tigerknows.share;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.android.app.TKActivity;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.ui.BaseActivity;

public class QZoneSendActivity extends BaseActivity implements View.OnClickListener, TKActivity.IAuthorizeCallback {
	
    private Button mLogoutBtn;

    private Button mSendBtn;

    private EditText mTextEdt;

    private TextView mTitleTxv;

    private String mContent = "";
    
    @Override
    public void onSuccess(String type) {
        if (ShareAPI.TYPE_TENCENT.equals(type)) {
            checkUserAccessIdenty(false);
        }
    }
        
    private boolean checkUserAccessIdenty(boolean needAuthorize) {
        boolean result;
        UserAccessIdenty userAccessIdenty = ShareAPI.readIdentity(QZoneSendActivity.this, ShareAPI.TYPE_TENCENT);
        if (userAccessIdenty == null) {
            result = false;
            mLogoutBtn.setText(R.string.back);
            mTitleTxv.setText(null);
            if (needAuthorize) {
                authorizeTencent(this);
            }
        } else {
            result = true;
            mLogoutBtn.setText(R.string.logout);
            mTitleTxv.setText(Html.fromHtml(userAccessIdenty.getUserName()));
        }
        
        return result;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.QzoneSend;
        mActionLog = ActionLog.getInstance(QZoneSendActivity.this);
        
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        this.setContentView(R.layout.share_qzone_send);
        findViews();
        setListener();
        
        initQZone();
        
        Intent in = this.getIntent();
        
        mContent = in.getStringExtra(ShareAPI.EXTRA_SHARE_CONTENT);
        mTextEdt.setText(mContent);
        mTextEdt.requestFocus();
        
        checkUserAccessIdenty(true);
    }

    protected void findViews() {
        super.findViews();
        mTitleTxv = (TextView)findViewById(R.id.title_txv);
        mLogoutBtn = (Button)this.findViewById(R.id.logout_btn);
        mSendBtn = (Button)this.findViewById(R.id.send_btn);
        mTextEdt = (EditText)this.findViewById(R.id.text_edt);
    }

    protected void setListener() {
        super.setListener();
        mLogoutBtn.setOnClickListener(this);
        mSendBtn.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId) {
            case R.id.logout_btn: {
                boolean finish = (checkUserAccessIdenty(false) == false);
                mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton, String.valueOf(finish));
                hideInputMethodManager();
                if (finish) {
                    finish();
                    return;
                }
                TKTencentOpenAPI.logout(QZoneSendActivity.this);     
                Toast.makeText(this, R.string.logout_success, Toast.LENGTH_LONG).show();
                finish();
                break;
            }
            case R.id.send_btn: {
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                if (checkUserAccessIdenty(true) == false) {
                    return;
                }
                mContent = mTextEdt.getText().toString();
                hideInputMethodManager();
                
                if (TextUtils.isEmpty(mContent) ) {
                    Toast.makeText(this, this.getString(R.string.tencent_share_content_empty), Toast.LENGTH_SHORT).show();
                    return;
                }

                TKTencentOpenAPI.addShare(QZoneSendActivity.this, mContent, true, true);
                break;
            }
            default:
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }
    
    /*
     * Add this method for 魅族 
     * on 魅族 phone, when Activity finish, the attached InputMethodManager won't hide auto,
     * so we need to hide the InputMethodManager manual.
     */
    private void hideInputMethodManager(){
        InputMethodManager imm = (InputMethodManager)QZoneSendActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSendBtn.getWindowToken(), 0);// 隐藏软键盘
    }
}
