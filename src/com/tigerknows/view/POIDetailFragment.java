/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tigerknows.view.user.User;
import com.tigerknows.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.BaseActivity;
import com.tigerknows.POICommentList;
import com.tigerknows.POIComment;
import com.tigerknows.POIErrorRecovery;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Comment;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.model.DataOperation.FendianQueryResponse;
import com.tigerknows.model.DataOperation.POIQueryResponse;
import com.tigerknows.model.DataOperation.TuangouQueryResponse;
import com.tigerknows.model.DataOperation.YanchuQueryResponse;
import com.tigerknows.model.DataOperation.ZhanlanQueryResponse;
import com.tigerknows.model.DataQuery.CommentResponse;
import com.tigerknows.model.DataQuery.CommentResponse.CommentList;
import com.tigerknows.model.POI.Description;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.ShareTextUtil;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.util.WidgetUtils;

/**
 * @author Peng Wenyue
 * <ul>
 * 类型 价格 人气 口味 环境 服务 推荐菜
 * </ul>
 * 
 */
public class POIDetailFragment extends BaseFragment implements View.OnClickListener, OnTouchListener {
    
    public POIDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private ScrollView mBodyScv;

    private TextView mNameTxt = null;
    
    private ImageView mStampImv = null;
    
    private ImageView mStampBigImv = null;

    private Button mShareBtn = null;
    
    private TextView mCategoryTxv;

    private TextView mMoneyTxv;

    private TextView mDistanceTxv;
    
    private TextView mDistanceFromTxv;
    
    private Drawable mIcAPOI;
    
    private String mDistance;
    
    private String mDistanceA;

    private RatingBar mStartsRtb;

    private LinearLayout mFeatureTxv;

    // Error Fix Button
    private Button mErrorFixBtn = null;
    
    private TextView mDescriptionTxv = null;

    private ViewGroup mCommentListView = null;
    
    private ViewGroup mCommentSumTotalView;
    
    private Button mFavoriteBtn;
    
    private Button mPOIBtn;
    
    private POI mPOI;
    
    private View mAddressAndPhoneView = null;
    
    private View mAddressView = null;
    
    private TextView mAddressTxv = null;
    
    private ImageView mAddressTelephoneDividerImv = null;
    
    private View mTelephoneView = null;
    
    private TextView mTelephoneTxv = null;
    
    private View mCommentTipView;
    
    private LinearLayout mDynamicPOIListView;
    
    private Button mCommentTipEdt;
    
    private View mLoadingView;
    
    private Animation mStampAnimation;
    
    public boolean isReLogin() {
        boolean isRelogin = this.isReLogin;
        this.isReLogin = false;
        if (isRelogin) {
            if (mBaseQuerying != null) {
                for(BaseQuery baseQuery : mBaseQuerying) {
                    baseQuery.setResponse(null);
                }
                mTkAsyncTasking = mSphinx.queryStart(mBaseQuerying);
            }
        }
        return isRelogin;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIDetail;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_detail, container, false);
        findViews();
        setListener();
        
        mStampAnimation = AnimationUtils.loadAnimation(mSphinx, R.anim.stamp);
        mStampAnimation.setAnimationListener(new AnimationListener() {
            
            @Override
            public void onAnimationStart(Animation arg0) {
            }
            
            @Override
            public void onAnimationRepeat(Animation arg0) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onAnimationEnd(Animation arg0) {
                mStampBigImv.setVisibility(View.GONE);
                refreshDetail();
                refreshComment();
            }
        });

        Resources resources = mSphinx.getResources();
        mIcAPOI = resources.getDrawable(R.drawable.ic_a_poi);
        mDistance = mSphinx.getString(R.string.distance);
        mDistanceA = mSphinx.getString(R.string.distanceA);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSphinx.showHint(TKConfig.PREFS_HINT_POI_DETAIL, R.layout.hint_poi_detail);
        mTitleBtn.setText(R.string.detail_info);
        mRightBtn.setBackgroundResource(R.drawable.btn_view_map);
        mRightBtn.setOnClickListener(this); 
        
