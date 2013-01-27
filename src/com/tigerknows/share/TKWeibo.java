package com.tigerknows.share;

import com.tigerknows.R;
import com.tigerknows.share.ShareAPI.LoginCallBack;
import com.weibo.net.AccessToken;
import com.weibo.net.AsyncWeiboRunner;
import com.weibo.net.DialogError;
import com.weibo.net.Utility;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboDialogListener;
import com.weibo.net.WeiboException;
import com.weibo.net.WeiboParameters;
import com.weibo.net.AsyncWeiboRunner.RequestListener;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import com.tigerknows.widget.Toast;

import java.io.IOException;

public class TKWeibo implements AsyncWeiboRunner.RequestListener {

    Activity activity;
    
    boolean showDialog;
    
    boolean finish;
    
    public TKWeibo(Activity activity, boolean showDialog, boolean finish) {
        this.activity = activity;
        this.showDialog = showDialog;
        this.finish = finish;
    }

    public static class AuthDialogListener implements WeiboDialogListener {
        
        Activity activity;
        LoginCallBack loginCallBack;
        
        public AuthDialogListener(Activity activity, LoginCallBack loginCallBack) {
            this.activity = activity;
            this.loginCallBack = loginCallBack;
        }

        @Override
        public void onComplete(Bundle values) {
            String token = values.getString("access_token");
            String expires_in = values.getString("expires_in");
            String temp = values.getString("uid");
            AccessToken accessToken = new AccessToken(token, Weibo.getAppSecret());
            accessToken.setExpiresIn(expires_in);
            Weibo.getInstance().setAccessToken(accessToken);
            String uid = temp; //getUID(activity);
            if (!TextUtils.isEmpty(uid)) {
                String name = getUserProfile(activity, uid);
                if (!TextUtils.isEmpty(name)) {
                    UserAccessIdenty userAccessIdenty = new UserAccessIdenty(name, accessToken.getToken(), Weibo.getAppSecret(), expires_in);
                    ShareAPI.writeIdentity(activity, ShareAPI.TYPE_WEIBO, userAccessIdenty);
                    loginCallBack.onSuccess();
                    return;
                }
            }
            ShareAPI.clearIdentity(activity, ShareAPI.TYPE_WEIBO);
            loginCallBack.onFailed();
        }

        @Override
        public void onError(DialogError e) {
            e.printStackTrace();
            ShareAPI.clearIdentity(activity, ShareAPI.TYPE_WEIBO);
            loginCallBack.onFailed();
        }

        @Override
        public void onCancel() {
            ShareAPI.clearIdentity(activity, ShareAPI.TYPE_WEIBO);
            loginCallBack.onCancel();
        }

        @Override
        public void onWeiboException(WeiboException e) {
            e.printStackTrace();
            ShareAPI.clearIdentity(activity, ShareAPI.TYPE_WEIBO);
            loginCallBack.onFailed();
        }

    }

