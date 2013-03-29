/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
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
import com.tigerknows.MapDownload;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.MapDownload.DownloadCity;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataOperation.DiaoyanQueryResponse;
import com.tigerknows.model.BootstrapModel.SoftwareUpdate;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.view.discover.BrowserActivity;
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
    public static final int SHOW_NICKNAME_MAX_WIDTH = 150;
    public static final int SHOW_NICKNAME_OMIT_WIDTH = 9;
    
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

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
    private Button mSettingsBtn;
    private Button mFeedbackBtn;
    private Button mAddMerchantBtn;
    private Button mHelpBtn;
    private Button mAboutBtn;
    private Button mGiveFavourableCommentBtn;
    private View mGiveFavourableCommentImv;
    
    private String mCityName;
    
    public static final int MESSAGE_TYPE_NONE = 0;
    public static final int MESSAGE_TYPE_SOFTWARE_UPDATE = 1;
    public static final int MESSAGE_TYPE_MAP_UPDATE = 2;
    public static final int MESSAGE_TYPE_USER_SURVEY = 3;
    public static final int MESSAGE_TYPE_COMMENT = 4;
    private int mMessageType = MESSAGE_TYPE_NONE;
    
    public static final int DIAOYAN_HASCLICKED = 2;
    private DiaoyanQueryResponse mDiaoyanQueryResponse;
    
    public static DownloadCity CurrentDownloadCity;
    
    public void setDiaoyanQueryResponse(DiaoyanQueryResponse diaoyanQueryResponse) {
    	mDiaoyanQueryResponse = diaoyanQueryResponse;
    }
    
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
        
        if (TKConfig.sSPREADER.startsWith(TKConfig.SPREADER_TENCENT)) {
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
        refreshCity(Globals.g_Current_City_Info.getCName());
    }
    
    public void refreshMoreBtn(boolean isCreate) {
        
        setFragmentMessage(MESSAGE_TYPE_NONE);
        
        //软件更新
        BootstrapModel bootstrapModel = Globals.g_Bootstrap_Model;
        if (bootstrapModel != null) {            
            SoftwareUpdate softwareUpdate = bootstrapModel.getSoftwareUpdate();
            if (softwareUpdate != null) {
                setFragmentMessage(MoreFragment.MESSAGE_TYPE_SOFTWARE_UPDATE);
                return;
            }
        }

        //地图更新
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
        DownloadCity currentDownloadCity = CurrentDownloadCity;
        if (currentDownloadCity != null
                && currentDownloadCity.state == DownloadCity.STATE_CAN_BE_UPGRADE
                && currentDownloadCity.cityInfo.getId() != MapEngine.CITY_ID_QUANGUO) {
            upgrade = true;
        }
        if (upgrade && out >= 7) {
            setFragmentMessage(MoreFragment.MESSAGE_TYPE_MAP_UPDATE);
            return;
        }
        
        //用户调研
        if (mDiaoyanQueryResponse != null) {
        	if(mDiaoyanQueryResponse.getHasSurveyed() == 0){
                setFragmentMessage(MESSAGE_TYPE_USER_SURVEY);
                return;
        	}else if(mDiaoyanQueryResponse.getHasSurveyed() == DIAOYAN_HASCLICKED){
        		/*
        		 * 本处规定如果用户已经点击了用户调研按钮，就把HasSurveyed赋值成2(服务器端不可能出现2)
        		 * 然后去服务端重新执行查询，再依据查询结果再次刷新
        		 */
                DataOperation diaoyanQuery = new DataOperation(mContext);
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(BaseQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_DIAOYAN);
                criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                diaoyanQuery.setup(criteria, Globals.g_Current_City_Info.getId());
                mSphinx.queryStart(diaoyanQuery);
                return;
        	}
        }

        //点评
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
            setFragmentMessage(MoreFragment.MESSAGE_TYPE_COMMENT);
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
        mMessageBtn = (Button)mRootView.findViewById(R.id.message_btn);
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
        mMessageBtn.setOnClickListener(this);
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
            case R.id.message_btn:
                if (mMessageType == MESSAGE_TYPE_SOFTWARE_UPDATE) {
                    mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "upgradeSoftWare");
                    showDownloadSoftwareDialog();
                } else if (mMessageType == MESSAGE_TYPE_MAP_UPDATE) {
                    mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "upgradeMap");
                    showUpgradeMapDialog();
                } else if (mMessageType == MESSAGE_TYPE_USER_SURVEY) {
                	String url=mDiaoyanQueryResponse.getUrl();
                	if(url != null){               		
                		Intent intent=new Intent();
                		intent.putExtra(BrowserActivity.TITLE, mSphinx.getString(R.string.user_survey));
                		intent.putExtra(BrowserActivity.URL, url);
                		mDiaoyanQueryResponse.setHasSurveyed(DIAOYAN_HASCLICKED);
                		mSphinx.showView(R.id.activity_browser, intent);
                	}
                } else if (mMessageType == MESSAGE_TYPE_COMMENT) {
                    mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "commentTip");
                    mSphinx.showView(R.id.view_go_comment);
                }
                break;
            case R.id.user_btn:
            	if (TextUtils.isEmpty(Globals.g_Session_Id) == false) {
                    mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "userHome");
            		mSphinx.showView(R.id.view_user_home);
            	} else {
                    mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "userLogin");
            		Intent intent = new Intent(mSphinx, UserLoginActivity.class);
                    intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, R.id.view_user_home);
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, getId());
            		mSphinx.startActivityForResult(intent, 0);
            	}
                break;
            case R.id.change_city_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "changeCity");
                mSphinx.showView(R.id.activity_change_city);
                break;
            case R.id.download_map_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "downloadMap");
                mSphinx.showView(R.id.activity_map_download);
                break;
            case R.id.app_recommend_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "appRecommend");
                if (TKConfig.sSPREADER.startsWith(TKConfig.SPREADER_TENCENT)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://a.wap.myapp.com/and2/s?aid=detail&appid=50801"));
                    mSphinx.startActivity(intent);
                } else {
                    mSphinx.showView(R.id.activity_app_recommend);
                }
                break;
            case R.id.favorite_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "favorite");
                mSphinx.showView(R.id.view_favorite);
                break;
            case R.id.history_browse_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "history");
                mSphinx.showView(R.id.view_history);
                break;
            case R.id.settings_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "setting");
                mSphinx.showView(R.id.activity_setting);
                break;
            case R.id.feedback_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "feedback");
                mSphinx.showView(R.id.activity_feedback);
                break;
            case R.id.add_merchant_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "addMerchant");
                mSphinx.showView(R.id.activity_add_merchant);
                break;
            case R.id.help_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "help");
                mSphinx.showView(R.id.activity_help);
                break;
            case R.id.about_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "aboutUs");
                mSphinx.showView(R.id.activity_about_us);
                break;
                
            case R.id.give_favourable_comment_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "giveFavourable");
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
        		/*
        		 * 下面一段代码衡量显示的昵称会否超出范围，如果超出，则显示时会截断并添加三个点
        		 * 目前可显示的范围为150个宽度，这相当于12+2/3个"@"的宽度
        		 * 截断后仍然可以显示11个"@"的宽度
        		 * 注：这不是最佳的方法，对于屏幕较窄的手机可能仍然并不奏效，但已经优于产品部于
        		 * 	   2013年03月29日(4.30版)所提出的依据字符数决定的办法，并报产品部通过
        		 * 注2：若该方法将来在N多地方使用，可能需要单独放在一个Utils里以便调用
        		 * fengtianxiao@tigerknows 2013/03/29
        		 */
        		Paint paint=new Paint();
        		String nickNameText = Globals.g_User.getNickName();
        		int strwid = (int)paint.measureText(nickNameText);
        		if(strwid > SHOW_NICKNAME_MAX_WIDTH){
        			while(strwid > SHOW_NICKNAME_MAX_WIDTH - SHOW_NICKNAME_OMIT_WIDTH){
        				nickNameText = nickNameText.substring(0 , nickNameText.length()-1);
        				strwid = (int)paint.measureText(nickNameText);
        			}
        			nickNameText = nickNameText + "...";
        		}
            	mUserNameTxv.setText(nickNameText);
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
                mMessageBtn.setText(R.string.message_tip_user_survey);
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
                            intent.putExtra(MapDownload.EXTRA_UPGRADE_ALL, true);
                        }
                        mSphinx.showView(R.id.activity_map_download, intent);
                    }
                });
    }
    
}
