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

import android.app.Activity;
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

import com.tigerknows.R;
import com.decarta.Globals;
import com.decarta.android.util.Util;
import android.widget.Toast;
import com.tigerknows.android.app.TKActivity;
import com.tigerknows.common.ActionLog;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.util.ThumbnailUtils;

public class WeiboSendActivity extends BaseActivity implements OnClickListener, TKActivity.IAuthorizeCallback {
	
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
	
	public void removeImage() {
	    mPic.setVisibility(View.GONE);
        mPicPath = "";
	}
	
	@Override
	public void onSuccess(String type) {
	    if (ShareAPI.TYPE_WEIBO.equals(type)) {
	        checkUserAccessIdenty(false);
	    }
	}
    
    private boolean checkUserAccessIdenty(boolean needAuthorize) {
        boolean result;
        UserAccessIdenty userAccessIdenty = ShareAPI.readIdentity(WeiboSendActivity.this, ShareAPI.TYPE_WEIBO);
        if (userAccessIdenty == null) {
            result = false;
            mLogoutBtn.setText(R.string.back);
            mTitleTxv.setText(null);
            if (needAuthorize) {
                authorizeWeibo(this);
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
        
        mActionTag = ActionLog.WeiboSend;
        mActionLog = ActionLog.getInstance(WeiboSendActivity.this);

        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        this.setContentView(R.layout.share_weibo_send);
        findViews();
        setListener();
        
        initWeibo(true, true);
        
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
        
        checkUserAccessIdenty(true);
    }

    protected void findViews() {
        super.findViews();
        mTitleTxv = (TextView)findViewById(R.id.title_txv);
        mLogoutBtn = (Button)this.findViewById(R.id.logout_btn);
        mSendBtn = (Button)this.findViewById(R.id.send_btn);
        mPic = (ImageView)findViewById(R.id.pic_imv);
        mTextNumTxv = (TextView)this.findViewById(R.id.text_limit_txv);
        mTextEdt = (EditText)this.findViewById(R.id.text_edt);
        mTextLimitUnitLnl = (LinearLayout)this.findViewById(R.id.text_limit_unit_lnl);
    }

    protected void setListener() {
        super.setListener();
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
                mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton, finish);
                hideInputMethodManager();
                if (finish) {
                    finish();
                    return;
                }
                new Thread() {
					@Override
					public void run() {
                        TKWeibo.logout(mTKWeibo);
					}
                }.start();
                Toast.makeText(this, R.string.logout_success, Toast.LENGTH_LONG).show();
                WeiboSendActivity.this.finish();
                break;
            }
            case R.id.send_btn: {
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                if (checkUserAccessIdenty(true) == false) {
                    return;
                }
                mContent = mTextEdt.getText().toString();
                if (TextUtils.isEmpty(mContent) || mContent.length() > MAX_LEN) {
                    Toast.makeText(this, this.getString(R.string.weibo_max_char, MAX_LEN), Toast.LENGTH_SHORT).show();
                    return;
                }
                hideInputMethodManager();
                ActionLog.getInstance(this).addAction(ActionLog.Dialog, getString(R.string.doing_and_wait));
                new Thread() {
                    
                    @Override
                    public void run() {
                        if (!TextUtils.isEmpty(mPicPath)) {
                            TKWeibo.upload(mTKWeibo, mContent, mPicPath, "", "");
                        } else {
                            // Just update a text weibo!
                            TKWeibo.update(mTKWeibo, mContent, "", "");
                        }
                    }
                }.start();
                break;
            }
            case R.id.text_limit_unit_lnl: {
                mActionLog.addAction(mActionTag +  ActionLog.WeiboSendDelWord);
                if (mTextEdt.getEditableText().length() > 0) {
                    Utility.showNormalDialog(WeiboSendActivity.this,
                            getString(R.string.attention),
                            getString(R.string.are_you_delete_all), 
                            null,
                            getString(R.string.yes),
                            getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (DialogInterface.BUTTON_POSITIVE == which) {
                                        mTextEdt.setText("");
                                    }
                                }
                            });
                }
                break;
            }
            case R.id.pic_imv: {
            	File file = new File(mPicPath);
                if (file.exists()) {
                	mActionLog.addAction(mActionTag +  ActionLog.WeiboSendPic);
                	hideInputMethodManager();
                    Bitmap source = BitmapFactory.decodeFile(this.mPicPath);
                    ViewImageDialog imageDialog = new ViewImageDialog(WeiboSendActivity.this, source);
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
        InputMethodManager imm = (InputMethodManager)WeiboSendActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSendBtn.getWindowToken(), 0);// 隐藏软键盘
    }
}
