package com.tigerknows.share.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;

import com.tencent.tauth.TAuthView;
import com.tencent.tauth.TencentOpenAPI;
import com.tencent.tauth.bean.OpenId;
import com.tencent.tauth.bean.UserInfo;
import com.tencent.tauth.http.Callback;
import com.tigerknows.R;
import com.tigerknows.share.CallBackSupportShare;
import com.tigerknows.share.ShareMediator;
import com.tigerknows.share.ShareMessageCenter;
import com.tigerknows.share.UserAccessIdenty;
import com.decarta.android.util.LogWrapper;

public class TencentOpenShare extends CallBackSupportShare {

	public String mAppid = "100271659";//申请时分配的appid 100271659
	
//	public String mKey = "10d5250358a2e35a15e1ddcd426bc5d3";
	/*
	 * QQ开发平台注册的回调地址为 www.tigerknows.com
	 * 发出的请求不能是浏览器能识别的协议，比如：http://www.tigerknows.com/
	 */
	private static final String CALLBACK = "tencentauth://www.tigerknows.com/";
	
	private String scope = "get_user_info,add_share";//授权范围
	
	private AuthReceiver receiver;
	
	private AccessToken mAccessToken = new AccessToken();
	
	private User mUser = new User();

	private final String TAG = "TencentOpenShare";
	
	private static TencentOpenShare mInstance = null;
	
