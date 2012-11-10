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

import java.io.File;
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
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.R.color;
import com.tigerknows.R.id;
import com.tigerknows.R.layout;
import com.tigerknows.R.string;
import com.tigerknows.share.IBaseShare;
import com.tigerknows.share.ShareEntrance;
import com.tigerknows.share.ShareMessageCenter;
import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.util.ThumbnailUtils;

public class WeiboSend extends Activity implements OnClickListener {
	
    public static final int MAX_LEN = 140;
    
    private TextView mTextNumTxv;

    private Button mLogoutBtn;

    private Button mSendBtn;

    private EditText mTextEdt;

    private TextView mTitleTxv;

    private ImageView mPic;

    private LinearLayout mTextLimitUnitLnl;

    private String mPicPath = "";

    private String mContent = "";
    
    private String mUserName = "";
    
    private IBaseShare mShareObject;

    public static final int WEIBO_MAX_LENGTH = 140;

    private String mActionTag;
    
    private ActionLog mActionLog;
    
    private ProgressDialog mProgressDialog;

    private final String TAG = "WeiboSend";
    
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
				WeiboSend.this.finish();  
		        LogWrapper.d("eric", "WeiboSend.this.finish()"); 	
			}
		}
	};
	
	BroadcastReceiver mImageRemoveReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String aciton = intent.getAction();
			if (ShareMessageCenter.ACTION_SHARE_IMAGE_REMOVE.equals(aciton)) { 
				mPic.setVisibility(View.GONE);
				mPicPath = "";
			}
		}
	};

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.WeiboSend;
        mActionLog = ActionLog.getInstance(WeiboSend.this);

        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        this.setContentView(R.layout.weibo_send);
        findViews();
        setListener();
        
        Intent in = this.getIntent();
        String str = in.getStringExtra(ShareMessageCenter.EXTRA_SHARE_PIC_URI);
        if (!TextUtils.isEmpty(str)) {
            mPicPath = Uri.parse(str).getPath();
        }
        if (TextUtils.isEmpty(this.mPicPath)) {
        	mPic.setVisibility(View.GONE);
        } else {
            File file = new File(mPicPath);
            if (file.exists()) {
                Bitmap source = BitmapFactory.decodeFile(this.mPicPath);
                Bitmap thumb = ThumbnailUtils.extractThumbnail(source, Util.dip2px(Globals.g_metrics.density, 32), Util.dip2px(Globals.g_metrics.density, 32), ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                mPic.setImageBitmap(thumb);
                mPic.setVisibility(View.VISIBLE);
            } else {
            	mPic.setVisibility(View.GONE);
            }
        } 
        
        mUserName = in.getStringExtra(ShareMessageCenter.EXTRA_SHARE_USER_NAME);
        LogWrapper.d(TAG, "get user name from intent: " + mUserName);
        if (!TextUtils.isEmpty(mUserName)) {
        	mTitleTxv.setText(mUserName);
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
        mPic = (ImageView)findViewById(R.id.pic_imv);
        mTextNumTxv = (TextView)this.findViewById(R.id.text_limit_txv);
        mTextEdt = (EditText)this.findViewById(R.id.text_edt);
        mTextLimitUnitLnl = (LinearLayout)this.findViewById(R.id.text_limit_unit_lnl);
    }

    private void setListener() {

        mLogoutBtn.setOnClickListener(this);
        mSendBtn.setOnClickListener(this);
        mPic.setOnClickListener(this);
        mTextLimitUnitLnl.setOnClickListener(this);

        mTextEdt.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String mText = mTextEdt.getText().toString();
                int len = mText.length();
                if (len <= WEIBO_MAX_LENGTH) {
                    len = WEIBO_MAX_LENGTH - len;
                    mTextNumTxv.setTextColor(R.color.text_num_gray);
                } else {
                    len = len - WEIBO_MAX_LENGTH;

                    mTextNumTxv.setTextColor(Color.RED);
                }
                mTextNumTxv.setText(String.valueOf(len));
            }
        });
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
                mActionLog.addAction(ActionLog.WeiboSendClickedLogoutBtn);
//                AccessToken accessToken = (AccessToken)mWeibo.getAccessToken();
//                if (accessToken == null) {
//                    return;
//                }
                final Context context = this;
                mProgressDialog = ProgressDialog.show(context, null,
                        WeiboSend.this.getString(R.string.logout)+"...", true, false);

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
            	LogWrapper.d("eric", "WeiboSend press send btn");
                mActionLog.addAction(ActionLog.WeiboSendClickedSendBtn, mContent);
//                LogWrapper.d("eric", "WeiboSend mWeibo.getAccessToken().getToken(): " + mWeibo.getAccessToken().getToken());
//                if (!TextUtils.isEmpty((String)(mWeibo.getAccessToken().getToken()))) {
                    mContent = mTextEdt.getText().toString();
                    if (TextUtils.isEmpty(mContent) || mContent.length() > MAX_LEN) {
                        Toast.makeText(this, this.getString(R.string.weibo_max_char, MAX_LEN), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    hideInputMethodManager();
                    final Context context = this;
                    mProgressDialog = ProgressDialog.show(context, null,
                    WeiboSend.this.getString(R.string.send)+"...", true, false);
                    new Thread() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							mShareObject.upload(mPicPath, mContent);
						}
					}.start();
//                    mWeibo.upload(WeiboSend.this, mPicPath, mContent, "", "", "");
//                } 
                break;
            }
            case R.id.text_limit_unit_lnl: {
                mActionLog.addAction(ActionLog.WeiboSendClickedDelWord);
                if (!TextUtils.isEmpty(mTextEdt.getText())) {
                	Dialog dialog = new AlertDialog.Builder(WeiboSend.this).setTitle(R.string.attention)
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
                }
                
                break;
            }
            case R.id.pic_imv: {
            	File file = new File(mPicPath);
                if (file.exists()) {
                	mActionLog.addAction(ActionLog.WeiboSendClickPic);
                	hideInputMethodManager();
                    Bitmap source = BitmapFactory.decodeFile(this.mPicPath);
                    AlterImageDialog imageDialog = new AlterImageDialog(WeiboSend.this, source);
                    imageDialog.show();
            	}
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
    			ShareMessageCenter.ACTION_SHARE_LOGOUT_FAIL,
    			ShareMessageCenter.ACTION_SHARE_OPERATION_FAILED
    		));
        ShareMessageCenter.observeAction(this, mMessageReceiver, shareActionList);
        
        List<String> imageActionList = new ArrayList<String>(Arrays.asList(
    			ShareMessageCenter.ACTION_SHARE_IMAGE_REMOVE
    		));
        ShareMessageCenter.observeAction(this, mImageRemoveReceiver, imageActionList);
    }
    
    private void unregistReceivers() {
      ShareMessageCenter.unObserve(this, mMessageReceiver);
      ShareMessageCenter.unObserve(this, mImageRemoveReceiver);
    }
    
    /*
     * Add this method for 魅族 
     * on 魅族 phone, when Activity finish, the attached InputMethodManager won't hide auto,
     * so we need to hide the InputMethodManager manual.
     */
    private void hideInputMethodManager(){
        InputMethodManager imm = (InputMethodManager)WeiboSend.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSendBtn.getWindowToken(), 0);// 隐藏软键盘
    }
}
