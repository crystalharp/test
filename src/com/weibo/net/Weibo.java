/*
 * Copyright 2011 Sina.
 *
 * Licensed under the Apache License and Weibo License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.open.weibo.com
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.weibo.net;

import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.decarta.android.util.LogWrapper;
/**
 * Encapsulation main Weibo APIs, Include: 1. getRquestToken , 2.
 * getAccessToken, 3. url request. Used as a single instance class. Implements a
 * weibo api as a synchronized way.
 * 
 * @author ZhangJie (zhangjie2@staff.sina.com.cn)
 */
public class Weibo {

	public enum RequestType {AUTHORIZE, GET_UID, GET_PROFILE, UPLOAD, END_SESSION, UNDEFINED};
	
    public static final String SERVER = "https://api.weibo.com/2/";

//    public static String URL_OAUTH2_ACCESS_TOKEN = "https://api.weibo.com/oauth2/access_token";

    public static final String URL_OAUTH2_ACCESS_AUTHORIZE = "https://api.weibo.com/oauth2/authorize";
    
    public static final String URL_DEFAULT_REDIRECT = "http://www.sina.com";
    
    public static final String URL_GET_UID = "account/get_uid.json";
    
    public static final String URL_GET_PROFILE = "users/show.json";
    
    public static final String URL_UPLOAD = "statuses/upload.json";
    
    public static final String URL_UPDATE = "statuses/update.json";
    
    public static final String URL_END_SESSION = "account/end_session.json";

    private static final int DEFAULT_AUTH_ACTIVITY_CODE = 32973;
    
    public static final String ACCESSTOKEN = "access_token";
    
    public static final String EXPIRES = "expires_in";
    
    public static String APP_KEY = "725632899"; //725632899
    
    public static String APP_SECRET = "10d7ad791693508c0f201e543faa2488"; //10d7ad791693508c0f201e543faa2488 94098772160b6f8ffc1315374d8861f9
    
    private Token mAccessToken = null;
    
    private static Weibo mWeiboInstance = null;

    private WeiboDialogListener mAuthDialogListener;
    
    private RequestListener mRequestListener;

    private String mRedirectUrl;

    private User mUser;
    
    private RequestType mRequestType = RequestType.UNDEFINED;
    
    private Weibo() {
        Utility.setRequestHeader("Accept-Encoding", "gzip");
        mRedirectUrl = URL_DEFAULT_REDIRECT;
    }

    public synchronized static Weibo getInstance() {
        if (mWeiboInstance == null) {
            mWeiboInstance = new Weibo();
        }
        return mWeiboInstance;
    }

    // 设置accessToken
    public void setAccessToken(AccessToken token) {
        mAccessToken = token;
    }

    public Token getAccessToken() {
        return this.mAccessToken;
    }

    public void setupConsumerConfig(String consumer_key, String consumer_secret) {
        Weibo.APP_KEY = consumer_key;
        Weibo.APP_SECRET = consumer_secret;
    }

    public static String getAppKey() {
        return Weibo.APP_KEY;
    }

    public static String getAppSecret() {
        return Weibo.APP_SECRET;
    }

    public String getRedirectUrl() {
        return mRedirectUrl;
    }

    public void setRedirectUrl(String mRedirectUrl) {
        this.mRedirectUrl = mRedirectUrl;
    }

    public User getUser() {
		return mUser;
	}

	public void setUser(User user) {
		this.mUser = user;
	}

	public RequestListener getRequestListener() {
		return mRequestListener;
	}
	
	public void setRequestListener(RequestListener listener) {
		mRequestListener = listener;
	}
	
	public RequestType getRequestType() {
		return mRequestType;
	}
	
	public boolean isTokenValid() {
		LogWrapper.d("eric", "mAccessToken != null" + (mAccessToken != null));
		LogWrapper.d("eric", "mAccessToken.getExpiresIn() > System.currentTimeMillis()" + (mAccessToken.getExpiresIn() > System.currentTimeMillis()));
		if (mAccessToken != null && mAccessToken.getExpiresIn() > System.currentTimeMillis()) {
			return true;
		}
		return false;
	}
	
