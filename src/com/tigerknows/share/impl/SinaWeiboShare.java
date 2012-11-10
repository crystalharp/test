package com.tigerknows.share.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.share.CallBackSupportShare;
import com.tigerknows.share.ICallBack;
import com.tigerknows.share.ShareMediator;
import com.tigerknows.share.ShareMessageCenter;
import com.tigerknows.share.UserAccessIdenty;
import com.decarta.android.util.LogWrapper;
import com.weibo.net.AccessToken;
import com.weibo.net.DialogError;
import com.weibo.net.User;
import com.weibo.net.Weibo;
import com.weibo.net.Weibo.RequestListener;
import com.weibo.net.Weibo.RequestType;
import com.weibo.net.WeiboDialogListener;
import com.weibo.net.WeiboException;

public class SinaWeiboShare extends CallBackSupportShare implements RequestListener {

	private Weibo mWeibo;
	
	private final String TAG = "SinaWeiboShare";
	
	private static SinaWeiboShare mInstance = null;
	
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
	

	public SinaWeiboShare() {
		super();
		mWeibo = Weibo.getInstance();
		mWeibo.setRequestListener(this);
    	observerMessage();
	}
	
	public static SinaWeiboShare getInstance(Activity activity) {
		if (mInstance == null) {
			mInstance = new SinaWeiboShare();
		}
		
		ShareMediator.applyMeadiator(activity, mInstance);
		return mInstance;
	}
	
	@Override
	public void auth() {
		// TODO Auto-generated method stub
		mWeibo.authorize(getActivity(), new AuthDialogListener());
	}

	@Override
	public String getUserName() {
		// TODO Auto-generated method stub
		if (mWeibo.getUser() != null && !TextUtils.isEmpty(mWeibo.getUser().getScreenName())) {
			return mWeibo.getUser().getScreenName();
		}
		return null;
	}
	
	@Override
	public void requestUID() {
		// 获取用户信息是同步操作
		mWeibo.getUserProfile(getActivity());
		if (!TextUtils.isEmpty(getUserName())) {
			callbackSuccessMessage(GET_USER_PROFILE, "");
			setValueForIdenty(null, null, null, getUserName());
		} else {
			callbackFailMessage(GET_USER_PROFILE, getActivity().getString(R.string.weibo_getprofile_failed));
		}
	}

	@Override
	public void upload(String picPath, String content) {
		// TODO Auto-generated method stub
		mWeibo.upload(getActivity(), picPath, content, "", "", "");
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub
		mWeibo.endSession(getActivity());
	}

	@Override
	public boolean satisfyCondition() {
		// TODO Auto-generated method stub
		return mWeibo.isSessionValid();
	}

	@Override
	public void endowIdentity(UserAccessIdenty identy) {
		// TODO Auto-generated method stub
		if (identy == null)
			return;
		
		String weiboToken = identy.getAccessToken();
		String weiboSecret = identy.getSecret();
		String weiboExpires = identy.getExpireIn();
		String weiboScreenName = identy.getUserName();
		
		if (!TextUtils.isEmpty(weiboToken) && !TextUtils.isEmpty(weiboSecret) && !TextUtils.isEmpty(weiboExpires)) {
            AccessToken accessToken = new AccessToken(weiboToken, weiboSecret, weiboExpires);
            Weibo weibo = Weibo.getInstance();
            weibo.setAccessToken(accessToken);
            if (!TextUtils.isEmpty(weiboScreenName)) {
                User user = new User();
                user.setScreenName(weiboScreenName);
                weibo.setUser(user);
            }
        }
	}

	class AuthDialogListener implements WeiboDialogListener {

        @Override
        public void onComplete(Bundle values) {
        	LogWrapper.d("eric", "AuthDialogListener.onComplete");
            String token = values.getString(Weibo.ACCESSTOKEN);
            String expires_in = values.getString(Weibo.EXPIRES);
            AccessToken accessToken = new AccessToken(token, Weibo.APP_SECRET);
            accessToken.setExpiresIn(expires_in);
            Weibo.getInstance().setAccessToken(accessToken);

            setValueForIdenty(token, String.valueOf(accessToken.getExpiresIn()), Weibo.APP_SECRET, null);
            
            LogWrapper.d("eric", "Get AccessToken.token: " + Weibo.getInstance().getAccessToken().getToken());
            callbackSuccessMessage(AUTH, "");
        }

        @Override
        public void onError(DialogError e) {
        	callbackFailMessage(AUTH, e.getMessage());
        }

        @Override
        public void onCancel() {
        }

        @Override
        public void onWeiboException(WeiboException e) {
        	callbackFailMessage(OTHER, getActivity().getString(R.string.operation_failed));
        }

    }

	@Override
	public void onComplete(String response) {
		// TODO Auto-generated method stub
		RequestType requestType = mWeibo.getRequestType();
		switch(requestType) {
		case UPLOAD:
			callbackSuccessMessage(UPLOAD, getActivity().getString(R.string.send_sucess));
			break;
		case END_SESSION:
			callbackSuccessMessage(LOGOUT, getActivity().getString(R.string.logout_sucess));
			break;
		default:
		}
	}

	@Override
	public void onIOException(IOException e) {
		// TODO Auto-generated method stub
		callbackFailMessage(OTHER, getActivity().getString(R.string.operation_failed));
	}

	@Override
	public void onError(WeiboException e) {
		// TODO Auto-generated method stub
		WeiboException.ErrorType type = e.getErrorType();
		switch(type) {
		case NETWORK_ERROR:
			callbackFailMessage(OTHER, getActivity().getString(R.string.weibo_network_error));
			break;
		case EXPIRED_TOKEN:
			callbackFailMessage(OTHER, getActivity().getString(R.string.weibo_token_expire));
			auth();
			break;
		case DUPLICATE_UPLOAD:
			callbackFailMessage(UPLOAD, getActivity().getString(R.string.weibo_same_content));
			break;
		case UPLOAD_FAILED:
			callbackFailMessage(UPLOAD, getActivity().getString(R.string.weibo_publish_failed_unknow));
			break;
		case LOGOUT_FAILED:
			callbackFailMessage(LOGOUT, getActivity().getString(R.string.logout_failed));
			break;
		case GET_PROFILE_FAILED:
			callbackFailMessage(GET_USER_PROFILE, getActivity().getString(R.string.weibo_getprofile_failed));
			break;
		default:
			callbackFailMessage(OTHER, getActivity().getString(R.string.operation_failed));
		}
	}

	@Override
	public Class getSenderName() {
		// TODO Auto-generated method stub
		return WeiboSend.class;
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
