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

package com.tigerknows.share.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Toast;

import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.share.IBaseShare;
import com.tigerknows.share.ShareEntrance;
import com.tigerknows.share.ShareMessageCenter;
import com.decarta.android.util.LogWrapper;

public class QZoneSend extends Activity implements OnClickListener {
	
    private Button mLogoutBtn;

    private Button mSendBtn;

    private EditText mTextEdt;

    private TextView mTitleTxv;

    private String mContent = "";
    
    private String mUserName = "";
    
    private IBaseShare mShareObject;

    private String mActionTag;
    
    private ActionLog mActionLog;
    
    private ProgressDialog mProgressDialog;

    private final String TAG = "QzoneSend";
    
    BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
	            mProgressDialog.dismiss();
	            LogWrapper.d("eric", "mProgressDialog.dismiss()");
			}
			
			String action = intent.getAction();
			if (ShareMessageCenter.ACTION_SHARE_UPLOAD_SUCCESS.equals(action)
					|| ShareMessageCenter.ACTION_SHARE_LOGOUT_SUCCESS.equals(action)) {
				QZoneSend.this.finish();  
		        LogWrapper.d("eric", "QzoneSend.this.finish()"); 	
			}
		}
	};

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.QzoneSend;
        mActionLog = ActionLog.getInstance(QZoneSend.this);
        
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        this.setContentView(R.layout.tencent_open_send);
        findViews();
        setListener();
        
        Intent in = this.getIntent();
        
        mUserName = in.getStringExtra(ShareMessageCenter.EXTRA_SHARE_USER_NAME);
        LogWrapper.d(TAG, "get user name from intent: " + mUserName);
        if (!TextUtils.isEmpty(mUserName)) {
        	mTitleTxv.setText(Html.fromHtml(mUserName));
        }
        
        mContent = in.getStringExtra(ShareMessageCenter.EXTRA_SHARE_CONTENT);
        mTextEdt.setText(mContent);
        mTextEdt.setText(mContent);
        mTextEdt.requestFocus();
        
        mShareObject = ShareEntrance.getInstance().getCurrentShareObject();
        registRecivers();
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
                final Context context = this;
                mProgressDialog = ProgressDialog.show(context, null,
                        QZoneSend.this.getString(R.string.logout)+"...", true, false);

                hideInputMethodManager();
                new Thread() {
					
					@Override
					public void run() {
						mShareObject.logout();
					}
                }.start();

                
                break;
            }
            case R.id.send_btn: {
            	LogWrapper.d("eric", "QzoneSend press send btn");
                mActionLog.addAction(ActionLog.QzoneSendClickSendBtn, mContent);
                mContent = mTextEdt.getText().toString();
                hideInputMethodManager();
                
                if (TextUtils.isEmpty(mContent) ) {
                    Toast.makeText(this, this.getString(R.string.tencent_share_content_empty), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                final Context context = this;
                mProgressDialog = ProgressDialog.show(context, null,
                QZoneSend.this.getString(R.string.send)+"...", true, false);
                new Thread() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						mShareObject.upload("", mContent);
					}
				}.start();

                break;
            }
            case R.id.text_limit_unit_lnl: {
                mActionLog.addAction(ActionLog.WeiboSendClickedDelWord);
                Dialog dialog = new AlertDialog.Builder(QZoneSend.this).setTitle(R.string.attention)
                        .setMessage(R.string.are_you_delete_all).setPositiveButton(R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        mActionLog.addAction(ActionLog.WeiboSendClickedDelWordYes);
                                        mTextEdt.setText("");
                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mActionLog.addAction(ActionLog.WeiboSendClickedDelWordNo);
                                        dialog.dismiss();
                                    }
                                }).create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                break;
            }
            default:
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
//        if (mProgressDialog != null && mProgressDialog.isShowing()) {
//            mProgressDialog.dismiss();
//        }
    }
    
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
        unregistReceivers();
	}

	private void registRecivers() {
    	List<String> shareActionList = new ArrayList<String>(Arrays.asList(
    			ShareMessageCenter.ACTION_SHARE_UPLOAD_SUCCESS,
    			ShareMessageCenter.ACTION_SHARE_UPLOAD_FAIL,
    			ShareMessageCenter.ACTION_SHARE_LOGOUT_SUCCESS,
    			ShareMessageCenter.ACTION_SHARE_LOGOUT_FAIL
    		));
        ShareMessageCenter.observeAction(this, mMessageReceiver, shareActionList);
    }
    
    private void unregistReceivers() {
        ShareMessageCenter.unObserve(this, mMessageReceiver);
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
