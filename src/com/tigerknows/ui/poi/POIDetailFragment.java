/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import java.util.ArrayList;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;


import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.TKFragmentManager;
import com.tigerknows.android.app.TKActivity;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;

import com.tigerknows.common.ActionLog;
import com.tigerknows.map.CityInfo;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.ItemizedOverlayHelper;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Comment;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.DataOperation.POIQueryResponse;
import com.tigerknows.model.DataQuery.CommentResponse;
import com.tigerknows.model.DataQuery.CommentResponse.CommentList;
import com.tigerknows.model.POI.Description;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.share.ShareAPI;
import com.tigerknows.share.TKWeixin;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.poi.POIResultFragment.POIAdapter;
import com.tigerknows.util.Utility;

/**
 * @author Peng Wenyue
 * <ul>
 * 类型 价格 人气 口味 环境 服务 推荐菜
 * </ul>
 *   由于动态POI在这页添加的越来越多,于是决定给这页添加一个机制,让这一页不再
 * 随着动态POI类型的增加而迅速的膨胀下去.
 *   经过两次调整,觉得这个结构应该可以用下去.[2013-06-16]
 *   
 * 动态POI添加指南:
 *   1.新建一个类(如DynamicNormalPOI等)来继承DynamicPOIView作为该动态POI的主要控制类.
 *   2.在本页定义一个该类变量,并在onCreatView的时候初始化.
 *   3.在本页的initDynamicPOIEnv函数中添加其在动态POI中的type和该类实例化的对象
 *   4.setData或者initDynamicPOIView中进行新动态POI的处理。若可以根据动态POI直接显示
 * 就在initDynamicPOIView中直接调用该POI的refresh，需要再次查询则在setData中调用该类
 * 的queryStart进行查询。
 *   ！注意：在动态POI中要使用继承类queryStart方法，不要使用mSphinx.queryStart，这样
 * 才能把其查询队列放到自己的OnPostExecute函数中处理.
 *   5.给继承类创建ViewBlock对象，实现getViewList方法来返回这些ViewBlock对象。
 *   6.给每个ViewBlock实现一个BlockRefresher对象，用来刷新那个ViewBlock。
 *   7.实现继承类的refresh函数，可以根据不同状态来控制每个ViewBlock的刷新。
 *   8.实现继承类的OnPostExecute函数，用来处理自己发出的各种请求。
 *   
 * 机制:
 *   0.动态POI的显示使用了动态添加的方式,显示单位为ViewBlock,即一个显示区块.下面的类
 * DPOIViewBlock为ViewBlock的控制类,包含一个ViewBlock可能用到的各种显示相关的方法.
 *   每个ViewBlock中含有了它自己的Layout,mOwnLayout和它所需要添加到的layout,
 * mBelongsLayout.这样可以在进入页面的时候把layout添加到对应的位置。
 *   每个ViewBlock需要有一个刷新方法,即实例化时需要传入一个该实例的刷新接口函数,
 * BlockRefresher.
 *   动态POI使用一次添加多次刷新的方法,即检测到动态POI存在就把它对应的ViewBlock加入
 * 到该页中,在数据变化的时候自己去刷新自己.
 *   1.该页有两个空的LinearLayout,在页面setData的时候把POI含有的动态POI列表所对应的
 * ViewBlock添加到本页中,并把Visibility设置为View.GONE.
 *   2.ViewBlock的refresh函数会在刷新完后把该块的Visibility设置为View.VISIBLE
 *   
 * 布局：
 *   为了让服务器可以控制动态POI的显示顺序，给服务器预留了排序接口，可以动态
 * 调整不同POI的顺序。为了动态调整的时候不同block的间隔不会出问题，现约定每个
 * block下面留8dp的padding，用来放动态Block的容器LinearLayout本身不带高度，这
 * 样调整顺序和不存在数据的时候都不会导致上下间距出问题
 * 
 * 注：
 *   因为该页是个ScrollView，不能用ListView作为其子控件，一个个的实现list很麻烦，
 * 于是写了一个LinearListView在com.tigerknows.widget中，可以用来实现类似于listview
 * 的功能。
 * 动态POI的重构记录在wiki上有,地址如下:
 * http://192.168.11.147/wiki/index.php/Android的灵活动态布局-POI详情页重构
 * http://192.168.11.147/wiki/index.php/重装上阵-POI详情页的动态POI方案改进
 */
public class POIDetailFragment extends BaseFragment implements View.OnClickListener, OnTouchListener {
    
    public POIDetailFragment(Sphinx sphinx) {
        super(sphinx);
    }
    
    private ScrollView mBodyScv;

    private TextView mNameTxt = null;
    
    private ImageView mStampImv = null;
    
    private ImageView mStampBigImv = null;
    
    private TextView mCategoryTxv;

    private TextView mMoneyTxv;

    private TextView mDistanceTxv;
    
    private TextView mDistanceFromTxv;
    
    private RatingBar mStartsRtb;

    private LinearLayout mFeatureView;
    
