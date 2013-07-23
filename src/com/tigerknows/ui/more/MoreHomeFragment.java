/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.more;

import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Bootstrap;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.DataOperation.DiaoyanQueryResponse;
import com.tigerknows.model.BootstrapModel.Recommend;
import com.tigerknows.model.BootstrapModel.SoftwareUpdate;
import com.tigerknows.model.BootstrapModel.Recommend.RecommendApp;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.BrowserActivity;
import com.tigerknows.ui.more.MapDownloadActivity.DownloadCity;
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
    
    public static final int SHOW_COMMENT_TIP_TIMES = 3;
    private static final int NUM_APP_RECOMMEND = 5;
    
    private ListView mListLsv;
    private TextView mUserNameTxv;
    private TextView mCurrentCityTxv;
    private Button mMessageBtn;
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
    private int mMessageType = MESSAGE_TYPE_NONE;
    
    private boolean addedGoCommentTimes = false;
    
    private DiaoyanQueryResponse mDiaoyanQueryResponse;
    
    public static DownloadCity CurrentDownloadCity;
    
    public void setDiaoyanQueryResponse(DiaoyanQueryResponse diaoyanQueryResponse) {
    	mDiaoyanQueryResponse = diaoyanQueryResponse;
    }
    
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
        	// do nothing
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.More;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.list_view, container, false);
        mListLsv = (ListView) mRootView.findViewById(R.id.list_lsv);
        
        View headerview = mLayoutInflater.inflate(R.layout.more_home, mListLsv, false);
        mListLsv.addHeaderView(headerview);
        mListLsv.setAdapter(null);
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
            mListLsv.setSelection(0);
        }

        mMenuFragment.display();
        
        refreshUserEntrance();
        refreshMoreBtn();
        refreshCity(Globals.getCurrentCityInfo().getCName());
    }
    
    public void refreshMoreBtn() {
        
    	refreshMoreData();
    	// 你可能喜欢的应用
        BootstrapModel bootstrapModel = Globals.g_Bootstrap_Model;
        setFragmentMessage(MESSAGE_TYPE_NONE);
        
        // 软件更新
        if (bootstrapModel != null) {            
            SoftwareUpdate softwareUpdate = bootstrapModel.getSoftwareUpdate();
            if (softwareUpdate != null) {
                setFragmentMessage(MoreHomeFragment.MESSAGE_TYPE_SOFTWARE_UPDATE);
                return;
            }
        }

        // 地图更新

        boolean upgrade = false;
        DownloadCity currentDownloadCity = CurrentDownloadCity;
        if (currentDownloadCity != null
                && currentDownloadCity.state == DownloadCity.STATE_CAN_BE_UPGRADE
                && currentDownloadCity.cityInfo.getId() != MapEngine.CITY_ID_QUANGUO) {
            upgrade = true;
        }
        if (upgrade) {
            setFragmentMessage(MoreHomeFragment.MESSAGE_TYPE_MAP_UPDATE);
            return;
        }
        
        // 用户调研
        if (mDiaoyanQueryResponse != null) {
        	if(mDiaoyanQueryResponse.getHasSurveyed() == 0 && mDiaoyanQueryResponse.getUrl() != null){
                setFragmentMessage(MESSAGE_TYPE_USER_SURVEY);
                return;
        	}
        }

        // 点评
        int showCommentTipTimes = 0;
        String commentTip = TKConfig.getPref(mContext, TKConfig.PREFS_SHOW_UPGRADE_COMMENT_TIP);
        if (!TextUtils.isEmpty(commentTip)) {
            showCommentTipTimes = Integer.parseInt(commentTip);
            if (addedGoCommentTimes == false) {
                addedGoCommentTimes = true;
                showCommentTipTimes++;
                TKConfig.setPref(mContext, TKConfig.PREFS_SHOW_UPGRADE_COMMENT_TIP, String.valueOf(showCommentTipTimes));
            }
        } else {
            if (addedGoCommentTimes == false) {
                addedGoCommentTimes = true;
                TKConfig.setPref(mContext, TKConfig.PREFS_SHOW_UPGRADE_COMMENT_TIP, String.valueOf(0));
            }
        }
        
        if (showCommentTipTimes < SHOW_COMMENT_TIP_TIMES) {
            setFragmentMessage(MoreHomeFragment.MESSAGE_TYPE_COMMENT);
            return;
        }
        
        // 满意度评分(不是button)
        if(TextUtils.isEmpty(TKConfig.getPref(mContext, TKConfig.PREFS_SATISFY_RATE_OPENED, ""))){
        	mSphinx.getMenuFragment().setFragmentMessage(View.VISIBLE);
        	return;
        }
    }
    
    private void refreshMoreData(){
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
                	refreshDrawable(mRecommendAppList.get(i).getIcon(), mAppRecommendImv[i]);
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
    }
    
    protected void findViews() {
        mCurrentCityTxv = (TextView) mRootView.findViewById(R.id.current_city_txv);
        mUserNameTxv = (TextView) mRootView.findViewById(R.id.user_name_txv);
        mMessageBtn = (Button)mRootView.findViewById(R.id.message_btn);
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
    }
    
    protected void setListener() {
    	mRightBtn.setOnClickListener(this);
        mMessageBtn.setOnClickListener(this);
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
	                    //mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, recommendApp.getName());
	                    final String uri = recommendApp.getUrl();
	                    if (!TextUtils.isEmpty(uri)) {
	                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
	                        mSphinx.startActivity(intent);
	                    }
	                }
				}
			});
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.message_btn:
                if (mMessageType == MESSAGE_TYPE_SOFTWARE_UPDATE) {
                    mActionLog.addAction(mActionTag +  ActionLog.MoreMessageSoft);
                    showDownloadSoftwareDialog();
                } else if (mMessageType == MESSAGE_TYPE_MAP_UPDATE) {
                    mActionLog.addAction(mActionTag +  ActionLog.MoreMessageMap);
                    mSphinx.showView(R.id.activity_more_map_download);
                } else if (mMessageType == MESSAGE_TYPE_USER_SURVEY) {
                	String url=mDiaoyanQueryResponse.getUrl();
                	if(url != null){               		
                		Intent intent=new Intent();
                		intent.putExtra(BrowserActivity.TITLE, mSphinx.getString(R.string.user_survey));
                		intent.putExtra(BrowserActivity.URL, url);
                		mDiaoyanQueryResponse.setHasSurveyed(1);
                		mSphinx.showView(R.id.activity_browser, intent);
                	}
                    mActionLog.addAction(mActionTag +  ActionLog.MoreMessageUserSurvey);
                } else if (mMessageType == MESSAGE_TYPE_COMMENT) {
                    mActionLog.addAction(mActionTag +  ActionLog.MoreMessageComment);
                    mSphinx.showView(R.id.view_more_go_comment);
                }
                break;
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
    

    
    private void setFragmentMessage(int messageType) {
        if (messageType == MESSAGE_TYPE_NONE) {
            mMessageType = MESSAGE_TYPE_NONE;
        } else if (messageType > mMessageType) {
            mMessageType = messageType;
        }
        if (mMessageType > MESSAGE_TYPE_NONE) {
            if (mMessageType == MESSAGE_TYPE_SOFTWARE_UPDATE) {
                mMessageBtn.setText(R.string.message_tip_software_update);
                mMessageBtn.setBackgroundResource(R.drawable.btn_update);
                mMessageBtn.setVisibility(View.VISIBLE);
                // setPadding需要在setVisibility之后调用，这样才能避免padding设置不起作用的情况
                mMessageBtn.setPadding(Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8));
                mSphinx.getMenuFragment().setFragmentMessage(View.VISIBLE);
                return;
            } else if (mMessageType == MESSAGE_TYPE_COMMENT) {
                mMessageBtn.setText(R.string.message_tip_comment);
                mMessageBtn.setBackgroundResource(R.drawable.btn_orangle);
                TKConfig.getPref(mContext, TKConfig.PREFS_SHOW_UPGRADE_MAP_TIP);
                mMessageBtn.setVisibility(View.VISIBLE);
                mMessageBtn.setPadding(Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8));
                mSphinx.getMenuFragment().setFragmentMessage(View.VISIBLE);
                return;
            } else if (mMessageType == MESSAGE_TYPE_MAP_UPDATE) {
                mMessageBtn.setText(R.string.message_tip_map_upgrade);
                mMessageBtn.setBackgroundResource(R.drawable.btn_update);
                mMessageBtn.setVisibility(View.VISIBLE);
                mMessageBtn.setPadding(Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8));
                mSphinx.getMenuFragment().setFragmentMessage(View.VISIBLE);
                return;
            } else if (mMessageType == MESSAGE_TYPE_USER_SURVEY) {
                mMessageBtn.setText(mDiaoyanQueryResponse.getDescription());
                mMessageBtn.setBackgroundResource(R.drawable.btn_update);
                mMessageBtn.setVisibility(View.VISIBLE);
                mMessageBtn.setPadding(Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8));
                mSphinx.getMenuFragment().setFragmentMessage(View.VISIBLE);
                return;
            }
        }
        mMessageBtn.setVisibility(View.GONE);
        mSphinx.getMenuFragment().setFragmentMessage(View.GONE);
    }

    private void showDownloadSoftwareDialog() {

        SoftwareUpdate softwareUpdate = null;
        BootstrapModel bootstrapModel = Globals.g_Bootstrap_Model;
        if (bootstrapModel != null) {
            softwareUpdate = bootstrapModel.getSoftwareUpdate();
        }
        final SoftwareUpdate finalSoftwareUpdate = softwareUpdate;
        if (finalSoftwareUpdate == null) {
            return;
        }
        Utility.showNormalDialog(mSphinx,
                mContext.getString(R.string.download_software_title), 
                softwareUpdate.getText(),
                new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        switch (id) {
                            case DialogInterface.BUTTON_POSITIVE:
                                String url = finalSoftwareUpdate.getURL();
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
    private void refreshDrawable(TKDrawable tkDrawable, ImageView imageView){
    	boolean result = false;
    	if (tkDrawable != null) {
    		Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, MoreHomeFragment.this.toString());
    		if(drawable != null){
    			result = true;
    			imageView.setBackgroundDrawable(drawable);
    		}
    	}
    }
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (baseQuery instanceof Bootstrap) {
            BootstrapModel bootstrapModel = ((Bootstrap) baseQuery).getBootstrapModel();
            if (bootstrapModel != null) {
                Globals.g_Bootstrap_Model = bootstrapModel;
                refreshMoreData();
            }
        }
    }
}
