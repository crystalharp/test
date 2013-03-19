package com.tigerknows.share;

import java.io.IOException;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.TKActivity;
import com.tigerknows.share.ShareAPI.LoginCallBack;
import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.WeiboAuthListener;
import com.weibo.sdk.android.WeiboDialogError;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.api.AccountAPI;
import com.weibo.sdk.android.api.StatusesAPI;
import com.weibo.sdk.android.api.UsersAPI;
import com.weibo.sdk.android.keep.AccessTokenKeeper;
import com.weibo.sdk.android.net.RequestListener;
import com.weibo.sdk.android.sso.SsoHandler;
import com.weibo.sdk.android.util.Utility;

import org.json.JSONException;
import org.json.JSONObject;
/**
 * 
 * @author liyan (liyan9@staff.sina.com.cn)
 */
public class TKWeibo implements RequestListener {
    
    static final String TAG = "TKWeiboSSO";

    public static final String CONSUMER_KEY = "725632899";// 替换为开发者的appkey，例如"1646212860";
    private static String APP_SECRET = "10d7ad791693508c0f201e543faa2488";
    public static final String REDIRECT_URL = "http://www.sina.com";
    
    public static Oauth2AccessToken accessToken; // 访问令牌
    
    // 访问方法
    static int ACCESS_METHOD_USERS_SHOW = 1;
    static int ACCESS_METHOD_STATUSES_UPDATE = 2;
    static int ACCESS_METHOD_STATUSES_UPLOAD = 3;
    static int ACCESS_METHOD_ACCOUNT_LOGOUT = 4;
    
    private int currentAccessMethod = 0; // 当前访问的哪个方法
    
    private String expires_in; // 过期时间
    private LoginCallBack loginCallBack; // 授权完成后的回调接口

    private Activity activity;
    
    private boolean showProgressDialog; // 在进行操作时是否显示进度对话框
    
    private boolean finishActivity; // 在操作完成时是否关闭当前activity
    
    public TKWeibo(Activity activity, boolean showProgressDialog, boolean finishActivity) {
        this.activity = activity;
        this.showProgressDialog = showProgressDialog;
        this.finishActivity = finishActivity;

        Weibo.getInstance(CONSUMER_KEY, REDIRECT_URL);
        accessToken = AccessTokenKeeper.readAccessToken(this.activity);
        if (accessToken.isSessionValid()) {
            Weibo.isWifi = Utility.isWifi(this.activity);
        }
    }

    public static class AuthDialogListener implements WeiboAuthListener {
        
        TKWeibo tkWeiboSSO;
        public AuthDialogListener(TKWeibo tkWeiboSSO, LoginCallBack loginCallBack) {
            this.tkWeiboSSO = tkWeiboSSO;
            this.tkWeiboSSO.loginCallBack = loginCallBack;
        }

        @Override
        public void onComplete(Bundle values) {
            String token = values.getString("access_token");
            tkWeiboSSO.expires_in = values.getString("expires_in");
            String uid = values.getString("uid");
            Log.d(TAG, "AuthDialogListener onComplete() uid="+uid);
            accessToken = new Oauth2AccessToken(token, tkWeiboSSO.expires_in);
            if (accessToken.isSessionValid()) {
//                String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
//                        .format(new java.util.Date(accessToken
//                                .getExpiresTime()));
                AccessTokenKeeper.keepAccessToken(tkWeiboSSO.activity,
                        accessToken);
//                Toast.makeText(tkWeiboSSO.activity, "认证成功: \r\n access_token: " + token + "\r\n"
//                        + "expires_in: " + tkWeiboSSO.expires_in + "\r\n有效期：" + date, Toast.LENGTH_SHORT)
//                        .show();
                
                show(tkWeiboSSO, Long.parseLong(uid));
            }
        }