    private boolean needForceReloadPOI = false;
    
    //如下两个layout用来添加动态POI内容到该页
    public LinearLayout mBelowAddressLayout;
    
    public LinearLayout mBelowCommentLayout;
    
    public LinearLayout mBelowFeatureView;

    private TextView mDescriptionTxv = null;

    private ViewGroup mCommentListView = null;
    
    private ViewGroup mCommentSumTotalView;
    
    private ViewGroup mPOIBtn;
    
    private ViewGroup mFavoriteBtn;

    private ViewGroup mShareBtn = null;
    
    private ViewGroup mErrorFixBtn = null;
    
    protected POI mPOI;
    
    private View mAddressAndPhoneView = null;
    
    private View mAddressView = null;
    
    private TextView mAddressTxv = null;
    
    private View mTelephoneView = null;
    
    private TextView mTelephoneTxv = null;
    
    private View mCommentTipView;
    
    private View mNavigationWidget;
    
    DynamicNormalPOI mDynamicNormalPOI;
    
    DynamicTuangouPOI mDynamicTuangouPOI;
    
    DynamicHotelPOI mDynamicHotelPOI;
    
    DynamicMoviePOI mDynamicMoviePOI;
    
    DynamicDishPOI mDynamicDishPOI;
    
    ExtraSubwayPOI mExtraSubwayPOI;
    
    ExtraBusstopPOI mExtraBusstopPOI;
    
    ExtraSameTypePOI mExtraSameTypePOI;
    
    private Button mCommentTipEdt;
    
    private View mLoadingView;
    
    private Animation mStampAnimation;
    
    private View mDoingView;

    private View mToolsView;
    
    private View mWeixinView;
    
    private Button mWeixinBtn;
    
    protected ImageButton mDishBtn;
    
    protected boolean mShowReLoginTip = false;
    
    @Override
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
    
    //存储当前页面POI所包含动态POI对应的ViewBlock
    List<DynamicPOIViewBlock> DPOIViewBlockList = new LinkedList<DynamicPOIViewBlock>();

    //目前可以处理的所有动态POI的Hashtab,key是它们的动态POI类型
    Hashtable<String, DynamicPOIView> DPOIViewTable = new Hashtable<String, DynamicPOIView>();
    
    private static int queryCount = 0;

    //目前可以处理的动态POI类型
    private void initDynamicPOIEnv(){
        DPOIViewTable.put(DynamicPOI.TYPE_HOTEL, mDynamicHotelPOI);
        DPOIViewTable.put(DynamicPOI.TYPE_TUANGOU, mDynamicTuangouPOI);
        DPOIViewTable.put(DynamicPOI.TYPE_ZHANLAN, mDynamicNormalPOI);
        DPOIViewTable.put(DynamicPOI.TYPE_YANCHU, mDynamicNormalPOI);
        DPOIViewTable.put(DynamicPOI.TYPE_COUPON, mDynamicNormalPOI);
        DPOIViewTable.put(DynamicPOI.TYPE_DIANYING, mDynamicMoviePOI);
        DPOIViewTable.put(DynamicPOI.TYPE_DISH, mDynamicDishPOI);
    }

    /**
     * @param poi
     * 检测当前POI的动态POI信息,有对应DynamicPOIView的就将其Block加入本页中
     */
    private void checkAndAddDynamicPOIView(POI poi) {
        List<DynamicPOI> list = poi.getDynamicPOIList();
        for (DynamicPOIView dpView : DPOIViewTable.values()) {
            dpView.mExist = false;
            dpView.mExistDynamicPOI = false;
        }
        int size = (list == null ? 0 : list.size());
        if (size > 0) {
            //先把动态POI对应的DynamicPOIViewBlock获取出来
            for (DynamicPOI dynamicPOI : list) {
                String dataType = dynamicPOI.getType();
                if (!DPOIViewTable.containsKey(dataType) || 
                        DPOIViewTable.get(dataType).isExist()) {
                    continue;
                }
                DynamicPOIView dpView = DPOIViewTable.get(dataType);
                dpView.mExist = true;
                dpView.mExistDynamicPOI = true;
                dpView.initData(poi);
                DPOIViewBlockList.addAll(dpView.getViewList());
            }
        }
        
        // 菜品类型的特殊处理，当存在旧数据的推荐菜时也判断为效
        DynamicPOIView dpView = DPOIViewTable.get(DynamicPOI.TYPE_DISH);
        if (dpView.mExist == false) {
            String recommendCook = mPOI.getDescriptionValue(Description.FIELD_RECOMMEND_COOK);
            if(!TextUtils.isEmpty(recommendCook)) {
                dpView.mExist = true;
                dpView.initData(poi);
                DPOIViewBlockList.addAll(dpView.getViewList());
            }
        }

        //获取到的Block全部加入到该页中
        for (DynamicPOIViewBlock block : DPOIViewBlockList) {
            block.clear();
            block.addToParent();
        }
    }

