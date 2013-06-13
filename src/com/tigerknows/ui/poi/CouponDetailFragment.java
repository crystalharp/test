/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.Coupon;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.ui.BaseFragment;

/**
 * @author Peng Wenyue, and Feng Tianxiao
 */
public class CouponDetailFragment extends BaseFragment {
    
    public CouponDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    protected Runnable mLoadedDrawableRun = new Runnable() {
            
            @Override
            public void run() {
                mSphinx.getHandler().removeCallbacks(mActualLoadedDrawableRun);
                mSphinx.getHandler().post(mActualLoadedDrawableRun);
            }
        };
    
    protected Runnable mActualLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            setPic(mData.getHintPicTKDrawable(), mHintImv, false);
            setPic(mData.getDetailPicTKDrawable(), mDetailImv, false);
            setPic(mData.getLogoTKDrawable(), mLogoImv, false);
        }
    };

    private ImageView mHintImv;
    
    private TextView mPOINameTxv = null;

    private TextView mDescriptionTxv = null;
    
    private TextView mHotTxv = null;

    private TextView mDetailTxv = null;
    
    private ImageView mDetailImv = null;
    
    private ImageView mLogoImv = null;
    
    private TextView mRemarkTxv = null;
    
    private Coupon mData = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIHomeInputQuery;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_coupon_detail, container, false);

        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.search);
        mRightBtn.setVisibility(View.INVISIBLE);
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }

    protected void findViews() {
        mHintImv = (ImageView) mRootView.findViewById(R.id.hint_imv);
        mPOINameTxv = (TextView) mRootView.findViewById(R.id.name_txv);
        mDescriptionTxv = (TextView) mRootView.findViewById(R.id.description_txv);
        mHotTxv = (TextView) mRootView.findViewById(R.id.hot_txv);
        mDetailTxv = (TextView) mRootView.findViewById(R.id.detail_txv);
        mLogoImv = (ImageView) mRootView.findViewById(R.id.logo_imv);
        mDetailImv = (ImageView) mRootView.findViewById(R.id.detail_imv);
        mRemarkTxv = (TextView) mRootView.findViewById(R.id.remark_txv);
    }

    protected void setListener() {
    }
    
    public void setData(Coupon coupon) {
        mData = coupon;

        setPic(mData.getHintPicTKDrawable(), mHintImv, true);
        setPic(mData.getDetailPicTKDrawable(), mDetailImv, true);
        setPic(mData.getLogoTKDrawable(), mLogoImv, true);
        
        mPOINameTxv.setText(mData.getListName());
        mDescriptionTxv.setText(coupon.getDescription());
        mHotTxv.setText(mSphinx.getString(R.string._used_sum_times, coupon.getHot()));
        mDetailTxv.setText(coupon.getDetail().replace("N_Line", "\n"));
        
        mRemarkTxv.setText(coupon.getRemark());
    }
    
    void setPic(TKDrawable tkDrawable, ImageView imageView, boolean reload) {
        if (tkDrawable != null) {
            Drawable drawable;
            if (reload) {
                drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, this.toString());
            } else {
                drawable = tkDrawable.loadDrawable(null, null, null);
            }
            if(drawable != null) {
                imageView.setBackgroundDrawable(drawable);
            } else {
                imageView.setBackgroundResource(R.drawable.bg_picture_coupon_detail_reload);
            }
        } else {
            //imageView.setBackgroundDrawable(null);
        }
    }
}
