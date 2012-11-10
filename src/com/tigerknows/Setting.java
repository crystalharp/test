/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.tigerknows.R;
import com.tigerknows.model.BaseQuery;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
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
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class Setting extends BaseActivity {

    private ListView mListView = null;
    
    private ArrayList<DataBean> mBeans;
    private SimpleAdapter mSettingAdatpter;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.Setting;

        setContentView(R.layout.settings);
        findViews();
        setListener();
        
        mTitleTxv.setText(R.string.settings);
        mRightTxv.setVisibility(View.GONE);

        mBeans = new ArrayList<DataBean>();
        DataBean dataBean = new DataBean(mThis.getString(R.string.settings_openg_gps), mThis.getString(R.string.settings_gps_description));
        dataBean.showIcon = true;
        mBeans.add(dataBean);
        
        dataBean = new DataBean(mThis.getString(R.string.settings_acquire_wakelock), mThis.getString(R.string.settings_acquire_wakelock_description));
        dataBean.onClickListener = new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view;
                if (mWakeLock.isHeld() == checkBox.isChecked()) {
                    return;
                }
                switchWakeLock();
                mActionLog.addAction(checkBox.isChecked() ? ActionLog.SettingAcquireWakeLockYes : ActionLog.SettingAcquireWakeLockNo);
            }
        };
        dataBean.showIcon = false;
        mBeans.add(dataBean);
        
        dataBean = new DataBean("Config", "View or Modify config.txt");
        dataBean.showIcon = false;
        mBeans.add(dataBean);
        
        dataBean = new DataBean("Clear data", "clear user data");
        dataBean.showIcon = false;
        mBeans.add(dataBean);
        
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

    protected void findViews() {
        super.findViews();
        mListView = (ListView)findViewById(R.id.listview);
    }
    
    protected void setListener() {
        super.setListener();
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                switch (arg2) {
                    case 0:
                        mActionLog.addAction(ActionLog.SettingGPS, checkGPS() ? "0" : "1");
                        Intent intent = new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
                        startActivityForResult(intent, R.id.activity_setting_location);
                        break;
                    case 1:
                        mBeans.get(1).checked = !mBeans.get(1).checked;
                        mActionLog.addAction(mBeans.get(1).checked ? ActionLog.SettingAcquireWakeLockYes : ActionLog.SettingAcquireWakeLockNo);
                        mSettingAdatpter.notifyDataSetChanged();
                        switchWakeLock();
                        break;
                    case 2:
                        Intent intent2 = new Intent(Intent.ACTION_VIEW);
                        File file = new File(TKConfig.getDataPath(true)+"config.txt");
                        if (file.exists()) {
                            Uri uri = Uri.fromFile(file);
                            intent2.setDataAndType(uri, "text/plain");
                            startActivity(intent2);
                        } else {
                            Toast.makeText(mThis, "config.ini not exists!", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case 3:
                        Class<?> iPackageDataObserverClass = null;
                        try {
                            iPackageDataObserverClass = Class.forName("android.content.pm.IPackageDataObserver");
                        } catch (ClassNotFoundException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }

                        Class<ActivityManager> activityManagerClass=ActivityManager.class;
                        ActivityManager activityManager=(ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

                        Method clearDataMethod=activityManagerClass.getMethods()[0];

                        Object iPackageDataObserverObject = Proxy.newProxyInstance(
                            Settings.class.getClassLoader(), new Class[]{iPackageDataObserverClass},
                                                new InvocationHandler() {

                                    public Object invoke(Object proxy, Method method, Object[] args)
                                            throws Throwable {
                                        return null;
                                    }
                                });

                        try {
                            clearDataMethod.invoke(activityManager, mThis.getApplicationInfo().packageName, iPackageDataObserverObject);
                        } catch (IllegalArgumentException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

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
        if (enableGps) {
            mBeans.get(0).title = mThis.getString(R.string.settings_close_gps);
        } else {
            mBeans.get(0).title = mThis.getString(R.string.settings_openg_gps);
        }
        mBeans.get(1).checked = TextUtils.isEmpty(TKConfig.getPref(mThis, TKConfig.PREFS_ACQUIRE_WAKELOCK));
        mBeans.get(2).checked = BaseQuery.Test;
        mSettingAdatpter.notifyDataSetChanged();
    }
    
    private class SimpleAdapter extends ArrayAdapter<DataBean>{
        private static final int sResource = R.layout.setting_list_item;

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
            view.setTag(data);
            
            TextView titleTxv = (TextView)view.findViewById(R.id.title_txv);
            TextView descriptionTxv = (TextView)view.findViewById(R.id.description_txv);
            ImageView iconImv = (ImageView)view.findViewById(R.id.icon_imv);
            CheckBox selectChb = (CheckBox)view.findViewById(R.id.select_chb);
            
            titleTxv.setText(data.title);
            descriptionTxv.setTextColor(mThis.getResources().getColor(R.color.description_background));  
            descriptionTxv.setText(data.description);
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

    private class DataBean {
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
