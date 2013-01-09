/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
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
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.BaseDialog;
import com.tigerknows.view.BaseFragment;
import com.tigerknows.view.user.UserBaseActivity;
import com.tigerknows.view.user.UserLoginActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

/**
 * @author Peng Wenyue
 */
public class BaseActivity extends Activity implements TKAsyncTask.EventListener {
    
    public static String SOURCE_VIEW_ID = "SOURCE_VIEW_ID";
    
    public static String SOURCE_USER_HOME = "SOURCE_USER_HOME";
    
    static final String TAG = "BaseActivity";
    
    protected int mSourceViewId;
    
    protected boolean mSourceUserHome = false;
    
    protected Activity mThis = null;
    
    protected LayoutInflater mLayoutInflater;
    
    protected InputMethodManager mInputMethodManager;

    protected PowerManager mPowerManager; //电源控制，比如防锁屏
    
    protected WakeLock mWakeLock;
    
    protected ActionLog mActionLog;
    
    protected String mActionTag;
    
    protected Button mTitleBtn;
    
    protected Button mLeftBtn;
    
    protected Button mRightBtn;
    
    protected Intent mIntent;
    
    protected MapEngine mMapEngine;
    
    protected Handler mHandler;
    
    protected int mId;
    
    protected boolean isReLogin = false;
    
    protected BaseQuery mBaseQuerying;
    
    protected TKAsyncTask mTkAsyncTasking;
    
    private final BroadcastReceiver mSDCardEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                try {
                    mMapEngine.initMapDataPath(mThis);
                } catch (APIException e) {
                    e.printStackTrace();
                    Toast.makeText(mThis, R.string.not_enough_space, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    };
    
    protected DialogInterface.OnClickListener mCancelLoginListener = new DialogInterface.OnClickListener() {
        
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            isReLogin();
        }
    };
    
    protected boolean isReLogin() {
        boolean isRelogin = this.isReLogin;
        this.isReLogin = false;
        if (isRelogin) {
            if (mBaseQuerying != null) {
                mBaseQuerying.setResponse(null);
                queryStart(mBaseQuerying);
            }
        }
        return isRelogin;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        mThis = this;
        mIntent = getIntent();
        getViewId(mIntent);
        mLayoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);  
        mMapEngine = MapEngine.getInstance();

        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName()); //处理屏幕防止锁屏
        mTKLocationManager = new TKLocationManager(TKApplication.getInstance());
        mLocationListener = new MyLocationListener(this, null);
        
        mHandler = new Handler();
        
        mActionLog = ActionLog.getInstance(mThis);
    }
    
    protected void findViews() {
        mTitleBtn = (Button) findViewById(R.id.title_btn);
        mLeftBtn = (Button) findViewById(R.id.left_btn);
        mRightBtn = (Button) findViewById(R.id.right_btn);
    }
    
    protected void setListener() {
        if (mLeftBtn != null) {
            mLeftBtn.setOnClickListener(new View.OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    mActionLog.addAction(ActionLog.Title_Left_Back, mActionTag);
                    finish();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            mMapEngine.initMapDataPath(this);
        } catch (Exception exception){
            exception.printStackTrace();
            Toast.makeText(this, R.string.not_enough_space, Toast.LENGTH_LONG).show();
            finish();
        }
        LogWrapper.d(TAG, "onResume()");
        if (!TextUtils.isEmpty(mActionTag)) {
            mActionLog.addAction(mActionTag);
        }
        if (mWakeLock.isHeld() == false && TextUtils.isEmpty(TKConfig.getPref(mThis, TKConfig.PREFS_ACQUIRE_WAKELOCK))) {
            mWakeLock.acquire();
        }
        
        mTKLocationManager.addLocationListener(mLocationListener);
        mTKLocationManager.prepareLocation();
        
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        registerReceiver(mSDCardEventReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        LogWrapper.d(TAG, "onPause()");
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        
        mTKLocationManager.removeUpdates();

        unregisterReceiver(mSDCardEventReceiver);
        super.onPause();
    }
    
    @Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
        if (null != mTkAsyncTasking) {
            mTkAsyncTasking.stop();
        }
	}

	@Override
    protected void onDestroy() {
        super.onDestroy();
        if (!TextUtils.isEmpty(mActionTag)) {
            mActionLog.addAction(ActionLog.Dismiss, mActionTag);
        }
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
        setIntent(newIntent);
        mIntent = getIntent();
        getViewId(mIntent);
    }
    
    protected void getViewId(Intent intent) {
        mSourceViewId = intent.getIntExtra(SOURCE_VIEW_ID, R.id.view_invalid);
        mSourceUserHome = intent.getBooleanExtra(SOURCE_USER_HOME, false);
    }
    
    protected void putViewId(Intent intent) {
        intent.putExtra(SOURCE_VIEW_ID, mId);
        intent.putExtra(SOURCE_USER_HOME, mSourceUserHome);
    }

    @Override
    public void finish() {
        hideSoftInput();
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
    public void showSoftInput(View view) {
        mInputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }
    
    public void hideSoftInput() {
        if (this.getCurrentFocus() != null) {
            hideSoftInput(this.getCurrentFocus().getWindowToken());
        }
    }
    
    public void hideSoftInput(IBinder token) {
        mInputMethodManager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);//隐藏软键盘  
    }
    // TODO: soft input end

    // TODO: query begin
    public void queryStart(BaseQuery baseQuery) {
        queryStart(baseQuery, true);
    }
    
    public void queryStart(BaseQuery baseQuery, boolean cancelable) {
        mTkAsyncTasking = new TKAsyncTask(mThis, baseQuery, BaseActivity.this, null, cancelable);
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
            DialogInterface.OnClickListener cancelOnClickListener) {
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
        final AlertDialog alertDialog = CommonUtils.showNormalDialog(activity, message);
        alertDialog.setOnDismissListener(new OnDismissListener() {
            
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
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {     
        super.onActivityResult(requestCode, resultCode, data);
    }    
    
    private TKLocationManager mTKLocationManager;
    private MyLocationListener mLocationListener;
    
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        putViewId(intent);
        super.startActivityForResult(intent, requestCode);
    }
        
    @Override
    public void startActivity(Intent intent) {
        putViewId(intent);
        super.startActivity(intent);
    }
}
