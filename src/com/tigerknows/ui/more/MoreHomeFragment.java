/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.more;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.Bootstrap;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.BootstrapModel.Recommend;
import com.tigerknows.model.BootstrapModel.Recommend.RecommendApp;
import com.tigerknows.model.NoticeQuery;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse.NoticeResult;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse.NoticeResult.Notice;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.BrowserActivity;
import com.tigerknows.ui.more.MapDownloadActivity.DownloadCity;
import com.tigerknows.ui.more.MoreHomeFragment.MyAdapter;
import com.tigerknows.ui.user.UserBaseActivity;
import com.tigerknows.ui.user.UserLoginActivity;
import com.tigerknows.util.Utility;

/**
 * @author Peng Wenyue
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
    
    private TextView mUserNameTxv;
    private TextView mCurrentCityTxv;
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
    private List<Notice> mNoticeList;
    private int mPagecount = -1;
    private ViewPager mViewPager;
    private HashMap<Integer, View> viewMap = new HashMap<Integer, View>();
    private ViewGroup mPageIndicatorView;

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
        	refreshCurrentNoticeDrawable();
        }
    };
    
    private Runnable mNoticeNextRun = new Runnable() {

		@Override
		public void run() {
			if(mPagecount > 1){
				mViewPager.setCurrentItem(mViewPager.getCurrentItem()+1);
			}
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
        refreshMoreData();
        refreshCity(Globals.getCurrentCityInfo().getCName());
        refreshCurrentNoticeDrawable();
        if(mPagecount > 1){
        	// 这两行代码用来解决onResume之后，首次定时器触发后，动画不正确的问题……
        	// 并且这样就不需要在onResume里调用mHandler.postDelayed(mNoticeNextRun, 4000);了
        	// 原理未知，反正试出来的。--fengtianxiao 2013.08.07
        	mViewPager.setCurrentItem(mViewPager.getCurrentItem()+1);
        	mViewPager.setCurrentItem(mViewPager.getCurrentItem()-1);
        }
    }
    public void refreshMoreData() {
    	refreshMoreNotice(null);
    	refreshAppRecommendData();
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
    public void refreshMoreNotice(NoticeResultResponse noticeResultResponse) {
    	
    	if(mNoticeResultResponse == null){
        	mNoticeResultResponse = noticeResultResponse;
        	if(mNoticeResultResponse != null){
        		mNoticeResult = mNoticeResultResponse.getNoticeResult();
        		if(mNoticeResult != null){
        			mPagecount = (int)mNoticeResult.getNum();
        			mNoticeList = mNoticeResult.getNoticeList();
        	        if(mPagecount > 1){
        	        	Utility.pageIndicatorInit(mSphinx, mPageIndicatorView, mPagecount, 0, R.drawable.ic_learn_dot_normal, R.drawable.ic_learn_dot_selected);
        	        	mNoticeRly.setVisibility(View.VISIBLE);
        	        	mViewPager.setCurrentItem(mPagecount * VIEW_PAGE_LEFT);
        	        	mPosition = 0;
        	        	mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
        	        		
        	        		@Override
        	        		public void onPageSelected(int index) {
        	        			LogWrapper.d("Trap", "Select:"+index);
        	        			mHandler.removeCallbacks(mNoticeNextRun);
        	        			mHandler.postDelayed(mNoticeNextRun, 4000);
        	        			mPosition = index % mPagecount;
        	        			Utility.pageIndicatorChanged(mSphinx, mPageIndicatorView, mPosition, R.drawable.ic_learn_dot_normal, R.drawable.ic_learn_dot_selected);
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
        	        }else if(mPagecount == 1){
        	        	mNoticeRly.setVisibility(View.VISIBLE);
        	        	mViewPager.setCurrentItem(0);
        	        	mPosition = 0;
        	        }
        		}
        	}else if(mPagecount < 0){
             	NoticeQuery noticeQuery = new NoticeQuery(mSphinx);
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                noticeQuery.setup(criteria, Globals.getCurrentCityInfo().getId());
                mSphinx.queryStart(noticeQuery);        		
        	}
    	}
    	LogWrapper.d("Trap", "size:"+(mNoticeList == null ? -1 : mNoticeList.size()));
        
        // 满意度评分(不是button)
        if(TextUtils.isEmpty(TKConfig.getPref(mContext, TKConfig.PREFS_SATISFY_RATE_OPENED, ""))){
        	mSphinx.getMenuFragment().setFragmentMessage(View.VISIBLE);
        	return;
        }
    }
    
    public void refreshAppRecommendData(){
    	BootstrapModel bootstrapModel = Globals.g_Bootstrap_Model;
        if (bootstrapModel != null) {
            Recommend recommend = bootstrapModel.getRecommend();
            if (recommend != null) {
                if (recommend.getRecommendAppList() == null) {
                    return;
                }
                mRecommendAppList.clear();
                mRecommendAppList.addAll(recommend.getRecommendAppList());
                final int len = Math.min(mRecommendAppList.size(), NUM_APP_RECOMMEND);
                for (int i=0; i<NUM_APP_RECOMMEND && i<len; i++){
                	refreshDrawable(mRecommendAppList.get(i).getIcon(), mAppRecommendImv[i], R.drawable.bg_picture_hotel_none);
                	mAppRecommendTxv[i].setText(mRecommendAppList.get(i).getName());
                }
            }
        }else{
            Bootstrap bootstrap = new Bootstrap(mSphinx);
            bootstrap.setup(null, Globals.getCurrentCityInfo().getId());
            mSphinx.queryStart(bootstrap);        	
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mNoticeNextRun);
    }
    
    protected void findViews() {
    	mNoticeRly = (RelativeLayout) mRootView.findViewById(R.id.notice_rly);
        mCurrentCityTxv = (TextView) mRootView.findViewById(R.id.current_city_txv);
        mUserNameTxv = (TextView) mRootView.findViewById(R.id.user_name_txv);
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
            	mActionLog.addAction(mActionTag +  ActionLog.MoreMessageComment);
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
    
    private void refreshCurrentNoticeDrawable(){
    	if(mPagecount > 0 && getView(mPosition).getClass() == ImageView.class){
    		refreshDrawable(mNoticeList.get(mPosition).getpicTkDrawable(), (ImageView)getView(mPosition), R.drawable.txt_app_name);
    	}
    }
    

    
    private void refreshDrawable(TKDrawable tkDrawable, ImageView imageView, int defaultResId){
    	if (tkDrawable != null) {
    		Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, MoreHomeFragment.this.toString());
    		if(drawable != null){
    			imageView.setBackgroundDrawable(drawable);
    		}else{
    			imageView.setBackgroundDrawable(mSphinx.getResources().getDrawable(defaultResId));
    		}
    	}
    }


    public View getView(int position) {
    	position = position % mPagecount;
        if (viewMap.containsKey(position)) {
            return viewMap.get(position);
        }
        Notice notice = mNoticeList.get(position);
        if((notice.getOperationType() & 1) == 0){
        	Button view = new Button(mSphinx);
        	view.setText(notice.getDescription());
        	view.setBackgroundResource(R.drawable.btn_update);
        	int padding = Utility.dip2px(mContext, 8);
        	view.setPadding(padding, padding, padding, padding);
        	viewMap.put(position, view);
        	return view;
        	
        }else{
        	ImageView view = new ImageView(mSphinx);
        	refreshDrawable(notice.getpicTkDrawable(), view, R.drawable.txt_app_name);
        	view.setScaleType(ScaleType.FIT_XY);
        	viewMap.put(position, view);
        	return view;
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
					// TODO mActionLog.addAction(mActionTag + ActionLog.?????);
					Notice notice = mNoticeList.get(fPosition % mPagecount);
                    String uri = notice.getUrl();
                    if (!TextUtils.isEmpty(uri)) {
                        Intent intent = new Intent();
                        intent.putExtra(BrowserActivity.TITLE, notice.getWebTitle());
                        intent.putExtra(BrowserActivity.URL, uri);
                        mSphinx.showView(R.id.activity_browser, intent);
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

}
