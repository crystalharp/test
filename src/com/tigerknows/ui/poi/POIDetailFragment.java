/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
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


import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;

import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Comment;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataOperation.DianyingQueryResponse;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.POI;
import com.tigerknows.model.ProxyQuery;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic;
import com.tigerknows.model.Response;
import com.tigerknows.model.Yingxun;
import com.tigerknows.model.DataOperation.POIQueryResponse;
import com.tigerknows.model.DataOperation.YingxunQueryResponse;
import com.tigerknows.model.DataQuery.CommentResponse;
import com.tigerknows.model.DataQuery.DianyingResponse;
import com.tigerknows.model.DataQuery.CommentResponse.CommentList;
import com.tigerknows.model.DataQuery.DianyingResponse.DianyingList;
import com.tigerknows.model.POI.Description;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.share.ShareAPI;
import com.tigerknows.share.TKWeixin;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;

/**
 * @author Peng Wenyue
 * <ul>
 * 类型 价格 人气 口味 环境 服务 推荐菜
 * </ul>
 * 动态POI添加说明:
 *   由于动态POI在这页添加的越来越多,于是决定给这页添加一个机制,让这一页不再
 * 随着动态POI类型的增加而迅速的膨胀下去.
 * TODO:完成这个说明
 * 布局：
 *   为了让服务器可以控制动态POI的显示顺序，给服务器预留了排序接口，可以动态
 * 调整不同POI的顺序。为了动态调整的时候不同block的间隔不会出问题，现约定每个
 * block下面留8dp的padding，用来放动态Block的容器LinearLayout本身不带高度，这
 * 样调整顺序和不存在数据的时候都不会导致上下间距出问题
 */
public class POIDetailFragment extends BaseFragment implements View.OnClickListener, OnTouchListener {
    