        if (isReLogin()) {
            return;
        }
        POI poi = mPOI;
        if (poi != null) {
            if (poi.getStatus() < POI.STATUS_NONE) {
                BaseActivity.showErrorDialog(mSphinx, mSphinx.getString(R.string.response_code_603), this, true);
            }
        }  
        mBodyScv.smoothScrollTo(0, 0);
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
    }
    
    public void refreshDetail() {
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
        String category = poi.getCategory().trim();
        setFavoriteState(mFavoriteBtn, poi.checkFavorite(mContext));
        mNameTxt.setText(poi.getName());

        if (poi.isGoldStamp()) {
            mStampImv.setBackgroundResource(R.drawable.ic_stamp_gold);
            mStampImv.setVisibility(View.VISIBLE);
            mCommentTipEdt.setHint(R.string.comment_tip_hit1);
        } else if (poi.isSilverStamp()) {
            mStampImv.setBackgroundResource(R.drawable.ic_stamp_silver);
            mStampImv.setVisibility(View.VISIBLE);
            mCommentTipEdt.setHint(R.string.comment_tip_hit1);
        } else {
            mStampImv.setVisibility(View.GONE);
            mCommentTipEdt.setHint(R.string.comment_tip_hit0);
        }

        float star = poi.getGrade();
        mStartsRtb.setRating(star/2.0f);
        
        if (!TextUtils.isEmpty(category)) {
            mCategoryTxv.setText(category);
        } else {
            mCategoryTxv.setText("");
        }
        
        int money = poi.getPerCapity();
        if (money > -1) {
            mMoneyTxv.setText(mContext.getString(R.string.yuan, money));
            mMoneyTxv.setVisibility(View.VISIBLE);
        } else {
            mMoneyTxv.setVisibility(View.GONE);
        }
        
        String distance = poi.getToCenterDistance();
        if (!TextUtils.isEmpty(distance)) {
            if (distance.startsWith(mDistanceA)) {
                mIcAPOI.setBounds(0, 0, mIcAPOI.getIntrinsicWidth(), mIcAPOI.getIntrinsicHeight());
                mDistanceFromTxv.setCompoundDrawables(null, null, mIcAPOI, null);
                mDistanceFromTxv.setText(mDistance);
                mDistanceTxv.setText(distance.replace(mDistanceA, ""));
            } else {
                mDistanceFromTxv.setText("");
                mDistanceFromTxv.setCompoundDrawables(null, null, null, null);
                mDistanceTxv.setText(distance);
            }
        } else {
            mDistanceFromTxv.setText("");
            mDistanceFromTxv.setCompoundDrawables(null, null, null, null);
            mDistanceTxv.setText("");
        }
        
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)mMoneyTxv.getLayoutParams();
        SpannableStringBuilder description = getDescription();
        if (description.length() > 0) {
            mDescriptionTxv.setText(description);
            mDescriptionTxv.setVisibility(View.VISIBLE);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
        } else {
            mDescriptionTxv.setVisibility(View.GONE);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
        }
        
        /**
         * 若地址或电话为空, 则不显示
         */
        String address = poi.getAddress();
        String telephone = poi.getTelephone();
        if (!TextUtils.isEmpty(telephone) || !TextUtils.isEmpty(address)) {
            
            if (!TextUtils.isEmpty(address)) {
                mAddressView.setVisibility(View.VISIBLE);
                mAddressTxv.setText(address);
                if (TextUtils.isEmpty(telephone)) {
                    mAddressView.setBackgroundResource(R.drawable.list_single);
                    mAddressTelephoneDividerImv.setVisibility(View.GONE);
                } else {
                    mAddressView.setBackgroundResource(R.drawable.list_header);
                    mAddressTelephoneDividerImv.setVisibility(View.VISIBLE);
                }
            } else {
                mAddressView.setVisibility(View.GONE);
            }
            
            if (!TextUtils.isEmpty(telephone)) {
                mTelephoneView.setVisibility(View.VISIBLE);
                mTelephoneTxv.setText(telephone.replace("|", mSphinx.getString(R.string.dunhao)));
                
                if (TextUtils.isEmpty(address)) {
                    mTelephoneView.setBackgroundResource(R.drawable.list_single);
                    mAddressTelephoneDividerImv.setVisibility(View.GONE);
                } else {
                    mTelephoneView.setBackgroundResource(R.drawable.list_footer);
                    mAddressTelephoneDividerImv.setVisibility(View.VISIBLE);
                }
            } else {
                mTelephoneView.setVisibility(View.GONE);
            }

            mAddressAndPhoneView.setVisibility(View.VISIBLE);
        } else {
            mAddressAndPhoneView.setVisibility(View.GONE);
        }
        
        makeFeature(mFeatureTxv);
        
        List<DynamicPOI> list = poi.getDynamicPOIList();
        int viewCount = mDynamicPOIListView.getChildCount();
        int viewIndex = 0;
        int size = list.size();
        if(size==0){
        	mDynamicPOIListView.setVisibility(View.GONE);
        }else{
        	mDynamicPOIListView.setVisibility(View.VISIBLE);
        }
        for(int i = 0; i < size; i++) {
            final DynamicPOI dynamicPOI = list.get(i);
            final String dataType = dynamicPOI.getType();
            if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType) ||
                    BaseQuery.DATA_TYPE_YANCHU.equals(dataType) ||
                    BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                View child;
                if (viewIndex < viewCount) {
                    child = mDynamicPOIListView.getChildAt(viewIndex);
                    child.setVisibility(View.VISIBLE);
                } else {
                    child = mLayoutInflater.inflate(R.layout.dynamic_poi_list_item, mDynamicPOIListView, false);
                    mDynamicPOIListView.addView(child, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                }
                child.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View view) {
                        DataOperation dataOperation = new DataOperation(mSphinx);
                        Hashtable<String, String> criteria = new Hashtable<String, String>();
                        criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, dataType);
                        criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                        criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamicPOI.getMasterUid());
                        
                        if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
                            mActionLog.addAction(mActionTag +  ActionLog.POIDetailTuangou);
                            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD,
                                         Tuangou.NEED_FILELD
                                            + Util.byteToHexString(Tuangou.FIELD_NOTICED)
                                            + Util.byteToHexString(Tuangou.FIELD_CONTENT_TEXT)
                                            + Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC));
                            criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
                                    Util.byteToHexString(Tuangou.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_LIST)+"_[0]" + ";" +
                                    Util.byteToHexString(Tuangou.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_DETAIL)+"_[0]" + ";" +
                                    Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_TAOCAN)+"_[0]");
                            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), POIDetailFragment.this.getId(), POIDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
                            List<BaseQuery> list = new ArrayList<BaseQuery>();
                            list.add(dataOperation);
                            dataOperation = new DataOperation(mSphinx);
                            criteria = new Hashtable<String, String>();
                            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                            criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_FENDIAN);
                            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamicPOI.getSlaveUid());
                            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, Fendian.NEED_FILELD);
                            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), POIDetailFragment.this.getId(), POIDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
                            list.add(dataOperation);
                            mTkAsyncTasking = mSphinx.queryStart(list);
                            mBaseQuerying = list;
                        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dataType)) {
                            mActionLog.addAction(mActionTag +  ActionLog.POIDetailYanchu);
                            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD,
                                         Yanchu.NEED_FILELD + Util.byteToHexString(Yanchu.FIELD_DESCRIPTION));
                            criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
                                    Util.byteToHexString(Yanchu.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
                                    Util.byteToHexString(Yanchu.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
                            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), POIDetailFragment.this.getId(), POIDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
                            List<BaseQuery> list = new ArrayList<BaseQuery>();
                            list.add(dataOperation);
                            mTkAsyncTasking = mSphinx.queryStart(list);
                            mBaseQuerying = list;
                        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                            mActionLog.addAction(mActionTag +  ActionLog.POIDetailZhanlan);
                            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD,
                                    Zhanlan.NEED_FILELD + Util.byteToHexString(Zhanlan.FIELD_DESCRIPTION));
                            criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
                                    Util.byteToHexString(Zhanlan.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
                                    Util.byteToHexString(Zhanlan.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
                            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), POIDetailFragment.this.getId(), POIDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
                            List<BaseQuery> list = new ArrayList<BaseQuery>();
                            list.add(dataOperation);
                            mTkAsyncTasking = mSphinx.queryStart(list);
                            mBaseQuerying = list;
                        }
                    }
                });
                ImageView iconImv = (ImageView) child.findViewById(R.id.icon_imv);
                TextView textTxv = (TextView) child.findViewById(R.id.text_txv);
                if (BaseQuery.DATA_TYPE_TUANGOU.equals(dynamicPOI.getType())) {
                    iconImv.setImageResource(R.drawable.ic_dynamicpoi_tuangou);
                } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dynamicPOI.getType())) {
                    iconImv.setImageResource(R.drawable.ic_dynamicpoi_yanchu);
                } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dynamicPOI.getType())) {
                    iconImv.setImageResource(R.drawable.ic_dynamicpoi_zhanlan);
                }
                textTxv.setText(dynamicPOI.getSummary());
                viewIndex++;
            }
        }
        
        viewCount = mDynamicPOIListView.getChildCount();
        for(int i = viewIndex; i < viewCount; i++) {
            mDynamicPOIListView.getChildAt(i).setVisibility(View.GONE);
        }
        
        for(int i = 0; i < viewIndex; i++) {
            View child = mDynamicPOIListView.getChildAt(i);
            if (i == 0) {
                if (viewIndex == 1) {
                    child.setBackgroundResource(R.drawable.list_single);
                    child.findViewById(R.id.list_separator_imv).setVisibility(View.GONE);
                } else {
                    child.setBackgroundResource(R.drawable.list_header);
                    child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
                }
            } else if (i == (viewIndex-1)) {
                child.setBackgroundResource(R.drawable.list_footer);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.GONE);
            } else {
                child.setBackgroundResource(R.drawable.list_middle);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
            }
        }
    }
    
    public void refreshStamp() {
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
        int resId = Integer.MIN_VALUE;
        if (poi.isGoldStamp()) {
            resId = R.drawable.ic_stamp_gold_big;
        } else if (poi.isSilverStamp()) {
            resId = R.drawable.ic_stamp_silver_big;
        }
        if (resId != Integer.MIN_VALUE) {
            mStampAnimation.reset();
            mStampBigImv.setAnimation(null);
            mStampBigImv.setBackgroundResource(resId);
            mStampBigImv.setVisibility(View.VISIBLE);
            mStampBigImv.startAnimation(mStampAnimation);
        }
    }
    
    public boolean refreshComment() {
        boolean result = false;
        mCommentTipView.setVisibility(View.GONE);
        int count = mCommentListView.getChildCount();
        for(int i = 2; i < count; i++) {
            mCommentListView.getChildAt(i).setVisibility(View.GONE);
        }
        mCommentListView.setVisibility(View.GONE);
        
        POI poi = mPOI;
        Comment lastComment = null;
        Comment myComment = null;
        if (poi != null && poi.getCommentQuery() != null) {

            CommentResponse commentResponse = (CommentResponse) poi.getCommentQuery().getResponse();
            CommentList commentList = commentResponse.getList();
            if (commentList != null) {
                List<Comment> list = commentList.getList();
                if (list != null) {
                    for(int i = 0, size = list.size(); i < size; i++) {
                        Comment comment = list.get(i);
                        if (Comment.isAuthorMe(comment) > 0) {
                            poi.setMyComment(comment);
                            myComment = comment;
                        } else if (lastComment == null){
                            lastComment = comment;
                        }
                    }
                }
            }
            mCommentTipView.setVisibility(View.VISIBLE);
        }
        
        if (lastComment == null) {
            lastComment = myComment;
        }
        
        if (lastComment != null) {
            result = true;
            mCommentListView.setVisibility(View.VISIBLE);
            View commentView = null;
            if (count == 3) {
                View view = mCommentListView.getChildAt(2);
                view.setVisibility(View.VISIBLE);
                commentView = getCommentItemView(view, mCommentListView, lastComment, poi);
            } else {
                commentView = getCommentItemView(null, mCommentListView, lastComment, poi);
                mCommentListView.addView(commentView);
            }
            commentView.setBackgroundResource(R.drawable.list_middle);
        }
        
        if (mCommentListView.getVisibility() == View.VISIBLE) {
            mCommentTipView.setBackgroundResource(R.drawable.list_footer);
        } else {
            mCommentTipView.setBackgroundResource(R.drawable.list_single);
        }
        return result;
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }

    protected void findViews() {
        mBodyScv = (ScrollView) mRootView.findViewById(R.id.body_scv);
        mNameTxt = (TextView)mRootView.findViewById(R.id.name_txv);
        mStampImv = (ImageView) mRootView.findViewById(R.id.stamp_imv);
        mStampBigImv = (ImageView) mRootView.findViewById(R.id.stamp_big_imv);
        mStartsRtb = (RatingBar) mRootView.findViewById(R.id.stars_rtb);
        mCategoryTxv = (TextView) mRootView.findViewById(R.id.category_txv);
        mMoneyTxv = (TextView) mRootView.findViewById(R.id.money_txv);
        mDescriptionTxv = (TextView)mRootView.findViewById(R.id.description_txv);
        mDistanceTxv = (TextView)mRootView.findViewById(R.id.distance_txv);
        mDistanceFromTxv = (TextView) mRootView.findViewById(R.id.distance_from_txv);
        
        mPOIBtn = (Button) mRootView.findViewById(R.id.poi_btn);
        mShareBtn = (Button)mRootView.findViewById(R.id.share_btn);
        mFavoriteBtn = (Button)mRootView.findViewById(R.id.favorite_btn);
        // Error Fix
        mErrorFixBtn = (Button)mRootView.findViewById(R.id.error_recovery_btn);
        
        mFeatureTxv = (LinearLayout)mRootView.findViewById(R.id.feature_txv);

        mCommentListView = (ViewGroup)mRootView.findViewById(R.id.comment_list_view);
        mCommentSumTotalView = (ViewGroup) mRootView.findViewById(R.id.comment_sum_total_view);
        
        mAddressAndPhoneView = mRootView.findViewById(R.id.address_phone_view);
        mAddressView = mRootView.findViewById(R.id.address_view);
        mAddressTxv = (TextView)mRootView.findViewById(R.id.address_txv);
        mAddressTelephoneDividerImv = (ImageView) mRootView.findViewById(R.id.address_telephome_divider_imv);
        mTelephoneView = mRootView.findViewById(R.id.telephone_view);
        mTelephoneTxv = (TextView)mRootView.findViewById(R.id.telephone_txv);
        
        mDynamicPOIListView = (LinearLayout) mRootView.findViewById(R.id.dynamic_poi_list_view);
        
        mCommentTipView = mRootView.findViewById(R.id.comment_tip_view);
        mCommentTipEdt = (Button) mRootView.findViewById(R.id.comment_tip_btn);
        mLoadingView = mRootView.findViewById(R.id.loading_view);
    }

    protected void setListener() {
        mShareBtn.setOnClickListener(this);
        mFavoriteBtn.setOnClickListener(this);
        mTelephoneView.setOnClickListener(this);
        mAddressView.setOnClickListener(this);
        mPOIBtn.setOnClickListener(this);
        mErrorFixBtn.setOnClickListener(this);
        mCommentSumTotalView.setOnClickListener(this);
        mCommentTipView.setOnTouchListener(this);
        mCommentTipEdt.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                POI poi = mPOI;
                if (poi == null) {
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mActionLog.addAction(mActionTag +  ActionLog.POIDetailInput);
                    boolean isMe = (poi.isGoldStamp() || poi.isSilverStamp());
                    if (poi.getStatus() < 0) {
                        int resId;
                        if (isMe) {
                            resId = R.string.poi_comment_poi_invalid_not_update;
                        } else {
                            resId = R.string.poi_comment_poi_invalid_not_create;
                        }

                        CommonUtils.showNormalDialog(mSphinx, 
                                mSphinx.getString(resId));
                    } else {
//                        if (isMe) {
//                            if (poi.getMyComment() != null) {
//                                POIComment.setPOI(poi, getId(), POIComment.STATUS_MODIFY);
//                                mSphinx.showView(R.id.activity_poi_comment);
//                            
//                            // 查询我的点评
//                            } else {
//                                Hashtable<String, String> criteria = new Hashtable<String, String>();
//                                criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANPING);
//                                criteria.put(DataQuery.SERVER_PARAMETER_POI_ID, poi.getUUID());
//                                criteria.put(DataQuery.SERVER_PARAMETER_REFER, DataQuery.REFER_POI);
//                                criteria.put(DataQuery.SERVER_PARAMETER_SIZE, "1");
//                                DataQuery commentQuery = new DataQuery(mSphinx);
//                                commentQuery.setup(criteria, Globals.g_Current_City_Info.getId(), getId(), getId(), mSphinx.getString(R.string.doing_and_wait), false, false, poi);
//                                List<BaseQuery> baseQueryList = new ArrayList<BaseQuery>();
//                                baseQueryList.add(commentQuery);
//                                mSphinx.queryStart(baseQueryList);
//                            }
//                        } else {
                            POIComment.setPOI(poi, getId(), poi.getMyComment() != null ? POIComment.STATUS_MODIFY : POIComment.STATUS_NEW);
                            mSphinx.showView(R.id.activity_poi_comment);
//                        }
                    }
                }
                return false;
            }
        });
    }

    public void onClick(View view) {
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
        switch (view.getId()) {                
            case R.id.right_btn:
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                viewMap();
                break;
            case R.id.telephone_view:
                mActionLog.addAction(mActionTag +  ActionLog.CommonTelphone);
                CommonUtils.telephone(mSphinx, mTelephoneTxv);
                break;
                
            case R.id.address_view:
                mActionLog.addAction(mActionTag +  ActionLog.CommonAddress);             
                CommonUtils.queryTraffic(mSphinx, poi, mActionTag);
                break;
                
            case R.id.poi_btn:
                mActionLog.addAction(mActionTag +  ActionLog.POIDetailSearch);
                mSphinx.getPOINearbyFragment().setData(poi);
                mSphinx.showView(R.id.view_poi_nearby);
                break;
                
            case R.id.share_btn:
                mActionLog.addAction(mActionTag +  ActionLog.CommonShare);
                share();
                break;

            case R.id.favorite_btn:
                favorite();
                break;
            case R.id.error_recovery_btn:
                // Error Recovery
                mActionLog.addAction(mActionTag +  ActionLog.CommonErrorRecovery);
                errorRecovery();
                break;
                
            case R.id.comment_sum_total_view:
                mActionLog.addAction(mActionTag +  ActionLog.POIDetailAllComment);
                showMoreComment();
                break;
                
            default:
                mActionLog.addAction(mActionTag +  ActionLog.POIDetailComment);
                showMoreComment();
                break;
        }
    }
    
    public void viewMap() {
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
        List<POI> pois = new ArrayList<POI>();
        pois.add(poi);
        mSphinx.showPOI(pois, 0);
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.result_map), ActionLog.POIDetailMap);
        mSphinx.showView(R.id.view_result_map);
    }
    
    public void favorite() {
        final POI poi = mPOI;
        if (poi == null) {
            return;
        }
        boolean isFavorite = poi.checkFavorite(mContext);
        mActionLog.addAction(mActionTag +  ActionLog.CommonFavorite, String.valueOf(isFavorite));
        if (isFavorite) {
            CommonUtils.showNormalDialog(mSphinx, 
                    mContext.getString(R.string.favorite_yet), 
                    mContext.getString(R.string.cancel_favorite_tip),
                    mContext.getString(R.string.yes),
                    mContext.getString(R.string.no),
                    new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface arg0, int id) {
                            if (id == DialogInterface.BUTTON_POSITIVE) {
                                poi.deleteFavorite(mContext);
                                setFavoriteState(mFavoriteBtn, false);
                            }
                        }
                    });
        } else {
            poi.writeToDatabases(mContext, -1, Tigerknows.STORE_TYPE_FAVORITE);
            setFavoriteState(mFavoriteBtn, true);
            Toast.makeText(mSphinx, R.string.favorite_toast, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 设置收藏按钮的状态
     * 1."未收藏"状态, 图标为'中空五角星', 文字为"未收藏"
     * 2."收藏"状态, 图标为"实心五角星", 文字为"收藏"
     * 
     * @param button
     * @param favoriteYet
     */
    private void setFavoriteState(Button button, boolean favoriteYet) {
    	if (favoriteYet) {
    		mFavoriteBtn.setBackgroundResource(R.drawable.btn_cancel_favorite);
    	} else {
            mFavoriteBtn.setBackgroundResource(R.drawable.btn_favorite);
    	}
    }
    
    public void errorRecovery() {
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
        POIErrorRecovery.addTarget(poi);
        mSphinx.showView(R.id.activity_poi_error_recovery);
    }
    
    /**
     * 
     * 弹出分享内容 对话框
     * 
     * 1.短信内容由根据POI生成
     * 2.微博内容调用ShareTextUtil生成
     * 3.根据传入的数据(data)及图层类型(layerType)截屏
     * 
     */
    public void share() {
        POI poi = mPOI;
        if(poi == null)
            return;

        String smsContent = ShareTextUtil.sharePOISmsContent(poi,mContext);
    	String weiboContent = ShareTextUtil.sharePOIWeiboContent(poi, mContext);
    	String qzoneContent = ShareTextUtil.sharePOIQzoneContent(poi, mContext);
    	
    	List<POI> pois = new ArrayList<POI>();
    	pois.add(poi);

    	mSphinx.showPOI(pois, 0);
    	WidgetUtils.share(mSphinx, smsContent, weiboContent, qzoneContent, poi.getPosition(), mActionTag);
    }
    
    public void setData(POI poi) {
        if (mStampAnimation != null) {
            mStampBigImv.setVisibility(View.GONE);
            mStampAnimation.reset();
            mStampBigImv.setAnimation(null);
        }
        mPOI = poi;
        if (null != poi) {
            List<BaseQuery> baseQueryList = new ArrayList<BaseQuery>();
            String uuid = poi.getUUID();
            if (poi.getFrom() == POI.FROM_LOCAL && TextUtils.isEmpty(uuid) == false) {
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_POI);
                criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, uuid);
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, POI.NEED_FILELD);
                int cityId = Globals.g_Current_City_Info.getId();
                if (poi.ciytId != 0) {
                    cityId = poi.ciytId;
                } else if (poi.getPosition() != null){
                    cityId = MapEngine.getInstance().getCityId(poi.getPosition());
                }
                DataOperation poiQuery = new DataOperation(mSphinx);
                poiQuery.setup(criteria, cityId, getId(), getId(), null);
                baseQueryList.add(poiQuery);
            }
            
            if (poi.getCommentQuery() == null) {
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANPING);
                criteria.put(DataQuery.SERVER_PARAMETER_POI_ID, poi.getUUID());
                criteria.put(DataQuery.SERVER_PARAMETER_REFER, DataQuery.REFER_POI);
                DataQuery commentQuery = new DataQuery(mSphinx);
                commentQuery.setup(criteria, Globals.g_Current_City_Info.getId(), getId(), getId(), null, false, false, poi);
                baseQueryList.add(commentQuery);
                mCommentTipEdt.setVisibility(View.GONE);
            } else {
                mCommentTipEdt.setVisibility(View.VISIBLE);
            }
            
            refreshDetail();
            refreshComment();
            if (baseQueryList.isEmpty() == false) {
                mTkAsyncTasking = mSphinx.queryStart(baseQueryList);
                mBaseQuerying = baseQueryList;
                mLoadingView.setVisibility(View.VISIBLE);
            } else {
                mLoadingView.setVisibility(View.GONE);
            }
            
            String category = poi.getCategory().trim();
            poi.updateHistory(mSphinx);
            mActionLog.addAction(ActionLog.POIDetailShow, poi.getUUID(), poi.getName(), TextUtils.isEmpty(category) ? "NULL" : category.split(" ")[0]);
        }
    }
    
    private void makeFeature(ViewGroup viewGroup) {
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
        int count = viewGroup.getChildCount();
        for(int i = 0; i < count; i++) {
            viewGroup.getChildAt(i).setVisibility(View.GONE);
        }
        byte[] showKeys = {Description.FIELD_RECOMMEND_COOK, Description.FIELD_FEATURE, Description.FIELD_RECOMMEND, Description.FIELD_GUEST_CAPACITY, Description.FIELD_BUSINESS_HOURS,
                Description.FIELD_HOUSING_PRICE, Description.FIELD_SYNOPSIS, Description.FIELD_CINEMA_FEATURE, Description.FIELD_MEMBER_POLICY, Description.FIELD_FEATURE_SPECIALTY, 
                Description.FIELD_TOUR_DATE, Description.FIELD_TOUR_LIKE, Description.FIELD_POPEDOM_SCENERY, Description.FIELD_RECOMMEND_SCENERY,
                Description.FIELD_NEARBY_INFO, Description.FIELD_COMPANY_WEB, Description.FIELD_COMPANY_TYPE,
                Description.FIELD_COMPANY_SCOPE, Description.FIELD_INDUSTRY_INFO};

        int margin = (int)(Globals.g_metrics.density*8);
        LayoutParams layoutParamsTitle = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        layoutParamsTitle.topMargin = margin;
        layoutParamsTitle.bottomMargin = 0;
        LayoutParams layoutParamsBody = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        layoutParamsBody.topMargin = 0;
        layoutParamsBody.bottomMargin = margin;
        LayoutParams layoutParamsSplit = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        TextView titleTxv;
        TextView bodyTxv;
        ImageView splitImv = null;
        for(int i = 0; i < showKeys.length; i++) {
            
            byte key = showKeys[i];
            String value = poi.getDescriptionValue(key);
            
            if(!TextUtils.isEmpty(value)) {
                if (i*3 < count) {
                    titleTxv = (TextView) viewGroup.getChildAt(i*3);
                    bodyTxv = (TextView) viewGroup.getChildAt(i*3+1);
                    splitImv = (ImageView) viewGroup.getChildAt(i*3+2);
                    titleTxv.setVisibility(View.VISIBLE);
                    bodyTxv.setVisibility(View.VISIBLE);
                    splitImv.setVisibility(View.VISIBLE);
                } else {
                    titleTxv = new TextView(mContext);
                    titleTxv.setGravity(Gravity.LEFT);
                    int color = mSphinx.getResources().getColor(R.color.black_middle);
                    titleTxv.setTextColor(color);
                    viewGroup.addView(titleTxv, layoutParamsTitle);
                    bodyTxv = new TextView(mContext);
                    bodyTxv.setGravity(Gravity.LEFT);
                    color = mSphinx.getResources().getColor(R.color.black_light);
                    bodyTxv.setTextColor(color);
                    viewGroup.addView(bodyTxv, layoutParamsBody);
                    splitImv = new ImageView(mContext);
                    splitImv.setBackgroundResource(R.drawable.bg_broken_line);
                    viewGroup.addView(splitImv, layoutParamsSplit);
                }
                String name = poi.getDescriptionName(mContext, key);
                titleTxv.setText(name.substring(0, name.length()-1));
                bodyTxv.setText(value);
            }
        }
        if (splitImv != null) {
            viewGroup.setVisibility(View.VISIBLE);
            splitImv.setVisibility(View.GONE);
        } else {
            viewGroup.setVisibility(View.GONE);
        }
    }
    
    private SpannableStringBuilder getDescription() {
    	
    	StringBuilder sb = new StringBuilder();
        POI poi = mPOI;
        if (poi == null) {
            return new SpannableStringBuilder();
        }
    	List<Integer> indexs = new ArrayList<Integer>();
    	
        byte[] showKeys = {Description.FIELD_PRODUCT_ATTITUDE, Description.FIELD_TASTE, Description.FIELD_SERVICE_ATTITUDE, Description.FIELD_ENVIRONMENT,
                Description.FIELD_FILM_EFFECT, Description.FIELD_SERVICE_QUALITY, Description.FIELD_PRICE_LEVEL, Description.FIELD_MEDICAL_TREATMENT_LEVEL};
        
        int addCount = 1;
        for(int i = 0; i < showKeys.length; i++) {
        	
        	byte key = showKeys[i];
        	String value = poi.getDescriptionValue(key);
        	
        	if(!TextUtils.isEmpty(value)) {
                
                if (addCount > 1 && (addCount-1)%4 == 0) {
                    sb.append('\n');
                }
                
        	    String name = poi.getDescriptionName(mContext, key);
            	sb.append(name);
                indexs.add(sb.length());
                
            	sb.append(value);
                indexs.add(sb.length());
            	sb.append("   ");
                
            	addCount++;
        	}
        }
        
        SpannableStringBuilder style = new SpannableStringBuilder(sb.toString());
        int orange = mSphinx.getResources().getColor(R.color.orange);
        for(int i = 0 ; i < indexs.size()-1; i+=2){
            style.setSpan(new ForegroundColorSpan(orange),indexs.get(i),indexs.get(i+1),Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        }
        
        return style;
    }
    
    public View getCommentItemView(View convertView, ViewGroup parent, Comment comment, final POI poi) {
        View view;
        if (convertView == null) {
            view = mLayoutInflater.inflate(R.layout.poi_comment_list_item, parent, false);
        } else {
            view = convertView;
        }
        

        try {
            RatingBar gradeRtb = (RatingBar)view.findViewById(R.id.grade_rtb);
            TextView authorTxv = (TextView) view.findViewById(R.id.author_txv);
            TextView dateTxv = (TextView) view.findViewById(R.id.date_txv);
            TextView commentTxv = (TextView) view.findViewById(R.id.comment_txv);
            TextView srcTxv = (TextView) view.findViewById(R.id.src_txv);
            
            float grade = comment.getGrade()/2.0f;
            gradeRtb.setRating(grade);

            authorTxv.setText(comment.getUser());
            // 在这里不区分最近这条点评是否为我的
//            if (Comment.isAuthorMe(comment) > 0) {
//                view.setBackgroundResource(R.drawable.list_middle);
//                authorTxv.setTextColor(mSphinx.getResources().getColor(R.color.blue));
//                User user = Globals.g_User;
//                if (user != null) {
//                    authorTxv.setText(user.getNickName());
//                }
//                poi.setMyComment(comment);
//                view.setOnClickListener(new OnClickListener() {
//                    
//                    @Override
//                    public void onClick(View arg0) {
//                        mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "myComment");
//                        if (poi.getStatus() >= 0) {
//                            Intent intent = new Intent();
//                            intent.setClass(mSphinx, POIComment.class);
//                            POIComment.setPOI(poi, getId(), POIComment.STATUS_MODIFY);
//                            mSphinx.startActivityForResult(intent, R.id.activity_poi_comment);
//                        } else {
//                            CommonUtils.showNormalDialog(mSphinx, 
//                                    mSphinx.getString(R.string.comment_poi_invalid_hit));
//                        }
//                    }
//                });
//            } else {
                view.setBackgroundResource(R.drawable.list_middle_normal);
                authorTxv.setTextColor(0xff000000);
                view.setOnClickListener(this);
//            }
            
            String time = comment.getTime();
            if (!TextUtils.isEmpty(time) && time.length() >= 10) {
                dateTxv.setText(time.subSequence(0, 10));
            } else {
                dateTxv.setText(null);
            }
            
            String body = comment.getContent();
            commentTxv.setText(body);
            commentTxv.setMaxLines(2);
            commentTxv.setEllipsize(TruncateAt.END);
            
            final String url = comment.getUrl();
            if (TextUtils.isEmpty(url)) {
                srcTxv.setVisibility(View.GONE);
            } else {
                srcTxv.setVisibility(View.VISIBLE);
                String source = mSphinx.getString(R.string.source_);
                String src = String.format(source, url);
                SpannableString srcSns = new SpannableString(src);
                srcSns.setSpan(new ClickableSpan() {
                    @Override
                    public void updateDrawState(TextPaint ds) {
                        ds.setUnderlineText(false);// 去掉链接的下划线
                    }
    
                    @Override
                    public void onClick(final View widget) {
                        CommonUtils.showNormalDialog(mSphinx,
                                mSphinx.getString(R.string.prompt), 
                                mSphinx.getString(R.string.are_you_view_url, url),
                                new DialogInterface.OnClickListener() {
                                    
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        switch (id) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                                mSphinx.startActivityForResult(intent, R.id.view_invalid);
                                                break;
    
                                            default:
                                                break;
                                        }
                                    }
                                });
                    }
                }, String.format(source, "").length(), src.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                srcTxv.setText(srcSns);
                srcTxv.setMovementMethod(LinkMovementMethod.getInstance()); 
            }
        } catch (Exception e) {
        }
        
        return view;
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);  
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
        mLoadingView.setVisibility(View.GONE);
        List<BaseQuery> baseQueryList = tkAsyncTask.getBaseQueryList();
        Tuangou tuangou = null;
        for(BaseQuery baseQuery : baseQueryList) {
            if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
                isReLogin = true;
                return;
            }
            Hashtable<String, String> criteria = baseQuery.getCriteria();
            String dataType = criteria.get(DataOperation.SERVER_PARAMETER_DATA_TYPE);
            Response response = baseQuery.getResponse();
            
            // 查询我的点评的结果
            if (baseQuery instanceof DataQuery && response instanceof CommentResponse) {
                if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, false, this, false)) {
                    return;
                }
                DataQuery dataQuery = (DataQuery) baseQuery;
                poi.setCommentQuery(dataQuery);
