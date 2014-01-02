/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.more;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.common.ImageCache;
import com.tigerknows.model.HelpQuery;
import com.tigerknows.radar.AlarmInitReceiver;
import com.tigerknows.radar.Alarms;
import com.tigerknows.service.PullService;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.common.BrowserActivity;
import com.tigerknows.util.Utility;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * @author Peng Wenyue
 */
public class SettingActivity extends BaseActivity {

    private View[] mViewList;
    private static final int NUM_OF_SETTINGS = 7;
    
    private ArrayList<DataBean> mBeans;
    
    private Context mContext;
    private Handler mHandler;
    private Dialog mProgressDialog;
    private ScrollView mBodyScv;
    
    private boolean mRefreshed;
    
    private static final int[] LIST_BACKGROUND = {R.drawable.list_header,
    	R.drawable.list_middle,
    	R.drawable.list_footer,
        R.drawable.list_header,
        R.drawable.list_footer,
    	R.drawable.list_header,
    	R.drawable.list_footer
    };

    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.Setting;

        setContentView(R.layout.more_setting);
        findViews();
        setListener();
        mContext = getBaseContext();
        mTitleBtn.setText(R.string.system_settings);
        mRightBtn.setVisibility(View.GONE);
        mRefreshed = false;
        
        mHandler = new Handler(){
        	public void handleMessage(Message msg){
        		switch (msg.what){
        		case HandlerMessage.CLEAR_CACHE_SUCCESS:
        			Toast.makeText(mContext, getString(R.string.settings_clear_cache_success), Toast.LENGTH_LONG).show();
        			break;
        		case HandlerMessage.BEFORE_CLEAR_CACHE:
        			showProgressDialog();
        			break;
        		case HandlerMessage.AFTER_CLEAR_CACHE:
        			dismissProgressDialog();
        			break;
        		default:
        		    break;
        		}
        	}
        };
        
        mBeans = new ArrayList<DataBean>();
        DataBean dataBean = null;
        