    public POIDetailFragment(Sphinx sphinx) {
        super(sphinx);
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
    
    //如下两个layout用来添加动态POI内容到该页
    public LinearLayout mBelowAddressLayout;
    
    public LinearLayout mBelowCommentLayout;

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
    
    private View mNavigationWidget;
    
    DynamicNormalPOI mDynamicNormalPOI;
    
    DynamicHotelPOI mDynamicHotelPOI;
    
    DynamicMoviePOI mDynamicMoviePOI;
    
    private Button mCommentTipEdt;
    
    private View mLoadingView;
    
    private Animation mStampAnimation;
    
    private View mDoingView;

    private View mToolsView;
    
    private View mWeixinView;
    
    private Button mWeixinBtn;
    
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
    //*******************DynamicPOI code start*************************
    
    List<DynamicPOIViewBlock> DPOIViewBlockList = new LinkedList<DynamicPOIViewBlock>();
    
    Hashtable<String, DynamicPOIView> DPOIViewTable = new Hashtable<String, DynamicPOIView>();
    
    List<DynamicPOIView> existView = new ArrayList<DynamicPOIView>();
    
    private void initDynamicPOIEnv(){
        //目前可以处理的动态POI类型
        DPOIViewTable.put(DynamicPOI.TYPE_HOTEL, mDynamicHotelPOI);
        DPOIViewTable.put(DynamicPOI.TYPE_TUANGOU, mDynamicNormalPOI);
        DPOIViewTable.put(DynamicPOI.TYPE_ZHANLAN, mDynamicNormalPOI);
        DPOIViewTable.put(DynamicPOI.TYPE_YANCHU, mDynamicNormalPOI);
        DPOIViewTable.put(DynamicPOI.TYPE_COUPON, mDynamicNormalPOI);
        DPOIViewTable.put(DynamicPOI.TYPE_DIANYING, mDynamicMoviePOI);
    }
    
    /**
     * Viewblock的添加顺序按照服务器给的动态POI顺序添加,这样显示动态POI的时候就不用
     * 手动排序了.
     * @param poi
     */
    private void checkAndAddDynamicPOIView(POI poi) {
        List<DynamicPOI> list = poi.getDynamicPOIList();
        existView.clear();
        int size = (list == null ? 0 : list.size());
        if (size == 0) {
            return;
        }
        for (DynamicPOI dynamicPOI : list) {
            String dataType = dynamicPOI.getType();
            if (!DPOIViewTable.containsKey(dataType) || 
                    existView.contains(DPOIViewTable.get(dataType))) {
                continue;
            }
            existView.add(DPOIViewTable.get(dataType));
            DPOIViewTable.get(dataType).initData(poi);
            DPOIViewBlockList.addAll(DPOIViewTable.get(dataType).getViewList());
        }
        
        for (DynamicPOIViewBlock block : DPOIViewBlockList) {
            block.addToParent();
            block.clear();
        }
    }
    
    /*
     * 清除掉所有的动态POI的ViewBlock
     */
    private void clearDynamicPOI(List<DynamicPOIViewBlock> POIList) {
        mBelowCommentLayout.removeAllViews();
        mBelowAddressLayout.removeAllViews();
        POIList.clear();
    }
        
//    private boolean containsDPOIType(DPOIType t) {
//        int size = DPOIViewBlockList.size();
//        if (size == 0) {
//            return false;
//        }
//        for (int i = size - 1; i >= 0; i--) {
//            DynamicPOIViewBlock a = DPOIViewBlockList.get(i);
//            if (a.mType == t) {
//                return true;
//            }
//        }
//        return false;
//    }
    
    /**
     * 有些动态POI并不止一个显示区块，但是刷新的时候却有着不同的刷新规则
     * 为了让刷新可以分开，而不是一次把相关的所有区块刷新，在ViewBlock下
     * 也添加了refresh的接口，用来控制单独的刷新。
     */
    public interface BlockRefresher {
        void refresh();
    }
    /**
     * 每个显示块都需要有一个这个类的对象,里面存有自己的Layout和所属的Layout
     * 需要有个block级别的刷新机制。
     */
    public static class DynamicPOIViewBlock {
		View mOwnLayout;
		LinearLayout mBelongsLayout;
		boolean mLoadSucceed = true;
		BlockRefresher mRefresher;
		
		public DynamicPOIViewBlock(LinearLayout belongsLayout, BlockRefresher refresher) {
		    mBelongsLayout = belongsLayout;
		    mRefresher = refresher;
		}
		
		final void addToParent() {
		    if (mBelongsLayout.indexOfChild(mOwnLayout) == -1) {
		        mBelongsLayout.addView(mOwnLayout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		    }
		}
		
		final void show() {
		    mOwnLayout.setVisibility(View.VISIBLE);
		}

        final void clear() {
            mOwnLayout.setVisibility(View.GONE);
		}
        
        final void remove() {
		    if (mBelongsLayout.indexOfChild(mOwnLayout) == -1) {
		        mBelongsLayout.removeView(mOwnLayout);
		    }
        }
        
        final public void refresh() {
            if (mRefresher != null) {
                mRefresher.refresh();
                show();
            }
        }
    }
    
    /*
     * 所有新增加的动态POI需要继承这个类
     */
	public abstract static class DynamicPOIView {

		POIDetailFragment mPOIDetailFragment;
		Sphinx mSphinx;
		POI mPOI;
		LayoutInflater mInflater;
		
//        protected List<BaseQuery> mBaseQuerying;
//        protected TKAsyncTask mTkAsyncTasking;
		
		static void query(POIDetailFragment fragment, List<BaseQuery> list){
		    fragment.mTkAsyncTasking = fragment.mSphinx.queryStart(list);
		    fragment.mBaseQuerying = list;
//		    mTkAsyncTasking = mSphinx.queryStart(list);
//		    mBaseQuerying = list; 
		}
		
		public void initData(POI poi) {
		    mPOI = poi;
		}
		
		public abstract List<DynamicPOIViewBlock> getViewList();
		
		public abstract void refresh();
		
		public abstract void msgReceived(Sphinx mSphinx, BaseQuery query, Response response);
		
		public abstract boolean checkExistence(POI poi);
		
	}
	    
	/**
	 * 进入页面时刷新所有动态POI的显示,以后再有就在onPostExecute中单刷
	 */
    final void initDynamicPOIView(POI poi) {
        checkAndAddDynamicPOIView(poi);
        //normal的团展演惠等都是随着POI一块获取的,可以直接刷新出来
        if (existView.contains(mDynamicNormalPOI)) {
            mDynamicNormalPOI.refresh();
        }
    }
    
    /**
     * 刷新动态酒店的显示区域（仅酒店类POI）
     * @param poi
     */
    final void refreshDynamicHotel(POI poi) {
        mDynamicHotelPOI.initData(poi);
        mDynamicHotelPOI.refresh();
    }
	
	//*************DynamicPOI code end*******************
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIDetail;
        initDynamicPOIEnv();
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
        
        mDynamicNormalPOI = new DynamicNormalPOI(this, mLayoutInflater);
        
        mDynamicHotelPOI = new DynamicHotelPOI(this, mLayoutInflater);
        
        mDynamicMoviePOI = new DynamicMoviePOI(this, mLayoutInflater);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.detail_info);
        mRightBtn.setBackgroundResource(R.drawable.btn_view_map);
        mRightBtn.setOnClickListener(this); 
        
        TKWeixin tkWeixin = TKWeixin.getInstance(mSphinx);
        if (tkWeixin.isWXAppInstalled() && mSphinx.getFromThirdParty() == Sphinx.THIRD_PARTY_WENXIN_REQUET) {
            mActionLog.addAction(mActionTag + ActionLog.POIDetailWeixinRequest);
            mToolsView.setVisibility(View.GONE);
            mWeixinView.setVisibility(View.VISIBLE);
        } else {
            mToolsView.setVisibility(View.VISIBLE);
            mWeixinView.setVisibility(View.GONE);
        }
        
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
        
        // Ugly
        if (mPOI.getHotel().getUuid() != null) {
            mDynamicHotelPOI.refreshPicture();
        }
        
        if (poi.getName() == null && poi.getUUID() != null) {
            mActionLog.addAction(mActionTag + ActionLog.POIDetailFromWeixin);
            List<BaseQuery> baseQueryList = new ArrayList<BaseQuery>();
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_POI);
            criteria.put(DataOperation.SERVER_PARAMETER_SUB_DATA_TYPE, DataOperation.SUB_DATA_TYPE_POI);
            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, poi.getUUID());
            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FIELD, POI.NEED_FIELD);
            int cityId = Globals.getCurrentCityInfo().getId();
            if (poi.ciytId != 0) {
                cityId = poi.ciytId;
            } else if (poi.getPosition() != null){
                cityId = MapEngine.getInstance().getCityId(poi.getPosition());
            }
            DataOperation poiQuery = new DataOperation(mSphinx);
            poiQuery.setup(criteria, cityId, getId(), getId(), mSphinx.getString(R.string.doing_and_wait));
            baseQueryList.add(poiQuery);
            mSphinx.queryStart(baseQueryList);
            mNavigationWidget.setVisibility(View.GONE);
        } else {
            if (poi.getHotel().getUuid() != null) {
                mNavigationWidget.setVisibility(View.VISIBLE);
            } else {
                mNavigationWidget.setVisibility(View.GONE);
            }
        }
        
        if (isReLogin()) {
            return;
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

        float star = poi.getGrade();
        mStartsRtb.setRating(star/2.0f);
        
        if (!TextUtils.isEmpty(category)) {
            mCategoryTxv.setText(category);
        } else {
            mCategoryTxv.setText("");
        }

        long money = poi.getPerCapity();
        if (mDynamicHotelPOI.checkExistence(poi) || poi.getHotel().getUuid() != null) {
            mMoneyTxv.setVisibility(View.GONE);
        } else if (money > -1) {
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

        if (poi != null) {
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
            
            if (poi.getCommentQuery() != null) {

                Comment lastComment = null;
                CommentResponse commentResponse = (CommentResponse) poi.getCommentQuery().getResponse();
                CommentList commentList = commentResponse.getList();
                if (commentList != null) {
                    List<Comment> list = commentList.getList();
                    if (list != null) {
                        for(int i = 0, size = list.size(); i < size; i++) {
                            Comment comment = list.get(i);
                            if (Comment.isAuthorMe(comment) > 0) {
                                poi.setMyComment(comment);
                            } else if (lastComment == null){
                                lastComment = comment;
                                break;
                            }
                        }
                    }
                }
                mCommentTipView.setVisibility(View.VISIBLE);
                if (lastComment == null) {
                    lastComment = poi.getMyComment();
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
            }
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
        if (mStampAnimation != null) {
            mStampBigImv.setVisibility(View.GONE);
            mStampAnimation.reset();
            mStampBigImv.setAnimation(null);
        }
    }

    protected void findViews() {
        mDoingView = mRootView.findViewById(R.id.doing_view);
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
        
        mBelowCommentLayout = (LinearLayout)mRootView.findViewById(R.id.below_comment);
        mBelowAddressLayout = (LinearLayout)mRootView.findViewById(R.id.below_address);
        mFeatureTxv = (LinearLayout)mRootView.findViewById(R.id.feature_txv);

        mCommentListView = (ViewGroup)mRootView.findViewById(R.id.comment_list_view);
        mCommentSumTotalView = (ViewGroup) mRootView.findViewById(R.id.comment_sum_total_view);
        
        mAddressAndPhoneView = mRootView.findViewById(R.id.address_phone_view);
        mAddressView = mRootView.findViewById(R.id.address_view);
        mAddressTxv = (TextView)mRootView.findViewById(R.id.address_txv);
        mAddressTelephoneDividerImv = (ImageView) mRootView.findViewById(R.id.address_telephome_divider_imv);
        mTelephoneView = mRootView.findViewById(R.id.telephone_view);
        mTelephoneTxv = (TextView)mRootView.findViewById(R.id.telephone_txv);
        
        mCommentTipView = mRootView.findViewById(R.id.comment_tip_view);
        mCommentTipEdt = (Button) mRootView.findViewById(R.id.comment_tip_btn);
        mLoadingView = mRootView.findViewById(R.id.loading_view);
        
        mToolsView = mRootView.findViewById(R.id.tools_view);
        mWeixinView = mRootView.findViewById(R.id.weixin_view);
        mWeixinBtn = (Button)mRootView.findViewById(R.id.weixin_btn);

        mNavigationWidget = mRootView.findViewById(R.id.navigation_widget);
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
        mWeixinBtn.setOnClickListener(this);
        mCommentTipEdt.setOnClickListener(this);
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
                Utility.telephone(mSphinx, mTelephoneTxv);
                break;
                
            case R.id.address_view:
                mActionLog.addAction(mActionTag +  ActionLog.CommonAddress);             
                Utility.queryTraffic(mSphinx, poi, mActionTag);
                break;
                
            case R.id.poi_btn:
                mActionLog.addAction(mActionTag +  ActionLog.POIDetailSearch);
                mSphinx.getPOINearbyFragment().setData(poi);
                mSphinx.showView(R.id.view_poi_nearby_search);
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
                
            case R.id.weixin_btn:
                mActionLog.addAction(mActionTag + ActionLog.POIDetailWeixinSend);
                TKWeixin tkWeixin = TKWeixin.getInstance(mSphinx);
                tkWeixin.sendResp(TKWeixin.makePOIResp(mSphinx, poi, mSphinx.getBundle()));
                mSphinx.finish();
                break;
                
            case R.id.comment_tip_btn:
                mActionLog.addAction(mActionTag +  ActionLog.POIDetailInput);
                boolean isMe = (poi.isGoldStamp() || poi.isSilverStamp());
                if (poi.getStatus() < 0) {
                    int resId;
                    if (isMe) {
                        resId = R.string.poi_comment_poi_invalid_not_update;
                    } else {
                        resId = R.string.poi_comment_poi_invalid_not_create;
                    }

                    Utility.showNormalDialog(mSphinx, 
                            mSphinx.getString(resId));
                } else {
                    EditCommentActivity.setPOI(poi, getId(), poi.getMyComment() != null ? EditCommentActivity.STATUS_MODIFY : EditCommentActivity.STATUS_NEW);
                    mSphinx.showView(R.id.activity_poi_edit_comment);
                }
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
            Utility.showNormalDialog(mSphinx, 
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
        POIReportErrorActivity.addTarget(poi);
        mSphinx.showView(R.id.activity_poi_report_error);
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

        List<POI> pois = new ArrayList<POI>();
        pois.add(poi);

        mSphinx.showPOI(pois, 0);
        ShareAPI.share(mSphinx, poi, poi.getPosition(), mActionTag);
    }
    
    public void setData(POI poi) {
        mPOI = poi;
        if (poi == null) {
            return;
        }
        if (poi.getHotel().getUuid() != null) {
            mNavigationWidget.setVisibility(View.VISIBLE);
        } else {
            mNavigationWidget.setVisibility(View.GONE);
        }
        mDoingView.setVisibility(View.VISIBLE);
        if (poi.getName() == null && poi.getUUID() != null) {
            return;
        }
        if (TKConfig.getPref(mSphinx, TKConfig.PREFS_HINT_POI_DETAIL) == null) {
            mSphinx.showHint(new String[]{TKConfig.PREFS_HINT_POI_DETAIL, TKConfig.PREFS_HINT_POI_DETAIL_WEIXIN}, new int[] {R.layout.hint_poi_detail, R.layout.hint_poi_detail_weixin});
        } else {
            mSphinx.showHint(TKConfig.PREFS_HINT_POI_DETAIL_WEIXIN, R.layout.hint_poi_detail_weixin);
        }
        mRootView.setVisibility(View.VISIBLE);
        if (mStampAnimation != null) {
            mStampBigImv.setVisibility(View.GONE);
            mStampAnimation.reset();
            mStampBigImv.setAnimation(null);
        }
        List<BaseQuery> baseQueryList = new ArrayList<BaseQuery>();
        String uuid = poi.getUUID();
        if (poi.getFrom() == POI.FROM_LOCAL && TextUtils.isEmpty(uuid) == false) {
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_POI);
            criteria.put(BaseQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, uuid);
            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FIELD, POI.NEED_FIELD);
            int cityId = Globals.getCurrentCityInfo().getId();
            if (poi.ciytId != 0) {
                cityId = poi.ciytId;
            } else if (poi.getPosition() != null){
                cityId = MapEngine.getInstance().getCityId(poi.getPosition());
            }
            DataOperation poiQuery = new DataOperation(mSphinx);
            poiQuery.setup(criteria, cityId, getId(), getId(), null);
            baseQueryList.add(poiQuery);
        }
        
        if (poi.getCommentQuery() == null || poi.getFrom() == POI.FROM_LOCAL) {
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANPING);
            criteria.put(DataQuery.SERVER_PARAMETER_POI_ID, poi.getUUID());
            criteria.put(DataQuery.SERVER_PARAMETER_REFER, DataQuery.REFER_POI);
            DataQuery commentQuery = new DataQuery(mSphinx);
            commentQuery.setup(criteria, Globals.getCurrentCityInfo().getId(), getId(), getId(), null, false, false, poi);
            baseQueryList.add(commentQuery);
            mCommentTipView.setVisibility(View.GONE);
        } else {
            mCommentTipView.setVisibility(View.VISIBLE);
        }
        
        refreshDetail();
        refreshComment();
        // DynamicPOI检测区域
        // 检查是否包含电影的动态信息
        if (mDynamicMoviePOI.checkExistence(poi)) {
            baseQueryList.add(mDynamicMoviePOI.buildQuery(poi));
        }
        //判断是否存在hotel信息
        if (mDynamicHotelPOI.checkExistence(mPOI)) {
            mDynamicHotelPOI.initDate();
            baseQueryList.addAll(mDynamicHotelPOI.generateQuery(mPOI));
        }
        clearDynamicPOI(DPOIViewBlockList);
        initDynamicPOIView(mPOI);
        
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
        mDoingView.setVisibility(View.GONE);
    }
        
    private void makeFeature(ViewGroup viewGroup) {
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
        
        if (mDynamicHotelPOI.checkExistence(mPOI)) {
            mFeatureTxv.setVisibility(View.GONE);
            return;
        } else {
            mFeatureTxv.setVisibility(View.VISIBLE);
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
                        Utility.showNormalDialog(mSphinx,
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
        Dianying dianying = null;
        boolean showErrorDialog = true;
        for(BaseQuery baseQuery : baseQueryList) {
            if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
                isReLogin = true;
                return;
            }
            Hashtable<String, String> criteria = baseQuery.getCriteria();
            String dataType = criteria.get(DataOperation.SERVER_PARAMETER_DATA_TYPE);
            Response response = baseQuery.getResponse();
            
            // 查询点评的结果
            if (baseQuery instanceof DataQuery) {
                if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, false, this, false) == false) {
                    DataQuery dataQuery = (DataQuery) baseQuery;
                    POI requestPOI = dataQuery.getPOI();
                    if (response instanceof CommentResponse) {
                        requestPOI.setCommentQuery(dataQuery);
                        refreshDetail();
                        refreshComment();
                        requestPOI.updateComment(mSphinx);
                    } else if (response instanceof DianyingResponse) {
                        DianyingResponse dianyingResponse = (DianyingResponse) response;
                        DianyingList dianyingList = dianyingResponse.getList();
                        if (dianyingList != null) {
                            requestPOI.setDynamicDianyingList(dianyingList.getList());
                            mDynamicMoviePOI.refresh();
                        }
                    }
                }
                
            } else if (baseQuery instanceof DataOperation) {
                // 查询POI的结果
                if (BaseQuery.DATA_TYPE_POI.equals(dataType)) {
                    if (poi.getName() == null && poi.getUUID() != null) {
                        if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, BaseActivity.SHOW_ERROR_MSG_TOAST, POIDetailFragment.this, true)) {
                            mActionLog.addAction(mActionTag+ActionLog.POIDetailPullFailed);
                        } else {
                            POI onlinePOI = ((POIQueryResponse)response).getPOI();
                            if (onlinePOI != null && onlinePOI.getUUID() != null && onlinePOI.getUUID().equals(poi.getUUID())) {
                                setData(onlinePOI);
                            }
                        }
                    } else {
                        mLoadingView.setVisibility(View.GONE);
                        boolean success = true;
                        if (BaseActivity.checkResponseCode(baseQuery, mSphinx, new int[]{603}, false, this, false)) {
                            if (response != null) {
                                int responseCode = response.getResponseCode();
                                if (responseCode == 603) {
                                    poi.setStatus(POI.STATUS_INVALID);
                                    BaseActivity.showErrorDialog(mSphinx, mSphinx.getString(R.string.response_code_603), this, true);
                                }
                            }
                            success = false;
                        }
                        if (success) {
                            POI onlinePOI = ((POIQueryResponse)response).getPOI();
                            if (onlinePOI != null && onlinePOI.getUUID() != null && onlinePOI.getUUID().equals(poi.getUUID())) {
                                String subDataType = baseQuery.getCriteria().get(BaseQuery.SERVER_PARAMETER_SUB_DATA_TYPE);
                                if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                                    //FIXME:移走
                                    try {
                                        poi.init(onlinePOI.getData(), false);
                                        mDynamicHotelPOI.loadSucceed(true);
                                        refreshDynamicHotel(poi);
                                        refreshDetail();
                                    } catch (APIException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    //收藏夹和历史记录进入的时候刷新POI,发现有动态信息则刷新整个POI的动态信息
                                    poi.updateData(mSphinx, onlinePOI.getData());
                                    poi.setFrom(POI.FROM_ONLINE);
                                    refreshDetail();
                                    refreshComment();
                                    initDynamicPOIView(mPOI);
                                    
                                    List<BaseQuery> list = new ArrayList<BaseQuery>();
                                    if (mDynamicHotelPOI.checkExistence(mPOI)) {
                                        mDynamicHotelPOI.initDate();
                                        list.addAll(mDynamicHotelPOI.generateQuery(mPOI));
                                    }
                                    if (mDynamicMoviePOI.checkExistence(poi)) {
                                        baseQueryList.add(mDynamicMoviePOI.buildQuery(poi));
                                    }
                                    if (list.size() > 0) {
                                        mLoadingView.setVisibility(View.VISIBLE);
                                        mTkAsyncTasking = mSphinx.queryStart(list);
                                        mBaseQuerying = list;
                                    }
                                }
                            }
                        } else {
                            //网络出问题的情况
                            if (mDynamicHotelPOI.checkExistence(mPOI)) {
                                mDynamicHotelPOI.loadSucceed(false);
                                refreshDynamicHotel(poi);
                            }
                        }
                    }
                    
                // 查询团购的结果
                } else if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType) || BaseQuery.DATA_TYPE_FENDIAN.equals(dataType)
                		|| BaseQuery.DATA_TYPE_YANCHU.equals(dataType) || BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)
                		|| BaseQuery.DATA_TYPE_COUPON.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, showErrorDialog, this, false) == false) {
                        mDynamicNormalPOI.msgReceived(mSphinx, baseQuery, response);
                    } else {
                        showErrorDialog = false;
                    }
                    
                // 电影
                } else if (BaseQuery.DATA_TYPE_DIANYING.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, showErrorDialog, this, false) == false) {
                        dianying = ((DianyingQueryResponse) response).getDianying();
                    } else {
                        showErrorDialog = false;
                    }
                // 影讯
                } else if (BaseQuery.DATA_TYPE_YINGXUN.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, showErrorDialog, this, false) == false) {
                        if (dianying != null) {
                            dianying.setYingxun(((YingxunQueryResponse) response).getYingxun());
                            List<Dianying> list = new ArrayList<Dianying>();
                            list.add(dianying);
                            dianying.getYingxun().setChangciOption(Yingxun.Changci.OPTION_DAY_TODAY);
                            mSphinx.showView(R.id.view_discover_dianying_detail);
                            mSphinx.getDianyingDetailFragment().setData(list, 0, null);
                        }
                    } else {
                        showErrorDialog = false;
                    }
                }
                
                //TODO:看情况移走
            } else if (baseQuery instanceof ProxyQuery) {
                if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, showErrorDialog, this, false) == false) {
                    //查询房态
                    if (response instanceof RoomTypeDynamic) {
                        mDynamicHotelPOI.msgReceived(mSphinx, baseQuery, response);
                    }
                } else {
                    showErrorDialog = false;
                }
            }
        }
    }
    
    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        POI poi = mPOI;
        if (poi.getName() == null) {
            dismiss();
        }
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

        CommentListActivity.setPOI(poi);
        mSphinx.showView(R.id.activity_poi_comment_list);
    }    
}
