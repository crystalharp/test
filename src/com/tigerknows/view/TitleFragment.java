/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author Peng Wenyue
 */
public class TitleFragment extends BaseFragment implements View.OnClickListener {
    
    public TitleFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    protected TextView titleTxv;
    
    protected ImageView leftBtn;
    
    protected ImageView rightBtn;
    
    protected Button leftTxv;
    
    protected Button rightTxv;
    
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
        titleTxv = (TextView) mRootView.findViewById(R.id.title_txv);
        leftBtn = (ImageView) mRootView.findViewById(R.id.left_btn);
        rightBtn = (ImageView) mRootView.findViewById(R.id.right_btn);
        leftTxv = (Button) mRootView.findViewById(R.id.left_txv);
        rightTxv = (Button) mRootView.findViewById(R.id.right_txv);
    }

    protected void setListener() {
        leftTxv.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.left_txv:
                // 关闭顶部的Fragment，显示第二个Fragment
                break;

            default:
                break;
        }
    }

    public TextView getTitleTxv() {
        return titleTxv;
    }

    public ImageView getLeftBtn() {
        return leftBtn;
    }

    public ImageView getRightBtn() {
        return rightBtn;
    }

    public Button getLeftTxv() {
        return leftTxv;
    }

    public Button getRightTxv() {
        return rightTxv;
    }
}
