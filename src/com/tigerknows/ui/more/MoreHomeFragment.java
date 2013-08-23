/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.more;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.Bootstrap;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.BootstrapModel.Recommend;
import com.tigerknows.model.BootstrapModel.SoftwareUpdate;
import com.tigerknows.model.BootstrapModel.Recommend.RecommendApp;
import com.tigerknows.model.NoticeQuery;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse.NoticeResult;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse.NoticeResult.Notice;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.BrowserActivity;
import com.tigerknows.ui.more.MapDownloadActivity.DownloadCity;
import com.tigerknows.ui.user.UserBaseActivity;
import com.tigerknows.ui.user.UserLoginActivity;
import com.tigerknows.util.Utility;

/**
 * @author Peng Wenyue
 * Reconstructed by Feng Tianxiao 
 */
public class MoreHomeFragment extends BaseFragment implements View.OnClickListener {
    
    public MoreHomeFragment(Sphinx sphinx) {
        super(sphinx);
    }

    static final String TAG = "MoreFragment";
    
    public static final int VIEW_PAGE_LEFT = 500;
    public static final int VIEW_PAGE_MULTIPLIER = 10;
    
    public static final int SHOW_COMMENT_TIP_TIMES = 3;
    private static final int NUM_APP_RECOMMEND = 5;
    
    private static final int NOTICE_VERSION = 0;
    
    private TextView mUserNameTxv;
    private TextView mCurrentCityTxv;
    private TextView mUpgradeMapTxv;
    private Button mUserBtn;
    private Button mChangeCityBtn;
    private Button mDownloadMapBtn;
    private Button mAppRecommendBtn;
    private Button mFavoriteBtn;
    private Button mHistoryBrowseBtn;
    private Button mGoCommentBtn;
    private Button mSatisfyRateBtn;
    private Button mFeedbackBtn;
    private Button mAddMerchantBtn;
    private Button mGiveFavourableCommentBtn;
    private View mGiveFavourableCommentImv;
    private View[] mAppRecommendView;
    private ImageView[] mAppRecommendImv;
    private TextView[] mAppRecommendTxv;
    private LinearLayout mAppRecommendLly;
    private ImageView mTencentAppRecommendImv;
    private static final int[] APP_RECOMMEND_ID = {R.id.app_item_1,
    	R.id.app_item_2,
    	R.id.app_item_3,
    	R.id.app_item_4,
    	R.id.app_item_5
    };
    private List<RecommendApp> mRecommendAppList = new ArrayList<RecommendApp>();
    
    private String mCityName;
    
    public static final int MESSAGE_TYPE_NONE = 0;
    public static final int MESSAGE_TYPE_SOFTWARE_UPDATE = 1;
    public static final int MESSAGE_TYPE_MAP_UPDATE = 2;
    public static final int MESSAGE_TYPE_USER_SURVEY = 3;
    public static final int MESSAGE_TYPE_COMMENT = 4;
    
    public static DownloadCity CurrentDownloadCity;
    
