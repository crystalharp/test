package com.tigerknows.ui.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.AccountManage.UserRespnose;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Response;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;

/**
 * 用户模块基础类
 * 
 * 1. 提供输入验证支持
 * 2. 提供公共参数支持
 * 3. 提供服务器响应码处理支持
 * 
 * @author linqingzu
 *
 */
public abstract class UserBaseActivity extends BaseActivity {

    public static String SOURCE_VIEW_ID_LOGIN = "SOURCE_VIEW_ID_LOGIN";
    public static String TARGET_VIEW_ID_LOGIN_SUCCESS = "TARGET_VIEW_ID_LOGIN_SUCCESS";
    public static String TARGET_VIEW_ID_LOGIN_FAILED = "TARGET_VIEW_ID_LOGIN_FAILED";
    
    protected int mSourceViewIdLogin;
    
    protected int mTargetViewIdLoginSuccess;
    
    protected int mTargetViewIdLoginFailed;
    
	protected Form mForm;

	protected ResponseHandler mResponseHandler = new ResponseHandler();
	
	protected ViewGroup mRootView;
	
	private static final String TAG = "UserBaseDialog";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
    protected void onNewIntent(Intent newIntent) {
        // TODO Auto-generated method stub
        super.onNewIntent(newIntent);
    }
	
	protected void getViewId(Intent intent) {
	    super.getViewId(intent);
	    mSourceViewIdLogin = intent.getIntExtra(SOURCE_VIEW_ID_LOGIN, R.id.view_invalid);
        mTargetViewIdLoginSuccess = intent.getIntExtra(TARGET_VIEW_ID_LOGIN_SUCCESS, R.id.view_invalid);
        mTargetViewIdLoginFailed = intent.getIntExtra(TARGET_VIEW_ID_LOGIN_FAILED, R.id.view_invalid);
    }
    
    protected void putViewId(Intent intent) {
        super.putViewId(intent);
        intent.putExtra(SOURCE_VIEW_ID_LOGIN, mSourceViewIdLogin);
        intent.putExtra(TARGET_VIEW_ID_LOGIN_SUCCESS, mTargetViewIdLoginSuccess);
        intent.putExtra(TARGET_VIEW_ID_LOGIN_FAILED, mTargetViewIdLoginFailed);
    }

