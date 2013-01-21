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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.tigerknows.widget.Toast;

import com.tencent.tauth.TAuthView;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.share.TKTencentOpenAPI.AuthReceiver;
import com.tigerknows.util.CommonUtils;

public class QZoneSend extends Activity implements OnClickListener {
	
    private Button mLogoutBtn;

    private Button mSendBtn;

    private EditText mTextEdt;

    private TextView mTitleTxv;

    private String mContent = "";
    
    private String mActionTag;
    
    private ActionLog mActionLog;
    
    private ShareAPI.LoginCallBack mLoginCallBack = new ShareAPI.LoginCallBack() {
        
        @Override
        public void onSuccess() {
            QZoneSend.this.runOnUiThread(new Runnable() {
                
                @Override
                public void run() {
                    checkUserAccessIdenty(false);
                }
            });
        }

        @Override
        public void onFailed() {
        }

        @Override
        public void onCancel() {
        }
    };
    
    private AuthReceiver mTencentAuthReceiver;

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
        case R.id.dialog_share_doing:
            dialog = new ProgressDialog(this);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            ((ProgressDialog)dialog).setMessage(getString(R.string.doing_and_wait));
            dialog.setOnDismissListener(new OnDismissListener() {
                
                @Override
                public void onDismiss(DialogInterface arg0) {
                    checkUserAccessIdenty(false);
                }
            });
            break;
        }
        
        return dialog;
    }
    
    private boolean checkUserAccessIdenty(boolean needLogin) {
        UserAccessIdenty userAccessIdenty = ShareAPI.readIdentity(QZoneSend.this, ShareAPI.TYPE_TENCENT);
        if (userAccessIdenty == null) {
            if (needLogin) {
                TKTencentOpenAPI.login(QZoneSend.this);
            }
            mLogoutBtn.setText(R.string.back);
            mTitleTxv.setText(R.string.share_tiger_user);
        } else {
            mLogoutBtn.setText(R.string.logout);
            mTitleTxv.setText(Html.fromHtml(userAccessIdenty.getUserName()));
        }
        
        return userAccessIdenty != null;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.QzoneSend;
        mActionLog = ActionLog.getInstance(QZoneSend.this);
        
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        this.setContentView(R.layout.tencent_open_send);
        findViews();
        setListener();
        
        Intent in = this.getIntent();
        
        mContent = in.getStringExtra(ShareAPI.EXTRA_SHARE_CONTENT);
        mTextEdt.setText(mContent);
        mTextEdt.requestFocus();
        
        mTencentAuthReceiver = new AuthReceiver(this, mLoginCallBack);
        registerIntentReceivers();
        checkUserAccessIdenty(true);
    }

    private void findViews() {

        mTitleTxv = (TextView)findViewById(R.id.title_txv);
        mLogoutBtn = (Button)this.findViewById(R.id.logout_btn);
        mSendBtn = (Button)this.findViewById(R.id.send_btn);
        mTextEdt = (EditText)this.findViewById(R.id.text_edt);
    }

    private void setListener() {
        mLogoutBtn.setOnClickListener(this);
        mSendBtn.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mActionLog.addAction(mActionTag+"DM");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActionLog.addAction(mActionTag);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId) {
            case R.id.logout_btn: {
                mActionLog.addAction(ActionLog.QzoneSendClickLogoutbBtn);
                if (checkUserAccessIdenty(false) == false) {
                    QZoneSend.this.finish();
                    return;
                }
                hideInputMethodManager();
                TKTencentOpenAPI.logout(QZoneSend.this);     
                Toast.makeText(this, R.string.logout_sucess, Toast.LENGTH_LONG).show();
                finish();
                break;
            }
            case R.id.send_btn: {
                mActionLog.addAction(ActionLog.QzoneSendClickSendBtn, mContent);
                if (checkUserAccessIdenty(true) == false) {
                    return;
                }
                mContent = mTextEdt.getText().toString();
                hideInputMethodManager();
                
                if (TextUtils.isEmpty(mContent) ) {
                    Toast.makeText(this, this.getString(R.string.tencent_share_content_empty), Toast.LENGTH_SHORT).show();
                    return;
                }

                TKTencentOpenAPI.addShare(QZoneSend.this, mContent, true, true);
                break;
            }
            case R.id.text_limit_unit_lnl: {
                mActionLog.addAction(ActionLog.WeiboSendClickedDelWord);
                CommonUtils.showNormalDialog(QZoneSend.this,
                        getString(R.string.attention),
                        getString(R.string.are_you_delete_all), 
                        null,
                        getString(R.string.yes),
                        getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (DialogInterface.BUTTON_POSITIVE == which) {
                                    mActionLog.addAction(ActionLog.WeiboSendClickedDelWordYes);
                                    mTextEdt.setText("");
                                } else {
                                    mActionLog.addAction(ActionLog.WeiboSendClickedDelWordNo);
                                }
                            }
                        });
                break;
            }
            default:
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }
    
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
        if (mTencentAuthReceiver != null) {
            unregisterIntentReceivers();
        }
    }

    private void registerIntentReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(TAuthView.AUTH_BROADCAST);
        registerReceiver(mTencentAuthReceiver, filter);
    }
    
    private void unregisterIntentReceivers() {
        unregisterReceiver(mTencentAuthReceiver);
    }
    
    /*
     * Add this method for 魅族 
     * on 魅族 phone, when Activity finish, the attached InputMethodManager won't hide auto,
     * so we need to hide the InputMethodManager manual.
     */
    private void hideInputMethodManager(){
        InputMethodManager imm = (InputMethodManager)QZoneSend.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSendBtn.getWindowToken(), 0);// 隐藏软键盘
    }
}