    @Override
    public void onComplete(String response) {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(activity, R.string.weibo_send_sucess, Toast.LENGTH_LONG).show();
                if (showDialog) {
                    activity.dismissDialog(R.id.dialog_share_doing);
                }
                
                if (finish)
                    activity.finish();
            }
        });

    }

    @Override
    public void onIOException(IOException e) {
        e.printStackTrace();
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(activity, R.string.weibo_send_failed, Toast.LENGTH_LONG).show();
                if (showDialog)
                    activity.dismissDialog(R.id.dialog_share_doing);
            }
        });
    }

    @Override
    public void onError(final WeiboException e) {
        e.printStackTrace();
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (showDialog)
                    activity.dismissDialog(R.id.dialog_share_doing);
                int statusCode = e.getStatusCode();
                // http://open.weibo.com/wiki/Error_code
                if (statusCode == 20019 || statusCode == 20111) {
                    Toast.makeText(activity, R.string.weibo_no_repeat, Toast.LENGTH_LONG).show();
                } else {
                    if (statusCode == 21314 
                            || statusCode == 21315 
                            || statusCode == 21316 
                            || statusCode == 21317
                            || statusCode == 21318
                            || statusCode == 21319
                            || statusCode == 21327) {
                        if (finish) {
                            if (activity instanceof WeiboSend) {
                                ShareAPI.clearIdentity(activity, ShareAPI.TYPE_WEIBO);
                                WeiboSend weiboSend = (WeiboSend) activity;
                                weiboSend.getLogoutBtn().setText(R.string.back);
                                TKWeibo.login(activity, weiboSend.getSinaAuthDialogListener());
                            }
                        }
                    }
                    Toast.makeText(activity, R.string.weibo_send_failed, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    public static void login(Activity activity, WeiboDialogListener weiboDialogListener) {
        Weibo weibo = Weibo.getInstance();
        weibo.setupConsumerConfig(Weibo.getAppKey(), Weibo.getAppSecret());

        // Oauth2.0
        // 隐式授权认证方式
        weibo.setRedirectUrl("http://www.sina.com");// 此处回调页内容应该替换为与appkey对应的应用回调页
        // 对应的应用回调页可在开发者登陆新浪微博开发平台之后，
        // 进入我的应用--应用详情--应用信息--高级信息--授权设置--应用回调页进行设置和查看，
        // 应用回调页不可为空

        weibo.authorize(activity, weiboDialogListener);
    }
    
    public static String getUID(Context context){        
        String url = Weibo.SERVER + "account/get_uid.json";
        Weibo weibo = Weibo.getInstance();
        WeiboParameters bundle = new WeiboParameters();
        
        String rlt = "";
        try {
            rlt = weibo.request(context, url, bundle, Utility.HTTPMETHOD_GET, weibo.getAccessToken());
        } catch (WeiboException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        if (!TextUtils.isEmpty(rlt)) {
            try {
                JSONObject json = new JSONObject(rlt);
                return json.getString("uid");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        return null;
    }
    
    public static String getUserProfile(Context context, String uid){        
        String url = Weibo.SERVER + "users/show.json";
        Weibo weibo = Weibo.getInstance();
        WeiboParameters bundle = new WeiboParameters();
        bundle.add("uid", uid);
        
        String rlt = "";
        try {
            rlt = weibo.request(context, url, bundle, Utility.HTTPMETHOD_GET, weibo.getAccessToken());
        } catch (WeiboException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        if (!TextUtils.isEmpty(rlt)) {
            try {
                JSONObject json = new JSONObject(rlt);
                return json.getString("screen_name");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        return null;
    }
    
    public static String logout(TKWeibo tkweibo, Weibo weibo, String source, RequestListener requestListener) {
        if (tkweibo.showDialog)
            tkweibo.activity.showDialog(R.id.dialog_share_doing);
        WeiboParameters bundle = new WeiboParameters();
        bundle.add("source", source);
        String rlt = "";
        String url = Weibo.SERVER + "account/end_session.json";
        AsyncWeiboRunner weiboRunner = new AsyncWeiboRunner(weibo);
        weiboRunner.request(tkweibo.activity, url, bundle, Utility.HTTPMETHOD_POST, requestListener);
        ShareAPI.clearIdentity(tkweibo.activity, ShareAPI.TYPE_WEIBO);
        return rlt;
    }

    public static String upload(TKWeibo tkweibo, Weibo weibo, String source, String file, String status, String lon,
            String lat) {
        if (tkweibo.showDialog)
            tkweibo.activity.showDialog(R.id.dialog_share_doing);
        WeiboParameters bundle = new WeiboParameters();
        bundle.add("source", source);
        bundle.add("pic", file);
        bundle.add("status", status);
        if (!TextUtils.isEmpty(lon)) {
            bundle.add("lon", lon);
        }
        if (!TextUtils.isEmpty(lat)) {
            bundle.add("lat", lat);
        }
        String rlt = "";
        String url = Weibo.SERVER + "statuses/upload.json";
        AsyncWeiboRunner weiboRunner = new AsyncWeiboRunner(weibo);
        weiboRunner.request(tkweibo.activity, url, bundle, Utility.HTTPMETHOD_POST, tkweibo);

        return rlt;
    }

    public static String update(TKWeibo tkweibo, Weibo weibo, String source, String status, String lon, String lat) {
        if (tkweibo.showDialog)
            tkweibo.activity.showDialog(R.id.dialog_share_doing);
        WeiboParameters bundle = new WeiboParameters();
        bundle.add("source", source);
        bundle.add("status", status);
        if (!TextUtils.isEmpty(lon)) {
            bundle.add("lon", lon);
        }
        if (!TextUtils.isEmpty(lat)) {
            bundle.add("lat", lat);
        }
        String rlt = "";
        String url = Weibo.SERVER + "statuses/update.json";
        AsyncWeiboRunner weiboRunner = new AsyncWeiboRunner(weibo);
        weiboRunner.request(tkweibo.activity, url, bundle, Utility.HTTPMETHOD_POST, tkweibo);
        return rlt;
    }
}
