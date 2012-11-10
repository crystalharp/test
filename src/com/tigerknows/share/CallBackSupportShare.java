package com.tigerknows.share;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKApplication;

public abstract class CallBackSupportShare implements IBaseShare, ICallbackSupport {

	private ICallBack callback;
	
	private UserAccessIdenty identy = new UserAccessIdenty();
	
	private Activity mActivity;
	
	private final String TAG = "CallBackSupportShare";
	
	protected Context mContext;
	
	protected CallBackSupportShare() {
		super();
		mContext = TKApplication.getInstance();
	}

	public Activity getActivity() {
		return mActivity;
	}

	@Override
	public void setActivity(Activity activity) {
		mActivity = activity;
	}
	
	public UserAccessIdenty getIdenty() {
		return identy;
	}
	
	public void clearIdenty() {
		this.identy = new UserAccessIdenty();
	}

	@Override
	public void addCallBack(ICallBack callBack) {
		this.callback = callBack;
		LogWrapper.d(TAG, "addCallBack callback != null:" + (callback != null));
	}

	protected void callbackSuccessMessage(int operate, String tip) {
		if (callback != null) {
			LogWrapper.d(TAG, "callback.onSuccess: " + "operate: " + operate + "tip: " + tip);
			callback.onSuccess(operate, tip);
		}
	}
	
	protected void callbackFailMessage(int operate, String tip) {
		if (callback != null) {
			LogWrapper.d(TAG, "callback.onFail: " + "operate: " + operate + "tip: " + tip);
			callback.onFail(operate, tip);
		}
	}
	
	protected void callbackStoreMessage(UserAccessIdenty identy) {
		if (callback != null) {
			LogWrapper.d(TAG, "callback.onStore: " + "identy: " + identy);
			callback.onStore(identy);
		}
	}
	
	protected void setValueForIdenty(String token, String expireIn, String secret, String userName) {
		if (!TextUtils.isEmpty(token)) {
			identy.setAccessToken(token);
		}
		if (!TextUtils.isEmpty(expireIn)) {
			identy.setExpireIn(expireIn);
		}
		if (!TextUtils.isEmpty(secret)) {
			identy.setSecret(secret);
		}
		if (!TextUtils.isEmpty(userName)) {
			identy.setUserName(userName);
		}
		
		callbackStoreMessage(identy);
	}
	
	@Override
	public abstract void endowIdentity(UserAccessIdenty identy);

	@Override
	public abstract void auth();

	@Override
	public abstract String getUserName();

	@Override
	public abstract void requestUID();

	@Override
	public abstract void upload(String picPath, String content);

	@Override
	public abstract void logout();

	@Override
	public abstract boolean satisfyCondition();

}