        @Override
        public void onError(WeiboDialogError e) {
//            Toast.makeText(tkWeiboSSO.activity.getApplicationContext(),
//                    "Auth error : " + e.getMessage(), Toast.LENGTH_LONG).show();
            Toast.makeText(tkWeiboSSO.activity.getApplicationContext(), R.string.weibo_logon_failed_unknow, Toast.LENGTH_LONG).show();
            this.tkWeiboSSO.loginCallBack.onFailed();
        }

        @Override
        public void onCancel() {
//            Toast.makeText(tkWeiboSSO.activity.getApplicationContext(), "Auth cancel",
//                    Toast.LENGTH_LONG).show();
            this.tkWeiboSSO.loginCallBack.onFailed();
        }

        @Override
        public void onWeiboException(WeiboException e) {
//            Toast.makeText(tkWeiboSSO.activity.getApplicationContext(),
//                    "Auth exception : " + e.getMessage(), Toast.LENGTH_LONG)
//                    .show();
            Toast.makeText(tkWeiboSSO.activity.getApplicationContext(), R.string.weibo_logon_failed_unknow, Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onComplete(final String response) {
        Log.d(TAG, "onComplete() response="+response);
        activity.runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                if (showProgressDialog) {
                    activity.dismissDialog(R.id.dialog_share_doing);
                }
                
                if (currentAccessMethod == ACCESS_METHOD_USERS_SHOW) {
                    try {
                        JSONObject json = new JSONObject(response);
                        String name = json.getString("screen_name");
                        UserAccessIdenty userAccessIdenty = new UserAccessIdenty(name, accessToken.getToken(), APP_SECRET, expires_in);
                        ShareAPI.writeIdentity(activity, ShareAPI.TYPE_WEIBO, userAccessIdenty);
                        if (loginCallBack != null) {
                            loginCallBack.onSuccess();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        ShareAPI.clearIdentity(activity, ShareAPI.TYPE_WEIBO);
                        if (loginCallBack != null) {
                            loginCallBack.onFailed();
                        }
                    }
                } else if (currentAccessMethod == ACCESS_METHOD_ACCOUNT_LOGOUT) {
                    Toast.makeText(activity, R.string.logout_sucess, Toast.LENGTH_LONG).show();
                    if (finishActivity) {
                        activity.finish();
                    }
                } else if (currentAccessMethod == ACCESS_METHOD_STATUSES_UPDATE
                        || currentAccessMethod == ACCESS_METHOD_STATUSES_UPLOAD) {
                    Toast.makeText(activity, R.string.weibo_send_sucess, Toast.LENGTH_LONG).show();
                    if (finishActivity) {
                        activity.finish();
                    }
                }
                
            }
        });
    }

