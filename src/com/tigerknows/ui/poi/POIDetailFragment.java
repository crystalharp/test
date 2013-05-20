/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Calendar;
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
import com.tigerknows.model.Fendian;
import com.tigerknows.model.Hotel;
import com.tigerknows.model.Hotel.RoomType;
import com.tigerknows.model.POI;
import com.tigerknows.model.ProxyQuery;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Yingxun;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.model.DataOperation.POIQueryResponse;
import com.tigerknows.model.DataOperation.YingxunQueryResponse;
import com.tigerknows.model.DataQuery.CommentResponse;
import com.tigerknows.model.DataQuery.DianyingResponse;
import com.tigerknows.model.DataQuery.CommentResponse.CommentList;
import com.tigerknows.model.DataQuery.DianyingResponse.DianyingList;
import com.tigerknows.model.POI.Description;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.util.ShareTextUtil;
import com.tigerknows.util.WidgetUtils;

/**
 * @author Peng Wenyue
 * <ul>
 * 类型 价格 人气 口味 环境 服务 推荐菜
 * </ul>
 * 
 */
public class POIDetailFragment extends BaseFragment implements View.OnClickListener, OnTouchListener {
    
	static final int SHOW_DYNAMIC_YINGXUN_MAX = 3;
    
    public POIDetailFragment(Sphinx sphinx) {
        super(sphinx);
    }
    
    //动态POI枚举类型，他们在枚举类型中的顺序会决定它们的显示顺序.
    //添加新的显示类型时需要添加它在枚举类型中对应的项。
    public static enum DPOIType {HOTEL, MOVIE, COUPON, GROUPBUY, EXHIBITION, SHOW};
    
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
//    
//    private LinearLayout mDynamicTuanguoListView;
//    
//    private LinearLayout mDynamicYanchuListView;
//    
//    private LinearLayout mDynamicZhanlanListView;
    
    private LinearLayout mDynamicDianyingView;
    
    private List<DynamicPOI> mDynamicDianyingList = new ArrayList<DynamicPOI>();
    
    private LinearLayout mDynamicDianyingListView;
    
    private LinearLayout mDynamicDianyingMoreView;
    
    DynamicNormalPOI mDynamicNormalPOI;
    
    DynamicHotelPOI mDynamicHotelPOI;
    
//    private LinearLayout mDynamicHotelUpperView;
    
    private boolean mShowDynamicDianyingMoreView = true;
    
    private Button mCommentTipEdt;
    
    private View mLoadingView;
    
