/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.Coupon;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.ui.discover.BaseDetailView;

/**
 * @author Peng Wenyue
 */
public class CouponDetailView extends BaseDetailView {
    
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
            refreshDrawable();
        }
    };
    
    private ImageView mHintImv;
    
    private TextView mNameTxv = null;

    private TextView mDescriptionTxv = null;
    
    private TextView mHotTxv = null;

    private TextView mDetailTxv = null;
    
    private ImageView mDetailImv = null;
    
    private View mLogoView = null;
    
    private ImageView mLogoImv = null;
    
    private TextView mRemarkTxv = null;
    
    private Coupon mData = null;
    
    public CouponDetailView(Sphinx sphinx, CouponDetailFragment parentFragment) {
        super(sphinx, parentFragment, R.layout.poi_coupon_detail);
        
        findViews();
    }

    protected void findViews() {
        super.findViews();
        mHintImv = (ImageView) findViewById(R.id.hint_imv);
        mNameTxv = (TextView) findViewById(R.id.name_txv);
        mDescriptionTxv = (TextView) findViewById(R.id.description_txv);
        mHotTxv = (TextView) findViewById(R.id.hot_txv);
        mDetailTxv = (TextView) findViewById(R.id.detail_txv);
        mLogoView = findViewById(R.id.logo_view);
        mLogoImv = (ImageView) findViewById(R.id.logo_imv);
        mDetailImv = (ImageView) findViewById(R.id.detail_imv);
        mRemarkTxv = (TextView) findViewById(R.id.remark_txv);
    }
    
    protected void setListener() {
        super.setListener();
    }
    
    public void setData(Coupon coupon, int position) {
        super.setData(coupon, position);
        mData = coupon;
        
        mNameTxv.setText(mData.getListName());
        mDescriptionTxv.setText(coupon.getDescription());
        mHotTxv.setText(mSphinx.getString(R.string._used_sum_times, coupon.getHot()));
        mDetailTxv.setText(coupon.getDetail());
        
        mRemarkTxv.setText(coupon.getRemark());
        
    }
    
    protected void refreshDrawable() {
        refreshDrawable(mData.getHintPicTKDrawable(), mHintImv, R.drawable.icon, true);
        refreshDrawable(mData.getDetailPicTKDrawable(), mDetailImv, R.drawable.bg_picture_coupon_detail, false);
        boolean loadedLogo = refreshDrawable(mData.getLogoTKDrawable(), mLogoImv, R.drawable.icon, true);
        if (loadedLogo) {
            mLogoView.setVisibility(View.VISIBLE);
        } else {
            mLogoView.setVisibility(View.GONE);
        }
    }
    
    boolean refreshDrawable(TKDrawable tkDrawable, ImageView imageView, int defaultResId, boolean isVisibility) {
        boolean result = false;
        if (tkDrawable != null) {
            Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, mParentFragment.toString());
            if(drawable != null) {
                result = true;
                imageView.setBackgroundDrawable(drawable);
            } else if (defaultResId != R.drawable.icon) {
                imageView.setBackgroundResource(defaultResId);
            } else {
                imageView.setBackgroundDrawable(null);
            }
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setBackgroundDrawable(null);
            if (isVisibility) {
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }
        }
        
        return result;
    }
}
