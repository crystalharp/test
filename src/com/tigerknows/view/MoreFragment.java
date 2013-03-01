/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.MapDownload.DownloadCity;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.model.UserLogonModel;
import com.tigerknows.model.UserLogonModel.Recommend;
import com.tigerknows.model.UserLogonModel.SoftwareUpdate;
import com.tigerknows.model.UserLogonModel.Recommend.RecommendApp;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.view.user.UserBaseActivity;
import com.tigerknows.view.user.UserLoginActivity;

/**
 * @author Peng Wenyue
 */
public class MoreFragment extends BaseFragment implements View.OnClickListener {
    
    public MoreFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    static final String TAG = "MoreFragment";
    
    public static final int SHOW_COMMENT_TIP_TIMES = 3;
    
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private ListView mListLsv;
    private TextView mUserNameTxv;
    private TextView mCurrentCityTxv;
    private Button mUpgradeBtn;
    private Button mUserBtn;
    private Button mChangeCityBtn;
    private Button mDownloadMapBtn;
    private Button mAppRecommendBtn;
    private Button mFavoriteBtn;
    private Button mHistoryBrowseBtn;
    private Button mSettingsBtn;
    private Button mFeedbackBtn;
    private Button mAddMerchantBtn;
    private Button mHelpBtn;
    private Button mAboutBtn;
    private Button mGiveFavourableCommentBtn;
    private View mGiveFavourableCommentImv;
    
    private String mCityName;
    
    public static final int UPGRADE_TYPE_NONE = 0;
    public static final int UPGRADE_TYPE_MAP = 1;
    public static final int UPGRADE_TYPE_COMMENT = 2;
    public static final int UPGRADE_TYPE_SOFTWARE = 3;
    public static final int UPGRADE_TYPE_PUBLIC_WELFARRE = 4;
    private int mUpgradeType = UPGRADE_TYPE_NONE;
    
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
        
        View headerview = mLayoutInflater.inflate(R.layout.more, mListLsv, false);
        mListLsv.addHeaderView(headerview);
        mListLsv.setAdapter(null);
        
        findViews();        
        setListener();
        
        if (TKConfig.sSPREADER.startsWith(TKConfig.sSPREADER_TENCENT)) {
            mAppRecommendBtn.setText(R.string.recommend_tencent);
        } else {
            mAppRecommendBtn.setText(R.string.app_recommend);
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
        mRightBtn.setVisibility(View.INVISIBLE);

        mMenuFragment.display();
        
        refreshUserEntrance();
        refreshMoreBtn(false);
    }
    
