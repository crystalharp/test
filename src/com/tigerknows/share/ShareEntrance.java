package com.tigerknows.share;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKApplication;
import com.tigerknows.share.impl.SinaWeiboShare;
import com.tigerknows.share.impl.TencentOpenShare;
import com.tigerknows.share.impl.WeiboSend;
import com.decarta.android.util.LogWrapper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Window;

public class ShareEntrance {

	public static final int TENCENT_ENTRANCE = 0;
	
	public static final int SINAWEIBO_ENTRANCE = 1;
	
	private Intent mShareIntent = null;
	
	private IBaseShare mShareObject;
	
	private static ShareEntrance mInstance;
	
	private Context mContext;
	
	private Activity mActivity;
	
	private ProgressDialog mSpinner;
	
	private static final String TAG = "ShareEntrance";
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver(){
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			LogWrapper.d(TAG, "receive action: " + action);
			if (ShareMessageCenter.ACTION_SHARE_AUTH_SUCCESS.equals(action)) {
				mHandler.sendEmptyMessage(0);
				mShareObject.requestUID();
			} else if (ShareMessageCenter.ACTION_SHARE_PROFILE_SUCCESS.equals(action)
					|| ShareMessageCenter.ACTION_SHARE_PROFILE_FAIL.equals(action)) {
				mHandler.sendEmptyMessage(1);
			} else if (ShareMessageCenter.EXTRA_SHARE_FINISH.equals(action)) {
				clear();
			}
		}
		
	};
	
	private ShareEntrance() {
		// TODO 注册消息监听
		LogWrapper.d(TAG, "ShareEntrance");
		mContext = TKApplication.getInstance();
		observerMessage(mReceiver);
	}
	
	public static ShareEntrance getInstance() {
		LogWrapper.d(TAG, "getInstance :" + mInstance);
		if (mInstance == null) {
			mInstance = new ShareEntrance();
		}
		return mInstance;
	}

	private void observerMessage(BroadcastReceiver receiver) {
		LogWrapper.d(TAG, "observerMessage");
		List<String> actionList = new ArrayList<String>(Arrays.asList(
			ShareMessageCenter.ACTION_SHARE_AUTH_SUCCESS,
			ShareMessageCenter.ACTION_SHARE_PROFILE_SUCCESS,
			ShareMessageCenter.ACTION_SHARE_PROFILE_FAIL,
			ShareMessageCenter.ACTION_SHARE_REQUIRE_PROFILE
		));
		
		ShareMessageCenter.observeAction(mContext, receiver, actionList);
	}
	
	private void unObserverMessage(BroadcastReceiver receiver) {
		ShareMessageCenter.unObserve(mContext, receiver);
	}
	
	/**
	 * 获取当前正在使用的IBaseShare对象，
	 * 一般在dealWithShare，injectOrAuth调用后使用
	 * @return
	 */
	public IBaseShare getCurrentShareObject() {
		return mShareObject;
	}
	
	public static IBaseShare getShareObject(Activity activity, int entrance) {
		return getShareObject(activity, entrance, false);
	}
	
	/**
	 * 根据传入的entrance获取IBaseShare对象，如IBaseShare对象已存在，则更新其关联的activity
	 * @param activity
	 * @param entrance
	 * @return
	 */
	public static IBaseShare getShareObject(Activity activity, int entrance, boolean isShowTip) {
		ShareMediator.isShowTip = isShowTip;
		
		IBaseShare shareObject = null;
		switch(entrance){
		case TENCENT_ENTRANCE:
			shareObject = TencentOpenShare.getInstance(activity);
			break;
		case SINAWEIBO_ENTRANCE:
			shareObject = SinaWeiboShare.getInstance(activity);
			break;
		}
		return shareObject;
	}
	
	boolean invokeSendProcess = false;
	public void dealWithShare(Activity activity, int entrance, Intent intent) {
		
		mActivity = activity;
		mSpinner = new ShareProgressDialog(mActivity);
		
		mShareObject = getShareObject(activity, entrance, true);
		mShareIntent = intent;
		proceedSend = true;
		
		invokeSendProcess = false;
		if (mShareObject.satisfyCondition()) {
			invokeSendProcess();
		} else {
			invokeAuthProcess();
		}
	}
	
	private boolean proceedSend = true;
	
	/**
	 * 临时方法, 用于点评页
	 * @param shareObject
	 */
	public void injectOrAuth(Activity activity, int entrance) {
		
		mActivity = activity;
		mSpinner = new ShareProgressDialog(mActivity);

		mShareObject = getShareObject(activity, entrance, false);
		proceedSend = false;
		
		if (!mShareObject.satisfyCondition()) {
			invokeAuthProcess();
		} 
	}
	
	protected void invokeAuthProcess() {
		LogWrapper.d(TAG, "invokeAuthProcess");
		mShareObject.auth();
	}
	
	protected void invokeSendProcess() {
	    if (invokeSendProcess || mShareObject == null || mShareIntent == null) {
	        return;
	    }
		LogWrapper.d(TAG, "invokeSendProcess");
		LogWrapper.d(TAG, "mShareObject.getUserName(): " + mShareObject.getUserName());

		mShareIntent.putExtra(ShareMessageCenter.EXTRA_SHARE_USER_NAME, mShareObject.getUserName());
		mShareIntent.setClass(mActivity, mShareObject.getSenderName());
		
		// 启动WeiboSend
		mActivity.startActivity(mShareIntent);
		invokeSendProcess = true;
	}
	
	class ShareProgressDialog extends ProgressDialog {

		public ShareProgressDialog(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
			requestWindowFeature(Window.FEATURE_NO_TITLE);
	        setMessage(context.getString(R.string.tencent_getprofile_loading));
	        setCancelable(true);
	        setCanceledOnTouchOutside(false);
	        
	        setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					// TODO Auto-generated method stub
					LogWrapper.d(TAG, "progressDialog canceled");
					canceled = true;
				}
			});
		}
		
	}
	
	private boolean canceled = false;
	
	Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			
			if (mActivity != null && !mActivity.isFinishing()) {
				if (msg.what == 0) {
					mSpinner.show();
				} else if (msg.what == 1) {
					mSpinner.dismiss();
					
					if (!canceled && proceedSend) {
						invokeSendProcess();
					} else {
						canceled = false;
						proceedSend = true;
					}
				}
			}
			
		}
    	
    };
    
    protected void clear() {
    	LogWrapper.d(TAG, "clear()");
    	unObserverMessage(mReceiver);
    	mInstance = null;
    }
}
