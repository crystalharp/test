package com.tigerknows.share;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.tigerknows.TKApplication;
import com.decarta.android.util.LogWrapper;

public class ShareMessageCenter {
	
	public static final String ACTION_SHARE_AUTH_SUCCESS = "com.tigerknows.ACTION_SHARE_AUTH_SUCCESS";
	
	public static final String ACTION_SHARE_PROFILE_SUCCESS = "com.tigerknows.ACTION_SHARE_PROFILE_SUCCESS";
	
	public static final String ACTION_SHARE_PROFILE_FAIL = "com.tigerknows.ACTION_SHARE_PROFILE_FAIL";
	
	public static final String ACTION_SHARE_UPLOAD_SUCCESS = "com.tigerknows.ACTION_SHARE_UPLOAD_SUCCESS";
	
	public static final String ACTION_SHARE_UPLOAD_FAIL = "com.tigerknows.ACTION_SHARE_UPLOAD_FAIL";
	
	public static final String ACTION_SHARE_LOGOUT_SUCCESS = "com.tigerknows.ACTION_SHARE_LOGOUT_SUCCESS";
	
	public static final String ACTION_SHARE_LOGOUT_FAIL = "com.tigerknows.ACTION_SHARE_LOGOUT_FAIL";
	
	public static final String ACTION_SHARE_IMAGE_REMOVE = "com.tigerknows.ACTION_SHARE_IMAGE_REMOVE";
	
	public static final String ACTION_SHARE_REQUIRE_PROFILE = "com.tigerknows.ACTION_SHARE_REQUIRE_PROFILE";
	
	public static final String ACTION_SHARE_OPERATION_FAILED = "com.tigerknows.ACTION_SHARE_OPERATION_FAILED";
	
	public static final String EXTRA_SHARE_CONTENT = "com.tigerknows.share.EXTRA_SHARE_CONTENT";

    public static final String EXTRA_SHARE_PIC_URI = "com.tigerknows.share.EXTRA_SHARE_PIC_URI";
    
    public static final String EXTRA_SHARE_USER_NAME = "com.tigerknows.share.EXTRA_SHARE_USER_NAME";
    
    public static final String EXTRA_SHARE_FINISH = "com.tigerknows.share.EXTRA_SHARE_FINISH";
	
//	private static ShareMessageCenter mInstance = null;
	
//	private Context mContext;
	
	private static final String TAG = "ShareMessageCenter";
	
//	private ShareMessageCenter() {
//		mContext = TKApplication.getInstance().getApplicationContext();
//	}

//	public static ShareMessageCenter getInstance() {
//		LogWrapper.d(TAG, "getInstance :"+ mInstance);
//		if (mInstance == null) {
//			mInstance = new ShareMessageCenter();
//		}
//		return mInstance;
//	}
	
	public static void observeAction(Context context, BroadcastReceiver receiver, List<String> actionList) {
		LogWrapper.d(TAG, "observeAction");
		IntentFilter filter = new IntentFilter();
		for(String action : actionList) {
			filter.addAction(action);
		}
		LogWrapper.d(TAG, "registerReceiver: " + receiver);
		context.registerReceiver(receiver, filter);
	}
	
	public static void observerReceiver(Context context, BroadcastReceiver receiver, IntentFilter filter) {
		LogWrapper.d(TAG, "registerReceiver: " + receiver);
		context.registerReceiver(receiver, filter);
	}
	
	public static void unObserve(Context context, BroadcastReceiver receiver) {
		LogWrapper.d(TAG, "unObserve");
		context.unregisterReceiver(receiver);
		LogWrapper.d(TAG, "unregisterRecever: " + receiver);
	}
	
	public static void sendAction(Context context, String action) {
		LogWrapper.d(TAG, "sendAction: " + action);
		context.sendBroadcast(new Intent(action));
	}
}