    /*
     * 进入这个页面时清除掉所有的动态POI的ViewBlock
     */
    private void clearDynamicView(List<DynamicPOIViewBlock> POIList) {
        mBelowCommentLayout.removeAllViews();
        mBelowAddressLayout.removeAllViews();
        mBelowFeatureView.removeAllViews();
        POIList.clear();
    }
    
    /**
     * 加载动态POI
     * @param fromType 加载时进入的标记
     */
    private void loadDynamicView(int fromType) {
        for (DynamicPOIView dpView : DPOIViewTable.values()) {
            if (dpView.isExist()) {
                dpView.loadData(fromType);
            }
        }
    }
    
    /**
     * 该类是动态POI的块级控制类,有着足够用的控制方法.
     * 每个显示块都需要有一个这个类的对象,里面存有自己的Layout和所属的Layout
     * 需要有个block级别的刷新机制。
     */
    public static abstract class DynamicPOIViewBlock {
        private View mOwnLayout;
        private LinearLayout mBelongsLayout;
        boolean mLoadSucceed = true;

        public DynamicPOIViewBlock(LinearLayout belongsLayout, View ownLayout) {
            mBelongsLayout = belongsLayout;
            mOwnLayout = ownLayout;
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

        //该block的刷新方法，调用者自己控制
        public abstract void refresh(); 
    }

    /*
     * 动态POI控制类,用于动态POI的数据操作等
     */
    public abstract static class DynamicPOIView implements TKAsyncTask.EventListener{

        public final static int FROM_ONRESUME = 0;
        public final static int FROM_FAV_HISTORY = 1;
        
        POIDetailFragment mPOIDetailFragment;
        Sphinx mSphinx;
        TKFragmentManager mFragmentManager;
        POI mPOI;
        LayoutInflater mInflater;
        boolean mExist;
        boolean mExistDynamicPOI;

        protected List<BaseQuery> mBaseQuerying;
        protected TKAsyncTask mTkAsyncTasking;

        void queryStart(List<BaseQuery> list, boolean cancelable) {
            mBaseQuerying = list; 
            mTkAsyncTasking = new TKAsyncTask(mSphinx, mBaseQuerying, this, null, cancelable);
            mTkAsyncTasking.execute();
        }
        
        void queryStart(List<BaseQuery> list) {
            queryStart(list, true);
        }

        void queryStart(BaseQuery baseQuery) {
            queryStart(baseQuery, true);
        }
        
        void queryStart(BaseQuery baseQuery, boolean cancelable) {
            List<BaseQuery> list = new ArrayList<BaseQuery>();
            list.add(baseQuery);
            queryStart(list, cancelable);
        }

        //这个函数一般情况下不需要关心,会在页面setData的时候在检测动态POI时被调到
        public void initData(POI poi) {
            mPOI = poi;
        }

        //这个函数只用于返回块列表,且只在初始化动态POI的时候调用
        public abstract List<DynamicPOIViewBlock> getViewList();

        //这个函数是一个动态POI的刷新主函数,刷新策略在其中实现.但是具体的刷新行为在BlockRefresher中实现
        public abstract void refresh();

        //普通动态POI初始化的时候会在checkAndAddDynamicPOIView中把这个变量初始化完毕,不用关心
        public boolean isExist() {
            return mExist;
        }
        
        //动态POI的数据加载。会在初始化后检测存在该动态POI的时候被调用，如果需要就做进一步查询，不需要则直接刷新显示
        public abstract void loadData(int fromType);
        
        public String getString(int resId) {
            return mPOIDetailFragment.mContext.getString(resId);
        }
        
        public String getString(int resId, Object... formatArgs) {
            return mPOIDetailFragment.mContext.getString(resId, formatArgs);
        }
    }

    /**
     * 进入页面时初始化所有动态POI的状态
     */
    final void initDynamicPOIView(POI poi) {
        checkAndAddDynamicPOIView(poi);
    }
    
    /**
     * 以下两个函数出现的原因是因为拆开POI和动态POI的onPostExecute函数后,正在加载几个字
     * 只和POI的查询相关,无法再标识动态POI的加载状态.
     * 但是不同的查询在不同的队列中,分散在代码各处,只好使用同步方法来解决这个问题.
     * 
     * 不要直接操作mLoadingView.
     */
    synchronized void addLoadingView() {
        if (queryCount == 0) {
            mLoadingView.setVisibility(View.VISIBLE);
        }
        queryCount++;
    }

    synchronized void minusLoadingView() {
        if (queryCount > 0) {
            queryCount--;
            if (queryCount == 0) {
                mLoadingView.setVisibility(View.GONE);
            }
        }
    }
    
    synchronized void resetLoadingView() {
        if (queryCount > 0) {
            mLoadingView.setVisibility(View.VISIBLE);
        } else {
            mLoadingView.setVisibility(View.GONE);
        }
    }
    
    public DynamicHotelPOI getDynamicHotelPOI() {
        return mDynamicHotelPOI;
    }
    
	//*************DynamicPOI code end*******************
    
