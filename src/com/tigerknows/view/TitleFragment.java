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

    protected Button titleBtn;
    
    protected ImageView leftImv;
    
    protected ImageView rightImv;
    
    protected Button leftBtn;
    
    protected Button rightBtn;
    
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
        leftImv = (ImageView) mRootView.findViewById(R.id.left_imv);
        rightImv = (ImageView) mRootView.findViewById(R.id.right_imv);
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

    public ImageView getLeftBtn() {
        return leftImv;
    }

    public ImageView getRightBtn() {
        return rightImv;
    }

    public Button getLeftTxv() {
        return leftBtn;
    }

    public Button getRightTxv() {
        return rightBtn;
    }
}