    @Override
    public void onError(final WeiboException e) {
        e.printStackTrace();
        activity.runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                if (showProgressDialog) {
                    activity.dismissDialog(R.id.dialog_share_doing);
                }
                
                if (currentAccessMethod == ACCESS_METHOD_USERS_SHOW) {
                    ShareAPI.clearIdentity(activity, ShareAPI.TYPE_WEIBO);
                    if (loginCallBack != null) {
                        loginCallBack.onFailed();
                    }
                } else if (currentAccessMethod == ACCESS_METHOD_ACCOUNT_LOGOUT) {

                } else if (currentAccessMethod == ACCESS_METHOD_STATUSES_UPDATE
                        || currentAccessMethod == ACCESS_METHOD_STATUSES_UPLOAD) {
                    String msg = e.getMessage();
                    LogWrapper.d(TAG, "onError() statusCode,message=" + e.getStatusCode()+","+msg);
                    // 详见 http://open.weibo.com/wiki/Error_code
                    if (msg.contains("20017")  || msg.contains("20019") || msg.contains("20111")) {
                        Toast.makeText(activity, R.string.weibo_no_repeat, Toast.LENGTH_LONG).show();
                    } else {
                        if (msg.contains("21301")
                                || msg.contains("21314")
                                || msg.contains("21315")
                                || msg.contains("21316")
                                || msg.contains("21317")
                                || msg.contains("21318")
                                || msg.contains("21319")
                                || msg.contains("21327")) {
                            if (finishActivity) {
                                if (activity instanceof WeiboSend) {
                                    ShareAPI.clearIdentity(activity, ShareAPI.TYPE_WEIBO);
                                    WeiboSend weiboSend = (WeiboSend) activity;
                                    weiboSend.getLogoutBtn().setText(R.string.back);
                                    authorize(TKWeibo.this, weiboSend.getSinaAuthDialogListener());
                                }
                            }
                        }
                        Toast.makeText(activity, R.string.weibo_send_failed, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    @Override
    public void onIOException(IOException e) {
        e.printStackTrace();
        activity.runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                if (showProgressDialog) {
                    activity.dismissDialog(R.id.dialog_share_doing);
                }
                
                if (currentAccessMethod == ACCESS_METHOD_USERS_SHOW) {
                    ShareAPI.clearIdentity(activity, ShareAPI.TYPE_WEIBO);
                    if (loginCallBack != null) {
                        loginCallBack.onFailed();
                    }
                } else if (currentAccessMethod == ACCESS_METHOD_ACCOUNT_LOGOUT) {

                } else if (currentAccessMethod == ACCESS_METHOD_STATUSES_UPDATE
                        || currentAccessMethod == ACCESS_METHOD_STATUSES_UPLOAD) {
                    Toast.makeText(activity, R.string.weibo_send_failed, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    public static void authorize(TKWeibo tkweibo, WeiboAuthListener weiboAuthListener) {
        Weibo weibo = Weibo.getInstance(CONSUMER_KEY, REDIRECT_URL);
        SsoHandler ssoHandler = new SsoHandler(tkweibo.activity, weibo);
        ((TKActivity) tkweibo.activity).mSsoHandler = ssoHandler;
        ssoHandler.authorize(weiboAuthListener);
    }
    
    public static void show(TKWeibo tkweibo, long uid) {
        if (tkweibo.showProgressDialog) {
            ActionLog.getInstance(tkweibo.activity).addAction(ActionLog.DIALOG, tkweibo.activity.getString(R.string.doing_and_wait));
            postShowDialog(tkweibo.activity);
        }
        
        tkweibo.currentAccessMethod = ACCESS_METHOD_USERS_SHOW;
        UsersAPI usersAPI = new UsersAPI(accessToken);
        usersAPI.show(uid, tkweibo);
    }
    
    public static void logout(TKWeibo tkweibo) {
        tkweibo.showProgressDialog = false;
        tkweibo.currentAccessMethod = ACCESS_METHOD_ACCOUNT_LOGOUT;
        AccountAPI accountAPI = new AccountAPI(accessToken);
        accountAPI.endSession(tkweibo);
        ShareAPI.clearIdentity(tkweibo.activity, ShareAPI.TYPE_WEIBO);
    }

    public static void upload(TKWeibo tkweibo, String status, String file, String lon, String lat) {
        if (tkweibo.showProgressDialog) {
            ActionLog.getInstance(tkweibo.activity).addAction(ActionLog.DIALOG, tkweibo.activity.getString(R.string.doing_and_wait));
            postShowDialog(tkweibo.activity);
        }

        tkweibo.currentAccessMethod = ACCESS_METHOD_STATUSES_UPLOAD;
        StatusesAPI statusesAPI = new StatusesAPI(accessToken);
        statusesAPI.upload(status, file, lat, lon, tkweibo);
    }

    public static void update(TKWeibo tkweibo, String status, String lon, String lat) {
        if (tkweibo.showProgressDialog) {
            ActionLog.getInstance(tkweibo.activity).addAction(ActionLog.DIALOG, tkweibo.activity.getString(R.string.doing_and_wait));
            postShowDialog(tkweibo.activity);
        }

        tkweibo.currentAccessMethod = ACCESS_METHOD_STATUSES_UPDATE;
        StatusesAPI statusesAPI = new StatusesAPI(accessToken);
        statusesAPI.update(status, lat, lon, tkweibo);
    }
    
    static void postShowDialog(final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                activity.showDialog(R.id.dialog_share_doing);
            }
        });
    }
}
