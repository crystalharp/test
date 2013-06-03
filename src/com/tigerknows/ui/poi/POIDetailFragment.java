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
import com.tigerknows.share.ShareAPI;
import com.tigerknows.share.TKWeixin;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.HintActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.util.ShareTextUtil;

/**
 * @author Peng Wenyue
 * <ul>
 * 类型 价格 人气 口味 环境 服务 推荐菜
 * </ul>
 * 动态POI添加说明:
 *   由于动态POI在这页添加的越来越多,于是决定给这页添加一个机制,让这一页不再
 * 随着动态POI类型的增加而迅速的膨胀下去.
 * TODO:完成这个说明
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
    
    private View mNavigationWidget;
    
    DynamicNormalPOI mDynamicNormalPOI;
    
    DynamicHotelPOI mDynamicHotelPOI;
    
    private boolean mShowDynamicDianyingMoreView = true;
    
    private Button mCommentTipEdt;
    
    private View mLoadingView;
    
    private Animation mStampAnimation;
    
    private View mDoingView;

    private View mToolsView;
    
    private View mWeixinView;
    
    private Button mWeixinBtn;
    
    // TODO:移走到DynamicMoviePOI中
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
//                DynamicPOI dynamic = (DynamicPOI) object;
//                String dataType = dynamic.getType();
//                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, dataType);
//                criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
//                criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamic.getMasterUid());
//                
//                if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
//                    mActionLog.addAction(mActionTag +  ActionLog.POIDetailTuangou);
//                    criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD,
//                            Tuangou.NEED_FILELD
//                            + Util.byteToHexString(Tuangou.FIELD_NOTICED)
//                            + Util.byteToHexString(Tuangou.FIELD_CONTENT_TEXT)
//                            + Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC));
//                    criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
//                            Util.byteToHexString(Tuangou.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_LIST)+"_[0]" + ";" +
//                                    Util.byteToHexString(Tuangou.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_DETAIL)+"_[0]" + ";" +
//                                    Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_TAOCAN)+"_[0]");
//                    dataOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), POIDetailFragment.this.getId(), POIDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
//                    List<BaseQuery> list = new ArrayList<BaseQuery>();
//                    list.add(dataOperation);
//                    dataOperation = new DataOperation(mSphinx);
//                    criteria = new Hashtable<String, String>();
//                    criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
//                    criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_FENDIAN);
//                    criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamic.getSlaveUid());
//                    criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, Fendian.NEED_FILELD);
//                    dataOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), POIDetailFragment.this.getId(), POIDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
//                    list.add(dataOperation);
//                    mTkAsyncTasking = mSphinx.queryStart(list);
//                    mBaseQuerying = list;
//                } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dataType)) {
//                    mActionLog.addAction(mActionTag +  ActionLog.POIDetailYanchu);
//                    criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD,
//                            Yanchu.NEED_FILELD + Util.byteToHexString(Yanchu.FIELD_DESCRIPTION));
//                    criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
//                            Util.byteToHexString(Yanchu.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
//                                    Util.byteToHexString(Yanchu.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
//                    dataOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), POIDetailFragment.this.getId(), POIDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
//                    List<BaseQuery> list = new ArrayList<BaseQuery>();
//                    list.add(dataOperation);
//                    mTkAsyncTasking = mSphinx.queryStart(list);
//                    mBaseQuerying = list;
//                } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
//                    mActionLog.addAction(mActionTag +  ActionLog.POIDetailZhanlan);
//                    criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD,
//                            Zhanlan.NEED_FILELD + Util.byteToHexString(Zhanlan.FIELD_DESCRIPTION));
//                    criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
//                            Util.byteToHexString(Zhanlan.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
//                                    Util.byteToHexString(Zhanlan.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
//                    dataOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), POIDetailFragment.this.getId(), POIDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
//                    List<BaseQuery> list = new ArrayList<BaseQuery>();
//                    list.add(dataOperation);
//                    mTkAsyncTasking = mSphinx.queryStart(list);
//                    mBaseQuerying = list;
//                }
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
                dataOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), POIDetailFragment.this.getId(), POIDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
                list.add(dataOperation);
                
                dataOperation = new DataOperation(mSphinx);
                criteria = new Hashtable<String, String>();
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_YINGXUN);
                criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamic.getYingxun().getUid());
                
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, Yingxun.NEED_FILELD);
                dataOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), POIDetailFragment.this.getId(), POIDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
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
    //*******************DynamicPOI code start*************************
    
    List<DynamicPOIViewBlock> DPOIViewBlockList = new LinkedList<DynamicPOIViewBlock>();
    
    /*
     * 动态POI的ViewBlock排序算法,按照枚举顺序进行排序
     */
    private class DPOICompare implements Comparator<DynamicPOIViewBlock> {

        @Override
        public int compare(DynamicPOIViewBlock lhs, DynamicPOIViewBlock rhs) {
            return lhs.mType.ordinal() - rhs.mType.ordinal();
        }
    }

    /*
     * 显示动态POI的ViewBlock
     */
    private void showDynamicPOI(List<DynamicPOIViewBlock> POIList) {
        Collections.sort(POIList, new DPOICompare());
        LogWrapper.d("conan", "showPOIList:" + POIList);
        for (DynamicPOIViewBlock iter : POIList){
            iter.show();
        }
    }
    
    /*
     * 清除掉所有的动态POI的ViewBlock
     */
    private void clearDynamicPOI(List<DynamicPOIViewBlock> POIList) {
        LogWrapper.d("conan", "clearPOIList:" + POIList);
        for (DynamicPOIViewBlock iter : POIList) {
            iter.clear();
        }
        POIList.clear();
    }
    
    /*
     * 页面中原来就存在动态POI的ViewBlock,又新添加了新种类的ViewBlock时使用
     * 会先删掉原来的ViewBlock,然后调整顺序再添加上去
     */
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
    
    /*
     * 在动态POI的ViewBlock列表中删掉某种特定类型的动态POI的ViewBlock
     */
    void removeDPOIViewBlock(DPOIType t) {
        int size = DPOIViewBlockList.size();
        if (size == 0) {
            return;
        }
        for (int i = size - 1; i <= 0; i++) {
            DynamicPOIViewBlock a = DPOIViewBlockList.get(i);
            if (a.mType == t) {
                DPOIViewBlockList.remove(a);
            }
        }
    }
    