    /**
     * 以下是处理本页动态添加进来的，和POI内容相关的extraView
     * extraView是那些不在POI的DynamicPOIList中，但是和某些POI内容相关的动态出现的View。
     * 由于这类信息处理过程和DynamicPOI不同，但是显示方式一样，于是另写一些代码来处理
     * 这个类别的动态信息。
     * 例如：
     * 地铁信息需要特殊显示，但是它的信息载体不是DynamicPOI，而是POI的Description里面的
     * 两个key，那么使用ExtraView的实现是：
     * 1.创建此额外信息的类，继承DynamicPOIView
     * 2.在该页创建该View的对象，并在initExtraViewEnv里面添加这个类型的POI
     * 3.重载isExist函数，实现该类型判断存在的方法
     * 4.实现所有的界面生成
     */
    
    //存储当前页面所有的extraView
    private List<DynamicPOIViewBlock> mExtraViewBlockList = new LinkedList<DynamicPOIViewBlock>();
    
    private List<DynamicPOIView> mExtraViewList = new LinkedList<DynamicPOIView>();
    
    private void initExtraViewEnv() {
        mExtraViewList.add(mExtraSubwayPOI);
        mExtraViewList.add(mExtraBusstopPOI);
        mExtraViewList.add(mExtraSameTypePOI);
    }
    
    /**
     * 加载ExtraView，需要实现isExist
     * ExtraView的loadData函数一般不涉及从服务器上取数据，刷新即可
     * @param fromType 此参数预留，以备从不同地方加载会有区别显示
     */
    private void loadExtraView(int fromType) {
        for (DynamicPOIView dpView : mExtraViewList) {
            if (dpView.isExist()) {
                dpView.loadData(fromType);
            }
        }
    }
    
    /**
     * 初始化ExtraView，把所有ExtraView的ViewBlock添加进来，但是不显示
     * 显示需要在它们的loadData中执行refresh进行刷新。
     * @param poi
     */
    private void initExtraView(POI poi) {
        for (DynamicPOIView extraView : mExtraViewList) {
            mExtraViewBlockList.addAll(extraView.getViewList());
            extraView.initData(poi);
        }
        
        for (DynamicPOIViewBlock block : mExtraViewBlockList) {
            block.clear();
            block.addToParent();
        }
    }
    /*************************ExtraPOI end **************************/
    
