/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.more;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.R.id;
import com.tigerknows.R.layout;
import com.tigerknows.R.string;
import com.tigerknows.common.ActionLog;
import com.tigerknows.radar.AlarmInitReceiver;
import com.tigerknows.radar.Alarms;
import com.tigerknows.radar.RadarReceiver;
import com.tigerknows.service.PullService;
import com.tigerknows.ui.BaseActivity;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class SettingActivity extends BaseActivity {

    private ListView mListView = null;
    
    private ArrayList<DataBean> mBeans;
    private SimpleAdapter mSettingAdatpter;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.Setting;

        setContentView(R.layout.more_setting);
        findViews();
        setListener();
        
        mTitleBtn.setText(R.string.system_settings);
        mRightBtn.setVisibility(View.GONE);

        mBeans = new ArrayList<DataBean>();
        DataBean dataBean = null;
        dataBean = new DataBean(mThis.getString(R.string.settings_open_gps), mThis.getString(R.string.settings_gps_description_open));
        dataBean.showIcon = true;
        dataBean.type = DataBean.TYPE_GPS;
        mBeans.add(dataBean);
        
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
        
        mSettingAdatpter = new SimpleAdapter(mThis, mBeans);
        mListView.setAdapter(mSettingAdatpter);

        mSettingAdatpter.notifyDataSetChanged();
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
    
    public static boolean radarOn(Context context) {
        return TextUtils.isEmpty(TKConfig.getPref(context, TKConfig.PREFS_RADAR_PULL_SERVICE_SWITCH, ""));
    }

    protected void findViews() {
        super.findViews();
        mListView = (ListView)findViewById(R.id.listview);
    }
    
    protected void setListener() {
        super.setListener();
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int index, long arg3) {
                DataBean dataBean = mBeans.get(index);
                if (dataBean == null) {
                    return;
                }
                int type = dataBean.type;
                switch (type) {
                    case DataBean.TYPE_GPS:
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItem, ActionLog.SettingGPS, checkGPS());
                        Intent intent = new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
                        startActivityForResult(intent, R.id.activity_setting_location);
                        break;
                    case DataBean.TYPE_WAKELOCK:
                        dataBean.checked = !dataBean.checked;
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItem, ActionLog.SettingWakeLock, dataBean.checked);
                        mSettingAdatpter.notifyDataSetChanged();
                        switchWakeLock();
                        break;
                    case DataBean.TYPE_RADARPUSH:
                        dataBean.checked = !dataBean.checked;
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItem, ActionLog.SettingRadar, dataBean.checked);
                        mSettingAdatpter.notifyDataSetChanged();
                        switchRadarPush();
                        break;
                    default:
                        break;
                }
            }});
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean enableGps = checkGPS();
        DataBean dataBean = null;
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
        mSettingAdatpter.notifyDataSetChanged();
    }
    
    private class SimpleAdapter extends ArrayAdapter<DataBean>{
        private static final int sResource = R.layout.more_setting_list_item;

        public SimpleAdapter(Context context, List<DataBean> apps) {
            super(context, sResource, apps);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(sResource, parent, false);
            } else {
                view = convertView;
            }

            DataBean data = getItem(position);
            
            TextView titleTxv = (TextView)view.findViewById(R.id.title_txv);
            TextView descriptionTxv = (TextView)view.findViewById(R.id.description_txv);
            ImageView iconImv = (ImageView)view.findViewById(R.id.icon_imv);
            CheckBox selectChb = (CheckBox)view.findViewById(R.id.select_chb);
            
            titleTxv.setText(data.title);
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
                iconImv.setVisibility(View.GONE);
            }

            return view;
        }
    }
    
    private boolean checkGPS() {
        boolean enableGps = false;            
        String locationProviders = Settings.Secure.getString(mThis.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
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
        
        static final int TYPE_GPS = 1;
        static final int TYPE_WAKELOCK = 2;
        static final int TYPE_RADARPUSH = 3;
        
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
}