//    public interface DPOIViewInitializer<T> {
//        public T init(POIDetailFragment poiFragment, LayoutInflater inflater, LinearLayout belongsLayout, DynamicPOI data);
//    }
    
    /*
     * 每个显示块都需要有一个这个类的对象,里面存有自己的Layout和所属的Layout
     */
    public static class DynamicPOIViewBlock {
		View mOwnLayout;
		LinearLayout mBelongsLayout;
		DPOIType mType;
		boolean mLoadSucceed = true;
		
		public DynamicPOIViewBlock(LinearLayout belongsLayout, DPOIType type) {
		    mBelongsLayout = belongsLayout;
		    mType = type;
        }
		
		void show() {
		    if (mBelongsLayout.indexOfChild(mOwnLayout) == -1) {
		        mBelongsLayout.addView(mOwnLayout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		    }
		}

        void clear() {
            if (mBelongsLayout.indexOfChild(mOwnLayout) != -1) {
                mBelongsLayout.removeView(mOwnLayout);
            }
		}
        
        public String toString() {
            return "ViewBlock, type:" + mType.toString();
        }
    }
    
    /*
     * 所有新增加的动态POI需要继承这个类
     */
	public abstract static class DynamicPOIView {

		POIDetailFragment mPOIDetailFragment;
		Sphinx mSphinx;
		LayoutInflater mInflater;
		
		static void query(POIDetailFragment fragment, List<BaseQuery> list){
		    fragment.mTkAsyncTasking = fragment.mSphinx.queryStart(list);
		    fragment.mBaseQuerying = list; 
		}
		
		public abstract List<DynamicPOIViewBlock> getViewList(POI poi);
		
		public abstract void msgReceived(Sphinx mSphinx, BaseQuery query, Response response);
		
		public abstract boolean checkExistence(POI poi);
		
	}
	
	/*
	 * 清理掉加载失败的ViewBlock.
	 * 如果有的种类的动态信息在加载失败且有block显示,需要在进入的时候
	 * 不显示这些加载失败的Block,只需要把它们的mLoadSucceed置为false即可
	 */
	private void clearFailedBlock(List<DynamicPOIViewBlock> POIList) {
	    int size = (POIList == null ? 0 : POIList.size());
	    if (size == 0) {
	        return;
	    }
	    for (int i = size - 1; i >= 0; i--) {
	        DynamicPOIViewBlock block = POIList.get(i);
	        if (!block.mLoadSucceed) {
	            POIList.remove(i);
	        }
	    }
	}
    
	/**
	 * 进入页面时刷新所有动态POI的显示,以后再有就在onPostExecute中单刷
	 */
    final void showAllDynamicPOI(POI poi) {
        refreshDynamicDinaying(poi);
        refreshDynamicNormalPOI(poi);
        refreshDynamicHotel(poi);
        clearFailedBlock(DPOIViewBlockList);
        showDynamicPOI(DPOIViewBlockList);
    }
    
    /**
     * 刷新动态POI（团购、演出、展览）的显示区域
     * @param poi
     */
    final void refreshDynamicNormalPOI(POI poi) {
        if (poi == null) {
            return;
        }

        DPOIViewBlockList.addAll(mDynamicNormalPOI.getViewList(poi));
    }
    
    /**
     * 刷新动态酒店的显示区域（仅酒店类POI）
     * @param poi
     */
    final void refreshDynamicHotel(POI poi) {
        removeDPOIViewBlock(DPOIType.HOTEL);
        DPOIViewBlockList.addAll(mDynamicHotelPOI.getViewList(poi));
    }
	
	//*************DynamicPOI code end*******************
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
        
        mDynamicNormalPOI = new DynamicNormalPOI(this, mLayoutInflater);
        
        mDynamicHotelPOI = new DynamicHotelPOI(this, mLayoutInflater);
        
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
            mDynamicHotelPOI.refresh();
        }
        
        if (poi.getName() == null && poi.getUUID() != null) {
            mActionLog.addAction(mActionTag + ActionLog.POIDetailFromWeixin);
            List<BaseQuery> baseQueryList = new ArrayList<BaseQuery>();
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_POI);
            criteria.put(DataOperation.SERVER_PARAMETER_SUB_DATA_TYPE, DataOperation.SUB_DATA_TYPE_POI);
            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, poi.getUUID());
            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, POI.NEED_FILELD);
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
    /**
     * 刷新动态电影的显示区域（仅电影院类POI）
     * @param poi
     */
    // TODO:移走到DynamicMoviePOI中
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
     * 初始化动态POI（电影）列表
     * @param list
     * @param contains
     * @param resId
     * @return
     */
    // TODO:移走到DynamicMoviePOI中
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
    // TODO:移走到DynamicMoviePOI中
    void initDianyingItemView(final Dianying data, View view) {
        final ImageView pictureImv = (ImageView) view.findViewById(R.id.picture_imv);
        TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
        RatingBar starsRtb = (RatingBar) view.findViewById(R.id.stars_rtb);
        TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
        TextView addressTxv = (TextView) view.findViewById(R.id.category_txv);
        TextView dateTxv = (TextView) view.findViewById(R.id.date_txv);
        
        view.findViewById(R.id.body_view).setBackgroundDrawable(null);

        Drawable drawable = data.getPictures().loadDrawable(mSphinx, new Runnable() {
            
            @Override
            public void run() {
                Drawable drawableLoaded = data.getPictures().loadDrawable(null, null, null);
                if(drawableLoaded.getBounds().width() != pictureImv.getWidth() || drawableLoaded.getBounds().height() != pictureImv.getHeight() ){
                    pictureImv.setBackgroundDrawable(null);
                }
                pictureImv.setBackgroundDrawable(drawableLoaded);
            }
        }, POIDetailFragment.this.toString());
        if(drawable != null) {
            //To prevent the problem of size change of the same pic 
            //After it is used at a different place with smaller size
            if( drawable.getBounds().width() != pictureImv.getWidth() || drawable.getBounds().height() != pictureImv.getHeight() ){
                pictureImv.setBackgroundDrawable(null);
            }
            pictureImv.setBackgroundDrawable(drawable);
        } else {
            pictureImv.setBackgroundDrawable(null);
        }
        
        nameTxv.setText(data.getName());
        starsRtb.setProgress((int) data.getRank());
        distanceTxv.setVisibility(View.GONE);
        
        addressTxv.setText(data.getTag());
        if (TextUtils.isEmpty(data.getLength())) {
            dateTxv.setText(R.string.dianying_no_length_now);
        } else {
            dateTxv.setText(String.valueOf(data.getLength()));
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
        
//        mDynamicPOIListView = (LinearLayout) mRootView.findViewById(R.id.dynamic_poi_list_view);
        
        mCommentTipView = mRootView.findViewById(R.id.comment_tip_view);
        mCommentTipEdt = (Button) mRootView.findViewById(R.id.comment_tip_btn);
        mLoadingView = mRootView.findViewById(R.id.loading_view);
        
        mToolsView = mRootView.findViewById(R.id.tools_view);
        mWeixinView = mRootView.findViewById(R.id.weixin_view);
        mWeixinBtn = (Button)mRootView.findViewById(R.id.weixin_btn);

        mDynamicDianyingView = (LinearLayout) mRootView.findViewById(R.id.dynamic_dianying_view);
        mDynamicDianyingListView = (LinearLayout) mRootView.findViewById(R.id.dynamic_dianying_list_view);
        mDynamicDianyingMoreView = (LinearLayout) mRootView.findViewById(R.id.dynamic_dianying_more_view);
        
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
        mDynamicDianyingMoreView.setOnClickListener(this);
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
                
            // TODO:移走到DynamicMoviePOI中
            case R.id.dynamic_dianying_more_view:
                mActionLog.addAction(mActionTag + ActionLog.POIDetailDianyingMore);
                mShowDynamicDianyingMoreView = false;
                mDynamicDianyingMoreView.setVisibility(View.GONE);
                
                List<Dianying> list = poi.getDynamicDianyingList();
            	int size = (list != null ? list.size() : 0);
                int viewCount = mDynamicDianyingListView.getChildCount();
                for(int i = SHOW_DYNAMIC_YINGXUN_MAX; i < viewCount && i < size; i++) {
                    View v = mDynamicDianyingListView.getChildAt(i);
                    v.setVisibility(View.VISIBLE);
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
        clearDynamicPOI(DPOIViewBlockList);
        if (TKConfig.getPref(mSphinx, TKConfig.PREFS_HINT_POI_DETAIL) == null) {
            mSphinx.showHint(new String[]{TKConfig.PREFS_HINT_POI_DETAIL, TKConfig.PREFS_HINT_POI_DETAIL_WEIXIN}, new int[] {R.layout.hint_poi_detail, R.layout.hint_poi_detail_weixin});
        } else {
            mSphinx.showHint(TKConfig.PREFS_HINT_POI_DETAIL_WEIXIN, R.layout.hint_poi_detail_weixin);
        }
        mShowDynamicDianyingMoreView = true;
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
            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, POI.NEED_FILELD);
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
        DataQuery dataQuery = checkDianying(poi);
        if (dataQuery != null) {
            baseQueryList.add(dataQuery);
        }
        //判断是否存在hotel信息
        if (mDynamicHotelPOI.checkExistence(mPOI)) {
            mDynamicHotelPOI.initDate();
            baseQueryList.addAll(mDynamicHotelPOI.generateQuery(mPOI));
        }
        showAllDynamicPOI(mPOI);
        
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
    
    // TODO:移走到DynamicMoviePOI中
    private DataQuery checkDianying(POI poi) {
        DataQuery dataQuery = null;
        if (poi == null) {
            return dataQuery;
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
            dataQuery = new DataQuery(mSphinx);
            dataQuery.setup(criteria, Globals.getCurrentCityInfo().getId(), getId(), getId(), null, false, false, poi);
        }
        
        return dataQuery;
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
                            refreshDynamicDinaying(poi);
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
                                    refreshDynamicPOI(DPOIViewBlockList);
                                } else {
                                    //收藏夹和历史记录进入的时候刷新POI,发现有动态信息则刷新整个POI的动态信息
                                    poi.updateData(mSphinx, onlinePOI.getData());
                                    poi.setFrom(POI.FROM_ONLINE);
                                    refreshDetail();
                                    refreshComment();
                                    showAllDynamicPOI(mPOI);
                                    
                                    List<BaseQuery> list = new ArrayList<BaseQuery>();
                                    if (mDynamicHotelPOI.checkExistence(mPOI)) {
                                        mDynamicHotelPOI.initDate();
                                        list.addAll(mDynamicHotelPOI.generateQuery(mPOI));
                                    }
                                    DataQuery dataQuery = checkDianying(poi);
                                    if (dataQuery != null) {
                                        list.add(dataQuery);
                                    }
                                    if (list.size() > 0) {
                                        mLoadingView.setVisibility(View.VISIBLE);
                                        mTkAsyncTasking = mSphinx.queryStart(list);
                                        mBaseQuerying = list;
                                    }
                                }
                            }
                        } else {
                            mDynamicHotelPOI.loadSucceed(false);
                            refreshDynamicHotel(poi);
                            refreshDynamicPOI(DPOIViewBlockList);
                        }
                    }
                    
                // 查询团购的结果
                } else if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType) || BaseQuery.DATA_TYPE_FENDIAN.equals(dataType)
                        || BaseQuery.DATA_TYPE_YANCHU.equals(dataType) || BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                    mDynamicNormalPOI.msgReceived(mSphinx, baseQuery, response);
                    
                // 电影
                } else if (BaseQuery.DATA_TYPE_DIANYING.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false) == false) {
                        dianying = ((DianyingQueryResponse) response).getDianying();
                    }
                // 影讯
                } else if (BaseQuery.DATA_TYPE_YINGXUN.equals(dataType)) {
                    if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false) == false && dianying != null) {
                        dianying.setYingxun(((YingxunQueryResponse) response).getYingxun());
                        List<Dianying> list = new ArrayList<Dianying>();
                        list.add(dianying);
                        dianying.getYingxun().setChangciOption(Yingxun.Changci.OPTION_DAY_TODAY);
                        mSphinx.showView(R.id.view_discover_dianying_detail);
                        mSphinx.getDianyingDetailFragment().setData(list, 0, null);
                    }
                }
                
                //TODO:看情况移走
            } else if (baseQuery instanceof ProxyQuery) {
                if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false) == false) {
                    //查询房态
                    if (response instanceof RoomTypeDynamic) {
                        mDynamicHotelPOI.msgReceived(mSphinx, baseQuery, response);
                    }
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