//                POI requestPOI = dataQuery.getPOI();
//                CommentResponse commentResponse = (CommentResponse) response;
//                CommentList commentList = commentResponse.getList();
//                if (commentList != null) {
//                    List<Comment> list = commentList.getList();
//                    if (list != null && list.size() > 0) {
//                        Comment comment = list.get(0);
//                        if (requestPOI.equals(poi) && Comment.isAuthorMe(comment) > 0) {
//                            requestPOI.setMyComment(comment);
//                            POIComment.setPOI(requestPOI, getId(), POIComment.STATUS_MODIFY);
//                            mSphinx.showView(R.id.activity_poi_comment);
//                        }
//                    }
//                }
                refreshComment();
                refreshDetail();
                poi.updateComment(mSphinx);
                mCommentTipEdt.setVisibility(View.VISIBLE);
                
            } else if (baseQuery instanceof DataOperation) {
                // 查询POI的结果
                if (BaseQuery.DATA_TYPE_POI.equals(dataType)) {
                    mLoadingView.setVisibility(View.GONE);
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, new int[]{603}, false, this, false)) {
                        if (response != null) {
                            int responseCode = response.getResponseCode();
                            if (responseCode == 603) {
                                poi.setStatus(POI.STATUS_INVALID);
                                BaseActivity.showErrorDialog(mSphinx, mSphinx.getString(R.string.response_code_603), this, true);
                            }
                        }
                        return;
                    }
                    POI onlinePOI = ((POIQueryResponse)response).getPOI();
                    if (onlinePOI != null && onlinePOI.getUUID() != null && onlinePOI.getUUID().equals(poi.getUUID())) {
                        poi.updateData(mSphinx, onlinePOI.getData());
                        poi.setFrom(POI.FROM_ONLINE);
                        refreshDetail();
                        refreshComment();
                    }
                    
                // 查询团购的结果
                } else if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                        return;
                    }
                    tuangou = ((TuangouQueryResponse) response).getTuangou();
                    
                // 查询团购分店的结果
                } else if (BaseQuery.DATA_TYPE_FENDIAN.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                        return;
                    }
                    tuangou.setFendian(((FendianQueryResponse) response).getFendian());
                    List<Tuangou> list = new ArrayList<Tuangou>();
                    list.add(tuangou);
                    mSphinx.showView(R.id.view_tuangou_detail);
                    mSphinx.getTuangouDetailFragment().setData(list, 0, null);
                    
                // 查询演出的结果
                } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                        return;
                    }
                    Yanchu yanchu = ((YanchuQueryResponse) response).getYanchu();
                    List<Yanchu> list = new ArrayList<Yanchu>();
                    list.add(yanchu);
                    mSphinx.showView(R.id.view_yanchu_detail);
                    mSphinx.getYanchuDetailFragment().setData(list, 0, null);
                    
                // 查询展览的结果
                } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                        return;
                    }
                    Zhanlan zhanlan = ((ZhanlanQueryResponse) response).getZhanlan();
                    List<Zhanlan> list = new ArrayList<Zhanlan>();
                    list.add(zhanlan);
                    mSphinx.showView(R.id.view_zhanlan_detail);
                    mSphinx.getZhanlanDetailFragment().setData(list, 0, null);
                }
            }
        }
    }
    
    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
    }

    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        return true;
    }

    private void showMoreComment() {
        POI poi = mPOI;
        if (poi == null) {
            return;
        }

        POICommentList.setPOI(poi);
        mSphinx.showView(R.id.activity_poi_comment_list);
    }    
}
