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

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
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
import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.share.ShareAPI.LoginCallBack;
import com.tigerknows.share.TKWeibo.AuthDialogListener;
import com.tigerknows.util.ThumbnailUtils;
import com.weibo.net.DialogError;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboException;
import com.weibo.net.AsyncWeiboRunner.RequestListener;

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
    
    public static final int WEIBO_MAX_LENGTH = 140;

    private String mActionTag;
    
    private ActionLog mActionLog;
    
    private TKWeibo mSina;
	
	public void removeImage() {
	    mPic.setVisibility(View.GONE);
        mPicPath = "";
	}
    
    private ShareAPI.LoginCallBack mLoginCallBack = new ShareAPI.LoginCallBack() {
        
        @Override
        public void onSuccess() {
            UserAccessIdenty userAccessIdenty = ShareAPI.readIdentity(WeiboSend.this, ShareAPI.TYPE_WEIBO);
            if (userAccessIdenty != null) {
                mLogoutBtn.setText(R.string.logout);
                mTitleTxv.setText(Html.fromHtml(userAccessIdenty.getUserName()));
            }
        }

        @Override
        public void onFailed() {
        }

        @Override
        public void onCancel() {
        }
    };
    
    private class MyAuthDialogListener extends AuthDialogListener {
        
        public MyAuthDialogListener(Activity activity, LoginCallBack loginCallBack) {
            super(activity, loginCallBack);
        }

        @Override
        public void onComplete(Bundle values) {
            super.onComplete(values);
        }
        
        @Override
        public void onError(DialogError e) {
            super.onError(e);
        }

        @Override
        public void onCancel() {
            super.onCancel();
        }

        @Override
        public void onWeiboException(WeiboException e) {
            super.onWeiboException(e);
        }
        
    };
    
    private MyAuthDialogListener mSinaAuthDialogListener;

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
        case R.id.dialog_share_doing:
            dialog = new ProgressDialog(this);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            ((ProgressDialog)dialog).setMessage(getString(R.string.doing_and_wait));
            break;
        }
        
        return dialog;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.WeiboSend;
        mActionLog = ActionLog.getInstance(WeiboSend.this);

        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        this.setContentView(R.layout.weibo_send);
        findViews();
        setListener();
        
        mSinaAuthDialogListener = new MyAuthDialogListener(this, mLoginCallBack);
        Intent in = this.getIntent();
        String str = in.getStringExtra(ShareAPI.EXTRA_SHARE_PIC_URI);
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
        
        mContent = in.getStringExtra(ShareAPI.EXTRA_SHARE_CONTENT);
        mTextEdt.setText(mContent);
        mTextEdt.requestFocus();
        
        mSina = new TKWeibo(this, true, true);
        UserAccessIdenty userAccessIdenty = ShareAPI.readIdentity(this, ShareAPI.TYPE_WEIBO);
        if (userAccessIdenty == null) {
            TKWeibo.login(this, mSinaAuthDialogListener);
            mLogoutBtn.setText(R.string.back);
        } else {
            mTitleTxv.setText(Html.fromHtml(userAccessIdenty.getUserName()));
            mLogoutBtn.setText(R.string.logout);
        }
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
//                    mTextNumTxv.setTextColor(R.color.text_num_gray);
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
                hideInputMethodManager();
                UserAccessIdenty userAccessIdenty = ShareAPI.readIdentity(this, ShareAPI.TYPE_WEIBO);
                if (userAccessIdenty == null) {
                    WeiboSend.this.finish();
                    return;
                }
//                showDialog(R.id.dialog_share_doing);
                new Thread() {
					@Override
					public void run() {
                        Weibo weibo = Weibo.getInstance();
					    TKWeibo.logout(mSina, weibo, Weibo.getAppKey(), new RequestListener() {
                            
                            @Override
                            public void onIOException(final IOException e) {
                                ShareAPI.clearIdentity(WeiboSend.this, ShareAPI.TYPE_WEIBO);
//                              WeiboSend.this.dismissDialog(R.id.dialog_share_doing);
                                WeiboSend.this.finish();
                            }
                            
                            @Override
                            public void onError(final WeiboException e) {
                                WeiboSend.this.runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        ShareAPI.clearIdentity(WeiboSend.this, ShareAPI.TYPE_WEIBO);
//                                        WeiboSend.this.dismissDialog(R.id.dialog_share_doing);
                                        WeiboSend.this.finish();
                                    }
                                });
                            }
                            
                            @Override
                            public void onComplete(String response) {
                                WeiboSend.this.runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        ShareAPI.clearIdentity(WeiboSend.this, ShareAPI.TYPE_WEIBO);
//                                        WeiboSend.this.dismissDialog(R.id.dialog_share_doing);
                                        WeiboSend.this.finish();
                                    }
                                });
                            }
                        });
					}
                }.start();
                
                break;
            }
            case R.id.send_btn: {
                mActionLog.addAction(ActionLog.WeiboSendClickedSendBtn, mContent);
                UserAccessIdenty userAccessIdenty = ShareAPI.readIdentity(this, ShareAPI.TYPE_WEIBO);
                if (userAccessIdenty == null) {
                    mLogoutBtn.setText(R.string.back);
                    TKWeibo.login(this, mSinaAuthDialogListener);
                    return;
                }
                mContent = mTextEdt.getText().toString();
                if (TextUtils.isEmpty(mContent) || mContent.length() > MAX_LEN) {
                    Toast.makeText(this, this.getString(R.string.weibo_max_char, MAX_LEN), Toast.LENGTH_SHORT).show();
                    return;
                }
                hideInputMethodManager();
                showDialog(R.id.dialog_share_doing);
                new Thread() {
                    
                    @Override
                    public void run() {
                        Weibo weibo = Weibo.getInstance();
                        // TODO Auto-generated method stub
                        if (!TextUtils.isEmpty(mPicPath)) {
                            TKWeibo.upload(mSina, weibo, Weibo.getAppKey(), mPicPath, mContent, "", "");
                            
                        } else {
                            // Just update a text weibo!
                            TKWeibo.update(mSina, weibo, Weibo.getAppKey(), mContent, "", "");
                        }
                    }
                }.start();
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
    }
    
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
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

    public MyAuthDialogListener getSinaAuthDialogListener() {
        return mSinaAuthDialogListener;
    }

    public Button getLogoutBtn() {
        return mLogoutBtn;
    }
}