        final DataBean mapZoomBean = new DataBean(mThis.getString(R.string.map_zoom_button), "");
        mapZoomBean.onClickListener = new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view;
                mapZoomBean.checked = !mapZoomBean.checked;
                switchMapZoom();
                mActionLog.addAction(mActionTag + ActionLog.ListViewItem, ActionLog.SettingMapZoom, checkBox.isChecked());
            }
        };
        mapZoomBean.showIcon = false;
        mapZoomBean.type = DataBean.TYPE_MAP_ZOOM;
        mBeans.add(mapZoomBean);
        
        // 保持屏幕常亮
        final DataBean wakeLockBean = new DataBean(mThis.getString(R.string.settings_acquire_wakelock), mThis.getString(R.string.settings_acquire_wakelock_description));
        wakeLockBean.onClickListener = new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view;
                if (mWakeLock.isHeld() == checkBox.isChecked()) {
                    return;
                }
                wakeLockBean.checked = !wakeLockBean.checked;
                switchWakeLock();
                mActionLog.addAction(mActionTag + ActionLog.ListViewItem, ActionLog.SettingWakeLock, checkBox.isChecked());
            }
        };
        wakeLockBean.showIcon = false;
        wakeLockBean.type = DataBean.TYPE_WAKELOCK;
        mBeans.add(wakeLockBean);
        
        //雷达推送
        final DataBean radarPushBean = new DataBean(mThis.getString(R.string.settings_radar_push), mThis.getString(R.string.settings_radar_push_description));
        radarPushBean.onClickListener = new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view;
                if (radarOn(mThis) == checkBox.isChecked()) {
                    return;
                }
                radarPushBean.checked = !radarPushBean.checked; 
                switchRadarPush();
                mActionLog.addAction(mActionTag + ActionLog.ListViewItem, ActionLog.SettingRadar, checkBox.isChecked());
            }
        };
        radarPushBean.showIcon = false;
        radarPushBean.type = DataBean.TYPE_RADARPUSH;
        mBeans.add(radarPushBean);

        // 是否开启GPS
        dataBean = new DataBean(mThis.getString(R.string.settings_open_gps), mThis.getString(R.string.settings_gps_description_open));
        dataBean.showIcon = true;
        dataBean.type = DataBean.TYPE_GPS;
        mBeans.add(dataBean);
        
        // 一键清理缓存
        final DataBean clearCacheBean = new DataBean(mThis.getString(R.string.settings_clear_cache), "");

		clearCacheBean.showIcon = false;
		clearCacheBean.type = DataBean.TYPE_CLEARCACHE;
		mBeans.add(clearCacheBean);

		// 帮助
		final DataBean helpBean = new DataBean(mThis.getString(R.string.help), "");
		helpBean.showIcon = true;
		helpBean.type = DataBean.TYPE_HELP;
		mBeans.add(helpBean);
		
		// 关于我们
		final DataBean aboutBean = new DataBean(mThis.getString(R.string.about_us), "");
		aboutBean.showIcon = true;
		aboutBean.type = DataBean.TYPE_ABOUT;
		mBeans.add(aboutBean);		

        refreshDataSetChanged();
        mBodyScv.smoothScrollTo(0, 0);
    }
    
    private void switchMapZoom() {
        TKConfig.reversePref(mThis, TKConfig.PREFS_SHOW_ZOOM_BUTTON);
    }
    
    private void switchWakeLock() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        } else {
            mWakeLock.acquire();
        }
        TKConfig.setPref(mThis, TKConfig.PREFS_ACQUIRE_WAKELOCK, mWakeLock.isHeld() ? "" : "1");
    }
    
    private void switchRadarPush() {
        if (radarOn(mThis)) {
            TKConfig.setPref(mThis, TKConfig.PREFS_RADAR_PULL_SERVICE_SWITCH, "off");
            Alarms.disableAlarm(mThis, PullService.alarmAction.getIntent());
        } else {
            TKConfig.setPref(mThis, TKConfig.PREFS_RADAR_PULL_SERVICE_SWITCH, "");
            Intent pullIntent = new Intent(AlarmInitReceiver.ACTION_ALARM_INIT);
            mThis.sendBroadcast(pullIntent);
        }
        LogWrapper.d("conan", "Radar status:" + TKConfig.getPref(mThis, TKConfig.PREFS_RADAR_PULL_SERVICE_SWITCH, "on"));
    }
    
    private void showClearCacheDialog(){
		Utility.showNormalDialog(mThis, mThis.getString(R.string.settings_clear_cache_confirm),
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						if (which == DialogInterface.BUTTON_POSITIVE){
			                new Thread(new Runnable(){

								@Override
								public void run() {
									ImageCache imageCache = ImageCache.getInstance();
									Message msg[] = new Message[3];
									for (int i=0; i<msg.length; i++){
										msg[i] = new Message();
									}
									msg[0].what = HandlerMessage.BEFORE_CLEAR_CACHE;
									msg[1].what = HandlerMessage.AFTER_CLEAR_CACHE;
									msg[2].what = HandlerMessage.CLEAR_CACHE_SUCCESS;
									mHandler.sendMessage(msg[0]);
									imageCache.clearImages();
									mHandler.sendMessage(msg[1]);
									mHandler.sendMessage(msg[2]);
								}
			                	
			                }).start();
						}
					}
				});
    }
    void showProgressDialog() {
        if (mProgressDialog == null) {
            View custom = mThis.getLayoutInflater().inflate(R.layout.loading, null);
            TextView loadingTxv = (TextView)custom.findViewById(R.id.loading_txv);
            loadingTxv.setText(mThis.getString(R.string.doing_and_wait));
            mProgressDialog = Utility.showNormalDialog(mThis, custom);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        if (mProgressDialog.isShowing() == false) {
            mProgressDialog.show();
        }
        
    }    

    void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
    
    public static boolean radarOn(Context context) {
        return TextUtils.isEmpty(TKConfig.getPref(context, TKConfig.PREFS_RADAR_PULL_SERVICE_SWITCH, ""));
    }

    protected void findViews() {
        super.findViews();
        mBodyScv = (ScrollView)findViewById(R.id.body_scv);
        mViewList = new View[NUM_OF_SETTINGS];
        mViewList[0] = (View)findViewById(R.id.map_zoom_view);
        mViewList[1] = (View)findViewById(R.id.wake_view);
        mViewList[2] = (View)findViewById(R.id.radar_view);
        mViewList[3] = (View)findViewById(R.id.gps_view);
        mViewList[4] = (View)findViewById(R.id.clear_cache_view);
        mViewList[5] = (View)findViewById(R.id.help_view);
        mViewList[6] = (View)findViewById(R.id.about_view);
        
    }
    
    protected void setListener() {
        super.setListener();
        for(int i=0; i<NUM_OF_SETTINGS; i++){
        	final int j = i;
        	mViewList[i].setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					DataBean dataBean = mBeans.get(j);
					if (dataBean == null){
						return;
					}
	                int type = dataBean.type;
	                Intent intent;
	                switch (type) {
                        case DataBean.TYPE_MAP_ZOOM:
                            mActionLog.addAction(mActionTag + ActionLog.SettingMapZoom, String.valueOf(dataBean.checked));
                            refreshDataSetChanged(type-1);
                            switchMapZoom();
                            break;
	                    case DataBean.TYPE_GPS:
	                        mActionLog.addAction(mActionTag + ActionLog.SettingGPS, String.valueOf(checkGPS(mThis)));
	                        intent = new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
	                        startActivityForResult(intent, R.id.activity_setting_location);
	                        break;
	                    case DataBean.TYPE_WAKELOCK:
	                        dataBean.checked = !dataBean.checked;
	                        mActionLog.addAction(mActionTag + ActionLog.SettingWakeLock, String.valueOf(dataBean.checked));
	                        refreshDataSetChanged(type-1);
	                        switchWakeLock();
	                        break;
	                    case DataBean.TYPE_RADARPUSH:
	                        dataBean.checked = !dataBean.checked;
	                        mActionLog.addAction(mActionTag + ActionLog.SettingRadar, String.valueOf(dataBean.checked));
	                        refreshDataSetChanged(type-1);
	                        switchRadarPush();
	                        break;
	                    case DataBean.TYPE_CLEARCACHE:
	                    	mActionLog.addAction(mActionTag + ActionLog.SettingClearCache);
	                    	showClearCacheDialog();
	                    	break;
	                    case DataBean.TYPE_HELP:
	                    	mActionLog.addAction(mActionTag + ActionLog.SettingHelp);
	                    	intent = new Intent(mThis, BrowserActivity.class);
	                        intent.putExtra(BrowserActivity.TITLE, getString(R.string.help));
	                        HelpQuery baseQuery = new HelpQuery(mThis);
	                        intent.putExtra(BrowserActivity.URL, String.format(TKConfig.getHelpUrl(), TKConfig.getHelpHost()) + "?" + baseQuery.getEncodedPostParam());
	                        startActivityForResult(intent, 0);
	                    	break;
	                    case DataBean.TYPE_ABOUT:
	                    	mActionLog.addAction(mActionTag + ActionLog.SettingAboutMe);
	                    	intent = new Intent(SettingActivity.this, AboutUsActivity.class);
	                    	startActivityForResult(intent, 0);
	                    	break;
	                    default:
	                        break;
	                }
	            }
			});
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean enableGps = checkGPS(mThis);
        DataBean dataBean = null;
        dataBean = getDataBeanByType(DataBean.TYPE_MAP_ZOOM);
        if (dataBean != null) {
            dataBean.checked = TextUtils.isEmpty(TKConfig.getPref(mThis, TKConfig.PREFS_SHOW_ZOOM_BUTTON));
        }
        dataBean = getDataBeanByType(DataBean.TYPE_GPS);
        if (dataBean != null) {
            if (enableGps) {
                dataBean.title = mThis.getString(R.string.settings_close_gps);
                dataBean.description = mThis.getString(R.string.settings_gps_description_close);
            } else {
                dataBean.title = mThis.getString(R.string.settings_open_gps);
                dataBean.description = mThis.getString(R.string.settings_gps_description_open);
            }
        }
        dataBean = getDataBeanByType(DataBean.TYPE_WAKELOCK);
        if (dataBean != null) {
            dataBean.checked = TextUtils.isEmpty(TKConfig.getPref(mThis, TKConfig.PREFS_ACQUIRE_WAKELOCK));
        }
        dataBean = getDataBeanByType(DataBean.TYPE_RADARPUSH);
        if (dataBean != null) {
            dataBean.checked = radarOn(mThis);
        }
        refreshDataSetChanged();


    }
    
    private void refreshDataSetChanged(){
    	for (int i=0; i<NUM_OF_SETTINGS; i++){
    		refreshDataSetChanged(i);
    	}
        mRefreshed = true;
    }
    
    private void refreshDataSetChanged(final int position){
        DataBean data = mBeans.get(position);
        View view = mViewList[position];
        TextView titleTxv = (TextView)view.findViewById(R.id.title_txv);
        TextView descriptionTxv = (TextView)view.findViewById(R.id.description_txv);
        ImageView iconImv = (ImageView)view.findViewById(R.id.icon_imv);
        CheckBox selectChb = (CheckBox)view.findViewById(R.id.select_chb);
        view.setBackgroundDrawable(getResources().getDrawable(LIST_BACKGROUND[position]));
        titleTxv.setText(data.title);
        if(TextUtils.isEmpty(data.description)){
        	descriptionTxv.setVisibility(View.GONE);
        	// 保持Top/Bottom的Padding距离一致
        	titleTxv.setPadding(titleTxv.getPaddingLeft(), 
        			titleTxv.getPaddingTop()+ (mRefreshed ? 0 : Utility.dip2px(mThis, 8)), 
        			titleTxv.getPaddingRight(), 
        			titleTxv.getPaddingTop()+ (mRefreshed ? 0 : Utility.dip2px(mThis, 8)));
        	titleTxv.setGravity(Gravity.CENTER_VERTICAL);
        }
        descriptionTxv.setText(data.description);
        LogWrapper.d("conan", "data:" + data.title +  "checked:" + data.checked);
        if (data.onClickListener != null) {
            selectChb.setOnClickListener(data.onClickListener);
            selectChb.setChecked(data.checked);
            selectChb.setVisibility(View.VISIBLE);
        } else {
            selectChb.setVisibility(View.INVISIBLE);
        }
        if (data.showIcon) {
            iconImv.setVisibility(View.VISIBLE);
        } else {
            iconImv.setVisibility(View.INVISIBLE);
        }

    }

    
    public static boolean checkGPS(Context context) {
        boolean enableGps = false;            
        String locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (!TextUtils.isEmpty(locationProviders)) {
            enableGps = locationProviders.contains(LocationManager.GPS_PROVIDER);
        }
        return enableGps;        
    }
    
    public DataBean getDataBeanByType(int type) {
        for(int i = mBeans.size()-1; i >= 0; i--) {
            DataBean dataBean = mBeans.get(i);
            if (dataBean.type == type) {
                return dataBean;
            }
        }
        return null;
    }
    

    private class DataBean {

        static final int TYPE_MAP_ZOOM = 1;
        static final int TYPE_WAKELOCK = 2;
        static final int TYPE_RADARPUSH = 3;
        static final int TYPE_GPS = 4;
        static final int TYPE_CLEARCACHE = 5;
        static final int TYPE_HELP = 6;
        static final int TYPE_ABOUT = 7;
        
        protected int type;
        
        protected String title;

        protected String description;
        
        protected OnClickListener onClickListener;
        
        protected boolean checked = false;
        
        protected boolean showIcon = false;
        
        public DataBean(String title, String description) {
            this.title = title;
            this.description = description;
        }

    }
    
    private class HandlerMessage{
    	
    	static final int BEFORE_CLEAR_CACHE = 10;
    	static final int AFTER_CLEAR_CACHE = 11;
    	static final int CLEAR_CACHE_SUCCESS = 12;
    }
}
