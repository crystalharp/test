package com.tigerknows.share;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.tigerknows.TKConfig;
import com.decarta.android.util.LogWrapper;

public class ShareMediator {

	private static Context mActivity;
	
	private static final String TAG = "ShareMediator";
	
	public static boolean isShowTip = true;
	
	public static void applyMeadiator(Activity activity, CallBackSupportShare share) {

		if (!(share instanceof ICallbackSupport)) {
			return;
		}
		
		mActivity = activity;

		share.setActivity(activity);
		// 读取之前存储的TOKEN, 用户名等信息
		UserAccessIdenty identy = readIdentity(translateClassNameToPrefsName(share.getClass().getName()));
		share.endowIdentity(identy);
		// 添加回调方法
		addCallBackToShare(activity, share);
	}
	
	/*
	 * Storage Mediator
	 */
	private static UserAccessIdenty readIdentity(String shareType) {
		UserAccessIdenty identity = null;
		LogWrapper.d(TAG, "readIdentity: " + shareType);
		try {
			String shareIdenty = TKConfig.getPref(mActivity, shareType);
			
			LogWrapper.d(TAG, "readIdentity shareIdenty: " + shareIdenty);
			if (!TextUtils.isEmpty(shareIdenty)) {
				byte [] data = Base64.decode(shareIdenty, Base64.DEFAULT);
//				ByteArrayInputStream byteArray = new ByteArrayInputStream(shareIdenty.getBytes());
				ByteArrayInputStream byteArray = new ByteArrayInputStream(data);
				ObjectInputStream in = new ObjectInputStream(byteArray);
				identity = (UserAccessIdenty) in.readObject();
				
				LogWrapper.d(TAG, "readIdentity identity.getAccessToken(): " + identity.getAccessToken());
				LogWrapper.d(TAG, "readIdentity identity.getExpireIn(): " + identity.getExpireIn());
				LogWrapper.d(TAG, "readIdentity identity.getSecret(): " + identity.getSecret());
				LogWrapper.d(TAG, "readIdentity identity.getUserName(): " + identity.getUserName());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return identity;
	}
	
	private static void writeIdentity(String shareType, UserAccessIdenty identity) {
		LogWrapper.d(TAG, "writeIdentity: " + shareType);
		LogWrapper.d(TAG, "writeIdentity identity.getAccessToken() : " + identity.getAccessToken());
		LogWrapper.d(TAG, "writeIdentity identity.getExpireIn() : " + identity.getExpireIn());
		LogWrapper.d(TAG, "writeIdentity identity.getSecret() : " + identity.getSecret());
		LogWrapper.d(TAG, "writeIdentity identity.getUserName() : " + identity.getUserName());
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream(); 
		ObjectOutputStream out;
		try {
		    out = new ObjectOutputStream(arrayOutputStream);
		    out.writeObject(identity);
		    out.close();
		    arrayOutputStream.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
//		TigerknowsConfig.setPref(mActivity, shareType, arrayOutputStream.toString());
		TKConfig.setPref(mActivity, shareType, new String(Base64.encode(arrayOutputStream.toByteArray(), Base64.DEFAULT)));
	}
	
	private static void clearIdentity(String shareType) {
		LogWrapper.d(TAG, "clearIdentity: " + shareType);
		TKConfig.removePref(mActivity, shareType);
	}
	
	/*
	 * Message Mediator
	 */
	private static void addCallBackToShare(final Context context, final ICallbackSupport share) {
		share.addCallBack(new ICallBack() {

			@Override
			public void onSuccess(int operate, String tip) {
				// TODO Auto-generated method stub
				if (operate == IBaseShare.AUTH) {
					ShareMessageCenter.sendAction(context, ShareMessageCenter.ACTION_SHARE_AUTH_SUCCESS);
				} else if (operate == IBaseShare.GET_USER_PROFILE) {
					ShareMessageCenter.sendAction(context, ShareMessageCenter.ACTION_SHARE_PROFILE_SUCCESS);
				} else if (operate == IBaseShare.UPLOAD) {
					ShareMessageCenter.sendAction(context, ShareMessageCenter.ACTION_SHARE_UPLOAD_SUCCESS);
				} else if (operate == IBaseShare.LOGOUT) {
					clearIdentity(translateClassNameToPrefsName(share.getClass().getName()));
					ShareMessageCenter.sendAction(context, ShareMessageCenter.ACTION_SHARE_LOGOUT_SUCCESS);
				}
				showToast(tip);
			}

			@Override
			public void onFail(int operate, String tip) {
				// TODO Auto-generated method stub
				if (operate == IBaseShare.GET_USER_PROFILE) {
					ShareMessageCenter.sendAction(context, ShareMessageCenter.ACTION_SHARE_PROFILE_FAIL);
				} else if (operate == IBaseShare.UPLOAD) {
					ShareMessageCenter.sendAction(context, ShareMessageCenter.ACTION_SHARE_UPLOAD_FAIL);
				} else if (operate == IBaseShare.LOGOUT) {
					ShareMessageCenter.sendAction(context, ShareMessageCenter.ACTION_SHARE_LOGOUT_FAIL);
				} else if (operate == IBaseShare.OTHER) {
					ShareMessageCenter.sendAction(context, ShareMessageCenter.ACTION_SHARE_OPERATION_FAILED);
				}
				showToast(tip);
			}

			@Override
			public void onStore(UserAccessIdenty accessIdenty) {
				// TODO Auto-generated method stub
				writeIdentity(translateClassNameToPrefsName(share.getClass().getName()), accessIdenty);
			}
			
		});
	}
	
	private static void showToast(final String tip) {
		
		if (!isShowTip) {
			return;
		}
		
		if (TextUtils.isEmpty(tip)) {
			return ;
		}
		
		((Activity)mActivity).runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(mActivity, tip, Toast.LENGTH_SHORT).show();
            }
        });
	}
	
	private static String translateClassNameToPrefsName(String className) {
		return className.replace(".", "_");
	}
}