    private Animation mStampAnimation;
    
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
            if (mSphinx.uiStackPeek() == getId() && mSphinx.isFinishing() == false) {
                POI poi = mPOI;
                if (poi == null) {
                    return;
                }
                List<Dianying> list = poi.getDynamicDianyingList();
                if (list == null) {
                    return;
                }
                int childCount = mDynamicDianyingListView.getChildCount();
                for(int i = 0, size = list.size(); i < childCount && i < size; i++) {
                    final Dianying dynamic = list.get(i);
                    View child = mDynamicDianyingListView.getChildAt(i);
                    if (child.getVisibility() == View.VISIBLE) {
                        ImageView iconImv = (ImageView) child.findViewById(R.id.icon_imv);
                        TKDrawable tkDrawable = dynamic.getPictures();
                        if (tkDrawable != null) {
                            Drawable drawable = tkDrawable.loadDrawable(mSphinx, null, POIDetailFragment.this.toString());
                            iconImv.setBackgroundDrawable(drawable);
                        } else {
                            iconImv.setBackgroundDrawable(null);
                        }
                    } else {
                        break;
                    }
                }
            }
        }
    };
    
    private OnClickListener mDynamicPOIListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View view) {
            Object object = view.getTag();
            if (object == null) {
                return;
            }
            DataOperation dataOperation = new DataOperation(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            if (object instanceof DynamicPOI) {
                DynamicPOI dynamic = (DynamicPOI) object;
                String dataType = dynamic.getType();
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, dataType);
                criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamic.getMasterUid());
                
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
                    criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamic.getSlaveUid());
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
            } else if (object instanceof Dianying) {
                Dianying dynamic = (Dianying) object;
                List<BaseQuery> list = new ArrayList<BaseQuery>();

                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_DIANYING);
                criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamic.getUid());
                
                dataOperation = new DataOperation(mSphinx);
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD,
                        Dianying.NEED_FILELD_ONLY_DIANYING
                        + Util.byteToHexString(Dianying.FIELD_DESCRIPTION));
                criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
                        Util.byteToHexString(Dianying.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
                        Util.byteToHexString(Dianying.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
                dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), POIDetailFragment.this.getId(), POIDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
                list.add(dataOperation);
                
                dataOperation = new DataOperation(mSphinx);
                criteria = new Hashtable<String, String>();
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_YINGXUN);
                criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamic.getYingxun().getUid());
                
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, Yingxun.NEED_FILELD);
                dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), POIDetailFragment.this.getId(), POIDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
                list.add(dataOperation);
                
                mTkAsyncTasking = mSphinx.queryStart(list);
                mBaseQuerying = list;
            }
            
        }
    };
    
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
    //*******************new code start*************************
    
    ArrayList<DynamicPOIViewBlock> DPOIViewBlockList = new ArrayList<DynamicPOIViewBlock>();
    
    private class DPOICompare implements Comparator<DynamicPOIViewBlock> {

        @Override
        public int compare(DynamicPOIViewBlock lhs, DynamicPOIViewBlock rhs) {
            return lhs.mType.ordinal() - rhs.mType.ordinal();
        }
    }

    private void showDynamicPOI(List<DynamicPOIViewBlock> POIList){
        Collections.sort(POIList, new DPOICompare());
        LogWrapper.d("conan", "showPOIList:" + POIList);
        for (DynamicPOIViewBlock iter : POIList){
            iter.show();
        }
    }
    
    private void clearDynamicPOI(List<DynamicPOIViewBlock> POIList){
        LogWrapper.d("conan", "clearPOIList:" + POIList);
        for (DynamicPOIViewBlock iter : POIList) {
            iter.clear();
        }
        POIList.clear();
    }
    
    private void refreshDynamicPOI(List<DynamicPOIViewBlock> POIList){
        for (DynamicPOIViewBlock iter : POIList) {
            iter.clear();
        }
        Collections.sort(POIList, new DPOICompare());
        LogWrapper.d("conan", "refreshPOIList:" + POIList);
        for (DynamicPOIViewBlock iter : POIList){
            iter.show();
        }
    }
    
    public interface DPOIViewInitializer<T> {
        public T init(POIDetailFragment poiFragment, LayoutInflater inflater, LinearLayout belongsLayout, DynamicPOI data);
    }
    
//    public interface DPOIQueryInterface {
//        //setData的时候检查是否存在该类型的动态POI信息
//		public List<BaseQuery> checkExistence(POI poi);
//		//处理返回的response
//		public void msgReceived(Sphinx mSphinx, BaseQuery query, Response response);
//    }
    
    public static class DynamicPOIViewBlock {
		View mOwnLayout;
		LinearLayout mBelongsLayout;
		DPOIType mType;
//		boolean needToShow = true;
		
		public DynamicPOIViewBlock(LinearLayout belongsLayout, DPOIType type) {
		    mBelongsLayout = belongsLayout;
		    mType = type;
        }
		
		void show(){
		    if (mBelongsLayout.indexOfChild(mOwnLayout) == -1){
		        mBelongsLayout.addView(mOwnLayout);
		    }
		}

        void clear(){
            if (mBelongsLayout.indexOfChild(mOwnLayout) != -1) {
                mBelongsLayout.removeView(mOwnLayout);
            }
//			needToShow = false;
		}
    }
    
	public abstract static class DynamicPOIView {

		POIDetailFragment mPOIDetailFragment;
		Sphinx mSphinx;
		LayoutInflater mInflater;
		
		static void query(POIDetailFragment fragment, List<BaseQuery> list){
		    fragment.mTkAsyncTasking = fragment.mSphinx.queryStart(list);
		    fragment.mBaseQuerying = list; 
		}
		
		protected abstract void addDynamicPOIViewBlock(LinearLayout belongsLayout);
		
		public abstract List<DynamicPOIViewBlock> getViewList(POI poi);
		
		public abstract void msgReceived(Sphinx mSphinx, BaseQuery query, Response response);
		
		public abstract boolean checkExistence(POI poi);
		
//		static <T> T getInstance(POIDetailFragment poiFragment, LinearLayout belongsLayout, DynamicPOI data, DPOIViewInitializer<T> initer, ArrayList<DynamicPOIView> DPOIPool){
//		    DynamicPOIView instance = null;
//		    if (DPOIPool.size() == 0) {
//		        instance = (DynamicPOIView) initer.init(poiFragment, poiFragment.mLayoutInflater, belongsLayout, data);
//		        DPOIPool.add(instance);
//		    } else {
//		       //遍历缓冲池 
//		        for (DynamicPOIView iter : DPOIPool) {
//		            //如果有不用的，则使用它
//		            LogWrapper.d("conan", "iter.needtoshow:" + iter.needToShow);
//		            if (!iter.needToShow) {
//		                instance = iter;
//		                instance.refreshData(data);
//		                instance.mBelongsLayout = belongsLayout;
//		                instance.needToShow = true;
//		                break;
//		            }
//		        }
//		        //遍历完发现都在用，则创建个新的
//		        if (instance == null) {
//    		        instance = (DynamicPOIView) initer.init(poiFragment, poiFragment.mLayoutInflater, belongsLayout, data);
//    		        DPOIPool.add(instance);
//		        }
//		    }
//		    return (T)instance;
//		}
		
//		public abstract void refreshData(DynamicPOI data);
		
	}
	
	//用来给动态POI类提供查询接口
