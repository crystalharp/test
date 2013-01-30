package com.tigerknows.share;

import com.tencent.tauth.TAuthView;
import com.tencent.tauth.TencentOpenAPI;
import com.tencent.tauth.bean.OpenId;
import com.tencent.tauth.bean.UserInfo;
import com.tencent.tauth.http.Callback;
import com.tencent.tauth.http.TDebug;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.share.ShareAPI.LoginCallBack;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.tigerknows.widget.Toast;

public class TKTencentOpenAPI {
    
    public static final String AUTH_BROADCAST = "com.tigerknows.tencent.auth.BROWSER";

    /**
     * <p>不能包含特殊字符“#”</br>
     * 不能是浏览器能识别的协议，比如：http://auth.qq.com</br>
     * 不设置时默认使用： auth://tauth.qq.com/</br></br>
     * 不区分大小写。在Manifest设置的scheme必须是小写</br>
     * 在Manifest中设置intent-filter：data android:scheme="auth"</p>
     * 例如：</br>
     *  &ltintent-filter>  </br>
     *      &ltaction android:name="android.intent.action.VIEW" />  </br>
     *      &ltcategory android:name="android.intent.category.DEFAULT" />  </br>
     *      &ltcategory android:name="android.intent.category.BROWSABLE" />  </br>
     *      &ltdata android:scheme="auth"/>   </br>
     *  &lt/intent-filter>  </br>
     *
     */
    private static final String CALLBACK = "tencentauth://www.tigerknows.com/";
    
    public static String mAppid = "100271659";//申请时分配的appid
    
    private static String scope = "get_user_info,add_share";//授权范围
    
    public static String mAccessToken, mOpenId;

    /**
     * 广播的侦听，授权完成后的回调是以广播的形式将结果返回
     * 
     * @author John.Meng<arzen1013@gmail> QQ:3440895
     * @date 2011-9-5
     */
    public static class AuthReceiver extends BroadcastReceiver {
        
        Activity activity;
        LoginCallBack loginCallBack;
        
        private static final String TAG="AuthReceiver";
        
