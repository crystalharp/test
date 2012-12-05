/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;

import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.DataQuery.DiscoverResponse.DiscoverCategoryList.DiscoverCategory;

/**
 * @author Peng Wenyue
 */
public class DiscoverCategoryView extends LinearLayout {

    public static final int TYPE_TUANGOU = 1;
    public static final int TYPE_DIANYING = 2;
    public static final int TYPE_YANCHU = 3;
    public static final int TYPE_ZHANLAN = 4;
    
    private Sphinx mSphinx;
    private ImageView mPictureImv;
    private TextView mNearbyNumTxv;
    private TextView mCityNumTxv;
    private ImageView mBubbleImv;
    private ProgressBar mProgressBar;
    
    private String mDataType;
    private DiscoverCategory mDiscoverCategory;
    private String mTitleText;
    
    private Runnable mLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            mSphinx.getHandler().removeCallbacks(mActualLoadedDrawableRun);
            mSphinx.getHandler().post(mActualLoadedDrawableRun);
        }
    };
    
    private Runnable mActualLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            TKDrawable tkDrawable = mDiscoverCategory.getTKDrawable();
            if (tkDrawable != null) {
                Drawable drawable = tkDrawable.loadDrawable(null, null, null);
                if (drawable != null) {
                    mPictureImv.setTag(R.id.view_invalid, "1");
                    mPictureImv.setImageDrawable(drawable);
                }
            }
        }
    };

    public DiscoverCategoryView(Sphinx sphinx) {
        super(sphinx);
        mSphinx = sphinx;
        LayoutInflater layoutInflater = mSphinx.getLayoutInflater();
        layoutInflater.inflate(R.layout.discover_category, this, true);
        findViews();
        setListener();
    }

    protected void findViews() {
        mPictureImv = (ImageView)findViewById(R.id.picture_imv);
        mNearbyNumTxv = (TextView)findViewById(R.id.nearby_num_txv);
        mCityNumTxv = (TextView)findViewById(R.id.city_num_txv);
        mBubbleImv = (ImageView)findViewById(R.id.bubble_imv);
        mProgressBar = (ProgressBar)findViewById(R.id.progress_prb);
    }

    protected void setListener() {
    }
    
    public void onResume() {
        setData(mDiscoverCategory);
    }
    
    public void onPause() {
        
    }
    
    public void dismiss() {
        mPictureImv.setImageDrawable(null);
    }

    public void setup(String dataType) {
        mDataType = dataType;
        mDiscoverCategory = null;
        refreshContent();
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            mBubbleImv.setBackgroundResource(R.drawable.ic_discover_tuangou);
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            mBubbleImv.setBackgroundResource(R.drawable.ic_discover_dianying);
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            mBubbleImv.setBackgroundResource(R.drawable.ic_discover_yanchu);
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            mBubbleImv.setBackgroundResource(R.drawable.ic_discover_zhanlan);
        } 
        mNearbyNumTxv.setText(formatStr(mSphinx.getString(R.string.nearby_, "12")));
        mNearbyNumTxv.setVisibility(View.INVISIBLE);
        mCityNumTxv.setText(" ");
        layoutBubbleImv();
        mProgressBar.setVisibility(View.VISIBLE);
    }
    
    public void setData(DiscoverCategory discoverCategory) {
        if (discoverCategory == null) {
            return;
        }
        mDiscoverCategory = discoverCategory;
        mDataType = mDiscoverCategory.getType();
        
        refreshContent();
        
        long numNearby = mDiscoverCategory.getNumNearby();
        if (numNearby > 0) {
            mNearbyNumTxv.setText(formatStr(mSphinx.getString(R.string.nearby_, numNearby)));
            mCityNumTxv.setText(mSphinx.getString(R.string.entire_city_, mDiscoverCategory.getNumCity()));
        } else {
            mNearbyNumTxv.setText(formatStr(mSphinx.getString(R.string.entire_city_, mDiscoverCategory.getNumCity())));
            mCityNumTxv.setText(" ");
        }
        mNearbyNumTxv.setVisibility(View.VISIBLE);
        layoutBubbleImv();
        mProgressBar.setVisibility(View.INVISIBLE);
    }
    
    private void layoutBubbleImv() {
        mNearbyNumTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int width = mNearbyNumTxv.getMeasuredWidth();
        ((RelativeLayout.LayoutParams) mBubbleImv.getLayoutParams()).leftMargin = width+Util.dip2px(Globals.g_metrics.density, 20);
    }
    
    private SpannableStringBuilder formatStr(String str) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);
        spannableStringBuilder.setSpan(new ForegroundColorSpan(0xffff6600),2,str.length()-1,Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        return spannableStringBuilder;
    }
    
    private void refreshContent() {
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            mTitleText = mSphinx.getString(R.string.tuangou);
            refreshPicture(R.drawable.ic_discover_home_tuangou_focused, R.drawable.ic_discover_home_tuangou_disabled);
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            mTitleText = mSphinx.getString(R.string.dianying);
            refreshPicture(R.drawable.ic_discover_home_dianying_focused, R.drawable.ic_discover_home_dianying_disabled);
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            mTitleText = mSphinx.getString(R.string.yanchu);
            refreshPicture(R.drawable.ic_discover_home_yanchu_focused, R.drawable.ic_discover_home_yanchu_disabled);
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            mTitleText = mSphinx.getString(R.string.zhanlan);
            refreshPicture(R.drawable.ic_discover_home_zhanlan_focused, R.drawable.ic_discover_home_zhanlan_disabled);
        }
    }
    
    private void refreshPicture(int focusedResId, int disabledResId) {
        boolean isConnectionFast = Globals.isConnectionFast();
        if (isConnectionFast == false) {
            mPictureImv.setImageResource(focusedResId);
        } else {
            if (mDiscoverCategory == null) {
                mPictureImv.setImageResource(disabledResId);
            } else {
                TKDrawable tkDrawable = mDiscoverCategory.getTKDrawable();
                if (tkDrawable != null) {
                    Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, mSphinx.getDiscoverFragment().toString());
                    if (drawable != null) {
                        mPictureImv.setImageDrawable(drawable);
                    } else {
                        Object tag = mPictureImv.getTag(R.id.view_invalid);
                        if (tag == null) {
                            mPictureImv.setImageResource(disabledResId);
                        }
                    }
                } else {
                    mPictureImv.setImageResource(disabledResId);
                }
            }
        }
    }
    
    public String getTitleText() {
        return mTitleText;
    }
}