//	public void query(List<BaseQuery> list){
//        mTkAsyncTasking = mSphinx.queryStart(list);
//        mBaseQuerying = list;
//	}
    
	//*************new code end*******************
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
        
        mDynamicNormalPOI = DynamicNormalPOI.getInstance(this, mLayoutInflater);
        
        mDynamicHotelPOI = DynamicHotelPOI.getInstance(this, mLayoutInflater);
        
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
        
        String perCapity = poi.getPerCapity();
        if (perCapity != null) {
            mMoneyTxv.setText(perCapity);
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
        
        refreshDynamicPOI(poi);
        
        refreshDynamicDinaying(poi);
        refreshDynamicHotel(poi);
    }
    
    /**
     * 刷新动态POI（团购、演出、展览）的显示区域
     * @param poi
     */
    void refreshDynamicPOI(POI poi) {
    	if (poi == null) {
    		return;
    	}
//    	clearDynamicPOI(DPOIViewBlockList);

//        List<DynamicPOI> list = poi.getDynamicPOIList();
//        List<DynamicPOI> normalDynamicPOIList = new LinkedList<DynamicPOI>();
//        int size = (list != null ? list.size() : 0);
//        for(int i = 0; i < size; i++) {
//            final DynamicPOI dynamicPOI = list.get(i);
//            final String dataType = dynamicPOI.getType();
//            if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType) ||
//                    BaseQuery.DATA_TYPE_YANCHU.equals(dataType) ||
//                    BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
//                normalDynamicPOIList.add(dynamicPOI);
//            }
//        }
        
        DPOIViewBlockList.addAll(mDynamicNormalPOI.getViewList(poi));
    }
    
    /**
     * 刷新动态酒店的显示区域（仅酒店类POI）
     * @param poi
     */
    final void refreshDynamicHotel(POI poi) {
       //显示 
//        if (poi == null || poi.getHotel() == null || poi.getHotel().getRoomTypeList() == null) {
//            return;
//        }
        
//        List<Hotel> list = new LinkedList<Hotel>();
//        list.add(poi.getHotel());
        DPOIViewBlockList.addAll(mDynamicHotelPOI.getViewList(poi));
//        DPOIViewBlockList.addAll(mDynamicHotelPOI.getViewList(list));
    }
    /**
     * 刷新动态电影的显示区域（仅电影院类POI）
     * @param poi
     */
    void refreshDynamicDinaying(POI poi) {
        if (poi == null) {
            return;
        }
        
        List<Dianying> list = poi.getDynamicDianyingList();        
        int size = (list != null ? list.size() : 0);
        mDynamicDianyingView.setVisibility(size > 0 ? View.VISIBLE : View.GONE);
        
        int viewCount = initDynamicPOIListView(list, mDynamicDianyingListView, R.layout.poi_dynamic_dianying_list_item);
        
        int childCount = mDynamicDianyingListView.getChildCount();
        if (size > SHOW_DYNAMIC_YINGXUN_MAX && mShowDynamicDianyingMoreView) {
            for(int i = SHOW_DYNAMIC_YINGXUN_MAX; i < childCount; i++) {
                mDynamicDianyingListView.getChildAt(i).setVisibility(View.GONE);
            }
            mDynamicDianyingMoreView.setVisibility(View.VISIBLE);
        } else {
            mDynamicDianyingMoreView.setVisibility(View.GONE);
        }
        
        for(int i = 0; i < viewCount; i++) {
            View child = mDynamicDianyingListView.getChildAt(i);
            if (i == (viewCount-1) && mDynamicDianyingMoreView.getVisibility() == View.GONE) {
                child.setBackgroundResource(R.drawable.list_footer);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.GONE);
            } else {
                child.setBackgroundResource(R.drawable.list_middle);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
            }
        }
    }
    
    @SuppressWarnings("rawtypes")
    /**
     * 初始化动态POI（团购、演出、展览、电影）列表
     * @param list
     * @param contains
     * @param resId
     * @return
     */
    int initDynamicPOIListView(List list, ViewGroup contains, int resId) {
        int childCount = contains.getChildCount();
        int viewCount = 0;
        int dataSize = (list != null ? list.size() : 0);
        if(dataSize == 0){
            contains.setVisibility(View.GONE);
        }else{
            contains.setVisibility(View.VISIBLE);
            for(int i = 0; i < dataSize; i++) {
                Object data = list.get(i);
                View child;
                if (viewCount < childCount) {
                    child = contains.getChildAt(viewCount);
                    child.setVisibility(View.VISIBLE);
                } else {
                    child = mLayoutInflater.inflate(resId, contains, false);
                    contains.addView(child, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                }
                child.setTag(data);
                child.setOnClickListener(mDynamicPOIListener);
                if (data instanceof DynamicPOI) {
//                    initDynamicPOIItemView((DynamicPOI)data, child);
                } else if (data instanceof Dianying) {
                    initDianyingItemView((Dianying)data, child);
                }
                viewCount++;
            }
            
            childCount = contains.getChildCount();
            for(int i = viewCount; i < childCount; i++) {
                contains.getChildAt(i).setVisibility(View.GONE);
            }
        }
        
        return viewCount;
    }
    
    /**
     * 初始化动态POI（团购、演出、展览）列表项的内容
     * @param data
     * @param view
     */
//    void initDynamicPOIItemView(DynamicPOI data, View view) {
//        ImageView iconImv = (ImageView) view.findViewById(R.id.icon_imv);
//        TextView textTxv = (TextView) view.findViewById(R.id.text_txv);
//        if (BaseQuery.DATA_TYPE_TUANGOU.equals(data.getType())) {
//            iconImv.setImageResource(R.drawable.ic_dynamicpoi_tuangou);
//        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(data.getType())) {
//            iconImv.setImageResource(R.drawable.ic_dynamicpoi_yanchu);
//        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(data.getType())) {
//            iconImv.setImageResource(R.drawable.ic_dynamicpoi_zhanlan);
//        }
//        textTxv.setText(data.getSummary());
//    }
    
    /**
     * 初始化电影列表项的内容
     * @param data
     * @param view
     */
    void initDianyingItemView(Dianying data, View view) {
        ImageView iconImv = (ImageView) view.findViewById(R.id.icon_imv);
        TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
        TextView gradeTxv = (TextView) view.findViewById(R.id.grade_txv);
        TextView typeTxv = (TextView) view.findViewById(R.id.type_txv);
        TextView lengthTxv = (TextView) view.findViewById(R.id.length_txv);
        TKDrawable tkDrawable = data.getPictures();
        if (tkDrawable != null) {
            Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, POIDetailFragment.this.toString());
            iconImv.setBackgroundDrawable(drawable);
        } else {
            iconImv.setBackgroundDrawable(null);
        }
        nameTxv.setText(data.getName());
        gradeTxv.setText(String.valueOf(data.getRank()));
        typeTxv.setText(data.getTag());
        lengthTxv.setText(data.getLength());
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
//        clearDynamicPOI(DPOIList);
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
        
//        mDynamicPOIListView = (LinearLayout) mRootView.findViewById(R.id.dynamic_poi_list_view);
        
        mCommentTipView = mRootView.findViewById(R.id.comment_tip_view);
        mCommentTipEdt = (Button) mRootView.findViewById(R.id.comment_tip_btn);
        mLoadingView = mRootView.findViewById(R.id.loading_view);

        mDynamicDianyingView = (LinearLayout) mRootView.findViewById(R.id.dynamic_dianying_view);
        mDynamicDianyingListView = (LinearLayout) mRootView.findViewById(R.id.dynamic_dianying_list_view);
        mDynamicDianyingMoreView = (LinearLayout) mRootView.findViewById(R.id.dynamic_dianying_more_view);
        
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
        mDynamicDianyingMoreView.setOnClickListener(this);
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

                        Utility.showNormalDialog(mSphinx, 
                                mSphinx.getString(resId));
                    } else {
                        EditCommentActivity.setPOI(poi, getId(), poi.getMyComment() != null ? EditCommentActivity.STATUS_MODIFY : EditCommentActivity.STATUS_NEW);
                        mSphinx.showView(R.id.activity_poi_edit_comment);
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

            case R.id.dynamic_dianying_more_view:
                mShowDynamicDianyingMoreView = false;
                mDynamicDianyingMoreView.setVisibility(View.GONE);
                
                List<Dianying> list = poi.getDynamicDianyingList();
                int size = (list != null ? list.size() : 0);
                int viewCount = mDynamicDianyingListView.getChildCount();
                for(int i = 0; i < viewCount && i < size; i++) {
                    View v = mDynamicDianyingListView.getChildAt(i);
                    v.setVisibility(View.VISIBLE);
                    if (i >= viewCount) {
                        initDianyingItemView(list.get(i), v);
                    }
                }
                
                for(int i = 0; i < viewCount; i++) {
                    View child = mDynamicDianyingListView.getChildAt(i);
                    if (i == (viewCount-1) && mDynamicDianyingMoreView.getVisibility() == View.GONE) {
                        child.setBackgroundResource(R.drawable.list_footer);
                        child.findViewById(R.id.list_separator_imv).setVisibility(View.GONE);
                    } else {
                        child.setBackgroundResource(R.drawable.list_middle);
                        child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
                    }
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

        String smsContent = ShareTextUtil.sharePOISmsContent(poi,mContext);
    	String weiboContent = ShareTextUtil.sharePOIWeiboContent(poi, mContext);
    	String qzoneContent = ShareTextUtil.sharePOIQzoneContent(poi, mContext);
    	
    	List<POI> pois = new ArrayList<POI>();
    	pois.add(poi);

    	mSphinx.showPOI(pois, 0);
    	WidgetUtils.share(mSphinx, smsContent, weiboContent, qzoneContent, poi.getPosition(), mActionTag);
    }
    
    public void setData(POI poi) {
    	mShowDynamicDianyingMoreView = true;
        if (mStampAnimation != null) {
            mStampBigImv.setVisibility(View.GONE);
            mStampAnimation.reset();
            mStampBigImv.setAnimation(null);
        }
        mPOI = poi;
        if (null != poi) {
        	clearDynamicPOI(DPOIViewBlockList);
        	
            List<BaseQuery> baseQueryList = new ArrayList<BaseQuery>();
            String uuid = poi.getUUID();
            if (poi.getFrom() == POI.FROM_LOCAL && TextUtils.isEmpty(uuid) == false) {
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_POI);
                criteria.put(BaseQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
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
         
            // 检查是否包含电影的动态信息
            boolean isContainDianying = false;
            List<DynamicPOI> list = poi.getDynamicPOIList();
            if (list != null) {
                for(int i = 0, size = list.size(); i < size; i++) {
                    DynamicPOI dynamic = list.get(i);
                    if (BaseQuery.DATA_TYPE_DIANYING.equals(dynamic.getType())) {
                        isContainDianying = true;
                        break;
                    }
                }
            }
            
            if (isContainDianying && poi.getDynamicDianyingList() == null) {
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANYING);
                criteria.put(DataQuery.SERVER_PARAMETER_POI_ID, poi.getUUID());
                criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD,
                        Dianying.NEED_FILELD_POI_DETAIL);
                criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
                       Util.byteToHexString(Dianying.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]");
                DataQuery dataQuery = new DataQuery(mSphinx);
                dataQuery.setup(criteria, Globals.g_Current_City_Info.getId(), getId(), getId(), null, false, false, poi);
                baseQueryList.add(dataQuery);
            }
            
            refreshDetail();
            refreshComment();
            showDynamicPOI(DPOIViewBlockList);
            
            //判断是否存在hotel信息
            if (mDynamicHotelPOI.checkExistence(mPOI)) {
                baseQueryList.addAll(mDynamicHotelPOI.generateQuery(mPOI));
            }
//            LogWrapper.d("conan", "POIDetailFragment.baseQueryList:" + baseQueryList);
            
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
        Tuangou tuangou = null;
        Dianying dianying = null;
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
                POI requestPOI = dataQuery.getPOI();
                if (response instanceof CommentResponse) {
                    requestPOI.setCommentQuery(dataQuery);
                    refreshDetail();
                    refreshComment();
                    requestPOI.updateComment(mSphinx);
                    mCommentTipEdt.setVisibility(View.VISIBLE);
                } else if (response instanceof DianyingResponse) {
                    DianyingResponse dianyingResponse = (DianyingResponse) response;
                    DianyingList dianyingList = dianyingResponse.getList();
                    if (dianyingList != null) {
                        requestPOI.setDynamicDianyingList(dianyingList.getList());
                        refreshDynamicDinaying(poi);
                    }
                }
                
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
                        String subDataType = baseQuery.getCriteria().get(BaseQuery.SERVER_PARAMETER_SUB_DATA_TYPE);
                        if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                            //FIXME:移走
                            Hotel hotel = poi.getHotel();
                            try {
                                if (hotel == null) {
                                    poi.init(onlinePOI.getData(), false);
                                } else {
                                    hotel.init(onlinePOI.getData(), false);
                                }
                                refreshDynamicHotel(poi);
                            } catch (APIException e) {
                                e.printStackTrace();
                            }
//                            List<Hotel> dataList = new LinkedList<Hotel>();
//                            dataList.add(hotel);
//                            DPOIViewBlockList.addAll(mDynamicHotelPOI.getViewList(dataList));
                            refreshDynamicPOI(DPOIViewBlockList);
                        } else {
                            poi.updateData(mSphinx, onlinePOI.getData());
                            poi.setFrom(POI.FROM_ONLINE);
                            refreshDetail();
                            refreshComment();
                        }
                    }
                    
                // 查询团购的结果
                } else if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType) || BaseQuery.DATA_TYPE_FENDIAN.equals(dataType)
                        || BaseQuery.DATA_TYPE_YANCHU.equals(dataType) || BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                    mDynamicNormalPOI.msgReceived(mSphinx, baseQuery, response);
                    
                // 电影
                } else if (BaseQuery.DATA_TYPE_DIANYING.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                        return;
                    }
                    dianying = ((DianyingQueryResponse) response).getDianying();
                // 影讯
                } else if (BaseQuery.DATA_TYPE_YINGXUN.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                        return;
                    }
                    dianying.setYingxun(((YingxunQueryResponse) response).getYingxun());
                    List<Dianying> list = new ArrayList<Dianying>();
                    list.add(dianying);
                    dianying.getYingxun().setChangciOption(Yingxun.Changci.OPTION_DAY_TODAY&Yingxun.Changci.OPTION_DAY_TOMORROW&Yingxun.Changci.OPTION_DAY_AFTER_TOMORROW);
                    mSphinx.showView(R.id.view_discover_dianying_detail);
                    mSphinx.getDianyingDetailFragment().setData(list, 0, null);
                }
                
                //TODO:看情况移走
            } else if (baseQuery instanceof ProxyQuery) {
                if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                    return;
                }
                //查询房态
                if (response instanceof RoomTypeDynamic) {
                    mDynamicHotelPOI.msgReceived(mSphinx, baseQuery, response);
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

        CommentListActivity.setPOI(poi);
        mSphinx.showView(R.id.activity_poi_comment_list);
    }    
}