	/**
     * Requst sina weibo open api by get or post
     * 
     * @param url
     *            Openapi request URL.
     * @param params
     *            http get or post parameters . e.g.
     *            gettimeling?max=max_id&min=min_id max and max_id is a pair of
     *            key and value for params, also the min and min_id
     * @param httpMethod
     *            http verb: e.g. "GET", "POST", "DELETE"
     * @throws IOException
     * @throws MalformedURLException
     * @throws WeiboException
     */
    public String request(Context context, String url, WeiboParameters params, String httpMethod,
            Token token) throws WeiboException, IOException {
//        String rlt = Utility.openUrl(context, url, httpMethod, params, this.mAccessToken);
    	String rlt = Utility.openUrl(context, url, httpMethod, params, null);
        return rlt;
    }
    
    public String request(Context context, String url, WeiboParameters params, String httpMethod) {
    	String rlt = "";
    	try {
    		rlt = Utility.openUrl(context, url, httpMethod, params, null);
    		mRequestListener.onComplete(rlt);
		} catch (WeiboException e) {
			// TODO Auto-generated catch block
			mRequestListener.onError(e);
		} catch (IOException e) {
			mRequestListener.onIOException(e);
		}
        return rlt;
    }

    private boolean startSingleSignOn(Activity activity, String applicationId,
            String[] permissions, int activityCode) {
        return false;
    }

    private void startDialogAuth(Activity activity, String[] permissions) {
    	Log.d("eric", "startDialogAuth invoked");
        WeiboParameters params = new WeiboParameters();
        if (permissions.length > 0) {
            params.add("scope", TextUtils.join(",", permissions));
        }
//        CookieSyncManager.createInstance(activity);
        dialog(activity, params, new WeiboDialogListener() {

            public void onComplete(Bundle values) {
                // ensure any cookies set by the dialog are saved
//                CookieSyncManager.getInstance().sync();
                if (null == mAccessToken) {
                    mAccessToken = new Token();
                }
                mAccessToken.setToken(values.getString(ACCESSTOKEN));
                mAccessToken.setExpiresIn(values.getString(EXPIRES));
                if (isSessionValid()) {
                    Log.d("Weibo-authorize",
                            "Login Success! access_token=" + mAccessToken.getToken() + " expires="
                                    + mAccessToken.getExpiresIn());
                    mAuthDialogListener.onComplete(values);
                } else {
                    Log.d("Weibo-authorize", "Failed to receive access token");
                    mAuthDialogListener.onWeiboException(new WeiboException(
                            "Failed to receive access token."));
                }
            }

            public void onError(DialogError error) {
                Log.d("Weibo-authorize", "Login failed: " + error);
                mAuthDialogListener.onError(error);
            }

            public void onWeiboException(WeiboException error) {
                Log.d("Weibo-authorize", "Login failed: " + error);
                mAuthDialogListener.onWeiboException(error);
            }

            public void onCancel() {
                Log.d("Weibo-authorize", "Login canceled");
                mAuthDialogListener.onCancel();
            }
        });
    }

    /**
     * User-Agent Flow
     * 
     * @param activity
     * 
     * @param listener
     *            授权结果监听器
     */
    public void authorize(Activity activity, final WeiboDialogListener listener) {
        authorize(activity, new String[] {}, DEFAULT_AUTH_ACTIVITY_CODE, listener);
    }

//    private void authorize(Activity activity, String[] permissions,
//            final WeiboDialogListener listener) {
//        authorize(activity, permissions, DEFAULT_AUTH_ACTIVITY_CODE, listener);
//    }

    private void authorize(Activity activity, String[] permissions, int activityCode,
            final WeiboDialogListener listener) {
        Utility.setAuthorization(new Oauth2AccessTokenHeader());

        boolean singleSignOnStarted = false;
        mAuthDialogListener = listener;

        // Prefer single sign-on, where available.
        if (activityCode >= 0) {
            singleSignOnStarted = startSingleSignOn(activity, APP_KEY, permissions, activityCode);
        }
        // Otherwise fall back to traditional dialog.
        
        if (!singleSignOnStarted && activity != null && !activity.isFinishing()) {
            startDialogAuth(activity, permissions);
        }

    }

    public void dialog(Context context, WeiboParameters parameters,
            final WeiboDialogListener listener) {
        parameters.add("client_id", APP_KEY);
        parameters.add("response_type", "token");
        parameters.add("redirect_uri", mRedirectUrl);
        parameters.add("display", "mobile");

        if (isSessionValid()) {
            parameters.add(ACCESSTOKEN, mAccessToken.getToken());
        }
        String url = URL_OAUTH2_ACCESS_AUTHORIZE + "?" + Utility.encodeUrl(parameters);
        Log.d("eric", "request url:" + url);
//        if (context.checkCallingOrSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
//            Utility.showAlert(context, "Error",
//                    "Application requires permission to access the Internet");
//        } else {
        mRequestType = RequestType.AUTHORIZE;
        new WeiboDialog(this, context, url, listener).show();
//        }
    }