    private RelativeLayout mNoticeRly;
    private NoticeResultResponse mNoticeResultResponse;
    private NoticeResult mNoticeResult;
    private List<Notice> mNoticeList = null;
    private int mPagecount = -1;
    private ViewPager mViewPager;
    private HashMap<Integer, View> viewMap = new HashMap<Integer, View>();
    private HashMap<Integer, ImageView> imageViewMap = new HashMap<Integer, ImageView>();
    private ViewGroup mPageIndicatorView;
    private boolean mUpgradeMap;
    private SoftwareUpdate mSoftwareUpdate;

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
        	refreshAllNoticeDrawable();
        	refreshAppRecommendDrawable();
        }
    };
    
    private Runnable mNoticeNextRun = new Runnable() {

		@Override
		public void run() {
			if(mSphinx.uiStackPeek() == R.id.view_more_home && mPagecount > 1){
				mViewPager.setCurrentItem(mViewPager.getCurrentItem()+1);
			}
		}
    };
    
    private Runnable mReloadAppRecommend = new Runnable() {

		@Override
		public void run() {
            Bootstrap bootstrap = new Bootstrap(mSphinx);
            bootstrap.setup(Globals.getCurrentCityInfo().getId());
            mSphinx.queryStart(bootstrap);
        }
    };
    
    private Handler mHandler;

	private MyAdapter mMyAdapter;
	private int mPosition;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.More;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.more_home, container, false);
    	mRightBtn = mSphinx.getTitleFragment().getRightTxv();
        findViews();        

        setListener();
        
        if (TKConfig.sSPREADER.startsWith(TKConfig.SPREADER_TENCENT)) {
            mAppRecommendBtn.setText(R.string.recommend_tencent);
            mTencentAppRecommendImv.setVisibility(View.GONE);
            mAppRecommendLly.setVisibility(View.GONE);
            mAppRecommendBtn.setBackgroundDrawable(mSphinx.getResources().getDrawable(R.drawable.list_single));
            mAppRecommendBtn.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
            mAppRecommendBtn.setPadding(Utility.dip2px(mSphinx, 16), Utility.dip2px(mSphinx, 8), Utility.dip2px(mSphinx, 8), Utility.dip2px(mSphinx, 8));
        } else {
            mAppRecommendBtn.setText(R.string.app_recommend_more);
        }
        
        if (mSphinx.getPackageManager().queryIntentActivities(makeGiveFavourableIntent(), PackageManager.MATCH_DEFAULT_ONLY).size() > 0) {
            mGiveFavourableCommentBtn.setVisibility(View.VISIBLE);
            mGiveFavourableCommentImv.setVisibility(View.VISIBLE);
        } else {
            mGiveFavourableCommentBtn.setVisibility(View.GONE);
            mGiveFavourableCommentImv.setVisibility(View.GONE);
        }
        mHandler = new Handler();

        return mRootView;
    }
    
    protected void findViews() {
    	mNoticeRly = (RelativeLayout) mRootView.findViewById(R.id.notice_rly);
        mCurrentCityTxv = (TextView) mRootView.findViewById(R.id.current_city_txv);
        mUserNameTxv = (TextView) mRootView.findViewById(R.id.user_name_txv);
        mUpgradeMapTxv = (TextView) mRootView.findViewById(R.id.upgrade_map_txv);
        mUserBtn = (Button)mRootView.findViewById(R.id.user_btn);
        mChangeCityBtn = (Button)mRootView.findViewById(R.id.change_city_btn);
        mDownloadMapBtn = (Button)mRootView.findViewById(R.id.download_map_btn);
        mAppRecommendBtn = (Button)mRootView.findViewById(R.id.app_recommend_btn);
        mFavoriteBtn = (Button)mRootView.findViewById(R.id.favorite_btn);
        mHistoryBrowseBtn = (Button)mRootView.findViewById(R.id.history_browse_btn);
        mGoCommentBtn = (Button)mRootView.findViewById(R.id.go_comment_btn);
        mSatisfyRateBtn = (Button)mRootView.findViewById(R.id.satisfy_btn);
        mFeedbackBtn = (Button)mRootView.findViewById(R.id.feedback_btn);
        mAddMerchantBtn = (Button)mRootView.findViewById(R.id.add_merchant_btn);
        mGiveFavourableCommentBtn = (Button)mRootView.findViewById(R.id.give_favourable_comment_btn);
        mGiveFavourableCommentImv = mRootView.findViewById(R.id.give_favourable_comment_imv);
        mAppRecommendView = new View[NUM_APP_RECOMMEND];
        mAppRecommendImv = new ImageView[NUM_APP_RECOMMEND];
        mAppRecommendTxv = new TextView[NUM_APP_RECOMMEND];
        mTencentAppRecommendImv = (ImageView)mRootView.findViewById(R.id.more_home_app_recommend_imv);
        mAppRecommendLly = (LinearLayout)mRootView.findViewById(R.id.more_home_app_recommend_lly);
        for (int i=0; i < NUM_APP_RECOMMEND; i++){
        	mAppRecommendView[i] = (View)mRootView.findViewById(APP_RECOMMEND_ID[i]);
        	mAppRecommendImv[i] = (ImageView)mAppRecommendView[i].findViewById(R.id.app_icon_imv);
        	mAppRecommendTxv[i] = (TextView)mAppRecommendView[i].findViewById(R.id.app_name_txv);
        }
        mViewPager = (ViewPager) mRootView.findViewById(R.id.view_pager);
        mPageIndicatorView = (ViewGroup) mRootView.findViewById(R.id.page_indicator_view);
    }
    
    protected void setListener() {
    	mRightBtn.setOnClickListener(this);
        mUserBtn.setOnClickListener(this);
        mChangeCityBtn.setOnClickListener(this);
        mDownloadMapBtn.setOnClickListener(this);
        mAppRecommendBtn.setOnClickListener(this);
        mFavoriteBtn.setOnClickListener(this);
        mGoCommentBtn.setOnClickListener(this);
        mHistoryBrowseBtn.setOnClickListener(this);
        mSatisfyRateBtn.setOnClickListener(this);
        mFeedbackBtn.setOnClickListener(this);
        mAddMerchantBtn.setOnClickListener(this);
        mGiveFavourableCommentBtn.setOnClickListener(this);
        for (int i=0; i < NUM_APP_RECOMMEND; i++){
        	final int position=i;
        	mAppRecommendImv[i].setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final int len = mRecommendAppList.size();
					if(position > len)return;
	                final RecommendApp recommendApp = mRecommendAppList.get(position);
	                if (recommendApp != null) {
	                    mActionLog.addAction(mActionTag + ActionLog.MoreAppDownload, position, recommendApp.getName());
	                    final String uri = recommendApp.getUrl();
	                    if (!TextUtils.isEmpty(uri)) {
	                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
	                        mSphinx.startActivity(intent);
	                    }
	                }
				}
			});
        }
        mMyAdapter = new MyAdapter();
    	mViewPager.setAdapter(mMyAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMenuFragment.updateMenuStatus(R.id.more_btn);
        mLeftBtn.setVisibility(View.INVISIBLE);
        mTitleBtn.setText(R.string.more);
        mRightBtn = mSphinx.getTitleFragment().getRightTxv();
        mRightBtn.setOnClickListener(this);
        mRightBtn.setVisibility(View.VISIBLE);
        mRightBtn.setEnabled(true);
        mRightBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_settings));
        if (mDismissed) {
            //mListLsv.setSelection(0);
        }

        mMenuFragment.display();
        
        refreshUserEntrance();
        refreshSatisfyRate();
        refreshBootStrapData();
        refreshMoreData();
        refreshCity(Globals.getCurrentCityInfo().getCName());
        refreshGoCommentData();
        refreshAllNoticeDrawable();
    	refreshMapDownloadData();

        if(mPagecount > 1){
        	// 这两行代码用来解决onResume之后，首次定时器触发后，动画不正确的问题……
        	// 并且这样就不需要在onResume里调用mHandler.postDelayed(mNoticeNextRun, 4000);了
        	// 原理未知，反正试出来的。--fengtianxiao 2013.08.07
        	mViewPager.setCurrentItem(mViewPager.getCurrentItem()+1);
        	mViewPager.setCurrentItem(mViewPager.getCurrentItem()-1);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mNoticeNextRun);
        mHandler.removeCallbacks(mReloadAppRecommend);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.user_btn:
            	if (TextUtils.isEmpty(Globals.g_Session_Id) == false) {
                    mActionLog.addAction(mActionTag +  ActionLog.MoreUserHome);
            		mSphinx.showView(R.id.view_user_home);
            	} else {
                    mActionLog.addAction(mActionTag +  ActionLog.MoreLoginRegist);
            		Intent intent = new Intent(mSphinx, UserLoginActivity.class);
                    intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, R.id.view_user_home);
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, getId());
            		mSphinx.startActivityForResult(intent, 0);
            	}
                break;
            case R.id.change_city_btn:
                mActionLog.addAction(mActionTag +  ActionLog.MoreChangeCity);
                mSphinx.showView(R.id.activity_more_change_city);
                break;
            case R.id.download_map_btn:
                mActionLog.addAction(mActionTag +  ActionLog.MoreMapDownload);
                mSphinx.showView(R.id.activity_more_map_download);
                break;
            case R.id.app_recommend_btn:
                mActionLog.addAction(mActionTag +  ActionLog.MoreAppRecommend);
                if (TKConfig.sSPREADER.startsWith(TKConfig.SPREADER_TENCENT)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://a.wap.myapp.com/and2/s?aid=detail&appid=50801"));
                    mSphinx.startActivity(intent);
                } else {
                    mSphinx.showView(R.id.activity_more_app_recommend);
                }
                break;
            case R.id.favorite_btn:
                mActionLog.addAction(mActionTag +  ActionLog.MoreFavorite);
                mSphinx.showView(R.id.view_more_favorite);
                break;
            case R.id.history_browse_btn:
                mActionLog.addAction(mActionTag +  ActionLog.MoreHistory);
                mSphinx.showView(R.id.view_more_history);
                break;
            case R.id.go_comment_btn:
            	mActionLog.addAction(mActionTag +  ActionLog.MoreGoComment);
                mSphinx.showView(R.id.view_more_go_comment);
                break;
            case R.id.right_btn:
                mActionLog.addAction(mActionTag +  ActionLog.MoreSetting);
                mSphinx.showView(R.id.activity_more_setting);
                break;
            case R.id.satisfy_btn:
            	mActionLog.addAction(mActionTag +  ActionLog.MoreSatisfyRate);
            	mSphinx.showView(R.id.activity_more_satisfy);
            	break;
            case R.id.feedback_btn:
                mActionLog.addAction(mActionTag +  ActionLog.MoreFeedback);
                mSphinx.showView(R.id.activity_more_feedback);
                break;
            case R.id.add_merchant_btn:
                mActionLog.addAction(mActionTag +  ActionLog.MoreAddMerchant);
                mSphinx.showView(R.id.activity_more_add_merchant);
                break;
            case R.id.give_favourable_comment_btn:
                mActionLog.addAction(mActionTag +  ActionLog.MoreGiveFavourableComment);
                mSphinx.startActivity(makeGiveFavourableIntent());  
                break;

            default:
                break;
        }
    }
    
    private Intent makeGiveFavourableIntent() {
        String str = "market://details?id=" + mSphinx.getPackageName();  
        Intent localIntent = new Intent("android.intent.action.VIEW");  
        localIntent.setData(Uri.parse(str));  
        return localIntent;
    }
    
    public void refreshMapDownloadData() {
        mUpgradeMap = false;
        DownloadCity currentDownloadCity = CurrentDownloadCity;
        if (currentDownloadCity != null
                && currentDownloadCity.state == DownloadCity.STATE_CAN_BE_UPGRADE
                && currentDownloadCity.cityInfo.getId() != MapEngine.CITY_ID_QUANGUO) {
        	mUpgradeMap = true;
        }
        if (mUpgradeMap) {
        	mUpgradeMapTxv.setVisibility(View.VISIBLE);
    	}else{
    		mUpgradeMapTxv.setVisibility(View.GONE);
    	}
    }
    
    public void refreshMenuFragment() {
        if(mUpgradeMap || TextUtils.isEmpty(TKConfig.getPref(mContext, TKConfig.PREFS_SATISFY_RATE_OPENED, ""))){
        	mSphinx.getMenuFragment().setFragmentMessage(View.VISIBLE);
        	return;
        }else{
        	mSphinx.getMenuFragment().setFragmentMessage(View.GONE);
        }
    }
    
    public void refreshGoCommentData() {
    	Calendar cal = Calendar.getInstance();
    	cal.set(2013, 9, 1, 0, 0, 0);
    	Calendar now = Calendar.getInstance();
    	if(now.before(cal)){
    		Drawable[] drawables = mGoCommentBtn.getCompoundDrawables();
    		drawables[2] = mContext.getResources().getDrawable(R.drawable.ic_go_comment_new);
    		int w = drawables[2].getIntrinsicWidth();
    		int h = drawables[2].getIntrinsicHeight();
    		int max = Utility.dip2px(mContext, 28);
    		int ww = (w > max)?max:w;
    		drawables[2].setBounds(0, 0, ww, (int)(h*ww/w));
    		mGoCommentBtn.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
    		mGoCommentBtn.setCompoundDrawablePadding(Utility.dip2px(mContext, 20));
    	}else{
    		Drawable[] drawables = mDownloadMapBtn.getCompoundDrawables();
    		mDownloadMapBtn.setCompoundDrawables(drawables[0], drawables[1], null, drawables[3]);
    	}
    }

    // 该函数在系统启动时，和每次进入更多页面时均调用
    public void refreshMoreData() {
    	refreshMoreNotice(null);
    	refreshMenuFragment();
    }

    public void refreshMoreNotice(NoticeResultResponse noticeResultResponse) {
    	
    	if(mNoticeResultResponse == null){
        	mNoticeResultResponse = noticeResultResponse;
        	if(mNoticeResultResponse != null){
        		setNoticeList();
        	}else if(mPagecount < 0){
             	NoticeQuery noticeQuery = new NoticeQuery(mSphinx);
                noticeQuery.setup(Globals.getCurrentCityInfo().getId());
                mSphinx.queryStart(noticeQuery);
            }
    	}
    }
    private void setNoticeList(){
    	BootstrapModel bootstrapModel = Globals.g_Bootstrap_Model;
    	if(mNoticeResultResponse == null || bootstrapModel == null || mPagecount >= 0){
    		return;
    	}
		mNoticeResult = mNoticeResultResponse.getNoticeResult();
		mNoticeList = new ArrayList<Notice>();
		mSoftwareUpdate = bootstrapModel.getSoftwareUpdate();
		if(mSoftwareUpdate != null){
			XMap data = new XMap();
			data.put(Notice.FIELD_NOTICE_ID, mSoftwareUpdate.getId());
			data.put(Notice.FIELD_OPERATION_TYPE, 0);
			data.put(Notice.FIELD_NOTICE_TITLE, mSphinx.getString(R.string.message_tip_software_update));
			data.put(Notice.FIELD_NOTICE_DESCRIPTION, "");
			data.put(Notice.FIELD_URL, mSoftwareUpdate.getURL());
			try {
				Notice notice = new Notice(data, 1);
				mNoticeList.add(notice);
			} catch (APIException e){
				e.printStackTrace();
			}
		}
		if(mNoticeResult != null){
			List<Notice> noticeList = mNoticeResult.getNoticeList();
			if(noticeList != null){
				for(int i=0; i<noticeList.size(); i++){
					Notice notice = noticeList.get(i);
					if(notice.getOperationType() <= NOTICE_VERSION){
						mNoticeList.add(notice);
					}
				}
			}
		}
		mPagecount = (int)mNoticeList.size();
        if(mPagecount > 1){
        	Utility.pageIndicatorInit(mSphinx, mPageIndicatorView, mPagecount, 0, R.drawable.ic_notice_dot_normal, R.drawable.ic_notice_dot_selected);
        	mNoticeRly.setVisibility(View.VISIBLE);
        	mViewPager.setCurrentItem(mPagecount * VIEW_PAGE_LEFT);
        	mPosition = 0;
        	mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
        		
        		@Override
        		public void onPageSelected(int index) {
        			LogWrapper.d("Trap", "Select:"+index);
        			refreshAllNoticeDrawable();
        			mHandler.removeCallbacks(mNoticeNextRun);
        			mHandler.postDelayed(mNoticeNextRun, 4000);
        			mPosition = index % mPagecount;
        			Utility.pageIndicatorChanged(mSphinx, mPageIndicatorView, mPosition, R.drawable.ic_notice_dot_normal, R.drawable.ic_notice_dot_selected);
        			mActionLog.addAction(mActionTag+ActionLog.ViewPageSelected, mPosition);
        		}
        		
        		@Override
        		public void onPageScrolled(int arg0, float arg1, int arg2) {
        		}
        		
        		@Override
        		public void onPageScrollStateChanged(int index) {
        			index = index % mPagecount;
        		}
        	});
        	mHandler.removeCallbacks(mNoticeNextRun);
        	mHandler.postDelayed(mNoticeNextRun, 4000);
        }else if(mPagecount == 1){
        	mNoticeRly.setVisibility(View.VISIBLE);
        	mViewPager.setCurrentItem(0);
        	refreshAllNoticeDrawable();
        	mPosition = 0;
        }
    }
    
    public void refreshBootStrapData(){
    	BootstrapModel bootstrapModel = Globals.g_Bootstrap_Model;
        if (bootstrapModel != null) {
            setNoticeList();
            Recommend recommend = bootstrapModel.getRecommend();
            if (recommend != null) {
                if (recommend.getRecommendAppList() == null) {
                    return;
                }
                mRecommendAppList.clear();
                mRecommendAppList.addAll(recommend.getRecommendAppList());
                refreshAppRecommendDrawable();
            }
        }else{
        	mHandler.removeCallbacks(mReloadAppRecommend);
        	mHandler.postDelayed(mReloadAppRecommend, 12000);
        }
    }
    
    private void refreshAppRecommendDrawable(){
    	final int len = Math.min(mRecommendAppList.size(), NUM_APP_RECOMMEND);
    	for (int i=0; i<NUM_APP_RECOMMEND && i<len; i++){
    		refreshDrawable(mRecommendAppList.get(i).getIcon(), mAppRecommendImv[i], R.drawable.bg_picture_none, true);
    		mAppRecommendTxv[i].setText(mRecommendAppList.get(i).getName());
    	}
    }

    private void refreshSatisfyRate() {
    	if (TextUtils.isEmpty(TKConfig.getPref(mContext, TKConfig.PREFS_SATISFY_RATE_OPENED, ""))){
    		Drawable[] drawables = mSatisfyRateBtn.getCompoundDrawables();
    		drawables[2] = mContext.getResources().getDrawable(R.drawable.ic_satisfy_new);
    		drawables[2].setBounds(0, 0, drawables[2].getIntrinsicWidth(), drawables[2].getIntrinsicHeight());
    		mSatisfyRateBtn.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
    		mSatisfyRateBtn.setCompoundDrawablePadding(Utility.dip2px(mContext, 20));
    	}else{
    		Drawable[] drawables = mSatisfyRateBtn.getCompoundDrawables();
    		mSatisfyRateBtn.setCompoundDrawables(drawables[0], drawables[1], null, drawables[3]);
    	}
    }        
    
    public void refreshCity(String cityName) {
        mCityName = cityName;
        
        if (!mCurrentCityTxv.getText().toString().equals(mCityName)) {
            mCurrentCityTxv.setText(mCityName);
        }
    }

    private void refreshUserEntrance() {
    	if (TextUtils.isEmpty(Globals.g_Session_Id) == false) {
        	mUserBtn.setText(mContext.getString(R.string.user_home));
        	mUserNameTxv.setMaxWidth(mContext.getResources().getDisplayMetrics().widthPixels -
        			mUserBtn.getPaddingLeft() -
        			mUserBtn.getPaddingRight() -
        			(int)mUserBtn.getTextSize() * 4 -
        			Utility.dip2px(mContext, 8));
        	if (Globals.g_User != null) {
        		String nickNameText = Globals.g_User.getNickName();
            	mUserNameTxv.setText(nickNameText);
            	mUserNameTxv.setVisibility(View.VISIBLE);
        	}
        } else {
        	mUserBtn.setText(mContext.getString(R.string.login_or_regist));
        	mUserNameTxv.setVisibility(View.GONE);
        }
    }
    
    private void refreshAllNoticeDrawable(){
    	if(mPagecount > 0){
    		int mapPagecount = (mPagecount == 2 || mPagecount ==3) ? 2*mPagecount : mPagecount;
    		for(int i=0; i<mapPagecount; i++){
    			if(imageViewMap.containsKey(i)){
    				if(getView(i).getClass() == ImageView.class){
    					refreshDrawable(mNoticeList.get(i % mPagecount).getpicTkDrawable(), imageViewMap.get(i), R.drawable.bg_picture_detail, false);
    				}else{
    					refreshDrawable(mNoticeList.get(i % mPagecount).getpicTkDrawable(), imageViewMap.get(i), R.drawable.bg_picture_none, false);
    				}
    			}
    		}
    	}
    }
    
    private void refreshDrawable(TKDrawable tkDrawable, ImageView imageView, int defaultResId, boolean needRefresh){
    	if (tkDrawable != null) {
    		Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, MoreHomeFragment.this.toString());
    		if(drawable != null){
    			if(needRefresh == true){
    				Rect bounds = drawable.getBounds();
    				if(bounds != null && (bounds.width() != imageView.getWidth() || bounds.height() != imageView.getHeight())){
    					imageView.setBackgroundDrawable(null);
    				}
    			}
    			imageView.setBackgroundDrawable(drawable);
    		}else{
    			imageView.setBackgroundDrawable(mSphinx.getResources().getDrawable(defaultResId));
    		}
    	}
    }

    public View getView(int position) {
    	if(mPagecount == 2 || mPagecount == 3)position = position % (2*mPagecount);
    	else position = position % mPagecount;
    	Notice notice = mNoticeList.get(position % mPagecount);
        if (viewMap.containsKey(position)) {
            return viewMap.get(position);
        }
        int noticeLayoutType = 0;
        String title = notice.getNoticeTitle();
        String description = notice.getDescription();
        String picUrl = notice.getPicUrl();
        int pd8 = Utility.dip2px(mContext, 8);
        int pd2 = Utility.dip2px(mContext, 2);
        if(title != null && !TextUtils.isEmpty(title)){
        	noticeLayoutType += 1;
        }
        if(picUrl != null && !TextUtils.isEmpty(picUrl)){
        	noticeLayoutType += 2;
        }
        if(description != null && !TextUtils.isEmpty(description)){
        	noticeLayoutType += 4;
        }
        switch(noticeLayoutType){
        case 1:
        	Button button1 = new Button(mSphinx);
        	button1.setText(title);
        	button1.setBackgroundResource(R.drawable.btn_notice);
        	button1.setPadding(pd8, pd8, pd8, pd8);
        	button1.setTextSize(16);
        	viewMap.put(position, button1);
        	return button1;
        case 2:
        	ImageView imageView2 = new ImageView(mSphinx);
        	refreshDrawable(notice.getpicTkDrawable(), imageView2, R.drawable.bg_picture_detail, false);
        	imageView2.setScaleType(ScaleType.FIT_XY);
        	imageViewMap.put(position, imageView2);
        	viewMap.put(position, imageView2);
        	return imageView2;
        case 5:
        	LinearLayout linearLayout5 = new LinearLayout(mSphinx);
        	linearLayout5.setOrientation(VERTICAL);
        	linearLayout5.setBackgroundResource(R.drawable.btn_notice);
        	linearLayout5.setGravity(Gravity.CENTER_HORIZONTAL);
        	TextView titleTxv5 = new TextView(mSphinx);
        	titleTxv5.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        	titleTxv5.setPadding(pd8, pd2, pd8, 0);
        	titleTxv5.setTextSize(16);
        	titleTxv5.setTextColor(getResources().getColor(R.color.black_dark));
        	titleTxv5.setSingleLine(true);
        	titleTxv5.setText(title);
        	TextView descriptionTxv5 = new TextView(mSphinx);
        	descriptionTxv5.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        	descriptionTxv5.setPadding(pd8, 0, pd8, 0);
        	descriptionTxv5.setTextSize(14);
        	descriptionTxv5.setTextColor(getResources().getColor(R.color.black_dark));
        	descriptionTxv5.setSingleLine(true);
        	descriptionTxv5.setText(description);
        	linearLayout5.addView(titleTxv5);
        	linearLayout5.addView(descriptionTxv5);
        	viewMap.put(position, linearLayout5);
        	return linearLayout5;
        case 3:
        	LinearLayout linearLayout3 = new LinearLayout(mSphinx);
        	ImageView imageView3 = new ImageView(mSphinx);
        	linearLayout3.setOrientation(HORIZONTAL);
        	linearLayout3.setBackgroundResource(R.drawable.btn_notice);
        	linearLayout3.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        	refreshDrawable(notice.getpicTkDrawable(), imageView3, R.drawable.bg_picture_none, false);
        	LinearLayout.LayoutParams layoutParams3 = new LayoutParams(Utility.dip2px(mContext, 48), Utility.dip2px(mContext, 48));
        	layoutParams3.setMargins(Utility.dip2px(mContext, 16), 0, 0, 0);
        	layoutParams3.gravity = Gravity.CENTER_VERTICAL;
        	imageView3.setLayoutParams(layoutParams3);
        	imageView3.setScaleType(ScaleType.FIT_XY);
        	TextView titleTxv3 = new TextView(mSphinx);
        	titleTxv3.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        	titleTxv3.setTextSize(16);
        	titleTxv3.setTextColor(getResources().getColor(R.color.black_dark));
        	titleTxv3.setSingleLine(true);
        	titleTxv3.setText(title);
        	titleTxv3.setGravity(Gravity.LEFT | Gravity.CENTER_HORIZONTAL);
        	titleTxv3.setPadding(Utility.dip2px(mContext, 26), pd8, pd8, pd8);
        	linearLayout3.addView(imageView3);
        	linearLayout3.addView(titleTxv3);
        	imageViewMap.put(position, imageView3);
        	viewMap.put(position, linearLayout3);
        	return linearLayout3;
        case 7:
        	LinearLayout linearLayout7 = new LinearLayout(mSphinx);
        	linearLayout7.setOrientation(HORIZONTAL);
        	linearLayout7.setBackgroundResource(R.drawable.btn_notice);
        	ImageView imageView7 = new ImageView(mSphinx);
        	refreshDrawable(notice.getpicTkDrawable(), imageView7, R.drawable.bg_picture_none, false);
        	LinearLayout.LayoutParams layoutParams7 = new LayoutParams(Utility.dip2px(mContext, 48), Utility.dip2px(mContext, 48));
        	layoutParams7.setMargins(Utility.dip2px(mContext, 16), 0, 0, 0);
        	layoutParams7.gravity = Gravity.CENTER_VERTICAL;
        	imageView7.setLayoutParams(layoutParams7);
        	imageView7.setScaleType(ScaleType.FIT_XY);
        	LinearLayout textLly7 = new LinearLayout(mSphinx);
        	textLly7.setOrientation(VERTICAL);
        	textLly7.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        	textLly7.setGravity(Gravity.LEFT);
        	TextView titleTxv7 = new TextView(mSphinx);
        	titleTxv7.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        	titleTxv7.setTextSize(16);
        	titleTxv7.setTextColor(getResources().getColor(R.color.black_dark));
        	titleTxv7.setSingleLine(true);
        	titleTxv7.setText(title);
        	titleTxv7.setPadding(Utility.dip2px(mContext, 26), pd2, pd8, 0);
        	TextView descriptionTxv7 = new TextView(mSphinx);
        	descriptionTxv7.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        	descriptionTxv7.setTextSize(14);
        	descriptionTxv7.setTextColor(getResources().getColor(R.color.black_dark));
        	descriptionTxv7.setSingleLine(true);
        	descriptionTxv7.setText(description);
        	descriptionTxv7.setPadding(Utility.dip2px(mContext, 26), 0, pd8, 0);
        	textLly7.addView(titleTxv7);
        	textLly7.addView(descriptionTxv7);
        	linearLayout7.addView(imageView7);
        	linearLayout7.addView(textLly7);
        	imageViewMap.put(position, imageView7);
        	viewMap.put(position, linearLayout7);
        	return linearLayout7;
        default:
        	Button button = new Button(mSphinx);
        	button.setText("该活动不存在或已失效");
        	button.setTextSize(16);
        	button.setBackgroundResource(R.drawable.btn_notice);
        	button.setPadding(pd8, pd8, pd8, pd8);
        	viewMap.put(position, button);
        	return button;
        	}
    }

	public class MyAdapter extends PagerAdapter {
	    
	    public MyAdapter() {
	    }
	    
	    @Override
	    public int getCount() {
	    	if(mPagecount < 0) return 0;
	    	else if(mPagecount == 1)return 1;
	        return mPagecount*VIEW_PAGE_LEFT*VIEW_PAGE_MULTIPLIER;
	    }
	
	    @Override
	    public boolean isViewFromObject(View arg0, Object arg1) {
	        return arg0 == arg1;
	    }
	
	    @Override
	    public int getItemPosition(Object object) {
	        return super.getItemPosition(object);
	    }
	
	    @Override
	    public void destroyItem(View contain, int position, Object arg2) {
	         ((ViewPager) contain).removeView(getView(position));
	    }
	
	    @Override
	    public Object instantiateItem(ViewGroup contain, int position) {
	        View view = getView(position);
	        final int fPosition = position;
	        view.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					mActionLog.addAction(mActionTag + ActionLog.MoreNotice);
					Notice notice = mNoticeList.get(fPosition % mPagecount);
					switch((int)notice.getLocalType()){
					case 0:
						break;
					case 1:
						mActionLog.addAction(mActionTag + ActionLog.MoreSoftwareUpdate);
						showDownloadSoftwareDialog();
						return;
					default:
						return;
					}

					switch((int)notice.getOperationType()){
					case 0:
						mActionLog.addAction(mActionTag + ActionLog.MoreNotice, notice.getNoticeId());
						String uri = notice.getUrl();
						if (!TextUtils.isEmpty(uri)) {
							Intent intent = new Intent();
							intent.putExtra(BrowserActivity.TITLE, notice.getWebTitle());
							intent.putExtra(BrowserActivity.URL, uri);
							mSphinx.showView(R.id.activity_browser, intent);
						}
						break;
					default:
						break;
                    }
				}
			});
	        contain.addView(view, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	        return view;
	    }
	
	    @Override
	    public void restoreState(Parcelable arg0, ClassLoader arg1) {
	    }
	
	    @Override
	    public Parcelable saveState() {
	        return null;
	    }
	
	    @Override
	    public void startUpdate(View arg0) {
	    }
	
	    @Override
	    public void finishUpdate(View arg0) {
	    }
	
	}
    private void showDownloadSoftwareDialog() {
        if (mSoftwareUpdate == null) {
            return;
        }
        Utility.showNormalDialog(mSphinx,
                mContext.getString(R.string.download_software_title), 
                mSoftwareUpdate.getText(),
                new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        switch (id) {
                            case DialogInterface.BUTTON_POSITIVE:
                                String url = mSoftwareUpdate.getURL();
                                if (url != null) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    mSphinx.startActivity(intent);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                });
    }

}