	private boolean receivedAuth = false;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver(){
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			LogWrapper.d(TAG, "receive action: " + action);
			if (ShareMessageCenter.EXTRA_SHARE_FINISH.equals(action)) {
				clear();
			}
		}
		
	};
	
	public TencentOpenShare() {
		super();
		Log.i(TAG, "TencentOpenShare");
		// TODO Auto-generated constructor stub
    	observerMessage();
		registerAuthIntentReceivers();
	}

	public static TencentOpenShare getInstance(Activity activity) {
		if (mInstance == null) {
			mInstance = new TencentOpenShare();
		}
		
		ShareMediator.applyMeadiator(activity, mInstance);
		return mInstance;
	}
	
	@Override
	public void endowIdentity(UserAccessIdenty identy) {
		// TODO Auto-generated method stub
		LogWrapper.d(TAG, "endowIdentity");
		if (identy == null)
			return;
		
		String token = identy.getAccessToken();
		String expire = identy.getExpireIn();
		String uid = identy.getSecret();
		String userName = identy.getUserName();
		
		if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(expire)) {
			mAccessToken.setToken(token);
			mAccessToken.setExpiresIn(expire);
            if (!TextUtils.isEmpty(userName)) {
            	mUser.setUserName(userName);
            }
            if (!TextUtils.isEmpty(uid)) {
            	mUser.setOpenId(uid);
            }
        }
		
		LogWrapper.d(TAG, "endowIdentity identity.getAccessToken(): " + mAccessToken.getToken());
		LogWrapper.d(TAG, "endowIdentity identity.getExpireIn(): " + String.valueOf(mAccessToken.getExpiresIn()));
		LogWrapper.d(TAG, "endowIdentity identity.getSecret(): " + mUser.getOpenId());
		LogWrapper.d(TAG, "endowIdentity identity.getUserName(): " + mUser.getUserName());
	}

	@Override
	public void auth() {
		// TODO Auto-generated method stub
		LogWrapper.d(TAG, "auth");
//		registerAuthIntentReceivers();
		auth(mAppid, "_slef"); // _blank
	}

	@Override
	public String getUserName() {
		// TODO Auto-generated method stub
		LogWrapper.d(TAG, "getUserName");
		return mUser.getUserName();
	}

	@Override
	public void requestUID() {
		// TODO Auto-generated method stub
		LogWrapper.d(TAG, "requestUID");
		TencentOpenAPI.openid(mAccessToken.getToken(), new Callback() {
			@Override
			public void onSuccess(final Object obj) {
				mUser.setOpenId(((OpenId)obj).getOpenId());
				requestUserInfo();
			}
			
			@Override
			public void onFail(int ret, final String msg) {
				LogWrapper.d(TAG, "获取UID出错" + "ret: " + ret + "msg: " + msg);
				callbackFailMessage(GET_USER_PROFILE, getActivity().getString(R.string.tencent_getprofile_failed));
			}
		});
	}

	private void requestUserInfo() {
		LogWrapper.d(TAG, "requestUserInfo");
		TencentOpenAPI.userInfo(mAccessToken.getToken(), mAppid, mUser.getOpenId(), new Callback() {
			
			@Override
			public void onSuccess(final Object obj) {
				try {
					UserInfo userInfo = (UserInfo)obj;
					mUser.setUserName(userInfo.getNickName());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (!TextUtils.isEmpty(mUser.getUserName())) {
					callbackSuccessMessage(GET_USER_PROFILE, "");
					setValueForIdenty(null, null, mUser.getOpenId(), mUser.getUserName());
				}
			}
			
			@Override
			public void onFail(final int ret, final String msg) {
				callbackFailMessage(GET_USER_PROFILE, getActivity().getString(R.string.tencent_getprofile_failed));
			}
		});
	}
	
	@Override
	public void upload(String picPath, String content) {
		// TODO Auto-generated method stub
		LogWrapper.d(TAG, "upload");
		Bundle bundle = null;
		bundle = new Bundle();
		bundle.putString("title", getActivity().getString(R.string.tencent_share_title));//必须。feeds的标题，最长36个中文字，超出部分会被截断。
		bundle.putString("url", "http://www.tigerknows.com");
		bundle.putString("comment", content);
		TencentOpenAPI.addShare(mAccessToken.getToken(), mAppid, mUser.getOpenId(), bundle, new Callback() {
			
			@Override
			public void onSuccess(final Object obj) {
				callbackSuccessMessage(UPLOAD, getActivity().getString(R.string.send_sucess));
			}
			
			@Override
			public void onFail(final int ret, final String msg) {
				callbackFailMessage(UPLOAD, getActivity().getString(R.string.operation_failed));
			}
		});
	}


	@Override
	public void logout() {
		// TODO Auto-generated method stub
		mUser = new User();
		mAccessToken = new AccessToken();
		receivedAuth = false;
		clearIdenty();
		
		callbackSuccessMessage(LOGOUT, getActivity().getString(R.string.logout_sucess));
	}

	@Override
	public boolean satisfyCondition() {
		// TODO Auto-generated method stub
		return 	mAccessToken.getToken() != null && 
				mAppid != null && 
				mUser.getOpenId() != null && 
				!mAccessToken.getToken().equals("") && 
				!mAppid.equals("") && 
				!mUser.getOpenId().equals("");
	}

	@Override
	public Class getSenderName() {
		// TODO Auto-generated method stub
		return QZoneSend.class;
	}
	
	/**
	 * 打开登录认证与授权页面
	 * 
	 * @param	String	clientId	申请时分配的appid
	 * @param	String	target		打开登录页面的方式：“_slef”以webview方式打开; "_blank"以内置安装的浏览器方式打开
	 * @author John.Meng<arzen1013@gmail> QQ:3440895
	 * @date 2011-9-5
	 */
    private void auth(String clientId, String target) {
		Intent intent = new Intent(getActivity(), com.tencent.tauth.TAuthView.class);
		
		intent.putExtra(TAuthView.CLIENT_ID, clientId);
		intent.putExtra(TAuthView.SCOPE, scope);
		intent.putExtra(TAuthView.TARGET, target);
		intent.putExtra(TAuthView.CALLBACK, CALLBACK);
		
		getActivity().startActivity(intent);
	}
    
    private void registerAuthIntentReceivers() {
    	Log.i(TAG, "registerIntentReceivers");
		receiver =  new AuthReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TAuthView.AUTH_BROADCAST);
        ShareMessageCenter.observerReceiver(mContext, receiver, filter);
	}
    
    /**
	 * 广播的侦听，授权完成后的回调是以广播的形式将结果返回
	 * 
	 * @author John.Meng<arzen1013@gmail> QQ:3440895
	 * @date 2011-9-5
	 */
    public class AuthReceiver extends BroadcastReceiver {
    	
    	private static final String TAG="AuthReceiver";

    	public AuthReceiver() {
			super();
			Log.i(TAG, "AuthReceiver construct");
			// TODO Auto-generated constructor stub
		}

		@Override
    	public void onReceive(Context context, Intent intent) {
    		Log.i(TAG, "AuthReceiver.onReceive" + TencentOpenShare.this);
    		/*
    		 * 有时候会收到多个相同的ACTION, 如多个授权成功的ACTION
    		 */
    		synchronized (this) {
    			Log.i(TAG, "received: " + receivedAuth);
    			if (!receivedAuth) {
    				receivedAuth = true;
            		
         			Bundle exts = intent.getExtras();
                	String raw =  exts.getString("raw");
                	String access_token =  exts.getString(TAuthView.ACCESS_TOKEN);
                	String expires_in =  exts.getString(TAuthView.EXPIRES_IN);
                	String error_ret =  exts.getString(TAuthView.ERROR_RET);
                	String error_des =  exts.getString(TAuthView.ERROR_DES);
                	Log.i(TAG, String.format("raw: %s, access_token:%s, expires_in:%s", raw, access_token, expires_in));
                	
                	if (access_token != null) {
                		mAccessToken.setToken(access_token);
                		if (expires_in != null && !expires_in.equals("0")) {
                        	mAccessToken.setExpiresIn(System.currentTimeMillis() + Long.parseLong(expires_in) * 1000);
                        }
                		
                		setValueForIdenty(mAccessToken.getToken(), String.valueOf(mAccessToken.getExpiresIn()), null, null);
                		
                		callbackSuccessMessage(AUTH, "");
        			}
                	if (error_ret != null) {
                		callbackFailMessage(AUTH, getActivity().getString(R.string.tencent_auth_fail));
                		Log.i(TAG, "获取access token失败" + "\n错误码: " + error_ret + "\n错误信息: " + error_des);
                	}
        		}
			}
    	}

    }
    
    public class AccessToken {
    	private String mToken = "";
    	
    	private long mExpiresIn = 0;
    	
    	private String mKey = "";
    	
    	public AccessToken() {
			super();
			// TODO Auto-generated constructor stub
		}

		public String getToken() {
			return mToken;
		}
		
    	public void setToken(String token) {
			this.mToken = token;
		}

    	public long getExpiresIn() {
			return mExpiresIn;
		}
    	
		public void setExpiresIn(long mExpiresIn) {
			this.mExpiresIn = mExpiresIn;
		}

		public void setExpiresIn(String expiresIn) {
            this.mExpiresIn = Long.parseLong(expiresIn);
        }

		public String getKey() {
			return mKey;
		}
    }
    
    public class User {
    	public String mOpenId = "";
    	
    	public String mUserName = "";

		public String getOpenId() {
			return mOpenId;
		}

		public void setOpenId(String mOpenId) {
			this.mOpenId = mOpenId;
		}

		public String getUserName() {
			return mUserName;
		}

		public void setUserName(String mUserName) {
			this.mUserName = mUserName;
		}
    }

    private void observerMessage() {
		LogWrapper.d(TAG, "observerMessage");
		List<String> actionList = new ArrayList<String>(Arrays.asList(
			ShareMessageCenter.EXTRA_SHARE_FINISH
		));
		
		ShareMessageCenter.observeAction(mContext, mReceiver, actionList);
	}
    
    private void unObserverMessage(BroadcastReceiver receiver) {
		ShareMessageCenter.unObserve(mContext, receiver);
	}
    
    protected void clear() {
    	LogWrapper.d(TAG, "clear()");
    	unObserverMessage(mReceiver);
    	mInstance = null;
    }
}
