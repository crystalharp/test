/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.util.TKAsyncTask;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author Peng Wenyue
 */
public class BaseDialog extends Dialog {
    
    protected Window mWindow = null;

    protected Context mContext = null;
    
    protected LayoutInflater mLayoutInflater;
    
    protected Sphinx mSphinx = null;
    
    protected boolean mDirectToMap = false;
    
    protected int mId = R.id.view_invalid;
    
    protected ActionLog mActionLog;
    
    protected String mActionTag;
    
    protected TextView mTitleTxv;
    
    protected ImageView mLeftBtn;
    
    protected ImageView mRightBtn;
    
    protected Button mLeftTxv;
    
    protected Button mRightTxv;
    
    public BaseDialog(Context context) {
        this(context, R.style.Theme_Dialog);
    }
    
    public BaseDialog(Context context, int theme) {
        super(context, theme);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN|WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        mContext = context;
        mSphinx = (Sphinx) mContext;
        mLayoutInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setProperty();
        mActionLog = ActionLog.getInstance(mContext);
    }

    public void setProperty() {
        mWindow = getWindow(); // 得到对话框的窗口．
        WindowManager.LayoutParams wl = mWindow.getAttributes();
        wl.gravity = Gravity.TOP;
        mWindow.setAttributes(wl);
    }
    
    protected void findViews() {
        mTitleTxv = (TextView) findViewById(R.id.title_txv);
        mLeftBtn = (ImageView) findViewById(R.id.left_btn);
        mRightBtn = (ImageView) findViewById(R.id.right_btn);
        mLeftTxv = (Button) findViewById(R.id.left_txv);
        mRightTxv = (Button) findViewById(R.id.right_txv);
    }
    
    protected void setListener() {
        if (mLeftTxv != null) {
            mLeftTxv.setOnClickListener(new View.OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    synchronized (mSphinx.mUILock) {
                        if (!mSphinx.mUIProcessing) {
                            mActionLog.addAction(ActionLog.Title_Left_Back, mActionTag);
                            dismiss();
                        }
                    }
                }
            });
        }
    }
    
    @Override
    public void dismiss() {
        if (!TextUtils.isEmpty(mActionTag)) {
            mActionLog.addAction(ActionLog.Dismiss, mActionTag);
        }
        
        if (mSphinx != null) {
            mSphinx.uiStackDismiss(mId);
            mSphinx.hideSoftInput();
        }
        onPause();
        super.dismiss();
    }

    @Override
    public void show() {
        if (!TextUtils.isEmpty(mActionTag)) {
            mActionLog.addAction(mActionTag);
        }
        if (mSphinx != null) {
            mSphinx.uiStackPush(mId);
        }
        onResume();
        super.show();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {                
            case KeyEvent.KEYCODE_SEARCH:
                return true;
            case KeyEvent.KEYCODE_BACK:
            	mActionLog.addAction(ActionLog.KeyCodeBack);
            	dismiss();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    public void setId(int id) {
        mId = id;
    }
    
    public void onResume() {
        
    }
    
    public void onPause() {
        
    }
}