    @Override
	protected void findViews() {
		// TODO Auto-generated method stub
		super.findViews();
		mRootView = (ViewGroup)findViewById(R.id.root_view);
		mForm = new Form(mRootView);
	}
	
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
	}

	/**
	 * 提供输入验证支持
	 * 
	 * @author linqingzu
	 *
	 */
	class Form {

		private List<ExtValidationEditText> childEditTxts = new ArrayList<ExtValidationEditText>();
		
		private ExtValidationEditText mErrorSource = null;
		
	    private Comparator<ExtValidationEditText> mComparator = new Comparator<ExtValidationEditText>() {

	        @Override
	        public int compare(ExtValidationEditText object1, ExtValidationEditText object2) {
	            return (int)(object1.flag - object2.flag);
	        };
	    };
	    
	    private final static String TAG = "FormView";

		public Form(ViewGroup group) {
			mineValidationEdt(group);
		}
		
		public void mineValidationEdt(ViewGroup group) {
			for(int i = 0; i < group.getChildCount(); i++) {
				View view = group.getChildAt(i);
				if (view instanceof ExtValidationEditText) {
					childEditTxts.add((ExtValidationEditText)view);
				} else if (view instanceof ViewGroup) {
					mineValidationEdt((ViewGroup)view);
				}
			}
			LogWrapper.d(TAG, "childEditTxts: " + childEditTxts);
		}
		
		/**
		 * 验证所有的输入框是否符合要求
		 * @return
		 */
		public boolean isValid() {
			Collections.sort(childEditTxts, mComparator);

			for(ExtValidationEditText child : childEditTxts) {
				child.setUpRelation(childEditTxts);
				if(!child.isValid()) {
					mErrorSource = child;
					return false;
				}
			}
			
			return true;
		}
		
		public ExtValidationEditText getErrorSource() {
			return mErrorSource;
		}
		
	}
	
	/**
	 * 提供公共参数支持
	 */
	protected void sendRequest(AccountManage accountManage, Hashtable<String, String> criteria) {
		hideSoftInput();
		accountManage.setup(criteria, getCityParameter());
		accountManage.setTipText(getString(R.string.query_loading_tip));
		queryStart(accountManage, false);
	}

	/**
	 * 获取城市ID参数, 若定到位, 使用定位城市; 若无定位信息, 使用当前地图城市
	 * @return
	 */
	private int getCityParameter() {
		int cityId = MapEngine.CITY_ID_INVALID;
		
		CityInfo mylocaCityInfo = Globals.g_My_Location_City_Info;
		if (mylocaCityInfo != null) {
			cityId = mylocaCityInfo.getId();
		}
		
		CityInfo myCurrentCityInfo = Globals.g_Current_City_Info;
		if (cityId == MapEngine.CITY_ID_INVALID && myCurrentCityInfo != null) {
			cityId = myCurrentCityInfo.getId();
		}
		LogWrapper.d(TAG, "cityId: " + cityId);
		
		return cityId;
	}
	
	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
		// TODO Auto-generated method stub
		super.onPostExecute(tkAsyncTask);
		BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (BaseActivity.checkReLogin(baseQuery, mThis, true, mId, R.id.view_user_home, R.id.view_more_home, null)) {
            return;
        }
		
		Response response = baseQuery.getResponse();
		if (response != null) {
    		AccountManage accountManage = (AccountManage)baseQuery;
        	dealResponse(accountManage);
		} else {
//            Toast.makeText(mThis, R.string.network_failed, Toast.LENGTH_LONG).show();
			responseBadNetwork();
		}
	}
	
	/**
	 * 对外公开的处理服务返回的响应码
	 * @param accountManage
	 */
	public void dealResponse(AccountManage accountManage) {
		if (accountManage != null && accountManage.getResponse() != null) {
			responseCodeAction(accountManage);
		} else {
			LogWrapper.d(TAG, "response is null");
		}
	}
	
	/**
	 * 提供服务器响应码处理支持
	 * @author linqingzu
	 *
	 */
	public class ResponseHandler {
		
		/**
		 * 显示错误提示框, 点击确定后, 全选输入框的内容, 并弹出键盘
		 * @param title
		 * @param body
		 * @param edittext
		 */
		public void showErrorThenSelectAllAndShowSoftkeyboard(int titleResId, int bodyResId, final ExtValidationEditText edittext) {
			Utility.showNormalDialog(UserBaseActivity.this, getString(titleResId), getString(bodyResId), getString(R.string.confirm),
	                null,
	                new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog,
						int which) {
					// TODO Auto-generated method stub
					if (which == DialogInterface.BUTTON_POSITIVE) {
						edittext.selectAll();
						edittext.requestFocus();
						showSoftInput(edittext);
					}
				}
				
			});
		}
		
		/**
		 * 显示错误提示框, 点击确定后, 清除输入框的内容, 并弹出键盘
		 * @param title
		 * @param body
		 * @param edittext
		 */
		public void showErrorThenClearAndShowSoftkeyboard(int titleResId, int bodyResId, final ExtValidationEditText edittext) {
			Utility.showNormalDialog(UserBaseActivity.this, getString(titleResId), getString(bodyResId), getString(R.string.confirm),
	                null,
	                new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog,
						int which) {
					// TODO Auto-generated method stub
					if (which == DialogInterface.BUTTON_POSITIVE) {
						edittext.setText("");
						edittext.requestFocus();
						showSoftInput(edittext);
					}
				}
				
			});
		}
		
	}
	
	protected abstract void responseCodeAction(AccountManage accountManage);
	
	protected void responseBadNetwork() {
		Toast.makeText(mThis, R.string.network_failed, Toast.LENGTH_LONG).show();
	}
	
	protected void showToast(int resId) {
		Toast.makeText(this, resId, 2000).show();
	}
    
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        putViewId(intent);
        super.startActivityForResult(intent, requestCode);
    }
        
    @Override
    public void startActivity(Intent intent) {
        putViewId(intent);
        super.startActivity(intent);
    }
}
