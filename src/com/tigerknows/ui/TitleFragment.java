/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.widget.TKEditText;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

/**
 * @author Peng Wenyue
 */
public class TitleFragment extends BaseFragment {
    
    public int mRightBtnConfirmColor;
    public int mRightBtnCancelColor;
    
    public TitleFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.title, container, false);
        
        findViews();
        setListener();
        
        Resources resources = mSphinx.getResources();
        mRightBtnConfirmColor = resources.getColor(R.color.white);
        mRightBtnCancelColor = resources.getColor(R.color.black_dark);
        
        return mRootView;
    }
    
    protected void findViews() {
        mTitleView = (ViewGroup) mRootView.findViewById(R.id.center_view);
        mTitleBtn = (Button) mRootView.findViewById(R.id.title_btn);
        mKeywordEdt = (TKEditText) mRootView.findViewById(R.id.keyword_edt);
        mLeftBtn = (Button) mRootView.findViewById(R.id.left_btn);
        mRightBtn = (Button) mRootView.findViewById(R.id.right_btn);
        mSkylineView = mRootView.findViewById(R.id.skyline_view);
    }
    
    public void reset() {
        
        mRootView.setBackgroundResource(R.drawable.bg_title);
        
        if (mTitleView.getChildAt(0) != mTitleBtn || mTitleView.getChildAt(1) != mKeywordEdt) {
            mTitleView.removeAllViews();
            mTitleView.addView(mTitleBtn);
            mTitleView.addView(mKeywordEdt);
        }
        
        mTitleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        mTitleBtn.setText(null);
        mTitleBtn.setBackgroundDrawable(null);
        mTitleBtn.setCompoundDrawables(null, null, null, null);
        mTitleBtn.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
        mTitleBtn.setOnClickListener(null);
        mTitleBtn.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams layoutParams = (android.widget.RelativeLayout.LayoutParams) mTitleBtn.getLayoutParams();
        layoutParams.topMargin = 0;
        layoutParams.bottomMargin = 0;
        
        mKeywordEdt.setVisibility(View.GONE);
        
        mLeftBtn.setText(null);
        mLeftBtn.setBackgroundResource(R.drawable.btn_back);
        mLeftBtn.setOnClickListener(null);
        mLeftBtn.setVisibility(View.VISIBLE);
        
        mRightBtn.setBackgroundResource(R.drawable.btn_confirm);
        mRightBtn.setTextColor(mRightBtnConfirmColor);
        mRightBtn.setText(null);
        mRightBtn.setEnabled(true);
        mRightBtn.setOnClickListener(null);
        mRightBtn.setVisibility(View.VISIBLE);
    }

    public void refreshRightBtn(String keyword) {
        if (keyword.trim().length() > 0) {
            mRightBtn.setText(R.string.confirm);
            mRightBtn.setTextColor(mRightBtnConfirmColor);
            mRightBtn.setBackgroundResource(R.drawable.btn_confirm);
        } else {
            mRightBtn.setText(R.string.cancel);
            mRightBtn.setTextColor(mRightBtnCancelColor);
            mRightBtn.setBackgroundResource(R.drawable.btn_cancel);
        }
    }
}
