/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.map.MapActivity;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx.MyLocationListener;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.Response;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.service.TKLocationManager;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.SoftInputManager;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.BaseDialog;
import com.tigerknows.view.BaseFragment;
import com.tigerknows.view.user.UserBaseActivity;
import com.tigerknows.view.user.UserLoginActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnDismissListener;
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
import com.tigerknows.widget.Toast;

/**
 * @author Peng Wenyue
 */
public class TKActivity extends MapActivity implements TKAsyncTask.EventListener {
    
    static final String TAG = "TKActivity";
    
    protected Activity mThis = null;
    
    protected LayoutInflater mLayoutInflater;
    
    protected SoftInputManager mSoftInputManager;

    protected PowerManager mPowerManager; //电源控制，比如防锁屏
    
    protected WakeLock mWakeLock;
    
    protected ActionLog mActionLog;
    
    protected MapEngine mMapEngine;
    
    protected Handler mHandler;
    
    protected BaseQuery mBaseQuerying;
    
    protected TKAsyncTask mTkAsyncTasking;    
    
    protected TKLocationManager mTKLocationManager;
    
    public MyLocationListener mLocationListener;

    private boolean mAirPlaneModeOn = false;
    private BroadcastReceiver mAirPlaneModeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean airPlaneModeOn = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
            if(airPlaneModeOn != mAirPlaneModeOn){
                TKConfig.init(mThis);
                mAirPlaneModeOn = airPlaneModeOn;
            }
            if (mAirPlaneModeOn) {
                if (hasWindowFocus()) {
                    Toast.makeText(mThis, R.string.network_error, Toast.LENGTH_LONG).show();
                }
            }
        }
    };
    
    private final BroadcastReceiver mNetworkStatusReportReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BaseQuery.ACTION_NETWORK_STATUS_REPORT)) {
                if (hasWindowFocus()) {
                    Toast.makeText(mThis, R.string.network_error, Toast.LENGTH_LONG).show();
                }
            }
        }
    };
    
    private final BroadcastReceiver mSDCardEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                try {
                    mMapEngine.initMapDataPath(mThis, true);
                } catch (APIException e) {
                    e.printStackTrace();
                    CommonUtils.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
                    finish();
                }
            }
        }
    };

    protected ConnectivityManager mConnectivityManager;
    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            TKConfig.updateIMSI(mConnectivityManager);
        }
    };
    private ConnectivityBroadcastReceiver mConnectivityReceiver = new ConnectivityBroadcastReceiver();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        mThis = this;
        mLayoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mSoftInputManager = new SoftInputManager(mThis);  
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
            mMapEngine.initMapDataPath(this, true);
        } catch (Exception exception){
            exception.printStackTrace();
            CommonUtils.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
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
        registerReceiver(mSDCardEventReceiver, intentFilter);
        
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

        unregisterReceiver(mSDCardEventReceiver);
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
    public void queryStart(BaseQuery baseQuery) {
        queryStart(baseQuery, true);
    }
    
    public void queryStart(BaseQuery baseQuery, boolean cancelable) {
        mTkAsyncTasking = new TKAsyncTask(mThis, baseQuery, TKActivity.this, null, cancelable);
        mTkAsyncTasking.execute();
        mBaseQuerying = baseQuery;
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
            
            if (resId != R.id.app_name && activity.isFinishing() == false) {
                Globals.clearSessionAndUser(activity);
                if (sourceUserHome == false) {
                    CommonUtils.showNormalDialog(activity, 
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
                    CommonUtils.showNormalDialog(activity, 
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
                                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, R.id.view_more);
                                    activity.startActivityForResult(intent, R.id.activity_user_login);
                                }
                            });
                }
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean checkResponseCode(final BaseQuery baseQuery, final Activity activity, final Object sourceView) {
        return checkResponseCode(baseQuery, activity, null, true, sourceView, true);
    }
    
    public static boolean checkResponseCode(final BaseQuery baseQuery, final Activity activity, int[] filterResponseCodes, boolean showErrorDialog, final Object sourceView, boolean exit) {
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
        if (result && (filter == false) && showErrorDialog) {
            showErrorDialog(activity, activity.getString(resId), sourceView, exit);
        }
        return result;
    }
    
    public static int getResponseResId(BaseQuery baseQuery) {

        int resId;
        Response response = baseQuery.getResponse();
        if (response != null) {
            int responseCode = response.getResponseCode(); 

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
        final Dialog dialog = CommonUtils.showNormalDialog(activity, message);
        dialog.setOnDismissListener(new OnDismissListener() {
            
            @Override
            public void onDismiss(DialogInterface arg0) {
                if (exit == false) {
                    return;
                }
                if (sourceView instanceof BaseFragment) {
                    ((BaseFragment) sourceView).dismiss();
                } else if (sourceView instanceof BaseDialog) {
                    ((BaseDialog) sourceView).dismiss();
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
