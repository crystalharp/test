/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.android.app;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.map.MapActivity;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.TKLocationManager;
import com.tigerknows.android.location.TKLocationListener;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.android.view.inputmethod.TKInputMethodManager;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.Response;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.service.MapDownloadService;
import com.tigerknows.service.MapStatsService;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.user.UserBaseActivity;
import com.tigerknows.ui.user.UserLoginActivity;
import com.tigerknows.util.Utility;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import com.weibo.sdk.android.sso.SsoHandler;

/**
 * 应用程序中所有的Acitvity都继承此类
 * 在onResume()中注册定位监听器（系统的gps定位或无线网络定位、Tigerknows的网络定位）
 * 在onPause()中撤消对定位的监听
 * 
 * @author Peng Wenyue
 */
public class TKActivity extends MapActivity implements TKAsyncTask.EventListener {
    
    static final String TAG = "TKActivity";
    
    protected Activity mThis = null;
    
    protected LayoutInflater mLayoutInflater;
    
    protected TKInputMethodManager mSoftInputManager;

    /**
     * 电源控制
     */
    protected PowerManager mPowerManager;
    
    /**
     * 锁屏控制
     */
    protected WakeLock mWakeLock;
    
    protected ActionLog mActionLog;
    
    protected MapEngine mMapEngine;
    
    protected Handler mHandler;
    
    /**
     * 当前的查询
     */
    protected List<BaseQuery> mBaseQuerying;
    
    /**
     * 执行当前查询的AsyncTask
     */
    protected TKAsyncTask mTkAsyncTasking;    
    
    protected TKLocationManager mTKLocationManager;
    
    public MyLocationListener mLocationListener;
    
    /**
     * 定位改变事件的监听器
     * 更新Globals的最近定位信息、定位城市及UI
     * 
     * @author pengwenyue
     *
     */
    public static class MyLocationListener implements TKLocationListener {
        
        private Activity activity;
        
        /**
         * 定位改变后执行的Runnable(Ui线程执行)
         */
        private Runnable runnable;
        public MyLocationListener(Activity activity, Runnable runnable) {
            this.activity = activity;
            this.runnable = runnable;
        }

        @Override
        public void onLocationChanged(final Location location) {  
            if (location != null) {
                Position myLocationPosition = new Position(location.getLatitude(), location.getLongitude());
                MapEngine mapEngine = MapEngine.getInstance();
                
                myLocationPosition = mapEngine.latlonTransform(myLocationPosition);
                if (myLocationPosition == null) {
                    Globals.g_My_Location = null;
                    Globals.g_My_Location_City_Info = null;
                } else {
                    myLocationPosition.setAccuracy(location.getAccuracy());
                    myLocationPosition.setProvider(location.getProvider());
                    int cityId = mapEngine.getCityId(myLocationPosition);
                    CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
                    
                    // 判断最新的定位是否在定位城市范围内，否则更新定位城市
                    if (myLocationCityInfo != null && myLocationCityInfo.getId() == cityId) {
                        myLocationCityInfo.setPosition(myLocationPosition);
                        Globals.g_My_Location = location;
                    } else {
                        CityInfo cityInfo = mapEngine.getCityInfo(cityId);
                        cityInfo.setPosition(myLocationPosition);
                        if (cityInfo.isAvailably()) {
                            Globals.g_My_Location = location;
                            Globals.g_My_Location_City_Info = cityInfo;
                        } else {
                            Globals.g_My_Location = null;
                            Globals.g_My_Location_City_Info = null;
                        }
                    }
                }
            } else {
                Globals.g_My_Location = null;
                Globals.g_My_Location_City_Info = null;
            } 
            
            // 如果是打开应用软件第一次定到位，则需要通过用户反馈服务通知服务器进行记录，目的是为统计定位成功率？
            if (Globals.g_My_Location_State == Globals.LOCATION_STATE_NONE && Globals.g_My_Location_City_Info != null) {
                ActionLog.getInstance(activity).addAction(ActionLog.LifecycleFirstLocationSuccess, Globals.g_My_Location_City_Info.getCName());
                final FeedbackUpload feedbackUpload = new FeedbackUpload(activity);
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                feedbackUpload.setup(criteria);
                new Thread(new Runnable() {
                    
                    @Override
                    public void run() {
                        feedbackUpload.query();
                    }
                }).start();
                Globals.g_My_Location_State = Globals.LOCATION_STATE_FIRST_SUCCESS;
            }
            
            if (this.activity != null && this.runnable != null) {
                this.activity.runOnUiThread(runnable);
            }
        }
    }