    public void refreshMoreBtn(boolean isCreate) {
        
        setUpgrade(UPGRADE_TYPE_NONE);
        
        UserLogonModel userLogonModel = Globals.g_User_Logon_Model;
        if (userLogonModel != null) {            
            SoftwareUpdate softwareUpdate = userLogonModel.getSoftwareUpdate();
            if (softwareUpdate != null) {
                setUpgrade(MoreFragment.UPGRADE_TYPE_SOFTWARE);
                return;
            }
        }

        int showCommentTipTimes = 0;
        String commentTip = TKConfig.getPref(mContext, TKConfig.PREFS_SHOW_UPGRADE_COMMENT_TIP);
        if (!TextUtils.isEmpty(commentTip)) {
            showCommentTipTimes = Integer.parseInt(commentTip);
            if (isCreate) {
                TKConfig.setPref(mContext, TKConfig.PREFS_SHOW_UPGRADE_COMMENT_TIP, String.valueOf(showCommentTipTimes+1));
            }
        } else {
            if (isCreate) {
                TKConfig.setPref(mContext, TKConfig.PREFS_SHOW_UPGRADE_COMMENT_TIP, String.valueOf(0));
            }
        }
        
        if (showCommentTipTimes < SHOW_COMMENT_TIP_TIMES) {
            setUpgrade(MoreFragment.UPGRADE_TYPE_COMMENT);
            return;
        }
        
        String upgradeMapTip = TKConfig.getPref(mContext, TKConfig.PREFS_SHOW_UPGRADE_MAP_TIP);
        int out = 8;
        if (!TextUtils.isEmpty(upgradeMapTip)) {
            try {
            Calendar calendar = Calendar.getInstance();
            String[] strs = upgradeMapTip.split("-");
            calendar.set(Integer.parseInt(strs[0]), Integer.parseInt(strs[1])-1, Integer.parseInt(strs[2]));
            out = CommonUtils.compareDate(calendar, Calendar.getInstance(), 0);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        
        boolean upgrade = false;
        MapEngine mapEngine = mSphinx.getMapEngine();
        synchronized (mapEngine) {
            List<DownloadCity> list = mapEngine.getDownloadCityList();
            for(DownloadCity downloadCity : list) {
                if (downloadCity.getState() == DownloadCity.MAYUPDATE && downloadCity.getCityInfo().getId() != MapEngine.CITY_ID_QUANGUO) {
                    upgrade = true;
                    break;
                }
            }
        }
        if (upgrade && out >= 7) {
            setUpgrade(MoreFragment.UPGRADE_TYPE_MAP);
            return;
        }
        
        RecommendApp recommendApp = getPublicWelfarre();
        if (recommendApp != null) {
            setUpgrade(UPGRADE_TYPE_PUBLIC_WELFARRE);
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    
    protected void findViews() {
        mCurrentCityTxv = (TextView) mRootView.findViewById(R.id.current_city_txv);
        mUserNameTxv = (TextView) mRootView.findViewById(R.id.user_name_txv);
        mUpgradeBtn = (Button)mRootView.findViewById(R.id.upgrade_btn);
        mUserBtn = (Button)mRootView.findViewById(R.id.user_btn);
        mChangeCityBtn = (Button)mRootView.findViewById(R.id.change_city_btn);
        mDownloadMapBtn = (Button)mRootView.findViewById(R.id.download_map_btn);
        mAppRecommendBtn = (Button)mRootView.findViewById(R.id.app_recommend_btn);
        mFavoriteBtn = (Button)mRootView.findViewById(R.id.favorite_btn);
        mHistoryBrowseBtn = (Button)mRootView.findViewById(R.id.history_browse_btn);
        mSettingsBtn = (Button)mRootView.findViewById(R.id.settings_btn);
        mFeedbackBtn = (Button)mRootView.findViewById(R.id.feedback_btn);
        mAddMerchantBtn = (Button)mRootView.findViewById(R.id.add_merchant_btn);
        mHelpBtn = (Button)mRootView.findViewById(R.id.help_btn);
        mAboutBtn = (Button)mRootView.findViewById(R.id.about_btn);
        mGiveFavourableCommentBtn = (Button)mRootView.findViewById(R.id.give_favourable_comment_btn);
        mGiveFavourableCommentImv = mRootView.findViewById(R.id.give_favourable_comment_imv);
    }
    
    protected void setListener() {
        mUpgradeBtn.setOnClickListener(this);
        mUserBtn.setOnClickListener(this);
        mChangeCityBtn.setOnClickListener(this);
        mDownloadMapBtn.setOnClickListener(this);
        mAppRecommendBtn.setOnClickListener(this);
        mFavoriteBtn.setOnClickListener(this);
        mHistoryBrowseBtn.setOnClickListener(this);
        mSettingsBtn.setOnClickListener(this);
        mFeedbackBtn.setOnClickListener(this);
        mAddMerchantBtn.setOnClickListener(this);
        mHelpBtn.setOnClickListener(this);
        mAboutBtn.setOnClickListener(this);
        mGiveFavourableCommentBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.upgrade_btn:
                if (mUpgradeType == UPGRADE_TYPE_SOFTWARE) {
                    mActionLog.addAction(ActionLog.MoreUpdateSoft);
                    showDownloadSoftwareDialog();
                } else if (mUpgradeType == UPGRADE_TYPE_COMMENT) {
                    mActionLog.addAction(ActionLog.MoreUpdateComment);
                    mSphinx.showView(R.id.view_go_comment);
                } else if (mUpgradeType == UPGRADE_TYPE_MAP) {
                    mActionLog.addAction(ActionLog.MoreUpdateMap);
                    showUpgradeMapDialog();
                } else if (mUpgradeType == UPGRADE_TYPE_PUBLIC_WELFARRE) {
                    RecommendApp publicWelfarre = getPublicWelfarre();
                    if (publicWelfarre != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(publicWelfarre.getUrl()));
                        mSphinx.startActivity(intent);
                    }
                }
                break;
            case R.id.user_btn:
            	if (TextUtils.isEmpty(Globals.g_Session_Id) == false) {
            		mActionLog.addAction(ActionLog.MoreUserHome);
            		mSphinx.showView(R.id.view_user_home);
            	} else {
            		mActionLog.addAction(ActionLog.MoreLoginRegist);
            		Intent intent = new Intent(mSphinx, UserLoginActivity.class);
                    intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, R.id.view_user_home);
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, getId());
            		mSphinx.startActivityForResult(intent, 0);
            	}
                break;
            case R.id.change_city_btn:
                mActionLog.addAction(ActionLog.MoreChangeCity);
                mSphinx.showView(R.id.activity_change_city);
                break;
            case R.id.download_map_btn:
                mActionLog.addAction(ActionLog.MoreDownloadMap);
                mSphinx.showView(R.id.activity_map_download);
                break;
            case R.id.app_recommend_btn:
                mActionLog.addAction(ActionLog.MoreAppRecommend);
                if (TKConfig.sSPREADER.startsWith(TKConfig.sSPREADER_TENCENT)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://a.wap.myapp.com/and2/s?aid=detail&appid=50801"));
                    mSphinx.startActivity(intent);
                } else {
                    mSphinx.showView(R.id.activity_app_recommend);
                }
                break;
            case R.id.favorite_btn:
                mActionLog.addAction(ActionLog.MoreFavorite);
                mSphinx.showView(R.id.view_favorite);
                break;
            case R.id.history_browse_btn:
                mActionLog.addAction(ActionLog.MoreHistory);
                mSphinx.showView(R.id.view_history);
                break;
            case R.id.settings_btn:
                mActionLog.addAction(ActionLog.MoreSetting);
                mSphinx.showView(R.id.activity_setting);
                break;
            case R.id.feedback_btn:
                mActionLog.addAction(ActionLog.MoreFeedback);
                mSphinx.showView(R.id.activity_feedback);
                break;
            case R.id.add_merchant_btn:
                mSphinx.showView(R.id.activity_add_merchant);
                break;
            case R.id.help_btn:
                mActionLog.addAction(ActionLog.MoreHelp);
                mSphinx.showView(R.id.activity_help);
                break;
            case R.id.about_btn:
                mActionLog.addAction(ActionLog.MoreAboutUs);
                mSphinx.showView(R.id.activity_about_us);
                break;
                
            case R.id.give_favourable_comment_btn:
                mActionLog.addAction(ActionLog.MoreGiveFavourableComment);
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
        	Drawable[] drawables = mUserBtn.getCompoundDrawables();
        	drawables[0] = mContext.getResources().getDrawable(R.drawable.ic_user_home);
        	drawables[0].setBounds(0, 0, drawables[0].getIntrinsicWidth(), drawables[0].getIntrinsicHeight());
        	mUserBtn.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
        	mUserBtn.setCompoundDrawablePadding(CommonUtils.dip2px(mContext, 10));
        	if (Globals.g_User != null) {
            	mUserNameTxv.setText(Globals.g_User.getNickName());
            	mUserNameTxv.setVisibility(View.VISIBLE);
        	}
        } else {
        	mUserBtn.setText(mContext.getString(R.string.login_or_regist));
        	Drawable[] drawables = mUserBtn.getCompoundDrawables();
        	drawables[0] = mContext.getResources().getDrawable(R.drawable.ic_login_regist);
        	drawables[0].setBounds(0, 0, drawables[0].getIntrinsicWidth(), drawables[0].getIntrinsicHeight());
        	mUserBtn.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
        	mUserNameTxv.setVisibility(View.GONE);
        }
    }
    
