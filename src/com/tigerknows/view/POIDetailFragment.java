/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
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

    private Button mTrafficBtn = null;

    private Button mShareBtn = null;
    
    private TextView mCategoryTxv;

    private TextView mMoneyTxv;

    private RatingBar mStartsRtb;

    private LinearLayout mFeatureTxv;

    // Error Fix Button
    private Button mErrorFixBtn = null;
    
    private TextView mDescriptionTxv = null;

    private ViewGroup mCommentListView = null;
    
    private ViewGroup mCommentSumTotalView;
    
    private TextView mCommentSumTotalTxv;
    
    private TextView mViewAllCommentTxv;
    
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
    
    private List<BaseQuery> mBaseQuerying;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIDetail;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_detail, container, false);
        findViews();
        setListener();
        
        mStampAnimation = AnimationUtils.loadAnimation(mSphinx, R.anim.stamp);
        mStampAnimation.setAnimationListener(new AnimationListener() {
            
            @Override
            public void onAnimationStart(Animation arg0) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onAnimationRepeat(Animation arg0) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onAnimationEnd(Animation arg0) {
                mStampBigImv.setVisibility(View.INVISIBLE);
                refreshComment();
            }
        });
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleTxv.setText(R.string.detail_info);
        mRightBtn.setImageResource(R.drawable.ic_view_map);
        mRightTxv.getLayoutParams().width = Util.dip2px(Globals.g_metrics.density, 72);
        mRightTxv.setOnClickListener(this); 
        
        if (isReLogin == true) {
            isReLogin = false;
            for(BaseQuery baseQuery : mBaseQuerying) {
                baseQuery.setResponse(null);
            }
            mTkAsyncTasking = mSphinx.queryStart(mBaseQuerying);
        }
        if (mPOI != null) {
            if (mPOI.getStatus() < POI.STATUS_NONE) {
                BaseActivity.showErrorDialog(mSphinx, mSphinx.getString(R.string.response_code_603), this, true);
            }
            DataQuery commentQuery = mPOI.getCommentQuery();
            if (null != commentQuery) {
                setCommentQuery(commentQuery);
            }
        }  
        mBodyScv.smoothScrollTo(0, 0);
    }
    
    public void refreshDetail() {
        if (mPOI == null) {
            return;
        }
        String category = mPOI.getCategory().trim();
        setFavoriteState(mFavoriteBtn, mPOI.checkFavorite(mContext));
        mNameTxt.setText(mPOI.getName());

        long attribute = mPOI.getAttribute();
        if ((attribute & POI.ATTRIBUTE_COMMENT_USER) > 0) {
            mStampImv.setBackgroundResource(R.drawable.ic_stamp_gold);
            mStampImv.setVisibility(View.VISIBLE);
            mCommentTipEdt.setHint(R.string.comment_tip_hit1);
        } else if ((attribute & POI.ATTRIBUTE_COMMENT_ANONYMOUS) > 0) {
            mStampImv.setBackgroundResource(R.drawable.ic_stamp_silver);
            mStampImv.setVisibility(View.VISIBLE);
            mCommentTipEdt.setHint(R.string.comment_tip_hit1);
        } else {
            mStampImv.setVisibility(View.GONE);
            mCommentTipEdt.setHint(R.string.comment_tip_hit0);
        }

        float star = mPOI.getGrade();
        mStartsRtb.setRating(star/2);
        
        if (!TextUtils.isEmpty(category)) {
            mCategoryTxv.setText(category);
        } else {
            mCategoryTxv.setText("");
        }
        
        int money = mPOI.getPerCapity();
        if (money > -1) {
            mMoneyTxv.setText(mContext.getString(R.string.yuan, money));
        } else {
            mMoneyTxv.setText("");
        }

        SpannableStringBuilder description = getDescription();
        if (description.length() > 0) {
            mDescriptionTxv.setText(description);
            mDescriptionTxv.setVisibility(View.VISIBLE);
        } else {
            mDescriptionTxv.setVisibility(View.GONE);
        }
        
        /**
         * 若地址或电话为空, 则不显示
         */
        String address = mPOI.getAddress();
        String telephone = mPOI.getTelephone();
        if (!TextUtils.isEmpty(telephone) || !TextUtils.isEmpty(address)) {
            
            if (!TextUtils.isEmpty(telephone)) {
                mTelephoneView.setVisibility(View.VISIBLE);
                mTelephoneTxv.setText(telephone.replace("|", mSphinx.getString(R.string.dunhao)));
                mTelephoneView.setPadding(Util.dip2px(Globals.g_metrics.density, 8), 
                        Util.dip2px(Globals.g_metrics.density, 8), 
                        Util.dip2px(Globals.g_metrics.density, 8), 
                        Util.dip2px(Globals.g_metrics.density, 8));
            } else {
                mTelephoneView.setVisibility(View.GONE);
            }
            
            if (!TextUtils.isEmpty(address)) {
                mAddressView.setVisibility(View.VISIBLE);
                mAddressTxv.setText(address);
                mAddressTelephoneDividerImv.setVisibility(View.VISIBLE);
                mAddressView.setPadding(Util.dip2px(Globals.g_metrics.density, 8), 
                        Util.dip2px(Globals.g_metrics.density, 8), 
                        Util.dip2px(Globals.g_metrics.density, 8), 
                        Util.dip2px(Globals.g_metrics.density, 8));
            } else {
                mAddressView.setVisibility(View.GONE);
            }
            
            if (TextUtils.isEmpty(telephone) || TextUtils.isEmpty(address)) {
                mAddressView.setBackgroundResource(R.drawable.list_single);
                mAddressTelephoneDividerImv.setVisibility(View.GONE);
            } else {
                mTelephoneView.setBackgroundResource(R.drawable.list_header);
                mAddressView.setBackgroundResource(R.drawable.list_footer);
            }
            mAddressAndPhoneView.setVisibility(View.VISIBLE);
        } else {
            mAddressAndPhoneView.setVisibility(View.GONE);
        }
        
        makeFeature(mFeatureTxv);
        
        List<DynamicPOI> list = mPOI.getDynamicPOIList();
        int viewCount = mDynamicPOIListView.getChildCount();
        int viewIndex = 0;
        int size = list.size();
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
                            mActionLog.addAction(ActionLog.POIDetailTuangou);
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
                            mActionLog.addAction(ActionLog.POIDetailYanchu);
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
                            mActionLog.addAction(ActionLog.POIDetailZhanlan);
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
                    child.findViewById(R.id.list_separator_imv).setVisibility(View.INVISIBLE);
                } else {
                    child.setBackgroundResource(R.drawable.list_header);
                    child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
                }
            } else if (i == (viewIndex-1)) {
                child.setBackgroundResource(R.drawable.list_footer);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.INVISIBLE);
            } else {
                child.setBackgroundResource(R.drawable.list_middle);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
            }
        }
    }
    
    public void refreshStamp() {
        if (mPOI == null) {
            return;
        }
        long attribute = mPOI.getAttribute();
        int resId = Integer.MIN_VALUE;
        if ((attribute & POI.ATTRIBUTE_COMMENT_USER) > 0) {
            resId = R.drawable.ic_stamp_gold_big;
        } else if ((attribute & POI.ATTRIBUTE_COMMENT_ANONYMOUS) > 0) {
            resId = R.drawable.ic_stamp_silver_big;
        }
        if (resId != Integer.MIN_VALUE) {
            mStampAnimation.cancel();
            mStampBigImv.setAnimation(null);
            mStampBigImv.setBackgroundResource(resId);
            mStampBigImv.setVisibility(View.VISIBLE);
            mStampBigImv.startAnimation(mStampAnimation);
        }
    }
    
    public DataQuery refreshComment() {
        if (mPOI == null) {
            return null;
        }
        DataQuery commentQuery = mPOI.getCommentQuery();
        if (null != commentQuery) {
            setCommentQuery(commentQuery);
            refreshDetail();

            if (commentQuery.getSourceViewId() != -1 && commentQuery.getTargetViewId() != -1) {
                return null;
            }
        } else {
            mCommentListView.setVisibility(View.INVISIBLE);
            refreshDetail();
        }
        return Comment.createPOICommentQuery(mSphinx, mPOI, getId(), getId());
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
        
        mPOIBtn = (Button) mRootView.findViewById(R.id.poi_btn);
        mTrafficBtn = (Button)mRootView.findViewById(R.id.traffic_btn);
        mShareBtn = (Button)mRootView.findViewById(R.id.share_btn);
        mFavoriteBtn = (Button)mRootView.findViewById(R.id.favorite_btn);
        // Error Fix
        mErrorFixBtn = (Button)mRootView.findViewById(R.id.error_recovery_btn);
        
        mFeatureTxv = (LinearLayout)mRootView.findViewById(R.id.feature_txv);

        mCommentListView = (ViewGroup)mRootView.findViewById(R.id.comment_list_view);
        mCommentSumTotalView = (ViewGroup) mRootView.findViewById(R.id.comment_sum_total_view);
        mCommentSumTotalTxv = (TextView) mRootView.findViewById(R.id.comment_sum_total_txv);
        mViewAllCommentTxv = (TextView) mRootView.findViewById(R.id.look_all_txv);
        
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
        mTrafficBtn.setOnClickListener(this);
        mShareBtn.setOnClickListener(this);
        mFavoriteBtn.setOnClickListener(this);
        mTelephoneTxv.setOnClickListener(this);
        mTelephoneTxv.setLongClickable(false);
        mAddressView.setOnClickListener(this);
        mPOIBtn.setOnClickListener(this);
        // Error Fix
        mErrorFixBtn.setOnClickListener(this);
        mCommentSumTotalView.setOnClickListener(this);
        mCommentTipView.setOnTouchListener(this);
        mCommentTipEdt.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mActionLog.addAction(ActionLog.POIDetailInputBox);
                    boolean isMe = false;
                    long attribute = mPOI.getAttribute();
                    if ((attribute & POI.ATTRIBUTE_COMMENT_USER) > 0) {
                        isMe = true;
                    } else if ((attribute & POI.ATTRIBUTE_COMMENT_ANONYMOUS) > 0) {
                        isMe = true;
                    }
                    if (mPOI.getStatus() < 0) {
                        int resId;
                        if (isMe) {
                            resId = R.string.poi_comment_poi_invalid_not_update;
                        } else {
                            resId = R.string.poi_comment_poi_invalid_not_create;
                        }
                        final AlertDialog alertDialog = CommonUtils.getAlertDialog(mSphinx);
                        alertDialog.setMessage(mSphinx.getString(resId));
                        alertDialog.setButton(mSphinx.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                alertDialog.dismiss();
                            }
                        });
                        alertDialog.show();
                    } else {
                        POIComment.setPOI(mPOI, getId(), isMe ? POIComment.STATUS_MODIFY : POIComment.STATUS_NEW);
                        mSphinx.showView(R.id.activity_poi_comment);
                    }
                }
                return false;
            }
        });
    }

    public void onClick(View view) {
        switch (view.getId()) {                
            case R.id.right_txv:
                mActionLog.addAction(ActionLog.POIDetailMap);
                viewMap();
                break;
            case R.id.telephone_txv:
                mActionLog.addAction(ActionLog.POIDetailTelephone);
                break;
                
            case R.id.address_view:
                mActionLog.addAction(ActionLog.POIDetailAddress);
                viewMap();
                break;
                
            case R.id.traffic_btn:
                mActionLog.addAction(ActionLog.POIDetailGoToHere);
                
                /* 交通界面的显示 */
                mSphinx.getTrafficQueryFragment().setData(mPOI);
                mSphinx.showView(R.id.view_traffic_query);
                break;
                
            case R.id.poi_btn:
                mActionLog.addAction(ActionLog.POIDetailSearch);
                mSphinx.getPOINearbyFragment().setData(mPOI);
                mSphinx.showView(R.id.view_poi_nearby);
                break;
                
            case R.id.share_btn:
                mActionLog.addAction(ActionLog.POIDetailShare);
                share();
                break;

            case R.id.favorite_btn:
                favorite();
                break;
            case R.id.error_recovery_btn:
                // Error Recovery
            	mActionLog.addAction(ActionLog.POIDetailErrorRecovery);
                errorRecovery();
                break;
                
            case R.id.comment_sum_total_view:
                mActionLog.addAction(ActionLog.POIDetailAllComment);
                showMoreComment();
                break;
                
            default:
                mActionLog.addAction(ActionLog.POIDetailOtherComment);
                showMoreComment();
                break;
        }
    }
    
    public void viewMap() {
        List<POI> pois = new ArrayList<POI>();
        pois.add(mPOI);
        mSphinx.showPOI(pois, 0);
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.result_map), ActionLog.MapPOI);
        mSphinx.showView(R.id.view_result_map);
    }
    
    public void favorite() {
        boolean isFavorite = mPOI.checkFavorite(mContext);
        mActionLog.addAction(ActionLog.POIDetailFavorite, isFavorite ? "0" : "1");
        if (isFavorite) {
            CommonUtils.showNormalDialog(mSphinx, 
                    mContext.getString(R.string.prompt),
                    mContext.getString(R.string.cancel_favorite_tip),
                    new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface arg0, int id) {
                            if (id == DialogInterface.BUTTON_POSITIVE) {
                                mPOI.deleteFavorite(mContext);
                                setFavoriteState(mFavoriteBtn, false);
                            }
                        }
                    });
        } else {
            mPOI.writeToDatabases(mContext, -1, Tigerknows.STORE_TYPE_FAVORITE);
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
    	Drawable drawable = null;
    	
    	if (favoriteYet) {
    	    drawable = mContext.getResources().getDrawable(R.drawable.ic_favorite);
    		mFavoriteBtn.setText(mContext.getResources().getString(R.string.favorite_yet));
    	} else {
    	    drawable = mContext.getResources().getDrawable(R.drawable.ic_favorite_cancel);
    	    mFavoriteBtn.setText(mContext.getResources().getString(R.string.favorite));
    	}
    	
    	drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        mFavoriteBtn.setCompoundDrawables(null, drawable, null, null);
    }
    
    public void errorRecovery() {
        POIErrorRecovery.addTarget(mPOI);
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

        if(mPOI == null)
            return;

        String smsContent = ShareTextUtil.sharePOISmsContent(mPOI,mContext);
    	String weiboContent = ShareTextUtil.sharePOIWeiboContent(mPOI, mContext);
    	String qzoneContent = ShareTextUtil.sharePOIQzoneContent(mPOI, mContext);
    	
    	List<POI> pois = new ArrayList<POI>();
    	pois.add(mPOI);

    	mSphinx.showPOI(pois, 0);
    	WidgetUtils.share(mSphinx, smsContent, weiboContent, qzoneContent, mPOI.getPosition());
    }
    
    public void setData(POI poi) {
        if (mStampAnimation != null) {
            mStampBigImv.setVisibility(View.INVISIBLE);
            mStampAnimation.cancel();
            mStampBigImv.setAnimation(null);
        }
        mPOI = poi;
        if (null != mPOI) {
            List<BaseQuery> baseQueryList = new ArrayList<BaseQuery>();
            if (mPOI.getFrom() == POI.FROM_LOCAL) {
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_POI);
                criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, mPOI.getUUID());
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, POI.NEED_FILELD);
                int cityId = Globals.g_Current_City_Info.getId();
                if (mPOI.ciytId != 0) {
                    cityId = mPOI.ciytId;
                } else if (mPOI.getPosition() != null){
                    cityId = MapEngine.getInstance().getCityId(mPOI.getPosition());
                }
                DataOperation poiQuery = new DataOperation(mSphinx);
                poiQuery.setup(criteria, cityId, getId(), getId(), null);
                baseQueryList.add(poiQuery);
            }
            
            DataQuery commentQuery = refreshComment();
            if (commentQuery != null) {
                baseQueryList.add(commentQuery);
                mCommentTipView.setVisibility(View.INVISIBLE);
            } else {
                mCommentTipView.setVisibility(View.VISIBLE);
            }
            if (baseQueryList.isEmpty() == false) {
                mTkAsyncTasking = mSphinx.queryStart(baseQueryList);
                mBaseQuerying = baseQueryList;
                mLoadingView.setVisibility(View.VISIBLE);
            } else {
                mLoadingView.setVisibility(View.GONE);
            }
            
            String category = mPOI.getCategory().trim();
            mPOI.updateHistory(mSphinx);
            mActionLog.addAction(ActionLog.POIDetailShow, mPOI.getUUID(), mPOI.getName(), TextUtils.isEmpty(category) ? "NULL" : category.split(" ")[0]);
        }
    }
    
    private void makeFeature(ViewGroup viewGroup) {
        
        viewGroup.removeAllViews();
        byte[] showKeys = {Description.FIELD_FEATURE, Description.FIELD_RECOMMEND, Description.FIELD_RECOMMEND_COOK, Description.FIELD_GUEST_CAPACITY, Description.FIELD_BUSINESS_HOURS,
                Description.FIELD_HOUSING_PRICE, Description.FIELD_SYNOPSIS, Description.FIELD_CINEMA_FEATURE, Description.FIELD_MEMBER_POLICY, Description.FIELD_FEATURE_SPECIALTY, 
                Description.FIELD_TOUR_DATE, Description.FIELD_TOUR_LIKE, Description.FIELD_POPEDOM_SCENERY, Description.FIELD_RECOMMEND_SCENERY,
                Description.FIELD_NEARBY_INFO, Description.FIELD_COMPANY_WEB, Description.FIELD_COMPANY_TYPE,
                Description.FIELD_COMPANY_SCOPE, Description.FIELD_INDUSTRY_INFO};
        
        for(int i = 0; i < showKeys.length; i++) {
            
            byte key = showKeys[i];
            String value = mPOI.getDescriptionValue(key);
            
            if(!TextUtils.isEmpty(value)) {
                TextView textView = new TextView(mContext);
                textView.setGravity(Gravity.LEFT);
                textView.setTextColor(0xff000000);
                String name = mPOI.getDescriptionName(mContext, key);
                SpannableStringBuilder style = new SpannableStringBuilder(name+value);
                style.setSpan(new ForegroundColorSpan(0xffa97036), 0, name.length(),Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                textView.setText(style);
                viewGroup.addView(textView);
                
            }
        }
    }
    
    private SpannableStringBuilder getDescription() {
    	
    	StringBuilder sb = new StringBuilder();
    	List<Integer> indexs = new ArrayList<Integer>();
    	
        byte[] showKeys = {Description.FIELD_MOODS, Description.FIELD_PRODUCT_ATTITUDE, Description.FIELD_TASTE, Description.FIELD_SERVICE_ATTITUDE, Description.FIELD_ENVIRONMENT,
                Description.FIELD_FILM_EFFECT, Description.FIELD_SERVICE_QUALITY, Description.FIELD_PRICE_LEVEL, Description.FIELD_MEDICAL_TREATMENT_LEVEL};
        
        int addCount = 1;
        for(int i = 0; i < showKeys.length; i++) {
        	
        	byte key = showKeys[i];
        	String value = mPOI.getDescriptionValue(key);
        	
        	if(!TextUtils.isEmpty(value)) {
                
                if (addCount > 1 && (addCount-1)%4 == 0) {
                    sb.append('\n');
                }
                
        	    String name = mPOI.getDescriptionName(mContext, key);
            	sb.append(name);
                indexs.add(sb.length());
                
            	sb.append(value);
                indexs.add(sb.length());
            	sb.append(' ');
                
            	addCount++;
        	}
        }
        
        SpannableStringBuilder style = new SpannableStringBuilder(sb.toString());
        for(int i = 0 ; i < indexs.size()-1; i+=2){
            style.setSpan(new ForegroundColorSpan(0xffa97036),indexs.get(i),indexs.get(i+1),Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        }
        
        return style;
    }
    
    private void setCommentQuery(DataQuery commentQuery) {
        Response response = commentQuery.getResponse();
        POI poi = commentQuery.getPOI();
        
        if (poi == null || poi.equals(mPOI) == false) {
            return;
        }

        mCommentTipView.setVisibility(View.VISIBLE);
        int count = mCommentListView.getChildCount();
        for(int i = 1; i < count; i++) {
            mCommentListView.getChildAt(i).setVisibility(View.GONE);
        }
        mCommentListView.setVisibility(View.INVISIBLE);
        mCommentSumTotalView.setBackgroundResource(R.drawable.list_single_normal);
        mCommentSumTotalTxv.setText("  "+mSphinx.getString(R.string.comment));
        mCommentSumTotalTxv.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        mViewAllCommentTxv.setVisibility(View.INVISIBLE);

        if (response != null && response instanceof CommentResponse) {
            CommentList commentList = ((CommentResponse)response).getList();
            if (commentList != null && commentList.getList() != null && commentList.getList().size() > 0) {
                List<Comment> commentArrayList = commentList.getList();
                poi.updateAttribute();
                Collections.sort(commentArrayList, Comment.COMPARATOR);
                int length = commentArrayList.size();
                mCommentListView.setVisibility(View.VISIBLE);
                mCommentSumTotalView.setBackgroundResource(R.drawable.list_header);
                mViewAllCommentTxv.setVisibility(View.VISIBLE);
                if (length > 3) {
                    length = 3;
                }
                LayoutParams layoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
                for (int i = 0; i < length; i++) {
                    View commentView = null;
                    if (i*2+2 < count) {
                        View view = null;
                        view = mCommentListView.getChildAt(i*2+1);
                        view.setVisibility(View.VISIBLE);
                        view = mCommentListView.getChildAt(i*2+2);
                        view.setVisibility(View.VISIBLE);
                        commentView = getCommentItemView(view, mCommentListView, commentArrayList.get(i), poi);
                    } else {
                        ImageView imageView = new ImageView(mContext);
                        imageView.setBackgroundResource(R.drawable.divider);
                        mCommentListView.addView(imageView, layoutParams);
                        commentView = getCommentItemView(null, mCommentListView, commentArrayList.get(i), poi);
                        mCommentListView.addView(commentView);
                    }
                    long attribute = poi.getAttribute();
                    if (i == 0 && ((attribute & POI.ATTRIBUTE_COMMENT_USER) > 0 || (attribute & POI.ATTRIBUTE_COMMENT_ANONYMOUS) > 0)) {
                        if (i < length -1) {
                            commentView.setBackgroundResource(R.drawable.list_middle);
                        } else {
                            commentView.setBackgroundResource(R.drawable.list_footer);
                        }
                    } else if (i < length -1) {
                        commentView.setBackgroundResource(R.drawable.list_middle);
                        commentView.setOnClickListener(this);
                    } else {
                        commentView.setBackgroundResource(R.drawable.list_footer);
                        commentView.setOnClickListener(this);
                    }
                }
            } else {
                mCommentListView.setVisibility(View.INVISIBLE);
                mCommentSumTotalTxv.setText("  "+mSphinx.getString(R.string.comment_empty));
                mCommentSumTotalView.setBackgroundResource(R.drawable.list_single);
                mCommentSumTotalTxv.setGravity(Gravity.CENTER);
                mViewAllCommentTxv.setVisibility(View.INVISIBLE);
            }
        }
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
            
            float grade = comment.getGrade()/2;
            gradeRtb.setRating(grade);
            
            if (comment.getAttribute() > 0) {
                view.setBackgroundResource(R.drawable.list_middle);
                authorTxv.setText(R.string.me);
                authorTxv.setTextColor(0xff009CFF);
                poi.setMyComment(comment);
                view.setOnClickListener(new OnClickListener() {
                    
                    @Override
                    public void onClick(View arg0) {
                        mActionLog.addAction(ActionLog.POIDetailMyComment);
                        if (poi.getStatus() >= 0) {
                            Intent intent = new Intent();
                            intent.setClass(mSphinx, POIComment.class);
                            POIComment.setPOI(poi, getId(), POIComment.STATUS_MODIFY);
                            mSphinx.startActivityForResult(intent, R.id.activity_poi_comment);
                        } else {
                            final AlertDialog alertDialog = CommonUtils.getAlertDialog(mSphinx);
                            alertDialog.setMessage(mSphinx.getString(R.string.comment_poi_invalid_hit));
                            alertDialog.setButton(mSphinx.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    alertDialog.dismiss();
                                }
                            });
                            alertDialog.show();
                        }
                    }
                });
            } else {
                view.setBackgroundResource(R.drawable.list_middle_normal);
                authorTxv.setTextColor(0xff000000);
                authorTxv.setText(comment.getUser());
                view.setOnClickListener(null);
            }
            
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
            if (baseQuery instanceof DataQuery) {
                mLoadingView.setVisibility(View.GONE);
                if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, false, this, false)) {
                    return;
                }
                DataQuery dataQuery = (DataQuery) baseQuery;
                POI poi = dataQuery.getPOI();
                poi.updateComment(mSphinx);
                refreshComment();
            } else if (baseQuery instanceof DataOperation) {
                if (BaseQuery.DATA_TYPE_POI.equals(dataType)) {
                    mLoadingView.setVisibility(View.GONE);
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, new int[]{603}, false, this, false)) {
                        if (response != null) {
                            int responseCode = response.getResponseCode();
                            if (responseCode == 603) {
                                mPOI.setStatus(POI.STATUS_INVALID);
                                BaseActivity.showErrorDialog(mSphinx, mSphinx.getString(R.string.response_code_603), this, true);
                            }
                        }
                        return;
                    }
                    POI poi = ((POIQueryResponse)response).getPOI();
                    if (poi != null) {
                        mPOI.updateData(mSphinx, poi.getData());
                        refreshComment();
                    }
                } else if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                        return;
                    }
                    tuangou = ((TuangouQueryResponse) response).getTuangou();
                } else if (BaseQuery.DATA_TYPE_FENDIAN.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                        return;
                    }
                    tuangou.setFendian(((FendianQueryResponse) response).getFendian());
                    mSphinx.getTuangouDetailFragment().setData(tuangou);
                    mSphinx.showView(R.id.view_tuangou_detail);
                } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                        return;
                    }
                    Yanchu yanchu = ((YanchuQueryResponse) response).getYanchu();
                    mSphinx.getYanchuDetailFragment().setData(yanchu);
                    mSphinx.showView(R.id.view_yanchu_detail);
                } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                        return;
                    }
                    Zhanlan zhanlan = ((ZhanlanQueryResponse) response).getZhanlan();
                    mSphinx.getZhanlanDetailFragment().setData(zhanlan);
                    mSphinx.showView(R.id.view_zhanlan_detail);
                }
            }
        }
        
        mPOI.updateHistory(mContext);
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
        DataQuery commentQuery = mPOI.getCommentQuery();
        if (commentQuery != null) {
            POICommentList.setCommentQuery(commentQuery);
            mSphinx.showView(R.id.activity_poi_comment_list);
        }
    }    
}
