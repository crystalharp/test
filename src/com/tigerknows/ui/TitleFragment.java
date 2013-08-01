/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;

import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.PopupWindow.OnDismissListener;

/**
 * @author Peng Wenyue
 */
public class TitleFragment extends BaseFragment implements View.OnClickListener {
    
    public TitleFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    protected Button titleBtn;
    
    protected Button leftBtn;
    
    protected Button rightBtn;
    
    protected ListView popupLsv;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.title, container, false);
        findViews();
        setListener();
        return mRootView;
    }
    
    protected void findViews() {
        titleBtn = (Button) mRootView.findViewById(R.id.title_btn);
        leftBtn = (Button) mRootView.findViewById(R.id.left_btn);
        rightBtn = (Button) mRootView.findViewById(R.id.right_btn);
    }

    protected void setListener() {
        leftBtn.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.left_btn:
                // 关闭顶部的Fragment，显示第二个Fragment
                break;

            default:
                break;
        }
    }

    public TextView getTitleTxv() {
        return titleBtn;
    }

    public Button getLeftTxv() {
        return leftBtn;
    }

    public Button getRightTxv() {
        return rightBtn;
    }
    
    public void showPopupWindow(ListAdapter adapter, OnItemClickListener listener, String actionTag) {
    	this.mActionTag = actionTag;
        mActionLog.addAction(this.mActionTag + ActionLog.PopupWindowTitle);
        if (mPopupWindow == null) {
            View view  = mLayoutInflater.inflate(R.layout.title_popup_list, this, false);
            popupLsv = (ListView) view.findViewById(R.id.listview);
            //在padding区域点击关掉窗口
            View titlePopupView = view.findViewById(R.id.title_popup_view);
            titlePopupView.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    dismissPopupWindow();
                    return true;
                }
            });

            mPopupWindow = new PopupWindow(view);
            mPopupWindow.setWindowLayoutMode(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            mPopupWindow.setFocusable(true);
            // 设置允许在外点击消失
            mPopupWindow.setOutsideTouchable(true);

            // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
            mPopupWindow.setOnDismissListener(new OnDismissListener() {
                
                @Override
                public void onDismiss() {
                    mActionLog.addAction(mActionTag+ActionLog.PopupWindowTitle+ActionLog.Dismiss);
                }
            });
        }
        popupLsv.setOnItemClickListener(listener);
        popupLsv.setAdapter(adapter);
        
        mPopupWindow.showAsDropDown(this, 0, 0);
    }
    
    public void dismissPopupWindow() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }
}