    private void setUpgrade(int upgradeType) {
        if (upgradeType == UPGRADE_TYPE_NONE) {
            mUpgradeType = UPGRADE_TYPE_NONE;
        } else if (upgradeType > mUpgradeType) {
            mUpgradeType = upgradeType;
        }
        if (mUpgradeType > UPGRADE_TYPE_NONE) {
            mUpgradeBtn.setPadding(Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8), Util.dip2px(Globals.g_metrics.density, 8));
            if (mUpgradeType == UPGRADE_TYPE_SOFTWARE) {
                mUpgradeBtn.setText(R.string.upgrade_tip_software);
                mUpgradeBtn.setBackgroundResource(R.drawable.btn_update);
                mUpgradeBtn.setVisibility(View.VISIBLE);
                mSphinx.getMenuFragment().setUpgrade(View.VISIBLE);
                return;
            } else if (mUpgradeType == UPGRADE_TYPE_COMMENT) {
                mUpgradeBtn.setText(R.string.upgrade_tip_comment);
                mUpgradeBtn.setBackgroundResource(R.drawable.btn_orangle);
                TKConfig.getPref(mContext, TKConfig.PREFS_SHOW_UPGRADE_MAP_TIP);
                mUpgradeBtn.setVisibility(View.VISIBLE);
                mSphinx.getMenuFragment().setUpgrade(View.VISIBLE);
                return;
            } else if (mUpgradeType == UPGRADE_TYPE_MAP) {
                mUpgradeBtn.setText(R.string.upgrade_tip_map);
                mUpgradeBtn.setBackgroundResource(R.drawable.btn_update);
                mUpgradeBtn.setVisibility(View.VISIBLE);
                mSphinx.getMenuFragment().setUpgrade(View.VISIBLE);
                return;
            } else if (mUpgradeType == UPGRADE_TYPE_PUBLIC_WELFARRE) {
                RecommendApp publicWelfarre = getPublicWelfarre();
                if (publicWelfarre != null) {
                    mUpgradeBtn.setText(publicWelfarre.getBody());
                    mUpgradeBtn.setBackgroundResource(R.drawable.btn_update);
                    mUpgradeBtn.setVisibility(View.VISIBLE);
                    mSphinx.getMenuFragment().setUpgrade(View.VISIBLE);
                    return;
                }
            }
        }
        mUpgradeBtn.setVisibility(View.GONE);
        mSphinx.getMenuFragment().setUpgrade(View.GONE);
    }

    private void showDownloadSoftwareDialog() {

        SoftwareUpdate softwareUpdate = null;
        UserLogonModel userLogonModel = Globals.g_User_Logon_Model;
        if (userLogonModel != null) {
            softwareUpdate = userLogonModel.getSoftwareUpdate();
        }
        final SoftwareUpdate finalSoftwareUpdate = softwareUpdate;
        if (finalSoftwareUpdate == null) {
            return;
        }
        CommonUtils.showNormalDialog(mSphinx,
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
    
    private void showUpgradeMapDialog() {
        final String oldUpgradeMapTip = TKConfig.getPref(mSphinx, TKConfig.PREFS_SHOW_UPGRADE_MAP_TIP);
        View view = mLayoutInflater.inflate(R.layout.alert_upgrade_map, null, false);
        
        final CheckBox checkChb = (CheckBox) view.findViewById(R.id.check_chb);
        checkChb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean checked) {
                TKConfig.setPref(mSphinx, TKConfig.PREFS_SHOW_UPGRADE_MAP_TIP, checked ? simpleDateFormat.format(Calendar.getInstance().getTime()) : oldUpgradeMapTip);
            }
        });
        
        CommonUtils.showNormalDialog(mSphinx,
                mSphinx.getString(R.string.prompt),
                null,
                view,
                mSphinx.getString(R.string.upgrade_all),
                mSphinx.getString(R.string.upgrade_manual),
                new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface arg0, int id) {
                        Intent intent = new Intent();
                        if (id == DialogInterface.BUTTON_POSITIVE) {
                            mActionLog.addAction(ActionLog.MoreUpdateMapAll);
                            intent.putExtra("upgradeAll", true);
                        } else {
                            mActionLog.addAction(ActionLog.MoreUpdateMapManual);
                        }
                        mSphinx.showView(R.id.activity_map_download, intent);
                    }
                });
    }
    
    public RecommendApp getPublicWelfarre() {
        RecommendApp publicWelfarre = null;
        UserLogonModel userLogonModel = Globals.g_User_Logon_Model;
        if (userLogonModel != null) {
            Recommend recommend = userLogonModel.getRecommend();
            if (recommend != null) {
                List<RecommendApp> list = recommend.getRecommendAppList();
                if (list != null) {
                    for(int i = 0, size = list.size(); i < size; i++) {
                        RecommendApp recommendApp = list.get(i);
                        if (recommendApp != null
                                && mSphinx.getString(R.string.public_welfarre).equals(recommendApp.getBody())) {
                            publicWelfarre = recommendApp;
                            break;
                        }
                    }
                }
            }
        }
        return publicWelfarre;
    }
}