    /**
     * 是否开启飞行模式
     */
    private boolean mAirPlaneModeOn = false;
    
    /**
     * 开启飞机模式的广播接收器
     */
    private BroadcastReceiver mAirPlaneModeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean airPlaneModeOn = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
            
            // 飞机模式状态与当前不同，则更新mcc,mnc,imsi,imei==
            if(airPlaneModeOn != mAirPlaneModeOn){
                TKConfig.getTelephonyInfo(mThis);
                mAirPlaneModeOn = airPlaneModeOn;
            }
            if (mAirPlaneModeOn) {
                if (isFinishing() == false) {
                    Toast.makeText(mThis, R.string.network_error, Toast.LENGTH_LONG).show();
                }
            }
        }
    };
    
    /**
     * 网络出错的广播接收器
     */
    private final BroadcastReceiver mNetworkStatusReportReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BaseQuery.ACTION_NETWORK_STATUS_REPORT)) {
                if (isFinishing() == false) {
                    Toast.makeText(mThis, R.string.network_error, Toast.LENGTH_LONG).show();
                }
            }
        }
    };
    
    /**
     * 扩展存储卡挂载广播接收器
     */
    BroadcastReceiver mExternalStorageMountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                onMediaChanged();
            }
        }
    };
    
    /**
     * 存储地图数据文件的路径发生变化时，需要停止地图统计服务和地图下载服务，并重新初始化地图引擎
     */
    protected void onMediaChanged() {
        Intent service = new Intent(mThis, MapStatsService.class);
        stopService(service);
        service = new Intent(mThis, MapDownloadService.class);
        stopService(service);
        
        try {
            mMapEngine.initMapDataPath(getApplicationContext());
        } catch (APIException e) {
            e.printStackTrace();
            Utility.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
            finish();
            return;
        }
        
    }

    protected ConnectivityManager mConnectivityManager;
    
    /**
     * 数据网络改变广播接收器
     * @author pengwenyue
     *
     */
    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            TKConfig.updateIMSI(mConnectivityManager);
        }
    };
    private ConnectivityBroadcastReceiver mConnectivityReceiver = new ConnectivityBroadcastReceiver();
    
    /**
     * SsoHandler 仅当sdk支持sso时有效，
     */
    public SsoHandler mSsoHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        mThis = this;
        mLayoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mSoftInputManager = new TKInputMethodManager(mThis);  
        mMapEngine = MapEngine.getInstance();

        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName()); //处理屏幕防止锁屏
        mTKLocationManager = new TKLocationManager(TKApplication.getInstance());
        
        mActionLog = ActionLog.getInstance(mThis);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            mMapEngine.initMapDataPath(getApplicationContext());
        } catch (Exception exception){
            exception.printStackTrace();
            Utility.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
            finish();
            return;
        }
        LogWrapper.d(TAG, "onResume()");
        if (mWakeLock.isHeld() == false && TextUtils.isEmpty(TKConfig.getPref(mThis, TKConfig.PREFS_ACQUIRE_WAKELOCK))) {
            mWakeLock.acquire();
        }
        
        mTKLocationManager.addLocationListener(mLocationListener);
        mTKLocationManager.prepareLocation();
        
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        registerReceiver(mExternalStorageMountReceiver, intentFilter);
        
        intentFilter = new IntentFilter(BaseQuery.ACTION_NETWORK_STATUS_REPORT);
        registerReceiver(mNetworkStatusReportReceiver, intentFilter);
        
        intentFilter = new IntentFilter("android.intent.action.SERVICE_STATE"); // "android.intent.action.SERVICE_STATE" Intent.ACTION_AIRPLANE_MODE_CHANGED Intent.ACTION_SERVICE_STATE_CHANGED
        registerReceiver(mAirPlaneModeReceiver, intentFilter);

        intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectivityReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        LogWrapper.d(TAG, "onPause()");
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        
        mTKLocationManager.removeUpdates();

        unregisterReceiver(mExternalStorageMountReceiver);
        unregisterReceiver(mNetworkStatusReportReceiver);
        unregisterReceiver(mAirPlaneModeReceiver);
        unregisterReceiver(mConnectivityReceiver);
        
        super.onPause();
    }
    
    @Override
	protected void onStop() {
		super.onStop();
	}

	@Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {                
            case KeyEvent.KEYCODE_BACK:
                mActionLog.addAction(ActionLog.KeyCodeBack);
                break;
                
            case KeyEvent.KEYCODE_SEARCH:
                return true;            
                
            case KeyEvent.KEYCODE_MENU:
                BaseQueryTest.showSetResponseCode(mLayoutInflater, this);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    protected void onNewIntent(Intent newIntent) {
        // TODO Auto-generated method stub
        super.onNewIntent(newIntent);
    }
    
    @Override
    public void finish() {
        super.finish();
        LogWrapper.d(TAG, "finish()");
    }

    @Override
    //为了防止万一程序被销毁的风险，这个方法可以保证重要数据的正确性
    //不写这个方法并不意味着一定出错，但是一旦遇到了一些非常奇怪的数据问题的时候
    //可以看看是不是由于某些重要的数据没有保存，在程序被销毁时被重置
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
      super.onSaveInstanceState(savedInstanceState);
      LogWrapper.d(TAG, "onSaveInstanceState()");

    }  

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      LogWrapper.d(TAG, "onRestoreInstanceState()");

    }
    
    // TODO: soft input begin
    public void showSoftInput() {
        mSoftInputManager.showSoftInput();
    }
    
    public void showSoftInput(View view) {
        mSoftInputManager.showSoftInput(view);
    }
    
    public void hideSoftInput() {
        mSoftInputManager.hideSoftInput();
    }
    
    public void hideSoftInput(View view) {
        mSoftInputManager.hideSoftInput(view);
    }
    
    public void postHideSoftInput() {
        mSoftInputManager.postHideSoftInput();
    }
    
    public void postHideSoftInput(View view) {
        mSoftInputManager.postHideSoftInput(view);
    }
    // TODO: soft input end

    // TODO: query begin
    public TKAsyncTask queryStart(List<BaseQuery> baseQuerying) {
        return queryStart(baseQuerying, true);
    }
    
    public TKAsyncTask queryStart(List<BaseQuery> baseQuerying, boolean cancelable) {
    	if (baseQuerying == null || baseQuerying.size() <= 0) {
    		return null;
    	}
    	mBaseQuerying = baseQuerying;
        mTkAsyncTasking = new TKAsyncTask(mThis, baseQuerying, this, null, cancelable);
        mTkAsyncTasking.execute();
        return mTkAsyncTasking;
    }
    
    public TKAsyncTask queryStart(BaseQuery baseQuery) {
        return queryStart(baseQuery, true);
    }
    
    public TKAsyncTask queryStart(BaseQuery baseQuery, boolean cancelable) {
        List<BaseQuery> baseQuerying = new ArrayList<BaseQuery>();
        baseQuerying.add(baseQuery);
        return queryStart(baseQuerying, cancelable);
    }

    public void onCancelled(TKAsyncTask tkAsyncTask) {
        mTkAsyncTasking = null;
    }

    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        mTkAsyncTasking = null;
    }
    
    public static boolean checkReLogin(BaseQuery baseQuery, final Activity activity, boolean sourceUserHome,
            final int sourceViewIdLogin, final int targetViewIdLoginSuccess, final int targetViewIdLoginFailed,
            final DialogInterface.OnClickListener cancelOnClickListener) {
        final Response response = baseQuery.getResponse();
        if (response != null) {
            int responseCode = response.getResponseCode();
            int resId = R.id.app_name;
            if (responseCode == Response.RESPONSE_CODE_SESSION_INVALID) {
                resId = R.string.response_code_300;
            } else if (responseCode == Response.RESPONSE_CODE_LOGON_EXIST) {
                resId = R.string.response_code_301;
            }
            
            if (resId != R.id.app_name
                    && activity.isFinishing() == false
                    && Globals.g_Session_Id != null
                    && Globals.g_User != null) {
                Globals.clearSessionAndUser(activity);
                Dialog dialog;
                if (sourceUserHome == false) {
                    dialog = Utility.showNormalDialog(activity, 
                            activity.getString(R.string.prompt), 
                            activity.getString(resId),
                            activity.getString(R.string.relogin),
                            activity.getString(R.string.nologin),
                            new DialogInterface.OnClickListener() {
                                                    
                                @Override
                                public void onClick(DialogInterface arg0, int id) {
                                    if (id == DialogInterface.BUTTON_POSITIVE) {
                                        Intent intent = new Intent(activity, UserLoginActivity.class);
                                        intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, sourceViewIdLogin);
                                        intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, targetViewIdLoginSuccess);
                                        intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, targetViewIdLoginFailed);
                                        activity.startActivityForResult(intent, R.id.activity_user_login);
                                	} else {
                            		    if (cancelOnClickListener != null) {
                            		    	cancelOnClickListener.onClick(arg0, id);
                                		}
                                	}
                                }
                            });
                } else {
                    dialog = Utility.showNormalDialog(activity, 
                            activity.getString(R.string.prompt), 
                            activity.getString(resId),
                            activity.getString(R.string.confirm),
                            null,
                            new DialogInterface.OnClickListener() {
                                                    
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    Intent intent = new Intent(activity, UserLoginActivity.class);
                                    intent.putExtra(BaseActivity.SOURCE_USER_HOME, true);
                                    intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, sourceViewIdLogin);
                                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, R.id.view_user_home);
                                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, R.id.view_more_home);
                                    activity.startActivityForResult(intent, R.id.activity_user_login);
                                }
                            });
                }
                dialog.setOnCancelListener(new OnCancelListener() {
                    
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (cancelOnClickListener != null) {
                            cancelOnClickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
                        } else {
                            Intent intent = new Intent(activity, Sphinx.class);
                            intent.putExtra(BaseActivity.SOURCE_USER_HOME, true);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            activity.startActivity(intent);
                        }
                    }
                });
            }
            return resId != R.id.app_name;
        }
        
        return false;
    }
    
    public static boolean checkResponseCode(final BaseQuery baseQuery, final Activity activity, final Object sourceView) {
        return checkResponseCode(baseQuery, activity, null, true, sourceView, true);
    }
    
    public static final int SHOW_ERROR_MSG_NO = 0;
    public static final int SHOW_ERROR_MSG_DIALOG = 1;
    public static final int SHOW_ERROR_MSG_TOAST = 2;
    
    /**
     * Check the response code and show a error msg if an error happens
     * @param baseQuery
     * @param activity
     * @param filterResponseCodes
     * @param showErrorDialog whether to shou a dialog when an error happens
     * @param sourceView
     * @param exit
     * @return
     */
    public static boolean checkResponseCode(final BaseQuery baseQuery, final Activity activity, int[] filterResponseCodes, boolean showErrorDialog, final Object sourceView, boolean exit) {
    	if(showErrorDialog){
    		return checkResponseCode(baseQuery, activity, filterResponseCodes, SHOW_ERROR_MSG_DIALOG, sourceView, exit);
    	}else{
    		return checkResponseCode(baseQuery, activity, filterResponseCodes, SHOW_ERROR_MSG_NO, sourceView, exit);
    	}
    }
    
    /**
     * Check the response code and show a error msg if an error happens
     * @param baseQuery
     * @param activity
     * @param filterResponseCodes
     * @param showErrorType	the type of presentation of error msg
     * @param sourceView
     * @param exit
     * @return
     */
    public static boolean checkResponseCode(final BaseQuery baseQuery, final Activity activity, int[] filterResponseCodes, int showErrorType, final Object sourceView, boolean exit) {
        boolean result = true;
        if (baseQuery == null || activity == null || sourceView == null) {
            return result;
        }
        final Response response = baseQuery.getResponse();
        boolean filter = false;
        int resId = R.string.network_failed;
        if (response != null) {
            int responseCode = response.getResponseCode();
            if (filterResponseCodes != null) {
                for(int i = filterResponseCodes.length-1; i >= 0; i--) {
                    if (filterResponseCodes[i] == responseCode) {
                        filter = true;
                        break;
                    }
                }
            }
            resId = getResponseResId(baseQuery);
        }
        
        result = (resId != R.string.response_code_200);
        if (result && (filter == false)) {
        	if(showErrorType == SHOW_ERROR_MSG_DIALOG){
        		showErrorDialog(activity, activity.getString(resId), sourceView, exit);
        	}else if(showErrorType == SHOW_ERROR_MSG_TOAST){
        		showErrorToast(activity, activity.getString(resId), sourceView, exit);
        	}
        }
        return result;
    }
    
    public static int getResponseResId(BaseQuery baseQuery) {

        int resId;
        Response response = baseQuery.getResponse();
        if (response != null) {
            int responseCode = response.getResponseCode();
            String responseString = baseQuery.getCriteria().get(BaseQuery.RESPONSE_CODE_ERROR_MSG_PREFIX + (responseCode));
            if(responseString!=null){
            	return Integer.parseInt(responseString);
            }

            switch (responseCode) {
                case 200:
                    resId = R.string.response_code_200;
                    break;
                case 201:
                    resId = R.string.response_code_201;
                    break;
                case 202:
                case 203:
                    resId = R.string.response_code_202;
                    break;
                case 300:
                    resId = R.string.response_code_300;
                    break;
                case 301:
                    resId = R.string.response_code_301;
                    break;
                case 400:
                    resId = R.string.response_code_400;
                    break;
                case 401:
                    resId = R.string.response_code_401;
                    break;
                case 402:
                    resId = R.string.response_code_402;
                    break;
                case 403:
                    if (baseQuery.getCriteria().get(BaseQuery.SERVER_PARAMETER_OPERATION_CODE).equals(DataOperation.OPERATION_CODE_CREATE)) {
                        resId = R.string.response_code_403_login;
                    } else {
                        resId = R.string.response_code_403_resetpassword;
                    }
                    break;
                case 404:
                    resId = R.string.response_code_404;
                    break;
                case 405:
                    resId = R.string.response_code_405;
                    break;
                case 406:
                    resId = R.string.response_code_406;
                    break;
                case 410:
                    resId = R.string.response_code_410;
                    break;
                case 503:
                    resId = R.string.response_code_503;
                    break;
                case 602:
                    resId = R.string.response_code_602;
                    break;
                case 603:
                    resId = R.string.response_code_603;
                    break;
                case 801:
                case 802:
                    resId = R.string.response_code_801;
                    break;
                default:
                    resId = R.string.response_code_407;
                    break;
            }
        } else {
            resId = R.string.network_failed;
        }
        
        return resId;
    }
    public static void showErrorToast(final Activity activity, String message, final Object sourceView, final boolean exit) {
        if (activity == null || TextUtils.isEmpty(message) || sourceView == null) {
            return;
        }
        if (sourceView instanceof BaseFragment && ((BaseFragment) sourceView).isShowing() == false) {
            return;
        } else if (sourceView instanceof Activity && ((Activity) sourceView).isFinishing()) {
            return;
        } else if (sourceView instanceof Dialog && ((Dialog) sourceView).isShowing() == false) {
            return;
        }
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        if (exit == false) {
        	return;
        }
        if (sourceView instanceof BaseFragment) {
        	((BaseFragment) sourceView).dismiss();
        } else if (sourceView instanceof Activity) {
        	((Activity) sourceView).finish();
        } else if (sourceView instanceof Dialog) {
        	((Dialog) sourceView).dismiss();
        }
        
    }
    
    public static void showErrorDialog(final Activity activity, String message, final Object sourceView, final boolean exit) {
        if (activity == null || TextUtils.isEmpty(message) || sourceView == null) {
            return;
        }
        if (sourceView instanceof BaseFragment && ((BaseFragment) sourceView).isShowing() == false) {
            return;
        } else if (sourceView instanceof Activity && ((Activity) sourceView).isFinishing()) {
            return;
        } else if (sourceView instanceof Dialog && ((Dialog) sourceView).isShowing() == false) {
            return;
        }
        final Dialog dialog = Utility.showNormalDialog(activity, message);
        dialog.setOnDismissListener(new OnDismissListener() {
            
            @Override
            public void onDismiss(DialogInterface arg0) {
                ActionLog.getInstance(activity).addAction(ActionLog.Dialog + ActionLog.Dismiss);
                if (exit == false) {
                    return;
                }
                if (sourceView instanceof BaseFragment) {
                    ((BaseFragment) sourceView).dismiss();
                } else if (sourceView instanceof Activity) {
                    ((Activity) sourceView).finish();
                } else if (sourceView instanceof Dialog) {
                    ((Dialog) sourceView).dismiss();
                }
            }
        });
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {     
        super.onActivityResult(requestCode, resultCode, data);

        /**
         * 下面两个注释掉的代码，仅当sdk支持sso时有效，
         */
        if (mSsoHandler != null) {
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }
    }    
    
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
    }
        
    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }
}