        public AuthReceiver(Activity activity, LoginCallBack loginCallBack) {
            this.activity = activity;
            this.loginCallBack = loginCallBack;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle exts = intent.getExtras();
            String raw =  exts.getString("raw");
            final String access_token =  exts.getString(TAuthView.ACCESS_TOKEN);
            String expires_in1 =  exts.getString(TAuthView.EXPIRES_IN);
            String error_ret =  exts.getString(TAuthView.ERROR_RET);
            if (expires_in1 != null && !expires_in1.equals("0")) {
                expires_in1 = String.valueOf((System.currentTimeMillis() + Long.parseLong(expires_in1) * 1000));
            }
            final String expires_in = expires_in1;
            Log.i(TAG, String.format("raw: %s, access_token:%s, expires_in:%s", raw, access_token, expires_in));
            
            if (access_token != null) {
                mAccessToken = access_token;
//              TDebug.msg("正在获取OpenID...", getApplicationContext());
                ActionLog.getInstance(activity).addAction(ActionLog.DIALOG, this.activity.getString(R.string.doing_and_wait));
                this.activity.showDialog(R.id.dialog_share_doing);
                //用access token 来获取open id
                TencentOpenAPI.openid(access_token, new Callback() {
                    @Override
                    public void onSuccess(final Object obj) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mOpenId = ((OpenId)obj).getOpenId();
                                TencentOpenAPI.userInfo(mAccessToken, mAppid, mOpenId, new Callback() {
                                    
                                    @Override
                                    public void onSuccess(final Object obj) {
                                        try {
                                            UserInfo userInfo = (UserInfo)obj;
                                            UserAccessIdenty userAccessIdenty = new UserAccessIdenty(userInfo.getNickName(), access_token, mOpenId, expires_in);
                                            ShareAPI.writeIdentity(activity, ShareAPI.TYPE_TENCENT, userAccessIdenty);
                                            loginCallBack.onSuccess();
                                        } catch (Exception e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                            ShareAPI.clearIdentity(activity, ShareAPI.TYPE_TENCENT);
                                            loginCallBack.onFailed();
                                        }
                                        activity.dismissDialog(R.id.dialog_share_doing);
                                    }
                                    
                                    @Override
                                    public void onFail(final int ret, final String msg) {
                                        ShareAPI.clearIdentity(activity, ShareAPI.TYPE_TENCENT);
                                        loginCallBack.onFailed();
                                        activity.dismissDialog(R.id.dialog_share_doing);
                                    }
                                });
                            }
                        });
                    }
                    @Override
                    public void onFail(int ret, final String msg) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.dismissDialog(R.id.dialog_share_doing);
//                                TDebug.msg(msg, activity);
                                ShareAPI.clearIdentity(activity, ShareAPI.TYPE_TENCENT);
                                loginCallBack.onFailed();
                            }
                        });
                    }
                });
            } else {
                ShareAPI.clearIdentity(activity, ShareAPI.TYPE_TENCENT);
                if (error_ret != null) {
                    loginCallBack.onFailed();
                } else {
                    loginCallBack.onCancel();
                }
            }
        }

    }
    
    public static boolean satisfyConditions() {
        return  mAccessToken != null && 
                mAppid != null && 
                mOpenId != null && 
                !mAccessToken.equals("") && 
                !mAppid.equals("") && 
                !mOpenId.equals("");
    }
    
    public static void login(Activity activity) {
        Intent intent = new Intent(activity, com.tencent.tauth.TAuthView.class);
        
        intent.putExtra(TAuthView.CLIENT_ID, mAppid);
        intent.putExtra(TAuthView.SCOPE, scope);
        intent.putExtra(TAuthView.TARGET, "_self");
        intent.putExtra(TAuthView.CALLBACK, CALLBACK);
        
        activity.startActivity(intent);
        
    }
    
    public static void logout(Activity activity) {
        // TODO Auto-generated method stub
        mAccessToken = null;
        mOpenId = null;
        ShareAPI.clearIdentity(activity, ShareAPI.TYPE_TENCENT);
    }
    
    public static void addShare(final Activity activity, String comment, final boolean showDialog, final boolean finish) {
        if (!satisfyConditions()) {
//            TDebug.msg("请先获取access token和open id", activity);
            return;
        }
        if (showDialog) {
            ActionLog.getInstance(activity).addAction(ActionLog.DIALOG, activity.getString(R.string.doing_and_wait));
            activity.showDialog(R.id.dialog_share_doing);
        }
        Bundle bundle = null;
        bundle = new Bundle();
        bundle.putString("title", activity.getString(R.string.tencent_share_title));//必须。feeds的标题，最长36个中文字，超出部分会被截断。
        bundle.putString("url", "http://www.tigerknows.com/");//必须。分享所在网页资源的链接，点击后跳转至第三方网页， 请以http://开头。
//        bundle.putString("comment", comment);//用户评论内容，也叫发表分享时的分享理由。禁止使用系统生产的语句进行代替。最长40个中文字，超出部分会被截断。
        bundle.putString("summary", comment);//所分享的网页资源的摘要内容，或者是网页的概要描述。 最长80个中文字，超出部分会被截断。
        bundle.putString("images", "http://www.tigerknows.com/wp-content/themes/tiger/images/logo.gif");//所分享的网页资源的代表性图片链接"，请以http://开头，长度限制255字符。多张图片以竖线（|）分隔，目前只有第一张图片有效，图片规格100*100为佳。
        bundle.putString("type", "4");//分享内容的类型。4表示网页；5表示视频（type=5时，必须传入playurl）。 
//        bundle.putString("playurl", "http://player.youku.com/player.php/Type/Folder/Fid/15442464/Ob/1/Pt/0/sid/XMzA0NDM2NTUy/v.swf");//长度限制为256字节。仅在type=5的时候有效。
        TencentOpenAPI.addShare(mAccessToken, mAppid, mOpenId, bundle, new Callback() {
            
            @Override
            public void onSuccess(final Object obj) {
                activity.runOnUiThread(new Runnable() {
                    
                    @Override
                    public void run() {
                        Toast.makeText(activity, R.string.tencent_share_sucess, Toast.LENGTH_LONG).show();
                        if (showDialog) {
                            activity.dismissDialog(R.id.dialog_share_doing);
                        }
                        
                        if (finish)
                            activity.finish();
                    }
                });
            }
            
            @Override
            public void onFail(final int ret, final String msg) {
                activity.runOnUiThread(new Runnable() {
                    
                    @Override
                    public void run() {
                        if (ret >= 1000) {
                            ShareAPI.clearIdentity(activity, ShareAPI.TYPE_TENCENT);
                            Toast.makeText(activity, R.string.tencent_share_failed_token_error, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(activity, R.string.tencent_share_failed, Toast.LENGTH_LONG).show();
                        }
                        if (showDialog) {
                            activity.dismissDialog(R.id.dialog_share_doing);
                        }
//                        TDebug.msg(ret + ": " + msg, activity);
                    }
                });
            }
        });
    }
}