    public boolean isSessionValid() {
        if (mAccessToken != null) {
            return (!TextUtils.isEmpty(mAccessToken.getToken()) && (mAccessToken.getExpiresIn() == 0 || (System
                    .currentTimeMillis() < mAccessToken.getExpiresIn())));
        }
        return false;
    }

    public Long getUserUID(Context context) {
    	String url = SERVER + URL_GET_UID;
    	
    	WeiboParameters bundle = new WeiboParameters();
        bundle.add(ACCESSTOKEN, mAccessToken.getToken());
        
        mRequestType = RequestType.GET_UID;
        
        String rlt = request(context, url, bundle, Utility.HTTPMETHOD_GET);

    	if (mUser == null) {
    		mUser = new User();
    	}
    	
        if (!TextUtils.isEmpty(rlt)) {
        	try {
				JSONObject json = new JSONObject(rlt);
				mUser.setUid(Long.valueOf(json.getString("uid")));
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
		
        LogWrapper.d("eric", "Weibo.getuserID: " + mUser.getUid());
        return mUser.getUid();
    }
    
    public void getUserProfile(Context context){
    	if (!User.isUidValid(getUserUID(context))) {
    		return ;
    	}
    	
    	String url = SERVER + URL_GET_PROFILE;
    	
    	WeiboParameters bundle = new WeiboParameters();
        bundle.add(ACCESSTOKEN, mAccessToken.getToken());
        bundle.add("uid", String.valueOf(mUser.getUid()));
        
        mRequestType = RequestType.GET_PROFILE;
        
		String rlt = request(context, url, bundle, Utility.HTTPMETHOD_GET);
		LogWrapper.d("eric", "getUserProfile result: " + rlt);

		if (!TextUtils.isEmpty(rlt)) {
        	if (mUser == null) {
        		mUser = new User();
        	}
        	try {
				JSONObject json = new JSONObject(rlt);
				mUser.setId(Long.valueOf(json.getString("id")));
				mUser.setScreenName(json.getString("screen_name"));
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
		
        LogWrapper.d("eric", "Weibo.getUserProfile: " + mUser);
    }
    
    public void upload(Context context, String file, String status, String lon, String lat, String annotations) {
    	
    	String url = SERVER + URL_UPLOAD;
    	
        WeiboParameters bundle = new WeiboParameters();
        bundle.add(ACCESSTOKEN, mAccessToken.getToken());
        bundle.add("status", status);
        if (!TextUtils.isEmpty(file)) {
        	bundle.add("pic", file);
        	url = SERVER + URL_UPLOAD;
        } else {
        	url = SERVER + URL_UPDATE;
        }
        	
        if(!TextUtils.isEmpty(lon)){
            bundle.add("long", lon);
        }
        if(!TextUtils.isEmpty(lat)){
            bundle.add("lat", lat);
        }
        if(!TextUtils.isEmpty(annotations)){
            bundle.add("annotations", annotations);
        }
        
        mRequestType = RequestType.UPLOAD;
        
//        AsyncWeiboRunner weiboRunner = new AsyncWeiboRunner(this);
//        weiboRunner.request(context, url, bundle, Utility.HTTPMETHOD_POST);
        request(context, url, bundle, Utility.HTTPMETHOD_POST);
    }

    public void endSession(Context context) {
    	String url = SERVER + URL_END_SESSION;
    	
        WeiboParameters bundle = new WeiboParameters();
        bundle.add(ACCESSTOKEN, mAccessToken.getToken());
        
        mRequestType = RequestType.END_SESSION;
        
//        AsyncWeiboRunner weiboRunner = new AsyncWeiboRunner(this);
//        weiboRunner.request(context, url, bundle, Utility.HTTPMETHOD_GET);
        String endSessionResult = request(context, url, bundle, Utility.HTTPMETHOD_GET);
        LogWrapper.d("eric", "endSessionResult: " + endSessionResult);
        mAccessToken = null;
        mUser = null;
    }
    
    public static interface RequestListener {

        public void onComplete(String response);

        public void onIOException(IOException e);

        public void onError(WeiboException e);

    }
}