    private final void refreshNavigation() {
        if (mDynamicHotelPOI.isExist()) {
            mNavigationWidget.setVisibility(View.VISIBLE);
        } else {
            mNavigationWidget.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIDetail;
        initDynamicPOIEnv();
        initExtraViewEnv();
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

        mDynamicNormalPOI = new DynamicNormalPOI(this, mLayoutInflater);
        
        mDynamicTuangouPOI = new DynamicTuangouPOI(this, mLayoutInflater);
        
        mDynamicHotelPOI = new DynamicHotelPOI(this, mLayoutInflater);
        
        mDynamicMoviePOI = new DynamicMoviePOI(this, mLayoutInflater);
        
        mDynamicDishPOI = new DynamicDishPOI(this, mLayoutInflater);
        
        mExtraSubwayPOI = new ExtraSubwayPOI(this, mLayoutInflater);
        
        mExtraBusstopPOI = new ExtraBusstopPOI(this, mLayoutInflater);
        
        mExtraSameTypePOI = new ExtraSameTypePOI(this, mLayoutInflater);
        
        return mRootView;
    }
    
    public final void needForceReload() {
        needForceReloadPOI = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.detail_info);
        mRightBtn.setVisibility(View.VISIBLE);
        mRightBtn.setText(R.string.map);
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
        if (mPOI.getSubType() == POI.SUB_TYPE_HOTEL) {
            mDynamicHotelPOI.refreshPicture();
        }
        refreshNavigation();
        
        if (poi.getName() == null && poi.getUUID() != null) {
            mActionLog.addAction(mActionTag + ActionLog.POIDetailFromWeixin);
            needForceReloadPOI = true;
        }
        
        if (needForceReloadPOI) {
            needForceReloadPOI = false;
            List<BaseQuery> baseQueryList = new ArrayList<BaseQuery>();
            DataOperation poiQuery = new DataOperation(mSphinx);
            poiQuery.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_POI);
            poiQuery.addParameter(DataOperation.SERVER_PARAMETER_SUB_DATA_TYPE, DataOperation.SUB_DATA_TYPE_POI);
            poiQuery.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            poiQuery.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, poi.getUUID());
            poiQuery.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD, POI.NEED_FIELD);
            int cityId = CityInfo.CITY_ID_INVALID;
            if (poi.ciytId != 0) {
                cityId = poi.ciytId;
            } else if (poi.getPosition() != null){
                cityId = MapEngine.getCityId(poi.getPosition());
            }
            poiQuery.setup(getId(), getId(), getString(R.string.doing_and_wait));
            poiQuery.setCityId(cityId);
            baseQueryList.add(poiQuery);
            mSphinx.queryStart(baseQueryList);
        }
        
        if (isReLogin()) {
            return;
        }
        
        if (mDismissed) {
            smoothScroolToTop();
        }
    }
    
    public void smoothScroolToTop() {
        mBodyScv.smoothScrollTo(0, 0);
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        mDynamicHotelPOI.clearDateCache();
    }
    
    public void refreshRecommendCook() {
        
        // 菜品类型的特殊处理，当存在旧数据的推荐菜时也判断为效
        DynamicPOIView dpView = DPOIViewTable.get(DynamicPOI.TYPE_DISH);
        if (dpView.mExistDynamicPOI) {
            dpView.loadData(DynamicPOIView.FROM_ONRESUME);
        }
    }
    
    public void refreshDetail() {
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
        String category = poi.getCategory().trim();
        Utility.setFavoriteBtn(mSphinx, mFavoriteBtn, poi.checkFavorite(mContext));
        mNameTxt.setText(poi.getName());

        float star = poi.getGrade();
        mStartsRtb.setRating(star/2.0f);
        
        if (poi.getUUID() == null) {
            mStartsRtb.setVisibility(View.GONE);
        } else {
            mStartsRtb.setVisibility(View.VISIBLE);
        }
        
        if (!TextUtils.isEmpty(category)) {
            mCategoryTxv.setText(category);
        } else {
            mCategoryTxv.setText("");
        }

        long money = poi.getPerCapity();
        if (mDynamicHotelPOI.isExist() || poi.getSubType() == POI.SUB_TYPE_HOTEL) {
            mMoneyTxv.setVisibility(View.GONE);
        } else if (money > -1) {
            mMoneyTxv.setText(getString(R.string.yuan, money));
            mMoneyTxv.setVisibility(View.VISIBLE);
        } else {
            mMoneyTxv.setVisibility(View.GONE);
        }
        
        String distance = poi.getToCenterDistance();
        POIAdapter.showDistance(mSphinx, mDistanceFromTxv, mDistanceTxv, distance);
        
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
                } else {
                    mAddressView.setBackgroundResource(R.drawable.list_header);
                }
            } else {
                mAddressView.setVisibility(View.GONE);
            }
            
            if (!TextUtils.isEmpty(telephone)) {
                mTelephoneView.setVisibility(View.VISIBLE);
                mTelephoneTxv.setText(telephone.replace("|", getString(R.string.dunhao)));
                
                if (TextUtils.isEmpty(address)) {
                    mTelephoneView.setBackgroundResource(R.drawable.list_single);
                } else {
                    mTelephoneView.setBackgroundResource(R.drawable.list_footer);
                }
            } else {
                mTelephoneView.setVisibility(View.GONE);
            }

            mAddressAndPhoneView.setVisibility(View.VISIBLE);
        } else {
            mAddressAndPhoneView.setVisibility(View.GONE);
        }
        
        makeFeature();
        
    }
    
    public void refreshStamp() {
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
        mBodyScv.smoothScrollTo(0, 0);
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
        for(int i = 1; i < count; i++) {
            mCommentListView.getChildAt(i).setVisibility(View.GONE);
        }
        mCommentListView.setVisibility(View.GONE);
        
        POI poi = mPOI;
        if (poi == null || poi.getUUID() == null) {
            mStampImv.setVisibility(View.GONE);
            return result;
        }

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

                Comment lastComment = poi.getLastComment();
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
                    if (count == 2) {
                        View view = mCommentListView.getChildAt(1);
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

    @Override
    protected void findViews() {
        super.findViews();
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
        
        mPOIBtn = (ViewGroup) mRootView.findViewById(R.id.nearby_search_btn);
        mShareBtn = (ViewGroup)mRootView.findViewById(R.id.share_btn);
        mFavoriteBtn = (ViewGroup)mRootView.findViewById(R.id.favorite_btn);
        mErrorFixBtn = (ViewGroup)mRootView.findViewById(R.id.error_recovery_btn);
        
        mBelowCommentLayout = (LinearLayout)mRootView.findViewById(R.id.below_comment);
        mBelowAddressLayout = (LinearLayout)mRootView.findViewById(R.id.below_address);
        mFeatureView = (LinearLayout)mRootView.findViewById(R.id.feature_view);
        mBelowFeatureView = (LinearLayout)mRootView.findViewById(R.id.layout_below_feature);

        mCommentListView = (ViewGroup)mRootView.findViewById(R.id.comment_list_view);
        mCommentSumTotalView = (ViewGroup) mRootView.findViewById(R.id.comment_sum_total_view);
        
        mAddressAndPhoneView = mRootView.findViewById(R.id.address_phone_view);
        mAddressView = mRootView.findViewById(R.id.address_view);
        mAddressTxv = (TextView)mRootView.findViewById(R.id.address_txv);
        mTelephoneView = mRootView.findViewById(R.id.telephone_view);
        mTelephoneTxv = (TextView)mRootView.findViewById(R.id.telephone_txv);
        
        mCommentTipView = mRootView.findViewById(R.id.comment_tip_view);
        mCommentTipEdt = (Button) mRootView.findViewById(R.id.comment_tip_btn);
        mLoadingView = mRootView.findViewById(R.id.loading_view);
        
        mToolsView = mRootView.findViewById(R.id.bottom_buttons_view);
        mWeixinView = mRootView.findViewById(R.id.weixin_view);
        mWeixinBtn = (Button)mRootView.findViewById(R.id.weixin_btn);
        mDishBtn = (ImageButton)mRootView.findViewById(R.id.dish_btn);

        mNavigationWidget = mRootView.findViewById(R.id.navigation_widget);
    }

    @Override
    protected void setListener() {
        super.setListener();
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
        mDishBtn.setOnClickListener(this);
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
                
            case R.id.nearby_search_btn:
                mActionLog.addAction(mActionTag +  ActionLog.POIDetailSearch);
                mFragmentManager.getNearbySearchFragment().setData(mSphinx.buildDataQuery(poi));
                mSphinx.showView(TKFragmentManager.ID_view_poi_nearby_search);
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
                
            case R.id.dish_btn:
                mActionLog.addAction(mActionTag + ActionLog.POIDetailDish);
                DishActivity.setPOI(poi);
                mSphinx.showView(R.id.activity_poi_dish);
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
                            getString(resId));
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
        mFragmentManager.getResultMapFragment().setData(getString(R.string.result_map), poi.getSourceType() == POI.SOURCE_TYPE_HOTEL ? ActionLog.POIHotelDetailMap : ActionLog.POIDetailMap);
        mSphinx.showView(TKFragmentManager.ID_view_result_map);
        List<POI> pois = new ArrayList<POI>();
        pois.add(poi);
        ItemizedOverlayHelper.drawPOIOverlay(mSphinx, pois, 0);
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
                    getString(R.string.prompt), 
                    getString(R.string.cancel_favorite_tip),
                    getString(R.string.yes),
                    getString(R.string.no),
                    new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface arg0, int id) {
                            if (id == DialogInterface.BUTTON_POSITIVE) {
                                poi.deleteFavorite(mContext);
                                Utility.setFavoriteBtn(mSphinx, mFavoriteBtn, false);
                            }
                        }
                    });
        } else {
            poi.writeToDatabases(mContext, -1, Tigerknows.STORE_TYPE_FAVORITE);
            Utility.setFavoriteBtn(mSphinx, mFavoriteBtn, true);
            Toast.makeText(mSphinx, R.string.favorite_toast, Toast.LENGTH_LONG).show();
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

        mSphinx.resetLoactionButtonState();
        mSphinx.resetMapDegree();
        ItemizedOverlayHelper.drawPOIOverlay(mSphinx, pois, 0, null, false);
        ShareAPI.share(mSphinx, poi, poi.getPosition(), mActionTag);
    }
    
    public void setData(POI poi) {
        setData(poi, -1);
    }
    
    public void setData(POI poi, int position) {
        mShowReLoginTip = true;
        mPOI = poi;
        if (poi == null) {
            return;
        }
        Animation animation = mDishBtn.getAnimation();
        if (animation != null) {
            animation.reset();
            mDishBtn.setAnimation(null);
        }
        mDishBtn.setVisibility(View.INVISIBLE);
        //这两个函数放在前面初始化动态POI信息
        clearDynamicView(DPOIViewBlockList);
        //初始化和动态POI信息相关的动态布局
        //初始化动态POI在初始化ExtraView之前，就会导致动态POI在ExtraView的上面显示
        initDynamicPOIView(mPOI);
        initExtraView(mPOI);
        // 重置查询计数
        queryCount = 0;
        loadExtraView(DynamicPOIView.FROM_ONRESUME);
        loadDynamicView(DynamicPOIView.FROM_ONRESUME);
        refreshNavigation();
        mDoingView.setVisibility(View.VISIBLE);
        if (poi.getName() == null && poi.getUUID() != null) {
            return;
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
            DataOperation poiQuery = new DataOperation(mSphinx);
            poiQuery.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_POI);
            poiQuery.addParameter(BaseQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
            poiQuery.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            poiQuery.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, uuid);
            poiQuery.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD, POI.NEED_FIELD);
            int cityId = CityInfo.CITY_ID_INVALID;
            if (poi.ciytId != 0) {
                cityId = poi.ciytId;
            } else if (poi.getPosition() != null){
                cityId = MapEngine.getCityId(poi.getPosition());
            }
            poiQuery.setup(getId(), getId(), null);
            poiQuery.setCityId(cityId);
            baseQueryList.add(poiQuery);
        }
        
        if (poi.getCommentQuery() == null || poi.getFrom() == POI.FROM_LOCAL) {
            DataQuery commentQuery = new DataQuery(mSphinx);
            commentQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANPING);
            commentQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, poi.getUUID());
            commentQuery.addParameter(DataQuery.SERVER_PARAMETER_REFER, DataQuery.REFER_POI);
            commentQuery.setup(getId(), getId(), null, false, false, poi);
            baseQueryList.add(commentQuery);
            mCommentTipView.setVisibility(View.GONE);
        } else {
            mCommentTipView.setVisibility(View.VISIBLE);
        }
        
        refreshDetail();
        refreshComment();

        FeedbackUpload.logEnterPOIDetailUI(mSphinx,
            DataQuery.DATA_TYPE_POI,
            mDynamicHotelPOI.isExist() ? BaseQuery.SUB_DATA_TYPE_HOTEL : BaseQuery.SUB_DATA_TYPE_POI,
            mActionTag,
            position,
            poi.getUUID(),
            MapEngine.getCityId(poi.getPosition()));
        
        
        if (baseQueryList.isEmpty() == false) {
            mTkAsyncTasking = mSphinx.queryStart(baseQueryList);
            mBaseQuerying = baseQueryList;
            addLoadingView();
        }
        
        String category = poi.getCategory().trim();
        poi.updateHistory(mSphinx);
        mActionLog.addAction(ActionLog.POIDetailShow, poi.getUUID(), poi.getName(), TextUtils.isEmpty(category) ? "NULL" : category.split(" ")[0]);
        mDoingView.setVisibility(View.GONE);
    }
        
    private void makeFeature() {
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
        
        int count = mFeatureView.getChildCount();
        for(int i = 0; i < count; i++) {
            mFeatureView.getChildAt(i).setVisibility(View.GONE);
        }
        
        List<String> nameList = new ArrayList<String>();
        List<String> valueList = new ArrayList<String>();

        byte[] showKeys = new byte[] {Description.FIELD_FEATURE, Description.FIELD_RECOMMEND, Description.FIELD_GUEST_CAPACITY, Description.FIELD_BUSINESS_HOURS,
                Description.FIELD_HOUSING_PRICE, Description.FIELD_SYNOPSIS, Description.FIELD_CINEMA_FEATURE, Description.FIELD_MEMBER_POLICY, Description.FIELD_FEATURE_SPECIALTY, 
                Description.FIELD_TOUR_DATE, Description.FIELD_TOUR_LIKE, Description.FIELD_POPEDOM_SCENERY, Description.FIELD_RECOMMEND_SCENERY,
                Description.FIELD_NEARBY_INFO, Description.FIELD_COMPANY_WEB, Description.FIELD_COMPANY_TYPE,
                Description.FIELD_COMPANY_SCOPE, Description.FIELD_INDUSTRY_INFO};

        if (!mDynamicHotelPOI.isExist()) {
            for(int i = 0; i < showKeys.length; i++) {
                byte key = showKeys[i];
                String value = poi.getDescriptionValue(key);

                if(!TextUtils.isEmpty(value)) {
                    String name = poi.getDescriptionName(mContext, key);
                    nameList.add(name.substring(0, name.length()-1));
                    valueList.add(value);
                }
            }
        }
        
        /**
         * 酒店部分特殊处理，要求所有酒店显示设施服务，可预订酒店不显示description里的简介
         */
        String hotelService = poi.getHotelService();
        String roomDescription = poi.getRoomDescription();
        if (hotelService != null) {
            nameList.add(getString(R.string.hotel_service_title));
            valueList.add(hotelService);
        }
        if (roomDescription != null) {
            nameList.add(getString(R.string.hotel_room_description_title));
            valueList.add(roomDescription);
        }
        
        int padding = Utility.dip2px(mSphinx, 8);
        TextView titleTxv;
        TextView bodyTxv;
        for(int i = 0, size = nameList.size(); i < size; i++) {
            if (i < count) {
                ViewGroup child = (ViewGroup) mFeatureView.getChildAt(i);
                titleTxv = (TextView) child.getChildAt(0);
                bodyTxv = (TextView) child.getChildAt(1);
                child.setVisibility(View.VISIBLE);
            } else {
                LinearLayout child = new LinearLayout(mSphinx);
                child.setOrientation(LinearLayout.VERTICAL);
                titleTxv = new TextView(mContext);
                child.addView(titleTxv);
                titleTxv.setGravity(Gravity.LEFT);
                int color = mSphinx.getResources().getColor(R.color.black_middle);
                titleTxv.setTextColor(color);
                titleTxv.setPadding(padding, padding, padding, padding);
                bodyTxv = new TextView(mContext);
                child.addView(bodyTxv);
                bodyTxv.setGravity(Gravity.LEFT);
                bodyTxv.setLineSpacing(0f, 1.2f);
                bodyTxv.setPadding(padding, 0, padding, 0);
                color = mSphinx.getResources().getColor(R.color.black_light);
                bodyTxv.setTextColor(color);
                mFeatureView.addView(child);
            }
            titleTxv.setText(nameList.get(i));
            bodyTxv.setText(valueList.get(i));
        }
        
        count = mFeatureView.getChildCount();
        ViewGroup firstVisibleChild = null;
        ViewGroup lastVisibleChild = null;
        for(int i = 0; i < count; i++) {
            ViewGroup child = (ViewGroup) mFeatureView.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                if (firstVisibleChild == null) {
                    firstVisibleChild = child;
                    child.setBackgroundResource(R.drawable.list_header);
                } else {
                    child.setBackgroundResource(R.drawable.list_middle);
                }
                lastVisibleChild = child;
            }
        }
        
        if (firstVisibleChild != null && lastVisibleChild != null) {
            if (firstVisibleChild == lastVisibleChild) {
                firstVisibleChild.setBackgroundResource(R.drawable.list_single);
            } else {
                lastVisibleChild.setBackgroundResource(R.drawable.list_footer);
            }
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
//                                    getString(R.string.comment_poi_invalid_hit));
//                        }
//                    }
//                });
//            } else {
                view.setBackgroundResource(R.drawable.list_middle_normal);
                authorTxv.setTextColor(TKConfig.COLOR_BLACK_LIGHT);
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
            commentTxv.setMaxLines(5);
            commentTxv.setEllipsize(TruncateAt.END);
            
            final String url = comment.getUrl();
            if (TextUtils.isEmpty(url)) {
                srcTxv.setVisibility(View.GONE);
            } else {
                srcTxv.setVisibility(View.VISIBLE);
                String source = getString(R.string.source_);
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
                                getString(R.string.prompt), 
                                getString(R.string.are_you_view_url, url),
                                new DialogInterface.OnClickListener() {
                                    
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        switch (id) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                                mSphinx.startActivityForResult(intent, TKFragmentManager.ID_view_invalid);
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
            e.printStackTrace();
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
        minusLoadingView();
        List<BaseQuery> baseQueryList = tkAsyncTask.getBaseQueryList();
        for(BaseQuery baseQuery : baseQueryList) {
            if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(TKFragmentManager.ID_view_user_home), getId(), getId(), getId(), mCancelLoginListener, mShowReLoginTip)) {
                mShowReLoginTip = false;
                isReLogin = true;
                return;
            }
            String dataType = baseQuery.getParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE);
            Response response = baseQuery.getResponse();
            
            // 查询点评的结果
            if (baseQuery instanceof DataQuery) {
                if (BaseActivity.hasAbnormalResponseCode(baseQuery, mSphinx, BaseActivity.SHOW_NOTHING, this, false) == false) {
                    DataQuery dataQuery = (DataQuery) baseQuery;
                    POI requestPOI = dataQuery.getPOI();
                    if (response instanceof CommentResponse) {
                        requestPOI.setCommentQuery(dataQuery);
                        refreshDetail();
                        refreshComment();
                        requestPOI.updateComment(mSphinx);
                    }
                }
                
            } else if (baseQuery instanceof DataOperation) {
                if (BaseQuery.DATA_TYPE_DIANPING.equals(dataType)) {
                    return;
                // 查询POI的结果
                } else if (BaseQuery.DATA_TYPE_POI.equals(dataType)) {
                    if (poi.getName() == null && poi.getUUID() != null) {
                        if (BaseActivity.hasAbnormalResponseCode(baseQuery, mSphinx, BaseActivity.SHOW_TOAST, POIDetailFragment.this, true)) {
                            mActionLog.addAction(mActionTag+ActionLog.POIDetailPullFailed);
                            if (mDoingView.getVisibility() == View.VISIBLE) {
                                dismiss();
                            }
                        } else {
                            POI onlinePOI = ((POIQueryResponse)response).getPOI();
                            if (onlinePOI != null && onlinePOI.getUUID() != null && onlinePOI.getUUID().equals(poi.getUUID())) {
                                setData(onlinePOI);
                            }
                        }
                    } else {
                        if (BaseActivity.hasAbnormalResponseCode(baseQuery, mSphinx, TKActivity.SHOW_NOTHING, this, false, new int[]{603})) {
                        	return;
                        }
                    	int responseCode = response.getResponseCode();
                    	if (responseCode == 603) {
                    		poi.setStatus(POI.STATUS_INVALID);
                    		BaseActivity.showErrorDialog(mSphinx, getString(R.string.response_code_603), this, true);
                    		return;
                    	}
                        POI onlinePOI = ((POIQueryResponse)response).getPOI();
                        if (onlinePOI != null && onlinePOI.getUUID() != null && onlinePOI.getUUID().equals(poi.getUUID())) {
                            //收藏夹和历史记录进入的时候刷新POI,发现有动态信息则刷新整个POI的动态信息
                            poi.updateData(mSphinx, onlinePOI.getData());
                            poi.setFrom(POI.FROM_ONLINE);
                            initDynamicPOIView(mPOI);
                            initExtraView(mPOI);
                            refreshDetail();
                            refreshComment();
                            refreshNavigation();

                            loadDynamicView(DynamicPOIView.FROM_FAV_HISTORY);
                            loadExtraView(DynamicPOIView.FROM_FAV_HISTORY);
                        }
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
